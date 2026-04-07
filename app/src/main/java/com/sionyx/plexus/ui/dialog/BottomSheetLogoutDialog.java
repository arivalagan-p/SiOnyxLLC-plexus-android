package com.sionyx.plexus.ui.dialog;

import static android.content.Context.MODE_PRIVATE;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.LogoutBottomDialogBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.login.LoginViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.NetworkUtils;

public class BottomSheetLogoutDialog extends BottomSheetDialogFragment {
    private static final String TAG = "BottomSheetLogoutDialog";
    private LoginViewModel loginViewModel;
    private HomeViewModel homeViewModel;
    private LogoutBottomDialogBinding logoutBottomDialogBinding;
    private LifecycleOwner lifecycleOwner;
    private SharedPreferences loginSharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.dialog_theme);
        lifecycleOwner = this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        logoutBottomDialogBinding = LogoutBottomDialogBinding.inflate(inflater, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        logoutBottomDialogBinding.setViewModel(loginViewModel);
        loginSharedPreferences = activity.getSharedPreferences("LoginState", MODE_PRIVATE);

        try {
            if (getLoginState()) {
                String loginUsername = getLoginUsername();
                if (loginUsername != null)
                    logoutBottomDialogBinding.awsLoginUsername.setText("Hello, " + loginUsername);
                logoutBottomDialogBinding.awsLogout.setText(getString(R.string.logout));
                logoutBottomDialogBinding.viewLine.setVisibility(View.VISIBLE);
                logoutBottomDialogBinding.profileLayout.setVisibility(View.VISIBLE);
            } else {
                logoutBottomDialogBinding.profileLayout.setVisibility(View.GONE);
                logoutBottomDialogBinding.awsLoginUsername.setText(R.string.please_login_to_continue);
                logoutBottomDialogBinding.awsLogout.setTextColor(getResources().getColor(R.color.white, null));
                logoutBottomDialogBinding.awsLogout.setText(getString(R.string.login));
                logoutBottomDialogBinding.viewLine.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        loginViewModel.isLogout.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.e("TAG", "signOut Called ");
                new Handler().post(() -> {
                    try {
                        if (getLoginState()) {
                            NetworkUtils.pingServer(isSuccessful -> {
                                // Handle the ping result here
                                if (isSuccessful) {
                                    // Ping successful
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (getLoginState()) {
                                            dismiss();
                                            showLogoutDialog();
                                        } else {
                                            dismiss();
                                            if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                                homeViewModel.onCancelGalleryView();
                                            }
                                            if (screenType == INFO_SCREEN) {
                                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                                homeViewModel.onCancelInfoView();
                                            }
                                            if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
                                                homeViewModel.cancelNearByDeviceView();
                                            }
                                            if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
                                                homeViewModel.onPopUpViewCancel();

                                            homeViewModel.getNavController().navigate(R.id.loginFragment);
                                        }
                                    });
                                } else {
                                    // Ping failed
                                    dismiss();
                                    showInternetConnectionFailedDialog();
                                }
                            });
                        } else {
                            dismiss();
                            if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                homeViewModel.onCancelGalleryView();
                            }
                            if (screenType == INFO_SCREEN) {
                                Log.e(TAG, "showHomeScreen: " + screenType.name());
                                homeViewModel.onCancelInfoView();
                            }
                            if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
                                homeViewModel.cancelNearByDeviceView();
                            }
                            if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
                                homeViewModel.onPopUpViewCancel();

                            homeViewModel.getNavController().navigate(R.id.loginFragment);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }));

        loginViewModel.isSelectProfile.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    NetworkUtils.pingServer(isSuccessful -> {
                        if (isSuccessful) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                dismiss();
                                if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                                    Log.e(TAG, "showHomeScreen: " + screenType.name());
                                    homeViewModel.onCancelGalleryView();
                                }
                                if (screenType == INFO_SCREEN) {
                                    Log.e(TAG, "showHomeScreen: " + screenType.name());
                                    homeViewModel.onCancelInfoView();
                                }
                                if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
                                    homeViewModel.cancelNearByDeviceView();
                                }
                                if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
                                    homeViewModel.onPopUpViewCancel();

                                homeViewModel.getNavController().navigate(R.id.productListFragment);
                            });
                        } else {
                            dismiss();
                            showInternetConnectionFailedDialog();
                        }
                    });
                });
            }
        }));
        return logoutBottomDialogBinding.getRoot();
    }

    private BottomSheetBehavior<View> getBottomSheetBehavior() {
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            return BottomSheetBehavior.from(parent);
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior<View> behavior = getBottomSheetBehavior();
        if (behavior != null) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void showLogoutDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.LOG_OUT_DIALOG;
            activity.showDialogLoginScreen("", "");
        }
    }

    MainActivity activity = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    private Boolean getLoginState() {
        boolean isAlreadyLogin = false;
        try {
            if (activity != null) {
                isAlreadyLogin = loginSharedPreferences.getBoolean("isLogin", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isAlreadyLogin;
    }

    private String getLoginUsername() {
        String userName = null;
        try {
            if (activity != null) {
                userName = loginSharedPreferences.getString("userName", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userName;
    }

    private void showInternetConnectionFailedDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG;
            activity.showDialogLoginScreen("Network Unavailable", "");
        }
    }
}
