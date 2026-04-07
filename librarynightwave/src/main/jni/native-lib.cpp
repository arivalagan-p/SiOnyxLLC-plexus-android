#include <jni.h>
#include <string>
#include <string.h>
#include <cstring>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <unistd.h>

#include <queue>
#include <mutex>
#include <thread>
#include <poll.h>
#include <fcntl.h>
#include <sys/time.h>
#include <errno.h>

#include <vector>
#include <algorithm>
#include <iostream>
#include <chrono>
#include <set>
#include <utility>
#include <unistd.h> // Include for usleep function
#include <time.h>
#include <map>


uint16_t calc_crc16(const void *data, uint32_t size) {
    uint32_t sum = 0;
    uint32_t swap = 0;
    uint32_t count = size;
    const uint8_t *pdata08 = ((uint8_t *) data);

    // Check for memory alignment
    if (((intptr_t) data) & 1) {
        sum = (*pdata08) << 8;
        swap = 1;
        pdata08++;
        count--;
    }

    // min summing loop
    while (count > 1) {
        sum += *((uint16_t *) pdata08);
        count -= 2;
        pdata08 += 2;
    }

    // Add left-over byte, if any
    if (count > 0) {
        sum += (*pdata08);
    }

    // Fold 32-bit sum to 16 bits
    while (sum >> 16) {
        sum = (sum & 0xFFFF) + (sum >> 16);
    }

    if (swap) {
        sum = ((sum & 0xff00) >> 8) + ((sum & 0x00ff) << 8);
    }

    // exit function
    return (~sum);
}

uint32_t sccp_calc_crc32(const void *data, uint32_t size) {
    size_t i = 0;
    uint32_t r = ~0;
    uint32_t t = 0;
    const uint8_t *p_data = ((const uint8_t *) data);
    const uint8_t *end = (p_data + size);

    while (p_data < end) {
        r ^= (*p_data);
        p_data++;
        for (i = 0; i < 8; i++) {
            t = ~((r & 1) - 1);
            r = (r >> 1) ^ (0xEDB88320 & t);
        }
    }

    return (~r);
}

