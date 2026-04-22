Additional Info For Android 10 Support:
From the extracted GStreamer files (version 1.26.0), we need to modify one value in a specific file to support Android 10.
 
Path from Gstreamer : armv7\share\gst-android\ndk-build
 
FileName - gstreamer-1.0
 
line no: 92
 
NEEDS_NOTEXT_FIX    := no (Default value is Yes and, need to change no)
