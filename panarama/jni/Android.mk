LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../../../../work/linux/opencv/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
LOCAL_SRC_FILES  := com_putao_ptx_image_service_ImageProcess.cpp ImageProcess.cpp
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true

LOCAL_LDLIBS += -llog
LOCAL_MODULE := image_proc

include $(BUILD_SHARED_LIBRARY)  