extern "C"
uint8_t *as_unsigned_char_array(jbyteArray array, JNIEnv *env) {
    int len = env->GetArrayLength(array);
    uint8_t *buf = new uint8_t[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return buf;
}

extern "C" JNIEXPORT jshort JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_calculateCRC16FromJNI(
        JNIEnv *env, jobject thiz, jbyteArray bar, jint size) {
    uint8_t *data = as_unsigned_char_array(bar, env);
    uint16_t crc16 = calc_crc16(data, size);
    return crc16;
}

extern "C" JNIEXPORT jshort JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_calculateCRC16FromJNI(JNIEnv *env,
                                                                                    jobject thiz,
                                                                                    jbyteArray bar,
                                                                                    jint size) {
    uint8_t *data = as_unsigned_char_array(bar, env);
    uint16_t crc16 = calc_crc16(data, size);
    return crc16;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_calculateCRC32FromJNI(
        JNIEnv *env, jobject thiz, jbyteArray bytes, jint size) {
    uint8_t *data = as_unsigned_char_array(bytes, env);
    uint32_t crc32 = sccp_calc_crc32(data, size);
    return crc32;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dome_librarynightwave_model_repository_TCPRepository_calculateCRC32FromJNI(JNIEnv *env,
                                                                                    jobject thiz,
                                                                                    jbyteArray bytes,
                                                                                    jint size) {
    uint8_t *data = as_unsigned_char_array(bytes, env);
    uint32_t crc32 = sccp_calc_crc32(data, size);
    return crc32;
}


/*TCP SOCKET Connection*/
int tcp_keep_idle = 300; // Initial time (in seconds) until the first keep-alive probe is sent
int tcp_keep_interval = 10; // Interval (in seconds) between successive keep-alive probes
int tcp_keep_count = 5; // Number of unacknowledged keep-alive probes before considering the connection dead
int tcp_socket_descriptor;
int bytes_received;
const int MAX_TCP_BUFFER_SIZE = 1024;
unsigned char buffer[MAX_TCP_BUFFER_SIZE];
JavaVM *g_vm = nullptr;
jobject g_obj = nullptr;


/*UDP SOCKET Connection*/
#define RTP_HEADER_SIZE 12
#define H264_PAYLOAD_TYPE 96
#define H264_BUFFER_SIZE 70
#define END_OF_THE_FRAME_SIZE 1024
#define IDR_TYPE 5 // NAL type for IDR
#define SPS_TYPE 7 // NAL type for SPS
#define PPS_TYPE 8 // NAL type for PPS
#define POLL_TIMEOUT_MS_OPSIN 1
#define POLL_TIMEOUT_MS_NW 20
#define CIRCULAR_BUFFER_SIZE 65536 * 2

// Define a comparison function for sorting based on the key
struct CompareKey {
    bool operator()(const std::pair<uint16_t, std::vector<uint8_t>> &a,const std::pair<uint16_t, std::vector<uint8_t>> &b) const {
        return a.first < b.first;
    }
};

const jsize MAX_UDP_BUFFER_SIZE = 1080;
int udp_socket_descriptor = -1;
int udp_received_length;
struct sockaddr_in udp_si_me, udp_si_other;
socklen_t udp_slen = sizeof(udp_si_me);
char udp_buffer[MAX_UDP_BUFFER_SIZE];
static jbyteArray udp_received_byte_array;

std::set<std::pair<uint16_t, std::vector<uint8_t>>, CompareKey> packetBuffer;
std::set<std::pair<uint16_t, std::vector<uint8_t>>, CompareKey> tempPacketBuffer;
std::vector<uint8_t> spsData, ppsData, frameBuffer;
std::vector<uint8_t> packetData;
std::vector<uint8_t> *payloadBuffer = nullptr;
uint8_t payloadType;
uint16_t currentSequenceNumber, previousSeqNum = 0;
uint16_t lastSequenceNumber = 0;
bool foundStartOfTheFrame = false;
bool isFrameMissing = false;
jbyteArray spsArray, ppsArray, frameArray;

void processFrame(JNIEnv *env, jobject obj, jmethodID frameCallback);

void insertAndPaint(JNIEnv *env, jobject obj, jmethodID frameCallback);

void handleUdpError(JNIEnv *env, jobject obj, int errorCode, const char *errorMessage) {
    if (obj == nullptr) {
        // Handle null object case
        return;
    }

    jclass cls = env->GetObjectClass(obj);
    if (cls == nullptr) {
        // Handle failure to get class
        return;
    }

    jmethodID mid = env->GetMethodID(cls, "handleUdpError", "(ILjava/lang/String;)V");
    if (mid != nullptr) {
        jstring errorString = (errorMessage != nullptr) ? env->NewStringUTF(errorMessage) : nullptr;
        env->CallVoidMethod(obj, mid, errorCode, errorString);
        if (errorString != nullptr) {
            env->DeleteLocalRef(errorString);
        }
    }
    env->DeleteLocalRef(cls);
}

// Function to open UDP socket
bool openUDPSocket() {
    if ((udp_socket_descriptor = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Socket Error");
        return false;
    }

    // Set socket receive timeout
    struct timeval timeout;
    timeout.tv_sec = 0;       // 10 ms is less than one second, so this part is 0 seconds
    timeout.tv_usec = 15000; // 15 ms converted to microseconds
    if (setsockopt(udp_socket_descriptor, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Setsockopt Error");
        close(udp_socket_descriptor);
        udp_socket_descriptor = -1;
        return false;
    }

    // Enable keep-alive
    int optval = 1;
    if (setsockopt(udp_socket_descriptor, SOL_SOCKET, SO_KEEPALIVE, &optval, sizeof(optval)) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Setsockopt Keep-Alive Error");
        close(udp_socket_descriptor);
        udp_socket_descriptor = -1;
        return false;
    }

    // Enable socket reuse
    int optval1 = 1;
    if (setsockopt(udp_socket_descriptor, SOL_SOCKET, SO_REUSEADDR, &optval1, sizeof(optval1)) <
        0) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Setsockopt SO_REUSEADDR Error");
        close(udp_socket_descriptor);
        udp_socket_descriptor = -1;
        return false;
    }

    int recvBufSize = 65536; // Adjust the buffer size as needed
    if (setsockopt(udp_socket_descriptor, SOL_SOCKET, SO_RCVBUF, &recvBufSize,
                   sizeof(recvBufSize)) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Error setting receive buffer size");
        return false;
    }

    // Prepare the sockaddr_in structure
    memset((char *) &udp_si_me, 0, sizeof(udp_si_me));
    udp_si_me.sin_family = AF_INET;
    udp_si_me.sin_port = htons(5004);
    udp_si_me.sin_addr.s_addr = htonl(INADDR_ANY);

    // Bind Socket
    if (bind(udp_socket_descriptor, (struct sockaddr *) &udp_si_me, sizeof(udp_si_me)) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Bind socket to port %d",udp_socket_descriptor);
        close(udp_socket_descriptor);
        udp_socket_descriptor = -1;
        return false;
    }
    return true;
}

// Function to close UDP socket
void closeUDPSocket() {
    try{
        if (udp_socket_descriptor != -1) {
            __android_log_print(ANDROID_LOG_ERROR, "closeUDPSocket", "Close UDP Socket");
            close(udp_socket_descriptor);
            udp_socket_descriptor = -1;

/*Commented due to fatel error-- Need to find a way to handle this error*/
//            if(!tempPacketBuffer.empty()){
//                tempPacketBuffer.clear();
//            }
//            if(!packetBuffer.empty()){
//                packetBuffer.clear();
//            }

        } else {
            __android_log_print(ANDROID_LOG_ERROR, "closeUDPSocket","Close UDP Socket -- Already Closed");
        }
    }catch (const std::exception& e) {
        // Catch all exceptions and handle them here
        __android_log_print(ANDROID_LOG_ERROR, "closeUDPSocket", "Caught an exception %s",e.what());
    }

}

void handleTcpError(JNIEnv *env, jobject obj, int errorCode, const char *errorMessage) {
    try {
        if (obj == nullptr) {
            // Handle null object case
            return;
        }

        jclass cls = env->GetObjectClass(obj);
        if (cls == nullptr) {
            // Handle failure to get class
            __android_log_print(ANDROID_LOG_ERROR, "JNI Error Handler",
                                "Failed to get object class");
            return;
        }

        jmethodID mid = env->GetMethodID(cls, "handleTcpError", "(ILjava/lang/String;)V");
        if (mid != nullptr) {
            jstring jErrorMessage = (errorMessage != nullptr) ? env->NewStringUTF(errorMessage)
                                                              : nullptr;
            env->CallVoidMethod(obj, mid, errorCode, jErrorMessage);
            if (jErrorMessage != nullptr) {
                env->DeleteLocalRef(jErrorMessage);
            }
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "JNI Error Handler",
                                "Failed to find handleTcpError method");
        }
        env->DeleteLocalRef(cls);
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        __android_log_print(ANDROID_LOG_ERROR, "handleTcpError", "Exception: %s", e.what());
    } catch (...) {
        // Catch all exceptions and handle them here
        __android_log_print(ANDROID_LOG_ERROR, "receiveThread", "Caught an exception");
    }
}

void resetSocketOption() {
    try {
        int idle = 2;
        int interval = 1;
        int count = 1;
        int reuse_val = 1;
        struct timeval timeout;
        timeout.tv_sec = 1;  // 1 seconds timeout
        timeout.tv_usec = 0;
        struct linger so_linger;
        so_linger.l_onoff = 1;
        so_linger.l_linger = 0;

        if (setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_KEEPIDLE, &idle, sizeof(idle)) ==-1 ||
            setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_KEEPINTVL, &interval,sizeof(interval)) == -1 ||
            setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_KEEPCNT, &count, sizeof(count)) ==-1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket","Set the keep-alive parameters Failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return; // Error setting keep-alive parameters
        }

        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_RCVTIMEO, (char *) &timeout,sizeof(timeout)) < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "setTcpSocketOption", "Set SO_RCVTIMEO Failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return; // Error setting keep-alive parameters
        }
        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_SNDTIMEO, (char *) &timeout,sizeof(timeout)) < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "setTcpSocketOption", "Set SO_SNDTIMEO Failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return; // Error setting keep-alive parameters
        }

        // Enable socket address reuse
        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_REUSEADDR, &reuse_val,sizeof(reuse_val)) == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket","Enable socket address reuse failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return; // Error enabling socket address reuse
        }

        /*force a connection to close immediately (sending a RST) by setting SO_LINGER with a zero timeout before calling close()*/
        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_LINGER, &so_linger,
                       sizeof(so_linger)) < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "setTcpSocketOption", "Set SO_LINGER Failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return; // Error setting keep-alive parameters
        }
        __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "All Success");
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        tcp_socket_descriptor = -1;
        __android_log_print(ANDROID_LOG_ERROR, "setTcpSocketOption", "Exception: %s", e.what());
    } catch (...) {
        // Catch all exceptions and handle them here
        tcp_socket_descriptor = -1;
        __android_log_print(ANDROID_LOG_ERROR, "setTcpSocketOption", "Caught an exception");
    }
}

int disconnectTcpSocket() {
    if (tcp_socket_descriptor != -1) {
        resetSocketOption();
        shutdown(tcp_socket_descriptor, SHUT_RDWR);
        int res = close(tcp_socket_descriptor);
        if (res == 0) {
            // Socket closed successfully
            tcp_socket_descriptor = -1;
            __android_log_print(ANDROID_LOG_ERROR, "disconnectTcpSocket",
                                "TCP Socket Disconnected");
            return 1;
        } else {
            // Error occurred while closing the socket
            int error_code = errno;
            const char *error_message = strerror(error_code);
            __android_log_print(ANDROID_LOG_ERROR, "disconnectTcpSocket",
                                "TCP Socket Disconnect Failed %s (%d)", error_message, res);
            return 2;
        }
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "disconnectTcpSocket",
                            "TCP Socket Disconnect-- Already Disconnected");
        return 3;
    }
}

