package com.sionyx.plexus.ui.cameramenu.fragment;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.getWiFiHistory;
import static com.sionyx.plexus.utils.Constants.apiErrorMessage;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.utils.Constants;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentChangePasswordBinding;
import com.sionyx.plexus.databinding.RegexLayoutBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResetCallback;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.PasswordResponseCallback;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.circulardotprogressbar.DotCircleProgressIndicator;

import java.util.Objects;


public class ChangePasswordFragment extends Fragment {

    private final String TAG = "ChangePasswordFragment";
    private FragmentChangePasswordBinding changePasswordBinding;
    private RegexLayoutBinding newPwdRulesBinding;
    private RegexLayoutBinding confirmPwdRulesBinding;

    private boolean isValidNewPwd = false;
    private boolean isValidConfirmPwd = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private HomeViewModel homeViewModel;
    private CameraPasswordSettingViewModel cameraSetPasswordViewModel;
    private DigitalCameraInfoViewModel digitalCameraInfoViewModel;

    private final int MAXIMUM_PASSWORD_LENGTH = 63;
    private final int MINIMUM_PASSWORD_LENGTH = 8;

    public static String cameraSsid;
    public static String cameraPassword;

    public static String currentPassword;
    private WiFiHistory wiFiHistory;

    private boolean isOldPwdFilled = false;

    private LifecycleOwner lifecycleOwner;
    private boolean isTransformationChanging = false;

    private enum FocusedField {NONE, NEW, CONFIRM}
    private FocusedField focusedField = FocusedField.NONE;

    private static MainActivity activity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        changePasswordBinding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        lifecycleOwner = this;
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        cameraSetPasswordViewModel = new ViewModelProvider(requireActivity()).get(CameraPasswordSettingViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(requireActivity()).get(DigitalCameraInfoViewModel.class);
        assert changePasswordBinding.customProgressBar != null;
        changePasswordBinding.customProgressBar.setIndicator(new DotCircleProgressIndicator());
        changePasswordBinding.setLifecycleOwner(lifecycleOwner);
        changePasswordBinding.setViewModel(cameraSetPasswordViewModel);
        initiateView();

        return changePasswordBinding.getRoot();
    }

