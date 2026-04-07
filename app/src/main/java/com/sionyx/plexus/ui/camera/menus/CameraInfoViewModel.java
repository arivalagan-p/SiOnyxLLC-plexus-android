package com.sionyx.plexus.ui.camera.menus;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.utils.Event;

public class CameraInfoViewModel extends AndroidViewModel {
    public CameraInfoViewModel(@NonNull Application application) {
        super(application);
    }

    private String TAG = "CameraInfoViewModel";
    public String cameraSSId;

    public Boolean isUpdateCameraName;

    public Boolean getUpdateCameraName() {
        return isUpdateCameraName;
    }

    public void setUpdateCameraName(Boolean updateCameraName) {
        isUpdateCameraName = updateCameraName;
    }

    private final MutableLiveData<Event<Boolean>> _isCancel = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancel = _isCancel;

    private final MutableLiveData<Event<Boolean>> _isCancelChangeNamePassword = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancelChangeNamePassword = _isCancelChangeNamePassword;

    private final MutableLiveData<Event<Boolean>> _isChangeNamePassword = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isChangeNamePassword = _isChangeNamePassword;

    private final MutableLiveData<Event<Boolean>> _isBackToSettings = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isBackToSettings = _isBackToSettings;

    private final MutableLiveData<Event<Boolean>> _isBackToDeviceInfo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isBackToDeviceInfo = _isBackToDeviceInfo;

    private final MutableLiveData<Event<Boolean>> _isSaveChanges = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSaveChanges = _isSaveChanges;

    private final MutableLiveData<Event<Boolean>> _isFactoryReset = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isFactoryReset = _isFactoryReset;

    private final MutableLiveData<Event<Boolean>> _isCancelFactoryReset = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancelFactoryReset = _isCancelFactoryReset;

    private final MutableLiveData<Event<Boolean>> _isResetMyDevice = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isResetMyDevice = _isResetMyDevice;

    private final MutableLiveData<Event<Object>> _opsinProductVersionDetails = new MutableLiveData<>();
    public LiveData<Event<Object>> opsinProductVersionDetails = _opsinProductVersionDetails;


    private final MutableLiveData<Event<Object>> _opsinSdCardInfo = new MutableLiveData<>();
    public LiveData<Event<Object>> opsinSdCardInfo = _opsinSdCardInfo;

    public void onCancel() {
        _isCancel.setValue(new Event<>(true));
    }

    public void onCancelChangeNamePassword() {
        _isCancelChangeNamePassword.setValue(new Event<>(true));
    }

    public void onFactoryReset() {
        _isFactoryReset.setValue(new Event<>(true));
    }

    public void onBackToSettings() {
        _isBackToSettings.setValue(new Event<>(true));
    }

    public void onChangeNamePassword() {
        _isChangeNamePassword.setValue(new Event<>(true));
    }

    public void onSaveChanges() {
        _isSaveChanges.setValue(new Event<>(true));
    }

    public void onBackToDeviceInfo() {
        _isBackToDeviceInfo.setValue(new Event<>(true));
    }

    public void onCancelFactoryReset() {
        _isCancelFactoryReset.setValue(new Event<>(true));
    }

    public void onRestMyDevice() {
        _isResetMyDevice.setValue(new Event<>(true));
    }

    public void observeCameraVersionDetails(Object opsinVersionDetails) {
        _opsinProductVersionDetails.setValue(new Event<>(opsinVersionDetails));
    }

    public void observeOpsinSdCardInfo(Object sdCardInfo) {
        _opsinSdCardInfo.setValue(new Event<>(sdCardInfo));
    }

    // Analog Firmware update

    public final MutableLiveData<com.sionyx.plexus.utils.Event<Integer>> _isFwAvailableDialogDismiss = new MutableLiveData<>();
    public LiveData<com.sionyx.plexus.utils.Event<Integer>> isFwAvailableDialogDismiss = _isFwAvailableDialogDismiss;

    private final MutableLiveData<Event<Boolean>> _isFirmwareUpdateAvailable = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isFirmwareUpdateAvailable = _isFirmwareUpdateAvailable;

    public final MutableLiveData<com.sionyx.plexus.utils.Event<Integer>> _isFwRetryDialogInLive = new MutableLiveData<>();
    public LiveData<com.sionyx.plexus.utils.Event<Integer>> isFwRetryDialogInLive = _isFwRetryDialogInLive;

    private final MutableLiveData<Boolean> firmwareDialogNeeded = new MutableLiveData<>(false);

    public void setIsFwAvailableDialogDismissInLive(int value) {
        _isFwAvailableDialogDismiss.setValue(new com.sionyx.plexus.utils.Event<>(value));
    }

    public void onFirmwareUpdate() {
        _isFirmwareUpdateAvailable.setValue(new Event<>(true));
    }
    public LiveData<Boolean> isFirmwareDialogNeeded() {
        return firmwareDialogNeeded;
    }
    public void setFirmwareDialogNeeded(boolean needed) {
        firmwareDialogNeeded.setValue(needed);
    }
}
