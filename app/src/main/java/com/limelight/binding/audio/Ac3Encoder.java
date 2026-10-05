package com.limelight.binding.audio;

import com.limelight.LimeLog;

/**
 * Streaming AC-3 (Dolby Digital) encoder backed by a minimal libavcodec
 * (see app/src/main/jni/ac3enc).
 *
 * Input is interleaved 16-bit PCM in Moonlight's channel order
 * (FL FR FC LFE BL BR for 5.1). Input chunks of any size are accepted;
 * output is produced whenever a complete 1536-sample AC-3 frame is available.
 *
 * Not thread-safe: use from the audio thread only.
 */
public class Ac3Encoder {
    // Largest AC-3 frame libavcodec can produce (see AC3ENC_MAX_FRAME_BYTES)
    private static final int MAX_FRAME_BYTES = 3840;

    private static final boolean LIBRARY_LOADED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("moonlight-ac3");
            loaded = true;
        } catch (Throwable t) {
            // The library is only built when the FFmpeg prebuilts are present
            LimeLog.warning("AC-3 encoder library unavailable: " + t.getMessage());
            loaded = false;
        }
        LIBRARY_LOADED = loaded;
    }

    private long handle;
    private final int channelCount;
    private final int frameSize;
    private byte[] outBuffer;

    /** @return true if libmoonlight-ac3.so is packaged in this build */
    public static boolean isAvailable() {
        return LIBRARY_LOADED;
    }

    /**
     * @param sampleRate sample rate in Hz (48000 for Moonlight)
     * @param channelCount 2, 4 or 6
     * @param bitrate bits per second, e.g. 640000
     * @throws IllegalStateException if the encoder can't be created
     */
    public Ac3Encoder(int sampleRate, int channelCount, int bitrate) {
        if (!LIBRARY_LOADED) {
            throw new IllegalStateException("AC-3 encoder library not available");
        }

        handle = nativeCreate(sampleRate, channelCount, bitrate);
        if (handle == 0) {
            throw new IllegalStateException("Failed to create AC-3 encoder ("
                    + sampleRate + " Hz, " + channelCount + " ch, " + bitrate + " bps)");
        }

        this.channelCount = channelCount;
        this.frameSize = nativeFrameSize(handle);
        this.outBuffer = new byte[MAX_FRAME_BYTES];
    }

    /** @return samples per channel in one AC-3 frame (1536) */
    public int getFrameSize() {
        return frameSize;
    }

    /**
     * Feeds interleaved PCM. The returned byte count refers to {@link #getOutput()}
     * and holds zero or more complete AC-3 frames.
     *
     * @param pcm interleaved samples; its length must be a multiple of the channel count
     * @return number of AC-3 bytes available in getOutput(), or a negative value on error
     */
    public int encode(short[] pcm) {
        int samplesPerChannel = pcm.length / channelCount;

        int maxOut = MAX_FRAME_BYTES * (1 + samplesPerChannel / frameSize);
        if (outBuffer.length < maxOut) {
            outBuffer = new byte[maxOut];
        }

        return nativeEncode(handle, pcm, samplesPerChannel, outBuffer);
    }

    /** Buffer holding the output of the last {@link #encode(short[])} call. */
    public byte[] getOutput() {
        return outBuffer;
    }

    public void release() {
        if (handle != 0) {
            nativeDestroy(handle);
            handle = 0;
        }
    }

    private static native long nativeCreate(int sampleRate, int channelCount, int bitrate);
    private static native int nativeFrameSize(long handle);
    private static native int nativeEncode(long handle, short[] pcm, int samplesPerChannel, byte[] out);
    private static native void nativeDestroy(long handle);
}
