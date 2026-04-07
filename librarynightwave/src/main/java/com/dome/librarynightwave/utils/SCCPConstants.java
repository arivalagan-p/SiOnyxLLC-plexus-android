package com.dome.librarynightwave.utils;

public class SCCPConstants {
    /*
     * SCCP_DOCUMENT_VERSION v3.5.1
     * SIONYX Protocol version number 3
     * SIONYX Protocol API major version number 4
     * SIONYX Protocol API minor version number 1
     * */

    /*NIGHTWAVE SETTING IDS*/
    public static final int SETTING_ID_NW_INVERT_VIDEO = 1;
    public static final int SETTING_ID_NW_FLIP_VIDEO = 2;
    public static final int SETTING_ID_NW_IRCUT = 3;
    public static final int SETTING_ID_NW_LED = 4;

    /*OPSIN SETTING IDS*/
    public static final int SETTING_ID_OPSIN_NUC = 1;
    public static final int SETTING_ID_OPSIN_MIC = 2;
    public static final int SETTING_ID_OPSIN_GPS = 3;
    public static final int SETTING_ID_OPSIN_FPS = 4;
    public static final int SETTING_ID_OPSIN_MONOCHROMATIC = 5;
    public static final int SETTING_ID_OPSIN_NOISE_REDUCTION = 6;
    public static final int SETTING_ID_OPSIN_ROI = 7;
    public static final int SETTING_ID_OPSIN_CLOCK_MODE = 8;
    public static final int SETTING_ID_OPSIN_META_DATA = 9;

    /*Night wave setting name*/
    public static final String SETTING_NAME_NW_INVERT_VIDEO = "Invert Video";
    public static final String SETTING_MANE_NW_FLIP_VIDEO = "Flip Video";
    public static final String SETTING_NAME_NW_IRCUT = "IR Cut";
    public static final String SETTING_NAME_NW_LED = "Led";

    /*Opsin setting name*/
    public static final String SETTING_NAME_OPSIN_NUC = "NUC";
    public static final String SETTING_NAME_OPSIN_MIC = "MIC";
    public static final String SETTING_NAME_OPSIN_GPS = "GPS";
    public static final String SETTING_NAME_OPSIN_FPS = "FPS";
    public static final String SETTING_NAME_OPSIN_MONOCHROMATIC = "Monochromatic";
    public static final String SETTING_NAME_OPSIN_NOISE_REDUCTION = "Noise Reduction";
    public static final String SETTING_NAME_OPSIN_ROI = "ROI";
    public static final String SETTING_NAME_OPSIN_CLOCK_MODE = "Clock";
    public static final String SETTING_NAME_OPSIN_META_DATA = "Meta Data";

    public static final int MAX_SEQUENCE_COUNT = 32767;
    public static final int GET_SET_PACKET_HEADER_LENGTH = 12;

