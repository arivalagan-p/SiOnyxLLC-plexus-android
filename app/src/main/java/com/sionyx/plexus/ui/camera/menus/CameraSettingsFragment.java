package com.sionyx.plexus.ui.camera.menus;

import static android.view.View.VISIBLE;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_FLIP_VIDEO;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_INVERT_VIDEO;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_IRCUT;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_LED;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_CLOCK_MODE;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_FPS;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_GPS;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_META_DATA;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_MIC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_MONOCHROMATIC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_NOISE_REDUCTION;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_NUC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_ROI;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_MANE_NW_FLIP_VIDEO;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_NW_INVERT_VIDEO;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_NW_IRCUT;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_NW_LED;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_CLOCK_MODE;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_FPS;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_GPS;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_META_DATA;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_MIC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_MONOCHROMATIC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_NOISE_REDUCTION;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_NUC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_NAME_OPSIN_ROI;
import static com.sionyx.plexus.ui.dialog.CameraSettingsTabLayoutDialog.clickedTimeStamp;
import static com.sionyx.plexus.ui.dialog.CameraSettingsTabLayoutDialog.mStrCurrentTab;
import static com.sionyx.plexus.ui.dialog.CameraSettingsTabLayoutDialog.previousPos;
import static com.sionyx.plexus.utils.Constants.OPSIN_RISCV_RTC_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OPSIN_WIFI_RTC_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.convert24to12Hrs;
import static com.sionyx.plexus.utils.Constants.isTimeFormat24Hr;
import static com.sionyx.plexus.utils.Constants.mSocketState;
import static com.sionyx.plexus.utils.Constants.simple12HourTimeFormat;
import static com.sionyx.plexus.utils.Constants.simple24HourTimeFormat;
import static com.sionyx.plexus.utils.Constants.simpleDateFormat;
import static com.sionyx.plexus.utils.Constants.simpleTimeFormat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.persistence.savesettings.SaveSettings;
import com.dome.librarynightwave.model.repository.opsinmodel.DateTime;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.utils.SCCPConstants;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.android.material.tabs.TabLayout;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCameraSettingsBinding;
import com.sionyx.plexus.databinding.TimeZoneLayoutBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.dialog.TimeZoneViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.circulardotprogressbar.DotCircleProgressIndicator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CameraSettingsFragment extends Fragment {
    private final String TAG = "CameraSettingsFragment";
    private FragmentCameraSettingsBinding binding;
    public TCPConnectionViewModel tcpConnectionViewModel;
    private LifecycleOwner lifecycleOwner;
    private CameraSettingsViewModel cameraSettingsViewModel;
    private CameraViewModel cameraViewModel;
    private int mCounter = 0;
    private int opsinTouchCounter = 0;
    private final Handler mHandler = new Handler();
    private final Handler opsinTouchHandler = new Handler();
    private HomeViewModel homeViewModel;
    private TimeZoneViewModel timeZoneViewModel;

    ProgressBar wiFiDisconnectProgressbar;
    TextView progressTextView;
    AlertDialog pleaseWaitAlertDialog;
    private int cameraXValue;

    private CameraPresetsViewModel cameraPresetsViewModel;
    private TimeZoneLayoutBinding timeZoneLayoutBinding;
    private Calendar calendar;
    private Calendar selectedDateTime;
    private Calendar setAutomaticModeDateTime;
    Date objDate;

    public enum RTC_MODE {
        MANUAL,
        AUTOMATIC,
    }

    public RTC_MODE rtcMode = RTC_MODE.MANUAL;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        cameraSettingsViewModel = new ViewModelProvider(requireActivity()).get(CameraSettingsViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        cameraPresetsViewModel = new ViewModelProvider(requireActivity()).get(CameraPresetsViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        timeZoneViewModel = new ViewModelProvider(requireActivity()).get(TimeZoneViewModel.class);
        calendar = Calendar.getInstance();
        selectedDateTime = Calendar.getInstance();

        /*Nightwave*/
        cameraSettingsViewModel.setInvertVideoTabPosition(0);
        cameraSettingsViewModel.setFlipVideoTabPosition(0);
        cameraSettingsViewModel.setIrCutTabPosition(0);
        cameraSettingsViewModel.setLedTabPosition(0);
        //cameraSettingsViewModel.setVideoModePosition(0);
        /*Opsin*/
        cameraSettingsViewModel.setNucTabPosition(0);
        cameraSettingsViewModel.setMicTabPosition(0);
        cameraSettingsViewModel.setFpsTabPosition(0);
        cameraSettingsViewModel.setJpegCompressionTabPosition(0);
        cameraSettingsViewModel.setTimeFormatTabPosition(0);
        cameraSettingsViewModel.setUtcTabPosition(0);
        cameraSettingsViewModel.setGpsTabPosition(0);
        cameraSettingsViewModel.setMonochromeTabPosition(0);
        cameraSettingsViewModel.setNoiseTabPosition(0);
        cameraSettingsViewModel.setRoiTabPosition(0);
        cameraSettingsViewModel.setSourceTabPosition(0);
        cameraSettingsViewModel.setClockFormatTabPosition(0);
        cameraSettingsViewModel.setMetadataTabPosition(0);
        cameraSettingsViewModel.setDateTimeModePosition(0);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCameraSettingsBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        timeZoneLayoutBinding = binding.timeZoneLayout;
        timeZoneLayoutBinding.setViewModel(cameraSettingsViewModel);
        lifecycleOwner = this;
        tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
        binding.customProgressBar.setIndicator(new DotCircleProgressIndicator());
        binding.videoModeResponseProgressBar.setIndicator(new DotCircleProgressIndicator());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG,"Camera_Name : " + currentCameraSsid);
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                initNightWaveUI();
                if (!CameraViewModel.hasNewFirmware()) { // for this condition old firmware only
                    tcpConnectionViewModel.isSocketConnected().observe(lifecycleOwner, mState -> {
                        if (mState == Constants.STATE_CONNECTED) {
                            tcpConnectionViewModel.getInvertImage();
                            tcpConnectionViewModel.getFlipImage();
                            tcpConnectionViewModel.getIRCut();
                            tcpConnectionViewModel.getLedEnableState();
                        } else {
                            showOpsinCameraSocketClosedDialog(requireContext().getString(R.string.socket_disconnected_messgae));
                        }
                    });
                }
                break;
            case OPSIN:
                initOpsinUI();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"onViewCreated : NW_Digital");
                break;
        }

        cameraViewModel.observeIsDismissCustomDialog().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.i(TAG, "initNightWaveUI: isDismissCustomDialog >> " + aBoolean);
            if (aBoolean) {
                tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
                tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
            }
        }));

        cameraSettingsViewModel.hasObserveSaveSettings.observe(lifecycleOwner, new EventObserver<>(presetName -> {
            if (!presetName.equals("")) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        prepareNightwaveSettingsToSave(presetName);
                        break;
                    case OPSIN:
                        prepareOpsinSettingsToSave(presetName);
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"ObserveSettings : NW_Digital");
                        break;
                }
                Log.d(TAG, "hasObserveSaveSettings: " + presetName);
            }
        }));

        // for this only observe success or failed insert camera settings on db
        cameraPresetsViewModel.hasSavedSettingsSuccessfully().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Toast.makeText(requireContext(), getString(R.string.settings_saved_successfully), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.settings_saved_failed), Toast.LENGTH_LONG).show();
            }
        }));

        cameraSettingsViewModel.isShowVideoModeResponseProgressbar.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            new Handler().post(() -> {
                if (aBoolean) {
                    binding.videoModeResponseProgressBar.smoothToShow();
                } else {
                    binding.videoModeResponseProgressBar.smoothToHide();
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                        hasEnableClockFormatTab(true);
                        hasEnableDateTimeModeTab(true);
                    }
                }
            });
        }));

        cameraViewModel.observeHasShowProgressbar().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.setIsLoading(true);
                binding.customProgressBar.smoothToShow();
            } else {
                binding.setIsLoading(false);
                binding.customProgressBar.smoothToHide();
            }
        }));
    }

    private void showOpsinCameraSocketClosedDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_NOT_RESPONSE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private TabLayout.Tab tabUtc;

    @SuppressLint("ClickableViewAccessibility")
    private void initOpsinUI() {
        //  String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
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

        binding.nightWaveLayout.setVisibility(View.GONE);
        binding.opsinUiLayout.setVisibility(VISIBLE);
        binding.opsinTabUtc.setVisibility(View.GONE); //temporary visibility
        binding.opsinUtcIcon.setVisibility(View.GONE); //temporary visibility

        binding.opsinClockIcon.setVisibility(View.GONE);
        binding.opsinTabClock.setVisibility(View.GONE);
        //new settings
        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
            binding.opsinTabMonochrome.setVisibility(VISIBLE);
            binding.opsinMonochromeIcon.setVisibility(VISIBLE);
            binding.opsinTabNoise.setVisibility(VISIBLE);
            binding.opsinNoiseIcon.setVisibility(VISIBLE);
            binding.opsinTabRoi.setVisibility(VISIBLE);
            binding.opsinRoiModeIcon.setVisibility(VISIBLE);
            binding.opsinTabSource.setVisibility(VISIBLE);
            binding.opsinSourceModeIcon.setVisibility(VISIBLE);
            binding.opsinTabMetadata.setVisibility(VISIBLE);
            binding.opsinMetadataIcon.setVisibility(VISIBLE);
            timeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.GONE);
            timeZoneLayoutBinding.opsinTabClockFormat.setVisibility(VISIBLE);
        } else {
            binding.opsinTabMonochrome.setVisibility(View.GONE);
            binding.opsinMonochromeIcon.setVisibility(View.GONE);
            binding.opsinTabNoise.setVisibility(View.GONE);
            binding.opsinNoiseIcon.setVisibility(View.GONE);
            binding.opsinTabRoi.setVisibility(View.GONE);
            binding.opsinRoiModeIcon.setVisibility(View.GONE);
            binding.opsinTabSource.setVisibility(View.GONE);
            binding.opsinSourceModeIcon.setVisibility(View.GONE);
            binding.opsinTabMetadata.setVisibility(View.GONE);
            binding.opsinMetadataIcon.setVisibility(View.GONE);
            timeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.GONE);
            timeZoneLayoutBinding.opsinTabClockFormat.setVisibility(View.GONE);
        }
        // for this RTC only
        String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
        String cameraWifi = homeViewModel.getCurrentFwVersion().getWiFiRtos();
        int riscvComparisonResult = -1;
        int wifiComparisonResult = -1;
        if (containsFourthDigit(cameraRiscv)) {
            String removedRiscv = removeFourthDigit(cameraRiscv);
            riscvComparisonResult = compareVersions(removedRiscv, OPSIN_RISCV_RTC_SUPPORTS_FROM);
        } else {
            riscvComparisonResult = compareVersions(cameraRiscv, OPSIN_RISCV_RTC_SUPPORTS_FROM);
        }

        if (cameraWifi == null) {
            cameraWifi = "2.1.3";
        }
        if (containsFourthDigit(cameraWifi)) {
            String removedWifi = removeFourthDigit(cameraWifi);
            wifiComparisonResult = compareVersions(removedWifi, OPSIN_WIFI_RTC_SUPPORTS_FROM);
        } else {
            wifiComparisonResult = compareVersions(cameraWifi, OPSIN_WIFI_RTC_SUPPORTS_FROM);
        }

        if (riscvComparisonResult >= 0 && wifiComparisonResult >= 0) {
            timeZoneLayoutBinding.dateTimeModeLayout.setVisibility(VISIBLE);
        } else {
            timeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.GONE);
        }


        //new settings
        binding.opsinTabMonochrome.addTab(binding.opsinTabMonochrome.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabMonochrome.addTab(binding.opsinTabMonochrome.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabMonochrome.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabMonochrome.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabNoise.addTab(binding.opsinTabNoise.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabNoise.addTab(binding.opsinTabNoise.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabNoise.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabNoise.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabRoi.addTab(binding.opsinTabRoi.newTab().setText("30"));
        binding.opsinTabRoi.addTab(binding.opsinTabRoi.newTab().setText(requireContext().getString(R.string._50)));
        binding.opsinTabRoi.addTab(binding.opsinTabRoi.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabRoi.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabRoi.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabSource.addTab(binding.opsinTabSource.newTab().setText(requireContext().getString(R.string.gps)));
        binding.opsinTabSource.addTab(binding.opsinTabSource.newTab().setText(requireContext().getString(R.string.none)));
        binding.opsinTabSource.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabSource.setTabGravity(TabLayout.GRAVITY_FILL);

        timeZoneLayoutBinding.opsinTabClockFormat.addTab(timeZoneLayoutBinding.opsinTabClockFormat.newTab().setText(requireContext().getString(R.string.hour_12)));
        timeZoneLayoutBinding.opsinTabClockFormat.addTab(timeZoneLayoutBinding.opsinTabClockFormat.newTab().setText(requireContext().getString(R.string.hour_24)));
        timeZoneLayoutBinding.opsinTabClockFormat.setSelectedTabIndicator(R.drawable.thumb_selector);
        timeZoneLayoutBinding.opsinTabClockFormat.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabMetadata.addTab(binding.opsinTabMetadata.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabMetadata.addTab(binding.opsinTabMetadata.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabMetadata.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabMetadata.setTabGravity(TabLayout.GRAVITY_FILL);

        timeZoneLayoutBinding.opsinTabDateTimeMode.addTab(timeZoneLayoutBinding.opsinTabDateTimeMode.newTab().setText(requireContext().getString(R.string.manual)));
        timeZoneLayoutBinding.opsinTabDateTimeMode.addTab(timeZoneLayoutBinding.opsinTabDateTimeMode.newTab().setText(requireContext().getString(R.string.automatic)));
        timeZoneLayoutBinding.opsinTabDateTimeMode.setSelectedTabIndicator(R.drawable.thumb_selector);
        timeZoneLayoutBinding.opsinTabDateTimeMode.setTabGravity(TabLayout.GRAVITY_FILL);

        // new settings
        binding.opsinTabMonochrome.addOnTabSelectedListener(opsinTabMonochrome);
        binding.opsinTabNoise.addOnTabSelectedListener(opsinTabNoise);
        binding.opsinTabRoi.addOnTabSelectedListener(opsinTabRoi);
        binding.opsinTabSource.addOnTabSelectedListener(opsinTabSourceMode);
        timeZoneLayoutBinding.opsinTabClockFormat.addOnTabSelectedListener(opsinTabClockFormat);
        binding.opsinTabMetadata.addOnTabSelectedListener(opsinTabMetadata);
        timeZoneLayoutBinding.opsinTabDateTimeMode.addOnTabSelectedListener(opsinTabDateTimeMode);

        binding.opsinNucIcon.setOnTouchListener((view, motionEvent) -> {
            int eventAction = motionEvent.getAction();
            if (eventAction == MotionEvent.ACTION_DOWN) {
                if (opsinTouchCounter == 0)
                    opsinTouchHandler.postDelayed(opsinTouchResetCounter, 3000);
                opsinTouchCounter++;

                if (opsinTouchCounter == 5) {
                    opsinTouchHandler.removeCallbacks(opsinTouchResetCounter);
                    opsinTouchCounter = 0;
                    cameraViewModel.hasShowStates();
                }
            }
            return false;
        });

        binding.opsinTabNuc.addTab(binding.opsinTabNuc.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabNuc.addTab(binding.opsinTabNuc.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabNuc.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabNuc.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabNuc.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mStrCurrentTab.equalsIgnoreCase("NUC")) {
                    clickedTimeStamp = -1;
                    previousPos = 0;
                }
                if (!cameraSettingsViewModel.isProgramatically()) {
                    if (clickedTimeStamp == -1) {
                        setNUC(tab);
                    } else if (clickedTimeStamp > 0) {
                        long currentMills = System.currentTimeMillis();
                        if (currentMills - clickedTimeStamp > 1000) {
                            setNUC(tab);
                        } else {
                            TabLayout.Tab tabAt = binding.opsinTabNuc.getTabAt(previousPos);
                            if (tabAt != null) {
                                tabAt.select();
                                cameraSettingsViewModel.setNucTabPosition(previousPos);
                                cameraSettingsViewModel.setNucSelected(true);
                            }
                        }
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.opsinTabMic.addTab(binding.opsinTabMic.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabMic.addTab(binding.opsinTabMic.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabMic.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabMic.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabMic.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mStrCurrentTab.equalsIgnoreCase("MIC")) {
                    clickedTimeStamp = -1;
                    previousPos = 0;
                }
                Log.e(TAG, "onTabSelected: opsinTabMic " + cameraSettingsViewModel.isProgramatically());
                if (!cameraSettingsViewModel.isProgramatically()) {
                    if (clickedTimeStamp == -1) {
                        setMic(tab);
                    } else if (clickedTimeStamp > 0) {
                        long currentMills = System.currentTimeMillis();
                        if (currentMills - clickedTimeStamp > 1000) {
                            setMic(tab);
                        } else {
                            Log.e(TAG, "onTabSelected: opsinTabMic");
                            TabLayout.Tab tabAt = binding.opsinTabMic.getTabAt(previousPos);
                            if (tabAt != null) {
                                tabAt.select();
                                cameraSettingsViewModel.setMicTabPosition(previousPos);
                                cameraSettingsViewModel.setMicSelected(true);
                            }
                        }
                    }
                } else {
                    setMic(tab);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.opsinTabJpeg.addTab(binding.opsinTabJpeg.newTab().setText(requireContext().getString(R.string.low)));
        binding.opsinTabJpeg.addTab(binding.opsinTabJpeg.newTab().setText(requireContext().getString(R.string.high)));
        binding.opsinTabJpeg.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabJpeg.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabJpeg.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mStrCurrentTab.equalsIgnoreCase("JPEG")) {
                    clickedTimeStamp = -1;
                    previousPos = 0;
                }
                if (clickedTimeStamp == -1) {
                    Log.e(TAG, "jpeg: onTabSelected: " + tab.getPosition());
                    setJPEG(tab);
                } else if (clickedTimeStamp > 0) {
                    long currentMills = System.currentTimeMillis();
                    if (currentMills - clickedTimeStamp > 1000) {
                        Log.e(TAG, "jpeg: onTabSelected: " + tab.getPosition());
                        setJPEG(tab);
                    } else {
                        TabLayout.Tab tabAt = binding.opsinTabJpeg.getTabAt(previousPos);
                        if (tabAt != null) {
                            tabAt.select();
                            cameraSettingsViewModel.setJpegCompressionTabPosition(previousPos);
                            cameraSettingsViewModel.setJpegCompressSelected(true);
                        }
                    }
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.opsinTabFps.addTab(binding.opsinTabFps.newTab().setText(requireContext().getString(R.string.fps_30)));
        binding.opsinTabFps.addTab(binding.opsinTabFps.newTab().setText(requireContext().getString(R.string.fps_60)));
        binding.opsinTabFps.addTab(binding.opsinTabFps.newTab().setText(requireContext().getString(R.string.fps_90)));
        binding.opsinTabFps.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabFps.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabFps.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mStrCurrentTab.equalsIgnoreCase("FPS")) {
                    clickedTimeStamp = -1;
                    previousPos = 0;
                }
                if (clickedTimeStamp == -1) {
                    setFPS(tab);
                } else if (clickedTimeStamp > 0) {
                    long currentMills = System.currentTimeMillis();
                    if (currentMills - clickedTimeStamp > 1000) {
                        setFPS(tab);
                    } else {
                        TabLayout.Tab tabAt = binding.opsinTabFps.getTabAt(previousPos);
                        if (tabAt != null) {
                            tabAt.select();
                            cameraSettingsViewModel.setFpsTabPosition(previousPos);
                            cameraSettingsViewModel.setFpsSelected(true);
                        }
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.opsinTabClock.addTab(binding.opsinTabClock.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabClock.addTab(binding.opsinTabClock.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabClock.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabClock.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabClock.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mStrCurrentTab.equalsIgnoreCase("CLOCK")) {
                    clickedTimeStamp = -1;
                    previousPos = 0;
                }
                if (clickedTimeStamp == -1) {
                    setClock(tab);
                } else if (clickedTimeStamp > 0) {
                    long currentMills = System.currentTimeMillis();
                    if (currentMills - clickedTimeStamp > 1000) {
                        setClock(tab);
                    } else {
                        TabLayout.Tab tabAt = binding.opsinTabClock.getTabAt(previousPos);
                        if (tabAt != null) {
                            tabAt.select();
                            cameraSettingsViewModel.setTimeFormatTabPosition(previousPos);
                            cameraSettingsViewModel.setTimeFormatSelected(true);
                        }
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        binding.opsinTabUtc.addTab(binding.opsinTabUtc.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabUtc.addTab(binding.opsinTabUtc.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabUtc.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabUtc.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabUtc.addOnTabSelectedListener(tabUtcSelectionListener);

        binding.opsinTabGps.addTab(binding.opsinTabGps.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabGps.addTab(binding.opsinTabGps.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabGps.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabGps.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabGps.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!mStrCurrentTab.equalsIgnoreCase("GPS")) {
                    clickedTimeStamp = -1;
                    previousPos = 0;
                }
                if (clickedTimeStamp == -1) {
                    setGPS(tab);
                } else if (clickedTimeStamp > 0) {
                    long currentMills = System.currentTimeMillis();
                    if (currentMills - clickedTimeStamp > 1000) {
                        setGPS(tab);
                    } else {
                        TabLayout.Tab tabAt = binding.opsinTabGps.getTabAt(previousPos);
                        if (tabAt != null) {
                            tabAt.select();
                            cameraSettingsViewModel.setGpsTabPosition(previousPos);
                            cameraSettingsViewModel.setGpsSelected(true);
                        }
                    }
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, clockMode);
        cameraSettingsViewModel.observeOpsinNUCValue.observe(lifecycleOwner, nucState);
        cameraSettingsViewModel.observeOpsinMICState.observe(lifecycleOwner, micState);
        cameraSettingsViewModel.observeOpsinFPSValue.observe(lifecycleOwner, frameRate);
        cameraSettingsViewModel.observeOpsinMonochromeState.observe(lifecycleOwner, getMonochromeState);
        cameraSettingsViewModel.observeOpsinNoiseState.observe(lifecycleOwner, getNoiseState);
        cameraSettingsViewModel.observeOpsinRoiState.observe(lifecycleOwner, getROIState);

        tcpConnectionViewModel.observeOpsinGetTimeZone().observe(lifecycleOwner, getTimeZone);
        tcpConnectionViewModel.observeOpsinSetTimeZone().observe(lifecycleOwner, setTimeZone);
        tcpConnectionViewModel.observeOpsinGetDateTime().observe(lifecycleOwner, getDateTime);
        tcpConnectionViewModel.observeOpsinSetDateTime().observe(lifecycleOwner, setDateTime);
        tcpConnectionViewModel.observeOpsinGetGpsInfo().observe(lifecycleOwner, gpsInfo);
        tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, getSourceState);
        tcpConnectionViewModel.getOpsinMetadataState().observe(lifecycleOwner, getMetadataState);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().observe(lifecycleOwner, getOpsinTimeFormat);


        /* set commands response*/
        tcpConnectionViewModel.observeOpsinSetNUCState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetNUCState : " + o);
                if ((boolean) o) {
                    tcpConnectionViewModel.getNUC();
                } else {
                    cameraSettingsViewModel.setProgramatically(true);
                    int nucTabPosition = cameraSettingsViewModel.getNucTabPosition();
                    if (nucTabPosition == 0) {
                        nucTabPosition = 1;
                    } else {
                        nucTabPosition = 0;
                    }
                     Log.d(TAG, "initOpsinUI: "+ nucTabPosition + " "+ cameraSettingsViewModel.getNucTabPosition()) ;
                    TabLayout.Tab tabAt = binding.opsinTabNuc.getTabAt(nucTabPosition);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                    tcpConnectionViewModel.getNUC();
                }
            }
        }));

        tcpConnectionViewModel.observeOpsinSetMICState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetMICState : " + o);
                tcpConnectionViewModel.getMicState();

            }
        }));

        tcpConnectionViewModel.observeOpsinSetFrameRate().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetFrameRate : " + o);
                tcpConnectionViewModel.getFrameRate();

            }
        }));

        tcpConnectionViewModel.observeOpsinSetJpegCompression().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetJpegCompression : " + o);
                tcpConnectionViewModel.getJpegCompression();

            }
        }));

        tcpConnectionViewModel.observeOpsinSetGpsPower().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetGpsPower : " + o);
                cameraSettingsViewModel.setGpsSelected(false);
            }
        }));

        tcpConnectionViewModel.observeSetOpsinMonochromeState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinMonochromeState : " + o);
                tcpConnectionViewModel.getOpsinMonochromaticState();

            }
        }));

        tcpConnectionViewModel.observeSetOpsinNoiseState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinNoiseState : " + o);
                tcpConnectionViewModel.getOpsinNoiseReductionState();

            }
        }));

        tcpConnectionViewModel.observeSetOpsinROIState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinROIState : " + o);

                tcpConnectionViewModel.getOpsinROI();
            }
        }));

        tcpConnectionViewModel.observeSetOpsinCameraClockMode().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinCameraClockMode : " + o);
                if ((boolean) o) {
                    tcpConnectionViewModel.getOpsinClockMode();
                }
            }
        }));

        tcpConnectionViewModel.observeSetOpsinMetadataState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinMetadataState : " + o);
                tcpConnectionViewModel.getOpsinMetadata();
            }
        }));

        tcpConnectionViewModel.observeSetOpsinTimeFormat().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinMetadataState : " + o);
                tcpConnectionViewModel.getOpsinTimeFormat();
            }
        }));

        tcpConnectionViewModel.observeopsinMICCommandError().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                CommandError commandError = (CommandError) o;
                Log.e(TAG, "opsin_command_error: " + commandError.getError() + " " + cameraSettingsViewModel.getMicTabPosition());
                if (commandError.getError().contains("CAMERA_SET_MIC_STATE")) {
                    cameraSettingsViewModel.setMicSelected(false);
                    cameraSettingsViewModel.setProgramatically(true);
                    int micStatePosition = cameraSettingsViewModel.getMicTabPosition();
                    TabLayout.Tab tabAt = binding.opsinTabMic.getTabAt(micStatePosition);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                    showOpsinCameraMICCommandFailed(requireContext().getString(R.string.mic_command_failed_msg));
                }
                Log.e(TAG, "opsin_command_error: " + commandError.getError() + " " + cameraSettingsViewModel.getMicTabPosition());
            }
        }));

        cameraSettingsViewModel.isSelectOpsinTimeZone.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                // show time zone dialog
                cameraViewModel.hasShowSettingsDialog(false);
                homeViewModel.getNavController().navigate(R.id.timeZoneDialogFragment);
            }
        }));

        cameraSettingsViewModel.isSelectOpsinDateSet.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean)
                showDatePickerDialog();
        }));

        cameraSettingsViewModel.isSelectOpsinTimeSet.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean)
                showTimePickerDialog();
        }));

        cameraSettingsViewModel.isSelectOpsinSetDateAndTime.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.opsinTabSource.getSelectedTabPosition() == 0) {
                    showUtcTimeZoneDialog(getString(R.string.time_zone_warning));
                } else {
                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(true);
                    hasEnableClockFormatTab(false);
                    hasEnableDateTimeModeTab(false);
                    setManualTimeZone(String.valueOf(timeZoneLayoutBinding.opsinSelectTimeZone.getText()));
                }
            }
        }));

        cameraSettingsViewModel.isSelectOpsinSyncLocalDateTime.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                // ass value to camera
                setAutomaticModeDateTime = Calendar.getInstance();
                Log.d(TAG, "opsinSyncLocalDateTime: " + simpleDateFormat().format(setAutomaticModeDateTime.getTime()) + " " + simpleTimeFormat().format(setAutomaticModeDateTime.getTime()));
                if (binding.opsinTabSource.getSelectedTabPosition() == 0) {
                    showUtcTimeZoneDialog(getString(R.string.time_zone_warning));
                } else {
                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(true);
                    hasEnableDateTimeModeTab(false);
                    syncWithLocalTimeZone();
                }
            }
        }));

        timeZoneViewModel.isSelectTimeZone.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(timeZoneValue -> {
            if (timeZoneValue != -1) {
                ArrayList<String> listOfTimeZones = new ArrayList<>(Arrays.asList(timeZoneViewModel.settingsArrayList));
                String myString = listOfTimeZones.get(timeZoneValue);
                String[] split = myString.split("[()]");
                binding.timeZoneLayout.opsinSelectTimeZone.setText(split[1]);
                cameraViewModel.hasShowSettingsDialog(true);
            }
        }));
    }

    private void hasEnableClockFormatTab(boolean isEnableTab) {
        int clockFormatTabPosition = cameraSettingsViewModel.getClockFormatTabPosition();
        if (isEnableTab) {
            if (clockFormatTabPosition == 1)
                timeZoneLayoutBinding.opsinTabClockFormat.getTabAt(0).view.setEnabled(true);
            else
                timeZoneLayoutBinding.opsinTabClockFormat.getTabAt(1).view.setEnabled(true);
        } else {
            if (clockFormatTabPosition == 1)
                timeZoneLayoutBinding.opsinTabClockFormat.getTabAt(0).view.setEnabled(false);
            else
                timeZoneLayoutBinding.opsinTabClockFormat.getTabAt(1).view.setEnabled(false);
        }
    }

    private void hasEnableDateTimeModeTab(boolean isEnable) {
        int dateTimeModePosition = cameraSettingsViewModel.getDateTimeModePosition();
        if (isEnable) {
            if (dateTimeModePosition == 1)
                timeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(0).view.setEnabled(true);
            else
                timeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(1).view.setEnabled(true);
        } else {
            if (dateTimeModePosition == 1)
                timeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(0).view.setEnabled(false);
            else
                timeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(1).view.setEnabled(false);
        }
    }


    private void prepareOpsinSettingsToSave(String presetName) {
        LocalDateTime localDateTime = LocalDateTime.now();
        int nucPos = binding.opsinTabNuc.getSelectedTabPosition();
        int micPos = binding.opsinTabMic.getSelectedTabPosition();
        int gpsPos = binding.opsinTabGps.getSelectedTabPosition();
        int fpsPos = binding.opsinTabFps.getSelectedTabPosition();
        int monochromaticPos = binding.opsinTabMonochrome.getSelectedTabPosition();
        int noisePos = binding.opsinTabNoise.getSelectedTabPosition();
        int roiPos = binding.opsinTabRoi.getSelectedTabPosition();
        int clockPos = binding.opsinTabSource.getSelectedTabPosition();
        int metadataPos = binding.opsinTabMetadata.getSelectedTabPosition();

        byte nucValue = cameraPresetsViewModel.getNUCValue(nucPos);
        byte micValue = cameraPresetsViewModel.getMicValue(micPos);
        byte gpsValue = cameraPresetsViewModel.getGpsValue(gpsPos);
        short fpsValue = cameraPresetsViewModel.getFpsValue(fpsPos);
        byte monochromaticValue = cameraPresetsViewModel.getMonochromaticValue(monochromaticPos);
        byte noiseReductionValue = cameraPresetsViewModel.getNoiseReductionValue(noisePos);
        byte roiValue = cameraPresetsViewModel.getRoiValue(roiPos);
        byte clockValue = cameraPresetsViewModel.getClockValue(clockPos);
        byte metadataValue = cameraPresetsViewModel.getMetadataValue(metadataPos);

        String nucDisplayValue = cameraPresetsViewModel.getNUCDisplayValue(nucPos);
        String micDisplayValue = cameraPresetsViewModel.getMicDisplayValue(micPos);
        String gpsDisplayValue = cameraPresetsViewModel.getGpsDisplayValue(gpsPos);
        String fpsDisplayValue = cameraPresetsViewModel.getFpsDisplayValue(fpsPos);
        String monochromaticDisplayValue = cameraPresetsViewModel.getMonochromaticDisplayValue(monochromaticPos);
        String noiseReductionDisplayValue = cameraPresetsViewModel.getNoiseReductionDisplayValue(noisePos);
        String roiDisplayValue = cameraPresetsViewModel.getRoiDisplayValue(roiPos);
        String clockDisplaValue = cameraPresetsViewModel.getClockDisplaValue(clockPos);
        String metadataDisplayValue = cameraPresetsViewModel.getMetadataDisplayValue(metadataPos);

        SaveSettings saveSettingNuc = new SaveSettings(SETTING_ID_OPSIN_NUC, SETTING_NAME_OPSIN_NUC, nucValue, nucDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingMic = new SaveSettings(SETTING_ID_OPSIN_MIC, SETTING_NAME_OPSIN_MIC, micValue, micDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingGps = new SaveSettings(SETTING_ID_OPSIN_GPS, SETTING_NAME_OPSIN_GPS, gpsValue, gpsDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingFps = new SaveSettings(SETTING_ID_OPSIN_FPS, SETTING_NAME_OPSIN_FPS, fpsValue, fpsDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingMonochromatic = new SaveSettings(SETTING_ID_OPSIN_MONOCHROMATIC, SETTING_NAME_OPSIN_MONOCHROMATIC, monochromaticValue, monochromaticDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingNoise = new SaveSettings(SETTING_ID_OPSIN_NOISE_REDUCTION, SETTING_NAME_OPSIN_NOISE_REDUCTION, noiseReductionValue, noiseReductionDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingRoi = new SaveSettings(SETTING_ID_OPSIN_ROI, SETTING_NAME_OPSIN_ROI, roiValue, roiDisplayValue, false, presetName, localDateTime);
        SaveSettings saveSettingClock = new SaveSettings(SETTING_ID_OPSIN_CLOCK_MODE, SETTING_NAME_OPSIN_CLOCK_MODE, clockValue, clockDisplaValue, false, presetName, localDateTime);
        SaveSettings saveSettingMetaData = new SaveSettings(SETTING_ID_OPSIN_META_DATA, SETTING_NAME_OPSIN_META_DATA, metadataValue, metadataDisplayValue, false, presetName, localDateTime);

        ArrayList<SaveSettings> saveSettings = new ArrayList<>();
        saveSettings.add(saveSettingNuc);
        saveSettings.add(saveSettingMic);
        saveSettings.add(saveSettingGps);
        saveSettings.add(saveSettingFps);
        saveSettings.add(saveSettingMonochromatic);
        saveSettings.add(saveSettingNoise);
        saveSettings.add(saveSettingRoi);
        saveSettings.add(saveSettingClock);
        saveSettings.add(saveSettingMetaData);

        cameraPresetsViewModel.isPresetAvailable(presetName, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Boolean isAvailable) {
                        if (isAvailable) {
                            cameraPresetsViewModel.saveSettingsValue(saveSettings);
                        } else {
                            Toast.makeText(requireActivity(), getString(R.string.already_exists), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getLocalizedMessage());
                    }
                });
    }

    private void prepareNightwaveSettingsToSave(String presetName) {
        LocalDateTime localDateTime = LocalDateTime.now();

        int ledPos = binding.tabLed.getSelectedTabPosition();
        int invertVideoPos = binding.tabInvertVideo.getSelectedTabPosition();
        int flipVideoPos = binding.tabFlipVideo.getSelectedTabPosition();
        int ircutPos = binding.tabIrcut.getSelectedTabPosition();

        byte ledValue = cameraPresetsViewModel.getLedValue(ledPos);
        byte invertVideoValue = cameraPresetsViewModel.getInvertVideoValue(invertVideoPos);
        byte flipVideoValue = cameraPresetsViewModel.getFlipVideoValue(flipVideoPos);
        byte irCutValue = cameraPresetsViewModel.getIRCutValue(ircutPos);

        String ledDisplayValue = cameraPresetsViewModel.getLedDisplayValue(ledPos);
        String invertVideoDisplayValue = cameraPresetsViewModel.getInvertVideoDisplayValue(invertVideoPos);
        String flipVideoDisplayValue = cameraPresetsViewModel.getFlipVideoDisplayValue(flipVideoPos);
        String irCutDisplayValue = cameraPresetsViewModel.getIRCutDisplayValue(ircutPos);

        SaveSettings saveSettingLed = new SaveSettings(SETTING_ID_NW_LED, SETTING_NAME_NW_LED, ledValue, ledDisplayValue, true, presetName, localDateTime);
        SaveSettings saveSettingInvertVideo = new SaveSettings(SETTING_ID_NW_INVERT_VIDEO, SETTING_NAME_NW_INVERT_VIDEO, invertVideoValue, invertVideoDisplayValue, true, presetName, localDateTime);
        SaveSettings saveSettingFlipVideo = new SaveSettings(SETTING_ID_NW_FLIP_VIDEO, SETTING_MANE_NW_FLIP_VIDEO, flipVideoValue, flipVideoDisplayValue, true, presetName, localDateTime);
        SaveSettings saveSettingIrcut = new SaveSettings(SETTING_ID_NW_IRCUT, SETTING_NAME_NW_IRCUT, irCutValue, irCutDisplayValue, true, presetName, localDateTime);

        ArrayList<SaveSettings> saveSettings = new ArrayList<>();
        saveSettings.add(saveSettingLed);
        saveSettings.add(saveSettingInvertVideo);
        saveSettings.add(saveSettingFlipVideo);
        saveSettings.add(saveSettingIrcut);

        cameraPresetsViewModel.isPresetAvailable(presetName, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Boolean isAvailable) {
                        if (isAvailable) {
                            cameraPresetsViewModel.saveSettingsValue(saveSettings);
                        } else {
                            Toast.makeText(requireActivity(), getString(R.string.already_exists), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getLocalizedMessage());
                    }
                });
    }

    TabLayout.OnTabSelectedListener tabUtcSelectionListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (!mStrCurrentTab.equalsIgnoreCase("UTC")) {
                clickedTimeStamp = -1;
                previousPos = 0;
                Log.e(TAG, "onTabSelected: ");
            }

            if (clickedTimeStamp == -1) {
                setUTC(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setUTC(tab);
                } else {
                    TabLayout.Tab tabAt = binding.opsinTabUtc.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setUtcTabPosition(previousPos);
                        cameraSettingsViewModel.setUtcSelected(true);
                    }
                }
            }

        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabMonochrome = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "monochrome: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("Monochrome")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setMonochromatic(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setMonochromatic(tab);
                } else {
                    TabLayout.Tab tabAt = binding.opsinTabMonochrome.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setMonochromeTabPosition(previousPos);
                        cameraSettingsViewModel.setMonochromeSelected(true);
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabNoise = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "2d noise mode: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("Noise")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setNoiseReduction(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setNoiseReduction(tab);
                } else {
                    TabLayout.Tab tabAt = binding.opsinTabNoise.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setNoiseTabPosition(previousPos);
                        cameraSettingsViewModel.setNoiseSelected(true);
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabRoi = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "roi mode: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("Roi")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setRoiMode(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setRoiMode(tab);
                } else {
                    TabLayout.Tab tabAt = binding.opsinTabRoi.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setRoiTabPosition(previousPos);
                        cameraSettingsViewModel.setRoiModeSelected(true);
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };
    private final TabLayout.OnTabSelectedListener opsinTabSourceMode = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "sourcemode: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("SourceMode")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setSourceMode(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setSourceMode(tab);
                } else {
                    TabLayout.Tab tabAt = binding.opsinTabSource.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setSourceTabPosition(previousPos);
                        cameraSettingsViewModel.setSourceModeSelected(true);
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabClockFormat = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "opsinclockFormat: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("Clock_Format")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setClockFormat(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setClockFormat(tab);
                } else {
                    TabLayout.Tab tabAt = timeZoneLayoutBinding.opsinTabClockFormat.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setClockFormatTabPosition(previousPos);
                        cameraSettingsViewModel.setClockFormatSelected(true);
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabMetadata = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "metadata: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("Metadata")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setMetadata(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setMetadata(tab);
                } else {
                    TabLayout.Tab tabAt = binding.opsinTabMetadata.getTabAt(previousPos);
                    if (tabAt != null) {
                        tabAt.select();
                        cameraSettingsViewModel.setMetadataTabPosition(previousPos);
                        cameraSettingsViewModel.setMetadataSelected(true);
                    }
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabDateTimeMode = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "opsinTabDateTimeMode: onTabSelected: " + tab.getPosition());
            if (!mStrCurrentTab.equalsIgnoreCase("DateTimeMode")) {
                clickedTimeStamp = -1;
                previousPos = 0;
            }
            if (clickedTimeStamp == -1) {
                setOpsinDateTimeMode(tab);
            } else if (clickedTimeStamp > 0) {
                long currentMills = System.currentTimeMillis();
                if (currentMills - clickedTimeStamp > 1000) {
                    setOpsinDateTimeMode(tab);
                } else {
                    timeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(previousPos).select();
                    cameraSettingsViewModel.setDateTimeModePosition(previousPos);
                    cameraSettingsViewModel.setDateTimeModeSelected(true);
                }
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private void showOpsinCameraMICCommandFailed(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_MIC_COMMAND_FAILED;
            activity.showDialog("", message, null);
        }
    }

    /* private final EventObserver<Boolean> clockMode = new EventObserver<>(aBoolean -> {
         if (aBoolean) {
             //SYSTEM
             int position = tabUtc.getPosition();
             setTimeZone(position);
         } else {
             //GPS
             //Display a dialog to user to change the clock mode into SYSTEM(NONE)
             int selectedTabPosition = binding.opsinTabUtc.getSelectedTabPosition();
             Log.e(TAG, "initOpsinUI: "+selectedTabPosition+" "+previousPos );
             if (selectedTabPosition == 0) {
                 previousPos = 1;
             } else if (selectedTabPosition == 1){
                 previousPos = 0;
             }
             binding.opsinTabUtc.getTabAt(previousPos).select();
             showUtcTimeZoneDialog(getString(R.string.time_zone_warning));
         }
     });*/


    private final Observer<Object> nucState = value -> {
        if (value != null) {
            if (value instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetNUCState: " + ((CommandError) value).getError());
            } else {
                Log.d(TAG, "observeOpsinGetNUCState : onChanged: " + CameraViewModel.isOpsinCommandInitiateByDialogFragment());
                switch ((int) value) {
                    case 0: // disabled -- No icon
                    case 2: // disabled and operationally active. This is a error state and should not happen -- no icon
                        if (binding.opsinTabNuc.getSelectedTabPosition() != 1) {
                            cameraSettingsViewModel.setNucSelected(false);
                            TabLayout.Tab tabAt = binding.opsinTabNuc.getTabAt(1);
                            if (tabAt != null) {
                                tabAt.select();
                            }
                        }
                        break;
                    case 1: // enabled and operationally inactive -- Red
                    case 3: //enabled and operationally active -- green
                        if (binding.opsinTabNuc.getSelectedTabPosition() != 0) {
                            cameraSettingsViewModel.setNucSelected(false);
                            TabLayout.Tab tabAt = binding.opsinTabNuc.getTabAt(0);
                            if (tabAt != null) {
                                tabAt.select();
                            }
                        }
                        break;
                }
                Log.e(TAG, "observeOpsinGetNUCState: " + value + " / " + cameraSettingsViewModel.getNucTabPosition());
            }
        }

    };

    private final Observer<Object> micState = aBoolean -> {
        if (aBoolean != null) {
            if (aBoolean instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetMICState: " + ((CommandError) aBoolean).getError());
            } else {
                int value;
                if ((byte) aBoolean == SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue()) {
                    value = 0;
                } else {
                    value = 1;
                }
                cameraSettingsViewModel.setMicTabPosition(value);
                cameraSettingsViewModel.setProgramatically(false);

                if (binding.opsinTabMic.getSelectedTabPosition() != value) {
                    TabLayout.Tab tabAt = binding.opsinTabMic.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                cameraSettingsViewModel.setMicSelected(false);

                Log.e(TAG, "observeOpsinCameraMICState : " + value + " / ");
            }
        }
    };

    private final Observer<Object> frameRate = aBoolean -> {
        if (aBoolean != null) {
            if (aBoolean instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetFrameRate: " + ((CommandError) aBoolean).getError());
            } else {
                int value;
                if ((short) aBoolean == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue()) {
                    value = 0;
                } else if ((short) aBoolean == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue()) {
                    value = 1;
                } else {
                    value = 2;
                }
                cameraSettingsViewModel.setFpsTabPosition(value);
                Log.e(TAG, "observeOpsinGetFrameRate: " + aBoolean + " " + value);
                if (binding.opsinTabFps.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setFpsSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabFps.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
            }
        }
    };

    private final EventObserver<Object> gpsInfo = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetGpsInfo: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_GPS_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                cameraSettingsViewModel.setGpsTabPosition(value);

                if (binding.opsinTabGps.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setGpsSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabGps.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                Log.e(TAG, "observeOpsinGetGpsInfo : " + o);
            }
            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                tcpConnectionViewModel.getOpsinClockMode();
            } else {
                cameraViewModel.setHasShowProgress(false);
            }
        }
    });

    private final EventObserver<Object> getSourceState = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetSourceState : " + ((CommandError) o).getError());
            } else {
                int value;
                if ((boolean) o) {
                    value = 1;
                } else {
                    value = 0;
                }
                cameraSettingsViewModel.setSourceTabPosition(value);

                if (binding.opsinTabSource.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setSourceModeSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabSource.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                Log.e(TAG, "observeOpsinGetSourceState : " + o);
            }
            tcpConnectionViewModel.getOpsinMetadata();
        }
    });

    private final EventObserver<Object> getMetadataState = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetMetadataState: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_METADATA_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                cameraSettingsViewModel.setMetadataTabPosition(value);

                if (binding.opsinTabMetadata.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setMetadataSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabMetadata.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                Log.e(TAG, "observeOpsinGetMetadataState : " + o);
            }
            /*TIME ZONE*/
            /*Get Timezone*/
            checkRTCSupportFromVersion();
        }
    });

    private void checkRTCSupportFromVersion() {
        String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
        String cameraWifi = homeViewModel.getCurrentFwVersion().getWiFiRtos();
        int riscvComparisonResult = -1;
        int wifiComparisonResult = -1;
        if (containsFourthDigit(cameraRiscv)) {
            String removedRiscv = removeFourthDigit(cameraRiscv);
            riscvComparisonResult = compareVersions(removedRiscv, OPSIN_RISCV_RTC_SUPPORTS_FROM);
        } else {
            riscvComparisonResult = compareVersions(cameraRiscv, OPSIN_RISCV_RTC_SUPPORTS_FROM);
        }

        if (cameraWifi == null) {
            cameraWifi = "2.1.3";
        }
        if (containsFourthDigit(cameraWifi)) {
            String removedWifi = removeFourthDigit(cameraWifi);
            wifiComparisonResult = compareVersions(removedWifi, OPSIN_WIFI_RTC_SUPPORTS_FROM);
        } else {
            wifiComparisonResult = compareVersions(cameraWifi, OPSIN_WIFI_RTC_SUPPORTS_FROM);
        }

        if (riscvComparisonResult >= 0 && wifiComparisonResult >= 0) {
            tcpConnectionViewModel.getTimeZone();
        } else {
            cameraViewModel.setHasShowProgress(false);
        }
    }

    private final EventObserver<Object> getTimeZone = new EventObserver<>(object -> {
        if (object != null) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetTimeZone: " + ((CommandError) object).getError());
            } else {
                Log.e(TAG, "Timezone From Camera : " + object);
                /*Select Dropdown based on the Timezone value*/
                binding.timeZoneLayout.opsinSelectTimeZone.setText(String.format("GMT%s", object));
                tcpConnectionViewModel.getDateTime();
            }
        }
    });

    private final EventObserver<Object> setTimeZone = new EventObserver<>(o -> {
        if (o != null) {
            Log.e(TAG, "observeOpsinSetTimeZone : " + o);
            if (rtcMode == RTC_MODE.AUTOMATIC) {
                /*Sync with Local Date Time*/
                syncLocalDateTime();
            } else {
                /*Response for Manual Set Timezone*/
                /*Update UI*/
                if (selectedDateTime != null) {
                    Date dateTime = selectedDateTime.getTime();
                    SimpleDateFormat sourceFormat = new SimpleDateFormat("d/M/yy H:m:s", Locale.getDefault(Locale.Category.FORMAT));
                    String sourceDateTime = sourceFormat.format(dateTime);
                    CharSequence cameraTimeZone = binding.timeZoneLayout.opsinSelectTimeZone.getText();
                    setUserSelectedDateTime(String.valueOf(cameraTimeZone), sourceDateTime);
                    Log.d("selectedDate&Time", "" + sourceDateTime + " " + cameraTimeZone);
                }
            }
        }
    });

    private final EventObserver<Object> getDateTime = new EventObserver<>(object -> {
        if (object != null) {
            if (object instanceof CommandError) {
                Log.e(TAG, "GetDateTime: " + ((CommandError) object).getError());
            } else {
                //Update Date and Time in the UI
                try {
                    //current date format
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                    objDate = dateFormat.parse(String.valueOf(object));
                    //Expected date format
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat2 = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                    String finalDate = null;
                    if (objDate != null) {
                        finalDate = dateFormat2.format(objDate.getTime());
                        String[] dateParts = finalDate.split(" ");
                        String date = dateParts[0];
                        String[] dateParts1 = date.split("/");
                        int day = Integer.parseInt(dateParts1[0]);
                        int month = Integer.parseInt(dateParts1[1]);
                        int year = Integer.parseInt(dateParts1[2]);

                        String time = dateParts[1];
                        String[] timeParts1 = time.split(":");
                        int hour = Integer.parseInt(timeParts1[0]);
                        int minutes = Integer.parseInt(timeParts1[1]);
                        int seconds = Integer.parseInt(timeParts1[2]);

                        // for this initiate first time set date time while press set date and time button directly without user select pickers
                        selectedDateTime = Calendar.getInstance();
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, day);
                        selectedDateTime.set(Calendar.MONTH, month-1);
                        selectedDateTime.set(Calendar.YEAR, year);
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                        selectedDateTime.set(Calendar.MINUTE, minutes);
                        selectedDateTime.set(Calendar.SECOND, seconds);
                        Log.e(TAG, "GetDateTime: " + object);

                        timeZoneLayoutBinding.opsinSelectDate.setText(simpleDateFormat().format(objDate));
                        timeZoneLayoutBinding.opsinSelectTime.setText(simpleTimeFormat().format(objDate));
                        updateTimeFormatInUI();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tcpConnectionViewModel.getOpsinTimeFormat();
        }
    });
    private final EventObserver<Object> setDateTime = new EventObserver<>(object -> {
        if (object != null) {
            if (object instanceof CommandError) {
                Log.e(TAG, "SetDateTime: " + ((CommandError) object).getError());
                Toast.makeText(requireContext(), "Failed set date and time", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "SetDateTime: " + object);
                if (rtcMode == RTC_MODE.AUTOMATIC) {
                    /*Response for Automatic Set Timezone*/
                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                    if (!isTimeFormat24Hr(requireContext())) {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_12.getValue());
                    } else {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_24.getValue());
                    }
                } else {
                    /*Response for Manual Set Timezone*/
                    /*Dismiss progress*/
                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                    int clockFormatTabPosition = cameraSettingsViewModel.getClockFormatTabPosition();
                    Log.d(TAG, "clock_Format_position:" + clockFormatTabPosition);
                    if (clockFormatTabPosition == 0) {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_12.getValue());
                    } else {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_24.getValue());
                    }
                }
                Toast.makeText(requireContext(), "Successfully set date and time", Toast.LENGTH_SHORT).show();
            }
        }
    });

    private final Observer<Object> getMonochromeState = o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetMonochromeState: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                cameraSettingsViewModel.setMonochromeTabPosition(value);

                if (binding.opsinTabMonochrome.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setMonochromeSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabMonochrome.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                Log.e(TAG, "observeOpsinGetMonochromeState : " + o);
            }
        }
    };

    private final Observer<Object> getNoiseState = o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetNoiseState: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                cameraSettingsViewModel.setNoiseTabPosition(value);

                if (binding.opsinTabNoise.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setNoiseSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabNoise.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                Log.e(TAG, "observeOpsinGetNoiseState : " + o);
            }
        }

    };

    private final Observer<Object> getROIState = o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetROIState: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.ordinal()) {
                    value = 0;
                } else if ((byte) o == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue()) {
                    value = 1;
                } else {
                    value = 2;
                }
                cameraSettingsViewModel.setRoiTabPosition(value);

                if (binding.opsinTabRoi.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setRoiModeSelected(false);
                    TabLayout.Tab tabAt = binding.opsinTabRoi.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
                Log.e(TAG, "observeOpsinGetROIState : " + o + "//" + CameraViewModel.isOpsinCommandInitiateByDialogFragment());
                if (!CameraViewModel.isOpsinCommandInitiateByDialogFragment()) {
                    //   getCurrentStates();
                    CameraViewModel.setOpsinCommandInitiateByDialogFragment(true);
                }
            }
        }

    };

    private final EventObserver<Object> getOpsinTimeFormat = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetTimeFormat: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_24.getValue()) {
                    value = 1;
                    cameraSettingsViewModel.setTimeFormat24Hrs(true);
                } else {
                    value = 0;
                    cameraSettingsViewModel.setTimeFormat24Hrs(false);
                }
                cameraSettingsViewModel.setClockFormatTabPosition(value);

                if (timeZoneLayoutBinding.opsinTabClockFormat.getSelectedTabPosition() != value) {
                    cameraSettingsViewModel.setClockFormatSelected(false);
                    TabLayout.Tab tabAt = timeZoneLayoutBinding.opsinTabClockFormat.getTabAt(value);
                    if (tabAt != null) {
                        tabAt.select();
                        //here update time format related value in ui
                        updateTimeFormatInUI();
                    }
                }
                cameraViewModel.setHasShowProgress(false);
                Log.e(TAG, "observeOpsinGetTimeFormat : " + o + "//" + CameraViewModel.isOpsinCommandInitiateByDialogFragment());
            }
        }

    });

    private void setNUC(TabLayout.Tab tab) {
        Log.e(TAG, "Nuc: onTabSelected:  " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "NUC";
        //cameraSettingsViewModel.setNucTabPosition(position);
        cameraSettingsViewModel.setNucSelected(true);
        Log.e(TAG, "Nuc:Selected:  " + position);
        if (position == 0) {
            tcpConnectionViewModel.setNUC(SCCPConstants.SCCP_OPSIN_NUC_STATE.ENABLED.getValue());
        } else {
            tcpConnectionViewModel.setNUC(SCCPConstants.SCCP_OPSIN_NUC_STATE.DISABLED.getValue());
        }
    }

    private void setMic(TabLayout.Tab tab) {
        Log.e(TAG, " Mic :onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "MIC";
        cameraSettingsViewModel.setMicSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setMicState(SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue());
        } else {
            tcpConnectionViewModel.setMicState(SCCPConstants.SCCP_OPSIN_MIC_STATE.DISABLED.getValue());
        }
    }

    private void setJPEG(TabLayout.Tab tab) {
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "JPEG";
        cameraSettingsViewModel.setJpegCompressionTabPosition(position);
        cameraSettingsViewModel.setJpegCompressSelected(true);
        if (position == 0) {
            tcpConnectionViewModel.setJpegCompression(SCCPConstants.SCCP_OPSIN_JPEG_COMPRESSION.JPEG_COMPRESSION_LOW.getValue());
        } else {
            tcpConnectionViewModel.setJpegCompression(SCCPConstants.SCCP_OPSIN_JPEG_COMPRESSION.JPEG_COMPRESSION_HIGH.getValue());
        }
    }

    private void setClock(TabLayout.Tab tab) {
        Log.e(TAG, "clockFormat: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "CLOCK";
        cameraSettingsViewModel.setTimeFormatTabPosition(position);
        cameraSettingsViewModel.setTimeFormatSelected(true);
    }

    private void setUTC(TabLayout.Tab tab) {
        Log.e(TAG, "utc: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "UTC";

        cameraSettingsViewModel.setUtcTabPosition(position);
        cameraSettingsViewModel.setUtcSelected(true);

        tabUtc = tab;
        // tcpConnectionViewModel.getOpsinClockMode();
    }

    private void setTimeZone(int position) {
        if (position == 0) {
            //Set UTC
            tcpConnectionViewModel.setTimeZone("0:0");
            Log.d(TAG, "set: TIMEZONE : 0:0");
        } else {
            //Set Local Timezone
            String displayNameLocal = getCurrentTimezoneOffset();

            String[] splitMinus = displayNameLocal.split("-");
            String[] splitPlus = displayNameLocal.split("\\+");

            if (splitMinus.length == 2) {
                displayNameLocal = "-" + splitMinus[1];
            } else if (splitPlus.length == 2) {
                displayNameLocal = "+" + splitPlus[1];
            }
            tcpConnectionViewModel.setTimeZone(displayNameLocal);
            Log.d(TAG, "set: TIMEZONE " + displayNameLocal);
        }
    }

    private void setGPS(TabLayout.Tab tab) {
        Log.e(TAG, "gps: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "GPS";

        cameraSettingsViewModel.setGpsTabPosition(position);
        cameraSettingsViewModel.setGpsSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setGpsPower(SCCPConstants.SCCP_OPSIN_GPS_STATE.ENABLED.getValue());
        } else {
            tcpConnectionViewModel.setGpsPower(SCCPConstants.SCCP_OPSIN_GPS_STATE.DISABLED.getValue());
        }
    }


    private void setFPS(TabLayout.Tab tab) {
        Log.e(TAG, "fps: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "FPS";

        cameraSettingsViewModel.setFpsTabPosition(position);
        cameraSettingsViewModel.setFpsSelected(true);
        Log.e(TAG, "fps2: onTabSelected: " + position);
        if (position == 0) {
            tcpConnectionViewModel.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue());
        } else if (position == 1) {
            tcpConnectionViewModel.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue());
        } else if (position == 2) {
            tcpConnectionViewModel.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_90.getValue());
        }
    }

    private void setMonochromatic(TabLayout.Tab tab) {
        Log.e(TAG, "Monochrome: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "Monochrome";

        cameraSettingsViewModel.setMonochromeTabPosition(position);
        cameraSettingsViewModel.setMonochromeSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setOpsinMonochromaticState(SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.ENABLED.getValue());
        } else {
            tcpConnectionViewModel.setOpsinMonochromaticState(SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.DISABLED.getValue());
        }
    }

    private void setNoiseReduction(TabLayout.Tab tab) {
        Log.e(TAG, "Noise: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "Noise";

        cameraSettingsViewModel.setNoiseTabPosition(position);
        cameraSettingsViewModel.setNoiseSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setOpsinNoiseReductionState(SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.ENABLED.getValue());
        } else {
            tcpConnectionViewModel.setOpsinNoiseReductionState(SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.DISABLED.getValue());
        }
    }

    private void setRoiMode(TabLayout.Tab tab) {
        Log.e(TAG, "Roi: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "Roi";

        cameraSettingsViewModel.setRoiTabPosition(position);
        cameraSettingsViewModel.setRoiModeSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue());
        } else if (position == 1) {
            tcpConnectionViewModel.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue());
        } else {
            tcpConnectionViewModel.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_OFF.getValue());
        }
    }

    private void setSourceMode(TabLayout.Tab tab) {
        Log.e(TAG, "SourceMode: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "SourceMode";

        cameraSettingsViewModel.setSourceTabPosition(position);
        cameraSettingsViewModel.setSourceModeSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setOpsinClockMode((byte) SCCPConstants.SCCP_CLOCK_MODE.GPS.getValue());
        } else {
            tcpConnectionViewModel.setOpsinClockMode((byte) SCCPConstants.SCCP_CLOCK_MODE.SYSTEM.getValue());
        }
    }

    private void setClockFormat(TabLayout.Tab tab) {
        Log.e(TAG, "Clock_Format: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "Clock_Format";
        cameraSettingsViewModel.setClockFormatTabPosition(position);
        cameraSettingsViewModel.setClockFormatSelected(true);

        if (position == 0) {
            cameraSettingsViewModel.setTimeFormat24Hrs(false);
        } else {
            cameraSettingsViewModel.setTimeFormat24Hrs(true);
        }
        // here update time format
        updateTimeFormatInUI();
    }

    private void setMetadata(TabLayout.Tab tab) {
        Log.e(TAG, "Metadata: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "Metadata";
        cameraSettingsViewModel.setMetadataTabPosition(position);
        cameraSettingsViewModel.setMetadataSelected(true);

        if (position == 0) {
            tcpConnectionViewModel.setOpsinMetadata(SCCPConstants.SCCP_OPSIN_METADATA_STATE.ENABLED.getValue());
        } else {
            tcpConnectionViewModel.setOpsinMetadata(SCCPConstants.SCCP_OPSIN_METADATA_STATE.DISABLED.getValue());
        }
    }

    private void setOpsinDateTimeMode(TabLayout.Tab tab) {
        Log.e(TAG, "opsinTabDateTimeMode: onTabSelected: " + tab.getPosition());
        clickedTimeStamp = System.currentTimeMillis();
        int position = tab.getPosition();
        previousPos = position;
        mStrCurrentTab = "DateTimeMode";
        cameraSettingsViewModel.setDateTimeModePosition(position);
        cameraSettingsViewModel.setDateTimeModeSelected(true);

        if (position == 0) {
            // manual
            timeZoneLayoutBinding.opsinDateTimeManualMode.setVisibility(VISIBLE);
            timeZoneLayoutBinding.opsinDateTimeAutomaticMode.setVisibility(View.GONE);
            rtcMode = RTC_MODE.MANUAL;
            checkRTCSupportFromVersion();
        } else {
            // automatic
            timeZoneLayoutBinding.opsinDateTimeManualMode.setVisibility(View.GONE);
            timeZoneLayoutBinding.opsinDateTimeAutomaticMode.setVisibility(VISIBLE);
            setAutomaticModeDateAndTime();
            rtcMode = RTC_MODE.AUTOMATIC;
        }
    }


    private void avoidDoubleClicks(final View view) {
        final long DELAY_IN_MS = 900;
        if (!view.isClickable()) {
            return;
        }
        view.setClickable(false);
        view.postDelayed(() -> view.setClickable(true), DELAY_IN_MS);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initNightWaveUI() {
        binding.opsinUiLayout.setVisibility(View.GONE);
        binding.nightWaveLayout.setVisibility(VISIBLE);

        binding.viewLedIcon.setOnTouchListener((view, motionEvent) -> {
            int eventAction = motionEvent.getAction();
            if (eventAction == MotionEvent.ACTION_DOWN) {
                if (mCounter == 0)
                    mHandler.postDelayed(mResetCounter, 3000);
                mCounter++;

                if (mCounter == 5) {
                    mHandler.removeCallbacks(mResetCounter);
                    mCounter = 0;
                    cameraViewModel.hasShowStates();
                }
            }
            return false;
        });

        binding.tabIrcut.addTab(binding.tabIrcut.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabIrcut.addTab(binding.tabIrcut.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabIrcut.addTab(binding.tabIrcut.newTab().setText(requireContext().getString(R.string.auto)));
        binding.tabIrcut.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabIrcut.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.tabIrcut.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.e("TAG", "onTabSelected1: " + tab.getPosition());
                if (!cameraSettingsViewModel.isIRCutSelected()) {
                    int position = tab.getPosition();
                    Log.e("TAG", "onTabSelected1: " + position);
                    cameraSettingsViewModel.setIrCutTabPosition(position);
                    cameraSettingsViewModel.setIRCutSelected(true);
                    tcpConnectionViewModel.setIRCut(position);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.tabInvertVideo.addTab(binding.tabInvertVideo.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabInvertVideo.addTab(binding.tabInvertVideo.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabInvertVideo.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabInvertVideo.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.tabInvertVideo.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!cameraSettingsViewModel.isInvertVideoSelected()) {
                    int position = tab.getPosition();
                    cameraSettingsViewModel.setInvertVideoTabPosition(position);
                    cameraSettingsViewModel.setInvertVideoSelected(true);
                    tcpConnectionViewModel.setInvertVideo(position == 0 ? 1 : 0);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.tabFlipVideo.addTab(binding.tabFlipVideo.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabFlipVideo.addTab(binding.tabFlipVideo.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabFlipVideo.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabFlipVideo.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.tabFlipVideo.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!cameraSettingsViewModel.isFlipVideoSelected()) {
                    int position = tab.getPosition();
                    cameraSettingsViewModel.setFlipVideoTabPosition(position);
                    cameraSettingsViewModel.setFlipVideoSelected(true);
                    tcpConnectionViewModel.setFlipVideo(position);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.tabLed.addTab(binding.tabLed.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabLed.addTab(binding.tabLed.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabLed.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabLed.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.tabLed.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!cameraSettingsViewModel.isLedSelected()) {
                    int position = tab.getPosition();
                    cameraSettingsViewModel.setLedTabPosition(position);
                    cameraSettingsViewModel.setLedSelected(true);
                    tcpConnectionViewModel.setLedEnableState(position);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // new firmware only show video mode settings
        if (CameraViewModel.hasNewFirmware() && !cameraViewModel.isAnalogMode()) {
            cameraViewModel.setHasShowProgress(true);
            binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.usb)));
            binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.wifi)));
            binding.tabVideoMode.setSelectedTabIndicator(R.drawable.thumb_selector);
            binding.tabVideoMode.setTabGravity(TabLayout.GRAVITY_FILL);
            binding.tabVideoMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    avoidDoubleClicks(tab.view);
                    cameraSettingsViewModel.setVideoModeSelected(true);
                    cameraSettingsViewModel.setVideoModePosition(tab.getPosition());
                    if (tab.getPosition() == 0) {
                        if (!CameraViewModel.isIsLiveScreenVideoModeWIFI() && CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                            CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                            cameraSettingsViewModel.setVideoModeSelected(false);
                            Log.d(TAG, "observeCameraVideoMode1 : Already in USB mode");
                        } else if (!CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                            // CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                            showWifiModeConfirmationDialog(requireContext().getString(R.string.wifi_mode_changes_message), 0);
                            Log.d(TAG, "observeCameraVideoMode1 : UVC ");
                        }
                    } else if (tab.getPosition() == 1) {
                        if (CameraViewModel.isIsLiveScreenVideoModeWIFI() && !CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                            CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                            cameraSettingsViewModel.setVideoModeSelected(false);
                            Log.d(TAG, "observeCameraVideoMode1 : Already in WIFI video mode");
                        } else if (CameraSettingsViewModel.isIsSettingsScreenVideoModeUVC()) {
                            showWifiModeConfirmationDialog(requireContext().getString(R.string.wifi_mode_changes_message), 1);
                        }
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });

            binding.tabBatteryMode.addTab(binding.tabBatteryMode.newTab().setText(requireContext().getString(R.string.on)));
            binding.tabBatteryMode.addTab(binding.tabBatteryMode.newTab().setText(requireContext().getString(R.string.off)));
            binding.tabBatteryMode.setSelectedTabIndicator(R.drawable.thumb_selector);
            binding.tabBatteryMode.setTabGravity(TabLayout.GRAVITY_FILL);

            binding.tabBatteryMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        } else {
            binding.tabBatteryMode.setVisibility(View.GONE);
            binding.batteryModeIcon.setVisibility(View.GONE);
            if (CameraViewModel.hasNewFirmware() && cameraViewModel.isAnalogMode()) {
                // below changes for analog mode grey out video mode switch
                binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.usb)));
                binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.wifi)));
                binding.tabVideoMode.setSelectedTabIndicator(R.drawable.thumb_selector_greyout);
                binding.tabVideoMode.setSelectedTabIndicatorColor(getResources().getColor(R.color.light_gray, null));
                binding.tabVideoMode.setTabGravity(TabLayout.GRAVITY_FILL);

                for (int i = 0; i < binding.tabVideoMode.getTabCount(); i++) {
                    TabLayout.Tab tab = binding.tabVideoMode.getTabAt(i);
                    if (tab != null) {
                        tab.view.setAlpha(0.5f);
                        tab.view.setClickable(false);
                        tab.view.setSelected(false);
                    }
                }
            } else {
                binding.videoModeIcon.setVisibility(View.GONE);
                binding.tabVideoMode.setVisibility(View.GONE);
            }
        }
        binding.batteryModeIcon.setVisibility(View.GONE);
        binding.tabBatteryMode.setVisibility(View.GONE);

        tcpConnectionViewModel.observeLedEnableState().observe(lifecycleOwner, ledEnableState);

        tcpConnectionViewModel.observeFlipVideo().observe(lifecycleOwner, flipVideo);

        tcpConnectionViewModel.observeIRCut().observe(lifecycleOwner, irCutObserver);

        tcpConnectionViewModel.observeInvertVideo().observe(lifecycleOwner, invertVideo);


        cameraViewModel.observeHasShowVideoModeWifiDialog().observe(this.getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.d(TAG, "observeHasShowVideoModeWifiDialog: true");
                // sent reset fpga command
                cameraSettingsViewModel.setVideoModeSelected(false);
                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                if (value != null && value == Constants.STATE_CONNECTED) {
                    tcpConnectionViewModel.setCameraVideoMode(1);// wifi
                } else {
                    tcpConnectionViewModel.connectSocket();
                    tcpConnectionViewModel.isSocketConnected().observe(lifecycleOwner, mState -> {
                        if (mState == Constants.STATE_CONNECTED) {
                            tcpConnectionViewModel.setCameraVideoMode(1);// wifi
                        }
                    });
                }
            } else {
                Log.d(TAG, "observeHasShowVideoModeWifiDialog: false");
                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                CameraViewModel.setIsLiveScreenVideoModeUVC(true);

                cameraViewModel.setHasShowUsbModeTextMessage(true); // for this handled live view below text
                int tab_position = cameraSettingsViewModel.getVideoModePosition();
                cameraSettingsViewModel.setVideoModeSelected(false);
                if (tab_position != 0) {
                    TabLayout.Tab tabAt = binding.tabVideoMode.getTabAt(0);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
            }
        }));

        cameraViewModel.observeHasShowVideoModeUSBDialog().observe(this.getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.d(TAG, "observeHasShowVideoModeUSBDialog: true");
                // sent reset fpga command
                cameraSettingsViewModel.setVideoModeSelected(false);
                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                tcpConnectionViewModel.stopLiveView();
                mHandler.postDelayed(() -> {
                    if (value != null && value == Constants.STATE_CONNECTED) {
                        tcpConnectionViewModel.setCameraVideoMode(0);// usb
                    } else {
                        tcpConnectionViewModel.connectSocket();
                        tcpConnectionViewModel.isSocketConnected().observe(lifecycleOwner, mState -> {
                            if (mState == Constants.STATE_CONNECTED) {
                                tcpConnectionViewModel.setCameraVideoMode(0);// usb
                            }
                        });
                    }
                }, 500);

            } else {
                CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(false);
                cameraViewModel.setHasShowUsbModeTextMessage(false); // for this handled live view below text
                Log.d(TAG, "observeHasShowVideoModeUSBDialog: false");
                int tab_position = cameraSettingsViewModel.getVideoModePosition();
                cameraSettingsViewModel.setVideoModeSelected(false);
                if (tab_position != 1) {
                    TabLayout.Tab tabAt = binding.tabVideoMode.getTabAt(1);
                    if (tabAt != null) {
                        tabAt.select();
                    }
                }
            }
        }));
