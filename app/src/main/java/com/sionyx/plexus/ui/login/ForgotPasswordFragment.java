package com.sionyx.plexus.ui.login;

import static com.sionyx.plexus.ui.home.HomeViewModel.isLoggedInUser;
import static com.sionyx.plexus.utils.Constants.makeToast;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentForgotPasswordBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.api.interfaces.CloudInterface;
import com.sionyx.plexus.ui.api.interfaces.ValidUserInterface;
import com.sionyx.plexus.ui.api.requestModel.RequestChangeNewPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestForgotPassword;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.splashscreen.SplashScreenViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.NetworkUtils;

import java.util.regex.Pattern;

public class ForgotPasswordFragment extends Fragment {
    private static final String TAG = "ForgotPasswordFragment";
    private FragmentForgotPasswordBinding binding;
    private LoginViewModel loginViewModel;
    private HomeViewModel homeViewModel;
    private ForgotPasswordViewModel forgotPasswordViewModel;
    private SplashScreenViewModel splashScreenViewModel;
    private LifecycleOwner lifecycleOwner;
    private NavController finalNavController;
    private int orientation;
    final String allowedCharacters = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"; // Add any additional special characters you want to allow

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        lifecycleOwner = this;
        forgotPasswordViewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        splashScreenViewModel = new ViewModelProvider(requireActivity()).get(SplashScreenViewModel.class);
        binding.setViewModel(forgotPasswordViewModel);
        finalNavController = getNavigationControl();
        orientation = requireContext().getResources().getConfiguration().orientation;

