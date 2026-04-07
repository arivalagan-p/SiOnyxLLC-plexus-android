package com.sionyx.plexus.ui;


import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CHANGE_NETWORK_STATE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN;
import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS;
import static android.provider.Settings.ACTION_WIFI_ADD_NETWORKS;
import static android.provider.Settings.ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED;
import static android.provider.Settings.ADD_WIFI_RESULT_ALREADY_EXISTS;
import static android.provider.Settings.ADD_WIFI_RESULT_SUCCESS;
import static android.provider.Settings.EXTRA_WIFI_NETWORK_LIST;
import static android.provider.Settings.EXTRA_WIFI_NETWORK_RESULT_LIST;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.NetworkRegisteredState.NETWORK_REGISTERED_STATE_NOT_REGISTERED;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.NetworkRegisteredState.NETWORK_REGISTERED_STATE_REGISTERED;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.bleDiscoverRequestCount;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.isHomeNetworkRequest;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.networkRegisteredStatus;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.wifiRequestCount;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.CAMERA_AVAILABLE;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.isCameraAvailableState;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.isUnknownSSIDAvailable;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.networkCapability;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_FPGA;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_NONE;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_BLE;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_FULL;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_RECOVERY;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_RISCV_FPGA;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_RISCV_OVERLAY;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_RISCV;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_WIFI_DIALOG;
import static com.dome.librarynightwave.model.repository.TCPRepository.commandRequested;
import static com.dome.librarynightwave.model.repository.TCPRepository.fwMode;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.utils.Constants.DEFAULT_CAMERA_PWD;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.NWD_FW_VERSION;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS_30;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS_31;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS_33;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS_34;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS_35;
import static com.dome.librarynightwave.utils.Constants.REQUIRED_SDK_PERMISSIONS_36;
import static com.dome.librarynightwave.utils.Constants.RTSP_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.RTSP_IN_LIVE;
import static com.dome.librarynightwave.utils.Constants.RTSP_NONE;
import static com.dome.librarynightwave.utils.Constants.STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.WIFI_IPADDRESS;
import static com.dome.librarynightwave.utils.Constants.WIFI_IPADDRESS_NEW;
import static com.dome.librarynightwave.utils.Constants.WIFI_RTOS;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.isSDK10;
import static com.dome.librarynightwave.utils.Constants.isSDK11;
import static com.dome.librarynightwave.utils.Constants.isSDK12;
import static com.dome.librarynightwave.utils.Constants.isSdk12AndAbove;
import static com.dome.librarynightwave.utils.Constants.isSDK13;
import static com.dome.librarynightwave.utils.Constants.isSDK14;
import static com.dome.librarynightwave.utils.Constants.isSDK15;
import static com.dome.librarynightwave.utils.Constants.isSDK16AndAbove;
import static com.dome.librarynightwave.utils.Constants.isSDKBelow13;
import static com.dome.librarynightwave.utils.Constants.locationLatitude;
import static com.dome.librarynightwave.utils.Constants.locationLongitude;
import static com.dome.librarynightwave.utils.Constants.mState;
import static com.dome.librarynightwave.utils.Constants.mWifiState;
import static com.dome.librarynightwave.utils.Constants.rtspState;
import static com.dome.librarynightwave.utils.Constants.setIsNightWaveCamera;
import static com.dome.librarynightwave.utils.Constants.setNetwork;
import static com.dome.librarynightwave.utils.Constants.setUpdateTransferProtocol;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.applyPreset;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.liveViewErrorMessage;
import static com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;
import static com.sionyx.plexus.ui.camera.CameraFragment.getNucScheduledExecutorService;
import static com.sionyx.plexus.ui.camera.CameraFragment.isInfoButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isSettingButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.timerState;
import static com.sionyx.plexus.ui.camera.CameraViewModel.cameraXValue;
import static com.sionyx.plexus.ui.camera.CameraViewModel.isSelectPIP;
import static com.sionyx.plexus.ui.camera.CameraViewModel.onSelectGallery;
import static com.sionyx.plexus.ui.camera.CameraViewModel.recordButtonState;
import static com.sionyx.plexus.ui.camera.CameraViewModel.videoCropState;
import static com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel.FW_POPUP_VALID_DAYS;
import static com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel.IS_AUTO_CONNECTED;
import static com.sionyx.plexus.ui.dialog.GalleryAllViewModel.itemClick;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpFwUpdateCheck;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpInfo;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpSettings;
import static com.sionyx.plexus.ui.home.HomeViewModel.CurrentScreen.CAMERA_SETTINGS_DIALOG_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.CurrentScreen.FIRMWARE_UPDATE;
import static com.sionyx.plexus.ui.home.HomeViewModel.CurrentScreen.LIVE_VIEW;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.FW_UPDATE_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.HOME;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.popupWindow;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.wiFiHistory;
import static com.sionyx.plexus.utils.Constants.OPSIN_FIRMWARE_UPGRADE_BATTERY_THRESHOLD;
import static com.sionyx.plexus.utils.Constants.OPSIN_RECORDING_STATUS_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.REQUEST_BATTERY_OPTIMIZATION;
import static com.sionyx.plexus.utils.Constants.REQUEST_ENABLE_BT;
import static com.sionyx.plexus.utils.Constants.REQUEST_ENABLE_GPS;
import static com.sionyx.plexus.utils.Constants.WIFI_AVAILABLE;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;
import static com.sionyx.plexus.utils.Constants.WIFI_NOT_CONNECTED;
import static com.sionyx.plexus.utils.Constants.WI_FI_PANEL_RESULT_CODE;
import static com.sionyx.plexus.utils.Constants.apiErrorMessage;
import static com.sionyx.plexus.utils.Constants.capitalizeFirstLetter;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;
import static com.sionyx.plexus.utils.Constants.mSocketState;
import static com.sionyx.plexus.utils.Constants.makeToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TransportInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback;
import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.opsinmodel.BatteryInfo;
import com.dome.librarynightwave.model.repository.pojo.NetworkChange;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.CameraDetails;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.Event;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.sionyx.plexus.BuildConfig;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResponseCallback;
import com.sionyx.plexus.ui.api.interfaces.CloudInterface;
import com.sionyx.plexus.ui.api.interfaces.ProfileInterface;
import com.sionyx.plexus.ui.api.interfaces.ValidUserInterface;
import com.sionyx.plexus.ui.api.requestModel.RequestChangeNewPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestForgotPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestProfileDeviceModel;
import com.sionyx.plexus.ui.api.responseModel.ProfileDevices;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraInfoViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraSettingsViewModel;
import com.sionyx.plexus.ui.cameramenu.fragment.CameraMenuBottomDialog;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.dialog.CameraInfoTabLayoutDialog;
import com.sionyx.plexus.ui.dialog.CameraSettingsTabLayoutDialog;
import com.sionyx.plexus.ui.dialog.GalleryAllViewModel;
import com.sionyx.plexus.ui.dialog.NoticeDialogFragment;
import com.sionyx.plexus.ui.dialog.PasswordDialogFragment;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.login.LoginViewModel;
import com.sionyx.plexus.ui.popup.PopUpCameraSettingsViewModel;
import com.sionyx.plexus.ui.profile.DeleteProductModel;
import com.sionyx.plexus.ui.profile.ProfileViewModel;
import com.sionyx.plexus.ui.profile.QRScanModel;
import com.sionyx.plexus.ui.splashscreen.SplashScreenViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.NetworkUtils;
import com.sionyx.plexus.utils.TooltipHelper;
import com.sionyx.plexus.utils.pinch.zoomcustomviews.ImageMatrixTouchHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements LocationListener, HomeViewModel.NoticeDialogListener, LoginViewModel.PasswordDialogListener {
    String TAG = "MainActivity";

    private LocationManager mLocationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ActivityResultLauncher<Intent> enableBleResultLauncher, navigateToWiFiPanelLauncher, enableIgnoreBatteryOptimizationResultLauncher;
    private ActivityResultLauncher<Intent> addWifiNetworkResultLauncher;
    private String[] permissionArray;
    private String[] rationalePermissionArray;
    private boolean isCheckIfWiFiEnabled;
    private HomeViewModel homeViewModel;
    private CameraPasswordSettingViewModel cameraPasswordSettingViewModel;
    private GalleryAllViewModel galleryAllViewModel;
    private CameraViewModel cameraViewModel;
    private CameraInfoViewModel cameraInfoViewModel;
    private DigitalCameraInfoViewModel digitalCameraInfoViewModel;
    private PopUpCameraSettingsViewModel popUpCameraSettingsViewModel;
    private CameraSettingsViewModel cameraSettingsViewModel;
    private CameraPresetsViewModel presetsViewModel;

    private static String selectedCameraSSID = "";
    private BroadcastReceiver broadcastReceiver;
    public static boolean hasAlreadyAddedInDialogTag;

    private LoginViewModel loginViewModel;
    private ProfileViewModel profileViewModel;
    private SplashScreenViewModel splashScreenViewModel;
    private boolean isTimerStarted = false;
    private String camera_password;
    private SharedPreferences toolTipSharedPreference;
    private TooltipHelper tooltipHelper;

    public enum ShowDialog {
        NONE,
        LOCATION_EXPLANATION_DIALOG,
        LOCATION_RATIONALE_DIALOG,
        WIFI_OFF_DIALOG,
        BLUETOOTH_OFF_DIALOG,
        WIFI_UNABLE_TO_CONNECT_WITH_REQUESTED_WIFI,
        WIFI_CONNECTED_WITH_OTHER_DEVICE,
        WIFI_DISCONNECT,
        SOCKET_FAILED,
        FW_UPDATE_AVAILABLE_DIALOG,
        WIFI_FW_UPDATE_AVAILABLE_DIALOG,
        FW_REBOOT_MODE_DIALOG, /* for this fw update is golden image*/
        FW_RECOVER_DIALOG_MESSAGE, /* for this fw update is golden image*/
        FW_RETRY_DIALOG_MESSAGE, /* for this fw update is failed to retry*/
        POP_UP_DELETE_ITEM_DIALOG,
        PACKET_ALL_ZERO_DIALOG, /* for this all received packet are zero in udp and up to 5 sec not response*/
        OPSIN_NOT_RESPONSE_DIALOG,
        OPSIN_RECYCLE_THE_CAMERA,/* for this opsin camera sent command after 5sec nothing return response show dialog*/
        OPSIN_BATTERY_CHARGE_DIALOG,
        USB_VIDEO_MODE_DIALOG, /* for this show only Camera as USB mode and video mode as UVC show this dialog */
        WIFI_VIDEO_MODE_DIALOG, /* for this show only Camera settings video mode UVC to changes WIFI mode to show this dialog */
        WIFI_TO_USB_VIDEO_MODE_DIALOG, /* for this show only Camera settings video mode UVC to changes WIFI mode to show this dialog */
        SPECIAL_CHARACTER_DIALOG, /* for this show only camera name contains restricted special character there to show this dialog */
        WIFI_SCAN_REQUEST,
        CONFIRM_ERASE,
        STORAGE_ALERT,
        NIGHT_WAVE_CAMERA_RESPONSE_FAILED, // for this NW video mode cmnd failed from camera on popup setting screen
        POPUP_SOCKET_CLOSED,    /* for this socket disconnection dialog for popup setting screen */
        OPSIN_CAMERA_RECORD_IN_PROGRESS,    /* for this while start live streaming on mobile check camera already to start record in progress to show dialog*/
        OPSIN_CAMERA_STREAMING_TO_STOP_RECORD,    /* for this while live streaming on mobile that to start record from camera to show dialog*/
        OPSIN_MIC_COMMAND_FAILED, /* for this while live streaming on mobile that to start record from camera to show dialog*/
        OPSIN_UTC_TIME_ZONE_DIALOG,
        OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE,
        OPSIN_CUSTOMER_CONTACT_DIALOG_TO_UPDATE_FPGA_USING_SDCARD,
        SAVE_CAMERA_SETTINGS, /* for this show camera settings save dialog*/
        DELETE_SAVE_CAMERA_SETTINGS_ALERT_DIALOG, /* for this show camera settings save dialog*/
        CAMERA_SD_CARD_RECORD_START_ALERT_DIALOG,
        CAMERA_SD_CARD_STORAGE_FULL_ALERT_DIALOG,
        SD_CARD_RECORD_AND_STREAMING_STATE_CHECK_DIALOG,
        NWD_SOFTWARE_UPDATE_DIALOG,
        NWD_CAMERA_REBOOT_DIALOG,
        NWD_CAMERA_PASSWORD_RESET,
        CAMERA_CLOSURE_DIALOG,
        APP_UPDATE_DIALOG,
        CAMERA_LOST_WHILE_SWITCHED_DIALOG,
    }

    public ShowDialog showDialog = ShowDialog.NONE;


    private String selectedSsidUntilConnection = "";
    public WiFiNetworkCallback networkCallback;
    private final Handler mLopperHandler = new Handler(Looper.getMainLooper());

    private int TIMER_FOR_FW_VERSION_POPUP = 4000;
    private Handler mHandlerRetryWIFIDisconnection = new Handler(Looper.getMainLooper());
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock powerWakelock;

    /* for this aws login dialog variables*/
    public static boolean hasAlreadyAddedInPasswordDialogTag;
    public ShowDialogLoginScreen showDialogLoginScreen = ShowDialogLoginScreen.NONE;

    public enum ShowDialogLoginScreen {
        NONE,
        SENT_OTP_DIALOG,
        CHANGE_NEW_PASSWORD_DIALOG,
        ALREADY_SIGNIN_DIALOG,
        LOG_OUT_DIALOG,
        NO_INTERNET_CONNECTION_DIALOG,
        ALREADY_SIGN_OUT_OTHER_DEVICE,
        USER_NOT_FOUND_DIALOG,
        QR_CODE_SCAN_RESULT_DIALOG,
        DELETE_PRODUCT_ALERT_DIALOG;
    }

    @Override
    public void onBluetoothOnOffState(boolean isOn) {
        Log.e(TAG, "onBluetoothOnOffState: " + isOn);
        if (!isOn) {
            if (viewModel.getCombinedCameraList().size() > 0) {
                viewModel.getCombinedCameraList().forEach(wiFiHistory -> {
                    String camera_ssid = wiFiHistory.getCamera_ssid();
                    viewModel.updateCameraConnectionState(camera_ssid, WIFI_NOT_CONNECTED);
                });
            }

            if (screenType != HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                showDialog = ShowDialog.BLUETOOTH_OFF_DIALOG;
                showDialog("", getString(R.string.enable_bluetooth_wifi_dialog_message), null);
            }
        }
    }

    @Override
    public void onWiFiOnOffState(boolean isOn) {
        Log.e(TAG, "onWiFiOnOffState: " + isOn);
        isCheckIfWiFiEnabled = isOn;
        if (!isOn) {
            if (!viewModel.getCombinedCameraList().isEmpty()) {
                viewModel.getCombinedCameraList().forEach(wiFiHistory -> {
                    String camera_ssid = wiFiHistory.getCamera_ssid();
                    viewModel.updateCameraConnectionState(camera_ssid, WIFI_NOT_CONNECTED);
                });
            }
            showDialog = ShowDialog.WIFI_OFF_DIALOG;
            showDialog("", getString(R.string.enable_bluetooth_wifi_dialog_message), null);
        }
    }


    @Override
    public void onSocketConnectionState(int mState) {
        Log.e(TAG, "onSocketConnectionState: " + mState);
        mSocketState = mState;
        if (mState == Constants.STATE_CONNECTED) {
            mWifiState = WIFI_STATE_CONNECTED;
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && homeViewModel.isReStartOpsinTCPCommandAndLiveStreaming()) {
                homeViewModel.setReStartOpsinTCPCommandAndLiveStreaming(false);
                if (homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LOGIN_SCREEN && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN) {
                    tcpConnectionViewModel.triggerKeepAlive(false);

                    applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
                    tcpConnectionViewModel.clearPeriodicRequestList();

                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_COMPASS);
                    }
                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.BATTERY_INFO);
                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_MIC_STATE);
                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_FRAME_RATE);
                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_NUC);
                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_SD_CARD_INFO);
                    tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_EV);
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_MONOCHROMATIC);
                        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_NOISE_REDUCTION_RATE);
                        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_ROI);
                        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_GPS_POSITION);
                    }
                }
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        tcpConnectionViewModel.resetOpsinLiveStreamingState();
                        tcpConnectionViewModel.startOpsinLiveStreaming();
                    }, 1000);
                }
            }
        } else if (mState == Constants.STATE_FAILED) {
            homeViewModel.setHasShowProgressBar(false);
            if (viewModel.getOnAvailableSSID().isEmpty()) {
                Log.e(TAG, "onSocketConnectionState: WIFI_DISCONNECTED");
                try {
                    viewModel.setNetworkCapability();
                    tcpConnectionViewModel.disconnectSocket();
                } catch (StackOverflowError | Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "onSocketConnectionState: " + viewModel.getOnAvailableSSID());
                if (fwMode == MODE_NONE) {
                    showDialog = ShowDialog.SOCKET_FAILED;
                    showDialog("", getString(R.string.socket_disconnected_messgae), null);
                } else {
                    //Handle connection broken when firmware upgrade
                    firmwareUpdateSequence.clear();
                    if (fwMode == MODE_WIFI_DIALOG) {
                        tcpConnectionViewModel.wifiUpdateComplete();
                        tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
                    }
                }
            }

        } else if (mState == Constants.STATE_DISCONNECTED) {
            Log.e(TAG, "onSocketConnectionState: STATE_DISCONNECTED " + fwMode);
            if (fwMode != MODE_NONE) {
                tcpConnectionViewModel.wifiUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            }
            homeViewModel.hasShowProgressBar(false);
            if (screenType == POP_UP_SETTINGS_SCREEN) {
                showSockedClosedDialogMessage(getString(R.string.wifi_disconnected_messgae));
            }
        }
    }

    /*
    Show wifi lost dialog for socket disconnection in popup and home screen
     */
    private void showSockedClosedDialogMessage(String message) {
        Log.e(TAG, "showSockedClosedDialogMessage: " + popUpCameraSettingsViewModel.isSocketReconnectCalled);
        if (!popUpCameraSettingsViewModel.isSocketReconnectCalled) {
            showDialog = MainActivity.ShowDialog.POPUP_SOCKET_CLOSED;
            showDialog("", message, null);
        } else {
            popUpCameraSettingsViewModel.setSocketReconnectCalled(false);
        }
    }

    private void hideShowDialog() {
        try {
            if (!homeViewModel.getFragmentManager().isDestroyed()) {
                if (!homeViewModel.getNoticeDialogFragments().isEmpty()) {
                    for (int i = 0; i < homeViewModel.getNoticeDialogFragments().size(); i++) {
                        NoticeDialogFragment dialogFragment = homeViewModel.getNoticeDialogFragments().get(i);
                        if (dialogFragment.isVisible()) {
                            String tag = null;
                            if (dialogFragment.getTag() != null) {
                                tag = dialogFragment.getTag();
                                Log.e(TAG, "showDialog: " + tag);
                            }
                            new Handler().post(() -> {
                                hasAlreadyAddedInDialogTag = false;
                                dialogFragment.dismissAllowingStateLoss(); // FIX
                            });
                        }
                    }
                } else {
                    new NoticeDialogFragment().dismissNow();
                    hasAlreadyAddedInDialogTag = false;
                }
                homeViewModel.getNoticeDialogFragments().clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        Log.e(TAG, "onMultiWindowModeChanged: " + isInMultiWindowMode);
        homeViewModel.isMultiwindowModeActivated(isInMultiWindowMode);
    }

    @Override
    public void onBecameForeground() {
        //addition: for android 12
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            if (isSelectPIP) {
                // isSelectPIP = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN ||
                        homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN ||  homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_SPLASH_SCREEN ) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    setRequestedOrientation(SCREEN_ORIENTATION_SENSOR);
                }
            }
        }
        homeViewModel.appBehaviour = HomeViewModel.AppBehaviour.FOREGROUND;

        // for this below condition while before background to foreground show dialog to dismiss on app foreground
        if (showDialog == ShowDialog.SPECIAL_CHARACTER_DIALOG) {
            try {
                hideShowDialog();
                showDialog = ShowDialog.NONE;
                hasAlreadyAddedInDialogTag = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (showDialog == ShowDialog.SAVE_CAMERA_SETTINGS) {
            try {
                hideShowDialog();
                showDialog = ShowDialog.NONE;
                hasAlreadyAddedInDialogTag = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
            // for this below condition while before background to foreground show dialog of firmware to dismiss on app foreground
        if (showDialog == ShowDialog.FW_UPDATE_AVAILABLE_DIALOG ||showDialog == ShowDialog.APP_UPDATE_DIALOG) {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)){
                Log.e(TAG, "onBecameForeground: sionyx " );
                try {
                    hideShowDialog();
                    showDialog = ShowDialog.NONE;
                    hasAlreadyAddedInDialogTag = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // for this below condition while before background to foreground show dialog to dismiss on app foreground
        if (showDialog == ShowDialog.LOCATION_EXPLANATION_DIALOG || showDialog == ShowDialog.LOCATION_RATIONALE_DIALOG) {
            try {
                hideShowDialog();
                showDialog = ShowDialog.NONE;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // for this below condition while before background to foreground show dialog to dismiss on app foreground
        if (showDialog == ShowDialog.WIFI_VIDEO_MODE_DIALOG) {
            try {
                hideShowDialog();
                showDialog = ShowDialog.NONE;
                cameraViewModel.setHasShowProgress(true);
                /*for this while switching video mode tab and app goes to bag to foreground to dismiss pop up window and show home screen ,
                now again long press pop up settings video mode tab not update that's why set to false. */
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                popUpCameraSettingsViewModel.setDismissConfirmationDialog(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // for this below condition while before background to foreground show dialog to dismiss on app foreground
        if (showDialog == ShowDialog.WIFI_TO_USB_VIDEO_MODE_DIALOG) {
            try {
                hideShowDialog();
                showDialog = ShowDialog.NONE;
                cameraViewModel.setHasShowProgress(true);
                /*for this while switching video mode tab and app goes to bag to foreground to dismiss pop up window and show home screen ,
                now again long press pop up settings video mode tab not update that's why set to false. */
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                popUpCameraSettingsViewModel.setDismissConfirmationDialog(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN) {
            Log.e(TAG, "onBecameForeground: state change to launcher splash screen");
            recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
            homeViewModel.getNavController().navigate(R.id.splashScreenFragment);
        }

        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LOGIN_SCREEN) {
            Log.e(TAG, "onBecameForeground: state change to login screen");
            recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
            homeViewModel.getNavController().navigate(R.id.loginFragment);
        }

        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.FORGOT_PASSWORD_SCREEN) {
            Log.e(TAG, "onBecameForeground: state change to login screen");
            recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
            homeViewModel.getNavController().navigate(R.id.forgotPasswordFragment);
        }

        // for this while connect camera on settings screen and clear all permissions and now open the app permission dialog show and dismiss not show connected camera that's hy use this case here
        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME && screenType == HOME) {
            Log.e(TAG, "onBecameForeground: home screen");
            try {
                if (!homeViewModel.isPermissionChecked()) {
                    try {
                        checkPermissions();
                        checkGpsOnOffState();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (networkCallback != null) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                            /*Wifi is connected in background if Unknown ssid avaialble, So, register default network request. else request specified SSID*/
                            if (isUnknownSSIDAvailable) {
                                isUnknownSSIDAvailable = false;
                                unregisterNetworkCallback();
                                registerDefaultNetwork();
                            } else if (!selectedSsidUntilConnection.isEmpty()) {
                                mLopperHandler.postDelayed(() -> {
                                    Log.e(TAG, "onBecameForeground: " + isCameraAvailableState + " " + selectedSsidUntilConnection + " - " + connectedSsid);
                                    if (isCameraAvailableState == CAMERA_AVAILABLE) {
                                        Log.e(TAG, "onBecameForeground: WIFI CAMERA_AVAILABLE");
                                    } else {
                                        wifiRequestCount = 0;
                                        unregisterNetworkCallback();
                                        registerNetwork(selectedSsidUntilConnection, true);
                                    }
                                }, 1000);
                            } else {
                                hideSpinner();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
            Log.e(TAG, "onBecameForeground: NWD camera " + rtspState + homeViewModel.currentScreen);
            if (homeViewModel.currentScreen == LIVE_VIEW || homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN) {
                if (rtspState == RTSP_IN_LIVE)
                    tcpConnectionViewModel.startServiceDigital();
            }
            if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_SPLASH_SCREEN) {
                Log.e(TAG, "onBecameForeground: CAMERA_SPLASH_SCREEN");
                homeViewModel.getNavController().navigate(R.id.cameraFragment);
            }
        }
        if (tcpConnectionViewModel != null && tcpConnectionViewModel.isProgramatically()) {
            if (screenType == HomeViewModel.ScreenType.POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN) {
                homeViewModel.hidePopUpScreenAndShowHomeScreen(true);
                tcpConnectionViewModel.setProgramatically(false);
                //to solve the after info screen the app goes Bg to Fg trying to open setting and
                // rotate the screen the info screen appear
                homeViewModel.hasSelectPopUpInfo(false);
                homeViewModel.hasSelectPopUpSettings(false);
            } else {
                if (mWifiState == WIFI_STATE_CONNECTED && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LOGIN_SCREEN &&
                        homeViewModel.currentScreen != FIRMWARE_UPDATE && screenType != FW_UPDATE_SCREEN) {
                    Log.e(TAG, "onBecameForeground: connectSocket initiated");
                    tcpConnectionViewModel.connectSocket();
                    tcpConnectionViewModel.setProgramatically(false);
                }
            }

            Log.e(TAG, "onBecameForeground: " + screenType.name());
            /*In this usecase only for firmware update UI*/
            if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                Log.e(TAG, "onBecameForeground: state change to home " + homeViewModel.currentScreen);
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) && homeViewModel.currentScreen == FIRMWARE_UPDATE){
                    homeViewModel.getNavController().popBackStack(R.id.homeFragment, false);
                } else {
                    homeViewModel.hasFwUpdateBackgroundToForeground(true);
                }
                screenType = HomeViewModel.ScreenType.HOME;
            }

            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                Log.e(TAG, "onBecameForeground: opsin camera ");
                cameraViewModel.resetOpsinRecordUI();
            }

            CameraViewModel.setHasPressedSettingCancelIcon(false);
            if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW) {
                cameraViewModel.hasShowLiveViewScreen();
            }
            new Handler().postDelayed(() -> {
                /* if tcp connected true go to camera fragment other wise exit to home screen(on background to foreground use case)*/
                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                if (value != null && value == Constants.STATE_CONNECTED) {
                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN || homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                                    CameraViewModel.setHasAppBackgroundToForeground(false);
                                }
                                // for this keyboard visible state handle
                                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                                    cameraViewModel.setAppHasForegroundOrBackground(true);
                                }

                                if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                                    if (CameraViewModel.hasNewFirmware() && !cameraViewModel.isAnalogMode()) {
                                        CameraViewModel.setHasPressedLiveViewButton(false);
                                        if (homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN)
                                            tcpConnectionViewModel.getVideoMode(); // for this background to foreground on settings tab
                                        else
                                            tcpConnectionViewModel.getCameraMode();
                                        Log.d(TAG, "onBecameForeground: new firmware get camera mode" + CameraViewModel.isHasVisibleSettingsInfoView());
                                    } else {
                                        Log.d(TAG, "onBecameForeground: start live view");
                                        tcpConnectionViewModel.startLiveView(false);
                                    }
                                } else {
                                    Log.e(TAG, "onBecameForeground :recordButtonState5 : " + recordButtonState.name());
                                }
                                break;
                            case OPSIN:
                                /* for this two methods avoid repeatedly observe and get commands*/
                                CameraViewModel.setHasCommandInitiate(false);
                                CameraViewModel.setOpsinCommandInitiateByDialogFragment(false);
                                //   tcpConnectionViewModel.getDigitalZoom();
                                Log.e(TAG, "onBecameForeground: opsin camera ");
                                break;
                            case NIGHTWAVE_DIGITAL:
                                // function will be add
                                Log.e(TAG, "onBecameForeground: Nightwave Digital camera ");
                                CameraViewModel.setHasAppBackgroundToForeground(false);
                                break;
                        }
                    }

                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN
                            || homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN) {
                        cameraViewModel.hasDismissCustomDialog(true);
                        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                            CameraViewModel.setHasCommandInitiate(false);
                            CameraViewModel.setOpsinCommandInitiateByDialogFragment(false);
                            //    tcpConnectionViewModel.getDigitalZoom();
                            Log.e(TAG, "onBecameForeground: CAMERA_INFO_DIALOG_SCREEN");
                        }
                    }

                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_SPLASH_SCREEN) {
                        Log.e(TAG, "onBecameForeground: CAMERA_SPLASH_SCREEN");
                        homeViewModel.getNavController().navigate(R.id.cameraFragment);
                    }
                } else {
                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_SPLASH_SCREEN) {
                        Log.e(TAG, "Tcp not connected: Navigate to home");
                        homeViewModel.getNavController().navigate(R.id.homeFragment);
                    } else {
                        Log.e(TAG, "onBecameForeground: normal state to home");
                    }
                }
            }, 3000);
        } else {
            /*while fW update the socket disconnected trying to the app goes BG to FG screen state update as a HOME*/
            if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                Log.e(TAG, "onBecameForeground else: state change to home");
                screenType = HomeViewModel.ScreenType.HOME;
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)){
                    homeViewModel.getNavController().popBackStack(R.id.homeFragment, false);
                } else {
                    homeViewModel.hasFwUpdateBackgroundToForeground(true);
                }
