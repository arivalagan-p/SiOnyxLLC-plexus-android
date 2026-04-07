package com.dome.librarynightwave.viewmodel;

import static com.dome.librarynightwave.model.repository.TCPRepository.opsinObserverUpgradeComplete;

import android.app.Application;
import android.graphics.Bitmap;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.opsinmodel.DateTime;
import com.dome.librarynightwave.model.repository.pojo.OpsinRTPStats;
import com.dome.librarynightwave.model.repository.pojo.RTPStats;
import com.dome.librarynightwave.utils.Event;
import com.dome.librarynightwave.utils.ZoomablePlayerView;
import com.dome.librarynightwave.utils.ZoomableTextureView;

public class TCPConnectionViewModel extends AndroidViewModel {
    private TCPRepository tcpRepository;
    private boolean isProgramatically = false;
    private boolean isFirmwareUpdateChecked = false;
    private boolean isSSIDSelectedManually = false;
    private boolean isFirmwareUpdateCompleted = false;

    public static Surface videoSurface = null;

    public static final MutableLiveData<String> liveViewErrorMessage = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> hasAllPacketZero = new MutableLiveData<>();
    public static final MutableLiveData<Event<Boolean>> opsinCameraErrorMessage = new MutableLiveData<>();

    public static final MutableLiveData<Event<Boolean>> hostUnreachable = new MutableLiveData<>();


    public LiveData<String> observeLiveViewErrorMessage() {
        return liveViewErrorMessage;
    }

    public LiveData<Event<Boolean>> observeAllPacketZero() {
        return hasAllPacketZero;
    }
    public LiveData<Event<Boolean>> observeHostUnreachable() {
        return hostUnreachable;
    }

    public TCPConnectionViewModel(@NonNull Application application) {
        super(application);
        tcpRepository = TCPRepository.getInstance(application);
    }

    public boolean isSSIDSelectedManually() {
        return isSSIDSelectedManually;
    }

    public void setSSIDSelectedManually(boolean SSIDSelectedManually) {
        isSSIDSelectedManually = SSIDSelectedManually;
    }

    public boolean isFirmwareUpdateCompleted() {
        return isFirmwareUpdateCompleted;
    }

    public void setFirmwareUpdateCompleted(boolean firmwareUpdateCompleted) {
        isFirmwareUpdateCompleted = firmwareUpdateCompleted;
    }

    public boolean isProgramatically() {
        return isProgramatically;
    }

    public void setProgramatically(boolean programatically) {
        isProgramatically = programatically;
    }

    public void connectSocket() {
        tcpRepository.startService();
    }

    /*
    Connect service for NWD
     */
    public void startServiceDigital(){
        tcpRepository.startPlayerService();
    }

    public void stopServiceDigital(){
        tcpRepository.stopRtspService();
    }

    public LiveData<Integer> isSocketConnected() {
        return tcpRepository.isSocketConnected();
    }

    public LiveData<Event<Integer>> isSocketConnectedEvent() {
        return tcpRepository.isSocketConnectedEvent();
    }

    public LiveData<Object> observeFlipVideo() {
        return tcpRepository.observeFlipVideo();
    }

    public LiveData<Object> observeInvertVideo() {
        return tcpRepository.observeInvertVideo();
    }

    public LiveData<Object> observeIRCut() {
        return tcpRepository.observeIRCut();
    }

    public LiveData<Object> observeFpgaVersion() {
        return tcpRepository.observeFPGAVersion();
    }

    public LiveData<Object> observeRisvVersion() {
        return tcpRepository.observeRiscVersion();
    }

    public LiveData<Object> observeReleaseVersion() {
        return tcpRepository.observeReleaseVersion();
    }

    public LiveData<Object> observeRiscBootMode() {
        return tcpRepository.observeRiscBootMode();
    }

    public LiveData<Object> observeCameraName() {
        return tcpRepository.observeCameraName();
    }

    public LiveData<Object> observeCameraPassword() {
        return tcpRepository.observeCameraPassword();
    }

    public LiveData<Object> observeCameraInfo() {
        return tcpRepository.observeCameraInfo();
    }

