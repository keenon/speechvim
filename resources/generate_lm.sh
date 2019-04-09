#!/usr/bin/env bash
lmplz -o 2 --discount_fallback 1 <custom_lm_training.txt >custom_lm.arpa
build_binary custom_lm.arpa custom_lm.binary
generate_trie ../python/model/alphabet.txt custom_lm.binary custom_trie
mv custom_lm.binary ../python/model
mv custom_trie ../python/model
rm custom_lm.arpa
