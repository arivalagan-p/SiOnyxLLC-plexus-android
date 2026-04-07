package com.sionyx.plexus.ui.login;

import static android.content.Context.MODE_PRIVATE;
import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;
import static com.sionyx.plexus.ui.home.HomeViewModel.isLoggedInUser;
import static com.sionyx.plexus.utils.Constants.REGISTERATION_BASE_URL;
import static com.sionyx.plexus.utils.Constants.makeToast;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
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
import com.sionyx.plexus.databinding.FragmentLoginBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.api.interfaces.CloudInterface;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.splashscreen.SplashScreenViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.NetworkUtils;

import java.util.Map;

public class LoginFragment extends Fragment implements SiOnyxLoginRepository.GlobalSignOutListener {
    private static final String TAG = "LoginFragment";
    FragmentLoginBinding binding;
    LifecycleOwner lifecycleOwner;
    private LoginViewModel loginViewModel;
    private SplashScreenViewModel splashScreenViewModel;
    private HomeViewModel homeViewModel;
    private NavController finalNavController;
    private int orientation;
    final String allowedCharacters = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"; // Add any additional special characters you want to allow

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashScreenViewModel = new ViewModelProvider(this).get(SplashScreenViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        lifecycleOwner = this;
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding.setViewModel(loginViewModel);
        finalNavController = getNavigationControl();
        binding.editPasswordText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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
        loginViewModel.setFragmentManager(requireActivity().getSupportFragmentManager());
        startAnimation();
        subscribeUI();
    }

    private void startAnimation() {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Animation slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_right_to_left);

            if (!loginViewModel.isAnimationComplete)
                binding.splashLogo.startAnimation(slideUpAnimation);
            else
                Log.d(TAG, "onViewCreated: " + loginViewModel.isAnimationComplete());

            slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    loginViewModel.setAnimationComplete(false);
                    hideLayoutFromAnimationStart();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    loginViewModel.setAnimationComplete(true);
                    showLayoutFromAnimationStart();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    if (!loginViewModel.isAnimationComplete())
                        animation.start();
                }
            });
        } else {
            Animation slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_animation);
            // Apply animation to the view
            binding.splashLogo.setVisibility(View.VISIBLE); // Make the view visible before applying animation
            if (!loginViewModel.isAnimationComplete)
                binding.splashLogo.startAnimation(slideUpAnimation);
            else
                Log.d(TAG, "onViewCreated: " + loginViewModel.isAnimationComplete());

            slideUpAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    loginViewModel.setAnimationComplete(false);
                    hideLayoutFromAnimationStart();
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    loginViewModel.setAnimationComplete(true);
                    showLayoutFromAnimationStart();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    if (!loginViewModel.isAnimationComplete())
                        animation.start();
                }
            });
        }
    }

    private void hideLayoutFromAnimationStart() {
        binding.userInputLayout.setVisibility(View.INVISIBLE);
    }

    private void showLayoutFromAnimationStart() {
        binding.userInputLayout.setVisibility(View.VISIBLE);
    }

    private void subscribeUI() {
        binding.editNameText.addTextChangedListener(new TextWatcher() {
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
                    binding.editNameText.setText(newText);
                    binding.editNameText.setSelection(newText.length()); // Move cursor to the end
                } else {
                    try {
                        disableEmojiInTitle();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        binding.editPasswordText.addTextChangedListener(new TextWatcher() {
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
                    binding.editPasswordText.setText(newText);
                    binding.editPasswordText.setSelection(newText.length()); // Move cursor to the end
                }
            }
        });

        loginViewModel.isLoadingProgressbar.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            activity.runOnUiThread(() -> {
                if (aBoolean) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        binding.loadingProgressbar.setVisibility(View.VISIBLE);
                        binding.register.setClickable(false);
                        binding.login.setClickable(false);
                        binding.forgotPassword.setClickable(false);
                        loginViewModel.setProgressState(true); // for this save progress visible state while rotate
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        binding.loadingProgressbar.setVisibility(View.GONE);
                        binding.register.setClickable(true);
                        binding.login.setClickable(true);
                        binding.forgotPassword.setClickable(true);
                        loginViewModel.setProgressState(false);
                    });
                }
            });
        }));

        // for this login screen
        binding.showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Check if the checkbox is checked
            if (isChecked) {
                // Show password
                binding.editPasswordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                // Hide password
                binding.editPasswordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Move cursor to the end of the text
            binding.editPasswordText.setSelection(binding.editPasswordText.getText().length());
        });

        loginViewModel.isLogin.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                String mInputUsername = binding.editNameText.getText().toString().trim();
                String mInputPassword = binding.editPasswordText.getText().toString().trim();
                if (mInputUsername.equalsIgnoreCase("") || mInputPassword.equalsIgnoreCase("")) {
                    if (activity != null)
                        makeToast(activity.getString(R.string.enterrequirefields));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (activity != null)
                            hideKeyboard(activity);
                        NetworkUtils.pingServer(isSuccessful -> {
                            if (isSuccessful) {
                                // Ping successful
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    loginViewModel.hasLoadingProgressbar(true);
                                    AWSMobileClient.getInstance().initialize(requireContext(), new Callback<UserStateDetails>() {
                                        @Override
                                        public void onResult(UserStateDetails result) {
                                            loginViewModel.signIN(mInputUsername, mInputPassword, new CloudInterface() {
                                                @Override
                                                public void onSuccess(String state, String msg) {
                                                    String username = AWSMobileClient.getInstance().getUsername();
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        AWSMobileClient.getInstance().getUserAttributes(new Callback<Map<String, String>>() {
                                                            @Override
                                                            public void onResult(Map<String, String> result) {
                                                                loginViewModel.hasLoadingProgressbar(false);
                                                                String email = result.get("email");
                                                                String custom = result.get("custom:signIN");
                                                                Log.d(TAG, "getUserAttributes: " + custom);
                                                                if (custom != null && custom.equals("True")) {
                                                                    // show dialog
                                                                    if (activity != null) {
                                                                        activity.runOnUiThread(() -> {
                                                                            hideKeyboard(activity);
                                                                            loginViewModel.hasLoadingProgressbar(false);
                                                                            showAlreadySignInDialog();
                                                                        });
                                                                    }
                                                                } else {
                                                                    // got to home
                                                                    if (activity != null) {
                                                                        activity.runOnUiThread(() -> {
                                                                            loginViewModel.hasLoadingProgressbar(false);
                                                                            clearEditTextValuesInLogin();
                                                                            loginViewModel.setUserName("");
                                                                            loginViewModel.setPassword("");
                                                                            binding.editNameText.setText("");
                                                                            binding.editPasswordText.setText("");
                                                                            makeToast(activity.getString(R.string.login_success));
                                                                            hideKeyboard(activity);

                                                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                                                loginViewModel.updateUserAttribute(email);
                                                                                isLoggedInUser = true;
                                                                                saveUserLoginState(username, true);
                                                                                splashScreenViewModel.setLoginState("loginState");
                                                                            });
                                                                        });
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onError(Exception e) {
                                                                loginViewModel.hasLoadingProgressbar(false);
                                                                String error = formatException(e);
                                                                if (error.contains("Password reset required for the user")) {
                                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                                        makeToast(getString(R.string.password_reset_required_for_the_user));
                                                                        loginViewModel.isPasswordResetRequire();
                                                                    });
                                                                } else {
                                                                    makeToast(error);
                                                                }
                                                            }
                                                        });
                                                    });
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    runOnUiThread(() -> {
                                                        Log.e(TAG, "SIGN IN{ ERROR " + error);
                                                        if (activity != null) {
                                                            hideKeyboard(activity);
                                                            loginViewModel.hasLoadingProgressbar(false);
                                                            if (error.contains("User is not confirmed.")) {
                                                                makeToast(activity.getString(R.string.user_not_confirmed_message));
                                                                //  showOtpSendAgainDialog(mInputusername);
                                                            } else if (error.contains("User does not exist.")) {
                                                                makeToast(activity.getString(R.string.user_not_exist));
                                                            } else if (error.contains("Incorrect username or password. ")) {
                                                                makeToast(activity.getString(R.string.incorrect_username_password));
                                                            } else if (error.contains("Password reset required for the user")) {
                                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                                    makeToast(getString(R.string.password_reset_required_for_the_user));
                                                                    loginViewModel.isPasswordResetRequire();
                                                                });
                                                            } else {
                                                                makeToast(error);
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            loginViewModel.hasLoadingProgressbar(false);
                                            makeToast(e.getMessage());
                                        }
                                    });
                                });
                            } else {
                                // Ping failed
                                showInternetConnectionFailedDialog();
                            }
                        });
                    });
                }
            }
        }));

        loginViewModel.isSkipLogin.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (activity != null)
                        hideKeyboard(activity);
                    clearEditTextValuesInLogin();
                    finalNavController.navigate(R.id.homeFragment);
                    isLoggedInUser = false;
                });
            }
        }));

        // forgot password button
        loginViewModel.isSelectForgotPassword.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    loginViewModel.resetForgotPasswordFields(true);
                    homeViewModel.getNavController().navigate(R.id.forgotPasswordFragment);
                });
            }
        }));

        loginViewModel.isRequirePasswordReset.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    loginViewModel.resetForgotPasswordFields(false);
                    homeViewModel.getNavController().navigate(R.id.forgotPasswordFragment);
                });
            }
        }));

        loginViewModel.isRegister.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    NetworkUtils.pingServer(isSuccessful -> {
                        // Handle the ping result here
                        if (isSuccessful) {
                            // Ping successful
                            new Handler(Looper.getMainLooper()).post(this::openChromeBrowserView);
                        } else {
                            // Ping failed
                            showInternetConnectionFailedDialog();
                        }
                    });
                });
            }
        }));

        activity.getOnBackPressedDispatcher().addCallback(requireActivity(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activity != null)
                    activity.finish();
            }
        });

        loginViewModel.isShowInternetConnectionFailed.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                showInternetConnectionFailedDialog();
            }
        }));

        // for this device orientation changed then navigate to other fragment screen not working issue handled
        splashScreenViewModel.getLoginState().observe(getViewLifecycleOwner(), newData -> {
            Log.d(TAG, "login fragment: " + newData);
            // Update UI with new data
            if (newData.contains("loginState")) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    binding.editNameText.setText("");
                    binding.editPasswordText.setText("");
                    loginViewModel.setUserName("");
                    loginViewModel.setPassword("");
                    finalNavController.navigate(R.id.homeFragment);
                    finalNavController.clearBackStack(R.id.splashScreenFragment);
                    finalNavController.clearBackStack(R.id.loginFragment);
                });
            }
        });
    }

    private void ShowSentConfirmationCodeDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.SENT_OTP_DIALOG;
            activity.showDialogLoginScreen(getString(R.string.forgot_password), "");
        }
    }

    private void saveUserLoginState(String userName, boolean isLogin) {
        try {
            if (activity != null) {
                SharedPreferences loginSharedPreferences = activity.getSharedPreferences("LoginState", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = loginSharedPreferences.edit();
                myEdit.putBoolean("isLogin", isLogin);
                myEdit.putString("userName", userName);
                myEdit.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInternetConnectionFailedDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG;
            activity.showDialogLoginScreen("Network Unavailable", "");
        }
    }

    private void showAlreadySignInDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.ALREADY_SIGNIN_DIALOG;
            activity.showDialogLoginScreen("", "");
        }
    }

    public void openChromeBrowserView() {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        // Customize the toolbar color
        CustomTabColorSchemeParams colorSchemeParams = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.color_lightgrey)) // Replace with your toolbar color
                .build();
        builder.setDefaultColorSchemeParams(colorSchemeParams);
        // Enable the URL bar to show the web address
        builder.setShowTitle(true);
        CustomTabsIntent customTabsIntent = builder.build();
        // Launch the URL in a Chrome Custom Tab
        customTabsIntent.launchUrl(requireContext(), Uri.parse(REGISTERATION_BASE_URL));
    }

    private void clearEditTextValuesInLogin() {
        binding.editNameText.setText("");
        binding.editPasswordText.setText("");
    }

    private NavController getNavigationControl() {
        final NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = null;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        return navController;
    }

    private void hideKeyboard(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            View view = activity.getCurrentFocus();
            if (view == null) {
                view = new View(activity);
            }
            imm.hideSoftInputFromWindow(binding.getRoot().getRootView().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void signOutSuccess(String error_msg) {
        runOnUiThread(() -> {
            clearEditTextValuesInLogin();
            Log.d(TAG, "signOutSuccess: " + error_msg);
            loginViewModel.hasLoadingProgressbar(false);
            if (activity != null) {
                if (error_msg.contains("ALREADY SIGNED_OUT") || error_msg.contains("Access Token has been revoked")) {
                    makeToast(activity.getString(R.string.message_signout_another_device));
                } else if (error_msg.contains("Access Token invalid")) {
                    makeToast(activity.getString(R.string.session_expired_message));
                } else {
                    makeToast(activity.getString(R.string.session_expired_message));
                }
            }
        });
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
        String username = binding.editNameText.getText().toString().trim();
        String password = binding.editPasswordText.getText().toString().trim();
        loginViewModel.setUserName(username);
        loginViewModel.setPassword(password);
        loginViewModel.setProgressState(loginViewModel.isProgressState());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loginViewModel != null) {
            String userName = loginViewModel.getUserName();
            String password = loginViewModel.getPassword();
            binding.editNameText.setText(userName);
            binding.editPasswordText.setText(password);
            loginViewModel.hasLoadingProgressbar(false);
        }
        handleSplitScreenEnabled();
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

    private String formatException(Exception exception) {
        String formattedString = "Internal Error";
        Log.e("AWS", " -- Error: " + exception.toString());
        Log.getStackTraceString(exception);
        String temp = exception.getMessage();
        if (temp != null && temp.length() > 0) {
            formattedString = temp.split("\\(")[0];
            if (temp != null && temp.length() > 0) {
                return formattedString;
            }
        }
        return formattedString;
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
        binding.editNameText.setFilters(new InputFilter[]{emojiFilter});
    }

    private boolean isAllowedCharacter(char c) {
        // Check if the character is a number, alphabet, special characters
        return Character.isLetterOrDigit(c) || allowedCharacters.contains(String.valueOf(c));
    }
}