    public LiveData<Object> observeLedEnableState() {
        return tcpRepository.observeLedEnableState();
    }

    public LiveData<Event<Object>> observeCameraMode() {
        return tcpRepository.observeCameraMode();
    }

    public LiveData<Event<Object>> observeCameraVideoMode() {
        return tcpRepository.observeCameraVideoMode();
    }

    public LiveData<Object> observeCameraExposure() {
        return tcpRepository.observeCameraExposure();
    }

    public MutableLiveData<Bitmap> getValueUpdateLiveViewBitmap() {
        return tcpRepository.getValueUpdateLiveViewBitmap();
    }

    public MutableLiveData<RTPStats> getValueUpdateLiveViewStats() {
        return tcpRepository.getValueUpdateLiveViewStats();
    }

    public MutableLiveData<OpsinRTPStats> getValueOpsinLiveViewStats() {
        return tcpRepository.getValueUpdateOpsinLiveViewStats();
    }

    public LiveData<Event<Object>> observeStartLiveView() {
        return tcpRepository.observeStartLiveView();
    }

    public LiveData<Event<Object>> observeStopLiveView() {
        return tcpRepository.observeStopLiveView();
    }

    public LiveData<Event<Boolean>> observeHasSendLiveViewCommand() {
        return tcpRepository.observeHasSendLiveViewCommand();
    }

    public LiveData<Event<Boolean>> observeHasRecordStoppedDuetoNoLivePackets() {
        return tcpRepository.observeHasRecordStoppedDuetoNoLivePackets();
    }

    public boolean isFirmwareUpdateChecked() {
        return isFirmwareUpdateChecked;
    }

    public void setFirmwareUpdateChecked(boolean firmwareUpdateChecked) {
        isFirmwareUpdateChecked = firmwareUpdateChecked;
    }

    public void startFwUpdate(String sequence) {
        tcpRepository.startFwUpdate(sequence);
    }

    public LiveData<Object> observeFwUpdateProgress() {
        return tcpRepository.observeFwUpdateProgress();
    }

    public LiveData<Event<Boolean>> observeCircleProgressIndicator() {
        return tcpRepository.observeCircleProgressIndicator();
    }

    public LiveData<Event<Object>> observeFwUpdateFailed() {
        return tcpRepository.observeFwUpdateFailed();
    }

    public void setFlipVideo(int isTrue) {
        tcpRepository.setFlipVideo(isTrue);
    }

    public void setInvertVideo(int isTrue) {
        tcpRepository.setInvertVideo(isTrue);
    }

    public void setIRCut(int val) {
        tcpRepository.setIRCut(val);
    }

    public void setChangeContrast(int value) {
        tcpRepository.setChangeContrast(value);
    }

    public void setChangeSharpness(int value) {
        tcpRepository.setChangeSharpness(value);
    }

    public void setChangeSaturation(int value) {
        tcpRepository.setChangeSaturation(value);
    }

    public void setCameraName(String cameraName) {
        tcpRepository.setCameraName(cameraName);
    }

    public void setTopReleaseVersion(String releaseVersion) {
        tcpRepository.setTopReleaseVersion(releaseVersion);
    }

    public void setLedEnableState(int isTrue) {
        tcpRepository.setLedEnableState(isTrue);
    }

    public void setCameraVideoMode(int videoMode) {
        tcpRepository.setCameraVideoMode(videoMode);
    }

    public void setCameraExposure(int exposureValue) {
        tcpRepository.setCameraExposure(exposureValue);
    }

    public void getInvertImage() {
        tcpRepository.getInvertImage();
    }

    public void getFlipImage() {
        tcpRepository.getFlipImage();
    }

    public void getIRCut() {
        tcpRepository.getIRCut();
    }

    public void getFPGAVersion() {
        tcpRepository.getFPGAVersion();
    }

    public void getRiscVersion() {
        tcpRepository.getRiscVersion();
    }

    public void getReleasePkgVer() {
        tcpRepository.getReleasePkgVer();
    }

    public void getRiscBootMode() {
        tcpRepository.getRiscBootMode();
    }

