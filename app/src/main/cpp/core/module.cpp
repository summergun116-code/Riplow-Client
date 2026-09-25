#include "module.hpp"
#include <sstream>
namespace riplow {
namespace {
const char* category_name(Category category) {
    switch (category) {
        case Category::Performance: return "Performance";
        case Category::PvP: return "PvP";
        case Category::Utility: return "Utility";
        case Category::Visual: return "Visual";
        case Category::HUD: return "HUD";
        case Category::Network: return "Network";
        case Category::Client: return "Client";
    }
    return "Client";
}
ModuleManager g_modules;
}
ModuleManager::ModuleManager() {
    modules_ = {
        {"fps","FPS Overlay",Category::Performance,true},
        {"frame_pacing","Frame Pacing",Category::Performance,true},
        {"performance_profile","Performance Profile",Category::Performance,false},
        {"cps","CPS Counter",Category::PvP,false},
        {"coordinates","Coordinates",Category::Utility,true},
        {"zoom","Zoom",Category::Visual,false},
        {"crosshair","Crosshair",Category::Visual,false},
        {"hud","HUD",Category::HUD,true},
        {"ping","Ping",Category::Network,true},
        {"network_diagnostics","Network Diagnostics",Category::Network,true},
        {"client_menu","Client Menu",Category::Client,true}
    };
}
bool ModuleManager::toggle(const std::string& id) {
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto& module : modules_) {
        if (module.id == id) { module.enabled = !module.enabled; return module.enabled; }
    }
    return false;
}
std::size_t ModuleManager::size() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return modules_.size();
}
std::size_t ModuleManager::enabled_count() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::size_t count = 0;
    for (const auto& module : modules_) if (module.enabled) ++count;
    return count;
}
std::string ModuleManager::summary() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::ostringstream out;
    for (const auto& module : modules_) {
        out << module.id << "|" << module.name << "|" << category_name(module.category)
            << "|" << (module.enabled ? "ON" : "OFF") << "\n";
    }
    return out.str();
}
const char* core_version() { return "0.2.0-native"; }
ModuleManager& modules() { return g_modules; }
}