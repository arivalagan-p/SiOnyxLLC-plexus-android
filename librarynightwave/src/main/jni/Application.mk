# Set the minimum platform version (change as needed)
APP_PLATFORM := android-29

# Target ABIs
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64

# Optimization: set to release (use 'debug' for debugging)
APP_OPTIM := release

# Use shared STL for C++ support
APP_STL := c++_shared

# Enable exceptions and RTTI for C++ code
APP_CPPFLAGS += -fexceptions -frtti

# Optional: Enable debug symbols
# APP_CPPFLAGS += -g
