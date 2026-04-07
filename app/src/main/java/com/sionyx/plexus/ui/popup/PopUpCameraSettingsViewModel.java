package com.sionyx.plexus.ui.popup;

import android.app.AlertDialog;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.utils.Event;

public class PopUpCameraSettingsViewModel extends AndroidViewModel {
    /*Nightwave*/
    private int irCutTabPosition;
    private int invertVideoTabPosition;
    private int flipVideoTabPosition;
    private int ledTabPosition;
    private int videoModePosition;

    private boolean isFlipVideoSelected = false;
    private boolean isInvertVideoSelected = false;
    private boolean isIRCutSelected = false;
    private boolean isLedSelected = false;
    private boolean isVideoModeSelected = false;
    public static boolean isSettingsScreenVideoModeUVC;
    public static boolean hasPopUpSettingsBackgroundToForeground;
    public static boolean isTimeFormat24Hrs;

    /*Opsin*/
    private int nucTabPosition;
    private int micTabPosition;
    private int jpegCompressionTabPosition;
    private int fpsTabPosition;
    private int timeFormatTabPosition;
    private int utcTabPosition;
    private int gpsTabPosition;
    private int monochromeTabPosition;
    private int noiseTabPosition;
    private int roiTabPosition;
    private int sourceTabPosition;
    private int clockFormatTabPosition;
    private int metadataTabPosition;
    private int dateTimeModePosition;

    private boolean isNucSelected = false;
    private boolean isMicSelected = false;
    private boolean isJpegCompressSelected = false;
    private boolean isFpsSelected = false;
    private boolean isTimeFormatSelected = false;
    private boolean isUtcSelected = false;
    private boolean isGpsSelected = false;
    private boolean isMonochromeSelected = false;
    private boolean isNoiseSelected = false;
    private boolean isRoiModeSelected = false;
    private boolean isSourceModeSelected = false;
    private boolean isClockFormatSelected = false;
    private boolean isMetadataSelected = false;
    private boolean isDateTimeModeSelected = false;
    private boolean isShowTimePicker = false;
    private boolean isShowDatePicker = false;
    private String cameraDate;
    private String cameraTime;

    public String getCameraDate() {
        return cameraDate;
    }

    public void setCameraDate(String cameraDate) {
        this.cameraDate = cameraDate;
    }

    public String getCameraTime() {
        return cameraTime;
    }

    public void setCameraTime(String cameraTime) {
        this.cameraTime = cameraTime;
    }

    private static boolean hasDismissPleaseWaitProgressDialog;

    public static final MutableLiveData<Event<Boolean>> hasShowVideoModeWifiDialog = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasShowVideoModeUSBDialog = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasShowProgress = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasDismissCustomProgressDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Boolean>> hasDismissConfirmationDialog = new MutableLiveData<>();

    public boolean isShowTimePicker() {
        return isShowTimePicker;
    }

    public void setShowTimePicker(boolean showTimePicker) {
        isShowTimePicker = showTimePicker;
    }

    public boolean isShowDatePicker() {
        return isShowDatePicker;
    }

    public void setShowDatePicker(boolean showDatePicker) {
        isShowDatePicker = showDatePicker;
    }

    public static boolean isIsSettingsScreenVideoModeUVC() {
        return isSettingsScreenVideoModeUVC;
    }

    public static void setIsSettingsScreenVideoModeUVC(boolean isSettingsScreenVideoModeUVC) {
        PopUpCameraSettingsViewModel.isSettingsScreenVideoModeUVC = isSettingsScreenVideoModeUVC;
    }

    public int getIrCutTabPosition() {
        return irCutTabPosition;
    }

    public void setIrCutTabPosition(int irCutTabPosition) {
        this.irCutTabPosition = irCutTabPosition;
    }

    public int getInvertVideoTabPosition() {
        return invertVideoTabPosition;
    }

    public void setInvertVideoTabPosition(int invertVideoTabPosition) {
        this.invertVideoTabPosition = invertVideoTabPosition;
    }

    public int getFlipVideoTabPosition() {
        return flipVideoTabPosition;
    }

    public void setFlipVideoTabPosition(int flipVideoTabPosition) {
        this.flipVideoTabPosition = flipVideoTabPosition;
    }

    public int getLedTabPosition() {
        return ledTabPosition;
    }

