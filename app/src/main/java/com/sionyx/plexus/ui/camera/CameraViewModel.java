package com.sionyx.plexus.ui.camera;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.utils.Event;
import com.sionyx.plexus.utils.Constants;

public class CameraViewModel extends AndroidViewModel {
    private final TCPRepository tcpRepository;

    public CameraViewModel(@NonNull Application application) {
        super(application);
        tcpRepository = new TCPRepository(application);
    }

    public static boolean isShowExpandMenu;
    public static boolean isShowAllMenu;
    public static boolean isShowAlertDialog;
    public static boolean isAnalogMode;
    public static boolean isNewFirmware;
    public static boolean isLiveScreenVideoModeUVC;
    public static boolean isLiveScreenVideoModeWIFI;
    public static boolean hasVisibleSettingsInfoView;
    public static boolean hasAppBackgroundToForeground;
    public static boolean hasPressedSettingCancelIcon;
    public static boolean hasPressedLiveViewButton;
    public static int CameraZoomLevel;
    public static boolean OpsinCommandInitiateByFragment;
    public static boolean OpsinCommandInitiateByDialogFragment;
    public static int cameraXValue;

    public static boolean isSelectArrow;
    public static boolean hasSelectRecordVideoButton;
    public boolean liveViewScreenInBackground;
    public boolean isLiveViewStopped;

    public boolean isLiveViewStopped() {
        return isLiveViewStopped;
    }

    public void setLiveViewStopped(boolean liveViewStopped) {
        isLiveViewStopped = liveViewStopped;
    }

    public boolean isLiveViewScreenInBackground() {
        return liveViewScreenInBackground;
    }

    public void setLiveViewScreenInBackground(boolean liveViewScreenInBackground) {
        this.liveViewScreenInBackground = liveViewScreenInBackground;
    }

    private final static MutableLiveData<Event<Boolean>> _isShowRecordViewMenuLayout = new MutableLiveData<>();
    public static LiveData<Event<Boolean>> isShowRecordViewMenuLayout = _isShowRecordViewMenuLayout;

    public static void hasShowRecordViewMenuLayout(boolean hasShowRecordViewMenuLayout) {
        _isShowRecordViewMenuLayout.setValue(new Event<>(hasShowRecordViewMenuLayout));
    }

    //addition: PiP button state
    public static boolean isSelectPIP = false;

    public static boolean hasCommandInitiated() {
        return OpsinCommandInitiateByFragment;
    }

    public static void setHasCommandInitiate(boolean hasCommandInitiate) {
        CameraViewModel.OpsinCommandInitiateByFragment = hasCommandInitiate;
    }

    public static boolean isOpsinCommandInitiateByDialogFragment() {
        return OpsinCommandInitiateByDialogFragment;
    }

    public static void setOpsinCommandInitiateByDialogFragment(boolean opsinCommandInitiateByDialogFragment) {
        OpsinCommandInitiateByDialogFragment = opsinCommandInitiateByDialogFragment;
    }

    public static int getCameraZoomLevel() {
        return CameraZoomLevel;
    }

    public static void setCameraZoomLevel(int cameraZoomLevel) {
        CameraZoomLevel = cameraZoomLevel;
    }

    public static boolean isHasPressedLiveViewButton() {
        return hasPressedLiveViewButton;
    }

    public static void setHasPressedLiveViewButton(boolean hasPressedLiveViewButton) {
        CameraViewModel.hasPressedLiveViewButton = hasPressedLiveViewButton;
    }

    public static boolean isHasPressedSettingCancelIcon() {
        return hasPressedSettingCancelIcon;
    }

    public static void setHasPressedSettingCancelIcon(boolean hasPressedSettingCancelIcon) {
        CameraViewModel.hasPressedSettingCancelIcon = hasPressedSettingCancelIcon;
    }

    public static boolean isHasAppBackgroundToForeground() {
        return hasAppBackgroundToForeground;
    }

    // To dismiss tooltip when called onforeground from background
    public static boolean orientationHandled = false;

    public static void setHasAppBackgroundToForeground(boolean hasAppBackgroundToForeground) {
        CameraViewModel.hasAppBackgroundToForeground = hasAppBackgroundToForeground;
    }


    public static boolean isHasVisibleSettingsInfoView() {
        return hasVisibleSettingsInfoView;
    }

    public static void setHasVisibleSettingsInfoView(boolean hasVisibleSettingsInfoView) {
        CameraViewModel.hasVisibleSettingsInfoView = hasVisibleSettingsInfoView;
    }

