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
        {"fps", "FPS Telemetry", Category::Performance, false},
        {"frame_pacing", "Frame Pacing", Category::Performance, false},
        {"performance_profile", "Performance Profile", Category::Performance, false},
        {"memory_monitor", "Memory Monitor", Category::Performance, false},
        {"battery_guard", "Battery Monitor", Category::Performance, false},
        {"session_timer", "Session Timer", Category::HUD, false},
        {"clock", "Clock", Category::HUD, false},
        {"coordinates", "Coordinates", Category::HUD, false},
        {"compass", "Compass", Category::HUD, false},
        {"fps_overlay", "FPS Overlay", Category::HUD, false},
        {"direction_hud", "Direction HUD", Category::HUD, false},
        {"potion_hud", "Potion HUD", Category::HUD, false},
        {"armor_hud", "Armor HUD", Category::HUD, false},
        {"scoreboard", "Scoreboard", Category::HUD, false},
        {"zoom", "Zoom", Category::Visual, false},
        {"crosshair", "Crosshair", Category::Visual, false},
        {"fullbright", "Fullbright", Category::Visual, false},
        {"no_fog", "No Fog", Category::Visual, false},
        {"view_bobbing", "View Bobbing", Category::Visual, false},
        {"fov_changer", "FOV Presets", Category::Visual, false},
        {"gui_scale", "GUI Scale", Category::Visual, false},
        {"low_fire", "Low Fire", Category::Visual, false},
        {"ping", "Ping", Category::Network, false},
        {"network_diagnostics", "Network Diagnostics", Category::Network, false},
        {"jitter_monitor", "Jitter Monitor", Category::Network, false},
        {"packet_loss", "Packet Loss", Category::Network, false},
        {"connection_status", "Connection Status", Category::Network, false},
        {"pack_switcher", "Pack Switcher", Category::Client, false},
        {"pack_profiles", "Pack Profiles", Category::Client, false},
        {"addon_manager", "Add-on Manager", Category::Client, false},
        {"world_manager", "World Manager", Category::Client, false},
        {"backup_manager", "World Backups", Category::Client, false},
        {"version_profiles", "Version Profiles", Category::Client, false},
        {"quick_launch", "Quick Launch", Category::Client, false},
        {"module_config", "Module Config", Category::Client, false}
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
    return "1.3.0-native";
}

ModuleManager& modules() {
    return g_modules;
}

}
