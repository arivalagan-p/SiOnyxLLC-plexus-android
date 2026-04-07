package com.sionyx.plexus.ui.dialog;

import static com.dome.librarynightwave.utils.Constants.CURRENT_CAMERA_SSID.NIGHTWAVE;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.SCCPConstants.SCCP_OPSIN_FACTORTY_RESET.RESET_SOFT;
import static com.sionyx.plexus.R.drawable.button_bg;
import static com.sionyx.plexus.ui.camera.CameraFragment.isChangeCameraNameButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isFactoryRestButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isInfoButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isSettingButtonPressed;
import static com.sionyx.plexus.utils.Constants.FPGA_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OVERLAY_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.RISCV_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.WIFI_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.repository.opsinmodel.OpsinVersionDetails;
import com.dome.librarynightwave.model.repository.opsinmodel.ProductInfo;
import com.dome.librarynightwave.model.repository.opsinmodel.SDCardInfo;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.gson.Gson;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCameraInfoBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.model.Manifest;

import java.io.InputStream;
import java.util.Objects;

public class CameraInfoTabLayoutDialog extends DialogFragment {
    private static final String TAG = "CameraTabLayoutDialog";
    private FragmentCameraInfoBinding binding;
    private CameraInfoViewModel viewModel;
    private BleWiFiViewModel bleWiFiViewModel;
    private CameraViewModel cameraViewModel;
    private HomeViewModel homeViewModel;
    private LifecycleOwner lifecycleOwner;
    public TCPConnectionViewModel tcpConnectionViewModel;
    private final Handler mHandler = new Handler();
    private String cameraSSId = "";

    private int cameraXValue;