extern "C" JNIEXPORT jint JNICALL checkTcpSocketStatus(JNIEnv *env, jobject obj) {
    int socketFD = tcp_socket_descriptor;
    int error = 0;
    try {
        socklen_t len = sizeof(error);
        int socketOption = getsockopt(socketFD, SOL_SOCKET, SO_ERROR, &error, &len);
        switch (socketOption) {
            case 0:
//                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "No error");
                return 0;
            case EACCES:
                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Permission denied");
                return -1;
            case EBADF:
                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus",
                                    "Bad file descriptor");
                return -2;
            case EFAULT:
                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Bad address");
                return -3;
            case EINVAL:
                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Invalid argument");
                return -4;
            case ENOTSOCK:
                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Not a socket");
                return -5;
                // Add more cases for other error codes as needed
            default:
                __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Unknown error: %d",
                                    error);
                return -6;
        }
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Exception: %s", e.what());
        return -7; // Return an appropriate error code
    } catch (...) {
        // Catch all exceptions and handle them here
        __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Caught an exception");
        return -8; // Return an appropriate error code
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_connectTcpSocket(JNIEnv *env,jobject obj,jstring ip,jint port,jboolean isNightwave) {
    try {
        int so_keep_alive_val = 1;
        struct timeval so_timeout;
        if (isNightwave) {
            so_timeout.tv_sec = 20;  // 15 seconds timeout
            so_timeout.tv_usec = 0;
        } else {
            so_timeout.tv_sec = 5;  // 5 seconds timeout
            so_timeout.tv_usec = 0;
        }
        int reuse_val = 1;
        int tcp_nodelay_flag = 1;
        const char *ip_address = (*env).GetStringUTFChars(ip, NULL);
        if (ip_address == NULL) {
            return -1; // Error getting IP address
        }

        tcp_socket_descriptor = socket(AF_INET, SOCK_STREAM, 0);
        if (tcp_socket_descriptor == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "recv error: errno=%d",
                                errno);
            switch (errno) {
                case ECONNABORTED:// Connection has been aborted by the local system.
                    return -2;
                case ECONNRESET:// The remote host abruptly terminated the connection while data was still being transmitted.
                    return -3;
                case ETIMEDOUT://Connection timeout.
                    return -4;
                case ECONNREFUSED://Connection refused by the remote host.
                    return -5;
                case EHOSTUNREACH://Destination host is unreachable.
                    return -6;
                case ENETDOWN://Network is down.
                    return -7;
                case ENETUNREACH:// Network is unreachable.
                    return -8;
                case ENETRESET://Network dropped connection on reset.
                    return -9;
                default:
                    return -10; // Generic Exception
            }
        }

        if (!isNightwave) {
            // Enable keep-alive
            if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_KEEPALIVE, &so_keep_alive_val,
                           sizeof(so_keep_alive_val)) == -1) {
                __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket",
                                    "Enable Keep-Alive Failed");
                close(tcp_socket_descriptor);
                tcp_socket_descriptor = -1;
                return -1; // Error setting socket option
            }

            // Set the keep-alive parameters (optional)
            if (setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_KEEPIDLE, &tcp_keep_idle,sizeof(tcp_keep_idle)) == -1 ||
                setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_KEEPINTVL, &tcp_keep_interval,sizeof(tcp_keep_interval)) == -1 ||
                setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_KEEPCNT, &tcp_keep_count,sizeof(tcp_keep_count)) == -1) {
                __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket","Set the keep-alive parameters Failed");
                close(tcp_socket_descriptor);
                tcp_socket_descriptor = -1;
                return -1; // Error setting keep-alive parameters
            }
        }

        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_RCVTIMEO, (char *) &so_timeout,sizeof(so_timeout)) < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "Set SO_RCVTIMEO Failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return -1; // Error setting keep-alive parameters
        }

        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_SNDTIMEO, (char *) &so_timeout,sizeof(so_timeout)) < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "Set SO_SNDTIMEO Failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return -1; // Error setting keep-alive parameters
        }

        // Enable socket address reuse
        if (setsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_REUSEADDR, &reuse_val,sizeof(reuse_val)) == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket","Enable socket address reuse failed");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return -1; // Error enabling socket address reuse
        }

        struct sockaddr_in serveraddr;
        serveraddr.sin_family = AF_INET;
        if (inet_pton(AF_INET, ip_address, &serveraddr.sin_addr) != 1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket","Error converting IP address");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return -1; // Error converting IP address
        }
        serveraddr.sin_port = htons(port);

        // Set the socket to non-blocking BEFORE calling connect
        int flags = fcntl(tcp_socket_descriptor, F_GETFL, 0);
        if (flags == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket","Error getting socket flags");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return -1;
        }
        flags |= O_NONBLOCK;
        if (fcntl(tcp_socket_descriptor, F_SETFL, flags) == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket",
                                "Error setting socket to non-blocking");
            close(tcp_socket_descriptor);
            tcp_socket_descriptor = -1;
            return -1;
        }

        // Now attempt to connect
        if (connect(tcp_socket_descriptor, (struct sockaddr *) &serveraddr, sizeof(serveraddr)) ==
            -1) {
            if (errno == EINPROGRESS || errno == EALREADY) {
                // Operation now in progress, use select() to wait for the socket to become writable
                fd_set writefds;
                FD_ZERO(&writefds);
                FD_SET(tcp_socket_descriptor, &writefds);

                struct timeval timeout;
                timeout.tv_sec = 10;  // Timeout after 10 seconds, adjust as needed
                timeout.tv_usec = 0;

                int result = select(tcp_socket_descriptor + 1, NULL, &writefds, NULL, &timeout);
                if (result == 0) {
                    __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket",
                                        "Connection timed out");
                    close(tcp_socket_descriptor);
                    tcp_socket_descriptor = -1;
                    return -1;
                } else if (result == -1) {
                    __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "Select failed: %s",
                                        strerror(errno));
                    close(tcp_socket_descriptor);
                    tcp_socket_descriptor = -1;
                    return -1;
                } else if (FD_ISSET(tcp_socket_descriptor, &writefds)) {
                    // Socket is writable, which means the connection is either successful or has failed
                    int error = 0;
                    socklen_t len = sizeof(error);
                    if (getsockopt(tcp_socket_descriptor, SOL_SOCKET, SO_ERROR, &error, &len) < 0 ||
                        error != 0) {
                        __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket",
                                            "Connection failed: %d %s", errno, strerror(error));
                        close(tcp_socket_descriptor);
                        tcp_socket_descriptor = -1;
                        return -1;
                    }
                }
            } else {
                // Immediate failure
                __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket",
                                    "Error connecting to server: %s", strerror(errno));
                close(tcp_socket_descriptor);
                tcp_socket_descriptor = -1;
                return -1;
            }
        }
        // Restore blocking mode if necessary (optional, depending on your use case)
        flags = fcntl(tcp_socket_descriptor, F_GETFL, 0);
        flags &= ~O_NONBLOCK;
        fcntl(tcp_socket_descriptor, F_SETFL, flags);

        setsockopt(tcp_socket_descriptor, IPPROTO_TCP, TCP_NODELAY, (char *) &tcp_nodelay_flag,
                   sizeof(int));

        (*env).ReleaseStringUTFChars(ip, ip_address);
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "Exception: %s", e.what());
    } catch (...) {
        // Catch all exceptions and handle them here
        __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "Caught an exception");
    }
    __android_log_print(ANDROID_LOG_ERROR, "connectTcpSocket", "tcp_socket_descriptor %d",
                        tcp_socket_descriptor);
    return tcp_socket_descriptor; // Return socket descriptor on success
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_disconnectTcpSocket(JNIEnv *env, jobject obj) {
    try {
        __android_log_print(ANDROID_LOG_ERROR, "disconnectTcpSocket",
                            "TCP Socket Disconnect Started");
        int isDisconnected = disconnectTcpSocket();
        if (isDisconnected == 2) {
            disconnectTcpSocket();
        }
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        __android_log_print(ANDROID_LOG_ERROR, "disconnectTcpSocket", "Exception: %s", e.what());
    } catch (...) {
        // Catch all other exceptions
        __android_log_print(ANDROID_LOG_ERROR, "disconnectTcpSocket",
                            "Caught an unknown exception");
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_sendTcpCommand(JNIEnv *env,jobject obj,jbyteArray data) {
    int bytes_sent = -1;
    try {
        if (data == nullptr) {
            return -13;
        }
        // Get byte array elements
        jbyte *data_bytes = env->GetByteArrayElements(data, NULL);
        if (data_bytes == nullptr) {
            return -1; // Error accessing byte array
        }

        // Get byte array length
        int data_len = env->GetArrayLength(data);

        // Send data
        bytes_sent = send(tcp_socket_descriptor, data_bytes, data_len, MSG_CONFIRM);

        // Release byte array elements
        env->ReleaseByteArrayElements(data, data_bytes, 0);

        // Handle errors
        if (bytes_sent == -1) {
            __android_log_print(ANDROID_LOG_ERROR, "writeBytesInTcp", "recv error: errno=%d",errno);
            switch (errno) {
                case ECONNABORTED:// Connection has been aborted by the local system.
                    return -2;
                case ECONNRESET:// The remote host abruptly terminated the connection while data was still being transmitted.
                    return -3;
                case ETIMEDOUT://Connection timeout.
                    return -4;
                case ECONNREFUSED://Connection refused by the remote host.
                    return -5;
                case EHOSTUNREACH://Destination host is unreachable.
                    return -6;
                case ENETDOWN://Network is down.
                    return -7;
                case ENETUNREACH:// Network is unreachable.
                    return -8;
                case ENETRESET://Network dropped connection on reset.
                    return -9;
                case EPIPE://Attempt was made to write to a pipe or socket that has been closed on the reading end.
                    return -10;
                case EBADF://Bad file descriptor
                    return -11;
                default:
                    return -12; // Generic Exception
            }
        }
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Exception: %s", e.what());
    } catch (...) {
        // Catch all exceptions and handle them here
        __android_log_print(ANDROID_LOG_ERROR, "checkTcpSocketStatus", "Caught an exception");
    }
    return bytes_sent;
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_receiveTcpResponse(JNIEnv *env, jobject obj) {
    env->GetJavaVM(&g_vm);
    if (g_obj == nullptr) {
        g_obj = env->NewGlobalRef(obj);
    } else {
        // g_obj is already set, ensure it's the same object or handle appropriately.
    }

    jclass cls = env->GetObjectClass(g_obj);
    if (cls == nullptr) {
        // Handle class retrieval error
        return;
    }

    jmethodID mid = env->GetMethodID(cls, "receiveTcpResponse", "([BI)V");
    if (mid == nullptr) {
        // Handle method retrieval error
        env->DeleteLocalRef(cls);
        return;
    }
    jbyteArray tcp_received_byte_array = env->NewByteArray(MAX_TCP_BUFFER_SIZE);
    while (tcp_socket_descriptor != -1) {
        if (checkTcpSocketStatus(env, g_obj) == 0 ) {
            bytes_received = recv(tcp_socket_descriptor, buffer, MAX_TCP_BUFFER_SIZE, 0);
            if (bytes_received == -1) {
                // Error: Handle error or server disconnection
                const char *error_message_str = strerror(errno);
                __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse", "recv error: %s (%d)",error_message_str, errno);
                const char *errorMessage;
                int errorCode;
                bool shouldBreak = false;
                switch (errno) {
                    case ECONNABORTED:// Wifi going to disconnect
                        errorMessage = "Connection Aborted";
                        errorCode = -2;
                        shouldBreak = true;
                        break;
                    case ECONNRESET:
                        errorMessage = "Connection Reset By Peer";
                        errorCode = -3;
                        shouldBreak = true;//to be fixed for wifi upgrade local host
                        break;
                    case ETIMEDOUT:
                        errorMessage = "Connection Timeout";
                        errorCode = -4;
                        shouldBreak = true;
                        break;
                    case ENOBUFS:
                        errorMessage = "Connection Dropped";
                        errorCode = -5;
                        shouldBreak = true;
                        break;
                    case EAGAIN | EWOULDBLOCK:
                        errorMessage = "Resource Temporarily Unavailable";
                        errorCode = -6;
                        if (checkTcpSocketStatus(env, g_obj) == 0) {
                            __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse",
                                                "Resource Temporarily Unavailable: %d (%d)", 0,
                                                tcp_socket_descriptor);
                            shouldBreak = false;
                        } else {
                            __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse",
                                                "Resource Temporarily Unavailable: %d",
                                                checkTcpSocketStatus(env, g_obj));
                            shouldBreak = true;
                        }
                        shouldBreak = false;
                        break;
                    case EHOSTUNREACH:
                        errorMessage = "NoRouteToHostException";
                        errorCode = -7;
                        shouldBreak = true;
                        break;
                    case ECONNREFUSED:
                        errorMessage = "Connection Refused";
                        errorCode = -8;
                        shouldBreak = true;
                        break;
                    case EPIPE:
                        errorMessage = "Broken Pipe";
                        errorCode = -9;
                        shouldBreak = true;
                        break;
                    case ESHUTDOWN:
                        errorMessage = "Cannot Send After Transport Endpoint Shutdown";
                        errorCode = -10;
                        shouldBreak = true;
                        break;
                    case EINTR:
                        errorMessage = "Interrupted system call";
                        errorCode = -11;
                        shouldBreak = true;
                        break;
                    case EIO:
                        errorMessage = "IOException";
                        errorCode = -12;
                        shouldBreak = true;
                        break;
                    default:
                        errorMessage = "Exception";
                        errorCode = -13;
                        shouldBreak = true;
                        break;
                }
                if (tcp_socket_descriptor != -1) {
                    handleTcpError(env, g_obj, errorCode, errorMessage);
                }
                if (shouldBreak) {
                    break;
                } else {
                    usleep(300000); // Wait for 300 milliseconds
                    continue;
                }
            } else if (bytes_received == 0) {
                // Connection closed by peer
                __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse","Connection closed by peer 1");
                const char *errorMessage = "Connection Reset By Peer";
                int errorCode = -3;
                handleTcpError(env, g_obj, errorCode, errorMessage);
                break;
            } else {
                try {
                    if (tcp_socket_descriptor != -1 && bytes_received > 0) {
                        env->SetByteArrayRegion(tcp_received_byte_array, 0, bytes_received,
                                                reinterpret_cast<jbyte *>(buffer));
                        env->CallVoidMethod(g_obj, mid, tcp_received_byte_array, bytes_received);
                        // Clear the tcp_buffer
                        memset(buffer, 0, sizeof(buffer));
                    }
                } catch (const std::exception &e) {
                    // Handle specific exceptions if necessary
                    __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse", "Exception: %s",
                                        e.what());
                } catch (...) {
                    // Catch all exceptions and handle them here
                    __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse",
                                        "Caught an exception");
                }
            }
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse", "SOCKET CLOSED");
            try {
                if (tcp_socket_descriptor != 1) {
                    tcp_socket_descriptor = -1;
                    handleTcpError(env, g_obj, -8, "SOCKET CLOSED");
                }
                break;
            } catch (const std::exception &e) {
                // Handle specific exceptions if necessary
                __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse SOCKET CLOSED",
                                    "Exception: %s", e.what());
            } catch (...) {
                // Catch all exceptions and handle them here
                __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse SOCKET CLOSED",
                                    "Caught an exception");
            }
        }
    }
    try {
        __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse END",
                            "TCP Disconnect Started %d", tcp_socket_descriptor);
        // Cleanup
        env->DeleteLocalRef(cls);
        env->DeleteGlobalRef(g_obj);
        g_obj = nullptr;

        int isDisconnected = disconnectTcpSocket();
        if (isDisconnected == 2) {
            disconnectTcpSocket();
        }
        closeUDPSocket();
        //close
    } catch (const std::exception &e) {
        // Handle specific exceptions if necessary
        __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse END", "Exception: %s", e.what());
    } catch (...) {
        // Catch all exceptions and handle them here
        __android_log_print(ANDROID_LOG_ERROR, "receiveTcpResponse END", "Caught an exception");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_startStopUdpReceiverNW(JNIEnv *env, jobject obj, jboolean keepSocketOpen) {
    jclass udp_object_class = env->GetObjectClass(obj);
    if (udp_object_class == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Failed to get class");
        return;
    }

    jmethodID udp_method_id = env->GetMethodID(udp_object_class, "handleNightWaveUdpData", "([BI)V");
    if (udp_method_id == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Failed to get method ID");
        env->DeleteLocalRef(udp_object_class);
        return;
    }

    try {
        if (keepSocketOpen) {
            if (!openUDPSocket()) {
                handleUdpError(env, obj, -1, "Error opening UDP socket");
                env->DeleteLocalRef(udp_object_class);
                return;
            }
        } else {
            closeUDPSocket();
            env->DeleteLocalRef(udp_object_class);
            return;
        }
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Called");
        struct pollfd fds[1];
        fds[0].fd = udp_socket_descriptor;
        fds[0].events = POLLIN;
        udp_received_byte_array = env->NewByteArray(MAX_UDP_BUFFER_SIZE);

        int64_t currentTime = 0;
        int64_t timeDiff = 0;
        int64_t lastTimeoutMessageTime = 0;

        while (udp_socket_descriptor != -1) {
            int ret = poll(fds, 1, POLL_TIMEOUT_MS_NW);
            currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
            if (ret == -1) {
                handleUdpError(env, obj, -3, "Poll Error");
                break;
            } else if (ret == 0) {
                timeDiff = currentTime - lastTimeoutMessageTime;
                if (timeDiff >= 1000) { // Check for 1 second elapsed time
                    __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "No data received for 1 second");
                    lastTimeoutMessageTime = currentTime; // Update last message time
                    handleUdpError(env, obj, -4, "Socket receive timeout");
                }
                usleep(5000); // Wait for 5 milliseconds
                continue;
            } else {
                lastTimeoutMessageTime = currentTime;
                if ((udp_received_length = recvfrom(udp_socket_descriptor, udp_buffer,
                                                    sizeof(udp_buffer), 0,
                                                    (struct sockaddr *) &udp_si_other,
                                                    &udp_slen)) == -1) {
                    if (errno == EAGAIN || errno == EWOULDBLOCK) {
                        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData",
                                            "EAGAIN || EWOULDBLOCK %d", errno);
                        usleep(10000); // Wait for 100 milliseconds
                        continue;
                    } else {
                        handleUdpError(env, obj, -5, "Receive Error");
                        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData",
                                            "Receive Error: %s", strerror(errno));
                        break;
                    }
                } else if (udp_received_length == 0) {
                    // Connection closed by peer
                    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData",
                                        "Connection closed by peer");
                    const char *errorMessage = "Connection Reset By Peer";
                    int errorCode = -3;
                    handleUdpError(env, obj, errorCode, errorMessage);
                    break;
                } else {
                    env->SetByteArrayRegion(udp_received_byte_array, 0, udp_received_length,
                                            reinterpret_cast<jbyte *>(udp_buffer));
                    env->CallVoidMethod(obj, udp_method_id, udp_received_byte_array,
                                        udp_received_length);
                    memset(udp_buffer, 0, sizeof(udp_buffer)); // Clear the buffer
                }
            }
        }
    } catch (...) {
        udp_socket_descriptor = -1;
        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Caught an unknown exception");
    }

    env->DeleteLocalRef(udp_object_class);
}

