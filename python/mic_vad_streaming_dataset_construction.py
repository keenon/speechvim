import time, logging
from datetime import datetime
import threading, collections, queue, os, os.path
import wave
import pyaudio
import webrtcvad
import numpy as np
import sys
import csv

logging.basicConfig(level=20)

class Audio(object):
    """Streams raw audio from microphone. Data is received in a separate thread, and stored in a buffer, to be read from."""

    FORMAT = pyaudio.paInt16
    RATE = 16000
    CHANNELS = 1
    BLOCKS_PER_SECOND = 50
    BLOCK_SIZE = int(RATE / float(BLOCKS_PER_SECOND))

    def __init__(self, callback=None):
        def proxy_callback(in_data, frame_count, time_info, status):
            callback(in_data)
            return (None, pyaudio.paContinue)
        if callback is None: callback = lambda in_data: self.buffer_queue.put(in_data)
        self.buffer_queue = queue.Queue()
        self.sample_rate = self.RATE
        self.block_size = self.BLOCK_SIZE
        self.pa = pyaudio.PyAudio()
        self.stream = self.pa.open(format=self.FORMAT,
                                   channels=self.CHANNELS,
                                   rate=self.sample_rate,
                                   input=True,
                                   frames_per_buffer=self.block_size,
                                   stream_callback=proxy_callback)
        self.stream.start_stream()

    def read(self):
        """Return a block of audio data, blocking if necessary."""
        return self.buffer_queue.get()

    def destroy(self):
        self.stream.stop_stream()
        self.stream.close()
        self.pa.terminate()

    frame_duration_ms = property(lambda self: 1000 * self.block_size // self.sample_rate)

    def write_wav(self, filename, data):
        logging.info("write wav %s", filename)
        wf = wave.open(filename, 'wb')
        wf.setnchannels(self.CHANNELS)
        # wf.setsampwidth(self.pa.get_sample_size(FORMAT))
        assert self.FORMAT == pyaudio.paInt16
        wf.setsampwidth(2)
        wf.setframerate(self.sample_rate)
        wf.writeframes(data)
        wf.close()

class VADAudio(Audio):
    """Filter & segment audio with voice activity detection."""

    def __init__(self, aggressiveness=3):
        super().__init__()
        self.vad = webrtcvad.Vad(aggressiveness)

    def frame_generator(self):
        """Generator that yields all audio frames from microphone."""
        while True:
            yield self.read()

    def vad_collector(self, padding_ms=300, ratio=0.75, frames=None):
        """Generator that yields series of consecutive audio frames comprising each utterence, separated by yielding a single None.
            Determines voice activity by ratio of frames in padding_ms. Uses a buffer to include padding_ms prior to being triggered.
            Example: (frame, ..., frame, None, frame, ..., frame, None, ...)
                      |---utterence---|        |---utterence---|
        """
        if frames is None: frames = self.frame_generator()
        num_padding_frames = padding_ms // self.frame_duration_ms
        ring_buffer = collections.deque(maxlen=num_padding_frames)
        triggered = False

        for frame in frames:
            is_speech = self.vad.is_speech(frame, self.sample_rate)

            if not triggered:
                ring_buffer.append((frame, is_speech))
                num_voiced = len([f for f, speech in ring_buffer if speech])
                if num_voiced > ratio * ring_buffer.maxlen:
                    triggered = True
                    for f, s in ring_buffer:
                        yield f
                    ring_buffer.clear()

            else:
                yield frame
                ring_buffer.append((frame, is_speech))
                num_unvoiced = len([f for f, speech in ring_buffer if not speech])
                if num_unvoiced > ratio * ring_buffer.maxlen:
                    triggered = False
                    yield None
                    ring_buffer.clear()

def query_yes_no(question, default="yes"):
    """Ask a yes/no question via raw_input() and return their answer.

    "question" is a string that is presented to the user.
    "default" is the presumed answer if the user just hits <Enter>.
        It must be "yes" (the default), "no" or None (meaning
        an answer is required of the user).

    The "answer" return value is True for "yes" or False for "no".
    """
    valid = {"yes": True, "y": True, "ye": True,
             "no": False, "n": False}
    if default is None:
        prompt = " [y/n] "
    elif default == "yes":
        prompt = " [Y/n] "
    elif default == "no":
        prompt = " [y/N] "
    else:
        raise ValueError("invalid default answer: '%s'" % default)

    while True:
        sys.stdout.write(question + prompt)
        choice = input().lower()
        if default is not None and choice == '':
            return valid[default]
        elif choice in valid:
            return valid[choice]
        else:
            sys.stdout.write("Please respond with 'yes' or 'no' "
                             "(or 'y' or 'n').\n")

def main(ARGS):
    previous_progress = 0
    csv_file_path = os.path.join(ARGS.output, "dataset.csv")
    try:
        with open(csv_file_path, 'r') as csv_file:
            csv = csv_file.readlines()
        previous_progress = len(csv)-1
    except FileNotFoundError:
        with open(csv_file_path, 'w') as csv_file:
            csv_file.write("wav_filename,wav_filesize,transcript\n")
        pass

    with open(ARGS.input, 'r') as infile:
        content = infile.readlines()

    # Start audio with VAD
    vad_audio = VADAudio(aggressiveness=ARGS.vad_aggressiveness)
    index = previous_progress
    while True:
        if index >= len(content):
            break
        l = content[index].rstrip().lower()
        vad_audio.destroy()
        vad_audio = VADAudio(aggressiveness=ARGS.vad_aggressiveness)
        frames = vad_audio.vad_collector()
        print("Listening (ctrl-C to exit)...")
        print("Please say: \""+l+"\"")

        # Stream from microphone to DeepSpeech using VAD
        started = False
        wav_data = bytearray()
        for frame in frames:
            if frame is not None:
                started = True
                wav_data.extend(frame)
            elif started:
                print("Finished utterance")
                if query_yes_no("Are you happy with that recording?"):
                    index += 1
                    # Example CSV for training:
                    #
                    # wav_filename,wav_filesize,transcript
                    # /Users/keenon/Desktop/DeepSpeech/data/ldc93s1/LDC93S1.wav,93638,she had your dark suit in greasy wash water all year
                    #
                    wav_path = os.path.abspath(os.path.join(ARGS.output, datetime.now().strftime("wavpath_%Y-%m-%d_%H-%M-%S_%f.wav")))
                    vad_audio.write_wav(wav_path, wav_data)
                    with open(csv_file_path, 'a') as csv_file:
                        csv_file.write(wav_path+","+str(len(wav_data))+","+l+"\n")
                break
    print('Finished creating dataset!')

if __name__ == '__main__':
    BEAM_WIDTH = 500
    LM_ALPHA = 0.75
    LM_BETA = 1.85
    N_FEATURES = 26
    N_CONTEXT = 9

    import argparse
    parser = argparse.ArgumentParser(description="Stream from microphone to DeepSpeech using VAD")

    parser.add_argument('-v', '--vad_aggressiveness', type=int, default=3,
        help="Set aggressiveness of VAD: an integer between 0 and 3, 0 being the least aggressive about filtering out non-speech, 3 the most aggressive. Default: 3")
    parser.add_argument('-i', '--input',
        help="The text file that we're going to read aloud (one utterance per line)", default='../resources/custom_lm_training.txt')
    parser.add_argument('-o', '--output',
                        help="The folder where we'll save the dataset we're creating", default="../resources/custom_lm_dataset")

    ARGS = parser.parse_args()
    if ARGS.output: os.makedirs(ARGS.output, exist_ok=True)
    main(ARGS)
