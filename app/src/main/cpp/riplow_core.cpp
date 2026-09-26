#include <jni.h>
#include <string>
#include "core/module.hpp"
#include "core/network.hpp"
#include "core/performance.hpp"

namespace {
jstring to_jstring(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

std::string read_id(JNIEnv* env, jstring id) {
    if (id == nullptr) return {};
    const char* raw = env->GetStringUTFChars(id, nullptr);
    if (raw == nullptr) return {};
    std::string value(raw);
    env->ReleaseStringUTFChars(id, raw);
    return value;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_nativeVersion(JNIEnv* env, jobject) {
    return env->NewStringUTF(riplow::core_version());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_nativeModuleSummaryNative(JNIEnv* env, jobject) {
    return to_jstring(env, riplow::modules().summary());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_riplow_client_NativeBridge_nativeToggleModuleNative(JNIEnv* env, jobject, jstring id) {
    const std::string module_id = read_id(env, id);
    if (module_id.empty()) return JNI_FALSE;
    return riplow::modules().toggle(module_id) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_riplow_client_NativeBridge_nativeSetModuleNative(JNIEnv* env, jobject, jstring id, jboolean enabled) {
    const std::string module_id = read_id(env, id);
    if (module_id.empty()) return JNI_FALSE;
    return riplow::modules().set_enabled(module_id, enabled == JNI_TRUE) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_nativeDiagnosticsNative(JNIEnv* env, jobject) {
    const auto frame = riplow::performance().metrics();
    std::string output =
        "Modules: " + std::to_string(riplow::modules().enabled_count()) +
        "/" + std::to_string(riplow::modules().size());
    output += "\nProfile: " + riplow::performance().profile_name();
    output += "\nFrame samples: " + std::to_string(frame.samples);
    output += "\nNative: healthy";
    output += "\nNetwork: " + riplow::network().summary();
    return to_jstring(env, output);
}