void sendFrameToJava(JNIEnv *env, jobject obj, jmethodID methodId, const std::vector<uint8_t> &frame) {
    spsArray = env->NewByteArray(spsData.size());
    ppsArray = env->NewByteArray(ppsData.size());
    frameArray = env->NewByteArray(frame.size());

    env->SetByteArrayRegion(spsArray, 0, spsData.size(),
                            reinterpret_cast<const jbyte *>(spsData.data()));
    env->SetByteArrayRegion(ppsArray, 0, ppsData.size(),
                            reinterpret_cast<const jbyte *>(ppsData.data()));
    env->SetByteArrayRegion(frameArray, 0, frame.size(),
                            reinterpret_cast<const jbyte *>(frame.data()));

    env->CallVoidMethod(obj, methodId, spsArray, ppsArray, frameArray);

    env->DeleteLocalRef(spsArray);
    env->DeleteLocalRef(ppsArray);
    env->DeleteLocalRef(frameArray);
}

bool detectStartCode(const uint8_t *data, size_t index) {
    return data[index] == 0x00 && data[index + 1] == 0x00 && data[index + 2] == 0x00 && data[index + 3] == 0x01;
}

bool isIframe(const std::vector<uint8_t> &payload) {
    size_t spsIndex = -1, ppsIndex = -1, idrIndex = -1;
    size_t index = 0;
    bool foundSPS = false, foundPPS = false, foundIDR = false;
    if (!spsData.empty()) {// Avoid storing sps,pps once its is already done (Time save).Because sps and pps value remains same for all frames.
        if (detectStartCode(payload.data(), index)) {
            return true;
        } else {
            return false;
        }
    } else {
        while (index + 4 < payload.size()) {
            if (detectStartCode(payload.data(), index)) {
                int naluType = payload[index + 4] & 0x1F;
                switch (naluType) {
                    case SPS_TYPE: // SPS
                        if (!foundSPS) {
                            spsIndex = index;
                            foundSPS = true;
                        }
                        break;
                    case PPS_TYPE: // PPS
                        if (!foundPPS) {
                            ppsIndex = index;
                            foundPPS = true;
                        }
                        break;
                    case IDR_TYPE: // IDR
                        idrIndex = index;
                        foundIDR = true;
                        break;
                }
                if (foundIDR) break; // We only break after finding IDR which ensures we collected potential SPS and PPS
            }
            index++;
        }

        // Extract SPS and PPS data if valid indices are found
        if (foundSPS && foundPPS && spsIndex != -1 && ppsIndex != -1 && idrIndex != -1) {
            __android_log_print(ANDROID_LOG_ERROR, "JNI", "isIframe found");
            spsData.assign(payload.begin() + spsIndex, payload.begin() + ppsIndex);
            ppsData.assign(payload.begin() + ppsIndex, payload.begin() + idrIndex);
            return true;
        }
        return false;
    }
}

