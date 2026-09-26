#include "RenderPipeline.hpp"

#include "HookEngine.hpp"

#include <EGL/egl.h>
#include <android/log.h>
#include <array>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <functional>
#include <thread>

namespace riplow::render {
namespace {

constexpr const char* kTag = "RiplowRender";
constexpr std::size_t kQueueCapacity = 256;
constexpr std::size_t kMaxEventsPerFrame = 64;

struct QueueCell {
    std::atomic<std::size_t> sequence{0};
    TouchEvent event{};
};

class EventQueue {
public:
    EventQueue() {
        for (std::size_t i = 0; i < kQueueCapacity; ++i) {
            cells_[i].sequence.store(i, std::memory_order_relaxed);
        }
    }

    bool push(const TouchEvent& event) {
        std::size_t position = enqueue_.load(std::memory_order_relaxed);

        for (;;) {
            QueueCell& cell = cells_[position % kQueueCapacity];
            const std::size_t sequence = cell.sequence.load(std::memory_order_acquire);
            const std::intptr_t difference =
                static_cast<std::intptr_t>(sequence) -
                static_cast<std::intptr_t>(position);

            if (difference == 0) {
                if (enqueue_.compare_exchange_weak(
                        position,
                        position + 1,
                        std::memory_order_relaxed,
                        std::memory_order_relaxed)) {
                    cell.event = event;
                    cell.sequence.store(position + 1, std::memory_order_release);
                    return true;
                }
            } else if (difference < 0) {
                return false;
            } else {
                position = enqueue_.load(std::memory_order_relaxed);
            }
        }
    }

    bool pop(TouchEvent& out) {
        std::size_t position = dequeue_.load(std::memory_order_relaxed);

        for (;;) {
            QueueCell& cell = cells_[position % kQueueCapacity];
            const std::size_t sequence = cell.sequence.load(std::memory_order_acquire);
            const std::intptr_t difference =
                static_cast<std::intptr_t>(sequence) -
                static_cast<std::intptr_t>(position + 1);

            if (difference == 0) {
                if (dequeue_.compare_exchange_weak(
                        position,
                        position + 1,
                        std::memory_order_relaxed,
                        std::memory_order_relaxed)) {
                    out = cell.event;
                    cell.sequence.store(
                        position + kQueueCapacity,
                        std::memory_order_release
                    );
                    return true;
                }
            } else if (difference < 0) {
                return false;
            } else {
                position = dequeue_.load(std::memory_order_relaxed);
            }
        }
    }

private:
    std::array<QueueCell, kQueueCapacity> cells_{};
    std::atomic<std::size_t> enqueue_{0};
    std::atomic<std::size_t> dequeue_{0};
};

std::uint64_t thread_token() {
    return static_cast<std::uint64_t>(
        std::hash<std::thread::id>{}(std::this_thread::get_id())
    );
}

class PipelineState {
public:
    std::atomic<bool> running{false};
    std::atomic<bool> hud_enabled{false};
    std::atomic<std::uint64_t> render_thread{0};
    std::atomic<std::size_t> dropped{0};
    EventQueue events;
    std::atomic<std::uint64_t> frames{0};
    std::atomic<std::uint64_t> last_frame_ns{0};

    bool bind_render_thread() {
        const auto token = thread_token();
        std::uint64_t expected = 0;
        return render_thread.compare_exchange_strong(
            expected,
            token,
            std::memory_order_acq_rel,
            std::memory_order_acquire
        ) || render_thread.load(std::memory_order_acquire) == token;
    }
};

PipelineState g_state;

void render_callback(EGLDisplay, EGLSurface) {
    RenderPipeline::instance().on_swap();
}

} // namespace

RenderPipeline& RenderPipeline::instance() {
    static RenderPipeline pipeline;
    return pipeline;
}

bool RenderPipeline::start() {
    if (g_state.running.exchange(true, std::memory_order_acq_rel)) return true;

    g_state.render_thread.store(0, std::memory_order_release);
    g_state.dropped.store(0, std::memory_order_release);
    g_state.frames.store(0, std::memory_order_release);
    g_state.last_frame_ns.store(0, std::memory_order_release);

    set_swap_callback(render_callback);
    if (!install_egl_swap_hook(render_callback)) {
        g_state.running.store(false, std::memory_order_release);
        set_swap_callback(nullptr);
        return false;
    }
    return true;
}

void RenderPipeline::stop() {
    g_state.running.store(false, std::memory_order_release);
    set_swap_callback(nullptr);

    // The hook intentionally remains installed for process lifetime when
    // process hooks are enabled. Destroying an inline hook while a render
    // thread may still be inside the trampoline can race with instruction
    // patching and crash the game.
}

void RenderPipeline::on_swap() {
    if (!g_state.running.load(std::memory_order_acquire)) return;
    if (!g_state.bind_render_thread()) return;

    const EGLContext context = eglGetCurrentContext();
    if (context == EGL_NO_CONTEXT) return;

    const auto now = std::chrono::steady_clock::now();
    const auto now_ns =
        static_cast<std::uint64_t>(
            std::chrono::duration_cast<std::chrono::nanoseconds>(
                now.time_since_epoch()
            ).count()
        );

    g_state.frames.fetch_add(1, std::memory_order_relaxed);
    g_state.last_frame_ns.store(now_ns, std::memory_order_release);

    if (!g_state.hud_enabled.load(std::memory_order_acquire)) return;

    TouchEvent event{};
    std::size_t processed = 0;
    while (processed < kMaxEventsPerFrame && g_state.events.pop(event)) {
        ++processed;
    }

    // Intentionally empty in the default companion build.
    // A verified ImGui/GLES renderer can be compiled behind a dedicated
    // integration flag without affecting the normal launcher APK.
}

bool RenderPipeline::push_touch(const TouchEvent& event) {
    if (!g_state.running.load(std::memory_order_acquire)) return false;
    if (!g_state.events.push(event)) {
        g_state.dropped.fetch_add(1, std::memory_order_relaxed);
        return false;
    }
    return true;
}

void RenderPipeline::set_hud_enabled(bool enabled) {
    g_state.hud_enabled.store(enabled, std::memory_order_release);
}

bool RenderPipeline::hud_enabled() const {
    return g_state.hud_enabled.load(std::memory_order_acquire);
}

std::size_t RenderPipeline::dropped_events() const {
    return g_state.dropped.load(std::memory_order_acquire);
}

bool RenderPipeline::render_thread_bound() const {
    return g_state.render_thread.load(std::memory_order_acquire) != 0;
}

const char* RenderPipeline::status() const {
    if (!g_state.running.load(std::memory_order_acquire)) return "stopped";
    if (!egl_swap_hook_installed()) return "running / hook unavailable";
    if (!render_thread_bound()) return "hooked / waiting for render thread";
    return "running";
}

} // namespace riplow::render
