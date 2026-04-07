package com.dome.librarynightwave.model.repository;

import static android.content.Context.WIFI_SERVICE;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;
import static com.dome.librarynightwave.model.services.PlayerService.ACTION_START_SERVICE;
import static com.dome.librarynightwave.model.services.PlayerService.ACTION_STOP_SERVICE;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.lastSentCommand;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.shouldReceiveUDP;
import static com.dome.librarynightwave.utils.Constants.ACTION;
import static com.dome.librarynightwave.utils.Constants.EVENT;
import static com.dome.librarynightwave.utils.Constants.FPGA;
import static com.dome.librarynightwave.utils.Constants.HIGHRES_LIVE_STREAMING_ACTION;
import static com.dome.librarynightwave.utils.Constants.LOCAL_SERVER_PORT;
import static com.dome.librarynightwave.utils.Constants.LOWRES_LIVE_STREAMING_ACTION;
import static com.dome.librarynightwave.utils.Constants.MAKE;
import static com.dome.librarynightwave.utils.Constants.NIGHTWAVE_APERTURE;
import static com.dome.librarynightwave.utils.Constants.NIGHTWAVE_FOCAL_LENGTH;
import static com.dome.librarynightwave.utils.Constants.NIGHTWAVE_MODEL;
import static com.dome.librarynightwave.utils.Constants.NWD_APERTURE;
import static com.dome.librarynightwave.utils.Constants.NWD_FOCAL_LENGTH;
import static com.dome.librarynightwave.utils.Constants.NWD_FW_VERSION;
import static com.dome.librarynightwave.utils.Constants.NWD_MODEL;
import static com.dome.librarynightwave.utils.Constants.OPSERY_FPGA_TABLE_INDEX;
import static com.dome.librarynightwave.utils.Constants.OPSERY_FPGA_TABLE_VALUE;
import static com.dome.librarynightwave.utils.Constants.OPSIN_APERTURE;
import static com.dome.librarynightwave.utils.Constants.OPSIN_FACTORY;
import static com.dome.librarynightwave.utils.Constants.OPSIN_FOCAL_LENGTH;
import static com.dome.librarynightwave.utils.Constants.OPSIN_FULL_IMAGE;
import static com.dome.librarynightwave.utils.Constants.OPSIN_MODEL;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RESTART;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_FPGA;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_OVERLAY;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_RECOVERY;
import static com.dome.librarynightwave.utils.Constants.OPSIN_WIFI_BLE;
import static com.dome.librarynightwave.utils.Constants.OPSIN_X_RESOLUTION;
import static com.dome.librarynightwave.utils.Constants.OPSIN_Y_RESOLUTION;
import static com.dome.librarynightwave.utils.Constants.RISCV;
import static com.dome.librarynightwave.utils.Constants.RTSP_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.RTSP_DISCONNECTED;
import static com.dome.librarynightwave.utils.Constants.RTSP_NONE;
import static com.dome.librarynightwave.utils.Constants.STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.STATE_FAILED;
import static com.dome.librarynightwave.utils.Constants.STREAM_QUALITY_HIGH;
import static com.dome.librarynightwave.utils.Constants.STREAM_QUALITY_LOW;
import static com.dome.librarynightwave.utils.Constants.VALUE;
import static com.dome.librarynightwave.utils.Constants.WIFI_RTOS;
import static com.dome.librarynightwave.utils.Constants.X_RESOLUTION;
import static com.dome.librarynightwave.utils.Constants.Y_RESOLUTION;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.currentStreamQuality;
import static com.dome.librarynightwave.utils.Constants.isDataBlockSent;
import static com.dome.librarynightwave.utils.Constants.isDisplayingWaitingTime;
import static com.dome.librarynightwave.utils.Constants.isEngineeringBuild;
import static com.dome.librarynightwave.utils.Constants.isLowres;
import static com.dome.librarynightwave.utils.Constants.locationLatitude;
import static com.dome.librarynightwave.utils.Constants.locationLongitude;
import static com.dome.librarynightwave.utils.Constants.mState;
import static com.dome.librarynightwave.utils.Constants.noOfByetsInBlock;
import static com.dome.librarynightwave.utils.Constants.opsinVersionDetails;
import static com.dome.librarynightwave.utils.Constants.opsinWiFiNewFirmwareVersion;
import static com.dome.librarynightwave.utils.Constants.rtspState;
import static com.dome.librarynightwave.utils.Constants.strUnknown;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_ACK_FAILURE;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_ACK_MAX;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_ACK_SUCCESS;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_ACK_UNKNOWN;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_ACK_UNSUPPORTED;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_OPSIN_FACTORTY_RESET.RESET_SOFT;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.applyPreset;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.lockApplySettings;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.responseReceived;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.liveViewErrorMessage;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.videoSurface;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.R;
import com.dome.librarynightwave.model.repository.mediaCodec.H264Utils;
import com.dome.librarynightwave.model.repository.mediaCodec.MediaDecoder;
import com.dome.librarynightwave.model.repository.mediaCodec.MediaEncoder;
import com.dome.librarynightwave.model.repository.opsinmodel.BatteryInfo;
import com.dome.librarynightwave.model.repository.opsinmodel.CompassAngles;
import com.dome.librarynightwave.model.repository.opsinmodel.DateTime;
import com.dome.librarynightwave.model.repository.opsinmodel.GpsPosition;
import com.dome.librarynightwave.model.repository.opsinmodel.ProductInfo;
import com.dome.librarynightwave.model.repository.opsinmodel.SDCardInfo;
import com.dome.librarynightwave.model.repository.pojo.OpsinPeriodicCommand;
import com.dome.librarynightwave.model.repository.pojo.OpsinRTPStats;
import com.dome.librarynightwave.model.repository.pojo.RTPStats;
import com.dome.librarynightwave.model.repository.videoencoder.Encoder;
import com.dome.librarynightwave.model.repository.videoencoder.MP4Encoder;
import com.dome.librarynightwave.model.services.PlayerService;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.CameraDetails;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.Event;
import com.dome.librarynightwave.utils.H264DecoderDeviceSpecific;
import com.dome.librarynightwave.utils.LocalWebServer;
import com.dome.librarynightwave.utils.SCCPConstants;
import com.dome.librarynightwave.utils.SCCPMessage;
import com.dome.librarynightwave.utils.VideoRecorder;
import com.dome.librarynightwave.utils.ZoomablePlayerView;
import com.dome.librarynightwave.utils.ZoomableTextureView;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPRepository implements TCPCommunicationService.Callbacks, MediaDecoder.Callbacks, H264DecoderDeviceSpecific.SizeChangedCallback {
    private final String TAG = "TCPRepository";
    private WifiManager.MulticastLock multicastLock;
    private final Application application;
    private static TCPRepository tcpRepository;
    private TCPCommunicationService mService;
    private boolean bound;
    private ZoomableTextureView opsinTextureView;
    private ZoomablePlayerView nwdTextureView;

    private Intent serviceIntent;

    private static MP4Encoder mp4encoderNightwave;
    public static boolean isRecordingStarted = false;
    public static boolean isCaptureImage = false;
    public static String RISCV_VERSION;
    public static String filepath;
    public static String imageFilePath;
    private boolean isVideoSizeSet = false;

    byte[] DIGITAL_SPS_BYTE_VALUE;

    byte[] DIGITAL_PPS_BYTE_VALUE;

    private static final int PROGRESS_TYPE_START = 0;
    private static final int PROGRESS_TYPE_CONTINUE = 1;
    private static final int PROGRESS_TYPE_COMPLETE = 2;
    private static final int PROGRESS_TYPE_CANCELED = 3;
    private static final int PROGRESS_TYPE_ERROR = 4;

    //    private static Surface videoSurface = null;
    private VideoRecorder videoRecorder;

    private boolean hasWrittenFirstKeyFrame = false;
    //    private H264DecoderDynamicFPS h264Decoder;
    private H264DecoderDeviceSpecific h264Decoder;
    private boolean shouldUseStartCode = false;
    private int VIDEO_WIDTH = 640;
    private int VIDEO_HEIGHT = 512;

    ExecutorService streamExecutor = Executors.newSingleThreadExecutor();

    /*
    GStreamed native functions
     */

    public native void nativeInit();

    public native void nativeStartStream(String rtspUrl);

    public native void nativeStopStream();

    public native void nativeRelease();

    public native void nativeSetGstDebug();

    public native void nativeChangeStreamUrl(String rtspUrl);

    public native void nativeStopRetryAndReset();

    private final MutableLiveData<Integer> isSocketConnected = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> _isSocketConnected = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateInvertVideo = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateIRCut = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateFpgaVer = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateRiscVer = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateReleaseVer = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateRiscBootMode = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateFlipVideo = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateCameraName = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateCameraPassword = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateCameraInfo = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateLedEnable = new MutableLiveData<>();
    public final MutableLiveData<Event<Object>> valueUpdateCameraMode = new MutableLiveData<>();
    public final MutableLiveData<Event<Object>> valueUpdateCameraVideoMode = new MutableLiveData<>();
    private final MutableLiveData<Object> valueUpdateCameraExposure = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> valueUpdateLiveViewBitmap = new MutableLiveData<>();
    private final MutableLiveData<RTPStats> valueUpdateLiveViewStats = new MutableLiveData<>();
    private final MutableLiveData<OpsinRTPStats> valueUpdateOpsinLiveViewStats = new MutableLiveData<>();
    public static final MutableLiveData<Object> fwUpdateProgress = new MutableLiveData<>();
    public final MutableLiveData<Event<Boolean>> circleProgressIndicator = new MutableLiveData<Event<Boolean>>();
    public static final MutableLiveData<Event<Object>> fwUpdateFailed = new MutableLiveData<>();
    public static final MutableLiveData<Event<Object>> startLiveView = new MutableLiveData<>();
    public static final MutableLiveData<Event<Object>> stopLiveView = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasSendLiveViewCommand = new MutableLiveData<>();
    private static final MutableLiveData<Event<Boolean>> hasRecordStoppedDuetoNoLivePackets = new MutableLiveData<>();

    /*Opsin*/
    private final MutableLiveData<Event<Object>> opsinCameraVersionInfo = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinProductInfo = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSerialNumber = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> opsinCameraStartRecord = new MutableLiveData<>();
    private final MutableLiveData<Boolean> opsinCameraStopRecord = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> opsinCameraImageCaptureSDCard = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> opsinCameraStartStreaming = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> opsinCameraStopStreaming = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> opsinCameraRecordingState = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> opsinCameraStreamingState = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> opsinCameraClockMode = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinCameraCommandError = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinWirelessPassword = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinWirelessMac = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetTimeZone = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetTimeZone = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetDateTime = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetDateTime = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetEv = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetBrightness = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetEv = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetDigitalZoom = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetDigitalZoom = new MutableLiveData<>();
    private final MutableLiveData<Object> opsinGetNUCState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetNUCState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetMICState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetMICState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetFrameRate = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetFrameRate = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetJpegCompression = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetJpegCompression = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetGpsInfo = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetGpsLocation = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetGpsPower = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetFactoryReset = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinCameraRestart = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetCameraName = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetCameraName = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetBatteryInfo = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinMICCommandError = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetImageState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetMasterVersion = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetMonochromeState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetMonochromeState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetNoiseState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetNoiseState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetROIState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetROIState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetClockMode = new MutableLiveData<>(); //system or gps
    private final MutableLiveData<Event<Object>> opsinGetMetadataState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetMetadataState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetSdCardState = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinGetCompassState = new MutableLiveData<>();
    public static final MutableLiveData<Event<Object>> opsinObserverUpgradeComplete = new MutableLiveData<>();
    public static final MutableLiveData<Event<Object>> opsinObserverUpgradeCompleteWaitMsg = new MutableLiveData<>();

    private final MutableLiveData<Event<Object>> opsinGetTimeFomat = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> opsinSetTimeFomat = new MutableLiveData<>();


    /*FW Update*/
    public static final String nightWaveWiFiDialogFileName = "nightwave/WIFI_DIALOG/WIFI_RTOS_3.2.4.0-2.2.0.img";
    private static final String nightWaveRiscvFileName = "nightwave/RISCV/RISCV_2.73.udf";
    private static final String nightWaveFpgaFileName = "nightwave/FPGA/FPGA_38.udf";


    /*Opsin*/
//    private static final String opsinFpgaFileName = "opsin/FPGA/sionyx-rtos-riscv-opsin_24_2_1-fpga_0206_8.udf";// opsin Falcon firmware file removed need to add again, if needed

    private static final String opsinFpgaFileName = "opsin/FPGA/sionyx-rtos-riscv-opsin_24_2_1-fpga_0206_2.udf";
    private static final String opsinFullFileName = "opsin/FULL/sionyx-rtos-riscv-opsin_24_2_1-full.udf";
    private static final String opsinRecoveryFileName = "opsin/RECOVERY/sionyx-rtos-riscv-opsin_recovery_24_2_1-riscv.udf";
    private static final String opsinRiscvFileName = "opsin/RISCV/sionyx-rtos-riscv-opsin_24_2_1-riscv.udf";
    private static final String opsinRiscvFpgaFileName = "opsin/RISCV_FPGA/sionyx-rtos-riscv-opsin_24_2_1-riscv+fpga_0206_2.udf";
    private static final String opsinRiscvOverlayFileName = "opsin/RISCV_OVERLAY/sionyx-rtos-riscv-opsin_24_2_1-riscv+overlay_1_1_4.udf";
    public static final String opsinWiFiDialogFileName = "opsin/WIFI_DIALOG/WIFI_RTOS_2_4_2.img";
    public static final String opsinWiFiBleFileName = "opsin/WIFI_BLE/WIFI_BLE_1_1_0.img";
    private InputStream input;
    private byte[] totalBuffersInRiscv = null;
    private boolean isRiscvUpdateStartCmdSent = false;
    private boolean riscOtaSendInProgress = false;
    public static boolean isRiscvUpdateCompleteSent = false;
    private short currentBlockNumber = 0;
    private int totalNumberOfBlocks = 0;
    private int totalFpgaRiscvFwLength = 0;
    private int receivedCount = -1;
    private LocalWebServer localWebServer;

    public static final int MODE_NONE = 0;
    public static final int MODE_RISCV = 2;
    public static final int MODE_FPGA = 3;
    public static final int MODE_WIFI_DIALOG = 4;
    public static final int MODE_RESET_FPGA = 5;
    public static final int MODE_WAIT = 6;
    public static final int MODE_OPSIN_FULL = 7;
    public static final int MODE_OPSIN_RISCV_OVERLAY = 8;
    public static final int MODE_OPSIN_RISCV_FPGA = 9;
    public static final int MODE_OPSIN_RECOVERY = 10;
    public static final int MODE_OPSIN_RESTART = 11;
    public static final int MODE_OPSIN_FACTORY = 12;
    public static final int MODE_OPSIN_BLE = 13;

    public static volatile int fwMode = MODE_NONE;
    private long waiting_time;
    private int countDown = 0;
    public static boolean isProgressCompleteReachedForWiFi;
    Timer timer = new Timer();
    private static boolean isSendCommand = false;
    public static boolean isStopPressed = false;
    private int retryCount = 0;
    private RTPStats rtpStats;
    private OpsinRTPStats opsinRTPStats;
    private int monitorUpgradeCompleteMessageCount = 0;
    private boolean opsinUpgradeCompleteReceived = false;
    private int opsinCommandRetryCount = 0;
    private boolean getVersionIsReceived = false;
    private int restartCountDown = 0;
    private final int restartWaitingTime = 6;
    private double previousTimeStamp = 0;
    private double waitingTimeForUpgradeCompleteProgress = 1;
    private ScheduledExecutorService scheduledTimerToMonitorUpgradeCompleteAfter100,
            scheduledWaitingTimerForUpgradeProgressUpgradeComplete,
            scheduledWaitMode;
    private Timer timerRestartResetWaitingTimer = null;

    private H264Utils h264Utils;
    private MediaDecoder mediaDecoder;
    private MediaEncoder mediaEncoder;
    public static int FRAME_RATE = 30;
    private static double previousTimeStampForRecording = -1;
    private static double currentTimeStampForRecording = -1;
    private ScheduledExecutorService scheduledExecutorService;

    /*
    NWD camera
     */
    private final MutableLiveData<Boolean> notifyDigitalStreamingStatus = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> notifyStreamingAttachedToSurfaceVew = new MutableLiveData<>();

    private final MutableLiveData<Integer> observeLiveFpsValue = new MutableLiveData<>();

    public MutableLiveData<Event<Integer>> notifyOnStreamingError = new MutableLiveData<>();

    public MutableLiveData<Boolean> notifyIsStreamingInBuffer = new MutableLiveData<>();

    public MutableLiveData<Event<Object>> notifyRetryValue = new MutableLiveData<>();

    public MutableLiveData<Boolean> notifyDigitalNoFpsFound = new MutableLiveData<>(); // no use 

    private MutableLiveData<Long> notifyDigitalMediaVideoSize = new MutableLiveData<>();

    private boolean isStreamingStarted = false;
    private boolean isStreamingSwitched = false;
    public static boolean isStartedReceivingPackets = false;
    private int BIT_RATE = 4000000;//4 MBPS

    // handled boolean to avoid popup overlay
    private boolean isErrorCaptured = false;
    private int retryCountValue = 0;
    private boolean isStreamingChanged = false;

    private void initializeObjects() {
        if (mediaDecoder == null) {
            mediaDecoder = MediaDecoder.getInstance(application);
            mediaDecoder.registerClient(TCPRepository.this);
            mediaDecoder.queueInputBuffer();
            mediaDecoder.renderOutputBuffer();
        }

        if (mediaEncoder == null) {
            mediaEncoder = MediaEncoder.getInstance(application);
        }

        if (h264Utils == null) {
            h264Utils = H264Utils.getInstance();
        }
    }


    public MutableLiveData<Integer> isSocketConnected() {
        return isSocketConnected;
    }

    public MutableLiveData<Event<Integer>> isSocketConnectedEvent() {
        return _isSocketConnected;
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {

            TCPCommunicationService.LocalBinder binder = (TCPCommunicationService.LocalBinder) service;
            mService = binder.getServiceInstance();
            mService.registerClient(TCPRepository.this);

            bound = true;
            connectSocket();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected: " + className.getClassName());
            mService = null;
            bound = false;
        }
    };

    public native short calculateCRC16FromJNI(byte[] bytes, int size);

    public native int calculateCRC32FromJNI(byte[] bytes, int size);

    public static TCPRepository getInstance(Application application) {
        if (tcpRepository == null) {
            tcpRepository = new TCPRepository(application);
            mp4encoderNightwave = new MP4Encoder();
        }
        return tcpRepository;
    }

    public TCPRepository(Application application) {
        this.application = application;
        WifiManager wifiManager = (WifiManager) application.getSystemService(WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("SiOnyx");
        multicastLock.setReferenceCounted(true);
        if (multicastLock != null && !multicastLock.isHeld())
            multicastLock.acquire();
    }


    public void startService() {
        if (bound) {
            Log.e(TAG, "startService: bounded");
            connectSocket();
        } else {
            Log.e(TAG, "startService: not bounded");
            Intent serviceIntent = new Intent(application, TCPCommunicationService.class);
            application.startForegroundService(serviceIntent);
            application.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }


    public void startPlayerService() {
        Log.e(TAG, "startPlayerService: ");
        if (serviceIntent == null) {
            serviceIntent = new Intent(application, PlayerService.class);
        }
        serviceIntent.setAction(ACTION_START_SERVICE);
        ContextCompat.startForegroundService(application, serviceIntent);
        application.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        nightwaveDigitalCameraInitializeStreaming();
    }


    public void stopRtspService() {
        Log.e(TAG, "stopRtspService: ");
        setStopStreamingDigitalCamera();

        mHandler.postDelayed(() -> {
            if (isStreamingStarted)
                nightwaveDigitalCameraReleaseLiveStreaming();

            mHandler.postDelayed(() -> {
                if (serviceIntent == null) {
                    serviceIntent = new Intent(application, PlayerService.class);
                }
                serviceIntent.setAction(ACTION_STOP_SERVICE);
                application.startService(serviceIntent);

            }, 1000);
        }, 1000);

    }


    public void onMediaRecordByteArrayWithPtsDts(ByteBuffer buffer, long ptsUs, long dtsUs) {
        ByteBuffer src = buffer.duplicate();
        src.position(buffer.position());
        src.limit(buffer.limit());
        int size = src.remaining();
        int flags = isIDRFrame(buffer) ? BUFFER_FLAG_KEY_FRAME : 0;

        if (videoRecorder != null && videoRecorder.isRecording()) {
            try {
                if (!hasWrittenFirstKeyFrame) {
                    if (isKeyFrame(buffer)) {
                        Log.d(TAG, "First_keyframe detected");
                        hasWrittenFirstKeyFrame = true;
                    }
                }

                if (hasWrittenFirstKeyFrame) {
                    boolean isKeyFrame = flags == BUFFER_FLAG_KEY_FRAME;
                    videoRecorder.writeFrame(buffer, System.nanoTime() / 1000, isKeyFrame);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while writing frame to recorder", e);
            }
        }

        if (h264Decoder != null && size > 0) {
            h264Decoder.enqueueFrame(buffer, size, 0, flags);
        }
    }


    private boolean isKeyFrame(ByteBuffer buffer) {
        int position = buffer.position();
        int limit = buffer.limit();

        while (position + 4 < limit) {
            // Look for start code (0x00000001)
            if (buffer.get(position) == 0x00 &&
                    buffer.get(position + 1) == 0x00 &&
                    buffer.get(position + 2) == 0x00 &&
                    buffer.get(position + 3) == 0x01) {

                int nalUnitHeader = buffer.get(position + 4) & 0x1F;
                // IDR frame NAL unit type in H.264 is 5
                if (nalUnitHeader == 5) {
                    return true;
                }
            }
            position++;
        }

        return false;
    }


    public static boolean isIDRFrame(ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() < 7) return false;

        ByteBuffer dup = buffer.duplicate();
        dup.position(0);
        int limit = dup.limit();

        while (dup.position() + 5 < limit) {
            if ((dup.get(dup.position()) & 0xFF) == 0x00 &&
                    (dup.get(dup.position() + 1) & 0xFF) == 0x00 &&
                    (dup.get(dup.position() + 2) & 0xFF) == 0x00 &&
                    (dup.get(dup.position() + 3) & 0xFF) == 0x01) {

                int nalHeaderIndex = dup.position() + 4;
                if (nalHeaderIndex < limit) {
                    int nalUnitType = dup.get(nalHeaderIndex) & 0x1F;
                    if (nalUnitType == 5) {
                        return true;
                    }
                }
            }
            dup.position(dup.position() + 1);
        }
        return false;
    }

    public void connectSocket() {
        if (bound) {
            mService.connectSocket();
        }
    }


    public LiveData<Object> observeInvertVideo() {
        return valueUpdateInvertVideo;
    }

    public LiveData<Object> observeFlipVideo() {
        return valueUpdateFlipVideo;
    }

    public LiveData<Object> observeIRCut() {
        return valueUpdateIRCut;
    }

    public LiveData<Object> observeFPGAVersion() {
        return valueUpdateFpgaVer;
    }

    public LiveData<Object> observeRiscVersion() {
        return valueUpdateRiscVer;
    }

    public LiveData<Object> observeReleaseVersion() {
        return valueUpdateReleaseVer;
    }

    public LiveData<Object> observeRiscBootMode() {
        return valueUpdateRiscBootMode;
    }

    public LiveData<Object> observeCameraName() {
        return valueUpdateCameraName;
    }

    public LiveData<Object> observeCameraPassword() {
        return valueUpdateCameraPassword;
    }

    public LiveData<Object> observeCameraInfo() {
        return valueUpdateCameraInfo;
    }

    public LiveData<Object> observeLedEnableState() {
        return valueUpdateLedEnable;
    }

    public LiveData<Event<Object>> observeCameraMode() {
        return valueUpdateCameraMode;
    }

    public LiveData<Event<Object>> observeCameraVideoMode() {
        return valueUpdateCameraVideoMode;
    }

    public LiveData<Object> observeCameraExposure() {
        return valueUpdateCameraExposure;
    }

    public MutableLiveData<Bitmap> getValueUpdateLiveViewBitmap() {
        return valueUpdateLiveViewBitmap;
    }

    public MutableLiveData<RTPStats> getValueUpdateLiveViewStats() {
        return valueUpdateLiveViewStats;
    }

    public MutableLiveData<OpsinRTPStats> getValueUpdateOpsinLiveViewStats() {
        return valueUpdateOpsinLiveViewStats;
    }

    public LiveData<Object> observeFwUpdateProgress() {
        return fwUpdateProgress;
    }

    public LiveData<Event<Boolean>> observeCircleProgressIndicator() {
        return circleProgressIndicator;
    }

    public LiveData<Event<Object>> observeFwUpdateFailed() {
        return fwUpdateFailed;
    }

    public LiveData<Event<Object>> observeStartLiveView() {
        return startLiveView;
    }

    public LiveData<Event<Object>> observeStopLiveView() {
        return stopLiveView;
    }

    public LiveData<Event<Boolean>> observeHasSendLiveViewCommand() {
        return hasSendLiveViewCommand;
    }

    public LiveData<Event<Boolean>> observeHasRecordStoppedDuetoNoLivePackets() {
        return hasRecordStoppedDuetoNoLivePackets;
    }


    /*Opsin*/
    public LiveData<Event<Object>> observeOpsinCommandError() {
        return opsinCameraCommandError;
    }

    public LiveData<Event<Object>> observeopsinMICCommandError() {
        return opsinMICCommandError;
    }

    public LiveData<Event<Object>> observeOpsinCameraVersionInfo() {
        return opsinCameraVersionInfo;
    }

    public LiveData<Event<Object>> observeOpsinProductInfo() {
        return opsinProductInfo;
    }

    public LiveData<Event<Object>> observeOpsinSerialNumber() {
        return opsinSerialNumber;
    }


    public LiveData<Event<Object>> observeOpsinGetTimeZone() {
        return opsinGetTimeZone;
    }

    public LiveData<Event<Object>> observeOpsinSetTimeZone() {
        return opsinSetTimeZone;
    }

    public LiveData<Event<Object>> observeOpsinGetDateTime() {
        return opsinGetDateTime;
    }

    public LiveData<Event<Object>> observeOpsinSetDateTime() {
        return opsinSetDateTime;
    }

    public LiveData<Event<Object>> observeOpsinGetEv() {
        return opsinGetEv;
    }

    public LiveData<Event<Object>> observeOpsinGetBrightness() {
        return opsinGetBrightness;
    }

    public LiveData<Event<Object>> observeOpsinSetEv() {
        return opsinSetEv;
    }

    public LiveData<Event<Object>> observeOpsinGetDigitalZoom() {
        return opsinGetDigitalZoom;
    }

    public LiveData<Event<Object>> observeOpsinSetDigitalZoom() {
        return opsinSetDigitalZoom;
    }

    public LiveData<Object> observeOpsinGetNUCState() {
        return opsinGetNUCState;
    }

    public LiveData<Event<Object>> observeOpsinSetNUCState() {
        return opsinSetNUCState;
    }

    public LiveData<Event<Object>> observeOpsinGetMICState() {
        return opsinGetMICState;
    }

    public LiveData<Event<Object>> observeOpsinSetMICState() {
        return opsinSetMICState;
    }

    public LiveData<Event<Object>> observeOpsinGetFrameRate() {
        return opsinGetFrameRate;
    }

    public LiveData<Event<Object>> observeOpsinSetFrameRate() {
        return opsinSetFrameRate;
    }

    public LiveData<Event<Object>> observeOpsinGetJpegCompression() {
        return opsinGetJpegCompression;
    }

    public LiveData<Event<Object>> observeOpsinSetJpegCompression() {
        return opsinSetJpegCompression;
    }

    public LiveData<Event<Object>> observeOpsinGetCameraName() {
        return opsinGetCameraName;
    }

    public LiveData<Event<Object>> observeOpsinSetCameraName() {
        return opsinSetCameraName;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStartRecord() {
        return opsinCameraStartRecord;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraImageCaptureSDCard() {
        return opsinCameraImageCaptureSDCard;
    }

    public LiveData<Boolean> observeOpsinCameraStopRecord() {
        return opsinCameraStopRecord;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStartStreaming() {
        return opsinCameraStartStreaming;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraRecordingState() {
        return opsinCameraRecordingState;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStreamingState() {
        return opsinCameraStreamingState;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraClockMode() {
        return opsinCameraClockMode;
    }

    public LiveData<Event<Object>> observeSetOpsinCameraClockMode() {
        return opsinSetClockMode;
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStopStreaming() {
        return opsinCameraStopStreaming;
    }

    public LiveData<Event<Object>> observeOpsinWirelessPassword() {
        return opsinWirelessPassword;
    }

    public LiveData<Event<Object>> observeOpsinWirelessMac() {
        return opsinWirelessMac;
    }

    public LiveData<Event<Object>> observeOpsinGetGpsInfo() {
        return opsinGetGpsInfo;
    }

    public LiveData<Event<Object>> observeOpsinGetGpsLocation() {
        return opsinGetGpsLocation;
    }

    public LiveData<Event<Object>> observeOpsinSetGpsPower() {
        return opsinSetGpsPower;
    }

    public LiveData<Event<Object>> observeOpsinSetFactoryReset() {
        return opsinSetFactoryReset;
    }

    public LiveData<Event<Object>> observeOpsinGetBatteryInfo() {
        return opsinGetBatteryInfo;
    }

    public LiveData<Event<Object>> observeOpsinGetImageState() {
        return opsinGetImageState;
    }

    public LiveData<Event<Object>> observeOpsinCameraRestart() {
        return opsinCameraRestart;
    }

    public LiveData<Event<Object>> observerUpgradeComplete() {
        return opsinObserverUpgradeComplete;
    }

    public LiveData<Event<Object>> observerUpgradeCompleteWaitMsg() {
        return opsinObserverUpgradeCompleteWaitMsg;
    }

    public LiveData<Event<Object>> observeOpsinSetMasterVersion() {
        return opsinSetMasterVersion;
    }

    public LiveData<Event<Object>> observeOpsinMonochromeState() {
        return opsinGetMonochromeState;
    }

    public LiveData<Event<Object>> observeSetOpsinMonochromeState() {
        return opsinSetMonochromeState;
    }

    public LiveData<Event<Object>> observeOpsinNoiseState() {
        return opsinGetNoiseState;
    }

    public LiveData<Event<Object>> observeSetOpsinNoiseState() {
        return opsinSetNoiseState;
    }

    public LiveData<Event<Object>> observeOpsinROIState() {
        return opsinGetROIState;
    }

    public LiveData<Event<Object>> observeSetOpsinROIState() {
        return opsinSetROIState;
    }

    public LiveData<Event<Object>> observeOpsinMetadataState() {
        return opsinGetMetadataState;
    }

    public LiveData<Event<Object>> observeSetOpsinMetadataState() {
        return opsinSetMetadataState;
    }

    public LiveData<Event<Object>> observeGetOpsinTimeFormat() {
        return opsinGetTimeFomat;
    }

    public LiveData<Event<Object>> observeSetOpsinTimeFormat() {
        return opsinSetTimeFomat;
    }

    public LiveData<Event<Object>> observeOpsinSdCardInfo() {
        return opsinGetSdCardState;
    }

    public LiveData<Event<Object>> observeOpsinCompassState() {
        return opsinGetCompassState;
    }

    /*
    NWD camera
     */
    public LiveData<Boolean> observeStreamingStatus() {
        return notifyDigitalStreamingStatus;
    }

    public LiveData<Event<Boolean>> observeStreamingAttachedToSurfaceView() {
        return notifyStreamingAttachedToSurfaceVew;
    }

    public void startFwUpdate(String sequences) {
        AssetManager assetManager = application.getAssets();
        if (sequences.equalsIgnoreCase(RISCV)) {
            Log.e(TAG, "MODE: riscv");
            try {
                resetToDefault();
                fwMode = MODE_RISCV;
                input = assetManager.open(nightWaveRiscvFileName);
                totalFpgaRiscvFwLength = input.available();
                totalNumberOfBlocks = totalFpgaRiscvFwLength / noOfByetsInBlock;
                startFpgaRiscUpdate(totalFpgaRiscvFwLength);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (sequences.equalsIgnoreCase(FPGA)) {
            Log.e(TAG, "MODE: fpga");
            fwMode = MODE_FPGA;
            try {
                resetToDefault();
                fwMode = MODE_RISCV;
                input = assetManager.open(nightWaveFpgaFileName);
                totalFpgaRiscvFwLength = input.available();
                totalNumberOfBlocks = totalFpgaRiscvFwLength / noOfByetsInBlock;
                startFpgaRiscUpdate(totalFpgaRiscvFwLength);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (sequences.equalsIgnoreCase(Constants.WIFI_RTOS)) {
            Log.e(TAG, "MODE: wifi_rtos");
            try {
                fwMode = MODE_WIFI_DIALOG;
                if (bound) {
                    localWebServer = new LocalWebServer(LOCAL_SERVER_PORT, nightWaveWiFiDialogFileName, application, application);
                }
                localWebServer.start();
                String wiFiDialogUrl = getWiFiDialogUrl();
                new Handler().postDelayed(() -> startWiFiDialogUpdate(wiFiDialogUrl), 1000);

            } catch (Exception e) {
                Log.e(TAG, "startFwUpdate: " + e.getLocalizedMessage());
                stopFwUpdate();
                e.printStackTrace();
            }
        } else if (sequences.equalsIgnoreCase(Constants.REBOOT_FPGA)) {
            Log.e(TAG, "MODE: reboot_fpga");
            fwMode = MODE_RESET_FPGA;
            resetFPGA(false);
            fwUpdateProgress.postValue(100);
        } else if (sequences.contains(Constants.WAIT)) {
            fwMode = MODE_WAIT;
            String[] waiting = sequences.split(" ");
            String waitingTime = waiting[1];
            int t = Integer.parseInt(waitingTime);
            waiting_time = t * 1000L;

            Log.e(TAG, "MODE: wait start");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (countDown < waiting_time) {
                        int percentage = (int) ((countDown * 100) / waiting_time);
                        fwUpdateProgress.postValue(percentage);
                        countDown++;
                    } else {
                        Log.e(TAG, "MODE: wait end");
                        fwMode = MODE_NONE;
                        fwUpdateProgress.postValue(100);
                        resetToDefault();

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        fwUpdateProgress.postValue(-1);
                        timer.cancel();
                    }
                }
            }, 1, 1);

        }
    }

    private void resetToDefault() {
        input = null;
        totalBuffersInRiscv = null;
        isRiscvUpdateStartCmdSent = false;
        riscOtaSendInProgress = false;
        isRiscvUpdateCompleteSent = false;
        currentBlockNumber = 0;
        totalNumberOfBlocks = 0;
        totalFpgaRiscvFwLength = 0;
        receivedCount = -1;

        waiting_time = 0;
        countDown = 0;
    }

    public void stopFwUpdate() {
        if (localWebServer != null)
            localWebServer.stop();
    }

    private String getWiFiDialogUrl() {
        WifiManager wm = (WifiManager) application.getSystemService(WIFI_SERVICE);
        String GET_URI = "";
        try {
            String ip = InetAddress.getByAddress(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(wm.getConnectionInfo().getIpAddress()).array()).getHostAddress();
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    GET_URI = "http://" + ip + ":" + LOCAL_SERVER_PORT + "/" + nightWaveWiFiDialogFileName;
                    break;
                case OPSIN:
                    GET_URI = "http://" + ip + ":" + LOCAL_SERVER_PORT + "/" + opsinWiFiDialogFileName;
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG, "getWifiDialogUrl : NW_Digital");
                    break;
            }
            Log.i("TAG", "onCreate: ota_update rtos " + GET_URI);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return GET_URI;
    }

    private void readFile(int index) {
        try {
            if (currentBlockNumber == 0) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                int available = input.available();
                totalBuffersInRiscv = new byte[available];

                int length = input.read(totalBuffersInRiscv);
                if (length != -1) {
                    output.write(totalBuffersInRiscv, index, length);
                    totalBuffersInRiscv = output.toByteArray();
                    output.flush();
                    output.close();

                    byte[] dest = new byte[noOfByetsInBlock];
                    System.arraycopy(totalBuffersInRiscv, index, dest, 0, dest.length);
                    sendDataBlockToRiscv(dest, currentBlockNumber);
                    currentBlockNumber++;
                }
                input.close();
            } else {
                byte[] destination = new byte[0];
                int remainingBytes = totalFpgaRiscvFwLength - index;
                if (remainingBytes < noOfByetsInBlock) {
                    if (remainingBytes > 0)
                        destination = new byte[remainingBytes];
                } else {
                    if (noOfByetsInBlock > 0)
                        destination = new byte[noOfByetsInBlock];
                }
                if (destination.length != 0) {
                    System.arraycopy(totalBuffersInRiscv, index, destination, 0, destination.length);
                    sendDataBlockToRiscv(destination, currentBlockNumber);
                    currentBlockNumber++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startWiFiDialogUpdate(String wiFiDialogUrl) {
        if (bound) {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    mService.startWiFiDialogUpdate(wiFiDialogUrl);
                    break;
                case OPSIN:
                    startOpsinDialogUpgrade(wiFiDialogUrl);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG, "startWiFiDialogUpdate : NW_Digital");
                    break;
            }
        }
    }


    public void startFpgaRiscUpdate(int length) {
        if (bound) {
            riscOtaSendInProgress = true;
            mService.startRiscUpdate(length);
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                startTimerRequest();// start timer and reset once fw update requested
                circleProgressIndicator.postValue(new Event<>(true));
            }
        }
    }


    private static final long TIMEOUT_MS = 10_000; // 10 seconds
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private void startTimerRequest() {
        stopTimerRequest(); // reset timer

        timeoutRunnable = () -> {
            // wait for 10 seconds for response then notify to user
            Log.e(TAG, "TimerRequest: timeout reached ");
                circleProgressIndicator.postValue(new Event<>(false));
                onResponseTimeout("NO_RESPONSE_TIMEOUT");
                stopTimerRequest(); // timeout reached, stopped the timer
        };

        handler.postDelayed(timeoutRunnable, TIMEOUT_MS);

    }

    private void stopTimerRequest() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    public void sendDataBlockToRiscv(byte[] dataBlock, short blockNum) {
        if (bound) {
            mService.sendDataBlockToRiscv(dataBlock, blockNum);
        }
    }

    public void completeUpdate() {
        if (bound) {
            mService.completeUpdate();
            riscOtaSendInProgress = false;
        }
    }

    public void completeUpdateWifiDialog() {
        if (bound) {
            mService.completeUpdateWifiDialog();
        }
    }

    public void fwUpdateCancel() {
        if (bound) {
            mService.fwUpdateCancel();
            stopTimerRequest();// BG behaviour
        }
    }

    public void resetFPGA(boolean isGolden) {
        if (bound) {
            Log.e(TAG, "resetFPGA:");
            TCPCommunicationService.seconds = 0;
            mService.resetFpga(isGolden);
        }
    }

    public void setInvertVideo(int isTrue) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.InvertImage.getValue();
            byte data;
            switch (isTrue) {
                default:
                case 0:
                    data = (byte) SCCPConstants.SCCP_INVERT_IMAGE.FALSE.getValue();
                    break;
                case 1:
                    data = (byte) SCCPConstants.SCCP_INVERT_IMAGE.TRUE.getValue();
                    break;
            }
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setFlipVideo(int isTrue) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.MirrorImage.getValue();
            byte data;
            switch (isTrue) {
                default:
                case 0:
                    data = (byte) SCCPConstants.SCCP_FLIP_IMAGE.FALSE.getValue();
                    break;
                case 1:
                    data = (byte) SCCPConstants.SCCP_FLIP_IMAGE.TRUE.getValue();
                    break;
            }
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setIRCut(int val) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.IR_Cut.getValue();
            byte data;
            switch (val) {
                default:
                case 0:
                    data = (byte) SCCPConstants.SCCP_IRCUT.OUT.getValue();
                    break;
                case 1:
                    data = (byte) SCCPConstants.SCCP_IRCUT.IN.getValue();
                    break;
                case 2:
                    data = (byte) SCCPConstants.SCCP_IRCUT.AUTO.getValue();
                    break;
            }
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setChangeContrast(int value) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.Contrast.getValue();
            byte data = (byte) value;
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setChangeSharpness(int value) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.Sharpness.getValue();
            byte data = (byte) value;
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setChangeSaturation(int value) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.Saturation.getValue();
            byte data = (byte) value;
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setCameraName(String cameraName) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue();
            byte[] data = cameraName.getBytes();
            mService.setValue(value_id, data);
        }
    }

    public void setTopReleaseVersion(String releaseVersion) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.ReleasePkgVer.getValue();
            byte[] data = releaseVersion.getBytes();
            mService.setValue(value_id, data);
            Log.e(TAG, "setTopReleaseVersion: " + releaseVersion);
        }
    }

    public void setLedEnableState(int isTrue) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue();
            byte data;
            switch (isTrue) {
                default:
                case 0:
                    data = (byte) SCCPConstants.SCCP_LED.TRUE.getValue();
                    break;
                case 1:
                    data = (byte) SCCPConstants.SCCP_LED.FALSE.getValue();
                    break;
            }
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setCameraExposure(int value) {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.Exposure.getValue();
            byte data = (byte) value;
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void setCameraVideoMode(int value) {
        if (bound) {
            retryCount = 0;
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.ForceAnalog.getValue();
            byte data;
            switch (value) {
                default:
                case 0:
                    data = (byte) SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue();
                    break;
                case 1:
                    data = (byte) SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue();
                    break;
            }
            mService.setValue(value_id, new byte[]{data});
        }
    }

    public void getInvertImage() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.InvertImage.getValue();
            mService.getValue(value_id);
        }
    }

    public void getFlipImage() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.MirrorImage.getValue();
            mService.getValue(value_id);
        }
    }

    public void getIRCut() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.IR_Cut.getValue();
            mService.getValue(value_id);
        }
    }

    public void getFPGAVersion() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.FpgaVer.getValue();
            mService.getValue(value_id);
        }
    }

    public void getRiscVersion() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.RiscVer.getValue();
            mService.getValue(value_id);
        }
    }

    public void getReleasePkgVer() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.ReleasePkgVer.getValue();
            mService.getValue(value_id);
        }
    }

    public void getRiscBootMode() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.RiscBootMode.getValue();
            mService.getValue(value_id);
        }
    }

    public void getCameraInfo() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.CameraInfo.getValue();
            mService.getValue(value_id);
        }
    }

    public void getCameraName() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue();
            mService.getValue(value_id);
        }
    }

    public void getWiFiPassword() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.WiFiPassword.getValue();
            mService.getValue(value_id);
        }
    }

    public void getLedEnableState() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue();
            mService.getValue(value_id);
        }
    }

    public void getCameraExposure() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.Exposure.getValue();
            mService.getValue(value_id);
        }
    }

    public void getCameraMode() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.BootMode.getValue();
            mService.getValue(value_id);
        }
    }

    public void getVideoMode() {
        if (bound) {
            byte value_id = (byte) SCCPConstants.SCCP_VALUE.VideoMode.getValue();
            mService.getValue(value_id);
        }
    }

    public void disconnectSocket() {
        if (bound) {
            mService.disconnectSocket();
            stopTimerRequest();// socket disconnected
        }
    }

    public void startLiveView(boolean isManual) {
        if (bound) {
            Log.e(TAG, "startLiveView: ");
            byte value_id = (byte) SCCPConstants.SCCP_ACTION.StartLiveView.getValue();
            mService.startStopLiveView(value_id);
            isStopPressed = false;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isSendCommand && !isStopPressed) {
                        retrySendCommand();
                    } else if (!isSendCommand && !isManual) {
                        TCPCommunicationService.seconds = 0;
                        retrySendCommand();
                    } else {
                        Log.d(TAG, "ELSE isSend Command isManual: ");
                    }
                }
            }, 3000);
        }
    }


    private void startListenUdp() {
        mService.startListenForUDPBroadcast();
    }

    public void stopLiveView() {
        if (bound) {
            Log.e(TAG, "stopLiveView: ");
            stopUdpListeners();
            byte value_id = (byte) SCCPConstants.SCCP_ACTION.StopLiveView.getValue();
            mService.startStopLiveView(value_id);
            isStopPressed = true;
            preGoodFrameCount = 0;
            retryCount = 0;
        }
    }



    private void onResponseTimeout(String error) {
        if (error != null) {
            Log.e(TAG, "onResponseTimeout: called ");
            stopFwUpdate();
            fwUpdateCancel();
            resetToDefault();
            fwUpdateFailed.postValue(new Event<>(error));
        }

    }

    @Override
    public void isTcpConnected(int tcpConnectionState) {
        Log.d(TAG, "TCP_CONNECTION : " + tcpConnectionState + " " + isSocketConnected);
        if (tcpConnectionState == STATE_CONNECTED) {//used to enable start live streaming when First time socket connection or Reset socket connection,
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    shouldReceiveUDP = false;
                    isSendCommand = false;
                    break;
                case OPSIN:
                    isOpsinLiveStreamingStarted = false;
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG, "isTCPCOnnected : NW_Digital");
                    break;
            }
        }
        isSocketConnected.postValue(tcpConnectionState);
        _isSocketConnected.postValue(new Event<>(tcpConnectionState));
    }

    private byte[] removeFirstElement(byte[] arr) {
        byte[] newArr = new byte[arr.length - 1];
        System.arraycopy(arr, 1, newArr, 0, arr.length - 1);
        return newArr;
    }

    @Override
    public void updateClient(byte[] data, int received_length, int valueId, int isValueEventAction) {
        String res = Arrays.toString(data);
        Log.d(TAG, "Recieved from server : " + res);

        byte valueEventActionIds = SCCPMessage.getInstance().getValueEventActionIds();
        byte msg_type = SCCPMessage.getInstance().getMsg_type();
        if (valueEventActionIds == (byte) SCCPConstants.SCCP_VALUE.ForceAnalog.getValue()) {
            if (data.length >= 14) {//sometimes added duplicate "Message type" in the response for ForceAnalog. so removing first value
                data = removeFirstElement(data);
            }
        }
        if (msg_type == (byte) SCCPConstants.SCCP_MSG_TYPE.DO_ACTION.getValue() && valueEventActionIds == (byte) SCCPConstants.SCCP_ACTION.StartLiveView.getValue()) {
            if (data[0] == 2) {// Start Live View response receiving as message type 2 when changing usb - wifi, so to trigger restart making boolean is false
                isSendCommand = false;
            }
        }
        if (msg_type == (byte) SCCPConstants.SCCP_MSG_TYPE.UPDATE_START.getValue() && valueEventActionIds == 0) {
            if (data[0] == 2) {// Start Live View response receiving as message type 2 when changing usb - wifi, so to trigger restart making boolean is false
                if (bound) {
                    riscOtaSendInProgress = true;
                    mService.startRiscUpdate(totalFpgaRiscvFwLength);
                }
            }
        }

        SCCPMessage sccpMessage = new SCCPMessage();
        sccpMessage.setMsg_type(data[0]);
        sccpMessage.setValueEventActionIds(data[1]);
        sccpMessage.setBlock_num(bytesToShort(new byte[]{data[2], data[3]}));
        sccpMessage.setData_size(bytesToShort(new byte[]{data[4], data[5]}));
        sccpMessage.setSequence_num(bytesToShort(new byte[]{data[6], data[7]}));
        sccpMessage.setPriority(data[8]);
        sccpMessage.setReserved(new byte[]{data[9], data[10], data[11]});

        byte[] payLoad;
        if (fwMode == MODE_NONE) {
            payLoad = Arrays.copyOfRange(data, 12, data.length);
        } else {
            if (sccpMessage.getMsg_type() == (byte) SCCPConstants.SCCP_MSG_TYPE.UPDATE_PROGRESS.getValue()) {
                if (data.length > 13)
                    payLoad = Arrays.copyOfRange(data, data.length - 1, data.length);
                else
                    payLoad = Arrays.copyOfRange(data, 12, data.length);
            } else {
                payLoad = Arrays.copyOfRange(data, 12, data.length);
            }
        }
        sccpMessage.setData(payLoad);

        if (sccpMessage.getMsg_type() == (byte) SCCPConstants.SCCP_MSG_TYPE.VALUE_UPDATE.getValue()) {
            byte[] resData = sccpMessage.getData();
            byte resValueId = sccpMessage.getValueEventActionIds();
            int length = resData.length;
            Log.e(TAG, "updateClient: " + Arrays.toString(resData) + " " + resValueId);
            if (length > 0) {
                byte value = resData[0];
                if (resValueId == (byte) SCCPConstants.SCCP_VALUE.MirrorImage.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    }
                    valueUpdateFlipVideo.postValue(value != 0);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.InvertImage.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    }
                    valueUpdateInvertVideo.postValue(value != 0);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.IR_Cut.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    }
                    valueUpdateIRCut.postValue((int) value);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.FpgaVer.getValue()) {
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    valueUpdateFpgaVer.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.RiscVer.getValue()) {
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    valueUpdateRiscVer.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.ReleasePkgVer.getValue()) {
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    valueUpdateReleaseVer.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.RiscBootMode.getValue()) {
                    Log.e(TAG, "updateClient: RiscBootMode");
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    valueUpdateRiscBootMode.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue()) {
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    Log.e(TAG, "updateCameraName: " + val + " " + (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue());
                    valueUpdateCameraName.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.WiFiPassword.getValue()) {
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    Log.e(TAG, "valueUpdateCameraPassword: " + val + " " + (byte) SCCPConstants.SCCP_VALUE.WiFiPassword.getValue());
                    valueUpdateCameraPassword.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.CameraInfo.getValue()) {
                    String val = new String(payLoad, StandardCharsets.UTF_8);
                    valueUpdateCameraInfo.postValue(val);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    }
                    valueUpdateLedEnable.postValue(value != 0);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.BootMode.getValue()) {
                    Log.e(TAG, "valueUpdateCameraMode: " + value);
                    if (value == (byte) SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                        valueUpdateCameraMode.postValue(new Event<>((int) value));
                    } else if (value == (byte) SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                        valueUpdateCameraMode.postValue(new Event<>((int) value));
                    }
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.VideoMode.getValue()) {
                    Log.e(TAG, "valueUpdateVideoMode: " + value);
                    if (value == (byte) SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                        valueUpdateCameraVideoMode.postValue(new Event<>((int) value));
                    } else if (value == (byte) SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                        //valueUpdateCameraVideoMode.postValue((int) value);
                        valueUpdateCameraVideoMode.postValue(new Event<>((int) value));
                    }
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.ForceAnalog.getValue()) {
                    Log.e(TAG, "valueUpdateForceAnalog: " + value);
                    valueUpdateCameraVideoMode.postValue(new Event<>((int) value));
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.Exposure.getValue()) {
                    Log.e(TAG, "valueUpdateCameraExposure: " + value + "/" + valueId);
                    valueUpdateCameraExposure.postValue((int) value);
                }
            } else {
                Log.e(TAG, "updateClient: ZERO LENGTH");
                if (valueId == (byte) SCCPConstants.SCCP_VALUE.MirrorImage.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateFlipVideo.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.InvertImage.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateInvertVideo.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.IR_Cut.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateIRCut.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.FpgaVer.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateFpgaVer.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.RiscVer.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateRiscVer.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.ReleasePkgVer.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateReleaseVer.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.RiscBootMode.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateRiscBootMode.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraName.postValue(error);
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.WiFiPassword.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraPassword.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.CameraInfo.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraInfo.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateLedEnable.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.BootMode.getValue()) {
                    Log.d(TAG, "updateClient: CameraMode : failed");
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraMode.postValue(new Event<>(error));
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.VideoMode.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    //valueUpdateCameraVideoMode.postValue(error);
                    valueUpdateCameraVideoMode.postValue(new Event<>(error));
                } else if (resValueId == (byte) SCCPConstants.SCCP_VALUE.ForceAnalog.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraVideoMode.postValue(new Event<>(error));
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.Exposure.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraExposure.postValue(error);
                }
            }
        } else if (sccpMessage.getMsg_type() == (byte) SCCPConstants.SCCP_MSG_TYPE.RESPONSE.getValue()) {
            if (isValueEventAction == VALUE) {
                if (valueId == (byte) SCCPConstants.SCCP_VALUE.MirrorImage.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateFlipVideo.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.InvertImage.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateInvertVideo.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.IR_Cut.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateIRCut.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.FpgaVer.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateFpgaVer.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.RiscVer.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateRiscVer.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.ReleasePkgVer.getValue()) {
                    Log.e(TAG, "ReleasePkgVer: " + valueId);
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateReleaseVer.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.RiscBootMode.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateRiscBootMode.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraName.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.WiFiPassword.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraPassword.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.CameraInfo.getValue()) {
                    Log.e(TAG, "CameraInfo: " + valueId);
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraInfo.postValue(error);
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue()) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                        applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                        synchronized (lockApplySettings) {
                            lockApplySettings.notifyAll();
                            responseReceived = true;
                        }
                    } else {
                        CommandError error = new CommandError();
                        error.setError("Failed");
                        valueUpdateLedEnable.postValue(error);
                    }
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.BootMode.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraMode.postValue(new Event<>(error));
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.VideoMode.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraVideoMode.postValue(new Event<>(error));
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.ForceAnalog.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraVideoMode.postValue(new Event<>(error));
                } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.Exposure.getValue()) {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateCameraExposure.postValue(error);
                } else if (riscOtaSendInProgress) {
                    circleProgressIndicator.postValue(new Event<>(false));// dismiss indicator
                    if (fwMode == MODE_RISCV || fwMode == MODE_FPGA) {
                        if (isRiscvUpdateStartCmdSent) {
                            isRiscvUpdateStartCmdSent = false;
                            readFile(0);
                        } else if (receivedCount == totalNumberOfBlocks) {
                            completeUpdate();
                            receivedCount = -1;
                            isRiscvUpdateCompleteSent = true;
                            Log.e(TAG, "completeUpdate:");
                            stopTimerRequest();// update completed
                        } else if (riscOtaSendInProgress && receivedCount <= totalNumberOfBlocks) {
                            if (isRiscvUpdateCompleteSent) {
                                isRiscvUpdateCompleteSent = false;
                            } else {
                                receivedCount = receivedCount + 1;
                                int index = (currentBlockNumber * noOfByetsInBlock);
                                int percentage = (int) ((currentBlockNumber * 100) / totalNumberOfBlocks) / 2;
                                fwUpdateProgress.postValue(percentage);
                                Log.d(TAG, "ReceivedCount: " + Arrays.toString(payLoad) + " " + receivedCount + " " + percentage + "%");
                                readFile(index);

                                startTimerRequest();// every update reset and start timer
                            }
                        }
                    }
                }
            } else if (isValueEventAction == ACTION) {
                byte[] resData = sccpMessage.getData();
                byte value = resData[0];
                Log.e(TAG, "updateClient: " + Arrays.toString(resData) + "  / valueId:" + valueId + "/ response value:" + value);

                if (valueId == (byte) SCCPConstants.SCCP_ACTION.StartLiveView.getValue()) {
                    isSendCommand = true;
                    startLiveView.postValue(new Event<>(value));
                    startListenUdp();
                    shouldReceiveUDP = true;
                    retryCount = 0;
                } else if (valueId == (byte) SCCPConstants.SCCP_ACTION.StopLiveView.getValue()) {
                    mService.closeNWUdp();
                    stopLiveView.postValue(new Event<>(value));
                    isSendCommand = false;
                }

            } else if (isValueEventAction == EVENT) {

            }


        } else if (sccpMessage.getMsg_type() == (byte) SCCPConstants.SCCP_MSG_TYPE.UPDATE_PROGRESS.getValue()) {
            if (fwMode == MODE_RISCV || fwMode == MODE_FPGA) {
                if (isRiscvUpdateCompleteSent) {
                    isRiscvUpdateCompleteSent = false;
                } else {
                    int baseValue = 50;
                    int progress = payLoad[0];
                    int i = baseValue + (progress / 2);
                    fwUpdateProgress.postValue(i);
                    if (i == 100) {
                        fwMode = MODE_NONE;
                    }
                    Log.e(TAG, "UPDATE_PROGRESS: " + progress);
                }
            }
            if (fwMode == MODE_WIFI_DIALOG) {
                Log.d(TAG, "updateClient: MODE_WIFI_DIALOG");
                int progress = payLoad[0];
                fwUpdateProgress.postValue(progress);
                if (progress == 100) {
                    isProgressCompleteReachedForWiFi = true;
                    fwMode = MODE_NONE;
                    try {
                        Thread.sleep(1000);
                        stopFwUpdate();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.e(TAG, "UPDATE_PROGRESS_WIFI_DIALOG: " + progress);
            }
        } else if (sccpMessage.getMsg_type() == (byte) SCCPConstants.SCCP_MSG_TYPE.SO_READ_TIMEOUT.getValue()) {
            if (valueId == (byte) SCCPConstants.SCCP_VALUE.MirrorImage.getValue()) {
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                    applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                } else {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateFlipVideo.postValue(error);
                    isTcpConnected(STATE_FAILED);
                }
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.InvertImage.getValue()) {
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                    applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                } else {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateInvertVideo.postValue(error);
                    isTcpConnected(STATE_FAILED);
                }
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.IR_Cut.getValue()) {
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                    applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                } else {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateIRCut.postValue(error);
                    isTcpConnected(STATE_FAILED);
                }
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.FpgaVer.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateFpgaVer.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.RiscVer.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateRiscVer.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.ReleasePkgVer.getValue()) {
                Log.e(TAG, "ReleasePkgVer1: " + valueId);
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateReleaseVer.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.RiscBootMode.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateRiscBootMode.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.CameraName.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateCameraName.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.WiFiPassword.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateCameraPassword.postValue(error);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.CameraInfo.getValue()) {
                Log.e(TAG, "CameraInfo1: " + valueId);
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateCameraInfo.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.LedEnable.getValue()) {
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                    applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
                } else {
                    CommandError error = new CommandError();
                    error.setError("Failed");
                    valueUpdateLedEnable.postValue(error);
                    isTcpConnected(STATE_FAILED);
                }
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.BootMode.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateCameraMode.postValue(new Event<>(error));
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.VideoMode.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateCameraVideoMode.postValue(new Event<>(error));
                isTcpConnected(STATE_FAILED);
            } else if (valueId == (byte) SCCPConstants.SCCP_VALUE.Exposure.getValue()) {
                CommandError error = new CommandError();
                error.setError("Failed");
                valueUpdateCameraExposure.postValue(error);
                isTcpConnected(STATE_FAILED);
            } else {
                if (fwMode == MODE_RISCV || fwMode == MODE_FPGA || fwMode == MODE_WIFI_DIALOG) {
                    fwMode = MODE_NONE;
                    sendUpgradeFailed();
                    Log.e(TAG, "UPDATE_PROGRESS_WIFI_DIALOG: FAILED FW UPDATE");
                } else {
                    Log.e(TAG, "UPDATE_PROGRESS_WIFI_DIALOG: NO VALUE");
                }
            }
        }
    }


    @Override
    public void updateLiveView(byte[] data, int received_length) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, received_length);
        valueUpdateLiveViewBitmap.postValue(bitmap);

        if (isCaptureImage) {
            Bitmap bitmapToSave = BitmapFactory.decodeByteArray(data, 0, data.length);
            saveImageToMediaStore(bitmapToSave);
        }
        if (isRecordingStarted) {
            doVideoRecording(data);
        }
    }


    private int preGoodFrameCount = 0;

    @Override
    public void updateGoodBadPackets(int goodFrameCount, int jpeg_last_fragment_offset, int fps, int receiveUdpPacketsCount, int rtpSkippedPacket, int jpegBadStart, int jpegBadEndCount, int jpegCorruptCounts, int fpsBasedOnGoodPackets) {
        if (rtpStats == null)
            rtpStats = new RTPStats();
        if (fpsBasedOnGoodPackets > 0) {
            FRAME_RATE = fpsBasedOnGoodPackets;
//            Log.e(TAG, "updateGoodBadPackets: " + fpsBasedOnGoodPackets);
        }

        rtpStats.setGoodFrames(goodFrameCount);
        if (preGoodFrameCount != goodFrameCount)
            rtpStats.setLastReceivedFrameSize(jpeg_last_fragment_offset);
        rtpStats.setFps(fps);
        rtpStats.setReceivedUdpPackets(receiveUdpPacketsCount);
        rtpStats.setSkippedPackets(rtpSkippedPacket);
        rtpStats.setJpegBadStart(jpegBadStart);
        rtpStats.setJpegBadEnd(jpegBadEndCount);
        rtpStats.setJpegBadOffset(jpegCorruptCounts);
        valueUpdateLiveViewStats.postValue(rtpStats);

        preGoodFrameCount = goodFrameCount;
    }


    public void captureImage(String riscv) {
        RISCV_VERSION = removeNonReadableChars(riscv);
        isCaptureImage = true;
    }

    private static String removeNonReadableChars(String text) {

        try {
            // Remove "???" characters
            text = text.replaceAll("\\?{3}", "");

            // Remove non-printable ASCII characters
            text = text.replaceAll("[^\\x20-\\x7E]", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return text;
    }

    public void saveImageToMediaStore(Bitmap bitmap) {
        String outputFile = createImageFilePath();
        Log.e(TAG, "saveImageToMediaStore: " + outputFile);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        File file = new File(outputFile);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(byteArray);
            outputStream.flush();
            outputStream.close();
            isCaptureImage = false;
        } catch (IOException e) {
            e.printStackTrace();
            isCaptureImage = false;
        }

        try {
            String dateTimeOriginal = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String latitudeRef;
            if (locationLatitude >= 0) {
                latitudeRef = "N"; // North
            } else {
                latitudeRef = "S"; // South
            }

            String longitudeRef;
            if (locationLongitude >= 0) {
                longitudeRef = "E"; // East
            } else {
                longitudeRef = "W"; // West
            }
            CameraDetails details = new CameraDetails();
            String model = details.getModelNumber();
            NWD_MODEL = MAKE + " " + model;
            NWD_FW_VERSION = details.getFirmwareVersion();
            Log.e(TAG, "saveImageToMediaStore: " + RISCV_VERSION);
            ExifInterface exif = new ExifInterface(file);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToRational(locationLatitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToRational(locationLongitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeRef);
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeRef);
            exif.setAttribute(ExifInterface.TAG_MAKE, MAKE);
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTimeOriginal);
            exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, X_RESOLUTION);
            exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, Y_RESOLUTION);
            exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, X_RESOLUTION);
            exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, Y_RESOLUTION);
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    exif.setAttribute(ExifInterface.TAG_MODEL, NIGHTWAVE_MODEL);
                    exif.setAttribute(ExifInterface.TAG_F_NUMBER, NIGHTWAVE_APERTURE);//APERTURE
                    exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, convertFloatIntoRational(NIGHTWAVE_FOCAL_LENGTH));//Focal Length
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, RISCV_VERSION);//Not visible in Mobile app but it is visible in PC
                    break;
                case OPSIN:
                    exif.setAttribute(ExifInterface.TAG_MODEL, OPSIN_MODEL);
                    exif.setAttribute(ExifInterface.TAG_F_NUMBER, OPSIN_APERTURE);//APERTURE
                    exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, convertFloatIntoRational(OPSIN_FOCAL_LENGTH));//Focal Length
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, RISCV_VERSION);//Not visible in Mobile app but it is visible in PC
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG, "saveImageToMediaStore : NW_Digital");
                    exif.setAttribute(ExifInterface.TAG_MODEL, NWD_MODEL);
                    exif.setAttribute(ExifInterface.TAG_F_NUMBER, NWD_APERTURE);//APERTURE
                    exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, convertFloatIntoRational(NWD_FOCAL_LENGTH));//Focal Length
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, NWD_FW_VERSION);
                    break;
            }