    public enum SCCP_MSG_TYPE {
        SET_VALUE,
        GET_VALUE,
        VALUE_UPDATE,
        DO_ACTION,
        EVENT,
        PERIODIC_DATA,
        RESPONSE,
        UPDATE_START,
        DATA_TRANSFER,
        UPDATE_COMPLETE,
        UPDATE_CANCEL,
        DIALOG_OTA_START,
        UPDATE_PROGRESS,
        DIALOG_OTA_COMPLETE,
        FPGA_RESET,
        FPGA_RESET_GOLD,
        LAST,
        SO_READ_TIMEOUT;//Created By me

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_PRIORITY {
        LOW,
        MED,
        HIGH,
        LAST_PRIORITY;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_VALUE {
        VideoRate,
        Contrast,
        Sharpness,
        Saturation,
        Scene,
        Binning,
        VideoFormat,
        MirrorImage,
        InvertImage,
        IR_Cut,
        WiFiPassword,
        CameraInfo,
        CameraName,
        Exposure,                // 14
        RiscVer,
        FpgaVer,
        WifiMAC,
        WifiSSID,
        LedEnable,
        ReleasePkgVer,
        RiscBootMode,
        SerialNum,                // this might change
        ForceAnalog,              // 23: 1 == force analog regardless of boot mode
        VideoRes,                 // 24:
        BootMode,                 // 25: Get Only 0 if power <9V (USB power) else 1
        VideoMode,                // 26: Get Only  analog == 1, usb == 0
        LAST_VALUE;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_PERIODIC_DATA {
        BrightnessAdcVal,
        BatteryAdcVal,
        LAST_DATA;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_ACTION {
        StartLiveView,
        StopLiveView,
        StartAR,
        StopAR,
        TakeStill,
        StartRec,
        StopRec,
        GetCameraInfo,
        LAST_ACTION;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_EVENT {
        ButtonPress,
        ButtonHoldShort,
        ButtonHoldLong,
        ButtonRelease,
        SdCardInserted,
        LAST_EVENT;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_ERROR {
        SCCP_SUCCESS,
        SCCP_ERROR_FAIL,   // Generic failure.
        SCCP_ERROR_INVALID_COMMAND,
        SCCP_ERROR_INVALID_PARAMETER,
        LAST_ERROR;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_FLIP_IMAGE {
        FALSE,
        TRUE;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_INVERT_IMAGE {
        FALSE,
        TRUE;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_BINNING {
        FALSE,
        TRUE;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_LED {
        FALSE,
        TRUE;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_VIDEO_RATE {
        FPS_30,
        FPS_60,
        FPS_90,
        SCCP_VIDEO_RATE_LAST;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_IRCUT {
        OUT,
        IN,
        AUTO;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_VIDEO_FORMAT {
        SCCP_NTSC,
        SCCP_PAL,
        LAST_VIDEO_FORMAT;

        public int getValue() {
            return ordinal() + 1;
        }
    }

    public enum SCCP_VIDEO_SCENE {
        SCCP_GEN,
        SCCP_RUAL,
        SCCP_METRO,
        SCCP_MARINE,
        LAST_VIDEO_SCENE;

        public int getValue() {
            return ordinal() + 1;
        }
    }


    /*OPSIN SCCP*/
    // Command 0 to 63 are reserved for SCCP management
    public enum SCCP_MSG_TYPE_OPSIN_SCCP_MANAGEMENT {
        SCCP_CMD_KEEPALIVE,                           //!< Notifies our peer we are still alive
        SCCP_CMD_GET_INFO,                            //!< Retrieves peer information
        SCCP_CMD_GET_COUNTERS,                        //!< Retrieves SCCP Counter information
        SCCP_CMD_RESET_COUNTERS,                      //!< Reset SCCP Counters
        SCCP_CMD_SET_SCD,                             //!< Set stale connection detection (SCD) configuration
        SCCP_CMD_GET_SCD;                          //!< Get stale connection detection (SCD) configuration

        public int getValue() {
            return ordinal();
        }
    }

    // Command 64 to 127 are reserved for upgrade protocol
    public enum SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL {
        SCCP_CMD_UPGRADE_START,                       //!< Upgrade start notification
        SCCP_CMD_UPGRADE_DATA,                        //!< Upgrade data notification
        SCCP_CMD_UPGRADE_DATA_COMPLETE,               //!< Upgrade data complete notification
        SCCP_CMD_UPGRADE_CANCEL,                      //!< Upgrade cancel notification
        SCCP_CMD_UPGRADE_PROGRESS,                    //!< Upgrade progress notification
        SCCP_CMD_UPGRADE_COMPLETE,                    //!< Upgrade has completed notification
        SCCP_CMD_UPGRADE_STATE,                       //!< Retrieves upgrade detailed state
        SCCP_CMD_UPGRADE_DIALOG_OTA,                  //!< Starts of OTA upgrade of dialog
        SCCP_CMD_UPGRADE_SET_VERSION,                 //!< Set camera system master version
        SCCP_CMD_UPGRADE_LAST;                        //!< Last upgrade module command ID

        public int getValue() {
            return ordinal() + 64;
        }
    }

    // Command 128 to 2047 are camera product specific
    public enum SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC {
        SCCP_CMD_CAMERA_GET_VERSION,                   //!< Retrieves the system assembly version information
        SCCP_CMD_CAMERA_GET_PRODUCT,                  //!< Retrieves Camera Product Information
        SCCP_CMD_CAMERA_GET_PRODUCT_CAPABILITIES,     //!< Retrieves Camera Product Capabilities
        SCCP_CMD_CAMERA_GET_CAPABILITIES,             //!< Retrieves Camera Module Capabilities
        SCCP_CMD_CAMERA_GET_SERIAL,                   //!< Retrieves Camera Product Serial Number
        SCCP_CMD_CAMERA_SET_SERIAL,                   //!< Sets Camera Serial Number
        SCCP_CMD_CAMERA_GET_NAME,                     //!< Retrieves Camera Name
        SCCP_CMD_CAMERA_SET_NAME,                     //!< Sets Camera Name
        SCCP_CMD_CAMERA_GET_PICTURE_MODE,             //!< Retrieves the camera picture mode
        SCCP_CMD_CAMERA_SET_PICTURE_MODE,             //!< Sets the camera picture mode
        SCCP_CMD_CAMERA_GET_BURST_PARAM,              //!< Retrieves the burst mode paramters
        SCCP_CMD_CAMERA_SET_BURST_PARAM,              //!< Sets the burst mode paramters
        SCCP_CMD_CAMERA_GET_TIME_LAPSE_PARAM,         //!< Retrieves the time-lapse mode parameters
        SCCP_CMD_CAMERA_SET_TIME_LAPSE_PARAM,         //!< Sets the time-lapse mode parameters
        SCCP_CMD_CAMERA_TAKE_PICTURE,                 //!< Take picture
        SCCP_CMD_CAMERA_START_RECORDING,              //!< Start video recording
        SCCP_CMD_CAMERA_STOP_RECORDING,               //!< Stop video recording
        SCCP_CMD_CAMERA_START_STREAMING,              //!< Start video streaming
        SCCP_CMD_CAMERA_STOP_STREAMING,               //!< Stop video streaming
        SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION,         //!< Retrieves JPEG compression
        SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION,         //!< Sets JPEG compression
        SCCP_CMD_CAMERA_GET_FRAMERATE,                //!< Retrieves camera framerate
        SCCP_CMD_CAMERA_SET_FRAMERATE,                //!< Sets camera framerate
        SCCP_CMD_CAMERA_GET_IMAGE_STATE,              //!< Get the Camera Image State
        SCCP_CMD_CAMERA_RESTART,                      //!< Restart the camera subsystem
        SCCP_CMD_CAMERA_GET_LOGS,                     //!< Retrieves remote logs state information
        SCCP_CMD_CAMERA_SET_LOGS,                     //!< Enables/Disables remote logs
        SCCP_CMD_CAMERA_NOTIFY_LOGS,                  //!< Sends camera logs for remote logging
        SCCP_CMD_CAMERA_GET_IRCUT,                    //!< Retrieves IRCUT filter state information
        SCCP_CMD_CAMERA_SET_IRCUT,                    //!< Enables/Disables IRCUT filter
        SCCP_CMD_CAMERA_GET_INVERT,                   //!< Retrieves image invert state information
        SCCP_CMD_CAMERA_SET_INVERT,                   //!< Enables/Disables image invert
        SCCP_CMD_CAMERA_GET_MIRROR,                   //!< Retrieves image mirror state information
        SCCP_CMD_CAMERA_SET_MIRROR,                   //!< Enables/Disables image mirror
        SCCP_CMD_CAMERA_GET_DVR_STATS,                //!< Get Camera DVR Statistics
        SCCP_CMD_CAMERA_NOTIFY_DVR_STATS,             //!< Camera DVR Statistics Notification
        SCCP_CMD_CAMERA_RESET_DVR_STATS,              //!< Camera Reset DVR Statistics
        SCCP_CMD_CAMERA_GET_DVR_H264_STATS,           //!< Get Camera DVR H264 Statistics
        SCCP_CMD_CAMERA_NOTIFY_DVR_H264_STATS,        //!< Camera DVR H264 Statistics Notification
        SCCP_CMD_CAMERA_RESET_DVR_H264_STATS,         //!< Camera Reset DVR H264 Statistics
        SCCP_CMD_CAMERA_GET_H264,                     //!< Retrieves H264 configuration
        SCCP_CMD_CAMERA_SET_H264,                     //!< Sets H264 configuration
        SCCP_CMD_CAMERA_GET_DATETIME,                 //!< Retrieves current date/time
        SCCP_CMD_CAMERA_SET_DATETIME,                 //!< Sets current date/time
        SCCP_CMD_CAMERA_GET_TIMEZONE,                 //!< Retrieves time zone UTC offset configuration
        SCCP_CMD_CAMERA_SET_TIMEZONE,                 //!< Sets time zone UTC offset configuration
        SCCP_CMD_CAMERA_FACTORY_RESET,                //!< Perform a factory reset of the camera
        SCCP_CMD_CAMERA_GET_MIC_GAIN,                 //!< Retrieves microphone gain
        SCCP_CMD_CAMERA_SET_MIC_GAIN,                 //!< Sets microphone gain
        SCCP_CMD_CAMERA_GET_MIC_STATE,                //!< Retrieves microphone state
        SCCP_CMD_CAMERA_SET_MIC_STATE,                //!< Sets microphone state
        SCCP_CMD_CAMERA_GET_MIC_INFO,                 //!< Retrieves microphone information
        SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM,             //!< Retrieves digital zoom factor
        SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM,             //!< Sets digital zoom factor
        SCCP_CMD_CAMERA_GET_NUC_STATE,                //!< Gets the camera NUC state
        SCCP_CMD_CAMERA_SET_NUC_STATE,                //!< Sets the camera NUC state
        SCCP_CMD_CAMERA_GET_RECORDING_STATE,          //!< Gets the camera recording state
        SCCP_CMD_CAMERA_GET_STREAMING_STATE,          //!< Gets the camera recording state
        SCCP_CMD_CAMERA_GET_CLOCKMODE,                //!< Retrieves clock mode configuration
        SCCP_CMD_CAMERA_SET_CLOCKMODE,                //!< Sets clock mode configuration
        SCCP_CMD_CAMERA_GET_METADATA,                 //!< Retrieves multimedia metadata configuration
        SCCP_CMD_CAMERA_SET_METADATA,                 //!< Sets multimedia metadata configuration
        SCCP_CMD_DUMMY,
        SCCP_CMD_CAMERA_GET_TIMEFORMAT,
        SCCP_CMD_CAMERA_SET_TIMEFORMAT,
        SCCP_CMD_CAMERA_LAST;  //!< Last camera module command ID

        public int getValue() {
            return ordinal() + 128;
        }
    }

    // Command 2048 to 4095 are power management specific
    public enum SCCP_MSG_TYPE_OPSIN_POWER_MANAGEMENT {
        SCCP_CMD_POWER_GET_CAPABILITIES,              //!< Retrieves Camera Power Module Capabilities
        SCCP_CMD_POWER_GET_BATTERY,                   //!< Retrieves Battery Information
        SCCP_CMD_POWER_GET_BATTERY_ADC,               //!< Retrieves Battery ADC Counter
        SCCP_CMD_POWER_NOTIFY_BATTERY_ADC,            //!< Battery ADC Counter notification
        SCCP_CMD_POWER_LAST;                          //!< Last power module command ID

        public int getValue() {
            return ordinal() + 2048;
        }
    }

    // Command 4096 to 6143 are System IO specific
    public enum SCCP_MSG_TYPE_OPSIN_SYSTEM_IO {
        SCCP_CMD_SYSIO_GET_CAPABILITIES,              //!< System I/O Module Capabilities
        SCCP_CMD_SYSIO_NOTIFY_PUSH,                   //!< Push button notification
        SCCP_CMD_SYSIO_GET_PUSH,                      //!< Get Push button state
        SCCP_CMD_SYSIO_NOTIFY_SELECT,                 //!< Select button notification
        SCCP_CMD_SYSIO_GET_SELECT,                    //!< Get Select button state
        SCCP_CMD_SYSIO_NOTIFY_METER,                  //!< Meter button notification
        SCCP_CMD_SYSIO_GET_METER,                     //!< Get Meter button state
        SCCP_CMD_SYSIO_LAST;                          //!< Last system I/O module command ID

        public int getValue() {
            return ordinal() + 4096;
        }
    }

    // Command 6144 to 8191 are Display specific
    public enum SCCP_MSG_TYPE_OPSIN_DISPLAY_SPECIFIC {
        SCCP_CMD_DISPLAY_GET_CAPABILITIES,            //!< Retrieves Camera Display Module Capabilities
        SCCP_CMD_DISPLAY_GET_BRIGHTNESS,              //!< Retrieves current display brightness
        SCCP_CMD_DISPLAY_SET_BRIGHTNESS,              //!< Set display brightness
        SCCP_CMD_DISPLAY_LAST;                        //!< Last display module command ID

        public int getValue() {
            return ordinal() + 6144;
        }
    }

    // Command 8192 to 10239 are Sensor specific
    public enum SCCP_MSG_TYPE_OPSIN_SENSOR_SPECIFIC {
        SCCP_CMD_SENSOR_GET_CAPABILITIES,             //!< Retrieves Camera sensor Module Capabilities
        SCCP_CMD_SENSOR_GET_INFO,                     //!< Retrieves Camera sensor Information (TBD)
        SCCP_CMD_SENSOR_GET_TEMPERATURE,              //!< Retrieves current sensor temperature
        SCCP_CMD_SENSOR_LAST;                         //!< Last sensor module command ID

        public int getValue() {
            return ordinal() + 8192;
        }
    }

    // Command 10240 to 12287 are ISP specific
    public enum SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC {
        SCCP_CMD_ISP_GET_CAPABILITIES,                //!< Retrieves Camera ISP Module Capabilities
        SCCP_CMD_ISP_GET_INFO,                        //!< Retrieves Camera ISP Information
        SCCP_CMD_ISP_RESERVED1,                       //!< UNUSED
        SCCP_CMD_ISP_RESERVED2,                       //!< UNUSED
        SCCP_CMD_ISP_RESERVED3,                       //!< UNUSED
        SCCP_CMD_ISP_RESERVED4,                       //!< UNUSED
        SCCP_CMD_ISP_GET_EV,                          //!< Get Exposure Value (EV)
        SCCP_CMD_ISP_SET_EV,                          //!< Set Exposure Value (EV)
        SCCP_CMD_ISP_GET_EV_RANGE,                    //!< Get Exposure Value (EV) Range
        SCCP_CMD_ISP_GET_HDR,                         //!< Get High Dynamic Range (HDR) Value
        SCCP_CMD_ISP_SET_HDR,                         //!< Set High Dynamic Range (HDR) Value
        SCCP_CMD_ISP_GET_NR,                          //!< Get Noise Reduction (NR) Value
        SCCP_CMD_ISP_SET_NR,                          //!< Set Noise Reduction (NR) Value
        SCCP_CMD_ISP_GET_SHARPNESS,                   //!< Get Sharpness Level Value
        SCCP_CMD_ISP_GET_NUC,                         //!< Gets the ISP NUC state
        SCCP_CMD_ISP_SET_NUC,                         //!< Sets the ISP NUC state
        SCCP_CMD_ISP_GET_MONOCHROMATIC,               //!< Get ISP Monochromatic Mode
        SCCP_CMD_ISP_SET_MONOCHROMATIC,               //!< Set ISP Monochromatic Mode
        SCCP_CMD_ISP_GET_ROI,                         //!< Get ROI Mode
        SCCP_CMD_ISP_SET_ROI,                         //!< Set ROI Mode
        SCCP_CMD_ISP_LAST;                              //!< Last ISP module command ID

        public int getValue() {
            return ordinal() + 10240;
        }
    }

    // Command 12288 to 14335 are SD Card specific
    public enum SCCP_MSG_TYPE_OPSIN_SDCARD_SPECIFIC {
        SCCP_CMD_SDCARD_GET_CAPABILITIES,             //!< Retrieves SD Card Module Capabilities
        SCCP_CMD_SDCARD_GET_INFO,                     //!< Retrieves SD Card Information
        SCCP_CMD_SDCARD_FORMAT,                       //!< Initiate formatting of an SD Card
        SCCP_CMD_SDCARD_GET_FILE_INFO,                //!< Retrieve specific file information
        SCCP_CMD_SDCARD_OPEN_FILE,                    //!< Open a file for reading
        SCCP_CMD_SDCARD_READ_FILE,                    //!< Read content from an open file
        SCCP_CMD_SDCARD_CLOSE_FILE,                   //!< Close an open file
        SCCP_CMD_SDCARD_LAST;                         //!< Last SD Card module command ID

        public int getValue() {
            return ordinal() + 12288;
        }
    }

    // Command 14336 to 16383 are Wireless specific
    public enum SCCP_MSG_TYPE_OPSIN_WIRELESS_SPECIFIC {
        SCCP_CMD_WIFI_GET_CAPABILITIES,               //!< Retrieves WIFI Module Capabilities
        SCCP_CMD_WIFI_GET_RADIO_STATE,                //!< Retrieves WIFI radio state
        SCCP_CMD_WIFI_NOTIFY_RADIO_STATE,             //!< Wireless radio state notification
        SCCP_CMD_WIFI_GET_RADIO_CONFIG,               //!< Retrieves WIFI Radio Configuration
        SCCP_CMD_WIFI_SET_RADIO_CONFIG,               //!< Sets WIFI Radio Configuration
        SCCP_CMD_WIFI_GET_SSID,                       //!< Retrieves WIFI SSID
        SCCP_CMD_WIFI_SET_SSID,                       //!< Sets WIFI SSID
        SCCP_CMD_WIFI_GET_MAC,                        //!< Retrieves WIFI MAC Address
        SCCP_CMD_WIFI_UPDATE_MAC,                     //!< Updates WIFI MAC Address
        SCCP_CMD_WIFI_GET_PASSWORD,                   //!< Retrieves WIFI Password
        SCCP_CMD_WIFI_SET_PASSWORD,                   //!< Set WIFI Password
        SCCP_CMD_WIFI_LAST;

        public int getValue() {
            return ordinal() + 14336;
        }
    }

    // Command 16384 to 18431 are GPS specific
    public enum SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC {
        SCCP_CMD_GPS_GET_CAPABILITIES,                //!< Retrieves GPS Module Capabilities
        SCCP_CMD_GPS_GET_INFO,                        //!< Retrieves GPS Information
        SCCP_CMD_GPS_GET_POSITION,                    //!< Retrieves current GPS posision
        SCCP_CMD_GPS_NOTIFY_POSITION,                 //!< GPS Position notification
        SCCP_CMD_GPS_GET_DATETIME,                    //!< Retrieves GPS clock
        SCCP_CMD_GPS_NOTIFY_DATETIME,                 //!< GPS Clock notification
        SCCP_CMD_GPS_SET_POWER,                       //!< Enables / Disables GPS
        SCCP_CMD_GPS_SET_BYPASS,                      //!< Enables / Disables GPS Bypass
        SCCP_CMD_GPS_NOTIFY_BYPASS,                   //!< GPS Bypass Message Notification
        SCCP_CMD_GPS_GET_SATELLITE_VIEW,              //!< Retrieves Satellite View
        SCCP_CMD_GPS_LAST;                            //!< Last GPS module command ID

        public int getValue() {
            return ordinal() + 16384;
        }
    }

    // Command 18432 to 20479 are Compass specific
    public enum SCCP_MSG_TYPE_OPSIN_COMPASS_SPECIFIC {
        SCCP_CMD_COMPASS_GET_CAPABILITIES,            //!< Retrieves Compass Module Capabilities
        SCCP_CMD_COMPASS_GET_INFO,                    //!< Retrieves Compass Information
        SCCP_CMD_COMPASS_NOTIFY_ANGLES,               //!< Compass angles update notification
        SCCP_CMD_COMPASS_GET_ANGLES,                  //!< Retrieves compass angles
        SCCP_CMD_COMPASS_CALIBRATE,                   //!< Calibrate the compass
        SCCP_CMD_COMPASS_LAST;                        //!< Last compass module command ID


        public int getValue() {
            return ordinal() + 18432;
        }
    }

    // Command 22528 to 24575 are Telemetry specific
    public enum SCCP_MSG_TYPE_OPSIN_TELEMETRY_SPECIFIC {
        SCCP_CMD_TELEMETRY_GET_CAPABILITIES,          //!< Retrieves Telemetry Module Capabilities
        SCCP_CMD_TELEMETRY_GET_INFO,                  //!< Retrieves Telemetry Information
        SCCP_CMD_TELEMETRY_ENABLE,                    //!< Enable telemetry services
        SCCP_CMD_TELEMETRY_DISABLE,                   //!< Disable telemetry services
        SCCP_CMD_TELEMETRY_LAST;                      //!< Last telemetry module command ID

        public int getValue() {
            return ordinal() + 22528;
        }
    }

    // Command 24576 to 26623 are Alarm specific
    public enum SCCP_MSG_TYPE_OPSIN_ALARM_SPECIFIC {
        SCCP_CMD_ALARMS_GET_CAPABILITIES,             //!< Retrieves Alarm Module Capabilities
        SCCP_CMD_ALARMS_SENSOR_TEMPERATURE,           //!< Sensor Temperature Alarm
        SCCP_CMD_ALARMS_BATTERY_LEVEL,                //!< Battery Level Alarm
        SCCP_CMD_ALARMS_SDCARD_USAGE,                 //!< SD Card Usage Alarm
        SCCP_CMD_ALARMS_LAST;                         //!< Last Alarm module command ID

        public int getValue() {
            return ordinal() + 24576;
        }
    }

    // Command 26624 to 28671 are Bluetooth specific
    public enum SCCP_MSG_TYPE_OPSIN_BLUETOOTH_SPECIFIC {
        SCCP_CMD_BLE_GET_CAPABILITIES,                  //!< Retrieves Bluetooth Module Capabilities
        SCCP_CMD_BLE_LAST;                              //!< Last Bluetooth module command ID

        public int getValue() {
            return ordinal() + 26624;
        }
    }

    // Command 28672 to 30719 are Platform specific
    public enum SCCP_MSG_TYPE_OPSIN_PLATFORM_SPECIFIC {
        SCCP_CMD_PLATFORM_SHUTDOWN,                  //!< Platform shutdown progress
        SCCP_CMD_PLATFORM_GET_UART_CONFIG,            //!< Retrieves camera platform UART configuration
        SCCP_CMD_PLATFORM_SET_UART_CONFIG,            //!< Configures camera platform UART
        SCCP_CMD_PLATFORM_LAST;                       //!< Last Platform module command ID

        public int getValue() {
            return ordinal() + 28672;
        }
    }

    // Command 61440 to 63487 are ENGINEERING specific
    public enum SCCP_MSG_TYPE_OPSIN_ENGINEERING_SPECIFIC1 {
        SCCP_CMD_ENGINEERING_REGISTER_READ,           //!< Engineering register read
        SCCP_CMD_ENGINEERING_REGISTER_WRITE,          //!< Platform register write
        SCCP_CMD_ENGINEERING_LAST;                    //!< Last Engineering module command ID

        public int getValue() {
            return ordinal() + 61440;
        }
    }

    public enum SCCP_MSG_TYPE_OPSIN_ENGINEERING_SPECIFIC2 {

        SCCP_CMD_ENGINEERING_ISP_GET_STATE,           //!< Retrieves ISP State information
        SCCP_CMD_ENGINEERING_ISP_NOTIFY_STATE,        //!< ISP State Notification
        SCCP_CMD_ENGINEERING_ISP_GET_CONFIG,          //!< Retrieves ISP Configuration information
        SCCP_CMD_ENGINEERING_ISP_SET_CONFIG,          //!< Set ISP Configuration information
        SCCP_CMD_ENGINEERING_ISP_GET_DEBUG,           //!< Retrieves ISP Debug information
        SCCP_CMD_ENGINEERING_ISP_SET_DEBUG,           //!< Set ISP Debug information
        SCCP_CMD_ENGINEERING_LAST;                    //!< Last Engineering module command ID

        public int getValue() {
            return ordinal() + 61600;
        }
    }

    public enum SCCP_MSG_TYPE_OPSIN_ENGINEERING_SPECIFIC3 {
        SCCP_CMD_ENGINEERING_SDCARD_LAST_SEQ,   //!< Get SD Card last recording seuqence number
        SCCP_CMD_ENGINEERING_LAST;              //!< Last Engineering module command ID

        public int getValue() {
            return ordinal() + 61700;
        }
    }

    public enum SCCP_MSG_TYPE_OPSIN_ENGINEERING_SPECIFIC4 {
        SCCP_CMD_ENGINEERING_MEMORY_READ,   //!< Engineering memory read
        SCCP_CMD_ENGINEERING_LAST;          //!< Last Engineering module command ID

        public int getValue() {
            return ordinal() + 61800;
        }
    }

    public enum SCCP_MSG_TYPE_OPSIN_ENGINEERING_SPECIFIC5 {
        SCCP_CMD_ENGINEERING_FLASH_READ,    //!< Engineering flash read
        SCCP_CMD_ENGINEERING_LAST;          //!< Last Engineering module command ID

        public int getValue() {
            return ordinal() + 61900;
        }
    }

    // Command 63488 to 65535 are MANUFACTURING specific
    public enum SCCP_MSG_TYPE_OPSIN_MANUFACTURING_SPECIFIC {
        SCCP_CMD_MANUFACTURING_GET_CAPABILITIES,          //!< Manufacturing module capabilities
        SCCP_CMD_MANUFACTURING_READ,                      //!< Read manufacturing table
        SCCP_CMD_MANUFACTURING_WRITE,                     //!< Write manufacturing table
        SCCP_CMD_MANUFACTURING_FLUSH,                     //!< Flush manufacturing table
        SCCP_CMD_MANUFACTURING_GET_PROTECTED,             //!< Get protected state of manufacturing table
        SCCP_CMD_MANUFACTURING_SET_PROTECTED,             //!< Set protected state of manufacturing table
        SCCP_CMD_MANUFACTURING_GET_DUST_TEST,             //!< Get dust test state
        SCCP_CMD_MANUFACTURING_SET_DUST_TEST,             //!< Set dust test state
        SCCP_CMD_MANUFACTURING_CALIBRATE_NUC,             //!< Calibrates NUC
        SCCP_CMD_MANUFACTURING_GET_NUC_CALIBRATION,       //!< Gets NUC Calibration state
        SCCP_CMD_MANUFACTURING_NOTIFY_SYSTEM_UP,          //!< System up and available notification
        SCCP_CMD_MANUFACTURING_LAST;                      //!< Last Manufacturing module command ID

        public int getValue() {
            return ordinal() + 63488;
        }
    }

    public static final byte EV_STEP0 = -25;
    public static final byte EV_STEP1 = -20;
    public static final byte EV_STEP2 = -15;
    public static final byte EV_STEP3 = -10;
    public static final byte EV_STEP4 = -5;
    public static final byte EV_STEP5 = 0;
    public static final byte EV_STEP6 = 5;
    public static final byte EV_STEP7 = 10;
    public static final byte EV_STEP8 = 15;


    public static final byte BRIGHTNESS_STEP0 = 5;
    public static final byte BRIGHTNESS_STEP1 = 25;
    public static final byte BRIGHTNESS_STEP2 = 50;
    public static final byte BRIGHTNESS_STEP3 = 75;
    public static final byte BRIGHTNESS_STEP4 = 100;

    public enum SCCP_OPSIN_ZOOM_LEVEL {
        LEVEL1,
        LEVEL2,
        LEVEL3;

        public byte getValue() {
            return (byte) (ordinal() + 1);
        }
    }

    public enum SCCP_OPSIN_NUC_STATE {
        DISABLED,
        ENABLED;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public enum SCCP_OPSIN_MIC_STATE {
        DISABLED,
        ENABLED;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public enum SCCP_OPSIN_FRAME_RATE {
        FRAME_RATE_30,
        FRAME_RATE_60,
        FRAME_RATE_90;

        public short getValue() {
            return (short) ((ordinal() + 1) * 30);
        }
    }

    public enum SCCP_OPSIN_JPEG_COMPRESSION {
        JPEG_COMPRESSION_LOW,
        JPEG_COMPRESSION_LEVEL1,
        JPEG_COMPRESSION_LEVEL2,
        JPEG_COMPRESSION_LEVEL3,
        JPEG_COMPRESSION_LEVEL4,
        JPEG_COMPRESSION_LEVEL5,
        JPEG_COMPRESSION_LEVEL6,
        JPEG_COMPRESSION_LEVEL7,
        JPEG_COMPRESSION_LEVEL8,
        JPEG_COMPRESSION_HIGH;

        public byte getValue() {
            return (byte) ((ordinal() + 1));
        }
    }

    public enum SCCP_OPSIN_GPS_STATE {
        DISABLED,
        ENABLED;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public enum SCCP_OPSIN_FACTORTY_RESET {
        RESET_SOFT,
        RESET_HARD;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public static final byte SCCP_ACK_SUCCESS = 0x00;
    public static final byte SCCP_ACK_FAILURE = 0x01;
    public static final byte SCCP_ACK_UNSUPPORTED = 0x02;
    public static final byte SCCP_ACK_UNKNOWN = 0x03;
    public static final byte SCCP_ACK_MAX = 0x04;

    public enum SCCP_VIDEO_MODE {
        SCCP_VIDEO_MODE_USB, // as 0
        SSCP_VIDEO_MODE_WIFI;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_CAMERA_MODE {
        SCCP_CAMERA_MODE_USB, // as 0
        SSCP_CAMERA_MODE_ANALOG;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_VIDEO_RES {
        SCCP_VIDEO_RES_VGA, // as 0
        SSCP_VIDEO_RES_720p;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_RECORDING_STATE {
        NOT_RECORDING,
        RECORDING;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_STREAMING_STATE {
        NOT_STREAMING,
        STREAMING;

        public int getValue() {
            return ordinal();
        }
    }

    public enum SCCP_CLOCK_MODE {
        SYSTEM,
        GPS;

        public int getValue() {
            return ordinal();
        }
    }

    public enum OPSIN_SCCP_ROI_MODE {
        SCCP_ROI_OFF,
        SCCP_ROI_30,
        SCCP_ROI_50;

        public byte getValue() {
            return (byte) ordinal();
        }
    }

    public enum SCCP_OPSIN_MONOCHROMATIC_STATE {
        DISABLED,
        ENABLED;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public enum SCCP_OPSIN_METADATA_STATE {
        DISABLED,
        ENABLED;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public enum SCCP_OPSIN_NOISE_REDUCTION_STATE {
        DISABLED,
        ENABLED;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

    public enum SCCP_OPSIN_CLOCK_FORMAT_STATE {
        MODE_12,
        MODE_24;

        public byte getValue() {
            return (byte) (ordinal());
        }
    }

}