    public void getCameraName() {
        tcpRepository.getCameraName();
    }

    public void getWiFiPassword() {
        tcpRepository.getWiFiPassword();
    }

    public void getCameraMode() {
        tcpRepository.getCameraMode();
    }

    public void getVideoMode() {
        tcpRepository.getVideoMode();
    }

    public void getCameraExposure() {
        tcpRepository.getCameraExposure();
    }

    public void resetFPGA(boolean isGolden) {
        tcpRepository.resetFPGA(isGolden);
    }

    public void getCameraInfo() {
        tcpRepository.getCameraInfo();
    }

    public void getLedEnableState() {
        tcpRepository.getLedEnableState();
    }

    public void startLiveView(boolean isManual) {
        tcpRepository.startLiveView(isManual);
    }

    public void stopLiveView() {
        tcpRepository.stopLiveView();
    }

    public void disconnectSocket() {
        tcpRepository.disconnectSocket();
    }

    @Override
    protected void onCleared() {
        tcpRepository.onCleared();
        tcpRepository.clearAllLiveData();
    }

    public void reset() {
        tcpRepository = null;
    }

    public void resetSocketState() {
        tcpRepository.resetSocketState();
    }

    public void fwUpdateCancel() {
        tcpRepository.fwUpdateCancel();
    }


    /*-----------------------------------------------------------------------------OPSIN------------------------------------------------------------------------------------------------*/
    /*OPSIN PRODUCT COMMANDS*/

