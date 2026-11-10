cmake_minimum_required(VERSION 3.20)
project(jhvwcs LANGUAGES C)

set(WCSLIB_SOURCE "" CACHE PATH "Unpacked pinned WCSLIB source")
set(WCSLIB_VERSION "" CACHE STRING "Pinned WCSLIB version")
set(wcs "${WCSLIB_SOURCE}/C")
if(NOT EXISTS "${wcs}/wcs.h" OR WCSLIB_VERSION STREQUAL "")
    message(FATAL_ERROR "Set WCSLIB_SOURCE and WCSLIB_VERSION from the pinned manifest")
endif()

file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/wcsconfig.h"
    "#define HAVE_WCSLIB_VERSION 1\n#define WCSLIB_VERSION ${WCSLIB_VERSION}\n"
    "#define WCSLIB_INT64 long long int\n")

add_library(wcslib STATIC
    "${wcs}/flexed/wcsbth.c" "${wcs}/flexed/wcspih.c"
    "${wcs}/flexed/wcsulex.c" "${wcs}/flexed/wcsutrn.c"
    "${wcs}/cel.c" "${wcs}/dis.c" "${wcs}/lin.c" "${wcs}/log.c"
    "${wcs}/prj.c" "${wcs}/spc.c" "${wcs}/sph.c" "${wcs}/spx.c"
    "${wcs}/tab.c" "${wcs}/wcs.c" "${wcs}/wcserr.c" "${wcs}/wcsfix.c"
    "${wcs}/wcshdr.c" "${wcs}/wcsprintf.c" "${wcs}/wcsunits.c"
    "${wcs}/wcsutil.c" "${wcs}/wcstrig.c")
target_include_directories(wcslib PUBLIC "${wcs}" "${CMAKE_CURRENT_BINARY_DIR}")
target_compile_definitions(wcslib PRIVATE WCSLIB_TLS=_Thread_local)
target_compile_options(wcslib PRIVATE -ffp-contract=off)
set_target_properties(wcslib PROPERTIES POSITION_INDEPENDENT_CODE ON C_VISIBILITY_PRESET hidden)

add_library(jhvwcs SHARED jhv_wcs.c)
target_link_libraries(jhvwcs PRIVATE wcslib)
target_compile_options(jhvwcs PRIVATE -Wall -Wextra -Werror -ffp-contract=off)
set_target_properties(wcslib jhvwcs PROPERTIES
    C_STANDARD 11 C_STANDARD_REQUIRED ON C_EXTENSIONS ON)
set_target_properties(jhvwcs PROPERTIES C_VISIBILITY_PRESET hidden)
if(WIN32)
    if(NOT MINGW)
        message(FATAL_ERROR "JHV's Windows WCS library requires the MinGW toolchain")
    endif()
    # WCSLIB headers also collide with MinGW's legacy wcsset alias in consumers.
    target_compile_definitions(wcslib
        PUBLIC _NO_OLDNAMES NO_OLDNAMES
        PRIVATE YY_NO_UNISTD_H _CRT_SECURE_NO_WARNINGS)
    set_target_properties(jhvwcs PROPERTIES PREFIX "")
    target_link_options(jhvwcs PRIVATE -static -static-libgcc
        -Wl,--no-insert-timestamp,--major-subsystem-version,5,--minor-subsystem-version,2)
elseif(APPLE)
    set_target_properties(jhvwcs PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE INSTALL_RPATH "@loader_path")
else()
    target_link_libraries(jhvwcs PRIVATE m)
    set_target_properties(jhvwcs PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE INSTALL_RPATH "$ORIGIN")
    target_link_options(jhvwcs PRIVATE -static-libgcc -Wl,-z,defs)
endif()
