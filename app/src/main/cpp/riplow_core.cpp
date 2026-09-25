#include <jni.h>
#include "core/module.hpp"
extern "C" JNIEXPORT jstring JNICALL Java_com_riplow_client_MainActivity_nativeVersion(JNIEnv* env, jobject) { return env->NewStringUTF(riplow::core_version()); }
