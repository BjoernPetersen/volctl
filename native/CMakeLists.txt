cmake_minimum_required(VERSION 3.14)
project(volctl)

set(CMAKE_CXX_STANDARD 17)

include(FindJNI)
include_directories(${JNI_INCLUDE_DIRS})

if (WIN32)
    add_library(volctl SHARED volctl-win.cpp volctl.h)
endif (WIN32)

if (UNIX)
    add_library(volctl SHARED volctl-alsa.cpp volctl.h)
endif (UNIX)
