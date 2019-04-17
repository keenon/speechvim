import grpc
import concurrent.futures
import deepspeech_pb2
import deepspeech_pb2_grpc
from deepspeech_pb2 import Request, StreamingResult
from queue import Queue


class GrpcDeepSpeechServicer(deepspeech_pb2_grpc.DeepSpeechServicer):
    """Service that implements Google Cloud Speech API.
    """
    def __init__(self):
        self.queue = Queue()

    def emit_utterance(self, utterance: str, intermediate: bool):
        result = StreamingResult(text=utterance, intermediate=intermediate)
        self.queue.put(result)

    def SpeechStream(self, request, context):
        print('New speech stream connected. Clearing queue')
        self.queue = Queue()
        """Performs bidirectional streaming speech recognition: receive results while
        sending audio. This method is only available via the gRPC API (not REST).
        """
        for item in iter(self.queue.get, None):
            yield item


class Server:
    def __init__(self):
        self.speech_stub = GrpcDeepSpeechServicer()

        # GRPC
        self.grpc_server = grpc.server(
            concurrent.futures.ThreadPoolExecutor(max_workers=3),  # note[gabor] Don't let this get higher than # cores
            # Partly, this is to prevent high load, but more
            # importantly this prevents memory growing too much
            maximum_concurrent_rpcs=16384                          # note[gabor] This can be huge -- they just queue :)
        )
        deepspeech_pb2_grpc.add_DeepSpeechServicer_to_server(self.speech_stub, self.grpc_server)
        self.grpc_server.add_insecure_port("[::]:{port}".format(port=5109))
        self.grpc_server.start()

    def emit_utterance(self, utterance: str, intermediate: bool):
        self.speech_stub.emit_utterance(utterance, intermediate)
