cmake_minimum_required(VERSION 3.22.1)
project("riplow_core")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(RIPLOW_ENABLE_PROCESS_HOOKS
    "Enable in-process eglSwapBuffers hooks. Requires a separately supplied Dobby tree."
    OFF
)

set(RIPLOW_DOBBY_DIR "" CACHE PATH "Path to a pinned Dobby source tree.")

add_library(riplow_core SHARED
    riplow_core.cpp
    core/module.cpp
    core/performance.cpp
    core/network.cpp
    core/HookEngine.cpp
    core/RenderPipeline.cpp
)

target_include_directories(riplow_core PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_definitions(riplow_core PRIVATE
    RIPLOW_ENABLE_PROCESS_HOOKS=$<BOOL:${RIPLOW_ENABLE_PROCESS_HOOKS}>
)

find_library(log-lib log)
find_library(egl-lib EGL)
find_library(dl-lib dl)

if(RIPLOW_ENABLE_PROCESS_HOOKS)
    if(NOT RIPLOW_DOBBY_DIR)
        message(FATAL_ERROR
            "RIPLOW_ENABLE_PROCESS_HOOKS=ON requires -DRIPLOW_DOBBY_DIR=/path/to/pinned/Dobby"
        )
    endif()

    add_subdirectory("${RIPLOW_DOBBY_DIR}" "${CMAKE_BINARY_DIR}/dobby")
    target_compile_definitions(riplow_core PRIVATE RIPLOW_USE_DOBBY=1)
    target_link_libraries(riplow_core dobby)
else()
    target_compile_definitions(riplow_core PRIVATE RIPLOW_USE_DOBBY=0)
endif()

target_link_libraries(riplow_core
    ${log-lib}
    ${egl-lib}
    ${dl-lib}
)

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(riplow_core PRIVATE -fno-omit-frame-pointer -fstack-protector-strong)
else()
    target_compile_options(riplow_core PRIVATE -fstack-protector-strong)
endif()