    public static boolean hasNewFirmware() {
        return isNewFirmware;
    }

    public static void setIsNewFirmware(boolean isNewFirmware) {
        CameraViewModel.isNewFirmware = isNewFirmware;
    }

    public static boolean isIsLiveScreenVideoModeUVC() {
        return isLiveScreenVideoModeUVC;
    }

    public static void setIsLiveScreenVideoModeUVC(boolean isLiveScreenVideoModeUVC) {
        CameraViewModel.isLiveScreenVideoModeUVC = isLiveScreenVideoModeUVC;
    }

    public static boolean isIsLiveScreenVideoModeWIFI() {
        return isLiveScreenVideoModeWIFI;
    }

    public static void setIsLiveScreenVideoModeWIFI(boolean isLiveScreenVideoModeWIFI) {
        CameraViewModel.isLiveScreenVideoModeWIFI = isLiveScreenVideoModeWIFI;
    }

    public boolean isAnalogMode() {
        return isAnalogMode;
    }

    public void setAnalogMode(boolean analogMode) {
        isAnalogMode = analogMode;
    }

    public boolean isIsShowAlertDialog() {
        return isShowAlertDialog;
    }

    public void setIsShowAlertDialog(Boolean isShowAlertDialog1) {
        isShowAlertDialog = isShowAlertDialog1;
    }

    public boolean isShowAllMenu() {
        return isShowAllMenu;
    }

    public void setShowAllMenu(Boolean showAllMenu) {
        Constants._isShowAllMenu1.setValue(showAllMenu);
        isShowAllMenu = showAllMenu;
    }

    public boolean isShowExpandMenu() {
        return isShowExpandMenu;
    }

    public void setShowExpandMenu(Boolean showExpandMenu) {
        Constants._isShowExpandMenu1.setValue(showExpandMenu);
        isShowExpandMenu = showExpandMenu;
    }

    private final MutableLiveData<Event<Boolean>> _isSelectSettings = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectSettings = _isSelectSettings;

    private final MutableLiveData<Event<Boolean>> _isSelectHome = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectHome = _isSelectHome;

    private final MutableLiveData<Event<Boolean>> _isSelectInfo = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectInfo = _isSelectInfo;

    private final MutableLiveData<Event<Boolean>> _isSelectCameraMenu = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraMenu = _isSelectCameraMenu;

    //adding pip- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectPictureInPicture = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectPictureInPicture = _isSelectPictureInPicture;

    //adding MenuView- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectMenuView = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectMenuView = _isSelectMenuView;

    //adding settingWebView- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectSettingBtnWeb = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectSettingBtnWeb = _isSelectSettingBtnWeb;

    //adding LeftMenuArrow- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectLeftMenuArrow = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectLeftMenuArrow = _isSelectLeftMenuArrow;

    //adding RightMenuArrow- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectRightMenuArrow = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectRightMenuArrow = _isSelectRightMenuArrow;

    //adding Gallery- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectGallery = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectGallery = _isSelectGallery;

    //adding Camera Record- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecord = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecord = _isSelectCameraRecord;

    //adding Select Camera Live View Screen- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraLiveViewScreen = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraLiveViewScreen = _isSelectCameraLiveViewScreen;

    //adding Camera Record View Menu Cancel- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordViewMenuCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordViewMenuCancel = _isSelectCameraRecordViewMenuCancel;

    //adding Camera Record View Menu Video- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordViewMenuVideo = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordViewMenuVideo = _isSelectCameraRecordViewMenuVideo;

    //adding Record- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectRecord = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectRecord = _isSelectRecord;

    //adding Camera Record Menu Pause- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordMenuPause = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordMenuPause = _isSelectCameraRecordMenuPause;

    //adding Camera Record Menu Cancel- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordMenuCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordMenuCancel = _isSelectCameraRecordMenuCancel;

    //adding Camera Record Save- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordSave = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordSave = _isSelectCameraRecordSave;

    //adding Camera Record Delete- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordDelete = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordDelete = _isSelectCameraRecordDelete;

    //adding Camera Record Confirm delete- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordConfirmDelete = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordConfirmDelete = _isSelectCameraRecordConfirmDelete;

    //adding Camera Record View Cancel- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordViewCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordViewCancel = _isSelectCameraRecordViewCancel;

    //adding Camera Capture- livedata
    private final MutableLiveData<Event<Boolean>> _isSelectCameraCapture = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraCapture = _isSelectCameraCapture;

