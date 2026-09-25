#include "performance.hpp"
#include <algorithm>
#include <numeric>
#include <vector>
namespace riplow {
namespace { PerformanceEngine g_performance; }
void PerformanceEngine::record_frame(double frame_ms) {
    if (frame_ms <= 0.0) return;
    std::lock_guard<std::mutex> lock(mutex_);
    samples_[cursor_] = frame_ms;
    cursor_ = (cursor_ + 1) % kCapacity;
    if (count_ < kCapacity) ++count_;
}
FrameMetrics PerformanceEngine::metrics() const {
    std::lock_guard<std::mutex> lock(mutex_);
    if (count_ == 0) return {};
    std::vector<double> data(samples_.begin(),samples_.begin()+count_);
    const double sum=std::accumulate(data.begin(),data.end(),0.0);
    std::sort(data.begin(),data.end());
    const std::size_t worst_count=std::max<std::size_t>(1,count_/100);
    const double worst_sum=std::accumulate(data.end()-worst_count,data.end(),0.0);
    const double average_ms=sum/static_cast<double>(count_);
    const double worst_frame_ms=worst_sum/static_cast<double>(worst_count);
    return {average_ms,average_ms>0.0?1000.0/average_ms:0.0,worst_frame_ms>0.0?1000.0/worst_frame_ms:0.0,count_};
}
void PerformanceEngine::set_profile(PerformanceProfile profile) {
    std::lock_guard<std::mutex> lock(mutex_);
    profile_=profile;
}
std::string PerformanceEngine::profile_name() const {
    std::lock_guard<std::mutex> lock(mutex_);
    switch(profile_) {
        case PerformanceProfile::Balanced: return "Balanced";
        case PerformanceProfile::Performance: return "Performance";
        case PerformanceProfile::Extreme: return "Extreme";
    }
    return "Balanced";
}
PerformanceEngine& performance(){return g_performance;}
}