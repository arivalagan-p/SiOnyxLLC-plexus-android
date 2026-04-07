package com.dome.librarynightwave.utils;

import android.Manifest;
import android.media.ExifInterface;
import android.net.Network;
import android.os.Build;
import android.util.Log;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.RequiresApi;

import com.dome.librarynightwave.model.repository.opsinmodel.OpsinVersionDetails;

import java.util.Locale;


public class Constants {
    /**
     * permissions request code
     */
    public final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    public static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
    };
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static final String[] REQUIRED_SDK_PERMISSIONS_30 = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static final String[] REQUIRED_SDK_PERMISSIONS_31 = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static final String[] REQUIRED_SDK_PERMISSIONS_33 = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.POST_NOTIFICATIONS,

    };

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static final String[] REQUIRED_SDK_PERMISSIONS_34 = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
            Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            Manifest.permission.POST_NOTIFICATIONS
    };

    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public static final String[] REQUIRED_SDK_PERMISSIONS_35 = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
            Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            Manifest.permission.POST_NOTIFICATIONS
    };

    @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
    public static final String[] REQUIRED_SDK_PERMISSIONS_36 = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
            Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
            Manifest.permission.POST_NOTIFICATIONS
    };


    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    public static boolean isSDK10() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.Q;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    public static boolean isSDK11() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.R;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    public static boolean isSDK12() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
    }

    public static boolean isSDK12xOr13() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2 || Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    public static boolean isSdk12AndAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2;
    }

    public static boolean isSDKBelow13() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    public static boolean isSDK13() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU;
    }

    @ChecksSdkIntAtLeast(api = 34)
    public static boolean isSDK14() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }

    @ChecksSdkIntAtLeast(api = 35)
    public static boolean isSDK15() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    public static boolean isSDK16AndAbove(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA;
    }

    public static final String WIFI_IPADDRESS = "10.0.0.2";
    public static String WIFI_IPADDRESS_NEW = "192.168.5.2";
    public static String DEFAULT_IP_ADDRESS = "10.0.0.1";
    public static String WIFI_IPADDRESS_NWD = "172.16.1.12";
    public static int DEFAULT_PORT;
    public static final int UDP_PORT = 5004;
    public static final int LOCAL_SERVER_PORT = 8080;
    public static final int SOCKET_TIMEOUT = 3000;
    public static final int SOCKET_SO_TIMEOUT = 25000;
    public static final int SOCKET_SO_TIMEOUT_TIMER_FW_NW = 25;
    public static final int SOCKET_SO_TIMEOUT_TIMER_FW_OPSIN = 25;
    public static final int SOCKET_SO_TIMEOUT_TIMER_NW = 10;
    public static final int SOCKET_SO_TIMEOUT_TIMER_OPSIN = 10;
    public static final float USB_VIDEO_NOT_SUPPORTED_VERSION = 2.57f;

    public static final String NWD_SETTING_INFO_HTML_FILE_PATH = "file:///android_asset/nightwave_digital/ChangePasswordRules.html";


    public static String DEFAULT_CAMERA_PWD = "sionyxcam";
    public static final String FILTER_STRING1 = "NIGHTWAVE";
    public static final String FILTER_STRING2 = "NightWave";
    public static final String FILTER_STRING3 = "Nightwave";
    public static final String FILTER_STRING4 = "Opsin";
    public static final String FILTER_STRING5 = "OPSIN";
    //Temporary IP Address
    public static final String FILTER_STRING6 = "NWD";


    public static final String RISCV = "riscv";
    public static final String FPGA = "fpga";
    public static final String REBOOT_FPGA = "reboot_fpga";
    public static final String WIFI_RTOS = "wifi_rtos";
    public static final String WAIT = "wait";
    public static final String OPSIN_FULL_IMAGE = "full_image";
    public static final String OPSIN_RISCV_OVERLAY = "riscv_overlay";
    public static final String OPSIN_RISCV_FPGA = "riscv_fpga";
    public static final String OPSIN_RISCV_RECOVERY = "riscv_recovery";
    public static final String OPSIN_RESTART = "restart";
    public static final String OPSIN_FACTORY = "factory";
    public static final String OPSIN_WIFI_BLE = "wifi_ble";


    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_FAILED = 2;
    public static final int STATE_DISCONNECTED = 3;
    public static volatile int mState = STATE_NONE;

    public static final int RTSP_NONE = 0;
    public static final int RTSP_CONNECTED = 1;
    public static final int RTSP_DISCONNECTED = 3;
    public static final int RTSP_IN_LIVE = 4;
    public static final int RTSP_FAILED = 2;

    public static volatile int rtspState = RTSP_NONE;

    private int getState() {
        return mState;
    }

    public static final int VALUE = 0;
    public static final int ACTION = 1;
    public static final int EVENT = 2;

    public static final int WIFI_STATE_NONE = 0;
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    public static volatile int mWifiState = WIFI_STATE_NONE;
    public static CURRENT_CAMERA_SSID currentCameraSsid = CURRENT_CAMERA_SSID.NIGHTWAVE;

    //Nightwave digital streaming error cases
    public static final int ERR_STREAM_PROGRESS_ERROR = 4; //Expect this
    public static final int ERR_INVALID_RTSP_URI_06 = 1006;
    public static final int ERR_INVALID_RTSP_URI_07 = 1007;
    public static final int ERR_AUTHENTICATION= 1011;
    public static final int ERR_NETWORK_ISSUES_10 = 1010;
    public static final int ERR_NETWORK_ISSUES_13 = 1013;
    public static final int ERR_NETWORK_ISSUES_16 = 1016;
    public static final int ERR_SERVER_ERRORS = 1012;
    public static final int ERR_STREAM_TERMINATED = 1017;
    public static final int ERR_DECODER_STREAM_01 = 1001;
    public static final int ERR_DECODER_STREAM_14 = 1014;
    public static final int ERR_PLAYBACK_STALLED = 1009;
    public static final int ERR_NO_VIDEO_DATA = 1008;
    public static final int ERR_PIPELINE_SETUP_FAIL_00 = 1000;
    public static final int ERR_PIPELINE_SETUP_FAIL_02 = 1002;
    public static final int ERR_PIPELINE_SETUP_FAIL_03 = 1003;
    public static final int ERR_PIPELINE_SETUP_FAIL_04 = 1004;
    public static final int ERR_PIPELINE_SETUP_FAIL_05 = 1005;
    public static final int ERR_INTERNAL_STATE_CORRUPTION = 1015;
    public static final int ERR_UNKNOWN_ERROR = 1018;

    private static Network objnetwork;

    public static Network getNetwork() {
        return objnetwork;
    }

    public static void setNetwork(Network network) {
        objnetwork = network;
    }

    public enum OPSIN_IMAGE_TYPE {
        CPU,
        WIFI_BOOT,
        WIFI_IMAGE,
        BLE_IMAGE,
        FPGA_BOOTER;

        public int getValue() {
            return ordinal();
        }
    }

    public enum CURRENT_CAMERA_SSID {
        NIGHTWAVE,
        OPSIN,
        NIGHTWAVE_DIGITAL,
        NONE;
    }

    public static boolean isEngineeringBuild = false;
    public static boolean isDataBlockSent = false;
    public static boolean isDisplayingWaitingTime = false;
    private static boolean isOpsinContainsOldWiFiFirmware;
    public static String compareRiscvVersion = "22.4.5";
    public static String opsinWiFiOldFirmwareVersion = "2.1.3";
    public static String opsinWiFiNewFirmwareVersion = "2.3.3";
    public static String strUnknown = "UNKNOWN";
    public static final OpsinVersionDetails opsinVersionDetails = new OpsinVersionDetails();
    public static int noOfByetsInBlock = 256;
    public static final String OPSERY_FPGA_TABLE_INDEX = "131278";
    public static final String OPSERY_FPGA_TABLE_VALUE = "206.2";

    // core:206.8 value is not updated properly from camera
    // opsin Falcon firmware version
