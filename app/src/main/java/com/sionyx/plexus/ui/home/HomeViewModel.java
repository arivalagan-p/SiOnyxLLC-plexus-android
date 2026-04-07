package com.sionyx.plexus.ui.home;

import static com.dome.librarynightwave.utils.Constants.OPSIN_FULL_IMAGE;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_RECOVERY;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.isSDK16AndAbove;
import static com.dome.librarynightwave.utils.Constants.isSdk12AndAbove;
import static com.sionyx.plexus.utils.Constants.WIFI_NOT_CONNECTED;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.FILE_DOWNLOAD;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_CHANNEL_ID;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.work.WorkManager;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.repository.opsinmodel.BatteryInfo;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.dialog.NoticeDialogFragment;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomAllFragment;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomImageFragment;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomManageModel;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomSelectedItemInfo;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomVideoFragment;
import com.sionyx.plexus.ui.model.CurrentFwVersion;
import com.sionyx.plexus.ui.model.Manifest;
import com.sionyx.plexus.ui.popup.PopUpCameraAdjustmentFragment;
import com.sionyx.plexus.ui.popup.PopUpCameraInfoFragment;
import com.sionyx.plexus.ui.popup.PopUpSettingsAdapter;
import com.sionyx.plexus.ui.popup.PopUpSettingsFragment;
import com.sionyx.plexus.utils.Event;
import com.sionyx.plexus.utils.model.CameraLookupState;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel {

    public boolean isSeekbarSeeked = false;
    public boolean menuState = false;
    public boolean leftMenuView = false;

    public boolean playVideo = false;

    public boolean pauseState = false;
    public boolean stopState = false;
    public String connectedSsid;
    public String lastConnectedSsid;
    public PopUpSettingsAdapter popUpSettingsAdapter;
    public static boolean isShowTimeZoneLayout;
    public static boolean isSelectedTimeZone;

    public static boolean isLoggedInUser;

    public boolean isLoggedInUser() {
        return isLoggedInUser;
    }

    public void setLoggedInUser(boolean loggedInUser) {
        isLoggedInUser = loggedInUser;
    }

    public ArrayList<GalleryBottomManageModel> getArrayListAll() {
        return arrayListAll;
    }

    public void addAllModel(GalleryBottomManageModel model) {
        this.arrayListAll.add(model);
    }

    private final ArrayList<GalleryBottomManageModel> arrayListAll = new ArrayList<>();

    public ArrayList<GalleryBottomManageModel> getArrayListPhoto() {
        return arrayListPhoto;
    }

    public void addModelPhoto(GalleryBottomManageModel model) {
        this.arrayListPhoto.add(model);
    }

    public ArrayList<GalleryBottomManageModel> arrayListPhoto = new ArrayList<>();

    public ArrayList<GalleryBottomManageModel> getArrayListVideo() {
        return arrayListVideo;
    }

    public void addVideo(GalleryBottomManageModel model) {
        this.arrayListVideo.add(model);
    }

    private final ArrayList<GalleryBottomManageModel> arrayListVideo = new ArrayList<>();
    public WorkManager workManager = WorkManager.getInstance(getApplication().getApplicationContext());

    private boolean isInfoSelected = false;
    //addition: Gallery Selected
    private boolean isGallerySelected = false;

    private boolean isGalleryManageSelected = false;
    private boolean isGalleryVideoRecordedInfoSelected = false;
    private boolean isGalleryVideoPlayerSelected = false;
    private boolean isSettingsSelected = false;
    private boolean isNwdCameraSelected = false;
    private boolean isPermissionChecked = false;
    private boolean isBatteryOptimized = false;
    private boolean isCameraSwitched = false;
    private boolean isWiFiStateFirstTime = true;
    private boolean isBleStateFirstTime = true;
    private boolean isPressCancelOrBackPopUpWindow;/* for this variable block after cloase popup window while rotate device go to live view screen block */

    private final PopUpSettingsFragment popUpSettingsFragment = new PopUpSettingsFragment();
    private final PopUpCameraAdjustmentFragment popUpCameraAdjustmentFragment = new PopUpCameraAdjustmentFragment();
    private final PopUpCameraInfoFragment popUpCameraInfoFragment = new PopUpCameraInfoFragment();

    private final ArrayList<Fragment> arrayList = new ArrayList<>();

    public ArrayList<Fragment> getListOfFragments() {
        arrayList.clear();
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                if (CameraViewModel.hasNewFirmware()) {
                    arrayList.add(popUpCameraAdjustmentFragment);
                    arrayList.add(popUpSettingsFragment);
                } else {
                    arrayList.add(popUpSettingsFragment);
                }
                break;
            case OPSIN:
                arrayList.add(popUpCameraAdjustmentFragment);
                arrayList.add(popUpSettingsFragment);
                break;
            case NIGHTWAVE_DIGITAL:
                //
                break;
        }
        return arrayList;
    }

    private final ArrayList<Fragment> arrayListInfoFragment = new ArrayList<>();

    public ArrayList<Fragment> getListOfInfoFragments() {
        if (arrayListInfoFragment.size() == 0) {
            arrayListInfoFragment.add(popUpCameraInfoFragment);
        }
        return arrayListInfoFragment;
    }


    public boolean isPressCancelOrBackPopUpWindow() {
        return isPressCancelOrBackPopUpWindow;
    }

    public void setPressCancelOrBackPopUpWindow(boolean pressCancelOrBackPopUpWindow) {
        isPressCancelOrBackPopUpWindow = pressCancelOrBackPopUpWindow;
    }

    public boolean isSettingScreenNavigated() {
        return isSettingScreenNavigated;
    }

    public void setSettingScreenNavigated(boolean settingScreenNavigated) {
        isSettingScreenNavigated = settingScreenNavigated;
    }

    private boolean isSettingScreenNavigated;
    private Manifest manifest;
    private CurrentFwVersion currentFwVersion;
    private int fwUpdateSequenceIndex = 0;
    private int abnormalFwUpdateSequenceIndex = 0;
    private boolean isAbnormalFwUpdate = false;
    private int bootModeCheckCount = 0;
    private int abnormalCompletedCount = 0;
    public boolean hasShowFwDialog = false;
    public boolean isShowFwDialogNone = false;
    private int opsinFwUpdateSequenceIndex = 0;
    public static boolean hasShowPowerCycleDialog = false;

    public boolean isReStartOpsinTCPCommandAndLiveStreaming() {
        return reStartOpsinTCPCommandAndLiveStreaming;
    }

    public void setReStartOpsinTCPCommandAndLiveStreaming(boolean reStartOpsinTCPCommandAndLiveStreaming) {
        this.reStartOpsinTCPCommandAndLiveStreaming = reStartOpsinTCPCommandAndLiveStreaming;
    }

    private boolean reStartOpsinTCPCommandAndLiveStreaming = false;

    public int pos = 0;
    public int max = 0;
    public boolean mCurrentState = false;
    public boolean bIsFpgaUpgradeAvailable = false;

    public boolean isFpgaUpgradeAvailable() {
        return bIsFpgaUpgradeAvailable;
    }

    public void setIsFpgaUpgradeAvailable(boolean bIsFpgaUpgradeAvailable) {
        this.bIsFpgaUpgradeAvailable = bIsFpgaUpgradeAvailable;
    }


    public boolean hasShowFwDialogNone() {
        return isShowFwDialogNone;
    }

    public void setShowFwDialogNone(boolean showFwDialogNone) {
        this.isShowFwDialogNone = showFwDialogNone;
    }

    public boolean isHasShowFwDialog() {
        return hasShowFwDialog;
    }

    public void setHasShowFwDialog(boolean hasShowFwDialog) {
        this.hasShowFwDialog = hasShowFwDialog;
    }

    ArrayList<WiFiHistory> bleResults = new ArrayList<>();

    public ArrayList<WiFiHistory> getBleResults() {
        return bleResults;
    }

    public MutableLiveData<Boolean> _showProgressbarLive = new MutableLiveData<>();
    LiveData<Boolean> showProgressbarLive = _showProgressbarLive;

    public static boolean hasShowProgressBar = false;

    public boolean hasShowProgressBar() {
        return hasShowProgressBar;
    }

    public void setHasShowProgressBar(boolean hasShowProgressBar) {
        HomeViewModel.hasShowProgressBar = hasShowProgressBar;
        _showProgressbarLive.setValue(hasShowProgressBar);
    }

    private boolean hasShowRecoverModeDialog = false;

    public boolean hasShowRecoverModeDialog() {
        return hasShowRecoverModeDialog;
    }

    public void setHasShowRecoverModeDialog(boolean hasShowRecoverModeDialog) {
        this.hasShowRecoverModeDialog = hasShowRecoverModeDialog;
    }

    public int getFwCompletedCount() {
        return abnormalCompletedCount;
    }

    public void setFwCompletedCount(int abnormalCompletedCount) {
        this.abnormalCompletedCount = abnormalCompletedCount;
    }

    public int getBootModeCheckCount() {
        return bootModeCheckCount;
    }

    public void setBootModeCheckCount(int bootModeCheckCount) {
        this.bootModeCheckCount = bootModeCheckCount;
    }

    public boolean isAbnormalFwUpdate() {
        return isAbnormalFwUpdate;
    }

    public void setAbnormalFwUpdate(boolean abnormalFwUpdate) {
        isAbnormalFwUpdate = abnormalFwUpdate;
    }

    public int getFwUpdateSequenceIndex() {
        return fwUpdateSequenceIndex;
    }

    public void setFwUpdateSequenceIndex(int fwUpdateSequenceIndex) {
        this.fwUpdateSequenceIndex = fwUpdateSequenceIndex;
    }

    public int getOpsinFwUpdateSequenceIndex() {
        return opsinFwUpdateSequenceIndex;
    }

    public void setOpsinFwUpdateSequenceIndex(int opsinFwUpdateSequenceIndex) {
        this.opsinFwUpdateSequenceIndex = opsinFwUpdateSequenceIndex;
    }

    public int getAbnormalFwUpdateSequenceIndex() {
        return abnormalFwUpdateSequenceIndex;
    }

    public void setAbnormalFwUpdateSequenceIndex(int abnormalFwUpdateSequenceIndex) {
        this.abnormalFwUpdateSequenceIndex = abnormalFwUpdateSequenceIndex;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    private final ArrayList<String> abnormalSequence = new ArrayList<>();
    private final ArrayList<String> opsinAbnormalSequence = new ArrayList<>();

    public ArrayList<String> getAbnormalSequence() {
        abnormalSequence.clear();
        abnormalSequence.add("RISCV");
        abnormalSequence.add("reboot_fpga");
        abnormalSequence.add("wait 15");
        return abnormalSequence;
    }

    public ArrayList<String> getOpsinAbnormalSequence() {
        opsinAbnormalSequence.clear();
        if (getBootModeCheckCount() == 2) {
            opsinAbnormalSequence.add(OPSIN_FULL_IMAGE);
//            opsinAbnormalSequence.add(RISCV);
            opsinAbnormalSequence.add("wait 60");
        } else if (getBootModeCheckCount() == 3) {
            opsinAbnormalSequence.add(OPSIN_RISCV_RECOVERY);
        }
        return opsinAbnormalSequence;
    }

    public CurrentFwVersion getCurrentFwVersion() {
        return currentFwVersion;
    }

    public void setCurrentFwVersion(CurrentFwVersion currentFwVersion) {
        this.currentFwVersion = currentFwVersion;
    }

    public ArrayList<NoticeDialogFragment> getNoticeDialogFragments() {
        return noticeDialogFragments;
    }

    public String getDialogMode() {
        return dialogMode;
    }

    public void setDialogMode(String dialogMode) {
        this.dialogMode = dialogMode;
    }

    private String dialogMode = "NORMAL";

    public void setNoticeDialogFragments(ArrayList<NoticeDialogFragment> noticeDialogFragments) {
        this.noticeDialogFragments = noticeDialogFragments;
    }

    ArrayList<NoticeDialogFragment> noticeDialogFragments = new ArrayList<>();


    private final MutableLiveData<String> insertOrUpdateSsid = new MutableLiveData<>();
    private final MutableLiveData<String> insertOrUpdateNWDSsid = new MutableLiveData<>();


    public void setWifiCompleted(Object message) {
        wifiCompleted.postValue(new Event<>(message));
    }

    public MutableLiveData<Event<Object>> getWifiCompleted() {
        return wifiCompleted;
    }

    public void setNotifyProgress(Object message) {
        notifyProgressDuringFWUpdate.postValue(new Event<>(message));
    }

    public MutableLiveData<Event<Object>> getNotifyProgress() {
        return notifyProgressDuringFWUpdate;
    }

    public void setNotifyUnableToConnectRequestedWiFi(Boolean message) {
        notifyUnableToConnectRequestedWiFi.postValue(new Event<>(message));
    }

    public MutableLiveData<Event<Boolean>> getNotifyUnableToConnectRequestedWiFi() {
        return notifyUnableToConnectRequestedWiFi;
    }

    private final MutableLiveData<Event<Object>> wifiCompleted = new MutableLiveData<>();
    private final MutableLiveData<Event<Object>> notifyProgressDuringFWUpdate = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> notifyUnableToConnectRequestedWiFi = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> _navigateToNextScreen = new MutableLiveData<>();
    public LiveData<Event<Boolean>> navigateToNextScreen = _navigateToNextScreen;


    private final ArrayList<WiFiHistory> selectDeviceModelArrayList;
    private final ArrayList<WiFiHistory> selectedDeviceHistoryArrayList;

    private FragmentManager fragmentManager;
    private NavController navController;
    private NoticeDialogListener listener;

    public static ScreenType screenType = ScreenType.HOME;
    public static AddButtonState addButtonState = AddButtonState.INIT;

    public CurrentScreen currentScreen = CurrentScreen.NONE;
    private final MutableLiveData<com.dome.librarynightwave.utils.Event<BatteryInfo>> _opsinBatteryInfo = new MutableLiveData<>();
    public LiveData<com.dome.librarynightwave.utils.Event<BatteryInfo>> opsinBatteryInfo = _opsinBatteryInfo;

    public void setOpsinBatteryInfo(BatteryInfo batteryInfo) {
        _opsinBatteryInfo.setValue(new com.dome.librarynightwave.utils.Event<>(batteryInfo));
    }


    public enum CurrentScreen {
        NONE,
        HOME,
        CAMERA_SPLASH_SCREEN,
        LIVE_VIEW,
        CAMERA_SETTINGS,
        FIRMWARE_UPDATE,
        LAUNCHER_SPLASH_SCREEN,
        CAMERA_SETTINGS_DIALOG_SCREEN,
        CAMERA_INFO_DIALOG_SCREEN,
        LOGIN_SCREEN,
        PROFILE_LIST_SCREEN,
        PRODUCT_SCAN_SCREEN,
        FORGOT_PASSWORD_SCREEN,
        CHANGE_PASSWORD_SCREEN,
        CAMERA_WIFI_SETTINGS_SCREEN,
    }

    public enum ScreenType {
        HOME,
        ADD_SCREEN,
        INFO_SCREEN,
        GALLERY_SCREEN,
        GALLERY_MANAGE_SCREEN,
        GALLERY_RECORDED_VIDEO_INFO_SCREEN,
        GALLERY_RECORDED_VIDEO_PLAYER_SCREEN,
        SETTINGS_SCREEN,
        POP_UP_INFO_SCREEN,
        POP_UP_SETTINGS_SCREEN,
        FW_UPDATE_SCREEN,
    }

    public enum AddButtonState {
        INIT,
        INITIATE_FINISH,
    }

    public enum AppBehaviour {
        NONE,
        BACKGROUND,
        FOREGROUND,
        DESTROYED
    }

    public FirmwareStatus firmwareStatus = FirmwareStatus.FW_UPDATE_NONE;

    public enum FirmwareStatus {
        FW_UPDATE_STARTED,
        FW_UPDATE_COMPLETED,
        FW_UPDATE_FAILED,
        FW_UPDATE_NONE
    }

    public AppBehaviour appBehaviour = AppBehaviour.NONE;


    public AdapterNotifyState adapterNotifyState = AdapterNotifyState.NOTIFY_NONE;

    public enum AdapterNotifyState {
        NOTIFY_ALL,
        NOTIFY_ADDED,
        NOTIFY_MOVED,
        NOTIFY_REMOVED,
        NOTIFY_NONE
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);

        void onDialogNegativeClick(DialogFragment dialog);

        void onDialogNeutralClick(DialogFragment dialog);

        void onDialogSaveClick(DialogFragment dialog, String cameraName, boolean isSpecialCharacterLayout);

        void onDialogSdCardButtonClick(DialogFragment dialog);

        void onDialogSiOnyxAppButtonClick(DialogFragment dialog);
    }

    private final Application application;
    public Context appContext;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        selectDeviceModelArrayList = new ArrayList<>();
        selectedDeviceHistoryArrayList = new ArrayList<>();
        appContext = application.getApplicationContext();
        bluetoothAdapter = getBluetoothAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public boolean isWiFiStateFirstTime() {
        return isWiFiStateFirstTime;
    }

    public void setWiFiStateFirstTime(boolean wiFiStateFirstTime) {
        isWiFiStateFirstTime = wiFiStateFirstTime;
    }

    public boolean isBleStateFirstTime() {
        return isBleStateFirstTime;
    }

    public void setBleStateFirstTime(boolean bleStateFirstTime) {
        isBleStateFirstTime = bleStateFirstTime;
    }

    public NoticeDialogListener getListener() {
        return listener;
    }

    public void setListener(NoticeDialogListener listener) {
        this.listener = listener;
    }


    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager1) {
        this.fragmentManager = fragmentManager1;
    }

    public NavController getNavController() {
        return navController;
    }

    public void setNavController(NavController navController) {
        this.navController = navController;
    }


    public MutableLiveData<String> getInsertOrUpdateSsid() {
        return insertOrUpdateSsid;
    }

    public void setInsertOrUpdateSsid(String value) {
        insertOrUpdateSsid.setValue(value);
    }

    public MutableLiveData<String> getInsertOrUpdateNWDSsid() {
        return insertOrUpdateNWDSsid;
    }

    public void setInsertOrUpdateNWDSsid(String value) {
        insertOrUpdateNWDSsid.setValue(value);
    }

    public LiveData<Event<Boolean>> getNavigateToNextScreen() {
        return navigateToNextScreen;
    }

    public void setNavigateToNextScreen(boolean value) {
        _navigateToNextScreen.setValue(new Event<>(value));
    }

    public boolean isPermissionChecked() {
        return isPermissionChecked;
    }

    public void setPermissionChecked(boolean permissionChecked) {
        isPermissionChecked = permissionChecked;
    }

    public boolean isInfoSelected() {
        return isInfoSelected;
    }

    //addition: On Gallery selected
    public boolean isGallerySelected() {
        return isGallerySelected;
    }

    public boolean isGalleryManageSelected() {
        return isGalleryManageSelected;
    }

    public boolean isGalleryVideoRecordedInfoSelected() {
        return isGalleryVideoRecordedInfoSelected;
    }

    public boolean isGalleryVideoPlayerSelected() {
        return isGalleryVideoPlayerSelected;
    }

    //addition: On Setting selected
    public boolean isSettingsSelected() {
        return isSettingsSelected;
    }

    public void setInfoSelected(boolean infoSelected) {
        isInfoSelected = infoSelected;
    }

    //addition: setGallery
    public void setGallerySelected(boolean gallerySelected) {
        isGallerySelected = gallerySelected;
    }

    public void setGalleryManageSelected(boolean galleryManageSelected) {
        isGalleryManageSelected = galleryManageSelected;
    }

    public void setGalleryVideoRecordedInfoSelected(boolean galleryVideoRecordedInfoSelected) {
        isGalleryVideoRecordedInfoSelected = galleryVideoRecordedInfoSelected;
    }

    public void setGalleryVideoPlayerSelected(boolean galleryVideoPlayerSelected) {
        isGalleryVideoPlayerSelected = galleryVideoPlayerSelected;
    }

    //addition: setSettings
    public void setSettingsSelected(boolean settingsSelected) {
        isSettingsSelected = settingsSelected;
    }

    public void setNwdCameraSelected(boolean nwdCameraSelected) {
        isNwdCameraSelected = nwdCameraSelected;
    }

    public boolean isNwdCameraPressed(){
        return isNwdCameraSelected;
    }

    public boolean isCameraLongPressed = false;

    public boolean isCameraSwitched() {
        return isCameraSwitched;
    }

    public void setCameraSwitched(boolean cameraSwitched) {
        isCameraSwitched = cameraSwitched;
    }


    private final MutableLiveData<ArrayList<WiFiHistory>> _loadSelectedDevices = new MutableLiveData<>();
    public LiveData<ArrayList<WiFiHistory>> loadSelectedDevices = _loadSelectedDevices;

    private final MutableLiveData<Event<Boolean>> _isAddDevice = new MutableLiveData<>();
    LiveData<Event<Boolean>> isAddDevice = _isAddDevice;

    private final MutableLiveData<Event<Boolean>> _isSelectDevice = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectDevice = _isSelectDevice;

    private final MutableLiveData<Event<Boolean>> _isCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isCancel = _isCancel;

    private final MutableLiveData<Event<Boolean>> _isSelectInfo = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectInfo = _isSelectInfo;

    private final MutableLiveData<Event<Boolean>> _isSignInOrSignOutAccount = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSignInOrSignOutAccount = _isSignInOrSignOutAccount;


    //addition: Gallery - livedata
    private final MutableLiveData<Event<Boolean>> _isSelectGallery = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectGallery = _isSelectGallery;

    //addition: Settings - livedata
    private final MutableLiveData<Event<Boolean>> _isSelectSettings = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectSettings = _isSelectSettings;

    private final MutableLiveData<Event<String>> _isSelectCamera = new MutableLiveData<>();
    public LiveData<Event<String>> isSelectCamera = _isSelectCamera;

    private final MutableLiveData<WiFiHistory> _loadNearByDeviceConnections = new MutableLiveData<>();
    LiveData<WiFiHistory> loadNearByDeviceConnections = _loadNearByDeviceConnections;

    private final MutableLiveData<ArrayList<WiFiHistory>> _selectedDeviceConnectionHistory = new MutableLiveData<>();
    LiveData<ArrayList<WiFiHistory>> selectedDeviceConnectionHistory = _selectedDeviceConnectionHistory;

    private final MutableLiveData<Event<Boolean>> _isRefreshWifiConnection = new MutableLiveData<>();
    LiveData<Event<Boolean>> isRefreshWifiConnection = _isRefreshWifiConnection;

    private final MutableLiveData<Event<Boolean>> _isInfoViewCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isInfoViewCancel = _isInfoViewCancel;

    private final MutableLiveData<Event<Integer>> _isUpdateFirmware = new MutableLiveData<>();
    LiveData<Event<Integer>> isUpdateFirmware = _isUpdateFirmware;

    private final MutableLiveData<Event<Boolean>> _isSelectWifiDialogPositiveButton = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectWifiDialogPositiveButton = _isSelectWifiDialogPositiveButton;

    private final MutableLiveData<Event<Boolean>> _isUpdateFirmwareBGToFG = new MutableLiveData<>();/*BG- background  FG-foreground*/
    LiveData<Event<Boolean>> isUpdateFirmwareBGToFG = _isUpdateFirmwareBGToFG;

    private final MutableLiveData<Event<Boolean>> _isUpdateFirmwareRecoverMode = new MutableLiveData<>();
    LiveData<Event<Boolean>> isUpdateFirmwareRecoverMode = _isUpdateFirmwareRecoverMode;

    private final MutableLiveData<Event<Boolean>> _hasUpgradeWifiFirmware = new MutableLiveData<>();
    public LiveData<Event<Boolean>> hasUpgradeWifiFirmwareOnlyIfWiFiVersionIsNull = _hasUpgradeWifiFirmware;

    private final MutableLiveData<Event<Boolean>> _isShowProgressBar = new MutableLiveData<>();
    LiveData<Event<Boolean>> isShowProgressBar = _isShowProgressBar;

    private final MutableLiveData<Event<Boolean>> _isStopFwUIProgress = new MutableLiveData<>();
    LiveData<Event<Boolean>> isStopFwUIProgress = _isStopFwUIProgress;

    private final MutableLiveData<Event<Boolean>> _isFWUpdateFailed = new MutableLiveData<>();
    LiveData<Event<Boolean>> isFWUpdateFailed = _isFWUpdateFailed;

    private final MutableLiveData<Event<Boolean>> _isSelectPopUpDelete = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectPopUpDelete = _isSelectPopUpDelete;

    private final MutableLiveData<Event<Boolean>> _isDeleteCameraItem = new MutableLiveData<>();
    LiveData<Event<Boolean>> isDeleteCameraItem = _isDeleteCameraItem;

    private final MutableLiveData<Event<Boolean>> _isDeleteRecordedVideo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDeleteRecordedVideo = _isDeleteRecordedVideo;

    private final MutableLiveData<Event<Boolean>> _isDeleteCapturedImage = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDeleteCapturedImage = _isDeleteCapturedImage;

    private final MutableLiveData<Event<Boolean>> _isEraseMedia = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isEraseMedia = _isEraseMedia;

    private final MutableLiveData<Event<Boolean>> _isEraseVideo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isEraseVideo = _isEraseVideo;

    private final MutableLiveData<Event<Boolean>> _isErasePhoto = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isErasePhoto = _isErasePhoto;


    private final MutableLiveData<Event<Boolean>> _isSelectPopUpInfo = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectPopUpInfo = _isSelectPopUpInfo;

    private final MutableLiveData<Boolean> _isSelectPopUpSettings = new MutableLiveData<>();
    LiveData<Boolean> isSelectPopUpSettings = _isSelectPopUpSettings;

    private final MutableLiveData<Event<Boolean>> _isPopUpInfoViewCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isPopUpInfoViewCancel = _isPopUpInfoViewCancel;

    private final MutableLiveData<Event<Boolean>> _isSelectPopUpFwUpdateCheck = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectPopUpFwUpdateCheck = _isSelectPopUpFwUpdateCheck;

    private final MutableLiveData<Event<Boolean>> _hideFwScreenAndShowHomeScreen = new MutableLiveData<>();
    LiveData<Event<Boolean>> hideFwScreenAndShowHomeScreen = _hideFwScreenAndShowHomeScreen;

    private final MutableLiveData<Event<Boolean>> _hidePopUpWindowAndShowHomeScreen = new MutableLiveData<>();
    LiveData<Event<Boolean>> hidePopUpWindowAndShowHomeScreen = _hidePopUpWindowAndShowHomeScreen;

    private final MutableLiveData<Event<Boolean>> _closePopupSettingFragment = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupSettingFragment = _closePopupSettingFragment;

    private final MutableLiveData<Event<Boolean>> _closePopupCameraAdjusmentFragment = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupCameraAdjusmentFragment = _closePopupCameraAdjusmentFragment;

    private final MutableLiveData<Event<Boolean>> _closePopupCameraInfoFragment = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupCameraInfoFragment = _closePopupCameraInfoFragment;

    private final MutableLiveData<Event<String>> _isUpdateCameraName = new MutableLiveData<>();
    LiveData<Event<String>> isUpdateCameraName = _isUpdateCameraName;

    //addition: gallery cancel - livedata
    private final MutableLiveData<Event<Boolean>> _isGalleryViewCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isGalleryViewCancel = _isGalleryViewCancel;

    private final MutableLiveData<Event<Boolean>> _isGalleryManageViewCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isGalleryManageViewCancel = _isGalleryManageViewCancel;

    private final MutableLiveData<Event<Boolean>> _isGalleryRecordedVideoInfoCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isGalleryRecordedVideoInfoCancel = _isGalleryRecordedVideoInfoCancel;

    private final MutableLiveData<Event<Boolean>> _isGalleryRecordedVideoPlayerCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isGalleryRecordedVideoPlayerCancel = _isGalleryRecordedVideoPlayerCancel;

    //addition: setting cancel - livedata
    private final MutableLiveData<Event<Boolean>> _isSettingsViewCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSettingsViewCancel = _isSettingsViewCancel;

    //    private final MutableLiveData<Event<String>> _isSelectGalleryBottomItemView = new MutableLiveData<>();
    //    public LiveData<Event<String>> isSelectGalleryBottomItemView = _isSelectGalleryBottomItemView;

    //Selected Bottom Video Item info from All and video
    private final MutableLiveData<Event<GalleryBottomSelectedItemInfo>> _isSelectGalleryBottomItemView = new MutableLiveData<>();

    public LiveData<Event<GalleryBottomSelectedItemInfo>> isSelectGalleryBottomItemView = _isSelectGalleryBottomItemView;

    private final MutableLiveData<Event<Boolean>> _isSelectManage = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectManage = _isSelectManage;

    private final MutableLiveData<Event<Boolean>> _isSelectShare = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectShare = _isSelectShare;

    private final MutableLiveData<Event<Boolean>> _isManageSelectSelectAll = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectSelectAll = _isManageSelectSelectAll;

    public final MutableLiveData<Event<Boolean>> isManageSelectDownload = new MutableLiveData<>();

    //aa
    private final MutableLiveData<Event<Boolean>> _isManageSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectErase = _isManageSelectErase;

    public final MutableLiveData<Event<Boolean>> isManageSelectBack = new MutableLiveData<>();
    // public LiveData<Event<Boolean>> isManageSelectBack = _isManageSelectBack;

    public MutableLiveData<Event<Boolean>> clearSelectedCheckBox = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> _isManageAllSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageAllSelectErase = _isManageAllSelectErase;

    private final MutableLiveData<Event<Boolean>> _isManagePhotoSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManagePhotoSelectErase = _isManagePhotoSelectErase;

    private final MutableLiveData<Event<Boolean>> _isManageVideoSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageVideoSelectErase = _isManageVideoSelectErase;


    private final MutableLiveData<Event<Boolean>> _isManageSelectAllView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllView = _isManageSelectAllView;

