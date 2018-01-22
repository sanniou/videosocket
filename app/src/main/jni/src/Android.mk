LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := display
SDL_PATH := ../SDL
FFMPEG_PATH := ../ffmpeg

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SDL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(FFMPEG_PATH)/include

# Add your application source files here...
#LOCAL_SRC_FILES := $(SDL_PATH)/src/main/android/SDL_android_main.c \

LOCAL_SRC_FILES := player.c

LOCAL_MODULE_RELATIVE_PATH := hw
LOCAL_SHARED_LIBRARIES := SDL2
LOCAL_SHARED_LIBRARIES += avcodec-56-prebuilt avdevice-56-prebuilt \
avfilter-5-prebuilt avformat-56-prebuilt avutil-54-prebuilt \
avswresample-1-prebuilt avswscale-3-prebuilt postproc-53-prebuilt 

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -llog

include $(BUILD_SHARED_LIBRARY)