/*    public static final String OPSERY_FPGA_TABLE_INDEX2 = "524494";
    public static final String OPSERY_FPGA_TABLE_VALUE2 = "206.8";*/
    public static String NWD_WEB_IPADDRESS = "";

    public static final int STREAM_QUALITY_LOW = 0;
    public static final int STREAM_QUALITY_HIGH = 1;

    public static int currentStreamQuality = STREAM_QUALITY_LOW;

    public static void setIsNightWaveCamera(String connectedSSID, String ipAddress) {
        if ((connectedSSID.contains(FILTER_STRING1) || connectedSSID.contains(FILTER_STRING2) || connectedSSID.contains(FILTER_STRING3))) {
            currentCameraSsid = CURRENT_CAMERA_SSID.NIGHTWAVE;
            DEFAULT_PORT = 2915;
            noOfByetsInBlock = 256;
            setConnectedCameraSSID(connectedSSID);
            if (!ipAddress.isEmpty()) {
                if (ipAddress.contains(WIFI_IPADDRESS)) {
                    DEFAULT_IP_ADDRESS = "10.0.0.1";
                } else if (ipAddress.contains(WIFI_IPADDRESS_NEW)) {
//                    DEFAULT_IP_ADDRESS = "192.168.5.1";
                    DEFAULT_IP_ADDRESS = WIFI_IPADDRESS_NEW;
                }
            }
            Log.e("Constants", "Camera: Nightwave " + noOfByetsInBlock);
        } else if (connectedSSID.contains(FILTER_STRING4) || connectedSSID.contains(FILTER_STRING5)) {
            currentCameraSsid = CURRENT_CAMERA_SSID.OPSIN;
            DEFAULT_PORT = 3915;
            noOfByetsInBlock = 4060;
            Log.e("Constants", "Camera: Opsin " + noOfByetsInBlock);
            setConnectedCameraSSID(connectedSSID);
        } else if (connectedSSID.contains(FILTER_STRING6)){
            currentCameraSsid = CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL;
            setConnectedCameraSSID(connectedSSID);
            Log.e("Constants", "NW_Digital");
        }
    }

    public static void setOpsinContainsOldWiFiFirmware(boolean isOldFirmware) {
        isOpsinContainsOldWiFiFirmware = isOldFirmware;
    }

    public static boolean isOpsinContainsOldWiFiFirmware() {
        return isOpsinContainsOldWiFiFirmware;
    }

    public static double locationLatitude = 0;
    public static double locationLongitude = 0;

    private static String ConnectedCameraSSID;

    public static String getConnectedCameraSSID() {
        return ConnectedCameraSSID;
    }

    public static void setConnectedCameraSSID(String connectedCameraSSID) {
        ConnectedCameraSSID = connectedCameraSSID;
    }

    // api callback error
    public final static int ON_SUCCESS = 200;
    public final static int FILE_NOT_FOUND = 404;
    public final static int UNAUTHORISED = 401;
    public final static int SERVER_ERROR = 500;

    public final static int BASE_URL_CAMERA_VALID_ACTION = 0;
    public final static int URL_CAMERA_VALID_ACTION = 1;
    public final static int URL_WEB_SETTINGS_ACTION = 2;
    public final static int LOWRES_LIVE_STREAMING_ACTION = 3;
    public final static int HIGHRES_LIVE_STREAMING_ACTION = 4;
    public final static int URL_GET_PASSWORD_ACTION = 5;
    public final static int URL_SET_PASSWORD_ACTION = 6;
    public final static int URL_CAMERA_RESET_ACTION = 7;
    public final static int URL_CAMERA_VALID_ACTION_JSON_RETURNS = 8;

    public static String getCameraDynamicIpAddress() {

        String cameraIpAddress = Constants.NWD_WEB_IPADDRESS;
        String[] components = cameraIpAddress.split("\\.");

        if (components.length == 4) {
            components[2] = "1";
            components[3] = "1";
        } else {
            Log.e("Constants", "getIPAddress: Invalid IP format");
            return null;
        }

        return String.join(".", components);
    }

    public enum StreamType {
        LOW,
        HIGH
    }
    public static StreamType streamType = StreamType.LOW;

    public static  boolean isLowres = false;

    public enum UpdateTransferProtocol {
        https,
        http
    }

    public static UpdateTransferProtocol updateTransferProtocol = UpdateTransferProtocol.https;

    public static void setUpdateTransferProtocol(UpdateTransferProtocol protocol) {
        updateTransferProtocol = protocol;
    }

    public static String getLoadUrl(int urlRequest) {

        String url = String.format("%s://%s", updateTransferProtocol, getCameraDynamicIpAddress());//updateTransferProtocol + "://" + getCameraDynamicIpAddress();

        switch (urlRequest) {
            case BASE_URL_CAMERA_VALID_ACTION:
                url = url.concat("/");
                break;
            case URL_CAMERA_VALID_ACTION: // xml response
                url = url.concat("/xml/description.xml");
                break;
            case URL_WEB_SETTINGS_ACTION:
                url = url.concat("/plexus/plexus.html#");
                break;
            case LOWRES_LIVE_STREAMING_ACTION:
                isLowres = true;
                streamType = StreamType.LOW;
                url = "rtsp://" + getCameraDynamicIpAddress() + "/lowres";
                break;
            case HIGHRES_LIVE_STREAMING_ACTION:
                isLowres = false;
                streamType = StreamType.HIGH;
                url = "rtsp://" + getCameraDynamicIpAddress() + "/highres";
                break;
            case URL_GET_PASSWORD_ACTION:
                url = url.concat("/api/wireless?wifi.pwd");
                break;
            case URL_SET_PASSWORD_ACTION:
                url = url.concat("/api/wireless");
                break;
            case URL_CAMERA_RESET_ACTION:
                url = url.concat("/api/operation");
                break;
            case URL_CAMERA_VALID_ACTION_JSON_RETURNS: //json response temporary use
                url = url.concat("/api/operation?system.information");
                break;

        }

        Log.e("Constants"," getLoadUrl " + url);
        return url;

    }
    //EXIF
    public static final String X_RESOLUTION = "640";
    public static final String Y_RESOLUTION = "480";
    public static final String MAKE = "SIONYX";
    public static final String NIGHTWAVE_MODEL = "SIONYX CRV-500C";
    public static final String OPSIN_MODEL = "SIONYX MDV-400C";
    public static String NWD_MODEL;
    public static final String NIGHTWAVE_APERTURE = "1.4";
    public static final float NIGHTWAVE_FOCAL_LENGTH = 16.0f;
    public static final String OPSIN_APERTURE = "1.3";
    public static final float OPSIN_FOCAL_LENGTH = 15.0f;
    public static final String NWD_APERTURE = "1.4";
    public static final float NWD_FOCAL_LENGTH = 16.0f;
    public static final int OPSIN_X_RESOLUTION = 1280;
    public static final int OPSIN_Y_RESOLUTION = 720;
    public static String NWD_FW_VERSION;

    public static String getFormattedFocalLength(ExifInterface exifInterface) {
        String focalLengthRaw = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH); // "16/1"
        if (focalLengthRaw != null && focalLengthRaw.contains("/")) {
            String[] parts = focalLengthRaw.split("/");
            try {
                float numerator = Float.parseFloat(parts[0]);
                float denominator = Float.parseFloat(parts[1]);
                float focalLength = numerator / denominator;
                return String.format(Locale.US, "%.0f mm", focalLength); // "16 mm"
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    public static String getFormattedAperture(ExifInterface exifInterface) {
        String apertureRaw = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER); // "1.4"
        return (apertureRaw != null && !apertureRaw.isEmpty()) ? "f/" + apertureRaw : "";
    }
}
