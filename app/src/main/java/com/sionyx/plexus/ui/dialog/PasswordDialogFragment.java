package com.sionyx.plexus.ui.dialog;

import static com.sionyx.plexus.utils.Constants.makeToast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.login.LoginViewModel;
import com.sionyx.plexus.ui.profile.ProfileViewModel;
import com.sionyx.plexus.ui.profile.QRScanModel;
import com.sionyx.plexus.ui.splashscreen.SplashScreenViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.NetworkUtils;

import java.util.regex.Pattern;

public class PasswordDialogFragment extends DialogFragment {
    private LoginViewModel loginViewModel;
    private ProfileViewModel profileViewModel;
    private ProgressBar customDialogProgressbar;
    private TextView Username_edit_text;
    private TextView qrNameAndModel;
    private TextView qrSerialNumber;
    private TextView qrClassification;
    private TextView qrSkuCode;
    private TextView camera_sku_code, userNotFoundMessage;
    private TextInputEditText etConfirmPassword, etNewPassword, etOtp;

    private String title = "";
    private String message = "";
    private boolean hasShowChangePasswordLayout = false;
    private boolean hasShowAlreadySignInLayout = false;
    private boolean hasShowOtpLayout = false;
    private boolean hasShowLogoutLayout = false;
    private boolean hasShowInternetConnectionFailedLayout = false;
    private boolean hasShowAlreadySignOutOtherDeviceLayout = false;
    private boolean hasShowUserNotFoundLayout = false;
    private boolean hasShowQRScannerLayout = false;
    private boolean hasShowDeleteProductAlertLayout = false;

