package com.sionyx.plexus.ui.camera.menus;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.utils.Event;

public class CameraSettingsViewModel extends AndroidViewModel {
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
    private boolean isProgramatically;
    private boolean isTimeFormat24Hrs;

    private final MutableLiveData<Event<String>> _hasObserveSaveSettings = new MutableLiveData<>();
    LiveData<Event<String>> hasObserveSaveSettings = _hasObserveSaveSettings;

    public void onSaveCameraSettingsBy(String presetName) {
        _hasObserveSaveSettings.setValue(new Event<>(presetName));
    }

    public boolean isProgramatically() {
        return isProgramatically;
    }

    public void setProgramatically(boolean programatically) {
        isProgramatically = programatically;
    }


    public static boolean isIsSettingsScreenVideoModeUVC() {
        return isSettingsScreenVideoModeUVC;
    }

    public static void setIsSettingsScreenVideoModeUVC(boolean isSettingsScreenVideoModeUVC) {
        CameraSettingsViewModel.isSettingsScreenVideoModeUVC = isSettingsScreenVideoModeUVC;
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

    public boolean isGpsSelected() {
        return isGpsSelected;
    }

    public void setGpsSelected(boolean gpsSelected) {
        isGpsSelected = gpsSelected;
    }

    public CameraSettingsViewModel(@NonNull Application application) {
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

    public boolean isDateTimeModeSelected() {
        return isDateTimeModeSelected;
    }

    public void setDateTimeModeSelected(boolean dateTimeModeSelected) {
        isDateTimeModeSelected = dateTimeModeSelected;
    }

    public int getVideoModePosition() {
        return videoModePosition;
    }

    public void setVideoModePosition(int videoModePosition) {
        this.videoModePosition = videoModePosition;
    }

    public boolean isTimeFormat24Hrs() {
        return isTimeFormat24Hrs;
    }

    public void setTimeFormat24Hrs(boolean timeFormat24Hrs) {
        isTimeFormat24Hrs = timeFormat24Hrs;
    }


    private final MutableLiveData<Object>_observeOpsinNUCValue = new MutableLiveData<>();
    LiveData<Object> observeOpsinNUCValue = _observeOpsinNUCValue;

    private final MutableLiveData<Object> _observeOpsinFPSValue = new MutableLiveData<>();
    LiveData<Object> observeOpsinFPSValue = _observeOpsinFPSValue;

    private final MutableLiveData<Object> _observeOpsinMICState = new MutableLiveData<>();
    LiveData<Object> observeOpsinMICState = _observeOpsinMICState;

    private final MutableLiveData<Event<Object>> _observeOpsinJpegCompression = new MutableLiveData<>();
    LiveData<Event<Object>> observeOpsinJpegCompression = _observeOpsinJpegCompression;

    private final MutableLiveData<Object> _observeOpsinMonochromeState = new MutableLiveData<>();
    LiveData<Object> observeOpsinMonochromeState = _observeOpsinMonochromeState;

    private final MutableLiveData<Object> _observeOpsinNoiseState = new MutableLiveData<>();
    LiveData<Object> observeOpsinNoiseState = _observeOpsinNoiseState;

    private final MutableLiveData<Object> _observeOpsinRoiState = new MutableLiveData<>();
    LiveData<Object> observeOpsinRoiState = _observeOpsinRoiState;

    private final MutableLiveData<Event<Boolean>> _isShowVideoModeResponseProgressbar = new MutableLiveData<>();
    LiveData<Event<Boolean>> isShowVideoModeResponseProgressbar = _isShowVideoModeResponseProgressbar;

    public void hasShowVideoModeResponseProgressbar(boolean hasShowProgressbar) {
        _isShowVideoModeResponseProgressbar.setValue(new Event<>(hasShowProgressbar));
    }

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

    private final MutableLiveData<Event<Object>> _isUpdateVideoModeState = new MutableLiveData<>();
    LiveData<Event<Object>> isUpdateVideoModeState = _isUpdateVideoModeState;

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

    public void observeOpsinNUCValue(Object o) {
        _observeOpsinNUCValue.setValue(o);
    }

    public void observeOpsinFPSValue(Object o) {
        _observeOpsinFPSValue.setValue(o);
    }

    public void observeOpsinMICState(Object o) {
        _observeOpsinMICState.setValue(o);
    }

    public void observeOpsinJpegCompression(Object o) {
        _observeOpsinJpegCompression.setValue(new Event<>(o));
    }

    public void observeOpsinMonochromeState(Object o) {
        _observeOpsinMonochromeState.setValue(o);
    }

    public void observeOpsinNoiseState(Object o) {
        _observeOpsinNoiseState.setValue(o);
    }

    public void observeOpsinRoiState(Object o) {
        _observeOpsinRoiState.setValue(o);
    }

    public void updateVideoModeState(Object object) {
      _isUpdateVideoModeState.postValue(new Event<>(object));
    }
}