//    private final MutableLiveData<Boolean> _isManageSelectAllView = new MutableLiveData<>();
//    public LiveData<Boolean> isManageSlectAllView = _isManageSelectAllView;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllPhotos = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllPhotos = _isManageSelectAllPhotos;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllVideos = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllVideos = _isManageSelectAllVideos;


    private final MutableLiveData<Event<Boolean>> _isManageSelectAllDownload = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllDownload = _isManageSelectAllDownload;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllPhotoDownload = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllPhotoDownload = _isManageSelectAllPhotoDownload;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllVideoDownload = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllVideoDownload = _isManageSelectAllVideoDownload;

    private final MutableLiveData<Event<Boolean>> _isClearSelectedCheckboxAllView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isClearSelectedCheckboxAllView = _isClearSelectedCheckboxAllView;

    private final MutableLiveData<Event<Boolean>> _isClearSelectedCheckboxPhotoView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isClearSelectedCheckboxPhotoView = _isClearSelectedCheckboxPhotoView;

    private final MutableLiveData<Event<Boolean>> _isClearSelectedCheckboxVideoView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isClearSelectedCheckboxVideoView = _isClearSelectedCheckboxVideoView;

//    public MutableLiveData<Event<Boolean>> _isUpdateGalleryItemView = new MutableLiveData<>();

    public MutableLiveData<Event<Boolean>> _isUpdateGalleryItemView = new MutableLiveData<>();

    public MutableLiveData<Event<Boolean>> enterExitMultiWindowMode = new MutableLiveData<>();
