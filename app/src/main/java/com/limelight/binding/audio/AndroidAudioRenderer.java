package com.limelight.binding.audio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.AudioEffect;
import android.os.Build;

import com.limelight.LimeLog;
import com.limelight.nvstream.av.audio.AudioRenderer;
import com.limelight.nvstream.jni.MoonBridge;

public class AndroidAudioRenderer implements AudioRenderer {

    // Dolby Digital passthrough settings. 640 kbps is the highest AC-3 bitrate
    // and gives 2560 byte frames at 48 kHz (1536 samples = 32 ms per frame).
    private static final int AC3_BITRATE = 640000;
    private static final int AC3_FRAME_BYTES = 2560;

    private final Context context;
    private final boolean enableAudioFx;
    private final boolean ac3Passthrough;
    private AudioTrack track;

    // Non-null when we're encoding to AC-3 and writing a passthrough bitstream
    private Ac3Encoder ac3Encoder;
    private boolean loggedWriteError;

    public AndroidAudioRenderer(Context context, boolean enableAudioFx) {
        this(context, enableAudioFx, false);
    }

    public AndroidAudioRenderer(Context context, boolean enableAudioFx, boolean ac3Passthrough) {
        this.context = context;
        this.enableAudioFx = enableAudioFx;
        this.ac3Passthrough = ac3Passthrough;
    }

    private AudioTrack createAudioTrack(int channelConfig, int sampleRate, int bufferSize, boolean lowLatency) {
        AudioAttributes.Builder attributesBuilder = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME);
        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Use FLAG_LOW_LATENCY on L through N
            if (lowLatency) {
                attributesBuilder.setFlags(AudioAttributes.FLAG_LOW_LATENCY);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioTrack.Builder trackBuilder = new AudioTrack.Builder()
                    .setAudioFormat(format)
                    .setAudioAttributes(attributesBuilder.build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(bufferSize);

            // Use PERFORMANCE_MODE_LOW_LATENCY on O and later
            if (lowLatency) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
            }

            return trackBuilder.build();
        }
        else {
            return new AudioTrack(attributesBuilder.build(),
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
        }
    }

    private static AudioTrack createAc3AudioTrack(AudioFormat format, AudioAttributes attributes, int bufferSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // No low latency performance mode here: it's only available for PCM
            return new AudioTrack.Builder()
                    .setAudioFormat(format)
                    .setAudioAttributes(attributes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(bufferSize)
                    .build();
        }
        else {
            return new AudioTrack(attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
        }
    }

    // Tries to set up an AC-3 passthrough AudioTrack fed by our AC-3 encoder.
    // Returns false (leaving no track or encoder behind) if that isn't possible,
    // in which case the caller falls back to PCM.
    private boolean setupAc3Passthrough(MoonBridge.AudioConfiguration audioConfiguration, int sampleRate) {
        if (!Ac3Encoder.isAvailable()) {
            LimeLog.warning("AC-3 passthrough: encoder library not included in this build");
            return false;
        }

        // Same attributes as video players use, which is what TV audio HALs expect
        // to route to a direct/passthrough HDMI output.
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build();

        // Some HALs only accept compressed formats declared as stereo
        int[] channelMasks = { AudioFormat.CHANNEL_OUT_5POINT1, AudioFormat.CHANNEL_OUT_STEREO };

        for (int channelMask : channelMasks) {
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_AC3)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !AudioTrack.isDirectPlaybackSupported(format, attributes)) {
                LimeLog.info(String.format("AC-3 passthrough: direct playback not supported with channel mask 0x%X", channelMask));
                continue;
            }

            // AudioFlinger won't start a streaming track until its buffer is full,
            // so the buffer size is effectively our output latency. Try a small
            // buffer first, then whatever the platform says the minimum is.
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_AC3);
            int[] bufferSizes = { AC3_FRAME_BYTES * 2, Math.max(minBufferSize, AC3_FRAME_BYTES * 4) };