    @Override
    public void onResume() {
        super.onResume();
        assert getDialog() != null;
        WindowManager.LayoutParams params = Objects.requireNonNull(getDialog().getWindow()).getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent);
        getDialog().getWindow().setAttributes(params);
    }

    private void showOpsinCameraNotResponding(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_NOT_RESPONSE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context ctx = getActivity();
        binding = FragmentCameraInfoBinding.inflate(inflater, null, false);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        binding.getRoot().setMinimumWidth((int) (width * 1f));
        binding.getRoot().setMinimumHeight((int) (height * 1f));

        binding.setLifecycleOwner(requireActivity());
        lifecycleOwner = this;
        viewModel = new ViewModelProvider(requireActivity()).get(CameraInfoViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        binding.setViewModel(viewModel);
        binding.setCameraName("");
        cameraSSId = bleWiFiViewModel.getConnectedSsidFromWiFiManager();
        binding.setCameraName(cameraSSId);
        viewModel.setUpdateCameraName(false);

        if (cameraSSId.contains(FILTER_STRING1)
                || cameraSSId.contains(FILTER_STRING2)
                || cameraSSId.contains(FILTER_STRING3)) {
            if (binding.cameraModelIcon != null) {
                binding.cameraModelIcon.setImageResource(R.drawable.ic_nw_analog_connected);
            }
        }

        if (cameraSSId.contains(FILTER_STRING4) || cameraSSId.contains(FILTER_STRING5)) {
            if (binding.cameraModelIcon != null)
                binding.cameraModelIcon.setImageResource(R.drawable.opsin_connected);
        }
    //  If need for future implementation
    /*    if (cameraSSId.contains(FILTER_STRING6)) {
            if (binding.cameraModelIcon != null)
                binding.cameraModelIcon.setImageResource(R.drawable.ic_nw_digital_connected);
        }*/
        subscribeUI();
        return binding.getRoot();
    }

    @SuppressLint("ResourceAsColor")
    private void subscribeUI() {
        setCancelable(false);
        binding.deviceInfoLayout.setVisibility(View.VISIBLE);
        binding.changeNamePasswordLayout.setVisibility(View.GONE);

        // String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
        try {
            String cameraRiscv = String.valueOf(CameraViewModel.cameraXValue);
            if (cameraRiscv != null) {
                String[] aCameraRiscVersion = cameraRiscv.split("\\.");
                cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);
                Log.e(TAG, "aCameraRiscVersion: " + cameraXValue);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        switch (currentCameraSsid) {
            case OPSIN:
                binding.cameraInfoLayout.setBackgroundResource(R.drawable.ic_tab_layout_background_with_border);
                binding.resetInProgressLayout.setBackgroundResource(R.drawable.ic_tab_layout_background_with_border);
                binding.changeUsernameBtn.setVisibility(View.VISIBLE);
                binding.backToSettingsBtn.setVisibility(View.VISIBLE);
                binding.factoryResetBtn.setVisibility(View.VISIBLE);
                binding.updateFwBtn.setVisibility(View.GONE);
                // here gone pwd, Mac Address, Top version
                binding.topVersion.setVisibility(View.GONE);
                binding.topVersionCurrent.setVisibility(View.GONE);
                binding.passwordTitle.setVisibility(View.GONE);
                binding.password.setVisibility(View.GONE);
                binding.macTitle.setVisibility(View.GONE);
                binding.macAddress.setVisibility(View.GONE);
                // here visible ISP, Overlay, Recovery versions
                binding.ispVersionTitle.setVisibility(View.VISIBLE);
                binding.ispVersion.setVisibility(View.VISIBLE);
                binding.recoveryVersionTitle.setVisibility(View.VISIBLE);
                binding.recoveryVersion.setVisibility(View.VISIBLE);
                binding.overlayVersionTitle.setVisibility(View.VISIBLE);
                binding.overlayVersion.setVisibility(View.VISIBLE);

                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    binding.sdCardInfoLayout.setVisibility(View.VISIBLE);
                    binding.serialNumber.setVisibility(View.VISIBLE);
                    binding.serialNumberLabel.setVisibility(View.VISIBLE);
                    binding.bleVersion.setVisibility(View.VISIBLE);
                    binding.bleVersionCurrent.setVisibility(View.VISIBLE);
                } else {
                    binding.sdCardInfoLayout.setVisibility(View.GONE);
                    binding.serialNumber.setVisibility(View.GONE);
                    binding.serialNumberLabel.setVisibility(View.GONE);
                    binding.bleVersion.setVisibility(View.GONE);
                    binding.bleVersionCurrent.setVisibility(View.GONE);
                }
                break;
            case NIGHTWAVE:
                binding.cameraInfoLayout.setBackgroundResource(R.drawable.ic_tab_layout_background);
                binding.resetInProgressLayout.setBackgroundResource(R.drawable.ic_factory_reset_background);
                binding.changeUsernameBtn.setVisibility(View.VISIBLE);
                binding.backToSettingsBtn.setVisibility(View.VISIBLE);
                binding.factoryResetBtn.setVisibility(View.GONE);
                binding.updateFwBtn.setVisibility(View.VISIBLE);
                // here visible pwd, Mac Address, Top version
                binding.topVersion.setVisibility(View.VISIBLE);
                binding.topVersionCurrent.setVisibility(View.VISIBLE);
                binding.passwordTitle.setVisibility(View.VISIBLE);
                binding.password.setVisibility(View.VISIBLE);
                binding.macTitle.setVisibility(View.VISIBLE);
                binding.macAddress.setVisibility(View.VISIBLE);
                // here gone ISP, Overlay, Recovery, ble versions
                binding.ispVersionTitle.setVisibility(View.GONE);
                binding.ispVersion.setVisibility(View.GONE);
                binding.recoveryVersionTitle.setVisibility(View.GONE);
                binding.recoveryVersion.setVisibility(View.GONE);
                binding.overlayVersionTitle.setVisibility(View.GONE);
                binding.overlayVersion.setVisibility(View.GONE);
                binding.sdCardInfoLayout.setVisibility(View.GONE);
                binding.serialNumber.setVisibility(View.GONE);
                binding.serialNumberLabel.setVisibility(View.GONE);
                binding.bleVersion.setVisibility(View.GONE);
                binding.bleVersionCurrent.setVisibility(View.GONE);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"subscribeUI : NW_Digital");
                break;
        }

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String allowedCharacters = "^[a-zA-Z0-9!@#$%&*]*$";
                if (!s.toString().matches(allowedCharacters)) {
                    // If the entered text does not match the pattern, remove the last entered character
                    String filteredText = s.toString().replaceAll("[^a-zA-Z0-9!@#$%&*]", "");
                    binding.editNameText.setText(filteredText);
                    binding.editNameText.setSelection(binding.editNameText.getText().length());
                    try {
                        if (isAdded()) {
                            Toast.makeText(requireActivity(), getString(R.string.camera_name_error), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
//        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
//            try {
//                for (int i = start; i < end; i++) {
//                    int asciiValue = source.charAt(i);
//                    Log.e(TAG, "filter: " + asciiValue);
//                    if (asciiValue < 33 || asciiValue == 96 || asciiValue == 126 || asciiValue > 127 && asciiValue != 8364) {
//                        Toast.makeText(requireContext(), R.string.camera_name_error, Toast.LENGTH_SHORT).show();
//                        return source.toString().substring(start, i) + "";
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return source;
//        };

            // --- Nightwave Analog firmware update ---
            String cameraReleaseVersion = homeViewModel.getCurrentFwVersion().getReleaseVersion();
            String appRelease = homeViewModel.getManifest().getVersions().getRelease();
            String releaseNotes = homeViewModel.getManifest().getReleaseNotes();

            cameraReleaseVersion = cameraReleaseVersion != null ? cameraReleaseVersion.trim() : "";
            appRelease = appRelease != null ? appRelease.trim() : "";
            releaseNotes = releaseNotes != null ? releaseNotes.trim() : "";

            final String finalCamRel = cameraReleaseVersion;
            final String finalAppRel = appRelease;
            final String finalNotes = releaseNotes;

            viewModel.isFirmwareUpdateAvailable.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
                if (!hasSelect) return;

                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);

                // Handling depends on UI state after isFirmwareDialogNeeded result
                Boolean needed = viewModel.isFirmwareDialogNeeded().getValue();
                if (Boolean.TRUE.equals(needed)) {
                    showFwUpdateDialog(finalNotes);
                } else if (!finalCamRel.equals(finalAppRel)) {
                    showAppUpdateDialog(getString(R.string.app_update));
                }
            }));

            viewModel.isFirmwareDialogNeeded().observe(this, needed -> {

                if (Boolean.TRUE.equals(needed)) {

                    Log.e(TAG, "Firmware dialog required");

                    binding.updateFwBtn.setText(R.string.fw_update_available);
                    binding.updateFwBtn.setEnabled(true);

                } else if (!finalCamRel.equals(finalAppRel)) {

                    Log.e(TAG, "App update required: app=" + finalAppRel + " camera=" + finalCamRel);

                    binding.updateFwBtn.setText(R.string.check_for_update);
                    binding.updateFwBtn.setEnabled(true);

                } else {

                    Log.e(TAG, "Firmware up to date");

                    binding.updateFwBtn.setText(R.string.fw_up_to_date);
                    binding.updateFwBtn.setEnabled(false);
                    binding.updateFwBtn.setBackgroundResource(button_bg);
                    binding.updateFwBtn.setTextColor(requireContext().getColorStateList(R.color.color_lightSliver));
                }
            });

        viewModel.isCancel.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                tcpConnectionViewModel.observeOpsinSerialNumber().removeObserver(opsinSerialNumberObserver);
                tcpConnectionViewModel.observeOpsinSerialNumber().removeObservers(lifecycleOwner);
            }
        }));

        viewModel.isCancelChangeNamePassword.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                viewModel.setUpdateCameraName(false);
            }
        }));

        viewModel.isBackToSettings.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(true);
                cameraViewModel.setShowAllMenu(false);
                viewModel.setUpdateCameraName(false);
            }
        }));

        viewModel.isChangeNamePassword.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (!isChangeCameraNameButtonPressed && !isFactoryRestButtonPressed) {
                    isChangeCameraNameButtonPressed = true;
                    cameraViewModel.setShowExpandMenu(false);
                    cameraViewModel.setShowAllMenu(true);
                    binding.deviceInfoLayout.setVisibility(View.GONE);
                    binding.changeNamePasswordLayout.setVisibility(View.VISIBLE);
                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            tcpConnectionViewModel.getCameraName();
                            break;
                        case OPSIN:
                            tcpConnectionViewModel.getOpsinCameraName();
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"chnagePasswordObserver : NW_Digital");
                            break;
                    }

                    binding.editNameText.addTextChangedListener(textWatcher);
                    //  binding.editNameText.setFilters(new InputFilter[]{inputFilter});
                }
            }
        }));

        viewModel.isSaveChanges.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                String name = binding.editNameText.getText().toString().trim();
                if (name.isEmpty())
                    binding.editNameText.setError("Device Name cannot be blank");

                if (!name.isEmpty()) {
                    if (isValidUsername(name)) {
                        hideKeyboard(requireActivity());
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                viewModel.setUpdateCameraName(true);
                                /*set*/
                                tcpConnectionViewModel.setCameraName(name);
                                /*get*/
                                try {
                                    Thread.sleep(200);
                                    tcpConnectionViewModel.getCameraName();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case OPSIN:
                                viewModel.setUpdateCameraName(true);
                                tcpConnectionViewModel.setOpsinCameraName(name);
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG,"saveChanges : NW_Digital");
                                break;
                        }
                        backToDeviceInfoLayout();
                    } else {
                        binding.editNameText.setError(getString(R.string.camera_name_length));
                    }
                }
            }
        }));

        viewModel.isFactoryReset.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (!isChangeCameraNameButtonPressed && !isFactoryRestButtonPressed) {
                    isFactoryRestButtonPressed = true;
                    binding.deviceInfoLayout.setVisibility(View.GONE);
                    binding.changeNamePasswordLayout.setVisibility(View.GONE);
                    binding.factoryResetLayout.setVisibility(View.VISIBLE);
                    binding.resetInProgressLayout.setVisibility(View.GONE);
                    cameraViewModel.setShowExpandMenu(false);
                    cameraViewModel.setShowAllMenu(true);
                }
            }
        }));

        viewModel.isBackToDeviceInfo.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                hideKeyboard();
                backToDeviceInfoLayout();
                viewModel.setUpdateCameraName(false);
            }
        }));

        viewModel.isCancelFactoryReset.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
            }
        }));


        viewModel.isResetMyDevice.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                binding.cameraInfoLayout.setVisibility(View.GONE);
                binding.resetInProgressLayout.setVisibility(View.VISIBLE);
                binding.resetInProgressView.setVisibility(View.VISIBLE);
                binding.factoryResetLayout.setVisibility(View.GONE);

                mHandler.postDelayed(() -> {
                    binding.cameraInfoLayout.setVisibility(View.GONE);
                    binding.factoryResetLayout.setVisibility(View.GONE);
                    binding.resetInProgressView.setVisibility(View.GONE);
                    binding.resetInProgressDoneLayout.setVisibility(View.VISIBLE);
                    /* sent factory reset command*/
                    tcpConnectionViewModel.setFactoryReset(RESET_SOFT.getValue());

                }, 3000);

            }
        }));

        /* for this condition avoid when connect Opsin camera don't observe camera info */
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                new Handler().postDelayed(() -> tcpConnectionViewModel.getCameraName(), 100);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getWiFiPassword(), 500);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getRiscVersion(), 700);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getFPGAVersion(), 900);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getCameraInfo(), 1100);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getReleasePkgVer(), 1300);


            tcpConnectionViewModel.observeCameraName().observe(requireActivity(), object -> {
                Log.e("TAG", "observeCameraInfo:4 " + object);
                if (object != null) {
                    if (object instanceof CommandError) {
                        Log.e("TAG", "observeCameraInfo5: " + ((CommandError) object).getError());
                        binding.setCameraName(cameraSSId);
                        if (binding.name != null) {
                            binding.name.setText(cameraSSId);
                            binding.editNameText.setText(cameraSSId);
                        }
                    } else {
                        String response = object.toString();
                        Log.e(TAG, "onChanged1: " + response);
                        if (!response.equals("")) {
                            if (binding.name != null) {
                                binding.name.setText(response.trim());
                            }
                            try {
                                binding.setCameraName(response.trim());
                                binding.editNameText.setText(!response.trim().equals("") ? response.trim() : cameraSSId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (viewModel.getUpdateCameraName()) {
                                bleWiFiViewModel.updateCameraName(cameraSSId, response.trim());
                                Log.e(TAG, "name onChanged2: " + response.trim());
                                viewModel.setUpdateCameraName(false);
                            }
                        } else {
                            Log.e(TAG, "onChanged3: " + response + " " + cameraSSId);
                            binding.name.setText(cameraSSId);
                            binding.setCameraName(cameraSSId);
                            binding.editNameText.setText(cameraSSId);
                        }
                    }
                }
            });

            tcpConnectionViewModel.observeCameraPassword().observe(requireActivity(), object -> {
                if (object != null) {
                    if (object instanceof CommandError) {
                        Log.e("TAG", "observeCameraInfo: " + ((CommandError) object).getError());
                        if (binding.password != null) {
                            binding.password.setText(getString(R.string.unknown));
                        }
                    } else {
                        String response = object.toString();
                        if (!response.equals("")) {
                            if (binding.password != null) {
                                binding.password.setText(removeStrings(response));
                            }
                            binding.password.setText(removeStrings(response));
                        }
                    }
                }
            });

            tcpConnectionViewModel.observeCameraInfo().observe(requireActivity(), object -> {
                if (object != null) {
                    if (!isAdded() || requireActivity().isFinishing())
                        return;

                    if (object instanceof CommandError) {
                        Log.e(TAG, "observeCameraInfo: " + ((CommandError) object).getError());
                        if (binding.wifiVersionCurrent != null) {
                            binding.wifiVersionCurrent.setText(getString(R.string.unknown));
                        }
                        if (binding.macAddress != null) {
                            binding.macAddress.setText(getString(R.string.unknown));
                        }
                        if (binding.ssid != null) {
                            binding.ssid.setText(getString(R.string.unknown));
                        }
                        if (binding.topVersionCurrent != null) {
                            binding.topVersionCurrent.setText(getString(R.string.unknown));
                        }
                    } else {
                        try {
                            String response = object.toString();
                            Log.e(TAG, "observeCameraInfo response : " + response);
                            String releaseVersion = response.substring(64, 80);
                            String wifiRtosVersion = response.substring(80, 128);
                            String wifiMac = response.substring(176, 194);
                            String ssid = response.substring(212);

                            if (binding.wifiVersionCurrent != null) {
                                binding.wifiVersionCurrent.setText(removeStrings(wifiRtosVersion));
                            }
                            if (binding.macAddress != null) {
                                binding.macAddress.setText(removeStrings(wifiMac));
                            }
                            if (binding.ssid != null) {
                                ssid = ssid.replace("\"", "");
                                binding.ssid.setText(removeStrings(ssid));
                            }
                            if (binding.topVersionCurrent != null) {
                                binding.topVersionCurrent.setText(removeStrings(releaseVersion));
                            }
                        } catch (Exception e) {
                            if (binding.wifiVersionCurrent != null) {
                                binding.wifiVersionCurrent.setText(getString(R.string.unknown));
                            }
                            if (binding.macAddress != null) {
                                binding.macAddress.setText(getString(R.string.unknown));
                            }
                            if (binding.ssid != null) {
                                binding.ssid.setText(getString(R.string.unknown));
                            }
                            if (binding.topVersionCurrent != null) {
                                binding.topVersionCurrent.setText(getString(R.string.unknown));
                            }
                            e.printStackTrace();
                        }
                    }
                }

            });

            tcpConnectionViewModel.observeReleaseVersion().observe(lifecycleOwner, aString -> {
                if (aString != null) {
                    if (aString instanceof CommandError) {
                        Log.e(TAG, "observeReleaseVersion: " + ((CommandError) aString).getError());
                    } else {
                        Log.e(TAG, "observeReleaseVersion: " + aString);
                        if (binding.topVersionCurrent != null) {
                            binding.topVersionCurrent.setText(removeStrings(aString.toString()));
                        }
                    }
                }
            });

            tcpConnectionViewModel.observeFpgaVersion().observe(requireActivity(), object -> {
                if (object instanceof CommandError) {
                    Log.e(TAG, "observeFpgaVersion: " + ((CommandError) object).getError());
                    if (binding.fpgaVersionCurrent != null) {
                        binding.fpgaVersionCurrent.setText(getString(R.string.unknown));
                    }
                } else {
                    try {
                        String response = object.toString();
                        if (binding.fpgaVersionCurrent != null) {
                            binding.fpgaVersionCurrent.setText(removeStrings(response));
                        }
                    } catch (Exception e) {
                        if (binding.fpgaVersionCurrent != null) {
                            binding.fpgaVersionCurrent.setText(getString(R.string.unknown));
                        }
                        e.printStackTrace();
                    }
                }
            });
            tcpConnectionViewModel.observeRisvVersion().observe(requireActivity(), object -> {
                if (object instanceof CommandError) {
                    Log.e(TAG, "observeFpgaVersion: " + ((CommandError) object).getError());
                    if (binding.riscvVersionCurrent != null) {
                        binding.riscvVersionCurrent.setText(getString(R.string.unknown));
                    }
                } else {
                    try {
                        String response = object.toString();
                        if (binding.riscvVersionCurrent != null) {
                            binding.riscvVersionCurrent.setText(removeStrings(response));
                        }
                    } catch (Exception e) {
                        if (binding.riscvVersionCurrent != null) {
                            binding.riscvVersionCurrent.setText(getString(R.string.unknown));
                        }
                        e.printStackTrace();
                    }
                }
            });

            cameraViewModel.observeAppHasForegroundOrBackground().observe(this.getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    new Handler().postDelayed(() -> {
                        binding.editNameText.clearFocus();
                        hideKeyboard();
                        Log.d(TAG, "appHasForegroundOrBackground: " + aBoolean);
                    }, 800);

                } else {
                    hideKeyboard();
                    Log.d(TAG, "appHasForegroundOrBackground: " + aBoolean);
                }
            }));

                break;
            case OPSIN: //Opsin
                new Handler().postDelayed(() -> tcpConnectionViewModel.getProductVersion(), 100);

            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                binding.sdCardInfoLayout.setVisibility(View.VISIBLE);
                binding.serialNumber.setVisibility(View.VISIBLE);
                binding.serialNumberLabel.setVisibility(View.VISIBLE);

                new Handler().postDelayed(() -> tcpConnectionViewModel.getOpsinSdCardInfomation(), 500);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getSerialNumber(), 1000);
            } else {
                binding.sdCardInfoLayout.setVisibility(View.GONE);
                binding.serialNumber.setVisibility(View.GONE);
                binding.serialNumberLabel.setVisibility(View.GONE);
            }


            binding.ssid.setText(cameraSSId);
            tcpConnectionViewModel.observeOpsinCameraVersionInfo().observe(this.getViewLifecycleOwner(), new EventObserver<>(object -> {
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                    // for this observe camera info dialog view
                    viewModel.observeCameraVersionDetails(object);
                } else {
                    Log.d(TAG, "observeOpsinCameraVersionInfo: not observe in info dialog screen");
                }
            }));

            /*tcpConnectionViewModel.observeOpsinCameraVersionInfo().observe(lifecycleOwner, new Observer<Object>() {
                @Override
                public void onChanged(Object object) {
                    if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                        if (object != null) {
                            if (object instanceof CommandError) {
                                Log.e(TAG, "observeOpsinCameraVersionInfo Error: " + ((CommandError) object).getError());
                            } else {
                                Log.e(TAG, "observeOpsinCameraVersionInfo ");
                                OpsinVersionDetails opsinVersionDetails = (OpsinVersionDetails) object;
                                binding.riscvVersionCurrent.setText(opsinVersionDetails.getRiscv());
                                binding.fpgaVersionCurrent.setText(opsinVersionDetails.getFpga());
                                binding.wifiVersionCurrent.setText(opsinVersionDetails.getWifi());
                                tcpConnectionViewModel.getProductInfo();
                            }
                        }
                    } else {
                        Log.d(TAG, "observeOpsinCameraVersionInfo: not observe in info dialog screen");
                    }
                }
            });*/

            tcpConnectionViewModel.observeOpsinProductInfo().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null) {
                    ProductInfo productInfos = (ProductInfo) o;
                    binding.name.setText(!productInfos.getCameraName().trim().equals("") ? productInfos.getCameraName().trim() : cameraSSId);
                    Log.d(TAG, "CameraName: " + productInfos.getCameraName());
                    tcpConnectionViewModel.getOpsinCameraName();
//                    tcpConnectionViewModel.getWirelessPassword();
                }
            }));

            tcpConnectionViewModel.observeOpsinWirelessPassword().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null) {
                    Log.d(TAG, "CameraPassword: " + o);
                    String result = o.toString().replaceAll("\"", "");
                    binding.password.setText(result);
//                    tcpConnectionViewModel.getWirelessMac();
                }
            }));
            tcpConnectionViewModel.observeOpsinWirelessMac().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null) {
                    binding.macAddress.setText(o.toString());
                    Log.d(TAG, "CameraMAC: " + o);
                }
            }));

            tcpConnectionViewModel.observeOpsinSetFactoryReset().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null) {
                    Log.e(TAG, "observeOpsinSetFactoryReset : " + o);
                    /* after 2sec go to reset done layout*/
                    mHandler.postDelayed(() -> {
                        firmwareUpdateSequence.clear();
                        bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), "");
                        binding.factoryResetLayout.setVisibility(View.GONE);
                        binding.resetInProgressLayout.setVisibility(View.GONE);
                        cameraViewModel.setShowExpandMenu(false);
                        cameraViewModel.setShowAllMenu(true);
                        backToLiveScreen();

                        showOpsinCameraNotResponding(getString(R.string.factory_reset_completed));
                    }, 2000);
                }
            }));

            tcpConnectionViewModel.observeOpsinGetCameraName().observe(lifecycleOwner, new EventObserver<>(object -> {
                if (object != null) {
                    if (object instanceof CommandError) {
                        Log.e("TAG", "observeOsinCameraInfo5: " + ((CommandError) object).getError());
                        binding.setCameraName(cameraSSId);
                        if (binding.name != null) {
                            binding.name.setText(cameraSSId);
                        }
                    } else {
                        String response = object.toString();
                        Log.e(TAG, "observeOpsinGetCameraName: " + response.trim());
                        if (!response.isEmpty()) {
                            if (binding.name != null) {
                                binding.name.setText(!response.trim().equals("") ? response.trim() : cameraSSId);
                            }
                            cameraSSId = cameraSSId.replace("\"", "");
                            binding.setCameraName(!response.trim().equals("") ? response.trim() : cameraSSId.trim());
                            binding.editNameText.setText(!response.trim().equals("") ? response.trim() : cameraSSId.trim());
                            if (viewModel.getUpdateCameraName()) {
                                bleWiFiViewModel.updateCameraName(cameraSSId, response.trim());
                                Log.e(TAG, "name onChanged2: " + response.trim());
                                viewModel.setUpdateCameraName(false);
                            }
                        } else {
                            Log.e(TAG, "onChanged3: " + response + " " + cameraSSId);
                            binding.name.setText(cameraSSId);
                            binding.setCameraName(cameraSSId);
                        }
                    }
                }
            }));

            tcpConnectionViewModel.observeOpsinSetCameraName().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null) {
                    Log.e(TAG, "observeOpsinSetCameraName : " + o);
                    tcpConnectionViewModel.getOpsinCameraName();
                }
            }));

            viewModel.opsinProductVersionDetails.observe(lifecycleOwner, new EventObserver<>(object -> {
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                    if (object != null) {
                        if (object instanceof CommandError) {
                            Log.e(TAG, "opsinProductVersionDetails Error: " + ((CommandError) object).getError());
                        } else {
                            OpsinVersionDetails opsinVersionDetails = (OpsinVersionDetails) object;
                            binding.riscvVersionCurrent.setText(opsinVersionDetails.getRiscv());
                            binding.fpgaVersionCurrent.setText(opsinVersionDetails.getFpga());
                            binding.ispVersion.setText(opsinVersionDetails.getIsp() != null && !opsinVersionDetails.getIsp().isEmpty() ? opsinVersionDetails.getIsp() : getString(R.string.unknown));
                            binding.overlayVersion.setText(opsinVersionDetails.getOverlay() != null && !opsinVersionDetails.getOverlay().isEmpty() ? opsinVersionDetails.getOverlay() : getString(R.string.unknown));
                            binding.recoveryVersion.setText(opsinVersionDetails.getRiscvRecovery() != null && !opsinVersionDetails.getRiscvRecovery().isEmpty() ? opsinVersionDetails.getRiscvRecovery() : getString(R.string.unknown));
                            String sCameraWiFiRtos = opsinVersionDetails.getWifi();
                            try {
                                if (sCameraWiFiRtos != null && !sCameraWiFiRtos.isEmpty()) {
                                    String[] discardWiFiVersion = sCameraWiFiRtos.split("-");
                                    binding.wifiVersionCurrent.setText(discardWiFiVersion[0]);
                                } else {
                                    binding.wifiVersionCurrent.setText(requireContext().getString(R.string.unknown));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                          //  binding.wifiVersionCurrent.setText(opsinVersionDetails.getWifi() != null && !opsinVersionDetails.getWifi().isEmpty() ? opsinVersionDetails.getWifi() : getString(R.string.unknown));
                            binding.bleVersionCurrent.setText(opsinVersionDetails.getBle() != null && !opsinVersionDetails.getBle().isEmpty() ? opsinVersionDetails.getBle() : getString(R.string.unknown));
                            showTopVersionUiText(opsinVersionDetails);
                            Log.e(TAG, "opsinProductVersionDetails " + "/" + opsinVersionDetails.getFpga() + "/" + opsinVersionDetails.getMajor() + "/" + opsinVersionDetails.getRiscv() + "/" + opsinVersionDetails.getWifi());
                            tcpConnectionViewModel.getProductInfo();
                        }
                    }
                } else {
                    Log.d(TAG, "observeOpsinCameraVersionInfo: not observe in info dialog screen");
                }
            }));

                tcpConnectionViewModel.observeOpsinSerialNumber().observe(lifecycleOwner, opsinSerialNumberObserver);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.d(TAG, "observeCameraVersionInfo: NW_Digital");
                break;
        }

        viewModel.opsinSdCardInfo.observe(lifecycleOwner, new EventObserver<>(sdCardInfo -> {
            if (sdCardInfo instanceof CommandError) {
                Log.e(TAG, "opsinSdCardInfo_Observer: " + ((CommandError) sdCardInfo).getError());
            } else {
                SDCardInfo sdCard = (SDCardInfo) sdCardInfo;
                binding.sdCardHeader.setText(sdCard.isMounted() ? requireContext().getString(R.string.mounted) : requireContext().getString(R.string.unmounted));
                if (sdCard.isMounted()) {
                    binding.sdCardDetailsLayout.setVisibility(View.VISIBLE);
                    binding.totalFiles.setText(String.valueOf(sdCard.getFileCount()));
                    binding.usedSize.setText(formatKiloBytes(sdCard.getUsedSize()));
                    binding.availableSize.setText(formatKiloBytes(sdCard.getAvailableSize()));
                    binding.totalSize.setText(formatKiloBytes(sdCard.getTotalSize()));
                    binding.usedPercentage.setText(getSdCardUsePercentage(sdCard.getAvailableSize(), sdCard.getTotalSize()) + "%");
                } else {
                    binding.sdCardDetailsLayout.setVisibility(View.GONE);
                }
            }
        }));

        if (requireActivity().isInMultiWindowMode()) {
            viewModel.onCancel();
            viewModel.onCancelFactoryReset();
        }
        cameraViewModel.isEnableMultiWindowMode.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                viewModel.onCancel();
                viewModel.onCancelFactoryReset();
            }
        }));
    }
    private String removeStrings(String value) {
        if (value == null) return "";
        return value.replaceAll("[^\\x20-\\x7E]", "").trim();
    }

    private final EventObserver<Object> opsinSerialNumberObserver = new EventObserver<>(serialNumber -> {
        if (serialNumber instanceof CommandError) {
            Log.e(TAG, "opsinSerialNumberObserver: " + ((CommandError) serialNumber).getError());
        } else {
            if (serialNumber != null) {
                String allowedCharacters = "^[a-zA-Z0-9!@#$%&*]*$";
                if (!serialNumber.toString().matches(allowedCharacters)) {
                    // If the entered text does not match the pattern, remove the last entered character
                    String filteredText = serialNumber.toString().replaceAll("[^a-zA-Z0-9!@#$%&*]", "");
                    if (filteredText.length() > 0)
                        binding.serialNumber.setText(filteredText.trim());
                    else
                        binding.serialNumber.setText("---");
                }
            }
        }
    });

    private void showFwUpdateDialog(String release_notes) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.FW_UPDATE_AVAILABLE_DIALOG;
            if (currentCameraSsid == NIGHTWAVE) {
                Log.e(TAG, "nightWaveFirmwareUpdateCheck: 8");
                activity.showDialog("", requireContext().getString(R.string.frimware_update_available), release_notes);
            }
        }
    }

    private void showAppUpdateDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.APP_UPDATE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private Manifest readManifest() {
        String tContents = "";
        Manifest manifest = null;
        try {
            InputStream stream = null;
            MainActivity activity = ((MainActivity) getActivity());
            if (activity != null) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        stream = activity.getAssets().open("nightwave/manifest.txt");
                        break;
                    case OPSIN:
                        stream = activity.getAssets().open("opsin/manifest.txt");
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"readManifest : NW_Digital");
                        break;
                }
            }

            int size = stream.available();
            byte[] buffer = new byte[size];
            int read = stream.read(buffer);
            stream.close();
            if (read > 0) {
                tContents = new String(buffer);
                Gson gson = new Gson();
                manifest = gson.fromJson(tContents, Manifest.class);
                Log.e(TAG, "readManifest: " + tContents);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return manifest;
    }

    private void showTopVersionUiText(OpsinVersionDetails opsinVersionDetails) {
        try {
            String fpga = opsinVersionDetails.getFpga();
            String wifi = opsinVersionDetails.getWifi();
            String riscv = opsinVersionDetails.getRiscv();
            String cameraOverlay = opsinVersionDetails.getOverlay();

            int fpgaComparisonResult = -1;
            int wifiComparisonResult = -1;
            int riscvComparisonResult = -1;
            int overlayComparisonResult = -1;
            boolean isEngineeringFirmware = false;

            if (fpga != null) {
                fpgaComparisonResult = compareVersions(fpga, FPGA_BASE_VERSION_TO_DISPLAY_TOP);
            }

            if (wifi != null && containsFourthDigit(wifi)) {
                String removedWifi = removeFourthDigit(wifi);
                wifiComparisonResult = compareVersions(removedWifi, WIFI_BASE_VERSION_TO_DISPLAY_TOP);
                isEngineeringFirmware = true;
            } else if (wifi != null) {
                wifiComparisonResult = compareVersions(wifi, WIFI_BASE_VERSION_TO_DISPLAY_TOP);
            }

            if (riscv != null && containsFourthDigit(riscv)) {
                String removedRiscv = removeFourthDigit(riscv);
                riscvComparisonResult = compareVersions(removedRiscv, RISCV_BASE_VERSION_TO_DISPLAY_TOP);
                isEngineeringFirmware = true;
            } else if (riscv != null) {
                riscvComparisonResult = compareVersions(riscv, RISCV_BASE_VERSION_TO_DISPLAY_TOP);
            }

            if (cameraOverlay != null && containsFourthDigit(cameraOverlay)) {
                String removedCameraOverlay = removeFourthDigit(cameraOverlay);
                overlayComparisonResult = compareVersions(removedCameraOverlay, OVERLAY_BASE_VERSION_TO_DISPLAY_TOP);
                isEngineeringFirmware = true;
            } else if (cameraOverlay != null) {
                overlayComparisonResult = compareVersions(cameraOverlay, OVERLAY_BASE_VERSION_TO_DISPLAY_TOP);
            }

            binding.topVersion.setVisibility(View.VISIBLE);
            binding.topVersionCurrent.setVisibility(View.VISIBLE);
            if (fpgaComparisonResult >= 0 && wifiComparisonResult >= 0 && riscvComparisonResult >= 0 && overlayComparisonResult >= 0) {
                //-1 lesser, 1 greater, 0 equal
                Manifest manifest = readManifest();
                String topVersion = manifest.getVersions().getRelease();
                String manifestRiscv = manifest.getVersions().getRiscv();
                String manifestFpga = manifest.getVersions().getFpga();
                String manifestWifi = manifest.getVersions().getWiFiRtos();
                String manifestOverlay = manifest.getVersions().getOverlay();
                String manifestBle = manifest.getVersions().getwIFI_BLE();

                if (topVersion != null && manifestFpga.equals(fpga) && manifestRiscv.equals(riscv) && manifestWifi.equals(wifi) && manifestOverlay.equals(cameraOverlay)) {
                    if (isEngineeringFirmware) {
                        binding.topVersionCurrent.setText(topVersion);
                    } else {
                        String[] splitTopVersion = topVersion.split("-");
                        binding.topVersionCurrent.setText(splitTopVersion[0]);
                    }
                } else {
                    binding.topVersionCurrent.setText("-");
                }
            } else {
                binding.topVersionCurrent.setText("-");
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public boolean containsFourthDigit(String version) {
        String[] parts = version.split("\\.");

        return parts.length >= 4;
    }

    public String removeFourthDigit(String version) {
        String[] parts = version.split("\\.");

        if (parts.length >= 4) {
            parts[3] = ""; // Remove the fourth part
            return String.join(".", parts);
        } else {
            return version; // No fourth part, return original version
        }
    }

    public int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 < num2) {
                return -1;
            } else if (num1 > num2) {
                return 1;
            }
        }

        if (parts1.length < parts2.length) {
            return -1;
        } else if (parts1.length > parts2.length) {
            return 1;
        } else {
            return 0;
        }
    }

    private int getSdCardUsePercentage(long availableSizeInKilobytes, long totalSizeInKilobytes) {
        return (int) (100 - ((double) (availableSizeInKilobytes) / (double) totalSizeInKilobytes) * 100);
    }

    public String formatKiloBytes(long size) {
        long Bytes = size * 1024; // convert kb to bytes
        return Formatter.formatFileSize(requireContext(), Bytes);
    }

    private static String cleanTextContent(String text) {
        // strips off all non-ASCII characters
        text = text.replaceAll("[^\\x00-\\x7F]", "");

        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // removes non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");

        return text.trim();
    }

    private boolean isValidUsername(String userName) {
        return userName.length() >= 8 && userName.length() <= 32;
    }

    private void backToDeviceInfoLayout() {
        cameraViewModel.setShowExpandMenu(true);
        cameraViewModel.setShowAllMenu(false);
        isInfoButtonPressed = false;
        isSettingButtonPressed = false;
        isChangeCameraNameButtonPressed = false;
        isFactoryRestButtonPressed = false;
        binding.deviceInfoLayout.setVisibility(View.VISIBLE);
        binding.changeNamePasswordLayout.setVisibility(View.GONE);
        binding.factoryResetLayout.setVisibility(View.GONE);

        //clear set error
        binding.editNameText.setError(null);
        hideKeyboard(requireActivity());
    }

    private void hideKeyboard(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            //Find the currently focused view, so we can grab the correct window token from it.
            View view = activity.getCurrentFocus();
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = new View(activity);
            }
            imm.hideSoftInputFromWindow(binding.editNameText.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void backToLiveScreen() {
        isInfoButtonPressed = false;
        isSettingButtonPressed = false;
        isChangeCameraNameButtonPressed = false;
        isFactoryRestButtonPressed = false;
        if (binding.getRoot().getVisibility() == View.VISIBLE) {
            cameraViewModel.setIsShowAlertDialog(false);
            dismiss();
        }
    }

    private void hideKeyboard() {
        try {
            View view = binding.editNameText.getRootView();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

