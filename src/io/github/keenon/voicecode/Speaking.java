package io.github.keenon.voicecode;

import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.protobuf.ByteString;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiShortNamesCache;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.github.keenon.voicecode.*;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * Created by keenon on 11/15/17.
 *
 * This manages the basic plumbing associated with speech recognition for coding on the IntelliJ platform.
 */
public class Speaking extends AnAction {
  boolean toggled = false;
  private int toggleEpoch = 0; // For if we rapidly toggle on and off, make sure old threads still die

  private final Queue<AudioResponse> audioResponses = new ArrayDeque<>();

  private static class AudioResponse {
    String text;
    boolean intermediate;

    public AudioResponse(String text, boolean intermediate) {
      this.text = text;
      this.intermediate = intermediate;
    }
  }

  private static final String API_KEY = "AIzaSyD8tsiM0DqcUWT1WelGzHHSivRQ0nT3XyY";

  private StreamingTranscription streamingTranscription = new StreamingTranscription();

  /**
   * This is the thread that manages reading from the microphone
   */
  void googleAudioThread(int epoch) {

    // Set up static configuration for speech recognition

    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
        .setSampleRateHertz(16000)
        .setLanguageCode("en-US")
        .setProfanityFilter(false)
        .addSpeechContexts(SpeechContext.newBuilder()
            .addPhrases("int")
            .addPhrases("integer")
            .addPhrases("a")
            .addPhrases("b")
            .addPhrases("c")
            .addPhrases("d")
            .addPhrases("e")
            .addPhrases("f")
            .addPhrases("g")
            .addPhrases("h")
            .addPhrases("i")
            .addPhrases("j")
            .addPhrases("k")
            .addPhrases("paren")
            .addPhrases("integer i")
            .addPhrases("integer j")
            .addPhrases("integer k")
            .addPhrases("plus plus")
            .addPhrases("semicolon")
            .addPhrases("plus equals")
            .addPhrases("equals")
        )
        .build();
    StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
        .setConfig(config)
        .setInterimResults(false)
        .setSingleUtterance(true)
        .build();

    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("speech.googleapis.com", 443)
        .intercept(new ApiKeyInterceptor(API_KEY))
        .build();
    SpeechGrpc.SpeechStub stub = SpeechGrpc.newStub(channel);

    // Open the streaming gRPC to Google - This will automatically reopen itself whenever the stream reaches the end and
    // closes or when the user stops talking.

    Optional<StreamObserver<StreamingRecognizeRequest>>[] speechStreamObserver = (Optional<StreamObserver<StreamingRecognizeRequest>>[])new Optional[]{Optional.empty()};
    final Runnable refreshStreamObserver[] = new Runnable[]{null};
    refreshStreamObserver[0] = () -> {
      System.out.println("Refreshing the stream observer");

      // Complete the previous speech stream request, if any is present
      if (speechStreamObserver[0].isPresent()) {
        speechStreamObserver[0].get().onCompleted();
      }

      speechStreamObserver[0] = Optional.of(stub.streamingRecognize(new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse streamingRecognizeResponse) {
          if (streamingRecognizeResponse.getSpeechEventType() == StreamingRecognizeResponse.SpeechEventType.END_OF_SINGLE_UTTERANCE) {
            System.out.println("Detected end of utterance. Refreshing...");

            if (toggled && toggleEpoch == epoch) {
              refreshStreamObserver[0].run();
              return;
            }
          }

          System.out.println("Got onNext() call");

          synchronized (audioResponses) {
            List<StreamingRecognitionResult> results = streamingRecognizeResponse.getResultsList();
            for (StreamingRecognitionResult result : results) {
              if (result.getAlternativesCount() > 0) {
                String text = result.getAlternatives(0).getTranscript();
                audioResponses.add(new AudioResponse(text, !result.getIsFinal()));
              }
            }

            audioResponses.notifyAll();
          }
        }

        @Override
        public void onError(Throwable throwable) {
          System.err.println("[Recognition Error]: "+throwable);
          throwable.printStackTrace();

          if (toggled && toggleEpoch == epoch) {
            refreshStreamObserver[0].run();
          }
        }

        @Override
        public void onCompleted() {
          System.err.println("[Recognition Completed]");
        }
      }));

      // The first request must *only* contain the audio configuration, and no audio data