            for (int bufferSize : bufferSizes) {
                try {
                    track = createAc3AudioTrack(format, attributes, bufferSize);
                    if (track.getState() != AudioTrack.STATE_INITIALIZED) {
                        throw new IllegalStateException("AudioTrack not initialized");
                    }

                    ac3Encoder = new Ac3Encoder(sampleRate, audioConfiguration.channelCount, AC3_BITRATE);
                    track.play();

                    LimeLog.info(String.format("AC-3 passthrough enabled: mask 0x%X, buffer %d bytes, %d kbps",
                            channelMask, bufferSize, AC3_BITRATE / 1000));
                    return true;
                } catch (Exception e) {
                    LimeLog.warning(String.format("AC-3 passthrough: mask 0x%X, buffer %d failed: %s",
                            channelMask, bufferSize, e));
                    releaseAc3();
                }
            }
        }

        return false;
    }

    private void releaseAc3() {
        if (track != null) {
            try {
                track.release();
            } catch (Exception ignored) {}
            track = null;
        }
        if (ac3Encoder != null) {
            ac3Encoder.release();
            ac3Encoder = null;
        }
    }

    @Override
    public int setup(MoonBridge.AudioConfiguration audioConfiguration, int sampleRate, int samplesPerFrame) {
        int channelConfig;
        int bytesPerFrame;

        // AC-3 is only worth it for multichannel audio; stereo PCM works everywhere
        if (ac3Passthrough && audioConfiguration.channelCount == 6) {
            if (setupAc3Passthrough(audioConfiguration, sampleRate)) {
                return 0;
            }
            LimeLog.warning("AC-3 passthrough unavailable, falling back to PCM");
        }

        switch (audioConfiguration.channelCount)
        {
            case 2:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            case 4:
                channelConfig = AudioFormat.CHANNEL_OUT_QUAD;
                break;
            case 6:
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                break;
            case 8:
                // AudioFormat.CHANNEL_OUT_7POINT1_SURROUND isn't available until Android 6.0,
                // yet the CHANNEL_OUT_SIDE_LEFT and CHANNEL_OUT_SIDE_RIGHT constants were added
                // in 5.0, so just hardcode the constant so we can work on Lollipop.
                channelConfig = 0x000018fc; // AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
                break;
            default:
                LimeLog.severe("Decoder returned unhandled channel count");
                return -1;
        }

        LimeLog.info("Audio channel config: "+String.format("0x%X", channelConfig));

        bytesPerFrame = audioConfiguration.channelCount * samplesPerFrame * 2;

        // We're not supposed to request less than the minimum
        // buffer size for our buffer, but it appears that we can
        // do this on many devices and it lowers audio latency.
        // We'll try the small buffer size first and if it fails,
        // use the recommended larger buffer size.

        for (int i = 0; i < 4; i++) {
            boolean lowLatency;
            int bufferSize;

            // We will try:
            // 1) Small buffer, low latency mode
            // 2) Large buffer, low latency mode
            // 3) Small buffer, standard mode
            // 4) Large buffer, standard mode

            switch (i) {
                case 0:
                case 1:
                    lowLatency = true;
                    break;
                case 2:
                case 3:
                    lowLatency = false;
                    break;
                default:
                    // Unreachable
                    throw new IllegalStateException();
            }

            switch (i) {
                case 0:
                case 2:
                    bufferSize = bytesPerFrame * 2;
                    break;

                case 1:
                case 3:
                    // Try the larger buffer size
                    bufferSize = Math.max(AudioTrack.getMinBufferSize(sampleRate,
                                    channelConfig,
                                    AudioFormat.ENCODING_PCM_16BIT),
                            bytesPerFrame * 2);

                    // Round to next frame
                    bufferSize = (((bufferSize + (bytesPerFrame - 1)) / bytesPerFrame) * bytesPerFrame);
                    break;
                default:
                    // Unreachable
                    throw new IllegalStateException();
            }

            // Skip low latency options if hardware sample rate doesn't match the content
            if (AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) != sampleRate && lowLatency) {
                continue;
            }

            // Skip low latency options when using audio effects, since low latency mode
            // precludes the use of the audio effect pipeline (as of Android 13).
            if (enableAudioFx && lowLatency) {
                continue;
            }

            try {
                track = createAudioTrack(channelConfig, sampleRate, bufferSize, lowLatency);
                track.play();

                // Successfully created working AudioTrack. We're done here.
                LimeLog.info("Audio track configuration: "+bufferSize+" "+lowLatency);
                break;
            } catch (Exception e) {
                // Try to release the AudioTrack if we got far enough
                e.printStackTrace();
                try {
                    if (track != null) {
                        track.release();
                        track = null;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (track == null) {
            // Couldn't create any audio track for playback
            return -2;
        }

        return 0;
    }

    @Override
    public void playDecodedAudio(short[] audioData) {
        // Only queue up to 40 ms of pending audio data in addition to what AudioTrack is buffering for us.
        if (MoonBridge.getPendingAudioDuration() < 40) {
            if (ac3Encoder != null) {
                // Accumulates PCM and returns complete AC-3 frames (every 32 ms)
                int len = ac3Encoder.encode(audioData);
                if (len > 0) {
                    // This blocks until the frames fit in the AudioTrack buffer
                    int ret = track.write(ac3Encoder.getOutput(), 0, len);
                    if (ret < 0 && !loggedWriteError) {
                        LimeLog.warning("AC-3 AudioTrack write failed: " + ret);
                        loggedWriteError = true;
                    }
                }
                else if (len < 0 && !loggedWriteError) {
                    LimeLog.warning("AC-3 encode failed: " + len);
                    loggedWriteError = true;
                }
                return;
            }

            // This will block until the write is completed. That can cause a backlog
            // of pending audio data, so we do the above check to be able to bound
            // latency at 40 ms in that situation.
            track.write(audioData, 0, audioData.length);
        }
        else {
            LimeLog.info("Too much pending audio data: " + MoonBridge.getPendingAudioDuration() +" ms");
        }
    }

    @Override
    public void start() {
        // Audio effects can't process a compressed bitstream
        if (enableAudioFx && ac3Encoder == null) {
            // Open an audio effect control session to allow equalizers to apply audio effects
            Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, track.getAudioSessionId());
            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            i.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_GAME);
            context.sendBroadcast(i);
        }
    }

    @Override
    public void stop() {
        if (enableAudioFx && ac3Encoder == null) {
            // Close our audio effect control session when we're stopping
            Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, track.getAudioSessionId());
            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            context.sendBroadcast(i);
        }
    }

    @Override
    public void cleanup() {
        // Immediately drop all pending data
        track.pause();
        track.flush();
        track.release();

        if (ac3Encoder != null) {
            ac3Encoder.release();
            ac3Encoder = null;
        }
    }
}
