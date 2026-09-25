#pragma once
#include <cstddef>
#include <mutex>
#include <string>
#include <vector>

namespace riplow {
enum class Category { Performance, PvP, Utility, Visual, HUD, Network, Client };

struct Module {
    std::string id;
    std::string name;
    Category category;
    bool enabled{false};
};

class ModuleManager {
public:
    ModuleManager();
    bool toggle(const std::string& id);
    bool set_enabled(const std::string& id, bool enabled);
    std::size_t size() const;
    std::size_t enabled_count() const;
    std::string summary() const;
private:
    std::vector<Module> modules_;
    mutable std::mutex mutex_;
};

const char* core_version();
ModuleManager& modules();
}
