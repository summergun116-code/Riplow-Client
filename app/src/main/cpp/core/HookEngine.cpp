#include "HookEngine.hpp"

#include <EGL/eglext.h>
#include <android/log.h>
#include <atomic>
#include <dlfcn.h>
#include <mutex>

#if RIPLOW_ENABLE_PROCESS_HOOKS
#include <dobby.h>
#endif

namespace riplow::render {
namespace {

constexpr const char* kTag = "RiplowRender";

using EglSwapBuffers = EGLBoolean (*)(EGLDisplay, EGLSurface);

std::atomic<SwapCallback> g_callback{nullptr};
std::atomic<EglSwapBuffers> g_original{nullptr};
std::atomic<bool> g_installed{false};
std::mutex g_install_mutex;

EglSwapBuffers resolve_egl_swap_buffers() {
    void* symbol = dlsym(RTLD_DEFAULT, "eglSwapBuffers");
    if (symbol) return reinterpret_cast<EglSwapBuffers>(symbol);

    using GetProcAddress = __eglMustCastToProperFunctionPointerType (*)(const char*);
    void* library = dlopen("libEGL.so", RTLD_NOW | RTLD_LOCAL);
    if (!library) return nullptr;

    auto get_proc = reinterpret_cast<GetProcAddress>(dlsym(library, "eglGetProcAddress"));
    EglSwapBuffers result = nullptr;
    if (get_proc) {
        result = reinterpret_cast<EglSwapBuffers>(get_proc("eglSwapBuffers"));
    }
    dlclose(library);
    return result;
}

#if RIPLOW_ENABLE_PROCESS_HOOKS
EGLBoolean egl_swap_detour(EGLDisplay display, EGLSurface surface) {
    const auto original = g_original.load(std::memory_order_acquire);
    if (!original) return EGL_FALSE;

    if (display == EGL_NO_DISPLAY || surface == EGL_NO_SURFACE) {
        return original(display, surface);
    }

    const EGLContext context = eglGetCurrentContext();
    if (context != EGL_NO_CONTEXT) {
        if (auto callback = g_callback.load(std::memory_order_acquire)) {
            callback(display, surface);
        }
    }

    return original(display, surface);
}
#endif

} // namespace

void set_swap_callback(SwapCallback callback) {
    g_callback.store(callback, std::memory_order_release);
}

bool install_egl_swap_hook(SwapCallback callback) {
    std::lock_guard<std::mutex> lock(g_install_mutex);

    g_callback.store(callback, std::memory_order_release);
    if (g_installed.load(std::memory_order_acquire)) return true;

#if !RIPLOW_ENABLE_PROCESS_HOOKS
    __android_log_print(
        ANDROID_LOG_INFO,
        kTag,
        "EGL hook adapter compiled in safe companion mode; process hooks are disabled"
    );
    return false;
#else
    auto target = resolve_egl_swap_buffers();
    if (!target) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "eglSwapBuffers was not resolvable");
        g_callback.store(nullptr, std::memory_order_release);
        return false;
    }

    void* original = nullptr;
    const int result = DobbyHook(
        reinterpret_cast<void*>(target),
        reinterpret_cast<void*>(egl_swap_detour),
        &original
    );
    if (result != 0 || !original) {
        __android_log_print(
            ANDROID_LOG_ERROR,
            kTag,
            "DobbyHook(eglSwapBuffers) failed: %d",
            result
        );
        g_callback.store(nullptr, std::memory_order_release);
        return false;
    }

    g_original.store(reinterpret_cast<EglSwapBuffers>(original), std::memory_order_release);
    g_installed.store(true, std::memory_order_release);

    __android_log_print(ANDROID_LOG_INFO, kTag, "eglSwapBuffers hook installed");
    return true;
#endif
}

bool egl_swap_hook_installed() {
    return g_installed.load(std::memory_order_acquire);
}

} // namespace riplow::render