/* if app bg to fg state not observe video here update video mode state otherwise not observe*/
        cameraSettingsViewModel.isUpdateVideoModeState.observe(lifecycleOwner, new EventObserver<>(object -> {
            if (object != null) {
                if (object instanceof CommandError) {
                    cameraSettingsViewModel.setVideoModeSelected(false);
                    cameraViewModel.setHasShowProgress(false);
                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                    Log.e(TAG, "isUpdateVideoModeState Error: " + ((CommandError) object).getError());
                } else {
                    cameraViewModel.setHasShowProgress(false);
                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                    if (!CameraViewModel.isHasVisibleSettingsInfoView()) { // for this condition to avoid observe on this fragment
                        int response = (int) object;
                        Log.d(TAG, "isUpdateVideoModeState: " + object);
                        cameraSettingsViewModel.setVideoModePosition(response);
                        cameraSettingsViewModel.setVideoModeSelected(false);
                        TabLayout.Tab tabAt = binding.tabVideoMode.getTabAt(response);
                        if (tabAt != null) {
                            tabAt.select();
                        }
                        if (response == SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                            // here show please wait dialog 20sec and dismiss
                            if (!CameraViewModel.isIsLiveScreenVideoModeWIFI() && CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                                Log.d(TAG, "isUpdateVideoModeState : Already in USB mode");
                            } else if (!CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                                cameraSettingsViewModel.setVideoModeSelected(false);
                                tcpConnectionViewModel.resetFPGA(false);
                                showWiFiVideoModeProgressbar();
                                int tab_position = cameraSettingsViewModel.getVideoModePosition();
                                if (tab_position != 1) {
                                    TabLayout.Tab tabAt1 = binding.tabVideoMode.getTabAt(0);
                                    if (tabAt1 != null) {
                                        tabAt1.select();
                                    }
                                }
                                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                                CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                                CameraViewModel.setIsLiveScreenVideoModeUVC(true);

                                new Handler().postDelayed(() -> {
                                    cameraViewModel.setHasShowUsbModeTextMessage(true); // for this handled live view below text
                                    hideWiFiVideoModeProgressbar();
                                }, 20000);// wait for 20 secs*/
                                Log.d(TAG, "isUpdateVideoModeState : UVC ");
                            }
                        } else if (response == SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                            // here show please wait dialog 20sec and start live view
                            if (CameraViewModel.isIsLiveScreenVideoModeWIFI() && !CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED && CameraViewModel.isHasAppBackgroundToForeground()) {
                                    CameraViewModel.setHasAppBackgroundToForeground(false);
                                    Log.d(TAG, "SSCP_VIDEO_MODE_WIFI: " + CameraViewModel.isHasAppBackgroundToForeground());
                                    tcpConnectionViewModel.startLiveView(false);
                                }
                                Log.d(TAG, "isUpdateVideoModeState : Already in WIFI video mode");
                            } else if (CameraSettingsViewModel.isIsSettingsScreenVideoModeUVC()) {
                                CameraViewModel.setHasAppBackgroundToForeground(false);
                                cameraSettingsViewModel.setVideoModeSelected(false);
                                tcpConnectionViewModel.resetFPGA(false);
                                showWiFiVideoModeProgressbar();

                                int tab_position = cameraSettingsViewModel.getVideoModePosition();
                                if (tab_position != 0) {
                                    TabLayout.Tab tabAt1 = binding.tabVideoMode.getTabAt(1);
                                    if (tabAt1 != null) {
                                        tabAt1.select();
                                    }
                                }
                                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(false);
                                CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                                new Handler().postDelayed(() -> {
                                    //sent live view command
                                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                                        tcpConnectionViewModel.startLiveView(false);
                                        cameraViewModel.setHasDismissPleaseWaitProgressDialog(true);
                                    }
                                    cameraViewModel.setHasShowUsbModeTextMessage(false); // for this handled live view below text
                                    hideWiFiVideoModeProgressbar();
                                }, 20000);// wait for 20 secs
                                Log.d(TAG, "isUpdateVideoModeState : WIFI");
                            }
                        }
                    } else {
                        Log.d(TAG, "isUpdateVideoModeState Else: not in settings dialog view");
                    }
                }
            } else {
                cameraViewModel.setHasShowProgress(false);
                cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
            }
        }));
    }

    private final EventObserver<Object> observeCameraVideoMode = new EventObserver<>(object -> {
        if (object instanceof CommandError) {
            cameraSettingsViewModel.setVideoModeSelected(false);
            cameraViewModel.setHasShowProgress(false);
            cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
            Log.e(TAG, "observeCameraVideoMode Error: " + ((CommandError) object).getError());
        } else {
            cameraViewModel.setHasShowProgress(false);
            cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
            if (!CameraViewModel.isHasVisibleSettingsInfoView()) { // for this condition to avoid observe on this fragment
                int response = (int) object;
                Log.d(TAG, "observeCameraVideoMode: " + object);
                cameraSettingsViewModel.setVideoModePosition(response);
                cameraSettingsViewModel.setVideoModeSelected(false);
                TabLayout.Tab tabAt = binding.tabVideoMode.getTabAt(response);
                if (tabAt != null) {
                    tabAt.select();
                }
                if (response == SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                    // here show please wait dialog 20sec and dismiss
                    if (!CameraViewModel.isIsLiveScreenVideoModeWIFI() && CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                        Log.d(TAG, "observeCameraVideoMode : Already in USB mode");
                    } else if (!CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        cameraSettingsViewModel.setVideoModeSelected(false);
                        tcpConnectionViewModel.resetFPGA(false);
                        showWiFiVideoModeProgressbar();
                        int tab_position = cameraSettingsViewModel.getVideoModePosition();
                        if (tab_position != 1) {
                            TabLayout.Tab tabAt1 = binding.tabVideoMode.getTabAt(0);
                            if (tabAt1 != null) {
                                tabAt1.select();
                            }
                        }
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                        CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                        CameraViewModel.setIsLiveScreenVideoModeUVC(true);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cameraViewModel.setHasShowUsbModeTextMessage(true); // for this handled live view below text
                                hideWiFiVideoModeProgressbar();
                            }
                        }, 20000);// wait for 20 secs*/
                        Log.d(TAG, "observeCameraVideoMode : UVC ");
                    }
                } else if (response == SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                    // here show please wait dialog 20sec and start live view
                    if (CameraViewModel.isIsLiveScreenVideoModeWIFI() && !CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                        if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED && CameraViewModel.isHasAppBackgroundToForeground()) {
                            CameraViewModel.setHasAppBackgroundToForeground(false);
                            Log.d(TAG, "SSCP_VIDEO_MODE_WIFI: " + CameraViewModel.isHasAppBackgroundToForeground());
                            tcpConnectionViewModel.startLiveView(false);
                        }
                        Log.d(TAG, "observeCameraVideoMode : Already in WIFI video mode");
                    } else if (CameraSettingsViewModel.isIsSettingsScreenVideoModeUVC()) {
                        CameraViewModel.setHasAppBackgroundToForeground(false);
                        cameraSettingsViewModel.setVideoModeSelected(false);
                        tcpConnectionViewModel.resetFPGA(false);
                        showWiFiVideoModeProgressbar();

                        int tab_position = cameraSettingsViewModel.getVideoModePosition();
                        if (tab_position != 0) {
                            TabLayout.Tab tabAt1 = binding.tabVideoMode.getTabAt(1);
                            if (tabAt1 != null) {
                                tabAt1.select();
                            }
                        }
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(false);
                        CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                        CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //sent live view command
                                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                                    tcpConnectionViewModel.startLiveView(false);
                                    cameraViewModel.setHasDismissPleaseWaitProgressDialog(true);
                                }
                                cameraViewModel.setHasShowUsbModeTextMessage(false); // for this handled live view below text
                                hideWiFiVideoModeProgressbar();
                            }
                        }, 20000);// wait for 20 secs
                        Log.d(TAG, "observeCameraVideoMode : WIFI");
                    }
                }
            } else {
                Log.d(TAG, "observeCameraVideoMode Else: not in settings dialog view");
            }
        }
    });

    private final Observer<Object> ledEnableState = aBoolean -> {
        if (aBoolean instanceof CommandError) {
            Log.e(TAG, "observeLedEnableState: " + ((CommandError) aBoolean).getError());
        } else {
            int tab_position = cameraSettingsViewModel.getLedTabPosition();
            int value = (Boolean) aBoolean ? 0 : 1;
            Log.e(TAG, "observeLedEnableState: " + aBoolean + " " + value);
            if (tab_position != value) {
                TabLayout.Tab tabAt = binding.tabLed.getTabAt(value);
                if (tabAt != null) {
                    tabAt.select();
                }
            }
        }
        cameraSettingsViewModel.setLedSelected(false);
    };

    private final Observer<Object> invertVideo = aBoolean -> {
        if (aBoolean instanceof CommandError) {
            Log.e(TAG, "invertVideo: " + ((CommandError) aBoolean).getError());
        } else {
            int tab_position = cameraSettingsViewModel.getInvertVideoTabPosition();
            int value = (Boolean) aBoolean ? 0 : 1;
            Log.e(TAG, "invertVideo: " + aBoolean + " " + value + "/" + tab_position);
            if (tab_position != value) {
                TabLayout.Tab tabAt = binding.tabInvertVideo.getTabAt(value);
                if (tabAt != null) {
                    tabAt.select();
                }
            }
        }
        cameraSettingsViewModel.setInvertVideoSelected(false);
    };

    private final Observer<Object> flipVideo = aBoolean -> {
        if (aBoolean instanceof CommandError) {
            Log.e(TAG, "observeflipVideo: " + ((CommandError) aBoolean).getError());
        } else {
            int tab_position = cameraSettingsViewModel.getFlipVideoTabPosition();

            int value = (Boolean) aBoolean ? 1 : 0;
            Log.e(TAG, "observeflipVideo: " + aBoolean + " " + value + " " + tab_position);
            if (tab_position != value) {
                TabLayout.Tab tabAt = binding.tabFlipVideo.getTabAt(value);
                if (tabAt != null) {
                    tabAt.select();
                }
            }
        }
        cameraSettingsViewModel.setFlipVideoSelected(false);
    };

    private final Observer<Object> irCutObserver = aInteger -> {
        if (aInteger instanceof CommandError) {
            Log.e(TAG, "observeIRCut: " + ((CommandError) aInteger).getError());
        } else {
            int tab_position = cameraSettingsViewModel.getIrCutTabPosition();
            Integer value = (Integer) aInteger;
            if (value == 17)
                value = 1;
            if (value == 18)
                value = 2;
            Log.e(TAG, "observeIRCut: " + value + " " + tab_position);
            if (tab_position != value && value < 3) {
                TabLayout.Tab tabAt = binding.tabIrcut.getTabAt(value);
                if (tabAt != null) {
                    tabAt.select();
                }
            }
        }
        cameraSettingsViewModel.setIRCutSelected(false);
    };

    public String getCurrentTimezoneOffset() {
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());
        String offset = String.format(getString(R.string.timezone_format), Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = "GMT" + (offsetInMillis >= 0 ? "+" : "-") + offset;
        return offset;
    }

    private void showWifiModeConfirmationDialog(String message, int mode) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            if (mode == 1)
                activity.showDialog = MainActivity.ShowDialog.WIFI_VIDEO_MODE_DIALOG;
            else
                activity.showDialog = MainActivity.ShowDialog.WIFI_TO_USB_VIDEO_MODE_DIALOG;

            activity.showDialog("", message, null);
        }
    }

    private void showWiFiVideoModeProgressbar() {
        try {
            ConstraintLayout constraintLayout = new ConstraintLayout(requireContext());
            ConstraintLayout.LayoutParams textViewParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textViewParams.bottomToBottom = 0;
            textViewParams.topToTop = 0;
            textViewParams.startToStart = 0;
            textViewParams.endToEnd = 0;

            wiFiDisconnectProgressbar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyle);
            ConstraintLayout.LayoutParams progressbarParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            progressbarParams.bottomToBottom = 0;
            progressbarParams.topToTop = 0;
            progressbarParams.startToStart = 0;
            progressbarParams.setMargins(30, 30, 30, 30);

            wiFiDisconnectProgressbar.setLayoutParams(progressbarParams);
            progressTextView = new TextView(requireContext());
            progressTextView.setText(requireContext().getString(R.string.video_mode_please_wait));
            progressTextView.setTextColor(requireContext().getColorStateList(R.color.black));
            progressTextView.setTextSize(20);
            progressTextView.setLayoutParams(textViewParams);

            constraintLayout.addView(wiFiDisconnectProgressbar);
            constraintLayout.addView(progressTextView);

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setCancelable(false);
            builder.setView(constraintLayout);

            pleaseWaitAlertDialog = builder.create();
            pleaseWaitAlertDialog.show();
            Window window = pleaseWaitAlertDialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(pleaseWaitAlertDialog.getWindow().getAttributes());
                layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                pleaseWaitAlertDialog.getWindow().setAttributes(layoutParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideWiFiVideoModeProgressbar() {
        binding.setIsLoading(false);
        try {
            if (isAdded()) {
                if (pleaseWaitAlertDialog != null && pleaseWaitAlertDialog.isShowing()) {
                    pleaseWaitAlertDialog.dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Runnable mResetCounter = () -> {
        mCounter = 0;
        Log.e("mCounter: ", "call back clear");
    };

    private final Runnable opsinTouchResetCounter = () -> {
        opsinTouchCounter = 0;
        Log.e("opsinTouchCounter: ", "call back clear");
    };

    private void showUtcTimeZoneDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_UTC_TIME_ZONE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        if (menuVisible) {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    if (!CameraViewModel.isHasVisibleSettingsInfoView()) {
                        try {
                            if (mSocketState == Constants.STATE_CONNECTED) {
                                if (CameraViewModel.hasNewFirmware()) {
                                    cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(true);
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getInvertImage(), 500);
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getFlipImage(), 1000);
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getIRCut(), 1500);
                                    //  new Handler().postDelayed(() -> tcpConnectionViewModel.getLedEnableState(), 2000);

                                    new Handler().postDelayed(() -> {
                                        if (!cameraViewModel.isAnalogMode())
                                            tcpConnectionViewModel.getVideoMode();
                                        else
                                            cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                                    }, 2000);
                                }
                            } else {
                                /*for this while pip mode exit to select settings tab to show socket connection error,so avoid use this here*/
                                tcpConnectionViewModel.connectSocket();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case OPSIN:
                    /* for this avoid continues observer and get command response*/
                    CameraViewModel.setOpsinCommandInitiateByDialogFragment(false);
                    CameraViewModel.setHasCommandInitiate(true);
                    cameraViewModel.setHasShowProgress(true);
                    getCurrentStates();
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"setMenuVisibility : NW_Digital");
                    break;
            }
        } else {
            CameraViewModel.setOpsinCommandInitiateByDialogFragment(true);
        }
        super.setMenuVisibility(menuVisible);
    }

    private void getCurrentStates() {
        //tcpConnectionViewModel.getNUC();
        CameraViewModel.setOpsinCommandInitiateByDialogFragment(false);
        new Handler().postDelayed(() -> tcpConnectionViewModel.getGpsInfo(), 500);

        cameraSettingsViewModel.observeOpsinNUCValue.observe(lifecycleOwner, nucState);
        cameraSettingsViewModel.observeOpsinMICState.observe(lifecycleOwner, micState);
        cameraSettingsViewModel.observeOpsinFPSValue.observe(lifecycleOwner, frameRate);

        tcpConnectionViewModel.observeOpsinGetTimeZone().observe(lifecycleOwner, getTimeZone);
        tcpConnectionViewModel.observeOpsinSetTimeZone().observe(lifecycleOwner, setTimeZone);
        tcpConnectionViewModel.observeOpsinGetDateTime().observe(lifecycleOwner, getDateTime);
        tcpConnectionViewModel.observeOpsinSetDateTime().observe(lifecycleOwner, setDateTime);

        tcpConnectionViewModel.observeOpsinGetGpsInfo().observe(lifecycleOwner, gpsInfo);
        cameraSettingsViewModel.observeOpsinMonochromeState.observe(lifecycleOwner, getMonochromeState);
        cameraSettingsViewModel.observeOpsinNoiseState.observe(lifecycleOwner, getNoiseState);
        cameraSettingsViewModel.observeOpsinRoiState.observe(lifecycleOwner, getROIState);
        tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, getSourceState);
        tcpConnectionViewModel.getOpsinMetadataState().observe(lifecycleOwner, getMetadataState);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().observe(lifecycleOwner, getOpsinTimeFormat);
    }

    private void setUserSelectedDateTime(String cameraTimeZone, String sourceDateTime) {
        //String sourceDateTime = "28/12/23 18:14:0";
        //   String cameraTimeZone = "GMT+7:00";

        TimeZone sourceTimeZone = TimeZone.getTimeZone(cameraTimeZone);
        // Set the target timezone (GMT+0:00)
        TimeZone targetTimeZone = TimeZone.getTimeZone("GMT+0:00");

        // Create a SimpleDateFormat with the source timezone
        SimpleDateFormat sourceFormatter = new SimpleDateFormat("d/M/yy H:m:s", Locale.getDefault(Locale.Category.FORMAT));
        sourceFormatter.setTimeZone(sourceTimeZone);

        // Manually set a date and time in the source timezone
        String targetDateTime = "";
        try {
            Date sourceDate = sourceFormatter.parse(sourceDateTime);

            // Create a SimpleDateFormat with the target timezone
            SimpleDateFormat targetFormatter = new SimpleDateFormat("d/M/yy H:m:s", Locale.getDefault(Locale.Category.FORMAT));
            targetFormatter.setTimeZone(targetTimeZone);

            // Convert the date and time to the target timezone
            if (sourceDate != null) {
                targetDateTime = targetFormatter.format(sourceDate);
            }

            Log.e(TAG, "setUserSelectedDateTime: Source Date and Time (GMT+7:00): " + sourceDateTime);
            Log.e(TAG, "setUserSelectedDateTime: Target Date and Time (GMT+0:00): " + targetDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yy H:m:s");
        LocalDateTime dateTime = LocalDateTime.parse(targetDateTime, formatter);
        int day = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();
        int year = dateTime.getYear();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();
        year = year - 1980;

        int date_time = (
                (year & 0x3F) << 26)
                | ((month & 0x0F) << 22)
                | ((day & 0x1F) << 17)
                | ((hour & 0x1F) << 12)
                | ((minute & 0x3F) << 6)
                | (second & 0x3F);

        DateTime objDateTime = new DateTime();
        objDateTime.setDateTime(date_time);
        tcpConnectionViewModel.setDateTime(objDateTime);
    }

    //Manual Button Click (Remove GMT String from timezone)
    private void setManualTimeZone(String timeZone) {
        String validTimeZone = timeZone.replace("GMT", "").trim();
        tcpConnectionViewModel.setTimeZone(validTimeZone);
    }

    //Automatic Button Click
    private void syncWithLocalTimeZone() {
        String gmtInfo = getGMTInfo();
        tcpConnectionViewModel.setTimeZone(gmtInfo);
    }

    private void syncLocalDateTime() {
        TimeZone timeZone = TimeZone.getDefault();
        SimpleDateFormat inputFormatter = new SimpleDateFormat("d/M/yy H:m:s", Locale.getDefault(Locale.Category.FORMAT));
        inputFormatter.setTimeZone(timeZone);
        Date date = null;
        String formattedDate;
        try {
            date = new Date();
            formattedDate = inputFormatter.format(date);
            Log.e(TAG, "Input Date Time: " + formattedDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TimeZone targetTimeZoneZero = TimeZone.getTimeZone("GMT+0:0");
        SimpleDateFormat targetFormatForZeroTimezone = new SimpleDateFormat("d/M/yy H:m:s", Locale.getDefault(Locale.Category.FORMAT));
        targetFormatForZeroTimezone.setTimeZone(targetTimeZoneZero);
        String outputDateTimeForTimezoneZero = null;
        if (date != null) {
            outputDateTimeForTimezoneZero = targetFormatForZeroTimezone.format(date);
        }
        Log.e(TAG, "Output Date Time: " + outputDateTimeForTimezoneZero);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yy H:m:s");
        LocalDateTime dateTime = LocalDateTime.parse(outputDateTimeForTimezoneZero, formatter);
        int day = dateTime.getDayOfMonth();
        int month = dateTime.getMonthValue();
        int year = dateTime.getYear();
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        int second = dateTime.getSecond();
        year = year - 1980;

        int date_time = (
                (year & 0x3F) << 26)
                | ((month & 0x0F) << 22)
                | ((day & 0x1F) << 17)
                | ((hour & 0x1F) << 12)
                | ((minute & 0x3F) << 6)
                | (second & 0x3F);

        DateTime objDateTime = new DateTime();
        objDateTime.setDateTime(date_time);
        tcpConnectionViewModel.setDateTime(objDateTime);
    }

    private String getGMTInfo() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        String timeZoneId = defaultTimeZone.getID();
        // Get the TimeZone object for the specified ID
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        // Get the GMT offset in milliseconds
        int rawOffsetMillis = timeZone.getRawOffset();
        // Convert the offset to hours and minutes
        int hours = rawOffsetMillis / (60 * 60 * 1000);
        int minutes = Math.abs((rawOffsetMillis / (60 * 1000)) % 60);

        // Determine the sign of the offset
        String sign = (rawOffsetMillis < 0) ? "-" : "+";
        String gmtValue = sign + hours + ":" + String.format(getString(R.string.minutes), minutes);
        Log.e(TAG, "getGMTInfo: " + gmtValue);
        return gmtValue;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //nightwave
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                removeNightwaveObservers();
                break;
            case OPSIN:
                //  removeOpsinObservers();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"onDetach : NW_Digital");
                break;
        }
    }

    private void removeNightwaveObservers() {
        //nightwave
        tcpConnectionViewModel.observeIRCut().removeObserver(irCutObserver);
        tcpConnectionViewModel.observeIRCut().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeFlipVideo().removeObserver(flipVideo);
        tcpConnectionViewModel.observeFlipVideo().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeInvertVideo().removeObserver(invertVideo);
        tcpConnectionViewModel.observeInvertVideo().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeLedEnableState().removeObserver(ledEnableState);
        tcpConnectionViewModel.observeLedEnableState().removeObservers(lifecycleOwner);
    }

    private void removeOpsinObservers() {
        //Opsin
        cameraSettingsViewModel.observeOpsinNUCValue.removeObserver(nucState);
        cameraSettingsViewModel.observeOpsinNUCValue.removeObservers(lifecycleOwner);
        cameraSettingsViewModel.observeOpsinMICState.removeObserver(micState);
        cameraSettingsViewModel.observeOpsinMICState.removeObservers(lifecycleOwner);
        cameraSettingsViewModel.observeOpsinFPSValue.removeObserver(frameRate);
        cameraSettingsViewModel.observeOpsinFPSValue.removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeOpsinGetTimeZone().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinGetTimeZone().removeObserver(getTimeZone);
        tcpConnectionViewModel.observeOpsinSetTimeZone().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinSetTimeZone().removeObserver(setTimeZone);
        tcpConnectionViewModel.observeOpsinSetDateTime().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinSetDateTime().removeObserver(setDateTime);
        tcpConnectionViewModel.observeOpsinGetDateTime().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinGetDateTime().removeObserver(getDateTime);

        tcpConnectionViewModel.observeOpsinGetGpsInfo().removeObserver(gpsInfo);
        tcpConnectionViewModel.observeOpsinGetGpsInfo().removeObservers(lifecycleOwner);
        cameraSettingsViewModel.observeOpsinMonochromeState.removeObserver(getMonochromeState);
        cameraSettingsViewModel.observeOpsinMonochromeState.removeObservers(lifecycleOwner);
        cameraSettingsViewModel.observeOpsinNoiseState.removeObserver(getNoiseState);
        cameraSettingsViewModel.observeOpsinNoiseState.removeObservers(lifecycleOwner);
        cameraSettingsViewModel.observeOpsinRoiState.removeObserver(getROIState);
        cameraSettingsViewModel.observeOpsinRoiState.removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinCameraClockMode().removeObserver(getSourceState);
        tcpConnectionViewModel.observeOpsinCameraClockMode().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.getOpsinMetadataState().removeObserver(getMetadataState);
        tcpConnectionViewModel.getOpsinMetadataState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().removeObserver(getOpsinTimeFormat);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().removeObservers(lifecycleOwner);
    }

    @Override
    public void onResume() {
        super.onResume();
        tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
//        if (binding.customProgressBar.getVisibility() == View.VISIBLE) {
//           cameraViewModel.setHasShowProgress(false);
//        }
    }

    private void showDatePickerDialog() {
        int mYear = 0;
        int mMonth = 0;
        int mDay = 0;
        String text = String.valueOf(timeZoneLayoutBinding.opsinSelectDate.getText());
        if (!text.equals(requireContext().getString(R.string.select_date))) {
            try {
                // Parse the date string into a Date object
                Date parsedDate = simpleDateFormat().parse(text);
                // Create a new SimpleDateFormat object to format the date with the desired year format
                SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
                // Format the parsed date to include the year in "yyyy" format
                if (parsedDate != null) {
                    String[] dateParts = text.split("/");
                    int camera_date = Integer.parseInt(dateParts[0]);
                    int camera_month = Integer.parseInt(dateParts[1]);
                    int camera_year = Integer.parseInt(sdfYear.format(parsedDate));
                    Calendar calendar = Calendar.getInstance();
                    // here month index start from 0
                    calendar.set(camera_year, camera_month - 1, camera_date);
                    mYear = calendar.get(Calendar.YEAR);
                    mMonth = calendar.get(Calendar.MONTH);
                    mDay = calendar.get(Calendar.DAY_OF_MONTH);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            calendar = Calendar.getInstance();
            mYear = calendar.get(Calendar.YEAR);
            mMonth = calendar.get(Calendar.MONTH);
            mDay = calendar.get(Calendar.DAY_OF_MONTH);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Handle the selected date
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    String formattedDate = simpleDateFormat().format(selectedDateTime.getTime());
                    timeZoneLayoutBinding.opsinSelectDate.setText(formattedDate);
                },
                mYear, mMonth, mDay);
        datePickerDialog.show();
        datePickerDialog.setCancelable(false);
    }

    private void showTimePickerDialog() {
        int mHour = 0;
        int mMinute = 0;
        String time = timeZoneLayoutBinding.opsinSelectTime.getText().toString();
        if (!time.equals(requireContext().getString(R.string.select_time))) {
            if (cameraSettingsViewModel.isTimeFormat24Hrs()) {
                String[] timeParts = time.split(":");
                int mHourOfDay = Integer.parseInt(timeParts[0]);
                String[] timeFormatSplit = timeParts[1].split(" ");
                int mMinute1 = Integer.parseInt(timeFormatSplit[0]);
                Calendar calendar = Calendar.getInstance();
                // set camera time
                calendar.set(Calendar.HOUR_OF_DAY, mHourOfDay);
                calendar.set(Calendar.MINUTE, mMinute1);
                // get
                mHour = calendar.get(Calendar.HOUR_OF_DAY);
                mMinute = calendar.get(Calendar.MINUTE);
            } else {
                String[] timeParts = time.split(":");
                int mHourOfDay = Integer.parseInt(timeParts[0]);
                String[] timeFormatSplit = timeParts[1].split(" ");
                int mMinute1 = Integer.parseInt(timeFormatSplit[0]);
                String timeFormat = timeFormatSplit[1];
                Calendar calendar = Calendar.getInstance();
                // set camera time
                calendar.set(Calendar.HOUR, mHourOfDay);
                calendar.set(Calendar.MINUTE, mMinute1);
                if (timeFormat.equals("AM"))
                    calendar.set(Calendar.AM_PM, Calendar.AM);
                else
                    calendar.set(Calendar.AM_PM, Calendar.PM);
                // get
                mHour = calendar.get(Calendar.HOUR_OF_DAY);
                mMinute = calendar.get(Calendar.MINUTE);
            }
        } else {
            calendar = Calendar.getInstance();
            mHour = calendar.get(Calendar.HOUR_OF_DAY);
            mMinute = calendar.get(Calendar.MINUTE);
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);

                    String formattedTime = simpleTimeFormat().format(selectedDateTime.getTime());
                    if (cameraSettingsViewModel.isTimeFormat24Hrs())
                        timeZoneLayoutBinding.opsinSelectTime.setText(formattedTime);
                    else
                        timeZoneLayoutBinding.opsinSelectTime.setText(convert24to12Hrs(formattedTime));
                    updateTimeFormatInUI();

                }, mHour, mMinute, cameraSettingsViewModel.isTimeFormat24Hrs());
        timePickerDialog.show();
        timePickerDialog.setCancelable(false);
    }

    private void setAutomaticModeDateAndTime() {
        setAutomaticModeDateTime = Calendar.getInstance();
        setAutomaticModeDateTime.get(Calendar.YEAR);
        setAutomaticModeDateTime.get(Calendar.MONTH);
        setAutomaticModeDateTime.get(Calendar.DAY_OF_MONTH);
        setAutomaticModeDateTime.get(Calendar.HOUR_OF_DAY);
        setAutomaticModeDateTime.get(Calendar.MINUTE);

        SimpleDateFormat automaticDateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        String automaticFormattedDate = automaticDateFormat.format(setAutomaticModeDateTime.getTime());

        SimpleDateFormat automaticTimeFormat = new SimpleDateFormat("HH:mm a", Locale.getDefault());
        String automaticFormattedTime = automaticTimeFormat.format(setAutomaticModeDateTime.getTime());

        timeZoneLayoutBinding.opsinLocalDate.setText(getString(R.string.colon_split, automaticFormattedDate));

        if (isTimeFormat24Hr(requireContext())) {
            Log.d("Time Format", "24-hour format");
            String automatic24HrTimeFormat = simple24HourTimeFormat().format(setAutomaticModeDateTime.getTime());
            timeZoneLayoutBinding.opsinLocalTime.setText(getString(R.string.colon_split, automatic24HrTimeFormat));
            timeZoneLayoutBinding.opsinLocalTimeFormat.setText(getString(R.string.colon_split, getString(R.string.hour_24)));
        } else {
            Log.d("Time Format", "12-hour format");
            String automatic12HrTimeFormat = simple12HourTimeFormat().format(setAutomaticModeDateTime.getTime());
            timeZoneLayoutBinding.opsinLocalTime.setText(getString(R.string.colon_split, automatic12HrTimeFormat.toUpperCase()));
            timeZoneLayoutBinding.opsinLocalTimeFormat.setText(getString(R.string.colon_split, getString(R.string.hour_12)));
        }

        DateFormat date = new SimpleDateFormat("z", Locale.getDefault());
        String localTimeZone = date.format(setAutomaticModeDateTime.getTime());
        timeZoneLayoutBinding.opsinLocalTimeZone.setText(getString(R.string.colon_split, localTimeZone));
        Log.d(TAG, "setAutomaticModeDateAndTime: " + automaticFormattedTime);
    }

    private void updateTimeFormatInUI() {
        String formattedTime = simpleTimeFormat().format(selectedDateTime.getTime());
        int clockFormatTabPosition = cameraSettingsViewModel.getClockFormatTabPosition();
        if (!formattedTime.isEmpty()) {
            if (clockFormatTabPosition == 0) {
                if (!cameraSettingsViewModel.isTimeFormat24Hrs())
                    timeZoneLayoutBinding.opsinSelectTime.setText(convert24to12Hrs(formattedTime));
            } else {
                if (cameraSettingsViewModel.isTimeFormat24Hrs()) {
                    String formatted24HrTime = simple24HourTimeFormat().format(selectedDateTime.getTime());
                    timeZoneLayoutBinding.opsinSelectTime.setText(formatted24HrTime);
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        tcpConnectionViewModel.isSocketConnected().removeObservers(lifecycleOwner);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
        tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
    }

    public boolean containsFourthDigit(String version) {
        String[] parts = new String[0];
        try {
            parts = version.split("\\.");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}