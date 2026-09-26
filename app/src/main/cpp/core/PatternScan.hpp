#pragma once

#include <android/log.h>
#include <cerrno>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <dlfcn.h>
#include <link.h>
#include <string>
#include <string_view>
#include <sys/mman.h>
#include <unistd.h>
#include <vector>

namespace riplow::memory {

inline constexpr const char* kLogTag = "RiplowNative";

struct ModuleMapping {
    uintptr_t start{0};
    uintptr_t end{0};
    int protection{PROT_READ};
};

inline int protection_from_perms(std::string_view perms) {
    int protection = 0;
    if (perms.size() >= 1 && perms[0] == 'r') protection |= PROT_READ;
    if (perms.size() >= 2 && perms[1] == 'w') protection |= PROT_WRITE;
    if (perms.size() >= 3 && perms[2] == 'x') protection |= PROT_EXEC;
    return protection;
}

inline bool find_mapping(uintptr_t address, ModuleMapping& out) {
    FILE* maps = std::fopen("/proc/self/maps", "re");
    if (!maps) return false;

    char line[512];
    bool found = false;
    while (std::fgets(line, sizeof(line), maps)) {
        unsigned long long start = 0;
        unsigned long long end = 0;
        char perms[5] = {};
        if (std::sscanf(line, "%llx-%llx %4s", &start, &end, perms) != 3) continue;
        if (address < static_cast<uintptr_t>(start) ||
            address >= static_cast<uintptr_t>(end)) {
            continue;
        }

        out.start = static_cast<uintptr_t>(start);
        out.end = static_cast<uintptr_t>(end);
        out.protection = protection_from_perms(perms);
        found = true;
        break;
    }

    std::fclose(maps);
    return found;
}

inline uintptr_t find_pattern(
    uintptr_t base,
    std::size_t region_size,
    const std::uint8_t* pattern,
    const char* mask,
    std::size_t pattern_size
) {
    if (!base || !region_size || !pattern || !mask || !pattern_size) return 0;

    const std::size_t mask_size = std::strlen(mask);
    if (mask_size != pattern_size || region_size < pattern_size) return 0;

    const auto* memory = reinterpret_cast<const std::uint8_t*>(base);
    const std::size_t last = region_size - pattern_size;

    for (std::size_t offset = 0; offset <= last; ++offset) {
        bool match = true;
        for (std::size_t i = 0; i < pattern_size; ++i) {
            if (mask[i] != '?' && memory[offset + i] != pattern[i]) {
                match = false;
                break;
            }
        }
        if (match) return base + offset;
    }
    return 0;
}

inline uintptr_t find_pattern(
    uintptr_t base,
    std::size_t region_size,
    std::string_view pattern,
    std::string_view mask
) {
    if (pattern.size() != mask.size() || pattern.empty()) return 0;

    return find_pattern(
        base,
        region_size,
        reinterpret_cast<const std::uint8_t*>(pattern.data()),
        mask.data(),
        pattern.size()
    );
}

struct ExecutableSegment {
    uintptr_t start{0};
    std::size_t size{0};
};

struct PatternSearchContext {
    std::string_view pattern;
    std::string_view mask;
    uintptr_t result{0};
};

inline int scan_executable_segment(struct dl_phdr_info* info, std::size_t, void* raw) {
    auto* context = static_cast<PatternSearchContext*>(raw);
    if (!info || !context || context->result) return 0;

    for (std::size_t i = 0; i < info->dlpi_phnum; ++i) {
        const ElfW(Phdr)& phdr = info->dlpi_phdr[i];
        if (phdr.p_type != PT_LOAD || !(phdr.p_flags & PF_X)) continue;

        const uintptr_t base = static_cast<uintptr_t>(info->dlpi_addr) + phdr.p_vaddr;
        const auto result = find_pattern(
            base,
            static_cast<std::size_t>(phdr.p_memsz),
            context->pattern,
            context->mask
        );
        if (result) {
            context->result = result;
            return 1;
        }
    }
    return 0;
}

inline uintptr_t find_module_pattern(
    const char* module_name,
    std::string_view pattern,
    std::string_view mask
) {
    if (!module_name || !*module_name) return 0;

    // dl_iterate_phdr exposes mapped ELF load segments without relying on
    // hard-coded image bases or offsets.
    PatternSearchContext context{pattern, mask, 0};
    dl_iterate_phdr(scan_executable_segment, &context);
    if (!context.result) {
        __android_log_print(ANDROID_LOG_DEBUG, kLogTag,
                            "Pattern not found in loaded images: %s", module_name);
    }
    return context.result;
}

class ScopedPageWrite {
public:
    explicit ScopedPageWrite(void* address, std::size_t length)
        : address_(reinterpret_cast<uintptr_t>(address)),
          length_(length) {
        if (!address_ || !length_) return;

        page_size_ = static_cast<std::size_t>(sysconf(_SC_PAGESIZE));
        if (!page_size_) page_size_ = 4096;

        page_start_ = address_ & ~(static_cast<uintptr_t>(page_size_ - 1));
        const uintptr_t end = address_ + length_;
        page_end_ = (end + page_size_ - 1) & ~(static_cast<uintptr_t>(page_size_ - 1));

        ModuleMapping mapping;
        if (!find_mapping(address_, mapping) || mapping.start > page_start_ || mapping.end < page_end_) {
            return;
        }

        original_protection_ = mapping.protection;
        if (mprotect(reinterpret_cast<void*>(page_start_),
                     page_end_ - page_start_,
                     PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
            return;
        }
        active_ = true;
    }

    ScopedPageWrite(const ScopedPageWrite&) = delete;
    ScopedPageWrite& operator=(const ScopedPageWrite&) = delete;

    ~ScopedPageWrite() {
        if (!active_) return;
        mprotect(reinterpret_cast<void*>(page_start_),
                 page_end_ - page_start_,
                 original_protection_);
    }

    explicit operator bool() const { return active_; }

private:
    uintptr_t address_{0};
    uintptr_t page_start_{0};
    uintptr_t page_end_{0};
    std::size_t length_{0};
    std::size_t page_size_{0};
    int original_protection_{PROT_READ};
    bool active_{false};
};

inline bool patch_bytes(void* target, const void* replacement, std::size_t length) {
    if (!target || !replacement || !length) return false;

    ScopedPageWrite writable(target, length);
    if (!writable) return false;

    std::memcpy(target, replacement, length);
    __builtin___clear_cache(
        reinterpret_cast<char*>(target),
        reinterpret_cast<char*>(target) + length
    );
    return true;
}

} // namespace riplow::memory