//                homeViewModel.hasFwUpdateBackgroundToForeground(true);
            }
        }
    }


    @Override
    public void onBecameBackground() {
        Log.e(TAG, "onBecameBackground: " + homeViewModel.currentScreen );

        if (screenType == FW_UPDATE_SCREEN || screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN ||
                currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ||
                 homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_SPLASH_SCREEN) {
            handleOnBecomeBackground();
        }
    }

    private void handleOnBecomeBackground() {
        homeViewModel.isSeekbarSeeked = true;
        homeViewModel.playVideo = false;
        Log.e(TAG, "onBecameBackground: " + isSelectPIP);
        homeViewModel.appBehaviour = HomeViewModel.AppBehaviour.BACKGROUND;

        if (tcpConnectionViewModel != null) {
            if (mSocketState == Constants.STATE_CONNECTED) {
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    if (CameraViewModel.opsinRecordButtonState == CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Start) {
                        Log.d(TAG, "opsin camera record : stopped");
                        CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
                        tcpConnectionViewModel.stopOpsinCameraRecord();
                    }
                    /* for this two methods avoid repeatedly observe and get commands*/
                    CameraViewModel.setHasCommandInitiate(false);
                    CameraViewModel.setOpsinCommandInitiateByDialogFragment(false);
                }

                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                    CameraViewModel.setHasAppBackgroundToForeground(true);
                    PopUpCameraSettingsViewModel.setHasPopUpSettingsBackgroundToForeground(true); // for this pop up setting custom progress dialog while showing app goes to bg to fg to dismiss progress dialog
                    CameraViewModel.setHasPressedLiveViewButton(false);
                    CameraViewModel.setHasPressedSettingCancelIcon(false);
                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW) {
                        Log.e(TAG, "onBecameBackground :recordButtonState : " + recordButtonState.name());
                        if (!isInMultiWindowMode()) {
                            if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED && !isSelectPIP) { //addition
                                tcpConnectionViewModel.stopLiveView();
                                Log.e(TAG, "onBecameBackground: recordButtonState2 " + recordButtonState.name());
                                recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                                homeViewModel.currentScreen = HomeViewModel.CurrentScreen.LIVE_VIEW;
                                cameraViewModel.setLiveViewScreenInBackground(true);
                            }
                        }
                    }

                    if (homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN) {
                        Log.e(TAG, "onBecameBackground :CAMERA_SETTINGS_DIALOG_SCREEN");
                        if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED && !currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                            tcpConnectionViewModel.stopLiveView();
                            Log.e(TAG, "onBecameBackground: recordButtonState2" + recordButtonState.name());
                            recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                            homeViewModel.currentScreen = CAMERA_SETTINGS_DIALOG_SCREEN;
                        }
                    }

                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                        Log.e(TAG, "onBecameBackground :CAMERA_INFO_DIALOG_SCREEN");
                        cameraViewModel.setAppHasForegroundOrBackground(false);
                        if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                            tcpConnectionViewModel.stopLiveView();
                            Log.e(TAG, "onBecameBackground: recordButtonState2" + recordButtonState.name());
                            recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                            homeViewModel.currentScreen = HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN;
                        }
                    }
                }

                if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN && homeViewModel.firmwareStatus == HomeViewModel.FirmwareStatus.FW_UPDATE_STARTED) {
                    switch (currentCameraSsid) {
                        case OPSIN:

                            if ((fwMode == MODE_RISCV
                                    || fwMode == MODE_FPGA
                                    || fwMode == MODE_OPSIN_FULL
                                    || fwMode == MODE_OPSIN_RISCV_FPGA
                                    || fwMode == MODE_OPSIN_RISCV_OVERLAY
                                    || fwMode == MODE_OPSIN_RECOVERY)) {
                                homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_NONE;
                                tcpConnectionViewModel.opsinUpgradeCancel();
                                homeViewModel.setOpsinFwUpdateSequenceIndex(0);
                                homeViewModel.setBootModeCheckCount(0);
                                homeViewModel.setAbnormalFwUpdateSequenceIndex(0);
                                tcpConnectionViewModel.stopOpsinFWTimers();
                                firmwareUpdateSequence.clear();
                                // fo this background to  foreground screen state changed
                                if (homeViewModel.hasShowFwDialog)
                                    homeViewModel.setHasShowFwDialog(false);

                            if (homeViewModel.hasShowRecoverModeDialog())
                                homeViewModel.setHasShowRecoverModeDialog(false);

                            new Handler().postDelayed(() -> {
                                tcpConnectionViewModel.disconnectSocket();
                            }, 1500);
                            tcpConnectionViewModel.setProgramatically(true);
                            //   homeViewModel.hasStopFWUIProgress(true);
                            fwMode = MODE_NONE;
                        } else {
                            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_NONE;
                            tcpConnectionViewModel.stopOpsinFWTimers();
                            homeViewModel.setOpsinFwUpdateSequenceIndex(0);
                            homeViewModel.setBootModeCheckCount(0);
                            homeViewModel.setAbnormalFwUpdateSequenceIndex(0);
                            tcpConnectionViewModel.stopOpsinFWTimers();
                            firmwareUpdateSequence.clear();

                                if (homeViewModel.hasShowFwDialog)
                                    homeViewModel.setHasShowFwDialog(false);

                                if (homeViewModel.hasShowRecoverModeDialog())
                                    homeViewModel.setHasShowRecoverModeDialog(false);

                                new Handler().postDelayed(() -> {
                                    tcpConnectionViewModel.disconnectSocket();
                                }, 1500);
                                tcpConnectionViewModel.setProgramatically(true);
                                //  homeViewModel.hasStopFWUIProgress(true);
                                fwMode = MODE_NONE;
                            }
                            break;
                        case NIGHTWAVE:
                            tcpConnectionViewModel.fwUpdateCancel();
                            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_NONE;

                        tcpConnectionViewModel.setFirmwareUpdateCompleted(false);
                        // BG to FG Behaviour
                            HomeViewModel.hasShowProgressBar = false;
                            firmwareUpdateSequence.clear();
                            fwMode = MODE_NONE;
                            isSettingButtonPressed = false;
                            isInfoButtonPressed = false;
                            cameraViewModel.setIsCapturedImageInLive(false);
                            cameraViewModel.setSelectPreset(false);
                            applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
                            homeViewModel.setSelectedCamera("");
                            homeViewModel.hasShowProgressBar(false);
                            videoCropState = CameraViewModel.VideoCropState.CROP_ON;

                        new Handler().postDelayed(() -> {
                            tcpConnectionViewModel.disconnectSocket();
                            tcpConnectionViewModel.resetSocketState();
                            Log.e(TAG, "onBecameBackground: fw_update cancelled");
                        }, 1500);
                        homeViewModel.setFwUpdateSequenceIndex(0);
                        homeViewModel.setBootModeCheckCount(0);
                        homeViewModel.setAbnormalFwUpdateSequenceIndex(0);

                        /*for this when fw update app goes to background and come back to foreground click camera not show fw update dialog. so reset boolean value*/
                            if (homeViewModel.hasShowFwDialog)
                                homeViewModel.setHasShowFwDialog(false);

                            if (homeViewModel.hasShowRecoverModeDialog())
                                homeViewModel.setHasShowRecoverModeDialog(false);
                            tcpConnectionViewModel.setProgramatically(true);
                            fwMode = MODE_NONE;
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG, "onBecameBackground: fw_update cancelled Nightwave Digital");
                            break;
                    }

                } else {
                    /*normally app goes to background tcp disconnect*/
                    Log.e(TAG, "onBecameBackground: from normal state");
                    //addition: handling pipState
                    if (isSelectPIP && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LOGIN_SCREEN && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN) {
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:

                                Log.d(TAG, "PIP-connected: ");
                                new Handler().postDelayed(() -> {
                                    if (tcpConnectionViewModel != null) {
                                        if (mSocketState == Constants.STATE_CONNECTED) {
                                            tcpConnectionViewModel.startLiveView(true);
                                        } else {
                                            tcpConnectionViewModel.connectSocket();
                                            new Handler().postDelayed(() -> tcpConnectionViewModel.startLiveView(true), 1000);

                                        }
                                    }
                                }, 1000);
                                break;
                            case OPSIN:
                                if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                                    mLopperHandler.postDelayed(() -> tcpConnectionViewModel.startOpsinLiveStreaming(), 500);
                                }
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.d(TAG, "PIP-connected: Nightwave Digital");
                                if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                                 new Handler().postDelayed(() -> tcpConnectionViewModel.startServiceDigital(),1000);
                                }
                                break;
                        }
                    } else {
                        Log.d(TAG, "PIP-Not-connected: ");
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:

                                if (!isInMultiWindowMode()) {
                                    tcpConnectionViewModel.stopLiveView();
                                    tcpConnectionViewModel.disconnectSocket();
                                }
                                break;
                            case OPSIN:
                                if (!isInMultiWindowMode()) {
                                    tcpConnectionViewModel.stopOpsinLiveStreaming();
                                    if (getNucScheduledExecutorService != null) {
                                        getNucScheduledExecutorService.shutdownNow();
                                        getNucScheduledExecutorService = null;
                                    }
                                    tcpConnectionViewModel.disconnectSocket();
                                }

                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.d(TAG, "PIP-Not-connected: Nightwave Digital");
                                if (!isInMultiWindowMode()) {
                                    tcpConnectionViewModel.stopServiceDigital();
                                }
                                break;
                        }
                        tcpConnectionViewModel.setProgramatically(true);
                    }
                }
            } else {
              if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                  if (!isSelectPIP) {
                      Log.e(TAG,"releaseStream called ");
                      if (rtspState == RTSP_CONNECTED) {
                          rtspState = RTSP_IN_LIVE;
                      }
                      if (homeViewModel.currentScreen == LIVE_VIEW || homeViewModel.currentScreen == CAMERA_SETTINGS_DIALOG_SCREEN) {
                          if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                              recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                              tcpConnectionViewModel.stopServiceDigital();
                          }
                      }
                  }
              }
            }
        }

         /*
        dialog value should be false and should be dismissed when app goes bg to fg
         */
        if (screenType == POP_UP_SETTINGS_SCREEN || screenType == POP_UP_INFO_SCREEN) {
            popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
            PopUpCameraSettingsViewModel.setHasDismissPleaseWaitProgressDialog(false);
            if (homeViewModel.dialogFragment != null && homeViewModel.dialogFragment.isVisible()) {
                hasAlreadyAddedInDialogTag = false;
                homeViewModel.dialogFragment.dismissAllowingStateLoss();
            }
            homeViewModel.hidePopUpScreenAndShowHomeScreen(true);
            Log.e(TAG, "onBecameBackground: temp dismissed");
            tcpConnectionViewModel.disconnectSocket();
            tcpConnectionViewModel.setProgramatically(false);
            popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.MANUAL;
            popUpCameraSettingsViewModel.setDateTimeModePosition(0);

            /* for this date/ time picker show i background mode hide*/
            if (screenType == POP_UP_SETTINGS_SCREEN) {
                popUpCameraSettingsViewModel.setShowDatePicker(false);
                popUpCameraSettingsViewModel.setShowTimePicker(false);
            }
        }

        /* for this reset state value from background*/
        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW) {
            cameraViewModel.setIsCapturedImageInLive(false);
            cameraViewModel.setSelectPreset(false);
            onSelectGallery = false;
            isInfoButtonPressed = false;
            isSettingButtonPressed = false;
        }
    }

    //addition: PiP mode with Configuration
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        Log.e(TAG, "tonPictureInPictureMode " + isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            isSelectPIP = true;
            Log.d(TAG, "PictureInPictureMode: " + isInPictureInPictureMode);
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.e(TAG, "onPictureInPictureModeChanged: ORIENTATION_LANDSCAPE");
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    setRequestedOrientation(SCREEN_ORIENTATION_SENSOR);
                    recreate();
                }
            } else {
                Log.e(TAG, "onPictureInPictureModeChanged: ORIENTATION_PORTRAIT");
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            }
        } else {
            Log.e(TAG, "PictureInPictureMode " + isInPictureInPictureMode);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            isSelectPIP = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //   aspectRatioState = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        }
    }

    @Override
    public void onBecameDestoryed() {
        homeViewModel.appBehaviour = HomeViewModel.AppBehaviour.DESTROYED;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private boolean isAutoConnectRequest = false;
    public static ImageMatrixTouchHandler imageMatrixTouchHandler;
    String connectedSsid = "";

    private void hideSystemUI() {
        ComponentActivity activity = (ComponentActivity) this;
        View decorView = activity.getWindow().getDecorView();

        // Enable edge-to-edge mode for modern Android versions
        EdgeToEdge.enable(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) and above
            WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            // Android 10 (API 29) and below
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION); // Android 15 is deprecated this FLAG
        setContentView(R.layout.activity_main);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        galleryAllViewModel = new ViewModelProvider(this).get(GalleryAllViewModel.class);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        cameraInfoViewModel = new ViewModelProvider(this).get(CameraInfoViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(this).get(DigitalCameraInfoViewModel.class);
        popUpCameraSettingsViewModel = new ViewModelProvider(this).get(PopUpCameraSettingsViewModel.class);
        cameraSettingsViewModel = new ViewModelProvider(this).get(CameraSettingsViewModel.class);
        presetsViewModel = new ViewModelProvider(this).get(CameraPresetsViewModel.class);
        cameraPasswordSettingViewModel = new ViewModelProvider(this).get(CameraPasswordSettingViewModel.class);
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        splashScreenViewModel = new ViewModelProvider(this).get(SplashScreenViewModel.class);
        toolTipSharedPreference = getSharedPreferences("ToolTipPromptState", MODE_PRIVATE);
        tooltipHelper = new TooltipHelper(this);
        updateBuildVersionCode();

        detectScreenVisibleState(); // for this detect when press power button
        if (mLocationManager == null)
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        imageMatrixTouchHandler = new ImageMatrixTouchHandler(this);

        final NavHostFragment navHostFragment = (NavHostFragment) this.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            homeViewModel.setNavController(navHostFragment.getNavController());
        }
        homeViewModel.getNavController().addOnDestinationChangedListener((navController, navDestination, bundle) -> {
            switch (navDestination.getId()) {
                case R.id.splashScreenFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN;
                    Log.e(TAG, "CurrentFragment: splashScreenFragment");
                    break;
                case R.id.loginFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.LOGIN_SCREEN;
                    Log.e(TAG, "CurrentFragment: loginFragment");
                    break;
                case R.id.forgotPasswordFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.FORGOT_PASSWORD_SCREEN;
                    Log.e(TAG, "CurrentFragment: forgotPasswordFragment");
                    break;
                case R.id.productScanFragment:
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.PRODUCT_SCAN_SCREEN;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    Log.e(TAG, "CurrentFragment: productScanFragment");
                    break;
                case R.id.productListFragment:
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.PROFILE_LIST_SCREEN;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    Log.e(TAG, "CurrentFragment: productListFragment");
                    break;
                case R.id.homeFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.HOME;
                    Log.e(TAG, "CurrentFragment: homeFragment");
                    break;
                case R.id.cameraSplashFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.CAMERA_SPLASH_SCREEN;
                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                    Log.e(TAG, "CurrentFragment: cameraSplashFragment");
                    break;
                case R.id.cameraFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.LIVE_VIEW;
                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                    //addition: to handle background of live view
                    cameraViewModel.observeHasShowLiveScreen();
                    Log.e(TAG, "CurrentFragment: cameraFragment");
                    break;
                case R.id.cameraInfoTabDialogFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN;
                    Log.e(TAG, "CurrentFragment: cameraInfoTabDialogFragment");
                    break;
                case R.id.cameraSettingsDialogFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    homeViewModel.currentScreen = CAMERA_SETTINGS_DIALOG_SCREEN;
                    Log.e(TAG, "CurrentFragment: cameraSettingsDialogFragment");
                    break;
                case R.id.customProgressDialogFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    Log.e(TAG, "CurrentFragment: customProgressDialogFragment");
                    break;
                case R.id.galleryTabLayoutDialog:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    Log.e(TAG, "CurrentFragment: galleryTabLayoutDialog");
                    break;
                case R.id.cameraPresetDialogFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    Log.e(TAG, "CurrentFragment: cameraPresetDialogFragment");
                    break;
                case R.id.timeZoneDialogFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    Log.e(TAG, "CurrentFragment: timeZoneDialogFragment");
                    break;
                    /* Digital Change password settings */
                case R.id.cameraWifiSettingsFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.CAMERA_WIFI_SETTINGS_SCREEN;
                    Log.e(TAG, "CurrentFragment: Digital wifi settings screen");
                    break;

                case R.id.connectPasswordDialogFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    Log.e(TAG, "CurrentFragment: Digital camera password screen");
                    break;

                case R.id.analogFirmwareUpdateFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    homeViewModel.currentScreen = FIRMWARE_UPDATE;
                    Log.e(TAG, "CurrentFragment: Analog Firmware Update Screen");
                    break;

                case R.id.changePasswordFragment:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    homeViewModel.currentScreen = HomeViewModel.CurrentScreen.CHANGE_PASSWORD_SCREEN;
                    Log.e(TAG, "CurrentFragment: CHANGE_PASSWORD_SCREEN");
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + navDestination.getId());
            }
        });

        homeViewModel.getNotifyUnableToConnectRequestedWiFi().observe(this, new EventObserver<>(object -> {
            Log.e(TAG, "onCreate: getNotifyUnableToConnectRequestedWiFi");
            displayUnableToConnect();
        }));

        startListenWifi();

        homeViewModel.setFragmentManager(getSupportFragmentManager());
        loginViewModel.setFragmentManager(getSupportFragmentManager());
        createLauncherObject();

        viewModel.getNetworkCallbackMutableLiveData().observe(this, networkCallback -> {
            if (networkCallback != null) {
                this.networkCallback = networkCallback;
            }
        });

        /* NWD FW Version prompt should be dismissed and navigate to splash screen */
        homeViewModel.getTimerFinishedAndNavigateToLive().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) {
                isTimerStarted = false;
                Log.e(TAG,"Timer stopped  ");
                showDialog = ShowDialog.NONE;
                navigateToLiveScreen();

                homeViewModel.navigateLiveScreen.postValue(false);
            }
        });

        viewModel.getSsidToRequestWiFiConnection().observe(this, new com.dome.librarynightwave.utils.EventObserver<>(ssid -> {
            if (!ssid.isEmpty()) {
                isAutoConnectRequest = true;
                connectedSsid = getWiFiSSID(mWifiManager);
                connectedSsid = connectedSsid.replace("\"", "");
                selectedCameraSSID = ssid;
                Log.e(TAG, "getSsidToRequestWiFiConnection: Selected: " + ssid + " Connected: " + connectedSsid);
                if (connectedSsid.equals(ssid)) {
                    Network boundNetworkForProcess = connectivityManager.getBoundNetworkForProcess();
                    if (boundNetworkForProcess != null) {//Handle screen base
                        Log.e(TAG, "connectToWiFi: BOUNDEDN NOT NULL");
                        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
                            long delay = connectedSsid.contains(FILTER_STRING6) ? 5000 : 0;
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    connectAndNavigate(connectedSsid);
                                }
                            },delay);
                        } else {
                            /*This function will be invoked when attempting to reconnect with the SOCKET if the WIFI connection experiences fluctuations,
                            and the RECONNECT function is triggered from the Live View within the WIFI lost dialog.*/
                            if (mState != STATE_CONNECTED) {
                                if (homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LOGIN_SCREEN && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN) {
                                    tcpConnectionViewModel.resetSocketState();
                                    switch (currentCameraSsid) {
                                        case NIGHTWAVE:

                                            new Handler().postDelayed(() -> tcpConnectionViewModel.connectSocket(), 5000);
                                            break;
                                        case OPSIN:
                                            tcpConnectionViewModel.connectSocket();
                                            if (isSelectPopUpSettings || isSelectPopUpInfo || isSelectPopUpFwUpdateCheck) {
                                                tcpConnectionViewModel.connectSocket();
                                            } else {
                                                tcpConnectionViewModel.connectSocket();
                                            }
                                            break;
                                        case NIGHTWAVE_DIGITAL:
                                            Log.d(TAG, "socket trying to connect: Nightwave Digital ");
                                            break;
                                    }
                                }
                            }
                        }
                        homeViewModel.getCameraLookupState().transitionToNone();
                    } else {
                        String wiFiSSID = getWiFiSSID(mWifiManager);
                        if (isCamera(wiFiSSID)) {
                            unregisterNetworkCallback();
                            registerNetwork(selectedCameraSSID, true);
                            Log.e(TAG, "connectToWiFi: BOUNDEDN NULL but Camera connected");
                            hideSpinner();
                        } else {
                            Log.e(TAG, "connectToWiFi: BOUNDEDN NULL and Camera not connected");
                            showSpinner();
                        }
                    }
                } else {
//                    addNetwork(ssid);
                    selectedCameraSSID = ssid;
                    discoverAndConnectWithSSID(ssid);
                }
            }
        }));


        viewModel.isWiFiEnabled().observeForever(aBoolean -> isCheckIfWiFiEnabled = aBoolean);

        homeViewModel.isSelectCamera.observe(this, new EventObserver<>(ssid -> {
            Log.i(TAG, "isSelectCamera: " + ssid);
            if (isCheckIfWiFiEnabled) {
                // Digital Camera Change pwd Implementation
                if (ssid.contains(FILTER_STRING6)){
                    Log.i(TAG, "isSelectCamera: " + ssid + " wifi_status " + wiFiHistory.getIs_wifi_connected());
                    if (wiFiHistory.getIs_wifi_connected() != WIFI_CONNECTED) {
                        getAutoConnectStatus(ssid);
                    } else {
                        connectionEstablishmentForSelectedCamera(ssid);
                    }

                } else {
                    connectionEstablishmentForSelectedCamera(ssid);
                }
            } else {
                navigateToWifiPanel();
            }
        }));

       /** onConnect from Dialog to connection establishment with password */
        cameraPasswordSettingViewModel.isConnectCamera.observe(this, new EventObserver<>( cameraPassword -> {
            Log.e(TAG,"isConnectCamera observed for NWD " + cameraPassword);
            if (isCheckIfWiFiEnabled) {
                showSpinner();
                cameraPasswordSettingViewModel.setOnDialogCameraPassword(cameraPassword);
                connectionEstablishmentForSelectedCamera(wiFiHistory.getCamera_ssid());
            } else {
                navigateToWifiPanel();
            }
        }));

        tcpConnectionViewModel.observeOpsinGetBatteryInfo().observe(this, new com.dome.librarynightwave.utils.EventObserver<>(objectEvent -> {
            if (objectEvent != null) {
                BatteryInfo batteryInfo = (BatteryInfo) objectEvent;
                homeViewModel.setOpsinBatteryInfo(batteryInfo);
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
                    hideSpinner();
                    if (screenType == HomeViewModel.ScreenType.HOME) {
                        if (batteryInfo.getState() >= OPSIN_FIRMWARE_UPGRADE_BATTERY_THRESHOLD) {
                            if (showDialog == ShowDialog.NONE) {
                                homeViewModel.startFirmwareUpdate(2);
                            }
                        } else {
                            //Display a dialog as "Please charge the battery to upgrade the firmware"
                            showDialog = ShowDialog.OPSIN_BATTERY_CHARGE_DIALOG;
                            showDialog("", getString(R.string.battery_charge_message), null);
                            homeViewModel.hasShowProgressBar(false);
                        }
                        Log.e(TAG, "observeOpsinGetBatteryInfo : " + batteryInfo.getState() + " showDialog:" + showDialog);
                    } else {
                        Log.d(TAG, "observeOpsinGetBatteryInfo: Not observe in home screen");
                    }
                } else {
                    Log.d(TAG, "observeOpsinGetBatteryInfo: current screen is not home screen");
                }
            }
        }));
        homeViewModel.isLookUpSSIDFound.observe(this, findSSIDUsingBle);


        tcpConnectionViewModel.observeHostUnreachable().observe(this, new com.dome.librarynightwave.utils.EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (isSDK14() || isSDK15() || isSDK16AndAbove()) {
                    viewModel.resetWifiState();
                    unregisterNetworkCallback();
                    registerNetwork(viewModel.getConnectedSsidFromWiFiManager(), true);
                } else {
                    viewModel.resetWifiState();
                    unregisterNetworkCallback();
                    releaseWifiLock();
                    Log.e(TAG, "Others " + viewModel.getConnectedSsidFromWiFiManager() + " " + viewModel.getOnAvailableSSID() + " " + showDialog + " " + fwMode);
                    mLopperHandler.post(this::hideSpinner);
                    if (viewModel.getOnAvailableSSID() == null || viewModel.getOnAvailableSSID().isEmpty()) {
                        if (fwMode == MODE_WIFI_DIALOG) {
                            tcpConnectionViewModel.wifiUpdateComplete();
                            tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
                        } else if (showDialog != ShowDialog.WIFI_DISCONNECT) {
                            fwMode = MODE_NONE;
                            showDialog = ShowDialog.WIFI_DISCONNECT;
                            String wifiLostMsg =  currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ? getString(R.string.wifi_connection_lost) : getString(R.string.wifi_disconnected_messgae);
                            showDialog("", wifiLostMsg, null);
                            timerState = false;
                        }
                    }
                }
            }
        }));

        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                tcpConnectionViewModel.observeOpsinCameraRecordingState().observe(this, getOpsinCameraRecordingState);
                tcpConnectionViewModel.observeOpsinCameraStreamingState().observe(this, getOpsinCameraStreamingState);
            }
        }

        homeViewModel.getInsertOrUpdateNWDSsid().observe(this, selectedSsid -> {
            Log.e(TAG, "onCreate: getInsertOrUpdateNWDSsid() " + selectedSsid);
            if (!selectedSsid.isEmpty() && wiFiHistory != null && wiFiHistory.getIs_wifi_connected() != WIFI_CONNECTED) {
                discoverAndConnectWithSSID(selectedSsid);
//                connectAndNavigate(selectedSsid);
                homeViewModel.setInsertOrUpdateNWDSsid("");
            } else {
                hideSpinner();
            }
        });

        digitalCameraInfoViewModel._cameraDetails.observe(this, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                Log.e(TAG, "getCameraIpResponse : " + object);
                if (object instanceof CameraDetails) {
                    if (((CameraDetails) object).getModelName() != null && Objects.equals(((CameraDetails) object).getRxStatus(), Constants.ON_SUCCESS)) {
                        NWD_FW_VERSION = ((CameraDetails) object).getFirmwareVersion();
                        insertOrUpdateNWD(NWD_FW_VERSION,((CameraDetails) object).getWifi_ssid());
                        homeViewModel.setInsertOrUpdateSsid("");
                        homeViewModel.setInsertOrUpdateNWDSsid("");
                    }
                } else if (object instanceof Integer) {
                    int responseCode = (Integer) object;
                        // server is not reachable
                        apiErrorMessage(MainActivity.this,responseCode);
                        mLopperHandler.post(() -> {
                            if (homeViewModel.hasShowProgressBar())
                                homeViewModel.setHasShowProgressBar(false);
                        });
                } else {
                    //onFailure case
                    showToast(getString(R.string.not_valid_camera));
                    mLopperHandler.post(() -> {
                        if (homeViewModel.hasShowProgressBar())
                            homeViewModel.setHasShowProgressBar(false);
                    });
                }
            }
        }));

        if ((homeViewModel.currentScreen != LIVE_VIEW && showDialog == ShowDialog.CAMERA_CLOSURE_DIALOG) ||
                (homeViewModel.currentScreen == LIVE_VIEW && recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED)) {
            Log.d(TAG, "dismissNoticeDialogFragment: called");
            dismissNoticeDialogFragment();
        }

    }

    private void updateBuildVersionCode() {
        try {
            SharedPreferences toolTipPreference = getSharedPreferences("ToolTipPromptState", MODE_PRIVATE);
            String oldBuildVersion = toolTipPreference.getString("BuildVersion","");
            String newBuildVersion = BuildConfig.VERSION_NAME;
            if (digitalCameraInfoViewModel.isBuildVersionHigher(oldBuildVersion,newBuildVersion)) {
                SharedPreferences.Editor myEdit = toolTipPreference.edit();
                myEdit.putBoolean("isToolTipShown",false);
                myEdit.apply();
            }
            SharedPreferences.Editor myEdit = toolTipPreference.edit();
            myEdit.putString("BuildVersion", oldBuildVersion);

            myEdit.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void connectionEstablishmentForSelectedCamera(String ssid) {
        Log.d(TAG, "connectionEstablishmentForSelectedCamera: called " + ssid);
        tcpConnectionViewModel.disconnectSocket();
        viewModel.resetWifiState();
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        homeViewModel.setHasShowProgressBar(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (manager.hasProvider(LocationManager.GPS_PROVIDER)) {
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    enableWiFiConnectionRequest(ssid);
                } else {
                    checkGpsOnOffState();
                }
            } else if (manager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                enableWiFiConnectionRequest(ssid);
            }
        } else {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                enableWiFiConnectionRequest(ssid);
            } else {
                checkGpsOnOffState();
            }
        }
    }

    /** validate whether the auto connect is true or false from local db, popup will show when the auto connect is 0 */
    private void getAutoConnectStatus(String ssid) {
        digitalCameraInfoViewModel.checkNWDSsidIsExit(ssid).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).
          subscribe(new SingleObserver<NightwaveDigitalWiFiHistory>() {

            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onSuccess(NightwaveDigitalWiFiHistory digitalCameraHistory) {
                try {
                    if (digitalCameraHistory != null){
                        cameraPasswordSettingViewModel.setCurrentDigitalWifiHistory(digitalCameraHistory);
                        Log.e(TAG,"getCurrentSSID isExist " + digitalCameraHistory.getCamera_ssid() + " autoConnect : " + digitalCameraHistory.getIs_auto_connected() + " password " + digitalCameraHistory.getCamera_password());
                        int isAutoConnected = digitalCameraHistory.getIs_auto_connected();
                        if (isAutoConnected == 1 && !digitalCameraHistory.getCamera_password().isEmpty()){
                            cameraPasswordSettingViewModel.onConnectCamera(digitalCameraHistory.getCamera_password());
                        } else {
                            hideSpinner();
                            homeViewModel.getNavController().navigate(R.id.connectPasswordDialogFragment);
                        }
                    }
                } catch (Exception e){
                   Log.e(TAG,"Exception_checkSSidExist " + Objects.requireNonNull(e.getMessage()));
                }
            }

            @Override
            public void onError(Throwable e) {
                hideSpinner();
                homeViewModel.getNavController().navigate(R.id.connectPasswordDialogFragment);
            }
        });
    }


    private void removeStreamingStateObservers() {
        Log.d(TAG, "removeStreamingStateObservers: called");
        tcpConnectionViewModel.observeOpsinCameraRecordingState().removeObserver(getOpsinCameraRecordingState);
        tcpConnectionViewModel.observeOpsinCameraRecordingState().removeObservers(this);
        tcpConnectionViewModel.observeOpsinCameraStreamingState().removeObserver(getOpsinCameraStreamingState);
        tcpConnectionViewModel.observeOpsinCameraStreamingState().removeObservers(this);
    }

    private final com.dome.librarynightwave.utils.EventObserver<Boolean> getOpsinCameraRecordingState = new com.dome.librarynightwave.utils.EventObserver<>(aBoolean -> {
        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
            if (!aBoolean) {
                // here check streaming state
                try {
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        tcpConnectionViewModel.getOpsinStreamingStatus();
                    } else {
                        Log.d(TAG, "onDialogPositiveClick: skipped streaming status");
                        getBatteryInfoBeforeStartFwUpdate();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                hideSpinner();
                showDialog = MainActivity.ShowDialog.SD_CARD_RECORD_AND_STREAMING_STATE_CHECK_DIALOG;
                showDialog("", getString(R.string.sd_card_recording_in_progress), null);
                firmwareUpdateSequence.clear();
            }
        }
    });

    private final com.dome.librarynightwave.utils.EventObserver<Boolean> getOpsinCameraStreamingState = new com.dome.librarynightwave.utils.EventObserver<>(aBoolean -> {
        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
            if (aBoolean) {
                // show alert
                hideSpinner();
                showDialog = MainActivity.ShowDialog.SD_CARD_RECORD_AND_STREAMING_STATE_CHECK_DIALOG;
                showDialog("", getString(R.string.streaming_in_progress_to_power_cycle), null);
                firmwareUpdateSequence.clear();
            } else {
                // start fw update
                getBatteryInfoBeforeStartFwUpdate();
            }
        }
    });

    private void getBatteryInfoBeforeStartFwUpdate() {
        Log.d(TAG, "opsinCameraStreamingState : start Fw update");
        removeStreamingStateObservers();
        String firstFirmware = firmwareUpdateSequence.get(0);
        if (firstFirmware.equalsIgnoreCase(WIFI_RTOS)) {
            tcpConnectionViewModel.getBatteryInfo();
        } else if (homeViewModel.isFpgaUpgradeAvailable()) {
            displayDialogToDecideFpgaUpdateUsingSDCardOrApp();
        } else {
            tcpConnectionViewModel.getBatteryInfo();
        }
    }

    public void setFirmwareSkipped(String ssid, boolean isSkipped) {
        SharedPreferences sharedPreferences = getSharedPreferences("FW_SKIPPED", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putBoolean(ssid, isSkipped);
        myEdit.apply();
    }

    public boolean getIsFirmwareSkipped(String ssid) {
        boolean ImageStateCount = false;
        SharedPreferences sh = getSharedPreferences("FW_SKIPPED", MODE_PRIVATE);
        ImageStateCount = sh.getBoolean(ssid, false);
        return ImageStateCount;
    }

    private void detectScreenVisibleState() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
                    cameraViewModel.setAppHasForegroundOrBackground(true);
                    /* for this app in pip mode now power button press to kill entire application process*/
                    if (isSelectPIP) {
                        finish();
                    }
                    /*to avoid the recording image in gallery while pressing power button from recording timer running state*/
                    HomeViewModel.isRecordingStarted = false;
                    Log.d(TAG, "ACTION_SCREEN_OFF ");
                } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
                    cameraViewModel.setAppHasForegroundOrBackground(false);
                    Log.d(TAG, "ACTION_SCREEN_On ");
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);

        if (isSDK14() || isSDK15() || isSDK16AndAbove()) {
            ContextCompat.registerReceiver(MainActivity.this, broadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, intentFilter);
        }

    }

    private void createLauncherObject() {
        Log.e(TAG, "createLauncherObject: ");
        if (isSDK12() || isSdk12AndAbove() || isSDK16AndAbove()) {
            enableBleResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                hideSpinner();
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    startBluetoothDiscovery();
                } else {
                    showDialog = ShowDialog.BLUETOOTH_OFF_DIALOG;
                    showDialog("", getString(R.string.enable_bluetooth_wifi_dialog_message), null);
                }
            });

            navigateToWiFiPanelLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                hideSpinner();
                isCheckIfWiFiEnabled = mWifiManager.isWifiEnabled();
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.e(TAG, "createLauncherObject: called");
            addWifiNetworkResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                hideSpinner();
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // User agreed to save configurations
                    Intent data = result.getData();
                    if (data != null && data.hasExtra(EXTRA_WIFI_NETWORK_RESULT_LIST)) {
                        for (int code : Objects.requireNonNull(data.getIntegerArrayListExtra(EXTRA_WIFI_NETWORK_RESULT_LIST))) {
                            switch (code) {
                                case ADD_WIFI_RESULT_SUCCESS:
                                    Log.e(TAG, "saveNetwork: ADD_WIFI_RESULT_SUCCESS");
                                    break;
                                case ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED:
                                    // Something went wrong - invalid configuration
                                    discoverAndConnectWithSSID(selectedCameraSSID);
                                    Log.e(TAG, "saveNetwork: ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED");
                                    break;
                                case ADD_WIFI_RESULT_ALREADY_EXISTS:
                                    // Configuration existed (as-is) on device, nothing changed
                                    Log.e(TAG, "saveNetwork: ADD_WIFI_RESULT_ALREADY_EXISTS");
                                    openWifiSettings();
//                                    discoverAndConnectWithSSID(selectedCameraSSID);
                                    break;
                                default:
                                    // Other errors
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "saveNetwork: Denied");
                }
            });
        }

    }

    @SuppressLint("QueryPermissionsNeeded")
    public void openWifiSettings() {
        // Create an Intent to open Wi-Fi Settings
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);

        // Start the activity (if possible)
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Handle situation where no Wi-Fi settings activity exists
            discoverAndConnectWithSSID(selectedCameraSSID);
        }
    }

    public void checkBluetoothIsSupported(boolean hasCheckBluetoothEnable) {
        viewModel.isBluetoothSupported().observe(this, aBoolean -> {
            if (aBoolean != null) {
                if (aBoolean) {
                    checkIsBluetoothDisabled(hasCheckBluetoothEnable);
                } else {
                    showDialog = ShowDialog.NONE;
                    showDialog("", getString(R.string.bluetooth_not_supported), null);
                }
            }
        });
    }

    private void checkIsBluetoothDisabled(boolean hasCheckBluetoothEnable) {
        viewModel.isBluetoothDisabled().observe(MainActivity.this, aBoolean -> {
            if (aBoolean != null) {
                if (aBoolean) {
                    enableBluetooth(hasCheckBluetoothEnable);
                } else {
                    startBluetoothDiscovery();
                }
            }
        });
    }

    private void enableBluetooth(boolean hasCheckBluetoothEnable) {
        if (hasCheckBluetoothEnable) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (isSDK12()  || isSdk12AndAbove() || isSDK16AndAbove()) { // Android 12 to 16
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Request permission if denied
                   checkPermissions();
                    return;
                }
                if (enableBleResultLauncher == null)
                    createLauncherObject();
                enableBleResultLauncher.launch(enableBtIntent);
            } else {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            Log.e(TAG, "enableBluetooth: false");

            viewModel.observeConnectionStateMonitor().removeObserver(networkChangeObserver);

            startListenWifi();
            viewModel.getNetworkCallbackMutableLiveData().observe(this, networkCallback -> {
                if (networkCallback != null) {
                    this.networkCallback = networkCallback;
                }
            });

            mLopperHandler.postDelayed(() -> {
                // for this while bluetooth off and connect camera on settings screen now come back to app without asking bluetooth permission and connect camera
                if (mWifiManager.isWifiEnabled() && mWifiManager.getConnectionInfo() != null) {
                    String ssid = "";
                    if (isSDKBelow13()) {
                        ssid = mWifiManager.getConnectionInfo().getSSID();
                        Log.d(TAG, "WiFiSSID: 12 and below devices");
                    } else {
                        if (viewModel.getNetworkCapability() != null) {
                            final TransportInfo transportInfo = ((NetworkCapabilities) viewModel.getNetworkCapability()).getTransportInfo();
                            if (transportInfo instanceof WifiInfo) {
                                final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                                ssid = wifiInfo.getSSID();
                            }
                        } else {
                            Network activeNetwork = connectivityManager.getActiveNetwork();
                            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                            if (networkCapabilities != null) {
                                final TransportInfo transportInfo = networkCapabilities.getTransportInfo();
                                if (transportInfo instanceof WifiInfo) {
                                    final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                                    ssid = wifiInfo.getSSID();
                                }
                            }
                        }
                    }

                    String connectedSsid = ssid.replace("\"", "");
                    if (!connectedSsid.equals("") && !connectedSsid.equals("<unknown ssid>")) {
                        if (isCamera(connectedSsid)) {
                            if (!viewModel.hasAlreadyExistSSId(viewModel.getConnectedSsidFromWiFiManager())) {
                                homeViewModel.setInsertOrUpdateSsid(connectedSsid);
                            }
                        }
                    }else {
                        ssid = mWifiManager.getConnectionInfo().getSSID();
                        Log.d("Connected WiFi SSID", ssid);
                    }
                    Log.d("Connected WiFi SSID", ssid);
                }
            },3000);


        }
    }

    private void startBluetoothDiscovery() {
        homeViewModel.setPermissionChecked(true);
        viewModel.setShouldStartToGetWifiHistory(true);
    }

    @Nullable
    private String getCameraIpAddress(String connectedSsid) {
        String cameraIpAddress = "";
        WifiInfo wifiInfo = getWifiInfo(mWifiManager);
        if (wifiInfo != null) {
            try {
                int ip = wifiInfo.getIpAddress();
                byte[] buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array();
                cameraIpAddress = InetAddress.getByAddress(buffer).getHostAddress();
                setIsNightWaveCamera(connectedSsid, cameraIpAddress); // for this set port number
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return cameraIpAddress;
    }

    private void registerNetwork(String ssid, boolean enableTimeout) {
        Log.e(TAG, "registerNetwork: " + ssid + " Password : " + cameraPasswordSettingViewModel.getOnDialogCameraPassword());
        if (networkRegisteredStatus == NETWORK_REGISTERED_STATE_REGISTERED) {
            unregisterNetworkCallback();
        }
        NetworkRequest networkRequest = viewModel.getNetworkRequest(ssid, cameraPasswordSettingViewModel.getOnDialogCameraPassword() != null ? cameraPasswordSettingViewModel.getOnDialogCameraPassword() : "");
        if (networkRequest != null) {
            if (enableTimeout) {
                connectivityManager.requestNetwork(networkRequest, networkCallback, 10000);
            } else {
                connectivityManager.requestNetwork(networkRequest, networkCallback);
            }
            isHomeNetworkRequest = false;
            networkRegisteredStatus = NETWORK_REGISTERED_STATE_REGISTERED;
        } else {
            hideSpinner();
            Toast.makeText(this, R.string.please_re_launch_the_application, Toast.LENGTH_SHORT).show();
        }
    }

    private void registerDefaultNetwork() {
        Log.e(TAG, "registerDefaultNetwork: ");
        if (networkRegisteredStatus == NETWORK_REGISTERED_STATE_REGISTERED) {
            unregisterNetworkCallback();
        }
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .removeTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        networkRegisteredStatus = NETWORK_REGISTERED_STATE_REGISTERED;
        isHomeNetworkRequest = false;
    }

    private void networkRequestToMonitor() {
        Log.e(TAG, "networkRequestToMonitor: ");
        NetworkRequest networkRequest = viewModel.getNetworkRequestToMonitor();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        networkRegisteredStatus = NETWORK_REGISTERED_STATE_REGISTERED;
        isHomeNetworkRequest = false;
    }

    public void unregisterNetworkCallback() {
        try {
            Log.e(TAG, "unregisterNetworkCallback: " + networkRegisteredStatus.name());
            if (networkRegisteredStatus == NETWORK_REGISTERED_STATE_REGISTERED) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkRegisteredStatus = NETWORK_REGISTERED_STATE_NOT_REGISTERED;
            } else {
                Log.e(TAG, "unregisterNetworkCallback: " + networkRegisteredStatus.name());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startListenWifi() {
        Log.e(TAG, "startListenWifi: processNetworkChange ");
        viewModel.observeConnectionStateMonitor().observeForever(networkChangeObserver);
    }

    private final com.dome.librarynightwave.utils.EventObserver<NetworkChange> networkChangeObserver = new com.dome.librarynightwave.utils.EventObserver<>(networkChange -> {
        Log.e(TAG, "networkChangeObserver: called");
        try {
            processNetworkChange(networkChange);
        } catch (Exception e) {
            e.printStackTrace();
        }
    });

    private void processNetworkChange(NetworkChange networkChange) {
        if (networkChange.isShouldHideProgress()) {
            mLopperHandler.post(this::hideSpinner);
        }
        String connectedSSID = networkChange.getConnectedSSID();
        Log.e(TAG, "processNetworkChange: " + networkChange.isAvailable()
                + " IS_CAMERA: " + isCamera(connectedSSID == null ? "NULL" : connectedSSID)
                + " SELECTED_SSID: " + selectedCameraSSID
                + " CONNECTED_SSID: " + connectedSSID
                + " IS_DISCONNECT_SOCKET: " + networkChange.isDisconnectSocket());
        if (networkChange.isDisconnectSocket()) {
            mHandlerRetryWIFIDisconnection.post(() -> tcpConnectionViewModel.disconnectSocket());
        } else if (networkChange.isAvailable() && connectedSSID != null && isCamera(connectedSSID)) {
            String cameraIpAddress = getCameraIpAddress(connectedSSID);
            Log.e(TAG, "WIFI_CONNECTED - processNetworkChange connectedSSID: " + connectedSSID
                    + " lastConnectedSSID: " + homeViewModel.lastConnectedSsid
                    + " isCamera(connectedSSID): " + isCamera(connectedSSID)
                    + " cameraIpAddress: " + cameraIpAddress);
            homeViewModel.connectedSsid = connectedSSID;
            Constants.NWD_WEB_IPADDRESS = cameraIpAddress;
            homeViewModel.setWiFiRetryAfterDisconnect(false);
            homeViewModel.getCameraLookupState().transitionToNone();
            setNetwork(networkChange.getNetwork());
            if (cameraIpAddress != null && cameraIpAddress.startsWith("10.0.0") || cameraIpAddress.equals(WIFI_IPADDRESS) || currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)
                /*cameraIpAddress.startsWith("172.16.1") || cameraIpAddress.equals(WIFI_IPADDRESS_NWD)  /*|| cameraIpAddress.equals(WIFI_IPADDRESS_NEW)*/) {
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW && recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED && homeViewModel.lastConnectedSsid != null &&
                        !homeViewModel.lastConnectedSsid.isEmpty() && !Objects.equals(homeViewModel.lastConnectedSsid, homeViewModel.connectedSsid)){
                        if (showDialog != ShowDialog.CAMERA_LOST_WHILE_SWITCHED_DIALOG) {
                            Log.e(TAG,"Camera switched wifi lost");
                            liveViewErrorMessage.postValue(null);
                            wifiRequestCount = 0;
                            fwMode = MODE_NONE;
                            homeViewModel.setDialogMode("SWITCH"); // temp added dialogmode
                            showDialog = ShowDialog.CAMERA_LOST_WHILE_SWITCHED_DIALOG;
                            String wifiLostMsg = getString(R.string.wifi_connection_lost);
                            showDialog("", wifiLostMsg, null);
                            timerState = false;
                            return;
                        }
                }
                WIFI_IPADDRESS_NEW = cameraIpAddress;
                connectivityManager.reportNetworkConnectivity(networkChange.getNetwork(), true);
                bleDiscoverRequestCount = 0;
                lockWifi();
//                addWiFiSuggestion(connectedSSID);
                makeSocketConnection(connectedSSID);
                liveViewErrorMessage.postValue(null);
                dismissNoticeDialogFragment();
            } else {
                showDialog = ShowDialog.WIFI_CONNECTED_WITH_OTHER_DEVICE;
                showDialog("", getString(R.string.wifi_connected_with_other_client), null);
            }
        } else if (!networkChange.isAvailable()) {
            Log.e(TAG, "WIFI_DISCONNECTED - processNetworkChange ConnectedSsidFromWiFiManager: " + viewModel.getConnectedSsidFromWiFiManager() + " getOnAvailableSSID: " + viewModel.getOnAvailableSSID());
            homeViewModel.lastConnectedSsid = viewModel.getOnAvailableSSID();
            // Look Wifi Discovery and reconnect with WIFI
            homeViewModel.getCameraLookupState().transitionToNone();
            if (homeViewModel.currentScreen != HomeViewModel.CurrentScreen.HOME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                if (viewModel.getConnectedSsidFromWiFiManager() != null && viewModel.getConnectedSsidFromWiFiManager().equals(viewModel.getOnAvailableSSID()) && fwMode == MODE_NONE) {
                    if (!currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                        liveViewErrorMessage.postValue("Buffering");
                    }

                    isCheckIfWiFiEnabled = mWifiManager.isWifiEnabled();
                    Log.e(TAG, "processNetworkChange...: "+ isCheckIfWiFiEnabled);
                    if (isCheckIfWiFiEnabled && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                        if (showDialog != ShowDialog.WIFI_DISCONNECT) {
                            liveViewErrorMessage.postValue(null);
                            wifiRequestCount = 0;
                            fwMode = MODE_NONE;
                            hideShowDialog();
                            showDialog = ShowDialog.WIFI_DISCONNECT;
                            String wifiLostMsg = currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ? getString(R.string.wifi_connection_lost) : getString(R.string.wifi_disconnected_messgae);
                            showDialog("", wifiLostMsg, null);
                            timerState = false;
                            viewModel.updateCameraConnectionState(viewModel.getOnAvailableSSID(), WIFI_NOT_CONNECTED);
                            homeViewModel.setCameraSwitched(false);
                            return;
                        }
                    }
                    releaseWifiLock();
                    String unableToConnect = networkChange.getUnableToConnect();
                    if (unableToConnect != null) {
                        Log.e(TAG, "WIFI_DISCONNECTED - processNetworkChange: Unable To connect ");
                    }
                    mHandlerRetryWIFIDisconnection.postDelayed(() -> {
                        // wifi lost popup is not appear when entered into PIP and disconnect the wifi
                        if (wifiRequestCount == 3 || currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                            mLopperHandler.post(() -> {
                                hideSpinner();
                                viewModel.updateCameraConnectionState(selectedCameraSSID, WIFI_NOT_CONNECTED);
                                homeViewModel.setCameraSwitched(false);
                            });

                            if (showDialog != ShowDialog.WIFI_DISCONNECT) {
                                liveViewErrorMessage.postValue(null);
                                wifiRequestCount = 0;
                                fwMode = MODE_NONE;
                                hideShowDialog();
                                showDialog = ShowDialog.WIFI_DISCONNECT;
                                String wifiLostMsg =  currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ? getString(R.string.wifi_connection_lost) : getString(R.string.wifi_disconnected_messgae);
                                showDialog("", wifiLostMsg, null);
                                timerState = false;
                            }
                        } else {
                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                                tcpConnectionViewModel.stopAndResetUDP();
                            }
                            connectedSsid = "";
                            selectedCameraSSID = viewModel.getConnectedSsidFromWiFiManager();
                            homeViewModel.setWiFiRetryAfterDisconnect(true);
                            discoverAndConnectWithSSID(viewModel.getConnectedSsidFromWiFiManager());
                            bleDiscoverRequestCount = bleDiscoverRequestCount + 1;
                        }

                    }, 5000);
                } else if (fwMode != MODE_NONE) {
                    handleWiFiDisconnectDuringFwUpgrade();
                }

            } else {
                if (viewModel.getConnectedSsidFromWiFiManager() != null && viewModel.getConnectedSsidFromWiFiManager().equals(viewModel.getOnAvailableSSID())) {
                    releaseWifiLock();
                    String unableToConnect = networkChange.getUnableToConnect();
                    if (unableToConnect != null) {
                        Log.e(TAG, "WIFI_DISCONNECTED - processNetworkChange: Unable To connect ");
                        mLopperHandler.post(this::hideSpinner);
                        //Handle previous connection
                        try {
                            tcpConnectionViewModel.disconnectSocket();
                        } catch (StackOverflowError | Exception e) {
                            e.printStackTrace();
                        }
                        selectedSsidUntilConnection = "";
                        if (selectedCameraSSID.contains(FILTER_STRING6)){
                            if (cameraPasswordSettingViewModel.getCurrentDigitalWifiHistory() != null){
                                digitalCameraInfoViewModel.updateCameraAutoConnect(selectedCameraSSID,0);
                            }
                        }
                        homeViewModel.setNotifyUnableToConnectRequestedWiFi(true);

                    } else {
                        Log.e(TAG, "WIFI_DISCONNECTED - processNetworkChange: Connection Lost from " + networkChange.getNetwork() + " " + homeViewModel.isCameraSwitched() + " " + fwMode);
                        handleWiFiDisconnectDuringFwUpgrade();
                    }
                } else {
                    releaseWifiLock();
                    Log.e(TAG, "Others " + viewModel.getConnectedSsidFromWiFiManager() + " " + viewModel.getOnAvailableSSID() + " " + showDialog + " " + fwMode);
                    mLopperHandler.post(this::hideSpinner);
                    if (viewModel.getOnAvailableSSID() == null || viewModel.getOnAvailableSSID().isEmpty()) {
                        if (fwMode == MODE_WIFI_DIALOG) {
                            tcpConnectionViewModel.wifiUpdateComplete();
                            tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
                        } else if (showDialog != ShowDialog.WIFI_DISCONNECT) {
                            String unableToConnect = networkChange.getUnableToConnect();
                            if (unableToConnect != null) {
                                mLopperHandler.post(this::hideSpinner);
                                if (!viewModel.getConnectedSsidFromWiFiManager().startsWith(FILTER_STRING6)) {
                                    try {
                                        tcpConnectionViewModel.disconnectSocket();
                                    } catch (StackOverflowError | Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                selectedSsidUntilConnection = "";
                                homeViewModel.setNotifyUnableToConnectRequestedWiFi(true);
                            } else {
                                fwMode = MODE_NONE;
                                hideShowDialog();
                                showDialog = ShowDialog.WIFI_DISCONNECT;
                                String wifiLostMsg =  currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ? getString(R.string.wifi_connection_lost) : getString(R.string.wifi_disconnected_messgae);
                                showDialog("", wifiLostMsg, null);
                                timerState = false;
                            }
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "Other Network Disconnected");
        }
    }

    private void handleWiFiDisconnectDuringFwUpgrade() {
        viewModel.setOnAvailableSSID("");
        Log.e(TAG, "handleWiFiDisconnectDuringFwUpgrade: " + homeViewModel.isCameraSwitched() + " fWMode " + fwMode + " wifiEnabled " + isCheckIfWiFiEnabled);
        if (!homeViewModel.isCameraSwitched()) {
            mLopperHandler.post(() -> viewModel.updateCameraConnectionState(viewModel.getConnectedSsidFromWiFiManager(), WIFI_NOT_CONNECTED));// updated in DB
            firmwareUpdateSequence.clear();
            isCheckIfWiFiEnabled = mWifiManager.isWifiEnabled();
            if (fwMode == MODE_WIFI_DIALOG) {
                tcpConnectionViewModel.wifiUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            } else if (fwMode == MODE_OPSIN_BLE) {
                tcpConnectionViewModel.bleUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            } else if (fwMode == MODE_OPSIN_RISCV_FPGA) {
                tcpConnectionViewModel.riscvFpgaUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            } else if (fwMode == MODE_OPSIN_RISCV_OVERLAY) {
                tcpConnectionViewModel.riscvOverlayUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            } else if (fwMode == MODE_OPSIN_RECOVERY) {
                tcpConnectionViewModel.riscvRecoveryUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            } else if (fwMode == MODE_RISCV) {
                tcpConnectionViewModel.bleUpdateComplete();
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            } else if (showDialog != ShowDialog.WIFI_DISCONNECT && isCheckIfWiFiEnabled) {
                fwMode = MODE_NONE;
                showDialog = ShowDialog.WIFI_DISCONNECT;
                String wifiLostMsg =  currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ? getString(R.string.wifi_connection_lost) : getString(R.string.wifi_disconnected_messgae);
                showDialog("", wifiLostMsg, null);
                timerState = false;
            }
            // turn off camera- Wifi Disconnected and wifi lost popup is not show while FW Progress, fwmode value is 2
            if (screenType == FW_UPDATE_SCREEN && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) && showDialog != ShowDialog.WIFI_DISCONNECT && isCheckIfWiFiEnabled) {
                fwMode = MODE_NONE;
                showDialog = ShowDialog.WIFI_DISCONNECT;
                String wifiLostMsg = getString(R.string.wifi_disconnected_messgae);
                showDialog("", wifiLostMsg, null);
                timerState = false;
            }
        } else {
            mLopperHandler.post(() -> {
                hideSpinner();
                viewModel.updateCameraConnectionState(viewModel.getConnectedSsidFromWiFiManager(), WIFI_AVAILABLE);
                homeViewModel.setCameraSwitched(false);
            });
        }
        selectedSsidUntilConnection = "";
    }

    private void makeSocketConnection(String connectedSSID) {
        viewModel.setConnectedSsidFromWiFiManager(connectedSSID);
        viewModel.setOnAvailableSSID(connectedSSID);
        selectedSsidUntilConnection = "";
        wifiRequestCount = 0;
        Log.e(TAG, "startListenWifi: homeViewModel.currentScreen " + homeViewModel.currentScreen);
        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
            Log.e(TAG, "startListenWifi: IF");
            mLopperHandler.post(() -> {
                viewModel.setIsAutoConnectEnabled(-1);
                homeViewModel.setInsertOrUpdateSsid(viewModel.getConnectedSsidFromWiFiManager());
                homeViewModel.setInsertOrUpdateNWDSsid(viewModel.getConnectedSsidFromWiFiManager());
                homeViewModel.setCameraSwitched(false);
            });
        } else {
            Log.e(TAG, "startListenWifi: ELSE");
            tcpConnectionViewModel.resetSocketState();
            if (homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LOGIN_SCREEN && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        new Handler().postDelayed(() -> tcpConnectionViewModel.connectSocket(), 5000);
                        break;
                    case OPSIN:
                        homeViewModel.setReStartOpsinTCPCommandAndLiveStreaming(true);
                        tcpConnectionViewModel.connectSocket();
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"Reset Socket: Nightwave Digital");
                        break;
                }
            }
        }
    }

    private void enableWiFiConnectionRequest(String selectedCamera) {
        Log.e(TAG,"enableWiFiConnectionRequest called " + selectedCamera);
        requestLocationUpdate();
        if (!isCheckIfWiFiEnabled) {
            if (screenType != HomeViewModel.ScreenType.ADD_SCREEN) {
                navigateToWifiPanel();
            }
        } else {
            if (selectedCamera != null && !selectedCamera.equalsIgnoreCase("")) {
                homeViewModel.setHasShowRecoverModeDialog(false);
                tcpConnectionViewModel.setFirmwareUpdateChecked(false);
                isAutoConnectRequest = false;

//                addNetwork(selectedCamera);
                selectedCameraSSID = selectedCamera;
                discoverAndConnectWithSSID(selectedCamera);
            }
        }
    }

    private void discoverAndConnectWithSSID(String selectedSsid) {
        Log.e(TAG,"discoverAndConnectWithSSID called " + selectedSsid);
        showSpinner();
        String SELECTED_SSID = selectedSsid;
        int hasWriteSettingsPermission = checkSelfPermission(CHANGE_NETWORK_STATE);
        if (hasWriteSettingsPermission == PackageManager.PERMISSION_GRANTED) {
            SELECTED_SSID = SELECTED_SSID.replace("\"", "");
            selectedCameraSSID = SELECTED_SSID;
            connectedSsid = getWiFiSSID(mWifiManager);
            connectedSsid = connectedSsid.replace("\"", "");
            Log.e(TAG, "connectToWiFi: Selected: " + SELECTED_SSID + " Connected: " + connectedSsid);
            if (connectedSsid.equals(SELECTED_SSID)) {
                Network boundNetworkForProcess = connectivityManager.getBoundNetworkForProcess();
                if (boundNetworkForProcess != null) {
                    Log.e(TAG, "connectToWiFi: BOUNDEDN NOT NULL");

                    long delay = connectedSsid.contains(FILTER_STRING6) ? 5000 : 0;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "run: discoverAndConnectWithSsid selectedCamera " +  homeViewModel.getSelectedCamera() );
                            if (homeViewModel.getSelectedCamera() != null && homeViewModel.getSelectedCamera().equals(connectedSsid)) { // to avoid auto connect when screen rotation
                                connectAndNavigate(connectedSsid);
                                homeViewModel.getCameraLookupState().transitionToNone();
                            } else {
                                hideSpinner();
                            }
                        }
                    },delay);

                } else {
                    String wiFiSSID = getWiFiSSID(mWifiManager);
                    if (isCamera(wiFiSSID)) {
                        unregisterNetworkCallback();
                        registerNetwork(selectedCameraSSID, true);
                        Log.e(TAG, "connectToWiFi: BOUNDEDN NULL but Camera connected");
                        hideSpinner();
                    } else {
                        Log.e(TAG, "connectToWiFi: BOUNDEDN NULL and Camera not connected");
                        showSpinner();
                    }
                }
            } else {
                if (homeViewModel.getCameraLookupState().isNone() || homeViewModel.getCameraLookupState().isNotFound()) {
                    homeViewModel.getCameraLookupState().transitionToLooking();
                }
                homeViewModel.setLookUpSSID(selectedCameraSSID);
                homeViewModel.scanLeDevice();
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.change_network_state_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private final EventObserver<Boolean> findSSIDUsingBle = new EventObserver<>(isLookUpSsidFound -> {
        Log.e(TAG, "discoverAndConnectWithSSIDIfLost: " + isLookUpSsidFound);
        if (isLookUpSsidFound) {
            // removed ble discover for connection
           /* if (homeViewModel.isWiFiRetryAfterDisconnect()) {
                mHandlerRetryWIFIDisconnection.postDelayed(() -> {
                    Log.e(TAG, "discoverAndConnectWithSSIDIfLost: Called");
                    mLopperHandler.post(() -> {
                        hideSpinner();
                        viewModel.updateCameraConnectionState(selectedCameraSSID, WIFI_AVAILABLE);
                        homeViewModel.setCameraSwitched(false);
                    });
                    connectToSSID(selectedCameraSSID);
                }, 2000);
            } else {
                Log.e(TAG, "discoverAndConnectWithSSIDIfLost: Called1");
                mLopperHandler.post(() -> {
                    hideSpinner();
                    viewModel.updateCameraConnectionState(selectedCameraSSID, WIFI_AVAILABLE);
                    homeViewModel.setCameraSwitched(false);
                });
                connectToSSID(selectedCameraSSID);
            }*/
            connectToSSID(selectedCameraSSID);
        } else {
            if (homeViewModel.getCameraLookupState().isNotFound()) {
                homeViewModel.getCameraLookupState().transitionToNone();
                mLopperHandler.post(() -> {
                    hideSpinner();
                    viewModel.updateCameraConnectionState(selectedCameraSSID, WIFI_NOT_CONNECTED);
                    homeViewModel.setCameraSwitched(false);
                });
                if (homeViewModel.isWiFiRetryAfterDisconnect()) {
                    if (bleDiscoverRequestCount < 3) {
                        connectedSsid = "";
                        selectedCameraSSID = viewModel.getConnectedSsidFromWiFiManager();
                        homeViewModel.setWiFiRetryAfterDisconnect(true);
                        discoverAndConnectWithSSID(viewModel.getConnectedSsidFromWiFiManager());
                        bleDiscoverRequestCount = bleDiscoverRequestCount + 1;
                    } else {
                        //show wifi disconnect dialog if camera disconnected manually by user
                        if (showDialog != ShowDialog.WIFI_DISCONNECT) {
                            bleDiscoverRequestCount = 0;
                            homeViewModel.setWiFiRetryAfterDisconnect(false);
                            fwMode = MODE_NONE;
                            showDialog = ShowDialog.WIFI_DISCONNECT;
                            String wifiLostMsg =  currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) ? getString(R.string.wifi_connection_lost) : getString(R.string.wifi_disconnected_messgae);
                            showDialog("", wifiLostMsg, null);
                            timerState = false;
                            liveViewErrorMessage.postValue(null);
                        }
                    }
                } else {
                    hideSpinner();
                    Toast.makeText(MainActivity.this, R.string.ble_discover_warning, Toast.LENGTH_LONG).show();
                    homeViewModel.getCameraLookupState().transitionToNone();
                }
            }
        }
    });

    private void connectToSSID(String selectedSsid) {
        Log.e(TAG, "connectToSSID: Called " + isAutoConnectRequest + " " + selectedSsid);
        if (!selectedCameraSSID.equalsIgnoreCase(selectedSsid)) {
            wifiRequestCount = 0;
        }
        if (selectedSsid != null && !selectedSsid.equals("")) {
            connectToWiFi(selectedSsid);
            viewModel.setSsidToRequestWiFiConnection("");
        }
    }

    public void connectToWiFi(final String selectedSsid) {
        Log.e(TAG,"connectToWiFi called " + selectedSsid);
        if (isCheckIfWiFiEnabled) {
            homeViewModel.setLookUpSSID("");
            viewModel.stopBleDiscovery();

            selectedSsidUntilConnection = selectedSsid;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                if (wifiRequestCount == 4) {
                    networkRequestToMonitor();
                    mLopperHandler.post(this::dismissNoticeDialogFragment);
                    showDialog = ShowDialog.WIFI_SCAN_REQUEST;
                    showDialog("", getString(R.string.wifi_scan_warning), null);
                } else {
                    showSpinner();
                    unregisterNetworkCallback();
                    registerNetwork(selectedSsidUntilConnection, true);
                }
                wifiRequestCount = wifiRequestCount + 1;
            } else {
                unregisterNetworkCallback();
                registerNetwork(selectedSsidUntilConnection, false);
            }
        } else {
            navigateToWifiPanel();
        }
    }


    private void connectAndNavigate(String connectedSsid) {
        String cameraIpAddress = getCameraIpAddress(connectedSsid);

        Log.e(TAG,"connectAndNavigate called ipAddress " + cameraIpAddress);
        if (cameraIpAddress != null) {

            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                Constants.NWD_WEB_IPADDRESS = cameraIpAddress;
                setUpdateTransferProtocol(Constants.UpdateTransferProtocol.https);  // First try with https
                digitalCameraInfoViewModel.getDigitalCameraInformation(new CameraResponseCallback() {
                    @Override
                    public void onSuccess(String responseStr, int code) {
                        if (code == Constants.ON_SUCCESS) {
                            // json response
                            JSONObject obj = null;
                            try {
                                obj = new JSONObject(responseStr);

                                if (obj.getString("type").equals("done") && obj.getJSONObject("reply").getString("errno").equals("0")) {
                                    JSONObject value = obj.getJSONObject("reply").getJSONObject("value");
                                    String modelName = value.optString("modelName");
                                    String fwVersion = value.optString("firmwareVersion");
                                    String modelNumber = value.optString("modelNumber");
                                    CameraDetails setCameraDetails = new CameraDetails();
                                    setCameraDetails.setRxStatus(code);
                                    setCameraDetails.setModelName(modelName);
                                    setCameraDetails.setFirmwareVersion(fwVersion);
                                    setCameraDetails.setModelNumber(modelNumber);
                                    setCameraDetails.setWifi_ssid(connectedSsid);

                                    digitalCameraInfoViewModel.cameraDetails.setValue(new Event<>(setCameraDetails));;
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSONException  " + e.getMessage());

                            }
                            // xml response
                            /*JSONObject value = cameraInfoViewModel.convertXmlToJson(responseStr);
                            String modelName = value.optString("modelName");

                            Log.e(TAG, "onResponse code " + code + " modelName " + modelName);
                            CameraDetails setCameraDetails = new CameraDetails();
                            setCameraDetails.setRxStatus(code);
                            setCameraDetails.setModelName(modelName);
                            cameraInfoViewModel.cameraDetails.setValue(new Event<>(setCameraDetails));*/

                        } else {
                            digitalCameraInfoViewModel.cameraDetails.setValue(new Event<>(code));
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        digitalCameraInfoViewModel.cameraDetails.setValue(new Event<>(message));
                    }
                });

            } else {
                // Nightwave analog and opsin
                hideSpinner();
                if (cameraIpAddress.equals(WIFI_IPADDRESS) || cameraIpAddress.startsWith("10.0.0")
                    /*|| cameraIpAddress.startsWith("172.")|| cameraIpAddress.equals(WIFI_IPADDRESS_NWD) *//*|| cameraIpAddress.equals(WIFI_IPADDRESS_NEW)*/) {
                    navigateToLiveScreen();
                } else {
                    showDialog = ShowDialog.WIFI_CONNECTED_WITH_OTHER_DEVICE;
                    showDialog("", getString(R.string.wifi_connected_with_other_client), null);
                }
            }
        }
    }

    private void navigateToLiveScreen() {
        if (tooltipHelper != null) {
            tooltipHelper.dismissIfShowing();
        }
        viewModel.setConnectedSsidFromWiFiManager(connectedSsid);
        viewModel.updateCameraConnectionState(connectedSsid, WIFI_CONNECTED);
        Log.e(TAG, "connectToWiFi: setNavigateToNextScreen");
        homeViewModel.setNavigateToNextScreen(true);
        homeViewModel.setCameraSwitched(false);
        homeViewModel.setNwdCameraSelected(false);
        homeViewModel.setSelectedCamera("");
        homeViewModel.isCameraLongPressed = false;
        selectedCameraSSID = "";
        wifiRequestCount = 0;
        TCPCommunicationService.lastCommandSentTime = -1;
    }

    private boolean isAlreadyAdded = false;

    private void addWiFiSuggestion(String connectedSSID) {
        /*WIFI Suggestions*/
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            List<WifiNetworkSuggestion> networkSuggestions = mWifiManager.getNetworkSuggestions();
            networkSuggestions.forEach(wifiNetworkSuggestion -> {
                String ssid = wifiNetworkSuggestion.getSsid();
                Log.e(TAG, "addWiFiSuggestion: " + ssid);
                if (ssid != null && ssid.equalsIgnoreCase(connectedSSID)) {
                    isAlreadyAdded = true;
                }
            });
            if (!isAlreadyAdded) {
                addSuggestion(connectedSSID);
            }
        }
    }

    public void addSuggestion(String connectedSSID) {
        ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();
        String currentCameraPwd = connectedSSID.contains(FILTER_STRING6) ? cameraPasswordSettingViewModel.getOnDialogCameraPassword() : DEFAULT_CAMERA_PWD; // change pwd
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suggestions.add(new WifiNetworkSuggestion.Builder()
                    .setSsid(connectedSSID)
                    .setWpa2Passphrase(currentCameraPwd)
                    .setPriorityGroup(100)
                    .build());
        }
        final int status = mWifiManager.addNetworkSuggestions(suggestions);
        switch (status) {
            case STATUS_NETWORK_SUGGESTIONS_SUCCESS:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_SUCCESS");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID");
                break;
            case STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN:
                Log.e(TAG, "saveNetwork: STATUS_NETWORK_SUGGESTIONS_ERROR_RESTRICTED_BY_ADMIN");
                break;

        }
    }

    private void addNetwork(String connectedSSID) {
        ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();
        String currentCameraPwd = connectedSSID.contains(FILTER_STRING6) ? cameraPasswordSettingViewModel.getOnDialogCameraPassword() : DEFAULT_CAMERA_PWD; // change pwd
        suggestions.add(new WifiNetworkSuggestion.Builder()
                .setSsid(connectedSSID)
                .setWpa2Passphrase(currentCameraPwd)
                .build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(EXTRA_WIFI_NETWORK_LIST, suggestions);

            Intent intentAdd = new Intent(ACTION_WIFI_ADD_NETWORKS);
            intentAdd.putExtras(bundle);

            addWifiNetworkResultLauncher.launch(intentAdd);
        }
    }


    private void lockWifi() {
        wifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "SIONYX-Plexus");
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
            Log.e(TAG, "lockWifi: acquired");
        }
    }

    private void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            Log.e(TAG, "lockWifi: released");
        }
    }

    public void acquireWakeLockForStreaming() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerWakelock == null) {
            powerWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Plexus:CameraStreaming");
            powerWakelock.acquire(24 * 60 * 60 * 1000 /*24 hours*/);
        }
    }

    public void releasePowerWakeLock() {
        if (powerWakelock != null && powerWakelock.isHeld()) {
            powerWakelock.release();
            powerWakelock = null;
        }
    }

    private void displayUnableToConnect() {
        if (showDialog != ShowDialog.WIFI_UNABLE_TO_CONNECT_WITH_REQUESTED_WIFI) {
            showDialog = ShowDialog.WIFI_UNABLE_TO_CONNECT_WITH_REQUESTED_WIFI;
            showDialog("", getString(R.string.unable_scan), null);

            viewModel.updateCameraConnectionState(viewModel.getConnectedSsidFromWiFiManager(), WIFI_AVAILABLE);
            homeViewModel.setCameraSwitched(false);
            connectivityManager.bindProcessToNetwork(null);
            unregisterNetworkCallback();
            networkCapability = null;
        }

        if (selectedCameraSSID.contains(FILTER_STRING6)){
            if (cameraPasswordSettingViewModel.getCurrentDigitalWifiHistory() != null){
                digitalCameraInfoViewModel.updateCameraAutoConnect(selectedCameraSSID,0);
            }
        }

        if (homeViewModel.appBehaviour == HomeViewModel.AppBehaviour.FOREGROUND) {
            Log.e(TAG, "displayUnableToConnect: true");

        } else {
            Log.e(TAG, "displayUnableToConnect: false ");
        }
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideSpinner() {
        if (homeViewModel.hasShowProgressBar())
            homeViewModel.setHasShowProgressBar(false);
    }

    private void showSpinner() {
        mLopperHandler.post(() -> {
            if (!homeViewModel.hasShowProgressBar())
                homeViewModel.setHasShowProgressBar(true);
        });
    }

    private boolean isCamera(String connectedSSID) {
        return connectedSSID.contains(FILTER_STRING1)
                || connectedSSID.contains(FILTER_STRING2)
                || connectedSSID.contains(FILTER_STRING3)
                || connectedSSID.contains(FILTER_STRING4)
                || connectedSSID.contains(FILTER_STRING5)
                || connectedSSID.contains(FILTER_STRING6);
    }


    /**
     * this method request for location permission
     * second permission request after permission denial it shows explaination dialog to user
     */
    public void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        final List<String> missingPermissionsRationale = new ArrayList<>();
        if (isSDK10()) {
            Log.d(TAG, "checkPermissions: isSDK LESS THAN 10");
            for (final String permission : REQUIRED_SDK_PERMISSIONS) {
                final int result = ContextCompat.checkSelfPermission(this, permission);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            showDialog = ShowDialog.LOCATION_RATIONALE_DIALOG;
                            missingPermissionsRationale.add(permission);
                        } else {
                            missingPermissions.add(permission);
                        }
                    } else {
                        missingPermissions.add(permission);
                    }
                }
            }
        } else if (isSDK11()) {
            Log.d(TAG, "checkPermissions: isSDK 11");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_30) {
                final int result = ContextCompat.checkSelfPermission(this, permission);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (shouldShowRequestPermissionRationale(permission)) {
                            showDialog = ShowDialog.LOCATION_RATIONALE_DIALOG;
                            missingPermissionsRationale.add(permission);
                        } else {
                            missingPermissions.add(permission);
                        }
                    } else {
                        missingPermissions.add(permission);
                    }
                }
            }
        } else if (isSDK12()) {
            Log.d(TAG, "checkPermissions: isSDK 12");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_31) {
                checkPermission(permission, missingPermissionsRationale, missingPermissions);
            }
        } else if (isSDK13()) {
            Log.d(TAG, "checkPermissions: isSDK 13");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_33) {
                checkPermission(permission, missingPermissionsRationale, missingPermissions);
            }
        } else if (isSDK14()) {
            Log.d(TAG, "checkPermissions: isSDK 14");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_34) {
                checkPermission(permission, missingPermissionsRationale, missingPermissions);
            }
        } else if (isSDK15()) {
            Log.d(TAG, "checkPermissions: isSDK 15");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_35) {
                checkPermission(permission, missingPermissionsRationale, missingPermissions);
            }
        } else if (isSDK16AndAbove()) {
            Log.d(TAG, "checkPermissions: isSDK 16");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_36) {
                checkPermission(permission, missingPermissionsRationale, missingPermissions);
            }
        } else {
            Log.d(TAG, "checkPermissions: isSDK 16 Above");
            for (final String permission : REQUIRED_SDK_PERMISSIONS_36) {
                checkPermission(permission, missingPermissionsRationale, missingPermissions);
            }
        }
        if (showDialog == ShowDialog.LOCATION_RATIONALE_DIALOG) {
            rationalePermissionArray = missingPermissionsRationale.toArray(new String[0]);
            showDialog("", getString(R.string.permission_message_second), null);
        } else if (!missingPermissions.isEmpty()) {
            permissionArray = missingPermissions.toArray(new String[0]);
            showDialog = ShowDialog.LOCATION_EXPLANATION_DIALOG;
            showDialog("", getString(R.string.permission_message_first), null);
        } else {
            Log.e(TAG, "checkPermissions: Permission Granted");
            if (isSDK10()) {
                Log.e(TAG, "checkPermissions: Permission Granted isSDK10()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS, grantResults);
            } else if (isSDK11()) {
                Log.e(TAG, "checkPermissions: Permission Granted isSDK11()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS_30.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS_30, grantResults);
            } else if (isSDK12()) {
                Log.e(TAG, "checkPermissions: Permission Granted isSDK12()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS_31.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS_31, grantResults);
            } else if (isSDK13()) {
                Log.e(TAG, "checkPermissions: Permission Granted isSDK13()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS_33.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS_33, grantResults);
            } else if (isSDK14()) {
                Log.e(TAG, "checkPermissions: Permission Granted isSDK14()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS_34.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS_34, grantResults);
            } else if (isSDK15()){
                Log.e(TAG, "checkPermissions: Permission Granted isSDK15()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS_35.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS_35, grantResults);
            } else {
                Log.e(TAG, "checkPermissions: Permission Granted isSDK15 Above()");
                final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS_35.length];
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
                onRequestPermissionsResult(Constants.REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS_35, grantResults);
            }

        }
    }

    private void checkPermission(String permission, List<String> missingPermissionsRationale, List<String> missingPermissions) {
        final int result = ContextCompat.checkSelfPermission(this, permission);
        if (result != PackageManager.PERMISSION_GRANTED) {
            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    showDialog = ShowDialog.LOCATION_RATIONALE_DIALOG;
                    missingPermissionsRationale.add(permission);
                } else {
                    missingPermissions.add(permission);
                }
            } else if (permission.equals(Manifest.permission.BLUETOOTH_SCAN) || permission.equals(Manifest.permission.BLUETOOTH_CONNECT)) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    showDialog = ShowDialog.LOCATION_RATIONALE_DIALOG;
                    missingPermissionsRationale.add(permission);
                } else {
                    missingPermissions.add(permission);
                }
            } else {
                missingPermissions.add(permission);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQUEST_CODE_ASK_PERMISSIONS) {
            boolean isAllPermissionGranted = true;
            for (int index = permissions.length - 1; index >= 0; --index) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    // exit the app if one permission is not granted
                    isAllPermissionGranted = false;
                    Log.e(TAG, "onRequestPermissionsResult: " + permissions[index] + " " + grantResults[index]);
                }
            }
            if (isAllPermissionGranted) {
                Log.e(TAG, "onRequestPermissionsResult: Permission Granted");
                checkBluetoothIsSupported(false);
            } else {
                Log.e(TAG, "onRequestPermissionsResult: Permission Denied");
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_DENIED)
                    return;
                exitFromApp();
            }
        }
    }

    public void exitFromApp() {
        for (WiFiHistory wifiHistory : viewModel.getCombinedCameraList()) {
            String camera_ssid = wifiHistory.getCamera_ssid();
            viewModel.updateCameraConnectionState(camera_ssid, WIFI_NOT_CONNECTED);
        }
        viewModel.removeConnectionStateMonitor();
        viewModel.stopObserveBleOnOffState();
        compositeDisposable.dispose();
        finishAffinity();
        System.exit(0);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                showDialog = ShowDialog.BLUETOOTH_OFF_DIALOG;
                showDialog("", getString(R.string.enable_bluetooth_wifi_dialog_message), null);
            } else {
                startBluetoothDiscovery();
            }
        } else if (requestCode == REQUEST_ENABLE_GPS) {
            if (resultCode == RESULT_CANCELED) {
                showDialog = ShowDialog.BLUETOOTH_OFF_DIALOG;
                showDialog("", getString(R.string.enable_bluetooth_wifi_dialog_message), null);
            } else {
                checkGPSProvideState();
            }
        } else if (requestCode == WI_FI_PANEL_RESULT_CODE) {
            isCheckIfWiFiEnabled = mWifiManager.isWifiEnabled();
        } else if (requestCode == REQUEST_BATTERY_OPTIMIZATION) {
            Log.e(TAG, "onActivityResult: Battery Optimization Enabled");
            cameraViewModel.triggerTimerToUpdateStatusIcons(true);
        }
    }

    public void checkGpsOnOffState() {
        Log.e(TAG, "checkGpsState: ");
        locationRequest = new LocationRequest.Builder(5000)
                .setMinUpdateIntervalMillis(5000)
                .setPriority(PRIORITY_HIGH_ACCURACY)
                .build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                Log.e(TAG, "checkGPSProvideState called: ");
                checkGPSProvideState();
            } else {
                // Task failed with an exception
                Exception exception = task1.getException();
                if (exception instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_ENABLE_GPS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        sendEx.printStackTrace();
                        // Ignore the error.
                    }
                }
            }
        });
    }

    private void checkGPSProvideState() {
        try {
            if (mLocationManager == null)
                mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (mLocationManager.hasProvider(LocationManager.GPS_PROVIDER) && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    enableWiFiConnectionRequest(homeViewModel.getSelectedCamera());
                } else if (mLocationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
                        || mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                        || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    enableWiFiConnectionRequest(homeViewModel.getSelectedCamera());
                }
            } else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                enableWiFiConnectionRequest(homeViewModel.getSelectedCamera());
            } else if (mLocationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
                    || mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
                    || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                enableWiFiConnectionRequest(homeViewModel.getSelectedCamera());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (mLocationManager.hasProvider(LocationManager.GPS_PROVIDER) && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
                    }
                    mLocationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 5000, 0, this);
                } else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
                }
                requestLocation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.e(TAG, "requestLocation: ");
        Location lastKnownLocation;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER);
        } else {
            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (lastKnownLocation != null) {
            locationLatitude = lastKnownLocation.getLatitude();
            locationLongitude = lastKnownLocation.getLongitude();
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());

    }


    private final LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            if (mLastLocation != null) {
                locationLatitude = mLastLocation.getLatitude();
                locationLongitude = mLastLocation.getLongitude();
//                Log.e(TAG, "onLocationChanged: " + mLastLocation.getLatitude() + " / " + mLastLocation.getLongitude());
            }
        }
    };

    @Override
    public void onLocationChanged(Location location) {
        locationLatitude = location.getLatitude();
        locationLongitude = location.getLongitude();
//        Log.e(TAG, "onLocationChanged: " + location.getLatitude() + " " + location.getLongitude());
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.e(TAG, "onProviderEnabled: ");
        if (!isCheckIfWiFiEnabled) {
            navigateToWifiPanel();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.e(TAG, "onProviderDisabled: ");
    }

    private void navigateToWifiPanel() {
        hideSpinner();
        if (isSDK12()) {
            if (navigateToWiFiPanelLauncher == null) {
                createLauncherObject();
            }
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            if (navigateToWiFiPanelLauncher != null) {
                navigateToWiFiPanelLauncher.launch(panelIntent);
            }
        } else if (isSdk12AndAbove() || isSDK16AndAbove()) {
            Log.e(TAG, "navigateToWifiPanel: " + navigateToWiFiPanelLauncher);
            if (navigateToWiFiPanelLauncher == null) {
                createLauncherObject();
            }
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            navigateToWiFiPanelLauncher.launch(panelIntent);
        } else {
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            startActivityForResult(panelIntent, WI_FI_PANEL_RESULT_CODE);
        }
    }

    String dialogTag = "";

    public synchronized void showDialog(String title, String message, String release_notes) {
        if (homeViewModel != null && homeViewModel.appBehaviour == HomeViewModel.AppBehaviour.FOREGROUND) {
            NoticeDialogFragment dialog = new NoticeDialogFragment();
            if (homeViewModel != null && !homeViewModel.getFragmentManager().isDestroyed()) {
                dialog.setListener(MainActivity.this, homeViewModel);
                Bundle args = new Bundle();
                args.putString("message", message);

                    if (showDialog == ShowDialog.NONE) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "NONE";
                    }
                    if (showDialog == ShowDialog.WIFI_OFF_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_OFF_DIALOG";
                    }
                    if (showDialog == ShowDialog.BLUETOOTH_OFF_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "BLUETOOTH_OFF_DIALOG";
                    }
                    if (showDialog == ShowDialog.WIFI_CONNECTED_WITH_OTHER_DEVICE) {
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_CONNECTED_WITH_OTHER_DEVICE";
                    }
                    if (showDialog == ShowDialog.WIFI_DISCONNECT) {
                      /*  boolean isValue = args.getBoolean("hasShowCameraRebootLayout", true);
                        Log.e(TAG, "showDialog: _______VALUE " + isValue );
                        if (isValue){
                            dialog.dismiss();
                            showDialog = ShowDialog.NONE;
                        }*/
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        if (/*HomeViewModel.screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN*/fwMode != MODE_NONE) {
                            homeViewModel.hasStopFWUIProgress(true);// for this stop fw update progress ui
                            homeViewModel.setHasShowFwDialog(true);
                        }
                        /* for this while start recording during this time wifi disconnect stp record timer and delete the file*/
                        if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW) {
                            runOnUiThread(() -> cameraViewModel.hasSaveRecordedVideo());
                        }
                        dialogTag = "WIFI_DISCONNECT";
                        wifiRequestCount = 0;
                        homeViewModel.setSelectedCamera("");
                    }
                    if (showDialog == ShowDialog.SOCKET_FAILED) {
                        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                            rtspState = RTSP_NONE;
                        } else {
                            firmwareUpdateSequence.clear();
                            fwMode = MODE_NONE;
                        }
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "SOCKET_FAILED";
                    }

                    if (showDialog == ShowDialog.FW_UPDATE_AVAILABLE_DIALOG) {
                        args.putBoolean("isNotesIcon", true);
                        args.putString("release_notes", release_notes);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "FW_UPDATE_AVAILABLE_DIALOG";
                    }
                    if (showDialog == ShowDialog.WIFI_FW_UPDATE_AVAILABLE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("displayTitle", true);
                        args.putBoolean("hasShowSDCardButton", true);
                        args.putBoolean("hasShowSiOnyxAppButton", true);
                        args.putBoolean("disable_Ok_button", true);
                        args.putString("title", title);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("is_wifi_fw_update_dialog", true);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_FW_UPDATE_AVAILABLE_DIALOG";
                    }
                    if (showDialog == ShowDialog.FW_REBOOT_MODE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "FW_REBOOT_MODE_DIALOG";
                    }

                    if (showDialog == ShowDialog.FW_RECOVER_DIALOG_MESSAGE) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "FW_RECOVER_DIALOG_MESSAGE";
                    }
                    if (showDialog == ShowDialog.FW_RETRY_DIALOG_MESSAGE) {
                        firmwareUpdateSequence.clear();
                        fwMode = MODE_NONE;
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "FW_RETRY_DIALOG_MESSAGE";
                    }
                    if (showDialog == ShowDialog.POP_UP_DELETE_ITEM_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "POP_UP_DELETE_ITEM_DIALOG";
                    }
                    if (showDialog == ShowDialog.PACKET_ALL_ZERO_DIALOG) {
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                args.putBoolean("isNotesIcon", false);
                                args.putBoolean("disable_cancel_button", false);
                                args.putBoolean("isWarningIcon", true);
                                args.putBoolean("hasShowSaveButton", false);
                                args.putBoolean("hasShowSDCardButton", false);
                                args.putBoolean("hasShowSiOnyxAppButton", false);
                                args.putBoolean("disable_Ok_button", false);
                                args.putBoolean("is_wifi_fw_update_dialog", false);
                                args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                                args.putBoolean("hasShowSoftwareVersionLayout", false);
                                args.putBoolean("hasShowCameraRebootLayout", false);
                                args.putBoolean("hasShowPasswordResetLayout", false);
                                dialogTag = "PACKET_ALL_ZERO_DIALOG";
                                break;
                            case OPSIN:
                                args.putBoolean("isNotesIcon", false);
                                args.putBoolean("disable_cancel_button", true);
                                args.putBoolean("isWarningIcon", true);
                                args.putBoolean("hasShowSaveButton", false);
                                args.putBoolean("hasShowSDCardButton", false);
                                args.putBoolean("hasShowSiOnyxAppButton", false);
                                args.putBoolean("disable_Ok_button", false);
                                args.putBoolean("is_wifi_fw_update_dialog", false);
                                args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                                args.putBoolean("hasShowSoftwareVersionLayout", false);
                                args.putBoolean("hasShowCameraRebootLayout", false);
                                args.putBoolean("hasShowPasswordResetLayout", false);
                                dialogTag = "PACKET_ALL_ZERO_DIALOG";
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG, "Dialog for udp packets are zero Nightwave Digital");
                                args.putBoolean("isNotesIcon", false);
                                args.putBoolean("disable_cancel_button", false);
                                args.putBoolean("isWarningIcon", false);
                                args.putBoolean("hasShowSaveButton", false);
                                args.putBoolean("hasShowSDCardButton", false);
                                args.putBoolean("hasShowSiOnyxAppButton", false);
                                args.putBoolean("disable_Ok_button", false);
                                args.putBoolean("is_wifi_fw_update_dialog", false);
                                args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                                args.putBoolean("hasShowSoftwareVersionLayout", false);
                                args.putBoolean("hasShowCameraRebootLayout", false);
                                args.putBoolean("hasShowPasswordResetLayout", false);
                                dialogTag = "NONE";
                                break;
                        }
                    }

                    if (showDialog == ShowDialog.OPSIN_NOT_RESPONSE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_NOT_RESPONSE_DIALOG";
                    }

                    if (showDialog == ShowDialog.OPSIN_RECYCLE_THE_CAMERA) {
                        fwMode = MODE_NONE;
                        firmwareUpdateSequence.clear();
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_RECYCLE_THE_CAMERA";
                    }
                    if (showDialog == ShowDialog.OPSIN_BATTERY_CHARGE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_BATTERY_CHARGE_DIALOG";
                    }

                    if (showDialog == ShowDialog.USB_VIDEO_MODE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "USB_VIDEO_MODE_DIALOG";
                    }
                    if (showDialog == ShowDialog.WIFI_VIDEO_MODE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_VIDEO_MODE_DIALOG";
                    }
                    if (showDialog == ShowDialog.WIFI_TO_USB_VIDEO_MODE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_TO_USB_VIDEO_MODE_DIALOG";
                    }
                    if (showDialog == ShowDialog.LOCATION_EXPLANATION_DIALOG) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "LOCATION_EXPLANATION_DIALOG";
                    }
                    if (showDialog == ShowDialog.LOCATION_RATIONALE_DIALOG) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "LOCATION_RATIONALE_DIALOG";
                    }
                    if (showDialog == ShowDialog.SPECIAL_CHARACTER_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", true);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", true);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "SPECIAL_CHARACTER_DIALOG";
                    }
                    if (showDialog == ShowDialog.WIFI_SCAN_REQUEST) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_SCAN_REQUEST";
                    }
                    if (showDialog == ShowDialog.WIFI_UNABLE_TO_CONNECT_WITH_REQUESTED_WIFI) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "WIFI_UNABLE_TO_CONNECT_WITH_REQUESTED_WIFI";
                    }

                    if (showDialog == ShowDialog.CONFIRM_ERASE) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "CONFIRM_ERASE";
                    }
                    if (showDialog == ShowDialog.NIGHT_WAVE_CAMERA_RESPONSE_FAILED) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "NIGHT_WAVE_CAMERA_RESPONSE_FAILED";
                    }
                    if (showDialog == ShowDialog.POPUP_SOCKET_CLOSED) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "POPUP_SOCKET_CLOSED";

