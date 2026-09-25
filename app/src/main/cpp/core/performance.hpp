#pragma once
#include <array>
#include <cstddef>
#include <mutex>
#include <string>
namespace riplow {
enum class PerformanceProfile { Balanced, Performance, Extreme };
struct FrameMetrics {
    double average_frame_ms{0.0};
    double average_fps{0.0};
    double one_percent_low_fps{0.0};
    std::size_t samples{0};
};
class PerformanceEngine {
public:
    void record_frame(double frame_ms);
    FrameMetrics metrics() const;
    void set_profile(PerformanceProfile profile);
    std::string profile_name() const;
private:
    static constexpr std::size_t kCapacity = 240;
    std::array<double,kCapacity> samples_{};
    std::size_t count_{0};
    std::size_t cursor_{0};
    PerformanceProfile profile_{PerformanceProfile::Balanced};
    mutable std::mutex mutex_;
};
PerformanceEngine& performance();
}