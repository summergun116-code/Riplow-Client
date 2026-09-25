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
        {"fps", "FPS Overlay", Category::Performance, false},
        {"frame_pacing", "Frame Pacing", Category::Performance, false},
        {"performance_profile", "Performance Profile", Category::Performance, false},
        {"battery_guard", "Battery Guard", Category::Performance, false},
        {"memory_monitor", "Memory Monitor", Category::Performance, false},
        {"cps", "CPS Counter", Category::PvP, false},
        {"keystrokes", "Keystrokes", Category::PvP, false},
        {"session_timer", "Session Timer", Category::Utility, false},
        {"clock", "Clock", Category::Utility, false},
        {"coordinates", "Coordinates", Category::Utility, false},
        {"compass", "Compass", Category::Utility, false},
        {"item_info", "Item Info", Category::Utility, false},
        {"zoom", "Zoom", Category::Visual, false},
        {"crosshair", "Crosshair", Category::Visual, false},
        {"hud", "HUD", Category::HUD, false},
        {"ping", "Ping", Category::Network, false},
        {"network_diagnostics", "Network Diagnostics", Category::Network, false},
        {"jitter_monitor", "Jitter Monitor", Category::Network, false},
        {"connection_status", "Connection Status", Category::Network, false},
        {"pack_switcher", "Pack Switcher", Category::Client, false},
        {"profile_switcher", "Profile Switcher", Category::Client, false},
        {"addon_manager", "Add-on Manager", Category::Client, false},
        {"launch_monitor", "Launch Monitor", Category::Client, false},
        {"client_menu", "Client Menu", Category::Client, false}
    };
}

bool ModuleManager::toggle(const std::string& id) {
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto& module : modules_) {
        if (module.id == id) {
            module.enabled = !module.enabled;
            return module.enabled;
        }
    }
    return false;
}

bool ModuleManager::set_enabled(const std::string& id, bool enabled) {
    std::lock_guard<std::mutex> lock(mutex_);
    for (auto& module : modules_) {
        if (module.id == id) {
            module.enabled = enabled;
            return true;
        }
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
    for (const auto& module : modules_) {
        if (module.enabled) ++count;
    }
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

const char* core_version() {
    return "0.3.0-native";
}

ModuleManager& modules() {
    return g_modules;
}
