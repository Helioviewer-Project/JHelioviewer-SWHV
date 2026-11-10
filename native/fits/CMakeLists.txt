cmake_minimum_required(VERSION 3.20)
project(jhvfits LANGUAGES C)

find_package(Threads REQUIRED)
find_package(cfitsio CONFIG REQUIRED)

add_library(jhvfits SHARED jhv_fits.c)
target_link_libraries(jhvfits PRIVATE CFITSIO::cfitsio)
set_target_properties(jhvfits PROPERTIES
    C_STANDARD 11 C_STANDARD_REQUIRED ON C_EXTENSIONS OFF)
target_compile_options(jhvfits PRIVATE -Wall -Wextra -Werror)
if(WIN32)
    if(NOT MINGW)
        message(FATAL_ERROR "JHV's Windows FITS reader requires the MinGW toolchain")
    endif()
    set_target_properties(jhvfits PROPERTIES PREFIX "")
    target_link_options(jhvfits PRIVATE -static -static-libgcc
        -Wl,--no-insert-timestamp,--major-subsystem-version,5,--minor-subsystem-version,2)
elseif(APPLE)
    set_target_properties(jhvfits PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE INSTALL_RPATH "@loader_path")
else()
    set_target_properties(jhvfits PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE INSTALL_RPATH "$ORIGIN")
    target_link_options(jhvfits PRIVATE -static-libgcc -Wl,-z,defs)
endif()
