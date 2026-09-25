#include <jni.h>
#include <string>
#include "core/module.hpp"
#include "core/network.hpp"
#include "core/performance.hpp"

namespace {
jstring to_jstring(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_version(JNIEnv* env, jobject) {
    return env->NewStringUTF(riplow::core_version());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_nativeModuleSummary(JNIEnv* env, jobject) {
    return to_jstring(env, riplow::modules().summary());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_riplow_client_NativeBridge_nativeToggleModule(JNIEnv* env, jobject, jstring id) {
    const char* raw = env->GetStringUTFChars(id, nullptr);
    const bool enabled = riplow::modules().toggle(raw ? raw : "");
    env->ReleaseStringUTFChars(id, raw);
    return enabled ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_riplow_client_NativeBridge_nativeSetModule(JNIEnv* env, jobject, jstring id, jboolean enabled) {
    const char* raw = env->GetStringUTFChars(id, nullptr);
    const bool found = riplow::modules().set_enabled(raw ? raw : "", enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(id, raw);
    return found ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_nativeDiagnostics(JNIEnv* env, jobject) {
    const auto frame = riplow::performance().metrics();
    std::string output =
        "Modules: " + std::to_string(riplow::modules().enabled_count()) +
        "/" + std::to_string(riplow::modules().size());
    output += "\nProfile: " + riplow::performance().profile_name();
    output += "\nFrame samples: " + std::to_string(frame.samples);
    output += "\nNetwork: " + riplow::network().summary();
    return to_jstring(env, output);
}
