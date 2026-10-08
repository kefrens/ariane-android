# Ariane

A game streaming client for Android TV and Google TV, built for a 4K TV, a remote or a gamepad, and a surround receiver. It streams games from your PC through [Apollo](https://github.com/ClassicOldSong/Apollo) or [Sunshine](https://github.com/LizardByte/Sunshine). Phones and tablets work too.

## What's new

- **Material Design 3 TV interface.** Home, apps, settings and the in-game menu were redesigned for the remote, with a clear focus ring and nothing in the TV's overscan area.
- **Settings in sections.** Each category is split into titled sections with icons, a column of shortcuts and a search.
- **Controller-aware hints.** On-screen hints show your controller's real buttons (Xbox or PlayStation), and its Menu button opens host options.
- **Surround passthrough.** Dolby Digital, DTS and TrueHD go to your receiver as a bitstream.
- **TV game mode.** Ariane asks the TV for its low-latency game mode (ALLM) while you stream.
- **OpenSSL 3.5 LTS** encrypts the stream, replacing OpenSSL 1.1.1.
- **Performance fixes** for frame pacing, controller input and packet-loss handling.
- **Android 16 SDK** (API 36), still running on Android 5.0 and later.

Everything Artemis added on top of Moonlight is still there: Apollo virtual display and server commands, clipboard sync, custom resolutions and bitrates, mouse modes, custom on-screen keys, and more.

## Download

Get the latest APK from [Releases](https://github.com/kefrens/ariane-android/releases). Most TVs and boxes, including the NVIDIA Shield, use **arm64-v8a**. If it doesn't install, use **armeabi-v7a**.

Test builds of every change on `ariane-main` are on the [Actions](https://github.com/kefrens/ariane-android/actions/workflows/ci.yml) page. They install as **Vega**, next to Ariane.

## Building

```
git submodule update --init --recursive
app/src/main/jni/ptenc/build-ffmpeg.sh   # optional, for surround passthrough
./gradlew assembleNonRoot_gameDebug
```

You need Android Studio and the Android NDK.

To publish a release, push a tag like `v1.0.0`. The Release workflow builds signed APKs and attaches them to a GitHub release. It needs four repository secrets: `ARIANE_KEYSTORE_BASE64`, `ARIANE_KEYSTORE_PASSWORD`, `ARIANE_KEY_ALIAS` and `ARIANE_KEY_PASSWORD`.

## Credits

Ariane is a fork of a fork:

- [Artemis](https://github.com/ClassicOldSong/moonlight-android) by [ClassicOldSong](https://github.com/ClassicOldSong) and contributors
- [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) by Cameron Gutman, Diego Waxemberg, Aaron Neyer and Andrew Hennessy

Thank you to everyone who built them.

## License

[GPL v3](LICENSE.txt), like Moonlight.