//    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryBottomAllItemView = new MutableLiveData<>();
//    public LiveData<Event<Boolean>> isUpdateGalleryBottomAllItemView = _isUpdateGalleryBottomAllItemView;

//    private final MutableLiveData<Event <Boolean>> _isUpdateGalleryBottomAllItemView = new MutableLiveData<>();
//    public LiveData<Event<Boolean>> isUpdateGalleryAllItemView = _isUpdateGalleryBottomAllItemView;

    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryBottomAllItemView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateGalleryBottomAllItemView = _isUpdateGalleryBottomAllItemView;
    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryBottomVideosItemView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateGalleryBottomVideosItemView = _isUpdateGalleryBottomVideosItemView;

    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryBottomPhotoItemView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateGalleryBottomPhotoItemView = _isUpdateGalleryBottomPhotoItemView;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoPlay = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoPlay = _isSelectRecordingInfoPlay;

    private final MutableLiveData<Event<Boolean>> _isSelectImageInfo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectImageInfo = _isSelectImageInfo;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoDelete = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoDelete = _isSelectRecordingInfoDelete;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoBack = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoBack = _isSelectRecordingInfoBack;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoVideoPlay = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoVideoPlay = _isSelectRecordingInfoVideoPlay;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoVideoPause = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoVideoPause = _isSelectRecordingInfoVideoPause;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoVideoStop = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoVideoStop = _isSelectRecordingInfoVideoStop;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoVideoPlayButton = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoVideoPlayButton = _isSelectRecordingInfoVideoPlayButton;

    private final MutableLiveData<Event<Boolean>> _isSelectRecordingInfoVideoBack = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectRecordingInfoVideoBack = _isSelectRecordingInfoVideoBack;

    private final MutableLiveData<Event<Boolean>> _isOpsinRecordInProgress = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isOpsinRecordInProgress = _isOpsinRecordInProgress;

    private final MutableLiveData<Event<Boolean>> _isOpsinStreamStopRecord = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isOpsinStreamStopRecord = _isOpsinStreamStopRecord;

    private final MutableLiveData<Boolean> _isUpdateOkIcon = new MutableLiveData<>();
    public LiveData<Boolean> isUpdateOkIcon = _isUpdateOkIcon;


    private final CameraLookupState cameraLookupState = new CameraLookupState();
    private final MutableLiveData<Event<Boolean>> _isLookUpSSIDFound = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isLookUpSSIDFound = _isLookUpSSIDFound;
    private String lookUpSSID;

    public CameraLookupState getCameraLookupState() {
        return cameraLookupState;
    }

    public void setIsLookUpSSIDFound(boolean value) {
        _isLookUpSSIDFound.postValue(new Event<>(value));
    }

    public String getLookUpSSID() {
        return lookUpSSID;
    }

    public void setLookUpSSID(String lookUpSSID) {
        this.lookUpSSID = lookUpSSID;
    }


    private boolean isWiFiRetryAfterDisconnect = false;

    public boolean isWiFiRetryAfterDisconnect() {
        return isWiFiRetryAfterDisconnect;
    }

    public void setWiFiRetryAfterDisconnect(boolean wiFiRetryAfterDisconnect) {
        isWiFiRetryAfterDisconnect = wiFiRetryAfterDisconnect;
    }


    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner;
    private static final long SCAN_PERIOD = 12000;
    private static boolean scanning;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String TAG = "HomeViewModel";

    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();

    public void scanLeDevice() {
        if (bluetoothLeScanner == null) {
            bluetoothAdapter = getBluetoothAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        // Scan filter for devices with matching name prefix
        ScanFilter nameFilter = new ScanFilter.Builder()
                .setDeviceName(getLookUpSSID())
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(application, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        if (bluetoothLeScanner != null && !scanning && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            scanning = true;
            bluetoothLeScanner.startScan(Collections.singletonList(nameFilter), scanSettings, leScanCallback);
            stopScanAfterTimeout();
        } else if (bluetoothLeScanner != null) {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    private void stopScanAfterTimeout() {
        handler.postDelayed(() -> {
            scanning = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(application, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            if (getCameraLookupState().isLooking()) {
                getCameraLookupState().transitionToNotFound();
                setIsLookUpSSIDFound(false);
                Log.e(TAG, "setIsLookUpSSIDFound false");
            }
            bluetoothLeScanner.stopScan(leScanCallback);
        }, SCAN_PERIOD);
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String SSID;
            if (isSdk12AndAbove() || isSDK16AndAbove()) {
                if (ActivityCompat.checkSelfPermission(application, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(application, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    SSID = device.getName();
                    if (SSID != null) {
                        Log.e(TAG, "onScanResult: " + SSID);
                    }
                    if (SSID != null && getLookUpSSID() != null && getCameraLookupState().isLooking() && getLookUpSSID().contains(SSID)) {
                        getCameraLookupState().transitionToFound();
                        setIsLookUpSSIDFound(true);
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                        Log.e(TAG, "setIsLookUpSSIDFound true");
                    }
                } else {
                    Log.e(TAG, "onScanResult: Permission Not Granted");
                }
            } else {
                SSID = device.getName();
                if (getCameraLookupState().isLooking() && getLookUpSSID().contains(SSID)) {
                    getCameraLookupState().transitionToFound();
                    setIsLookUpSSIDFound(true);
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                    Log.e(TAG, "setIsLookUpSSIDFound true");
                }
            }
        }
    };


    public void hasUpdateOkIcon(boolean value) {
        _isUpdateOkIcon.setValue(value);
    }

    public void hasShowOpsinRecordInProgressDialog(boolean value) {
        _isOpsinRecordInProgress.setValue(new Event<>(value));
    }

    public void hasShowOpsinStreamStopRecordDialog(boolean value) {
        _isOpsinStreamStopRecord.setValue(new Event<>(value));
    }

    //addition: gallery all screen
    public void onSelectManage() {
        _isSelectManage.setValue(new Event<>(true));
    }

    public void onSelectShare() {
        _isSelectShare.setValue(new Event<>(true));
    }

    public void onSelectGalleryBottomItemView(GalleryBottomSelectedItemInfo selectedVideoItem) {
        _isSelectGalleryBottomItemView.setValue(new Event<>(selectedVideoItem));
    }

//    public void onSelectGalleryBottomItemView(String filePath){
//        _isSelectGalleryBottomItemView.setValue(new Event<>(filePath));
//    }


    //addition: manage screen
    public void onManageSelectAll() {
        _isManageSelectSelectAll.setValue(new Event<>(true));
    }

    public void onManageSelectAllView(boolean isSelectAll) {
        _isManageSelectAllView.setValue(new Event<>(isSelectAll));
    }

//    public void onManageSelectAllView(boolean isSelectAll) {
//        _isManageSelectAllView.setValue(isSelectAll);
//    }

    public void onManageSelectAllVideo(boolean isSelectAll) {
        _isManageSelectAllVideos.setValue(new Event<>(isSelectAll));
    }

    public void onManageSelectAllPhotos(boolean isSelectAll) {
        _isManageSelectAllPhotos.setValue(new Event<>(isSelectAll));
    }

    //aa
    public void onManageSelectErase() {
        _isManageSelectErase.setValue(new Event<>(true));
    }

    public void onManageAllSelectErase(boolean isErased) {
        _isManageAllSelectErase.setValue(new Event<>(isErased));
    }

    public void onManagePhotoSelectErase(boolean isErased) {
        _isManagePhotoSelectErase.setValue(new Event<>(isErased));
    }

    public void onManageVideoSelectErase(boolean isErased) {
        _isManageVideoSelectErase.setValue(new Event<>(isErased));
    }

    public void onManageSelectDownload() {
        isManageSelectDownload.setValue(new Event<>(true));
    }

    public void onManageSelectAllDownload(boolean isDownload) {
        _isManageSelectAllDownload.setValue(new Event<>(isDownload));
    }

    public void onManageSelectAllPhotoDownload(boolean isDownload) {
        _isManageSelectAllPhotoDownload.setValue(new Event<>(isDownload));
    }

    public void onManageSelectAllVideoDownload(boolean isDownload) {
        _isManageSelectAllVideoDownload.setValue(new Event<>(isDownload));
    }

    public void onClearSelectedCheckboxAllView(boolean isClear) {
        _isClearSelectedCheckboxAllView.setValue(new Event<>(isClear));
    }

    public void onClearSelectedCheckboxPhotoView(boolean isClear) {
        _isClearSelectedCheckboxPhotoView.setValue(new Event<>(isClear));
    }

    public void onClearSelectedCheckboxVideoView(boolean isClear) {
        _isClearSelectedCheckboxVideoView.setValue(new Event<>(isClear));
    }

    public void onManageSelectBack() {
        isManageSelectBack.setValue(new Event<>(true));
    }

    public void clearSelectedCheckBox() {
        clearSelectedCheckBox.setValue(new Event<>(true));
    }

    public void updateGalleryBottomItemView(boolean isUpdate) {
        _isUpdateGalleryItemView.setValue(new Event<>(isUpdate));
    }

    public void isMultiwindowModeActivated(boolean isMultiWindowModeActivated) {
        enterExitMultiWindowMode.setValue(new Event<>(isMultiWindowModeActivated));
    }

    public MutableLiveData<Event<Boolean>> getEnterExitMultiWindowMode() {
        return enterExitMultiWindowMode;
    }

    public void isUpdateGalleryBottomAllItemView(boolean isUpdateItemView) {
        _isUpdateGalleryBottomAllItemView.setValue(new Event<>(isUpdateItemView));
    }

    public void updateGalleryBottomVideosItemView(boolean isUpdateItemView) {
        _isUpdateGalleryBottomVideosItemView.setValue(new Event<>(isUpdateItemView));
    } // in video fragment

    public void updateGalleryBottomPhotosItemView(boolean isUpdateItemView) {
        _isUpdateGalleryBottomPhotoItemView.setValue(new Event<>(isUpdateItemView));
    }// in image fragment

    public void onSelectRecordingInfoPlay() {
        _isSelectRecordingInfoPlay.setValue(new Event<>(true));
    }

    public void onSelectImageInfo() {
        _isSelectImageInfo.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoDelete() {
        _isSelectRecordingInfoDelete.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoBack() {
        _isSelectRecordingInfoBack.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoVideoPlay() {
        _isSelectRecordingInfoVideoPlay.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoVideoPause() {
        _isSelectRecordingInfoVideoPause.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoVideoStop() {
        _isSelectRecordingInfoVideoStop.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoVideoPlayButton() {
        _isSelectRecordingInfoVideoPlayButton.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoVideoBack() {
        _isSelectRecordingInfoVideoBack.setValue(new Event<>(true));
    }

    public void setWifiDialogPositiveButtonClick(boolean wifiDisconnectDialogState) {
        _isSelectWifiDialogPositiveButton.setValue(new Event<>(wifiDisconnectDialogState));
    }

    private final MutableLiveData<com.dome.librarynightwave.utils.Event<Boolean>> _isEnableMultiWindowMode = new MutableLiveData<>();
    public LiveData<com.dome.librarynightwave.utils.Event<Boolean>> isEnableMultiWindowMode = _isEnableMultiWindowMode;

    public void hasEnableMultiWindowMode() {
        _isEnableMultiWindowMode.setValue(new com.dome.librarynightwave.utils.Event<>(true));
    }


    private final ArrayList<WiFiHistory> selectDeviceModelss = new ArrayList<>();

    private final ArrayList<WiFiHistory> deviceHistoryModelss = new ArrayList<>();
    private int addedPos = 0;

    public int getAddedRemovedPos() {
        return addedPos;
    }

    public void setAddedRemovedPos(int addedPos) {
        this.addedPos = addedPos;
    }

    public ArrayList<WiFiHistory> getSelectDeviceModelss() {
        return selectDeviceModelss;
    }

    public ArrayList<WiFiHistory> getDeviceHistoryModelss() {
        return deviceHistoryModelss;
    }

    public ArrayList<WiFiHistory> getselectDeviceModelArrayList() {
        return selectDeviceModelArrayList;
    }

    public void initiateSelectDeviceView() {
        String camera_mac_address = "";
        String camera_fw_version = "";
        String camera_password = "";
        long last_connected_date_time = System.currentTimeMillis();

        WiFiHistory wiFiHistory = new WiFiHistory("Add Device", "Add Device", camera_mac_address, camera_fw_version, camera_password, last_connected_date_time, WIFI_NOT_CONNECTED);
        selectDeviceModelArrayList.clear();
        selectDeviceModelArrayList.add(0, wiFiHistory);
        adapterNotifyState = AdapterNotifyState.NOTIFY_ADDED;
        _loadSelectedDevices.setValue(selectDeviceModelArrayList);
    }

    /* added selected device connections into grid*/
    public void selectedDeviceConnections(WiFiHistory wiFiHistory, boolean shouldAutoClose) {
        if (!selectDeviceModelArrayList.contains(wiFiHistory)) {
            adapterNotifyState = AdapterNotifyState.NOTIFY_ADDED;
            selectDeviceModelArrayList.add(wiFiHistory);
        } else {
            int from = selectDeviceModelArrayList.indexOf(wiFiHistory);
            if (from != -1) {
                int is_wifi_connected = wiFiHistory.getIs_wifi_connected();
                selectDeviceModelArrayList.get(from).setIs_wifi_connected(is_wifi_connected);
                adapterNotifyState = AdapterNotifyState.NOTIFY_MOVED;
                Collections.swap(selectDeviceModelArrayList, from, selectDeviceModelArrayList.size() - 1);
            } else {
                adapterNotifyState = AdapterNotifyState.NOTIFY_ALL;
                selectDeviceModelArrayList.remove(wiFiHistory);
                selectDeviceModelArrayList.add(wiFiHistory);
            }
        }
        _loadSelectedDevices.setValue(selectDeviceModelArrayList);

        if (shouldAutoClose) {
            _isCancel.setValue(new Event<>(true));
        }
    }

    /* remove selected device connections into grid*/
    public void removeSelectedDeviceConnections(WiFiHistory wiFiHistory) {
        Log.e("TAG", "removeSelectedDeviceConnections: " + selectDeviceModelArrayList.size());
        adapterNotifyState = AdapterNotifyState.NOTIFY_REMOVED;
        selectDeviceModelArrayList.remove(wiFiHistory);
        _loadSelectedDevices.setValue(selectDeviceModelArrayList);
    }

    /* add device button event */
    private String selectedCamera;

    public void onAddDevice() {
        _isAddDevice.setValue(new Event<>(true));
    }

    public void onSelectCamera(String camera_ssid) {
        _isSelectCamera.setValue(new Event<>(camera_ssid));
        setSelectedCamera(camera_ssid);

    }

    public String getSelectedCamera() {
        return selectedCamera;
    }

    public void setSelectedCamera(String selectedCamera) {
        this.selectedCamera = selectedCamera;
    }

    public void selectNearByDevice() {
        _isCancel.setValue(new Event<>(true));
        _isSelectDevice.setValue(new Event<>(true));
    }

    public void cancelNearByDeviceView() {
        _isCancel.setValue(new Event<>(true));
    }

    public void loadWifiHistory(WiFiHistory nearByDeviceModelArrayList) {
        _loadNearByDeviceConnections.setValue(nearByDeviceModelArrayList);
    }

    public void onCancelInfoView() {
        _isInfoViewCancel.setValue(new Event<>(true));
    }

    public void refreshWifiConnection() {
        _isRefreshWifiConnection.setValue(new Event<>(true));
    }

    public void onClickInfo() {
        _isSelectInfo.setValue(new Event<>(true));
    }

    public void signInOrSignOutAccount() {
        _isSignInOrSignOutAccount.postValue(new Event<>(true));
    }

    //addition: on click gallery
    public void onClickGallery() {
        _isSelectGallery.setValue(new Event<>(true));
    }

    //addition: on click settings
    public void onClickSettings() {
        _isSelectSettings.setValue(new Event<>(true));
    }

    //addition: cancel gallery view
    public void onCancelGalleryView() {
        _isGalleryViewCancel.setValue(new Event<>(true));
    }

    //addition: cancel setting view
    public void onCancelSettingsView() {
        _isSettingsViewCancel.setValue(new Event<>(true));
    }


    public void startFirmwareUpdate(int value) {
        _isUpdateFirmware.setValue(new Event<>(value));
    }

    public void hasFwUpdateBackgroundToForeground(boolean value) {
        _isUpdateFirmwareBGToFG.setValue(new Event<>(value));
    }

    public void startFirmwareRecoverMode(Boolean value) {
        _isUpdateFirmwareRecoverMode.setValue(new Event<>(value));
    }

    public void startOpsinWiFiFirmwareUpgradeOnlyIfWiFiVersionISNull(Boolean value) {
        _hasUpgradeWifiFirmware.setValue(new Event<>(value));
    }

    public void hasShowProgressBar(Boolean value) {
        _isShowProgressBar.setValue(new Event<>(value));
    }

    public void hasStopFWUIProgress(Boolean value) {
        _isStopFwUIProgress.setValue(new Event<>(value));
    }

    public void hasFWUpdateFailed(Boolean value) {
        _isFWUpdateFailed.setValue(new Event<>(value));
    }

    /* pop up */
    public void hasSelectPopUpInfo(Boolean value) {
        _isSelectPopUpInfo.setValue(new Event<>(value));
    }

    public void hasSelectPopUpFwUpdateCheck(Boolean value) {
        _isSelectPopUpFwUpdateCheck.setValue(new Event<>(value));
    }

    public void hasSelectPopUpSettings(Boolean value) {
        _isSelectPopUpSettings.setValue(value);
    }

    public void onPopUpViewCancel() {
        _isPopUpInfoViewCancel.setValue(new Event<>(true));
    }

    public void hasSelectPopUpDelete(Boolean value) {
        _isSelectPopUpDelete.setValue(new Event<>(value));
    }

    public void hasDeleteCamera(Boolean value) {
        _isDeleteCameraItem.setValue(new Event<>(value));
    }

    public void hasDeleteRecordedVideo(Boolean recordedVideo) {
        _isDeleteRecordedVideo.setValue(new Event<>(recordedVideo));
    }

    public void hasDeleteCapturedImage(Boolean capturedImage) {
        _isDeleteCapturedImage.setValue(new Event<>(capturedImage));
    }

    public void hasEraseMedia(Boolean value) {
        _isEraseMedia.setValue(new Event<>(value));
    }

    public void hasEraseVideo(Boolean value) {
        _isEraseVideo.setValue(new Event<>(value));
    }

    public void hasErasePhoto(Boolean value) {
        _isErasePhoto.setValue(new Event<>(value));
    }

    public void hideFwScreenAndShowHomeScreen(Boolean value) {
        _hideFwScreenAndShowHomeScreen.setValue(new Event<>(value));
    }

    public void hidePopUpScreenAndShowHomeScreen(Boolean value) {
        _hidePopUpWindowAndShowHomeScreen.setValue(new Event<>(value));
    }

    public void closePopupSettingFragment(Boolean value) {
        _closePopupSettingFragment.setValue(new Event<>(value));
    }

    public void closePopupCameraAdjusmentFragment(Boolean value) {
        _closePopupCameraAdjusmentFragment.setValue(new Event<>(value));
    }

    public void closePopupCameraInfoFragment(Boolean value) {
        _closePopupCameraInfoFragment.setValue(new Event<>(value));
    }

    public void hasUpdateCameraName(String value) {
        _isUpdateCameraName.setValue(new Event<>(value));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        setPermissionChecked(false);
        setBleStateFirstTime(true);
        setWiFiStateFirstTime(true);
        executor.shutdownNow();
        if (timer != null) {
            timer.cancel();
        }
    }

    private final MutableLiveData<Integer> BottomLayoutType = new MutableLiveData<>();

    public LiveData<Integer> getBottomLayoutType() {
        return BottomLayoutType;
    }

    public void setBottomLayoutType(Integer BottomlayoutType) {
        BottomLayoutType.setValue(BottomlayoutType);
    }

    private final MutableLiveData<Event<Boolean>> _UpdateManageAllItem = new MutableLiveData<>();
    public LiveData<Event<Boolean>> ManageUpdateAllItem = _UpdateManageAllItem;

    private final MutableLiveData<Event<Boolean>> _UpdateManagePhotoItem = new MutableLiveData<>();
    public LiveData<Event<Boolean>> ManageUpdatePhotoItem = _UpdateManagePhotoItem;

    private final MutableLiveData<Event<Boolean>> _UpdateManageVideoItem = new MutableLiveData<>();
    public LiveData<Event<Boolean>> ManageUpdateVideoItem = _UpdateManageVideoItem;

    public void UpdateManageVideoItem(boolean updateItem) {
        _UpdateManageVideoItem.setValue(new Event<>(updateItem));
    }

    public void UpdateManagePhotoItem(boolean updateItem) {
        _UpdateManagePhotoItem.setValue(new Event<>(updateItem));
    }

    public void UpdateManageAllItem(boolean updateItem) {
        _UpdateManageAllItem.setValue(new Event<>(updateItem));
    }

    private final MutableLiveData<Event<Boolean>> _isManageAllBack = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageAllBack = _isManageAllBack;

    private final MutableLiveData<Event<Boolean>> _isManagePhotoBack = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManagePhotoBack = _isManagePhotoBack;

    private final MutableLiveData<Event<Boolean>> _isManageVideoBack = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageVideoBack = _isManageVideoBack;

    public void onManageAllSelectBack(boolean isManageAllBack) {
        _isManageAllBack.setValue(new Event<>(isManageAllBack));
    }

    public void onManagePhotoSelectBack(boolean isManagePhotoBack) {
        _isManagePhotoBack.setValue(new Event<>(isManagePhotoBack));
    }

    public void onManageVideoSelectBack(boolean isManageVideoBack) {
        _isManageVideoBack.setValue(new Event<>(isManageVideoBack));
    }

    private final MutableLiveData<Integer> selectedTabIndex = new MutableLiveData<>();
    public int tabPos = 0;


    public MutableLiveData<Integer> getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int Index) {
        selectedTabIndex.setValue(Index);
    }


    private final MutableLiveData<String> _opsinPopupCameraName = new MutableLiveData<>();
    public final MutableLiveData<Object> opsinPopupProductVersionDetails = new MutableLiveData<>();
    public LiveData<String> opsinPopupCameraName = _opsinPopupCameraName;

    public void observePopupOpsinCameraName(String cameraName) {
        _opsinPopupCameraName.setValue(cameraName);
    }

    public void observePopupCameraVersionDetails(Object opsinVersionDetails) {
        opsinPopupProductVersionDetails.postValue(opsinVersionDetails);
    }

    private final MutableLiveData<Event<Boolean>> _UpdateManageLayout = new MutableLiveData<>();

    public LiveData<Event<Boolean>> UpdateManageLayout = _UpdateManageLayout;

    public void onUpdateManageLayout(boolean UpdateLayout) {
        _UpdateManageLayout.setValue(new Event<>(UpdateLayout));
    }

    public int getCurrentManageTabPosition() {
        return currentManageTabPosition;
    }

    public void setCurrentManageTabPosition(int currentManageTabPosition) {
        this.currentManageTabPosition = currentManageTabPosition;
    }

    private int currentManageTabPosition;

    public static String selectedtab;

    public static boolean isRecordingStarted = false;
    public static boolean isSdCardRecordingStarted = false;

    public MutableLiveData<Event<Boolean>> isGallerySelectAllMedia = new MutableLiveData<>();

    public void setGallerySelectAll(boolean gallerySelectAll) {
        this.isGallerySelectAllMedia.setValue(new Event<>(gallerySelectAll));
    }

    public boolean isErasedAll;
    public boolean isErasedPic;
    public boolean isErasedVideo;

    public void getAllFiles(FragmentActivity activity) {
        try {
            Log.e("GALLERY", "getAllFiles: called");
            File photosDirectory = createAppFolderForImage(activity, "SiOnyx");
            File videosDirectory = createAppFolderForVideo(activity, "SiOnyx");

            List<File> photoFileList = getAllMediaFiles(photosDirectory, true);
            List<File> videoFileList = getAllMediaFiles(videosDirectory, false);

            File[] videoFile = videoFileList.toArray(new File[0]);
            File[] imageFiles = photoFileList.toArray(new File[0]);

            File[] combinedFiles = new File[videoFile.length + imageFiles.length];
            System.arraycopy(videoFile, 0, combinedFiles, 0, videoFile.length);
            System.arraycopy(imageFiles, 0, combinedFiles, videoFile.length, imageFiles.length);

            Arrays.sort(combinedFiles, (file1, file2) -> {
                long lastModified1 = file1.lastModified();
                long lastModified2 = file2.lastModified();
                return Long.compare(lastModified2, lastModified1);
            });

            for (File file : combinedFiles) {
                if (file.isFile()) {
                    String path = file.getAbsolutePath();
                    if ((path.endsWith(".mp4")) || (path.endsWith(".jpg"))) {
                        GalleryBottomManageModel bottomManageModel = new GalleryBottomManageModel();
                        bottomManageModel.setFilePath(path);
                        bottomManageModel.setFileName(file.getName());
                        if (!getArrayListAll().contains(bottomManageModel)) {
                            addAllModel(bottomManageModel);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<File> getAllMediaFiles(File rootFolder, boolean isImage) {
        List<File> mediaFiles = new ArrayList<>();

        if (rootFolder.exists() && rootFolder.isDirectory()) {
            File[] subDirs = rootFolder.listFiles(File::isDirectory);// ssid_1 ssid_2, new - analog, digital

            if (subDirs != null) {
                for (File subDir : subDirs) {
                    File[] subSsidDirs = subDir.listFiles(File::isDirectory);
                    for (File subSsidDir : subSsidDirs) {
                        File[] files = subSsidDir.listFiles();
                        if (files != null) {
                            traverseDirectory(rootFolder, mediaFiles, isImage);
                        }
                    }

                }
            }
        }
        return mediaFiles;
    }

    // Recursive function to traverse the directory structure
    private void traverseDirectory(File folder, List<File> mediaFiles, boolean isImage) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    traverseDirectory(file, mediaFiles, isImage);
                } else if ((isImage && isImageFile(file)) || (!isImage && isVideoFile(file))) {
                    mediaFiles.add(file);
                }
            }
        }
    }

    // Function to check if a file is an image file based on its extension
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg");
    }

    // Function to check if a file is a video file based on its extension
    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4");
    }


    public void setIsMediaVideoDeleted(boolean isMediaVideoDeleted) {
        this._isMediaVideoDeleted.setValue(isMediaVideoDeleted);
    }

    private final MutableLiveData<Boolean> _isMediaVideoDeleted = new MutableLiveData<>();

    public LiveData<Boolean> isMediaVideoDeleted = _isMediaVideoDeleted;

    private final MutableLiveData<Boolean> _isManageSelected = new MutableLiveData<>();

    public void setIsManageSelect(Boolean isManageSelect) {
        this._isManageSelected.setValue(isManageSelect);
    }

    public LiveData<Boolean> isManageSelect = _isManageSelected;

    private final GalleryBottomAllFragment galleyAllViewsFragment = new GalleryBottomAllFragment();
    private final GalleryBottomImageFragment galleryImageFragment = new GalleryBottomImageFragment();
    private final GalleryBottomVideoFragment galleryVideoFragment = new GalleryBottomVideoFragment();
    private final ArrayList<Fragment> fragmentArrayList = new ArrayList<>();

    public ArrayList<Fragment> getFragments() {
        if (fragmentArrayList.size() == 0) {
            fragmentArrayList.add(galleyAllViewsFragment);
            fragmentArrayList.add(galleryImageFragment);
            fragmentArrayList.add(galleryVideoFragment);
        }
        return fragmentArrayList;
    }

    private final MutableLiveData<Event<Boolean>> _closePopupAllMedia = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupAllMedia = _closePopupAllMedia;

    private final MutableLiveData<Event<Boolean>> _closePopupImage = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupImage = _closePopupImage;

    private final MutableLiveData<Event<Boolean>> _closePopupVideo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupVideo = _closePopupVideo;

    private final MutableLiveData<Boolean> _noRecordsFoundAll = new MutableLiveData<>();
    public LiveData<Boolean> noRecordsFoundAll = _noRecordsFoundAll;

    private final MutableLiveData<Boolean> _noRecordsFoundImage = new MutableLiveData<>();
    public LiveData<Boolean> noRecordsFoundImage = _noRecordsFoundImage;

    private final MutableLiveData<Boolean> _noRecordsFoundVideo = new MutableLiveData<>();
    public LiveData<Boolean> noRecordsFoundVideo = _noRecordsFoundVideo;

    public void setClosePopupAllMedia(boolean closePopupAllMedia) {
        this._closePopupAllMedia.setValue(new Event<>(closePopupAllMedia));
    }

    public void setClosePopupImage(boolean closePopupImage) {
        this._closePopupImage.setValue(new Event<>(closePopupImage));
    }

    public void setClosePopupVideo(boolean closePopupVideo) {
        this._closePopupVideo.setValue(new Event<>(closePopupVideo));
    }

    public void setNoRecordsFoundAll(boolean noRecordsFoundAll) {
        this._noRecordsFoundAll.setValue(noRecordsFoundAll);
    }

    public void setNoRecordsFoundImage(boolean noRecordsFoundImage) {
        this._noRecordsFoundImage.setValue(noRecordsFoundImage);
    }

    public void setNoRecordsFoundVideo(boolean noRecordsFoundVideo) {
        this._noRecordsFoundVideo.setValue(noRecordsFoundVideo);
    }

    public MutableLiveData<Event<Boolean>> isBackToGallery = new MutableLiveData<>();

    public void setBackToGallery(boolean isUpdateErase) {
        this.isBackToGallery.setValue(new Event<>(isUpdateErase));
    }

    public boolean isDeleteRecording;
    public boolean isDownloaded;
    public boolean isPhotoDownload;
    public boolean isVideoDownload;

    public int isSelectedItemPosition;

    public int popupPos = -1;

    public ArrayList<GalleryBottomManageModel> filterImageList = new ArrayList<>();

    public GalleryBottomManageModel isSelectedFilePath;


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

    /*
    dialog dismissed when app goes bg to fg if its shown
     */
    public DialogFragment dialogFragment;
    public int checkedItemCount;

    /* for this handled work manager task complete status*/
    public boolean hasWorkMangerRunAll;
    public boolean hasWorkMangerRunImage;
    public boolean hasWorkMangerRunVideo;

    public int totalImageFileCompleteCount = 0;
    public int totalImageFileCheckedCount = 0;
    public int totalVideoFileCompleteCount = 0;
    public int totalVideoFileCheckedCount = 0;
    public int totalAllFileCompleteCount = 0;
    public int totalAllFileCheckedCount = 0;

    public int getTotalCheckedImageFileCount() {
        return totalImageFileCheckedCount;
    }

    public void incrementImageCheckedFileCount() {
        this.totalImageFileCheckedCount++;
    }

    public int getTotalCheckedVideoFileCount() {
        return totalVideoFileCheckedCount;
    }

    public void incrementVideoCheckedFileCount() {
        this.totalVideoFileCheckedCount++;
    }


    public int getTotalCheckedAllFileCount() {
        return totalAllFileCheckedCount;
    }

    public void incrementAllCheckedFileCount() {
        this.totalAllFileCheckedCount++;
    }

    public int getTotalVideoFileCompleteCount() {
        return totalVideoFileCompleteCount;
    }

    public void incrementVideoFileCount() {
        this.totalVideoFileCompleteCount++;
    }

    public int getTotalAllFileCompleteCount() {
        return totalAllFileCompleteCount;
    }

    public void incrementAllFileCount() {
        this.totalAllFileCompleteCount++;
    }

    public void incrementImageFileCount() {
        totalImageFileCompleteCount++;
    }

    public int getTotalImageFileCount() {
        return totalImageFileCompleteCount;
    }

    private final MutableLiveData<Boolean> _hasShowGalleryAllProgressbar = new MutableLiveData<>();
    public LiveData<Boolean> hasShowGalleryAllProgressbar = _hasShowGalleryAllProgressbar;

    private final MutableLiveData<Boolean> _hasShowGalleryImgProgressbar = new MutableLiveData<>();
    public LiveData<Boolean> hasShowGalleryImgProgressbar = _hasShowGalleryImgProgressbar;

    private final MutableLiveData<Boolean> _hasShowGalleryVideoProgressbar = new MutableLiveData<>();
    public LiveData<Boolean> hasShowGalleryVideoProgressbar = _hasShowGalleryVideoProgressbar;

    public boolean isHasWorkMangerRunAll() {
        return hasWorkMangerRunAll;
    }

    public void setHasWorkMangerRunAll(boolean hasWorkMangerRunAll) {
        this.hasWorkMangerRunAll = hasWorkMangerRunAll;
    }

    public boolean isHasWorkMangerRunImage() {
        return hasWorkMangerRunImage;
    }

    public void setHasWorkMangerRunImage(boolean hasWorkMangerRunImage) {
        this.hasWorkMangerRunImage = hasWorkMangerRunImage;
    }

    public boolean isHasWorkMangerRunVideo() {
        return hasWorkMangerRunVideo;
    }

    public void setHasWorkMangerRunVideo(boolean hasWorkMangerRunVideo) {
        this.hasWorkMangerRunVideo = hasWorkMangerRunVideo;
    }

    public void notificationBuilder(String message, int notificationId) {
        NotificationCompat.Builder completeNotificationBuilder =
                new NotificationCompat.Builder(getApplication().getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(FILE_DOWNLOAD)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .setSmallIcon(R.drawable.image_sionyx);


        if (ActivityCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(getApplication().getApplicationContext()).notify(notificationId, completeNotificationBuilder.build());
    }

    public void hasShowGalleryAllProgressbar(boolean hasShowProgressBar) {
        _hasShowGalleryAllProgressbar.setValue(hasShowProgressBar);
    }

    public void hasShowGalleryImgProgressbar(boolean hasShowProgressBar) {
        _hasShowGalleryImgProgressbar.setValue(hasShowProgressBar);
    }

    public void hasShowGalleryVideoProgressbar(boolean hasShowProgressBar) {
        _hasShowGalleryVideoProgressbar.setValue(hasShowProgressBar);
    }

    private final MutableLiveData<Event<Boolean>> _isOpenTimeZoneListView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isOpenTimeZoneListView = _isOpenTimeZoneListView;

    private final MutableLiveData<Event<Integer>> _isClosedTimeZoneListView = new MutableLiveData<>();
    public LiveData<Event<Integer>> isClosedTimeZoneListView = _isClosedTimeZoneListView;

    public void openTimeZoneListview() {
        _isOpenTimeZoneListView.setValue(new Event<>(true));
    }

    public void hasSetAndClosedTimeZoneListLayout(Integer selectedTimeZoneValue) {
        _isClosedTimeZoneListView.setValue(new Event<>(selectedTimeZoneValue));
    }

    public static boolean isShowTimeZoneLayout() {
        return isShowTimeZoneLayout;
    }

    public static void setShowTimeZoneLayout(boolean isShowTimeZoneLayout) {
        HomeViewModel.isShowTimeZoneLayout = isShowTimeZoneLayout;
    }

    public static boolean isSelectedTimeZone() {
        return isSelectedTimeZone;
    }

    public static void setSelectedTimeZone(boolean isSelectedTimeZone) {
        HomeViewModel.isSelectedTimeZone = isSelectedTimeZone;
    }
                 /* NWD password reset 10sec delay logic*/
    private CountDownTimer timer;
    private MutableLiveData<Integer> progressLiveData = new MutableLiveData<>(0);

    public LiveData<Integer> getProgress() {
        return progressLiveData;
    }

    public int progress = 0;
    public boolean start = false;

    public void startProgress() {
        Log.e(TAG, "startProgress: called");
        if (progressLiveData.getValue() != null && progressLiveData.getValue() > 0) {
            // already running, don’t restart
            return;
        }else {
            start = true;
        }

        new Thread(() -> {
        while (start) {
            progress += 1;
            int finalProgress = progress;

            handler.post(() -> {
                progressLiveData.postValue(finalProgress);
            });

            try {
                Thread.sleep(100); // simulate work
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        }).start();
    }


    public void resetProgress() {
        Log.e(TAG, "resetProgress: called" + progress );
        start = false;
        progress = 0;
        progressLiveData.postValue(0);
    }

    // to display password prompt dialog
    private final MutableLiveData<Boolean> message = new MutableLiveData<>();
    public LiveData<Boolean> getShowDialog() { return message; }

    public void showDialog(boolean bool) {
        message.postValue(bool);
    }

    // NWD Firmware version dialog not dismissed //
    private final MutableLiveData<Boolean> timerFinished = new MutableLiveData<>();
    public final MutableLiveData<Boolean> navigateLiveScreen = new MutableLiveData<>();
    private ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    public LiveData<Boolean> firmwarePromptDismissed() {
        return timerFinished;
    }

    public LiveData<Boolean> getTimerFinishedAndNavigateToLive() {
        return navigateLiveScreen;
    }

    public void resetTimer(){
        executor.shutdownNow();
    }

    public void startTimer() {
        ensureExecutor();
        executor.schedule(() -> {
            timerFinished.postValue(true);
        }, 4, TimeUnit.SECONDS);
    }

    private void ensureExecutor() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            Log.e(TAG, "ensureExecutor terminated" );
            executor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    //Nightwave Analog golden update
    public static boolean isGoldenUpdate = false;// to change true if this update required in Home screen

}
