cmake_minimum_required(VERSION 3.10.2)

project("a2livestacker")

# Try project-local OpenCV first
set(OpenCV_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../libs/opencv/sdk/native/jni)
find_package(OpenCV QUIET)

if(NOT OpenCV_FOUND)
    # Search for any opencv on the system
    find_package(OpenCV REQUIRED)
endif()

add_library(native-lib SHARED
            native-lib.cpp
            external/OpenLiveStacker/src/camera.cpp
            external/OpenLiveStacker/src/util.cpp)

target_include_directories(native-lib PRIVATE
                           ${CMAKE_CURRENT_SOURCE_DIR}
                           external/OpenLiveStacker/src
                           ${OpenCV_INCLUDE_DIRS})

# Include mocking headers first
target_compile_options(native-lib PRIVATE -include ${CMAKE_CURRENT_SOURCE_DIR}/booster_mock.h)

target_compile_definitions(native-lib PRIVATE
                           ANDROID_SUPPORT
                           USE_CV_SIMD)

target_link_libraries(native-lib
                      ${OpenCV_LIBS}
                      android
                      log)
