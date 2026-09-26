#include <jni.h>
#include <cstdint>
#include <string>

#include "core/module.hpp"
#include "core/network.hpp"
#include "core/performance.hpp"
#include "core/RenderPipeline.hpp"

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
    output += "
Profile: " + riplow::performance().profile_name();
    output += "
Frame samples: " + std::to_string(frame.samples);
    output += "
Renderer adapter: " + std::string(riplow::render::RenderPipeline::instance().status());
    output += "
Network: " + riplow::network().summary();
    return to_jstring(env, output);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_riplow_client_NativeBridge_nativeRenderStatusNative(JNIEnv* env, jobject) {
    return env->NewStringUTF(riplow::render::RenderPipeline::instance().status());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_riplow_client_NativeBridge_nativeStartRenderPipelineNative(JNIEnv*, jobject) {
    return riplow::render::RenderPipeline::instance().start() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_riplow_client_NativeBridge_nativeStopRenderPipelineNative(JNIEnv*, jobject) {
    riplow::render::RenderPipeline::instance().stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_riplow_client_NativeBridge_nativeSetRenderHudEnabledNative(JNIEnv*, jobject, jboolean enabled) {
    riplow::render::RenderPipeline::instance().set_hud_enabled(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_riplow_client_NativeBridge_nativePushTouchNative(
    JNIEnv*, jobject, jint action, jfloat x, jfloat y, jfloat pressure
) {
    return riplow::render::RenderPipeline::instance().push_touch(
        riplow::render::TouchEvent{
            static_cast<std::int32_t>(action),
            x,
            y,
            pressure
        }
    ) ? JNI_TRUE : JNI_FALSE;
}
