# Android.mk for the optional AC-3 (Dolby Digital) passthrough encoder
#
# This module links against a minimal static libavcodec/libavutil that only
# contains FFmpeg's AC-3 encoder. Those libraries are not checked in; build
# them with ./build-ffmpeg-ac3.sh (see that script for details). If they are
# missing for an ABI, the module is skipped and the app falls back to PCM.

LOCAL_PATH := $(call my-dir)

AC3_FFMPEG_DIR := $(LOCAL_PATH)/ffmpeg/$(TARGET_ARCH_ABI)

ifneq (,$(wildcard $(AC3_FFMPEG_DIR)/lib/libavcodec.a))

include $(CLEAR_VARS)
LOCAL_MODULE := ac3-avcodec
LOCAL_SRC_FILES := ffmpeg/$(TARGET_ARCH_ABI)/lib/libavcodec.a
LOCAL_EXPORT_C_INCLUDES := $(AC3_FFMPEG_DIR)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ac3-avutil
LOCAL_SRC_FILES := ffmpeg/$(TARGET_ARCH_ABI)/lib/libavutil.a
LOCAL_EXPORT_C_INCLUDES := $(AC3_FFMPEG_DIR)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := moonlight-ac3
LOCAL_SRC_FILES := ac3enc.c ac3enc_jni.c
LOCAL_STATIC_LIBRARIES := ac3-avcodec ac3-avutil
LOCAL_LDLIBS := -llog -lm
LOCAL_LDFLAGS += -Wl,--exclude-libs,ALL
LOCAL_BRANCH_PROTECTION := standard
include $(BUILD_SHARED_LIBRARY)

else

$(warning AC-3 encoder: no prebuilt FFmpeg for $(TARGET_ARCH_ABI) in $(AC3_FFMPEG_DIR), skipping libmoonlight-ac3. Run app/src/main/jni/ac3enc/build-ffmpeg-ac3.sh to enable Dolby Digital passthrough.)

endif