//            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Comments If any");
//            exif.setAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS, "490");//ISO
//            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, "0.02");//Shutter Speed equalent to 1/50
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @NonNull
    private String createImageFilePath() {
        File folderForVideo = createAppFolderForImage(application, "SiOnyx");
        File subFolder = getFile(folderForVideo);

        String fileName = Constants.getConnectedCameraSSID() + "_" + System.currentTimeMillis() + ".jpg";
        String outputFile = subFolder.getAbsolutePath() + "/" + fileName;
        imageFilePath = outputFile;
        return outputFile;
    }

    private String convertDecimalToDMS(double value) {
        // Split the double value into degrees, minutes, and seconds
        double degrees = Math.floor(value);
        double minutes = Math.floor((value - degrees) * 60);
        double seconds = ((value - degrees) * 60 - minutes) * 60;

        // Convert the degrees, minutes, and seconds to integer values
        int degreesInt = (int) degrees;
        int minutesInt = (int) minutes;
        int secondsInt = (int) Math.round(seconds);

        // Construct the rational format string

        return degreesInt + "/1," + minutesInt + "/1," + secondsInt + "/1";
    }

    // Helper function to convert double to rational format
    private String convertToRational(double value) {
        String[] parts = Double.toString(Math.abs(value)).split("\\.");
        int degrees = Integer.parseInt(parts[0]);
        double minutesSeconds = Double.parseDouble("0." + parts[1]) * 60.0;
        int minutes = (int) minutesSeconds;
        double seconds = (minutesSeconds - minutes) * 60.0;

        return String.format(application.getString(R.string.location_dms_format), degrees, minutes, (int) seconds);
    }

    public static String convertFloatIntoRational(float floatValue) {
        BigDecimal decimal = new BigDecimal(Float.toString(floatValue));
        BigDecimal denominator = BigDecimal.ONE;
        int scale = decimal.scale();

        for (int i = 0; i < scale; i++) {
            decimal = decimal.multiply(BigDecimal.TEN);
            denominator = denominator.multiply(BigDecimal.TEN);
        }

        // Find the greatest common divisor (gcd) between the numerator and denominator
        BigInteger gcd = decimal.toBigInteger().gcd(denominator.toBigInteger());

        // Divide both the numerator and denominator by the gcd to simplify the fraction
        decimal = decimal.divide(new BigDecimal(gcd), RoundingMode.CEILING);
        denominator = denominator.divide(new BigDecimal(gcd), RoundingMode.CEILING);

        return decimal.toPlainString() + "/" + denominator.toPlainString();
    }


    public void startVideoRecording() {
        currentTimeStampForRecording = -1;
        previousTimeStampForRecording = -1;

        FRAME_RATE = 30;
        String outputFile = createVideoFilePath();
        Log.e(TAG, "startVideoRecording outputFilePath: " + outputFile);
        mp4encoderNightwave.setOutputFilePath(outputFile);
        mp4encoderNightwave.setOutputSize(Integer.parseInt(X_RESOLUTION), Integer.parseInt(Y_RESOLUTION));
        mp4encoderNightwave.startEncode();
        isRecordingStarted = true;

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            currentTimeStampForRecording = System.currentTimeMillis();
            double difference = currentTimeStampForRecording - previousTimeStampForRecording;
            Log.e(TAG, "difference Called: " + difference);
            if (previousTimeStampForRecording == -1 || difference < 2000) {

            } else {
                Log.e(TAG, "stopVideoRecording Called: 1");
                stopVideoRecording(true);
                //Handle UI Here by passing stopped state and perform stop rec button ui state
            }

        }, 1, 1, TimeUnit.SECONDS);
    }

    public void doVideoRecording(byte[] data) {
        if (isRecordingStarted) {
            previousTimeStampForRecording = System.currentTimeMillis();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            mp4encoderNightwave.addFrame(bitmap);
        }
    }


    public void stopVideoRecording(boolean shouldTriggerMessage) {
        if (mp4encoderNightwave != null) {
            if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
                if (shouldTriggerMessage)
                    hasRecordStoppedDuetoNoLivePackets.postValue(new Event<>(true));
                scheduledExecutorService.shutdownNow();
            }

            isRecordingStarted = false;
            mp4encoderNightwave.stopEncode();
            mp4encoderNightwave.setEncodeFinishListener(new Encoder.EncodeFinishListener() {
                @Override
                public void onEncodeFinished() {
                    Log.e(TAG, "onEncodeFinished: ");
                }

                @Override
                public void onEncodingError(String err) {
                    Log.e(TAG, "onEncodingError: " + err);
                }
            });
        }
    }


    public short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    public int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public void onCleared() {
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
    }

    /* onClear all live data for NWD */
    public void clearAllLiveData() {
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
            notifyDigitalStreamingStatus.postValue(null);
            notifyDigitalNoFpsFound.postValue(null);
            notifyOnStreamingError.postValue(null);
            notifyIsStreamingInBuffer.postValue(null);
            notifyDigitalMediaVideoSize.postValue(null);
            notifyRetryValue.postValue(null);
            observeLiveFpsValue.postValue(null);
            notifyStreamingAttachedToSurfaceVew.postValue(null);
        }
    }

    public void resetSocketState() {
        if (bound) {
            mService.resetSocketState();
        } else {
            Log.e(TAG, "resetSocketState: false");
        }
    }

    private void retrySendCommand() {
        Log.e(TAG, "retrySendCommand: " + retryCount);
        if (retryCount < 3) {
            retryCount = retryCount + 1;
            startLiveView(false);
        } else {
            retryCount = 0;
            hasSendLiveViewCommand.postValue(new Event<>(false));
            Log.e(TAG, "retrySendCommand:  failed");
        }
    }

    public LiveData<Integer> observeLiveFps() {
        return observeLiveFpsValue;
    }

    public LiveData<Event<Integer>> observeOnStreamingErrorCode() {
        return notifyOnStreamingError;
    }

    public LiveData<Boolean> observeIsStreamingInBuffer() {
        return notifyIsStreamingInBuffer;
    }

    public LiveData<Boolean> observerNoFpsFoundForDigital() {
        return notifyDigitalNoFpsFound;
    }

    public LiveData<Long> observeDigitalMediaVideoSize() {
        return notifyDigitalMediaVideoSize;
    }

    public LiveData<Event<Object>> observeRetryCountData() {
        return notifyRetryValue;
    }

    @Override
    public void onOutputFormatChanged(int width, int height) {
        long packed = (((long) height) << 32) | (width & 0xFFFFFFFFL);
        notifyDigitalMediaVideoSize.postValue(packed);
    }

    @Override
    public void setSpsPpsWithStartCode() {
        //Decoder throws illegalstate exception, so we need to try with start code
        shouldUseStartCode = true;
        if (isStreamingStarted && nwdTextureView != null && videoSurface != null && videoSurface.isValid()) {
            Log.e(TAG, "setSpsPpsWithStartCode: called " + h264Decoder.isInitialized());
            mHandler.postDelayed(this::setSurfaceTextureView, 100);
        }
    }


    @Override
    public void enableStreamingView(boolean isVisible) {
        if (isVisible && isStreamingStarted) {
            Log.e(TAG, "enableStreamingView " + isVisible);
            notifyIsStreamingInBuffer.postValue(false);
            notifyStreamingAttachedToSurfaceVew.postValue(new Event<>(true));
        }
    }

    /*------------------------------------------------------------------------------------------------OPSIN-------------------------------------------------------------------------------*/
    /* OPSIN PRODUCT SPECIFIC COMMANDS, RESPONSES */
    public enum PERIODIC_COMMAND {
        KEEP_ALIVE,
        BATTERY_INFO,
        GET_NUC,
        GET_EV,
        GET_FRAME_RATE,
        GET_MIC_STATE,
        GET_COMPASS,
        GET_SD_CARD_INFO,
        GET_MONOCHROMATIC,
        GET_NOISE_REDUCTION_RATE,
        GET_ROI,
        GET_GPS_POSITION;
    }

    public void clearPeriodicRequestList() {
        mService.getSynchronizedPeriodicList().clear();
    }

    public void triggerKeepAlive(boolean shouldStartStreaming) {
        mService.triggerKeepAlive(shouldStartStreaming);
    }

    public void addOpsinPeriodicTimerCommand(PERIODIC_COMMAND command) {
        switch (command) {
            case KEEP_ALIVE:
                if (bound) {
                    short sccp_command = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_SCCP_MANAGEMENT.SCCP_CMD_KEEPALIVE.getValue();
                    if (!mService.getSynchronizedPeriodicList().contains(sccp_command)) {
                        if (Constants.isOpsinContainsOldWiFiFirmware()) {
                            Log.e("KEEP-ALIVE", "OPSIN Old firmware");
                            if (mState == STATE_CONNECTED && fwMode == MODE_NONE) {
                                boolean isGetCommand = false;
                                byte[] data = new byte[]{opsinVersionDetails.getMajor(), opsinVersionDetails.getMinor(), opsinVersionDetails.getPatch(), 0x00, mService.getOpsinRxSequence(), mService.getOpsinTxSequence()};
                                boolean isResponseRequired = true;
                                boolean isKeepAlive = true;

                                OpsinPeriodicCommand opsinPeriodicCommand = new OpsinPeriodicCommand();
                                opsinPeriodicCommand.setIsGetCommand(isGetCommand);
                                opsinPeriodicCommand.setCommand(sccp_command);
                                opsinPeriodicCommand.setData(data);
                                opsinPeriodicCommand.setResponseRequired(isResponseRequired);
                                opsinPeriodicCommand.setKeepAlive(isKeepAlive);
                                mService.getSynchronizedPeriodicList().add(opsinPeriodicCommand);
                            }
                        } else {
                            if (mState == STATE_CONNECTED && fwMode == MODE_NONE) {
                                boolean isGetCommand = false;
                                byte[] data = new byte[]{mService.getOpsinRxSequence(), mService.getOpsinTxSequence()};
                                boolean isResponseRequired = true;
                                boolean isKeepAlive = true;

                                OpsinPeriodicCommand opsinPeriodicCommand = new OpsinPeriodicCommand();
                                opsinPeriodicCommand.setIsGetCommand(isGetCommand);
                                opsinPeriodicCommand.setCommand(sccp_command);
                                opsinPeriodicCommand.setData(data);
                                opsinPeriodicCommand.setResponseRequired(isResponseRequired);
                                opsinPeriodicCommand.setKeepAlive(isKeepAlive);
                                mService.getSynchronizedPeriodicList().add(opsinPeriodicCommand);
                            }
                        }
                    }
                }

                break;
            case BATTERY_INFO:
                short sccp_command = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_POWER_MANAGEMENT.SCCP_CMD_POWER_GET_BATTERY.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccp_command)) {
                    boolean isGetCommand = false;
                    byte[] data = new byte[]{0x00};
                    boolean isResponseRequired = true;
                    boolean isKeepAlive = false;

                    OpsinPeriodicCommand opsinPeriodicCommand = new OpsinPeriodicCommand();
                    opsinPeriodicCommand.setIsGetCommand(isGetCommand);
                    opsinPeriodicCommand.setCommand(sccp_command);
                    opsinPeriodicCommand.setData(data);
                    opsinPeriodicCommand.setResponseRequired(isResponseRequired);
                    opsinPeriodicCommand.setKeepAlive(isKeepAlive);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommand);
                }
                break;
            case GET_NUC:
                short sccpNuc = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_NUC_STATE.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpNuc)) {
                    OpsinPeriodicCommand opsinPeriodicCommandNuc = new OpsinPeriodicCommand();
                    opsinPeriodicCommandNuc.setIsGetCommand(true);
                    opsinPeriodicCommandNuc.setCommand(sccpNuc);
                    opsinPeriodicCommandNuc.setData(null);
                    opsinPeriodicCommandNuc.setResponseRequired(true);
                    opsinPeriodicCommandNuc.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandNuc);
                }

                break;
            case GET_EV:
                short sccpEv = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_EV.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpEv)) {
                    OpsinPeriodicCommand opsinPeriodicCommandEv = new OpsinPeriodicCommand();
                    opsinPeriodicCommandEv.setIsGetCommand(true);
                    opsinPeriodicCommandEv.setCommand(sccpEv);
                    opsinPeriodicCommandEv.setData(null);
                    opsinPeriodicCommandEv.setResponseRequired(true);
                    opsinPeriodicCommandEv.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandEv);
                }

                break;
            case GET_FRAME_RATE:
                short sccpFps = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_FRAMERATE.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpFps)) {
                    OpsinPeriodicCommand opsinPeriodicCommandFps = new OpsinPeriodicCommand();
                    opsinPeriodicCommandFps.setIsGetCommand(true);
                    opsinPeriodicCommandFps.setCommand(sccpFps);
                    opsinPeriodicCommandFps.setData(null);
                    opsinPeriodicCommandFps.setResponseRequired(true);
                    opsinPeriodicCommandFps.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandFps);
                }

                break;
            case GET_MIC_STATE:
                short micState = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_MIC_STATE.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(micState)) {
                    OpsinPeriodicCommand opsinPeriodicCommandMic = new OpsinPeriodicCommand();
                    opsinPeriodicCommandMic.setIsGetCommand(true);
                    opsinPeriodicCommandMic.setCommand(micState);
                    opsinPeriodicCommandMic.setData(null);
                    opsinPeriodicCommandMic.setResponseRequired(true);
                    opsinPeriodicCommandMic.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandMic);
                }

                break;
            case GET_COMPASS:
                short sccpCompass = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_COMPASS_SPECIFIC.SCCP_CMD_COMPASS_GET_ANGLES.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpCompass)) {
                    OpsinPeriodicCommand opsinPeriodicCommandCompass = new OpsinPeriodicCommand();
                    opsinPeriodicCommandCompass.setIsGetCommand(true);
                    opsinPeriodicCommandCompass.setCommand(sccpCompass);
                    opsinPeriodicCommandCompass.setData(null);
                    opsinPeriodicCommandCompass.setResponseRequired(true);
                    opsinPeriodicCommandCompass.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandCompass);
                }

                break;
            case GET_SD_CARD_INFO:
                short sccpSdcard = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_SDCARD_SPECIFIC.SCCP_CMD_SDCARD_GET_INFO.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpSdcard)) {
                    OpsinPeriodicCommand opsinPeriodicCommandSdCard = new OpsinPeriodicCommand();
                    opsinPeriodicCommandSdCard.setIsGetCommand(true);
                    opsinPeriodicCommandSdCard.setCommand(sccpSdcard);
                    opsinPeriodicCommandSdCard.setData(null);
                    opsinPeriodicCommandSdCard.setResponseRequired(true);
                    opsinPeriodicCommandSdCard.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandSdCard);
                }

                break;
            case GET_MONOCHROMATIC:
                short sccpMono = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_MONOCHROMATIC.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpMono)) {
                    OpsinPeriodicCommand opsinPeriodicCommandMono = new OpsinPeriodicCommand();
                    opsinPeriodicCommandMono.setIsGetCommand(true);
                    opsinPeriodicCommandMono.setCommand(sccpMono);
                    opsinPeriodicCommandMono.setData(null);
                    opsinPeriodicCommandMono.setResponseRequired(true);
                    opsinPeriodicCommandMono.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandMono);
                }
                break;
            case GET_NOISE_REDUCTION_RATE:
                short sccpNoise = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_NR.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpNoise)) {
                    OpsinPeriodicCommand opsinPeriodicCommandNoise = new OpsinPeriodicCommand();
                    opsinPeriodicCommandNoise.setIsGetCommand(true);
                    opsinPeriodicCommandNoise.setCommand(sccpNoise);
                    opsinPeriodicCommandNoise.setData(null);
                    opsinPeriodicCommandNoise.setResponseRequired(true);
                    opsinPeriodicCommandNoise.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandNoise);
                }

                break;
            case GET_ROI:
                short sccpRoi = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_ROI.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpRoi)) {
                    OpsinPeriodicCommand opsinPeriodicCommandRoi = new OpsinPeriodicCommand();
                    opsinPeriodicCommandRoi.setIsGetCommand(true);
                    opsinPeriodicCommandRoi.setCommand(sccpRoi);
                    opsinPeriodicCommandRoi.setData(null);
                    opsinPeriodicCommandRoi.setResponseRequired(true);
                    opsinPeriodicCommandRoi.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandRoi);
                }

                break;
            case GET_GPS_POSITION:
                short sccpGpsPosition = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_GET_POSITION.getValue();
                if (!mService.getSynchronizedPeriodicList().contains(sccpGpsPosition)) {
                    OpsinPeriodicCommand opsinPeriodicCommandGps = new OpsinPeriodicCommand();
                    opsinPeriodicCommandGps.setIsGetCommand(true);
                    opsinPeriodicCommandGps.setCommand(sccpGpsPosition);
                    opsinPeriodicCommandGps.setData(null);
                    opsinPeriodicCommandGps.setResponseRequired(true);
                    opsinPeriodicCommandGps.setKeepAlive(false);
                    mService.getSynchronizedPeriodicList().add(opsinPeriodicCommandGps);
                }
                break;
        }
    }

    public void cancelOpsinPeriodicTimer() {
        if (bound) {
            mService.cancelOpsinPeriodicTimer();
            stopOpsinLiveStreaming();
        }
    }


    public enum COMMAND_REQUESTED {
        NONE,
        GET_VERSION,
        GET_PRODUCT,
        GET_SERIAL,
        GET_TIMEZONE,
        GET_DATETIME,
        GET_DIGITAL_ZOOM,
        GET_NUC_STATE,
        GET_MIC_STATE,
        GET_FRAMERATE,
        GET_JPEG_COMPRESSION,
        GET_NAME,
        GET_CLOCKMODE,
        SET_TIMEZONE,
        SET_DATETIME,
        SET_DIGITAL_ZOOM,
        SET_NUC_STATE,
        SET_MIC_STATE,
        SET_FRAMERATE,
        SET_JPEG_COMPRESSION,
        FACTORY_RESET,
        CAMERA_RESTART,
        SET_NAME,
        START_RECORDING,
        STOP_RECORDING,
        TAKE_PICTURE,
        START_STREAMING,
        STOP_STREAMING,
        GET_RECORDING_STATE,
        GET_STREAMING_STATE,
        GET_PASSWORD,
        GET_MAC,
        GET_EV,
        SET_EV,
        GET_BRIGHTNESS,
        GET_NR,
        SET_NR,
        GET_MONOCHROMATIC,
        SET_MONOCHROMATIC,
        GET_ROI,
        SET_ROI,
        SET_CLOCKMODE,
        GET_METADATA,
        SET_METADATA,
        SDCARD_GET_INFO,
        GPS_GET_INFO,
        GPS_GET_POSITION,
        GPS_SET_POWER,
        POWER_GET_BATTERY,
        GET_IMAGE_STATE,
        COMPASS_GET_ANGLES,
        GET_TIME_FORMAT,
        SET_TIME_FORMAT
    }

    public static COMMAND_REQUESTED commandRequested = COMMAND_REQUESTED.NONE;
    private final Handler waitHandler = new Handler(Looper.getMainLooper());
    private final Handler sendBlockHandler = new Handler(Looper.getMainLooper());
    private final int DELAY = 300;

    public void getProductVersion() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                Log.e(TAG, "getProductVersion: Called " + commandRequested.name());
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_VERSION;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_VERSION.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getProductVersion, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getProductVersion, DELAY);
                }
            }
        }
    }

    public void getProductInfo() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_PRODUCT;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_PRODUCT.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getProductInfo, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getProductInfo, DELAY);
                }
            }
        }
    }

    public void getSerialNumber() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_SERIAL;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_SERIAL.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getSerialNumber, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getSerialNumber, DELAY);
                }
            }
        }
    }

    public void getTimeZone() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_TIMEZONE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_TIMEZONE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getTimeZone, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getTimeZone, DELAY);
                }
            }
        }
    }

    public void getDateTime() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_DATETIME;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_DATETIME.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getDateTime, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getDateTime, DELAY);
                }
            }
        }
    }

    public void getDigitalZoom() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_DIGITAL_ZOOM;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getDigitalZoom, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getDigitalZoom, DELAY);
                }
            }
        }
    }

    public void getNUC() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_NUC_STATE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_NUC_STATE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getNUC, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getNUC, DELAY);
                }
            }
        }
    }

    public void getMicState() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_MIC_STATE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_MIC_STATE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getMicState, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getMicState, DELAY);
                }
            }
        }
    }

    public void getFrameRate() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_FRAMERATE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_FRAMERATE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getFrameRate, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getFrameRate, DELAY);
                }
            }
        }
    }

    public void getJpegCompression() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_JPEG_COMPRESSION;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getJpegCompression, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getJpegCompression, DELAY);
                }
            }
        }
    }

    public void getOpsinCameraName() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_NAME;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_NAME.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinCameraName, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinCameraName, DELAY);
                }
            }
        }
    }

    public void getOpsinClockMode() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_CLOCKMODE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_CLOCKMODE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinClockMode, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinClockMode, DELAY);
                }
            }
        }
    }

    public void setTimeZone(String timeZone) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                String[] split = timeZone.split(":");
                int hour = Integer.parseInt(split[0]);
                int minute = Integer.parseInt(split[1]);
                hour = Math.abs(hour);

                byte[] data = new byte[2];
                data[0] = (byte) minute;//first
                data[1] = (byte) hour;//last

                if (data[0] == 0 && data[1] == 0) {
                    Log.e(TAG, "setTimeZone: UTC True");
                } else {
                    boolean isNagative = timeZone.contains("-");
                    int sign;
                    if (isNagative) {
                        sign = -1;
                    } else {
                        sign = 0;
                    }
                    // Combine sign, hour, and minutes into the short value
                    short value = (short) ((sign & 0x01) << 15 | (hour & 0x7F) << 8 | (minute & 0xFF));
                    // Convert the short value to little-endian byte array
                    data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
                }
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_TIMEZONE;
                    short sccp_timezone = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_TIMEZONE.getValue();
                    mService.sendOpsinSetCommand(sccp_timezone, data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setTimeZone(timeZone), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setTimeZone(timeZone), DELAY);
                }
            }
        }
    }

    public void setDateTime(DateTime dateTime) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                byte[] dateTimeBytes = intToBytess(dateTime.getDateTime());
                byte[] data = {dateTimeBytes[0], dateTimeBytes[1], dateTimeBytes[2], dateTimeBytes[3]};
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_DATETIME;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_DATETIME.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setDateTime(dateTime), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setDateTime(dateTime), DELAY);
                }
            }
        }
    }

    public void setDigitalZoom(byte level) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = new byte[]{level};
                    commandRequested = COMMAND_REQUESTED.SET_DIGITAL_ZOOM;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setDigitalZoom(level), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setDigitalZoom(level), DELAY);
                }
            }
        }
    }

    public void setNUC(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = new byte[]{state, 0x00, 0x00, 0x00};
                    commandRequested = COMMAND_REQUESTED.SET_NUC_STATE;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_NUC_STATE.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setNUC(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setNUC(state), DELAY);
                }
            }
        }
    }

    public void setMicState(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = new byte[]{state};
                    commandRequested = COMMAND_REQUESTED.SET_MIC_STATE;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_MIC_STATE.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setMicState(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setMicState(state), DELAY);
                }
            }
        }
    }

    public void setSetFrameRate(short frameRate) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = shortToByteArray(frameRate);
                    commandRequested = COMMAND_REQUESTED.SET_FRAMERATE;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_FRAMERATE.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setSetFrameRate(frameRate), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setSetFrameRate(frameRate), DELAY);
                }
            }
        }
    }

    public void setJpegCompression(byte value) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = new byte[]{value};
                    commandRequested = COMMAND_REQUESTED.SET_JPEG_COMPRESSION;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setJpegCompression(value), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setJpegCompression(value), DELAY);
                }
            }
        }
    }


    public void setFactoryReset(byte value) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = new byte[]{value};
                    commandRequested = COMMAND_REQUESTED.FACTORY_RESET;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_FACTORY_RESET.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setFactoryReset(value), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setFactoryReset(value), DELAY);
                }
            }
        }
    }

    public void restartOpsinCamera() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.CAMERA_RESTART;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_RESTART.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::restartOpsinCamera, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::restartOpsinCamera, DELAY);
                }
            }
        }
    }

    public void setOpsinCameraName(String name) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    name = name + "\0";
                    byte[] data = name.getBytes();
                    byte[] bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).array();
                    byte[] nameBytes = new byte[bb.length];

                    System.arraycopy(bb, 0, nameBytes, 0, nameBytes.length);
                    commandRequested = COMMAND_REQUESTED.SET_NAME;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_NAME.getValue(), nameBytes, true, false);
                } else {
                    String finalName = name;
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinCameraName(finalName), DELAY);
                    }
                }
            } else {
                String finalName = name;
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinCameraName(finalName), DELAY);
                }
            }
        }
    }

    public void startOpsinCameraRecord() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.START_RECORDING;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_START_RECORDING.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::startOpsinCameraRecord, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::startOpsinCameraRecord, DELAY);
                }
            }
        }
    }

    public void stopOpsinCameraRecord() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.STOP_RECORDING;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_STOP_RECORDING.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::stopOpsinCameraRecord, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::stopOpsinCameraRecord, DELAY);
                }
            }
        }
    }

    public void opsinCameraTakeImageToSDCard() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    byte[] data = new byte[]{0x00, 0x01, 0x00, 0x00};
                    commandRequested = COMMAND_REQUESTED.TAKE_PICTURE;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_TAKE_PICTURE.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::opsinCameraTakeImageToSDCard, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::opsinCameraTakeImageToSDCard, DELAY);
                }
            }
        }
    }

    public void setTextureView(ZoomableTextureView textureView) {
        Log.e(TAG, "setSurfaceTextureListener: setTextureView");
        opsinTextureView = textureView;
        initializeObjects();
        opsinTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                Surface surface = new Surface(surfaceTexture);
                mediaDecoder.setSurface(surface);
                Log.e(TAG, "setSurfaceTextureListener: onSurfaceTextureAvailable: ");
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                // Handle surface texture size change if needed
                Log.e(TAG, "setSurfaceTextureListener: onSurfaceTextureSizeChanged: ");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                // Handle surface texture destruction if needed
                Log.e(TAG, "setSurfaceTextureListener: onSurfaceTextureDestroyed: ");
                surfaceTexture.release();
                stopAndResetUDP();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // Handle surface texture updates if needed
                if (isRecordingStarted) {
                    Bitmap bitmap = opsinTextureView.getBitmap(OPSIN_X_RESOLUTION, OPSIN_Y_RESOLUTION);
                    mp4encoderNightwave.addFrame(bitmap);
                }
            }
        });
    }

    public void setVideoContainer(ZoomablePlayerView textureView) {
        Log.d(TAG, "setVideoContainer called ");
        nwdTextureView = textureView;
        if (videoRecorder == null) {
            videoRecorder = new VideoRecorder(VIDEO_WIDTH, VIDEO_HEIGHT, BIT_RATE); // uncomment for video record
        }
        setSurfaceTextureView();
    }
    private void setSurfaceTextureView() {
        clearSurfaceAndDecoderState();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Log.e(TAG, "setSurfaceTextureView: interrupted sleep");
        }
        nwdTextureView.setSurfaceTextureListener(surfaceTextureListener);
        if (nwdTextureView.isAvailable()) {
            SurfaceTexture surfaceTexture = nwdTextureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                Log.d(TAG, "Surface already available, handling manually");
                videoSurface = new Surface(surfaceTexture);
                mHandler.postDelayed(this::initDecoder,200);
            }
        }
    }

    private void clearSurfaceAndDecoderState() {
        Log.d(TAG, "Clearing Surface & Decoder state");

//        releaseH264Decoder();
        stopAndReleaseDecoderSafely(h264Decoder);


        if (videoSurface != null) {
            videoSurface = null;
        }

        if (nwdTextureView != null) {
            nwdTextureView.setSurfaceTextureListener(null);
        }
        Log.d(TAG, "Clearing Surface & Decoder state is done");
    }

    public void stopAndResetUDP() {
        mService.stopListeningOpsinUdp();
        shouldReceiveUDP = false;
        mediaDecoder.stopDecoder();
        stopOpsinLiveStreaming();
    }

    public static boolean isOpsinLiveStreamingStarted = false;

    public void resetOpsinLiveStreamingState() {
        isOpsinLiveStreamingStarted = false;
    }

    public void startOpsinLiveStreaming() {
        if (bound && !isOpsinLiveStreamingStarted) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.START_STREAMING;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_START_STREAMING.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::startOpsinLiveStreaming, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::startOpsinLiveStreaming, DELAY);
                }
            }
        }
    }

    public void captureOpsinImageLocally(String riscv) {
        RISCV_VERSION = removeNonReadableChars(riscv);
        Bitmap bitmap = opsinTextureView.getBitmap(OPSIN_X_RESOLUTION, OPSIN_Y_RESOLUTION);
        saveImageToMediaStore(bitmap);
    }

    // uncomment for video record and Image capture
    /*
    Image capture for NWD
     */
    public void captureDigitalImageLocally() {
        // texture view
        Bitmap bitmap = nwdTextureView.getBitmap();
        if (bitmap != null) {
            saveImageToMediaStore(bitmap);
        }
        Log.e(TAG, "ImageCapture called");
    }

    /*
    Video capture start snd stop for NWD
     */
    public void startDigitalVideoRecordLocally() {
        startDigitalRecord();
    }

    public void stopDigitalVideoRecordLocally() {
        stopDigitalRecord();
    }

    private void startDigitalRecord() {

        String outputFile = createVideoFilePath();
        Log.e(TAG, "startVideoRecording outputFilePath: " + outputFile);

        try {
            if (videoRecorder != null) {
                videoRecorder.setSpsPpsData(DIGITAL_SPS_BYTE_VALUE, DIGITAL_PPS_BYTE_VALUE);
                videoRecorder.startRecording(new File(outputFile)); // check history
            }
        } catch (IOException e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
        }
    }

    private void stopDigitalRecord() {
        hasWrittenFirstKeyFrame = false;
        videoRecorder.stopRecording();

    }

    // when started to switching
    public void onStreamSwitchStatus(boolean b_isSwitching) {
        if (b_isSwitching) {
            notifyIsStreamingInBuffer.postValue(true);
            isStreamingSwitched = true;
            Log.d(TAG, "Stream switch started");
            // Show progress indicator or overlay
        } else if(isStreamingSwitched){
            isStreamingSwitched = false;
            notifyIsStreamingInBuffer.postValue(false);
            Log.d(TAG, "Stream switch ended");
        }

    }

    public void onSpsPpsDataReceived(byte[] sps, byte[] pps) {
        Log.d(TAG, "SPS received, : " + Arrays.toString(sps));
        Log.d(TAG, "PPS received, : " + Arrays.toString(pps));
        if (sps != null)
            DIGITAL_SPS_BYTE_VALUE = sps;
        if (pps != null)
            DIGITAL_PPS_BYTE_VALUE = pps;

    }

    private void initDecoder() {
        Log.e(TAG, "initDecoder called : ");

        if (videoSurface != null && !videoSurface.isValid())
            return;

        try {
            boolean isHighRes = false;
            if (!isLowres) {
                VIDEO_WIDTH = 1280;
                VIDEO_HEIGHT = 1024;
                BIT_RATE = 4 * 1_000_000;
                isHighRes = true;
            } else {
                VIDEO_WIDTH = 640;
                VIDEO_HEIGHT = 512;
                BIT_RATE = 2 * 1_000_000;
            }

            // Use fallback SPS/PPS if not available
            if (DIGITAL_SPS_BYTE_VALUE == null || DIGITAL_PPS_BYTE_VALUE == null || DIGITAL_SPS_BYTE_VALUE.length == 0 || DIGITAL_PPS_BYTE_VALUE.length == 0) {
                Log.w(TAG, "Using fallback SPS/PPS");
                DIGITAL_SPS_BYTE_VALUE = new byte[]{0x00, 0x00, 0x01, 0x67, 0x4d, (byte) 0xc0, 0x28, (byte) 0x8d,
                        (byte) 0x8d, 0x50, 0x28, 0x01, 0x02, 0x42, 0x00, 0x00,
                        0x03, 0x00, 0x02, 0x00, 0x00, 0x03, 0x00, 0x79,
                        0x1e, 0x11, 0x08, (byte) 0xd4};
                DIGITAL_PPS_BYTE_VALUE = new byte[]{0x00, 0x00, 0x01, 0x68, (byte) 0xee, 0x3c, (byte) 0x80};
            }

            byte[] bytesSps = stripStartCode(DIGITAL_SPS_BYTE_VALUE);
            byte[] bytesPps = stripStartCode(DIGITAL_PPS_BYTE_VALUE);
            try {
                if (h264Decoder == null) {
                    h264Decoder = new H264DecoderDeviceSpecific();
                    h264Decoder.setSizeChangedCallback(this);
                }

                if (!h264Decoder.isInitialized())
                    h264Decoder.initialize(videoSurface, VIDEO_WIDTH, VIDEO_HEIGHT, isHighRes);

                if (shouldUseStartCode) {
                    h264Decoder.setSpsPpsData(DIGITAL_SPS_BYTE_VALUE, DIGITAL_PPS_BYTE_VALUE);
                    h264Decoder.enqueueFrame(ByteBuffer.wrap(DIGITAL_SPS_BYTE_VALUE), DIGITAL_SPS_BYTE_VALUE.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    h264Decoder.enqueueFrame(ByteBuffer.wrap(DIGITAL_PPS_BYTE_VALUE), DIGITAL_PPS_BYTE_VALUE.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                } else {
                    h264Decoder.setSpsPpsData(bytesSps, bytesPps);
                    h264Decoder.enqueueFrame(ByteBuffer.wrap(bytesSps), bytesSps.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    h264Decoder.enqueueFrame(ByteBuffer.wrap(bytesPps), bytesPps.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                }
            } catch (MediaCodec.CodecException e) {
                Log.e(TAG, "Decoder codec failure on initialize", e);
                if (e.isTransient()) {
                    Log.e(TAG, "Transient error – retry later - on initialize");
                } else if (e.isRecoverable()) {
                    Log.e(TAG, "Recoverable error – reinit decoder - on initialize");
                    setSurfaceTextureView();
                } else {
                    Log.e(TAG, "Fatal codec error – disabling decoder - on initialize");
                    setSurfaceTextureView();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Decoder misuse or bad surface - on initialize", e);
                setSurfaceTextureView();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected initDecoder error - on initialize", e);
                setSurfaceTextureView();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected initDecoder error", e);
        }
    }

    private byte[] stripStartCode(byte[] nal) {
        int offset = 0;

        // Skip 3-byte or 4-byte start code
        if (nal.length >= 4 && nal[0] == 0x00 && nal[1] == 0x00) {
            if (nal[2] == 0x01) {
                offset = 3;
            } else if (nal[2] == 0x00 && nal[3] == 0x01) {
                offset = 4;
            }
        }

        return Arrays.copyOfRange(nal, offset, nal.length);
    }


    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable");
            videoSurface = new Surface(surface);
            mHandler.post(() -> initDecoder());
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed");
//            releaseH264Decoder();
            stopAndReleaseDecoderSafely(h264Decoder);
            videoSurface = null;
            return true; // release SurfaceTexture
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };



    public void startOpsinVideoRecordLocally() {
//        String outputFile = createVideoFilePath();
//        mediaEncoder.startRecording(outputFile);

        startOpsinVideoRecordLocallyByBitmap();
    }

    public void stopOpsinVideoRecordLocally() {
//        mediaEncoder.stopRecording();
        stopVideoRecordingOpsin(false);
    }

    private void startOpsinVideoRecordLocallyByBitmap() {
        currentTimeStampForRecording = -1;
        previousTimeStampForRecording = -1;

        String outputFile = createVideoFilePath();
        Log.e(TAG, "startVideoRecording outputFilePath: " + outputFile);
        mp4encoderNightwave.setOutputFilePath(outputFile);
        mp4encoderNightwave.setOutputSize(OPSIN_X_RESOLUTION, OPSIN_Y_RESOLUTION);
        mp4encoderNightwave.startEncode();
        isRecordingStarted = true;

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            currentTimeStampForRecording = System.currentTimeMillis();
            double difference = currentTimeStampForRecording - previousTimeStampForRecording;
            Log.e(TAG, "difference Called: " + difference);
            if (previousTimeStampForRecording == -1 || difference < 2000) {

            } else {
                Log.e(TAG, "stopVideoRecording Called: ");
                stopVideoRecordingOpsin(false);
                //Handle UI Here by passing stopped state and perform stop rec button ui state
            }

        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stopVideoRecordingOpsin(boolean shouldTriggerMessage) {
        if (mp4encoderNightwave != null) {
            if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
                if (shouldTriggerMessage)
                    hasRecordStoppedDuetoNoLivePackets.postValue(new Event<>(true));
                scheduledExecutorService.shutdownNow();
            }

            isRecordingStarted = false;
            mp4encoderNightwave.stopEncode();
            mp4encoderNightwave.setEncodeFinishListener(new Encoder.EncodeFinishListener() {
                @Override
                public void onEncodeFinished() {
                    Log.e(TAG, "onEncodeFinished: ");
                }

                @Override
                public void onEncodingError(String err) {
                    Log.e(TAG, "onEncodingError: " + err);
                }
            });
        }
    }

    public void stopOpsinLiveStreaming() {
        if (bound && isOpsinLiveStreamingStarted) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.STOP_STREAMING;
                    stopUdpListeners();
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_STOP_STREAMING.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::stopOpsinLiveStreaming, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::stopOpsinLiveStreaming, DELAY);
                }
            }
        }
    }

    public void stopOpsinStreamingStates() {
        handleStopOpsinStreaming();
    }

    private void stopUdpListeners() {
        shouldReceiveUDP = false;
        mService.stopListeningOpsinUdp();
    }

    public void getOpsinRecordingStatus() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_RECORDING_STATE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_RECORDING_STATE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinRecordingStatus, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinRecordingStatus, DELAY);
                }
            }
        }
    }

    public void getOpsinStreamingStatus() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_STREAMING_STATE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_STREAMING_STATE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinStreamingStatus, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinStreamingStatus, DELAY);
                }
            }
        }
    }

    /* OPSIN WIRELESS SPECIFIC COMMANDS, RESPONSES */
    public void getWirelessPassword() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_PASSWORD;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_WIRELESS_SPECIFIC.SCCP_CMD_WIFI_GET_PASSWORD.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getWirelessPassword, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getWirelessPassword, DELAY);
                }
            }
        }
    }

    public void getWirelessMac() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_MAC;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_WIRELESS_SPECIFIC.SCCP_CMD_WIFI_GET_MAC.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getWirelessMac, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getWirelessMac, DELAY);
                }
            }
        }
    }


    /* OPSIN ISP SPECIFIC COMMANDS, RESPONSES */
    public void getEv() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_EV;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_EV.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getEv, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getEv, DELAY);
                }
            }
        }
    }

    public void setEv(byte[] data) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_EV;
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_EV.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setEv(data), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setEv(data), DELAY);
                }
            }
        }
    }

    public void getOpsinBrightness() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                Log.e(TAG, "getOpsinBrightness: " + applyOpsinPeriodicRequest.name() + " " + commandRequested.name());
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_DISPLAY_SPECIFIC.SCCP_CMD_DISPLAY_GET_BRIGHTNESS.getValue(), true);
                    commandRequested = COMMAND_REQUESTED.GET_BRIGHTNESS;
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinBrightness, DELAY);
                    }

                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinBrightness, DELAY);
                }
            }
        }
    }

    public void getOpsinNoiseReduction() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_NR;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_NR.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinNoiseReduction, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinNoiseReduction, DELAY);
                }
            }
        }
    }

    public void setOpsinNoiseReduction(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_NR;
                    byte[] data = new byte[]{state};
                    short sccp_noiseReduction = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_NR.getValue();
                    mService.sendOpsinSetCommand(sccp_noiseReduction, data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinNoiseReduction(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinNoiseReduction(state), DELAY);
                }
            }
        }
    }

    public void getOpsinMonochromatic() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_MONOCHROMATIC;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_MONOCHROMATIC.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinMonochromatic, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinMonochromatic, DELAY);
                }
            }
        }
    }

    public void setOpsinMonochromatic(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_MONOCHROMATIC;
                    byte[] data = new byte[]{state};
                    short sccp_Mono = (short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_MONOCHROMATIC.getValue();
                    mService.sendOpsinSetCommand(sccp_Mono, data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinMonochromatic(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinMonochromatic(state), DELAY);
                }
            }
        }
    }

    public void getOpsinROI() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_ROI;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_ROI.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinROI, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinROI, DELAY);
                }
            }
        }
    }

    public void setOpsinROI(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_ROI;
                    byte[] data = new byte[]{state};
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_ROI.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinROI(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinROI(state), DELAY);
                }
            }
        }
    }

    /* OPSIN SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC COMMAND, RESPONSE*/
    public void setOpsinClockMode(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_CLOCKMODE;
                    byte[] data = new byte[]{state};
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_CLOCKMODE.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinClockMode(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinClockMode(state), DELAY);
                }
            }
        }
    }

    public void getOpsinMetadata() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_METADATA;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_METADATA.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinMetadata, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinMetadata, DELAY);
                }
            }
        }
    }

    public void setOpsinMetadata(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_METADATA;
                    byte[] data = new byte[]{state};
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_METADATA.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinMetadata(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinMetadata(state), DELAY);
                }
            }
        }
    }

    public void getOpsinTimeFormat() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_TIME_FORMAT;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_TIMEFORMAT.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinTimeFormat, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinTimeFormat, DELAY);
                }
            }
        }
    }

    public void setOpsinTimeFormat(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SET_TIME_FORMAT;
                    byte[] data = new byte[]{state};
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_TIMEFORMAT.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setOpsinTimeFormat(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setOpsinTimeFormat(state), DELAY);
                }
            }
        }
    }


    /*OPSIN SCCP_MSG_TYPE_OPSIN_SDCARD_SPECIFIC COMMAND, RESPONSE*/
    public void getOpsinSdCardInfo() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.SDCARD_GET_INFO;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_SDCARD_SPECIFIC.SCCP_CMD_SDCARD_GET_INFO.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinSdCardInfo, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinSdCardInfo, DELAY);
                }
            }
        }
    }

    /* OPSIN GPS SPECIFIC COMMANDS, RESPONSES */
    public void getGpsInfo() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GPS_GET_INFO;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_GET_INFO.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getGpsInfo, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getGpsInfo, DELAY);
                }
            }
        }
    }

    public void getGpsPosition() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GPS_GET_POSITION;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_GET_POSITION.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getGpsPosition, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getGpsPosition, DELAY);
                }
            }
        }
    }

    public void setGpsPower(byte state) {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GPS_SET_POWER;
                    byte[] data = new byte[]{state};
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_SET_POWER.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(() -> setGpsPower(state), DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(() -> setGpsPower(state), DELAY);
                }
            }
        }
    }

    /* OPSIN POWER SPECIFIC COMMANDS, RESPONSES */
    public void getPowerInfo() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.POWER_GET_BATTERY;
                    byte[] data = new byte[]{0x00};
                    mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_POWER_MANAGEMENT.SCCP_CMD_POWER_GET_BATTERY.getValue(), data, true, false);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getPowerInfo, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getPowerInfo, DELAY);
                }
            }
        }
    }


    /* OPSIN FIRMWARE UPGRADE SPECIFIC COMMANDS, RESPONSES */
    public void getOpsinImageState() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.GET_IMAGE_STATE;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_IMAGE_STATE.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getOpsinImageState, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getOpsinImageState, DELAY);
                }
            }
        }
    }

    private String currentSequence;
    private int upgradeStartCount = 0;

    public void startOpsinFwUpdate(String sequences) {
        resetToDefault();
        currentSequence = sequences;
        AssetManager assetManager = application.getAssets();
        Log.e(TAG, "MODE: " + sequences);

        if (sequences.contains("WAIT") || sequences.contains("wait")) {
            fwMode = MODE_WAIT;
            String[] waiting = sequences.split(" ");
            String waitingTime = "10";
            if (waiting.length == 2) {
                waitingTime = waiting[1];
            }
            Log.e(TAG, "startOpsinFwUpdate: REPOSITORY WAIT " + waitingTime);
            waiting_time = Integer.parseInt(waitingTime);
            getVersionIsReceived = false;
            if (scheduledWaitMode == null || scheduledWaitMode.isShutdown()) {
                scheduledWaitMode = Executors.newScheduledThreadPool(1);
                scheduledWaitMode.scheduleWithFixedDelay(() -> {
                    if (countDown < waiting_time) {
                        if (getVersionIsReceived) {
                            receivedOpsinUpgradeComplete();
                            countDown = 0;
                            cancelWaitModeTimer();
                            getVersionIsReceived = false;
                        } else {
                            if (countDown % 5 == 0) {
                                getProductVersion();
                            }
                        }
                        int percentage = (int) ((countDown * 100) / waiting_time);
                        fwUpdateProgress.postValue(percentage);
                        countDown = countDown + 1;
                    } else {
                        String message = "POWER_CYCLE_CAMERA";
                        opsinObserverUpgradeComplete.postValue(new Event<>(message));
                        countDown = 0;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        fwUpdateProgress.postValue(-1);
                        cancelWaitModeTimer();
                    }
                }, 0, 1, TimeUnit.SECONDS);
            }
        } else {
            try {
                switch (sequences) {
                    case RISCV:
                        waitingTimeForUpgradeCompleteProgress = 1;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY RISCV");
                        fwMode = MODE_RISCV;
                        input = assetManager.open(opsinRiscvFileName);
                        break;
                    case FPGA:
                        waitingTimeForUpgradeCompleteProgress = 3;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY FPGA");
                        fwMode = MODE_FPGA;
                        input = assetManager.open(opsinFpgaFileName);
                        break;
                    case WIFI_RTOS:
                        try {
                            fwMode = MODE_WIFI_DIALOG;
                            if (Constants.isOpsinContainsOldWiFiFirmware()) {
                                Log.e(TAG, "startOpsinFwUpdate: REPOSITORY WIFI LOCAL SERVER");
                                if (bound) {
                                    if (localWebServer != null) {
                                        localWebServer.stop();
                                        localWebServer = null;
                                    }
                                    localWebServer = new LocalWebServer(LOCAL_SERVER_PORT, opsinWiFiDialogFileName, application, application);
                                    localWebServer.start();
                                }
                                String wiFiDialogUrl = getWiFiDialogUrl();
                                new Handler().postDelayed(() -> startWiFiDialogUpdate(wiFiDialogUrl), 1000);
                            } else {
                                Log.e(TAG, "startOpsinFwUpdate: REPOSITORY WIFI FILE TRANSFER");
                                if (opsinWiFiDialogFileName.contains(".udf"))
                                    waitingTimeForUpgradeCompleteProgress = 2.5;
                                input = assetManager.open(opsinWiFiDialogFileName);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "startOpsinFwUpdate: " + e.getLocalizedMessage());
                            stopFwUpdate();
                            e.printStackTrace();
                        }
                        break;
                    case OPSIN_WIFI_BLE:
                        waitingTimeForUpgradeCompleteProgress = 2.5;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY OPSIN_WIFI_BLE");
                        fwMode = MODE_OPSIN_BLE;
                        input = assetManager.open(opsinWiFiBleFileName);
                        break;
                    case OPSIN_FULL_IMAGE:
                        waitingTimeForUpgradeCompleteProgress = 3.5;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY FULL_IMAGE");
                        fwMode = MODE_OPSIN_FULL;
                        input = assetManager.open(opsinFullFileName);
                        break;
                    case OPSIN_RISCV_OVERLAY:
                        waitingTimeForUpgradeCompleteProgress = 2;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY RISCV_OVERLAY");
                        fwMode = MODE_OPSIN_RISCV_OVERLAY;
                        input = assetManager.open(opsinRiscvOverlayFileName);
                        break;
                    case OPSIN_RISCV_FPGA:
                        waitingTimeForUpgradeCompleteProgress = 2.5;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY RISCV_FPGA");
                        fwMode = MODE_OPSIN_RISCV_FPGA;
                        input = assetManager.open(opsinRiscvFpgaFileName);
                        break;
                    case OPSIN_RISCV_RECOVERY:
                        waitingTimeForUpgradeCompleteProgress = 1;
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY RISCV_RECOVERY");
                        fwMode = MODE_OPSIN_RECOVERY;
                        input = assetManager.open(opsinRecoveryFileName);
                        break;
                    case OPSIN_RESTART:
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY RESTART");
                        fwMode = MODE_OPSIN_RESTART;
                        restartOpsinCamera();
                        startRestartResetWaitingTimer();
                        break;
                    case OPSIN_FACTORY:
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY FACTORY");
                        fwMode = MODE_OPSIN_FACTORY;
                        setFactoryReset(RESET_SOFT.getValue());
                        startRestartResetWaitingTimer();
                        break;

                }
                if (fwMode == MODE_WIFI_DIALOG) {
                    if (!Constants.isOpsinContainsOldWiFiFirmware()) {
                        totalFpgaRiscvFwLength = input.available();
                        totalNumberOfBlocks = totalFpgaRiscvFwLength / noOfByetsInBlock;
                        int blockSize = (totalFpgaRiscvFwLength + noOfByetsInBlock) / noOfByetsInBlock;

                        Log.e(TAG, "FIRMWARE: totalLength: " + totalFpgaRiscvFwLength + " totalNumberOfBlocks: " + totalNumberOfBlocks + " blockSize: " + blockSize);
                        opsinUpgradeStart(totalFpgaRiscvFwLength, blockSize);
                    }
                } else if (fwMode != MODE_WAIT && fwMode != MODE_OPSIN_RESTART && fwMode != MODE_OPSIN_FACTORY) {
                    totalFpgaRiscvFwLength = input.available();
                    totalNumberOfBlocks = totalFpgaRiscvFwLength / noOfByetsInBlock;
                    int blockSize = (totalFpgaRiscvFwLength + noOfByetsInBlock) / noOfByetsInBlock;

                    Log.e(TAG, "FIRMWARE: totalLength: " + totalFpgaRiscvFwLength + " totalNumberOfBlocks: " + totalNumberOfBlocks + " blockSize: " + blockSize);
                    opsinUpgradeStart(totalFpgaRiscvFwLength, blockSize);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void opsinUpgradeStart(int totalRiscvFwLength, int blockSize) {
        if (bound) {
            byte[] imageTypeBytes = new byte[4];
            switch (fwMode) {
                case MODE_WIFI_DIALOG:
                    if (opsinWiFiDialogFileName.contains(".img")) {
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY OPSIN_IMAGE_TYPE.WIFI_IMAGE");
                        imageTypeBytes = intToBytess(Constants.OPSIN_IMAGE_TYPE.WIFI_IMAGE.getValue());
                    } else if (opsinWiFiDialogFileName.contains(".udf")) {
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY OPSIN_IMAGE_TYPE.CPU");
                        imageTypeBytes = intToBytess(Constants.OPSIN_IMAGE_TYPE.CPU.getValue());
                    }
                    break;
                case MODE_RISCV:
                case MODE_FPGA:
                case MODE_OPSIN_FULL:
                case MODE_OPSIN_RISCV_OVERLAY:
                case MODE_OPSIN_RISCV_FPGA:
                case MODE_OPSIN_RECOVERY:
                    Log.e(TAG, "startOpsinFwUpdate: REPOSITORY OPSIN_IMAGE_TYPE.CPU");
                    imageTypeBytes = intToBytess(Constants.OPSIN_IMAGE_TYPE.CPU.getValue());
                    break;
                case MODE_OPSIN_BLE:
                    if (opsinWiFiBleFileName.contains(".img")) {
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY OPSIN_IMAGE_TYPE.WIFI_IMAGE");
                        imageTypeBytes = intToBytess(Constants.OPSIN_IMAGE_TYPE.BLE_IMAGE.getValue());
                    } else if (opsinWiFiBleFileName.contains(".udf")) {
                        Log.e(TAG, "startOpsinFwUpdate: REPOSITORY OPSIN_IMAGE_TYPE.CPU");
                        imageTypeBytes = intToBytess(Constants.OPSIN_IMAGE_TYPE.CPU.getValue());
                    }
                    break;

            }
            byte[] fwLengthBytes = intToBytess(totalRiscvFwLength);
            byte[] blockSizeBytes = intToBytess(blockSize);

            ByteBuffer bbRequest = ByteBuffer.allocate(12);
            bbRequest.put(imageTypeBytes);
            bbRequest.put(fwLengthBytes);
            bbRequest.put(blockSizeBytes);

            mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_START.getValue(), bbRequest.array(), true, false);
        }
    }

    private void opsinUpgradeData(byte[] data) {
        if (bound) {
            isDataBlockSent = true;
//            Log.e(TAG, "opsinUpgradeData: SCCP_CMD_UPGRADE_DATA");
            mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DATA.getValue(), data, true, false);
        }
    }

    private boolean upgradeDataCompleteSent = false;
    private int wifiDataBlockPercentage = 0;

    private void opsinUpgradeDataComplete() {
        if (bound && !isRiscvUpdateCompleteSent) {
            Log.e(TAG, "opsinUpgradeDataComplete: sent");
            isRiscvUpdateCompleteSent = true;
            upgradeDataCompleteSent = true;
            mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DATA_COMPLETE.getValue(), true);
        }
    }

    public void opsinUpgradeCancel() {
        if (bound) {
            mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_CANCEL.getValue(), true);
        }
    }

    private void startOpsinDialogUpgrade(String wiFiDialogUrl) {
        if (bound) {
            byte[] urlBytes = wiFiDialogUrl.getBytes();
            short length = (short) urlBytes.length;
            byte[] lengthBytes = shortToByteArray(length);

            ByteBuffer bbRequest = ByteBuffer.allocate(length + 4);
            bbRequest.put(lengthBytes);
            bbRequest.put(new byte[]{0x00, 0x00});
            bbRequest.put(urlBytes);

            mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DIALOG_OTA.getValue(), bbRequest.array(), true, false);
        }
    }

    public void setOpsinMasterVersion(String sMajor, String sMinor, String sPatch) {
        if (bound) {
            byte iMajor = Byte.parseByte(sMajor);
            byte iMinor = Byte.parseByte(sMinor);
            byte iPatch = Byte.parseByte(sPatch);
            Log.e(TAG, "setOpsinMasterVersion: " + iMajor + " " + iMinor + " " + iPatch);
            byte[] data = new byte[]{iMajor, iMinor, iPatch, 0x00, 0x00, 0x00};
            mService.sendOpsinSetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_SET_VERSION.getValue(), data, true, false);
        }
    }

    /*OPSIN COMPASS_SPECIFIC*/
    public void getCompassValue() {
        if (bound) {
            if (applyOpsinPeriodicRequest != TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
                if (commandRequested == COMMAND_REQUESTED.NONE) {
                    commandRequested = COMMAND_REQUESTED.COMPASS_GET_ANGLES;
                    mService.sendOpsinGetCommand((short) SCCPConstants.SCCP_MSG_TYPE_OPSIN_COMPASS_SPECIFIC.SCCP_CMD_COMPASS_GET_ANGLES.getValue(), true);
                } else {
                    if (mState == STATE_CONNECTED) {
                        waitHandler.postDelayed(this::getCompassValue, DELAY);
                    }
                }
            } else {
                if (mState == STATE_CONNECTED) {
                    waitHandler.postDelayed(this::getCompassValue, DELAY);
                }
            }
        }
    }

    public void triggerNextBlock() {
        Log.e(TAG, "triggerNextBlock:");
        opsinCommandRetryCount = 0;
        isDataBlockSent = false;
        sendNextBlock();
    }

    public void triggerCancelUpgrade() {
        opsinUpgradeCancel();
    }


    public void nightwaveDigitalCameraInitializeStreaming() {
        Log.e(TAG, "nightwaveDigitalCameraInitializeStreaming called ");
        streamExecutor.execute(() -> {
            nativeInit();
            nativeSetGstDebug(); // safe
        });

    }

    public void nightwaveDigitalCameraStartStreaming() {
        notifyIsStreamingInBuffer.postValue(true);
        isErrorCaptured = false;
        isStreamingChanged = false;

        if (!isStreamingStarted) {
            nightwaveDigitalCameraInitializeStreaming();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            streamExecutor.execute(() -> {
                nativeStartStream(getCurrentStreamingUrl()); // safe
            });

        }

        Log.e(TAG, "nightwaveDigitalCameraStartStreaming called " + getCurrentStreamingUrl());

        if (rtspState == RTSP_DISCONNECTED || rtspState == RTSP_NONE) {
            rtspState = RTSP_CONNECTED;
        }
    }




    public void setStopStreamingDigitalCamera() {
        Log.e(TAG, "setStopStreamingDigitalCamera called " + isStreamingStarted);
//        if (!isStreamingStarted)
//            return;

        try {
            // 1. First stop feeding frames from GStreamer to decoder
            // This should be done in your GStreamer callback/thread
            // Add: notifyFrameSourceToStop();

            // 2. Stop and release the H264 decoder PROPERLY
            if (h264Decoder != null) {
                Log.e(TAG, "Stopping H264 decoder");

                // Use a combined stop-and-release method
                stopAndReleaseDecoderSafely(h264Decoder);
                h264Decoder = null;
            }

            // 3. Now stop the native stream
            Log.e(TAG, "Stopping native stream");
            // safe
            streamExecutor.execute(this::nativeStopStream);

            // 4. Give native stream time to clean up (this should be in native code)
            // Remove the sleep here, nativeStopStream() should block until done

        } catch (Exception e) {
            Log.e("MainActivity", "Error stopping stream", e);

        } finally {
            // 5. Force cleanup if anything is stuck
            forceCleanupIfNeeded();

            // 6. Finally release native resources
            Log.e(TAG, "Releasing native resources");
            streamExecutor.execute(this::nativeRelease);
        }

        if (rtspState == RTSP_CONNECTED)
            rtspState = RTSP_DISCONNECTED;

        // Mark streaming as stopped
        isStreamingStarted = false;
        Log.e(TAG, "Streaming stopped completely");
    }

    private void stopAndReleaseDecoderSafely(H264DecoderDeviceSpecific decoder) {
        if (decoder == null) return;

        Log.e(TAG, "Starting safe decoder shutdown");

        // Step 1: Stop accepting new frames and signal threads
        decoder.safeStop(); // You need to add this method to decoder

        // Step 2: Wait for decoder to actually stop (with timeout)
        long startTime = System.currentTimeMillis();
        while (decoder.isRunning() && (System.currentTimeMillis() - startTime) < 1000) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Step 3: If still running, force stop
        if (decoder.isRunning()) {
            Log.w(TAG, "Decoder still running after timeout, forcing stop");
            h264Decoder.forceStop(); // You need to add this method
        }

        // Step 4: Now release resources
        try {
            h264Decoder.release();
        } catch (Exception e) {
            Log.e(TAG, "Error during decoder release, trying force release", e);
            // Try force release if normal release fails
            decoder.forceRelease();
        }

        Log.e(TAG, "Safe decoder shutdown complete");
    }

    private void forceCleanupIfNeeded() {
        // Check if decoder is still referenced and force cleanup
        if (h264Decoder != null) {
            Log.w(TAG, "Decoder still exists in force cleanup, forcing release");
            h264Decoder.forceRelease();
            h264Decoder = null;
        }

        // Also check for any other resources that might be stuck
        System.gc();
    }