//                        if (tcpConnectionViewModel != null)
//                            tcpConnectionViewModel.disconnectSocket();
                    }
                    if (showDialog == ShowDialog.OPSIN_CAMERA_RECORD_IN_PROGRESS) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_CAMERA_RECORD_IN_PROGRESS";
                    }
                    if (showDialog == ShowDialog.OPSIN_CAMERA_STREAMING_TO_STOP_RECORD) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_CAMERA_STREAMING_TO_STOP_RECORD";
                    }

                    if (showDialog == ShowDialog.STORAGE_ALERT) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "STORAGE_ALERT";
                    }
                    if (showDialog == ShowDialog.OPSIN_MIC_COMMAND_FAILED) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_MIC_COMMAND_FAILED";
                    }
                    if (showDialog == ShowDialog.OPSIN_UTC_TIME_ZONE_DIALOG) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_MIC_COMMAND_FAILED";
                    }
                    if (showDialog == ShowDialog.OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", true);
                        args.putBoolean("hasShowSiOnyxAppButton", true);
                        args.putBoolean("disable_Ok_button", true);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE";
                    }
                    if (showDialog == ShowDialog.OPSIN_CUSTOMER_CONTACT_DIALOG_TO_UPDATE_FPGA_USING_SDCARD) {
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "OPSIN_CUSTOMER_CONTACT_DIALOG_TO_UPDATE_FPGA_USING_SDCARD";
//                        dialogTag = "OPSIN_CUSTOMER_CONTACT_FPGA_DIALOG";
                    }
                    if (showDialog == ShowDialog.SAVE_CAMERA_SETTINGS) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", true);
                        args.putString("title", title);
                        args.putBoolean("displayTitle", true);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "SAVE_CAMERA_SETTINGS";
                    }
                    if (showDialog == ShowDialog.DELETE_SAVE_CAMERA_SETTINGS_ALERT_DIALOG) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "DELETE_SAVE_CAMERA_SETTINGS_ALERT_DIALOG";
                    }
                    if (showDialog == ShowDialog.CAMERA_SD_CARD_RECORD_START_ALERT_DIALOG) {
                        args.putBoolean("disable_cancel_button", false);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "CAMERA_SD_CARD_RECORD_START_ALERT_DIALOG";
                    }
                    if (showDialog == ShowDialog.CAMERA_SD_CARD_STORAGE_FULL_ALERT_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putString("title", title);
                        args.putBoolean("displayTitle", true);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "CAMERA_SD_CARD_STORAGE_FULL_ALERT_DIALOG";
                    }
                    if (showDialog == ShowDialog.SD_CARD_RECORD_AND_STREAMING_STATE_CHECK_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", true);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "SD_CARD_RECORD_AND_STREAMING_STATE_CHECK_DIALOG";
                    }
                    if (showDialog == ShowDialog.CAMERA_CLOSURE_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "CAMERA_CLOSURE_DIALOG";
                    }
                    if (showDialog == ShowDialog.APP_UPDATE_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "APP_UPDATE_DIALOG";
                    }
                    if (showDialog == ShowDialog.CAMERA_LOST_WHILE_SWITCHED_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "CAMERA_LOST_WHILE_SWITCHED_DIALOG";
                    }

                    if (showDialog == ShowDialog.NWD_SOFTWARE_UPDATE_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", true);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "NWD_SOFTWARE_UPDATE_DIALOG";
                    }
                    if (showDialog == ShowDialog.NWD_CAMERA_REBOOT_DIALOG) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", true);
                        args.putBoolean("hasShowPasswordResetLayout", false);
                        dialogTag = "NWD_CAMERA_REBOOT_DIALOG";
                    }

                    if (showDialog == ShowDialog.NWD_CAMERA_PASSWORD_RESET) {
                        args.putBoolean("disable_cancel_button", true);
                        args.putBoolean("isNotesIcon", false);
                        args.putBoolean("isWarningIcon", false);
                        args.putBoolean("hasShowSaveButton", false);
                        args.putBoolean("hasShowSDCardButton", false);
                        args.putBoolean("hasShowSiOnyxAppButton", false);
                        args.putBoolean("disable_Ok_button", false);
                        args.putBoolean("is_wifi_fw_update_dialog", false);
                        args.putBoolean("hasShowSaveCameraSettingsLayout", false);
                        args.putBoolean("hasShowSoftwareVersionLayout", false);
                        args.putBoolean("hasShowCameraRebootLayout", false);
                        args.putBoolean("hasShowPasswordResetLayout", true);
                        dialogTag = "NWD_CAMERA_PASSWORD_RESET";
                    }

                    dialog.setArguments(args);
                    dialog.setCancelable(false);
                    if (!hasAlreadyAddedInDialogTag) {
                        runOnUiThread(() -> {
                            if (!homeViewModel.getNoticeDialogFragments().isEmpty()) {
                                boolean isTagAvailable = false;
                                NoticeDialogFragment dialogFragmentT0Show = null;
                                int pos = -1;
                                for (int i = 0; i < homeViewModel.getNoticeDialogFragments().size(); i++) {
                                    NoticeDialogFragment dialogFragment = homeViewModel.getNoticeDialogFragments().get(i);
                                    String tag = dialogFragment.getTag() != null ? dialogFragment.getTag() : null;
                                    Log.e(TAG, "showDialog: " + tag + " haha");
                                    if (tag != null && tag.equalsIgnoreCase(dialogTag)) {
                                        isTagAvailable = true;
                                        dialogFragmentT0Show = dialogFragment;
                                        pos = i;
                                    }
                                }
                                try {
                                    if (isTagAvailable) {
                                        Log.e(TAG, "showDialog: " + isTagAvailable + " haha ");
                                        dialogFragmentT0Show.dismiss();
                                        homeViewModel.getNoticeDialogFragments().remove(pos);
                                        dialog.show(homeViewModel.getFragmentManager(), dialogTag);
                                        homeViewModel.getNoticeDialogFragments().add(dialog);
                                    } else {
                                        dialog.show(homeViewModel.getFragmentManager(), dialogTag);
                                        homeViewModel.getNoticeDialogFragments().add(dialog);
                                        Log.e(TAG, "showDialog: add tag1");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                dialog.show(homeViewModel.getFragmentManager(), dialogTag);
                                homeViewModel.getNoticeDialogFragments().add(dialog);
                                Log.e(TAG, "showDialog: add tag");
                            }
                        });

                }

                // for this condition while rotate the device again add tag to be avoid
                hasAlreadyAddedInDialogTag = dialog.getShowsDialog() && dialogTag.equals(ShowDialog.LOCATION_EXPLANATION_DIALOG.name())
                        || dialogTag.equals(ShowDialog.LOCATION_RATIONALE_DIALOG.name())
                        || dialogTag.equals(ShowDialog.SPECIAL_CHARACTER_DIALOG.name())
                        || dialogTag.equals(ShowDialog.WIFI_CONNECTED_WITH_OTHER_DEVICE.name())
                        || dialogTag.equals(ShowDialog.POPUP_SOCKET_CLOSED.name())
                        || dialogTag.equals(ShowDialog.WIFI_VIDEO_MODE_DIALOG.name())
                        || dialogTag.equals(ShowDialog.WIFI_TO_USB_VIDEO_MODE_DIALOG.name())
                        || dialogTag.equals(ShowDialog.FW_UPDATE_AVAILABLE_DIALOG.name())
                        || dialogTag.equals(ShowDialog.SAVE_CAMERA_SETTINGS.name());
            }
        } else {
            Log.e(TAG, "showDialog: on background");
            showDialog = ShowDialog.NONE;
        }
    }

    // if tooltip is visible dismiss and also update preference value
    private void dismissToolTip(){
        boolean isTooltipShown = toolTipSharedPreference.getBoolean("isToolTipShown", false);
        Log.d(TAG, "dismissToolTip: called " + isTooltipShown);
        if (!isTooltipShown){
            tooltipHelper.dismissTooltip();
        }

    }

    private void startTimer() {
        Log.e(TAG, "Timer started");
        dismissToolTip();
        isTimerStarted = true;
        showDialog = MainActivity.ShowDialog.NWD_SOFTWARE_UPDATE_DIALOG;
        showDialog("", "", null);
        homeViewModel.startTimer(); // To start 4sec timer
    }

    private void insertOrUpdateNWD(String nwdFwVersion, String ssid) {
        Log.e(TAG, "insertOrUpdateNWD: " + ssid);
        digitalCameraInfoViewModel.checkNWDSsidIsExit(ssid).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<NightwaveDigitalWiFiHistory>() {
            @Override
            public void onSubscribe(Disposable d) {
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(NightwaveDigitalWiFiHistory nightwaveDigitalWiFiHistory) {
                try {
                    if (nightwaveDigitalWiFiHistory != null) {
                        String connectedSSSID = getWiFiSSID(mWifiManager);
                        String camera_ssid = nightwaveDigitalWiFiHistory.getCamera_ssid();
                        Log.e(TAG, "onSuccess: " + connectedSSSID + " " + camera_ssid + cameraPasswordSettingViewModel.getOnDialogCameraPassword());
                        if (connectedSSSID.equals(camera_ssid)) {
                            long date = nightwaveDigitalWiFiHistory.getLast_popup_displayed_date();
                            digitalCameraInfoViewModel.updateCameraFirmwareVersion(ssid,nwdFwVersion);

                            if (cameraPasswordSettingViewModel.getOnDialogCameraPassword() != null)
                                digitalCameraInfoViewModel.updateCameraPassword(ssid, cameraPasswordSettingViewModel.getOnDialogCameraPassword());

                            Log.e(TAG, "insertOrUpdateNWD: auto_connected " + nightwaveDigitalWiFiHistory.getIs_auto_connected() + " last_date " + date + " pwd_DB : " + nightwaveDigitalWiFiHistory.getCamera_password());
                            Thread.sleep(300);

                            if (date > 0L) {
                                Date lastDate = new Date(date);
                                Date today = new Date();

                                long diffInMillis = today.getTime() - lastDate.getTime();
                                long daysDiff = TimeUnit.MILLISECONDS.toDays(diffInMillis);

                                Log.i(TAG, "showSoftwareUpdateDialog diffDays = " + daysDiff);
                                Log.i(TAG, "isCameraSelectedForLive " + homeViewModel.isNwdCameraPressed() + " isCameraLongPressed " + homeViewModel.isCameraLongPressed);

                                hideSpinner();
                                if (homeViewModel.isNwdCameraPressed()) {
                                    if (daysDiff >= FW_POPUP_VALID_DAYS) {
                                        long currentDate = System.currentTimeMillis();
                                        digitalCameraInfoViewModel.updateLastFWDisplayDate(ssid, currentDate);
                                        if (!isTimerStarted)
                                            startTimer(); // show dialog + update date
                                    } else {
                                            navigateToLiveScreen(); // if not navigate to live
                                    }
                                } else {
                                    if (homeViewModel.isCameraLongPressed) {
                                        CameraMenuBottomDialog bottomSheetDialog = new CameraMenuBottomDialog();
                                        bottomSheetDialog.show(getSupportFragmentManager(), "CameraMenu");
                                        homeViewModel.isCameraLongPressed = false;
                                        homeViewModel.setNwdCameraSelected(false);
                                    } else {
                                       /* if (viewModel.isAutoConnectionEnabled()) {
                                            navigateToLiveScreen(); // navigate to live when auto connect in enabled
                                        }*/
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    hideSpinner();
                    Log.e(TAG, "insertOrUpdateNWD: Exception " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onError(Throwable e) {
                hideSpinner();
                camera_password = cameraPasswordSettingViewModel.getOnDialogCameraPassword() != null ? cameraPasswordSettingViewModel.getOnDialogCameraPassword() : "";
                Log.i(TAG, "insertOrUpdateNWD: insertData SSID " + ssid +
                            "AUTO_CONNECT : " + IS_AUTO_CONNECTED +
                            "FW_VERSION " + nwdFwVersion +
                            "PASSWORD " + camera_password);
                long popup_last_displayed_date = System.currentTimeMillis();
                NightwaveDigitalWiFiHistory wiFiHistory = new NightwaveDigitalWiFiHistory(ssid, ssid, nwdFwVersion, popup_last_displayed_date,
                        camera_password, IS_AUTO_CONNECTED);
                digitalCameraInfoViewModel.insertNWDCamera(wiFiHistory);
                startTimer();
            }
        });
    }

    public void dismissNoticeDialogFragment() {
        Log.e(TAG, "dismissNoticeDialogFragment: called");
        for (int i = 0; i < homeViewModel.getNoticeDialogFragments().size(); i++) {
            NoticeDialogFragment dialogFragment = homeViewModel.getNoticeDialogFragments().get(i);
            if (dialogFragment.isVisible()) {
                String tag = dialogFragment.getTag() != null ? dialogFragment.getTag() : null;
                Log.e(TAG, "showDialog: visible " + tag);
                if (tag != null) {
                    Log.e(TAG, "showDialog: dismissed " + tag);
                }
                runOnUiThread(dialogFragment::dismiss);

            } else {
                Log.e(TAG, "showDialog: not visible ");
            }
        }
        showDialog = ShowDialog.NONE;
        homeViewModel.getNoticeDialogFragments().clear();
    }


    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        Log.e(TAG, "onDialogPositiveClick: " + dialog.getTag());
        dialog.dismiss();
        hasAlreadyAddedInDialogTag = false;
        CameraInfoTabLayoutDialog cameraInfoTabLayoutDialog = new CameraInfoTabLayoutDialog();
        CameraSettingsTabLayoutDialog cameraSettingsTabLayoutDialog = new CameraSettingsTabLayoutDialog();
        if (cameraInfoTabLayoutDialog.isVisible()) {
            cameraInfoTabLayoutDialog.dismiss();
        }
        if (cameraSettingsTabLayoutDialog.isVisible()) {
            cameraSettingsTabLayoutDialog.dismiss();
        }

        if (dialog.getTag() != null) {
            switch (dialog.getTag()) {
                case "WIFI_OFF_DIALOG":
                    showDialog = ShowDialog.NONE;
                    /* for this when firmware update to disable wifi on mobile device*/
                    if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN)
                        homeViewModel.setWifiDialogPositiveButtonClick(true); // for this ui show home screen
                    else exitFromApp();
                    break;
                case "BLUETOOTH_OFF_DIALOG":
                    showDialog = ShowDialog.NONE;
                    //cameraViewModel.analyticsButtonState = CameraViewModel.AnalyticsButtonState.Analytics_STOPPED;
                    recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    exitFromApp();
                    break;
                case "SOCKET_FAILED":
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                        if (rtspState == RTSP_CONNECTED)
                            tcpConnectionViewModel.stopServiceDigital();
                        dialog.dismiss();
                        handlePositiveButton();
                    } else {
                        tcpConnectionViewModel.resetSocketState();
                        handlePositiveButton();
                    }
                    showDialog = ShowDialog.NONE;
                    break;
                case "WIFI_DISCONNECT":
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                        handlePositiveButton();
                        homeViewModel.setWifiDialogPositiveButtonClick(screenType != HomeViewModel.ScreenType.FW_UPDATE_SCREEN);
                    } else {
                        tcpConnectionViewModel.resetSocketState();

                        if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                            fwMode = MODE_NONE;
                            homeViewModel.setWifiDialogPositiveButtonClick(false);
                        } else {
                            homeViewModel.setWifiDialogPositiveButtonClick(true);
                        }
                        /*Retry Connection*/
                        homeViewModel.onSelectCamera(viewModel.getConnectedSsidFromWiFiManager());
                    }
                    showDialog = ShowDialog.NONE;
                    homeViewModel.setHasShowFwDialog(false);
                    //cameraViewModel.analyticsButtonState = CameraViewModel.AnalyticsButtonState.Analytics_STOPPED;
                    recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    break;
                case "FW_UPDATE_AVAILABLE_DIALOG":
                    switch (currentCameraSsid) {
                        case OPSIN:

                            if (!firmwareUpdateSequence.isEmpty()) {
                                showDialog = ShowDialog.NONE;
                                showSpinner();
                                try {
                                    int riscvComparisonResult = -1;
                                    String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
                                    if (containsFourthDigit(cameraRiscv)) {
                                        String removedRiscv = removeFourthDigit(cameraRiscv);
                                        riscvComparisonResult = compareVersions(removedRiscv, OPSIN_RECORDING_STATUS_SUPPORTS_FROM);
                                    } else {
                                        riscvComparisonResult = compareVersions(cameraRiscv, OPSIN_RECORDING_STATUS_SUPPORTS_FROM);
                                    }
                                    if (riscvComparisonResult >= 0) {
                                        tcpConnectionViewModel.getOpsinRecordingStatus();
                                        tcpConnectionViewModel.observeOpsinCameraRecordingState().observe(this, getOpsinCameraRecordingState);
                                        tcpConnectionViewModel.observeOpsinCameraStreamingState().observe(this, getOpsinCameraStreamingState);
                                    } else {
                                        Log.d(TAG, "onDialogPositiveClick: skipped record status");
                                        getBatteryInfoBeforeStartFwUpdate();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            break;
                        case NIGHTWAVE:
                            //navigate from live screen to firmware update screen
                            if (!HomeViewModel.isGoldenUpdate) { // No golden and skip golden user navigate to live
                                tcpConnectionViewModel.stopLiveView();
                                homeViewModel.getNavController().navigate(R.id.analogFirmwareUpdateFragment);
                            }
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG, "FW Update available Dialog : NightwaveDigital");
                            break;
                    }
                    break;
//                case "WIFI_FW_UPDATE_AVAILABLE_DIALOG":
//                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
//                        homeViewModel.startOpsinWiFiFirmwareUpgradeOnlyIfWiFiVersionISNull(true);
//                    }
//                    break;
                case "FW_RECOVER_DIALOG_MESSAGE":
                    homeViewModel.startFirmwareRecoverMode(true);
                    showDialog = ShowDialog.NONE;
                    break;
                case "FW_REBOOT_MODE_DIALOG":
                    if (screenType.equals(FW_UPDATE_SCREEN) && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                        fwMode = MODE_NONE;
                        tcpConnectionViewModel.disconnectSocket();
                        homeViewModel.hideFwScreenAndShowHomeScreen(true);
                        screenType = HomeViewModel.ScreenType.HOME;
                    }
                    showDialog = ShowDialog.NONE;
                    homeViewModel.hasShowProgressBar(false);
                    break;
                case "FW_RETRY_DIALOG_MESSAGE":
                    showDialog = ShowDialog.NONE;
                    if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                        fwMode = MODE_NONE;
                        homeViewModel.hasFWUpdateFailed(true);/* for this firmware update failed*/
                        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) && !HomeViewModel.isGoldenUpdate) {
                            cameraInfoViewModel._isFwRetryDialogInLive.postValue(new com.sionyx.plexus.utils.Event<>(1)); // For Analog update and all screens are relocated to live setting page
                        }
                    }
                    homeViewModel.setHasShowFwDialog(false);
                    break;
                case "WIFI_CONNECTED_WITH_OTHER_DEVICE":
                    showDialog = ShowDialog.NONE;
                    homeViewModel.hasShowProgressBar(false);
                    break;
                case "POP_UP_DELETE_ITEM_DIALOG":
                    homeViewModel.hasDeleteCamera(true);
                    showDialog = ShowDialog.NONE;
                    break;
                case "PACKET_ALL_ZERO_DIALOG":
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE))
                        cameraViewModel.hasAllPacketZero();
                    showDialog = ShowDialog.NONE;
                    break;
                case "OPSIN_NOT_RESPONSE_DIALOG":
                    if (dialog.getArguments().getString("message") != null
                            && dialog.getArguments().getString("message").equals(getString(R.string.socket_disconnected_messgae))
                            || dialog.getArguments().getString("message").equals(getString(R.string.factory_reset_completed))) {
                        handlePositiveButton();
                        showDialog = ShowDialog.NONE;
                    } else {
                        showDialog = ShowDialog.NONE;
                    }
                    break;
                case "OPSIN_RECYCLE_THE_CAMERA":
                    if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                        fwMode = MODE_NONE;
                        homeViewModel.setWifiDialogPositiveButtonClick(false);
                    } else {
                        homeViewModel.setWifiDialogPositiveButtonClick(true);
                    }
                    homeViewModel.setHasShowFwDialog(false);
                    break;
                case "OPSIN_BATTERY_CHARGE_DIALOG":
                    showDialog = ShowDialog.NONE;
                    hideSpinner();
                    if (tcpConnectionViewModel != null) {
                        Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                        if (value != null && value == Constants.STATE_CONNECTED) {
                            tcpConnectionViewModel.disconnectSocket();
                            tcpConnectionViewModel.isSocketConnected().removeObservers(this);
                        }
                    }
                    break;
                case "WIFI_VIDEO_MODE_DIALOG":
                    if (screenType == POP_UP_SETTINGS_SCREEN)
                        popUpCameraSettingsViewModel.hasShowVideoModeWifiDialog(true);//dialog show and select yes switch from UVC to Wifi mode
                    else
                        cameraViewModel.hasShowVideoModeWifiDialog(true);//dialog show and select yes switch from UVC to Wifi mode
                    showDialog = ShowDialog.NONE;
                    break;
                case "WIFI_TO_USB_VIDEO_MODE_DIALOG":
                    if (screenType == POP_UP_SETTINGS_SCREEN)
                        popUpCameraSettingsViewModel.hasShowVideoModeUSBDialog(true);//dialog show and select yes switch from Wifi to USB mode
                    else
                        cameraViewModel.hasShowVideoModeUSBDialog(true);//dialog show and select yes switch from Wifi to USB mode
                    showDialog = ShowDialog.NONE;
                    break;
                case "LOCATION_EXPLANATION_DIALOG":
                    Log.e(TAG, "onDialogPositiveClick: permissionArray " + permissionArray.length);
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(MainActivity.this, permissionArray, Constants.REQUEST_CODE_ASK_PERMISSIONS);
                    showDialog = ShowDialog.NONE;
                    break;
                case "LOCATION_RATIONALE_DIALOG":
                    Log.e(TAG, "onDialogPositiveClick: rationalePermissionArray " + rationalePermissionArray.length);
                    boolean isPrompted = ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION);
                    if (isPrompted) {
                        // The requestPermissions() method has previously prompted for this permission
                        // You may want to handle this case differently, such as providing additional rationale
                        showToast(getString(R.string.enable_precise_location_from_location_permission));
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // For Android 12 (API level 31) and above
                            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                        } else {
                            // For versions below Android 12
                            intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                        }
                        startActivity(intent);
                    } else {
                        // The requestPermissions() method has not previously prompted for this permission
                        // Proceed with requesting permission
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(MainActivity.this, rationalePermissionArray, Constants.REQUEST_CODE_ASK_PERMISSIONS);
                        showDialog = ShowDialog.NONE;
                    }
                    break;
                case "WIFI_SCAN_REQUEST":
                    dialog.dismiss();
                    homeViewModel.setSettingScreenNavigated(true);
                    wifiRequestCount = 0;
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                    showDialog = ShowDialog.NONE;
                    break;
                case "WIFI_UNABLE_TO_CONNECT_WITH_REQUESTED_WIFI":
                case "CAMERA_CLOSURE_DIALOG":
                    dialog.dismiss();
                    showDialog = ShowDialog.NONE;
                    break;
                case "NIGHT_WAVE_CAMERA_RESPONSE_FAILED":
                    dialog.dismiss();
                    homeViewModel.setWifiDialogPositiveButtonClick(false);
                    showDialog = ShowDialog.NONE;
                    break;
                case "POPUP_SOCKET_CLOSED":
                    if (homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LOGIN_SCREEN && homeViewModel.currentScreen != HomeViewModel.CurrentScreen.LAUNCHER_SPLASH_SCREEN) {
                        dialog.dismiss();
                        popUpCameraSettingsViewModel.setSocketReconnectCalled(true);
                        tcpConnectionViewModel.connectSocket();
                        showDialog = ShowDialog.NONE;
                    }
                    break;
                case "OPSIN_CAMERA_RECORD_IN_PROGRESS":
                    dialog.dismiss();
                    homeViewModel.hasShowOpsinRecordInProgressDialog(true);
                    showDialog = ShowDialog.NONE;
                    break;
                case "OPSIN_CAMERA_STREAMING_TO_STOP_RECORD":
                    dialog.dismiss();
                    homeViewModel.hasShowOpsinStreamStopRecordDialog(true);
                    showDialog = ShowDialog.NONE;
                    break;
                case "OPSIN_MIC_COMMAND_FAILED":
                case "OPSIN_UTC_TIME_ZONE_DIALOG":
                case "CAMERA_SD_CARD_STORAGE_FULL_ALERT_DIALOG":
                case "SD_CARD_RECORD_AND_STREAMING_STATE_CHECK_DIALOG":
                    dialog.dismiss();
                    showDialog = ShowDialog.NONE;
                    break;
