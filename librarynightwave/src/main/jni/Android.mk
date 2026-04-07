LOCAL_PATH := $(call my-dir)

# Define GStreamer root path (edit as needed)
GSTREAMER_ROOT_ANDROID := D:/GStreamerSDK/gstreamer-1.0-android-universal-1.26.2

ifndef GSTREAMER_ROOT_ANDROID
$(error GSTREAMER_ROOT_ANDROID is not defined!)
endif

# Set GStreamer root path for the current ABI
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    GSTREAMER_ROOT := $(GSTREAMER_ROOT_ANDROID)/armv7
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    GSTREAMER_ROOT := $(GSTREAMER_ROOT_ANDROID)/arm64
else ifeq ($(TARGET_ARCH_ABI),x86)
    GSTREAMER_ROOT := $(GSTREAMER_ROOT_ANDROID)/x86
else ifeq ($(TARGET_ARCH_ABI),x86_64)
    GSTREAMER_ROOT := $(GSTREAMER_ROOT_ANDROID)/x86_64
else
    $(error Unsupported TARGET_ARCH_ABI: $(TARGET_ARCH_ABI))
endif

# GStreamer NDK Build Support
GSTREAMER_NDK_BUILD_PATH := $(GSTREAMER_ROOT)/share/gst-android/ndk-build
include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk

# Required GStreamer plugins
GSTREAMER_EXTRA_DEPS := gstreamer-video-1.0 gstreamer-app-1.0

GSTREAMER_PLUGINS := \
    $(GSTREAMER_PLUGINS_CORE) \
    $(GSTREAMER_PLUGINS_PLAYBACK) \
    $(GSTREAMER_PLUGINS_CODECS) \
    $(GSTREAMER_PLUGINS_NET) \
    $(GSTREAMER_PLUGINS_SYS)

G_IO_MODULES := openssl

# Build plexus lib .so
include $(CLEAR_VARS)
LOCAL_MODULE := plexus
LOCAL_SRC_FILES := native-lib.cpp
LOCAL_CPPFLAGS  := -std=c++17 -fexceptions -fPIC
LOCAL_CFLAGS += -fPIC
LOCAL_LDLIBS += -llog -landroid
include $(BUILD_SHARED_LIBRARY)

# Build rtsp_stream lib .so
include $(CLEAR_VARS)
LOCAL_MODULE := rtsp_stream
LOCAL_SRC_FILES := rtsp_stream_cpp.cpp
LOCAL_C_INCLUDES += \
    $(GSTREAMER_ROOT)/include/gstreamer-1.0 \
    $(GSTREAMER_ROOT)/include/gstreamer-1.0/android \
    $(GSTREAMER_ROOT)/include \
    $(GSTREAMER_ROOT)/lib/gstreamer-1.0/include \
    $(GSTREAMER_NDK_BUILD_PATH) \
    $(LOCAL_PATH) \
    $(GSTREAMER_NDK_BUILD_PATH)/gst
LOCAL_CPPFLAGS += -std=c++17 -fexceptions -frtti
LOCAL_CFLAGS += -std=c11
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -llog -landroid
include $(BUILD_SHARED_LIBRARY)

# Final GStreamer setup
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk
