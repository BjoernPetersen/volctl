cmake_minimum_required(VERSION 3.14)
project(volctl)

set(CMAKE_CXX_STANDARD 17)

include(FindJNI)
include_directories(${JNI_INCLUDE_DIRS})
add_library(volctl SHARED volctl.cpp volctl.h)
