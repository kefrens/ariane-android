# Ariane

Ariane is an open source game streaming client for Android TV, Google TV and Android devices. It plays games from your PC through [Apollo](https://github.com/ClassicOldSong/Apollo) or [Sunshine](https://github.com/LizardByte/Sunshine), at home or over the internet.

It is built first for the living room: a 4K TV, a remote or a gamepad, and a receiver for surround sound. Phones and tablets keep working too.

## What's new in Ariane

- **A TV interface.** Home, apps, settings and the in-game menu were redesigned for the remote: a clear focus ring, D-pad paths that make sense, text readable from the sofa, and nothing in the TV's overscan area.
  - Home shows your hosts as cards, with the running game on top and an "Add host" card at the end of the row.
  - The apps screen shows box art, with Resume and Quit for the running game and a jump-to-letter search.
  - The in-game menu is a side panel that leaves the game visible.
  - Settings use two panes, with categories split into sections, a column of shortcuts to each section, and a search.
  - Hints show the real buttons of your controller (Xbox or PlayStation), and the controller's Menu button opens host options.
- **Surround sound passthrough.** Dolby Digital, DTS and TrueHD are sent to your receiver as a bitstream, instead of plain PCM.
- **TV game mode.** Ariane asks the TV for its low-latency game mode (ALLM) while you stream, and the stats overlay shows whether it was requested. You can turn it off in Settings > Video.
- **Updated encryption.** The stream is encrypted with OpenSSL 3.5 LTS, replacing OpenSSL 1.1.1, which stopped getting updates in 2023.
- **Performance.** Frame pacing follows your setting instead of being forced to Balanced, controller input reads its settings once instead of on every event, and the anti-packet-loss ping runs on its own thread.
- **Up-to-date Android build.** Ariane builds against the Android 16 SDK (API 36) and still runs on Android 5.0 and later.
- **Cleaner code.** The largest classes were split up, and every change is built and tested by GitHub Actions.

Ariane keeps everything Artemis added on top of Moonlight, including:

- Virtual display and server commands with Apollo, and clipboard sync
- Custom resolutions, bitrates and refresh rates
- Several mouse modes (multi-touch, trackpad, local cursor) and a virtual trackpad
- Custom on-screen buttons and keys, with import and export
- Video scale modes (fit, fill, stretch), pan and zoom
- External display mode and SBS 3D for 3D displays
- Joy-Con support, gamepad motion sensors and rumble options

## Download

- **Releases:** get the latest APK from the [Releases page](https://github.com/kefrens/ariane-android/releases).
  - Most TVs and streaming boxes, including the NVIDIA Shield, use the **arm64-v8a** APK.
  - Some TVs run a 32-bit system; if the arm64-v8a APK doesn't install, use **armeabi-v7a**.
- **Test builds:** every change on the main branch is built by GitHub Actions. With the [GitHub CLI](https://cli.github.com/), download a build and install it with adb:

  ```
  gh run download <run-id> -R kefrens/ariane-android -n artemis-debug-apks -D ariane
  adb install -r ariane/app-nonRoot_game-arm64-v8a-debug.apk
  ```

  Test builds install as **Vega**, next to Ariane, so they don't replace your main install.

## Building

1. Install Android Studio and the Android NDK.
2. Run `git submodule update --init --recursive` in the repository.
3. Optional, for surround passthrough: run `app/src/main/jni/ptenc/build-ffmpeg.sh` to build FFmpeg's encoders. Without them, the app plays PCM audio.
4. Build with Android Studio, or with `./gradlew assembleNonRoot_gameDebug`.

## A fork of a fork

Ariane builds on the work of two projects:

- [Moonlight Android](https://github.com/moonlight-stream/moonlight-android), the original client, by [Cameron Gutman](https://github.com/cgutman), [Diego Waxemberg](https://github.com/dwaxemberg), [Aaron Neyer](https://github.com/Aaronneyer) and [Andrew Hennessy](https://github.com/yetanothername). Moonlight started as a student project at Case Western Reserve University.
- [Artemis](https://github.com/ClassicOldSong/moonlight-android) (previously Moonlight Noir), by [ClassicOldSong](https://github.com/ClassicOldSong) and its contributors, which added Apollo integration and most of the features listed above.

Thank you to everyone who worked on them.

## License

Ariane is released under the GNU General Public License v3.0, like Moonlight. See [LICENSE.txt](LICENSE.txt).
