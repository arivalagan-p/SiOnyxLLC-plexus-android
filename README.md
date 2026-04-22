**GStreamer Setup for Android**

We are using the GStreamer framework for media streaming.

**Download**

Download GStreamer for Android from the official website:
https://gstreamer.freedesktop.org/data/pkg/android/

**Version used:** 1.26.2

**Setup**

Extract the downloaded package (this will extract the ARM libraries).
Add the GStreamer path in your gradle.properties file:
gstAndroidRoot=E:\\SIONYX_2025\\gstreamer_universal

**Note:** This path(gstAndroidRoot) is currently set to a local machine. Please update it to your local GStreamer installation path.

**Additional Configuration for Android 10 Support**

To ensure compatibility with Android 10, update the following configuration:

Path: armv7/share/gst-android/ndk-build
File: gstreamer-1.0
Line: 92

Update the following value:

NEEDS_NOTEXT_FIX := no

**Note:** The default value is yes. It must be changed to no for Android 10 support.
