#include "performance.hpp"
#include <algorithm>
#include <array>
#include <numeric>

namespace riplow {
namespace { PerformanceEngine g_performance; }

void PerformanceEngine::record_frame(double frame_ms) {
    if (frame_ms <= 0.0 || frame_ms > 10000.0) return;

    std::lock_guard<std::mutex> lock(mutex_);
    samples_[cursor_] = frame_ms;
    cursor_ = (cursor_ + 1) % kCapacity;
    if (count_ < kCapacity) ++count_;
}

FrameMetrics PerformanceEngine::metrics() const {
    std::lock_guard<std::mutex> lock(mutex_);
    if (count_ == 0) return {};

    // Keep diagnostics allocation-free: metrics can be requested frequently by
    // the Android overlay, so avoid a heap-backed vector on every read.
    std::array<double, kCapacity> data{};
    std::copy_n(samples_.begin(), count_, data.begin());

    const double sum = std::accumulate(data.begin(), data.begin() + count_, 0.0);
    std::sort(data.begin(), data.begin() + count_);

    // At least one frame belongs to the 1% tail, even while the buffer is small.
    const std::size_t low_count =
        std::max<std::size_t>(1, (count_ + 99) / 100);
    const double low_sum =
        std::accumulate(data.end() - low_count, data.end(), 0.0);

    const double average_ms = sum / static_cast<double>(count_);
    const double low_frame_ms = low_sum / static_cast<double>(low_count);

    return {
        average_ms,
        average_ms > 0.0 ? 1000.0 / average_ms : 0.0,
        low_frame_ms > 0.0 ? 1000.0 / low_frame_ms : 0.0,
        count_
    };
}

void PerformanceEngine::set_profile(PerformanceProfile profile) {
    std::lock_guard<std::mutex> lock(mutex_);
    profile_ = profile;
}

std::string PerformanceEngine::profile_name() const {
    std::lock_guard<std::mutex> lock(mutex_);
    switch (profile_) {
        case PerformanceProfile::Balanced: return "Balanced";
        case PerformanceProfile::Performance: return "Performance";
        case PerformanceProfile::Extreme: return "Extreme";
    }
    return "Balanced";
}

PerformanceEngine& performance() { return g_performance; }

}
