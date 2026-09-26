#pragma once

#include <EGL/egl.h>

namespace riplow::render {

using SwapCallback = void (*)(EGLDisplay, EGLSurface);

bool install_egl_swap_hook(SwapCallback callback);
bool egl_swap_hook_installed();
void set_swap_callback(SwapCallback callback);

} // namespace riplow::render
