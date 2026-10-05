// JNI glue for com.limelight.binding.audio.Ac3Encoder

#include <jni.h>
#include <stdlib.h>

#include <android/log.h>

#include "ac3enc.h"

#define LOG_TAG "moonlight-ac3"

typedef struct {
    Ac3Enc* enc;
    int channels;
    int16_t* pcm;
    int pcm_cap;      // in samples per channel
    uint8_t* out;
    int out_cap;      // in bytes
} JniAc3Enc;

static void destroy(JniAc3Enc* j) {
    if (j == NULL) {
        return;
    }
    ac3enc_destroy(j->enc);
    free(j->pcm);
    free(j->out);
    free(j);
}

JNIEXPORT jlong JNICALL
Java_com_limelight_binding_audio_Ac3Encoder_nativeCreate(JNIEnv* env, jclass clazz,
                                                        jint sampleRate, jint channels, jint bitrate) {
    char err[256] = "";
    JniAc3Enc* j = calloc(1, sizeof(*j));
    if (j == NULL) {
        return 0;
    }

    j->enc = ac3enc_create(sampleRate, channels, bitrate, err, sizeof(err));
    if (j->enc == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to create AC-3 encoder: %s", err);
        free(j);
        return 0;
    }
    j->channels = channels;

    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "AC-3 encoder ready: %d Hz, %d ch, %d bps, %d samples/frame",
                        sampleRate, channels, bitrate, ac3enc_frame_size(j->enc));
    return (jlong)(intptr_t)j;
}

JNIEXPORT jint JNICALL
Java_com_limelight_binding_audio_Ac3Encoder_nativeFrameSize(JNIEnv* env, jclass clazz, jlong handle) {
    JniAc3Enc* j = (JniAc3Enc*)(intptr_t)handle;
    return ac3enc_frame_size(j->enc);
}

JNIEXPORT jint JNICALL
Java_com_limelight_binding_audio_Ac3Encoder_nativeEncode(JNIEnv* env, jclass clazz, jlong handle,
                                                        jshortArray pcm, jint sampleCount, jbyteArray out) {
    JniAc3Enc* j = (JniAc3Enc*)(intptr_t)handle;

    if (sampleCount <= 0) {
        return 0;
    }
    if ((*env)->GetArrayLength(env, pcm) < sampleCount * j->channels) {
        return -1;
    }

    // Grow scratch buffers as needed (only happens on the first call or two)
    if (sampleCount > j->pcm_cap) {
        int16_t* p = realloc(j->pcm, (size_t)sampleCount * j->channels * sizeof(int16_t));
        if (p == NULL) {
            return -1;
        }
        j->pcm = p;
        j->pcm_cap = sampleCount;
    }
    int needed_out = AC3ENC_MAX_FRAME_BYTES * (1 + sampleCount / ac3enc_frame_size(j->enc));
    if (needed_out > j->out_cap) {
        uint8_t* o = realloc(j->out, needed_out);
        if (o == NULL) {
            return -1;
        }
        j->out = o;
        j->out_cap = needed_out;
    }

    (*env)->GetShortArrayRegion(env, pcm, 0, sampleCount * j->channels, j->pcm);

    int len = ac3enc_encode(j->enc, j->pcm, sampleCount, j->out, j->out_cap);
    if (len < 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "AC-3 encode failed: %d", len);
        return -1;
    }
    if (len > 0) {
        if ((*env)->GetArrayLength(env, out) < len) {
            return -2;
        }
        (*env)->SetByteArrayRegion(env, out, 0, len, (const jbyte*)j->out);
    }
    return len;
}

JNIEXPORT void JNICALL
Java_com_limelight_binding_audio_Ac3Encoder_nativeDestroy(JNIEnv* env, jclass clazz, jlong handle) {
    destroy((JniAc3Enc*)(intptr_t)handle);
}
