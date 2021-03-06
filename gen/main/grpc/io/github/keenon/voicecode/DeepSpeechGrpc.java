package io.github.keenon.voicecode;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 * <pre>
 * Service that implements Google Cloud Speech API.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.7.0)",
    comments = "Source: deepspeech.proto")
public final class DeepSpeechGrpc {

  private DeepSpeechGrpc() {}

  public static final String SERVICE_NAME = "DeepSpeech";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<io.github.keenon.voicecode.Request,
      io.github.keenon.voicecode.StreamingResult> METHOD_SPEECH_STREAM =
      io.grpc.MethodDescriptor.<io.github.keenon.voicecode.Request, io.github.keenon.voicecode.StreamingResult>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
          .setFullMethodName(generateFullMethodName(
              "DeepSpeech", "SpeechStream"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              io.github.keenon.voicecode.Request.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              io.github.keenon.voicecode.StreamingResult.getDefaultInstance()))
          .setSchemaDescriptor(new DeepSpeechMethodDescriptorSupplier("SpeechStream"))
          .build();

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DeepSpeechStub newStub(io.grpc.Channel channel) {
    return new DeepSpeechStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DeepSpeechBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DeepSpeechBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DeepSpeechFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DeepSpeechFutureStub(channel);
  }

  /**
   * <pre>
   * Service that implements Google Cloud Speech API.
   * </pre>
   */
  public static abstract class DeepSpeechImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Performs bidirectional streaming speech recognition: receive results while
     * sending audio. This method is only available via the gRPC API (not REST).
     * </pre>
     */
    public void speechStream(io.github.keenon.voicecode.Request request,
        io.grpc.stub.StreamObserver<io.github.keenon.voicecode.StreamingResult> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SPEECH_STREAM, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SPEECH_STREAM,
            asyncServerStreamingCall(
              new MethodHandlers<
                io.github.keenon.voicecode.Request,
                io.github.keenon.voicecode.StreamingResult>(
                  this, METHODID_SPEECH_STREAM)))
          .build();
    }
  }

  /**
   * <pre>
   * Service that implements Google Cloud Speech API.
   * </pre>
   */
  public static final class DeepSpeechStub extends io.grpc.stub.AbstractStub<DeepSpeechStub> {
    private DeepSpeechStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DeepSpeechStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DeepSpeechStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DeepSpeechStub(channel, callOptions);
    }

    /**
     * <pre>
     * Performs bidirectional streaming speech recognition: receive results while
     * sending audio. This method is only available via the gRPC API (not REST).
     * </pre>
     */
    public void speechStream(io.github.keenon.voicecode.Request request,
        io.grpc.stub.StreamObserver<io.github.keenon.voicecode.StreamingResult> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_SPEECH_STREAM, getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Service that implements Google Cloud Speech API.
   * </pre>
   */
  public static final class DeepSpeechBlockingStub extends io.grpc.stub.AbstractStub<DeepSpeechBlockingStub> {
    private DeepSpeechBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DeepSpeechBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DeepSpeechBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DeepSpeechBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Performs bidirectional streaming speech recognition: receive results while
     * sending audio. This method is only available via the gRPC API (not REST).
     * </pre>
     */
    public java.util.Iterator<io.github.keenon.voicecode.StreamingResult> speechStream(
        io.github.keenon.voicecode.Request request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_SPEECH_STREAM, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Service that implements Google Cloud Speech API.
   * </pre>
   */
  public static final class DeepSpeechFutureStub extends io.grpc.stub.AbstractStub<DeepSpeechFutureStub> {
    private DeepSpeechFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DeepSpeechFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DeepSpeechFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DeepSpeechFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_SPEECH_STREAM = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DeepSpeechImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DeepSpeechImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SPEECH_STREAM:
          serviceImpl.speechStream((io.github.keenon.voicecode.Request) request,
              (io.grpc.stub.StreamObserver<io.github.keenon.voicecode.StreamingResult>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class DeepSpeechBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DeepSpeechBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.github.keenon.voicecode.DeepSpeechProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DeepSpeech");
    }
  }

  private static final class DeepSpeechFileDescriptorSupplier
      extends DeepSpeechBaseDescriptorSupplier {
    DeepSpeechFileDescriptorSupplier() {}
  }

  private static final class DeepSpeechMethodDescriptorSupplier
      extends DeepSpeechBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DeepSpeechMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DeepSpeechGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DeepSpeechFileDescriptorSupplier())
              .addMethod(METHOD_SPEECH_STREAM)
              .build();
        }
      }
    }
    return result;
  }
}
