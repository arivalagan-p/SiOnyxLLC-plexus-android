package com.sionyx.plexus.ui.fwupdateview;

import static android.view.View.VISIBLE;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_FPGA;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_NONE;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_BLE;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_FACTORY;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_RECOVERY;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_RESTART;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_OPSIN_RISCV_OVERLAY;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_RISCV;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_WAIT;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_WIFI_DIALOG;
import static com.dome.librarynightwave.model.repository.TCPRepository.fwMode;
import static com.dome.librarynightwave.utils.Constants.CURRENT_CAMERA_SSID.NIGHTWAVE;
import static com.dome.librarynightwave.utils.Constants.FPGA;
import static com.dome.librarynightwave.utils.Constants.RISCV;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.applyPreset;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.FW_UPDATE_SCREEN;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentAnalogFirmwareUpdateBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.menus.CameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class AnalogFirmwareUpdateFragment extends Fragment {

    private final String TAG = "AnalogFirmwareUpdateFragment";
    private FragmentAnalogFirmwareUpdateBinding binding;
    private TCPConnectionViewModel tcpConnectionViewModel;
    private HomeViewModel homeViewModel;
    private CameraInfoViewModel cameraInfoViewModel;
    private BleWiFiViewModel bleWiFiViewModel;
    private LifecycleOwner lifecycleOwner;

    private final int NEGATIVE_BUTTON_CLICKED = 2;
    private final int POSITIVE_BUTTON_CLICKED = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalogFirmwareUpdateBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        cameraInfoViewModel = new ViewModelProvider(requireActivity()).get(CameraInfoViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
        subscribeUI();
        startFwUpdate(); // start fw progress once fragment created
        return binding.getRoot();
    }

    private void subscribeUI() {
        Log.e(TAG, "Before start resetFirmwareUpdate Progress");
        tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
        tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);

        // Navigate to camera info fragment when click action from fw failed dialog
        cameraInfoViewModel.isFwRetryDialogInLive.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(value -> {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                if (value == POSITIVE_BUTTON_CLICKED) {
                    Log.e(TAG, "resetFirmwareUpdateFailed show available dialog in info screen");
                    tcpConnectionViewModel.setFirmwareUpdateCompleted(false);// changed true to false
                    homeViewModel.setAbnormalFwUpdate(false);
                    homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
                    tcpConnectionViewModel.getRiscBootMode();
                    // positive button show fw available dialog
                    cameraInfoViewModel.onFirmwareUpdate();
                } else if (value == NEGATIVE_BUTTON_CLICKED) {
                    homeViewModel.setHasShowFwDialog(false);
                }
                homeViewModel.getNavController().popBackStack(R.id.cameraFragment, false);
                cameraInfoViewModel.setIsFwAvailableDialogDismissInLive(1);
                tcpConnectionViewModel.observeFwUpdateFailed().removeObservers(lifecycleOwner);
            }
        }));

        // Fw update failed while fw update progress
        tcpConnectionViewModel.observeFwUpdateFailed().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                    Log.e(TAG, "subscribeUI: observeFwUpdateFailed " + object);
                    showFwRetryDialogMessage(getString(R.string.firmware_update_failed_retry));
                }
            }
        }));
    }

    private void showFwRetryDialogMessage(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.FW_RETRY_DIALOG_MESSAGE;
            activity.showDialog("", message, null);
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private void startFwUpdate() {

        if (homeViewModel.getManifest() != null && homeViewModel.getCurrentFwVersion() != null) {
            ArrayList<String> sequence = homeViewModel.getManifest().getSequence();
            int fwUpdateSequenceIndex = homeViewModel.getFwUpdateSequenceIndex();
            Log.e(TAG, "startFwUpdate: " + fwUpdateSequenceIndex + " " + Arrays.toString(sequence.toArray(new String[0])));

            if (fwUpdateSequenceIndex == 0) {
                Log.e(TAG, "startFwUpdate   fwUpdateSequenceIndex : " + fwUpdateSequenceIndex);
                tcpConnectionViewModel.observeFwUpdateProgress().observe(lifecycleOwner, fwProgress);
            }
            if (!sequence.isEmpty()) {
                Log.e(TAG, "startFwUpdate:  sequence " + sequence);

                if (fwUpdateSequenceIndex < sequence.size()) {
                    String sequences = sequence.get(fwUpdateSequenceIndex);
                    homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_STARTED;
                    Log.e(TAG, "startFwUpdate: " + sequences);
                    if (sequences.equalsIgnoreCase(RISCV)) {
                        String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
                        if (cameraRiscv != null) cameraRiscv = cameraRiscv.trim();
                        String appRisc = homeViewModel.getManifest().getVersions().getRiscv().trim();
                        if (cameraRiscv != null && !appRisc.equals(cameraRiscv) && !appRisc.equals("")) {

                            Log.e(TAG, "startFwUpdate: RISC-V inside");
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else {
                            int nextIndex = homeViewModel.getFwUpdateSequenceIndex() + 1;
                            Log.e(TAG, "Skipping update for: " + sequences + " → moving to index " + nextIndex);
                            homeViewModel.setFwUpdateSequenceIndex(nextIndex);
                            startFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(FPGA)) {
                        String camerafpga = homeViewModel.getCurrentFwVersion().getFpga();
                        if (camerafpga != null) camerafpga = camerafpga.trim();
                        String appVersion = homeViewModel.getManifest().getVersions().getFpga().trim();
                        if (camerafpga != null && !appVersion.equals(camerafpga) && !appVersion.equals("")) {

                            Log.e(TAG, "startFwUpdate: fpga inside");
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else {
                            int nextIndex = homeViewModel.getFwUpdateSequenceIndex() + 1;

                            Log.e(TAG, "Skipping update for: " + sequences + " → moving to index " + nextIndex);

                            homeViewModel.setFwUpdateSequenceIndex(nextIndex);
                            startFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(Constants.WIFI_RTOS)) {
                        String cameraWifiRtos = homeViewModel.getCurrentFwVersion().getWiFiRtos().trim();
                        String appRtos = homeViewModel.getManifest().getVersions().getWiFiRtos().trim();
                        if (cameraWifiRtos != null && !appRtos.equals(cameraWifiRtos) && !appRtos.equals("")) {

                            Log.e(TAG, "startFwUpdate: RTOS inside");
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else {
                            int nextIndex = homeViewModel.getFwUpdateSequenceIndex() + 1;

                            Log.e(TAG, "Skipping update for: " + sequences + " → moving to index " + nextIndex);

                            homeViewModel.setFwUpdateSequenceIndex(nextIndex);
                            startFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(Constants.REBOOT_FPGA)) {
                        tcpConnectionViewModel.startFwUpdate(sequences);
                        showFirmwareUpdateScreen();
                    } else if (sequences.contains(Constants.WAIT)) {
                        tcpConnectionViewModel.startFwUpdate(sequences);
                        showFirmwareUpdateScreen();
                    }
                } else {
                    Log.e(TAG, "startFwUpdate: completed");
                    if (tcpConnectionViewModel != null) {
                        Log.e(TAG, "startFwUpdate: completed inside");
                        tcpConnectionViewModel.setFirmwareUpdateCompleted(true);
                        homeViewModel.setAbnormalFwUpdate(false);
                        homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
                        tcpConnectionViewModel.getRiscBootMode();
                        tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
                        tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
                        tcpConnectionViewModel.observeFwUpdateFailed().removeObservers(lifecycleOwner);
                        homeViewModel.setHasShowFwDialog(false);/* for this show fw update dialog with all fw_version in ui. after complete fw update ui hide and show home screen */
                        tcpConnectionViewModel.setSSIDSelectedManually(false);


                        if (tcpConnectionViewModel.isFirmwareUpdateCompleted()) {
                            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_COMPLETED;

                            new Handler().postDelayed(() -> {
                                Log.e(TAG, "isHasShowFwDialog: " + homeViewModel.isHasShowFwDialog());//
                                homeViewModel.setFwUpdateSequenceIndex(0);
                                homeViewModel.setBootModeCheckCount(0);
                                navigateToHome();
                            }, 1500);

                            homeViewModel.setAbnormalFwUpdate(false);
                            tcpConnectionViewModel.setFirmwareUpdateChecked(false);
                        }
                    }
                }
            }
        }
    }

    private void navigateToHome() {
        if (tcpConnectionViewModel != null) {
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                new Handler().postDelayed(() -> {
                    tcpConnectionViewModel.disconnectSocket();
                    tcpConnectionViewModel.isSocketConnected().removeObservers(lifecycleOwner);
                    Log.e(TAG, "Navigate to HomeScreen: fw_update completed");
                }, 1500);
                tcpConnectionViewModel.setFirmwareUpdateCompleted(false);
            }
        }
        if (homeViewModel != null) {
            // reset all the variables once FW completed
            bleWiFiViewModel.setShouldAutoConnectionEnabled(false);
            homeViewModel.getNoticeDialogFragments().clear();
            homeViewModel.getManifest().getSequence().clear();
            homeViewModel.getOpsinAbnormalSequence().clear();
            homeViewModel.getAbnormalSequence().clear();
            homeViewModel.setFwCompletedCount(0);
            homeViewModel.setAbnormalFwUpdateSequenceIndex(0);
            homeViewModel.setOpsinFwUpdateSequenceIndex(0);
            homeViewModel.setBootModeCheckCount(0);
            homeViewModel.setFwUpdateSequenceIndex(0);
            homeViewModel.setBootModeCheckCount(0);
            homeViewModel.setAbnormalFwUpdateSequenceIndex(0);
            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_NONE;
            HomeViewModel.hasShowProgressBar = false;
            firmwareUpdateSequence.clear();
            fwMode = MODE_NONE;
            applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
            homeViewModel.setSelectedCamera("");
            homeViewModel.hasShowProgressBar(false);

            HomeViewModel.screenType = HomeViewModel.ScreenType.HOME;

            if (homeViewModel.hasShowFwDialog)
                homeViewModel.setHasShowFwDialog(false);

            if (homeViewModel.hasShowRecoverModeDialog())
                homeViewModel.setHasShowRecoverModeDialog(false);

            fwMode = MODE_NONE;

            // to avoid relaunch the live when FW update completed
            homeViewModel.getNavController().popBackStack(R.id.analogFirmwareUpdateFragment, true);
            homeViewModel.getNavController().popBackStack(R.id.cameraInfoTabDialogFragment, true);
            homeViewModel.getNavController().popBackStack(R.id.cameraFragment, true);
            homeViewModel.getNavController().popBackStack(R.id.cameraSplashFragment, true);
            homeViewModel.getNavController().navigate(R.id.homeFragment);

            homeViewModel.hasStopFWUIProgress(true);
        }
    }

    private void showFirmwareUpdateScreen() {
        if (fwMode == MODE_NONE) {
            binding.progressBar.setProgress(0);
            binding.progressText.setText(getString(R.string.intial_percentage));
        }
        CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
        HomeViewModel.screenType = FW_UPDATE_SCREEN;
        binding.firmwareUpdateLayout.setVisibility(VISIBLE);
        binding.firmwareCameraUpload.setVisibility(VISIBLE);
        binding.firmwareCameraUploadDone.setVisibility(View.GONE);
    }

    public Observer<Object> fwProgress = object -> {
        if (object instanceof CommandError) {
            showMessage("READ TIME OUT");
            changeProgressCenterCameraIcon();
            Log.e(TAG, "onFirmwareUpgrade: " + ((CommandError) object).getError());
        } else if (object instanceof Integer) {
            int value = (int) object;
            changeProgressCenterCameraIcon();
            if (value >= 0) {
                String percentage = String.format(getString(R.string.percentage), value);
                binding.progressText.setText(percentage);
                binding.progressBar.setProgress(value);
                changeProgressCenterCameraIcon();
                if (value < 100) {
                    if (fwMode != MODE_NONE && binding.firmwareUpdateLayout.getVisibility() == View.GONE || binding.firmwareUpdateLayout.getVisibility() == View.INVISIBLE) {
                        showFirmwareUpdateScreen();
                    }
                    if (value <= 50) {
                        if (fwMode == MODE_WAIT) {
                            binding.loadingText.setText(getString(R.string.please_wait));
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                            binding.estimationTime.setText("");
                        } else if (fwMode == MODE_OPSIN_RESTART) {
                            binding.loadingText.setText(getString(R.string.please_wait_opsin_restart));
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                            binding.estimationTime.setText("");
                        } else if (fwMode == MODE_OPSIN_FACTORY) {
                            binding.loadingText.setText(getString(R.string.please_wait_opsin_reset));
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                            binding.estimationTime.setText("");
                        } else {
                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_RISCV) {
                                binding.loadingText.setText(getString(R.string.up_loading_riscv));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_FPGA) {
                                binding.loadingText.setText(getString(R.string.up_loading_fpga));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "15"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_OPSIN_RISCV_OVERLAY) {
                                binding.loadingText.setText(getString(R.string.up_loading_riscv_overlay));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "6"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_WIFI_DIALOG) {
                                binding.loadingText.setText(getString(R.string.up_loading_wifi));
                                if (Constants.isOpsinContainsOldWiFiFirmware()) {
                                    binding.estimationTime.setText(getString(R.string.estimated_time, "4"));
                                } else {
                                    binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                                }
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_OPSIN_BLE) {
                                binding.loadingText.setText(getString(R.string.up_loading_ble));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_OPSIN_RECOVERY) {
                                binding.loadingText.setText(getString(R.string.up_loading_recovery));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else {
                                binding.estimationTime.setText("");
                                binding.loadingText.setText(getString(R.string.up_loading));
                            }

                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_upload);
                        }
                        binding.firmwareCameraUploadDone.setVisibility(View.GONE);
                    } else {
                        if (fwMode == MODE_WAIT) {
                            binding.loadingText.setText(getString(R.string.please_wait));
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                            binding.estimationTime.setText("");
                        } else if (fwMode == MODE_OPSIN_RESTART) {
                            binding.loadingText.setText(getString(R.string.please_wait_opsin_restart));
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                            binding.estimationTime.setText("");
                        } else if (fwMode == MODE_OPSIN_FACTORY) {
                            binding.loadingText.setText(getString(R.string.please_wait_opsin_reset));
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                            binding.estimationTime.setText("");
                        } else {
                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_RISCV) {
                                binding.loadingText.setText(getString(R.string.updating_riscv));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_FPGA) {
                                binding.loadingText.setText(getString(R.string.updating_fpga));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "15"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_OPSIN_RISCV_OVERLAY) {
                                binding.loadingText.setText(getString(R.string.updating_riscv_overlay));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "6"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_WIFI_DIALOG) {
                                binding.loadingText.setText(getString(R.string.updating_wifi));
                                if (Constants.isOpsinContainsOldWiFiFirmware()) {
                                    binding.estimationTime.setText(getString(R.string.estimated_time, "4"));
                                } else {
                                    binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                                }
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_OPSIN_BLE) {
                                binding.loadingText.setText(getString(R.string.updating_ble));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && fwMode == MODE_OPSIN_RECOVERY) {
                                binding.loadingText.setText(getString(R.string.updating_recovery));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else {
                                binding.estimationTime.setText("");
                                binding.loadingText.setText(getString(R.string.updating));
                            }
                            binding.firmwareCameraUpload.setVisibility(VISIBLE);
                            changeProgressCenterCameraIcon();
                            binding.firmwareCameraUpload.setImageResource(R.drawable.ic_firmware_settings);
                        }
                        binding.firmwareCameraUploadDone.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "FirmwareUpdate_Completed: " + HomeViewModel.screenType);

                    if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                        binding.loadingText.setText(getString(R.string.completed));
                        binding.firmwareCameraUpload.setVisibility(View.GONE);
                        binding.firmwareCameraUploadDone.setVisibility(VISIBLE);
                        changeProgressCenterCameraIcon();

                        if (!homeViewModel.isAbnormalFwUpdate()) {
                            int nextIndex = homeViewModel.getFwUpdateSequenceIndex() + 1;
                            homeViewModel.setFwUpdateSequenceIndex(nextIndex);
                            startFwUpdate();
                        } else {
                            int nextIndex = homeViewModel.getAbnormalFwUpdateSequenceIndex() + 1;
                            homeViewModel.setAbnormalFwUpdateSequenceIndex(nextIndex);
                            startAbnormalBehaviourFwUpdateSequence();
                        }
                    } else {
                        Log.e(TAG, ":NO NEED TO HANDLE ");
                    }
                }
            }
        }
    };

    private void startAbnormalBehaviourFwUpdateSequence() {
        if (homeViewModel.getManifest() != null && homeViewModel.getCurrentFwVersion() != null) {
            int abnormalFwUpdateSequenceIndex = homeViewModel.getAbnormalFwUpdateSequenceIndex();
            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_STARTED;
            Log.e(TAG, "fwupdateProgress startAbnormalBehaviourFwUpdateSequence: " + HomeViewModel.screenType.name());

            if (abnormalFwUpdateSequenceIndex == 0)
                tcpConnectionViewModel.observeFwUpdateProgress().observe(lifecycleOwner, fwProgress);

            changeProgressCenterCameraIcon();
            if (Objects.requireNonNull(currentCameraSsid) == NIGHTWAVE) {
                ArrayList<String> abnormalSequence = homeViewModel.getAbnormalSequence();
                if (!abnormalSequence.isEmpty()) {
                    if (abnormalFwUpdateSequenceIndex < abnormalSequence.size()) {
                        String sequences = abnormalSequence.get(abnormalFwUpdateSequenceIndex);
                        if (sequences.equalsIgnoreCase(RISCV)) {
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else if (sequences.equalsIgnoreCase(FPGA)) {
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else if (sequences.equalsIgnoreCase(Constants.REBOOT_FPGA)) {
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else if (sequences.contains(Constants.WAIT)) {
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        }
                    } else {
                        if (tcpConnectionViewModel != null) {
                            tcpConnectionViewModel.setFirmwareUpdateCompleted(true);
                            homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
//                            homeViewModel.setBootModeCheckCount(homeViewModel.getBootModeCheckCount() + 1);
                            tcpConnectionViewModel.getRiscBootMode();
                            tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
                            tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
                            homeViewModel.setHasShowFwDialog(false);
                            tcpConnectionViewModel.setSSIDSelectedManually(false);
                        }
                    }
                }
            }
        }
    }

    private void changeProgressCenterCameraIcon() {
        binding.firmwareCamera.setImageResource(R.drawable.ic_firmware_upload_camera);
    }
    private void showMessage(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null && !activity.isFinishing())
            Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show();
    }
}
