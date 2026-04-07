package com.sionyx.plexus.ui.cameramenu.fragment;

import static com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel.IS_AUTO_CONNECTED;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.wiFiHistory;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.DialogEnterPasswordConnectBinding;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.EventObserver;

import java.util.Objects;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ConnectPasswordDialogFragment extends DialogFragment {

    private static String TAG = "ConnectPasswordDialogFragment";

    private CameraPasswordSettingViewModel cameraPasswordSettingViewModel;
    private DigitalCameraInfoViewModel digitalCameraInfoViewModel;
    private HomeViewModel homeViewModel;

    private LifecycleOwner lifecycleOwner;

    private DialogEnterPasswordConnectBinding binding;
    private FrameLayout showPasswordSwitch,autoConnectSwitch;
    private View showPasswordKnob,autoConnectKnob;

    private final int MAXIMUM_PASSWORD_LENGTH = 63;
    private final int MINIMUM_PASSWORD_LENGTH = 8;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final boolean[] showPasswordIsChecked = {false};
    private final boolean[] autoConnectIsChecked = {false};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = DialogEnterPasswordConnectBinding.inflate(inflater,null,false);
        lifecycleOwner = this;
        binding.setLifecycleOwner(lifecycleOwner);
        cameraPasswordSettingViewModel = new ViewModelProvider(requireActivity()).get(CameraPasswordSettingViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(requireActivity()).get(DigitalCameraInfoViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding.setViewModel(cameraPasswordSettingViewModel);
        showPasswordSwitch = binding.showPasswordSwitch;
        autoConnectSwitch = binding.autoConnectSwitch;
        showPasswordKnob = binding.showPasswordKnob;
        autoConnectKnob = binding.autoConnectKnob;
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Transparent2);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e(TAG," onCreated " );
        if (savedInstanceState != null) {
            showPasswordIsChecked[0] = savedInstanceState.getBoolean("showPassword");
            autoConnectIsChecked[0] = savedInstanceState.getBoolean("autoConnect");
        }
        initiateUi();
    }

    @SuppressLint({"NotifyDataSetChanged", "UseCompatLoadingForDrawables"})
    private void initiateUi() {
        binding.dialogPwdParentConstraint.setBackground(requireActivity().getDrawable(R.drawable.vertical_scrollbar_track));
        setCancelable(false);
        WiFiHistory wiFiHistory = cameraPasswordSettingViewModel.get_wifiHistory();//getWiFiHistory();
        binding.dialogChangePwdCameraName.setText(wiFiHistory.getCamera_ssid());
        binding.dialogConnectBtn.setEnabled(false);
        binding.dialogConnectBtn.setAlpha(0.5f);

        binding.dialogEnterPasswordEdittext.setLongClickable(false);
        binding.dialogEnterPasswordEdittext.setTextIsSelectable(false);

        getCurrentCameraDetailsFromLocalDb(wiFiHistory.getCamera_ssid());

        binding.dialogEnterPasswordEdittext.post(() -> {

            // Restore Show Password state
            cameraPasswordSettingViewModel.onDialogShowPassword(showPasswordIsChecked[0]);

            float showPwdX = showPasswordIsChecked[0] ? showPasswordSwitch.getWidth() - showPasswordKnob.getWidth() - 16 : 0;

            showPasswordKnob.setTranslationX(showPwdX);
            showPasswordSwitch.setBackgroundResource(showPasswordIsChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);

            // Restore Auto-Connect toggle
            float autoX = autoConnectIsChecked[0] ? autoConnectSwitch.getWidth() - autoConnectKnob.getWidth() - 16 : 0;

            autoConnectKnob.setTranslationX(autoX);
            autoConnectSwitch.setBackgroundResource(autoConnectIsChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);

            // Sync back to DB & static variable
            updateAutoToggle();
        });
        cameraPasswordSettingViewModel.isDialogCancel.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(aBoolean -> {
            Log.e(TAG,"isDialogCancel called " + aBoolean);
            if (aBoolean){
                mHandler.post(this::dismiss);
                homeViewModel.hasShowProgressBar(false);
            }
        }));

        cameraPasswordSettingViewModel.isDialogShowPassword.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.e(TAG, "isDialogShowPassword called " + aBoolean);

            binding.dialogEnterPasswordEdittext.setTransformationMethod(
                    aBoolean ? null : PasswordTransformationMethod.getInstance());
            binding.dialogEnterPasswordEdittext.setSelection(binding.dialogEnterPasswordEdittext.length());

            showPasswordKnob.animate()
                    .translationX(aBoolean ? showPasswordSwitch.getWidth() - showPasswordKnob.getWidth() - 16 : 0)
                    .setDuration(200).start();

            showPasswordSwitch.setBackgroundResource(aBoolean ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);

        }));

        cameraPasswordSettingViewModel.isDialogConnect.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.e(TAG,"isDialogConnect called " + aBoolean);

            getIsAutoConnectStatus(); // get from toggle

                if (Objects.requireNonNull(binding.dialogEnterPasswordEdittext.getText()).toString().isEmpty()){
                    Log.e(TAG,"dialogEnterPasswordEdittext empty ");
                    // password authentication logic from the edittext to database
                } else {
                    cameraPasswordSettingViewModel.setOnDialogCameraPassword(binding.dialogEnterPasswordEdittext.getText().toString());

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            cameraPasswordSettingViewModel.onConnectCamera(binding.dialogEnterPasswordEdittext.getText().toString());
                        }
                    },500);

                    mHandler.post(this::dismiss);

                }

        }));

        binding.dialogEnterPasswordEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.toString().isEmpty()){
                    binding.dialogConnectBtn.setEnabled(false);
                    binding.dialogConnectBtn.setAlpha(0.5f);
                }

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().isEmpty()){
                    binding.dialogConnectBtn.setEnabled(false);
                    binding.dialogConnectBtn.setAlpha(0.5f);
                } else {
                    binding.dialogConnectBtn.setEnabled(true);
                    binding.dialogConnectBtn.setAlpha(1.0f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                if (length >= MINIMUM_PASSWORD_LENGTH && length <= MAXIMUM_PASSWORD_LENGTH){
                    binding.dialogConnectBtn.setEnabled(true);
                    binding.dialogConnectBtn.setAlpha(1.0f);
                } else {
                    binding.dialogConnectBtn.setEnabled(false);
                    binding.dialogConnectBtn.setAlpha(0.5f);
                }
            }
        });

        showPasswordSwitch.setOnClickListener(v -> {
            showPasswordIsChecked[0] = !showPasswordIsChecked[0];
            cameraPasswordSettingViewModel.onDialogShowPassword(showPasswordIsChecked[0]);
        });

        autoConnectSwitch.setOnClickListener(v -> {
            autoConnectIsChecked[0] = !autoConnectIsChecked[0];

            // Animate knob
            float translationX = autoConnectIsChecked[0] ? autoConnectSwitch.getWidth() - autoConnectKnob.getWidth() - 16 : 0;
            autoConnectKnob.animate().translationX(translationX).setDuration(200).start();

            // Change background
            autoConnectSwitch.setBackgroundResource(autoConnectIsChecked[0] ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);
            updateAutoToggle();
        });
    }

    private void updateAutoToggle() {
        Log.e(TAG,"updateAutoToggle " + autoConnectIsChecked[0]);
        int autoToggle = autoConnectIsChecked[0] ? 1 : 0;
        digitalCameraInfoViewModel.updateCameraAutoConnect(wiFiHistory.getCamera_ssid(),autoToggle);
    }

    private void getCurrentCameraDetailsFromLocalDb(String cameraSsid) {
        digitalCameraInfoViewModel.checkNWDSsidIsExit(cameraSsid).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<NightwaveDigitalWiFiHistory>() {

            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onSuccess(NightwaveDigitalWiFiHistory digitalCameraHistory) {
                if (digitalCameraHistory !=null){
                    String currentPassword = digitalCameraHistory.getCamera_password();
                    Log.e(TAG, "getCurrentPasswordFromDb: pwd " + currentPassword);
                    binding.dialogEnterPasswordEdittext.setText(currentPassword);
                } else {
                    Log.e(TAG, "getCurrentPasswordFromDb: null " );
                    binding.dialogEnterPasswordEdittext.setText("");
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "getCurrentPasswordFromDb noData : " + e.getMessage());
            }
        });
    }

    private void getIsAutoConnectStatus() {
        IS_AUTO_CONNECTED = autoConnectIsChecked[0] ? 1 : 0;

    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showPassword", showPasswordIsChecked[0]);
        outState.putBoolean("autoConnect", autoConnectIsChecked[0]);
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent2);
        getDialog().getWindow().setAttributes(params);
    }
}
