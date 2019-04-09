package io.github.keenon.voicecode;

import io.grpc.*;

/**
 * Created by keenon on 11/21/17.
 *
 * This authenticates a gRPC ManagedChannel using an API key.
 *
 * See https://stackoverflow.com/questions/40060656/authenticating-google-cloud-speech-via-grpc-on-android-using-an-api-key/44884023#44884023
 */
public class ApiKeyInterceptor implements ClientInterceptor {
  private final String apiKey;
  private static Metadata.Key<String> API_KEY_HEADER = Metadata.Key.of("x-goog-api-key", Metadata.ASCII_STRING_MARSHALLER);

  public ApiKeyInterceptor(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
    ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
    call = new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        if (apiKey != null && !apiKey.isEmpty()) {
          headers.put(API_KEY_HEADER, apiKey);
        }
        super.start(responseListener, headers);
      }
    };
    return call;
  }
}