    // for this sprint3 camera record ui
    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordOptionsCancel = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordOptionsCancel = _isSelectCameraRecordOptionsCancel;

    private final MutableLiveData<Event<Boolean>> _isSelectCameraRecordType = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraRecordType = _isSelectCameraRecordType;

    private final MutableLiveData<Event<Boolean>> _isSelectSaveToMobile = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectSaveToMobile = _isSelectSaveToMobile;

    private final MutableLiveData<Event<Boolean>> _isSelectSaveToCamera = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectSaveToCamera = _isSelectSaveToCamera;

    //adding HomeLiveScreen -livedata
    public static final MutableLiveData<Event<Boolean>> isShowLiveScreen = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> isSaveRecordedVideo = new MutableLiveData<>();

    public LiveData<Event<Boolean>> observeHasShowLiveScreen() {
        return isShowLiveScreen;
    }

    public LiveData<Event<Boolean>> observeHasSaveRecordedVideo() {
        return isSaveRecordedVideo;
    }

    public static boolean onButtonState = false;

    private final MutableLiveData<Event<Boolean>> _isSelectRightArrow = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectRightArrow = _isSelectRightArrow;

    private final MutableLiveData<Event<Boolean>> _isSelectLeftArrow = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectLeftArrow = _isSelectLeftArrow;

    private final MutableLiveData<Event<Boolean>> _isSelectZoomIn = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectZoomIn = _isSelectZoomIn;

    private final MutableLiveData<Event<Boolean>> _isSelectZoomOut = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectZoomOut = _isSelectZoomOut;

    private final MutableLiveData<Event<Boolean>> _isUpdateAspectRatioButtonState = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateAspectRatioButtonState = _isUpdateAspectRatioButtonState;

    private final MutableLiveData<Event<Boolean>> _enableObserver = new MutableLiveData<>();
    public LiveData<Event<Boolean>> enableObserver = _enableObserver;

    public void setEnableObserver() {
        _enableObserver.setValue(new Event<>(true));
    }

    public void setAspectRatioValue() {
        _isUpdateAspectRatioButtonState.setValue(new Event<>(true));
    }

    public static final MutableLiveData<Event<Boolean>> isAllPacketZero = new MutableLiveData<>();

    public LiveData<Event<Boolean>> observeAllPacketZero() {
        return isAllPacketZero;
    }

    public static final MutableLiveData<Event<Boolean>> hasShowVideoModeWifiDialog = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasShowVideoModeUSBDialog = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasShowUsbModeTextMessage = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasDismissPleaseWaitProgressDialog = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> appHasForegroundOrBackground = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasShowProgress = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasSelectRecord = new MutableLiveData<>();

    public LiveData<Event<Boolean>> observeAppHasForegroundOrBackground() {
        return appHasForegroundOrBackground;
    }

    public LiveData<Event<Boolean>> observeHasShowUsbModeTextMessage() {
        return hasShowUsbModeTextMessage;
    }

    public LiveData<Event<Boolean>> observeHasDismissPleaseWaitProgressDialog() {
        return hasDismissPleaseWaitProgressDialog;
    }

    public LiveData<Event<Boolean>> observeHasShowVideoModeWifiDialog() {
        return hasShowVideoModeWifiDialog;
    }

    public LiveData<Event<Boolean>> observeHasShowVideoModeUSBDialog() {
        return hasShowVideoModeUSBDialog;
    }

    public LiveData<Event<Boolean>> observeHasShowProgressbar() {
        return hasShowProgress;
    }

    private final MutableLiveData<Event<Boolean>> _isCropVideo = new MutableLiveData<>();
    LiveData<Event<Boolean>> isCropVideo = _isCropVideo;

    private static final MutableLiveData<Event<Boolean>> _isResetOpsinUI = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isResetOpsinUI = _isResetOpsinUI;

    private static final MutableLiveData<Event<Boolean>> _isVisibleStates = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isVisibleStates = _isVisibleStates;

    private final MutableLiveData<Event<Boolean>> _isSelectCameraPreset = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectCameraPreset = _isSelectCameraPreset;

    private final MutableLiveData<Event<Boolean>> _isStopLiveStreamAndStartSdCardRecording = new MutableLiveData<>();
    LiveData<Event<Boolean>> isStopLiveStreamAndStartSdCardRecording = _isStopLiveStreamAndStartSdCardRecording;

    private final MutableLiveData<Event<Boolean>> _isEnableMultiWindowMode = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isEnableMultiWindowMode = _isEnableMultiWindowMode;

