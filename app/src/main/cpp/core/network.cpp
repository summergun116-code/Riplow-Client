#include "network.hpp"
#include <cmath>
#include <iomanip>
#include <sstream>
namespace riplow {
namespace { NetworkMonitor g_network; }
void NetworkMonitor::sample_ping(double ping_ms) {
    if (ping_ms < 0.0) return;
    std::lock_guard<std::mutex> lock(mutex_);
    if (has_samples_) jitter_ms_=std::abs(ping_ms-last_ping_ms_);
    last_ping_ms_=ping_ms;
    has_samples_=true;
}
void NetworkMonitor::record_packet_loss(double percent) {
    std::lock_guard<std::mutex> lock(mutex_);
    packet_loss_percent_=percent<0.0?0.0:(percent>100.0?100.0:percent);
}
NetworkMetrics NetworkMonitor::metrics() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return {last_ping_ms_,jitter_ms_,packet_loss_percent_,has_samples_};
}
std::string NetworkMonitor::summary() const {
    const auto snapshot=metrics();
    std::ostringstream out;
    out<<std::fixed<<std::setprecision(1);
    if(!snapshot.has_samples) return "Waiting for an active Bedrock session";
    out<<"Ping "<<snapshot.ping_ms<<" ms • Jitter "<<snapshot.jitter_ms<<" ms • Loss "<<snapshot.packet_loss_percent<<"%";
    return out.str();
}
NetworkMonitor& network(){return g_network;}
}