package com.limelight.binding.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DecodeLatencyTrackerTest {
    @Test
    public void returnsEnqueueTimeOnceThenForgetsIt() {
        DecodeLatencyTracker tracker = new DecodeLatencyTracker();
        tracker.record(1000, 5_000_000L);
        tracker.record(2000, 6_000_000L);

        assertEquals(6_000_000L, tracker.take(2000));
        assertEquals(5_000_000L, tracker.take(1000));
        assertEquals(-1, tracker.take(1000));
    }

    @Test
    public void unknownTimestampReturnsMinusOne() {
        DecodeLatencyTracker tracker = new DecodeLatencyTracker();
        assertEquals(-1, tracker.take(0));
        tracker.record(42, 7L);
        assertEquals(-1, tracker.take(43));
    }

    @Test
    public void framesNeverTakenAreOverwrittenInsteadOfAccumulating() {
        DecodeLatencyTracker tracker = new DecodeLatencyTracker();
        // Simulate a long session where the renderer never takes some frames (drops)
        for (long pts = 0; pts < 10_000; pts++) {
            tracker.record(pts, pts * 10);
        }
        // Only the most recent CAPACITY frames are still known
        long oldestKept = 10_000 - DecodeLatencyTracker.CAPACITY;
        assertEquals(-1, tracker.take(oldestKept - 1));
        assertEquals(oldestKept * 10, tracker.take(oldestKept));
        assertEquals(9_999 * 10L, tracker.take(9_999));
    }

    @Test
    public void concurrentRecordAndTakeDoNotThrow() throws Exception {
        final DecodeLatencyTracker tracker = new DecodeLatencyTracker();
        final int frames = 200_000;
        Thread producer = new Thread(() -> {
            for (int i = 0; i < frames; i++) {
                tracker.record(i, System.nanoTime());
            }
        });
        final Throwable[] failure = new Throwable[1];
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < frames; i++) {
                    tracker.take(i);
                }
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        assertEquals(null, failure[0]);
    }
}