//                case "OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE":
//                    firmwareUpdateSequence.clear();//If Use select SDcard update
//                    showOpsinFpgaContactCustomerDialog();
//                    break;
                case "OPSIN_CUSTOMER_CONTACT_DIALOG_TO_UPDATE_FPGA_USING_SDCARD":
                    homeViewModel.setIsFpgaUpgradeAvailable(false);
                    dialog.dismiss();
                    showDialog = ShowDialog.NONE;
                    hideSpinner();
                    break;
                case "DELETE_SAVE_CAMERA_SETTINGS_ALERT_DIALOG":
                    presetsViewModel.hasDeletePreset(true);
                    dialog.dismiss();
                    showDialog = ShowDialog.NONE;
                    break;
                case "CAMERA_SD_CARD_RECORD_START_ALERT_DIALOG":
                    cameraViewModel.hasStopLiveStreamAndStartSdCardRecording(true);
                    dialog.dismiss();
                    showDialog = ShowDialog.NONE;
                    break;
                case "CONFIRM_ERASE":
                    dialog.dismiss();
                    if (homeViewModel.tabPos == 0) {
                        homeViewModel.hasEraseMedia(true);

                    } else if (homeViewModel.tabPos == 1) {
                        homeViewModel.hasErasePhoto(true);
                    } else {
                        homeViewModel.hasEraseVideo(true);
                    }

                    if (homeViewModel.isDeleteRecording) {
                        if (homeViewModel.tabPos == 0) {
                            homeViewModel.hasDeleteRecordedVideo(true);
                        }
                        if (homeViewModel.tabPos == 1) {
                            homeViewModel.hasDeleteRecordedVideo(true);
                        }
                        if (homeViewModel.tabPos == 2) {
                            homeViewModel.hasDeleteRecordedVideo(true);
                        }
                    }

                    if (itemClick) {
                        if (galleryAllViewModel.isCurrentTab == 0) {
                            galleryAllViewModel.hasDeleteRecordedVideo(true);
                            itemClick = false;
                        }
                        if (galleryAllViewModel.isCurrentTab == 1) {
                            galleryAllViewModel.hasDeleteRecordedVideo(true);
                            itemClick = false;
                        }
                        if (galleryAllViewModel.isCurrentTab == 2) {
                            galleryAllViewModel.hasDeleteRecordedVideo(true);
                            itemClick = false;
                        }
                    }

                    //
                    /*homeViewModel.hasDeleteRecordedVideo(true);*/
                    galleryAllViewModel.hasEraseLiveMedia(true);
                    galleryAllViewModel.hasEraseLiveVideoMedia(true);
                    galleryAllViewModel.hasEraseLivePhotoMedia(true);
                    showDialog = ShowDialog.NONE;
                    break;
                case "APP_UPDATE_DIALOG":
                    dialog.dismiss();
                    cameraInfoViewModel.setIsFwAvailableDialogDismissInLive(1);
                    showDialog = ShowDialog.NONE;
                    break;
                case "CAMERA_LOST_WHILE_SWITCHED_DIALOG":
                case "NWD_CAMERA_REBOOT_DIALOG":
                    dialog.dismiss();
                    showDialog = ShowDialog.NONE;
                    handlePositiveButton();
                    break;
            }
        }
    }

    private void handlePositiveButton() {
        switch (homeViewModel.currentScreen) {
            case NONE:
                Log.e(TAG, "handlePositiveButton: NONE");
                break;
            case HOME:
                Log.e(TAG, "handlePositiveButton: HOME");
                homeViewModel.hasShowProgressBar(false);
                break;
            case LIVE_VIEW:
                Log.e(TAG, "handlePositiveButton: LIVE");
                isAutoConnectRequest = false;
                viewModel.setSsidToRequestWiFiConnection("");
                backToHome(false);
                break;
            case FIRMWARE_UPDATE:
                Log.e(TAG, "handlePositiveButton: FIRMWARE_UPDATE");
                backToHome(false);
                break;
            case CAMERA_INFO_DIALOG_SCREEN:
            case CAMERA_SETTINGS_DIALOG_SCREEN:
                isAutoConnectRequest = false;
                viewModel.setSsidToRequestWiFiConnection("");
                Log.e(TAG, "handlePositiveButton: CAMERA_SETTINGS");
                backToHome(true);
                break;
            case CHANGE_PASSWORD_SCREEN:
                Log.e(TAG, "handlePositiveButton: CHANGE_PASSWORD_SCREEN");
                backToHome(false);
                break;
            default:
                Log.e(TAG, "DEFAULT");
                break;
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
        hasAlreadyAddedInDialogTag = false;
        hideSpinner();
        switch (showDialog) {
            case LOCATION_EXPLANATION_DIALOG:
            case LOCATION_RATIONALE_DIALOG:
            case WIFI_OFF_DIALOG:
            case BLUETOOTH_OFF_DIALOG:
                exitFromApp();
                break;
            case WIFI_DISCONNECT:
                tcpConnectionViewModel.resetSocketState();
                showDialog = ShowDialog.NONE;
                handlePositiveButton();
                homeViewModel.setHasShowFwDialog(false);
                homeViewModel.setWifiDialogPositiveButtonClick(screenType != HomeViewModel.ScreenType.FW_UPDATE_SCREEN); // for this show fw update show all version dialog ui
                break;
            case FW_RETRY_DIALOG_MESSAGE:
                showDialog = ShowDialog.NONE;
                fwMode = MODE_NONE;
                homeViewModel.setHasShowFwDialog(false);
                if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                    homeViewModel.hasFWUpdateFailed(false); /* for this firmware update failed*/
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) && !HomeViewModel.isGoldenUpdate) {
                        cameraInfoViewModel._isFwRetryDialogInLive.postValue(new com.sionyx.plexus.utils.Event<>(2));
                    }
                }
                break;
            case FW_UPDATE_AVAILABLE_DIALOG:
                homeViewModel.setHasShowProgressBar(false);
                homeViewModel.setSelectedCamera("");
                showDialog = ShowDialog.NONE;
                homeViewModel.startFirmwareUpdate(0);
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)  && !HomeViewModel.isGoldenUpdate) {
                    cameraInfoViewModel.setIsFwAvailableDialogDismissInLive(1);
                }
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN))
                    removeStreamingStateObservers();
                break;
