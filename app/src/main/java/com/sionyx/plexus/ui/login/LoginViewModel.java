package com.sionyx.plexus.ui.login;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.sionyx.plexus.ui.api.AWSCommunication;
import com.sionyx.plexus.ui.api.interfaces.CloudInterface;
import com.sionyx.plexus.ui.api.interfaces.ForgotPasswordResultCallback;
import com.sionyx.plexus.ui.api.interfaces.ValidUserInterface;
import com.sionyx.plexus.ui.api.requestModel.RequestChangeNewPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestForgotPassword;
import com.sionyx.plexus.ui.dialog.PasswordDialogFragment;
import com.sionyx.plexus.ui.profile.QRScanModel;
import com.sionyx.plexus.utils.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginViewModel extends AndroidViewModel {

    public final String TAG = "LoginViewModel";
    public String currentDeviceKey;
    private SiOnyxLoginRepository siOnyxLoginRepository;
    public DialogFragment passwordDialogFragment;
    private PasswordDialogListener passwordDialogListener;
    private FragmentManager fragmentManager;

    public boolean isAnimationComplete = false;

    public boolean isFirstTimeAttached = false;
    public boolean isAlreadySignedOut = false;
    public boolean progressState = false;
    public String ForgotPasswordUserName;

    public String getForgotPasswordUserName() {
        return ForgotPasswordUserName;
    }

    public void setForgotPasswordUserName(String forgotPasswordUserName) {
        ForgotPasswordUserName = forgotPasswordUserName;
    }

    private String userName;
    private String password;

    public boolean isProgressState() {
        return progressState;
    }

    public void setProgressState(boolean progressState) {
        this.progressState = progressState;
    }

    public boolean isAlreadySignedOut() {
        return isAlreadySignedOut;
    }

    public void setAlreadySignedOut(boolean alreadySignedOut) {
        isAlreadySignedOut = alreadySignedOut;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isFirstTimeAttached() {
        return isFirstTimeAttached;
    }

    public void setFirstTimeAttached(boolean firstTimeAttached) {
        isFirstTimeAttached = firstTimeAttached;
    }

    public boolean isAnimationComplete() {
        return isAnimationComplete;
    }

    public void setAnimationComplete(boolean animationComplete) {
        isAnimationComplete = animationComplete;
    }

    ArrayList<PasswordDialogFragment> passwordDialogFragments = new ArrayList<>();

    public String getCurrentDeviceKey() {
        return currentDeviceKey;
    }

    public void setCurrentDeviceKey(String currentDeviceKey) {
        this.currentDeviceKey = currentDeviceKey;
    }

    private final MutableLiveData<Event<Boolean>> _isLogin = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isLogin = _isLogin;

    private final MutableLiveData<Event<Boolean>> _isSkipLogin = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSkipLogin = _isSkipLogin;
    private final MutableLiveData<Event<Boolean>> _isRegister = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isRegister = _isRegister;

    private final MutableLiveData<Event<Boolean>> _isSelectForgotPassword = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectForgotPassword = _isSelectForgotPassword;

    private final MutableLiveData<Event<Boolean>> _isShowCustomProgressbar = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isShowCustomProgressbar = _isShowCustomProgressbar;

    private final MutableLiveData<Event<Boolean>> _isShowInternetConnectionFailed = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isShowInternetConnectionFailed = _isShowInternetConnectionFailed;

    private final MutableLiveData<Event<Boolean>> _isRequirePasswordReset = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isRequirePasswordReset = _isRequirePasswordReset;

    private final MutableLiveData<Event<Boolean>> _isLogout = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isLogout = _isLogout;

    private final MutableLiveData<Event<Boolean>> _isSelectProfile = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectProfile = _isSelectProfile;

    private final MutableLiveData<Event<Boolean>> _isLoadingProgressbar = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isLoadingProgressbar = _isLoadingProgressbar;

    private final MutableLiveData<Event<Boolean>> _resetEditTextFields = new MutableLiveData<>();
    public LiveData<Event<Boolean>> resetEditTextFields = _resetEditTextFields;

    public void resetForgotPasswordFields(boolean iseResetByUser) {
        _resetEditTextFields.setValue(new Event<>(iseResetByUser));
    }

    public PasswordDialogListener getPasswordDialogListener() {
        return passwordDialogListener;
    }

    public void setPasswordDialogListener(PasswordDialogListener passwordDialogListener) {
        this.passwordDialogListener = passwordDialogListener;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public ArrayList<PasswordDialogFragment> getPasswordDialogFragments() {
        return passwordDialogFragments;
    }

    public void setPasswordDialogFragments(ArrayList<PasswordDialogFragment> passwordDialogFragments) {
        this.passwordDialogFragments = passwordDialogFragments;
    }

    public LoginViewModel(@NonNull Application application) {
        super(application);
        siOnyxLoginRepository = SiOnyxLoginRepository.getInstance(application);
    }

    public void signOut() {
        _isLogout.postValue(new Event<>(true));
    }

    public void onSelectProfile() {
        _isSelectProfile.postValue(new Event<>(true));
    }

    public void register() {
        _isRegister.setValue(new Event<>(true));
    }

    public void login() {
        _isLogin.setValue(new Event<>(true));
    }

    public void skipLogin() {
        _isSkipLogin.postValue(new Event<>(true));
    }

    public void hasLoadingProgressbar(boolean hasShowProgressbar) {
        _isLoadingProgressbar.postValue(new Event<>(hasShowProgressbar));
    }

    public void hasShowCustomProgressbar(boolean isShowProgressbar) {
        _isShowCustomProgressbar.postValue(new Event<>(isShowProgressbar));
    }

    public void showInternetConnectionFailedDialog() {
        _isShowInternetConnectionFailed.postValue(new Event<>(true));
    }

    public void onSelectForgotPassword() {
        _isSelectForgotPassword.setValue(new Event<>(true));
    }

    public void isPasswordResetRequire() {
        _isRequirePasswordReset.setValue(new Event<>(true));
    }

    private AWSCommunication awsCommunication = null;

    private AWSCommunication getAwsCommunication() {
        if (awsCommunication == null) {
            awsCommunication = new AWSCommunication();
        }
        return awsCommunication;
    }

    // Sionyx Login Methods
    public void getUserEmailAlreadyExist(RequestForgotPassword requestForgotPassword, final ValidUserInterface validUserInterface) {
        getAwsCommunication().sentForgotPasswordOtp(requestForgotPassword, new ForgotPasswordResultCallback() {
            @Override
            public void onSuccess(String forgotPasswordResponse, int statusCode) {
                validUserInterface.onSuccess(statusCode, forgotPasswordResponse);
                Log.d(TAG, "onSuccess getMessage: ");
            }

            @Override
            public void onFailure(String error) {
                validUserInterface.onFailure(error);
                Log.d(TAG, "onFailure: " + error);
            }
        });
    }

    public void changeNewPassword(RequestChangeNewPassword requestChangeNewPassword, final CloudInterface cloudInterface) {
        getAwsCommunication().changeNewPassword(requestChangeNewPassword, new ForgotPasswordResultCallback() {
            @Override
            public void onSuccess(String forgotPasswordResponse, int status) {
                cloudInterface.onSuccess(String.valueOf(status), forgotPasswordResponse);
                Log.d(TAG, "onSuccess getMessage: " + forgotPasswordResponse);
            }

            @Override
            public void onFailure(String error) {
                cloudInterface.onFailure(error);
                Log.d(TAG, "onFailure: " + error);
            }
        });
    }

    public void initializeAWS(Context context, final CloudInterface cloudInterface) {
        siOnyxLoginRepository.initializeAWS(context, new CloudInterface() {
            @Override
            public void onSuccess(final String token, final String state) {
                Log.e(TAG, "initialize process 2" + " " + state);
                if (state.equalsIgnoreCase("SIGNED_IN")) {
                    siOnyxLoginRepository.getDeviceList(new CloudInterface() {
                        @Override
                        public void onSuccess(String deviceId, String msg) {
                            //setCurrentDeviceKey(deviceId);
                            if (msg.equalsIgnoreCase("User Registered Successfully")) {
                                cloudInterface.onSuccess(token, state);//should send state only
                            } else {
                                cloudInterface.onSuccess(token, state);
                            }
                        }

                        @Override
                        public void onFailure(final String error) {
                            if (error.equalsIgnoreCase("ALREADY SIGNED_OUT")) {
                                try {
                                    siOnyxLoginRepository.signOutAwscall();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            cloudInterface.onFailure(error);
                        }
                    });

                } else if (state.equalsIgnoreCase("SIGNED_OUT")) {
                    cloudInterface.onSuccess(token, state);
                }
            }

            @Override
            public void onFailure(final String error) {
                Log.d(TAG, "onFailure: " + error);
                cloudInterface.onFailure(error);
            }
        });
    }

    public void updateUserAttribute(String email) {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("custom:signIN", "True");
        attributeMap.put("email", email);
        // Update user attributes
        AWSMobileClient.getInstance().updateUserAttributes(attributeMap, new Callback<List<UserCodeDeliveryDetails>>() {
            @Override
            public void onResult(List<UserCodeDeliveryDetails> result) {
                // User attributes updated successfully
                Log.d(TAG, "updateUserAttributes: " + result);
            }

            @Override
            public void onError(Exception e) {
                // Error updating user attributes
                Log.d(TAG, "onError: " + e.getMessage());
            }
        });
    }

    public void signIN(String mInputUserName, String password, final CloudInterface cloudInterface) {
        siOnyxLoginRepository.signIN(mInputUserName, password, new CloudInterface() {
            @Override
            public void onSuccess(final String token, final String state) {
                cloudInterface.onSuccess(token, state);
            }

            @Override
            public void onFailure(final String error) {
                cloudInterface.onFailure(error);
            }
        });
    }

    public void userAlreadySignIn(final CloudInterface cloudInterface) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Map<String, String> userAttributes = AWSMobileClient.getInstance().getUserAttributes();
                String email = userAttributes.get("email");
                Log.d(TAG, "userAlreadySignIn: " + email);

                Map<String, String> attributeMap = new HashMap<>();
                attributeMap.put("custom:signIN", "False");
                attributeMap.put("email", email);
                siOnyxLoginRepository.signOutAlreadyLogin(attributeMap, new CloudInterface() {
                    @Override
                    public void onSuccess(final String deviceKey, String msg) {
                        cloudInterface.onSuccess(getCurrentDeviceKey(), msg);
                    }

                    @Override
                    public void onFailure(String error) {
                        cloudInterface.onFailure(error);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void globalLogout(final CloudInterface cloudInterface) {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Map<String, String> userAttributes = AWSMobileClient.getInstance().getUserAttributes();
                String email = userAttributes.get("email");
                Log.d(TAG, "userAlreadySignIn: " + email);

                Map<String, String> attributeMap = new HashMap<>();
                attributeMap.put("custom:signIN", "False");
                attributeMap.put("email", email);
                siOnyxLoginRepository.globalLogout(attributeMap, new CloudInterface() {
                    @Override
                    public void onSuccess(final String deviceKey, String msg) {
                        executor.shutdownNow();
                        cloudInterface.onSuccess(getCurrentDeviceKey(), msg);
                    }

                    @Override
                    public void onFailure(String error) {
                        executor.shutdownNow();
                        cloudInterface.onFailure(error);
                    }
                });
            } catch (Exception e) {
                cloudInterface.onFailure(e.getMessage());
            }
        });

    }

    public void confirmForgotPassword(String newpwd, String confimationcode, final CloudInterface cloudInterface) {
        siOnyxLoginRepository.confirmForgotPassword(newpwd, confimationcode, new CloudInterface() {
            @Override
            public void onSuccess(String state, String msg) {
                cloudInterface.onSuccess(state, msg);
            }

            @Override
            public void onFailure(String error) {
                cloudInterface.onFailure(error);
            }
        });
    }

    public interface PasswordDialogListener {
        void onDialogSentConfirmationClick(DialogFragment dialog, String strUserName, boolean showCustomProgressbar);

        void onDialogNewPasswordSubmitClick(DialogFragment dialog, String strUserName, String newPassword, String confirmationCode, boolean showCustomProgressbar);

        void onDialogAlreadySignedInOKClick(DialogFragment dialog);

        void onDialogLogoutClick(DialogFragment dialog);

        void onDialogQRCodeResultSave(DialogFragment dialog, QRScanModel qrScanModel);

        void onDialogCancelClick(DialogFragment dialog);

        void onDialogDeleteProductOkClick(DialogFragment dialog);

    }

}
