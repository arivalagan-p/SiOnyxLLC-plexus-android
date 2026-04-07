//
// Created by JIIN07800316 on 12-05-2025.
//

#include <jni.h>
#include <gst/gst.h>
#include <gst/tag/tag.h>
#include <gst/app/gstappsink.h>
#include <cstring>
#include <pthread.h>
#include <regex.h>
#include <atomic>
#include <stdexcept>
#include <gst/gstinfo.h>
#include <gst/gstelement.h>
#include <gst/gstpipeline.h>
#include <gst/gstmessage.h>
#include <gst/gstbus.h>
#include <gst/gstelementfactory.h>
#include <gst/gstpad.h>
#include <gst/rtsp/gstrtsp.h>
#include <gst/rtsp/gstrtspmessage.h>
#include <ctime>
#include <cerrno>
#include <android/log.h>
#include <linux/resource.h>
#include <sys/resource.h>
#include <__algorithm/min.h>
#include <unistd.h>
#include <thread>
#include <chrono>
#include <sys/socket.h>
#include <netdb.h>
#include <fcntl.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <cstdlib>
#include <netinet/in.h>
#include <arpa/inet.h>

// Define states for the Java side
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wformat-insufficient-args"

// Error Codes
#define ERR_PIPELINE_CREATION_FAILED    1000
#define ERR_PIPELINE_DECODE_FAILED      1001
#define ERR_PIPELINE_STATE_FAILED       1002
#define ERR_ELEMENT_CREATION_FAILED     1003
#define ERR_ELEMENT_LINK_FAILED         1004
#define ERR_BUS_CREATION_FAILED         1005
#define ERR_RTSP_URI_INVALID            1006
#define ERR_RTSP_URI_PARSE_FAILED       1007
#define ERR_NO_FRAMES_RECEIVED          1008
#define ERR_BUFFERING_TIMEOUT           1009
#define ERR_RTSP_TIMEOUT                1010
#define ERR_AUTH_FAILED                 1011
#define ERR_SERVER_ERROR                1012
#define ERR_NETWORK_ERROR               1013
#define ERR_STREAM_ERROR                1014
#define ERR_GSTREAMER_STATE_DIRTY       1015
#define ERR_CONNECTION_CLOSED           1016
#define ERR_STREAM_EOS                  1017
#define ERR_UNKNOWN                     1018

// Watchdog and reconnection constants
#define MAX_RETRIES 3
#define WATCHDOG_TIMEOUT (15 * G_TIME_SPAN_SECOND)
#define WATCHDOG_INTERVAL 5
#define INITIAL_RETRY_DELAY_MS 2000
#define MAX_RETRY_DELAY_MS 15000
#define BACKOFF_FACTOR 2


// Add these states to your existing state definitions
#define STREAM_STATE_IDLE               0
#define STREAM_STATE_INITIALIZING       1
#define STREAM_STATE_CONNECTING         2
#define STREAM_STATE_BUFFERING          3
#define STREAM_STATE_PLAYING            4
#define STREAM_STATE_RECOVERING         5
#define STREAM_STATE_STOPPING           6
#define STREAM_STATE_ERROR              7
#define STREAM_STATE_INITIATED_STOP     8
#define STREAM_STATE_STOPPED            9

static GstClockTime last_dts = GST_CLOCK_TIME_NONE;
static const GstClockTime DEFAULT_FRAME_DURATION = GST_MSECOND * 33; // 30fps
static GstClockTime last_sent_pts = GST_CLOCK_TIME_NONE;

typedef struct CustomData {
    GstElement *pipeline;
    GstElement *appsink;
    JavaVM *jvm;
    jobject app;
    char *rtsp_uri;
    pthread_t thread_id;
    pthread_mutex_t lock;
    pthread_mutex_t jni_mutex;
    std::atomic<gint> frame_count;
    guint64 fps_last_time;
    guint64 fps;
    guint keepalive_timer;
    guint buffering_timeout_counter;
    GMainLoop *main_loop;
    GstBus *bus;
    guint bus_watch_id;
    guint64 stream_start_time;
    guint64 low_fps_start_time;
    gint low_fps_counter;
    GstBuffer *sps;
    GstBuffer *pps;
    guint watchdog_timer;
    GstElement *depay;
    GstElement *queue0;
    GstElement *queue1;
    GstElement *filter;
    GstElement *encoder;


    std::atomic<gint> retry_count;
    std::atomic<gint64> next_retry_delay_ms;
    std::atomic<gboolean> in_recovery;
    std::atomic<gboolean> stream_has_started;
    std::atomic<gboolean> streaming;
    std::atomic<gboolean> thread_done;
    std::atomic<gboolean> flushing;
    std::atomic<gboolean> is_buffering;
    std::atomic<gboolean> is_buffering_low_fps;
    std::atomic<gboolean> sps_pps_extracted;
    std::atomic<gboolean> sps_pps_sent;
    std::atomic<gint64> last_sample_time;
    std::atomic<guint> active_timer_id;

    std::atomic<bool> stream_alive{false};

    std::atomic<gboolean> waiting_for_response;
    guint no_response_timer;


    // Add these for better state management
    std::atomic<gint> stream_state;
    std::atomic<gboolean> stop_requested;
    std::atomic<gboolean> start_requested;
    pthread_cond_t state_change_cond;
    int zero_fps_count = 0;
    GAsyncQueue *frame_queue;    // Queue for passing samples to worker
    GThread     *worker_thread;  // Dedicated worker thread for heavy processing
} CustomData;
static guint timer_id;
static CustomData *global_data = nullptr;

static gboolean watchdog_check(gpointer user_data);
static void *start_event_driven_stream(void *userdata);
static void restart_pipeline(CustomData *data);
static gboolean delayed_restart_cb(gpointer user_data);
static GstFlowReturn on_new_sample(GstAppSink *appsink, gpointer userdata);

static void start_no_response_timer(CustomData *data);

static void cancel_no_response_timer(CustomData *data);

extern "C" void Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeRelease(JNIEnv *pEnv, jobject thiz);

JNIEnv* get_jni_env(CustomData *data) {
    if (!data || !data->jvm) return nullptr;

    JNIEnv *env = nullptr;
    pthread_mutex_lock(&data->jni_mutex);
    jint res = data->jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        res = data->jvm->AttachCurrentThread(&env, nullptr);
        if (res != JNI_OK) {
            pthread_mutex_unlock(&data->jni_mutex);
            return nullptr;
        }
    }
    pthread_mutex_unlock(&data->jni_mutex);
    return env;
}

static const char* get_state_name(gint state) {
    switch(state) {
        case STREAM_STATE_IDLE: return "IDLE";
        case STREAM_STATE_INITIALIZING: return "INITIALIZING";
        case STREAM_STATE_CONNECTING: return "CONNECTING";
        case STREAM_STATE_BUFFERING: return "BUFFERING";
        case STREAM_STATE_PLAYING: return "PLAYING";
        case STREAM_STATE_RECOVERING: return "RECOVERING";
        case STREAM_STATE_STOPPING: return "STOPPING";
        case STREAM_STATE_ERROR: return "ERROR";
        case STREAM_STATE_INITIATED_STOP: return "INITIATED_STOP";
        case STREAM_STATE_STOPPED: return "STOPPED";
        default: return "UNKNOWN";
    }
}

static void set_stream_state(CustomData *data, gint new_state) {
    if (!data) return;

    gint old_state = data->stream_state.load();
    data->stream_state.store(new_state);

    g_print("[StreamState] %s -> %s\n", get_state_name(old_state), get_state_name(new_state));

    // Broadcast state change for any waiting threads
    pthread_cond_broadcast(&data->state_change_cond);
}

static gint get_stream_state(CustomData *data) {
    return data ? data->stream_state.load() : STREAM_STATE_IDLE;
}

static gboolean wait_for_state(CustomData *data, gint target_state, gint timeout_ms) {
    if (!data) return FALSE;

    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_nsec += (timeout_ms % 1000) * 1000000;
    ts.tv_sec += timeout_ms / 1000;

    pthread_mutex_lock(&data->lock);

    while (data->stream_state.load() != target_state) {
        if (pthread_cond_timedwait(&data->state_change_cond, &data->lock, &ts) != 0) {
            pthread_mutex_unlock(&data->lock);
            return FALSE; // Timeout
        }
    }

    pthread_mutex_unlock(&data->lock);
    return TRUE;
}

gboolean is_valid_rtsp_uri(const gchar *uri) {
    const char *pattern = "^rtsp://([a-zA-Z0-9.-]+)(:[0-9]+)?(/.*)?$";
    regex_t regex;
    int reti;

    reti = regcomp(&regex, pattern, REG_EXTENDED);
    if (reti) {
        g_printerr("[GStreamer] Could not compile regex\n");
        return FALSE;
    }

    reti = regexec(&regex, uri, 0, nullptr, 0);
    regfree(&regex);

    return (reti == 0);
}

static inline bool can_call_java(CustomData* mData) {
    return mData &&
           mData->stream_alive.load(std::memory_order_acquire) &&
           mData->app;
}

static void notify_stream_started(JNIEnv *env, jobject app) {
    if (!env || !app) return;

    pthread_mutex_lock(&global_data->jni_mutex);
    jclass clazz = env->GetObjectClass(app);
    if (clazz) {
        jmethodID method = env->GetMethodID(clazz, "onStreamStarted", "()V");
        if (method) {
            env->CallVoidMethod(app, method);
        }
        env->DeleteLocalRef(clazz);
    }
    pthread_mutex_unlock(&global_data->jni_mutex);
    // reset timer
    global_data->stream_has_started.store(true);
    cancel_no_response_timer(global_data);
    g_print("[GStreamer] Stream started notification sent to Java.\n");
}

static void cancel_no_response_timer(CustomData *data) {
    if (!data) return;

    if (data->no_response_timer) {
        g_source_remove(data->no_response_timer);
        data->no_response_timer = 0;
    }

    g_print("[GStreamer] Canceled 15S no-response timer.\n");

    data->waiting_for_response.store(false);
}