//            case WIFI_FW_UPDATE_AVAILABLE_DIALOG:
//                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
//                    homeViewModel.startOpsinWiFiFirmwareUpgradeOnlyIfWiFiVersionISNull(false);
//                }
//                break;
            case SAVE_CAMERA_SETTINGS:
            case POP_UP_DELETE_ITEM_DIALOG:
            case PACKET_ALL_ZERO_DIALOG:
            case USB_VIDEO_MODE_DIALOG:
                showDialog = ShowDialog.NONE;
                break;
            case WIFI_VIDEO_MODE_DIALOG:
                if (screenType == POP_UP_SETTINGS_SCREEN)
                    popUpCameraSettingsViewModel.hasShowVideoModeWifiDialog(false); // simply dialog close and revert button to USB mode
                else
                    cameraViewModel.hasShowVideoModeWifiDialog(false); // simply dialog close and revert button to USB mode
                showDialog = ShowDialog.NONE;
                break;
            case WIFI_TO_USB_VIDEO_MODE_DIALOG:
                if (screenType == POP_UP_SETTINGS_SCREEN)
                    popUpCameraSettingsViewModel.hasShowVideoModeUSBDialog(false);//dialog show and select yes switch from Wifi to USB mode
                else
                    cameraViewModel.hasShowVideoModeUSBDialog(false); // simply dialog close and revert button to WIFI mode
                showDialog = ShowDialog.NONE;
                break;
            case CONFIRM_ERASE:
                break;
            case POPUP_SOCKET_CLOSED:
                if (tcpConnectionViewModel != null)
                    tcpConnectionViewModel.disconnectSocket();
                homeViewModel.onPopUpViewCancel(); // for this cancel popup window and show home screen
                showDialog = ShowDialog.NONE;
                break;
            case OPSIN_CAMERA_RECORD_IN_PROGRESS:
                dialog.dismiss();
                homeViewModel.hasShowOpsinRecordInProgressDialog(false);
                showDialog = ShowDialog.NONE;
                break;
            case CAMERA_SD_CARD_RECORD_START_ALERT_DIALOG:
                dialog.dismiss();
                cameraViewModel.hasStopLiveStreamAndStartSdCardRecording(false);
                showDialog = ShowDialog.NONE;
                break;
            case DELETE_SAVE_CAMERA_SETTINGS_ALERT_DIALOG:
                dialog.dismiss();
                presetsViewModel.hasDeletePreset(false);
                showDialog = ShowDialog.NONE;
                break;