    public void setLedTabPosition(int ledTabPosition) {
        this.ledTabPosition = ledTabPosition;
    }

    public int getNucTabPosition() {
        return nucTabPosition;
    }

    public void setNucTabPosition(int nucTabPosition) {
        this.nucTabPosition = nucTabPosition;
    }

    public int getMicTabPosition() {
        return micTabPosition;
    }

    public void setMicTabPosition(int micTabPosition) {
        this.micTabPosition = micTabPosition;
    }

    public int getJpegCompressionTabPosition() {
        return jpegCompressionTabPosition;
    }

    public void setJpegCompressionTabPosition(int jpegCompressionTabPosition) {
        this.jpegCompressionTabPosition = jpegCompressionTabPosition;
    }

    public int getFpsTabPosition() {
        return fpsTabPosition;
    }

    public void setFpsTabPosition(int fpsTabPosition) {
        this.fpsTabPosition = fpsTabPosition;
    }

    public int getTimeFormatTabPosition() {
        return timeFormatTabPosition;
    }

    public void setTimeFormatTabPosition(int timeFormatTabPosition) {
        this.timeFormatTabPosition = timeFormatTabPosition;
    }

    public int getUtcTabPosition() {
        return utcTabPosition;
    }

    public void setUtcTabPosition(int utcTabPosition) {
        this.utcTabPosition = utcTabPosition;
    }

    public boolean isNucSelected() {
        return isNucSelected;
    }

    public void setNucSelected(boolean nucSelected) {
        isNucSelected = nucSelected;
    }

    public boolean isMicSelected() {
        return isMicSelected;
    }

    public void setMicSelected(boolean micSelected) {
        isMicSelected = micSelected;
    }

    public boolean isJpegCompressSelected() {
        return isJpegCompressSelected;
    }

    public void setJpegCompressSelected(boolean jpegCompressSelected) {
        isJpegCompressSelected = jpegCompressSelected;
    }

    public boolean isFpsSelected() {
        return isFpsSelected;
    }

    public void setFpsSelected(boolean fpsSelected) {
        isFpsSelected = fpsSelected;
    }

    public boolean isTimeFormatSelected() {
        return isTimeFormatSelected;
    }

    public void setTimeFormatSelected(boolean timeFormatSelected) {
        isTimeFormatSelected = timeFormatSelected;
    }

    public boolean isUtcSelected() {
        return isUtcSelected;
    }

    public void setUtcSelected(boolean utcSelected) {
        isUtcSelected = utcSelected;
    }

    public int getGpsTabPosition() {
        return gpsTabPosition;
    }

    public void setGpsTabPosition(int gpsTabPosition) {
        this.gpsTabPosition = gpsTabPosition;
    }

    public int getMonochromeTabPosition() {
        return monochromeTabPosition;
    }

    public void setMonochromeTabPosition(int monochromeTabPosition) {
        this.monochromeTabPosition = monochromeTabPosition;
    }

    public int getNoiseTabPosition() {
        return noiseTabPosition;
    }

    public void setNoiseTabPosition(int noiseTabPosition) {
        this.noiseTabPosition = noiseTabPosition;
    }

    public int getRoiTabPosition() {
        return roiTabPosition;
    }

    public void setRoiTabPosition(int roiTabPosition) {
        this.roiTabPosition = roiTabPosition;
    }

    public int getSourceTabPosition() {
        return sourceTabPosition;
    }

    public void setSourceTabPosition(int sourceTabPosition) {
        this.sourceTabPosition = sourceTabPosition;
    }

    public int getClockFormatTabPosition() {
        return clockFormatTabPosition;
    }

    public void setClockFormatTabPosition(int clockFormatTabPosition) {
        this.clockFormatTabPosition = clockFormatTabPosition;
    }

    public int getMetadataTabPosition() {
        return metadataTabPosition;
    }

    public void setMetadataTabPosition(int metadataTabPosition) {
        this.metadataTabPosition = metadataTabPosition;
    }

    public int getDateTimeModePosition() {
        return dateTimeModePosition;
    }

    public void setDateTimeModePosition(int dateTimeModePosition) {
        this.dateTimeModePosition = dateTimeModePosition;
    }

    public boolean isDateTimeModeSelected() {
        return isDateTimeModeSelected;
    }

