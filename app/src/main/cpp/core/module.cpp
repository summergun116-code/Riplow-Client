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
        {"client_menu", "Client Menu", Category::Client, false},

        {"fps_boost_profile", "FPS Boost Profile", Category::Performance, false},
        {"fps_unlocker", "FPS Unlocker", Category::Performance, false},
        {"shader_loader", "Shader Loader", Category::Performance, false},
        {"render_budget", "Render Budget", Category::Performance, false},
        {"gpu_budget", "GPU Budget", Category::Performance, false},
        {"thermal_guard", "Thermal Guard", Category::Performance, false},
        {"fps_overlay", "FPS Overlay", Category::HUD, false},
        {"ping_counter", "Ping Counter", Category::HUD, false},
        {"speed_display", "Speed Display", Category::HUD, false},
        {"potion_hud", "Potion HUD", Category::HUD, false},
        {"armor_hud", "Armor HUD", Category::HUD, false},
        {"boss_bar", "Boss Bar", Category::HUD, false},
        {"scoreboard", "Scoreboard", Category::HUD, false},
        {"tablist", "Tablist", Category::HUD, false},
        {"debug_panel", "Debug Panel", Category::HUD, false},
        {"direction_hud", "Direction HUD", Category::HUD, false},
        {"break_indicator", "Break Indicator", Category::HUD, false},
        {"combo_display", "Combo Display", Category::HUD, false},
        {"timer", "Timer", Category::HUD, false},
        {"tnt_timer", "TNT Timer", Category::HUD, false},
        {"fullbright", "Fullbright", Category::Visual, false},
        {"no_fog", "No Fog", Category::Visual, false},
        {"fog_color", "Fog Color", Category::Visual, false},
        {"view_model", "View Model", Category::Visual, false},
        {"block_outline", "Block Outline", Category::Visual, false},
        {"light_overlay", "Light Overlay", Category::Visual, false},
        {"hit_particles", "Hit Particles", Category::Visual, false},
        {"better_nametags", "Better Nametags", Category::Visual, false},
        {"third_person_nametag", "Third Person Nametag", Category::Visual, false},
        {"chunk_borders", "Chunk Borders", Category::Visual, false},
        {"motion_blur", "Motion Blur", Category::Visual, false},
        {"view_bobbing", "View Bobbing", Category::Visual, false},
        {"fov_changer", "FOV Changer", Category::Visual, false},
        {"gui_scale", "GUI Scale", Category::Visual, false},
        {"low_fire", "Low Fire", Category::Visual, false},
        {"shulker_preview", "Shulker Preview", Category::Visual, false},
        {"connected_glass", "Connected Glass", Category::Visual, false},
        {"minimap", "Minimap", Category::Visual, false},
        {"waypoints", "Waypoints", Category::Visual, false},
        {"breadcrumbs", "Breadcrumbs", Category::Visual, false},
        {"loot_beams", "Loot Beams", Category::Visual, false},
        {"auto_reconnect", "Auto Reconnect", Category::Utility, false},
        {"anti_afk", "Anti-AFK", Category::Utility, false},
        {"no_disconnect", "No Disconnect", Category::Utility, false},
        {"chat_timestamps", "Chat Timestamps", Category::Utility, false},
        {"better_chat", "Better Chat", Category::Utility, false},
        {"drop_prevention", "Drop Prevention", Category::Utility, false},
        {"world_manager", "World Manager", Category::Utility, false},
        {"backup_manager", "Backup Manager", Category::Utility, false},
        {"content_scanner", "Content Scanner", Category::Utility, false},
        {"quick_launch", "Quick Launch", Category::Utility, false},
        {"version_profiles", "Version Profiles", Category::Utility, false},
        {"packet_loss", "Packet Loss", Category::Network, false},
        {"transport_inspector", "Transport Inspector", Category::Network, false},
        {"session_transport", "Session Transport", Category::Network, false},
        {"pack_profiles", "Pack Profiles", Category::Client, false},
        {"version_manager", "Version Manager", Category::Client, false},
        {"module_config", "Module Config", Category::Client, false},
        {"theme_manager", "Theme Manager", Category::Client, false},
        {"extension_center", "Extension Center", Category::Client, false},
        {"cosmetics_preview", "Cosmetics Preview", Category::Client, false},
        {"replay_timeline", "Replay Timeline", Category::Client, false},
        {"script_center", "Script Center", Category::Client, false},
        {"replay_preview", "Replay Preview", Category::Client, false}
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
    return "1.2.0-native";
}

ModuleManager& modules() {
    return g_modules;
}

}