        homeViewModel.getEnterExitMultiWindowMode().observe(lifecycleOwner, new EventObserver<>(isMultiWindowModeActivated -> {
            Log.e(TAG, "getEnterExitMultiWindowMode: " + isMultiWindowModeActivated);
            if (isMultiWindowModeActivated) {
                handleSplitScreenEnabled();
            }
        }));

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscribeUI();
    }

    private void subscribeUI() {

        forgotPasswordViewModel.countdownValue.observe(getViewLifecycleOwner(), countdownValue -> {
            // Update UI with countdownValue
            if (countdownValue != 0) {
                binding.reSentOtpDurationText.setClickable(false);
                binding.reSentOtpDurationText.setText(String.format("%s %d%s", getString(R.string.resend_otp_in), countdownValue, "s"));
            } else {
                binding.reSentOtpDurationText.setClickable(true);
                binding.reSentOtpDurationText.setText(getString(R.string.tap_here_to_resend_otp));
                forgotPasswordViewModel.stopCountdown(); // Stop countdown when fragment is destroyed or no longer needed
            }
        });

        binding.UsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used, but required by interface
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used, but required by interface
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Check for whitespace and remove it
                if (s.toString().contains(" ")) {
                    String newText = s.toString().replaceAll("\\s", "");
                    binding.UsernameEditText.setText(newText);
                    binding.UsernameEditText.setSelection(newText.length()); // Move cursor to the end
                } else {
                    try {
                        disableEmojiInTitle();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        // if (forgotPasswordViewModel.isShowChangePasswordScreen()) {
        binding.newPwdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used, but required by interface
                Log.d(TAG, "beforeTextChanged:hasSpecialCharacter " + s.toString().length());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String enteredText = s.toString();
                if (enteredText.isEmpty()) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        binding.userDescriptionLayout.setVisibility(View.VISIBLE);
                        binding.tvPwdRulesLayout.setVisibility(View.GONE);
                    } else {
                        binding.userDescriptionLayout.setVisibility(View.GONE);
                        binding.tvPwdRulesLayout.setVisibility(View.GONE);
                    }
                } else {
                    binding.userDescriptionLayout.setVisibility(View.GONE);
                    binding.tvPwdRulesLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Check for whitespace and remove it
                if (s.toString().contains(" ")) {
                    String newText = s.toString().replaceAll("\\s", "");
                    binding.newPwdEditText.setText(newText);
                    binding.newPwdEditText.setSelection(newText.length()); // Move cursor to the end
                } else if (s.length() == 0) {
                    binding.userDescriptionLayout.setVisibility(View.VISIBLE);
                    binding.tvPwdRulesLayout.setVisibility(View.GONE);
                } else {
                    binding.userDescriptionLayout.setVisibility(View.GONE);
                    binding.tvPwdRulesLayout.setVisibility(View.VISIBLE);
                    String enteredText = s.toString();
                    Log.d(TAG, "onTextChanged:hasSpecialCharacter " + enteredText.length());

                    boolean hasUpperCase = !enteredText.equals(enteredText.toLowerCase());
                    boolean hasLowerCase = !enteredText.equals(enteredText.toUpperCase());
                    boolean hasNumber = enteredText.matches(".*\\d.*");
                    boolean hasSpecialCharacter = !enteredText.matches("[A-Za-z0-9 ]*");

                    // Perform actions based on the conditions
                    if (hasUpperCase) {
                        // Text contains uppercase letters
                        binding.passwordIncludeUpperCaseLetterTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_green_circle));
                    } else {
                        binding.passwordIncludeUpperCaseLetterTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_cancel_red));
                    }

                    if (hasLowerCase) {
                        // Text contains lowercase letters
                        binding.passwordIncludeLowerCaseLetterTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_green_circle));
                    } else {
                        binding.passwordIncludeLowerCaseLetterTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_cancel_red));
                    }

                    if (hasNumber) {
                        // Text contains numbers
                        binding.passwordIncludeNumberTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_green_circle));
                    } else {
                        binding.passwordIncludeNumberTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_cancel_red));
                    }

                    if (hasSpecialCharacter) {
                        // Text contains special characters
                        binding.passwordIncludeSpecialCharacterTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_green_circle));
                    } else {
                        binding.passwordIncludeSpecialCharacterTextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_cancel_red));
                    }

                    if (enteredText.length() >= 8) {
                        binding.passwordMust8TextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_green_circle));
                    } else {
                        binding.passwordMust8TextStatusIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_cancel_red));
                    }
                }
            }
        });
        //    }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (forgotPasswordViewModel.isShowChangePasswordScreen()) {
                binding.changePasswordViewLayout.setVisibility(View.VISIBLE);
                binding.sentConfirmCode.setVisibility(View.GONE);
                binding.responseMessageTextview.setVisibility(View.VISIBLE);
                binding.reSentOtpDurationText.setVisibility(View.VISIBLE);
                if (binding.confirmNewPwdEditText.getText().length() > 0 || binding.newPwdEditText.getText().length() > 0) {
                    binding.userDescriptionLayout.setVisibility(View.GONE);
                    binding.tvPwdRulesLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.userDescriptionLayout.setVisibility(View.VISIBLE);
                    binding.tvPwdRulesLayout.setVisibility(View.GONE);
                }
                binding.forgotPasswordTitleText.setText(getString(R.string.reset_password));
            } else {
                binding.changePasswordViewLayout.setVisibility(View.GONE);
                binding.sentConfirmCode.setVisibility(View.VISIBLE);
                binding.responseMessageTextview.setVisibility(View.GONE);
                binding.reSentOtpDurationText.setVisibility(View.GONE);
                binding.forgotPasswordTitleText.setText(getString(R.string.forgot_password));
                binding.userDescriptionLayout.setVisibility(View.VISIBLE);
                binding.tvPwdRulesLayout.setVisibility(View.GONE);
            }
        } else {
            if (forgotPasswordViewModel.isShowChangePasswordScreen()) {
                binding.changePasswordViewLayout.setVisibility(View.VISIBLE);
                binding.sentConfirmCode.setVisibility(View.GONE);
                binding.responseMessageTextview.setVisibility(View.VISIBLE);
                binding.reSentOtpDurationText.setVisibility(View.VISIBLE);
                binding.forgotPasswordTitleText.setText(getString(R.string.reset_password));
                if (binding.confirmNewPwdEditText.getText().length() > 0 || binding.newPwdEditText.getText().length() > 0) {
                    binding.userDescriptionLayout.setVisibility(View.GONE);
                    binding.tvPwdRulesLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.userDescriptionLayout.setVisibility(View.VISIBLE);
                    binding.tvPwdRulesLayout.setVisibility(View.GONE);
                }
            } else {
                binding.changePasswordViewLayout.setVisibility(View.GONE);
                binding.sentConfirmCode.setVisibility(View.VISIBLE);
                binding.responseMessageTextview.setVisibility(View.GONE);
                binding.reSentOtpDurationText.setVisibility(View.GONE);
                binding.forgotPasswordTitleText.setText(getString(R.string.forgot_password));
                binding.tvPwdRulesLayout.setVisibility(View.GONE);
                binding.userDescriptionLayout.setVisibility(View.VISIBLE);
            }
        }

        forgotPasswordViewModel.isShowCustomProgressbar.observe(getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.customProgressbar.setVisibility(View.VISIBLE);
                forgotPasswordViewModel.setProgressState(true); // for this save progress visible state while rotate
                binding.sentConfirmCode.setClickable(false);
            } else {
                binding.customProgressbar.setVisibility(View.GONE);
                forgotPasswordViewModel.setProgressState(false); // for this save progress visible state while rotate
                binding.sentConfirmCode.setClickable(true);
            }
        }));

        /* forgot password*/
        forgotPasswordViewModel.isSentOtpCode.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                final String strUserName = binding.UsernameEditText.getText().toString().trim();
                if (!strUserName.isEmpty()) {
                    // here checked is valid user or not then proceed to change pwd
                    forgotPasswordViewModel.hasShowCustomProgressbar(true);
                    hideKeyboard(requireActivity(), binding.getRoot());
                    new Handler().post(() -> {
                        NetworkUtils.pingServer(isSuccessful -> {
                            // Handle the ping result here
                            if (isSuccessful) {
                                // Ping successful
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    AWSMobileClient.getInstance().initialize(requireContext(), new Callback<UserStateDetails>() {
                                        @Override
                                        public void onResult(UserStateDetails result) {
                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                RequestForgotPassword requestForgotPassword = new RequestForgotPassword();
                                                requestForgotPassword.setUserName(strUserName);
                                                loginViewModel.getUserEmailAlreadyExist(requestForgotPassword, new ValidUserInterface() {
                                                    @Override
                                                    public void onSuccess(int status, String msg) {
                                                        if (status >= 400) {
                                                            if (activity != null) {
                                                                activity.runOnUiThread(() -> {
                                                                    forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                                                    binding.changePasswordViewLayout.setVisibility(View.GONE);
                                                                    forgotPasswordViewModel.setShowChangePasswordScreen(false);
                                                                    forgotPasswordViewModel.setHoldResponseMessage(msg); // for temp hold msg
                                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                                        splashScreenViewModel.setForgotPasswordState("forgotPassword");// for this handled device orientation value cleared usecase
                                                                    });
                                                                });
                                                            }
                                                        } else if (status == 200) {
                                                            if (activity != null) {
                                                                activity.runOnUiThread(() -> {
                                                                    forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                                                    forgotPasswordViewModel.setShowChangePasswordScreen(true); // if visible change password layout
                                                                    forgotPasswordViewModel.setHoldResponseMessage(msg); // for temp hold msg
                                                                    makeToast(msg);
                                                                    binding.responseMessageTextview.setVisibility(View.VISIBLE);
                                                                    binding.responseMessageTextview.setText(msg);
                                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                                        forgotPasswordViewModel.startCountdown(60000);
                                                                        splashScreenViewModel.setForgotPasswordState("forgotPassword");// for this handled device orientation value cleared usecase
                                                                    });
                                                                });
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(String error) {
                                                        makeToast(error);
                                                        forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                                    }
                                                });
                                            });
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                        }
                                    });
                                });
                            } else {
                                // Ping failed
                                forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                hideKeyboard(requireActivity(), binding.getRoot());
                                new Handler(Looper.getMainLooper()).postDelayed(this::showInternetConnectionFailedDialog, 500);
                            }
                        });
                    });
                } else {
                    makeToast(getString(R.string.please_enter_your_username_to_receive_the_otp));
                }
            }
        }));

        forgotPasswordViewModel.isSubmitChangePassword.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                String newPassword = binding.newPwdEditText.getText().toString().trim();
                String confirm_pwd = binding.confirmNewPwdEditText.getText().toString().trim();
                String confirmationCode = binding.confirmCodeEditText.getText().toString().trim();
                String userName = binding.UsernameEditText.getText().toString().trim();

                Log.e("AWS", "mForgetPassowrdDialog" + " " + newPassword + " " + confirm_pwd + " " + confirmationCode);
                if (confirmationCode.equalsIgnoreCase("") || newPassword.equalsIgnoreCase("") || confirm_pwd.equalsIgnoreCase("")) {
                    if (confirmationCode.equalsIgnoreCase("")) {
                        binding.confirmCodeEditText.requestFocus();
                        makeToast(getString(R.string.enterrequirefields));
                    }
                    if (newPassword.equalsIgnoreCase("")) {
                        binding.confirmNewPwdEditText.requestFocus();
                        makeToast(getString(R.string.enterrequirefields));
                    }
                    if (confirm_pwd.equalsIgnoreCase("")) {
                        binding.newPwdEditText.requestFocus();
                        makeToast(getString(R.string.enterrequirefields));
                    }
                } else {
                    if (isValidPassword(newPassword, confirm_pwd)) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            NetworkUtils.pingServer(isSuccessful -> {
                                // Handle the ping result here
                                if (isSuccessful) {
                                    // Ping successful
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        forgotPasswordViewModel.hasShowCustomProgressbar(true);
                                        hideKeyboard(requireActivity(), binding.getRoot());
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            RequestChangeNewPassword requestChangeNewPassword = new RequestChangeNewPassword();
                                            requestChangeNewPassword.setUsername(userName);
                                            requestChangeNewPassword.setOtp(confirmationCode);
                                            requestChangeNewPassword.setNewPassword(newPassword);
                                            loginViewModel.changeNewPassword(requestChangeNewPassword, new CloudInterface() {
                                                @Override
                                                public void onSuccess(String state, String msg) {
                                                    activity.runOnUiThread(() -> {
                                                        forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                                        makeToast(msg);
                                                        new Handler(Looper.getMainLooper()).post(() -> {
                                                            splashScreenViewModel.setNewPasswordChangeState("newPasswordChange");
                                                        });
                                                    });
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    activity.runOnUiThread(() -> {
                                                        Log.e(TAG, "confirmForgotPassword OnError" + " " + error);
                                                        forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                                        if (error.contains("Attempt limit exceeded, please try after some time")) {
                                                            makeToast(getString(R.string.attempt_limit_exceeded_please_try_after_some_time));
                                                        } else {
                                                            makeToast(error);
                                                        }
                                                    });
                                                }
                                            });
                                        });

                                    });
                                } else {
                                    // Ping failed
                                    forgotPasswordViewModel.hasShowCustomProgressbar(false);
                                    hideKeyboard(requireActivity(), binding.getRoot());
                                    new Handler(Looper.getMainLooper()).postDelayed(this::showInternetConnectionFailedDialog, 500);
                                }
                            });
                        });
                    }
                }
            }
        }));

        binding.showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show password
                binding.newPwdEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.confirmNewPwdEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                // Hide password
                binding.newPwdEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.confirmNewPwdEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Move cursor to the end of the text
            binding.newPwdEditText.setSelection(binding.newPwdEditText.getText().length());
            binding.confirmNewPwdEditText.setSelection(binding.confirmNewPwdEditText.getText().length());
        });

        forgotPasswordViewModel.isBackToLogin.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        forgotPasswordViewModel.setUserName("");
                        forgotPasswordViewModel.setOptCode("");
                        forgotPasswordViewModel.setNewPassword("");
                        forgotPasswordViewModel.setConfirmPassword("");
                        forgotPasswordViewModel.setShowChangePasswordScreen(false);
                        forgotPasswordViewModel.setHoldResponseMessage("");
                        splashScreenViewModel.setForgotPasswordState(""); // reset key value
                        splashScreenViewModel.setNewPasswordChangeState(""); // reset key value
                        homeViewModel.getNavController().navigate(R.id.loginFragment);
                    });
                }
            }
        }));

        binding.reSentOtpDurationText.setOnClickListener(v -> {
            binding.confirmCodeEditText.setText("");
            forgotPasswordViewModel.onSentOTPCode();
        });

        // for this device orientation changed then navigate to other fragment screen not working issue handled
        splashScreenViewModel.getForgotPasswordState().observe(getViewLifecycleOwner(), newData -> {
            Log.d(TAG, "forgotPassword fragment: " + newData);
            // Update UI with new data
            if (forgotPasswordViewModel.isShowChangePasswordScreen()) {
                if (newData.contains("forgotPassword")) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        binding.changePasswordViewLayout.setVisibility(View.VISIBLE);
                        binding.responseMessageTextview.setVisibility(View.VISIBLE);
                        binding.sentConfirmCode.setVisibility(View.GONE);
                        binding.forgotPasswordTitleText.setText(getString(R.string.reset_password));
                        binding.reSentOtpDurationText.setVisibility(View.VISIBLE);
                        forgotPasswordViewModel.setShowChangePasswordScreen(true);
                        binding.responseMessageTextview.setText(forgotPasswordViewModel.getHoldResponseMessage());
                        binding.UsernameEditText.setEnabled(false);
                        binding.passwordResetUserMessage.setVisibility(View.GONE);
                        forgotPasswordViewModel.setShowPasswordResetRequireMessage(false);

                        if (binding.confirmNewPwdEditText.getText().length() > 0 || binding.newPwdEditText.getText().length() > 0) {
                            binding.userDescriptionLayout.setVisibility(View.GONE);
                            binding.tvPwdRulesLayout.setVisibility(View.VISIBLE);
                        } else {
                            binding.userDescriptionLayout.setVisibility(View.VISIBLE);
                            binding.tvPwdRulesLayout.setVisibility(View.GONE);
                        }
                    });
                }
            } else {
                if (newData.contains("forgotPassword")) {
                    splashScreenViewModel.setForgotPasswordState(""); // reset key value
                    showUserNotFoundDialog(forgotPasswordViewModel.getHoldResponseMessage());
                }

                if (forgotPasswordViewModel.isShowPasswordResetRequireMessage()) {
                    binding.passwordResetUserMessage.setVisibility(View.VISIBLE);
                } else {
                    binding.passwordResetUserMessage.setVisibility(View.GONE);
                }
            }
        });

        splashScreenViewModel.getNewPasswordChangeState().observe(getViewLifecycleOwner(), newData -> {
            Log.d(TAG, "getNewPasswordChangeState: " + newData);
            // Update UI with new data
            if (newData.contains("newPasswordChange")) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    forgotPasswordViewModel.setUserName("");
                    forgotPasswordViewModel.setOptCode("");
                    forgotPasswordViewModel.setNewPassword("");
                    forgotPasswordViewModel.setConfirmPassword("");
                    forgotPasswordViewModel.setShowChangePasswordScreen(false);
                    binding.UsernameEditText.setEnabled(true);
                    forgotPasswordViewModel.onBackToLogin();
                    splashScreenViewModel.setNewPasswordChangeState(""); // reset key value
                });
            } else {
                Log.d(TAG, "getNewPasswordChangeState: " + newData);
            }
        });

        loginViewModel.resetEditTextFields.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            forgotPasswordViewModel.setUserName("");
            forgotPasswordViewModel.setOptCode("");
            forgotPasswordViewModel.setNewPassword("");
            forgotPasswordViewModel.setConfirmPassword("");
            forgotPasswordViewModel.setHoldResponseMessage("");
            binding.UsernameEditText.setEnabled(true);
            binding.sentOtpLayout.setVisibility(View.VISIBLE);
            forgotPasswordViewModel.setShowChangePasswordScreen(false);
            if (aBoolean) {
                binding.reSentOtpDurationText.setVisibility(View.GONE);
                binding.changePasswordViewLayout.setVisibility(View.GONE);
                binding.passwordResetUserMessage.setVisibility(View.GONE);
                forgotPasswordViewModel.setShowPasswordResetRequireMessage(false);
            } else {
                forgotPasswordViewModel.setShowPasswordResetRequireMessage(true);
                binding.passwordResetUserMessage.setVisibility(View.VISIBLE);
            }
        }));

        requireActivity().getOnBackPressedDispatcher().addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                forgotPasswordViewModel.onBackToLogin();
            }
        });
    }

    private void showInternetConnectionFailedDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG;
            activity.showDialogLoginScreen("Network Unavailable", "");
        }
    }

    private void showUserNotFoundDialog(String msg) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.USER_NOT_FOUND_DIALOG;
            activity.showDialogLoginScreen("", msg);
        }
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

    private NavController getNavigationControl() {
        final NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = null;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        return navController;
    }

    private void handleSplitScreenEnabled() {
        if (activity != null) {
            if (activity.isInMultiWindowMode()) {
                activity.runOnUiThread(() -> {
                    isLoggedInUser = false;
                    makeToast("Split-screen mode does not support the login page");
                    if (finalNavController != null)
                        finalNavController.navigate(R.id.homeFragment);
                });
            }
        }
    }

    private boolean isValidPassword(String newPwd, String confirmPwd) {
        Pattern specialCharPatten = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern UpperCasePatten = Pattern.compile("[A-Z ]");
        Pattern lowerCasePatten = Pattern.compile("[a-z ]");
        Pattern digitCasePatten = Pattern.compile("[0-9 ]");

        if (newPwd.length() < 8) {
            binding.newPwdEditText.requestFocus();
            // makeToast(getString(R.string.message_password_character_length));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (confirmPwd.length() < 8) {
            binding.confirmNewPwdEditText.requestFocus();
            //    makeToast(getString(R.string.message_password_character_length));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!newPwd.equals(confirmPwd)) {
            binding.confirmNewPwdEditText.requestFocus();
            makeToast(getString(R.string.message_password_same_error));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!specialCharPatten.matcher(newPwd).find()) {
            binding.newPwdEditText.requestFocus();
            // makeToast(getString(R.string.message_password_specialcharacter));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!specialCharPatten.matcher(confirmPwd).find()) {
            binding.confirmNewPwdEditText.requestFocus();
            //   makeToast(getString(R.string.message_password_specialcharacter));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!UpperCasePatten.matcher(newPwd).find()) {
            binding.newPwdEditText.requestFocus();
            //    makeToast(getString(R.string.message_password_uppercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!UpperCasePatten.matcher(confirmPwd).find()) {
            binding.confirmNewPwdEditText.requestFocus();
            //  makeToast(getString(R.string.message_password_uppercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!lowerCasePatten.matcher(newPwd).find()) {
            binding.newPwdEditText.requestFocus();
            //     makeToast(getString(R.string.message_password_lowercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!lowerCasePatten.matcher(confirmPwd).find()) {
            binding.confirmNewPwdEditText.requestFocus();
            //   makeToast(getString(R.string.message_password_lowercase));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!digitCasePatten.matcher(newPwd).find()) {
            binding.newPwdEditText.requestFocus();
            //   makeToast(getString(R.string.message_password_number));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }

        if (!digitCasePatten.matcher(confirmPwd).find()) {
            binding.confirmNewPwdEditText.requestFocus();
            //     makeToast(getString(R.string.message_password_number));
            loginViewModel.hasShowCustomProgressbar(false);
            return false;
        }
        return true;
    }

    private void disableEmojiInTitle() {
        InputFilter emojiFilter = (source, start, end, dest, dstart, dend) -> {
            for (int index = start; index < end - 1; index++) {
                int type = Character.getType(source.charAt(index));
                if (!isAllowedCharacter(source.charAt(index))) {
                    return "";
                }
            }
            return null;
        };
        binding.UsernameEditText.setFilters(new InputFilter[]{emojiFilter});
    }

    private boolean isAllowedCharacter(char c) {
        // Check if the character is a number, alphabet, special characters
        return Character.isLetterOrDigit(c) || allowedCharacters.contains(String.valueOf(c));
    }

    MainActivity activity = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Override
    public void onPause() {
        super.onPause();
        String username = binding.UsernameEditText.getText().toString().trim();
        String newPassword = binding.newPwdEditText.getText().toString().trim();
        String confirmPassword = binding.confirmNewPwdEditText.getText().toString().trim();
        String otp = binding.confirmCodeEditText.getText().toString().trim();
        forgotPasswordViewModel.setUserName(username);
        forgotPasswordViewModel.setNewPassword(newPassword);
        forgotPasswordViewModel.setConfirmPassword(confirmPassword);
        forgotPasswordViewModel.setOptCode(otp);
        forgotPasswordViewModel.setProgressState(forgotPasswordViewModel.isProgressState());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (forgotPasswordViewModel != null) {
            String userName = forgotPasswordViewModel.getUserName();
            String newPassword = forgotPasswordViewModel.getNewPassword();
            String confirmPassword = forgotPasswordViewModel.getConfirmPassword();
            String otpCode = forgotPasswordViewModel.getOptCode();
            binding.UsernameEditText.setText(userName);
            binding.newPwdEditText.setText(newPassword);
            binding.confirmNewPwdEditText.setText(confirmPassword);
            binding.confirmCodeEditText.setText(otpCode);
            forgotPasswordViewModel.hasShowCustomProgressbar(forgotPasswordViewModel.isProgressState());
        }
        handleSplitScreenEnabled();
    }
}