    public void addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND command){
        tcpRepository.addOpsinPeriodicTimerCommand(command);
    }
    public void clearPeriodicRequestList(){
        tcpRepository.clearPeriodicRequestList();
    }
    public void  triggerKeepAlive(boolean shouldStartStreaming){
        tcpRepository.triggerKeepAlive(shouldStartStreaming);
    }
    public void cancelOpsinPeriodicTimer(){
        tcpRepository.cancelOpsinPeriodicTimer();
    }
    public void getProductVersion() {
        tcpRepository.getProductVersion();
    }

    public void getProductInfo() {
        tcpRepository.getProductInfo();
    }

    public void getSerialNumber() {
        tcpRepository.getSerialNumber();
    }

    public void getTimeZone() {
        tcpRepository.getTimeZone();
    }

    public void getDateTime() {
        tcpRepository.getDateTime();
    }

    public void getDigitalZoom() {
        tcpRepository.getDigitalZoom();
    }

    public void getNUC() {
        tcpRepository.getNUC();
    }

    public void getMicState() {
        tcpRepository.getMicState();
    }

    public void getFrameRate() {
        tcpRepository.getFrameRate();
    }

    public void getJpegCompression() {
        tcpRepository.getJpegCompression();
    }

    public void getOpsinCameraName() {
        tcpRepository.getOpsinCameraName();
    }

    public void getGpsInfo() {
        tcpRepository.getGpsInfo();
    }
    public void getGpsPosition() {
        tcpRepository.getGpsPosition();
    }
    public void startOpsinCameraRecord() {
        tcpRepository.startOpsinCameraRecord();
    }

    public void stopOpsinCameraRecord() {
        tcpRepository.stopOpsinCameraRecord();
    }

    public void setTextureView(ZoomableTextureView textureView) {
        tcpRepository.setTextureView(textureView);
    }

    public void stopAndResetUDP() {
        tcpRepository.stopAndResetUDP();
    }

    public void resetOpsinLiveStreamingState(){
        tcpRepository.resetOpsinLiveStreamingState();
    }
    public void startOpsinLiveStreaming() {
        tcpRepository.startOpsinLiveStreaming();
    }

    public void stopOpsinLiveStreaming() {
        tcpRepository.stopOpsinLiveStreaming();
    }

    public void stopOpsinStreamingStates() {
        tcpRepository.stopOpsinStreamingStates();
    }

    public void getOpsinRecordingStatus() {
        tcpRepository.getOpsinRecordingStatus();
    }

    public void getOpsinStreamingStatus() {
        tcpRepository.getOpsinStreamingStatus();
    }

    public void getOpsinClockMode() {
        tcpRepository.getOpsinClockMode();
    }

    public void opsinCameraTakeImageToSDCard() {
        tcpRepository.opsinCameraTakeImageToSDCard();
    }

    public void captureOpsinImageLocally(String riscv) {
        tcpRepository.captureOpsinImageLocally(riscv);
    }

    public void startOpsinVideoRecordLocally() {
        tcpRepository.startOpsinVideoRecordLocally();
    }

    public void stopOpsinVideoRecordLocally() {
        tcpRepository.stopOpsinVideoRecordLocally();
    }

    public void restartOpsinCamera() {
        tcpRepository.restartOpsinCamera();
    }

    public void setTimeZone(String timeZone) {
        tcpRepository.setTimeZone(timeZone);
    }

    public void setDateTime(DateTime dateTime) {
        tcpRepository.setDateTime(dateTime);
    }

    public void setDigitalZoom(byte digitalZoom) {
        tcpRepository.setDigitalZoom(digitalZoom);
    }

    public void setNUC(byte state) {
        tcpRepository.setNUC(state);
    }

    public void setMicState(byte state) {
        tcpRepository.setMicState(state);
    }

    public void setSetFrameRate(short frameRate) {
        tcpRepository.setSetFrameRate(frameRate);
    }

    public void setJpegCompression(byte value) {
        tcpRepository.setJpegCompression(value);
    }

    public void setGpsPower(byte value) {
        tcpRepository.setGpsPower(value);
    }

    public void setFactoryReset(byte value) {
        tcpRepository.setFactoryReset(value);
    }

    public void setOpsinCameraName(String name) {
        tcpRepository.setOpsinCameraName(name);
    }

    public void getOpsinImageState() {
        tcpRepository.getOpsinImageState();
    }

    /*OPSIN Upgrade COMMANDS*/
    public void startOpsinFwUpdate(String sequence) {
        tcpRepository.startOpsinFwUpdate(sequence);
    }

    public void opsinUpgradeCancel() {
        tcpRepository.opsinUpgradeCancel();
    }

    /*OPSIN WIRELESS COMMANDS*/
    public void getWirelessPassword() {
        tcpRepository.getWirelessPassword();
    }

    public void getWirelessMac() {
        tcpRepository.getWirelessMac();
    }

    /*OPSIN ISP COMMANDS*/
    public void getEv() {
        tcpRepository.getEv();
    }

    public void setEv(byte[] data) {
        tcpRepository.setEv(data);
    }

    public void getOpsinBrightness() {
        tcpRepository.getOpsinBrightness();
    }

    public void getOpsinMonochromaticState() {
        tcpRepository.getOpsinMonochromatic();
    }

    public void setOpsinMonochromaticState(byte data) {
        tcpRepository.setOpsinMonochromatic(data);
    }

    public void getOpsinNoiseReductionState() {
        tcpRepository.getOpsinNoiseReduction();
    }

    public void setOpsinNoiseReductionState(byte data) {
        tcpRepository.setOpsinNoiseReduction(data);
    }

    public void getOpsinROI() {
        tcpRepository.getOpsinROI();
    }

    public void setOpsinROI(byte data) {
        tcpRepository.setOpsinROI(data);
    }

    public void getOpsinMetadata() {
        tcpRepository.getOpsinMetadata();
    }

    public void setOpsinMetadata(byte data) {
        tcpRepository.setOpsinMetadata(data);
    }

    public void getOpsinTimeFormat() {
        tcpRepository.getOpsinTimeFormat();
    }

    public void setOpsinTimeFormat(byte data) {
        tcpRepository.setOpsinTimeFormat(data);
    }

    public void setOpsinClockMode(byte data) {
        tcpRepository.setOpsinClockMode(data);
    }

    public void getOpsinSdCardInfomation() {
        tcpRepository.getOpsinSdCardInfo();
    }

    public void getOpsinCompassValue() {
        tcpRepository.getCompassValue();
    }

    public void setOpsinMasterVersion(String sMajor, String sMinor, String sPatch) {
        tcpRepository.setOpsinMasterVersion(sMajor, sMinor, sPatch);
    }

    /*OPSIN POWER COMMANDS*/
    public void getBatteryInfo() {
        tcpRepository.getPowerInfo();
    }

    /*OPSIN PRODUCT Observer*/
    public LiveData<Event<Object>> observeOpsinCommandError() {
        return tcpRepository.observeOpsinCommandError();
    }

    public LiveData<Event<Object>> observeopsinMICCommandError() {
        return tcpRepository.observeopsinMICCommandError();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraErrorMessage() {
        return opsinCameraErrorMessage;
    }

    public LiveData<Event<Object>> observeOpsinCameraVersionInfo() {
        return tcpRepository.observeOpsinCameraVersionInfo();
    }

    public LiveData<Event<Object>> observeOpsinProductInfo() {
        return tcpRepository.observeOpsinProductInfo();
    }

    public LiveData<Event<Object>> observeOpsinGetTimeZone() {
        return tcpRepository.observeOpsinGetTimeZone();
    }

    public LiveData<Event<Object>> observeOpsinGetDateTime() {
        return tcpRepository.observeOpsinGetDateTime();
    }

    public LiveData<Event<Object>> observeOpsinGetDigitalZoom() {
        return tcpRepository.observeOpsinGetDigitalZoom();
    }

    public LiveData<Object> observeOpsinGetNUCState() {
        return tcpRepository.observeOpsinGetNUCState();
    }

    public LiveData<Event<Object>> observeOpsinGetMICState() {
        return tcpRepository.observeOpsinGetMICState();
    }

    public LiveData<Event<Object>> observeOpsinGetFrameRate() {
        return tcpRepository.observeOpsinGetFrameRate();
    }

    public LiveData<Event<Object>> observeOpsinGetJpegCompression() {
        return tcpRepository.observeOpsinGetJpegCompression();
    }

    public LiveData<Event<Object>> observeOpsinGetCameraName() {
        return tcpRepository.observeOpsinGetCameraName();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStartRecord() {
        return tcpRepository.observeOpsinCameraStartRecord();
    }

    public LiveData<Boolean> observeOpsinCameraStopRecord() {
        return tcpRepository.observeOpsinCameraStopRecord();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraImageCaptureSDCard() {
        return tcpRepository.observeOpsinCameraImageCaptureSDCard();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStartStreaming() {
        return tcpRepository.observeOpsinCameraStartStreaming();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraRecordingState() {
        return tcpRepository.observeOpsinCameraRecordingState();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStreamingState() {
        return tcpRepository.observeOpsinCameraStreamingState();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraClockMode() {
        return tcpRepository.observeOpsinCameraClockMode();
    }

    public LiveData<Event<Object>> observeSetOpsinCameraClockMode() {
        return tcpRepository.observeSetOpsinCameraClockMode();
    }

    public LiveData<Event<Boolean>> observeOpsinCameraStopStreaming() {
        return tcpRepository.observeOpsinCameraStopStreaming();
    }

    public LiveData<Event<Object>> observeOpsinSetTimeZone() {
        return tcpRepository.observeOpsinSetTimeZone();
    }

    public LiveData<Event<Object>> observeOpsinSetDateTime() {
        return tcpRepository.observeOpsinSetDateTime();
    }

    public LiveData<Event<Object>> observeOpsinSetDigitalZoom() {
        return tcpRepository.observeOpsinSetDigitalZoom();
    }

    public LiveData<Event<Object>> observeOpsinSetNUCState() {
        return tcpRepository.observeOpsinSetNUCState();
    }

    public LiveData<Event<Object>> observeOpsinSetMICState() {
        return tcpRepository.observeOpsinSetMICState();
    }

    public LiveData<Event<Object>> observeOpsinSetFrameRate() {
        return tcpRepository.observeOpsinSetFrameRate();
    }

    public LiveData<Event<Object>> observeOpsinSetJpegCompression() {
        return tcpRepository.observeOpsinSetJpegCompression();
    }

    public LiveData<Event<Object>> observeOpsinSetFactoryReset() {
        return tcpRepository.observeOpsinSetFactoryReset();
    }

    public LiveData<Event<Object>> observeOpsinSetCameraName() {
        return tcpRepository.observeOpsinSetCameraName();
    }

    public LiveData<Event<Object>> observeOpsinGetImageState() {
        return tcpRepository.observeOpsinGetImageState();
    }

    public LiveData<Event<Object>> observeOpsinCameraRestart() {
        return tcpRepository.observeOpsinCameraRestart();
    }

    public LiveData<Event<Object>> observeOpsinSetMasterVersion() {
        return tcpRepository.observeOpsinSetMasterVersion();
    }

    public LiveData<Event<Object>> getOpsinMonochromeState() {
        return tcpRepository.observeOpsinMonochromeState();
    }

    public LiveData<Event<Object>> observeSetOpsinMonochromeState() {
        return tcpRepository.observeSetOpsinMonochromeState();
    }

    public LiveData<Event<Object>> getOpsinNoiseState() {
        return tcpRepository.observeOpsinNoiseState();
    }

    public LiveData<Event<Object>> observeSetOpsinNoiseState() {
        return tcpRepository.observeSetOpsinNoiseState();
    }

    public LiveData<Event<Object>> getOpsinROIState() {
        return tcpRepository.observeOpsinROIState();
    }

    public LiveData<Event<Object>> observeSetOpsinROIState() {
        return tcpRepository.observeSetOpsinROIState();
    }

    public LiveData<Event<Object>> getOpsinMetadataState() {
        return tcpRepository.observeOpsinMetadataState();
    }

    public LiveData<Event<Object>> observeSetOpsinMetadataState() {
        return tcpRepository.observeSetOpsinMetadataState();
    }

    public LiveData<Event<Object>> observeGetOpsinTimeFormat() {
        return tcpRepository.observeGetOpsinTimeFormat();
    }

    public LiveData<Event<Object>> observeSetOpsinTimeFormat() {
        return tcpRepository.observeSetOpsinTimeFormat();
    }

    /*Opsin SD Card Specific Observer*/
    public LiveData<Event<Object>> getOpsinSdCardInfo() {
        return tcpRepository.observeOpsinSdCardInfo();
    }

    /*OPSIN UPGRADE Observer*/
    public LiveData<Event<Object>> observerUpgradeComplete() {
        return tcpRepository.observerUpgradeComplete();
    }

    public LiveData<Event<Object>> observerUpgradeCompleteWaitMsg() {
        return tcpRepository.observerUpgradeCompleteWaitMsg();
    }

    /*OPSIN ISP Observer*/
    public LiveData<Event<Object>> observeOpsinGetEv() {
        return tcpRepository.observeOpsinGetEv();
    }

    public LiveData<Event<Object>> observeOpsinGetBrightness() {
        return tcpRepository.observeOpsinGetBrightness();
    }

    public LiveData<Event<Object>> observeOpsinSetEv() {
        return tcpRepository.observeOpsinSetEv();
    }

    /*OPSIN Wireless Observer*/
    public LiveData<Event<Object>> observeOpsinWirelessPassword() {
        return tcpRepository.observeOpsinWirelessPassword();
    }

    public LiveData<Event<Object>> observeOpsinWirelessMac() {
        return tcpRepository.observeOpsinWirelessMac();
    }

    /*OPSIN GPS Observer*/
    public LiveData<Event<Object>> observeOpsinGetGpsInfo() {
        return tcpRepository.observeOpsinGetGpsInfo();
    }
    public LiveData<Event<Object>> observeOpsinGetGpsLocation() {
        return tcpRepository.observeOpsinGetGpsLocation();
    }
    public LiveData<Event<Object>> observeOpsinSetGpsPower() {
        return tcpRepository.observeOpsinSetGpsPower();
    }

    /*OPSIN POWER Observer*/
    public LiveData<Event<Object>> observeOpsinGetBatteryInfo() {
        return tcpRepository.observeOpsinGetBatteryInfo();
    }


    /*Opsin Compass observer*/
    public LiveData<Event<Object>> getOpsinCompassState() {
        return tcpRepository.observeOpsinCompassState();
    }

    public LiveData<Event<Object>> observeOpsinSerialNumber() {
        return tcpRepository.observeOpsinSerialNumber();
    }

    public void stopOpsinFWTimers() {
        tcpRepository.stopWaitingTimerForUpgradeProgressUpgradeComplete();
        tcpRepository.cancelWaitModeTimer();
        tcpRepository.cancelRestartResetWaitingTimer();
    }

    public void wifiUpdateComplete() {
        opsinObserverUpgradeComplete.postValue(new Event<>("WIFI:UPGRADE_COMPLETE_OTHER"));
    }
    public void bleUpdateComplete() {
        opsinObserverUpgradeComplete.postValue(new Event<>("BLE:UPGRADE_COMPLETE_OTHER"));
    }
    public void fpgaUpdateComplete() {
        opsinObserverUpgradeComplete.postValue(new Event<>("FPGA:UPGRADE_COMPLETE_OTHER"));
    }
    public void riscvFpgaUpdateComplete() {
        opsinObserverUpgradeComplete.postValue(new Event<>("RISCV_FPGA:UPGRADE_COMPLETE_OTHER"));
    }
    public void riscvOverlayUpdateComplete() {
        opsinObserverUpgradeComplete.postValue(new Event<>("RISCV_OVERLAY:UPGRADE_COMPLETE_OTHER"));
    }
    public void riscvRecoveryUpdateComplete() {
        opsinObserverUpgradeComplete.postValue(new Event<>("RECOVERY_WIFI_DISCONNECT:UPGRADE_COMPLETE_OTHER"));
    }
    public void stopPleaseWaitProgressUpdate() {
        tcpRepository.stopPleaseWaitProgressUpdate();
    }


    /*---------------------------------------------------Nightwave Digital --------------------------------------------------------------------- */

    /** Attach surface view for NWD streaming  **/

    public void setVideoContainer(ZoomablePlayerView textureView) {
        tcpRepository.setVideoContainer(textureView);
    }

    // uncomment for video record and Image capture

    public void captureDigitalImageLocally() {
        tcpRepository.captureDigitalImageLocally();
    }

    public void startDigitalVideoRecordLocally() {
        tcpRepository.startDigitalVideoRecordLocally();
    }

    public void stopDigitalVideoRecordLocally() {
        tcpRepository.stopDigitalVideoRecordLocally();
    }

    public LiveData<Event<Integer>> observeOnStreamingErrorCode(){
        return tcpRepository.observeOnStreamingErrorCode();
    }

    public LiveData<Boolean> observeBufferingState(){
        return tcpRepository.observeIsStreamingInBuffer();
    }

    public LiveData<Boolean> observerNoFpsFoundForDigital(){
        return tcpRepository.observerNoFpsFoundForDigital();
    }

    public LiveData<Long> notifyDigitalMediaVideoSize(){
        return tcpRepository.observeDigitalMediaVideoSize();
    }

    public LiveData<Event<Object>> observeRetryCountData() {
        return tcpRepository.observeRetryCountData();
    }

    public LiveData<Boolean> observeStreamingStatus() {
        return tcpRepository.observeStreamingStatus();
    }

    public LiveData<Event<Boolean>> observeStreamingReadyToVisible() {
        return tcpRepository.observeStreamingAttachedToSurfaceView();
    }

    public LiveData<Integer> observeLiveFps() {
        return tcpRepository.observeLiveFps();
    }

    public void releaseStream() {
        tcpRepository.nightwaveDigitalCameraReleaseLiveStreaming();
    }

    public void setInitiateStreaming() {
        tcpRepository.nightwaveDigitalCameraInitializeStreaming();
    }

    public void setStartStreamDigitalCamera() {
        tcpRepository.nightwaveDigitalCameraStartStreaming();
    }

    public void setChangeStreamingDigitalCamera() {
        tcpRepository.nightwaveDigitalCameraChangeResolution();
    }

    public void setStopStreamDigitalCamera() {
        tcpRepository.setStopStreamingDigitalCamera();
    }

    public void setStopRetryingCommand() {
        tcpRepository.setStopRetryAndResetCommand();
    }
}
