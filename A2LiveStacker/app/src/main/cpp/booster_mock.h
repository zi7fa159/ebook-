#pragma once
#include <android/log.h>
#include <string>
#include <sstream>
#include <memory>
#include <chrono>
#include <thread>
#include <vector>

#define LOG_TAG "OLS_ENGINE"
#define BOOSTER_INFO(x) ols::LogHelper(ANDROID_LOG_INFO)
#define BOOSTER_WARNING(x) ols::LogHelper(ANDROID_LOG_WARN)
#define BOOSTER_ERROR(x) ols::LogHelper(ANDROID_LOG_ERROR)

namespace booster {
    using std::shared_ptr;

    namespace log {
        enum level { error, warn, info, debug };
    }

    namespace ptime {
        inline std::chrono::steady_clock::time_point now() {
            return std::chrono::steady_clock::now();
        }
        inline double to_number(std::chrono::duration<double> d) {
            return d.count();
        }
        inline void millisleep(int ms) {
            std::this_thread::sleep_for(std::chrono::milliseconds(ms));
        }
    }

    template<typename T>
    std::string trace(const T& e) { return ""; }

    class shared_object {
    public:
        shared_object() : h_(nullptr) {}
        shared_object(std::string const &name) : h_(nullptr) {}
        bool loaded() const { return false; }
        template<typename T>
        T get(std::string const &) const { return nullptr; }
    private:
        void *h_;
    };
}

namespace ols {
    class LogHelper {
    public:
        LogHelper(int priority) : priority_(priority) {}
        template<typename T>
        LogHelper& operator<<(const T& msg) {
            oss_ << msg;
            return *this;
        }
        LogHelper& operator<<(std::ostream& (*f)(std::ostream&)) {
            f(oss_);
            return *this;
        }
        ~LogHelper() {
            __android_log_print(priority_, LOG_TAG, "%s", oss_.str().c_str());
        }
    private:
        int priority_;
        std::ostringstream oss_;
    };
}