      speechStreamObserver[0].get().onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build());
    };
    refreshStreamObserver[0].run();

    // Open the stream to the microphone

    AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
    try {
      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
      TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
      microphone.open(format);
      microphone.start();

      int CHUNK_SIZE = 1024;
      assert(CHUNK_SIZE < microphone.getBufferSize() / 5);
      byte[] data = new byte[CHUNK_SIZE * 64];
      int offset = 0;

      while (toggled && toggleEpoch == epoch) {
        int numBytesRead = microphone.read(data, offset, CHUNK_SIZE);
        offset += numBytesRead;

        if (offset >= data.length) {
          offset = 0;

          if (speechStreamObserver[0].isPresent()) {

            // These subsequent requests must *only* contain the audio data, and no configuration

            speechStreamObserver[0].get().onNext(
              StreamingRecognizeRequest.newBuilder()
                  .setAudioContent(ByteString.copyFrom(data, 0, data.length))
                  .build()
            );
          }
        }
      }

      speechStreamObserver[0].ifPresent(StreamObserver::onCompleted);
      microphone.close();
    } catch (LineUnavailableException e) {
      e.printStackTrace();
    }
  }

  private Optional<ManagedChannel> managedChannel = Optional.empty();

  /**
   * This is the thread that manages reading from the microphone
   */
  void startPythonAudio() {
    System.out.println("Starting python audio channel");
    managedChannel = Optional.of(ManagedChannelBuilder
        .forAddress("localhost", 5109)
        .usePlaintext(true)
        .build());
    DeepSpeechGrpc.DeepSpeechStub stub = DeepSpeechGrpc.newStub(managedChannel.get());

    stub.speechStream(Request.newBuilder().build(), new StreamObserver<StreamingResult>() {
      @Override
      public void onNext(StreamingResult value) {
        System.out.println("Got python audio recognition: \""+value.getText()+"\"");
        synchronized (audioResponses) {
          audioResponses.add(new AudioResponse(value.getText(), value.getIntermediate()));
          audioResponses.notifyAll();
        }
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        System.out.println("Completed");
      }
    });
  }

  void endPythonAudio() {
    managedChannel.get().shutdown();
    managedChannel = Optional.empty();
  }

  /**
   * This is the thread that consumes from the speech recognition queue
   */
  private void consumerThread(int epoch) {
    while (toggled && toggleEpoch == epoch) {

      // Wait until there is a streaming response available

      synchronized (audioResponses) {
        while (audioResponses.size() == 0) {
          try {
            audioResponses.wait(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }

      if (!toggled) return;
      if (EditorFactory.getInstance().getAllEditors().length == 0) continue;

      // This magical nested invocation executes on the UI thread with a write lock on the app
      ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(this::consumeSpeechQueue));
    }
  }

  private void handleKey(KeyStroke key) {
    Editor editor = EditorFactory.getInstance().getAllEditors()[0];
    KeyHandler.getInstance().handleKey(editor, key, DataContext.EMPTY_CONTEXT);
  }

  private static final KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
  private static final KeyStroke returnStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
  private static final PatternTranscriber transcriber = new PatternTranscriber();

  /**
   * This executes with a write lock for all of IDEA, so it has to be fast, or we'll freeze the editor.
   */
  private void consumeSpeechQueue() {

    // Get the latest streaming response, if any are present

    String text;
    boolean intermediate;

    synchronized (audioResponses) {
      if (audioResponses.size() == 0) return;

      AudioResponse audioResponse = audioResponses.poll();
      text = audioResponse.text;
      intermediate = audioResponse.intermediate;
    }

    // Get handles on every IDE object that we care about

    Project project = ProjectManager.getInstance().getOpenProjects()[0];
    Editor editor = EditorFactory.getInstance().getAllEditors()[0];
    final CommandState editorState = CommandState.getInstance(editor);

    if (editor == null) return;

    // Pass along the transcription as keystrokes

    if (!intermediate) {
      System.out.println("Processing \"" + text + "\"");

      if (text.trim().contains("escape") && editorState.getMode() == CommandState.Mode.INSERT) {
        System.out.println("Pressing \"escape\"");
        KeyHandler.getInstance().handleKey(editor, escapeStroke, new EditorDataContext(editor));
        return;
      } else if (text.trim().contains("insert") && editorState.getMode() == CommandState.Mode.COMMAND) {
        System.out.println("Pressing \"i\"");
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('i'), new EditorDataContext(editor));
        return;
      } else if (text.trim().contains("insert new line") && editorState.getMode() == CommandState.Mode.COMMAND) {
        System.out.println("Pressing \"o\"");
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('o'), new EditorDataContext(editor));
        return;
      } else if (text.trim().contains("down") && editorState.getMode() == CommandState.Mode.COMMAND) {
        System.out.println("Pressing \"j\"");
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('j'), new EditorDataContext(editor));
        return;
      } else if (text.trim().contains("up") && editorState.getMode() == CommandState.Mode.COMMAND) {
        System.out.println("Pressing \"k\"");
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('k'), new EditorDataContext(editor));
        return;
      } else if (text.trim().contains("copy") && editorState.getMode() == CommandState.Mode.COMMAND) {
        System.out.println("Pressing \"yy\"");
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('y'), new EditorDataContext(editor));
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('y'), new EditorDataContext(editor));
        return;
      } else if (text.trim().contains("paste") && editorState.getMode() == CommandState.Mode.COMMAND) {
        System.out.println("Pressing \"p\"");
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('p'), new EditorDataContext(editor));
        return;
      }
    }

    // If we're in insert mode then work on transcribing this

    if (editorState.getMode() == CommandState.Mode.INSERT) {
      System.out.println("Transcribing (intermediate="+intermediate+"): \""+text+"\"");

      // Escape is a reserved keyword, so don't transcribe that as an intermediate transcription
      if (intermediate && text.trim().equalsIgnoreCase("escape")) return;

      Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
      int startOffset = primaryCaret.getSelectionStart();

      String toType = transcriber.transcribe(text).orElse(text);

      /*
      if (!transcription.isPresent()) {
        Notifications.Bus.notify(new Notification("Voicecode", "Failed to parse", streamingTranscription.getUnparsedTokens(), NotificationType.WARNING));
      }
      */

      for (int i = 0; i < toType.length(); i++) {
        if (toType.charAt(i) == '\n') {
          KeyHandler.getInstance().handleKey(editor, returnStroke, new EditorDataContext(editor));
        }
        else {
          KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke(toType.charAt(i)), new EditorDataContext(editor));
        }
      }

      if (intermediate) {
        primaryCaret.setSelection(startOffset, primaryCaret.getOffset());
      }
    }

    /*
    final Document document = editor.getDocument();
    final SelectionModel selectionModel = editor.getSelectionModel();
    final IndentsModel indentsModel = editor.getIndentsModel();

    // Run the actual write command

    WriteCommandAction.runWriteCommandAction(project, () -> {

      ////////////////////////////////////////////////////////
      // Understand the document
      ////////////////////////////////////////////////////////

      PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
      PsiFile psiFile = psiDocumentManager.getPsiFile(document);

      ////////////////////////////////////////////////////////
      // Write raw text to the Editor
      ////////////////////////////////////////////////////////

      List<StreamingRecognitionResult> results = recognitionResult.getResultsList();
      for (StreamingRecognitionResult result : results) {

        if (result.getIsFinal() && result.getAlternativesCount() > 0) {
          streamingTranscription.streamText(result.getAlternatives(0).getTranscript().replaceAll(", ", " comma ").replaceAll(" \\.", " dot ").replaceAll(" = ", " equals "));

          Optional<String> transcription = streamingTranscription.getParsedExpression(Arrays.asList(PsiShortNamesCache.getInstance(project).getAllClassNames()));
          if (transcription.isPresent()) {
            int transcriptionEnd = selectionModel.getSelectionEnd();
            document.replaceString(selectionModel.getSelectionStart(), transcriptionEnd, transcription.get()+"\n");
            transcriptionEnd += transcription.get().length() + 1;
            editor.getCaretModel().moveToOffset(transcriptionEnd);
            int indentLevel = indentsModel.getCaretIndentGuide() != null ? indentsModel.getCaretIndentGuide().indentLevel : 0;
            for (int i = 0; i < indentLevel; i++) document.insertString(transcriptionEnd, "\t");
            editor.getCaretModel().moveToOffset(transcriptionEnd + indentLevel);
          }
          else {
            Notifications.Bus.notify(new Notification("Voicecode", "Failed to parse", streamingTranscription.getUnparsedTokens(), NotificationType.WARNING));
          }
        }
      }
    });
    */
  }

  @Override
  public void actionPerformed(AnActionEvent ignored) {
    toggled = !toggled;

    // Create our threads if we've been toggled on

    if (toggled) {
      Notifications.Bus.notify(new Notification("Voicecode", "Voicecode activated", "You may start speaking", NotificationType.INFORMATION));
      toggleEpoch++;

      // Every time we turn back on, clear everything

      streamingTranscription.clearTranscription();
      audioResponses.clear();

      // Spawn the consumer thread to read from the speech recognition queue and edit the text

      new Thread(() -> consumerThread(toggleEpoch)).start();

      // Spawn the audio thread to read from the microphone and add to the producer thread

      // new Thread(() -> googleAudioThread(toggleEpoch)).start();
      startPythonAudio();
    }
    else {
      Notifications.Bus.notify(new Notification("Voicecode", "Voicecode deactivated", "You will not be transcribed", NotificationType.INFORMATION));
      endPythonAudio();
    }
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(true);
    e.getPresentation().setText(toggled ? "Stop Speaking" : "Start Speaking");
  }
}
