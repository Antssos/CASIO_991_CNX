#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
CORE=app/src/main/cpp/core
mkdir -p build/host
c++ -std=c++17 -g -O1 -fsanitize=address,undefined -fno-omit-frame-pointer -I"$CORE" tests/native_test.cpp "$CORE"/Chipset/*.cpp "$CORE"/Peripheral/*.cpp -o build/host/native-test
ASAN_OPTIONS=detect_leaks=1 build/host/native-test
