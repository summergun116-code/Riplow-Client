#pragma once

#include <cstddef>
#include <cstdint>

namespace riplow::render {

struct TouchEvent {
    std::int32_t action{0};
    float x{0.0f};
    float y{0.0f};
    float pressure{0.0f};
};

class RenderPipeline {
public:
    static RenderPipeline& instance();

    bool start();
    void stop();
    void on_swap();
    bool push_touch(const TouchEvent& event);
    void set_hud_enabled(bool enabled);
    bool hud_enabled() const;
    std::size_t dropped_events() const;
    bool render_thread_bound() const;

    const char* status() const;

private:
    RenderPipeline() = default;
    ~RenderPipeline() = default;
    RenderPipeline(const RenderPipeline&) = delete;
    RenderPipeline& operator=(const RenderPipeline&) = delete;

    RenderPipeline* self() { return this; }
};

} // namespace riplow::render