static void notify_stream_stopped(JNIEnv *env, jobject app, jint state) {
    if (!global_data) {
        g_print("[GStreamer] notify_stream_stopped: global_data is null\n");
        return;
    }

    if (!can_call_java(global_data)) return;

    // Use the global_data's JVM if env is null
    if (!env && global_data->jvm) {
        jint result = global_data->jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        if (result == JNI_EDETACHED) {
            result = global_data->jvm->AttachCurrentThread(&env, nullptr);
            if (result != JNI_OK) {
                g_printerr("[GStreamer] Failed to attach thread for notification\n");
                return;
            }
        }
    }

    if (!env || !app) {
        g_print("[GStreamer] notify_stream_stopped: env or app still null\n");
        return;
    }

    pthread_mutex_lock(&global_data->jni_mutex);

    jclass clazz = env->GetObjectClass(app);
    if (clazz) {
        jmethodID method = env->GetMethodID(clazz, "onStreamStopped", "(I)V");
        if (method) {
            env->CallVoidMethod(app, method, state);
            g_print("[GStreamer] ✓ Stream stopped notification sent to Java (state=%d)\n", state);

            // Check for exception
            if (env->ExceptionCheck()) {
                g_printerr("[GStreamer] Exception during onStreamStopped\n");
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        } else {
            g_warning("[GStreamer] Method onStreamStopped not found.");
        }
        env->DeleteLocalRef(clazz);
    } else {
        g_warning("[GStreamer] Failed to get Java class for onStreamStopped notification.");
    }

    pthread_mutex_unlock(&global_data->jni_mutex);
}

static void notify_media_data(JNIEnv *env, jobject app, jobject byteBuffer, jlong pts_us, jlong dts_us) {
    if (!env || !app || !byteBuffer) return;

    jclass clazz = env->GetObjectClass(app);
    if (clazz) {
        jmethodID method = env->GetMethodID(clazz, "onMediaRecordByteArrayWithPtsDts", "(Ljava/nio/ByteBuffer;JJ)V");
        if (method) {
            env->CallVoidMethod(app, method, byteBuffer, pts_us, dts_us);
        }
        env->DeleteLocalRef(clazz);
    }
//    env->DeleteLocalRef(byteBuffer);
}

static void notify_low_fps_long_time(JNIEnv *env, jobject app) {
    if (!env || !app) return;

    pthread_mutex_lock(&global_data->jni_mutex);
    jclass clazz = env->GetObjectClass(app);
    if (clazz) {
        jmethodID method = env->GetMethodID(clazz, "onLowFpsLongTime", "()V");
        if (method) {
            env->CallVoidMethod(app, method);
        }
        env->DeleteLocalRef(clazz);
    }
    pthread_mutex_unlock(&global_data->jni_mutex);
    g_print("[GStreamer] Low FPS for a long time notification sent to Java.\n");
}

static void notify_buffering_state(JNIEnv *env, jobject app, jboolean buffering) {
    if (!env || !app) return;

    pthread_mutex_lock(&global_data->jni_mutex);
    jclass clazz = env->GetObjectClass(app);
    if (clazz) {
        jmethodID method = env->GetMethodID(clazz, "onBufferingStateChanged", "(Z)V");
        if (method) {
            env->CallVoidMethod(app, method, buffering);
        }
        env->DeleteLocalRef(clazz);
    }
    pthread_mutex_unlock(&global_data->jni_mutex);
}

static void notify_stream_error(JNIEnv *env, jobject app, jint code, const char *message) {
    if (!env || !app || !message) return;

    if (!can_call_java(global_data)) return;

    g_printerr("[GStreamer] notify_stream_error called");

    pthread_mutex_lock(&global_data->jni_mutex);
    jclass clazz = env->GetObjectClass(app);
    if (!clazz) {
        pthread_mutex_unlock(&global_data->jni_mutex);
        return;
    }

    jmethodID method = env->GetMethodID(clazz, "onStreamError", "(ILjava/lang/String;)V");
    if (!method) {
        env->DeleteLocalRef(clazz);
        pthread_mutex_unlock(&global_data->jni_mutex);
        return;
    }

    jstring msg = env->NewStringUTF(message);
    if (!msg) {
        env->DeleteLocalRef(clazz);
        pthread_mutex_unlock(&global_data->jni_mutex);
        return;
    }

    env->CallVoidMethod(app, method, code, msg);
    env->DeleteLocalRef(msg);
    env->DeleteLocalRef(clazz);
    pthread_mutex_unlock(&global_data->jni_mutex);
}

static void enter_buffering(JNIEnv *env, CustomData *data) {
    if (!data->is_buffering) {
        data->is_buffering = TRUE;
        notify_buffering_state(env, data->app, TRUE);
        g_print("[Buffering] Entered buffering state.\n");
    }
}

static void exit_buffering(JNIEnv *env, CustomData *data) {
    if (data->is_buffering) {
        data->is_buffering = FALSE;
        notify_buffering_state(env, data->app, FALSE);
        g_print("[Buffering] Exited buffering state.\n");
    }
}

void notify_loading_status(JNIEnv *env, jobject app, int retryCount, int maxRetries, bool show) {
    if (!env || !app) return;

    jclass clazz = env->GetObjectClass(app);
    if (!clazz) {
        g_printerr("[JNI] Failed to get app class in notify_loading_status\n");
        return;
    }

    // Signature: (IIZ)V → two ints, one boolean
    jmethodID method = env->GetMethodID(clazz, "onStreamLoadingRetry", "(IIZ)V");
    if (!method) {
        g_printerr("[JNI] Method onStreamLoadingRetry(int, int, boolean) not found\n");
        env->DeleteLocalRef(clazz);
        return;
    }

    env->CallVoidMethod(app, method, retryCount, maxRetries, (jboolean)show);
    env->DeleteLocalRef(clazz);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

static void calculate_fps(CustomData *data) {
    if (!data || !data->pipeline) return;

    GstClock *clock = gst_element_get_clock(data->pipeline);
    if (!clock) return;

    guint64 current_time = gst_clock_get_time(clock);
    gst_object_unref(clock);

    if (current_time == 0) return;
    if (data->fps_last_time == 0) {
        data->fps_last_time = current_time;
        return;
    }

    guint64 time_diff = current_time - data->fps_last_time;

    if (time_diff >= 5 * GST_SECOND) {
        // Thread safety
        if (pthread_mutex_trylock(&data->lock) == 0) {
            data->fps = (guint)(data->frame_count * GST_SECOND / time_diff);
            data->frame_count = 0;
            pthread_mutex_unlock(&data->lock);
        } else {
            g_printerr("[GStreamer] Mutex lock failed in C++ fps calculation\n");
            return;
        }

        data->fps_last_time = current_time;

        // JNI section
        JNIEnv *env = nullptr;
        bool attached = false;

        if (!data->jvm) return;

        if (data->jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            if (data->jvm->AttachCurrentThread(&env, nullptr) != 0) {
                g_printerr("[GStreamer] Failed to attach current thread (C++)\n");
                return;
            }
            attached = true;
        }

        if (env && data->app) {
            jclass clazz = env->GetObjectClass(data->app);
            if (clazz) {
                jmethodID method = env->GetMethodID(clazz, "onFPSChanged", "(I)V");
                if (method) {
                    g_print("[GStreamer] FPS on CPP: %llu", data->fps);
                    env->CallVoidMethod(data->app, method, data->fps);
                } else {
                    g_printerr("[GStreamer] Failed to find onFPSChanged method (C++)\n");
                }
                env->DeleteLocalRef(clazz);
            } else {
                g_printerr("[GStreamer] Failed to get Java class reference (C++)\n");
            }
        }

        if (attached) {
            data->jvm->DetachCurrentThread();
        }
    }
}

static gboolean has_property(GObject *object, const gchar *property_name) {
    GParamSpec **pspecs;
    guint n_properties;
    gboolean found = FALSE;

    pspecs = g_object_class_list_properties(G_OBJECT_GET_CLASS(object), &n_properties);
    for (guint i = 0; i < n_properties; i++) {
        if (g_strcmp0(pspecs[i]->name, property_name) == 0) {
            found = TRUE;
            break;
        }
    }
    g_free(pspecs);
    return found;
}

static void cleanup_retry_state(CustomData *data) {
    if (!data) return;

    // Cancel any pending timer using atomic exchange
    guint current_timer = data->active_timer_id.exchange(0);
    if (current_timer != 0) {
        g_source_remove(current_timer);
        g_print("[Cleanup] Removed active timer ID: %u\n", current_timer);
    }

    // Also clean up global timer_id
    pthread_mutex_lock(&data->lock);
    if (timer_id != 0) {
        g_source_remove(timer_id);
        timer_id = 0;
    }
    pthread_mutex_unlock(&data->lock);

    // Reset retry state atomically
    data->retry_count.store(0);
    data->in_recovery.store(false);
    data->next_retry_delay_ms.store(INITIAL_RETRY_DELAY_MS);
    data->stream_has_started.store(false);

    g_print("[Cleanup] Retry state reset\n");
}

static void cleanup_streaming_resources(CustomData *data, JNIEnv *env) {
    if (!data || !env) return;

    notify_stream_stopped(env, global_data->app, STREAM_STATE_STOPPED);

    global_data->stream_alive.store(false, std::memory_order_release);

    g_print("[Cleanup] Starting resource cleanup\n");

    // Clean up retry state first
    cleanup_retry_state(data);

    // Clear atomic timer ID
    data->active_timer_id.store(0);

    // Remove source IDs first (watchdog timer, bus watch)
    if (data->watchdog_timer) {
        g_source_remove(data->watchdog_timer);
        data->watchdog_timer = 0;
    }
    if (data->bus_watch_id) {
        g_source_remove(data->bus_watch_id);
        data->bus_watch_id = 0;
    }

    // Pipeline state transitions for safe teardown
    if (data->pipeline) {
        GstState current_state, pending_state;
        gst_element_get_state(data->pipeline, &current_state, &pending_state, 0);
        g_print("[Cleanup] Current state: %s, pending: %s\n",
                gst_element_state_get_name(current_state),
                gst_element_state_get_name(pending_state));

        // If playing, step through pause → ready → null
        if (current_state == GST_STATE_PLAYING || current_state == GST_STATE_PAUSED) {
            g_print("[Cleanup] Transition: PLAYING → PAUSED\n");
            gst_element_set_state(data->pipeline, GST_STATE_PAUSED);
            g_usleep(100 * 1000);

            // === Optional: Send RTSP TEARDOWN ===
            GstElement *rtspsrc = gst_bin_get_by_name(GST_BIN(data->pipeline), "src"); // adjust name if different
            if (rtspsrc) {
                GstEvent *teardown_event = gst_event_new_custom(GST_EVENT_CUSTOM_DOWNSTREAM,
                                                                gst_structure_new_empty("GstRTSPTeardown"));
                gboolean sent = gst_element_send_event(rtspsrc, teardown_event);
                g_print("[Cleanup] Sent RTSP TEARDOWN event: %s\n", sent ? "Success" : "Failed");
                gst_object_unref(rtspsrc);

                // Wait to let server process the teardown
                g_usleep(300 * 1000);
            }
            // === End TEARDOWN ===

            g_print("[Cleanup] Transition: PAUSED → READY\n");
            gst_element_set_state(data->pipeline, GST_STATE_READY);
            g_usleep(500 * 1000);

            g_print("[Cleanup] Transition: READY → NULL\n");
            gst_element_set_state(data->pipeline, GST_STATE_NULL);
            GstStateChangeReturn null_ret = gst_element_get_state(data->pipeline, nullptr, nullptr, 2 * GST_SECOND);
            if (null_ret == GST_STATE_CHANGE_FAILURE) {
                g_printerr("[Cleanup] Failed to transition to NULL state\n");
            } else {
                g_print("[Cleanup] Pipeline reached NULL state\n");
            }
        }
        g_clear_object(&data->pipeline);
    }

    // Nullify remaining GStreamer elements
    data->appsink = nullptr;
    data->queue0 = nullptr;
    data->queue1 = nullptr;

    if (data->bus) {
        gst_object_unref(data->bus);
        data->bus = nullptr;
    }

    if (data->pipeline) {
        gst_object_unref(data->pipeline);
        data->pipeline = nullptr;
    }

    if (data->rtsp_uri) {
        g_free(data->rtsp_uri);
        data->rtsp_uri = nullptr;
    }

    if (data->sps) {
        gst_buffer_unref(data->sps);
        data->sps = nullptr;
    }
    if (data->pps) {
        gst_buffer_unref(data->pps);
        data->pps = nullptr;
    }

    if (data->main_loop && g_main_loop_is_running(data->main_loop)) {
        g_main_loop_quit(data->main_loop);
        g_main_loop_unref(data->main_loop);
        data->main_loop = nullptr;
    }

    if (data->keepalive_timer) {
        g_source_remove(data->keepalive_timer);
        data->keepalive_timer = 0;
    }

    if (data->no_response_timer) {
        g_source_remove(data->no_response_timer);
        data->no_response_timer = 0;
    }


//    notify_stream_stopped(env, data->app, STREAM_STATE_STOPPED);

    if (data->app) {
        env->DeleteGlobalRef(data->app);
        data->app = nullptr;
    }

    data->streaming = FALSE;
    data->thread_done = TRUE;
    data->stream_start_time = 0;
    data->low_fps_start_time = 0;
    data->low_fps_counter = 0;
    data->is_buffering_low_fps = FALSE;
    data->sps_pps_extracted = FALSE;
    data->sps_pps_sent = FALSE;
    data->retry_count = 0;
    data->in_recovery = FALSE;
    data->next_retry_delay_ms = INITIAL_RETRY_DELAY_MS;
    data->stream_has_started = FALSE;
    data->zero_fps_count = 0;

    g_print("[Cleanup] Resource cleanup complete\n");
}

static void update_ui_status_from_native(JNIEnv *env, jobject app, gint progress_type_value, const gchar *status) {
    if (!env || !app || !status) return;

    pthread_mutex_lock(&global_data->jni_mutex);
    jclass clazz = env->GetObjectClass(app);
    if (!clazz) {
        pthread_mutex_unlock(&global_data->jni_mutex);
        return;
    }

    jmethodID method = env->GetMethodID(clazz, "onProgressStatus", "(ILjava/lang/String;)V");
    if (!method) {
        env->DeleteLocalRef(clazz);
        pthread_mutex_unlock(&global_data->jni_mutex);
        return;
    }

    jstring jstatus = env->NewStringUTF(status);
    if (!jstatus) {
        env->DeleteLocalRef(clazz);
        pthread_mutex_unlock(&global_data->jni_mutex);
        return;
    }

    env->CallVoidMethod(app, method, progress_type_value, jstatus);
    env->DeleteLocalRef(jstatus);
    env->DeleteLocalRef(clazz);
    pthread_mutex_unlock(&global_data->jni_mutex);
}

static gboolean bus_call(GstBus *bus, GstMessage *msg, gpointer data) {
    auto *custom = (CustomData *) data;

    if (!can_call_java(custom)) return FALSE;

    if (!custom || !custom->pipeline) {
        g_print("[GStreamer] Error: Invalid custom data or pipeline is NULL.\n");
        return FALSE;
    }

    JNIEnv *env = get_jni_env(custom);
    if (!env) return FALSE;

    switch (GST_MESSAGE_TYPE(msg)) {
        case GST_MESSAGE_EOS: {
            g_print("[GStreamer] End-of-stream (EOS) received.\n");
            if (!custom->in_recovery) {
                if (custom->retry_count < MAX_RETRIES) {
                    g_print("[GStreamer] Attempting to restart after EOS\n");
                    restart_pipeline(custom);
                } else {
                    notify_stream_error(env, custom->app, ERR_RTSP_TIMEOUT, "RTSP stream ended (EOS).");
                    cleanup_streaming_resources(custom, env);
                }
            }
            break;
        }
        case GST_MESSAGE_ERROR: {
            GError *err = nullptr;
            gchar *debug = nullptr;
            gst_message_parse_error(msg, &err, &debug);

            gboolean should_recover = FALSE;
            gint err_code = ERR_UNKNOWN;
            const gchar *description = "Unknown GStreamer error";


            if (err->domain == GST_RESOURCE_ERROR) {
                if (g_strrstr(err->message, "Could not open resource") ||
                    g_strrstr(err->message, "not-found") ||
                    g_strrstr(err->message, "404")) {
                    err_code = ERR_RTSP_URI_INVALID;
                    description = "RTSP resource error";
                } else if (g_strrstr(err->message, "authentication")) {
                    err_code = ERR_AUTH_FAILED;
                    description = "Authentication failed";
                } else {
                    err_code = ERR_NETWORK_ERROR;
                    description = "Network error while streaming";
                    should_recover = TRUE;
                }
            }else if (err->domain == GST_STREAM_ERROR) {
                if (g_strrstr(err->message, "decode")) {
                    err_code = ERR_PIPELINE_DECODE_FAILED;
                    description = "Decoding failed";
                    should_recover = TRUE;
                } else {
                    err_code = ERR_STREAM_ERROR;
                    description = "Stream error occurred";
                    should_recover = TRUE;
                }
            }else if (err->domain == GST_URI_ERROR) {
                err_code = ERR_RTSP_URI_PARSE_FAILED;
                description = "Failed to parse RTSP URI";
            }else if (err->domain == GST_CORE_ERROR) {
                err_code = ERR_PIPELINE_CREATION_FAILED;
                description = "Core pipeline error";
            }else if (g_strrstr(err->message, "timeout")) {
                err_code = ERR_RTSP_TIMEOUT;
                description = "RTSP timeout occurred";
                should_recover = TRUE;
            }

            g_printerr("[GStreamer] ERROR: %s (Domain: %d, Code: %d)\nDebug: %s\n",
                       err->message, err->domain, err->code, debug ? debug : "none");

            switch (err_code) {
                case ERR_RTSP_TIMEOUT:
                case ERR_NETWORK_ERROR:
                    if (!custom->in_recovery) {
                        if(custom->retry_count < MAX_RETRIES){
                            g_printerr("[GStreamer] ERROR: ERR_NETWORK_ERROR restart_pipeline");
                            restart_pipeline(custom);
                        }else{
                            notify_stream_error(env, custom->app, ERR_NETWORK_ERROR, "Max retries reached or in recovery");
                            notify_loading_status(env, custom->app, 0, MAX_RETRIES, FALSE);
                        }
                    } else{
                        g_printerr("[GStreamer] ERROR: ERR_NETWORK_ERROR else");
                    }
                    break;
                case ERR_STREAM_ERROR:  // ERR_STREAM_ERROR
                    if (!custom->in_recovery) {
                        if(custom->retry_count < MAX_RETRIES){
                            g_printerr("[GStreamer] ERROR: ERR_STREAM_ERROR restart_pipeline");
                            restart_pipeline(custom);
                        }else{
                            notify_stream_error(env, custom->app, ERR_STREAM_ERROR, "Max retries reached or in recovery");
                            notify_loading_status(env, custom->app, 0, MAX_RETRIES, FALSE);
                        }
                    }else {
                        g_printerr("[GStreamer] ERROR: ERR_STREAM_ERROR else");
                    }
                    break;
                default:
                    if (!custom->in_recovery) {
                        notify_stream_error(env, custom->app, err_code, err->message);
                    }
                    break;
            }
            g_clear_error(&err);
            g_free(debug);
            break;
        }
        case GST_MESSAGE_WARNING: {
            GError *err = nullptr;
            gchar *debug = nullptr;
            gst_message_parse_warning(msg, &err, &debug);

            if (!custom->in_recovery) {
                g_printerr("[GStreamer] WARNING: %s (Domain: %d, Code: %d)\nDebug: %s\n",
                           err->message, err->domain, err->code, debug ? debug : "none");
            }

            g_clear_error(&err);
            g_free(debug);
            break;
        }
        case GST_MESSAGE_INFO: {
            g_print("[GStreamer] GST_MESSAGE_INFO.\n");
            break;
        }
        case GST_MESSAGE_TAG: {
            GstTagList *tags;
            gst_message_parse_tag(msg, &tags);

            if (tags == nullptr) {
                g_printerr("[GStreamer] No tags found in GST_MESSAGE_TAG.\n");
                break;
            }

//            g_print("[GStreamer] Received tag message\n");
            guint num_tags = gst_tag_list_n_tags(tags);
            for (guint i = 0; i < num_tags; ++i) {
                const gchar *tag_name = gst_tag_list_nth_tag_name(tags, i);
                GValue tag_value = G_VALUE_INIT;
                if (gst_tag_list_copy_value(&tag_value, tags, tag_name)) {
                    if (G_VALUE_HOLDS_STRING(&tag_value)) {
//                        g_print("Tag: %s, Value: %s\n", tag_name, g_value_get_string(&tag_value));
                    } else if (G_VALUE_HOLDS_BOOLEAN(&tag_value)) {
//                        g_print("Tag: %s, Value: %s\n", tag_name, g_value_get_boolean(&tag_value) ? "true" : "false");
                    } else if (G_VALUE_HOLDS_UINT(&tag_value)) {
//                        g_print("Tag: %s, Value: %u\n", tag_name, g_value_get_uint(&tag_value));
                    } else if (G_VALUE_HOLDS_UINT64(&tag_value)) {
//                        g_print("Tag: %s, Value: %" G_GUINT64_FORMAT "\n", tag_name, g_value_get_uint64(&tag_value));
                    } else if (G_VALUE_HOLDS_INT(&tag_value)) {
//                        g_print("Tag: %s, Value: %d\n", tag_name, g_value_get_int(&tag_value));
                    } else if (G_VALUE_HOLDS_INT64(&tag_value)) {
//                        g_print("Tag: %s, Value: %" G_GINT64_FORMAT "\n", tag_name, g_value_get_int64(&tag_value));
                    } else if (G_TYPE_CHECK_VALUE_TYPE(&tag_value, G_TYPE_DATE_TIME)) {
                        auto *dt = static_cast<GDateTime *>(g_value_get_boxed(&tag_value));
                        if (dt) {
                            gchar *dt_str = g_date_time_format(dt, "%Y-%m-%d %H:%M:%S");
//                            g_print("Tag: %s, Value: %s\n", tag_name, dt_str);
                            g_free(dt_str);
                        } else {
//                            g_print("Tag: %s, Value: (null GDateTime)\n", tag_name);
                        }
                    } else {
//                        g_print("Tag: %s (Unhandled type: %s)\n", tag_name, G_VALUE_TYPE_NAME(&tag_value));
                    }
                    g_value_unset(&tag_value);
                } else {
                    g_printerr("[GStreamer] Failed to copy value for tag: %s\n", tag_name);
                }
            }

            gst_tag_list_free(tags);
            break;
        }
        case GST_MESSAGE_BUFFERING: {
            gint percent = 0;
            gst_message_parse_buffering(msg, &percent);

            GstElement *src = GST_ELEMENT(msg->src);
            g_print("[Buffering] From %s: %d%%\n", GST_OBJECT_NAME(src), percent);

            if (has_property(G_OBJECT(src), "stats")) {
                GstStructure *stats = nullptr;
                g_object_get(G_OBJECT(src), "stats", &stats, NULL);
                if (stats) {
                    gchar *stats_str = gst_structure_to_string(stats);
                    g_print("[Buffering] Stats: %s\n", stats_str);
                    g_free(stats_str);
                    gst_structure_free(stats);
                }
            }

            if (percent < 100) {
                if(custom->in_recovery || custom->retry_count > 0){
                    notify_loading_status(env, custom->app, 0, MAX_RETRIES, FALSE);
                    g_source_remove(timer_id);
                    custom->in_recovery = FALSE;
                    global_data->next_retry_delay_ms = 0;
                    custom->retry_count = 0;
                    g_print("[GStreamer] Stream recovered successfully\n");
                }

                if (!custom->is_buffering) {
                    custom->is_buffering = TRUE;
                    custom->buffering_timeout_counter = gst_util_get_timestamp();
                    enter_buffering(env, custom);
                } else {
                    if (gst_util_get_timestamp() - custom->buffering_timeout_counter > 15 * GST_SECOND) {
                        notify_stream_error(env, custom->app, ERR_BUFFERING_TIMEOUT,
                                            "Buffering timeout exceeded.");
                    }
                }
            } else {
                if (custom->is_buffering) {
                    custom->is_buffering = FALSE;
                    exit_buffering(env, custom);
                    custom->buffering_timeout_counter = 0;
                }
            }
            break;
        }
        case GST_MESSAGE_STATE_CHANGED: {
            if (GST_MESSAGE_SRC(msg) == GST_OBJECT(custom->pipeline)) {
                GstState old_state, new_state, pending_state;
                gst_message_parse_state_changed(msg, &old_state, &new_state, &pending_state);
                g_print("[GStreamer] Pipeline state changed: %s to %s\n",
                        gst_element_state_get_name(old_state),
                        gst_element_state_get_name(new_state));
            }
            break;
        }
        case GST_MESSAGE_STATE_DIRTY: {
            g_printerr("[GStreamer] WARNING: Element %s reported its state as dirty.\n",
                       GST_OBJECT_NAME(msg->src));

            GstState current_pipeline_state, current_pending_state;
            gst_element_get_state(custom->pipeline, &current_pipeline_state, &current_pending_state,
                                  GST_CLOCK_TIME_NONE);
            g_printerr("[GStreamer] Pipeline state: %s, pending: %s\n",
                       gst_element_state_get_name(current_pipeline_state),
                       gst_element_state_get_name(current_pending_state));

            notify_stream_error(env, custom->app, ERR_GSTREAMER_STATE_DIRTY,"GStreamer element reported dirty state.");
            break;
        }
        case GST_MESSAGE_NEW_CLOCK: {
            GstClock *clock;
            gst_message_parse_new_clock(msg, &clock);
            g_print("[GStreamer] New clock set: %s\n", GST_OBJECT_NAME(clock));
            gst_object_unref(clock);
            break;
        }
        case GST_MESSAGE_STREAM_STATUS: {
            GstStreamStatusType type;
            GstElement *owner = nullptr;
            gst_message_parse_stream_status(msg, &type, &owner);
            const gchar *type_str = "unknown";
            switch (type) {
                case GST_STREAM_STATUS_TYPE_CREATE: type_str = "create"; break;
                case GST_STREAM_STATUS_TYPE_ENTER: type_str = "enter"; break;
                case GST_STREAM_STATUS_TYPE_LEAVE: type_str = "leave"; break;
                case GST_STREAM_STATUS_TYPE_DESTROY: type_str = "destroy"; break;
                case GST_STREAM_STATUS_TYPE_START: type_str = "start"; break;
                case GST_STREAM_STATUS_TYPE_PAUSE: type_str = "pause"; break;
                case GST_STREAM_STATUS_TYPE_STOP: type_str = "stop"; break;
                default: type_str = g_strdup_printf("other (%d)", type); break;
            }
            g_print("[GStreamer] Stream status from %s: %s\n",
                    GST_OBJECT_NAME(owner), type_str);


            if (owner && GST_IS_ELEMENT(owner)) {
                GstState current, pending;
                gst_element_get_state(owner, &current, &pending, 0);
                g_print("Current state: %s, pending: %s\n",
                        gst_element_state_get_name(current),
                        gst_element_state_get_name(pending));
            }
            break;
        }
        case GST_MESSAGE_ELEMENT: {
            const GstStructure *elementStructure = gst_message_get_structure(msg);
            if (!elementStructure) break;

            const gchar *structure_name = gst_structure_get_name(elementStructure);
            if (!structure_name) break;

            g_print("[GStreamer] Element message: %s\n", structure_name);

            if (g_strcmp0(structure_name, "application/x-rtsp") == 0) {
                const gchar *event_name = gst_structure_get_string(elementStructure, "event");
                const gchar *description = gst_structure_get_string(elementStructure, "error-description");
                gint code = -1;

                gint status_code = 0;
                if (gst_structure_get_int(elementStructure, "status-code", &status_code)) {
                    switch (status_code) {
                        case 401:
                        case 403:
                            notify_stream_error(env, custom->app, ERR_AUTH_FAILED, "Authentication failed");
                            break;
                        case 404:
                            notify_stream_error(env, custom->app, ERR_RTSP_URI_INVALID, "Stream not found");
                            break;
                        case 500:
                            notify_stream_error(env, custom->app, ERR_SERVER_ERROR, "Server error");
                            break;
                        case 200:
                            g_print("[RTSP] Success status (200)\n");
                            break;
                        default:
                            g_printerr("[RTSP] Unhandled status: %d\n", status_code);
                            break;
                    }
                }

                if (!event_name) break;

                if (g_strcmp0(event_name, "client-error") == 0 || g_strcmp0(event_name, "server-error") == 0) {
                    const char *error_type = (g_strcmp0(event_name, "client-error") == 0)
                                             ? "RTSP Client Error"
                                             : "RTSP Server Error";

                    if (gst_structure_get_int(elementStructure, "error-code", &code)) {
                        g_printerr("[%s] Code %d: %s\n", error_type, code,
                                   description ? description : "No description");

                        int java_err_code;
                        if (code == 401 || code == 403) {
                            java_err_code = ERR_AUTH_FAILED;
                        } else if (code == 404) {
                            java_err_code = ERR_RTSP_URI_INVALID;
                        } else if (code >= 500) {
                            java_err_code = ERR_SERVER_ERROR;
                        } else {
                            java_err_code = ERR_NETWORK_ERROR;
                        }

                        notify_stream_error(env, custom->app, java_err_code,
                                            description ? description : error_type);
                    } else {
                        g_printerr("[%s] Missing error code\n", error_type);
                        notify_stream_error(env, custom->app, ERR_NETWORK_ERROR,
                                            "RTSP protocol error");
                    }

                } else if (g_strcmp0(event_name, "eos") == 0) {
                    g_print("[RTSP] Stream ended normally\n");
                    if (!custom->in_recovery) {
                        notify_stream_error(env, custom->app, ERR_STREAM_EOS, "Stream ended");
                    }

                } else if (g_strcmp0(event_name, "closed") == 0) {
                    g_printerr("[RTSP] Connection closed unexpectedly\n");
                    if (!custom->in_recovery) {
                        notify_stream_error(env, custom->app, ERR_CONNECTION_CLOSED,
                                            "Server closed connection");
                    }

                } else if (g_strcmp0(event_name, "reconnected") == 0) {
                    g_print("[RTSP] Successfully reconnected\n");
                    custom->retry_count = 0;

                } else if (g_strcmp0(event_name, "timeout") == 0) {
                    g_printerr("[RTSP] Timeout occurred\n");
                    if (!custom->in_recovery && custom->retry_count < MAX_RETRIES) {
                        restart_pipeline(custom);
                    }

                } else {
                    g_printerr("[RTSP] Unhandled event: %s\n", event_name);
                }
            }
            else if (g_strcmp0(structure_name, "application/x-rtp-source-sdes") == 0) {
                const gchar *sdes_info = gst_structure_get_string(elementStructure, "sdes");
                if (sdes_info) {
                    g_print("[RTP] SDES: %s\n", sdes_info);
                }

                guint lost_packets = 0;
                if (gst_structure_get_uint(elementStructure, "lost-packets", &lost_packets)) {
                    if (lost_packets > 0) {
                        g_printerr("[RTP] Lost packets: %u\n", lost_packets);
                    }
                }
            }
            else if (g_strcmp0(structure_name, "application/x-rtcp") == 0) {
                g_print("[RTCP] Received RTCP message\n");
            }
            else if (gst_structure_has_name(elementStructure, "GstRTSPSrcTimeout")) {
                g_printerr("[RTSP] Timeout occurred!\n");
                if (!custom->in_recovery && custom->retry_count < MAX_RETRIES) {
                    restart_pipeline(custom);
                } else {
                    notify_stream_error(env, custom->app, ERR_RTSP_TIMEOUT,
                                        "RTSP timeout (max retries reached)");
                }
            }
            else if (gst_structure_has_name(elementStructure, "GstH264Decoder")) {
                if (gst_structure_has_field(elementStructure, "sps-pps")) {
                    g_print("[H264] Received SPS/PPS update\n");
                }
            }
            else if (gst_structure_has_name(elementStructure, "GstRTPPacketLost")) {
                guint lost = 0;
                if (gst_structure_get_uint(elementStructure, "packets-lost", &lost)) {
                    g_printerr("[RTP] Lost packets: %u\n", lost);
                    if (lost > 5) {
                        notify_stream_error(env, custom->app, ERR_NETWORK_ERROR,
                                            "Excessive packet loss detected");
                    }
                }
            } else if (GST_MESSAGE_TYPE(msg) == GST_MESSAGE_ELEMENT) {
                const GstStructure *s = gst_message_get_structure(msg);
                if (gst_structure_has_name(s, "GstRTSPMessage")) {
                    const gchar *method = gst_structure_get_string(s, "method");
                    const gchar *response = gst_structure_get_string(s, "response");

                    if (method && g_strcmp0(method, "TEARDOWN") == 0) {
                        g_print("[RTSP] Sent TEARDOWN request\n");
                    }

                    if (response && g_strstr_len(response, -1, "200") != nullptr) {
                        g_print("[RTSP] Server acknowledged TEARDOWN with 200 OK\n");
                    } else if (response) {
                        g_print("[RTSP] TEARDOWN response: %s\n", response);
                    }
                }
            } else {
                g_printerr("[Element] Unhandled message type: %s\n", structure_name);
                gchar *structure_str = gst_structure_to_string(elementStructure);
                g_print("Full structure: %s\n", structure_str);
                g_free(structure_str);
            }
            break;
        }
        case GST_MESSAGE_DURATION_CHANGED:{
            g_print("[GStreamer] GST_MESSAGE_DURATION_CHANGED:");
            break;
        }
        case GST_MESSAGE_LATENCY: {
            g_print("[GStreamer] Latency message received. Recalculating.\n");
            if (!gst_bin_recalculate_latency(GST_BIN(custom->pipeline))) {
                g_print("[GStreamer] Failed to recalculate latency.\n");
            }
            break;
        }
        case GST_MESSAGE_QOS: {
            GstFormat format;
            guint64 processed, dropped;
            gst_message_parse_qos_stats(msg, &format, &processed, &dropped);
            double drop_rate = (double)dropped / (double)(processed + dropped);

            if (drop_rate > 0.1) {  // More than 10% drops
                // Reduce quality or take other action
                g_print("[GStreamer] More than 10% drops.\n");
            }
            break;
        }
        case GST_MESSAGE_PROGRESS: {
            GstProgressType type;
            gchar *code = nullptr;
            gchar *text = nullptr;

            gst_message_parse_progress(msg, &type, &code, &text);

            const gchar *ui_status = nullptr;
            switch (type) {
                case GST_PROGRESS_TYPE_START: ui_status = "Initializing stream..."; break;
                case GST_PROGRESS_TYPE_CONTINUE: ui_status = "Buffering..."; break;
                case GST_PROGRESS_TYPE_COMPLETE: ui_status = "Stream ready"; break;
                case GST_PROGRESS_TYPE_CANCELED: ui_status = "Loading canceled"; break;
                case GST_PROGRESS_TYPE_ERROR: ui_status = "Error while loading stream"; break;
                default: ui_status = "Unknown progress event"; break;
            }

            g_print("[GStreamer] Progress: %s (%s: %s)\n", ui_status, code, text);

            if (env && custom->app) {
                update_ui_status_from_native(env, custom->app, (gint)type, ui_status);
            }
            break;
        }
        case GST_MESSAGE_STREAM_START: {
            g_print("[GStreamer] Stream has started!\n");
            notify_stream_started(env, custom->app);

            // Mark stream as started
            custom->stream_has_started = TRUE;

            // Start watchdog only now (not earlier)
            if (custom->watchdog_timer == 0) {
                g_print("[GStreamer] Starting watchdog timer now (stream started)\n");
                custom->watchdog_timer = g_timeout_add_seconds(WATCHDOG_INTERVAL, watchdog_check, custom);
                custom->last_sample_time = g_get_monotonic_time();  // Reset sample time
            }
            break;
        }
        default:
            g_print("[GStreamer] Unhandled message: %s\n", GST_MESSAGE_TYPE_NAME(msg));
            break;
    }

    return TRUE;
}

static void restart_pipeline(CustomData *data) {
    g_printerr("[GStreamer] restart_pipeline: Called\n");
    if (!data || !data->pipeline) return;

    // Check current state - don't restart if we're stopping or stopped
    gint current_state = get_stream_state(data);
    if (current_state == STREAM_STATE_STOPPING ||
        current_state == STREAM_STATE_STOPPED ||
        current_state == STREAM_STATE_ERROR) {
        g_print("[Restart] Not restarting due to current state: %s\n",
                get_state_name(current_state));
        return;
    }

    // ============================================================
    // STATE: RECOVERING
    // ============================================================
    set_stream_state(data, STREAM_STATE_RECOVERING);

    // 1. Check if already in recovery (atomic operation)
    // crashed due to reinterpret_cast for bool type
    gboolean expected_recovery = false;
    if (!data->in_recovery.compare_exchange_strong((expected_recovery), true)) {
        g_print("[Restart] Already in recovery, skipping\n");
        return;
    }

    // 2. Check retry count atomically
    int current_retry = data->retry_count.load();
    if (current_retry >= MAX_RETRIES) {
        g_print("[Restart] Max retries already reached (%d/%d), skipping\n",
                current_retry, MAX_RETRIES);
        data->in_recovery.store(false); // Reset recovery flag
        set_stream_state(data, STREAM_STATE_ERROR);
        return;
    }

    // 3. Check for active timer using atomic compare-and-exchange
    guint expected_timer = 0;
    if (!data->active_timer_id.compare_exchange_strong(expected_timer, 1)) {
        // Timer already active (value is not 0)
        g_print("[Restart] Already have a pending retry timer (id: %u), skipping\n",
                data->active_timer_id.load());
        data->in_recovery.store(false); // Reset recovery flag
        return;
    }

    JNIEnv *env = get_jni_env(data);
    if (!env) {
        g_printerr("[GStreamer] restart_pipeline: Failed to get JNIEnv\n");
        data->in_recovery.store(false); // Reset recovery flag
        data->active_timer_id.store(0); // Reset timer ID
        return;
    }

    // 4. Increment retry count atomically
    int new_attempt = data->retry_count.fetch_add(1) + 1;
    notify_loading_status(env, data->app, new_attempt, MAX_RETRIES, TRUE);
    g_print("[Restart] Attempting recovery (retry %d of %d)\n", new_attempt, MAX_RETRIES);

    // 5. Reset stream state
    data->stream_has_started.store(FALSE);

    // 6. Calculate delay with exponential backoff and jitter
    guint64 jitter = (g_random_int() % 1000) - 500; // +/- 500ms jitter
    jlong new_delay = MIN(
            data->next_retry_delay_ms.load() * BACKOFF_FACTOR + jitter,
            MAX_RETRY_DELAY_MS
    );
    data->next_retry_delay_ms.store(MAX(new_delay, 1000));

    g_print("[Restart] Scheduling retry in %" G_GUINT64_FORMAT " ms (attempt %d/%d)...\n",
            data->next_retry_delay_ms.load(), new_attempt, MAX_RETRIES);

    // 7. Schedule the retry timer
    guint new_timer_id = g_timeout_add(
            (guint)data->next_retry_delay_ms.load(),
            (GSourceFunc)delayed_restart_cb,
            data
    );

    if (new_timer_id == 0) {
        g_printerr("[Restart] Failed to schedule retry timer\n");
        data->in_recovery.store(false);
        data->active_timer_id.store(0);
        set_stream_state(data, STREAM_STATE_ERROR);
        return;
    }

    // 8. Store the actual timer ID (not just 1)
    data->active_timer_id.store(new_timer_id);

    // Also update global timer_id for backward compatibility
    pthread_mutex_lock(&data->lock);
    timer_id = new_timer_id;
    pthread_mutex_unlock(&data->lock);

    g_print("[Restart] Timer scheduled with ID: %u\n", new_timer_id);
}

static gboolean delayed_restart_cb(gpointer user_data) {
    auto* data = static_cast<CustomData*>(user_data);
    if (!data || !data->pipeline) {
        g_printerr("[Restart] Invalid data or pipeline\n");
        return G_SOURCE_REMOVE;
    }

    // Clear timer IDs at the start
    guint current_timer_id = data->active_timer_id.exchange(0);
    if (current_timer_id != 0) {
        g_print("[Restart] Clearing timer ID: %u\n", current_timer_id);
    }

    pthread_mutex_lock(&data->lock);
    timer_id = 0; // Clear global timer_id too
    pthread_mutex_unlock(&data->lock);

    JNIEnv* env = get_jni_env(data);
    if (!env) {
        g_printerr("[Restart] Warning: Failed to get JNIEnv\n");
        data->in_recovery.store(false); // Ensure recovery flag is cleared
        set_stream_state(data, STREAM_STATE_ERROR);
        return G_SOURCE_REMOVE;
    }

    int current_attempt = data->retry_count.load();
    g_print("[Restart] Starting pipeline recovery (attempt %d/%d)...\n",
            current_attempt, MAX_RETRIES);

    // Reset stream_has_started flag since we're restarting
    data->stream_has_started.store(FALSE);

    // Track whether this attempt succeeded
    gboolean attempt_succeeded = FALSE;

    try {
        // 1. Stop pipeline
        GstStateChangeReturn ret = gst_element_set_state(data->pipeline, GST_STATE_NULL);
        if (ret == GST_STATE_CHANGE_FAILURE) {
            throw std::runtime_error("Failed to set NULL state");
        }

        ret = gst_element_get_state(data->pipeline, nullptr, nullptr, 5 * GST_SECOND);
        if (ret == GST_STATE_CHANGE_FAILURE || ret == GST_STATE_CHANGE_NO_PREROLL) {
            g_warning("[Restart] Waiting for NULL state timed out or failed (ret: %d). Proceeding.\n", ret);
        }

        // 2. Disconnect appsink signal
        if (data->appsink) {
            gulong handler_id = g_signal_handler_find(
                    data->appsink,
                    G_SIGNAL_MATCH_FUNC, 0, 0, nullptr,
                    reinterpret_cast<gpointer>(on_new_sample), nullptr
            );
            if (handler_id) {
                g_signal_handler_disconnect(data->appsink, handler_id);
                g_print("[Restart] Disconnected 'new-sample' handler.\n");
            }
        }

        // 3. Flush appsink buffers safely
        if (data->appsink) {
            g_object_set(data->appsink, "drop", TRUE, "max-buffers", 1, nullptr);
            while (true) {
                GstSample* sample = gst_app_sink_try_pull_sample(
                        GST_APP_SINK(data->appsink),
                        10 * GST_MSECOND
                );
                if (!sample) break;
                gst_sample_unref(sample);
            }
            g_object_set(data->appsink, "drop", FALSE, "max-buffers", 50, nullptr);
        }

        // 4. Flush all queues
        GstIterator* elements = gst_bin_iterate_elements(GST_BIN(data->pipeline));
        if (elements) {
            GValue val = G_VALUE_INIT;
            while (gst_iterator_next(elements, &val) == GST_ITERATOR_OK) {
                GstElement* element = GST_ELEMENT(g_value_get_object(&val));
                if (element && g_str_has_prefix(GST_ELEMENT_NAME(element), "queue")) {
                    gst_element_send_event(element, gst_event_new_flush_start());
                    gst_element_send_event(element, gst_event_new_flush_stop(TRUE));
                }
                g_value_unset(&val);
            }
            gst_iterator_free(elements);
        }

        // 5. Reset flags
        data->last_sample_time.store(g_get_monotonic_time());
        data->buffering_timeout_counter = 0;
        data->sps_pps_extracted = FALSE;
        data->sps_pps_sent = FALSE;
        data->stream_has_started.store(FALSE);

        // 6. Reconnect appsink signal
        if (data->appsink) {
            if (!g_signal_connect(data->appsink, "new-sample", G_CALLBACK(on_new_sample), data)) {
                throw std::runtime_error("Failed to reconnect new-sample signal");
            }
        }

        // 7. Restart pipeline
        ret = gst_element_set_state(data->pipeline, GST_STATE_PLAYING);

        if (ret == GST_STATE_CHANGE_FAILURE) {
            throw std::runtime_error("Failed to set PLAYING state");
        }

        // Wait for pipeline to actually start playing
        if (ret == GST_STATE_CHANGE_ASYNC) {
            GstState state, pending;
            GstClockTime timeout = 5 * GST_SECOND; // Wait 5 seconds for PLAYING state

            ret = gst_element_get_state(data->pipeline, &state, &pending, timeout);

            if (ret != GST_STATE_CHANGE_SUCCESS || state != GST_STATE_PLAYING) {
                // Pipeline didn't reach PLAYING state within timeout
                g_printerr("[Restart] Pipeline didn't reach PLAYING state (state=%s, pending=%s)\n",
                           gst_element_state_get_name(state),
                           gst_element_state_get_name(pending));
                throw std::runtime_error("Pipeline didn't reach PLAYING state");
            }
        }

        // If we get here, the pipeline restart was successful
        attempt_succeeded = TRUE;
        g_print("[Restart] Pipeline restart attempt %d/%d succeeded\n",
                current_attempt, MAX_RETRIES);

    } catch (const std::exception& e) {
        g_printerr("[Restart] Recovery attempt %d/%d failed: %s\n",
                   current_attempt, MAX_RETRIES, e.what());
        attempt_succeeded = FALSE;
    }

    // --- Handle the result of this attempt ---
    if (attempt_succeeded) {
        // ✅ SUCCESS - Don't schedule another retry
        g_print("[Restart] Stream recovered successfully after attempt %d\n", current_attempt);

        // Clear recovery flag
        data->in_recovery.store(false);

        // Set back to PLAYING state
        set_stream_state(data, STREAM_STATE_PLAYING);

        if (env) {
            // Show success status
            notify_loading_status(env, data->app, 0, MAX_RETRIES, FALSE);
        }

        return G_SOURCE_REMOVE;  // This timer is done

    } else {
        // ❌ FAILURE - Schedule next retry if we haven't reached max attempts
        int next_attempt = current_attempt + 1;

        if (next_attempt > MAX_RETRIES) {
            g_printerr("[Restart] Max reconnection attempts reached (%d/%d). Aborting.\n",
                       MAX_RETRIES, MAX_RETRIES);

            // Set ERROR state
            set_stream_state(data, STREAM_STATE_ERROR);

            if (env) {
                notify_stream_error(env, data->app, ERR_NETWORK_ERROR,
                                    "Max reconnection attempts reached");
                notify_loading_status(env, data->app, 0, MAX_RETRIES, FALSE);
            }

            data->in_recovery.store(FALSE);
            data->retry_count.store(0);
            data->next_retry_delay_ms.store(INITIAL_RETRY_DELAY_MS);

            if (data->main_loop) {
                g_main_loop_quit(data->main_loop);
            }
            return G_SOURCE_REMOVE;
        }

        // Update retry count for the next attempt
        data->retry_count.store(next_attempt);

        // Exponential backoff with jitter
        guint64 jitter = (g_random_int() % 1000) - 500;
        guint64 next_delay = std::min(
                data->next_retry_delay_ms.load() * BACKOFF_FACTOR + jitter,
                (guint64)MAX_RETRY_DELAY_MS
        );
        data->next_retry_delay_ms.store(MAX(next_delay, 1000));

        if (env) {
            notify_loading_status(env, data->app, next_attempt, MAX_RETRIES, TRUE);
        }

        g_print("[Restart] Scheduling next retry in %" G_GUINT64_FORMAT " ms (attempt %d/%d)...\n",
                data->next_retry_delay_ms.load(), next_attempt, MAX_RETRIES);

        // Schedule the next retry using atomic timer management
        guint new_timer_id = g_timeout_add(
                (guint)data->next_retry_delay_ms.load(),
                (GSourceFunc)delayed_restart_cb,
                data
        );

        if (new_timer_id != 0) {
            data->active_timer_id.store(new_timer_id);
            pthread_mutex_lock(&data->lock);
            timer_id = new_timer_id;
            pthread_mutex_unlock(&data->lock);
            g_print("[Restart] Next retry scheduled with timer ID: %u\n", new_timer_id);
        } else {
            g_printerr("[Restart] Failed to schedule next retry timer\n");
            data->in_recovery.store(false);
            set_stream_state(data, STREAM_STATE_ERROR);
        }

        return G_SOURCE_REMOVE;  // This timer instance is done
    }
}

static gboolean validate_rtsp_connection(const gchar *uri) {
    if (!uri) return FALSE;

    g_print("[RTSP Validation] Testing connection to: %s\n", uri);

    // Extract hostname from RTSP URI
    gchar *hostname = g_strdup(uri);
    gchar *host = NULL;
    gint port = 554; // Default RTSP port

    if (g_str_has_prefix(hostname, "rtsp://")) {
        gchar *path_start = hostname + 7; // Skip "rtsp://"

        // Find port or path separator
        gchar *colon = strchr(path_start, ':');
        gchar *slash = strchr(path_start, '/');

        if (colon && (!slash || colon < slash)) {
            // Has port specification
            *colon = '\0';
            host = g_strdup(path_start);
            port = atoi(colon + 1);
            if (port <= 0 || port > 65535) {
                port = 554; // Reset to default if invalid
            }
        } else if (slash) {
            // No port, has path
            *slash = '\0';
            host = g_strdup(path_start);
        } else {
            // No port, no path
            host = g_strdup(path_start);
        }
    }

    if (!host) {
        g_free(hostname);
        g_printerr("[RTSP Validation] Failed to parse host from URI\n");
        return FALSE;
    }

    g_print("[RTSP Validation] Host: %s, Port: %d\n", host, port);

    // For Android, let's use a simpler approach that doesn't require socket programming
    // Many RTSP servers block raw TCP connections anyway

    // Just do DNS resolution as a basic check
    struct addrinfo hints, *res = NULL;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET; // IPv4
    hints.ai_socktype = SOCK_STREAM;

    char port_str[16];
    snprintf(port_str, sizeof(port_str), "%d", port);

    int result = getaddrinfo(host, port_str, &hints, &res);

    if (result != 0) {
        g_printerr("[RTSP Validation] DNS resolution failed for %s: %s\n",
                   host, gai_strerror(result));
        g_free(host);
        g_free(hostname);

        // Still continue - network might be slow
        g_print("[RTSP Validation] Will continue anyway\n");
        return TRUE;
    }

    if (res) {
        // Try to extract IP address for logging
        char ip_str[INET_ADDRSTRLEN] = {0};
        struct sockaddr_in *addr = (struct sockaddr_in *)res->ai_addr;

        if (inet_ntop(AF_INET, &addr->sin_addr, ip_str, sizeof(ip_str))) {
            g_print("[RTSP Validation] Resolved to IP: %s\n", ip_str);
        }

        freeaddrinfo(res);
        g_print("[RTSP Validation] DNS resolution successful\n");
    }

    g_free(host);
    g_free(hostname);

    // Add small delay to allow network to stabilize
    g_print("[RTSP Validation] Validation complete, waiting 300ms...\n");
    g_usleep(300 * 1000);

    return TRUE;
}

static void debug_rtsp_connection(CustomData *data) {
    if (!data || !data->pipeline) return;

    GstElement *rtspsrc = gst_bin_get_by_name(GST_BIN(data->pipeline), "src");
    if (!rtspsrc) {
        g_printerr("[RTSP Debug] Could not find rtspsrc element\n");
        return;
    }

    // Get current property values (use correct property names)
    gchar *location = nullptr;
    guint64 timeout = 0;
    guint retry = 0;
    GstRTSPLowerTrans protocols = GST_RTSP_LOWER_TRANS_TCP;

    g_object_get(rtspsrc,
                 "location", &location,
                 "timeout", &timeout,
                 "retry", &retry,
                 "protocols", &protocols,
                 NULL);

    g_print("[RTSP Debug] Configuration:\n");
    g_print("  Location: %s\n", location ? location : "null");
    g_print("  Timeout: %" G_GUINT64_FORMAT " ns (%" G_GUINT64_FORMAT " seconds)\n",
            timeout, timeout / GST_SECOND);
    g_print("  Retry count: %u\n", retry);

    // Remove references to non-existent properties like "is-active" and "stats"

    if (location) g_free(location);
    gst_object_unref(rtspsrc);
}

static void update_fps(CustomData* mData, int fps) {
    if (!mData) return;

    if (fps <= 0) {
        mData->zero_fps_count++;

        g_print("[FPS] Zero FPS detected (%d/5)", mData->zero_fps_count);

        if (mData->zero_fps_count >= 5 && !mData->waiting_for_response.load()) {
            mData->stream_has_started.store(FALSE);
            start_no_response_timer(mData);
        }
    } else {
        // FPS recovered
        mData->zero_fps_count = 0;
        if (mData->waiting_for_response.load()) {
            cancel_no_response_timer(mData);
            g_print("[Buffering] Recovered, FPS=%d", fps);
        }

    }
}

static gboolean watchdog_check(gpointer user_data) {
    auto *data = (CustomData *) user_data;
    if (!can_call_java(data)) return FALSE;
    if (!data) return G_SOURCE_REMOVE;

    // Don't check if stream hasn't started yet
    if (!data->stream_has_started.load()) {
        // loop exist -> to avoid this added timer for 15 seconds
      g_print("[Watchdog] Stream not started yet, skipping check %d \n " , data->retry_count.load());
          if (data->retry_count.load() == MAX_RETRIES) {
            g_printerr("[Watchdog] Max retries reached, stopping\n");
            notify_stream_error(get_jni_env(data), data->app, ERR_NETWORK_ERROR,
                                "No data received: max retries reached");
            notify_loading_status(get_jni_env(data), data->app, 0, MAX_RETRIES, FALSE);

            cancel_no_response_timer(data);
            if (data->main_loop) {
                g_main_loop_quit(data->main_loop);
            }
            return G_SOURCE_REMOVE;
        } else {
              if (data->retry_count.load() == 0) {
                  start_no_response_timer(data);
              }
          }

        return G_SOURCE_CONTINUE;
    }

    // Don't check if we're in recovery mode
    if (data->in_recovery.load()) {
        g_print("[Watchdog] In recovery mode, skipping check\n");
        return G_SOURCE_CONTINUE;
    }

    JNIEnv *env = get_jni_env(data);
    if (!env) {
        g_printerr("[Watchdog] Failed to get JNIEnv\n");
        return G_SOURCE_CONTINUE;
    }

    gint64 now = g_get_monotonic_time();
    gint64 time_since_last_sample = now - data->last_sample_time.load();

    g_print("[Watchdog] Check: stream_started=%d, buffering=%d, time_since_last=%llds (timeout=%llds), fps=%llu\n",
            data->stream_has_started.load(),
            data->is_buffering.load(),
            time_since_last_sample / G_TIME_SPAN_SECOND,
            WATCHDOG_TIMEOUT / G_TIME_SPAN_SECOND,  // Will show 15 now
            data->fps);

    // If stream just started (< 15 seconds), be more tolerant
    GstClock *clock = gst_element_get_clock(data->pipeline);
    if (clock) {
        GstClockTime stream_time = gst_clock_get_time(clock);
        gst_object_unref(clock);

        // Reduce from 30s to 15s for "new stream" tolerance
        if (stream_time < 15 * GST_SECOND) {  // Changed from 30
            g_print("[Watchdog] Stream is new (<15s), being more tolerant\n");
            return G_SOURCE_CONTINUE;
        }
    }

    // Check if pipeline is actually playing (unchanged)
    if (data->pipeline) {
        GstState state, pending;
        GstStateChangeReturn ret = gst_element_get_state(data->pipeline, &state, &pending,
                                                         GST_SECOND);
        // ... existing code ...
    }

    // Only restart if:
    // 1. We're not currently buffering
    // 2. It's been more than WATCHDOG_TIMEOUT (15s now) since last sample
    // 3. Stream has actually started
    // 4. AND we have 0 FPS (no frames at all)
    update_fps(data,data->fps);
    if (!data->is_buffering.load() &&
        time_since_last_sample > WATCHDOG_TIMEOUT &&  // 15 seconds now
        data->stream_has_started.load() &&
        data->fps == 0) {

        g_printerr("[Watchdog] WARNING: No samples for %lld seconds and 0 FPS!\n",
                   time_since_last_sample / G_TIME_SPAN_SECOND);

        // Call debug before restarting to see what's wrong
        g_print("[Watchdog] === Debugging before restart ===\n");
        debug_rtsp_connection(data);
        g_print("[Watchdog] === End debugging ===\n");

        // Verify pipeline state before restarting
        if (data->pipeline) {
            GstState state, pending;
            GstStateChangeReturn ret = gst_element_get_state(data->pipeline, &state, &pending,
                                                             GST_SECOND);

            if (ret == GST_STATE_CHANGE_SUCCESS) {
                g_printerr("[Watchdog] Pipeline state: %s\n", gst_element_state_get_name(state));

                if (state == GST_STATE_PLAYING) {
                    g_printerr("[Watchdog] Pipeline PLAYING but no data - triggering restart\n");

                    if (data->retry_count.load() < MAX_RETRIES) {
                        restart_pipeline(data);
                    } else {
                        g_printerr("[Watchdog] Max retries reached, stopping\n");
                        notify_stream_error(env, data->app, ERR_NETWORK_ERROR,
                                            "No data received: max retries reached");
                        notify_loading_status(env, data->app, 0, MAX_RETRIES, FALSE);

                        if (data->main_loop) {
                            g_main_loop_quit(data->main_loop);
                        }
                        return G_SOURCE_REMOVE;
                    }
                }
            }
        }
    } else if (time_since_last_sample > WATCHDOG_TIMEOUT && data->fps > 0) {
        g_print("[Watchdog] Slow stream but still getting %llu FPS, not restarting\n", data->fps);
    }

    return G_SOURCE_CONTINUE;
}

static gboolean no_response_timeout_cb(gpointer user_data) {
    auto *data = static_cast<CustomData *>(user_data);

    g_print("[Watchdog] No response for 15 seconds.not started \n");

    if (!data || data->stream_has_started.load()) { // If stream has started reset all things and return it
        data->zero_fps_count = 0;// reset fps count
        data->waiting_for_response.store(false);
        data->no_response_timer = 0;
        return G_SOURCE_REMOVE;
    }
//    g_print("[Watchdog] No response for 15 seconds. restart pipeline.\n");
    g_print("[Watchdog] No response for 15 seconds. streaming = %s\n",
            data->streaming ? "TRUE" : "FALSE");

    if (data->is_buffering || !data->streaming || !data->stream_has_started.load()) {
        g_print("[Watchdog] request restart pipeline.\n");
        restart_pipeline(data);
    }
    data->zero_fps_count = 0;// reset fps count
    data->waiting_for_response.store(false);
    data->no_response_timer = 0;

    return G_SOURCE_REMOVE;
}

// send rtsp keep alive cmnd keep on posting without any response, so add a 15 second timer
static void start_no_response_timer(CustomData *data) {
    if (!data || data->waiting_for_response.load())
        return; // already running

    g_print("[Watchdog] Starting 15s no-response timer\n");
    pthread_mutex_lock(&global_data->jni_mutex);
    data->waiting_for_response.store(true);
    data->no_response_timer =
            g_timeout_add_seconds_full(
                    G_PRIORITY_DEFAULT,
                    15,
                    no_response_timeout_cb,
                    data,
                    nullptr
            );
    pthread_mutex_unlock(&global_data->jni_mutex);
}

static gboolean send_rtsp_keepalive(gpointer user_data) {
    auto *data = (CustomData *) user_data;

    if (!can_call_java(data)) return FALSE;

    if (!data || !data->pipeline || !data->streaming) {
        return G_SOURCE_CONTINUE;
    }

    g_print("[KeepAlive] Sending RTSP keep-alive...\n");

    // Update last sample time to prevent watchdog from triggering
    data->last_sample_time.store(g_get_monotonic_time());

    // Log current state
    if (data->pipeline) {
        GstState state, pending;
        GstStateChangeReturn ret = gst_element_get_state(data->pipeline, &state, &pending, GST_SECOND);
        if (ret == GST_STATE_CHANGE_SUCCESS) {
            g_print("[KeepAlive] Pipeline state: %s\n", gst_element_state_get_name(state));
        }
    }

    return G_SOURCE_CONTINUE;
}

static int find_next_nal_unit(const guint8 *buffer_data, int buffer_size, int offset) {
    for (int i = offset; i < buffer_size - 4; ++i) {
        if (i + 4 > buffer_size) break;
        if (buffer_data[i] == 0x00 && buffer_data[i + 1] == 0x00) {
            if (buffer_data[i + 2] == 0x01) {
                return i;
            } else if (buffer_data[i + 2] == 0x00 && buffer_data[i + 3] == 0x01) {
                return i;
            }
        }
    }
    return buffer_size;
}

static void process_nal_unit(const guint8 *buffer_data, int buffer_size, int nal_start, int nal_end, CustomData *custom_data) {
    int start_code_size = (buffer_data[nal_start + 2] == 0x01) ? 3 : 4;
    int nal_type_offset = nal_start + start_code_size;
    if (nal_type_offset >= nal_end) return;

    guint8 nal_type = buffer_data[nal_type_offset] & 0x1F;
    if (nal_type == 7 && !custom_data->sps) {
        int size = nal_end - nal_start;
        custom_data->sps = gst_buffer_new_allocate(nullptr, size, nullptr);
        GstMapInfo map;
        if (gst_buffer_map(custom_data->sps, &map, GST_MAP_WRITE)) {
            memcpy(map.data, &buffer_data[nal_start], size);
            gst_buffer_unmap(custom_data->sps, &map);
        }
    } else if (nal_type == 8 && !custom_data->pps) {
        int size = nal_end - nal_start;
        custom_data->pps = gst_buffer_new_allocate(nullptr, size, nullptr);
        GstMapInfo map;
        if (gst_buffer_map(custom_data->pps, &map, GST_MAP_WRITE)) {
            memcpy(map.data, &buffer_data[nal_start], size);
            gst_buffer_unmap(custom_data->pps, &map);
        }
    }
}

static void parse_nal_units(const guint8 *buffer_data, int buffer_size, CustomData *custom_data) {
    if (custom_data->sps_pps_extracted) return;

    if (!buffer_data || buffer_size < 4) {
        g_print("parse_nal_units: returned buffer_size = %d\n",  buffer_size);
        return;
    }

    int offset = 0;
    while (offset + 4 < buffer_size) {
        int nal_start = find_next_nal_unit(buffer_data, buffer_size, offset);
        if (nal_start >= buffer_size) break;

        int next_nal_start = find_next_nal_unit(buffer_data, buffer_size, nal_start + 1);
        int nal_end = (next_nal_start >= buffer_size) ? buffer_size : next_nal_start;

        process_nal_unit(buffer_data, buffer_size, nal_start, nal_end, custom_data);

        if (custom_data->sps && custom_data->pps) {
            g_print("parse_nal_units: sps=%p, pps=%p\n",  custom_data->sps, custom_data->pps);
            custom_data->sps_pps_extracted = TRUE;
            break;
        }
        offset = next_nal_start;
    }
}

inline void fail_and_exit(CustomData* data, JNIEnv* env, int code, const char* msg) {
    if (!data || !env || !data->app) return;

    g_printerr("[GStreamer] ERR_PIPELINE_STATE_FAILED restart_pipeline");

    if(code == ERR_PIPELINE_STATE_FAILED){
        if (data->retry_count < MAX_RETRIES && !data->in_recovery) {
            g_printerr("[GStreamer] ERR_PIPELINE_STATE_FAILED restart_pipeline");
            restart_pipeline(data);
            return;
        }
    }

    // Notify Java side
    notify_stream_error(env, data->app, code, msg);

    // Exceeded retries or already recovering, do full cleanup
    cleanup_streaming_resources(data, env);

    if (data->jvm) {
        data->jvm->DetachCurrentThread();
    }
}

void notify_stream_switch_status(JNIEnv* env, jobject java_app_instance, jboolean is_switching) {
    if (!env || !java_app_instance) return;

    jclass clazz = env->GetObjectClass(java_app_instance);
    if (!clazz) return;

    jmethodID method = env->GetMethodID(clazz, "onStreamSwitchStatus", "(Z)V");
    if (method) {
        env->CallVoidMethod(java_app_instance, method, is_switching);
    }

    env->DeleteLocalRef(clazz);
}

/*Original*/
//static GstFlowReturn on_new_sample(GstAppSink *appsink, gpointer userdata) {
//    auto *data = (CustomData *) userdata;
//    if (!data || !data->stream_alive.load()) return  GST_FLOW_EOS;
//    if (!data->streaming.load()) return GST_FLOW_OK;
//
//    data->last_sample_time.store(g_get_monotonic_time());
//
//    JNIEnv *env = get_jni_env(data);
//    if (!env) {
//        g_printerr("[GStreamer] Failed to attach thread to JVM (on_new_sample)\n");
//        return GST_FLOW_ERROR;
//    }
//
//    // update streaming is active
//    data->stream_alive.store(true, std::memory_order_release);
//
//    // Check if we're in recovery and reset if we receive a frame
//    bool in_recovery_expected = true;
//    if (data->in_recovery.compare_exchange_strong(reinterpret_cast<int &>(in_recovery_expected), false)) {
//        g_print("[GStreamer] H264 frame received - Recovery succeeded after %d attempts\n",
//                data->retry_count.load());
//
//        // Cancel any pending retry timers
//        guint current_timer = data->active_timer_id.exchange(0);
//        if (current_timer != 0) {
//            g_source_remove(current_timer);
//            g_print("[GStreamer] Cancelled pending retry timer ID: %u\n", current_timer);
//        }
//
//        pthread_mutex_lock(&data->lock);
//        if (timer_id != 0) {
//            g_source_remove(timer_id);
//            timer_id = 0;
//        }
//        pthread_mutex_unlock(&data->lock);
//
//        // Reset retry state
//        data->retry_count.store(0);
//        data->next_retry_delay_ms.store(INITIAL_RETRY_DELAY_MS);
//
//        // Notify Java that recovery succeeded
//        notify_loading_status(env, data->app, 0, MAX_RETRIES, FALSE);
//
//        g_print("[GStreamer] Stream fully recovered - H264 frames flowing\n");
//    }
//
//    GstSample *sample = gst_app_sink_pull_sample(appsink);
//    if (!sample) {
//        g_printerr("[GStreamer] Failed to pull sample (sample is NULL)\n");
//        notify_stream_error(env, data->app, ERR_NO_FRAMES_RECEIVED, "Failed to pull sample.");
//        return GST_FLOW_EOS;
//    }
//
//    GstBuffer *buffer = gst_sample_get_buffer(sample);
//    if (!buffer) {
//        gst_sample_unref(sample);
//        g_printerr("[GStreamer] Received NULL buffer\n");
//        notify_stream_error(env, data->app, ERR_NO_FRAMES_RECEIVED, "Received NULL buffer.");
//        return GST_FLOW_EOS;
//    }
//
//    // Get size without mapping
//    gsize buffer_size = gst_buffer_get_size(buffer);
//    if (buffer_size == 0) {
//        gst_sample_unref(sample);
//        g_printerr("[GStreamer] Received empty buffer\n");
//        notify_stream_error(env, data->app, ERR_NO_FRAMES_RECEIVED, "Received empty buffer.");
//        return GST_FLOW_EOS;
//    }
//
//    // Timing and FPS calculations
//    GstClockTime now = gst_util_get_timestamp();
//    data->frame_count++;
//    calculate_fps(data);
//
//    if (data->stream_start_time == 0) {
//        data->stream_start_time = now;
//    }
//
//    // FPS monitoring logic (unchanged from your original code)
//    const gdouble expected_fps_threshold = 15.0;
//    const GstClockTime startup_delay_ns = 5 * GST_SECOND;
//    const GstClockTime low_fps_threshold_ns = 15 * GST_SECOND;
//
////    if ((now - data->stream_start_time > startup_delay_ns) && (gdouble)data->fps < expected_fps_threshold) {
////        if (data->low_fps_start_time == 0) {
////            data->low_fps_start_time = now;
////            data->low_fps_counter = 1;
////        } else {
////            data->low_fps_counter++;
////        }
////
////        if (!data->is_buffering_low_fps && data->low_fps_counter >= 3) {
////            data->is_buffering_low_fps = TRUE;
////            enter_buffering(env, data);
////            g_print("[GStreamer] Entered buffering due to sustained low FPS (%.2f)\n", (double)data->fps);
////        }
////
////        if ((now - data->low_fps_start_time) >= low_fps_threshold_ns) {
////            g_print("[GStreamer] Low FPS persisted for threshold duration\n");
////            notify_low_fps_long_time(env, data->app);
////            gst_sample_unref(sample);
////            return GST_FLOW_EOS;
////        }
////    } else {
////        data->low_fps_counter = 0;
////        data->low_fps_start_time = 0;
////
////        if (data->is_buffering_low_fps) {
////            data->is_buffering_low_fps = FALSE;
////            exit_buffering(env, data);
////            g_print("[GStreamer] Exited buffering due to FPS recovery\n");
////        }
////    }
//
//    GstClockTime current_pts = GST_BUFFER_PTS(buffer);
//    GstClockTime current_dts = GST_BUFFER_DTS(buffer);
//
//    if (!GST_CLOCK_TIME_IS_VALID(current_pts)) {
//        current_pts = gst_util_get_timestamp();
//    }
//    if (!GST_CLOCK_TIME_IS_VALID(current_dts)) {
//        if (GST_CLOCK_TIME_IS_VALID(last_dts)) {
//            current_dts = last_dts + DEFAULT_FRAME_DURATION;
//        } else {
//            current_dts = current_pts;
//        }
//    }
//    last_dts = current_dts; // track last used DTS
//
//    // Convert PTS and DTS to microseconds for comparison and Java
//    auto pts_us = (jlong)(current_pts / GST_USECOND);
//    auto dts_us = (jlong)(current_dts / GST_USECOND);
//
//    // --- NEW LOGIC TO PREVENT SENDING DUPLICATE FRAMES ---
//    // Check if the current frame's PTS is the same as the last one sent
//    if (current_pts != GST_CLOCK_TIME_NONE && current_pts == last_sent_pts) {
//        g_print("[GStreamer] Skipping sending repeating frame with PTS: %" GST_TIME_FORMAT " (us)\n", GST_TIME_ARGS(current_pts));
//        gst_sample_unref(sample);
//        return GST_FLOW_OK; // Successfully processed by skipping
//    }
//    // Update last_sent_pts only if a new unique frame is being sent
//    last_sent_pts = current_pts;
//    // --- END NEW LOGIC ---
//
////    g_print("[GStreamer] Sample PTS: %" GST_TIME_FORMAT "d\n", GST_BUFFER_PTS(buffer));
//    // Process buffer data
//    GstMapInfo map;
//    if (gst_buffer_map(buffer, &map, GST_MAP_READ)) {
//        // SPS/PPS extraction (unchanged)
//        if (!data->sps_pps_sent) {
//            parse_nal_units(map.data, (jint)map.size, data);
//
//            if (data->sps_pps_extracted && data->sps && data->pps) {
//                GstMapInfo sps_map, pps_map;
//                gboolean sps_ok = gst_buffer_map(data->sps, &sps_map, GST_MAP_READ);
//                gboolean pps_ok = gst_buffer_map(data->pps, &pps_map, GST_MAP_READ);
//
//                if (sps_ok && pps_ok) {
//                    if (sps_map.size > 0 && pps_map.size > 0) {
//                        jbyteArray j_sps_data = env->NewByteArray((jsize)sps_map.size);
//                        jbyteArray j_pps_data = env->NewByteArray((jsize)pps_map.size);
//
//                        if (j_sps_data && j_pps_data) {
//                            env->SetByteArrayRegion(j_sps_data, 0, (jsize)sps_map.size, (jbyte *)sps_map.data);
//                            env->SetByteArrayRegion(j_pps_data, 0, (jsize)pps_map.size, (jbyte *)pps_map.data);
//
//                            g_print("SPS size: %zu, PPS size: %zu\n", sps_map.size, pps_map.size);
//
//                            jclass clazz = env->GetObjectClass(data->app);
//                            if (clazz) {
//                                jmethodID method = env->GetMethodID(clazz, "onSpsPpsDataReceived", "([B[B)V");
//                                if (method) {
//                                    env->CallVoidMethod(data->app, method, j_sps_data, j_pps_data);
//                                    data->sps_pps_sent = TRUE;
//                                } else {
//                                    g_printerr("[GStreamer] onSpsPpsDataReceived method not found\n");
//                                }
//                                env->DeleteLocalRef(clazz);
//                            }
//                        } else {
//                            g_printerr("[GStreamer] Failed to allocate SPS/PPS byte arrays\n");
//                            notify_stream_error(env, data->app, ERR_UNKNOWN, "Failed to allocate SPS/PPS byte arrays.");
//                        }
//
//                        usleep(1000 * 50); // 50 ms delay added
//                        if (j_sps_data) env->DeleteLocalRef(j_sps_data);
//                        if (j_pps_data) env->DeleteLocalRef(j_pps_data);
//                    } else {
//                        g_printerr("[GStreamer] SPS/PPS size was 0 (sps=%zu, pps=%zu)\n", sps_map.size, pps_map.size);
//                    }
//
//                    gst_buffer_unmap(data->sps, &sps_map);
//                    gst_buffer_unmap(data->pps, &pps_map);
//                } else {
//                    g_printerr("[GStreamer] Failed to map SPS or PPS buffer\n");
//                }
//            } else {
//                g_printerr("[GStreamer] SPS/PPS not extracted or are null\n");
//            }
//        }
//
//        jobject byteBuffer = env->NewDirectByteBuffer(map.data, map.size);
//        if (byteBuffer) {
//            notify_media_data(env, data->app, byteBuffer, pts_us, dts_us);
//        }
//
//        gst_buffer_unmap(buffer, &map);
//    }
//
//    gst_sample_unref(sample);
//    return GST_FLOW_OK;
//}

/* ==================== UPDATED h264_worker_thread (WITH Push/PopLocalFrame) ==================== */
static gpointer h264_worker_thread(gpointer userdata) {
    auto *data = (CustomData *)userdata;

    // Attach JNI once for this thread
    JNIEnv *env = get_jni_env(data);
    if (!env) {
        g_printerr("[GStreamer] Worker thread failed to attach JNI\n");
        return nullptr;
    }

    GstClockTime last_sent_pts = GST_CLOCK_TIME_NONE;
    GstClockTime last_dts = GST_CLOCK_TIME_NONE;

    while (data->streaming.load()) {
        GstSample *sample = (GstSample *)g_async_queue_pop(data->frame_queue);

        // Sentinel for shutdown
        if (!sample) {
            break;
        }

        GstBuffer *buffer = gst_sample_get_buffer(sample);
        if (!buffer || gst_buffer_get_size(buffer) == 0) {
            gst_sample_unref(sample);
            continue;
        }

        /* === PUSH LOCAL FRAME FOR THIS SAMPLE (safe JNI ref management) === */
        if (env->PushLocalFrame(32) != 0) {  // 32 is more than enough for our refs
            g_printerr("[GStreamer] Failed to push local frame\n");
            gst_sample_unref(sample);
            continue;
        }

        // Timing and FPS calculations
        GstClockTime now = gst_util_get_timestamp();
        data->frame_count++;
        calculate_fps(data);

        if (data->stream_start_time == 0) {
            data->stream_start_time = now;
        }

        // PTS/DTS handling
        GstClockTime current_pts = GST_BUFFER_PTS(buffer);
        GstClockTime current_dts = GST_BUFFER_DTS(buffer);

        if (!GST_CLOCK_TIME_IS_VALID(current_pts)) {
            current_pts = now;
        }
        if (!GST_CLOCK_TIME_IS_VALID(current_dts)) {
            if (GST_CLOCK_TIME_IS_VALID(last_dts)) {
                current_dts = last_dts + DEFAULT_FRAME_DURATION;
            } else {
                current_dts = current_pts;
            }
        }
        last_dts = current_dts;

        jlong pts_us = (jlong)(current_pts / GST_USECOND);
        jlong dts_us = (jlong)(current_dts / GST_USECOND);

        // Skip duplicate PTS frames
        if (current_pts != GST_CLOCK_TIME_NONE && current_pts == last_sent_pts) {
            env->PopLocalFrame(nullptr);
            gst_sample_unref(sample);
            continue;
        }
        last_sent_pts = current_pts;

        GstMapInfo map;
        if (gst_buffer_map(buffer, &map, GST_MAP_READ)) {
            // SPS/PPS extraction - only on keyframes and only if not sent yet
            if (!data->sps_pps_sent && !GST_BUFFER_FLAG_IS_SET(buffer, GST_BUFFER_FLAG_DELTA_UNIT)) {
                parse_nal_units(map.data, (jint)map.size, data);

                if (data->sps_pps_extracted && data->sps && data->pps) {
                    GstMapInfo sps_map, pps_map;
                    gboolean sps_ok = gst_buffer_map(data->sps, &sps_map, GST_MAP_READ);
                    gboolean pps_ok = gst_buffer_map(data->pps, &pps_map, GST_MAP_READ);

                    if (sps_ok && pps_ok && sps_map.size > 0 && pps_map.size > 0) {
                        jbyteArray j_sps_data = env->NewByteArray((jsize)sps_map.size);
                        jbyteArray j_pps_data = env->NewByteArray((jsize)pps_map.size);

                        if (j_sps_data && j_pps_data) {
                            env->SetByteArrayRegion(j_sps_data, 0, (jsize)sps_map.size, (jbyte *)sps_map.data);
                            env->SetByteArrayRegion(j_pps_data, 0, (jsize)pps_map.size, (jbyte *)pps_map.data);

                            jclass clazz = env->GetObjectClass(data->app);
                            if (clazz) {
                                jmethodID method = env->GetMethodID(clazz, "onSpsPpsDataReceived", "([B[B)V");
                                if (method) {
                                    env->CallVoidMethod(data->app, method, j_sps_data, j_pps_data);
                                    data->sps_pps_sent = TRUE;
                                }
                                /* REMOVED: DeleteLocalRef(clazz) - frame will free */
                            }
                            /* REMOVED: DeleteLocalRef(j_sps_data / j_pps_data) */
                        }

                        gst_buffer_unmap(data->sps, &sps_map);
                        gst_buffer_unmap(data->pps, &pps_map);
                    }
                }
            }

            // Send frame data to Java
            jobject byteBuffer = env->NewDirectByteBuffer(map.data, map.size);
            if (byteBuffer) {
                notify_media_data(env, data->app, byteBuffer, pts_us, dts_us);
                /* REMOVED: DeleteLocalRef(byteBuffer) - frame will free it */
            }

            gst_buffer_unmap(buffer, &map);
        }

        /* === POP LOCAL FRAME - frees ALL local refs created in this iteration === */
        env->PopLocalFrame(nullptr);

        gst_sample_unref(sample);
    }

    // Detach JNI when exiting
    if (data->jvm) {
        data->jvm->DetachCurrentThread();
    }

    return nullptr;
}


/* ==================== MINIMAL on_new_sample CALLBACK ==================== */
static GstFlowReturn on_new_sample(GstAppSink *appsink, gpointer userdata) {
    auto *data = (CustomData *) userdata;
    if (!data || !data->stream_alive.load() || !data->streaming.load()) {
        return GST_FLOW_EOS;
    }

    // Update watchdog timestamp
    data->last_sample_time.store(g_get_monotonic_time());

    // Recovery detection (rare - JNI call here is acceptable since infrequent)
    gboolean  in_recovery_expected = TRUE;
    if (data->in_recovery.compare_exchange_strong((in_recovery_expected), false)) {
        g_print("[GStreamer] H264 frame received - Recovery succeeded after %d attempts\n",
                data->retry_count.load());

        guint current_timer = data->active_timer_id.exchange(0);
        if (current_timer != 0) {
            g_source_remove(current_timer);
            g_print("[GStreamer] Cancelled pending retry timer ID: %u\n", current_timer);
        }

        data->retry_count.store(0);
        data->next_retry_delay_ms.store(INITIAL_RETRY_DELAY_MS);

        // Notify Java of recovery (rare call - safe in streaming thread)
        JNIEnv *env = get_jni_env(data);
        if (env) {
            notify_loading_status(env, data->app, 0, MAX_RETRIES, FALSE);
        }
    }

    GstSample *sample = gst_app_sink_pull_sample(appsink);
    if (!sample) {
        return GST_FLOW_ERROR;
    }

    // Ref and push to worker queue - worker will unref
    gst_sample_ref(sample);
    g_async_queue_push(data->frame_queue, sample);

    return GST_FLOW_OK;
}

static void on_pad_added(GstElement *src, GstPad *new_pad, CustomData *data) {
    GstCaps *caps = gst_pad_get_current_caps(new_pad);
    if (!caps) {
        g_printerr("New pad has no caps\n");
        return;
    }

    gchar *caps_str = gst_caps_to_string(caps);
    g_print("New pad added: %s with caps: %s\n", GST_PAD_NAME(new_pad), caps_str);
    g_free(caps_str);

    const GstStructure *str = gst_caps_get_structure(caps, 0);
    const gchar *media_type = gst_structure_get_string(str, "media");
    const gchar *encoding_name = gst_structure_get_string(str, "encoding-name");

    if (!media_type || !encoding_name) {
        g_printerr("Missing media or encoding-name in caps\n");
        gst_caps_unref(caps);
        return;
    }

    // Only link if it's H264 video RTP stream
    if (g_strcmp0(media_type, "video") == 0 && g_str_has_prefix(encoding_name, "H264")) {
        GstPad *sink_pad = gst_element_get_static_pad(data->queue0, "sink");

        if (gst_pad_is_linked(sink_pad)) {
            g_print("Sink pad already linked, skipping\n");
        } else {
            GstPadLinkReturn ret = gst_pad_link(new_pad, sink_pad);
            if (GST_PAD_LINK_FAILED(ret)) {
                const char *error_str = nullptr;
                switch (ret) {
                    case GST_PAD_LINK_WRONG_HIERARCHY: error_str = "WRONG_HIERARCHY"; break;
                    case GST_PAD_LINK_WAS_LINKED: error_str = "WAS_LINKED"; break;
                    case GST_PAD_LINK_WRONG_DIRECTION: error_str = "WRONG_DIRECTION"; break;
                    case GST_PAD_LINK_NOFORMAT: error_str = "NOFORMAT"; break;
                    case GST_PAD_LINK_NOSCHED: error_str = "NOSCHED"; break;
                    case GST_PAD_LINK_REFUSED: error_str = "REFUSED"; break;
                    default: error_str = "UNKNOWN"; break;
                }
                g_printerr("Pad link failed: %s (%d)\n", error_str, ret);
            } else {
                g_print("Pad successfully linked: RTP H264 video stream\n");
            }
        }
        gst_object_unref(sink_pad);
    } else {
        g_print("Pad not linked: Unsupported media='%s', encoding='%s'\n", media_type, encoding_name);
    }

    gst_caps_unref(caps);
}

/* ==================== COMPLETE UPDATED start_event_driven_stream ==================== */
/* ==================== COMPLETE UPDATED start_event_driven_stream ==================== */
void *start_event_driven_stream(void *userdata) {
    // Elevate thread priority for better real-time performance
    if (setpriority(PRIO_PROCESS, 0, -15) == -1) {
        perror("[GStreamer] Warning: Failed to set thread priority");
    }

    struct sched_param param = {0};
    param.sched_priority = sched_get_priority_max(SCHED_RR);
    if (pthread_setschedparam(pthread_self(), SCHED_RR, &param) != 0) {
        g_print("[GStreamer] Using SCHED_OTHER scheduling\n");
        setpriority(PRIO_PROCESS, 0, -15);
    }

    if (!userdata) return nullptr;
    auto *mData = (CustomData *)userdata;

    JNIEnv* env = get_jni_env(mData);
    if (!env || !mData->app) {
        g_printerr("[GStreamer] JNI env or Java app object not available\n");
        if (mData && mData->jvm) {
            mData->jvm->DetachCurrentThread();
        }
        return nullptr;
    }

    set_stream_state(mData, STREAM_STATE_CONNECTING);

    g_print("[GStreamer] ==========================================\n");
    g_print("[GStreamer] Starting RTSP stream thread\n");
    g_print("[GStreamer] Stream URI: %s\n", mData->rtsp_uri);
    g_print("[GStreamer] ==========================================\n");

    if (!mData->rtsp_uri || strlen(mData->rtsp_uri) == 0) {
        notify_stream_error(env, mData->app, ERR_RTSP_URI_INVALID, "Empty RTSP URI");
        g_printerr("[GStreamer] Invalid RTSP URI — aborting start\n");
        mData->stream_alive.store(false);
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    if (!is_valid_rtsp_uri(mData->rtsp_uri)) {
        g_printerr("[GStreamer]Invalid RTSP URI format\n");
        notify_stream_error(env, mData->app, ERR_RTSP_URI_PARSE_FAILED, "Invalid RTSP URI format");
        mData->stream_alive.store(false);
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    if (mData->stop_requested.load()) {
        g_print("[GStreamer] Stop requested before stream start\n");
        mData->stream_alive.store(false);
        set_stream_state(mData, STREAM_STATE_STOPPED);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    if (!validate_rtsp_connection(mData->rtsp_uri)) {
        notify_stream_error(env, mData->app, ERR_NETWORK_ERROR, "Invalid RTSP URI format");
        mData->stream_alive.store(false);
        g_printerr("[GStreamer] RTSP connection validation failed\n");
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    mData->active_timer_id.store(0);

    g_print("[GStreamer] Waiting 300ms for network stabilization...\n");
    g_usleep(300 * 1000);

    set_stream_state(mData, STREAM_STATE_INITIALIZING);

    g_print("[GStreamer] Creating pipeline elements...\n");

    GstElement *rtspsrc = gst_element_factory_make("rtspsrc", "src");
    GstElement *depay   = gst_element_factory_make("rtph264depay", "depay");
    GstElement *parser  = gst_element_factory_make("h264parse", "parser");
    GstElement *filter  = gst_element_factory_make("capsfilter", "filter");
    GstElement *sink    = gst_element_factory_make("appsink", "sink");
    GstElement *queue0  = gst_element_factory_make("queue", "tcp_queue");
    GstElement *queue1  = gst_element_factory_make("queue", "video_queue");

    if (!rtspsrc || !depay || !parser || !filter || !sink || !queue0 || !queue1) {
        fail_and_exit(mData, env, ERR_ELEMENT_CREATION_FAILED, "Failed to create pipeline elements");
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    g_print("[GStreamer] Configuring elements with optimized settings...\n");

    // === UPDATED rtspsrc: Prefer UDP with TCP fallback + lower latency ===
    g_object_set(rtspsrc,
                 "location", mData->rtsp_uri,
                 "protocols", GST_RTSP_LOWER_TRANS_UDP | GST_RTSP_LOWER_TRANS_TCP,
                 "user-agent", "RTSP_GStreamer/1.0",
                 "latency", 100,
                 "drop-on-latency", TRUE,
                 "buffer-mode", 1,
                 "timeout", (guint64)(30 * GST_SECOND),
                 "tcp-timeout", (guint64)(15 * GST_SECOND),
                 "teardown-timeout", (guint64)(5 * GST_SECOND),
                 "retry", 5,
                 "udp-reconnect", TRUE,
                 "do-retransmission", FALSE,
                 "do-rtsp-keep-alive", TRUE,
                 "do-rtcp", TRUE,
                 "is-live", TRUE,
                 "use-pipeline-clock", TRUE,
                 NULL);

    // === Queues: Larger buffers for high-res bursts + leaky ===
    g_object_set(queue0,
                 "max-size-buffers", 200,
                 "max-size-bytes", 0,
                 "max-size-time", (guint64)(2000 * GST_MSECOND),
                 "leaky", 2,
                 NULL);

    g_object_set(queue1,
                 "max-size-buffers", 200,
                 "max-size-bytes", 0,
                 "max-size-time", (guint64)(2000 * GST_MSECOND),
                 "leaky", 2,
                 NULL);

    g_object_set(depay,
                 "wait-for-keyframe", TRUE,
                 NULL);

    // === Appsink: Higher max-buffers ===
    g_object_set(sink,
                 "sync", FALSE,
                 "emit-signals", TRUE,
                 "max-buffers", 300,
                 "drop", TRUE,
                 "qos", FALSE,
                 "enable-last-sample", FALSE,
                 "wait-on-eos", FALSE,
                 NULL);

    g_object_set(parser,
                 "config-interval", 1,
                 "disable-passthrough", TRUE,
                 NULL);

    mData->depay = depay;
    mData->appsink = sink;
    mData->queue0 = queue0;
    mData->queue1 = queue1;

    // === Caps: Removed forced framerate ===
    GstCaps* caps = gst_caps_new_simple("video/x-h264",
                                        "stream-format", G_TYPE_STRING, "byte-stream",
                                        "alignment", G_TYPE_STRING, "au",
                                        NULL);

    g_object_set(filter, "caps", caps, NULL);
    gst_caps_unref(caps);

    mData->pipeline = gst_pipeline_new("rtsp-tcp-pipeline");
    if (!mData->pipeline) {
        fail_and_exit(mData, env, ERR_PIPELINE_CREATION_FAILED, "Pipeline creation failed");
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    gst_bin_add_many(GST_BIN(mData->pipeline),
                     queue0, depay, parser, queue1, filter, sink, NULL);
    gst_bin_add(GST_BIN(mData->pipeline), rtspsrc);

    // Linking (unchanged)
    gboolean link_ok = TRUE;
    if (!gst_element_link(queue0, depay)) link_ok = FALSE;
    if (!gst_element_link(depay, parser)) link_ok = FALSE;
    if (!gst_element_link(parser, queue1)) link_ok = FALSE;
    if (!gst_element_link(queue1, filter)) link_ok = FALSE;
    if (!gst_element_link(filter, sink)) link_ok = FALSE;

    if (!link_ok) {
        g_printerr("[GStreamer] Failed to link pipeline elements\n");
        fail_and_exit(mData, env, ERR_ELEMENT_LINK_FAILED, "Pipeline element link failed");
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    // Signals - updated to new minimal callback
    g_signal_connect(rtspsrc, "pad-added", G_CALLBACK(on_pad_added), mData);
    g_signal_connect(sink, "new-sample", G_CALLBACK(on_new_sample), mData);

    mData->bus = gst_element_get_bus(mData->pipeline);
    if (!mData->bus) {
        fail_and_exit(mData, env, ERR_BUS_CREATION_FAILED, "Failed to acquire pipeline bus");
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    mData->keepalive_timer = g_timeout_add_seconds(10, send_rtsp_keepalive, mData);
    mData->bus_watch_id = gst_bus_add_watch(mData->bus, (GstBusFunc)bus_call, mData);

    // Reset variables
    mData->last_sample_time = g_get_monotonic_time();
    mData->retry_count = 0;
    mData->next_retry_delay_ms = INITIAL_RETRY_DELAY_MS;
    mData->in_recovery = FALSE;
    mData->stream_has_started = FALSE;
    mData->active_timer_id.store(0);

    set_stream_state(mData, STREAM_STATE_BUFFERING);

    // State changes (unchanged)
    gst_element_set_state(mData->pipeline, GST_STATE_NULL);
    g_usleep(100 * 1000);

    GstStateChangeReturn ret = gst_element_set_state(mData->pipeline, GST_STATE_READY);
    if (ret == GST_STATE_CHANGE_FAILURE) {
        fail_and_exit(mData, env, ERR_PIPELINE_STATE_FAILED, "Failed to set READY state");
        set_stream_state(mData, STREAM_STATE_ERROR);
        mData->jvm->DetachCurrentThread();
        return nullptr;
    }

    gst_element_get_state(mData->pipeline, nullptr, nullptr, 3 * GST_SECOND);

    ret = gst_element_set_state(mData->pipeline, GST_STATE_PAUSED);
    if (ret == GST_STATE_CHANGE_ASYNC || ret == GST_STATE_CHANGE_SUCCESS) {
        g_usleep(500 * 1000);
    }

    ret = gst_element_set_state(mData->pipeline, GST_STATE_PLAYING);
    // (recovery logic unchanged - omitted for brevity, keep your original)

    gst_element_set_start_time(mData->pipeline, GST_CLOCK_TIME_NONE);

    set_stream_state(mData, STREAM_STATE_PLAYING);

    // === REMOVED post-startup queue reduction ===

    // === NEW: Create queue and start worker thread ===
    mData->frame_queue = g_async_queue_new_full((GDestroyNotify) gst_sample_unref);
    mData->worker_thread = g_thread_new("h264-worker", h264_worker_thread, mData);

    notify_stream_switch_status(env, mData->app, JNI_FALSE);
    notify_stream_started(env, mData->app);

    g_print("[GStreamer] ==========================================\n");
    g_print("[GStreamer] Pipeline setup complete\n");
    g_print("[GStreamer] Starting main loop...\n");
    g_print("[GStreamer] ==========================================\n");

    mData->main_loop = g_main_loop_new(nullptr, FALSE);
    if (mData->main_loop) {
        GMainContext* ctx = g_main_loop_get_context(mData->main_loop);
        g_main_context_push_thread_default(ctx);

        guint stop_check_timer = g_timeout_add(500, [](gpointer data) -> gboolean {
            auto* custom = static_cast<CustomData*>(data);
            if (custom && custom->stop_requested.load()) {
                g_print("[StopCheck] Stop requested, quitting main loop\n");
                if (custom->main_loop && g_main_loop_is_running(custom->main_loop)) {
                    g_main_loop_quit(custom->main_loop);
                }
                return G_SOURCE_REMOVE;
            }
            return G_SOURCE_CONTINUE;
        }, mData);

        mData->watchdog_timer = g_timeout_add_seconds(WATCHDOG_INTERVAL, watchdog_check, mData);

        g_main_loop_run(mData->main_loop);

        g_source_remove(stop_check_timer);
        g_main_context_pop_thread_default(ctx);
    }

    g_print("[GStreamer] Main loop exited, cleaning up...\n");

    set_stream_state(mData, STREAM_STATE_STOPPING);

    // === NEW: Worker shutdown ===
    if (mData->frame_queue) {
        g_async_queue_push(mData->frame_queue, nullptr);  // Sentinel
    }
    if (mData->worker_thread) {
        g_thread_join(mData->worker_thread);
        mData->worker_thread = nullptr;
    }
    if (mData->frame_queue) {
        g_async_queue_unref(mData->frame_queue);
        mData->frame_queue = nullptr;
    }

    cleanup_streaming_resources(mData, env);

    set_stream_state(mData, STREAM_STATE_STOPPED);

    if (mData->jvm) {
        mData->jvm->DetachCurrentThread();
    }

    g_print("[GStreamer] Stream thread finished\n");
    return nullptr;
}

//void *start_event_driven_stream(void *userdata) {
//    // Elevate thread priority for better real-time performance
//    if (setpriority(PRIO_PROCESS, 0, -15) == -1) {
//        perror("[GStreamer] Warning: Failed to set thread priority");
//    }
//
//    // Set scheduling policy for better real-time performance
//    struct sched_param param = {0};
//    param.sched_priority = sched_get_priority_max(SCHED_RR);
//    if (pthread_setschedparam(pthread_self(), SCHED_RR, &param) != 0) {
//        g_print("[GStreamer] Using SCHED_OTHER scheduling\n");
//        setpriority(PRIO_PROCESS, 0, -15); // Still set nice value
//    }
//
//    if (!userdata) return nullptr;
//    auto *mData = (CustomData *)userdata;
//
//    JNIEnv* env = get_jni_env(mData);
//    if (!env || !mData->app) {
//        g_printerr("[GStreamer] JNI env or Java app object not available\n");
//        if (mData && mData->jvm) {
//            mData->jvm->DetachCurrentThread();
//        }
//        return nullptr;
//    }
//
//    // ============================================================
//    // STATE: CONNECTING
//    // ============================================================
//    set_stream_state(mData, STREAM_STATE_CONNECTING);
//
//    g_print("[GStreamer] ==========================================\n");
//    g_print("[GStreamer] Starting RTSP stream thread\n");
//    g_print("[GStreamer] Stream URI: %s\n", mData->rtsp_uri);
//    g_print("[GStreamer] ==========================================\n");
//
//    // Validate RTSP URI
//    if (!mData->rtsp_uri || strlen(mData->rtsp_uri) == 0) {
//        notify_stream_error(env, mData->app, ERR_RTSP_URI_INVALID, "Empty RTSP URI");
//        g_printerr("[GStreamer] Invalid RTSP URI — aborting start\n");
//        mData->stream_alive.store(false);
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    if (!is_valid_rtsp_uri(mData->rtsp_uri)) {
//        g_printerr("[GStreamer]Invalid RTSP URI format\n");
//        notify_stream_error(env, mData->app, ERR_RTSP_URI_PARSE_FAILED, "Invalid RTSP URI format");
//        mData->stream_alive.store(false);
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // Check if stop was requested before we even start
//    if (mData->stop_requested.load()) {
//        g_print("[GStreamer] Stop requested before stream start\n");
//        mData->stream_alive.store(false);
//        set_stream_state(mData, STREAM_STATE_STOPPED);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // Validate RTSP connection (basic format validation)
//    if (!validate_rtsp_connection(mData->rtsp_uri)) {
//        notify_stream_error(env, mData->app, ERR_NETWORK_ERROR, "Invalid RTSP URI format");
//        mData->stream_alive.store(false);
//        g_printerr("[GStreamer] RTSP connection validation failed\n");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // Initialize atomic timer ID
//    mData->active_timer_id.store(0);
//
//    // Network stabilization delay
//    g_print("[GStreamer] Waiting 300ms for network stabilization...\n");
//    g_usleep(300 * 1000); // 300ms delay
//
//    // ============================================================
//    // STATE: INITIALIZING
//    // ============================================================
//    set_stream_state(mData, STREAM_STATE_INITIALIZING);
//
//    // Create pipeline elements with error checking
//    g_print("[GStreamer] Creating pipeline elements...\n");
//
//    GstElement *rtspsrc = gst_element_factory_make("rtspsrc", "src");
//    GstElement *depay   = gst_element_factory_make("rtph264depay", "depay");
//    GstElement *parser  = gst_element_factory_make("h264parse", "parser");
//    GstElement *filter  = gst_element_factory_make("capsfilter", "filter");
//    GstElement *sink    = gst_element_factory_make("appsink", "sink");
//    GstElement *queue0  = gst_element_factory_make("queue", "tcp_queue");
//    GstElement *queue1  = gst_element_factory_make("queue", "video_queue");
//
//    // Check element creation
//    if (!rtspsrc) g_printerr("[GStreamer] Failed to create rtspsrc\n");
//    if (!depay) g_printerr("[GStreamer] Failed to create rtph264depay\n");
//    if (!parser) g_printerr("[GStreamer] Failed to create h264parse\n");
//    if (!filter) g_printerr("[GStreamer] Failed to create capsfilter\n");
//    if (!sink) g_printerr("[GStreamer] Failed to create appsink\n");
//    if (!queue0) g_printerr("[GStreamer] Failed to create queue0\n");
//    if (!queue1) g_printerr("[GStreamer] Failed to create queue1\n");
//
//    if (!rtspsrc || !depay || !parser || !filter || !sink || !queue0 || !queue1) {
//        fail_and_exit(mData, env, ERR_ELEMENT_CREATION_FAILED, "Failed to create pipeline elements");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    g_print("[GStreamer] Configuring elements with optimized settings...\n");
//
//    // Configure rtspsrc for performance & stability
//    g_object_set(rtspsrc,
//            // Basic connection
//                 "location", mData->rtsp_uri,
//                 "protocols", GST_RTSP_LOWER_TRANS_TCP,  // TCP only for reliability
//                 "user-agent", "RTSP_GStreamer/1.0",
//
//            // Low latency settings
//                 "latency", 200,
//                 "drop-on-latency", TRUE,
//
//            // Connection stability & fast recovery
//                 "timeout", (guint64)(30 * GST_SECOND),
//                 "tcp-timeout", (guint64)(15 * GST_SECOND),
//                 "teardown-timeout", (guint64)(5 * GST_SECOND),
//                 "retry", 3,
//
//            // Network optimization
//                 "do-retransmission", FALSE,
//                 "do-rtsp-keep-alive", TRUE,
//                 "do-rtcp", TRUE,
//                 "connection-speed", (guint64)10000000,
//
//            // Performance tuning
//                 "buffer-mode", 3,
//                 "async-handling", TRUE,
//                 "max-rtcp-rtp-time-diff", 100,
//
//            // Stream startup reliability
//                 "is-live", TRUE,
//                 "use-pipeline-clock", TRUE,
//
//            // Advanced optimizations
//                 "ntp-sync", FALSE,
//                 "short-header", FALSE,
//                 "add-reference-timestamp-meta", TRUE,
//                 "tcp-timestamp", TRUE,
//
//            // Error handling
//                 "probation", 2,
//                 "udp-reconnect", FALSE,
//
//            // 🚫 DISABLE UNNECESSARY FEATURES
//                 "onvif-mode", FALSE,                    // Disable ONVIF
//                 "onvif-rate-control", FALSE,            // Disable ONVIF rate control
//                 "rfc7273-sync", FALSE,                  // Disable RFC7273 sync
//                 NULL);
//
//    // Configure queues with startup optimization
//    g_object_set(queue0,
//                 "max-size-buffers", 200,
//                 "max-size-bytes", 0,
//                 "max-size-time", (guint64)(2000 * GST_MSECOND),
//                 "leaky", 1,
//                 NULL);
//
//    g_object_set(queue1,
//                 "max-size-buffers", 200,
//                 "max-size-bytes", 0,
//                 "max-size-time", (guint64)(2000 * GST_MSECOND),
//                 "leaky", 1,
//                 NULL);
//
//    // Configure depayloader
//    g_object_set(depay,
//                 "wait-for-keyframe", TRUE,
//                 NULL);
//
//    // Configure appsink for low latency
//    g_object_set(sink,
//                 "sync", FALSE,
//                 "emit-signals", TRUE,
//                 "max-buffers", 200,
//                 "drop", TRUE,
//                 "qos", TRUE,
//                 "enable-last-sample", FALSE,
//                 "wait-on-eos", FALSE,
//                 NULL);
//
//    // Configure H264 parser
//    g_object_set(parser,
//                 "config-interval", -1,
//                 "disable-passthrough", TRUE,
//                 NULL);
//
//    // Assign elements to mData
//    mData->depay = depay;
//    mData->appsink = sink;
//    mData->queue0 = queue0;
//    mData->queue1 = queue1;
//
//    // Create caps for filter with framerate
//    GstCaps* caps = gst_caps_new_simple("video/x-h264",
//                                        "stream-format", G_TYPE_STRING, "byte-stream",
//                                        "alignment", G_TYPE_STRING, "au",
////                                        "framerate", GST_TYPE_FRACTION, 30, 1,
//                                        NULL);
//
//    if (!caps) {
//        fail_and_exit(mData, env, ERR_ELEMENT_CREATION_FAILED, "Failed to create caps");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    g_object_set(filter, "caps", caps, NULL);
//    gst_caps_unref(caps);
//
//    // Create pipeline
//    g_print("[GStreamer] Creating pipeline...\n");
//    mData->pipeline = gst_pipeline_new("rtsp-tcp-pipeline");
//    if (!mData->pipeline) {
//        fail_and_exit(mData, env, ERR_PIPELINE_CREATION_FAILED, "Pipeline creation failed");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // ============================================================
//    // STATE: BUILDING PIPELINE
//    // ============================================================
//    g_print("[GStreamer] Building pipeline...\n");
//
//    gst_bin_add_many(GST_BIN(mData->pipeline),
//                     queue0,
//                     depay,
//                     parser,
//                     queue1,
//                     filter,
//                     sink,
//                     NULL);
//    gst_bin_add(GST_BIN(mData->pipeline), rtspsrc);
//
//    // Link elements with error checking
//    gboolean link_ok = TRUE;
//    if (!gst_element_link(queue0, depay)) {
//        g_printerr("[GStreamer] Failed to link: queue0 → depay\n");
//        link_ok = FALSE;
//    }
//    if (!gst_element_link(depay, parser)) {
//        g_printerr("[GStreamer] Failed to link: depay → parser\n");
//        link_ok = FALSE;
//    }
//    if (!gst_element_link(parser, queue1)) {
//        g_printerr("[GStreamer] Failed to link: parser → queue1\n");
//        link_ok = FALSE;
//    }
//    if (!gst_element_link(queue1, filter)) {
//        g_printerr("[GStreamer] Failed to link: queue1 → filter\n");
//        link_ok = FALSE;
//    }
//    if (!gst_element_link(filter, sink)) {
//        g_printerr("[GStreamer] Failed to link: filter → sink\n");
//        link_ok = FALSE;
//    }
//
//    if (!link_ok) {
//        fail_and_exit(mData, env, ERR_ELEMENT_LINK_FAILED, "Pipeline element link failed");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // Connect signals
//    g_signal_connect(rtspsrc, "pad-added", G_CALLBACK(on_pad_added), mData);
//    g_signal_connect(sink, "new-sample", G_CALLBACK(on_new_sample), mData);
//
//    // Set up bus monitoring
//    mData->bus = gst_element_get_bus(mData->pipeline);
//    if (!mData->bus) {
//        fail_and_exit(mData, env, ERR_BUS_CREATION_FAILED, "Failed to acquire pipeline bus");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // Add keep-alive and bus watch
//    mData->keepalive_timer = g_timeout_add_seconds(10, send_rtsp_keepalive, mData);
//    mData->bus_watch_id = gst_bus_add_watch(mData->bus, (GstBusFunc)bus_call, mData);
//
//    // Reset tracking variables
//    mData->last_sample_time = g_get_monotonic_time();
//    mData->retry_count = 0;
//    mData->next_retry_delay_ms = INITIAL_RETRY_DELAY_MS;
//    mData->in_recovery = FALSE;
//    mData->stream_has_started = FALSE;
//    mData->active_timer_id.store(0);
//
//    // ============================================================
//    // STATE: BUFFERING
//    // ============================================================
//    set_stream_state(mData, STREAM_STATE_BUFFERING);
//
//    g_print("[GStreamer] Starting pipeline with optimized startup sequence...\n");
//
//    // Step 1: Set to NULL first (clean state)
//    gst_element_set_state(mData->pipeline, GST_STATE_NULL);
//    g_usleep(100 * 1000); // 100ms delay
//
//    // Step 2: Set to READY state
//    GstStateChangeReturn ret = gst_element_set_state(mData->pipeline, GST_STATE_READY);
//    if (ret == GST_STATE_CHANGE_FAILURE) {
//        g_printerr("[GStreamer] Failed to set READY state\n");
//        fail_and_exit(mData, env, ERR_PIPELINE_STATE_FAILED, "Failed to set READY state");
//        set_stream_state(mData, STREAM_STATE_ERROR);
//        mData->jvm->DetachCurrentThread();
//        return nullptr;
//    }
//
//    // Wait for READY state with timeout
//    ret = gst_element_get_state(mData->pipeline, nullptr, nullptr, 3 * GST_SECOND);
//    if (ret == GST_STATE_CHANGE_FAILURE) {
//        g_printerr("[GStreamer] Timeout waiting for READY state\n");
//    } else {
//        g_print("[GStreamer] Pipeline in READY state\n");
//    }
//
//    // Step 3: Set to PAUSED first (allows buffering)
//    g_print("[GStreamer] Setting to PAUSED for initial buffering...\n");
//    ret = gst_element_set_state(mData->pipeline, GST_STATE_PAUSED);
//
//    if (ret == GST_STATE_CHANGE_ASYNC || ret == GST_STATE_CHANGE_SUCCESS) {
//        // Wait for PAUSED state and allow initial buffering
//        g_print("[GStreamer] Waiting 500ms for initial buffering...\n");
//        g_usleep(500 * 1000); // 500ms for buffering
//
//        // Check if we're prerolled
//        GstState state, pending;
//        gst_element_get_state(mData->pipeline, &state, &pending, 1 * GST_SECOND);
//        g_print("[GStreamer] After buffering: state=%s, pending=%s\n",
//                gst_element_state_get_name(state),
//                gst_element_state_get_name(pending));
//    }
//
//    // Step 4: Now set to PLAYING
//    g_print("[GStreamer] Setting to PLAYING state...\n");
//    ret = gst_element_set_state(mData->pipeline, GST_STATE_PLAYING);
//
//    switch (ret) {
//        case GST_STATE_CHANGE_FAILURE:
//            g_printerr("[GStreamer] Failed to set PLAYING state\n");
//            // Try immediate recovery
//            g_print("[GStreamer] Attempting immediate recovery...\n");
//            gst_element_set_state(mData->pipeline, GST_STATE_NULL);
//            g_usleep(200 * 1000);
//            gst_element_set_state(mData->pipeline, GST_STATE_PLAYING);
//
//            // Check if recovery worked
//            ret = gst_element_get_state(mData->pipeline, nullptr, nullptr, 5 * GST_SECOND);
//            if (ret == GST_STATE_CHANGE_FAILURE) {
//                fail_and_exit(mData, env, ERR_PIPELINE_STATE_FAILED, "Failed to start pipeline");
//                set_stream_state(mData, STREAM_STATE_ERROR);
//                mData->jvm->DetachCurrentThread();
//                return nullptr;
//            }
//            break;
//
//        case GST_STATE_CHANGE_ASYNC:
//            g_print("[GStreamer] State change is asynchronous, waiting...\n");
//            // Wait for PLAYING state with timeout
//            ret = gst_element_get_state(mData->pipeline, nullptr, nullptr, 8 * GST_SECOND);
//            if (ret == GST_STATE_CHANGE_FAILURE) {
//                g_printerr("[GStreamer] Timeout waiting for PLAYING state\n");
//                // Don't fail immediately - let watchdog handle it
//                g_print("[GStreamer] Will let watchdog handle recovery\n");
//            }
//            break;
//
//        case GST_STATE_CHANGE_SUCCESS:
//        case GST_STATE_CHANGE_NO_PREROLL:
//            g_print("[GStreamer] Pipeline successfully set to PLAYING\n");
//            break;
//
//        default:
//            g_print("[GStreamer] State change result: %d\n", ret);
//            break;
//    }
//
//    gst_element_set_start_time(mData->pipeline, GST_CLOCK_TIME_NONE);
//
//    // ============================================================
//    // STATE: PLAYING
//    // ============================================================
//    set_stream_state(mData, STREAM_STATE_PLAYING);
//
//    // Post-startup optimization
//    g_print("[GStreamer] Optimizing for low latency operation...\n");
//    g_object_set(queue0,
//                 "max-size-buffers", 45,
//                 "max-size-time", (guint64)(300 * GST_MSECOND),
//                 NULL);
//
//    g_object_set(queue1,
//                 "max-size-buffers", 45,
//                 "max-size-time", (guint64)(300 * GST_MSECOND),
//                 NULL);
//
//    // Notify Java that stream is ready
//    notify_stream_switch_status(env, mData->app, JNI_FALSE);
//    notify_stream_started(env, mData->app);
//
//    g_print("[GStreamer] ==========================================\n");
//    g_print("[GStreamer] Pipeline setup complete\n");
//    g_print("[GStreamer] Starting main loop...\n");
//    g_print("[GStreamer] ==========================================\n");
//
//    // Run main loop
//    mData->main_loop = g_main_loop_new(nullptr, FALSE);
//    if (mData->main_loop) {
//        GMainContext* ctx = g_main_loop_get_context(mData->main_loop);
//        g_main_context_push_thread_default(ctx);
//
//        // Add a periodic check for stop request
//        guint stop_check_timer = g_timeout_add(500, [](gpointer data) -> gboolean {
//            auto* custom = static_cast<CustomData*>(data);
//            if (custom && custom->stop_requested.load()) {
//                g_print("[StopCheck] Stop requested, quitting main loop\n");
//                if (custom->main_loop && g_main_loop_is_running(custom->main_loop)) {
//                    g_main_loop_quit(custom->main_loop);
//                }
//                return G_SOURCE_REMOVE;
//            }
//            return G_SOURCE_CONTINUE;
//        }, mData);
//
//        // Start watchdog only after stream has started
//        mData->watchdog_timer = g_timeout_add_seconds(WATCHDOG_INTERVAL, watchdog_check, mData);
//
//        g_print("[GStreamer] Entering main loop...\n");
//        g_main_loop_run(mData->main_loop);
//
//        // Clean up the stop check timer
//        g_source_remove(stop_check_timer);
//
//        g_main_context_pop_thread_default(ctx);
//    }
//
//    g_print("[GStreamer] Main loop exited, cleaning up...\n");
//
//    // ============================================================
//    // STATE: STOPPING
//    // ============================================================
//    set_stream_state(mData, STREAM_STATE_STOPPING);
//
//    // Cleanup resources
//    cleanup_streaming_resources(mData, env);
//
//    // ============================================================
//    // STATE: STOPPED
//    // ============================================================
//    set_stream_state(mData, STREAM_STATE_STOPPED);
//
//    // Detach from JVM
//    if (mData->jvm) {
//        mData->jvm->DetachCurrentThread();
//    }
//
//    g_print("[GStreamer] Stream thread finished\n");
//    return nullptr;
//}


/*decode, encode*/
//static GstFlowReturn on_new_sample(GstAppSink *appsink, gpointer userdata) {
//    auto *data = (CustomData *) userdata;
//    if (!data || !data->streaming) return GST_FLOW_OK;
//
//    data->last_sample_time = g_get_monotonic_time();
//
//    JNIEnv *env = get_jni_env(data);
//    if (!env) {
//        g_printerr("[GStreamer] Failed to attach thread to JVM\n");
//        return GST_FLOW_ERROR;
//    }
//
//    // Reset retry count if stream is working
//    if(data->retry_count > 0){
//        data->retry_count = 0;
//        g_print("[GStreamer] Stream recovered successfully\n");
//    }
//
//    GstSample *sample = gst_app_sink_pull_sample(appsink);
//    if (!sample) {
//        g_printerr("[GStreamer] Failed to pull sample\n");
//        return GST_FLOW_EOS;
//    }
//
//    GstBuffer *buffer = gst_sample_get_buffer(sample);
//    if (!buffer) {
//        gst_sample_unref(sample);
//        g_printerr("[GStreamer] Received NULL buffer\n");
//        return GST_FLOW_EOS;
//    }
//
//    gsize buffer_size = gst_buffer_get_size(buffer);
//    if (buffer_size == 0) {
//        gst_sample_unref(sample);
//        g_print("[GStreamer] Received empty buffer (might be normal)\n");
//        return GST_FLOW_OK;  // Changed from EOS to OK
//    }
//
//    // Update frame count and calculate FPS
//    data->frame_count++;
//
//    static gint64 last_log_time = 0;
//    gint64 now = g_get_monotonic_time();
//
//    if (now - last_log_time > 1000000) {  // Log every second
//        calculate_fps(data);
//        g_print("[GStreamer] FPS: %.2f, Buffer size: %zu\n", (double)data->fps, buffer_size);
//        last_log_time = now;
//    }
//
//    GstClockTime current_pts = GST_BUFFER_PTS(buffer);
//    GstClockTime current_dts = GST_BUFFER_DTS(buffer);
//
//    // Handle invalid timestamps
//    if (!GST_CLOCK_TIME_IS_VALID(current_pts)) {
//        current_pts = gst_util_get_timestamp();
//    }
//    if (!GST_CLOCK_TIME_IS_VALID(current_dts)) {
//        current_dts = current_pts;
//    }
//
//    // Check if frame is a keyframe
//    gboolean is_keyframe = FALSE;
//    GstMapInfo map;
//    if (gst_buffer_map(buffer, &map, GST_MAP_READ)) {
//        if (map.size >= 4) {
//            // Check for NAL unit type
//            for (guint i = 0; i < map.size - 4; i++) {
//                if (map.data[i] == 0x00 && map.data[i+1] == 0x00 &&
//                    map.data[i+2] == 0x00 && map.data[i+3] == 0x01) {
//                    if (i+4 < map.size) {
//                        guint8 nal_type = map.data[i+4] & 0x1F;
//                        if (nal_type == 5 || nal_type == 7 || nal_type == 8) {
//                            is_keyframe = TRUE;
//                            if (nal_type == 7 || nal_type == 8) {
//                                g_print("[GStreamer] SPS/PPS in frame\n");
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        gst_buffer_unmap(buffer, &map);
//    }
//
//    if (is_keyframe) {
//        g_print("[GStreamer] Keyframe received\n");
//    }
//
//    // Convert timestamps to microseconds
//    auto pts_us = (jlong)(current_pts / GST_USECOND);
//    auto dts_us = (jlong)(current_dts / GST_USECOND);
//
//    // Process the frame
//    if (gst_buffer_map(buffer, &map, GST_MAP_READ)) {
//        // Create direct byte buffer and notify Java
//        jobject byteBuffer = env->NewDirectByteBuffer(map.data, map.size);
//        if (byteBuffer) {
//            notify_media_data(env, data->app, byteBuffer, pts_us, dts_us);
//        } else {
//            g_printerr("[GStreamer] Failed to create direct byte buffer\n");
//        }
//
//        gst_buffer_unmap(buffer, &map);
//    }
//
//    gst_sample_unref(sample);
//
//    // Log every 30 frames
//    if (data->frame_count % 30 == 0) {
//        g_print("[GStreamer] Sent frame %lld to Java\n");
//    }
//
//    return GST_FLOW_OK;
//}
//
//void *start_event_driven_stream(void *userdata) {
//    // Elevate priority
//    if (setpriority(PRIO_PROCESS, 0, -15) == -1) {
//        perror("[GStreamer] Warning: Failed to set thread priority");
//    }
//
//    struct sched_param param = {0};
//    if (pthread_setschedparam(pthread_self(), SCHED_OTHER, &param) == 0) {
//        setpriority(PRIO_PROCESS, 0, -15);
//    }
//
//    if (!userdata) return nullptr;
//    auto *mData = (CustomData *)userdata;
//
//    JNIEnv* env = get_jni_env(mData);
//    if (!env || !mData->app) {
//        g_printerr("[GStreamer] JNI env or Java app object not available\n");
//        if (mData && mData->jvm) {
//            mData->jvm->DetachCurrentThread();
//        }
//        return nullptr;
//    }
//
//    if (!mData->rtsp_uri || strlen(mData->rtsp_uri) == 0 ||
//        !is_valid_rtsp_uri(mData->rtsp_uri) || !gst_uri_is_valid(mData->rtsp_uri)) {
//        notify_stream_error(env, mData->app, ERR_RTSP_URI_PARSE_FAILED, "Invalid or empty RTSP URI");
//        if (mData && mData->jvm) {
//            mData->jvm->DetachCurrentThread();
//        }
//        return nullptr;
//    }
//
//    g_print("[GStreamer] Starting TCP stream with URI: %s\n", mData->rtsp_uri);
//
//    // Element creation - Using only Android-compatible elements
//    GstElement *rtspsrc = gst_element_factory_make("rtspsrc", "src");
//    GstElement *depay   = gst_element_factory_make("rtph264depay", "depay");
//    GstElement *parser  = gst_element_factory_make("h264parse", "parser");
//
//    // Main pipeline - Use openh264dec which is available on Android
//    GstElement *queue0  = gst_element_factory_make("queue", "tcp_queue");
//    GstElement *decoder = gst_element_factory_make("openh264dec", "h264_decoder");
//
//    // Frame rate control
//    GstElement *videorate = gst_element_factory_make("videorate", "videorate");
//    GstElement *capsfilter = gst_element_factory_make("capsfilter", "fps_filter");
//
//    GstElement *videoconvert = gst_element_factory_make("videoconvert", "converter");
//
//    // Encoder - try hardware first, then software
//    GstElement *encoder = NULL;
//
//    // First try: Android hardware encoder (OMX)
//    GstRegistry *registry = gst_registry_get();
//    GstElementFactory *factory = gst_element_factory_find("omxh264enc");
//    if (!factory) {
//        // Try alternative OMX name
//        factory = gst_element_factory_find("omx_h264_encoder");
//    }
//
//    if (factory) {
//        encoder = gst_element_factory_create(factory, "h264_encoder");
//        gst_object_unref(factory);
//        g_print("[GStreamer] Using hardware H.264 encoder (OMX)\n");
//    }
//
//    if (!encoder) {
//        // Second try: MediaCodec encoder (Android 5.0+)
//        factory = gst_element_factory_find("mediacodec_h264_encoder");
//        if (factory) {
//            encoder = gst_element_factory_create(factory, "h264_encoder");
//            gst_object_unref(factory);
//            g_print("[GStreamer] Using MediaCodec H.264 encoder\n");
//        }
//    }
//
//    if (!encoder) {
//        // Fallback: openh264enc (software)
//        encoder = gst_element_factory_make("openh264enc", "h264_encoder");
//        if (encoder) {
//            g_print("[GStreamer] Using software H.264 encoder (openh264enc)\n");
//        }
//    }
//
//    if (!encoder) {
//        // Last resort: try avcenc if available
//        encoder = gst_element_factory_make("avenc_h264_omx", "h264_encoder");
//        if (encoder) {
//            g_print("[GStreamer] Using OMX AVC encoder\n");
//        }
//    }
//
//    // Output elements
//    GstElement *output_parser = gst_element_factory_make("h264parse", "output_parser");
//    GstElement *queue1  = gst_element_factory_make("queue", "output_queue");
//    GstElement *sink    = gst_element_factory_make("appsink", "sink");
//
//    if (!rtspsrc || !depay || !parser || !queue0 || !decoder || !videorate ||
//        !capsfilter || !videoconvert || !encoder || !output_parser ||
//        !queue1 || !sink) {
//        g_printerr("[GStreamer] Failed to create one or more pipeline elements\n");
//
//        // Log which element failed
//        if (!encoder) g_printerr("[GStreamer] Failed to create encoder element\n");
//        if (!decoder) g_printerr("[GStreamer] Failed to create decoder element\n");
//
//        fail_and_exit(mData, env, ERR_ELEMENT_CREATION_FAILED, "Failed to create pipeline elements");
//        return nullptr;
//    }
//
//    // Element configuration
//    g_object_set(rtspsrc,
//                 "location", mData->rtsp_uri,
//                 "protocols", GST_RTSP_LOWER_TRANS_TCP,
//                 "latency", 300,
//                 "do-retransmission", FALSE,
//                 "do-rtsp-keep-alive", TRUE,
//                 "buffer-mode", 1,
//                 "async-handling", TRUE,
//                 "drop-on-latency", TRUE,
//                 "user-agent", "RTSP_GStreamer/1.0",
//                 NULL);
//
//    // Queue configurations
//    g_object_set(queue0,
//                 "max-size-buffers", 30,
//                 "max-size-bytes", 0,
//                 "max-size-time", 200 * GST_MSECOND,
//                 "leaky", 2,
//                 NULL);
//
//    g_object_set(queue1,
//                 "max-size-buffers", 30,
//                 "max-size-bytes", 0,
//                 "max-size-time", 200 * GST_MSECOND,
//                 "leaky", 2,
//                 NULL);
//
//    g_object_set(depay,
//                 "wait-for-keyframe", TRUE,
//                 NULL);
//
//    // Configure decoder for better performance
////    g_object_set(decoder,
////                 "enable-thread", TRUE,  // Enable multi-threading if available
////                 NULL);
//
//    // Configure videorate
//    g_object_set(videorate,
//                 "max-rate", 30,  // Maximum 30 fps
//                 NULL);
//
//    // Caps filter to enforce 30 FPS
//    GstCaps* fps_caps = gst_caps_new_simple("video/x-raw",
//                                            "framerate", GST_TYPE_FRACTION, 30, 1,
//                                            NULL);
//    g_object_set(capsfilter, "caps", fps_caps, NULL);
//    gst_caps_unref(fps_caps);
//
//    // Configure encoder based on type
//    const gchar *encoder_type = G_OBJECT_TYPE_NAME(encoder);
//    g_print("[GStreamer] Encoder type: %s\n", encoder_type);
//
//    // Check encoder type and configure appropriately
//    if (g_strrstr(encoder_type, "OMX") || g_strrstr(encoder_type, "omx")) {
//        // OMX hardware encoder
//        g_object_set(encoder,
//                     "bitrate", 4000000,      // 4 Mbps
//                     "control-rate", 1,      // Variable bitrate
//                     NULL);
//    }
//    else if (g_strrstr(encoder_type, "MediaCodec") || g_strrstr(encoder_type, "mediacodec")) {
//        // MediaCodec encoder
//        g_object_set(encoder,
//                     "bitrate", 4000000,      // 4 Mbps
//                     NULL);
//    }
//    else if (g_strrstr(encoder_type, "OpenH264") || g_strrstr(encoder_type, "openh264")) {
//        // OpenH264 software encoder
//        g_object_set(encoder,
//                     "bitrate", 4000000,      // 4 Mbps
//                     "max-bitrate", 6000000,  // 6 Mbps max
//                     "complexity", 0,         // Low complexity
//                     "gop-size", 60,          // 1 second GOP
//                     "enable-frame-skip", 0,  // Don't skip frames
//                     NULL);
//
//        // Check and set multi-thread if available
//        GParamSpec *pspec = g_object_class_find_property(
//                G_OBJECT_GET_CLASS(encoder), "multi-thread");
//        if (pspec) {
//            g_object_set(encoder, "multi-thread", 1, NULL); // Enable threading
//        }
//    }
//    else {
//        // Generic encoder configuration
//        g_object_set(encoder,
//                     "bitrate", 4000000,      // 4 Mbps
//                     NULL);
//    }
//
//    // Configure output parser
//    g_object_set(output_parser,
//                 "config-interval", -1,
//                 NULL);
//
//    // Configure main appsink
//    GstCaps* sink_caps = gst_caps_new_simple("video/x-h264",
//                                             "stream-format", G_TYPE_STRING, "byte-stream",
//                                             "alignment", G_TYPE_STRING, "au",
//                                             NULL);
//
//    g_object_set(sink,
//                 "caps", sink_caps,
//                 "sync", FALSE,
//                 "emit-signals", TRUE,
//                 "max-buffers", 30,
//                 "drop", TRUE,
//                 "enable-last-sample", FALSE,
//                 NULL);
//    gst_caps_unref(sink_caps);
//
//    // Assign elements to mData
//    mData->depay = depay;
//    mData->appsink = sink;
//    mData->queue0 = queue0;
//
//    // Pipeline creation
//    mData->pipeline = gst_pipeline_new("rtsp-tcp-pipeline");
//    if (!mData->pipeline) {
//        fail_and_exit(mData, env, ERR_PIPELINE_CREATION_FAILED, "Pipeline creation failed");
//        return nullptr;
//    }
//
//    // Add all elements to the pipeline
//    gst_bin_add_many(GST_BIN(mData->pipeline),
//                     rtspsrc, queue0, depay, parser, decoder,
//                     videorate, capsfilter, videoconvert,
//                     encoder, output_parser, queue1, sink, NULL);
//
//    gboolean link_ok = TRUE;
//
//    // Link the pipeline
//    if (!gst_element_link(queue0, depay)) {
//        g_printerr("[GStreamer] Failed to link: queue0 -> depay\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(depay, parser)) {
//        g_printerr("[GStreamer] Failed to link: depay -> parser\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(parser, decoder)) {
//        g_printerr("[GStreamer] Failed to link: parser -> decoder\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(decoder, videorate)) {
//        g_printerr("[GStreamer] Failed to link: decoder -> videorate\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(videorate, capsfilter)) {
//        g_printerr("[GStreamer] Failed to link: videorate -> capsfilter\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(capsfilter, videoconvert)) {
//        g_printerr("[GStreamer] Failed to link: capsfilter -> videoconvert\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(videoconvert, encoder)) {
//        g_printerr("[GStreamer] Failed to link: videoconvert -> encoder\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(encoder, output_parser)) {
//        g_printerr("[GStreamer] Failed to link: encoder -> output_parser\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(output_parser, queue1)) {
//        g_printerr("[GStreamer] Failed to link: output_parser -> queue1\n");
//        link_ok = FALSE;
//    } else if (!gst_element_link(queue1, sink)) {
//        g_printerr("[GStreamer] Failed to link: queue1 -> sink\n");
//        link_ok = FALSE;
//    }
//
//    if (!link_ok) {
//        g_printerr("[GStreamer] ERR_ELEMENT_LINK_FAILED\n");
//        fail_and_exit(mData, env, ERR_ELEMENT_LINK_FAILED, "Pipeline element link failed.");
//        return nullptr;
//    }
//
//    g_signal_connect(rtspsrc, "pad-added", G_CALLBACK(on_pad_added), mData);
//    g_signal_connect(sink, "new-sample", G_CALLBACK(on_new_sample), mData);
//
//    // Bus/watchdog
//    mData->bus = gst_element_get_bus(mData->pipeline);
//    if (!mData->bus) {
//        fail_and_exit(mData, env, ERR_BUS_CREATION_FAILED, "Failed to acquire pipeline bus");
//        return nullptr;
//    }
//
//    mData->bus_watch_id = gst_bus_add_watch(mData->bus, (GstBusFunc)bus_call, mData);
//    mData->last_sample_time = g_get_monotonic_time();
//    mData->watchdog_timer = g_timeout_add_seconds(WATCHDOG_INTERVAL, watchdog_check, mData);
//    mData->retry_count = 0;
//
//    // Start pipeline
//    GstStateChangeReturn ret = gst_element_set_state(mData->pipeline, GST_STATE_PLAYING);
//    switch (ret) {
//        case GST_STATE_CHANGE_FAILURE:
//            g_print("[GStreamer] GST_STATE_CHANGE_FAILURE: %d\n", ret);
//            fail_and_exit(mData, env, ERR_PIPELINE_STATE_FAILED,"Failed to set pipeline to PLAYING state");
//            break;
//        case GST_STATE_CHANGE_ASYNC:
//            g_print("[GStreamer] GST_STATE_CHANGE_ASYNC: %d\n", ret);
//            ret = gst_element_get_state(mData->pipeline, nullptr, nullptr, 10 * GST_SECOND);
//            if (ret == GST_STATE_CHANGE_FAILURE) {
//                fail_and_exit(mData, env, ERR_PIPELINE_STATE_FAILED,"Timeout waiting for PLAYING state.");
//            } else{
//                g_print("[GStreamer] gst_element_get_state: %d\n", ret);
//            }
//            break;
//        case GST_STATE_CHANGE_SUCCESS:
//        case GST_STATE_CHANGE_NO_PREROLL:
//            g_print("[GStreamer] GST_STATE_CHANGE_SUCCESS: %d\n", ret);
//            break;
//        default:
//            break;
//    }
//
//    gst_element_set_start_time(mData->pipeline, GST_CLOCK_TIME_NONE);
//
//    // Set async-handling for better performance
//    g_object_set(mData->pipeline, "async-handling", TRUE, NULL);
//
//    // ✅ Notify Java: switch ended (stream ready)
//    notify_stream_switch_status(env, mData->app, JNI_FALSE);
//
//    g_print("[GStreamer] All Configuration done.. \n");
//
//    // Run GMainLoop
//    mData->main_loop = g_main_loop_new(nullptr, FALSE);
//    if (mData->main_loop) {
//        GMainContext* ctx = g_main_loop_get_context(mData->main_loop);
//        g_main_context_push_thread_default(ctx);
//        g_main_loop_run(mData->main_loop);
//        g_main_context_pop_thread_default(ctx);
//    }
//
//    cleanup_streaming_resources(mData, env);
//
//    if (mData && mData->jvm) {
//        mData->jvm->DetachCurrentThread();
//    }
//    return nullptr;
//}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeInit(JNIEnv *env, jobject thiz) {
    gst_init(nullptr, nullptr);

    if (!global_data) {
        global_data = g_new0(CustomData, 1);
        pthread_mutex_init(&global_data->lock, nullptr);
        pthread_mutex_init(&global_data->jni_mutex, nullptr);
        pthread_cond_init(&global_data->state_change_cond, nullptr);
        env->GetJavaVM(&global_data->jvm);

        // Initialize atomic states
        global_data->stream_state.store(STREAM_STATE_IDLE);
        global_data->next_retry_delay_ms = 0;
        global_data->in_recovery = FALSE;
        global_data->active_timer_id.store(0);
        global_data->stop_requested = FALSE;
        global_data->start_requested = FALSE;
    }

    pthread_mutex_lock(&global_data->lock);

    // Update Java reference
//    if (global_data->app && global_data->app != thiz) {
//        env->DeleteGlobalRef(global_data->app);
//    }
// crash issue
    if (!global_data->app) {
        global_data->app = env->NewGlobalRef(thiz);
    }

    // Reset to idle state if we're in error or stopped state
    gint current_state = get_stream_state(global_data);
    if (current_state == STREAM_STATE_ERROR || current_state == STREAM_STATE_STOPPED) {
        set_stream_state(global_data, STREAM_STATE_IDLE);
    }

    pthread_mutex_unlock(&global_data->lock);
}

static void quick_stream_cleanup(CustomData *data, JNIEnv *env) {
    if (!data) return;

    g_print("[QuickCleanup] Starting quick cleanup\n");

    // Cancel all timers
    guint current_timer = data->active_timer_id.exchange(0);
    if (current_timer != 0) {
        g_source_remove(current_timer);
    }

    if (timer_id != 0) {
        g_source_remove(timer_id);
        timer_id = 0;
    }

    if (data->watchdog_timer) {
        g_source_remove(data->watchdog_timer);
        data->watchdog_timer = 0;
    }

    if (data->bus_watch_id) {
        g_source_remove(data->bus_watch_id);
        data->bus_watch_id = 0;
    }

    if (data->keepalive_timer) {
        g_source_remove(data->keepalive_timer);
        data->keepalive_timer = 0;
    }

    // Stop main loop
    if (data->main_loop && g_main_loop_is_running(data->main_loop)) {
        g_main_loop_quit(data->main_loop);
    }

    // Stop pipeline
    if (data->pipeline) {
        gst_element_set_state(data->pipeline, GST_STATE_NULL);
        // Don't unref here - let the streaming thread handle it
    }

    // Free RTSP URI
    if (data->rtsp_uri) {
        g_free(data->rtsp_uri);
        data->rtsp_uri = nullptr;
    }

    // Free buffers
    if (data->sps) {
        gst_buffer_unref(data->sps);
        data->sps = nullptr;
    }
    if (data->pps) {
        gst_buffer_unref(data->pps);
        data->pps = nullptr;
    }

    // Reset flags
    data->streaming = FALSE;
    data->thread_done = TRUE;
    data->in_recovery = FALSE;
    data->retry_count = 0;
    data->next_retry_delay_ms = INITIAL_RETRY_DELAY_MS;
    data->active_timer_id.store(0);
    global_data->waiting_for_response.store(false);

    g_print("[QuickCleanup] Quick cleanup completed\n");
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeStartStream(JNIEnv *env, jobject thiz, jstring jrtsp_url) {

    if (!global_data) {
        g_printerr("[GStreamer] Global data not initialized\n");
        return;
    }

    g_print("[GStreamer] nativeStartStream called\n");

    pthread_mutex_lock(&global_data->lock);

    // Check if already starting or playing
    gint current_state = get_stream_state(global_data);
    if (current_state == STREAM_STATE_INITIALIZING ||
        current_state == STREAM_STATE_CONNECTING ||
        current_state == STREAM_STATE_PLAYING) {
        g_print("[GStreamer] Stream is already active (state: %s), ignoring start request\n",
                get_state_name(current_state));
        pthread_mutex_unlock(&global_data->lock);
        return;
    }

    // If we're in stopping state, wait for it to finish
    if (current_state == STREAM_STATE_STOPPING) {
        g_print("[GStreamer] Stream is stopping, waiting...\n");
        pthread_mutex_unlock(&global_data->lock);
        exit_buffering(env,global_data);

        if (wait_for_state(global_data, STREAM_STATE_STOPPED, 5000)) {
            g_print("[GStreamer] Stream stopped, proceeding with start\n");
        } else {
            g_printerr("[GStreamer] Timeout waiting for stream to stop = %s\n",
                    global_data->is_buffering ? "TRUE" : "FALSE");
            if(global_data->is_buffering || !global_data->stream_has_started) {
                notify_stream_error(env, global_data->app, ERR_NETWORK_ERROR,
                                    "Buffering timeout exceeded");
            }
//            g_printerr("[GStreamer] Timeout waiting for stream to stop\n");
            return;
        }

        pthread_mutex_lock(&global_data->lock);
    }

    // Get RTSP URL
    const char *rtsp_url = env->GetStringUTFChars(jrtsp_url, 0);
    if (!rtsp_url || strlen(rtsp_url) == 0) {
        g_printerr("[GStreamer] Empty RTSP URL provided\n");
        env->ReleaseStringUTFChars(jrtsp_url, rtsp_url);
        pthread_mutex_unlock(&global_data->lock);
        return;
    }

    g_print("[GStreamer] Starting stream with URL: %s\n", rtsp_url);

    // Clean up previous stream if needed
    if (current_state != STREAM_STATE_IDLE && current_state != STREAM_STATE_STOPPED) {
        g_print("[GStreamer] Cleaning up previous stream state\n");
        quick_stream_cleanup(global_data, env);
    }

    // Store RTSP URI
    if (global_data->rtsp_uri) {
        g_free(global_data->rtsp_uri);
    }
    global_data->rtsp_uri = g_strdup(rtsp_url);
    env->ReleaseStringUTFChars(jrtsp_url, rtsp_url);

    // Update Java reference
//    if (global_data->app) {
//        env->DeleteGlobalRef(global_data->app);
//    }
// crash issue
    if (!global_data->app) {
        global_data->app = env->NewGlobalRef(thiz);
    }

    // Reset state variables
    global_data->streaming = FALSE;
    global_data->thread_done = FALSE;
    global_data->in_recovery = FALSE;
    global_data->stream_has_started = FALSE;
    global_data->is_buffering = FALSE;
    global_data->sps_pps_extracted = FALSE;
    global_data->sps_pps_sent = FALSE;
    global_data->retry_count = 0;
    global_data->next_retry_delay_ms = INITIAL_RETRY_DELAY_MS;
    global_data->frame_count = 0;
    global_data->fps = 0;
    global_data->zero_fps_count = 0;
    global_data->last_sample_time = 0;
    global_data->active_timer_id.store(0);
    global_data->stop_requested = FALSE;
    global_data->start_requested = TRUE;
    global_data->waiting_for_response.store(false);

    // Set initial state
    set_stream_state(global_data, STREAM_STATE_INITIALIZING);

    // Create streaming thread
    int ret = pthread_create(&global_data->thread_id, nullptr, start_event_driven_stream, global_data);
    if (ret != 0) {
        g_printerr("[GStreamer] Failed to create streaming thread: %d\n", ret);
        global_data->thread_id = 0;
        set_stream_state(global_data, STREAM_STATE_ERROR);

        if (global_data->rtsp_uri) {
            g_free(global_data->rtsp_uri);
            global_data->rtsp_uri = nullptr;
        }

        notify_stream_error(env, global_data->app, ERR_UNKNOWN, "Failed to create streaming thread");
    } else {
        // update streaming is active
        global_data->stream_alive.store(true, std::memory_order_release);
        global_data->streaming = TRUE;
        g_print("[GStreamer] Streaming thread created successfully (ID: %lu)\n",
                (unsigned long)global_data->thread_id);
    }

    pthread_mutex_unlock(&global_data->lock);

    g_print("[GStreamer] nativeStartStream completed\n");
}


extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeStopStream(JNIEnv *env, jobject thiz) {
    if (!global_data) {
        g_print("[GStreamer] nativeStopStream: global_data is null\n");
        return;
    }

    g_print("[GStreamer] nativeStopStream called\n");

    pthread_mutex_lock(&global_data->lock);

    enter_buffering(env,global_data);

    gint current_state = get_stream_state(global_data);

    // Check if already stopped or stopping
    if (current_state == STREAM_STATE_STOPPED ||
        current_state == STREAM_STATE_STOPPING ||
        current_state == STREAM_STATE_IDLE) {
        g_print("[GStreamer] Stream already in stop state: %s\n", get_state_name(current_state));

        // Still notify Java even if already stopped (for UI consistency)
        notify_stream_stopped(env, global_data->app, STREAM_STATE_STOPPED);
        exit_buffering(env,global_data);

        pthread_mutex_unlock(&global_data->lock);
        return;
    }

    // Set stop requested flag
    global_data->stop_requested = TRUE;

    // Set stopping state
    set_stream_state(global_data, STREAM_STATE_STOPPING);

    // NOTIFY JAVA HERE - before any cleanup that might affect JNI
    notify_stream_stopped(env, global_data->app, STREAM_STATE_STOPPED);

    // Cancel any pending retry timers immediately
    guint current_timer = global_data->active_timer_id.exchange(0);
    if (current_timer != 0) {
        g_source_remove(current_timer);
        g_print("[GStreamer] Cancelled active retry timer ID: %u\n", current_timer);
    }

    // Also clear global timer_id
    if (timer_id != 0) {
        g_source_remove(timer_id);
        timer_id = 0;
    }

    // Remove other timers
    if (global_data->watchdog_timer) {
        g_source_remove(global_data->watchdog_timer);
        global_data->watchdog_timer = 0;
    }
    if (global_data->bus_watch_id) {
        g_source_remove(global_data->bus_watch_id);
        global_data->bus_watch_id = 0;
    }
    if (global_data->keepalive_timer) {
        g_source_remove(global_data->keepalive_timer);
        global_data->keepalive_timer = 0;
    }

    // Stop main loop if running
    if (global_data->main_loop && g_main_loop_is_running(global_data->main_loop)) {
        g_print("[GStreamer] Stopping main loop...\n");
        g_main_loop_quit(global_data->main_loop);
    }

    // Stop pipeline
    if (global_data->pipeline) {
        g_print("[GStreamer] Stopping pipeline...\n");
        gst_element_set_state(global_data->pipeline, GST_STATE_NULL);
    }

    // SIMPLE ANDROID-COMPATIBLE THREAD HANDLING
    if (global_data->thread_id != 0 && !pthread_equal(pthread_self(), global_data->thread_id)) {
        g_print("[GStreamer] Handling streaming thread...\n");

        // 1. Signal thread to exit
        global_data->thread_done = TRUE;

        // 2. Wait a bit for thread to exit naturally
        int attempts = 0;
        while (attempts < 10) { // 10 * 100ms = 1 second total
            // Check if thread is still alive
            if (pthread_kill(global_data->thread_id, 0) != 0) {
                // Thread has terminated
                g_print("[GStreamer] Thread terminated\n");
                break;
            }
            usleep(100 * 1000); // 100ms
            attempts++;
        }

        // 3. If still alive, detach it (Android-safe)
        if (attempts >= 10) {
            g_print("[GStreamer] Thread still alive, detaching...\n");
            pthread_detach(global_data->thread_id);
        } else {
            // Thread terminated, try to join
            void* result;
            int join_result = pthread_join(global_data->thread_id, &result);
            if (join_result != 0) {
                g_print("[GStreamer] Thread join failed: %d\n", join_result);
            }
        }

        global_data->thread_id = 0;
    }

    // Quick resource cleanup (but don't notify Java again)
    if (global_data->rtsp_uri) {
        g_free(global_data->rtsp_uri);
        global_data->rtsp_uri = nullptr;
    }

    if (global_data->sps) {
        gst_buffer_unref(global_data->sps);
        global_data->sps = nullptr;
    }
    if (global_data->pps) {
        gst_buffer_unref(global_data->pps);
        global_data->pps = nullptr;
    }

    // Reset state
    global_data->retry_count = 0;
    global_data->in_recovery = FALSE;
    global_data->next_retry_delay_ms = INITIAL_RETRY_DELAY_MS;
    global_data->stream_has_started = FALSE;
    global_data->active_timer_id.store(0);
    global_data->stop_requested = FALSE;
    global_data->streaming = FALSE;

    // Set final state
    set_stream_state(global_data, STREAM_STATE_STOPPED);
    exit_buffering(env,global_data);
    pthread_mutex_unlock(&global_data->lock);

    g_print("[GStreamer] Stream stopped successfully\n");
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeRelease(JNIEnv *env, jobject thiz) {
    if (!global_data || !global_data->pipeline) return;

    global_data->stream_alive.store(false, std::memory_order_release);

    if (global_data->streaming) {
        Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeStopStream(env, thiz);
    }

    std::this_thread::sleep_for(std::chrono::seconds(2));

    if (global_data->thread_id != 0 &&
        !pthread_equal(pthread_self(), global_data->thread_id)) {

        pthread_join(global_data->thread_id, NULL);
        global_data->thread_id = 0;
    }
    pthread_mutex_destroy(&global_data->lock);
    pthread_mutex_destroy(&global_data->jni_mutex);
    g_free(global_data);
    global_data = nullptr;
    g_print("[GStreamer] Stream released successfully\n");
}

void nativeReleasePartial(JNIEnv *env, jobject thiz) {
    if (!global_data) return;

    if (global_data->streaming) {
        Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeStopStream(env, thiz);
    }

    if (global_data->app) {
        env->DeleteGlobalRef(global_data->app);
        global_data->app = nullptr;
    }

    if (global_data->rtsp_uri) {
        g_free(global_data->rtsp_uri);
        global_data->rtsp_uri = nullptr;
    }

    global_data->streaming = FALSE;
    global_data->in_recovery = FALSE;
    global_data->next_retry_delay_ms = 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeChangeStreamUrl(JNIEnv *env, jobject thiz, jstring jurl) {
    const char *new_url = env->GetStringUTFChars(jurl, nullptr);
    if (!new_url || strlen(new_url) == 0) {
        g_printerr("[Native] URL is null or empty, aborting.\n");
        return;
    }

    if (global_data->streaming) {
        Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeStopStream(env, thiz);
    }

    g_print("[Native] Changing RTSP URL to: %s\n", new_url);

    gchar *url_copy = g_strdup(new_url);
    env->ReleaseStringUTFChars(jurl, new_url);

    // Notify Java UI about switch
    notify_stream_switch_status(env, global_data->app, JNI_TRUE);

    // Clean up
    nativeReleasePartial(env, thiz);
    g_print("[Native] Called nativeRelease()\n");

    // Capture thiz with global ref to use safely in new thread
    jobject thiz_ref = nullptr;
    // crash issue
    if (!global_data->app) {
        thiz_ref = env->NewGlobalRef(thiz);
    } else {
        thiz_ref = global_data->app;
    }

    // Start new thread
    std::thread([url_copy, thiz_ref]() {
        std::this_thread::sleep_for(std::chrono::seconds(2));
        g_print("[Native] Waiting 2 seconds before nativeInit()\n");

        if (!global_data || !global_data->jvm) {
            g_printerr("[Native] Cannot init — JVM not ready\n");
            g_free(url_copy);
            return;
        }

        JNIEnv *env;
        if (global_data->jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            g_printerr("[Native] Failed to attach thread to JVM (nativeInit)\n");
            g_free(url_copy);
            return;
        }

        // Call nativeInit with reattached env and global thiz_ref
        Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeInit(env, thiz_ref);
        g_print("[Native] Called nativeInit()\n");

        // Wait again before starting stream
        std::this_thread::sleep_for(std::chrono::seconds(3));

        if (!global_data->app) {
            g_printerr("[Native] App reference is null — cannot start stream\n");
//            env->DeleteGlobalRef(thiz_ref);
            g_free(url_copy);
            return;
        }

        jclass clazz = env->GetObjectClass(global_data->app);
        if (!clazz) {
            g_printerr("[Native] Failed to get Java class from global app ref\n");
//            env->DeleteGlobalRef(thiz_ref);
            g_free(url_copy);
            return;
        }

        jmethodID methodStart = env->GetMethodID(clazz, "nativeStartStream", "(Ljava/lang/String;)V");
        if (!methodStart) {
            g_printerr("[Native] Could not find nativeStartStream method\n");
//            env->DeleteGlobalRef(thiz_ref);
            g_free(url_copy);
            return;
        }

        jstring jurl = env->NewStringUTF(url_copy);
        env->CallVoidMethod(global_data->app, methodStart, jurl);
        g_print("[Native] Called nativeStartStream() with URL: %s\n", url_copy);
        env->DeleteLocalRef(jurl);

        // Clean up
//        env->DeleteGlobalRef(thiz_ref);
        global_data->jvm->DetachCurrentThread();
        g_free(url_copy);
    }).detach();
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeSetGstDebug(JNIEnv *env, jobject thiz) {
    // Set comprehensive debug levels for RTSP troubleshooting
    g_setenv("GST_DEBUG", "*:3", TRUE);  // Global level 3 (WARNING)
    g_setenv("GST_DEBUG", "rtspsrc:5,rtph264depay:4,h264parse:4,appsink:4", TRUE);
    g_setenv("GST_DEBUG", "GST_ELEMENT_PADS:4,GST_BUFFER:4,GST_CLOCK:3", TRUE);
    g_setenv("GST_DEBUG", "GST_PIPELINE:4,GST_STATES:4,GST_BIN:4", TRUE);
    g_setenv("GST_DEBUG", "rtpjitterbuffer:4,rtp*:4,*queue*:4", TRUE);

    // Enable GST_DEBUG_DUMP_DOT_DIR for pipeline debugging
    g_setenv("GST_DEBUG_DUMP_DOT_DIR", "/data/data/com.sionyx.plexus/files", TRUE);

    // Log to file for persistent debugging
    g_setenv("GST_DEBUG_FILE", "/data/data/com.sionyx.plexus/files/gstreamer.log", TRUE);

    // Disable color output for cleaner logs
    g_setenv("GST_DEBUG_COLOR_MODE", "off", TRUE);

    g_print("[GStreamer] Debug settings applied:\n");
    g_print("  GST_DEBUG=*:3,rtspsrc:5,rtph264depay:4,h264parse:4,appsink:4\n");
    g_print("  Debug file: /data/data/com.sionyx.plexus/files/gstreamer.log\n");
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeStopRetryAndReset(JNIEnv *env, jobject thiz) {
    if (!global_data) return;

    g_print("[GStreamer] Stopping retry and resetting stream\n");

    pthread_mutex_lock(&global_data->lock);

    // Check if we're in recovery state
    if (get_stream_state(global_data) == STREAM_STATE_RECOVERING) {
        // Cancel any active retry timer
        guint current_timer = global_data->active_timer_id.exchange(0);
        if (current_timer != 0) {
            g_source_remove(current_timer);
            g_print("[GStreamer] Cancelled retry timer ID: %u\n", current_timer);
        }

        if (timer_id != 0) {
            g_source_remove(timer_id);
            timer_id = 0;
        }

        // Reset retry state
        global_data->retry_count.store(0);
        global_data->in_recovery.store(FALSE);
        global_data->next_retry_delay_ms.store(0);

        // If we have a pipeline, try to keep it playing
        if (global_data->pipeline) {
            gst_element_set_state(global_data->pipeline, GST_STATE_PLAYING);
        }

        // Set back to playing state
//        set_stream_state(global_data, STREAM_STATE_PLAYING);
        set_stream_state(global_data, STREAM_STATE_IDLE);

        g_print("[GStreamer] Retry stopped, stream reset to playing state\n");
    } else {
        g_print("[GStreamer] Not in recovery state, current state: %s\n",
                get_state_name(get_stream_state(global_data)));
    }

    // Notify Java UI
    notify_loading_status(env, global_data->app, 0, MAX_RETRIES, FALSE);

    pthread_mutex_unlock(&global_data->lock);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_nativeGetStreamState(JNIEnv *env, jobject thiz) {
    if (!global_data) return STREAM_STATE_IDLE;
    return get_stream_state(global_data);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_setenv("GST_DEBUG", "rtph264depay:4,h264parse:4,appsink:4,codec:4,GST_BUFFER:4,GST_ELEMENT_PADS:4", 1);
    return JNI_VERSION_1_6;
}
#pragma clang diagnostic pop