    public void setDateTimeModeSelected(boolean dateTimeModeSelected) {
        isDateTimeModeSelected = dateTimeModeSelected;
    }

    public boolean isGpsSelected() {
        return isGpsSelected;
    }

    public void setGpsSelected(boolean gpsSelected) {
        isGpsSelected = gpsSelected;
    }

    public PopUpCameraSettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean isFlipVideoSelected() {
        return isFlipVideoSelected;
    }

    public void setFlipVideoSelected(boolean flipVideoSelected) {
        isFlipVideoSelected = flipVideoSelected;
    }

    public boolean isInvertVideoSelected() {
        return isInvertVideoSelected;
    }

    public void setInvertVideoSelected(boolean invertVideoSelected) {
        isInvertVideoSelected = invertVideoSelected;
    }

    public boolean isIRCutSelected() {
        return isIRCutSelected;
    }

    public void setIRCutSelected(boolean IRCutSelected) {
        isIRCutSelected = IRCutSelected;
    }

    public boolean isLedSelected() {
        return isLedSelected;
    }

    public void setLedSelected(boolean ledSelected) {
        isLedSelected = ledSelected;
    }

    public boolean isVideoModeSelected() {
        return isVideoModeSelected;
    }

    public void setVideoModeSelected(boolean videoModeSelected) {
        isVideoModeSelected = videoModeSelected;
    }

    public boolean isMonochromeSelected() {
        return isMonochromeSelected;
    }

    public void setMonochromeSelected(boolean monochromeSelected) {
        isMonochromeSelected = monochromeSelected;
    }

    public boolean isNoiseSelected() {
        return isNoiseSelected;
    }

    public void setNoiseSelected(boolean noiseSelected) {
        isNoiseSelected = noiseSelected;
    }

    public boolean isRoiModeSelected() {
        return isRoiModeSelected;
    }

    public void setRoiModeSelected(boolean roiModeSelected) {
        isRoiModeSelected = roiModeSelected;
    }

    public boolean isSourceModeSelected() {
        return isSourceModeSelected;
    }

    public void setSourceModeSelected(boolean sourceModeSelected) {
        isSourceModeSelected = sourceModeSelected;
    }

    public boolean isClockFormatSelected() {
        return isClockFormatSelected;
    }

    public void setClockFormatSelected(boolean clockFormatSelected) {
        isClockFormatSelected = clockFormatSelected;
    }

    public boolean isMetadataSelected() {
        return isMetadataSelected;
    }

    public void setMetadataSelected(boolean metadataSelected) {
        isMetadataSelected = metadataSelected;
    }

    public int getVideoModePosition() {
        return videoModePosition;
    }

    public void setVideoModePosition(int videoModePosition) {
        this.videoModePosition = videoModePosition;
    }
    //time zone Ui
    private final MutableLiveData<Event<Boolean>> _isSelectOpsinTimeZone = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectOpsinTimeZone = _isSelectOpsinTimeZone;

    private final MutableLiveData<Event<Boolean>> _isSelectOpsinTimeSet = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectOpsinTimeSet = _isSelectOpsinTimeSet;

    private final MutableLiveData<Event<Boolean>> _isSelectOpsinDateSet = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectOpsinDateSet = _isSelectOpsinDateSet;

    private final MutableLiveData<Event<Boolean>> _isSelectOpsinSetDateAndTime = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectOpsinSetDateAndTime = _isSelectOpsinSetDateAndTime;

    private final MutableLiveData<Event<Boolean>> _isSelectOpsinSyncLocalDateTime = new MutableLiveData<>();
    LiveData<Event<Boolean>> isSelectOpsinSyncLocalDateTime = _isSelectOpsinSyncLocalDateTime;

    private final MutableLiveData<Event<Boolean>> _isShowVideoModeResponseProgressbar = new MutableLiveData<>();
    LiveData<Event<Boolean>> isShowVideoModeResponseProgressbar = _isShowVideoModeResponseProgressbar;

    private final MutableLiveData<Event<Object>> _isUpdateVideoModeState = new MutableLiveData<>();
    LiveData<Event<Object>> isUpdateVideoModeState = _isUpdateVideoModeState;

    public void isUpdateVideoModeState(Object object) {
        _isUpdateVideoModeState.postValue(new Event<>(object));
    }

