#!/usr/bin/env bash

../bin/protoc --proto_path=../proto --python_out=. ../proto/deepspeech.proto
././../venv/bin/python3 -m grpc_tools.protoc --proto_path=../proto --grpc_python_out=. ../proto/deepspeech.proto