//            case OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE:
//                homeViewModel.setIsFpgaUpgradeAvailable(false);
//                tcpConnectionViewModel.getBatteryInfo();//If user select App Update
//                break;
        }
    }

    private void displayDialogToDecideFpgaUpdateUsingSDCardOrApp() {
        showDialog = MainActivity.ShowDialog.OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE;
        showDialog("", getString(R.string.decide_sdcard_or_app_to_update_fpga), null);
    }

    private void showOpsinFpgaContactCustomerDialog() {
        showDialog = MainActivity.ShowDialog.OPSIN_CUSTOMER_CONTACT_DIALOG_TO_UPDATE_FPGA_USING_SDCARD;
        showDialog("", getString(R.string.contact_customer_to_upgrade_fpga), null);
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog) {
        dialog.dismiss();
        if (Objects.requireNonNull(showDialog) == ShowDialog.FW_UPDATE_AVAILABLE_DIALOG) {
            showDialog = ShowDialog.NONE;
            homeViewModel.startFirmwareUpdate(1);
        }
    }

    @Override
    public void onDialogSaveClick(DialogFragment dialog, String cameraName, boolean isSpecialCharacterLayout) {
        if (isSpecialCharacterLayout) {
            if (dialog.getTag() != null && dialog.getTag().equals("SPECIAL_CHARACTER_DIALOG")) {
                if (cameraName != null && !cameraName.isEmpty()) {
                    dialog.dismiss();
                    hasAlreadyAddedInDialogTag = false;
                    if (showDialog == ShowDialog.SPECIAL_CHARACTER_DIALOG) {
                        showDialog = ShowDialog.NONE;
                        homeViewModel.hasUpdateCameraName(cameraName);
                    }
                }
            }
        } else {
            if (dialog.getTag() != null && dialog.getTag().equals("SAVE_CAMERA_SETTINGS")) {
                // here cameraName as presetName
                if (cameraName != null && !cameraName.isEmpty()) {
                    dialog.dismiss();
                    hasAlreadyAddedInDialogTag = false;
                    if (showDialog == ShowDialog.SAVE_CAMERA_SETTINGS) {
                        showDialog = ShowDialog.NONE;
                        cameraSettingsViewModel.onSaveCameraSettingsBy(cameraName);
                    }
                }
            }
        }
    }

    @Override
    public void onDialogSdCardButtonClick(DialogFragment dialog) {
        dialog.dismiss();
        hasAlreadyAddedInDialogTag = false;
        switch (showDialog) {
            case OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE:
                firmwareUpdateSequence.clear();//If Use select SDcard update
                showOpsinFpgaContactCustomerDialog();
                showDialog = ShowDialog.NONE;
                hideSpinner();
                break;
            case WIFI_FW_UPDATE_AVAILABLE_DIALOG:
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    homeViewModel.startOpsinWiFiFirmwareUpgradeOnlyIfWiFiVersionISNull(true);
                }
                showDialog = ShowDialog.NONE;
                break;
        }
    }

    @Override
    public void onDialogSiOnyxAppButtonClick(DialogFragment dialog) {
        dialog.dismiss();
        hasAlreadyAddedInDialogTag = false;
        switch (showDialog) {
            case OPSIN_FPGA_DECIDE_SDCARD_UPDATE_OR_PLEXUS_UPDATE:
                applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
                applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.NONE;
                commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;

                homeViewModel.setIsFpgaUpgradeAvailable(false);
                tcpConnectionViewModel.getBatteryInfo();//If user select App Update
                showDialog = ShowDialog.NONE;
                break;
            case WIFI_FW_UPDATE_AVAILABLE_DIALOG:
                showDialog = ShowDialog.NONE;
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    homeViewModel.startOpsinWiFiFirmwareUpgradeOnlyIfWiFiVersionISNull(false);
                }
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return homeViewModel.getNavController().navigateUp() || super.onSupportNavigateUp();
    }

    private void backToHome(boolean hasCameraSettings) {
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
            tcpConnectionViewModel.stopServiceDigital();
        }
        if (getNucScheduledExecutorService != null) {
            getNucScheduledExecutorService.shutdownNow();
            getNucScheduledExecutorService = null;
        }
        homeViewModel.setWiFiRetryAfterDisconnect(false);
        tcpConnectionViewModel.cancelOpsinPeriodicTimer();
        applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.NONE;
        commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;

        if (!currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL) && tcpConnectionViewModel.isSocketConnected().getValue() != null && tcpConnectionViewModel.isSocketConnected().getValue() == Constants.STATE_CONNECTED) {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN))
                tcpConnectionViewModel.stopOpsinLiveStreaming();
            tcpConnectionViewModel.disconnectSocket();
            tcpConnectionViewModel.isSocketConnected().removeObservers(this);
        }
        CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
        cameraViewModel.resetOpsinRecordUI();
        if (hasCameraSettings) {
            if (homeViewModel.getNavController() != null) {
                Log.e(TAG, "backToHome: called " + homeViewModel.currentScreen);
                if (homeViewModel.currentScreen == FIRMWARE_UPDATE) {
                    homeViewModel.getNavController().popBackStack(R.id.analogFirmwareUpdateFragment, true);
                    homeViewModel.getNavController().popBackStack(R.id.cameraInfoTabDialogFragment, true);
                }
                homeViewModel.getNavController().popBackStack(R.id.cameraFragment, true);
                homeViewModel.getNavController().popBackStack(R.id.cameraSplashFragment, true);
                homeViewModel.getNavController().navigate(R.id.homeFragment);
            }
        } else {
            if (homeViewModel.getNavController() != null) {
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CHANGE_PASSWORD_SCREEN) {
                    homeViewModel.getNavController().popBackStack(R.id.changePasswordFragment, true);
                    homeViewModel.getNavController().popBackStack(R.id.cameraWifiSettingsFragment, true);
                } else {
                    homeViewModel.getNavController().popBackStack(R.id.cameraFragment, true);
                    homeViewModel.getNavController().popBackStack(R.id.cameraSplashFragment, true);
                }
                homeViewModel.getNavController().navigate(R.id.homeFragment);
            }
        }
    }

    public String getWiFiSSID(WifiManager mWifiManager) {
        String wifiSSID = "";
        if (isSDKBelow13()) {
            wifiSSID = mWifiManager.getConnectionInfo().getSSID();
            wifiSSID = wifiSSID.replace("\"", "");
            Log.d(TAG, "getWiFiSSID: 12 and below devices");
        } else {
            if (viewModel.getNetworkCapability() != null) {
                final TransportInfo transportInfo = ((NetworkCapabilities) viewModel.getNetworkCapability()).getTransportInfo();
                if (transportInfo instanceof WifiInfo) {
                    final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                    wifiSSID = wifiInfo.getSSID();
                    wifiSSID = wifiSSID.replace("\"", "");
                    Log.d(TAG, "getWiFiSSID:  13 devices" + wifiSSID);
                }
            }
        }
        if (isCamera(wifiSSID)) {
            return wifiSSID;
        } else {
            return "";
        }

    }

    public WifiInfo getWifiInfo(WifiManager mWifiManager) {
        WifiInfo wifiInfo = null;
        if (isSDKBelow13()) {
            Log.d(TAG, "getWifiInfo: Android Below 13  ");
            wifiInfo = mWifiManager.getConnectionInfo();
        } else {
            Log.d(TAG, "getWifiInfo:Android 13");
            if (viewModel.getNetworkCapability() != null) {
                final TransportInfo transportInfo = ((NetworkCapabilities) viewModel.getNetworkCapability()).getTransportInfo();
                if (transportInfo instanceof WifiInfo) {
                    wifiInfo = (WifiInfo) transportInfo;
                }
            }
        }
        return wifiInfo;
    }

    @Override
    protected void onDestroy() {
        if (viewModel.isAppCleared()) {
            viewModel.setAppClearedFalse();
            // Constants.hasNightWaveCamera = false;
            exitFromApp();
        }
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        super.onDestroy();
    }

    /* for this aws login dialog*/
    String showLoginScreenDialogTag = "";

    public synchronized void showDialogLoginScreen(String title, String message) {
        if (loginViewModel != null && homeViewModel.appBehaviour == HomeViewModel.AppBehaviour.FOREGROUND) {
            if (!loginViewModel.getFragmentManager().isDestroyed()) {
                PasswordDialogFragment dialog = new PasswordDialogFragment();
                if (loginViewModel != null && !loginViewModel.getFragmentManager().isDestroyed()) {
                    dialog.setPasswordDialogListener(this, loginViewModel);
                    Bundle args = new Bundle();
                    args.putString("message", message);

                    if (showDialogLoginScreen == ShowDialogLoginScreen.SENT_OTP_DIALOG) {
                        args.putString("title", title);
                        args.putBoolean("displayTitle", true);
                        args.putBoolean("hasShowOtpLayout", true);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "SENT_OTP_DIALOG";
                    }

                    if (showDialogLoginScreen == ShowDialogLoginScreen.CHANGE_NEW_PASSWORD_DIALOG) {
                        args.putString("title", title);
                        args.putBoolean("displayTitle", true);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", true);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "CHANGE_NEW_PASSWORD_DIALOG";
                    }

                    if (showDialogLoginScreen == ShowDialogLoginScreen.ALREADY_SIGNIN_DIALOG) {
                        args.putBoolean("displayTitle", false);
                        args.putBoolean("hasShowAlreadySignInLayout", true);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "ALREADY_SIGNIN_DIALOG";
                    }
                    if (showDialogLoginScreen == ShowDialogLoginScreen.LOG_OUT_DIALOG) {
                        args.putBoolean("displayTitle", false);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", true);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "LOG_OUT_DIALOG";
                    }

                    if (showDialogLoginScreen == ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG) {
                        args.putBoolean("displayTitle", true);
                        args.putString("title", title);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", true);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "NO_INTERNET_CONNECTION_DIALOG";
                    }

                    if (showDialogLoginScreen == ShowDialogLoginScreen.ALREADY_SIGN_OUT_OTHER_DEVICE) {
                        args.putBoolean("displayTitle", false);
                        args.putString("title", title);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", true);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "ALREADY_SIGN_OUT_OTHER_DEVICE";
                    }

                    if (showDialogLoginScreen == ShowDialogLoginScreen.USER_NOT_FOUND_DIALOG) {
                        args.putBoolean("displayTitle", false);
                        args.putString("title", title);
                        args.putString("message", message);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", true);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        showLoginScreenDialogTag = "USER_NOT_FOUND_DIALOG";
                    }
                    if (showDialogLoginScreen == ShowDialogLoginScreen.QR_CODE_SCAN_RESULT_DIALOG) {
                        args.putBoolean("displayTitle", true);
                        args.putString("title", title);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", true);
                        args.putBoolean("hasShowDeleteProductAlertLayout", false);
                        args.putString("message", message);
                        showLoginScreenDialogTag = "QR_CODE_SCAN_RESULT_DIALOG";
                    }
                    if (showDialogLoginScreen == ShowDialogLoginScreen.DELETE_PRODUCT_ALERT_DIALOG) {
                        args.putBoolean("displayTitle", true);
                        args.putString("title", title);
                        args.putBoolean("hasShowAlreadySignInLayout", false);
                        args.putBoolean("hasShowOtpLayout", false);
                        args.putBoolean("hasShowChangePasswordLayout", false);
                        args.putBoolean("hasShowLogoutLayout", false);
                        args.putBoolean("hasShowInternetConnectionFailedLayout", false);
                        args.putBoolean("hasShowAlreadySignOutOtherDeviceLayout", false);
                        args.putBoolean("hasShowUserNotFoundLayout", false);
                        args.putBoolean("hasShowQRScannerLayout", false);
                        args.putBoolean("hasShowDeleteProductAlertLayout", true);
                        args.putString("message", message);
                        showLoginScreenDialogTag = "DELETE_PRODUCT_ALERT_DIALOG";
                    }

                    dialog.setArguments(args);
                    dialog.setCancelable(false);
                    if (!hasAlreadyAddedInPasswordDialogTag) {
                        runOnUiThread(() -> {
                            if (!loginViewModel.getPasswordDialogFragments().isEmpty()) {
                                boolean isTagAvailable = false;
                                PasswordDialogFragment dialogFragmentT0Show = null;
                                int pos = -1;
                                for (int i = 0; i < loginViewModel.getPasswordDialogFragments().size(); i++) {
                                    PasswordDialogFragment passwordDialogFragment = loginViewModel.getPasswordDialogFragments().get(i);
                                    String tag = passwordDialogFragment.getTag() != null ? passwordDialogFragment.getTag() : null;
                                    Log.e(TAG, "showDialog: " + tag + " haha");
                                    if (tag != null && tag.equalsIgnoreCase(showLoginScreenDialogTag)) {
                                        isTagAvailable = true;
                                        dialogFragmentT0Show = passwordDialogFragment;
                                        pos = i;
                                    }
                                }
                                try {
                                    if (isTagAvailable) {
                                        Log.e(TAG, "showDialog: " + isTagAvailable + " haha ");
                                        dialogFragmentT0Show.dismiss();
                                        loginViewModel.getPasswordDialogFragments().remove(pos);
                                        dialog.show(loginViewModel.getFragmentManager(), showLoginScreenDialogTag);
                                        loginViewModel.getPasswordDialogFragments().add(dialog);
                                    } else {
                                        dialog.show(loginViewModel.getFragmentManager(), showLoginScreenDialogTag);
                                        loginViewModel.getPasswordDialogFragments().add(dialog);
                                        Log.e(TAG, "showDialog: add tag1");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                dialog.show(loginViewModel.getFragmentManager(), showLoginScreenDialogTag);
                                loginViewModel.getPasswordDialogFragments().add(dialog);
                                Log.e(TAG, "showDialog: add tag");
                            }
                        });
                    }

                    // for this condition while rotate the device again add tag to be avoid
                    hasAlreadyAddedInPasswordDialogTag = dialog.getShowsDialog() && showLoginScreenDialogTag.equals(ShowDialogLoginScreen.SENT_OTP_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.CHANGE_NEW_PASSWORD_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.ALREADY_SIGNIN_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.LOG_OUT_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.ALREADY_SIGN_OUT_OTHER_DEVICE.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.USER_NOT_FOUND_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.QR_CODE_SCAN_RESULT_DIALOG.name())
                            || showLoginScreenDialogTag.equals(ShowDialogLoginScreen.DELETE_PRODUCT_ALERT_DIALOG.name());
                }
            }
        } else {
            Log.e(TAG, "showDialog: on background");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
        }
    }

    @Override
    public void onDialogSentConfirmationClick(DialogFragment dialog, String strUserName, boolean isShowCustomProgressbar) {
        hasAlreadyAddedInPasswordDialogTag = false;
        loginViewModel.hasShowCustomProgressbar(true);
        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    RequestForgotPassword requestForgotPassword = new RequestForgotPassword();
                    requestForgotPassword.setUserName(strUserName);
                    loginViewModel.getUserEmailAlreadyExist(requestForgotPassword, new ValidUserInterface() {
                        @Override
                        public void onSuccess(int status, String msg) {
                            if (status == 400) {
                                loginViewModel.hasShowCustomProgressbar(false);
                                showDialogLoginScreen = ShowDialogLoginScreen.NONE;
                                dialog.dismiss();
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    showDialogLoginScreen = ShowDialogLoginScreen.USER_NOT_FOUND_DIALOG;
                                    showDialogLoginScreen("", msg);
                                }, 500);
                            } else {
                                runOnUiThread(() -> {
                                    loginViewModel.hasShowCustomProgressbar(false);
                                    showDialogLoginScreen = ShowDialogLoginScreen.NONE;
                                    loginViewModel.setForgotPasswordUserName(strUserName);
                                    dialog.dismiss();
                                    showDialogLoginScreen = ShowDialogLoginScreen.CHANGE_NEW_PASSWORD_DIALOG;
                                    showDialogLoginScreen(getString(R.string.new_password), "");
                                    makeToast(msg);
                                });
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            makeToast(error);
                            loginViewModel.hasShowCustomProgressbar(false);
                        }
                    });
                });
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    @Override
    public void onDialogNewPasswordSubmitClick(DialogFragment dialog, String userName, String newPassword, String confirmationCode, boolean isShowCustomProgressbar) {
        hasAlreadyAddedInPasswordDialogTag = false;
        new Handler(Looper.getMainLooper()).post(() -> {
            RequestChangeNewPassword requestChangeNewPassword = new RequestChangeNewPassword();
            requestChangeNewPassword.setUsername(loginViewModel.getForgotPasswordUserName());
            requestChangeNewPassword.setOtp(confirmationCode);
            requestChangeNewPassword.setNewPassword(newPassword);
            Log.d(TAG, "onDialogNewPasswordSubmitClick: " + loginViewModel.getForgotPasswordUserName() + "  " + userName);
            loginViewModel.changeNewPassword(requestChangeNewPassword, new CloudInterface() {
                @Override
                public void onSuccess(String state, String msg) {
                    runOnUiThread(() -> {
                        loginViewModel.hasShowCustomProgressbar(false);
                        dialog.dismiss();
                        showDialogLoginScreen = ShowDialogLoginScreen.NONE;
                        makeToast(msg);
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "confirmForgotPassword OnError" + " " + error);
                        loginViewModel.hasShowCustomProgressbar(false);
                        if (error.contains("Invalid code provided, please request a code again.")) {
                            makeToast(getString(R.string.message_invalid_otp));
                        } else {
                            makeToast(error);
                        }
                    });
                }
            });
        });
    }

    @Override
    public void onDialogAlreadySignedInOKClick(DialogFragment dialog) {
        hasAlreadyAddedInPasswordDialogTag = false;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Map<String, String> userAttributes = AWSMobileClient.getInstance().getUserAttributes();
                String email = userAttributes.get("email");
                Log.d(TAG, "userAlreadySignIn: " + email);

                Map<String, String> attributeMap = new HashMap<>();
                attributeMap.put("custom:signIN", "False");
                attributeMap.put("email", email);

                AWSMobileClient.getInstance().updateUserAttributes(attributeMap, new Callback<List<UserCodeDeliveryDetails>>() {
                    @Override
                    public void onResult(List<UserCodeDeliveryDetails> result) {
                        Log.d(TAG, "updateUserAttributes: " + result);
                        runOnUiThread(() -> {
                            loginViewModel.hasShowCustomProgressbar(false);
                            loginViewModel.setAlreadySignedOut(true);
                            makeToast(getString(R.string.signout_successfully));
                            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
                            dialog.dismiss();
                            executor.shutdownNow();
                            new Handler(Looper.getMainLooper()).post(() -> {
                                loginViewModel.globalLogout(new CloudInterface() {
                                    @Override
                                    public void onSuccess(String state, String msg) {
                                        Log.d(TAG, "signout onSuccess: ");
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.d(TAG, "signout error: " + error);
                                    }
                                });

                            });
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        // Error updating user attributes
                        runOnUiThread(() -> {
                            loginViewModel.hasShowCustomProgressbar(false);
                            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
                            dialog.dismiss();
                            executor.shutdownNow();
                        });

                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.d(TAG, "onDialogAlreadySignedInOKClick: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDialogLogoutClick(DialogFragment dialog) {
        hasAlreadyAddedInPasswordDialogTag = false;
        new Handler().post(() -> {
            NetworkUtils.pingServer(isSuccessful -> {
                // Handle the ping result here
                if (isSuccessful) {
                    // Ping successful
                    new Handler(Looper.getMainLooper()).post(() -> {
                        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
                            @Override
                            public void onResult(UserStateDetails result) {
                                loginViewModel.globalLogout(new CloudInterface() {
                                    @Override
                                    public void onSuccess(String state, String msg) {
                                        loginViewModel.hasShowCustomProgressbar(false);
                                        loginViewModel.setPassword("");
                                        loginViewModel.setUserName("");
                                        saveUserLoginState(false);
                                        loginViewModel.setAlreadySignedOut(false);
                                        showDialogLoginScreen = ShowDialogLoginScreen.NONE;
                                        dialog.dismiss();
                                        Log.d(TAG, "signOut success : " + result);
                                        runOnUiThread(() -> {
                                            makeToast(getString(R.string.logout_success));
                                            if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                                homeViewModel.onCancelGalleryView();
                                            }
                                            if (screenType == INFO_SCREEN) {
                                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                                homeViewModel.onCancelInfoView();
                                            }
                                            if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
                                                homeViewModel.cancelNearByDeviceView();
                                            }
                                            if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
                                                homeViewModel.onPopUpViewCancel();

                                            homeViewModel.getNavController().navigate(R.id.loginFragment);
                                        });
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        loginViewModel.hasShowCustomProgressbar(false);
                                        saveUserLoginState(false);
                                        loginViewModel.setAlreadySignedOut(false);
                                        dialog.dismiss();
                                        runOnUiThread(() -> {
                                            makeToast(getString(R.string.logout_success));
                                            if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                                homeViewModel.onCancelGalleryView();
                                            }
                                            if (screenType == INFO_SCREEN) {
                                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                                homeViewModel.onCancelInfoView();
                                            }
                                            if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
                                                homeViewModel.cancelNearByDeviceView();
                                            }
                                            if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
                                                homeViewModel.onPopUpViewCancel();

                                            homeViewModel.getNavController().navigate(R.id.loginFragment);
                                        });
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                loginViewModel.hasShowCustomProgressbar(false);
                                saveUserLoginState(false);
                                loginViewModel.setAlreadySignedOut(false);
                                dialog.dismiss();
                                runOnUiThread(() -> {
                                    makeToast(getString(R.string.logout_success));
                                    if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                                        Log.e(TAG, "showHomeScreen: " + screenType.name());
                                        homeViewModel.onCancelGalleryView();
                                    }
                                    if (screenType == INFO_SCREEN) {
                                        Log.e(TAG, "showHomeScreen: " + screenType.name());
                                        homeViewModel.onCancelInfoView();
                                    }
                                    if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
                                        homeViewModel.cancelNearByDeviceView();
                                    }
                                    if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
                                        homeViewModel.onPopUpViewCancel();

                                    homeViewModel.getNavController().navigate(R.id.loginFragment);
                                });
                                Log.d(TAG, "onError: " + e.getMessage());
                            }
                        });
                    });
                } else {
                    // Ping failed
                    loginViewModel.hasShowCustomProgressbar(false);
                    showInternetConnectionFailedDialog();
                }
            });
        });
    }

    @Override
    public void onDialogQRCodeResultSave(DialogFragment dialog, QRScanModel qrScanModel1) {
        hasAlreadyAddedInPasswordDialogTag = false;
        showDialogLoginScreen = ShowDialogLoginScreen.NONE;
        QRScanModel qrScanModel = profileViewModel.getQrScanModel();
        if (qrScanModel != null) {
            new Handler().post(() -> {
                NetworkUtils.pingServer(isSuccessful -> {
                    if (isSuccessful) {
                        try {
                            String loginUsername = getLoginUsername();
                            RequestProfileDeviceModel requestProfileDeviceModel = new RequestProfileDeviceModel();
                            if (loginUsername != null) {
                                requestProfileDeviceModel.setUserName(loginUsername);
                                String cameraName = qrScanModel.getDescription() != null ? qrScanModel.getDescription() : "Nightwave";
                                requestProfileDeviceModel.setCamera(capitalizeFirstLetter(cameraName));
                                requestProfileDeviceModel.setModel(qrScanModel.getModel());
                                requestProfileDeviceModel.setSerialNumber(qrScanModel.getSerialNumber());
                                requestProfileDeviceModel.setClassification(qrScanModel.getClassification());
                                requestProfileDeviceModel.setsKU(qrScanModel.getSku());
                                // call api
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    profileViewModel.uploadProductToAws(requestProfileDeviceModel, new ProfileInterface() {
                                        @Override
                                        public void onSuccess(String message, ArrayList<ProfileDevices> profileDevicesArrayList) {
                                            if (message != null) {
                                                runOnUiThread(() -> {
                                                    Log.d(TAG, "onSuccess: product");
                                                    makeToast(getString(R.string.the_product_details_have_been_added_successfully));
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        profileViewModel.setAlreadyExistQRData(false);
                                                        splashScreenViewModel.setQrCodeSaveState("qrCodeSaveState");
                                                    });
                                                });
                                            }
                                        }

                                        @Override
                                        public void onFailure(String error, String message) {
                                            loginViewModel.hasShowCustomProgressbar(false);
                                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                profileViewModel.setAlreadyExistQRData(true);
                                                splashScreenViewModel.setQrCodeSaveState("qrCodeSaveState");
                                            });
                                        }
                                    });
                                });
                            }
                            //     profileViewModel.setQrScanModel(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        dialog.dismiss();
                        profileViewModel.setQrScanModel(null);
                        loginViewModel.hasShowCustomProgressbar(false);
                        showInternetConnectionFailedDialog();
                    }
                });
            });
        }
    }

    private void showInternetConnectionFailedDialog() {
        showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG;
        showDialogLoginScreen("Network Unavailable", "");
    }

    private void saveUserLoginState(boolean isLogin) {
        try {
            SharedPreferences loginSharedPreferences = getSharedPreferences("LoginState", MODE_PRIVATE);
            SharedPreferences.Editor myEdit = loginSharedPreferences.edit();
            myEdit.putBoolean("isLogin", isLogin);
            myEdit.putString("userName", "");
            myEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDialogCancelClick(DialogFragment dialog) {
        hasAlreadyAddedInPasswordDialogTag = false;
        if (Objects.equals(showLoginScreenDialogTag, "SENT_OTP_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: sent confirmation code view dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("CHANGE_NEW_PASSWORD_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: change password view dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("LOG_OUT_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: logout view dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("NO_INTERNET_CONNECTION_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: no internet view dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
            if (HomeViewModel.CurrentScreen.PRODUCT_SCAN_SCREEN == homeViewModel.currentScreen) {
                profileViewModel.hasFailedUploadProduct();
            }
        }
        if (showLoginScreenDialogTag.equals("ALREADY_SIGN_OUT_OTHER_DEVICE")) {
            Log.d(TAG, "onDialogCancelClick: already sign out from other device view dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("USER_NOT_FOUND_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: user not found dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("ALREADY_SIGNIN_DIALOG")) {
            loginViewModel.hasShowCustomProgressbar(false);
            loginViewModel.setAlreadySignedOut(false);
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("QR_CODE_SCAN_RESULT_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: scan result view dismiss");
            profileViewModel.deleteQrScanResult();
            loginViewModel.hasShowCustomProgressbar(false);
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            dialog.dismiss();
        }
        if (showLoginScreenDialogTag.equals("DELETE_PRODUCT_ALERT_DIALOG")) {
            Log.d(TAG, "onDialogCancelClick: delete product item view dismiss");
            showDialogLoginScreen = ShowDialogLoginScreen.NONE;
            profileViewModel.setDeleteProductModel(null); // for this hold any value to reset while select cancel
            dialog.dismiss();
        }
    }

    @Override
    public void onDialogDeleteProductOkClick(DialogFragment dialog) {
        hasAlreadyAddedInPasswordDialogTag = false;
        showDialogLoginScreen = ShowDialogLoginScreen.NONE;
        dialog.dismiss();
        try {
            String loginUsername = getLoginUsername();
            RequestProfileDeviceModel requestProfileDeviceModel = new RequestProfileDeviceModel();
            if (loginUsername != null) {
                DeleteProductModel deleteProductModel = profileViewModel.getDeleteProductModel();
                profileViewModel.deleteProductOnAws(deleteProductModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLoginUsername() {
        String userName = null;
        try {
            SharedPreferences loginSharedPreferences = getSharedPreferences("LoginState", MODE_PRIVATE);
            userName = loginSharedPreferences.getString("userName", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userName;
    }

    public int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 < num2) {
                return -1;
            } else if (num1 > num2) {
                return 1;
            }
        }

        if (parts1.length < parts2.length) {
            return -1;
        } else if (parts1.length > parts2.length) {
            return 1;
        } else {
            return 0;
        }
    }

    public String removeFourthDigit(String version) {
        String[] parts = version.split("\\.");

        if (parts.length >= 4) {
            parts[3] = ""; // Remove the fourth part
            return String.join(".", parts);
        } else {
            return version; // No fourth part, return original version
        }
    }

    public boolean containsFourthDigit(String version) {
        String[] parts = version.split("\\.");

        return parts.length >= 4;
    }

    @Override
    protected void attachBaseContext(Context newBase) {

        final Configuration override = new Configuration(newBase.getResources().getConfiguration());
        override.fontScale = 1.0f;
        applyOverrideConfiguration(override);

        super.attachBaseContext(newBase);
    }

}