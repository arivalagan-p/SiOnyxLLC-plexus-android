package com.sionyx.plexus.ui.cameramenu.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.sionyx.plexus.utils.Event;

public class CameraPasswordSettingViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> isNewPwdValidOnce = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isConfirmPwdValidOnce = new MutableLiveData<>(false);

    /** Settings Long press functionality */
    private final MutableLiveData<Event<Boolean>> _isSelectDelete = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectDelete = _isSelectDelete;

    private final MutableLiveData<Event<Boolean>> _isSelectWifiSetting = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectWifiSetting = _isSelectWifiSetting;

    private final MutableLiveData<Event<Boolean>> _isSelectCancel = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectCancel = _isSelectCancel;
    private final MutableLiveData<Event<Boolean>> _isSelectBack = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectBack = _isSelectBack;

    private final MutableLiveData<Event<Boolean>> _hideBottomSheet = new MutableLiveData<>();
    public LiveData<Event<Boolean>> hideBottomSheet = _hideBottomSheet;


    private final MutableLiveData<Event<Boolean>> _isSelectChangePassword = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectChangePassword = _isSelectChangePassword;

    public CameraPasswordSettingViewModel(@NonNull Application application) {
        super(application);
    }

    public void onSelectDelete() {
        _isSelectDelete.postValue(new Event<>(true));
    }

    public void onSelectWifiSetting() {
        _isSelectWifiSetting.postValue(new Event<>(true));
    }
    public void onSelectCancel() {
        _isSelectCancel.setValue(new Event<>(true));
    }
    public void onSelectBack() {
        _isSelectBack.setValue(new Event<>(true));
    }

    public void onSelectChangePassword() {
        _isSelectChangePassword.setValue(new Event<>(true));
    }

    public void hideBottomSheetDialog(Boolean isHide){
        _hideBottomSheet.postValue(new Event<>(isHide));
    }

    public static int IS_AUTO_CONNECTED = 0;

    public static int FW_POPUP_VALID_DAYS = 90;

    /* Change password fragment functionality */
    private final MutableLiveData<Event<Boolean>> _isChangePasswordButton = new MutableLiveData<>();

    public LiveData<Event<Boolean>> isChangePasswordButton = _isChangePasswordButton;

    public void onChangePasswordButton(){
        _isChangePasswordButton.setValue(new Event<>(true));
    }

    /** change password popup live data **/

    private final MutableLiveData<Event<Boolean>> _isDialogShowPassword = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDialogShowPassword = _isDialogShowPassword;

    private final MutableLiveData<Event<Boolean>> _isDialogConnect = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDialogConnect = _isDialogConnect;

    private final MutableLiveData<Event<String>> _isConnectCamera = new MutableLiveData<>();
    public LiveData<Event<String>> isConnectCamera = _isConnectCamera;

    private final MutableLiveData<Event<Boolean>> _isDialogCancel = new MutableLiveData<>();

    public LiveData<Event<Boolean>> isDialogCancel = _isDialogCancel;

    public void onDialogConnectCamera(){
        _isDialogConnect.setValue(new Event<>(true));
    }

    public void onConnectCamera(String onDialogCameraPassword){
        _isConnectCamera.setValue(new Event<>(onDialogCameraPassword));
    }

    public static WiFiHistory _wifiHistory;

    public WiFiHistory get_wifiHistory(){
        return _wifiHistory;
    }

    private NightwaveDigitalWiFiHistory digitalCameraHistory;

    public void setCurrentDigitalWifiHistory(NightwaveDigitalWiFiHistory wifiHistory){
        this.digitalCameraHistory = wifiHistory;
    }

    public NightwaveDigitalWiFiHistory getCurrentDigitalWifiHistory(){
        return digitalCameraHistory;
    }

    public void onDialogShowPassword(boolean isShown){
        _isDialogShowPassword.setValue(new Event<>(isShown));
    }

    public void onDialogCancel(){
        _isDialogCancel.setValue(new Event<>(true));
    }

    private String onDialogCameraPassword;

    public void setOnDialogCameraPassword(String _onDialogCameraPassword) {
        onDialogCameraPassword = _onDialogCameraPassword;
    }

    public String getOnDialogCameraPassword() {
        return onDialogCameraPassword;
    }

    public LiveData<Boolean> isNewPwdValidOnce() {
        return isNewPwdValidOnce;
    }

    public void setNewPwdValidOnce(boolean isNewPwdValidOnce) {
        this.isNewPwdValidOnce.setValue(isNewPwdValidOnce);
    }

    public LiveData<Boolean> isConfirmPwdValidOnce() {
        return isConfirmPwdValidOnce;
    }

    public void setConfirmPwdValidOnce(boolean isConfirmPwdValidOnce) {
        this.isConfirmPwdValidOnce.setValue(isConfirmPwdValidOnce);
    }
}
