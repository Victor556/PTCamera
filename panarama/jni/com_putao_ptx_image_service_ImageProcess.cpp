#include <com_putao_ptx_image_service_ImageProcess.h>

#include <string.h>

#include <android/log.h>

#include "ImageProcess.h"


#define LOG_TAG "myTag"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

ImageProcess * pIP = NULL;

JNIEXPORT jboolean JNICALL Java_com_putao_ptx_image_service_ImageProcess_stitch
(JNIEnv * env, jclass obj, jobjectArray arrayStringData, jstring outPath){
    vector<Mat> paths;
    jint i;
    jint count = env->GetArrayLength(arrayStringData);
    for (i = 0; i < count; i++){
        jstring str = (jstring)env->GetObjectArrayElement(arrayStringData, i);
        const char *path = env->GetStringUTFChars(str, 0);
//      LOGD("before change testval = %s", path);
        Mat img=imread(path);
        paths.push_back(img);
        env->ReleaseStringUTFChars(str, path);
    }
    
    if(pIP == NULL){
        pIP = new ImageProcess;
    }
    jboolean res = false;
    const char * nativeOut = env->GetStringUTFChars(outPath, 0);
    res = pIP->stitch(paths, nativeOut);
    env->ReleaseStringUTFChars(outPath, nativeOut);
    delete pIP;
    pIP = NULL;
    return res;
}

JNIEXPORT jboolean JNICALL Java_com_putao_ptx_image_service_ImageProcess_cropImage
            (JNIEnv * env, jclass obj, jstring inPath, jstring outPath){

    if(pIP == NULL){
        pIP = new ImageProcess;
    }

    jboolean res = false;
    const char *nativeIn = env->GetStringUTFChars(inPath, 0);
    const char *nativeOut = env->GetStringUTFChars(outPath, 0);
    res = pIP->cropImage(nativeIn,nativeOut);
    env->ReleaseStringUTFChars(inPath, nativeIn);
    env->ReleaseStringUTFChars(outPath, nativeOut);
    delete pIP;
    pIP = NULL;
    return res;
}