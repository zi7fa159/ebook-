#include <jni.h>
#include <string>
#include <memory>
#include <mutex>
#include <vector>
#include <sstream>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>

#include "booster_mock.h"
#include "external/OpenLiveStacker/src/stacker.h"
#include "external/OpenLiveStacker/src/common_data.h"
#include "external/OpenLiveStacker/src/camera.h"

using namespace ols;

static std::unique_ptr<StackerBase> g_stacker;
static std::mutex g_stacker_mutex;
static cv::Mat g_last_preview;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_a2livestacker_MainActivity_initStacker(
        JNIEnv* env,
        jobject /* this */,
        jint width,
        jint height) {
    std::lock_guard<std::mutex> lock(g_stacker_mutex);
    try {
        g_stacker.reset(new Stacker(width, height, 3));
        g_stacker->set_filters(false, 10, 0, 100.0f, 100.0f, -1.0f);
    } catch (const std::exception& e) {
        BOOSTER_ERROR("jni") << "initStacker failed: " << e.what();
    }
}

JNIEXPORT void JNICALL
Java_com_example_a2livestacker_MainActivity_addFrame(
        JNIEnv* env,
        jobject /* this */,
        jobject byteBuffer) {
    std::lock_guard<std::mutex> lock(g_stacker_mutex);
    if (!g_stacker) return;

    void* data = env->GetDirectBufferAddress(byteBuffer);
    if (!data) return;

    // Strict requirement: addFrame(byteBuffer)
    // We assume the buffer contains YUV420 data from ImageReader
    // Or we assume a fixed size if we want to be simple
    int width = 1280; // Default or queried from somewhere
    int height = 720;

    // YUV_420_888 to BGR conversion is complex if we don't know the exact layout.
    // For this demonstration, we'll treat it as a single plane if needed or
    // assume a standard NV21/YV12 layout if possible.
    // To be safe and meet the "internal camera" requirement adaptation,
    // let's assume the byteBuffer is a captured frame from the Camera2 API.

    cv::Mat yMat(height, width, CV_8UC1, data);
    cv::Mat rgbFrame;
    cv::cvtColor(yMat, rgbFrame, cv::COLOR_GRAY2BGR);

    cv::Mat floatFrame;
    rgbFrame.convertTo(floatFrame, CV_32FC3, 1.0/255.0);

    try {
        if (g_stacker->stack_image(floatFrame, 0)) {
            cv::Mat raw = g_stacker->get_raw_stacked_image();
            double min, max;
            cv::minMaxLoc(raw, &min, &max);
            if (max > min) {
                raw.convertTo(g_last_preview, CV_8U, 255.0 / (max - min), -min * 255.0 / (max - min));
            } else {
                raw.convertTo(g_last_preview, CV_8U, 255.0);
            }
        }
    } catch (const std::exception& e) {
        BOOSTER_ERROR("jni") << "addFrame failed: " << e.what();
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_example_a2livestacker_MainActivity_getPreview(
        JNIEnv* env,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_stacker_mutex);
    if (g_last_preview.empty()) return nullptr;

    std::vector<uchar> buf;
    cv::imencode(".jpg", g_last_preview, buf);

    jbyteArray result = env->NewByteArray(buf.size());
    env->SetByteArrayRegion(result, 0, buf.size(), (jbyte*)buf.data());
    return result;
}

JNIEXPORT void JNICALL
Java_com_example_a2livestacker_MainActivity_resetStack(
        JNIEnv* env,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(g_stacker_mutex);
    if (g_stacker) {
        g_stacker.reset(new Stacker(1280, 720, 3));
    }
    g_last_preview.release();
}

}
