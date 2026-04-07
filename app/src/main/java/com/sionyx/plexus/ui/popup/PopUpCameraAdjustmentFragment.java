package com.sionyx.plexus.ui.popup;

import static com.dome.librarynightwave.model.repository.TCPRepository.isOpsinLiveStreamingStarted;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.SCCPConstants.BRIGHTNESS_STEP0;
import static com.dome.librarynightwave.utils.SCCPConstants.BRIGHTNESS_STEP1;
import static com.dome.librarynightwave.utils.SCCPConstants.BRIGHTNESS_STEP2;
import static com.dome.librarynightwave.utils.SCCPConstants.BRIGHTNESS_STEP3;
import static com.dome.librarynightwave.utils.SCCPConstants.BRIGHTNESS_STEP4;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP0;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP1;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP2;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP3;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP4;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP5;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP6;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP7;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP8;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.mSocketState;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentPopUpCameraAdjustmentBinding;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraSettingsInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;

public class PopUpCameraAdjustmentFragment extends Fragment {
    private static final String TAG = "PopUpCameraAdjustmentFragment";
    private FragmentPopUpCameraAdjustmentBinding binding;
    private TCPConnectionViewModel tcpConnectionViewModel;
    private CameraSettingsInfoViewModel settingsInfoViewModel;
    private LifecycleOwner lifecycleOwner;
    private boolean isViewShown = false;
    private HomeViewModel homeViewModel;
    private final Handler mHandler = new Handler();
    private int cameraXValue;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPopUpCameraAdjustmentBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        settingsInfoViewModel = new ViewModelProvider(requireActivity()).get(CameraSettingsInfoViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (screenType == POP_UP_SETTINGS_SCREEN) {
            subscribeUI();
        }
        homeViewModel.closePopupCameraAdjusmentFragment.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                removeObservers();
            }
        }));
    }

    private void getEv() {
        if (tcpConnectionViewModel != null) {
            try {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        if (mSocketState == Constants.STATE_CONNECTED) {
                            tcpConnectionViewModel.getCameraExposure();
                        }
                        break;
                    case OPSIN:
                        if (mSocketState == Constants.STATE_CONNECTED) {
                            new Handler().postDelayed(() -> tcpConnectionViewModel.getEv(), 500);
                            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                                new Handler().postDelayed(() -> tcpConnectionViewModel.getOpsinBrightness(), 1000);
                            }
                        }
                        tcpConnectionViewModel.observeOpsinGetEv().observe(lifecycleOwner, getEv);
                        tcpConnectionViewModel.observeOpsinGetBrightness().observe(lifecycleOwner, getOpsinBrightness);
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"getEV : NW_Digital");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mHandler.postDelayed(this::getEv, 500);
        }
    }

    private void removeObservers() {
        tcpConnectionViewModel.observeOpsinGetEv().removeObserver(getEv);
        tcpConnectionViewModel.observeOpsinGetEv().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinSetEv().removeObserver(setEv);
        tcpConnectionViewModel.observeOpsinSetEv().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeOpsinGetBrightness().removeObserver(getOpsinBrightness);
        tcpConnectionViewModel.observeOpsinGetBrightness().removeObservers(lifecycleOwner);
    }

    private void subscribeUI() {
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) && CameraViewModel.hasNewFirmware()) {
            binding.nightwaveSettingsInfoLayout.setVisibility(View.VISIBLE);
            binding.opsinSettingsInfoLayout.setVisibility(View.GONE);

            binding.nightwaveBrightnessSeekbar.setMin(0);
            binding.nightwaveBrightnessSeekbar.setMax(6);
            binding.nightwaveBrightnessSeekbar.setProgress(0);
            if (mSocketState == Constants.STATE_CONNECTED) {
                binding.nightwaveBrightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seekBar.setProgress(progress);
                        Log.e(TAG, "onProgressChanged:" + progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.e(TAG, "onProgressChanged Stopped:" + seekBar.getProgress());
                        // if (!settingsInfoViewModel.isEvSeekbarSelected()) {
                        tcpConnectionViewModel.setCameraExposure(seekBar.getProgress());
                        settingsInfoViewModel.setEvSeekbarSelected(true);
                        //}
                    }
                });

                tcpConnectionViewModel.observeCameraExposure().observe(lifecycleOwner, object -> {
                    if (object instanceof CommandError) {
                        Log.e(TAG, "observeCameraExposure: " + ((CommandError) object).getError());
                    } else {
                        Log.e(TAG, "observeCameraExposure : " + object);
                        binding.nightwaveBrightnessSeekbar.setProgress((int) object);
                    }
                    settingsInfoViewModel.setEvSeekbarSelected(false);
                });
            }

        } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
            isOpsinLiveStreamingStarted = true;// Just avoiding start live streaming during this fragment visible
            applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
            tcpConnectionViewModel.clearPeriodicRequestList();
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);

            try {
                String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
                String[] aCameraRiscVersion = cameraRiscv.split("\\.");
                cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }

            binding.nightwaveSettingsInfoLayout.setVisibility(View.GONE);
            binding.opsinSettingsInfoLayout.setVisibility(View.VISIBLE);
            binding.opsinEvSeekbar.setMin(0);
            binding.opsinEvSeekbar.setMax(8);
            binding.opsinEvSeekbar.setProgress(0);

            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                binding.opsinBrightnessSeekbar.setVisibility(View.VISIBLE);
                binding.opsinBrightnessTv1.setVisibility(View.VISIBLE);
                binding.opsinBrightnessTv3.setVisibility(View.VISIBLE);
                binding.opsinBrightnessTv5.setVisibility(View.VISIBLE);
                binding.opsinBrightnessIcon.setVisibility(View.VISIBLE);
            } else {
                binding.opsinBrightnessSeekbar.setVisibility(View.GONE);
                binding.opsinBrightnessTv1.setVisibility(View.GONE);
                binding.opsinBrightnessTv3.setVisibility(View.GONE);
                binding.opsinBrightnessTv5.setVisibility(View.GONE);
                binding.opsinBrightnessIcon.setVisibility(View.GONE);
            }

            binding.opsinBrightnessSeekbar.setMin(0);
            binding.opsinBrightnessSeekbar.setMax(4);
            binding.opsinBrightnessSeekbar.setProgress(0);
            if (mSocketState == Constants.STATE_CONNECTED) {
                binding.opsinEvSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seekBar.setProgress(progress);
                        disableSeekbar();
                        Log.e(TAG, "onProgressChanged:" + progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        switch (seekBar.getProgress()) {
                            case 0:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP0});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 1:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP1});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 2:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP2});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 3:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP3});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 4:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP4});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 5:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP5});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 6:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP6});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 7:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP7});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            case 8:
                                if (settingsInfoViewModel.isEvSeekbarSelected()) {
                                    tcpConnectionViewModel.setEv(new byte[]{EV_STEP8});
                                    settingsInfoViewModel.setEvSeekbarSelected(true);
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + seekBar.getProgress());
                        }
                        disableSeekbar();
                    }
                });

                binding.opsinBrightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seekBar.setProgress(progress);
                        disableBrightnessSeekbar();
                        Log.e(TAG, "opsinBrightnessSeekbarChanged:" + progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        disableBrightnessSeekbar();
                    }
                });

                tcpConnectionViewModel.observeOpsinGetEv().observe(lifecycleOwner, getEv);

                tcpConnectionViewModel.observeOpsinSetEv().observe(lifecycleOwner, setEv);
                tcpConnectionViewModel.observeOpsinGetBrightness().observe(lifecycleOwner, getOpsinBrightness);

            } else {
//                Toast.makeText(requireActivity(), R.string.socket_disconnected_messgae, Toast.LENGTH_LONG).show();
            }
        } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
            Log.e(TAG,"subscribeUI : NW_Digital");
        }
    }

    EventObserver<Object> setEv = new EventObserver<>(o -> {
        if (o != null) {
            Log.e(TAG, "observeOpsinSetEv : " + o);
            settingsInfoViewModel.setEvSeekbarSelected(false);
            tcpConnectionViewModel.getEv();
        }
    });
    EventObserver<Object> getEv = new EventObserver<>(object -> {
        if (object != null) {
            Log.e(TAG, "observeOpsinGetEv : " + (byte) object);
            switch ((byte) object) {
                case EV_STEP0:
                    binding.opsinEvSeekbar.setProgress(0);
                    break;
                case EV_STEP1:
                    binding.opsinEvSeekbar.setProgress(1);
                    break;
                case EV_STEP2:
                    binding.opsinEvSeekbar.setProgress(2);
                    break;
                case EV_STEP3:
                    binding.opsinEvSeekbar.setProgress(3);
                    break;
                case EV_STEP4:
                    binding.opsinEvSeekbar.setProgress(4);
                    break;
                case EV_STEP5:
                    binding.opsinEvSeekbar.setProgress(5);
                    break;
                case EV_STEP6:
                    binding.opsinEvSeekbar.setProgress(6);
                    break;
                case EV_STEP7:
                    binding.opsinEvSeekbar.setProgress(7);
                    break;
                case EV_STEP8:
                    binding.opsinEvSeekbar.setProgress(8);
                    break;

                default:
                    Log.e(TAG, "onChanged: UnExpected Value");
            }
            settingsInfoViewModel.setEvSeekbarSelected(false);
        }
    });

    EventObserver<Object> getOpsinBrightness = new EventObserver<>(object -> {
        if (object != null) {
            Log.e(TAG, "getOpsinBrightness : " + (byte) object);
            switch ((byte) object) {
                case BRIGHTNESS_STEP0:
                    binding.opsinBrightnessSeekbar.setProgress(0);
                    break;
                case BRIGHTNESS_STEP1:
                    binding.opsinBrightnessSeekbar.setProgress(1);
                    break;
                case BRIGHTNESS_STEP2:
                    binding.opsinBrightnessSeekbar.setProgress(2);
                    break;
                case BRIGHTNESS_STEP3:
                    binding.opsinBrightnessSeekbar.setProgress(3);
                    break;
                case BRIGHTNESS_STEP4:
                    binding.opsinBrightnessSeekbar.setProgress(4);
                    break;
                default:
                    Log.e(TAG, "onChanged: UnExpected Value");
                    // for this only v23
                    if (((byte) object) >= 0 && ((byte) object) < 10) {
                        binding.opsinBrightnessSeekbar.setProgress(0);
                        break;
                    } else if (((byte) object) > 10 && ((byte) object) < 30) {
                        binding.opsinBrightnessSeekbar.setProgress(1);
                        break;
                    } else if (((byte) object) > 30 && ((byte) object) < 60) {
                        binding.opsinBrightnessSeekbar.setProgress(2);
                        break;
                    } else if (((byte) object) > 60 && ((byte) object) < 80) {
                        binding.opsinBrightnessSeekbar.setProgress(3);
                        break;
                    } else if (((byte) object) > 80 && ((byte) object) < 110) {
                        binding.opsinBrightnessSeekbar.setProgress(4);
                        break;
                    }
                    settingsInfoViewModel.setBrightnessSeekbarSelected(false);
            }
        }
    });

    private void disableSeekbar() {
        Drawable customTrackDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.custom_seekbar);
        Drawable customThumbDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.seek_bar_thumb);
        if (cameraXValue < OPSIN_STREAMING_SUPPORTS_FROM) {
            binding.opsinEvSeekbar.setEnabled(true);
            //  binding.opsinBrightnessSeekbar.setEnabled(true);
        } else {
            //EV
            binding.opsinEvSeekbar.setEnabled(false);
            binding.opsinEvSeekbar.setProgressDrawable(customTrackDrawable);
            binding.opsinEvSeekbar.setThumb(customThumbDrawable);
            binding.opsinEvSeekbar.setBackground(null);
        }
    }

    private void disableBrightnessSeekbar() {
        Drawable customTrackDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.custom_seekbar);
        Drawable customThumbDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.seek_bar_thumb);
        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
            // Brightness
            binding.opsinBrightnessSeekbar.setEnabled(false);
            binding.opsinBrightnessSeekbar.setProgressDrawable(customTrackDrawable);
            binding.opsinBrightnessSeekbar.setThumb(customThumbDrawable);
            binding.opsinBrightnessSeekbar.setBackground(null);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        Log.d(TAG, "setMenuVisibility: " + menuVisible);
        if (menuVisible) {
            isViewShown = true;
            getEv();
        } else {
            isViewShown = false;
        }
        super.setMenuVisibility(menuVisible);
    }

}