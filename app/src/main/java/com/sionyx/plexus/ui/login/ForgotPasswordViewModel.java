package com.sionyx.plexus.ui.login;

import android.app.Application;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sionyx.plexus.utils.Event;

public class ForgotPasswordViewModel extends AndroidViewModel {

    private String userName;
    private String newPassword;
    private String confirmPassword;
    private String optCode;
    public boolean progressState = false;
    public boolean isShowChangePasswordScreen;
    public String holdResponseMessage;
    private CountDownTimer countDownTimer;

    public boolean isShowPasswordResetRequireMessage;

    public boolean isShowPasswordResetRequireMessage() {
        return isShowPasswordResetRequireMessage;
    }

    public void setShowPasswordResetRequireMessage(boolean showPasswordResetRequireMessage) {
        isShowPasswordResetRequireMessage = showPasswordResetRequireMessage;
    }

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
    }

    public String getHoldResponseMessage() {
        return holdResponseMessage;
    }

    public void setHoldResponseMessage(String holdResponseMessage) {
        this.holdResponseMessage = holdResponseMessage;
    }

    public boolean isShowChangePasswordScreen() {
        return isShowChangePasswordScreen;
    }

    public void setShowChangePasswordScreen(boolean showChangePasswordScreen) {
        isShowChangePasswordScreen = showChangePasswordScreen;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getOptCode() {
        return optCode;
    }

    public void setOptCode(String optCode) {
        this.optCode = optCode;
    }

    public boolean isProgressState() {
        return progressState;
    }

    public void setProgressState(boolean progressState) {
        this.progressState = progressState;
    }

    private final MutableLiveData<Event<Boolean>> _isSentOtpCode = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSentOtpCode = _isSentOtpCode;

    private final MutableLiveData<Event<Boolean>> _isSubmitChangePassword = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSubmitChangePassword = _isSubmitChangePassword;

    private final MutableLiveData<Event<Boolean>> _isBackToLogin = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isBackToLogin = _isBackToLogin;

    private final MutableLiveData<Event<Boolean>> _isShowCustomProgressbar = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isShowCustomProgressbar = _isShowCustomProgressbar;

    private final MutableLiveData<Long> _countdownValue = new MutableLiveData<>();
    public final LiveData<Long> countdownValue = _countdownValue;

    public void hasShowCustomProgressbar(boolean isShowProgressbar) {
        _isShowCustomProgressbar.postValue(new Event<>(isShowProgressbar));
    }

    public void onSentOTPCode() {
        _isSentOtpCode.setValue(new Event<>(true));
    }

    public void onSubmitChangePassword() {
        _isSubmitChangePassword.setValue(new Event<>(true));
    }

    public void onBackToLogin() {
        _isBackToLogin.setValue(new Event<>(true));
    }


    public void startCountdown(long milliseconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(milliseconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                _countdownValue.postValue(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                _countdownValue.postValue(0L); // Countdown finished
            }
        };
        countDownTimer.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
