package io.github.keenon.voicecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This manages streaming a transcription of multiple fragments, where the transcription boundary doesn't cleanly line
 * up with the parsing boundary.
 */
public class StreamingTranscription {
  /**
   * An SLF4J Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(StreamingTranscription.class);

  private List<String> unparsedTokens = new ArrayList<>();

  /**
   * This streams tokens from a raw transcription text
   */
  public synchronized void streamText(String text) {
    streamTokens(text.trim().split(" "));
  }

  /**
   * This streams tokens from a transcription (usually had with a String.split())
   */
  public synchronized void streamTokens(String[] tokens) {
    unparsedTokens.addAll(Arrays.asList(tokens));
    if (unparsedTokens.size() > 50) clearTranscription();
  }

  /**
   * Clears the unparsed tokens in the transcription so far.
   */
  public synchronized void clearTranscription() {
    unparsedTokens.clear();
  }

  /**
   * This pops a single parsed expression off the list, if any are present. It consumes any tokens that were used in
   * parsing the expression.
   */
  public synchronized Optional<String> getParsedExpression(List<String> classNames) {
    String[] pointer = new String[]{null};

    Thread t = new Thread(() -> {
      long TIMEOUT = 500;
      long startTime = System.currentTimeMillis();
      // Go from largest to smallest, checking for a parse
      for (int i = unparsedTokens.size(); i >= 1; i--) {
        Optional<String> parse = TranscriptionToCode.translateTranscriptionWithGrammar(String.join(" ", unparsedTokens.subList(0, i)), classNames);
        if (parse.isPresent()) {
          unparsedTokens = unparsedTokens.subList(i, unparsedTokens.size());
          pointer[0] = parse.get();
          break;
        }

        if (System.currentTimeMillis() - startTime > TIMEOUT) {
          System.err.println("Hit a timeout processing a parsed expression");
          break;
        }
      }
    }, "get-parsed-expression");
    t.start();

    try {
      t.join(500);
    } catch (InterruptedException e) {
      System.out.println(Arrays.toString(t.getStackTrace()));
      System.err.println("Hit a timeout processing a parsed expression");
      e.printStackTrace();
    }

    return Optional.ofNullable(pointer[0]);
  }

  /**
   * This returns a list of tokens we haven't parsed yet, formatted with spaces, for displaying to the user.
   */
  public synchronized String getUnparsedTokens() {
    return String.join(" ", unparsedTokens);
  }
}
