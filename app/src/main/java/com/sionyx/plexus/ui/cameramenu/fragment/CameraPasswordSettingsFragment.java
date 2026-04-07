package com.sionyx.plexus.ui.cameramenu.fragment;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.dome.librarynightwave.utils.Constants.NWD_SETTING_INFO_HTML_FILE_PATH;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.getWiFiHistory;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.wiFiHistory;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentDigitalWifiSettingsBinding;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.EventObserver;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CameraPasswordSettingsFragment extends Fragment {

    private static String TAG = "CameraPasswordSettingsFragment";

    private FragmentDigitalWifiSettingsBinding binding;

    private HomeViewModel homeViewModel;
    private CameraPasswordSettingViewModel cameraSetPasswordViewModel;
    private DigitalCameraInfoViewModel digitalCameraInfoViewModel;

    private LifecycleOwner lifecycleOwner;
    private FrameLayout showPasswordSwitch, autoConnectSwitch;
    private View showPasswordKnob, autoConnectKnob;
    boolean[] isChecked = {false}; // mutable boolean wrapper
    boolean[] hasChecked = {false}; // mutable boolean wrapper
    private String currentPassword;

    private final Handler mHandler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDigitalWifiSettingsBinding.inflate(inflater, container, false);
        lifecycleOwner = this;
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        cameraSetPasswordViewModel = new ViewModelProvider(requireActivity()).get(CameraPasswordSettingViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(requireActivity()).get(DigitalCameraInfoViewModel.class);
        binding.setLifecycleOwner(lifecycleOwner);
        binding.setViewModel(cameraSetPasswordViewModel);

        showPasswordSwitch = binding.showPasswordSwitch;
        autoConnectSwitch = binding.autoConnectSwitch;
        showPasswordKnob = binding.showPasswordKnob;
        autoConnectKnob = binding.autoConnectKnob;
        if (savedInstanceState != null) {
            isChecked[0] = savedInstanceState.getBoolean("showPassword");
            hasChecked[0] = savedInstanceState.getBoolean("autoConnect");
        }
        initiateView();

        return binding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initiateView() {

        requireActivity().getOnBackPressedDispatcher().addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToHome();
            }
        });

        WiFiHistory wiFiHistory = getWiFiHistory();

            if (wiFiHistory.getIs_wifi_connected() == WIFI_CONNECTED) {

            digitalCameraInfoViewModel.getUpdatedPasswordFromCamera(wiFiHistory.getCamera_ssid(), requireContext());
            digitalCameraInfoViewModel.cameraResponse.observe(lifecycleOwner, new EventObserver<>(password -> {
                currentPassword = password;
                Log.e(TAG, "Current password: "+currentPassword);
                updatePasswordText();
            }));


                getAutoConnectToggleDatFromDataBase(wiFiHistory.getCamera_ssid());

                // show change password view
                binding.wifiSettingsCameraConnectedView.setVisibility(VISIBLE);
                binding.wifiSettingCameraNameTxt.setVisibility(VISIBLE);
                binding.wifiSettingShowInfoWebview.setVisibility(GONE);

                binding.wifiSettingCameraNameTxt.setText(wiFiHistory.getCamera_ssid());
                binding.wifiSettingTitle.setText(getString(R.string.digital_wifi_settings));
            } else {
                showInfoWebView();
        }

        binding.wifiSettingCloseIcon.setOnClickListener(v ->
                cameraSetPasswordViewModel.onSelectCancel()
        );

        cameraSetPasswordViewModel.isSelectCancel.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean){
                homeViewModel.getNavController().navigate(R.id.homeFragment);
            }
        }));

        cameraSetPasswordViewModel.isSelectChangePassword.observe(lifecycleOwner,new EventObserver<>(aBoolean -> {
            homeViewModel.getNavController().navigate(R.id.changePasswordFragment);
        }));

        binding.getRoot().post(() -> {


            float pwdX = isChecked[0]
                    ? showPasswordSwitch.getWidth() - showPasswordKnob.getWidth() - 16
                    : 0;

            showPasswordKnob.setTranslationX(pwdX);
            showPasswordSwitch.setBackgroundResource(
                    isChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off
            );

            updatePasswordText();


            float autoX = hasChecked[0]
                    ? autoConnectSwitch.getWidth() - autoConnectKnob.getWidth() - 16
                    : 0;

            autoConnectKnob.setTranslationX(autoX);
            autoConnectSwitch.setBackgroundResource(
                    hasChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off
            );

            updateAutoToggle();
        });

        showPasswordSwitch.setOnClickListener(v -> {
            isChecked[0] = !isChecked[0];

            float translationX = isChecked[0] ? showPasswordSwitch.getWidth() - showPasswordKnob.getWidth() - 16 : 0;
            showPasswordKnob.animate().translationX(translationX).setDuration(200).start();

            showPasswordSwitch.setBackgroundResource(isChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);

            updatePasswordText();

            if (currentPassword == null) {
                mHandler.postDelayed(() -> {
                    if (isChecked[0]) {
                        showPasswordKnob.animate().translationX(0).setDuration(200).start();
                        showPasswordSwitch.setBackgroundResource(R.drawable.bg_switch_off);
                        binding.wifiShowPwdText.setText(getString(R.string.show_password));
                        binding.wifiShowPwdText.setAllCaps(true);
                        isChecked[0] = !isChecked[0];
                        Toast.makeText(requireActivity(), "Unable to fetch password.", Toast.LENGTH_SHORT).show();
                    }

                }, 200);
            }

        });

        autoConnectSwitch.setOnClickListener(v -> {
            hasChecked[0] = !hasChecked[0];

            // Animate knob
            float translationX = hasChecked[0] ? autoConnectSwitch.getWidth() - autoConnectKnob.getWidth() - 16 : 0;

            autoConnectKnob.animate().translationX(translationX).setDuration(200).start();

            // Change background
            autoConnectSwitch.setBackgroundResource(hasChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);

            updateAutoToggle();
        });
    }

    private void updatePasswordText() {
        if (isChecked[0]) {
            binding.wifiShowPwdText.setText(currentPassword != null ? currentPassword : "");
            binding.wifiShowPwdText.setAllCaps(false);
        } else {
            binding.wifiShowPwdText.setText(getString(R.string.show_password));
            binding.wifiShowPwdText.setAllCaps(true);
        }
    }

    private void showInfoWebView() {
        binding.wifiSettingTitle.setText(getString(R.string.only_info));
        binding.wifiSettingTitle.setVisibility(VISIBLE);
        binding.wifiSettingCloseIcon.setVisibility(VISIBLE);
        binding.viewLine1.setVisibility(VISIBLE);
        binding.wifiSettingsCameraConnectedView.setVisibility(GONE);
        binding.wifiSettingCameraNameTxt.setVisibility(GONE);
        binding.wifiSettingShowInfoWebview.setVisibility(VISIBLE);
        WebView webView = binding.wifiSettingShowInfoWebview;
        webView.setVisibility(VISIBLE);
        try {
            setWebView(webView);
            webView.loadUrl(NWD_SETTING_INFO_HTML_FILE_PATH);
        } catch (Exception e) {
            Log.e(TAG, "WebView error: " + e.getMessage());
        }
    }

    private void backToHome() {
        if (homeViewModel.getNavController() != null) {
            homeViewModel.getNavController().popBackStack(R.id.cameraWifiSettingsFragment, true);
            homeViewModel.getNavController().navigate(R.id.homeFragment);
        }
    }

    private void updateAutoToggle() {
        Log.e(TAG,"updateAutoToggle " + hasChecked[0]);
        int autoToggle = hasChecked[0] ? 1 : 0;
        digitalCameraInfoViewModel.updateCameraAutoConnect(wiFiHistory.getCamera_ssid(),autoToggle);
    }


    private void getAutoConnectToggleDatFromDataBase(String cameraSsid) {
        digitalCameraInfoViewModel.checkNWDSsidIsExit(cameraSsid).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<NightwaveDigitalWiFiHistory>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(NightwaveDigitalWiFiHistory digitalCameraHistory) {
                        if (digitalCameraHistory != null) {

                            boolean DBvalue = digitalCameraHistory.getIs_auto_connected() == 1;

                            // IMPORTANT — FIX: update your boolean state
                            hasChecked[0] = DBvalue;

                            float translationX = DBvalue ? autoConnectSwitch.getWidth() - autoConnectKnob.getWidth() - 16 : 0;

                            autoConnectKnob.setTranslationX(translationX);
                            autoConnectSwitch.setBackgroundResource(DBvalue ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);

                            updateAutoToggle(); // sync ViewModel
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "getCurrentPasswordFromDb noData : " + e.getMessage());
                    }
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showPassword", isChecked[0]);
        outState.putBoolean("autoConnect", hasChecked[0]);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
    }

}