/*Included Buffer Concept*/
extern "C" JNIEXPORT void JNICALL
Java_com_dome_librarynightwave_model_services_TCPCommunicationService_startStopUdpReceiver(JNIEnv *env, jobject obj, jboolean keepSocketOpen) {
    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "startStopUdpReceiver");
    jclass udp_object_class = env->GetObjectClass(obj);
    if (!udp_object_class) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Failed to get class");
        return;
    }

    jmethodID frameCallback = env->GetMethodID(udp_object_class, "handleOpsinH264Payload","([B[B[B)V");
    if (!frameCallback) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Failed to get method ID");
        env->DeleteLocalRef(udp_object_class);
        return;
    }

    if (keepSocketOpen) {
        if (!openUDPSocket()) {
            __android_log_print(ANDROID_LOG_ERROR, "JNI", "Error opening UDP socket");
            env->DeleteLocalRef(udp_object_class);
            return;
        }
    } else {
        closeUDPSocket();
        env->DeleteLocalRef(udp_object_class);
        return;
    }

    // Set the socket to non-blocking mode
    int flags = fcntl(udp_socket_descriptor, F_GETFL, 0);
    fcntl(udp_socket_descriptor, F_SETFL, flags | O_NONBLOCK);

    /*With Buffer*/
    struct pollfd fds[1];
    fds[0].fd = udp_socket_descriptor;
    fds[0].events = POLLIN;

    int64_t currentTime, lastTimeoutMessageTime = 0, timeDiff;
    int pollResult;
    bool isReachedMax = false;
    while (udp_socket_descriptor != -1) {
        pollResult = poll(fds, 1, POLL_TIMEOUT_MS_OPSIN);
        currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
        if (pollResult > 0) {
            if (fds[0].revents & POLLIN) {
                lastTimeoutMessageTime = currentTime;
                udp_received_length = recvfrom(udp_socket_descriptor, udp_buffer, sizeof(udp_buffer), 0,(struct sockaddr *) &udp_si_other, &udp_slen);
                if (udp_received_length > RTP_HEADER_SIZE) {
                    payloadType = udp_buffer[1] & 0x7F;
                    currentSequenceNumber = ntohs(*reinterpret_cast<uint16_t *>(udp_buffer + 2));
                    if (!isReachedMax && previousSeqNum >= 64000 && currentSequenceNumber >= 0 && currentSequenceNumber <= 200) {
                        isReachedMax = true;
                        __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "packetBuffer Size: %d - tempPacketBuffer %d ", packetBuffer.size(),tempPacketBuffer.size());
                    }
                    if (payloadType == H264_PAYLOAD_TYPE) {
                        if (!isReachedMax) {
                            packetData.assign(udp_buffer + RTP_HEADER_SIZE,udp_buffer + udp_received_length);
                            packetBuffer.emplace(currentSequenceNumber, std::move(packetData));
                            packetData.clear();
                        } else {
                            packetData.assign(udp_buffer + RTP_HEADER_SIZE,udp_buffer + udp_received_length);
                            tempPacketBuffer.emplace(currentSequenceNumber, std::move(packetData));
                            packetData.clear();
                        }

                        if (!isReachedMax && packetBuffer.size() >= H264_BUFFER_SIZE) {
                            processFrame(env, obj, frameCallback);
                        } else if (isReachedMax && !packetBuffer.empty()) {
                            processFrame(env, obj, frameCallback);
                        } else if(!tempPacketBuffer.empty()) {
                            __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "FALSE packetBuffer Size: %d - tempPacketBuffer %d ", packetBuffer.size(),tempPacketBuffer.size());
                            isReachedMax = false;
                            packetBuffer.insert(tempPacketBuffer.begin(), tempPacketBuffer.end());
                            __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "FALSE1 packetBuffer Size: %d - tempPacketBuffer %d ", packetBuffer.size(),tempPacketBuffer.size());
                            tempPacketBuffer.clear();
                            processFrame(env, obj, frameCallback);
                        }
                        memset(udp_buffer, 0, sizeof(udp_buffer)); // Clear the buffer
                        struct timeval tv1;
                        gettimeofday(&tv1, NULL);
                        jlong end = (jlong)tv1.tv_sec * 1000 + tv1.tv_usec / 1000;
                    }
                    previousSeqNum = currentSequenceNumber;
                } else if (udp_received_length == 0) {
                    // Connection closed by peer
                    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData","Connection closed by peer");
                    const char *errorMessage = "Connection Reset By Peer";
                    handleUdpError(env, obj, -6, errorMessage);
                    break;
                } else if (udp_received_length == -1) {
                    if (errno == EAGAIN || errno == EWOULDBLOCK) {
                        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData","EAGAIN || EWOULDBLOCK %d", errno);
                        usleep(5000); // Wait for 5 milliseconds
                        continue;
                    } else {
                        handleUdpError(env, obj, -5, "Receive Error");
                        __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Receive Error: %s",strerror(errno));
                        break;
                    }
                }
            } else{
                __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Receive Error: %s",strerror(errno));
            }
        } else if (pollResult == 0) {
            // Timeout, no data available
            timeDiff = currentTime - lastTimeoutMessageTime;
            if (timeDiff >= 1000) { // Check for 1 second elapsed time
                __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "No data received for 1 second");
                lastTimeoutMessageTime = currentTime; // Update last message time
                handleUdpError(env, obj, -4, "Socket receive timeout");
            }
            usleep(5000); // Wait for 5 milliseconds
            continue;
        } else {
            handleUdpError(env, obj, errno, "Poll Error");
            break;
        }
    }
    //Send the last accumulated frame if any
    if (!frameBuffer.empty()) {
        sendFrameToJava(env, obj, frameCallback, frameBuffer);
    }
    env->DeleteLocalRef(udp_object_class);
}
void processFrame(JNIEnv *env, jobject obj, jmethodID frameCallback) {
    auto it = packetBuffer.begin();
    currentSequenceNumber = it->first;
    payloadBuffer = new std::__ndk1::vector<uint8_t>{std::move(it->second)};

    if (!foundStartOfTheFrame) {
        if (isFrameMissing) {
            isFrameMissing = false;
            handleUdpError(env, obj, -7, "Frame Missing");
        }
        foundStartOfTheFrame = isIframe(*payloadBuffer);
        if(foundStartOfTheFrame){
            frameBuffer.clear();
            frameBuffer = std::move(*payloadBuffer);
            lastSequenceNumber = currentSequenceNumber;
        } else if (!frameBuffer.empty()){
            frameBuffer.clear();
        }
    } else if (foundStartOfTheFrame && currentSequenceNumber == lastSequenceNumber + 1) {
        insertAndPaint(env, obj, frameCallback);
    } else {
        frameBuffer.insert(frameBuffer.end(), payloadBuffer->begin(),payloadBuffer->end());
        if (payloadBuffer->size() < END_OF_THE_FRAME_SIZE) { // End of a frame
            if (!frameBuffer.empty()) {
                sendFrameToJava(env, obj, frameCallback, frameBuffer);
                frameBuffer.clear();
                foundStartOfTheFrame = false;
                isFrameMissing = true;
            }
            lastSequenceNumber = currentSequenceNumber;
        } else{
            __android_log_print(ANDROID_LOG_ERROR, "JNI","Missing RTP packet detected, sequence number skipped from %d to %d",lastSequenceNumber, currentSequenceNumber);
            foundStartOfTheFrame = false;
            isFrameMissing = true;
        }
    }
    packetBuffer.erase(it);
    // Clear payloadBuffer after inserting into frameBuffer
    delete payloadBuffer;
    payloadBuffer = nullptr;
}