    private void initiateView() {
        changePasswordBinding.changePwdBack.setOnClickListener(v -> cameraSetPasswordViewModel.onSelectBack());
        cameraSetPasswordViewModel.isSelectBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                homeViewModel.getNavController().navigate(R.id.cameraWifiSettingsFragment);
            }
        }));

        cameraSetPasswordViewModel.isNewPwdValidOnce().observe(lifecycleOwner, isValid -> isValidNewPwd = isValid);
        cameraSetPasswordViewModel.isConfirmPwdValidOnce().observe(lifecycleOwner, isValid -> isValidConfirmPwd = isValid);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToHome();
            }
        });

        wiFiHistory = getWiFiHistory();
        newPwdRulesBinding = changePasswordBinding.newPwdRules;
        confirmPwdRulesBinding = changePasswordBinding.confirmPwdRules;

        newPwdRulesBinding.getRoot().setVisibility(View.GONE);
        confirmPwdRulesBinding.getRoot().setVisibility(View.GONE);

        changePasswordBinding.editNewPwdText.setLongClickable(false);
        changePasswordBinding.editNewPwdText.setTextIsSelectable(false);
        changePasswordBinding.editConfirmPwdText.setLongClickable(false);
        changePasswordBinding.editConfirmPwdText.setTextIsSelectable(false);
        changePasswordBinding.cameraName.setText(wiFiHistory.getCamera_ssid());


        changePasswordBinding.editNewPwdText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Log.e(TAG, "focusedField = FocusedField.NEW" );
                focusedField = FocusedField.NEW;
                if (!TextUtils.isEmpty(changePasswordBinding.editNewPwdText.getText()) && !isValidNewPwd) {
                    Log.e(TAG, "showRegexLayout called in changePasswordBinding.editNewPwdText" );
                    updatePasswordRulesUI(changePasswordBinding.editNewPwdText.getText().toString());
                    showRegexLayout();
                }
            } else if (focusedField == FocusedField.NEW) {
                focusedField = FocusedField.NONE;
            }
        });

        changePasswordBinding.editConfirmPwdText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Log.e(TAG, "focusedField = FocusedField.CONFIRM" );
                focusedField = FocusedField.CONFIRM;
                if (!TextUtils.isEmpty(changePasswordBinding.editConfirmPwdText.getText()) && !isValidConfirmPwd) {
                    Log.e(TAG, "showRegexLayout called in changePasswordBinding.editConfirmPwdText" );
                    updatePasswordRulesUI(changePasswordBinding.editConfirmPwdText.getText().toString());
                    showRegexLayout();
                }
            } else if (focusedField == FocusedField.CONFIRM) {
                focusedField = FocusedField.NONE;
            }
        });
        ///NWD password reset 10sec delay logic
        /*homeViewModel.getShowDialog().observe(getViewLifecycleOwner(), isBoolean -> {
            if (isBoolean) {
                activity.showDialog = MainActivity.ShowDialog.NWD_CAMERA_REBOOT_DIALOG;
                activity.showDialog("", getString(R.string.camera_reboot_msg), null);
                restartCamera();
            }
        });*/

        changePasswordBinding.editNewPwdText.addTextChangedListener(createWatcher());
        changePasswordBinding.editConfirmPwdText.addTextChangedListener(createWatcher());

        changePasswordBinding.showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isTransformationChanging = true; // stop regex override temporarily
            changePasswordBinding.editNewPwdText.setTransformationMethod(isChecked ? null : new PasswordTransformationMethod());
            changePasswordBinding.editConfirmPwdText.setTransformationMethod(isChecked ? null : new PasswordTransformationMethod());
            changePasswordBinding.editOldPwdText.setTransformationMethod(isChecked ? null : new PasswordTransformationMethod());

            changePasswordBinding.editNewPwdText.setSelection(changePasswordBinding.editNewPwdText.length());
            changePasswordBinding.editConfirmPwdText.setSelection(changePasswordBinding.editConfirmPwdText.length());
            changePasswordBinding.editOldPwdText.setSelection(changePasswordBinding.editOldPwdText.length());

            if(isChecked){
                Log.e(TAG, "showRegexLayout called in showPassword");
                showRegexLayout();
            }else {
                Log.e(TAG, "showRegexLayout called in hideAllRegexLayouts");
                hideAllRegexLayouts();
            }
            isTransformationChanging = false;
        });

        cameraSetPasswordViewModel.isChangePasswordButton.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                String oldPwd = Objects.requireNonNull(changePasswordBinding.editOldPwdText.getText()).toString();
                String newPwd = Objects.requireNonNull(changePasswordBinding.editNewPwdText.getText()).toString();
                String confirmPwd = Objects.requireNonNull(changePasswordBinding.editConfirmPwdText.getText()).toString();

                if (changePasswordBinding.customProgressBar != null) {
                    changePasswordBinding.customProgressBar.setVisibility(VISIBLE);
                }

                if (TextUtils.isEmpty(newPwd) || TextUtils.isEmpty(confirmPwd)) {
                    if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                        changePasswordBinding.customProgressBar.setVisibility(GONE);
                    }
                    Toast.makeText(getActivity(), getText(R.string.password_empty_msg), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isPasswordValid(newPwd)) {
                    if (!oldPwd.equals(newPwd)) {
                        if (newPwd.equals(confirmPwd)) {
                            updatePasswordToDb(wiFiHistory.getCamera_ssid(), confirmPwd);
                        } else {
                            if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                                changePasswordBinding.customProgressBar.setVisibility(GONE);
                            }
                            Toast.makeText(getActivity(), getText(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                            changePasswordBinding.editConfirmPwdText.setText("");
                        }
                    } else {
                        if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                            changePasswordBinding.customProgressBar.setVisibility(GONE);
                        }
                        Toast.makeText(getActivity(), getText(R.string.old_new_pwd_same_error), Toast.LENGTH_SHORT).show();
                        changePasswordBinding.editNewPwdText.setText("");
                        changePasswordBinding.editConfirmPwdText.setText("");
                    }
                } else {
                    if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                        changePasswordBinding.customProgressBar.setVisibility(GONE);
                    }
                    Toast.makeText(getActivity(), getText(R.string.password_invalid_msg), Toast.LENGTH_SHORT).show();
                    changePasswordBinding.editNewPwdText.requestFocus();
                    changePasswordBinding.editNewPwdText.setSelection(changePasswordBinding.editNewPwdText.getText().length());
                }
            }
        }));
    }
    private void startTimer(String ssid, String password) {
        cameraSsid = ssid;
        cameraPassword = password;
        activity = (MainActivity) getActivity();
        if (activity == null || !isAdded()) return;

        Log.d(TAG, "Timer started → showDialog");

        activity.showDialog = MainActivity.ShowDialog.NWD_CAMERA_PASSWORD_RESET;
        activity.showDialog("", "", null);

    }

    @Override
    public void onResume() {
        super.onResume();
        Editable pwdText = changePasswordBinding.editOldPwdText.getText();
        if (pwdText != null) {
            currentPassword = pwdText.toString();
        }

        if (TextUtils.isEmpty(currentPassword) && currentPassword.isEmpty()) {
            if (!isOldPwdFilled) {
                digitalCameraInfoViewModel.getUpdatedPasswordFromCamera(wiFiHistory.getCamera_ssid(), requireContext());
                digitalCameraInfoViewModel.cameraResponse.observe(lifecycleOwner, new EventObserver<>(password ->
                        requireActivity().runOnUiThread(() -> {
                            Log.e(TAG, "getUpdatedPasswordFromCamera " + password);
                            currentPassword = password;
                            changePasswordBinding.editOldPwdText.setText(currentPassword);
                            isOldPwdFilled = true;
                        })
                ));
            }
        }
    }

    private void updatePasswordToDb(String cameraSsid, String password) {
        // update password to server
        digitalCameraInfoViewModel.postCameraPassword(password, new PasswordResponseCallback() {
            @Override
            public void onSuccess(String response, int responseCode) {
                if (responseCode == Constants.ON_SUCCESS) {
                    Log.e(TAG, "Post password successfully " + response);

                    // after password changed, we forcefully updated the auto connect value is 0 = false
                    digitalCameraInfoViewModel.updateCameraAutoConnect(cameraSsid, 0);

                    // handle progress bar
                    mHandler.postDelayed(() -> restartCamera(cameraSsid, password), 2000);
//                    startTimer(cameraSsid,password); //NWD 10sec delay
                } else {
                    apiErrorMessage(getContext(), responseCode);
                }
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Post password failure " + message);
            }
        });
    }

    // once posted the password camera should be rebooted
    private void restartCamera(String cameraSsid, String password) {
        digitalCameraInfoViewModel.setCameraFactoryReboot(new CameraResetCallback() {
            @Override
            public void onSuccess(boolean isReset, int responseCode) {
                if (responseCode == Constants.ON_SUCCESS) {
                    Log.e(TAG, "Camera rebooted successfully " + password);
                    // update password to local DB

                    mHandler.postDelayed(() -> {
                        digitalCameraInfoViewModel.updateCameraPassword(cameraSsid, password);
                        assert changePasswordBinding.customProgressBar != null;
                        if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                            changePasswordBinding.customProgressBar.setVisibility(GONE);
                        }

//                        homeViewModel.showDialog(false);
                        showCameraRebootDialog(getString(R.string.camera_reboot_msg));
                    }, 0);
                } else {
                    apiErrorMessage(getContext(), responseCode);
                    assert changePasswordBinding.customProgressBar != null;
                    if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                        changePasswordBinding.customProgressBar.setVisibility(GONE);
                    }
                }
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Camera rebooted failure " + message);
                assert changePasswordBinding.customProgressBar != null;
                if (changePasswordBinding.customProgressBar.getVisibility() == VISIBLE) {
                    changePasswordBinding.customProgressBar.setVisibility(GONE);
                }
            }
        });
    }

    private void showCameraRebootDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.NWD_CAMERA_REBOOT_DIALOG;
            activity.showDialog("", message, null);
        }
    }
    private void backToHome() {
        if (homeViewModel.getNavController() != null) {
            homeViewModel.getNavController().popBackStack(R.id.changePasswordFragment, true);
            homeViewModel.getNavController().popBackStack(R.id.cameraWifiSettingsFragment, true);
            homeViewModel.getNavController().navigate(R.id.homeFragment);
        }
    }
    private TextWatcher createWatcher() {
        return new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (isTransformationChanging) return;

                String password = charSequence.toString();

                // Update UI rule indicators
                updatePasswordRulesUI(password);

                // Determine validity
                boolean isValid = isPasswordValid(password);

                // Update ViewModel using a clean helper
                updateFieldValidity(isValid);

                // Expand/collapse rules section
                showRegexLayout();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    collapseView(false);
                }
            }
        };
    }

    private void updateFieldValidity(boolean isValid) {
        if (focusedField == FocusedField.NEW) {
            cameraSetPasswordViewModel.setNewPwdValidOnce(isValid);
        } else if (focusedField == FocusedField.CONFIRM) {
            cameraSetPasswordViewModel.setConfirmPwdValidOnce(isValid);
        }
    }

    private void showRegexLayout() {
        String text = "";
        boolean isValid = true;

        switch (focusedField) {
            case NEW:
                text = getText(changePasswordBinding.editNewPwdText);
                isValid = isValidNewPwd;
                break;

            case CONFIRM:
                text = getText(changePasswordBinding.editConfirmPwdText);
                isValid = isValidConfirmPwd;
                break;

            default:
                Log.e(TAG, "showRegexLayout: default");
                return;
        }

        Log.e(TAG, "showRegexLayout: " + isValid + " " + !TextUtils.isEmpty(text));

        if (!isValid && !TextUtils.isEmpty(text)) {
            expandView();
        } else {
            collapseView(false);
        }
    }

    private String getText(EditText editText) {
        Editable editable = editText != null ? editText.getText() : null;
        return editable != null ? editable.toString() : "";
    }


    public void expandView() {
        View shownRules = (focusedField == FocusedField.NEW)
                ? newPwdRulesBinding.getRoot()
                : confirmPwdRulesBinding.getRoot();

        View hiddenRules = (shownRules == newPwdRulesBinding.getRoot())
                ? confirmPwdRulesBinding.getRoot()
                : newPwdRulesBinding.getRoot();

        hiddenRules.setVisibility(GONE);

        boolean isLandscape = isIsLandscape();
        if (isLandscape && (shownRules.getVisibility() == GONE || shownRules.getVisibility() == INVISIBLE)) {
            Log.e(TAG, "expandView: called isLandscape = true" );
            alignChangeLayoutToLeft();
            shownRules.setVisibility(INVISIBLE);
            shownRules.post(() -> {
                int width = shownRules.getWidth() == 0 ? shownRules.getMeasuredWidth() : shownRules.getWidth();
                if (width == 0) {
                    int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
                    width = (int) (screenWidth * 0.3f);
                }
                shownRules.setTranslationX(width);
                shownRules.setVisibility(View.VISIBLE);
                shownRules.animate()
                        .translationX(0)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            });
        } else if(shownRules.getVisibility() == GONE || shownRules.getVisibility() == INVISIBLE){
            Log.e(TAG, "expandView: called isLandscape = false");
            shownRules.setVisibility(View.VISIBLE);
        }
    }
    public void collapseView(boolean shouldHideBothRegexLayouts) {
        Log.e(TAG, "collapseView()");

        // Case 1: hide both immediately
        if (shouldHideBothRegexLayouts) {
            newPwdRulesBinding.getRoot().setVisibility(GONE);
            confirmPwdRulesBinding.getRoot().setVisibility(GONE);
            return;
        }

        boolean isLandscape = isIsLandscape();

        Log.e("collapseView: ",focusedField == FocusedField.NEW?"NEW":"Confirm");
        // Determine which rules to show/hide
        View shownRules = (focusedField == FocusedField.NEW)
                ? newPwdRulesBinding.getRoot()
                : confirmPwdRulesBinding.getRoot();

        View hiddenRules = (shownRules == newPwdRulesBinding.getRoot())
                ? confirmPwdRulesBinding.getRoot()
                : newPwdRulesBinding.getRoot();

        hiddenRules.setVisibility(GONE);

        // If already hidden → nothing to collapse
        if (shownRules.getVisibility() != VISIBLE) {
            alignChangeLayoutCenter();
            return;
        }

        // Landscape collapse with animation
        if (isLandscape) {
            Log.e(TAG, "collapseView: landscape");

            int width = shownRules.getWidth() == 0
                    ? shownRules.getMeasuredWidth()
                    : shownRules.getWidth();

            shownRules.animate()
                    .translationX(width)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        Log.e(TAG, "collapseView: animation end");

                        shownRules.setVisibility(GONE);
                        shownRules.setTranslationX(0);

                        String newPassword = getText(changePasswordBinding.editNewPwdText);
                        String confirmPassword = getText(changePasswordBinding.editConfirmPwdText);
                        if (didPassPasswordRules()) {
                            alignChangeLayoutCenter();
                        } else if (focusedField == FocusedField.NEW && newPassword.isEmpty()) {
                            alignChangeLayoutCenter();
                        } else if (focusedField == FocusedField.CONFIRM && confirmPassword.isEmpty()) {
                            alignChangeLayoutCenter();
                        }
                    })
                    .start();

            return;
        }

        // Portrait / fallback collapse
        Log.e(TAG, "collapseView: portrait");
        shownRules.setVisibility(GONE);
        alignChangeLayoutCenter();
    }

    private boolean didPassPasswordRules() {
        Editable text = (focusedField == FocusedField.NEW)
                ? changePasswordBinding.editNewPwdText.getText()
                : changePasswordBinding.editConfirmPwdText.getText();

        if (text == null) return true;   // Treat empty as "align to center"

        return isPasswordValid(text.toString());
    }

    private void alignChangeLayoutCenter() {
        Log.e(TAG, "alignChangeLayoutCenter: set" );
        ConstraintLayout rootLayout = (ConstraintLayout) changePasswordBinding.getRoot();
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(rootLayout);

        // Center the ScrollView horizontally
        constraintSet.clear(R.id.scrollViewLeft, ConstraintSet.START);
        constraintSet.clear(R.id.scrollViewLeft, ConstraintSet.END);
        constraintSet.connect(R.id.scrollViewLeft, ConstraintSet.START, R.id.pwdStartGuideline, ConstraintSet.END);
        constraintSet.connect(R.id.scrollViewLeft, ConstraintSet.END, R.id.pwdEndGuideline, ConstraintSet.END);

        constraintSet.applyTo(rootLayout);
    }

    private void alignChangeLayoutToLeft() {
        Log.e(TAG, "alignChangeLayoutToLeft: left set" );
        ConstraintLayout rootLayout = (ConstraintLayout) changePasswordBinding.getRoot();
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(rootLayout);

        // Align ScrollView to left half
        constraintSet.clear(R.id.scrollViewLeft, ConstraintSet.START);
        constraintSet.clear(R.id.scrollViewLeft, ConstraintSet.END);
        constraintSet.connect(R.id.scrollViewLeft, ConstraintSet.START, R.id.StartGuideline, ConstraintSet.START);
        constraintSet.connect(R.id.scrollViewLeft, ConstraintSet.END, R.id.vertical_guideline_center, ConstraintSet.START);

        constraintSet.applyTo(rootLayout);

    }
    private boolean isPasswordValid(String password) {
        return password.length() >= MINIMUM_PASSWORD_LENGTH && password.length() <= MAXIMUM_PASSWORD_LENGTH &&
                password.matches(".*[!@#$%^&*()_+=<>?].*") &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*");
    }

    private void updatePasswordRulesUI(String password) {
        Log.e(TAG, "updatePasswordRulesUI: called" );
        RegexLayoutBinding  viewPasswordRules;
        if(focusedField == FocusedField.NEW){
            viewPasswordRules = RegexLayoutBinding.bind(newPwdRulesBinding.getRoot());
        }else {
            viewPasswordRules = RegexLayoutBinding.bind(confirmPwdRulesBinding.getRoot());
        }

        boolean lengthOK = password.length() >= MINIMUM_PASSWORD_LENGTH && password.length() <= MAXIMUM_PASSWORD_LENGTH;
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+=<>?].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");

        setRuleStatus(viewPasswordRules.iconLength, lengthOK);
        setRuleStatus(viewPasswordRules.iconSpecial, hasSpecial);
        setRuleStatus(viewPasswordRules.iconUpper, hasUpper);
        setRuleStatus(viewPasswordRules.iconLower, hasLower);
        setRuleStatus(viewPasswordRules.iconNumber, hasNumber);
    }

    private void setRuleStatus(ImageView icon, boolean passed) {
        int newResId = passed ? R.drawable.ic_green_circle : R.drawable.ic_circle_cancel_red;
        if (icon.getTag() == null || (Integer) icon.getTag() != newResId) {
            icon.setImageResource(newResId);
            icon.setTag(newResId);
            icon.setScaleX(0.8f);
            icon.setScaleY(0.8f);
            icon.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        }
    }
    private boolean isIsLandscape() {
        return requireContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void hideAllRegexLayouts() {
        collapseView(true);
        alignChangeLayoutCenter();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.e(TAG, "onAttach: called");
        activity = (MainActivity) context;
    }
}

