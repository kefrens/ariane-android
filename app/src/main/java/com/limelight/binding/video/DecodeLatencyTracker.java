package com.limelight.binding.video;

/**
 * Remembers when each frame was queued to the decoder, keyed by presentation time, so the
 * renderer thread can measure enqueue-to-dequeue decode time.
 *
 * The input thread records and the renderer thread takes, so access is synchronized. Storage is
 * a fixed ring of primitive slots: no allocation per frame, and entries for frames that never
 * come out of the decoder (dropped frames, codec config buffers) are simply overwritten instead
 * of accumulating for the whole session.
 */
class DecodeLatencyTracker {
    // Power of two, comfortably more than the frames a decoder can hold in flight
    static final int CAPACITY = 64;

    private final long[] ptsUs = new long[CAPACITY];
    private final long[] enqueueNs = new long[CAPACITY];
    private final boolean[] used = new boolean[CAPACITY];
    private int next;

    synchronized void record(long presentationTimeUs, long nowNs) {
        int slot = next;
        next = (next + 1) & (CAPACITY - 1);
        ptsUs[slot] = presentationTimeUs;
        enqueueNs[slot] = nowNs;
        used[slot] = true;
    }

    /**
     * Removes the entry for this presentation time and returns its enqueue time,
     * or -1 if there is none.
     */
    synchronized long take(long presentationTimeUs) {
        // Search newest first: the frame we want was almost always queued recently
        for (int i = 1; i <= CAPACITY; i++) {
            int slot = (next - i) & (CAPACITY - 1);
            if (used[slot] && ptsUs[slot] == presentationTimeUs) {
                used[slot] = false;
                return enqueueNs[slot];
            }
        }
        return -1;
    }
}