    private final MutableLiveData<Event<Boolean>> _isShowSettingsDialog = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isShowSettingsDialog = _isShowSettingsDialog;

    private final MutableLiveData<Event<Boolean>> _triggerTimerTOUpdateStatusIcons = new MutableLiveData<>();
    LiveData<Event<Boolean>> triggerTimerTOUpdateStatusIcons = _triggerTimerTOUpdateStatusIcons;

    public void triggerTimerToUpdateStatusIcons(boolean shouldTrigger) {
        _triggerTimerTOUpdateStatusIcons.setValue(new Event<>(shouldTrigger));
    }

    public void hasEnableMultiWindowMode() {
        _isEnableMultiWindowMode.setValue(new Event<>(true));
    }

    //adding pip
    public void onSelectPictureInPicture() {
        _isSelectPictureInPicture.setValue(new Event<>(true));
    }

    //adding Menu View
    public void onSelectMenuView() {
        _isSelectMenuView.setValue(new Event<>(true));
    }

    //adding webView for setting
    public void onSelectSettingWebView(){
        _isSelectSettingBtnWeb.setValue(new Event<>(true));
    }

    //adding gallery
    public void onSelectGallery() {
        _isSelectGallery.setValue(new Event<>(true));
    }

    //adding left menu-arrow
    public void onSelectLeftMenuArrow() {
        _isSelectLeftMenuArrow.setValue(new Event<>(true));
    }

    //adding right menu-arrow
    public void onSelectRightMenuArrow() {
        _isSelectRightMenuArrow.setValue(new Event<>(true));
    }

    //adding camera record
    public void onCameraRecord() {
        _isSelectCameraRecord.setValue(new Event<>(true));
    }

    public void onCameraRecordType(boolean recordType) {
        _isSelectCameraRecordType.setValue(new Event<>(recordType));
    }

    //adding camera live view screen
    public void onCameraLiveViewScreen() {
        _isSelectCameraLiveViewScreen.setValue(new Event<>(true));
    }

    //adding camera live view menu cancel
    public void onCameraLiveViewMenuCancel() {
        _isSelectCameraRecordViewMenuCancel.setValue(new Event<>(true));
    }

    //adding camera live view menu video
    public void onCameraLiveViewMenuVideo() {
        _isSelectCameraRecordViewMenuVideo.setValue(new Event<>(true));
    }

    //adding record
    public void onRecord() {
        _isSelectRecord.setValue(new Event<>(true));
    }

    //adding camera record menu pause
    public void onCameraRecordMenuPause() {
        _isSelectCameraRecordMenuPause.setValue(new Event<>(true));
    }

    //adding camera record screen menu cancel
    public void onCameraRecordScreenMenuCancel() {
        _isSelectCameraRecordMenuCancel.setValue(new Event<>(true));
    }

    //adding record save
    public void onRecordSave() {
        _isSelectCameraRecordSave.setValue(new Event<>(true));
    }

    //adding record delete
    public void onRecordDelete() {
        _isSelectCameraRecordDelete.setValue(new Event<>(true));
    }

    //adding confirm delete
    public void onConfirmDelete() {
        _isSelectCameraRecordConfirmDelete.setValue(new Event<>(true));
    }

    //adding record view cancel
    public void onRecordViewCancel() {
        _isSelectCameraRecordViewCancel.setValue(new Event<>(true));
    }

    //adding camera capture
    public void onSelectCameraCapture() {
        _isSelectCameraCapture.setValue(new Event<>(true));
    }

    public void onCameraRecordTypeImageOrVideo() {
        _isSelectCameraRecord.setValue(new Event<>(true));
    }

    public void onSelectRecordOptionsCancel() {
        _isSelectCameraRecordOptionsCancel.setValue(new Event<>(true));
    }

    public void onRecordSaveToMobile() {
        _isSelectSaveToMobile.setValue(new Event<>(true));
    }

    public void onRecordSaveToCamera() {
        _isSelectSaveToCamera.setValue(new Event<>(true));
    }

    public void onSelectSettings() {
        _isSelectSettings.setValue(new Event<>(true));
    }

    public void onSelectHome() {
        _isSelectHome.setValue(new Event<>(true));
    }

    public void onSelectInfo() {
        _isSelectInfo.setValue(new Event<>(true));
    }

    public void onCropVideo() {
        _isCropVideo.setValue(new Event<>(true));
    }

    public void resetOpsinRecordUI() {
        _isResetOpsinUI.setValue(new Event<>(true));
    }

