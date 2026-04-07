package com.sionyx.plexus.ui.splashscreen;

import static android.content.Context.MODE_PRIVATE;
import static com.sionyx.plexus.ui.home.HomeViewModel.isLoggedInUser;
import static com.sionyx.plexus.utils.Constants.isLoginEnable;
import static com.sionyx.plexus.utils.Constants.makeToast;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentSplashScreenBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.api.interfaces.CloudInterface;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.login.LoginViewModel;
import com.sionyx.plexus.utils.NetworkUtils;

public class SplashScreenFragment extends Fragment {
    private static final String TAG = "SplashScreenFragment";
    private FragmentSplashScreenBinding binding;
    private NavController finalNavController;
    private LoginViewModel loginViewModel;
    private SplashScreenViewModel splashScreenViewModel;
    SharedPreferences loginSharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        splashScreenViewModel = new ViewModelProvider(requireActivity()).get(SplashScreenViewModel.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSplashScreenBinding.inflate(inflater, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        finalNavController = getNavigationControl();
        try {
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("FW_SKIPPED", MODE_PRIVATE);
            SharedPreferences.Editor myEdit = sharedPreferences.edit();
            myEdit.clear();
            myEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });

        /* for this observer handled saved state of fragment and device orientation changes(i.e launch splash screen then rotate immediately screen freeze issue) */
        // for this device orientation changed then navigate to other fragment screen freeze issue handled
        splashScreenViewModel.getData().observe(getViewLifecycleOwner(), newData -> {
            Log.d(TAG, "splashScreenViewModel: " + newData);
            // Update UI with new data
            if (newData.contains("SIGNED_IN")) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    navigateToHomeScreen();
                    loginViewModel.setFirstTimeAttached(false);
                    isLoggedInUser = true;
                });
            } else if (newData.contains("SIGNED_OUT")) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    navigateToLoginScreen();
                    loginViewModel.setFirstTimeAttached(false);
                    isLoggedInUser = false;
                });
            }
        });

        new Handler().postDelayed(() -> {
            /*for this state while wifi off and open app and go to background. after that tap app icon to relaunch app now add device button name changed.So avoid use this */
            HomeViewModel.addButtonState = HomeViewModel.AddButtonState.INIT;
            //       CameraViewModel.analyticsButtonState = CameraViewModel.AnalyticsButtonState.Analytics_STOPPED;
            CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;

            if (isLoginEnable) {
                // check connectivity and handle sign-in
                NetworkUtils.pingServer(isSuccessful -> {
                    if (isSuccessful) {
                        Log.d(TAG, "onViewCreated: ping success");
                        if (!loginViewModel.isFirstTimeAttached()) {
                            checkSignInState();
                            loginViewModel.setFirstTimeAttached(true);
                        }
                    } else {
                        Log.d(TAG, "onViewCreated: ping failed");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (getLoginState()) {
                                splashScreenViewModel.setData("SIGNED_IN");
                            } else {
                                splashScreenViewModel.setData("SIGNED_OUT");
                            }
                        });
                    }
                });
            } else {
                // Directly go to home without checking network
                navigateToHomeScreen();
            }
        }, 2000);
    }

    private void checkSignInState() {
        new Handler(Looper.getMainLooper()).post(() -> {
            loginViewModel.initializeAWS(activity, new CloudInterface() {
                @Override
                public void onSuccess(String state, String msg) {
                    Log.d(TAG, "initializeAWS onSuccess: " + msg);
                    if (msg.equalsIgnoreCase("SIGNED_IN")) {
                        loginViewModel.setFirstTimeAttached(false);
                        isLoggedInUser = true;
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                splashScreenViewModel.setData("SIGNED_IN");
                            });
                        }
                    } else if (msg.equalsIgnoreCase("SIGNED_OUT")) {
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                isLoggedInUser = false;
                                splashScreenViewModel.setData("SIGNED_OUT");
                                loginViewModel.setFirstTimeAttached(false);
                            });
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    loginViewModel.setFirstTimeAttached(false);
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            if (((String) error) != null && ((String) error).equalsIgnoreCase("User does not exist. ")) {
                                activity.runOnUiThread(() -> {
                                    isLoggedInUser = false;
                                    splashScreenViewModel.setData("SIGNED_OUT");
                                });
                            } else if (((String) error) != null && ((String) error).contains("Operation requires a signed-in state")) {
                                activity.runOnUiThread(() -> {
                                    makeToast(getString(R.string.user_not_exist));
                                    isLoggedInUser = false;
                                    splashScreenViewModel.setData("SIGNED_OUT");
                                });
                            } else if (((String) error) != null && ((String) error).contains("ALREADY SIGNED_OUT")) {
                                activity.runOnUiThread(() -> {
                                    splashScreenViewModel.setData("SIGNED_OUT");
                                    showAlreadySignOutFromOtherDeviceDialog();
                                    isLoggedInUser = false;
                                });
                            } else {
                                activity.runOnUiThread(() -> {
                                    splashScreenViewModel.setData("SIGNED_OUT");
                                    showAlreadySignOutFromOtherDeviceDialog();
                                    isLoggedInUser = false;
                                });
                            }
                            Log.e(TAG, "initializeAWS onFailure" + " " + error);
                        });
                    }
                }
            });
        });
    }

    private void showAlreadySignOutFromOtherDeviceDialog() {
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.ALREADY_SIGN_OUT_OTHER_DEVICE;
            activity.showDialogLoginScreen("", "");
        }
    }

    private void navigateToLoginScreen() {
        saveUserLoginState(false);
        if (finalNavController != null) {
            finalNavController.navigate(R.id.loginFragment);
            finalNavController.clearBackStack(R.id.splashScreenFragment);
        } else {
            NavController finalNavController1 = getNavigationControl();
            if (finalNavController1 != null) {
                finalNavController1.navigate(R.id.loginFragment);
                finalNavController1.clearBackStack(R.id.splashScreenFragment);
            }
        }
    }

    private void navigateToHomeScreen() {
     //   saveUserLoginState(true);
        NavController finalNavController = getNavigationControl();
        if (finalNavController != null) {
            finalNavController.navigate(R.id.homeFragment);
        } else {
            NavController finalNavController1 = getNavigationControl();
            if (finalNavController1 != null) {
                finalNavController1.navigate(R.id.homeFragment);
            }
        }
    }

    private NavController getNavigationControl() {
        NavHostFragment navHostFragment = null;
        if (activity != null) {
            navHostFragment = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        }
        NavController navController = null;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        return navController;
    }

    MainActivity activity = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    private void saveUserLoginState(boolean isLogin) {
        try {
            if (activity != null) {
                loginSharedPreferences = activity.getSharedPreferences("LoginState", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = loginSharedPreferences.edit();
                myEdit.putBoolean("isLogin", isLogin);
                myEdit.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Boolean getLoginState() {
        boolean isAlreadyLogin = false;
        try {
            if (activity != null) {
                loginSharedPreferences = activity.getSharedPreferences("LoginState", MODE_PRIVATE);
                isAlreadyLogin = loginSharedPreferences.getBoolean("isLogin", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isAlreadyLogin;
    }
}