void insertAndPaint(JNIEnv *env, jobject obj, jmethodID frameCallback) {
    frameBuffer.insert(frameBuffer.end(), payloadBuffer->begin(), payloadBuffer->end());
    if (payloadBuffer->size() < END_OF_THE_FRAME_SIZE) { // End of a frame
        if (!frameBuffer.empty()) {
            sendFrameToJava(env, obj, frameCallback, frameBuffer);
            frameBuffer.clear();
            foundStartOfTheFrame = false;
        }
    }
    lastSequenceNumber = currentSequenceNumber;
}

/*No Buffer Concept*/
//extern "C" JNIEXPORT void JNICALL
//Java_com_dome_librarynightwave_model_services_TCPCommunicationService_startStopUdpReceiver(JNIEnv *env, jobject obj, jboolean keepSocketOpen) {
//    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "startStopUdpReceiver");
//    jclass udp_object_class = env->GetObjectClass(obj);
//    if (!udp_object_class) {
//        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Failed to get class");
//        return;
//    }
//
//    jmethodID frameCallback = env->GetMethodID(udp_object_class, "handleOpsinH264Payload","([B[B[B)V");
//    if (!frameCallback) {
//        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Failed to get method ID");
//        env->DeleteLocalRef(udp_object_class);
//        return;
//    }
//
//    if (keepSocketOpen) {
//        if (!openUDPSocket()) {
//            __android_log_print(ANDROID_LOG_ERROR, "JNI", "Error opening UDP socket");
//            env->DeleteLocalRef(udp_object_class);
//            return;
//        }
//    } else {
//        closeUDPSocket();
//        env->DeleteLocalRef(udp_object_class);
//        return;
//    }
//
//    /*No Buffer*/
//    struct pollfd fds[1];
//    fds[0].fd = udp_socket_descriptor;
//    fds[0].events = POLLIN;
//    int64_t currentTime, lastTimeoutMessageTime = 0, timeDiff;
//    int ret;
//    while (udp_socket_descriptor != -1) {
//        ret = poll(fds, 1, POLL_TIMEOUT_MS_OPSIN);
//        currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//        if (ret == -1) {
//            handleUdpError(env, obj, -3, "Poll Error");
//            break;
//        } else if (ret == 0) {
//            timeDiff = currentTime - lastTimeoutMessageTime;
//            if (timeDiff >= 1000) { // Check for 1 second elapsed time
//                __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "No data received for 1 second");
//                lastTimeoutMessageTime = currentTime; // Update last message time
//                handleUdpError(env, obj, -4, "Socket receive timeout");
//            }
//            usleep(5000); // Wait for 5 milliseconds
//            continue;
//        } else {
//            lastTimeoutMessageTime = currentTime;
//            udp_received_length = recvfrom(udp_socket_descriptor, udp_buffer, sizeof(udp_buffer), 0,(struct sockaddr *) &udp_si_other, &udp_slen);
//            if (udp_received_length > RTP_HEADER_SIZE) {
//                payloadType = udp_buffer[1] & 0x7F;
//                currentSequenceNumber = ntohs(*reinterpret_cast<uint16_t *>(udp_buffer + 2));
//                if (payloadType == H264_PAYLOAD_TYPE) {
//                    frameBuffer.insert(frameBuffer.end(), udp_buffer + RTP_HEADER_SIZE,udp_buffer + udp_received_length);
//                    if (!foundStartOfTheFrame) {//Start of the frame
//                        if(isIframe(frameBuffer)){
//                            foundStartOfTheFrame = true;
//                            lastSequenceNumber = currentSequenceNumber;
//                        } else if(!frameBuffer.empty()){
//                            frameBuffer.clear();
//                        }
//                    } else if(currentSequenceNumber == lastSequenceNumber + 1) {
//                        if (udp_received_length < END_OF_THE_FRAME_SIZE && !frameBuffer.empty()) { // end of a frame
//                            sendFrameToJava(env, obj, frameCallback, frameBuffer);
//                            frameBuffer.clear(); // Reset buffer for new frame
//                            foundStartOfTheFrame = false;
//                        }
//                        lastSequenceNumber = currentSequenceNumber;
//                    } else {
//                        __android_log_print(ANDROID_LOG_ERROR, "JNI","Missing RTP packet detected, sequence number skipped from %d to %d",lastSequenceNumber, currentSequenceNumber);
//                        handleUdpError(env, obj, -7, "Frame Missing");
//                        if(!frameBuffer.empty()){
//                            frameBuffer.clear();
//                        }
//                        foundStartOfTheFrame = false;
//                    }
//                }
//            }else if (udp_received_length == 0) {
//                // Connection closed by peer
//                __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData","Connection closed by peer");
//                const char *errorMessage = "Connection Reset By Peer";
//                handleUdpError(env, obj, -6, errorMessage);
//                break;
//            } else if (udp_received_length == -1) {
//                if (errno == EAGAIN || errno == EWOULDBLOCK) {
//                    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData","EAGAIN || EWOULDBLOCK %d", errno);
//                    usleep(5000); // Wait for 5 milliseconds
//                    continue;
//                } else {
//                    handleUdpError(env, obj, -5, "Receive Error");
//                    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Receive Error: %s",strerror(errno));
//                    break;
//                }
//            }
//        }
//    }
//    // Send the last accumulated frame if any
//    if (!frameBuffer.empty()) {
//        sendFrameToJava(env, obj, frameCallback,  frameBuffer);
//    }
//    env->DeleteLocalRef(udp_object_class);
//}

