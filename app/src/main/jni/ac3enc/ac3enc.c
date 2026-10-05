#include "ac3enc.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <libavcodec/avcodec.h>
#include <libavutil/channel_layout.h>
#include <libavutil/error.h>
#include <libavutil/frame.h>

struct Ac3Enc {
    AVCodecContext* ctx;
    AVFrame* frame;
    AVPacket* pkt;
    int channels;
    int frame_size;   // samples per channel per AC-3 frame
    int fill;         // samples per channel currently buffered in frame
    int64_t next_pts;
};

static void set_err(char* err, size_t err_len, const char* what, int averr) {
    if (err == NULL || err_len == 0) {
        return;
    }
    if (averr != 0) {
        char buf[AV_ERROR_MAX_STRING_SIZE];
        av_strerror(averr, buf, sizeof(buf));
        snprintf(err, err_len, "%s: %s", what, buf);
    }
    else {
        snprintf(err, err_len, "%s", what);
    }
}

Ac3Enc* ac3enc_create(int sample_rate, int channels, int bitrate, char* err, size_t err_len) {
    AVChannelLayout layout;
    switch (channels) {
    case 2:
        layout = (AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO;
        break;
    case 4:
        // Moonlight's quad is FL FR BL BR -> AC-3 2/2 mode
        layout = (AVChannelLayout)AV_CHANNEL_LAYOUT_QUAD;
        break;
    case 6:
        // Moonlight's 5.1 is FL FR FC LFE BL BR. With a native-order layout the
        // planes are ordered by channel bit, which is exactly that order.
        layout = (AVChannelLayout)AV_CHANNEL_LAYOUT_5POINT1_BACK;
        break;
    default:
        set_err(err, err_len, "Unsupported channel count for AC-3", 0);
        return NULL;
    }

    const AVCodec* codec = avcodec_find_encoder(AV_CODEC_ID_AC3);
    if (codec == NULL) {
        set_err(err, err_len, "AC-3 encoder not compiled into libavcodec", 0);
        return NULL;
    }

    Ac3Enc* enc = calloc(1, sizeof(*enc));
    if (enc == NULL) {
        set_err(err, err_len, "Out of memory", 0);
        return NULL;
    }
    enc->channels = channels;

    enc->ctx = avcodec_alloc_context3(codec);
    if (enc->ctx == NULL) {
        set_err(err, err_len, "avcodec_alloc_context3 failed", 0);
        goto fail;
    }

    enc->ctx->sample_rate = sample_rate;
    enc->ctx->sample_fmt = AV_SAMPLE_FMT_FLTP;
    enc->ctx->bit_rate = bitrate > 0 ? bitrate : 640000;
    enc->ctx->time_base = (AVRational){ 1, sample_rate };
    int ret = av_channel_layout_copy(&enc->ctx->ch_layout, &layout);
    if (ret < 0) {
        set_err(err, err_len, "av_channel_layout_copy", ret);
        goto fail;
    }

    ret = avcodec_open2(enc->ctx, codec, NULL);
    if (ret < 0) {
        set_err(err, err_len, "avcodec_open2", ret);
        goto fail;
    }

    enc->frame_size = enc->ctx->frame_size;

    enc->frame = av_frame_alloc();
    enc->pkt = av_packet_alloc();
    if (enc->frame == NULL || enc->pkt == NULL) {
        set_err(err, err_len, "Out of memory", 0);
        goto fail;
    }

    enc->frame->format = enc->ctx->sample_fmt;
    enc->frame->nb_samples = enc->frame_size;
    enc->frame->sample_rate = sample_rate;
    ret = av_channel_layout_copy(&enc->frame->ch_layout, &enc->ctx->ch_layout);
    if (ret < 0) {
        set_err(err, err_len, "av_channel_layout_copy", ret);
        goto fail;
    }
    ret = av_frame_get_buffer(enc->frame, 0);
    if (ret < 0) {
        set_err(err, err_len, "av_frame_get_buffer", ret);
        goto fail;
    }

    return enc;

fail:
    ac3enc_destroy(enc);
    return NULL;
}

int ac3enc_frame_size(const Ac3Enc* enc) {
    return enc->frame_size;
}

// Sends the full frame to the encoder and appends all available packets to out.
static int flush_frame(Ac3Enc* enc, uint8_t* out, int out_cap, int* out_len) {
    enc->frame->pts = enc->next_pts;
    enc->next_pts += enc->frame_size;

    int ret = avcodec_send_frame(enc->ctx, enc->frame);
    if (ret < 0) {
        return ret;
    }

    for (;;) {
        ret = avcodec_receive_packet(enc->ctx, enc->pkt);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            break;
        }
        else if (ret < 0) {
            return ret;
        }

        if (*out_len + enc->pkt->size > out_cap) {
            av_packet_unref(enc->pkt);
            return AVERROR(ENOSPC);
        }
        memcpy(out + *out_len, enc->pkt->data, enc->pkt->size);
        *out_len += enc->pkt->size;
        av_packet_unref(enc->pkt);
    }

    return 0;
}

int ac3enc_encode(Ac3Enc* enc, const int16_t* pcm, int sample_count, uint8_t* out, int out_cap) {
    int out_len = 0;
    int consumed = 0;

    while (consumed < sample_count) {
        if (enc->fill == 0) {
            // The encoder may still hold a reference to the previous frame's buffers
            int ret = av_frame_make_writable(enc->frame);
            if (ret < 0) {
                return ret;
            }
        }

        int todo = enc->frame_size - enc->fill;
        if (todo > sample_count - consumed) {
            todo = sample_count - consumed;
        }

        // Deinterleave S16 -> planar float
        const int16_t* src = pcm + (size_t)consumed * enc->channels;
        for (int ch = 0; ch < enc->channels; ch++) {
            float* dst = (float*)enc->frame->extended_data[ch] + enc->fill;
            const int16_t* s = src + ch;
            for (int i = 0; i < todo; i++) {
                dst[i] = s[(size_t)i * enc->channels] * (1.0f / 32768.0f);
            }
        }

        enc->fill += todo;
        consumed += todo;

        if (enc->fill == enc->frame_size) {
            enc->fill = 0;
            int ret = flush_frame(enc, out, out_cap, &out_len);
            if (ret < 0) {
                return ret;
            }
        }
    }

    return out_len;
}

void ac3enc_reset(Ac3Enc* enc) {
    enc->fill = 0;
}

void ac3enc_destroy(Ac3Enc* enc) {
    if (enc == NULL) {
        return;
    }
    av_packet_free(&enc->pkt);
    av_frame_free(&enc->frame);
    avcodec_free_context(&enc->ctx);
    free(enc);
}
