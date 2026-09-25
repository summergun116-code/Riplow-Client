#pragma once
#include <mutex>
#include <string>
namespace riplow {
struct NetworkMetrics { double ping_ms{0.0}; double jitter_ms{0.0}; double packet_loss_percent{0.0}; bool has_samples{false}; };
class NetworkMonitor {
public:
    void sample_ping(double ping_ms);
    void record_packet_loss(double percent);
    NetworkMetrics metrics() const;
    std::string summary() const;
private:
    double last_ping_ms_{0.0};
    double jitter_ms_{0.0};
    double packet_loss_percent_{0.0};
    bool has_samples_{false};
    mutable std::mutex mutex_;
};
NetworkMonitor& network();
}