    private SplashScreenViewModel splashScreenViewModel;
    private HomeViewModel homeViewModel;
    private boolean displayTitle = false;
    private ConstraintLayout sentOtpLayout, changePasswordLayout, alreadySignInLayout, logoutLayout, internetConnectionFailedLayout, alreadySignOutOtherDeviceLayout,
            userNotFoundlayout, qrScannerLayout, qrCodeResultLayout, qrCodeFailedLayout, deleteProductLayout;
    private AppCompatImageView internetFailedOkBtn, alreadySignOutOtherDeviceOkBtn, userNotFoundOkBtn, qrCameraIcon, qrCodeFailedOkBtn, deleteProductOkBtn, deleteProductCancelBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.dialog_theme);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        splashScreenViewModel = new ViewModelProvider(requireActivity()).get(SplashScreenViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        if (getArguments() != null) {
            title = getArguments().getString("title");
            displayTitle = getArguments().getBoolean("displayTitle");
            message = getArguments().getString("message");
            hasShowChangePasswordLayout = getArguments().getBoolean("hasShowChangePasswordLayout");
            hasShowAlreadySignInLayout = getArguments().getBoolean("hasShowAlreadySignInLayout");
            hasShowOtpLayout = getArguments().getBoolean("hasShowOtpLayout");
            hasShowLogoutLayout = getArguments().getBoolean("hasShowLogoutLayout");
            hasShowInternetConnectionFailedLayout = getArguments().getBoolean("hasShowInternetConnectionFailedLayout");
            hasShowAlreadySignOutOtherDeviceLayout = getArguments().getBoolean("hasShowAlreadySignOutOtherDeviceLayout");
            hasShowUserNotFoundLayout = getArguments().getBoolean("hasShowUserNotFoundLayout");
            hasShowQRScannerLayout = getArguments().getBoolean("hasShowQRScannerLayout");
            hasShowDeleteProductAlertLayout = getArguments().getBoolean("hasShowDeleteProductAlertLayout");
        }
        loginViewModel.passwordDialogFragment = PasswordDialogFragment.this;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View dialog = inflater.inflate(R.layout.forgot_password_dialog, container, false);
        /* for this internet connection failed*/
        internetConnectionFailedLayout = dialog.findViewById(R.id.internet_connection_failed_layout);
        internetFailedOkBtn = dialog.findViewById(R.id.internet_failed_ok);

        /*delete product item alert dialog view*/
        deleteProductLayout = dialog.findViewById(R.id.delete_product_alert_layout);
        deleteProductOkBtn = dialog.findViewById(R.id.delete_product_ok_btn);
        deleteProductCancelBtn = dialog.findViewById(R.id.delete_product_cancel_btn);

        /* qr code scan result show dialog*/
        qrScannerLayout = dialog.findViewById(R.id.qr_code_layout);
        qrCodeResultLayout = dialog.findViewById(R.id.qr_code_result_layout);
        qrCodeFailedLayout = dialog.findViewById(R.id.qr_code_failed_layout);
        qrCodeFailedOkBtn = dialog.findViewById(R.id.qr_code_failed_ok);
        qrNameAndModel = dialog.findViewById(R.id.camera_name_model);
        qrSerialNumber = dialog.findViewById(R.id.camera_serial_number);
        qrClassification = dialog.findViewById(R.id.camera_classification);
        qrSkuCode = dialog.findViewById(R.id.camera_sku_code);
        qrCameraIcon = dialog.findViewById(R.id.qr_camera_icon);

        TextView scannerSaveBtn = dialog.findViewById(R.id.scanner_save);
        TextView scannerDeleteBtn = dialog.findViewById(R.id.scanner_delete);

        /* user not found dialog*/
        userNotFoundlayout = dialog.findViewById(R.id.user_not_found_layout);
        userNotFoundOkBtn = dialog.findViewById(R.id.user_not_found_ok);
        userNotFoundMessage = dialog.findViewById(R.id.user_not_found_msg);

        /* Already sign out from other device*/
        alreadySignOutOtherDeviceLayout = dialog.findViewById(R.id.already_sign_out_other_device_layout);
        alreadySignOutOtherDeviceOkBtn = dialog.findViewById(R.id.already_sign_out_other_device_ok);

        /* for this logout*/
        logoutLayout = dialog.findViewById(R.id.logout_layout);
        TextView logoutCancelBtn = dialog.findViewById(R.id.logout_tv_cancel);
        TextView logoutOkBtn = dialog.findViewById(R.id.logout_tv_ok);

        /*for this already sign in layout*/
        TextView tvOk = dialog.findViewById(R.id.tv_ok);
        TextView tvCancel = dialog.findViewById(R.id.tv_cancel);
        alreadySignInLayout = dialog.findViewById(R.id.already_signin_layout);

        /*for this forgot password layout*/
        Username_edit_text = dialog.findViewById(R.id.Username_edit_text);
        Username_edit_text.setText(""); // clear filed initial state
        TextView sent_confirm_code_btn = dialog.findViewById(R.id.sent_confirm_code);
        TextView tv_title = dialog.findViewById(R.id.title_text);
        TextView otp_cancel_btn = dialog.findViewById(R.id.otp_cancel_btn);
        sentOtpLayout = dialog.findViewById(R.id.sent_otp_layout);

        /* for this new password change layout*/
        changePasswordLayout = dialog.findViewById(R.id.change_password_layout);
        TextView pwdSubmitBtn = dialog.findViewById(R.id.new_pwd_submit_btn);
        TextView newPwdCancelBtn = dialog.findViewById(R.id.new_pwd_cancel_btn);
        TextInputLayout etNewPasswordLayout = dialog.findViewById(R.id.new_pwd_edit_text_layout);
        TextInputLayout etConfirmPasswordLayout = dialog.findViewById(R.id.confirm_new_pwd_edit_text_layout);

        etOtp = dialog.findViewById(R.id.confirm_code_edit_text);
        etNewPassword = dialog.findViewById(R.id.new_pwd_edit_text);
        etConfirmPassword = dialog.findViewById(R.id.confirm_new_pwd_edit_text);
        customDialogProgressbar = dialog.findViewById(R.id.dialog_custom_progressbar);
        // for initial state
        etOtp.setText("");
        etOtp.requestFocus();
        etOtp.setFocusable(true);
        etNewPassword.setText("");
        etConfirmPassword.setText("");
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        if (!displayTitle) {
            tv_title.setVisibility(View.GONE);
        } else if (!title.equals("")) {
            tv_title.setVisibility(View.VISIBLE);
            tv_title.setText(title);
        }

        if (hasShowOtpLayout) {
            sentOtpLayout.setVisibility(View.VISIBLE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
        }

        if (hasShowChangePasswordLayout) {
            sentOtpLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.VISIBLE);
            userNotFoundlayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            if (customDialogProgressbar.getVisibility() == View.VISIBLE)
                customDialogProgressbar.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.GONE);
            etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }

        if (hasShowAlreadySignInLayout) {
            alreadySignInLayout.setVisibility(View.VISIBLE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
        }

        if (hasShowLogoutLayout) {
            logoutLayout.setVisibility(View.VISIBLE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
        }

        if (hasShowInternetConnectionFailedLayout) {
            internetConnectionFailedLayout.setVisibility(View.VISIBLE);
            logoutLayout.setVisibility(View.GONE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
        }

        if (hasShowAlreadySignOutOtherDeviceLayout) {
            internetConnectionFailedLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.VISIBLE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
        }

        if (hasShowUserNotFoundLayout) {
            if (!message.equals(""))
                userNotFoundMessage.setText(message);
            internetConnectionFailedLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.VISIBLE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.GONE);
        }

        if (hasShowDeleteProductAlertLayout) {
            internetConnectionFailedLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.GONE);
            deleteProductLayout.setVisibility(View.VISIBLE);
        }

        if (hasShowQRScannerLayout) {
            internetConnectionFailedLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.GONE);
            sentOtpLayout.setVisibility(View.GONE);
            changePasswordLayout.setVisibility(View.GONE);
            alreadySignInLayout.setVisibility(View.GONE);
            alreadySignOutOtherDeviceLayout.setVisibility(View.GONE);
            userNotFoundlayout.setVisibility(View.GONE);
            qrScannerLayout.setVisibility(View.VISIBLE);
            deleteProductLayout.setVisibility(View.GONE);

            if (message.equals("True")) {
                qrCodeResultLayout.setVisibility(View.VISIBLE);
                qrCodeFailedLayout.setVisibility(View.GONE);
                QRScanModel qrScanModel = profileViewModel.getQrScanModel();
                if (qrScanModel.getDescription() != null) {
                    qrCameraIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.opsin_connected));
                } else {
                    qrCameraIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_nw_analog_connected));
                }

                qrNameAndModel.setText(qrScanModel.getModel());
                qrSerialNumber.setText(qrScanModel.getSerialNumber());
                qrClassification.setText(qrScanModel.getClassification());
                qrSkuCode.setText(qrScanModel.getSku());
            } else {
                qrCodeResultLayout.setVisibility(View.GONE);
                qrCodeFailedLayout.setVisibility(View.VISIBLE);
            }
        }

        /* forgot password*/
        sent_confirm_code_btn.setOnClickListener(v -> {
            final String strUserName = Username_edit_text.getText().toString().trim();
            if (!strUserName.isEmpty()) {
                // here checked is valid user or not then proceed to change pwd
                loginViewModel.hasShowCustomProgressbar(true);
                hideKeyboard(requireActivity(), dialog.getRootView());
                new Handler().post(() -> {
                    NetworkUtils.pingServer(isSuccessful -> {
                        // Handle the ping result here
                        if (isSuccessful) {
                            // Ping successful
                            new Handler(Looper.getMainLooper()).post(() -> {
                                loginViewModel.setForgotPasswordUserName(strUserName);
                                loginViewModel.getPasswordDialogListener().onDialogSentConfirmationClick(PasswordDialogFragment.this, strUserName, true);
                                //Username_edit_text.setText("");
                            });
                        } else {
                            // Ping failed
                            loginViewModel.hasShowCustomProgressbar(false);
                            hideKeyboard(requireActivity(), dialog.getRootView());
                            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
                            new Handler(Looper.getMainLooper()).postDelayed(() -> loginViewModel.showInternetConnectionFailedDialog(), 500);
                        }
                    });
                });
            } else {
                makeToast(getString(R.string.please_enter_your_username_to_receive_the_otp));
            }
        });

        otp_cancel_btn.setOnClickListener(v -> {
            hideKeyboard(requireActivity(), dialog.getRootView());
            Username_edit_text.setText("");
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        /* new password cahnge*/
        etNewPasswordLayout.setEndIconOnClickListener(v -> {
            // Toggle password visibility
            if (etNewPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                etNewPasswordLayout.setEndIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_eye_open));
            } else {
                etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                etNewPasswordLayout.setEndIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_eye_close));
            }
            // Move cursor to the end of the text
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

        etConfirmPasswordLayout.setEndIconOnClickListener(v -> {
            // Toggle password visibility
            if (etConfirmPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                etConfirmPasswordLayout.setEndIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_eye_open));
            } else {
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                etConfirmPasswordLayout.setEndIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_eye_close));
            }
            // Move cursor to the end of the text
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        userNotFoundOkBtn.setOnClickListener(v -> {
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        pwdSubmitBtn.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();
            String confirm_pwd = etConfirmPassword.getText().toString().trim();
            String confirmationCode = etOtp.getText().toString().trim();

            Log.e("AWS", "mForgetPassowrdDialog" + " " + newPassword + " " + confirm_pwd + " " + confirmationCode);
            if (confirmationCode.equalsIgnoreCase("") || newPassword.equalsIgnoreCase("") || confirm_pwd.equalsIgnoreCase("")) {
                if (confirmationCode.equalsIgnoreCase("")) {
                    etOtp.requestFocus();
                    makeToast(getString(R.string.enterrequirefields));
                }
                if (newPassword.equalsIgnoreCase("")) {
                    etNewPassword.requestFocus();
                    makeToast(getString(R.string.enterrequirefields));
                }
                if (confirm_pwd.equalsIgnoreCase("")) {
                    etConfirmPassword.requestFocus();
                    makeToast(getString(R.string.enterrequirefields));
                }
            } else {
                if (isValidPassword(newPassword, confirm_pwd)) {
                    //   showCustomProgressbar(getString(R.string.pleasewait));
                    new Handler(Looper.getMainLooper()).post(() -> {
                        NetworkUtils.pingServer(isSuccessful -> {
                            // Handle the ping result here
                            if (isSuccessful) {
                                // Ping successful
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    loginViewModel.hasShowCustomProgressbar(true);
                                    hideKeyboard(requireActivity(), dialog.getRootView());
                                    if (!loginViewModel.getForgotPasswordUserName().isEmpty())
                                        loginViewModel.getPasswordDialogListener().onDialogNewPasswordSubmitClick(PasswordDialogFragment.this, loginViewModel.getForgotPasswordUserName(), newPassword, confirmationCode, true);
                                });
                            } else {
                                // Ping failed
                                loginViewModel.hasShowCustomProgressbar(false);
                                hideKeyboard(requireActivity(), dialog.getRootView());
                                loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
                                new Handler(Looper.getMainLooper()).postDelayed(() -> loginViewModel.showInternetConnectionFailedDialog(), 500);
                            }
                        });
                    });
                }
            }
        });

        newPwdCancelBtn.setOnClickListener(v -> {
            hideKeyboard(requireActivity(), dialog.getRootView());
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        /*Already sign in*/
        tvOk.setOnClickListener(v -> {
            new Handler().post(() -> {
                NetworkUtils.pingServer(isSuccessful -> {
                    // Handle the ping result here
                    if (isSuccessful) {
                        // Ping successful
                        new Handler(Looper.getMainLooper()).post(() -> {
                            loginViewModel.hasShowCustomProgressbar(true);
                            loginViewModel.getPasswordDialogListener().onDialogAlreadySignedInOKClick(PasswordDialogFragment.this);
                        });
                    } else {
                        // Ping failed
                        loginViewModel.hasShowCustomProgressbar(false);
                        hideKeyboard(requireActivity(), dialog.getRootView());
                        loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> loginViewModel.showInternetConnectionFailedDialog(), 500);
                    }
                });
            });
        });

        tvCancel.setOnClickListener(v -> {
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        /* Logout user*/
        logoutOkBtn.setOnClickListener(v -> {
            new Handler().post(() -> {
                NetworkUtils.pingServer(isSuccessful -> {
                    // Handle the ping result here
                    if (isSuccessful) {
                        // Ping successful
                        new Handler(Looper.getMainLooper()).post(() -> {
                            loginViewModel.hasShowCustomProgressbar(true);
                            loginViewModel.getPasswordDialogListener().onDialogLogoutClick(PasswordDialogFragment.this);
                        });
                    } else {
                        // Ping failed
                        loginViewModel.hasShowCustomProgressbar(false);
                        loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> loginViewModel.showInternetConnectionFailedDialog(), 500);
                    }
                });
            });
        });

        logoutCancelBtn.setOnClickListener(v -> {
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        internetFailedOkBtn.setOnClickListener(v -> loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this));

        alreadySignOutOtherDeviceOkBtn.setOnClickListener(v -> loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this));

        loginViewModel.isShowCustomProgressbar.observe(getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                customDialogProgressbar.setVisibility(View.VISIBLE);
            } else {
                customDialogProgressbar.setVisibility(View.GONE);
            }
        }));


        scannerSaveBtn.setOnClickListener(v -> {
            if (customDialogProgressbar.getVisibility() != View.VISIBLE) {
                loginViewModel.hasShowCustomProgressbar(true);
                QRScanModel qrScanModel = new QRScanModel();
                loginViewModel.getPasswordDialogListener().onDialogQRCodeResultSave(PasswordDialogFragment.this, qrScanModel);
            }
        });

        scannerDeleteBtn.setOnClickListener(v -> {
            if (customDialogProgressbar.getVisibility() != View.VISIBLE)
                loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        qrCodeFailedOkBtn.setOnClickListener(v -> {
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        deleteProductOkBtn.setOnClickListener(v -> {
            if (customDialogProgressbar.getVisibility() != View.VISIBLE) {
                loginViewModel.getPasswordDialogListener().onDialogDeleteProductOkClick(PasswordDialogFragment.this);
            }
        });

        deleteProductCancelBtn.setOnClickListener(v -> {
            loginViewModel.getPasswordDialogListener().onDialogCancelClick(PasswordDialogFragment.this);
        });

        splashScreenViewModel.getQrCodeSaveState().observe(this, newData -> {
            Log.d("PasswordDialogFragment", "getQrCodeSaveState: " + newData);
            // Update UI with new data
            if (newData.contains("qrCodeSaveState")) {
                loginViewModel.hasShowCustomProgressbar(false);
                if (profileViewModel.isAlreadyExistQRData()) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dismiss();
                        splashScreenViewModel.setQrCodeSaveState("");
                        profileViewModel.hasFailedUploadProduct();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        dismiss();
                        homeViewModel.getNavController().popBackStack(R.id.productListFragment, false);
                        splashScreenViewModel.setQrCodeSaveState(""); // reset key value
                    });

                }
            }
        });


        return dialog;
    }

    private void setWidth() {
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            int widthh = 0;
            int orientation = this.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                widthh = (int) (width * .800);
            } else {
                widthh = (int) (width * .50);
            }

            Dialog dialog = getDialog();
            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.width = widthh;
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    window.setAttributes(params);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isValidPassword(String newPwd, String confirmPwd) {
        Pattern specialCharPatten = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern UpperCasePatten = Pattern.compile("[A-Z ]");
        Pattern lowerCasePatten = Pattern.compile("[a-z ]");
        Pattern digitCasePatten = Pattern.compile("[0-9 ]");

        if (newPwd.length() < 8) {
            etNewPassword.requestFocus();
            makeToast(getString(R.string.message_password_character_length));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (confirmPwd.length() < 8) {
            etConfirmPassword.requestFocus();
            makeToast(getString(R.string.message_password_character_length));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!newPwd.equals(confirmPwd)) {
            etConfirmPassword.requestFocus();
            makeToast(getString(R.string.message_password_same_error));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!specialCharPatten.matcher(newPwd).find()) {
            etNewPassword.requestFocus();
            makeToast(getString(R.string.message_password_specialcharacter));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!specialCharPatten.matcher(confirmPwd).find()) {
            etConfirmPassword.requestFocus();
            makeToast(getString(R.string.message_password_specialcharacter));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!UpperCasePatten.matcher(newPwd).find()) {
            etNewPassword.requestFocus();
            makeToast(getString(R.string.message_password_uppercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!UpperCasePatten.matcher(confirmPwd).find()) {
            etConfirmPassword.requestFocus();
            makeToast(getString(R.string.message_password_uppercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!lowerCasePatten.matcher(newPwd).find()) {
            etNewPassword.requestFocus();
            makeToast(getString(R.string.message_password_lowercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!lowerCasePatten.matcher(confirmPwd).find()) {
            etConfirmPassword.requestFocus();
            makeToast(getString(R.string.message_password_lowercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!digitCasePatten.matcher(newPwd).find()) {
            etNewPassword.requestFocus();
            makeToast(getString(R.string.message_password_number));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!digitCasePatten.matcher(confirmPwd).find()) {
            etConfirmPassword.requestFocus();
            makeToast(getString(R.string.message_password_number));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }
        return true;
    }

    public boolean isValidEmail(CharSequence target) {
        return target != null && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }


    public void setPasswordDialogListener(LoginViewModel.PasswordDialogListener listener, LoginViewModel viewModel) {
        this.loginViewModel = viewModel;
        viewModel.setPasswordDialogListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();
        setWidth();
    }

    private void hideKeyboard(Activity activity, View mView) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            View view = activity.getCurrentFocus();
            if (view == null) {
                view = new View(activity);
            }
            imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}