cmake_minimum_required(VERSION 3.20)
project(jhvj2k LANGUAGES C CXX)

set(ESAJPIP "" CACHE PATH "esajpip source directory")
set(KDU_VENDOR "" CACHE PATH "Kakadu SDK directory")
set(KDU_LIBRARY "" CACHE FILEPATH "JHV's Kakadu shared library or import library")
set(KDU_CORE_LIBRARY "" CACHE FILEPATH "Kakadu core DLL or import library, for Windows")

# Build the core client with the bridge's toolchain; OpenJPEG is not used.
option(ESAJPIP_CLIENT_ONLY "Build the client without the server or tests" ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
add_subdirectory("${ESAJPIP}" client EXCLUDE_FROM_ALL)

add_library(jhvj2k SHARED jhv_j2k.cc jhv_kdu_geometry.cc jhv_kdu_decode.cc)
set_target_properties(jhvj2k PROPERTIES
    CXX_STANDARD 11 CXX_STANDARD_REQUIRED ON CXX_EXTENSIONS OFF)
# Match the client's LTO settings at the final link (including older GCC).
foreach(config RELEASE RELWITHDEBINFO MINSIZEREL)
    get_target_property(client_ipo esajpip_client INTERPROCEDURAL_OPTIMIZATION_${config})
    if(client_ipo)
        set_property(TARGET jhvj2k PROPERTY INTERPROCEDURAL_OPTIMIZATION_${config} ON)
    endif()
endforeach()
target_include_directories(jhvj2k SYSTEM PRIVATE
    "${KDU_VENDOR}/coresys/common" "${KDU_VENDOR}/apps/support" "${KDU_VENDOR}/apps/compressed_io")
target_link_libraries(jhvj2k PRIVATE esajpip_client "${KDU_LIBRARY}")
if(WIN32)
    if(NOT MINGW)
        message(FATAL_ERROR "JHV's Windows Kakadu libraries require the MinGW toolchain")
    endif()
    if(NOT EXISTS "${KDU_CORE_LIBRARY}")
        message(FATAL_ERROR "KDU_CORE_LIBRARY must name the Windows Kakadu core library")
    endif()
    # v7's auxiliary headers mark inline definitions dllimport with KDU_AUX_IMPORTS.
    # MinGW can link their external functions through import thunks without it.
    target_compile_definitions(jhvj2k PRIVATE CORESYS_IMPORTS)
    target_link_libraries(jhvj2k PRIVATE "${KDU_CORE_LIBRARY}")
    target_link_options(jhvj2k PRIVATE -static -static-libgcc -static-libstdc++
        -Wl,--major-subsystem-version,5,--minor-subsystem-version,2)
endif()
if(APPLE)
    set_target_properties(jhvj2k PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE INSTALL_RPATH "@loader_path")
elseif(UNIX)
    set_target_properties(jhvj2k PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE INSTALL_RPATH "$ORIGIN")
    target_link_options(jhvj2k PRIVATE -Wl,-z,defs)
endif()
if(NOT MSVC)
    target_compile_options(jhvj2k PRIVATE -Wall -Wextra -Werror)
endif()
