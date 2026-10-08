#!/bin/bash
# Builds the static OpenSSL libraries in openssl/ for every Android ABI.
# Run from an OpenSSL source tree: OUTPUT_DIR=/path/to/openssl ./build-openssl.sh
# Needs ANDROID_NDK_HOME. Only libcrypto is used (moonlight-common-c's stream
# encryption in PlatformCrypto.c); libssl is kept so the makefile stays the same.
set -e

export ANDROID_NDK_ROOT=$ANDROID_NDK_HOME
PATH=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
OUTPUT_DIR=${OUTPUT_DIR:-~/openssl}
API=21

BASE_ARGS="no-shared no-module no-engine no-ssl3 no-comp no-tests no-apps no-docs"

for pair in android-arm:armeabi-v7a android-arm64:arm64-v8a android-x86:x86 android-x86_64:x86_64; do
    target=${pair%%:*}
    abi=${pair##*:}
    ./Configure $target $BASE_ARGS -D__ANDROID_API__=$API
    make clean
    make build_libs -j`nproc`
    mkdir -p $OUTPUT_DIR/$abi
    cp libcrypto.a libssl.a $OUTPUT_DIR/$abi/
done

rm -rf $OUTPUT_DIR/include
cp -R include $OUTPUT_DIR/include
