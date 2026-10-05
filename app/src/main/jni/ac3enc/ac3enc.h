#pragma once

// Minimal streaming AC-3 (Dolby Digital) encoder built on libavcodec.
//
// Moonlight hands us decoded PCM in small chunks (typically 5 or 10 ms).
// AC-3 frames are always 1536 samples (32 ms at 48 kHz), so this wrapper
// accumulates PCM until a full frame is available, encodes it, and returns
// the resulting AC-3 frame(s) ready to be written to a passthrough AudioTrack.

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct Ac3Enc Ac3Enc;

// Largest AC-3 frame we can produce (640 kbps @ 32 kHz is 3840 bytes; at 48 kHz it is 2560).
#define AC3ENC_MAX_FRAME_BYTES 3840

// Creates an encoder for interleaved S16 PCM.
//
// channels: 2 (FL FR), 4 (FL FR BL BR) or 6 (FL FR FC LFE BL BR).
// That is the channel order Moonlight's Opus decoder produces, which is
// also Android's CHANNEL_OUT_* order.
//
// bitrate: bits per second (e.g. 640000). 0 selects 640 kbps.
//
// Returns NULL on failure; if err is non-NULL a message is written to it.
Ac3Enc* ac3enc_create(int sample_rate, int channels, int bitrate, char* err, size_t err_len);

// Samples per channel in one AC-3 frame (always 1536).
int ac3enc_frame_size(const Ac3Enc* enc);

// Feeds sample_count samples per channel of interleaved S16 PCM.
//
// Every AC-3 frame completed by this call is appended to out.
// Returns the number of bytes written to out (0 if no frame was completed),
// or a negative value on error (including out being too small; an out_cap of
// AC3ENC_MAX_FRAME_BYTES * (1 + sample_count / 1536) is always sufficient).
int ac3enc_encode(Ac3Enc* enc, const int16_t* pcm, int sample_count, uint8_t* out, int out_cap);

// Drops any partially accumulated frame (e.g. after a discontinuity).
void ac3enc_reset(Ac3Enc* enc);

void ac3enc_destroy(Ac3Enc* enc);

#ifdef __cplusplus
}
#endif