    public void hasShowVideoModeResponseProgressbar(boolean hasShowProgressbar) {
        _isShowVideoModeResponseProgressbar.setValue(new Event<>(hasShowProgressbar));
    }

    public void hasSelectTimeZone() {
        _isSelectOpsinTimeZone.setValue(new Event<>(true));
    }

    public void hasSelectDate() {
        _isSelectOpsinDateSet.setValue(new Event<>(true));
    }

    public void hasSelectTime() {
        _isSelectOpsinTimeSet.setValue(new Event<>(true));
    }

    public void hasSelectOpsinSetDateAndTime() {
        _isSelectOpsinSetDateAndTime.setValue(new Event<>(true));
    }

    public void hasSelectOpsinSyncLocalDateTime() {
        _isSelectOpsinSyncLocalDateTime.setValue(new Event<>(true));
    }

    public LiveData<Event<Boolean>> observeHasShowVideoModeWifiDialog() {
        return hasShowVideoModeWifiDialog;
    }

    public LiveData<Event<Boolean>> observeHasShowVideoModeUSBDialog() {
        return hasShowVideoModeUSBDialog;
    }

    public void hasShowVideoModeWifiDialog(boolean hasShowDialog) {
        hasShowVideoModeWifiDialog.setValue(new Event<>(hasShowDialog));
    }

    public void hasShowVideoModeUSBDialog(boolean hasShowDialog) {
        hasShowVideoModeUSBDialog.setValue(new Event<>(hasShowDialog));
    }

    public LiveData<Event<Boolean>> observeHasShowProgressbar() {
        return hasShowProgress;
    }

    public void setHasShowProgress(boolean isShowProgress) {
        hasShowProgress.setValue(new Event<>(isShowProgress));
    }

    public static boolean isHasDismissPleaseWaitProgressDialog() {
        return hasDismissPleaseWaitProgressDialog;
    }

    public static void setHasDismissPleaseWaitProgressDialog(boolean hasDismissPleaseWaitProgressDialog) {
        PopUpCameraSettingsViewModel.hasDismissPleaseWaitProgressDialog = hasDismissPleaseWaitProgressDialog;
    }

    public AlertDialog getPleaseWaitAlertDialog() {
        return pleaseWaitAlertDialog;
    }

    public void setPleaseWaitAlertDialog(AlertDialog pleaseWaitAlertDialog) {
        this.pleaseWaitAlertDialog = pleaseWaitAlertDialog;
    }

    public AlertDialog pleaseWaitAlertDialog;

    public boolean isConfirmationDialogShown;

    public boolean isConfirmationDialogShown() {
        return isConfirmationDialogShown;
    }

    public void setConfirmationDialogShown(boolean confirmationDialogShown) {
        isConfirmationDialogShown = confirmationDialogShown;
    }

    public static boolean isHasPopUpSettingsBackgroundToForeground() {
        return hasPopUpSettingsBackgroundToForeground;
    }

    public static void setHasPopUpSettingsBackgroundToForeground(boolean hasPopUpSettingsBackgroundToForeground) {
        PopUpCameraSettingsViewModel.hasPopUpSettingsBackgroundToForeground = hasPopUpSettingsBackgroundToForeground;
    }

    public LiveData<Event<Boolean>> observeHasDismissCustomProgressDialog() {
        return hasDismissCustomProgressDialog;
    }

    public void setDismissCustomProgressDialog(boolean dismissCustomProgressDialog) {
        hasDismissCustomProgressDialog.setValue(new Event<>(dismissCustomProgressDialog));
    }

    public LiveData<Event<Boolean>> observeHasDismissConfirmationDialog() {
        return hasDismissConfirmationDialog;
    }

    public void setDismissConfirmationDialog(boolean dismissConfirmationDialog) {
        hasDismissConfirmationDialog.setValue(new Event<>(dismissConfirmationDialog));
    }

    public boolean isSocketReconnectCalled;

    public void setSocketReconnectCalled(boolean socketReconnectCalled) {
        isSocketReconnectCalled = socketReconnectCalled;
    }

    public enum RTC_MODE {
        MANUAL,
        AUTOMATIC,
    }

    public RTC_MODE rtcMode = RTC_MODE.MANUAL;
}