/*Circular Buffer*/
//template<typename T>
//class CircularBuffer {
//public:
//    CircularBuffer(size_t size) : maxSize(size), buffer(size), head(0), tail(0), full(false) {}
//
//    void put(uint16_t sequenceNumber, T item) {
//        if (full) {
//            tail = (tail + 1) % maxSize;
//        }
//        buffer[head] = {sequenceNumber, item};
//        head = (head + 1) % maxSize;
//        full = head == tail;
//
//        // Maintain order based on sequence numbers
//        if (!full) {
//            std::sort(buffer.begin(), buffer.begin() + (full ? maxSize : head), [](const auto& a, const auto& b) {
//                return a.first < b.first;
//            });
//        } else {
//            std::vector<std::pair<uint16_t, T>> tempBuffer(buffer.begin(), buffer.end());
//            std::sort(tempBuffer.begin(), tempBuffer.end(), [](const auto& a, const auto& b) {
//                return a.first < b.first;
//            });
//            for (size_t i = 0; i < maxSize; ++i) {
//                buffer[i] = tempBuffer[i];
//            }
//        }
//    }
//
//    bool get(T& item) {
//        if (empty()) {
//            return false;
//        }
//        item = buffer[tail].second;
//        tail = (tail + 1) % maxSize;
//        full = false;
//        return true;
//    }
//
//    void reset() {
//        head = tail;
//        full = false;
//    }
//
//    bool empty() const {
//        return (!full && (head == tail));
//    }
//
//    bool fullBuffer() const {
//        return full;
//    }
//
//    size_t capacity() const {
//        return maxSize;
//    }
//
//    size_t size() const {
//        if (full) {
//            return maxSize;
//        }
//        if (head >= tail) {
//            return head - tail;
//        } else {
//            return maxSize + head - tail;
//        }
//    }
//
//private:
//    std::vector<std::pair<uint16_t, T>> buffer;
//    size_t maxSize;
//    size_t head;
//    size_t tail;
//    bool full;
//};
//
//
//CircularBuffer<std::vector<uint8_t>> circularFrameBuffer(CIRCULAR_BUFFER_SIZE);
//extern "C" JNIEXPORT void JNICALL
//Java_com_dome_librarynightwave_model_services_TCPCommunicationService_startStopUdpReceiver(JNIEnv *env, jobject obj, jboolean keepSocketOpen) {
//    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "startStopUdpReceiver");
//    jclass udp_object_class = env->GetObjectClass(obj);
//    if (!udp_object_class) {
//        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Failed to get class");
//        return;
//    }
//
//    jmethodID frameCallback = env->GetMethodID(udp_object_class, "handleOpsinH264Payload", "([B[B[B)V");
//    if (!frameCallback) {
//        __android_log_print(ANDROID_LOG_ERROR, "JNI", "Failed to get method ID");
//        env->DeleteLocalRef(udp_object_class);
//        return;
//    }
//
//    if (keepSocketOpen) {
//        if (!openUDPSocket()) {
//            __android_log_print(ANDROID_LOG_ERROR, "JNI", "Error opening UDP socket");
//            env->DeleteLocalRef(udp_object_class);
//            return;
//        }
//    } else {
//        closeUDPSocket();
//        env->DeleteLocalRef(udp_object_class);
//        return;
//    }
//
//    struct pollfd fds[1];
//    fds[0].fd = udp_socket_descriptor;
//    fds[0].events = POLLIN;
//    int64_t currentTime, lastTimeoutMessageTime = 0, timeDiff;
//    int ret;
//
//    while (udp_socket_descriptor != -1) {
//        ret = poll(fds, 1, POLL_TIMEOUT_MS_OPSIN);
//        currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//
//        if (ret == -1) {
//            handleUdpError(env, obj, -3, "Poll Error");
//            break;
//        } else if (ret == 0) {
//            timeDiff = currentTime - lastTimeoutMessageTime;
//            if (timeDiff >= 1000) {
//                __android_log_print(ANDROID_LOG_INFO, "receiveUdpData", "No data received for 1 second");
//                lastTimeoutMessageTime = currentTime;
//                handleUdpError(env, obj, -4, "Socket receive timeout");
//            }
//            usleep(5000);
//            continue;
//        } else {
//            lastTimeoutMessageTime = currentTime;
//            udp_received_length = recvfrom(udp_socket_descriptor, udp_buffer, sizeof(udp_buffer), 0, (struct sockaddr *) &udp_si_other, &udp_slen);
//
//            if (udp_received_length > RTP_HEADER_SIZE) {
//                payloadType = udp_buffer[1] & 0x7F;
//                currentSequenceNumber = ntohs(*reinterpret_cast<uint16_t *>(udp_buffer + 2));
//
//                if (payloadType == H264_PAYLOAD_TYPE) {
//                    std::vector<uint8_t> packet(udp_buffer + RTP_HEADER_SIZE, udp_buffer + udp_received_length);
//                    circularFrameBuffer.put(currentSequenceNumber, packet);
//
//                    if (!foundStartOfTheFrame) {
//                        if (isIframe(packet)) {
//                            foundStartOfTheFrame = true;
//                            lastSequenceNumber = currentSequenceNumber;
//                        } else if (!circularFrameBuffer.empty()) {
//                            circularFrameBuffer.reset();
//                        }
//                    } else {
//                        if (currentSequenceNumber == lastSequenceNumber + 1) {
//                            if (udp_received_length < END_OF_THE_FRAME_SIZE && !circularFrameBuffer.empty()) {
//                                std::vector<uint8_t> completeFrame;
//                                std::vector<uint8_t> tmp;
//                                while (circularFrameBuffer.get(tmp)) {
//                                    completeFrame.insert(completeFrame.end(), tmp.begin(), tmp.end());
//                                }
//                                sendFrameToJava(env, obj, frameCallback, completeFrame);
//                                circularFrameBuffer.reset();
//                                foundStartOfTheFrame = false;
//                            }
//                            lastSequenceNumber = currentSequenceNumber;
//                        } else {
//                            __android_log_print(ANDROID_LOG_ERROR, "JNI", "Missing RTP packet detected, sequence number skipped from %d to %d", lastSequenceNumber, currentSequenceNumber);
//                            handleUdpError(env, obj, -7, "Frame Missing");
//                            circularFrameBuffer.reset();
//                            foundStartOfTheFrame = false;
//                        }
//                    }
//                }
//            } else if (udp_received_length == 0) {
//                __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Connection closed by peer");
//                handleUdpError(env, obj, -6, "Connection Reset By Peer");
//                break;
//            } else if (udp_received_length == -1) {
//                if (errno == EAGAIN || errno == EWOULDBLOCK) {
//                    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "EAGAIN || EWOULDBLOCK %d", errno);
//                    usleep(5000);
//                    continue;
//                } else {
//                    handleUdpError(env, obj, -5, "Receive Error");
//                    __android_log_print(ANDROID_LOG_ERROR, "receiveUdpData", "Receive Error: %s", strerror(errno));
//                    break;
//                }
//            }
//        }
//    }
//
//    if (!circularFrameBuffer.empty()) {
//        std::vector<uint8_t> completeFrame;
//        std::vector<uint8_t> tmp;
//        while (circularFrameBuffer.get(tmp)) {
//            completeFrame.insert(completeFrame.end(), tmp.begin(), tmp.end());
//        }
//        sendFrameToJava(env, obj, frameCallback, completeFrame);
//    }
//
//    env->DeleteLocalRef(udp_object_class);
//}