//    public void setStopStreamingDigitalCamera() {
//        Log.e(TAG, "setStopStreamingDigitalCamera called " + isStreamingStarted);
//        if (!isStreamingStarted)
//            return;
//
//        try {
//            // 1. First stop the H264 decoder (stop receiving frames)
//            if (h264Decoder != null) {
//                Log.e(TAG, "Stopping H264 decoder");
//                h264Decoder.stopDecoding();  // NEW: Stop decoding first
//
//                // Wait for decoder to stop processing
//                Thread.sleep(100);
//
//                // Now release it
//                releaseH264Decoder();
//            }
//
//            // 2. Give decoder time to fully stop
//            Thread.sleep(50);
//
//            // 3. Now stop the native stream
//            Log.e(TAG, "Stopping native stream");
//            nativeStopStream();
//
//            // 4. Give native stream time to clean up
//            Thread.sleep(200);
//
//        } catch (Exception e) {
//            Log.e("MainActivity", "Error stopping stream", e);
//
//        } finally {
//            // 5. Wait to ensure all threads have exited
//            try {
//                Thread.sleep(300);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            // 6. Finally release native resources
//            Log.e(TAG, "Releasing native resources");
//            nativeRelease();
//        }
//
//        if (rtspState == RTSP_CONNECTED)
//            rtspState = RTSP_DISCONNECTED;
//
//        // Mark streaming as stopped
//        isStreamingStarted = false;
//        Log.e(TAG, "Streaming stopped completely");
//    }
//
//    private void releaseH264Decoder() {
//        if (h264Decoder != null) {
//            try {
//                Log.e(TAG, "H264Decoder released ");
//                h264Decoder.release();
//            } catch (Exception e) {
//                Log.e(TAG, "Error releasing decoder", e);
//            }
//            h264Decoder = null;
//        }
//
//        // mediacodec is released and streaming should be posted as stopped to avoid black screen
//        if (retryCountValue > 0) {
//            notifyDigitalStreamingStatus.postValue(false);
//        }
//    }

    public void setStopRetryAndResetCommand(){
        Log.e(TAG, "setStopRetryAndResetCommand: " );
        streamExecutor.execute(this::nativeStopRetryAndReset);
    }

    public void nightwaveDigitalCameraChangeResolution() {

        notifyIsStreamingInBuffer.postValue(true);
        streamExecutor.execute(() -> {
            nativeChangeStreamUrl(getCurrentStreamingUrl());
        });

        Log.e(TAG, "nightwaveDigitalCameraChangeResolution called ");
    }

    public void nightwaveDigitalCameraReleaseLiveStreaming() {
        Log.e(TAG, "nightwaveDigitalCameraReleaseLiveStreaming called ");
//        nwdTextureView = null;
        videoSurface = null;
        nativeRelease();
        if (rtspState == RTSP_CONNECTED || rtspState == RTSP_NONE)
            rtspState = RTSP_DISCONNECTED;

    }

    /**
     * Use current stream quality to select URL
     */
    private String getCurrentStreamingUrl() {
        return currentStreamQuality == STREAM_QUALITY_LOW
                ? Constants.getLoadUrl(LOWRES_LIVE_STREAMING_ACTION)
                : Constants.getLoadUrl(HIGHRES_LIVE_STREAMING_ACTION);
    }


    public void onStreamStarted() {
        Log.d(TAG, "Stream started!");
        isStreamingStarted = true;
        isVideoSizeSet = false;
        notifyDigitalStreamingStatus.postValue(true);
        rtspState = RTSP_CONNECTED;
    }

    public void onStreamStopped(int state) {
        if (state == 6) {
            Log.i(TAG, "Stream stopping initiated.");
        } else if (state == 9) {
            Log.i(TAG, "Stream completely stopped.");
            isStreamingStarted = false;
            isVideoSizeSet = false;
            if (rtspState == RTSP_CONNECTED || rtspState == RTSP_NONE)
                rtspState = RTSP_DISCONNECTED;

            notifyDigitalStreamingStatus.postValue(false);
            isStartedReceivingPackets = false;
        }
    }

    public void onLowFpsLongTime() {
        Log.d(TAG, "onLowFpsLongTime for an extended period. isStreamingStarted = " + isStreamingStarted);
        if (isStreamingStarted && rtspState == RTSP_CONNECTED)
            notifyDigitalNoFpsFound.postValue(true);

        isStreamingStarted = false;

    }

    public void onBufferingStateChanged(boolean isBuffering) {
        notifyIsStreamingInBuffer.postValue(isBuffering);
        Log.d(TAG, "Buffering state changed: " + isBuffering);
    }

    public void onFPSChanged(int fps) {
        Log.d(TAG, "onFPSChanged: " + fps);
        if (h264Decoder != null) {
            h264Decoder.onFpsChanged(fps);
        }
        // get dynamic frame rate for recorded video
        if (videoRecorder != null && videoRecorder.isRecording()) videoRecorder.onFpsChanged(fps);
        observeLiveFpsValue.postValue(fps);

    }

    public void onProgressStatus(final int errorCode, final String message) {
        switch (errorCode) {
            case PROGRESS_TYPE_START:
            case PROGRESS_TYPE_CONTINUE:
                //stream initializing
                notifyIsStreamingInBuffer.postValue(true);
                break;
            case PROGRESS_TYPE_COMPLETE:
                //stream ready
                isStreamingStarted = true;
//                notifyRetryValue.postValue(new Event<>(false));
                isErrorCaptured = false;
                break;
            case PROGRESS_TYPE_CANCELED:
                //stream cancelled
                if (isStreamingStarted) {
                    notifyDigitalNoFpsFound.postValue(false);
                }
                break;
            case PROGRESS_TYPE_ERROR:
                //stream returns error need to power cycle the camera
                notifyIsStreamingInBuffer.postValue(false);
                isStreamingStarted = false;
                break;
        }
        Log.e(TAG, "onProgressStatus (code " + errorCode + "): " + message);
    }

    public void onStreamError(final int errorCode, final String message) {
        Log.i(TAG, "onStreamError errorCode : " + errorCode + " message " + message + " streamStarted " + isStreamingStarted);
        Log.i(TAG, "onStreamError isErrorCaptured : " + isErrorCaptured + " retryValue " + retryCountValue + "isLowres = 0 >> " + currentStreamQuality);
        notifyIsStreamingInBuffer.postValue(false);

        if (currentStreamQuality == STREAM_QUALITY_HIGH && retryCountValue >= 3 && !isErrorCaptured) {
            //Automatically changed the streaming from highres to lowres when max retry has reached
            notifyIsStreamingInBuffer.postValue(true);
            notifyRetryValue.postValue(new Event<>(true));
            isStreamingChanged = true;
            retryCountValue = 0;
            isStreamingStarted = false;
        }

//        if (/*isStreamingStarted &&*/ (!isErrorCaptured && retryCountValue == 0) || (!isErrorCaptured && retryCountValue >= 3)) {
        if (!isErrorCaptured /*&& (retryCountValue == 0 ||  retryCountValue >= 3)*/) {
            notifyOnStreamingError.postValue(new Event<>(errorCode));
            isErrorCaptured = true;
            retryCountValue = 0;
            isStreamingStarted = false;
            // Mark streaming as stopped
//            if (isStreamingStarted)
//                setStopStreamingDigitalCamera();
        }

    }

    public void onStreamLoadingRetry(int retryCount, int maxRetries, boolean show) {
        if (show) {
            Log.i(TAG, "Retrying stream: attempt " + retryCount + " of " + maxRetries + " isErrorCaptured " + isErrorCaptured);
//            releaseH264Decoder();
            stopAndReleaseDecoderSafely(h264Decoder);

            long retryData = (((long) maxRetries) << 32) | (retryCount & 0xFFFFFFFFL);
            retryCountValue = retryCount;

            if (!isErrorCaptured) {
                notifyRetryValue.postValue(new Event<>(retryData));
                notifyIsStreamingInBuffer.postValue(false);
            }
        } else {
            Log.i(TAG, "Stream resumed. Dismissing loader.");
            if (!isStreamingChanged || retryCount == 3)
                notifyRetryValue.postValue(new Event<>(false));
            retryCountValue = 0;
        }
    }

    public void onStreamRecovered() {
        Log.d(TAG, "onStreamRecovered: ");
    }


    private void sendNextBlock() {
        if (noOfByetsInBlock == 256) {
//            Log.e(TAG, "FIRMWARE: sendNextBlock " + receivedCount + " " + totalNumberOfBlocks);
            if (receivedCount == totalNumberOfBlocks) {
                if (!isRiscvUpdateCompleteSent) {
                    Log.e(TAG, "FIRMWARE: SEND UPDATE_COMPLETE receivedCount == totalNumberOfBlocks");
                    opsinUpgradeDataComplete();
                }
            } else if (receivedCount < totalNumberOfBlocks) {
                if (totalNumberOfBlocks != 0) {
                    receivedCount = receivedCount + 1;
                    int index = (currentBlockNumber * noOfByetsInBlock);
                    int percentage = (int) ((currentBlockNumber * 100) / totalNumberOfBlocks) / 2;
                    fwUpdateProgress.postValue(percentage);
                    readOpsinFile(index);
                }
            }
        } else {
            sendBlockHandler.postDelayed(() -> {
//                Log.e(TAG, "FIRMWARE: sendNextBlock " + receivedCount + " " + totalNumberOfBlocks);
                try {
                    if (receivedCount == totalNumberOfBlocks) {
                        if (!isRiscvUpdateCompleteSent) {
                            Log.e(TAG, "FIRMWARE: SEND UPDATE_COMPLETE receivedCount == totalNumberOfBlocks");
                            opsinUpgradeDataComplete();
                        }
                    } else if (receivedCount < totalNumberOfBlocks) {
                        if (totalNumberOfBlocks != 0) {
                            receivedCount = receivedCount + 1;
                            int index = (currentBlockNumber * noOfByetsInBlock);
                            int percentage = (int) ((currentBlockNumber * 100) / totalNumberOfBlocks) / 2;
                            wifiDataBlockPercentage = percentage;
                            Log.e(TAG, "sendNextBlock: Percentage: " + percentage);
                            if (receivedCount != totalNumberOfBlocks)
                                fwUpdateProgress.postValue(percentage);
                            readOpsinFile(index);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 100);
        }
    }

    private void readOpsinFile(int index) {
        try {
            if (currentBlockNumber == 0) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                int available = input.available();
                totalBuffersInRiscv = new byte[available];

                int length = input.read(totalBuffersInRiscv);
                if (length != -1) {
                    output.write(totalBuffersInRiscv, index, length);
                    totalBuffersInRiscv = output.toByteArray();
                    output.flush();
                    output.close();

                    byte[] data = new byte[noOfByetsInBlock];
                    System.arraycopy(totalBuffersInRiscv, index, data, 0, data.length);
                    short lengthOfData = (short) data.length;


                    ByteBuffer bbRequest = ByteBuffer.allocate(data.length + 8);
                    bbRequest.put(shortToByteArray(currentBlockNumber));
                    bbRequest.put(shortToByteArray(lengthOfData));
                    bbRequest.put(intToBytess(index));
                    bbRequest.put(data);

                    opsinUpgradeData(bbRequest.array());
                    currentBlockNumber++;
                }
                input.close();
            } else {
                byte[] data = new byte[0];
                int remainingBytes = totalFpgaRiscvFwLength - index;
                Log.e(TAG, "readOpsinFile: "
                        + " ReceivedCount: " + receivedCount
                        + " TotalBlocks: " + totalNumberOfBlocks
                        + " CurrentBlock: " + currentBlockNumber
                        + " CurrentIndex: " + index
                        + " BytesToSend: " + remainingBytes);
                if (remainingBytes < noOfByetsInBlock) {
                    if (remainingBytes > 0) {
                        data = new byte[remainingBytes];
                    } else {
                        if (!isRiscvUpdateCompleteSent) {
                            opsinUpgradeDataComplete();
                        }
                    }
                } else {
                    data = new byte[noOfByetsInBlock];
                }
                if (data.length != 0) {
                    System.arraycopy(totalBuffersInRiscv, index, data, 0, data.length);
                    short lengthOfData = (short) data.length;

                    ByteBuffer bbRequest = ByteBuffer.allocate(data.length + 8);
                    bbRequest.put(shortToByteArray(currentBlockNumber));
                    bbRequest.put(shortToByteArray(lengthOfData));
                    bbRequest.put(intToBytess(index));
                    bbRequest.put(data);
                    opsinUpgradeData(bbRequest.array());
                    currentBlockNumber++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private final CommandError error = new CommandError();

    public boolean isInManagementSpecificRange(int value) {
        return value >= 0 && value <= 63;
    }

    public boolean isInUpgradeProtocolRange(int value) {
        return value >= 64 && value <= 127;
    }

    public boolean isInProductSpecificRange(int value) {
        return value >= 128 && value <= 2047;
    }

    public boolean isInPowerManagementRange(int value) {
        return value >= 2048 && value <= 4095;
    }

    public boolean isInSystemIORange(int value) {
        return value >= 4096 && value <= 6143;
    }

    public boolean isInDisplaySpecificRange(int value) {
        return value >= 6144 && value <= 8191;
    }

    public boolean isInSensorSpecificRange(int value) {
        return value >= 8192 && value <= 10239;
    }

    public boolean isInISPSpecificRange(int value) {
        return value >= 10240 && value <= 12287;
    }

    public boolean isInSDCardSpecificRange(int value) {
        return value >= 12288 && value <= 14335;
    }

    public boolean isInWirelessSpecificRange(int value) {
        return value >= 14336 && value <= 16383;
    }

    public boolean isInGpsSpecificRange(int value) {
        return value >= 16384 && value <= 18431;
    }

    public boolean isInCompassSpecificRange(int value) {
        return value >= 18432 && value <= 20479;
    }

    public boolean isInTelemetrySpecificRange(int value) {
        return value >= 22528 && value <= 24575;
    }

    public boolean isInManufacturingSpecificRange(int value) {
        return value >= 63488 && value <= 65535;
    }


    @Override
    public void updateOpsinResponse(byte[] result) {
        byte[] SOF = {result[0], result[1], result[2], result[3]};

        //Length,Version
        byte[] headerBytes = {result[4], result[5]};
        short header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        int len = headerToReceiveLength(header);
        int version = headerToReceiveVersion(header);

        //Flag
        byte flag = result[6];

        //Seq
        byte seq = result[7];

        //Command
        byte[] commandBytes = {result[8], result[9]};
        byte[] commandBytesToCheckUnsignedValue = {result[8], result[9], 0, 0};
        int command = ByteBuffer.wrap(commandBytesToCheckUnsignedValue).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int shortCommand = ByteBuffer.wrap(commandBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
//        Log.d(TAG, "RECEIVED_COMMAND : " + command + " flag: " + flag);

        //CRC16
        byte[] crc16Bytes = {result[10], result[11]};

        //DATA
        byte[] dataArray = new byte[len];
        System.arraycopy(result, 12, dataArray, 0, dataArray.length);

        //CRC32
        int lastDataPos = 12 + (len - 1);
        byte[] crc32Bytes = {result[lastDataPos + 1], result[lastDataPos + 2], result[lastDataPos + 3], result[lastDataPos + 4]};

        //EOF
        byte eof = result[lastDataPos + 5];

        /*----------Check CRC Values-------------*/
        boolean isCrcValuesAreMatched = checkCRCValues(SOF, headerBytes, flag, seq, commandBytes, crc16Bytes, dataArray, crc32Bytes, eof);

        if (isCrcValuesAreMatched) {
            if (isInManagementSpecificRange(command)) {
                processManagementSpecificResponse(command, dataArray);
            } else if (isInUpgradeProtocolRange(command)) {
                processUpgradeSpecificResponse(command, dataArray);
            } else if (isInProductSpecificRange(command)) {
                processProductSpecificResponse(command, dataArray, flag);
            } else if (isInPowerManagementRange(command)) {
                processPowerManagementSpecificResponse(command, dataArray);
            } else if (isInDisplaySpecificRange(command)) {
                processDisplaySpecificResponse(command, dataArray);
            } else if (isInISPSpecificRange(command)) {
                processISPSpecificResponse(command, dataArray);
            } else if (isInSDCardSpecificRange(command)) {
                processSDCardSpecificResponse(command, dataArray);
            } else if (isInWirelessSpecificRange(command)) {
                processWirelessSpecificResponse(command, dataArray);
            } else if (isInGpsSpecificRange(command)) {
                processGpsSpecificResponse(command, dataArray);
            } else if (isInCompassSpecificRange(command)) {
                processCompassSpecificResponse(command, dataArray);
            } else if (isInManufacturingSpecificRange(command)) {
                processManufacturingSpecificResponse(command, dataArray);
            }
        } else {
            Log.e(TAG, "CRC Values Mismatch || Flag is 1 (Request from camera)");
            error.setError("CRC Values Mismatch");
            opsinCameraCommandError.postValue(new Event<>(error));
        }

    }

    private void processManagementSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_SCCP_MANAGEMENT.SCCP_CMD_KEEPALIVE.getValue()) {
            Log.e(TAG, "updateOpsinResponse: keepAlive");
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (Constants.isOpsinContainsOldWiFiFirmware()) {
                        if (dataArray.length >= 10) {
                            byte majorVersion = dataArray[4];
                            byte minorVersion = dataArray[5];
                            byte patchVersion = dataArray[6];
                            byte rx_seq = dataArray[8];
                            byte tx_seq = dataArray[9];
                            Log.e(TAG, "SCCP_ACK_SUCCESS: \nACK: " + ack + "\nmajorVersion:" + majorVersion + "\nminorVersion:" + minorVersion + "\npatchVersion:" + patchVersion + "\nrx_seq:" + rx_seq + "\nrx_seq:" + tx_seq);
                        }
                    } else {
                        byte rx_seq = dataArray[4];
                        byte tx_seq = dataArray[5];
                        Log.e(TAG, "SCCP_ACK_SUCCESS: \nACK: " + ack + "\nrx_seq:" + rx_seq + "\nrx_seq:" + tx_seq + "\n");
                    }

                    notifyPeriodicRequest();
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_KEEPALIVE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_KEEPALIVE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_KEEPALIVE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_KEEPALIVE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    break;
            }
        }
    }

    private void processUpgradeSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DIALOG_OTA.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DIALOG_OTA: ACK: SCCP_ACK_SUCCESS\n");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DIALOG_OTA: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DIALOG_OTA: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DIALOG_OTA: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DIALOG_OTA: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_START.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_START: ACK: SCCP_ACK_SUCCESS\n");
                    upgradeStartCount = 0;
                    wifiDataBlockPercentage = 0;
                    readOpsinFile(0);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_START: SCCP_ACK_FAILURE\n");
                    if (upgradeStartCount < 3) {
                        startOpsinFwUpdate(currentSequence);
                        upgradeStartCount = upgradeStartCount + 1;
                    } else {
                        upgradeStartCount = 0;
                        sendUpgradeFailed();
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_START: SCCP_ACK_UNSUPPORTED\n");
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_START: SCCP_ACK_UNKNOWN\n");
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_START: SCCP_ACK_MAX\n");
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DATA.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA: SCCP_ACK_SUCCESS\n");
                    opsinCommandRetryCount = 0;
                    isDataBlockSent = false;
                    byte[] blockNumber = new byte[2];
                    byte[] lengthOfTheData = new byte[2];
                    byte[] offset = new byte[4];

                    System.arraycopy(dataArray, 4, blockNumber, 0, blockNumber.length);
                    short blockNumberr = ByteBuffer.wrap(blockNumber).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    System.arraycopy(dataArray, 6, lengthOfTheData, 0, lengthOfTheData.length);
                    short lengthOfTheDataa = ByteBuffer.wrap(lengthOfTheData).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    System.arraycopy(dataArray, 8, offset, 0, offset.length);
                    short offsett = ByteBuffer.wrap(offset).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    sendNextBlock();
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA: SCCP_ACK_FAILURE\n");
                    sendSameDataBlock();
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA: SCCP_ACK_UNSUPPORTED\n");
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA: SCCP_ACK_UNKNOWN\n");
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA:SCCP_ACK_MAX\n");
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_DATA_COMPLETE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    receivedCount = -1;
                    upgradeDataCompleteSent = false;

                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA_COMPLETE: ACK: SCCP_ACK_SUCCESS\n");
                    //Start Waiting Timer of desired minutes based on firmware component and display dialog to user if no response were received
                    startWaitingTimerForUpgradeProgressUpgradeComplete();
                    break;
                case SCCP_ACK_FAILURE:
                    lastSentCommand.setData(null);
                    opsinUpgradeCancel();
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA_COMPLETE: SCCP_ACK_FAILURE\n");
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA_COMPLETE: SCCP_ACK_UNSUPPORTED\n");
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA_COMPLETE: SCCP_ACK_UNKNOWN\n");
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_DATA_COMPLETE: SCCP_ACK_MAX\n");
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_PROGRESS.getValue()) {
            Log.e(TAG, "SCCP_CMD_UPGRADE_PROGRESS: " + Arrays.toString(dataArray) + " " + dataArray[0] + " " + isDataBlockSent + "\n");
            int progress = dataArray[0];
            /*Sending same block during firmware upgrade is now eliminated, since it is taking care in countdown timer for base version and EAGAIN for other versions*/
            /*if (!isRiscvUpdateCompleteSent) {
                if (fwMode == MODE_WIFI_DIALOG) {
                    if (!isOpsinContainsOldWiFiFirmware()) {
                        if (isDataBlockSent) {
                            sendSameDataBlock();
                        }
                    }
                } else {
                    if (isDataBlockSent) {
                        sendSameDataBlock();
                    }
                }
            } else */

            if (progress > 50) {
                if (upgradeDataCompleteSent) {
                    startWaitingTimerForUpgradeProgressUpgradeComplete();
                    upgradeDataCompleteSent = false;
                }
                if (wifiDataBlockPercentage < 50) {
                    Log.e(TAG, "processUpgradeSpecificResponse: do not consider the progress value received from camera if data block is not sent 100%");
                } else {
                    lastSentCommand.setData(null);
                    fwUpdateProgress.postValue(progress);
                    hideSpinnerDialog();
                    previousTimeStamp = System.currentTimeMillis();
                    if (progress == 100) {
                        //Start Executor Timer for 10 seconds to monitor SCCP_CMD_UPGRADE_COMPLETE
                        monitorUpgradeCompleteMessageCount = 0;
                        monitorUpgradeCompleteAfter100Percent();
                    }
                }
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_COMPLETE.getValue()) {
            Log.e(TAG, "SCCP_CMD_UPGRADE_COMPLETE: " + Arrays.toString(dataArray));
            opsinUpgradeCompleteReceived = true;
            stopWaitingTimerForUpgradeProgressUpgradeComplete();
            hideSpinnerDialog();
            if (fwMode == MODE_WIFI_DIALOG) {
                if (scheduledWaitingTimerForUpgradeProgressUpgradeComplete != null && !scheduledWaitingTimerForUpgradeProgressUpgradeComplete.isShutdown()) {
                    Log.e(TAG, "receivedOpsinUpgradeComplete: MODE_WIFI_DIALOG scheduledWaitingTimerForUpgradeProgressUpgradeComplete is Alive");
                    waitingTimeForUpgradeCompleteProgress = 3;
                    previousTimeStamp = System.currentTimeMillis();
                    startWaitingTimerForUpgradeProgressUpgradeComplete();
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "receivedOpsinUpgradeComplete: MODE_WIFI_DIALOG");
                    waitingTimeForUpgradeCompleteProgress = 3;
                    previousTimeStamp = System.currentTimeMillis();
                    startWaitingTimerForUpgradeProgressUpgradeComplete();
                }
            } else {
                receivedOpsinUpgradeComplete();
            }

        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_CANCEL.getValue()) {
            Log.e(TAG, "SCCP_CMD_UPGRADE_CANCEL: ACK: SCCP_CMD_UPGRADE_CANCEL " + Arrays.toString(dataArray));
            stopWaitingTimerForUpgradeProgressUpgradeComplete();
            hideSpinnerDialog();
            if (dataArray.length == 0) {
                sendUpgradeFailed();
            } else {
                byte[] ackBytes = new byte[4];
                System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
                byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
                switch (ack) {
                    case SCCP_ACK_SUCCESS:
                        Log.e(TAG, "SCCP_CMD_UPGRADE_CANCEL: ACK: SCCP_ACK_SUCCESS");
                        lastSentCommand.setData(null);
                        sendUpgradeFailed();
                        break;
                    case SCCP_ACK_FAILURE:
                        Log.e(TAG, "SCCP_CMD_UPGRADE_CANCEL: SCCP_ACK_FAILURE: ");
                        sendUpgradeFailed();
                        break;
                    case SCCP_ACK_UNSUPPORTED:
                        Log.e(TAG, "SCCP_CMD_UPGRADE_CANCEL: SCCP_ACK_UNSUPPORTED: ");
                        break;
                    case SCCP_ACK_UNKNOWN:
                        Log.e(TAG, "SCCP_CMD_UPGRADE_CANCEL: SCCP_ACK_UNKNOWN: ");
                        break;
                    case SCCP_ACK_MAX:
                        Log.e(TAG, "SCCP_CMD_UPGRADE_CANCEL: SCCP_ACK_MAX: ");
                        break;
                }
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_UPGRADE_PROTOCOL.SCCP_CMD_UPGRADE_SET_VERSION.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_SET_VERSION : ACK: SCCP_ACK_SUCCESS\n");
                    opsinSetMasterVersion.postValue(new Event<>(true));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_UPGRADE_SET_VERSION : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
            }
        }
    }

    private void processProductSpecificResponse(int command, byte[] dataArray, byte flag) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_VERSION.getValue()) {
            if (dataArray.length > 0) {
                byte[] ackBytes = new byte[4];
                byte majorVersion = dataArray[4];
                byte minorVersion = dataArray[5];
                byte patchVersion = dataArray[6];
                byte reserved = dataArray[7];

                opsinVersionDetails.setMajor(majorVersion);
                opsinVersionDetails.setMinor(minorVersion);
                opsinVersionDetails.setPatch(patchVersion);

                System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
                byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
                Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: " + ack);
                switch (ack) {
                    case SCCP_ACK_SUCCESS:
                        if (lastSentCommand.getCommand() == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_VERSION.getValue()) {
                            if (fwMode == MODE_WAIT) {
                                Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: MODE_WAIT");
                                getVersionIsReceived = true;
                            } else {
                                Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION ELSE");
                                byte[] arrayLength = new byte[4];
                                System.arraycopy(dataArray, 8, arrayLength, 0, arrayLength.length);
                                int size = ByteBuffer.wrap(arrayLength).order(ByteOrder.LITTLE_ENDIAN).getInt();

                                byte[] arrComponents = new byte[size];
                                System.arraycopy(dataArray, 12, arrComponents, 0, arrComponents.length);
                                String components = new String(arrComponents, StandardCharsets.UTF_8);
                                String updatedComponents = components.replaceAll("\0", "");
                                Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: " + updatedComponents);
                                String[] componentsNameList = updatedComponents.split(",");

                                opsinVersionDetails.setRiscv(null);
                                opsinVersionDetails.setFpga(null);
                                opsinVersionDetails.setOverlay(null);
                                opsinVersionDetails.setRiscvRecovery(null);
                                opsinVersionDetails.setIsp(null);
                                opsinVersionDetails.setWifi(null);
                                opsinVersionDetails.setBle(null);

                                for (String component : componentsNameList) {
                                    String[] componentNameVersion = component.split("=");
                                    switch (componentNameVersion[0]) {
                                        case "RISCV":
                                            if (componentNameVersion[1].contains("-")) {
                                                String[] split = componentNameVersion[1].split("-");
                                                opsinVersionDetails.setRiscv(split[0]);
                                            } else {
                                                opsinVersionDetails.setRiscv(componentNameVersion[1]);
                                            }
                                            break;
                                        case "FPGA":
                                            if (componentNameVersion[1].equalsIgnoreCase(OPSERY_FPGA_TABLE_INDEX)) {
                                                componentNameVersion[1] = OPSERY_FPGA_TABLE_VALUE;
                                            }
                                            // opsin Falcon firmware upgrade version comparison
                                            /* else if (componentNameVersion[1].equalsIgnoreCase(OPSERY_FPGA_TABLE_INDEX2)){
                                                componentNameVersion[1] = OPSERY_FPGA_TABLE_VALUE2;
                                            }*/
                                            opsinVersionDetails.setFpga(componentNameVersion[1]);
                                            break;
                                        case "OSD":
                                            opsinVersionDetails.setOverlay(componentNameVersion[1]);
                                            break;
                                        case "RISCV-RECOVERY":
                                            opsinVersionDetails.setRiscvRecovery(componentNameVersion[1]);
                                            break;
                                        case "ISP":
                                            opsinVersionDetails.setIsp(componentNameVersion[1]);
                                            break;
                                        case "WI-FI":
                                        case "WIFI":
                                            opsinVersionDetails.setWifi(componentNameVersion[1]);
                                            break;
                                        case "BLE":
                                            String pattern = "\\d+\\.\\d+\\.\\d+(\\.\\d+)?"; // Matches X.Y.Z or X.Y.Z.ID
                                            Pattern regex = Pattern.compile(pattern);
                                            Matcher matcher = regex.matcher(componentNameVersion[1]);
                                            if (matcher.matches()) {
                                                opsinVersionDetails.setBle(componentNameVersion[1]);
                                                Log.e(TAG, "updateOpsinResponse: " + componentNameVersion[1] + " is allowed");
                                            } else {
                                                opsinVersionDetails.setBle("1.0.0");
                                                Log.e(TAG, "updateOpsinResponse: " + componentNameVersion[1] + " is skipped");
                                            }
                                            break;
                                    }
                                }
                                String ble = opsinVersionDetails.getBle();
                                String overlay = opsinVersionDetails.getOverlay();
                                String recovery = opsinVersionDetails.getRiscvRecovery();
                                if (overlay == null) {
                                    opsinVersionDetails.setOverlay(strUnknown);
                                }
                                if (recovery == null) {
                                    opsinVersionDetails.setRiscvRecovery(strUnknown);
                                }
                                if (ble == null) {
                                    opsinVersionDetails.setBle("1.0.0");
                                }

                                String wifi = opsinVersionDetails.getWifi();
                                if (wifi != null) {
                                    String[] discardCurrentWifiVersion = wifi.split("-");
                                    String[] currentWifiVersion = discardCurrentWifiVersion[0].split("\\.");
                                    String[] newWifiVersion = opsinWiFiNewFirmwareVersion.split("\\.");
                                    boolean isLessVersion = false;
                                    if (currentWifiVersion.length == newWifiVersion.length) {
                                        for (int i = 0; i < currentWifiVersion.length; i++) {
                                            int currentValue = (int) Double.parseDouble(currentWifiVersion[i]);
                                            int comparableValue = Integer.parseInt(newWifiVersion[i]);
                                            if (currentValue < comparableValue) {
                                                isLessVersion = true;
                                                Log.e(TAG, "CURRENT RTOS IS LESSER Than ComparableValue");
                                                break;
                                            }
                                        }
                                        Constants.setOpsinContainsOldWiFiFirmware(isLessVersion);
                                    } else {
                                        Constants.setOpsinContainsOldWiFiFirmware(false);
                                    }
                                } else {
                                    Constants.setOpsinContainsOldWiFiFirmware(false);
                                }

                                if (commandRequested == COMMAND_REQUESTED.GET_VERSION) {
                                    commandRequested = COMMAND_REQUESTED.NONE;
                                }
                                opsinCameraVersionInfo.postValue(new Event<>(opsinVersionDetails));
                                Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION : SCCP_ACK_SUCCESS: " + updatedComponents + "\n");
                            }
                        }
                        break;
                    case SCCP_ACK_FAILURE:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: SCCP_ACK_FAILURE\n");
                        if (commandRequested == COMMAND_REQUESTED.GET_VERSION) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_VERSION : SCCP_ACK_FAILURE");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        break;
                    case SCCP_ACK_UNSUPPORTED:
                        if (commandRequested == COMMAND_REQUESTED.GET_VERSION) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: SCCP_ACK_UNSUPPORTED\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_VERSION : SCCP_ACK_UNSUPPORTED");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        break;
                    case SCCP_ACK_UNKNOWN:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: SCCP_ACK_UNKNOWN\n");
                        if (commandRequested == COMMAND_REQUESTED.GET_VERSION) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_VERSION : SCCP_ACK_UNKNOWN");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        break;
                    case SCCP_ACK_MAX:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_VERSION: SCCP_ACK_MAX\n");
                        if (commandRequested == COMMAND_REQUESTED.GET_VERSION) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_VERSION : SCCP_ACK_MAX");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        break;
                }
            } else {
                Log.e(TAG, "GET VERSION NULL\n " + flag);
            }

        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_PRODUCT.getValue()) {
            byte[] ackBytes = new byte[4];
            byte[] arrayProductId = new byte[12];
            byte[] arrayCameraName = new byte[32];
            byte[] arraySerialNum = new byte[16];

            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    System.arraycopy(dataArray, 4, arrayProductId, 0, arrayProductId.length);
                    System.arraycopy(dataArray, 16, arrayCameraName, 0, arrayCameraName.length);
                    System.arraycopy(dataArray, 48, arraySerialNum, 0, arraySerialNum.length);

                    String productId = new String(arrayProductId, StandardCharsets.UTF_8);
                    String cameraName = new String(arrayCameraName, StandardCharsets.UTF_8);
                    String serialNum = new String(arraySerialNum, StandardCharsets.UTF_8);

                    ProductInfo productInfo = new ProductInfo();
                    productInfo.setProductId(productId);
                    productInfo.setCameraName(cameraName);
                    productInfo.setSerialNum(serialNum);
                    if (commandRequested == COMMAND_REQUESTED.GET_PRODUCT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinProductInfo.postValue(new Event<>(productInfo));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_PRODUCT: ProductId: " + productId.trim() + "\n CameraName: " + cameraName.trim() + "\n SerialNumber: " + serialNum.trim());
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_PRODUCT: SCCP_ACK_FAILURE\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_PRODUCT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_PRODUCT : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_PRODUCT: SCCP_ACK_UNSUPPORTED\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_PRODUCT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_PRODUCT : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_PRODUCT: SCCP_ACK_UNKNOWN\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_PRODUCT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_PRODUCT : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_PRODUCT: SCCP_ACK_MAX\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_PRODUCT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_PRODUCT : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_SERIAL.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte[] arraySerialNumber = new byte[16];
                    System.arraycopy(dataArray, 4, arraySerialNumber, 0, arraySerialNumber.length);
                    String serialNumber = new String(arraySerialNumber, StandardCharsets.UTF_8);
                    if (commandRequested == COMMAND_REQUESTED.GET_SERIAL) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSerialNumber.postValue(new Event<>(serialNumber));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_SERIAL: ProductId: " + serialNumber);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_SERIAL: SCCP_ACK_FAILURE\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_SERIAL) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_SERIAL : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_SERIAL: SCCP_ACK_UNSUPPORTED\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_SERIAL) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_SERIAL : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_SERIAL: SCCP_ACK_UNKNOWN\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_SERIAL) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_SERIAL : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_SERIAL: SCCP_ACK_MAX\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_SERIAL) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_SERIAL : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_TIMEZONE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte[] timeZoneBytes = new byte[2];
                    System.arraycopy(dataArray, 4, timeZoneBytes, 0, timeZoneBytes.length);
                    short value = ByteBuffer.wrap(timeZoneBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    int sign = (value & 0x8000) >> 15;
                    int hour = (value & 0x7F00) >> 8;
                    int minutes = value & 0xFF;
                    String timeZone;
                    if (sign == 0) {
                        if (minutes == 0)
                            timeZone = "+" + hour + ":" + minutes + "0";
                        else
                            timeZone = "+" + hour + ":" + minutes;
                    } else {
                        if (minutes == 0)
                            timeZone = "-" + hour + ":" + minutes + "0";
                        else
                            timeZone = "-" + hour + ":" + minutes;
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetTimeZone.postValue(new Event<>(timeZone));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEZONE: " + "SIGN " + sign + " " + hour + " " + minutes + " == " + timeZone);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEZONE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEZONE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEZONE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEZONE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEZONE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEZONE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEZONE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEZONE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_TIMEZONE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEZONE: SCCP_ACK_SUCCESS");
                    if (commandRequested == COMMAND_REQUESTED.SET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetTimeZone.postValue(new Event<>(true));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEZONE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEZONE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEZONE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEZONE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEZONE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEZONE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEZONE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEZONE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_TIMEZONE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_DATETIME.getValue()) {
            if (dataArray.length > 0) {
                byte[] ackBytes = new byte[4];
                System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
                byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
                switch (ack) {
                    case SCCP_ACK_SUCCESS:
                        byte[] dateTimeBytes = new byte[4];
                        System.arraycopy(dataArray, 4, dateTimeBytes, 0, dateTimeBytes.length);

                        byte[] hightResolutionClockBytes = new byte[4];
                        System.arraycopy(dataArray, 8, hightResolutionClockBytes, 0, hightResolutionClockBytes.length);

                        int dateTime = ByteBuffer.wrap(dateTimeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                        int hightResolutionClock = ByteBuffer.wrap(hightResolutionClockBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                        DateTime dateTimeObj = new DateTime();
                        dateTimeObj.setDateTime(dateTime);
                        dateTimeObj.setHighResolutionClock(hightResolutionClock);

                        int year = (dateTime >> 26) & 0x3F;
                        int month = (dateTime >> 22) & 0x0F;
                        int day = (dateTime >> 17) & 0x1F;
                        int hour = (dateTime >> 12) & 0x1F;
                        int minute = (dateTime >> 6) & 0x3F;
                        int second = (dateTime) & 0x3F;

                        year = 1980 + year;

                        int padding = (hightResolutionClock >> 27) & 0x1F;
                        int hours = (hightResolutionClock >> 22) & 0x1F;
                        int minutes = (hightResolutionClock >> 16) & 0x3F;
                        int seconds = (hightResolutionClock >> 10) & 0x3F;
                        int milliseconds = (hightResolutionClock) & 0x3FF;

                        String formDateTime = day + "/" + month + "/" + year + " " + hour + ":" + minute + ":" + second;

                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: " + dateTime + " " + hightResolutionClock + " " + formDateTime);
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: " + padding + ":" + hours + ":" + minutes + ":" + seconds + ":" + milliseconds);

                        if (commandRequested == COMMAND_REQUESTED.GET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        opsinGetDateTime.postValue(new Event<>(formDateTime));
                        break;
                    case SCCP_ACK_FAILURE:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: SCCP_ACK_FAILURE\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_DATETIME : SCCP_ACK_FAILURE");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.GET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                    case SCCP_ACK_UNSUPPORTED:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: SCCP_ACK_UNSUPPORTED\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_DATETIME : SCCP_ACK_UNSUPPORTED");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.GET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                    case SCCP_ACK_UNKNOWN:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: SCCP_ACK_UNKNOWN\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_DATETIME : SCCP_ACK_UNKNOWN");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.GET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                    case SCCP_ACK_MAX:
                        Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: SCCP_ACK_MAX\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_GET_DATETIME : SCCP_ACK_MAX");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.GET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                }
            } else {
                Log.e(TAG, "SCCP_CMD_CAMERA_GET_DATETIME: EMPTY RESPONSE\n");
            }

        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_DATETIME.getValue()) {
            if (fwMode == MODE_NONE) {
                byte[] ackBytes = new byte[4];
                System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
                byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
                switch (ack) {
                    case SCCP_ACK_SUCCESS:
                        Log.e(TAG, "SCCP_CMD_CAMERA_SET_DATETIME: SCCP_ACK_SUCCESS");
                        if (commandRequested == COMMAND_REQUESTED.SET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        opsinSetDateTime.postValue(new Event<>(true));
                        break;
                    case SCCP_ACK_FAILURE:
                        Log.e(TAG, "SCCP_CMD_CAMERA_SET_DATETIME: SCCP_ACK_FAILURE\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_SET_DATETIME : SCCP_ACK_FAILURE");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.SET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                    case SCCP_ACK_UNSUPPORTED:
                        Log.e(TAG, "SCCP_CMD_CAMERA_SET_DATETIME: SCCP_ACK_UNSUPPORTED\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_SET_DATETIME : SCCP_ACK_UNSUPPORTED");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.SET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                    case SCCP_ACK_UNKNOWN:
                        Log.e(TAG, "SCCP_CMD_CAMERA_SET_DATETIME: SCCP_ACK_UNKNOWN\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_SET_DATETIME : SCCP_ACK_UNKNOWN");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.SET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                    case SCCP_ACK_MAX:
                        Log.e(TAG, "SCCP_CMD_CAMERA_SET_DATETIME: SCCP_ACK_MAX\n");
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_SET_DATETIME : SCCP_ACK_MAX");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                        if (commandRequested == COMMAND_REQUESTED.SET_DATETIME) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        break;
                }
            } else {
                Log.e(TAG, "SCCP_CMD_CAMERA_SET_DATETIME: fw update inprogress");
            }

        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_IMAGE_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte imageState = dataArray[4];
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_IMAGE_STATE: " + imageState + "\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_IMAGE_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetImageState.postValue(new Event<>(imageState));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_IMAGE_STATE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_IMAGE_STATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_IMAGE_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_IMAGE_STATE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_IMAGE_STATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_IMAGE_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_IMAGE_STATE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_IMAGE_STATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_IMAGE_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_IMAGE_STATE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_IMAGE_STATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_IMAGE_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_CLOCKMODE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetClockMode.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_CLOCKMODE: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_CLOCKMODE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_CLOCKMODE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_CLOCKMODE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_CLOCKMODE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_CLOCKMODE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_CLOCKMODE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_CLOCKMODE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_CLOCKMODE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_METADATA.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetMetadataState.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_METADATA: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_METADATA: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_METADATA : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_METADATA: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_METADATA : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_METADATA: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_METADATA : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_METADATA: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_METADATA : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_METADATA.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetMetadataState.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_METADATA: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_METADATA: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_METADATA : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_METADATA: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_METADATA : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_METADATA: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_METADATA : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_METADATA: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_METADATA : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_METADATA) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_TIMEFORMAT.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.GET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();

                    byte value = dataArray[4];
                    opsinGetTimeFomat.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEFORMAT: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEFORMAT: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEFORMAT : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEFORMAT: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEFORMAT : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEFORMAT: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEFORMAT : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_TIMEFORMAT: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_TIMEFORMAT : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_TIMEFORMAT.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetTimeFomat.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEFORMAT: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEFORMAT: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEFORMAT : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEFORMAT: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEFORMAT : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_TIMEFORMAT: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_TIMEFORMAT : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_METADATA: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_METADATA : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_TIME_FORMAT) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetDigitalZoom.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM: ACK: " + bytesToHex(ackBytes) + " ZOOM_VALUE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_DIGITAL_ZOOM : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetDigitalZoom.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_DIGITAL_ZOOM : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_DIGITAL_ZOOM) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_NUC_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            byte value = dataArray[4];
            byte[] reserved = {dataArray[5], dataArray[6], dataArray[7]};

            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();

            boolean isEnabled = false;
            boolean isOperationallyActive = false;
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if ((value & 0x01) == 1) {
                        isEnabled = true;
                        //NUC Enabled
                    }
                    if ((value & 0x02) == 2) {
                        //NUC Operationally Active
                        isOperationallyActive = true;
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetNUCState.postValue((int) value);
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NUC_STATE: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NUC_STATE: SCCP_ACK_FAILURE\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NUC_STATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinSetNUCState.postValue(new Event<>(new Event<>(error)));
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NUC_STATE: SCCP_ACK_UNSUPPORTED\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NUC_STATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinSetNUCState.postValue(new Event<>(new Event<>(error)));
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NUC_STATE: SCCP_ACK_UNKNOWN\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NUC_STATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinSetNUCState.postValue(new Event<>(new Event<>(error)));
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NUC_STATE: SCCP_ACK_MAX\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NUC_STATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinSetNUCState.postValue(new Event<>(new Event<>(error)));
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_NUC_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetNUCState.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NUC_STATE: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NUC_STATE: SCCP_ACK_FAILURE\n");
                    if (commandRequested == COMMAND_REQUESTED.SET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetNUCState.postValue(new Event<>(false));
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NUC_STATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NUC_STATE: SCCP_ACK_UNSUPPORTED\n");
                    if (commandRequested == COMMAND_REQUESTED.SET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NUC_STATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NUC_STATE: SCCP_ACK_UNKNOWN\n");
                    if (commandRequested == COMMAND_REQUESTED.SET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NUC_STATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NUC_STATE: SCCP_ACK_MAX\n");
                    if (commandRequested == COMMAND_REQUESTED.SET_NUC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NUC_STATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }

                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_MIC_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            byte micState = dataArray[4];

            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.GET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetMICState.postValue(new Event<>(micState));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_MIC_STATE: " + micState);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_MIC_STATE: SCCP_ACK_FAILURE\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_MIC_STATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_MIC_STATE: SCCP_ACK_UNSUPPORTED\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_MIC_STATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_MIC_STATE: SCCP_ACK_UNKNOWN\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_MIC_STATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_MIC_STATE: SCCP_ACK_MAX\n");
                    if (commandRequested == COMMAND_REQUESTED.GET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_MIC_STATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_MIC_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetMICState.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_MIC_STATE: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_MIC_STATE: SCCP_ACK_FAILURE: ");
                    if (commandRequested == COMMAND_REQUESTED.SET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    error.setError("CAMERA_SET_MIC_STATE : SCCP_ACK_FAILURE");
                    if (isEngineeringBuild) {
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinMICCommandError.postValue(new Event<>(error));
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_MIC_STATE: SCCP_ACK_UNSUPPORTED: ");
                    if (commandRequested == COMMAND_REQUESTED.SET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    error.setError("CAMERA_SET_MIC_STATE : SCCP_ACK_UNSUPPORTED");
                    if (isEngineeringBuild) {
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinMICCommandError.postValue(new Event<>(error));
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_MIC_STATE: SCCP_ACK_UNKNOWN: ");
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    error.setError("CAMERA_SET_MIC_STATE : SCCP_ACK_UNKNOWN");
                    if (isEngineeringBuild) {
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinMICCommandError.postValue(new Event<>(error));
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_MIC_STATE: SCCP_ACK_MAX: ");
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_MIC_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    error.setError("CAMERA_SET_MIC_STATE : SCCP_ACK_MAX");
                    if (isEngineeringBuild) {
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    opsinMICCommandError.postValue(new Event<>(error));
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_FRAMERATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte[] fpsArray = new byte[]{dataArray[4], dataArray[5]};
                    short FPS = ByteBuffer.wrap(fpsArray).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    if (commandRequested == COMMAND_REQUESTED.GET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetFrameRate.postValue(new Event<>(FPS));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_FRAMERATE: " + FPS);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_FRAMERATE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_FRAMERATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_FRAMERATE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_FRAMERATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_FRAMERATE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_FRAMERATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_FRAMERATE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_FRAMERATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_FRAMERATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetFrameRate.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_FRAMERATE: ACK: SCCP_ACK_SUCCESS\n");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_FRAMERATE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_FRAMERATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_FRAMERATE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_FRAMERATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_FRAMERATE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_FRAMERATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_FRAMERATE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_FRAMERATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_FRAMERATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte jpegCompression = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetJpegCompression.postValue(new Event<>(jpegCompression));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION: " + jpegCompression);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_JPEG_COMPRESSION : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetJpegCompression.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_JPEG_COMPRESSION : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_JPEG_COMPRESSION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_FACTORY_RESET.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_FACTORY_RESET: SCCP_ACK_SUCCESS\n " + fwMode);
                    if (fwMode != MODE_OPSIN_FACTORY && fwMode == MODE_NONE) {
                        if (commandRequested == COMMAND_REQUESTED.FACTORY_RESET) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        opsinSetFactoryReset.postValue(new Event<>(true));
                    }
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_FACTORY_RESET: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_FACTORY_RESET : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.FACTORY_RESET) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_FACTORY_RESET: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_FACTORY_RESET : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.FACTORY_RESET) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_FACTORY_RESET: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_FACTORY_RESET : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.FACTORY_RESET) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_FACTORY_RESET: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_FACTORY_RESET : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.FACTORY_RESET) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_RESTART.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_RESTART: ACK: SCCP_ACK_SUCCESS\n" + fwMode);
                    if (commandRequested == COMMAND_REQUESTED.CAMERA_RESTART) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (fwMode != MODE_OPSIN_RESTART) {
                        opsinCameraRestart.postValue(new Event<>(true));
                    }

                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_RESTART: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_RESTART : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.CAMERA_RESTART) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_RESTART: SCCP_ACK_UNSUPPORTED\n");
                    if (fwMode == MODE_OPSIN_RESTART) {
                        cancelRestartResetWaitingTimer();
                        receivedOpsinUpgradeComplete();
                    } else {
                        if (isEngineeringBuild) {
                            error.setError("SCCP_CMD_CAMERA_RESTART : SCCP_ACK_UNSUPPORTED");
                            opsinCameraCommandError.postValue(new Event<>(error));
                        }
                    }
                    if (commandRequested == COMMAND_REQUESTED.CAMERA_RESTART) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_RESTART: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_RESTART : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.CAMERA_RESTART) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_RESTART: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_RESTART : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.CAMERA_RESTART) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_NAME.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte[] nameBytes = new byte[dataArray.length - 4];
                    System.arraycopy(dataArray, 4, nameBytes, 0, nameBytes.length);
                    byte[] array = ByteBuffer.wrap(nameBytes).order(ByteOrder.LITTLE_ENDIAN).array();
                    String cameraName = new String(array, StandardCharsets.UTF_8);

                    if (commandRequested == COMMAND_REQUESTED.GET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetCameraName.postValue(new Event<>(cameraName));
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NAME: " + cameraName);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NAME: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NAME : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NAME: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NAME : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NAME: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NAME : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_NAME: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_NAME : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_SET_NAME.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetCameraName.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NAME: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NAME: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NAME : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NAME: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NAME : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NAME: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NAME : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_SET_NAME: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_SET_NAME : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_NAME) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_TAKE_PICTURE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();

            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_TAKE_PICTURE: ACK: " + ack);
                    if (commandRequested == COMMAND_REQUESTED.TAKE_PICTURE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinCameraImageCaptureSDCard.postValue(new Event<>(true));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_TAKE_PICTURE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_TAKE_PICTURE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    } else {
                        error.setError("SCCP_CMD_CAMERA_TAKE_PICTURE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.TAKE_PICTURE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_TAKE_PICTURE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_TAKE_PICTURE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.TAKE_PICTURE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_TAKE_PICTURE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_TAKE_PICTURE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.TAKE_PICTURE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_TAKE_PICTURE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.TAKE_PICTURE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_START_RECORDING.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();

            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_RECORDING: ACK: " + ack);
                    if (commandRequested == COMMAND_REQUESTED.START_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinCameraStartRecord.postValue(new Event<>(true));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_RECORDING: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    } else {
                        error.setError("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_RECORDING: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_RECORDING: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_RECORDING: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_STOP_RECORDING.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_RECORDING: ACK: " + ack);
                    if (commandRequested == COMMAND_REQUESTED.STOP_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinCameraStopRecord.postValue(true);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_RECORDING: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_RECORDING : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_RECORDING: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_RECORDING : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_RECORDING: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_RECORDING : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_RECORDING: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_RECORDING : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_RECORDING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_START_STREAMING.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_STREAMING: ACK: " + ack);
                    liveViewErrorMessage.postValue(null);
                    shouldReceiveUDP = true;
                    isOpsinLiveStreamingStarted = true;
                    if (mediaDecoder == null) {
                        initializeObjects();
                        mService.startListenOpsinUdp(mediaDecoder);
                        if (commandRequested == COMMAND_REQUESTED.START_STREAMING) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        opsinCameraStartStreaming.postValue(new Event<>(true));
                    } else {
                        mService.startListenOpsinUdp(mediaDecoder);
                        if (commandRequested == COMMAND_REQUESTED.START_STREAMING) {
                            commandRequested = COMMAND_REQUESTED.NONE;
                        }
                        opsinCameraStartStreaming.postValue(new Event<>(true));
                    }

                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_STREAMING: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_STREAMING : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_STREAMING: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_STREAMING : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_STREAMING: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_STREAMING : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_START_STREAMING: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_START_STREAMING : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.START_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_STOP_STREAMING.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_STREAMING: ACK: " + ack);
                    isOpsinLiveStreamingStarted = false;
                    try {
                        mediaDecoder.stopDecoder();
                        if (mediaEncoder.isRecording()) {
                            mediaEncoder.stopRecording();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinCameraStopStreaming.postValue(new Event<>(true));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_STREAMING: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_STREAMING : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_STREAMING: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_STREAMING : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_STREAMING: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_STREAMING : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_STOP_STREAMING: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_STOP_STREAMING : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.STOP_STREAMING) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_RECORDING_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_RECORDING_STATE: ACK: " + ack);
                    byte recordingState = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_RECORDING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (recordingState == SCCPConstants.SCCP_RECORDING_STATE.NOT_RECORDING.getValue()) {
                        opsinCameraRecordingState.postValue(new Event<>(false));
                    } else {
                        opsinCameraRecordingState.postValue(new Event<>(true));
                    }
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_RECORDING_STATE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_RECORDING_STATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_RECORDING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_RECORDING_STATE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_RECORDING_STATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_RECORDING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_RECORDING_STATE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_RECORDING_STATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_RECORDING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_RECORDING_STATE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_RECORDING_STATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_RECORDING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_STREAMING_STATE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_STREAMING_STATE: ACK: " + ack);
                    byte streamingState = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_STREAMING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (streamingState == SCCPConstants.SCCP_STREAMING_STATE.NOT_STREAMING.getValue()) {
                        opsinCameraStreamingState.postValue(new Event<>(false));
                    } else {
                        opsinCameraStreamingState.postValue(new Event<>(true));
                    }
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_STREAMING_STATE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_STREAMING_STATE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_STREAMING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_STREAMING_STATE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_STREAMING_STATE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_STREAMING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_STREAMING_STATE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_STREAMING_STATE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_STREAMING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_STREAMING_STATE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_STREAMING_STATE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_STREAMING_STATE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_PRODUCT_SPECIFIC.SCCP_CMD_CAMERA_GET_CLOCKMODE.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_CLOCKMODE: ACK: " + ack);
                    byte streamingState = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    if (streamingState == SCCPConstants.SCCP_CLOCK_MODE.SYSTEM.getValue()) {
                        opsinCameraClockMode.postValue(new Event<>(true));
                    } else {
                        opsinCameraClockMode.postValue(new Event<>(false));
                    }
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_CLOCKMODE: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_CLOCKMODE : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_CLOCKMODE: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_CLOCKMODE : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_CLOCKMODE: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_CLOCKMODE : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_CAMERA_GET_CLOCKMODE: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_CAMERA_GET_CLOCKMODE : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_CLOCKMODE) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processPowerManagementSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_POWER_MANAGEMENT.SCCP_CMD_POWER_GET_BATTERY.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte id = dataArray[4];
                    byte state = dataArray[5];

                    byte[] adcBytes = new byte[2];
                    System.arraycopy(dataArray, 6, adcBytes, 0, adcBytes.length);
                    short adc = ByteBuffer.wrap(adcBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    byte[] voltageBytes = new byte[2];
                    System.arraycopy(dataArray, 8, voltageBytes, 0, voltageBytes.length);
                    short voltage = ByteBuffer.wrap(voltageBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    byte[] temperatureBytes = new byte[2];
                    System.arraycopy(dataArray, 8, temperatureBytes, 0, temperatureBytes.length);
                    short temperature = ByteBuffer.wrap(temperatureBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    BatteryInfo batteryInfo = new BatteryInfo();
                    batteryInfo.setId(id);
                    batteryInfo.setState(state);
                    batteryInfo.setAdc(adc);
                    batteryInfo.setVoltage(voltage);
                    batteryInfo.setTemperature(temperature);

                    if (commandRequested == COMMAND_REQUESTED.POWER_GET_BATTERY) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetBatteryInfo.postValue(new Event<>(batteryInfo));
                    Log.e(TAG, "SCCP_CMD_POWER_GET_BATTERY: ACK: " + ack + " " + state);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_POWER_GET_BATTERY: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_POWER_GET_BATTERY : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.POWER_GET_BATTERY) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_POWER_GET_BATTERY: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_POWER_GET_BATTERY : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.POWER_GET_BATTERY) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_POWER_GET_BATTERY: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_POWER_GET_BATTERY : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.POWER_GET_BATTERY) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_POWER_GET_BATTERY: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_POWER_GET_BATTERY : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.POWER_GET_BATTERY) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processDisplaySpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_DISPLAY_SPECIFIC.SCCP_CMD_DISPLAY_GET_BRIGHTNESS.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_BRIGHTNESS) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetBrightness.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_DISPLAY_GET_BRIGHTNESS: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_DISPLAY_GET_BRIGHTNESS: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_DISPLAY_GET_BRIGHTNESS : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_BRIGHTNESS) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_DISPLAY_GET_BRIGHTNESS: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_DISPLAY_GET_BRIGHTNESS : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_BRIGHTNESS) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_DISPLAY_GET_BRIGHTNESS: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_DISPLAY_GET_BRIGHTNESS : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_BRIGHTNESS) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_DISPLAY_GET_BRIGHTNESS: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_DISPLAY_GET_BRIGHTNESS : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_BRIGHTNESS) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processISPSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_EV.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetEv.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_EV.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetEv.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_EV: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_EV : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.SET_EV) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_MONOCHROMATIC.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetMonochromeState.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_ISP_GET_MONOCHROMATIC: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_MONOCHROMATIC: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_MONOCHROMATIC : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_MONOCHROMATIC: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_MONOCHROMATIC : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_MONOCHROMATIC: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_MONOCHROMATIC : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_MONOCHROMATIC: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_MONOCHROMATIC : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_MONOCHROMATIC.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetMonochromeState.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_ISP_SET_MONOCHROMATIC: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_MONOCHROMATIC: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_MONOCHROMATIC : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_MONOCHROMATIC: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_MONOCHROMATIC : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_MONOCHROMATIC: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_MONOCHROMATIC : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_MONOCHROMATIC: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_MONOCHROMATIC : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_MONOCHROMATIC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_NR.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetNoiseState.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_ISP_GET_NR: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_NR: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_NR : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_NR: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_NR : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_NR: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_NR : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_NR: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_NR : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_NR.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetNoiseState.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_ISP_SET_NR: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_NR: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_NR : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_NR: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_NR : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_NR: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_NR : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_NR: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_NR : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_NR) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_GET_ROI.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte value = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetROIState.postValue(new Event<>(value));
                    Log.e(TAG, "SCCP_CMD_ISP_GET_ROI: ACK: " + bytesToHex(ackBytes) + " STATE: " + value);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_ROI: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_ROI : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_ROI: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_ROI : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_ROI: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_ROI : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_GET_ROI: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_GET_ROI : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_ISP_SPECIFIC.SCCP_CMD_ISP_SET_ROI.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.SET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyApplySetting();
                    opsinSetROIState.postValue(new Event<>(true));
                    Log.e(TAG, "SCCP_CMD_ISP_SET_ROI: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_ROI: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_ROI : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_ROI: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_ROI : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_ROI: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_ROI : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_ISP_SET_ROI: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_ISP_SET_ROI : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.SET_ROI) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processSDCardSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_SDCARD_SPECIFIC.SCCP_CMD_SDCARD_GET_INFO.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte isPresent = dataArray[4];
                    byte isMounted = dataArray[5];
                    byte isFormatting = dataArray[6];
                    byte isBusy = dataArray[7];

                    byte[] arrTotalSize = new byte[4];
                    System.arraycopy(dataArray, 8, arrTotalSize, 0, arrTotalSize.length);
                    int totalSize = ByteBuffer.wrap(arrTotalSize).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] arrAvailableSize = new byte[4];
                    System.arraycopy(dataArray, 12, arrAvailableSize, 0, arrAvailableSize.length);
                    int availableSize = ByteBuffer.wrap(arrAvailableSize).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] arrUsed = new byte[4];
                    System.arraycopy(dataArray, 16, arrUsed, 0, arrUsed.length);
                    int usedSize = ByteBuffer.wrap(arrUsed).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] arrFileCount = new byte[4];
                    System.arraycopy(dataArray, 20, arrFileCount, 0, arrFileCount.length);
                    int fileCount = ByteBuffer.wrap(arrFileCount).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] arrSerialNum = new byte[4];
                    System.arraycopy(dataArray, 24, arrSerialNum, 0, arrSerialNum.length);
                    int serialNumber = ByteBuffer.wrap(arrSerialNum).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] arrLabel = new byte[16];
                    System.arraycopy(dataArray, 28, arrLabel, 0, arrLabel.length);
                    byte[] array = ByteBuffer.wrap(arrLabel).order(ByteOrder.LITTLE_ENDIAN).array();
                    String label = new String(array, StandardCharsets.UTF_8);


                    SDCardInfo sdCardInfo = new SDCardInfo();
                    sdCardInfo.setPresent(isPresent != 0x00);
                    sdCardInfo.setMounted(isMounted != 0x00);
                    sdCardInfo.setFormatting(isFormatting != 0x00);
                    sdCardInfo.setBusy(isBusy != 0x00);
                    sdCardInfo.setTotalSize(totalSize);
                    sdCardInfo.setAvailableSize(availableSize);
                    sdCardInfo.setUsedSize(usedSize);
                    sdCardInfo.setFileCount(fileCount);
                    sdCardInfo.setSerialNumber(serialNumber);
                    sdCardInfo.setLabel(label);

                    if (commandRequested == COMMAND_REQUESTED.SDCARD_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetSdCardState.postValue(new Event<>(sdCardInfo));
                    Log.e(TAG, "SCCP_CMD_SDCARD_GET_INFO: ACK: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_SDCARD_GET_INFO: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_SDCARD_GET_INFO : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.SDCARD_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_SDCARD_GET_INFO: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_SDCARD_GET_INFO : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.SDCARD_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_SDCARD_GET_INFO: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_SDCARD_GET_INFO : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.SDCARD_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_SDCARD_GET_INFO: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_SDCARD_GET_INFO : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.SDCARD_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processWirelessSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_WIRELESS_SPECIFIC.SCCP_CMD_WIFI_GET_PASSWORD.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte[] passwordBytes = new byte[64];
                    System.arraycopy(dataArray, 4, passwordBytes, 0, passwordBytes.length);
                    String password = new String(passwordBytes, StandardCharsets.UTF_8);
                    if (commandRequested == COMMAND_REQUESTED.GET_PASSWORD) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinWirelessPassword.postValue(new Event<>(password));
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_PASSWORD: ACK: " + ack + " " + password.trim());
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_PASSWORD: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_PASSWORD : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_PASSWORD) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_PASSWORD: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_PASSWORD : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_PASSWORD) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_PASSWORD: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_PASSWORD : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_PASSWORD) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_PASSWORD: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_PASSWORD : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_PASSWORD) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_WIRELESS_SPECIFIC.SCCP_CMD_WIFI_GET_MAC.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();

            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte[] macBytes = new byte[8];
                    System.arraycopy(dataArray, 4, macBytes, 0, macBytes.length);
                    String mac = new String(macBytes, StandardCharsets.UTF_8);
                    if (commandRequested == COMMAND_REQUESTED.GET_MAC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinWirelessMac.postValue(new Event<>(mac));
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_MAC: ACK: " + ack + " " + mac);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_MAC: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_MAC : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_MAC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_MAC: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_MAC : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_MAC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_MAC: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_MAC : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_MAC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_WIFI_GET_MAC: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_WIFI_GET_MAC : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GET_MAC) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processGpsSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_GET_INFO.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte gpsPowerState = dataArray[4];
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinGetGpsInfo.postValue(new Event<>(gpsPowerState));
                    Log.e(TAG, "SCCP_CMD_GPS_GET_INFO: " + gpsPowerState);
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_INFO: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_INFO : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_INFO: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_INFO : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_INFO: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_INFO : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_INFO: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_INFO : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_INFO) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_GET_POSITION.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    byte gpsPowerState = dataArray[4];
                    byte valid = dataArray[5];
                    char n_s = (char) (dataArray[16] & 0xFF);
                    char e_w = (char) (dataArray[17] & 0xFF);
                    byte nbSatellite = dataArray[18];

                    byte[] latitudeBytes = new byte[4];
                    System.arraycopy(dataArray, 8, latitudeBytes, 0, latitudeBytes.length);
                    int anInt = ByteBuffer.wrap(latitudeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    // Extract degrees, minutes, and decimal minutes from the integer value
                    int degrees = (anInt >> 24) & 0xFF;    // Shift right 24 bits to get degrees (8 bits)
                    int minutes = (anInt >> 16) & 0xFF;    // Shift right 16 bits to get minutes (8 bits)
                    int decimalMinutes = anInt & 0xFFFF;   // Mask to get decimal minutes (16 bits)
                    // Calculate decimal minutes as a floating-point value (decimal section / 10000)
                    double minutesWithDecimal = decimalMinutes / 10000.0;
                    double latitudeVal = degrees + (minutes + minutesWithDecimal) / 60;
                    String nsValue = String.valueOf(n_s);
                    if (nsValue.equalsIgnoreCase("S")) {
                        latitudeVal = -latitudeVal;
                    }
                    String degreeFormatLatitude = String.format("%d° %02d′ %.4f″", degrees, minutes, minutesWithDecimal);


                    byte[] longitudeBytes = new byte[4];
                    System.arraycopy(dataArray, 12, longitudeBytes, 0, longitudeBytes.length);
                    int longitudeInt = ByteBuffer.wrap(longitudeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    // Extract degrees, minutes, and decimal minutes from the integer value
                    int degreesLan = (longitudeInt >> 24) & 0xFF;    // Shift right 24 bits to get degrees (8 bits)
                    int minutesLan = (longitudeInt >> 16) & 0xFF;    // Shift right 16 bits to get minutes (8 bits)
                    int decimalMinutesLan = longitudeInt & 0xFFFF;   // Mask to get decimal minutes (16 bits)
                    // Calculate decimal minutes as a floating-point value (decimal section / 10000)
                    double minutesWithDecimalLan = decimalMinutesLan / 10000.0;
                    double longitudeVal = degreesLan + (minutesLan + minutesWithDecimalLan) / 60;
                    String ewValue = String.valueOf(e_w);
                    if (ewValue.equalsIgnoreCase("W")) {
                        longitudeVal = -longitudeVal;
                    }
                    String degreeFormatLongitude = String.format("%d° %02d′ %.4f″", degreesLan, minutesLan, minutesWithDecimalLan);


                    byte[] mslAltitudeBytes = new byte[4];
                    System.arraycopy(dataArray, 20, mslAltitudeBytes, 0, mslAltitudeBytes.length);
                    int mslAltitudeInt = ByteBuffer.wrap(mslAltitudeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();


                    byte[] timeBytes = new byte[4];
                    System.arraycopy(dataArray, 24, timeBytes, 0, timeBytes.length);
                    int utcTimeInt = ByteBuffer.wrap(timeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    int hours = (utcTimeInt >> 24) & 0xFF;   // Shift right 24 bits to get hours (8 bits)
                    int minutess = (utcTimeInt >> 16) & 0xFF; // Shift right 16 bits to get minutes (8 bits)
                    int seconds = (utcTimeInt >> 8) & 0xFF;  // Shift right 8 bits to get seconds (8 bits)
                    int hundredths = utcTimeInt & 0xFF;      // Mask to get 1/100th seconds (8 bits)
                    String utcTime = String.format("%02d:%02d:%02d.%02d", hours, minutess, seconds, hundredths);


                    byte[] dateBytes = new byte[4];
                    System.arraycopy(dataArray, 28, dateBytes, 0, dateBytes.length);
                    int utcDateInt = ByteBuffer.wrap(dateBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    int year = (utcDateInt >> 16) & 0xFFFF; // Shift right 16 bits to get year (16 bits)
                    int month = (utcDateInt >> 8) & 0xFF;   // Shift right 8 bits to get month (8 bits)
                    int day = utcDateInt & 0xFF;           // Mask to get day (8 bits)
                    String utcDate = String.format("%04d-%02d-%02d", year, month, day);

                    GpsPosition gpsPosition = new GpsPosition();
                    gpsPosition.setPowerStateOn(gpsPowerState != 0);
                    gpsPosition.setValid(valid != 0);
                    gpsPosition.setLatitude(latitudeVal);
                    gpsPosition.setLongitude(longitudeVal);
                    gpsPosition.setNs(n_s);
                    gpsPosition.setEw(e_w);
                    gpsPosition.setSatelliteCount(nbSatellite);
                    gpsPosition.setMslAltitude(mslAltitudeInt);
                    gpsPosition.setTime(utcTime);
                    gpsPosition.setDate(utcDate);

                    Log.e(TAG, "SCCP_CMD_GPS_GET_POSITION:" + "\n"
                            + " POWER_STATE: " + gpsPowerState + "\n"
                            + " VALID: " + valid + "\n"
                            + " LATITUDE: " + latitudeVal + " " + degreeFormatLatitude + "\n"
                            + " LONGITUDE: " + longitudeVal + " " + degreeFormatLongitude + "\n"
                            + " N_S: " + n_s + "\n"
                            + " E_W: " + e_w + "\n"
                            + " NB_SATELLITE: " + nbSatellite + "\n"
                            + " MSL_ALTITUDE: " + mslAltitudeInt + "\n"
                            + " TIME: " + utcTime + "\n"
                            + " DATE: " + utcDate + "\n"
                    );
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_POSITION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    if (valid != 0 && gpsPowerState != 0) {
                        opsinGetGpsLocation.postValue(new Event<>(gpsPosition));
                    }
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_POSITION: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_POSITION : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_POSITION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_POSITION: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_POSITION : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_POSITION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_POSITION: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_POSITION : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_POSITION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_GPS_GET_POSITION: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_GET_POSITION : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.GPS_GET_POSITION) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        } else if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_GPS_SPECIFIC.SCCP_CMD_GPS_SET_POWER.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    if (commandRequested == COMMAND_REQUESTED.GPS_SET_POWER) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    opsinSetGpsPower.postValue(new Event<>(true));
                    notifyApplySetting();
                    Log.e(TAG, "SCCP_CMD_GPS_SET_POWER: SCCP_ACK_SUCCESS");
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_GPS_SET_POWER: SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_SET_POWER : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GPS_SET_POWER) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_GPS_SET_POWER: SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_SET_POWER : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GPS_SET_POWER) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_GPS_SET_POWER: SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_SET_POWER : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GPS_SET_POWER) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_CMD_GPS_SET_POWER: SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_GPS_SET_POWER : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyApplySetting();
                    if (commandRequested == COMMAND_REQUESTED.GPS_SET_POWER) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    private void processCompassSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_COMPASS_SPECIFIC.SCCP_CMD_COMPASS_GET_ANGLES.getValue()) {
            byte[] ackBytes = new byte[4];
            System.arraycopy(dataArray, 0, ackBytes, 0, ackBytes.length);
            byte ack = ByteBuffer.wrap(ackBytes).order(ByteOrder.LITTLE_ENDIAN).get();
            switch (ack) {
                case SCCP_ACK_SUCCESS:
                    Log.e(TAG, "SCCP_CMD_COMPASS_GET_ANGLES : ACK: SCCP_ACK_SUCCESS\n");

                    byte calibrated = dataArray[4];// 1 --> Calibrated; 0--> not calibrated
                    byte calibratedLevel = dataArray[5];//Percentage of calibration level

                    byte[] arrYaw = new byte[2];
                    System.arraycopy(dataArray, 6, arrYaw, 0, arrYaw.length);
                    short yaw = ByteBuffer.wrap(arrYaw).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    byte[] arrRoll = new byte[2];
                    System.arraycopy(dataArray, 8, arrRoll, 0, arrRoll.length);
                    short roll = ByteBuffer.wrap(arrRoll).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    byte[] arrPitch = new byte[2];
                    System.arraycopy(dataArray, 10, arrPitch, 0, arrPitch.length);
                    short pitch = ByteBuffer.wrap(arrPitch).order(ByteOrder.LITTLE_ENDIAN).getShort();

                    CompassAngles compassAngles = new CompassAngles();
                    compassAngles.setCalibrated(calibrated != 0x00);
                    compassAngles.setCalibratedLevel(calibratedLevel);
                    compassAngles.setYaw(yaw);
                    compassAngles.setRoll(roll);
                    compassAngles.setPitch(pitch);
                    Log.e(TAG, "SCCP_CMD_COMPASS_GET_ANGLES : " + calibratedLevel + " " + calibrated);

                    if (commandRequested == COMMAND_REQUESTED.COMPASS_GET_ANGLES) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    notifyPeriodicRequest();
                    opsinGetCompassState.postValue(new Event<>(compassAngles));
                    break;
                case SCCP_ACK_FAILURE:
                    Log.e(TAG, "SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_FAILURE\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_FAILURE");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.COMPASS_GET_ANGLES) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNSUPPORTED:
                    Log.e(TAG, "SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_UNSUPPORTED\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_UNSUPPORTED");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.COMPASS_GET_ANGLES) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_UNKNOWN:
                    Log.e(TAG, "SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_UNKNOWN\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_UNKNOWN");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.COMPASS_GET_ANGLES) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
                case SCCP_ACK_MAX:
                    Log.e(TAG, "SCCP_ACK_MAX\n");
                    if (isEngineeringBuild) {
                        error.setError("SCCP_CMD_COMPASS_GET_ANGLES : SCCP_ACK_MAX");
                        opsinCameraCommandError.postValue(new Event<>(error));
                    }
                    notifyPeriodicRequest();
                    if (commandRequested == COMMAND_REQUESTED.COMPASS_GET_ANGLES) {
                        commandRequested = COMMAND_REQUESTED.NONE;
                    }
                    break;
            }
        }
    }

    @Override
    public void triggerStopOpsinLiveStreaming() {
        isOpsinLiveStreamingStarted = true;
        stopOpsinLiveStreaming();
    }

    @Override
    public void triggerStartOpsinLiveStreaming() {
        Log.e(TAG, "triggerStartOpsinLiveStreaming:");
        isOpsinLiveStreamingStarted = false;
        startOpsinLiveStreaming();
    }

    private void processManufacturingSpecificResponse(int command, byte[] dataArray) {
        if (command == SCCPConstants.SCCP_MSG_TYPE_OPSIN_MANUFACTURING_SPECIFIC.SCCP_CMD_MANUFACTURING_LAST.getValue()) {
            Log.e(TAG, "SCCP_CMD_MANUFACTURING_LAST: " + Arrays.toString(dataArray));
        } else {
            Log.e(TAG, "updateOpsinResponse: else");
        }
    }

    private void notifyApplySetting() {
        if (applyPreset == CameraPresetsViewModel.ApplyPreset.PRESET_COMMAND_PROCEEDED) {
            applyPreset = CameraPresetsViewModel.ApplyPreset.PRESET_PROCEED_NEXT_COMMAND;
            Log.e(TAG, "applyPreset: PRESET_PROCEED_NEXT_COMMAND" + applyPreset.name());
            synchronized (lockApplySettings) {
                lockApplySettings.notifyAll();
                responseReceived = true;
            }
        }
    }

    private void notifyPeriodicRequest() {
        if (applyOpsinPeriodicRequest == TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_COMMAND_PROCEEDED) {
            applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.OPSIN_PERIODIC_PROCEED_NEXT_COMMAND;
        }
    }

    public void handleStopOpsinStreaming() {
        if (mediaEncoder.isRecording()) {
            mediaEncoder.stopRecording();
        }
        mediaDecoder.stopDecoder();
        stopUdpListeners();
    }

    private void sendSameDataBlock() {
        Log.e(TAG, "sendSameDataBlock: called");
        if (opsinCommandRetryCount < 3) {
            if (bound) {
                mService.sendLastOpsinDataBlock();
                opsinCommandRetryCount++;
            }
        } else {
            sendOpsinUpgradeCancel();
        }
    }

    @Override
    public void sendOpsinUpgradeCancel() {
        opsinUpgradeCancel();
    }

    @Override
    public void sendOpsinUpgradeFailed() {
        sendUpgradeFailed();
    }

    private void sendUpgradeFailed() {
        try {
            cancelWaitModeTimer();
            stopWaitingTimerForUpgradeProgressUpgradeComplete();
            hideSpinnerDialog();
            cancelRestartResetWaitingTimer();
            if (scheduledTimerToMonitorUpgradeCompleteAfter100 != null && !scheduledTimerToMonitorUpgradeCompleteAfter100.isShutdown()) {
                scheduledTimerToMonitorUpgradeCompleteAfter100.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CommandError error = new CommandError();
        error.setError("Failed");
        fwUpdateFailed.postValue(new Event<>(error));
    }

    private void monitorUpgradeCompleteAfter100Percent() {
        scheduledTimerToMonitorUpgradeCompleteAfter100 = Executors.newScheduledThreadPool(1);
        scheduledTimerToMonitorUpgradeCompleteAfter100.scheduleWithFixedDelay(() -> {
            Log.e(TAG, "monitorUpgradeCompleteMessageCount: " + monitorUpgradeCompleteMessageCount);
            if (monitorUpgradeCompleteMessageCount == 10) {
                opsinUpgradeCompleteReceived = false;
                hideSpinnerDialog();
                stopWaitingTimerForUpgradeProgressUpgradeComplete();
                receivedOpsinUpgradeComplete();
            } else if (!opsinUpgradeCompleteReceived) {
                monitorUpgradeCompleteMessageCount = monitorUpgradeCompleteMessageCount + 1;
            } else {
                opsinUpgradeCompleteReceived = false;
                scheduledTimerToMonitorUpgradeCompleteAfter100.shutdownNow();
                scheduledTimerToMonitorUpgradeCompleteAfter100 = null;
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void startWaitingTimerForUpgradeProgressUpgradeComplete() {
        Log.e(TAG, "startWaitingTimerForUpgradeProgressUpgradeComplete: UPGRADE_DATA_COMPLETE_WAIT TIMER START");
        showSpinnerDialog();
        AtomicInteger count = new AtomicInteger();
        if (scheduledWaitingTimerForUpgradeProgressUpgradeComplete != null && !scheduledWaitingTimerForUpgradeProgressUpgradeComplete.isShutdown()) {
            scheduledWaitingTimerForUpgradeProgressUpgradeComplete.shutdownNow();
            scheduledWaitingTimerForUpgradeProgressUpgradeComplete = null;
        }
        scheduledWaitingTimerForUpgradeProgressUpgradeComplete = Executors.newScheduledThreadPool(1);
        scheduledWaitingTimerForUpgradeProgressUpgradeComplete.scheduleWithFixedDelay(() -> {
            double currentTimeStamp = System.currentTimeMillis();
            int numOfCount = count.get();
            int percentage = (int) ((numOfCount * 100) / (waitingTimeForUpgradeCompleteProgress * 60));
            String message = "UPGRADE_DATA_COMPLETE_WAIT_PERCENTAGE " + percentage + "%";
            opsinObserverUpgradeCompleteWaitMsg.postValue(new Event<>(message));
            Log.e(TAG, "startWaitingTimerForUpgradeProgressUpgradeComplete: UPGRADE_DATA_COMPLETE_WAIT " + percentage);

            if (numOfCount >= waitingTimeForUpgradeCompleteProgress * 60) {
                Log.e(TAG, "startWaitingTimerForUpgradeProgressUpgradeComplete: UPGRADE_DATA_COMPLETE_WAIT TIMER END");
                hideSpinnerDialog();
                receivedOpsinUpgradeComplete();
                stopWaitingTimerForUpgradeProgressUpgradeComplete();
            } else if (currentTimeStamp - previousTimeStamp >= 10 * 1000) {//Wait 10 secs for progress update, if not received show spinner wheel
                Log.e(TAG, "startWaitingTimerForUpgradeProgressUpgradeComplete: UPGRADE_DATA_COMPLETE_WAIT- Progress Not Received more than 10 seconds ");
                showSpinnerDialog();
            }
            count.getAndIncrement();
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void startRestartResetWaitingTimer() {
        timerRestartResetWaitingTimer = new Timer();
        timerRestartResetWaitingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (restartCountDown < restartWaitingTime) {
                    int percentage = (int) ((restartCountDown * 100) / restartWaitingTime);
                    fwUpdateProgress.postValue(percentage);
                    restartCountDown = restartCountDown + 1;
                } else {
                    Log.e(TAG, "MODE: RESTART/RESET");
                    receivedOpsinUpgradeComplete();
                    restartCountDown = 0;
                    timerRestartResetWaitingTimer.cancel();
                }
            }
        }, 0, 1000);
    }

    public void stopWaitingTimerForUpgradeProgressUpgradeComplete() {
        if (scheduledWaitingTimerForUpgradeProgressUpgradeComplete != null && !scheduledWaitingTimerForUpgradeProgressUpgradeComplete.isShutdown()) {
            Log.e(TAG, "stopWaitingTimer: UPGRADE_DATA_COMPLETE_WAIT TIMER SHUTDOWN");
            scheduledWaitingTimerForUpgradeProgressUpgradeComplete.shutdownNow();
            scheduledWaitingTimerForUpgradeProgressUpgradeComplete = null;
        }
    }

    public void cancelWaitModeTimer() {
        try {
            if (scheduledWaitMode != null && !scheduledWaitMode.isShutdown()) {
                scheduledWaitMode.shutdownNow();
                scheduledWaitMode = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelRestartResetWaitingTimer() {
        try {
            if (timerRestartResetWaitingTimer != null) {
                timerRestartResetWaitingTimer.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSpinnerDialog() {
        if (!isDisplayingWaitingTime) {
            isDisplayingWaitingTime = true;
            String message1 = "UPGRADE_DATA_COMPLETE_WAIT";
            opsinObserverUpgradeComplete.postValue(new Event<>(message1));
        }
    }

    private void hideSpinnerDialog() {
        if (isDisplayingWaitingTime) {
            Log.e(TAG, "hideSpinnerDialog: ");
            isDisplayingWaitingTime = false;
            String message = "UPGRADE_DATA_COMPLETE_WAIT_CANCEL";
            opsinObserverUpgradeComplete.postValue(new Event<>(message));
        }
    }

    private void receivedOpsinUpgradeComplete() {
        Message message = mHandler.obtainMessage(1);
        message.sendToTarget();
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                if (scheduledTimerToMonitorUpgradeCompleteAfter100 != null) {
                    scheduledTimerToMonitorUpgradeCompleteAfter100.shutdownNow();
                    scheduledTimerToMonitorUpgradeCompleteAfter100 = null;
                }
                previousTimeStamp = 0;
                monitorUpgradeCompleteMessageCount = 0;
                fwUpdateProgress.postValue(100);

                switch (fwMode) {
                    case MODE_WIFI_DIALOG:
//                        Log.e(TAG, "receivedOpsinUpgradeComplete: UPGRADE_COMPLETE_WIFI");
//                        sfwState = "UPGRADE_COMPLETE_WIFI";
//                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
//                        new Handler().postDelayed(() -> {
//                            waitingTimeForUpgradeCompleteProgress = 3;
//                            previousTimeStamp = System.currentTimeMillis();
//                            startWaitingTimerForUpgradeProgressUpgradeComplete();
//                        }, 1000);

                        String sfwState = "WIFI:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        break;
                    case MODE_OPSIN_BLE:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: BLE:UPGRADE_COMPLETE_OTHER");
                        sfwState = "BLE:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_RISCV:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: RISCV:UPGRADE_COMPLETE_OTHER");
                        sfwState = "RISCV:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_FPGA:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: FPGA:UPGRADE_COMPLETE_OTHER");
                        sfwState = "FPGA:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_OPSIN_RECOVERY:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: RECOVERY:UPGRADE_COMPLETE_OTHER");
                        sfwState = "RECOVERY:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_OPSIN_FULL:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: FULL:UPGRADE_COMPLETE_OTHER");
                        sfwState = "FULL:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_OPSIN_RISCV_FPGA:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: RISCV_FPGA:UPGRADE_COMPLETE_OTHER");
                        sfwState = "RISCV_FPGA:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_OPSIN_RISCV_OVERLAY:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: RISCV_OVERLAY:UPGRADE_COMPLETE_OTHER");
                        sfwState = "RISCV_OVERLAY:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_OPSIN_RESTART:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: RESTART:UPGRADE_COMPLETE_OTHER");
                        sfwState = "RESTART:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_OPSIN_FACTORY:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: FACTORY:UPGRADE_COMPLETE_OTHER");
                        sfwState = "FACTORY:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                    case MODE_WAIT:
                        Log.e(TAG, "receivedOpsinUpgradeComplete: WAIT:UPGRADE_COMPLETE_OTHER");
                        sfwState = "WAIT:UPGRADE_COMPLETE_OTHER";
                        opsinObserverUpgradeComplete.postValue(new Event<>(sfwState));
                        fwMode = MODE_NONE;
                        break;
                }
                fwUpdateProgress.postValue(-1);
                isRiscvUpdateCompleteSent = false;
                resetToDefault();
            }
        }
    };

    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }

    /*Opsin*/
    private boolean checkCRCValues(byte[] SOF, byte[] headerBytes, byte flag, byte seq,
                                   byte[] commandBytes, byte[] crc16Bytes, byte[] dataArray, byte[] crc32Bytes, byte eof) {
        int HEADER_SIZE = 10;
        int CRC16_BUFFER_SIZE = 2;
        int CRC32_BUFFER_SIZE = 4;
        int EOF_SIZE = 1;
        ByteBuffer bbRequest = ByteBuffer.allocate(HEADER_SIZE);
        bbRequest.put(SOF);//sof+magic
        bbRequest.put(headerBytes);//data length, Protocol header version
        bbRequest.put(flag);//flags 0x80 -->0, 0x40-->1,0x20-->2
        bbRequest.put(seq);//sequence_num
        bbRequest.put(commandBytes);//command

        //CRC 16
        ByteBuffer bbCRC16 = ByteBuffer.allocate(HEADER_SIZE + CRC16_BUFFER_SIZE + dataArray.length);
        byte[] crcArr16 = bbRequest.array();
        short crc16 = calculateCRC16FromJNI(crcArr16, crcArr16.length);
        bbCRC16.put(bbRequest.array());
        bbCRC16.put(shortToByteArray(crc16));//CRC16
        bbCRC16.put(dataArray);//CRC16

        //CRC 32
        ByteBuffer bbCRC32 = ByteBuffer.allocate(HEADER_SIZE + CRC16_BUFFER_SIZE + CRC32_BUFFER_SIZE + EOF_SIZE + dataArray.length);
        byte[] crcArr32 = bbCRC16.array();
        int crc32 = calculateCRC32FromJNI(crcArr32, crcArr32.length);
        bbCRC32.put(bbCRC16.array());
        bbCRC32.put(intToBytess(crc32));//CRC32

        //EOF
        bbCRC32.put(eof);

        String calculatedCRC16 = convertBytesToHex1(shortToByteArray(crc16));
        String receivedCRC16 = convertBytesToHex1(crc16Bytes);

        String calculatedCRC32 = convertBytesToHex1(intToBytess(crc32));
        String receivedCRC32 = convertBytesToHex1(crc32Bytes);

//        Log.e(TAG, "CRC16: " + calculatedCRC16 + " " + receivedCRC16);
//        Log.e(TAG, "CRC32: " + calculatedCRC32 + " " + receivedCRC32);

        return calculatedCRC16.equals(receivedCRC16) && calculatedCRC32.equals(receivedCRC32);

    }

    public static String convertBytesToHex1(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte temp : bytes) {
            result.append(String.format("%02x", temp));
        }
        return result.toString();
    }

    private short headerToReceiveLength(short lengthVersion) {
        return (short) ((short) lengthVersion & 0x0FFF);
    }

    private short headerToReceiveVersion(short lengthVersion) {
        return (short) ((short) (lengthVersion & 0xF000) >> 12);
    }


    private byte[] shortToByteArray(short sequence) {
        byte[] ret = new byte[2];
        ret[0] = (byte) (sequence & 0xff);
        ret[1] = (byte) ((sequence >> 8) & 0xff);
        return ret;
    }

    private static byte[] intToBytess(final int data) {
        return new byte[]{
                (byte) ((data) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 24) & 0xff)
        };
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String cleanTextContent(String text) {
        // strips off all non-ASCII characters
        text = text.replaceAll("[^\\x00-\\x7F]", "");

        // erases all the ASCII control characters
        text = text.replaceAll("[^\\p{Print}]", "");
        text = text.replaceAll("[^\\p{ASCII}]", "");

        // removes non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");
        text = text.replaceAll("\\ufffd", "");

        return text.trim();
    }

    @NonNull
    private String createVideoFilePath() {
        File folderForVideo = createAppFolderForVideo(application, "SiOnyx");
        File subFolder = getFile(folderForVideo);

        String fileName = Constants.getConnectedCameraSSID() + "_" + System.currentTimeMillis() + ".mp4";
        String outputFile = subFolder.getAbsolutePath() + "/" + fileName;
        filepath = outputFile;
        return outputFile;
    }

    private File getFile(File folderForVideo) {
        String currentCamera = "";
        switch (currentCameraSsid) {
            case OPSIN:
                currentCamera = "Opsin";
                break;
            case NIGHTWAVE:
                currentCamera = "Nightwave_Analog";
                break;
            case NIGHTWAVE_DIGITAL:
                currentCamera = "Nightwave_Digital";
                break;
        }
        File folderFor = new File(folderForVideo, currentCamera);
        File subFolder = new File(folderFor, Constants.getConnectedCameraSSID());
        if (!subFolder.exists())
            subFolder.mkdirs();
        return subFolder;
    }

    private File createAppFolderForImage(Context context, String folderName) {
        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalFilesDir != null) {
            File folder = new File(externalFilesDir, folderName);
            if (!folder.exists()) {
                boolean folderCreated = folder.mkdirs();
                if (!folderCreated) {
                    Log.e("TCPRepository", "Failed to create folder: " + folder.getAbsolutePath());
                    return null;
                }
            }
            return folder;
        } else {
            Log.e("TCPRepository ", "External files directory is null.");
            return null;
        }
    }

    private File createAppFolderForVideo(Context context, String folderName) {
        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (externalFilesDir != null) {
            File folder = new File(externalFilesDir, folderName);
            if (!folder.exists()) {
                boolean folderCreated = folder.mkdirs();
                if (!folderCreated) {
                    Log.e("TCPRepository", "Failed to create folder: " + folder.getAbsolutePath());
                    return null;
                }
            }
            return folder;
        } else {
            Log.e("TCPRepository ", "External files directory is null.");
            return null;
        }
    }

    @Override
    public void updateOpsinLiveStreamingStats(int iFrameSize, int outOfOrderCount,
                                              int fpscount, int totalUdpReceivedOpsin) {
//        Log.e(TAG, "updateOpsinLiveStreamingStats: FrameSize: " + iFrameSize + " OutOfOrder: " + outOfOrderCount + " fps: " + fpscount + " totalUdpReceived: " + totalUdpReceivedOpsin);
        if (opsinRTPStats == null)
            opsinRTPStats = new OpsinRTPStats();

        opsinRTPStats.setOpsin_Fps(fpscount);
        opsinRTPStats.setOpsin_frame_size(iFrameSize);
        opsinRTPStats.setOpsin_out_of_order(outOfOrderCount);
        opsinRTPStats.setOpsin_rtp_received(totalUdpReceivedOpsin);
        valueUpdateOpsinLiveViewStats.postValue(opsinRTPStats);
    }

    @Override
    public void onOpsinBitmapAvailable(Bitmap bitmap) {
        saveImageToMediaStore(bitmap);
    }

    @Override
    public void triggerGetOpsinRecordingState() {
        Log.e(TAG, "triggerGetOpsinRecordingState: triggerOpsingRecordingState");
        getOpsinRecordingStatus();
    }

    public void stopPleaseWaitProgressUpdate() {
        if (localWebServer != null) {
            localWebServer.stopWaitingTimerForUpgradeProgressUpgradeComplete();
        } else {
            stopWaitingTimerForUpgradeProgressUpgradeComplete();
        }
        mService.disconnectSocket();
    }

    public static String getHemisphereIndicatorLatitude(double latitude) {
        return latitude >= 0 ? "N" : "S";
    }

    public String getHemisphereIndicatorLongitude(double longitude) {
        return longitude >= 0 ? "E" : "W";
    }
}