    public void selectZoomIn() {
        _isSelectZoomIn.setValue(new Event<>(true));
    }

    public void selectZoomOut() {
        _isSelectZoomOut.setValue(new Event<>(true));
    }

    public void hasShowStates() {
        _isVisibleStates.setValue(new Event<>(true));
    }

    public void onSelectCameraPreset() {
        _isSelectCameraPreset.setValue(new Event<>(true));
    }

    public void hasStopLiveStreamAndStartSdCardRecording(boolean hasStopStream) {
        _isStopLiveStreamAndStartSdCardRecording.setValue(new Event<>(hasStopStream));
    }


    public static RecordButtonState recordButtonState = RecordButtonState.LIVE_VIEW_STARTED;
    public static OpsinRecordButtonState opsinRecordButtonState = OpsinRecordButtonState.Opsin_Camera_Record_Stop;
    public static AnalyticsButtonState analyticsButtonState = AnalyticsButtonState.Analytics_STOPPED;

    public enum RecordButtonState {
        LIVE_VIEW_STARTED,
        LIVE_VIEW_STOPPED,
    }

    public enum OpsinRecordButtonState {
        Opsin_Camera_Record_Start,
        Opsin_Camera_Record_Stop,
    }

    public enum AnalyticsButtonState {
        Analytics_STARTED,
        Analytics_STOPPED,
    }

    public static VideoCropState videoCropState = VideoCropState.CROP_ON;

    public enum VideoCropState {
        CROP_ON,
        CROP_OFF,
    }

    public void hasAllPacketZero() {
        isAllPacketZero.setValue(new Event<>(true));
    }

    public void hasShowVideoModeWifiDialog(boolean hasShowDialog) {
        hasShowVideoModeWifiDialog.setValue(new Event<>(hasShowDialog));
    }

    public void hasShowVideoModeUSBDialog(boolean hasShowDialog) {
        hasShowVideoModeUSBDialog.setValue(new Event<>(hasShowDialog));
    }

    public void setHasShowUsbModeTextMessage(boolean hasShowMessage) {
        hasShowUsbModeTextMessage.setValue(new Event<>(hasShowMessage));
    }

    public void setAppHasForegroundOrBackground(boolean hasShowMessage) {
        appHasForegroundOrBackground.postValue(new Event<>(hasShowMessage));
    }

    public void setHasDismissPleaseWaitProgressDialog(boolean hasDismissDialog) {
        hasDismissPleaseWaitProgressDialog.setValue(new Event<>(hasDismissDialog));
    }

    public void setHasShowProgress(boolean isShowProgress) {
        hasShowProgress.setValue(new Event<>(isShowProgress));
    }

    public void clearCameraModeState() {
        tcpRepository.valueUpdateCameraMode.postValue(new Event<>(-1));
        //tcpRepository.valueUpdateCameraVideoMode.postValue(-1);
    }

    //addition: handling live view
    public void hasShowLiveViewScreen() {
        isShowLiveScreen.setValue(new Event<>(true));
    }

    public void hasSaveRecordedVideo() {
        isSaveRecordedVideo.setValue(new Event<>(true));
    }

    public void startRecordingVideo() {
        tcpRepository.startVideoRecording();
    }

    public void stopRecordingVideo() {
        tcpRepository.stopVideoRecording(false);
    }

    public void captureImage(String riscv) {
        tcpRepository.captureImage(riscv);
    }

    public static boolean onSelectGallery = false;

    public boolean isCapturedImage;
    public boolean isSelectPreset;

    public boolean isSelectPreset() {
        return isSelectPreset;
    }

    public void setSelectPreset(boolean selectCameraPreset) {
        isSelectPreset = selectCameraPreset;
    }

    public void setIsCapturedImageInLive(boolean isImageCaptured) {
        this.isCapturedImage = isImageCaptured;
    }

    public boolean onSaveImage = false;

    public MutableLiveData<Event<Boolean>> _isDismissCustomDialog = new MutableLiveData<>();

    public LiveData<Event<Boolean>> observeIsDismissCustomDialog() {
        return isDismissCustomDialog;
    }

    public LiveData<Event<Boolean>> isDismissCustomDialog = _isDismissCustomDialog;

    public void hasDismissCustomDialog(boolean value) {
        _isDismissCustomDialog.setValue(new Event<>(value));
    }

    public void hasShowSettingsDialog(boolean hasShow) {
        _isShowSettingsDialog.setValue(new Event<>(hasShow));
    }

}
