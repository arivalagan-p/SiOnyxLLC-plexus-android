package com.sionyx.plexus.ui.popup;

import static com.dome.librarynightwave.model.repository.TCPRepository.isOpsinLiveStreamingStarted;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.sionyx.plexus.ui.camera.CameraViewModel.cameraXValue;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.isSelectedTimeZone;
import static com.sionyx.plexus.ui.home.HomeViewModel.isShowTimeZoneLayout;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.ui.home.HomeViewModel.setSelectedTimeZone;
import static com.sionyx.plexus.ui.home.HomeViewModel.setShowTimeZoneLayout;
import static com.sionyx.plexus.ui.popup.PopUpCameraSettingsViewModel.isTimeFormat24Hrs;
import static com.sionyx.plexus.utils.Constants.OPSIN_RISCV_RTC_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OPSIN_WIFI_RTC_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.convert24to12Hrs;
import static com.sionyx.plexus.utils.Constants.isTimeFormat24Hr;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.opsinmodel.DateTime;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.utils.SCCPConstants;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.android.material.tabs.TabLayout;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentPopUpSettingsBinding;
import com.sionyx.plexus.databinding.PopUpTimeZoneLayoutBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraSettingsViewModel;
import com.sionyx.plexus.ui.dialog.TimeZoneViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.circulardotprogressbar.DotCircleProgressIndicator;

import java.lang.reflect.Field;
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

public class PopUpSettingsFragment extends Fragment {
    private static final String TAG = "PopUpSettingsFragment";
    private FragmentPopUpSettingsBinding binding;
    private PopUpTimeZoneLayoutBinding popUpTimeZoneLayoutBinding;
    private TimeZoneViewModel timeZoneViewModel;
    public TCPConnectionViewModel tcpConnectionViewModel;
    private LifecycleOwner lifecycleOwner;
    private PopUpCameraSettingsViewModel popUpCameraSettingsViewModel;
    private HomeViewModel homeViewModel;
    private CameraViewModel cameraViewModel;
    private boolean isViewShown;
    private final Handler mHandler = new Handler();
    ProgressBar wiFiDisconnectProgressbar;
    TextView progressTextView;
    private Calendar calendar;
    private Calendar selectedDateTime;
    private Calendar setAutomaticModeDateTime;
    private Date objDate;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    private String[] split;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        popUpCameraSettingsViewModel = new ViewModelProvider(requireActivity()).get(PopUpCameraSettingsViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        timeZoneViewModel = new ViewModelProvider(requireActivity()).get(TimeZoneViewModel.class);

        /*Nightwave*/
        popUpCameraSettingsViewModel.setInvertVideoTabPosition(0);
        popUpCameraSettingsViewModel.setFlipVideoTabPosition(0);
        popUpCameraSettingsViewModel.setIrCutTabPosition(0);
        popUpCameraSettingsViewModel.setLedTabPosition(0);

        /*Opsin*/
        popUpCameraSettingsViewModel.setNucTabPosition(0);
        popUpCameraSettingsViewModel.setMicTabPosition(0);
        popUpCameraSettingsViewModel.setFpsTabPosition(0);
        popUpCameraSettingsViewModel.setJpegCompressionTabPosition(0);
        popUpCameraSettingsViewModel.setTimeFormatTabPosition(0);
        popUpCameraSettingsViewModel.setUtcTabPosition(0);
        popUpCameraSettingsViewModel.setGpsTabPosition(0);
        popUpCameraSettingsViewModel.setMonochromeTabPosition(0);
        popUpCameraSettingsViewModel.setNoiseTabPosition(0);
        popUpCameraSettingsViewModel.setRoiTabPosition(0);
        popUpCameraSettingsViewModel.setSourceTabPosition(0);
        popUpCameraSettingsViewModel.setClockFormatTabPosition(0);
        popUpCameraSettingsViewModel.setMetadataTabPosition(0);
        if (popUpCameraSettingsViewModel.rtcMode == PopUpCameraSettingsViewModel.RTC_MODE.MANUAL)
            popUpCameraSettingsViewModel.setDateTimeModePosition(0);
        else
            popUpCameraSettingsViewModel.setDateTimeModePosition(1);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPopUpSettingsBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        popUpTimeZoneLayoutBinding = binding.popTimeZoneLayout;
        popUpTimeZoneLayoutBinding.setViewModel(popUpCameraSettingsViewModel);
        binding.tabBatteryMode.setVisibility(View.GONE);
        binding.batteryModeIcon.setVisibility(View.GONE);
        binding.customProgressBar.setIndicator(new DotCircleProgressIndicator());
        binding.setDateTimeModeResponseProgressBar.setIndicator(new DotCircleProgressIndicator());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        if (screenType == POP_UP_SETTINGS_SCREEN) {/*rotate device in home screen the screenType pop up is not updated */
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                initNightWaveUI();
                break;
            case OPSIN:
                popUpCameraSettingsViewModel.setNucSelected(true);
                popUpCameraSettingsViewModel.setMicSelected(true);
                popUpCameraSettingsViewModel.setJpegCompressSelected(true);
                popUpCameraSettingsViewModel.setFpsSelected(true);
                popUpCameraSettingsViewModel.setUtcSelected(true);
                popUpCameraSettingsViewModel.setGpsSelected(true);

                popUpCameraSettingsViewModel.setMonochromeSelected(true);
                popUpCameraSettingsViewModel.setNoiseSelected(true);
                popUpCameraSettingsViewModel.setRoiModeSelected(true);
                popUpCameraSettingsViewModel.setSourceModeSelected(true);
                popUpCameraSettingsViewModel.setClockFormatSelected(true);
                popUpCameraSettingsViewModel.setMetadataSelected(true);
                popUpCameraSettingsViewModel.setDateTimeModeSelected(true);
                initOpsinUI();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"onViewCreated : NW_Digital");
                break;
        }
        Log.e(TAG, "onViewCreated: closePopup before ");
//        }

        homeViewModel.closePopupSettingFragment.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                Log.e(TAG, "onViewCreated: closePopup");
                popUpCameraSettingsViewModel.setCameraTime(null);
                popUpCameraSettingsViewModel.setCameraDate(null);
                removeObservers();
            }
        }));

        popUpCameraSettingsViewModel.observeHasShowProgressbar().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.customProgressBar.smoothToShow();
            } else {
                binding.customProgressBar.smoothToHide();
            }
            Log.d(TAG, "observeHasShowProgressbar: " + aBoolean);
        }));
    }

    private TabLayout.Tab tabUtc;

    private void initOpsinUI() {
        try {
            String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
            if (cameraRiscv != null) {
                String[] aCameraRiscVersion = cameraRiscv.split("\\.");
                cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);
                Log.e(TAG, "aCameraRiscVersion: " + cameraXValue);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        tcpConnectionViewModel.getOpsinCameraName();
        binding.nightWaveLayout.setVisibility(View.GONE);
        binding.opsinUiLayout.setVisibility(View.VISIBLE);

        binding.opsinTabNuc.addTab(binding.opsinTabNuc.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabNuc.addTab(binding.opsinTabNuc.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabNuc.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabNuc.setTabGravity(TabLayout.GRAVITY_FILL);


        binding.opsinTabMic.addTab(binding.opsinTabMic.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabMic.addTab(binding.opsinTabMic.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabMic.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabMic.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabJpeg.addTab(binding.opsinTabJpeg.newTab().setText(requireContext().getString(R.string.low)));
        binding.opsinTabJpeg.addTab(binding.opsinTabJpeg.newTab().setText(requireContext().getString(R.string.high)));
        binding.opsinTabJpeg.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabJpeg.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabFps.addTab(binding.opsinTabFps.newTab().setText(requireContext().getString(R.string.fps_30)));
        binding.opsinTabFps.addTab(binding.opsinTabFps.newTab().setText(requireContext().getString(R.string.fps_60)));
        binding.opsinTabFps.addTab(binding.opsinTabFps.newTab().setText(requireContext().getString(R.string.fps_90)));
        binding.opsinTabFps.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabFps.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabClock.addTab(binding.opsinTabClock.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabClock.addTab(binding.opsinTabClock.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabClock.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabClock.setTabGravity(TabLayout.GRAVITY_FILL);
        binding.opsinClockIcon.setVisibility(View.GONE);
        binding.opsinTabClock.setVisibility(View.GONE);

        binding.opsinTabUtc.addTab(binding.opsinTabUtc.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabUtc.addTab(binding.opsinTabUtc.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabUtc.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabUtc.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabGps.addTab(binding.opsinTabGps.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabGps.addTab(binding.opsinTabGps.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabGps.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabGps.setTabGravity(TabLayout.GRAVITY_FILL);
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

        popUpTimeZoneLayoutBinding.opsinTabClockFormat.addTab(popUpTimeZoneLayoutBinding.opsinTabClockFormat.newTab().setText(requireContext().getString(R.string.hour_12)));
        popUpTimeZoneLayoutBinding.opsinTabClockFormat.addTab(popUpTimeZoneLayoutBinding.opsinTabClockFormat.newTab().setText(requireContext().getString(R.string.hour_24)));
        popUpTimeZoneLayoutBinding.opsinTabClockFormat.setSelectedTabIndicator(R.drawable.thumb_selector);
        popUpTimeZoneLayoutBinding.opsinTabClockFormat.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabMetadata.addTab(binding.opsinTabMetadata.newTab().setText(requireContext().getString(R.string.on)));
        binding.opsinTabMetadata.addTab(binding.opsinTabMetadata.newTab().setText(requireContext().getString(R.string.off)));
        binding.opsinTabMetadata.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.opsinTabMetadata.setTabGravity(TabLayout.GRAVITY_FILL);

        popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.addTab(popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.newTab().setText(requireContext().getString(R.string.manual)));
        popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.addTab(popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.newTab().setText(requireContext().getString(R.string.automatic)));
        popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.setSelectedTabIndicator(R.drawable.thumb_selector);
        popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.opsinTabUtc.setVisibility(View.GONE);
        binding.opsinUtcIcon.setVisibility(View.GONE);


        //new settings
        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
            binding.opsinTabMonochrome.setVisibility(View.VISIBLE);
            binding.opsinMonochromeIcon.setVisibility(View.VISIBLE);
            binding.opsinTabNoise.setVisibility(View.VISIBLE);
            binding.opsinNoiseIcon.setVisibility(View.VISIBLE);
            binding.opsinTabRoi.setVisibility(View.VISIBLE);
            binding.opsinRoiModeIcon.setVisibility(View.VISIBLE);
            binding.opsinTabSource.setVisibility(View.VISIBLE);
            binding.opsinSourceModeIcon.setVisibility(View.VISIBLE);
            binding.opsinTabMetadata.setVisibility(View.VISIBLE);
            binding.opsinMetaDataIcon.setVisibility(View.VISIBLE);
            popUpTimeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.GONE);
            popUpTimeZoneLayoutBinding.opsinTabClockFormat.setVisibility(View.VISIBLE);
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
            binding.opsinMetaDataIcon.setVisibility(View.GONE);
            popUpTimeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.GONE);
            popUpTimeZoneLayoutBinding.opsinTabClockFormat.setVisibility(View.GONE);
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
            popUpTimeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.VISIBLE);
        } else {
            popUpTimeZoneLayoutBinding.dateTimeModeLayout.setVisibility(View.GONE);
        }

        binding.opsinTabNuc.addOnTabSelectedListener(opsinTabNuc);
        binding.opsinTabMic.addOnTabSelectedListener(opsinTabMic);
        binding.opsinTabJpeg.addOnTabSelectedListener(opsinTabJpeg);
        binding.opsinTabFps.addOnTabSelectedListener(opsinTabFps);
        binding.opsinTabClock.addOnTabSelectedListener(opsinTabClock);
        binding.opsinTabUtc.addOnTabSelectedListener(opsinTabUtc);
        binding.opsinTabGps.addOnTabSelectedListener(opsinTabGps);
        // new settings
        binding.opsinTabMonochrome.addOnTabSelectedListener(opsinTabMonochrome);
        binding.opsinTabNoise.addOnTabSelectedListener(opsinTabNoise);
        binding.opsinTabRoi.addOnTabSelectedListener(opsinTabRoi);
        binding.opsinTabSource.addOnTabSelectedListener(opsinTabSourceMode);
        popUpTimeZoneLayoutBinding.opsinTabClockFormat.addOnTabSelectedListener(opsinTabClockFormat);
        binding.opsinTabMetadata.addOnTabSelectedListener(opsinTabMetadata);
        popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.addOnTabSelectedListener(opsinTabDateTimeMode);


        //Opsin
        // tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, clockMode);
        tcpConnectionViewModel.observeOpsinGetNUCState().observe(lifecycleOwner, nucState);

        tcpConnectionViewModel.observeOpsinGetMICState().observe(lifecycleOwner, micState);

        tcpConnectionViewModel.observeOpsinGetFrameRate().observe(lifecycleOwner, frameRate);

        // tcpConnectionViewModel.observeOpsinGetTimeZone().observe(lifecycleOwner, timeZone);

        tcpConnectionViewModel.observeOpsinGetGpsInfo().observe(lifecycleOwner, gpsInfo);
        tcpConnectionViewModel.getOpsinMonochromeState().observe(lifecycleOwner, observeMonochrome);
        tcpConnectionViewModel.getOpsinNoiseState().observe(lifecycleOwner, observeNoiseMode);
        tcpConnectionViewModel.getOpsinROIState().observe(lifecycleOwner, observeRoiMode);
        tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, observeSourceMode);
        // tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, observeClockFormat);
        tcpConnectionViewModel.getOpsinMetadataState().observe(lifecycleOwner, observeMetadata);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().observe(lifecycleOwner, observeOpsinGetTimeFormat);
        tcpConnectionViewModel.observeOpsinGetTimeZone().observe(lifecycleOwner, getTimeZone);
        tcpConnectionViewModel.observeOpsinSetTimeZone().observe(lifecycleOwner, setTimeZone);
        tcpConnectionViewModel.observeOpsinGetDateTime().observe(lifecycleOwner, getDateTime);
        tcpConnectionViewModel.observeOpsinSetDateTime().observe(lifecycleOwner, setDateTime);

        tcpConnectionViewModel.observeOpsinGetDateTime().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                if (o instanceof CommandError) {
                    Log.e(TAG, "observeOpsinGetDateTime: " + ((CommandError) o).getError());
                } else {
                    if (!CameraViewModel.isOpsinCommandInitiateByDialogFragment()) {
                        DateTime date = (DateTime) o;
                        Log.e(TAG, "observeOpsinGetDateTime : " + date.getDateTime() + " " + date.getHighResolutionClock());
                    }
                }
            }
        }));

        tcpConnectionViewModel.observeOpsinSetDateTime().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                if (o instanceof CommandError) {
                    Log.e(TAG, "observeOpsinSetDateTime: " + ((CommandError) o).getError());
                } else {
                    Log.e(TAG, "observeOpsinSetDateTime: " + o);
                }
            }
        }));

        /* set commands response*/
        tcpConnectionViewModel.observeOpsinSetNUCState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetNUCState : " + o);
                popUpCameraSettingsViewModel.setNucSelected(false);
            }
        }));

        tcpConnectionViewModel.observeOpsinSetMICState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetMICState : " + o);
                popUpCameraSettingsViewModel.setMicSelected(false);
            }
        }));

        tcpConnectionViewModel.observeOpsinSetFrameRate().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetFrameRate : " + o);
                popUpCameraSettingsViewModel.setFpsSelected(false);
            }
        }));

        tcpConnectionViewModel.observeOpsinSetJpegCompression().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetJpegCompression : " + o);
                popUpCameraSettingsViewModel.setJpegCompressSelected(false);
            }
        }));

        tcpConnectionViewModel.observeOpsinSetTimeZone().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetTimeZone : " + o);
                popUpCameraSettingsViewModel.setUtcSelected(false);
            }
        }));

        tcpConnectionViewModel.observeOpsinSetGpsPower().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeOpsinSetGpsPower : " + o);
                popUpCameraSettingsViewModel.setGpsSelected(false);
            }
        }));

        tcpConnectionViewModel.observeSetOpsinMonochromeState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinMonochromeState : " + o);
                popUpCameraSettingsViewModel.setMonochromeSelected(false);
            }
        }));

        tcpConnectionViewModel.observeSetOpsinNoiseState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinNoiseState : " + o);
                popUpCameraSettingsViewModel.setNoiseSelected(false);
            }
        }));

        tcpConnectionViewModel.observeSetOpsinROIState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinROIState : " + o);
                popUpCameraSettingsViewModel.setRoiModeSelected(false);
            }
        }));

        tcpConnectionViewModel.observeSetOpsinCameraClockMode().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                if ((boolean) o) {
                    Log.e(TAG, "observeSetOpsinCameraClockMode : " + o);
                    popUpCameraSettingsViewModel.setSourceModeSelected(false);
                }
            }
        }));

        tcpConnectionViewModel.observeSetOpsinMetadataState().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinMetadataState : " + o);
                popUpCameraSettingsViewModel.setMetadataSelected(false);
            }
        }));

        tcpConnectionViewModel.observeSetOpsinTimeFormat().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                Log.e(TAG, "observeSetOpsinMetadataState : " + o);
                popUpCameraSettingsViewModel.setClockFormatSelected(false);
            }
        }));

        tcpConnectionViewModel.observeopsinMICCommandError().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o != null) {
                CommandError commandError = (CommandError) o;
                if (commandError.getError().contains("CAMERA_SET_MIC_STATE")) {
                    popUpCameraSettingsViewModel.setMicSelected(false);
                    int micStatePosition = popUpCameraSettingsViewModel.getMicTabPosition();

                    binding.opsinTabMic.removeOnTabSelectedListener(opsinTabMic);
                    binding.opsinTabMic.post(() -> {
                        TabLayout.Tab tab = binding.opsinTabMic.getTabAt(micStatePosition);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabMic.addOnTabSelectedListener(opsinTabMic);
                        showOpsinCameraMICCommandFailed(requireContext().getString(R.string.mic_command_failed_msg));
                    });
                }
                Log.e(TAG, "opsin_command_error: " + commandError.getError());
            }
        }));

        popUpCameraSettingsViewModel.isSelectOpsinTimeZone.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                // show time zone list view
                homeViewModel.openTimeZoneListview();
            }
        }));

        popUpCameraSettingsViewModel.isSelectOpsinDateSet.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean)
                showDatePickerDialog();
        }));

        popUpCameraSettingsViewModel.isSelectOpsinTimeSet.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean)
                showTimePickerDialog();
        }));

        popUpCameraSettingsViewModel.isSelectOpsinSetDateAndTime.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                if (binding.opsinTabSource.getSelectedTabPosition() == 0) {
                    showUtcTimeZoneDialog(getString(R.string.time_zone_warning));
                } else {
                    popUpCameraSettingsViewModel.hasShowVideoModeResponseProgressbar(true);
                    hasEnableClockFormatTab(false);
                    hasEnableDateTimeModeTab(false);
                    setManualTimeZone(String.valueOf(popUpTimeZoneLayoutBinding.opsinSelectTimeZone.getText()));
                }
            }
        }));

        popUpCameraSettingsViewModel.isSelectOpsinSyncLocalDateTime.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                // ass value to camera
                setAutomaticModeDateTime = Calendar.getInstance();
                Log.d(TAG, "opsinSyncLocalDateTime: " + simpleDateFormat().format(setAutomaticModeDateTime.getTime()) + " " + simpleTimeFormat().format(setAutomaticModeDateTime.getTime()));
                if (binding.opsinTabSource.getSelectedTabPosition() == 0) {
                    showUtcTimeZoneDialog(getString(R.string.time_zone_warning));
                } else {
                    popUpCameraSettingsViewModel.hasShowVideoModeResponseProgressbar(true);
                    hasEnableDateTimeModeTab(false);
                    syncWithLocalTimeZone();
                }
            }
        }));

        homeViewModel.isClosedTimeZoneListView.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(timeZoneValue -> {
            if (timeZoneValue != -1) {
                ArrayList<String> listOfTimeZones = new ArrayList<>(Arrays.asList(timeZoneViewModel.settingsArrayList));
                String myString = listOfTimeZones.get(timeZoneValue);
                split = myString.split("[()]");
                binding.popTimeZoneLayout.opsinSelectTimeZone.setText(split[1]);
            }
        }));

        popUpCameraSettingsViewModel.isShowVideoModeResponseProgressbar.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            new Handler().post(() -> {
                if (aBoolean) {
                    binding.setDateTimeModeResponseProgressBar.smoothToShow();
                } else {
                    binding.setDateTimeModeResponseProgressBar.smoothToHide();
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                        hasEnableClockFormatTab(true);
                        hasEnableDateTimeModeTab(true);
                    }
                }
            });
        }));

        isOpsinLiveStreamingStarted = true;// Just avoiding start live streaming during this fragment visible
        applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
        tcpConnectionViewModel.clearPeriodicRequestList();
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);
    }

    private void hasEnableClockFormatTab(boolean isEnableTab) {
        int clockFormatTabPosition = popUpCameraSettingsViewModel.getClockFormatTabPosition();
        if (isEnableTab) {
            if (clockFormatTabPosition == 1)
                popUpTimeZoneLayoutBinding.opsinTabClockFormat.getTabAt(0).view.setEnabled(true);
            else
                popUpTimeZoneLayoutBinding.opsinTabClockFormat.getTabAt(1).view.setEnabled(true);
        } else {
            if (clockFormatTabPosition == 1)
                popUpTimeZoneLayoutBinding.opsinTabClockFormat.getTabAt(0).view.setEnabled(false);
            else
                popUpTimeZoneLayoutBinding.opsinTabClockFormat.getTabAt(1).view.setEnabled(false);
        }
    }

    private void hasEnableDateTimeModeTab(boolean isEnable) {
        int dateTimeModePosition = popUpCameraSettingsViewModel.getDateTimeModePosition();
        if (isEnable) {
            if (dateTimeModePosition == 1)
                popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(0).view.setEnabled(true);
            else
                popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(1).view.setEnabled(true);
        } else {
            if (dateTimeModePosition == 1)
                popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(0).view.setEnabled(false);
            else
                popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(1).view.setEnabled(false);
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

    private void showOpsinCameraMICCommandFailed(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_MIC_COMMAND_FAILED;
            activity.showDialog("", message, null);
        }
    }

    private final TabLayout.OnTabSelectedListener opsinTabNuc = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "Nuc: onTabSelected:  " + tab.getPosition() + " " + popUpCameraSettingsViewModel.isNucSelected());
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setNucSelected(true);
            Log.e(TAG, "Nuc:Selected:  " + position);
            if (position == 0) {
                tcpConnectionViewModel.setNUC(SCCPConstants.SCCP_OPSIN_NUC_STATE.ENABLED.getValue());
            } else {
                tcpConnectionViewModel.setNUC(SCCPConstants.SCCP_OPSIN_NUC_STATE.DISABLED.getValue());
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };
    private final TabLayout.OnTabSelectedListener opsinTabMic = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, " Mic :onTabSelected: " + tab.getPosition() + " " + popUpCameraSettingsViewModel.isMicSelected());
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setMicSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setMicState(SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue());
            } else {
                tcpConnectionViewModel.setMicState(SCCPConstants.SCCP_OPSIN_MIC_STATE.DISABLED.getValue());
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabJpeg = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "jpeg: onTabSelected: " + tab.getPosition());
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setJpegCompressionTabPosition(position);
            popUpCameraSettingsViewModel.setJpegCompressSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setJpegCompression(SCCPConstants.SCCP_OPSIN_JPEG_COMPRESSION.JPEG_COMPRESSION_LOW.getValue());
            } else {
                tcpConnectionViewModel.setJpegCompression(SCCPConstants.SCCP_OPSIN_JPEG_COMPRESSION.JPEG_COMPRESSION_HIGH.getValue());
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };
    private final TabLayout.OnTabSelectedListener opsinTabFps = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "fps: onTabSelected: " + tab.getPosition());
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setFpsTabPosition(position);
            popUpCameraSettingsViewModel.setFpsSelected(true);
            Log.e(TAG, "fps2: onTabSelected: " + position);
            if (position == 0) {
                tcpConnectionViewModel.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue());
            } else if (position == 1) {
                tcpConnectionViewModel.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue());
            } else if (position == 2) {
                tcpConnectionViewModel.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_90.getValue());
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };
    private final TabLayout.OnTabSelectedListener opsinTabClock = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "clockFormat: onTabSelected: " + tab.getPosition());
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setTimeFormatTabPosition(position);
            popUpCameraSettingsViewModel.setTimeFormatSelected(true);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private final TabLayout.OnTabSelectedListener opsinTabUtc = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "utc: onTabSelected: " + tab.getPosition());
            tabUtc = tab;
            //  tcpConnectionViewModel.getOpsinClockMode();

        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };


    private final TabLayout.OnTabSelectedListener tabIrCut = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (!popUpCameraSettingsViewModel.isIRCutSelected()) {
                if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    int position = tab.getPosition();
                    binding.tabIrcut.addOnTabSelectedListener(tabIrCut);
                    popUpCameraSettingsViewModel.setIrCutTabPosition(position);
                    popUpCameraSettingsViewModel.setIRCutSelected(true);
                    tcpConnectionViewModel.setIRCut(position);
                    Log.e("tabIrCut", "onTabSelected: " + position);
                } else {
                    binding.tabIrcut.removeOnTabSelectedListener(tabIrCut);
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


    private final TabLayout.OnTabSelectedListener tabInvertVideo = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (!popUpCameraSettingsViewModel.isInvertVideoSelected()) {
                if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    binding.tabInvertVideo.addOnTabSelectedListener(tabInvertVideo);
                    int position = tab.getPosition();
                    popUpCameraSettingsViewModel.setInvertVideoTabPosition(position);
                    popUpCameraSettingsViewModel.setInvertVideoSelected(true);
                    tcpConnectionViewModel.setInvertVideo(position == 0 ? 1 : 0);
                    Log.e("tabInvertVideo", "onTabSelected: " + tab.getPosition());
                } else {
                    binding.tabInvertVideo.removeOnTabSelectedListener(tabInvertVideo);
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


    private final TabLayout.OnTabSelectedListener tabFlipVideo = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (!popUpCameraSettingsViewModel.isFlipVideoSelected()) {
                if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    binding.tabFlipVideo.addOnTabSelectedListener(tabFlipVideo);
                    int position = tab.getPosition();
                    popUpCameraSettingsViewModel.setFlipVideoTabPosition(position);
                    popUpCameraSettingsViewModel.setFlipVideoSelected(true);
                    tcpConnectionViewModel.setFlipVideo(position);
                    Log.e("tabFlipVideo", "onTabSelected: " + tab.getPosition());
                } else {
                    binding.tabFlipVideo.removeOnTabSelectedListener(tabFlipVideo);
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

    private final TabLayout.OnTabSelectedListener tabLed = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (!popUpCameraSettingsViewModel.isLedSelected()) {
                if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    binding.tabLed.addOnTabSelectedListener(tabLed);
                    int position = tab.getPosition();
                    popUpCameraSettingsViewModel.setLedTabPosition(position);
                    popUpCameraSettingsViewModel.setLedSelected(true);
                    tcpConnectionViewModel.setLedEnableState(position);
                    Log.e("tabLed", "onTabSelected: " + tab.getPosition());
                } else {
                    binding.tabLed.removeOnTabSelectedListener(tabLed);
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

    private final TabLayout.OnTabSelectedListener tabVideoMode = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            avoidDoubleClicks(tab.view);
            /* this condition for avoid recall this observe when confirmation dialog shown and rotating the device     */
            if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                binding.tabVideoMode.addOnTabSelectedListener(tabVideoMode);
                popUpCameraSettingsViewModel.setVideoModeSelected(true);
                popUpCameraSettingsViewModel.setVideoModePosition(tab.getPosition());
                if (tab.getPosition() == 0) {
                    if (!CameraViewModel.isIsLiveScreenVideoModeWIFI() && CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                        popUpCameraSettingsViewModel.setVideoModeSelected(false);
                        Log.d(TAG, "observeCameraVideoMode1 : Already in USB mode");
                    } else if (!CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        // CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                        showWifiModeConfirmationDialog(requireContext().getString(R.string.wifi_mode_changes_message), 0);
                        Log.d(TAG, "observeCameraVideoMode1 : UVC ");
                    }
                } else if (tab.getPosition() == 1) {
                    if (CameraViewModel.isIsLiveScreenVideoModeWIFI() && !CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                        popUpCameraSettingsViewModel.setVideoModeSelected(false);
                        Log.d(TAG, "observeCameraVideoMode1 : Already in WIFI video mode");
                    } else if (CameraSettingsViewModel.isIsSettingsScreenVideoModeUVC()) {
                        showWifiModeConfirmationDialog(requireContext().getString(R.string.wifi_mode_changes_message), 1);
                    }
                }
            } else {
                //binding.tabVideoMode.removeOnTabSelectedListener(tabVideoMode);
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };


    /* private final EventObserver<Boolean> clockMode = new EventObserver<>(aBoolean -> {
         if (aBoolean) {
             //SYSTEM
             int position = tabUtc.getPosition();
             setTimeZone(position);
         } else {
             //GPS
             //Display a dialog to user to change the clock mode into SYSTEM(NONE)
             int selectedTabPosition = binding.opsinTabUtc.getSelectedTabPosition();
             Log.e(TAG, "initOpsinUI: " + selectedTabPosition + " " + previousPos);
             if (selectedTabPosition == 0) {
                 previousPos = 1;
             } else if (selectedTabPosition == 1) {
                 previousPos = 0;
             }
             binding.opsinTabUtc.removeOnTabSelectedListener(opsinTabUtc);
             binding.opsinTabUtc.getTabAt(previousPos).select();
             showUtcTimeZoneDialog(getString(R.string.time_zone_warning));
             binding.opsinTabUtc.addOnTabSelectedListener(opsinTabUtc);
         }
     });
 */
    private void setTimeZone(int position) {
//        int position = tab.getPosition();
        popUpCameraSettingsViewModel.setUtcTabPosition(position);
        popUpCameraSettingsViewModel.setUtcSelected(true);

        if (position == 0) {
            //Set UTC
            tcpConnectionViewModel.setTimeZone("0:0");
            Log.d(TAG, "set: TIMEZONE : 0:0");
        } else {
            //Set Local Timezone
            String displayNameLocal = getCurrentTimezoneOffset();
            String displayNameUTC = "00:00";

            String[] splitMinus = displayNameLocal.split("-");
            String[] splitPlus = displayNameLocal.split("\\+");

            if (splitMinus.length == 2) {
                displayNameLocal = "-" + splitMinus[1];
            } else if (splitPlus.length == 2) {
                displayNameLocal = "+" + splitPlus[1];
            }
            tcpConnectionViewModel.setTimeZone(displayNameLocal);
            Log.d(TAG, "set: TIMEZONE " + displayNameLocal + " " + displayNameUTC);
        }
    }

    private final TabLayout.OnTabSelectedListener opsinTabGps = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Log.e(TAG, "clockFormat: onTabSelected: " + tab.getPosition());
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setGpsTabPosition(position);
            popUpCameraSettingsViewModel.setGpsSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setGpsPower(SCCPConstants.SCCP_OPSIN_GPS_STATE.ENABLED.getValue());
            } else {
                tcpConnectionViewModel.setGpsPower(SCCPConstants.SCCP_OPSIN_GPS_STATE.DISABLED.getValue());
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setMonochromeTabPosition(position);
            popUpCameraSettingsViewModel.setMonochromeSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setOpsinMonochromaticState(SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.ENABLED.getValue());
            } else {
                tcpConnectionViewModel.setOpsinMonochromaticState(SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.DISABLED.getValue());
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setNoiseTabPosition(position);
            popUpCameraSettingsViewModel.setNoiseSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setOpsinNoiseReductionState(SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.ENABLED.getValue());
            } else {
                tcpConnectionViewModel.setOpsinNoiseReductionState(SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.DISABLED.getValue());
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setRoiTabPosition(position);
            popUpCameraSettingsViewModel.setRoiModeSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue());
            } else if (position == 1) {
                tcpConnectionViewModel.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue());
            } else {
                tcpConnectionViewModel.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_OFF.getValue());
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setSourceTabPosition(position);
            popUpCameraSettingsViewModel.setSourceModeSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setOpsinClockMode((byte) SCCPConstants.SCCP_CLOCK_MODE.GPS.getValue());
            } else {
                tcpConnectionViewModel.setOpsinClockMode((byte) SCCPConstants.SCCP_CLOCK_MODE.SYSTEM.getValue());
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setClockFormatTabPosition(position);
            popUpCameraSettingsViewModel.setClockFormatSelected(true);
            if (position == 0) {
                isTimeFormat24Hrs = false;
            } else {
                isTimeFormat24Hrs = true;
            }
            // here update time format
            updateTimeFormatInUI();
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setMetadataTabPosition(position);
            popUpCameraSettingsViewModel.setMetadataSelected(true);
            if (position == 0) {
                tcpConnectionViewModel.setOpsinMetadata(SCCPConstants.SCCP_OPSIN_GPS_STATE.ENABLED.getValue());
            } else {
                tcpConnectionViewModel.setOpsinMetadata(SCCPConstants.SCCP_OPSIN_GPS_STATE.DISABLED.getValue());
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
            int position = tab.getPosition();
            popUpCameraSettingsViewModel.setDateTimeModePosition(position);
            popUpCameraSettingsViewModel.setDateTimeModeSelected(true);
            if (position == 0) {
                // manual
                popUpTimeZoneLayoutBinding.opsinDateTimeManualMode.setVisibility(View.VISIBLE);
                popUpTimeZoneLayoutBinding.opsinDateTimeAutomaticMode.setVisibility(View.GONE);
                popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.MANUAL;
                checkRTCSupportFromVersion();
            } else {
                // automatic
                popUpTimeZoneLayoutBinding.opsinDateTimeManualMode.setVisibility(View.GONE);
                popUpTimeZoneLayoutBinding.opsinDateTimeAutomaticMode.setVisibility(View.VISIBLE);
                setAutomaticModeDateAndTime();
                popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.AUTOMATIC;
                resetPickersViewValues();
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
    };

    private void avoidDoubleClicks(final View view) {
        final long DELAY_IN_MS = 900;
        if (!view.isClickable()) {
            return;
        }
        view.setClickable(false);
        view.postDelayed(() -> view.setClickable(true), DELAY_IN_MS);
    }

    private void initNightWaveUI() {
        binding.opsinUiLayout.setVisibility(View.GONE);
        binding.nightWaveLayout.setVisibility(View.VISIBLE);

        binding.batteryModeIcon.setVisibility(View.GONE);
        binding.tabBatteryMode.setVisibility(View.GONE);

        binding.tabIrcut.addTab(binding.tabIrcut.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabIrcut.addTab(binding.tabIrcut.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabIrcut.addTab(binding.tabIrcut.newTab().setText(requireContext().getString(R.string.auto)));
        binding.tabIrcut.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabIrcut.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.tabInvertVideo.addTab(binding.tabInvertVideo.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabInvertVideo.addTab(binding.tabInvertVideo.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabInvertVideo.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabInvertVideo.setTabGravity(TabLayout.GRAVITY_FILL);


        binding.tabFlipVideo.addTab(binding.tabFlipVideo.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabFlipVideo.addTab(binding.tabFlipVideo.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabFlipVideo.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabFlipVideo.setTabGravity(TabLayout.GRAVITY_FILL);


        binding.tabLed.addTab(binding.tabLed.newTab().setText(requireContext().getString(R.string.on)));
        binding.tabLed.addTab(binding.tabLed.newTab().setText(requireContext().getString(R.string.off)));
        binding.tabLed.setSelectedTabIndicator(R.drawable.thumb_selector);
        binding.tabLed.setTabGravity(TabLayout.GRAVITY_FILL);

        binding.tabIrcut.addOnTabSelectedListener(tabIrCut);
        binding.tabInvertVideo.addOnTabSelectedListener(tabInvertVideo);
        binding.tabFlipVideo.addOnTabSelectedListener(tabFlipVideo);
        binding.tabLed.addOnTabSelectedListener(tabLed);

        // new firmware only show video mode settings
        if (CameraViewModel.hasNewFirmware() && !cameraViewModel.isAnalogMode()) {
            binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.usb)));
            binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.wifi)));
            binding.tabVideoMode.setSelectedTabIndicator(R.drawable.thumb_selector);
            binding.tabVideoMode.setTabGravity(TabLayout.GRAVITY_FILL);
            binding.tabVideoMode.addOnTabSelectedListener(tabVideoMode);

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
            popUpCameraSettingsViewModel.setHasShowProgress(false);
            binding.tabBatteryMode.setVisibility(View.GONE);
            binding.batteryModeIcon.setVisibility(View.GONE);
            if (CameraViewModel.hasNewFirmware() && cameraViewModel.isAnalogMode()) {
                // below changes for analog mode grey out video mode switch
                binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.usb)));
                binding.tabVideoMode.addTab(binding.tabVideoMode.newTab().setText(requireContext().getString(R.string.wifi)));
                binding.tabVideoMode.setSelectedTabIndicator(R.drawable.thumb_selector_greyout);
                binding.tabVideoMode.setSelectedTabIndicatorColor(getResources().getColor(R.color.light_gray, null));
                binding.tabVideoMode.setTabGravity(TabLayout.GRAVITY_FILL);
                binding.tabVideoMode.removeOnTabSelectedListener(tabVideoMode);

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
                popUpCameraSettingsViewModel.setHasShowProgress(false);
            }
        }
        binding.batteryModeIcon.setVisibility(View.GONE);
        binding.tabBatteryMode.setVisibility(View.GONE);


        //Nightwave
        /*
        observe should be not called again when please wait progress shown even in screen rotate
         */
        if (!PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
            tcpConnectionViewModel.observeLedEnableState().observe(lifecycleOwner, ledEnableState);

            tcpConnectionViewModel.observeFlipVideo().observe(lifecycleOwner, flipVideo);

            tcpConnectionViewModel.observeIRCut().observe(lifecycleOwner, irCutObserver);

            tcpConnectionViewModel.observeInvertVideo().observe(lifecycleOwner, invertVideo);
            tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
        }
        popUpCameraSettingsViewModel.observeHasShowVideoModeWifiDialog().observe(this.getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.d(TAG, "observeHasShowVideoModeWifiDialog: true");
                // sent reset fpga command
                popUpCameraSettingsViewModel.setVideoModeSelected(false);
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
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                AddAllTabs();
            } else {
                Log.d(TAG, "observeHasShowVideoModeWifiDialog: false");
                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                CameraViewModel.setIsLiveScreenVideoModeUVC(true);
                int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                popUpCameraSettingsViewModel.setVideoModeSelected(false);
                try {
                    if (tab_position != 0) {
                        binding.tabVideoMode.getTabAt(0).select();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                AddAllTabs();
            }
            popUpCameraSettingsViewModel.setHasShowProgress(false);
        }));

        popUpCameraSettingsViewModel.observeHasShowVideoModeUSBDialog().observe(this.getViewLifecycleOwner(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.d(TAG, "observeHasShowVideoModeUSBDialog: true");
                // sent reset fpga command
                popUpCameraSettingsViewModel.setVideoModeSelected(false);
                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
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
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                AddAllTabs();
            } else {
                CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(false);
                Log.d(TAG, "observeHasShowVideoModeUSBDialog: false");
                int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                popUpCameraSettingsViewModel.setVideoModeSelected(false);
                try {
                    if (tab_position != 1) {
                        binding.tabVideoMode.getTabAt(1).select();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                AddAllTabs();
            }
            popUpCameraSettingsViewModel.setHasShowProgress(false);
        }));

        /*
        dismiss custom progress dialog while BG to FG
        */
        popUpCameraSettingsViewModel.observeHasDismissCustomProgressDialog().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.d(TAG, "observeHasDismissCustomProgressDialog: " + aBoolean);
            if (aBoolean) {
                hideWiFiVideoModeProgressbar();
                removeObservers();
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                popUpCameraSettingsViewModel.setDismissCustomProgressDialog(false);
            }
        }));

        /*
        check whether socket connected or not, if connected the progress bar should dismissed
        */
        tcpConnectionViewModel.isSocketConnected().observe(lifecycleOwner, integer -> {
            if (integer == Constants.STATE_CONNECTED) {
                popUpCameraSettingsViewModel.setHasShowProgress(false);
                Log.i(TAG, "onChanged: STATE_CONNECTED ");
            } else if (integer == Constants.STATE_DISCONNECTED) {
                new Handler().postDelayed(() -> {
                    Log.i(TAG, "onChanged: STATE_DISCONNECTED ");
                    popUpCameraSettingsViewModel.setHasShowProgress(false);
                    if (binding.customProgressBar.isShown()) {
                        popUpCameraSettingsViewModel.setHasShowProgress(false);
                        // need to show wifi connection lost popup (mainActivity) here if ,ore than a minuted can't able to observe the value
                    }
                }, 5000);
            }
        });
        // for this observe if failed to call video mode observer call this observer
        popUpCameraSettingsViewModel.isUpdateVideoModeState.observe(lifecycleOwner, new EventObserver<>(object -> {
            if (object != null) {
                if (object instanceof CommandError) {
                    popUpCameraSettingsViewModel.setVideoModeSelected(false);
                    Log.e(TAG, "isUpdateVideoModeState Error: " + ((CommandError) object).getError());
                    popUpCameraSettingsViewModel.setHasShowProgress(false);
                    responseFailed();
                } else {
                    int response = (int) object;
                    Log.d(TAG, "isUpdateVideoModeState: " + object);
                    popUpCameraSettingsViewModel.setHasShowProgress(false);
            /*
            this condition for avoid recall the observe class while confiramtion dialog shown
            */
                    if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                        popUpCameraSettingsViewModel.setVideoModePosition(response);
                        popUpCameraSettingsViewModel.setVideoModeSelected(false);
                        if (binding.tabVideoMode.getTabAt(response) != null) {
                            binding.tabVideoMode.getTabAt(response).select();
                        }
                        if (response == SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                            // here show please wait dialog 20sec and dismiss
                            if (!CameraViewModel.isIsLiveScreenVideoModeWIFI() && CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                                Log.d(TAG, "isUpdateVideoModeState : Already in USB mode");
                            } else if (!CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                                popUpCameraSettingsViewModel.setVideoModeSelected(false);
                                tcpConnectionViewModel.resetFPGA(false);
                                showWiFiVideoModeProgressbar();
                                int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                                if (tab_position != 1) {
                                    binding.tabVideoMode.getTabAt(0).select();
                                }
                                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                                CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                                CameraViewModel.setIsLiveScreenVideoModeUVC(true);

                                new Handler().postDelayed(this::hideWiFiVideoModeProgressbar, 20000);// wait for 20 secs*/
                                Log.d(TAG, "isUpdateVideoModeState : UVC ");
                            }
                        } else if (response == SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                            // here show please wait dialog 20sec and start live view
                            if (CameraViewModel.isIsLiveScreenVideoModeWIFI() && !CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                                Log.d(TAG, "isUpdateVideoModeState : Already in WIFI video mode");
                            } else if (CameraSettingsViewModel.isIsSettingsScreenVideoModeUVC()) {
                                popUpCameraSettingsViewModel.setVideoModeSelected(false);
                                tcpConnectionViewModel.resetFPGA(false);
                                showWiFiVideoModeProgressbar();
                                int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                                if (tab_position != 0) {
                                    binding.tabVideoMode.getTabAt(1).select();
                                }
                                CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(false);
                                CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                                new Handler().postDelayed(this::hideWiFiVideoModeProgressbar, 20000);// wait for 20 secs
                                Log.d(TAG, "isUpdateVideoModeState : WIFI");
                            }
                        }
                    }
                }
            }
        }));
    }

    private void AddAllTabs() {
        binding.tabIrcut.addOnTabSelectedListener(tabIrCut);
        binding.tabInvertVideo.addOnTabSelectedListener(tabInvertVideo);
        binding.tabFlipVideo.addOnTabSelectedListener(tabFlipVideo);
        binding.tabLed.addOnTabSelectedListener(tabLed);
        binding.tabVideoMode.addOnTabSelectedListener(tabVideoMode);
    }

    private final EventObserver<Object> observeCameraVideoMode = new EventObserver<>(object -> {
        if (object instanceof CommandError) {
            popUpCameraSettingsViewModel.setVideoModeSelected(false);
            Log.e(TAG, "observeCameraVideoMode Error: " + ((CommandError) object).getError());
            popUpCameraSettingsViewModel.setHasShowProgress(false);
            responseFailed();
        } else {
            int response = (int) object;
            Log.d(TAG, "observeCameraVideoMode: " + object);
            popUpCameraSettingsViewModel.setHasShowProgress(false);
            /*
            this condition for avoid recall the observe class while confiramtion dialog shown
            */
            if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                popUpCameraSettingsViewModel.setVideoModePosition(response);
                popUpCameraSettingsViewModel.setVideoModeSelected(false);
                if (binding.tabVideoMode.getTabAt(response) != null) {
                    binding.tabVideoMode.getTabAt(response).select();
                }
                if (response == SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                    // here show please wait dialog 20sec and dismiss
                    if (!CameraViewModel.isIsLiveScreenVideoModeWIFI() && CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                        Log.d(TAG, "observeCameraVideoMode : Already in USB mode");
                    } else if (!CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        popUpCameraSettingsViewModel.setVideoModeSelected(false);
                        tcpConnectionViewModel.resetFPGA(false);
                        showWiFiVideoModeProgressbar();
                        int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                        if (tab_position != 1) {
                            binding.tabVideoMode.getTabAt(0).select();
                        }
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(true);
                        CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                        CameraViewModel.setIsLiveScreenVideoModeUVC(true);

                        new Handler().postDelayed(this::hideWiFiVideoModeProgressbar, 20000);// wait for 20 secs*/
                        Log.d(TAG, "observeCameraVideoMode : UVC ");
                    }
                } else if (response == SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                    // here show please wait dialog 20sec and start live view
                    if (CameraViewModel.isIsLiveScreenVideoModeWIFI() && !CameraViewModel.isIsLiveScreenVideoModeUVC()) {
                        CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                        Log.d(TAG, "observeCameraVideoMode : Already in WIFI video mode");
                    } else if (CameraSettingsViewModel.isIsSettingsScreenVideoModeUVC()) {
                        popUpCameraSettingsViewModel.setVideoModeSelected(false);
                        tcpConnectionViewModel.resetFPGA(false);
                        showWiFiVideoModeProgressbar();
                        int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                        if (tab_position != 0) {
                            binding.tabVideoMode.getTabAt(1).select();
                        }
                        CameraSettingsViewModel.setIsSettingsScreenVideoModeUVC(false);
                        CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                        CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                        new Handler().postDelayed(this::hideWiFiVideoModeProgressbar, 20000);// wait for 20 secs
                        Log.d(TAG, "observeCameraVideoMode : WIFI");
                    }
                }
            }
        }
    });

    /*
    sometimes respond failed from camera (obsercameravideomode)
    so that the mode position revert to old mode position
     */
    private void responseFailed() {
        Log.e(TAG, "observe ui_tab " + binding.tabVideoMode.getSelectedTabPosition() + " mode_tab " + popUpCameraSettingsViewModel.getVideoModePosition());
        if (!PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog() && !popUpCameraSettingsViewModel.isConfirmationDialogShown()) {
            if (/*binding.tabVideoMode.getSelectedTabPosition()*/ popUpCameraSettingsViewModel.getVideoModePosition() == 1) {
                popUpCameraSettingsViewModel.hasShowVideoModeWifiDialog(false);
            } else {
                popUpCameraSettingsViewModel.hasShowVideoModeUSBDialog(false);
            }
        }
    }

    private void showWifiModeConfirmationDialog(String message, int mode) {
        if (!PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog() && screenType == POP_UP_SETTINGS_SCREEN) {
            MainActivity activity = ((MainActivity) getActivity());
            if (activity != null) {
                if (mode == 1) activity.showDialog = MainActivity.ShowDialog.WIFI_VIDEO_MODE_DIALOG;
                else activity.showDialog = MainActivity.ShowDialog.WIFI_TO_USB_VIDEO_MODE_DIALOG;

                activity.showDialog("", message, null);
            }
            popUpCameraSettingsViewModel.setConfirmationDialogShown(true);
        }
    }

    private void hideWiFiVideoModeProgressbar() {
        try {
            popUpCameraSettingsViewModel.setHasShowProgress(false);
            PopUpCameraSettingsViewModel.setHasDismissPleaseWaitProgressDialog(false);
            if (isAdded()) {
                if (popUpCameraSettingsViewModel.getPleaseWaitAlertDialog() != null && popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().isShowing() && PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().show();
                } else {
                    popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().dismiss();
                }
            } else {
                Log.e(TAG, "run: hideWiFiVideoModeProgressbar: >>>>> ");
                if (popUpCameraSettingsViewModel.getPleaseWaitAlertDialog() != null && popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().isShowing() && PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().show();
                } else {
                    popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().dismiss();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWiFiVideoModeProgressbar() {
        try {
            popUpCameraSettingsViewModel.setHasShowProgress(false);
            PopUpCameraSettingsViewModel.setHasDismissPleaseWaitProgressDialog(true);

            ConstraintLayout constraintLayout = new ConstraintLayout(requireContext());
            ConstraintLayout.LayoutParams textViewParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textViewParams.bottomToBottom = 0;
            textViewParams.topToTop = 0;
            textViewParams.startToStart = 0;
            textViewParams.endToEnd = 0;

            ConstraintLayout.LayoutParams progressbarParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            wiFiDisconnectProgressbar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyle);
            progressbarParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

            popUpCameraSettingsViewModel.setPleaseWaitAlertDialog(builder.create());//pleaseWaitAlertDialog = builder.create();
            popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().show();

            Window window = popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().getWindow();
            if (window != null) {
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().getWindow().getAttributes());
                layoutParams.width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().getWindow().setAttributes(layoutParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*Opsin*/
    private final Observer<Object> nucState = new Observer<Object>() {
        @Override
        public void onChanged(Object value) {
            if (value != null) {
                if (value instanceof CommandError) {
                    Log.e(TAG, "observeOpsinGetNUCState: " + ((CommandError) value).getError());
                } else {
                    Log.d(TAG, "observeOpsinGetNUCState : onChanged: " + CameraViewModel.isOpsinCommandInitiateByDialogFragment());
                    switch ((int) value) {
                        case 0: // disabled -- No icon
                        case 2: // disabled and operationally active. This is a error state and should not happen -- no icon
                            Log.d(TAG, "observeOpsinGetNUCState : onChanged: " + binding.opsinTabNuc.getSelectedTabPosition() + " " + value + " " + popUpCameraSettingsViewModel.isMicSelected());
                            binding.opsinTabNuc.removeOnTabSelectedListener(opsinTabNuc);
                            popUpCameraSettingsViewModel.setNucSelected(false);

                            binding.opsinTabNuc.post(() -> {
                                TabLayout.Tab tab = binding.opsinTabNuc.getTabAt(1);
                                if (tab != null) {
                                    tab.select();
                                }
                                binding.opsinTabNuc.addOnTabSelectedListener(opsinTabNuc);
                            });
//                            binding.opsinTabNuc.getTabAt(1).select();
                            break;
                        case 1: // enabled and operationally inactive -- Red
                        case 3: //enabled and operationally active -- green
                            Log.d(TAG, "observeOpsinGetNUCState : onChanged: " + binding.opsinTabNuc.getSelectedTabPosition() + " " + value + " " + popUpCameraSettingsViewModel.isMicSelected());
                            binding.opsinTabNuc.removeOnTabSelectedListener(opsinTabNuc);
                            popUpCameraSettingsViewModel.setNucSelected(false);

                            binding.opsinTabNuc.post(() -> {
                                TabLayout.Tab tab = binding.opsinTabNuc.getTabAt(0);
                                if (tab != null) {
                                    tab.select();
                                }
                                binding.opsinTabNuc.addOnTabSelectedListener(opsinTabNuc);
                            });

//                            binding.opsinTabNuc.getTabAt(0).select();

                            break;
                    }
                    Log.e(TAG, "observeOpsinGetNUCState: " + value + " / " + popUpCameraSettingsViewModel.getNucTabPosition());
                }
                if (popUpCameraSettingsViewModel.isMicSelected()) {
                    tcpConnectionViewModel.getMicState();
                }
            }
        }
    };

    private final EventObserver<Object> micState = new EventObserver<>(aBoolean -> {
        if (aBoolean != null) {
            if (aBoolean instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetMICState: " + ((CommandError) aBoolean).getError());
            } else {
                int value;
                if ((byte) aBoolean == SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue()) {
                    value = 0;
                    binding.opsinTabMic.removeOnTabSelectedListener(opsinTabMic);
                    popUpCameraSettingsViewModel.setMicSelected(false);
                    binding.opsinTabMic.post(new Runnable() {
                        @Override
                        public void run() {
                            TabLayout.Tab tab = binding.opsinTabMic.getTabAt(0);
                            if (tab != null) {
                                tab.select();
                            }
                            binding.opsinTabMic.addOnTabSelectedListener(opsinTabMic);
                        }
                    });
                } else if ((byte) aBoolean == SCCPConstants.SCCP_OPSIN_MIC_STATE.DISABLED.getValue()) {
                    value = 1;
                    binding.opsinTabMic.removeOnTabSelectedListener(opsinTabMic);
                    popUpCameraSettingsViewModel.setMicSelected(false);
                    binding.opsinTabMic.post(new Runnable() {
                        @Override
                        public void run() {
                            TabLayout.Tab tab = binding.opsinTabMic.getTabAt(1);
                            if (tab != null) {
                                tab.select();
                            }
                            binding.opsinTabMic.addOnTabSelectedListener(opsinTabMic);
                        }
                    });
                } else {
                    value = 1;
                }
                popUpCameraSettingsViewModel.setMicTabPosition(value);

                Log.e(TAG, "observeOpsinCameraMICState : " + binding.opsinTabMic.getSelectedTabPosition() + " " + value + " / " + popUpCameraSettingsViewModel.isJpegCompressSelected());
//                binding.opsinTabMic.getTabAt(value).select();
            }
            if (popUpCameraSettingsViewModel.isJpegCompressSelected()) {
                tcpConnectionViewModel.getFrameRate();
            }
        }
    });

    private final EventObserver<Object> frameRate = new EventObserver<>(aBoolean -> {
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
                popUpCameraSettingsViewModel.setFpsTabPosition(value);
                Log.e(TAG, "observeOpsinGetFrameRate: " + aBoolean + " " + value + " " + popUpCameraSettingsViewModel.isUtcSelected());

                binding.opsinTabFps.removeOnTabSelectedListener(opsinTabFps);
                popUpCameraSettingsViewModel.setFpsSelected(false);
                binding.opsinTabFps.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabFps.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabFps.addOnTabSelectedListener(opsinTabFps);
                    }
                });
//                binding.opsinTabFps.getTabAt(value).select();

            }
            /* if (popUpCameraSettingsViewModel.isUtcSelected()) {
             *//*tcpConnectionViewModel.getTimeZone();*//*
            }*/
            if (popUpCameraSettingsViewModel.isGpsSelected()) {
                tcpConnectionViewModel.getGpsInfo();
            }

        }
    });

  /*  private final EventObserver<Object> timeZone = new EventObserver<>(object -> {
        if (object != null) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetTimeZone: " + ((CommandError) object).getError());
            } else {
                int value;
                if (object.equals("0:0")) {
                    value = 0;
                } else {
                    value = 1;
                }
                popUpCameraSettingsViewModel.setUtcTabPosition(value);
                Log.e(TAG, "observeOpsinGetTimeZone: " + object + " " + value);

                binding.opsinTabUtc.removeOnTabSelectedListener(opsinTabUtc);
                popUpCameraSettingsViewModel.setUtcSelected(false);
                binding.opsinTabUtc.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabUtc.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabUtc.addOnTabSelectedListener(opsinTabUtc);
                    }
                });
//                binding.opsinTabUtc.getTabAt(value).select();
            }
            if (popUpCameraSettingsViewModel.isGpsSelected()) {
                tcpConnectionViewModel.getGpsInfo();
            }

        }
    });*/

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
                popUpCameraSettingsViewModel.setGpsTabPosition(value);

                binding.opsinTabGps.removeOnTabSelectedListener(opsinTabGps);
                popUpCameraSettingsViewModel.setGpsSelected(false);
                binding.opsinTabGps.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabGps.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabGps.addOnTabSelectedListener(opsinTabGps);
                    }
                });
                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    if (popUpCameraSettingsViewModel.isMonochromeSelected()) {
                        tcpConnectionViewModel.getOpsinMonochromaticState();
                    }
                } else {
                    popUpCameraSettingsViewModel.setHasShowProgress(false);
                }
                Log.e(TAG, "observeOpsinGetGpsInfo : " + o);
            }
        }
    });

    private final EventObserver<Object> observeMonochrome = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeMonochrome: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                popUpCameraSettingsViewModel.setMonochromeTabPosition(value);

                binding.opsinTabMonochrome.removeOnTabSelectedListener(opsinTabMonochrome);
                popUpCameraSettingsViewModel.setMonochromeSelected(false);
                binding.opsinTabMonochrome.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabMonochrome.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabMonochrome.addOnTabSelectedListener(opsinTabMonochrome);
                    }
                });
                if (popUpCameraSettingsViewModel.isNoiseSelected()) {
                    tcpConnectionViewModel.getOpsinNoiseReductionState();
                }
                Log.e(TAG, "observeMonochrome : " + o);
            }
        }
    });

    private final EventObserver<Object> observeNoiseMode = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeNoiseMode: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                popUpCameraSettingsViewModel.setNoiseTabPosition(value);

                binding.opsinTabNoise.removeOnTabSelectedListener(opsinTabNoise);
                popUpCameraSettingsViewModel.setNoiseSelected(false);
                binding.opsinTabNoise.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabNoise.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabNoise.addOnTabSelectedListener(opsinTabNoise);
                    }
                });
                if (popUpCameraSettingsViewModel.isRoiModeSelected()) {
                    tcpConnectionViewModel.getOpsinROI();
                }
                Log.e(TAG, "observeNoiseMode : " + o);
            }
        }
    });

    private final EventObserver<Object> observeRoiMode = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeRoiMode: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue()) {
                    value = 0;
                } else if ((byte) o == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue()) {
                    value = 1;
                } else {
                    value = 2;
                }
                popUpCameraSettingsViewModel.setRoiTabPosition(value);

                binding.opsinTabRoi.removeOnTabSelectedListener(opsinTabRoi);
                popUpCameraSettingsViewModel.setRoiModeSelected(false);
                binding.opsinTabRoi.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabRoi.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabRoi.addOnTabSelectedListener(opsinTabRoi);
                    }
                });
                if (popUpCameraSettingsViewModel.isSourceModeSelected()) {
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        tcpConnectionViewModel.getOpsinClockMode();
                    } else {
                        cameraViewModel.hasShowSettingsDialog(false);
                    }
                }
                Log.e(TAG, "observeRoiMode : " + o);
            }
        }
    });

    private final EventObserver<Object> observeSourceMode = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeSourceMode: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((boolean) o) {
                    value = 1;
                } else {
                    value = 0;
                }
                popUpCameraSettingsViewModel.setSourceTabPosition(value);

                binding.opsinTabSource.removeOnTabSelectedListener(opsinTabSourceMode);
                popUpCameraSettingsViewModel.setSourceModeSelected(false);
                binding.opsinTabSource.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabSource.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabSource.addOnTabSelectedListener(opsinTabSourceMode);
                    }
                });
                if (popUpCameraSettingsViewModel.isMetadataSelected()) {
                    tcpConnectionViewModel.getOpsinMetadata();
                }
                Log.e(TAG, "observeSourceMode : " + o);
            }
        }
    });

    private final EventObserver<Object> observeOpsinGetTimeFormat = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeClockFormat: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_24.getValue()) {
                    value = 1;
                    if (!popUpCameraSettingsViewModel.isShowTimePicker())
                        isTimeFormat24Hrs = true;
                } else {
                    value = 0;
                    if (!popUpCameraSettingsViewModel.isShowTimePicker())
                        isTimeFormat24Hrs = false;
                }
                popUpCameraSettingsViewModel.setClockFormatTabPosition(value);
                popUpTimeZoneLayoutBinding.opsinTabClockFormat.removeOnTabSelectedListener(opsinTabClockFormat);
                popUpCameraSettingsViewModel.setClockFormatSelected(false);
                popUpTimeZoneLayoutBinding.opsinTabClockFormat.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = popUpTimeZoneLayoutBinding.opsinTabClockFormat.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                            updateTimeFormatInUI();
                        }
                        popUpTimeZoneLayoutBinding.opsinTabClockFormat.addOnTabSelectedListener(opsinTabClockFormat);
                    }
                });
                popUpCameraSettingsViewModel.setHasShowProgress(false);
                Log.e(TAG, "observeClockFormat : " + o);
            }
        }
    });

    private void updateTimeFormatInUI() {
        try {
            String formattedTime = simpleTimeFormat().format(selectedDateTime.getTime());
            String getTime = popUpTimeZoneLayoutBinding.opsinSelectTime.getText().toString();
            int clockFormatTabPosition = popUpCameraSettingsViewModel.getClockFormatTabPosition();
            if (!getTime.isEmpty()) {
                if (clockFormatTabPosition == 0) {
                    popUpTimeZoneLayoutBinding.opsinSelectTime.setText(convert24to12Hrs(formattedTime));
                } else {
                    String formatted24HrTime = simple24HourTimeFormat().format(selectedDateTime.getTime());
                    popUpTimeZoneLayoutBinding.opsinSelectTime.setText(formatted24HrTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final EventObserver<Object> observeMetadata = new EventObserver<>(o -> {
        if (o != null) {
            if (o instanceof CommandError) {
                Log.e(TAG, "observeMetadata: " + ((CommandError) o).getError());
            } else {
                int value;
                if ((byte) o == SCCPConstants.SCCP_OPSIN_METADATA_STATE.DISABLED.getValue()) {
                    value = 1;
                } else {
                    value = 0;
                }
                popUpCameraSettingsViewModel.setMetadataTabPosition(value);

                binding.opsinTabMetadata.removeOnTabSelectedListener(opsinTabMetadata);
                popUpCameraSettingsViewModel.setMetadataSelected(false);
                binding.opsinTabMetadata.post(new Runnable() {
                    @Override
                    public void run() {
                        TabLayout.Tab tab = binding.opsinTabMetadata.getTabAt(value);
                        if (tab != null) {
                            tab.select();
                        }
                        binding.opsinTabMetadata.addOnTabSelectedListener(opsinTabMetadata);
                    }
                });
                Log.e(TAG, "observeMetadata : " + o);
                checkRTCSupportFromVersion();
            }
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
            if (popUpCameraSettingsViewModel.rtcMode == PopUpCameraSettingsViewModel.RTC_MODE.MANUAL) {
                popUpCameraSettingsViewModel.setHasShowProgress(true);
                tcpConnectionViewModel.getTimeZone();
            } else {
                popUpCameraSettingsViewModel.setHasShowProgress(false);
            }
        } else {
            popUpCameraSettingsViewModel.setHasShowProgress(false);
        }
    }


    private final EventObserver<Object> getTimeZone = new EventObserver<>(object -> {
        if (object != null) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeOpsinGetTimeZone: " + ((CommandError) object).getError());
            } else {
                Log.e(TAG, "Timezone From Camera : " + object);
                /*Select Dropdown based on the Timezone value*/
                if (!isShowTimeZoneLayout() && !isSelectedTimeZone()) {
                    binding.popTimeZoneLayout.opsinSelectTimeZone.setText(String.format("GMT%s", object));
                } else {
                    // for this if selected timezone list any one item
                    if (isSelectedTimeZone()) {
                        setShowTimeZoneLayout(false);
                        setSelectedTimeZone(false);
                        // for this rotate device after select time zone text view not update to show 'select time zone' text so avoid use this usecase
                        try {
                            if (split[1] != null) {
                                binding.popTimeZoneLayout.opsinSelectTimeZone.setText(split[1]);
                            } else {
                                binding.popTimeZoneLayout.opsinSelectTimeZone.setText(String.format("GMT%s", object));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            binding.popTimeZoneLayout.opsinSelectTimeZone.setText(String.format("GMT%s", object));
                        }
                    }
                }
                tcpConnectionViewModel.getDateTime();
            }
        }
    });

    private final EventObserver<Object> setTimeZone = new EventObserver<>(o -> {
        if (o != null) {
            Log.e(TAG, "observeOpsinSetTimeZone : " + o);
            if (popUpCameraSettingsViewModel.rtcMode == PopUpCameraSettingsViewModel.RTC_MODE.AUTOMATIC) {
                /*Sync with Local Date Time*/
                syncLocalDateTime();
            } else {
                /*Response for Manual Set Timezone*/
                /*Update UI*/
                if (selectedDateTime != null) {
                    Date dateTime = selectedDateTime.getTime();
                    SimpleDateFormat sourceFormat = new SimpleDateFormat("d/M/yy H:m:s", Locale.getDefault(Locale.Category.FORMAT));
                    String sourceDateTime = sourceFormat.format(dateTime);
                    CharSequence cameraTimeZone = binding.popTimeZoneLayout.opsinSelectTimeZone.getText();
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
                        selectedDateTime.set(Calendar.MONTH, month - 1);
                        selectedDateTime.set(Calendar.YEAR, year);
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                        selectedDateTime.set(Calendar.MINUTE, minutes);
                        selectedDateTime.set(Calendar.SECOND, seconds);
                        Log.e(TAG, "GetDateTime: " + object);

                        popUpTimeZoneLayoutBinding.opsinSelectDate.setText(simpleDateFormat().format(objDate));
                        popUpTimeZoneLayoutBinding.opsinSelectTime.setText(simpleTimeFormat().format(objDate));
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
                if (popUpCameraSettingsViewModel.rtcMode == PopUpCameraSettingsViewModel.RTC_MODE.AUTOMATIC) {
                    /*Response for Automatic Set Timezone*/
                    popUpCameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                    /*here check local time format*/
                    if (!isTimeFormat24Hr(requireContext())) {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_12.getValue());
                    } else {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_24.getValue());
                    }
                } else {
                    /*Response for Manual Set Timezone*/
                    /*Dismiss progress*/
                    popUpCameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
                    int clockFormatTabPosition = popUpCameraSettingsViewModel.getClockFormatTabPosition();
                    Log.d(TAG, "clock_Format_position:" + clockFormatTabPosition);
                    if (clockFormatTabPosition == 0) {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_12.getValue());
                    } else {
                        tcpConnectionViewModel.setOpsinTimeFormat(SCCPConstants.SCCP_OPSIN_CLOCK_FORMAT_STATE.MODE_24.getValue());
                    }
                }
                resetPickersViewValues();
                Toast.makeText(requireContext(), "Successfully set date and time", Toast.LENGTH_SHORT).show();
            }
        }
    });

    private final Observer<Object> irCutObserver = aInteger -> {
        if (aInteger instanceof CommandError) {
            Log.e(TAG, "observeIRCut: " + ((CommandError) aInteger).getError());
        } else {
            int tab_position = popUpCameraSettingsViewModel.getIrCutTabPosition();
            Integer value = (Integer) aInteger;
            if (value == 17) value = 1;
            if (value == 18) value = 2;
            Log.e(TAG, "observeIRCut: " + value + " " + tab_position);
//            if (tab_position != value && value < 3) {
            if (binding.tabIrcut.getTabAt(value) != null) {
                binding.tabIrcut.getTabAt(value).select();
            }
        }
        popUpCameraSettingsViewModel.setIRCutSelected(false);
    };

    private final Observer<Object> flipVideo = aBoolean -> {
        if (aBoolean instanceof CommandError) {
            Log.e(TAG, "observeInvertImage: " + ((CommandError) aBoolean).getError());
        } else {
            int tab_position = popUpCameraSettingsViewModel.getFlipVideoTabPosition();

            int value = (Boolean) aBoolean ? 1 : 0;
            Log.e(TAG, "observeInvertImage: " + aBoolean + " " + value + " " + tab_position);
//            if (tab_position != value) {
            if (binding.tabFlipVideo.getTabAt(value) != null) {
                binding.tabFlipVideo.getTabAt(value).select();
            }
        }
        popUpCameraSettingsViewModel.setFlipVideoSelected(false);
    };

    private final Observer<Object> invertVideo = aBoolean -> {
        if (aBoolean instanceof CommandError) {
            Log.e(TAG, "observeFlipImage: " + ((CommandError) aBoolean).getError());
        } else {
            int tab_position = popUpCameraSettingsViewModel.getInvertVideoTabPosition();
            int value = (Boolean) aBoolean ? 0 : 1;
            Log.e(TAG, "observeFlipImage: " + aBoolean + " " + value);
//            if (tab_position != value) {
            if (binding.tabInvertVideo.getTabAt(value) != null) {
                binding.tabInvertVideo.getTabAt(value).select();
            }
        }
        popUpCameraSettingsViewModel.setInvertVideoSelected(false);
    };

    private final Observer<Object> ledEnableState = aBoolean -> {
        if (aBoolean instanceof CommandError) {
            Log.e(TAG, "observeLedEnableState: " + ((CommandError) aBoolean).getError());
        } else {
            int tab_position = popUpCameraSettingsViewModel.getLedTabPosition();
            int value = (Boolean) aBoolean ? 0 : 1;
            Log.e(TAG, "observeLedEnableState: " + aBoolean + " " + value);
//            if (tab_position != value) {
            if (binding.tabLed.getTabAt(value) != null) {
                binding.tabLed.getTabAt(value).select();
            }
        }
        popUpCameraSettingsViewModel.setLedSelected(false);
    };

    private void removeObservers() {
        //nightwave
        tcpConnectionViewModel.observeIRCut().removeObserver(irCutObserver);
        tcpConnectionViewModel.observeIRCut().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeFlipVideo().removeObserver(flipVideo);
        tcpConnectionViewModel.observeFlipVideo().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeInvertVideo().removeObserver(invertVideo);
        tcpConnectionViewModel.observeInvertVideo().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeLedEnableState().removeObserver(ledEnableState);
        tcpConnectionViewModel.observeLedEnableState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
        tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
        Log.d(TAG, "onDetach: ");

        //Opsin
        /*tcpConnectionViewModel.observeOpsinCameraClockMode().removeObserver(clockMode);*/
        tcpConnectionViewModel.observeOpsinGetNUCState().removeObserver(nucState);
        tcpConnectionViewModel.observeOpsinGetNUCState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinGetMICState().removeObserver(micState);
        tcpConnectionViewModel.observeOpsinGetMICState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinGetFrameRate().removeObserver(frameRate);
        tcpConnectionViewModel.observeOpsinGetFrameRate().removeObservers(lifecycleOwner);
        // tcpConnectionViewModel.observeOpsinGetTimeZone().removeObserver(timeZone);
        //  tcpConnectionViewModel.observeOpsinGetTimeZone().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinGetGpsInfo().removeObserver(gpsInfo);
        tcpConnectionViewModel.observeOpsinGetGpsInfo().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.getOpsinMonochromeState().removeObserver(observeMonochrome);
        tcpConnectionViewModel.getOpsinMonochromeState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.getOpsinNoiseState().removeObserver(observeNoiseMode);
        tcpConnectionViewModel.getOpsinNoiseState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.getOpsinROIState().removeObserver(observeRoiMode);
        tcpConnectionViewModel.getOpsinROIState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeOpsinCameraClockMode().removeObserver(observeSourceMode);
        tcpConnectionViewModel.observeOpsinCameraClockMode().removeObservers(lifecycleOwner);
//        tcpConnectionViewModel.observeOpsinCameraClockMode().removeObserver(observeClockFormat);
//        tcpConnectionViewModel.observeOpsinCameraClockMode().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.getOpsinMetadataState().removeObserver(observeMetadata);
        tcpConnectionViewModel.getOpsinMetadataState().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().removeObserver(observeOpsinGetTimeFormat);
        tcpConnectionViewModel.observeGetOpsinTimeFormat().removeObservers(lifecycleOwner);


    }

    @Override
    public void onStop() {
        super.onStop();
        //   tcpConnectionViewModel.isSocketConnected().removeObservers(lifecycleOwner);
    }

    private void showOpsinCameraSocketClosedDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_NOT_RESPONSE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    public String getCurrentTimezoneOffset() {
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());
        String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = "GMT" + (offsetInMillis >= 0 ? "+" : "-") + offset;
        return offset;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            isViewShown = true;
            getValues();
        } else {
            isViewShown = false;
            setShowTimeZoneLayout(false);
            popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.MANUAL;
            popUpCameraSettingsViewModel.setDateTimeModePosition(0);
            resetPickersViewValues();
        }
    }

    private void resetPickersViewValues() {
        popUpCameraSettingsViewModel.setShowTimePicker(false);
        popUpCameraSettingsViewModel.setShowDatePicker(false);
        popUpCameraSettingsViewModel.setCameraDate(null);
        popUpCameraSettingsViewModel.setCameraTime(null);
    }

    private void getValues() {
        if (tcpConnectionViewModel != null) {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    Log.d(TAG, "setMenuVisibility: true");
                    try {
                        popUpCameraSettingsViewModel.setHasShowProgress(true);
                        boolean hasLedObserver = tcpConnectionViewModel.observeLedEnableState().hasObservers();
                        boolean hasFlipObserver = tcpConnectionViewModel.observeFlipVideo().hasObservers();
                        boolean hasIrObserver = tcpConnectionViewModel.observeIRCut().hasObservers();
                        boolean hasInvertObserver = tcpConnectionViewModel.observeInvertVideo().hasObservers();
                        boolean hasVideoModeObserver = tcpConnectionViewModel.observeCameraVideoMode().hasObservers();

                    if (!hasLedObserver)
                        tcpConnectionViewModel.observeLedEnableState().observe(lifecycleOwner, ledEnableState);

                    if (!hasFlipObserver)
                        tcpConnectionViewModel.observeFlipVideo().observe(lifecycleOwner, flipVideo);

                    if (!hasIrObserver)
                        tcpConnectionViewModel.observeIRCut().observe(lifecycleOwner, irCutObserver);

                    if (!hasInvertObserver)
                        tcpConnectionViewModel.observeInvertVideo().observe(lifecycleOwner, invertVideo);

                    if (!hasVideoModeObserver) {
                        tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
                    }

                    if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getInvertImage(), 500);
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getFlipImage(), 1000);
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getIRCut(), 1500);
                        //new Handler().postDelayed(() -> tcpConnectionViewModel.getLedEnableState(), 2000);
                    }

                    new Handler().postDelayed(() -> {
                        if (CameraViewModel.hasNewFirmware() && !cameraViewModel.isAnalogMode()) {
                            if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog())
                                tcpConnectionViewModel.getVideoMode();

                            } else {
                                popUpCameraSettingsViewModel.setHasShowProgress(false);
                            }
                        }, 2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case OPSIN:
                    popUpCameraSettingsViewModel.setHasShowProgress(true);
                    /* for this avoid continues observer and get command response*/
                    boolean hasNucObserver = tcpConnectionViewModel.observeOpsinGetNUCState().hasObservers();
                    boolean hasMicObserver = tcpConnectionViewModel.observeOpsinGetMICState().hasObservers();
                    boolean hasFpsObserver = tcpConnectionViewModel.observeOpsinGetFrameRate().hasObservers();
                    //boolean hasTimeZoneObserver = tcpConnectionViewModel.observeOpsinGetTimeZone().hasObservers();
                    boolean hasGpsInfoObserver = tcpConnectionViewModel.observeOpsinGetGpsInfo().hasObservers();
                    boolean hasMonochromeObserver = tcpConnectionViewModel.getOpsinMonochromeState().hasObservers();
                    boolean hasNoiseObserver = tcpConnectionViewModel.getOpsinNoiseState().hasObservers();
                    boolean hasROIObserver = tcpConnectionViewModel.getOpsinROIState().hasObservers();
                    boolean hasSourceObserver = tcpConnectionViewModel.observeOpsinCameraClockMode().hasObservers();
                    boolean hasClockFormatObserver = tcpConnectionViewModel.observeGetOpsinTimeFormat().hasObservers(); // 12hr Or 24 hr

                    boolean hasMetadataObserver = tcpConnectionViewModel.getOpsinMetadataState().hasObservers();

                /*if (!hasClockModeObserver)
                    tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, clockMode);*/
                if (!hasNucObserver)
                    tcpConnectionViewModel.observeOpsinGetNUCState().observe(lifecycleOwner, nucState);
                if (!hasMicObserver)
                    tcpConnectionViewModel.observeOpsinGetMICState().observe(lifecycleOwner, micState);
                if (!hasFpsObserver)
                    tcpConnectionViewModel.observeOpsinGetFrameRate().observe(lifecycleOwner, frameRate);
               /* if (!hasTimeZoneObserver)
                    tcpConnectionViewModel.observeOpsinGetTimeZone().observe(lifecycleOwner, timeZone);*/
                if (!hasGpsInfoObserver)
                    tcpConnectionViewModel.observeOpsinGetGpsInfo().observe(lifecycleOwner, gpsInfo);

                if (!hasMonochromeObserver)
                    tcpConnectionViewModel.getOpsinMonochromeState().observe(lifecycleOwner, observeMonochrome);
                if (!hasNoiseObserver)
                    tcpConnectionViewModel.getOpsinNoiseState().observe(lifecycleOwner, observeNoiseMode);
                if (!hasROIObserver)
                    tcpConnectionViewModel.getOpsinROIState().observe(lifecycleOwner, observeRoiMode);
                if (!hasSourceObserver)
                    tcpConnectionViewModel.observeOpsinCameraClockMode().observe(lifecycleOwner, observeSourceMode);
                if (!hasClockFormatObserver)
                    tcpConnectionViewModel.observeGetOpsinTimeFormat().observe(lifecycleOwner, observeOpsinGetTimeFormat);
                if (!hasMetadataObserver)
                    tcpConnectionViewModel.getOpsinMetadataState().observe(lifecycleOwner, observeMetadata);

                tcpConnectionViewModel.getNUC();
                popUpCameraSettingsViewModel.setNucSelected(true);
                popUpCameraSettingsViewModel.setMicSelected(true);
                popUpCameraSettingsViewModel.setJpegCompressSelected(true);
                popUpCameraSettingsViewModel.setFpsSelected(true);
                popUpCameraSettingsViewModel.setUtcSelected(true);
                popUpCameraSettingsViewModel.setGpsSelected(true);

                    popUpCameraSettingsViewModel.setMonochromeSelected(true);
                    popUpCameraSettingsViewModel.setNoiseSelected(true);
                    popUpCameraSettingsViewModel.setRoiModeSelected(true);
                    popUpCameraSettingsViewModel.setSourceModeSelected(true);
                    popUpCameraSettingsViewModel.setClockFormatSelected(true);
                    popUpCameraSettingsViewModel.setMetadataSelected(true);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"getValues() : NW_Digital");
                    break;
            }
        } else {
            mHandler.postDelayed(this::getValues, 500);
        }
    }

    private void showUtcTimeZoneDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_UTC_TIME_ZONE_DIALOG;
            activity.showDialog("", message, null);
        }
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

    private void showDatePickerDialog() {
        int mYear = 0;
        int mMonth = 0;
        int mDay = 0;
        String text = String.valueOf(popUpTimeZoneLayoutBinding.opsinSelectDate.getText());
        // initial time load value set to date picker
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
                    popUpCameraSettingsViewModel.setCameraDate(text);// temp hold current value
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // for this device rotate date picker value reset usecase handled
            if (popUpCameraSettingsViewModel.getCameraDate() != null) {
                try {
                    // Parse the date string into a Date object
                    Date parsedDate = simpleDateFormat().parse(popUpCameraSettingsViewModel.getCameraDate());
                    // Create a new SimpleDateFormat object to format the date with the desired year format
                    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
                    // Format the parsed date to include the year in "yyyy" format
                    if (parsedDate != null) {
                        String[] dateParts = popUpCameraSettingsViewModel.getCameraDate().split("/");
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
                // if user select before load date show current date
                if (calendar == null)
                    calendar = Calendar.getInstance();
                mYear = calendar.get(Calendar.YEAR);
                mMonth = calendar.get(Calendar.MONTH);
                mDay = calendar.get(Calendar.DAY_OF_MONTH);
            }
        }

        if (datePickerDialog == null)
            datePickerDialog = new DatePickerDialog(requireContext());

        datePickerDialog.getDatePicker().init(mYear, mMonth, mDay, (view, year, monthOfYear, dayOfMonth) -> {
            Log.d(TAG, "showDatePickerDialog:onDateChanged ");
            try {
                if (calendar == null)
                    calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String formattedDate = simpleDateFormat().format(calendar.getTime());
                popUpCameraSettingsViewModel.setCameraDate(formattedDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        datePickerDialog.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            // Handle the selected date
            try {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String formattedDate = simpleDateFormat().format(selectedDateTime.getTime());
                popUpTimeZoneLayoutBinding.opsinSelectDate.setText(formattedDate);
                popUpCameraSettingsViewModel.setShowDatePicker(false);
                // popUpCameraSettingsViewModel.setCameraDate(null);
                datePickerDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        datePickerDialog.show();
        datePickerDialog.setCancelable(false);
        popUpCameraSettingsViewModel.setShowDatePicker(true);
        datePickerDialog.setOnCancelListener(dialog -> {
            popUpCameraSettingsViewModel.setShowDatePicker(false);
            popUpCameraSettingsViewModel.setCameraDate(null);
            datePickerDialog.dismiss();
        });
    }

    private void showTimePickerDialog() {
        int mHour = 0;
        int mMinute = 0;

        String time = popUpTimeZoneLayoutBinding.opsinSelectTime.getText().toString();
        // initial time load value set to time picker
        if (!time.equals(requireContext().getString(R.string.select_time))) {
            if (isTimeFormat24Hrs) {
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
            popUpCameraSettingsViewModel.setCameraTime(time); // temp hold current value
        } else {
            try {
                // for this device rotate date picker value reset usecase handled
                if (popUpCameraSettingsViewModel.getCameraTime() != null) {
                    if (isTimeFormat24Hrs) {
                        String[] timeParts = popUpCameraSettingsViewModel.getCameraTime().split(":");
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
                        String[] timeParts = popUpCameraSettingsViewModel.getCameraTime().split(":");
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
                    // for this before load time select to show current time
                    calendar = Calendar.getInstance();
                    mHour = calendar.get(Calendar.HOUR_OF_DAY);
                    mMinute = calendar.get(Calendar.MINUTE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            try {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);

                String formattedTime = simpleTimeFormat().format(selectedDateTime.getTime());
                if (isTimeFormat24Hrs)
                    popUpTimeZoneLayoutBinding.opsinSelectTime.setText(formattedTime);
                else
                    popUpTimeZoneLayoutBinding.opsinSelectTime.setText(convert24to12Hrs(formattedTime));

                updateTimeFormatInUI();
                popUpCameraSettingsViewModel.setShowTimePicker(false);
                //   popUpCameraSettingsViewModel.setCameraTime(null);
                timePickerDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, mHour, mMinute, isTimeFormat24Hrs);

        // Set an OnTimeChangedListener to detect changes in time
        try {
            @SuppressLint("DiscouragedPrivateApi") Field field = TimePickerDialog.class.getDeclaredField("mTimePicker");
            field.setAccessible(true);
            TimePicker timePicker = (TimePicker) field.get(timePickerDialog);
            if (timePicker != null) {
                timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
                    Log.d(TAG, "onTimeChanged:" + hourOfDay + ":" + minute);
                    try {
                        if (calendar == null)
                            calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        String formattedTime = simpleTimeFormat().format(calendar.getTime());
                        popUpCameraSettingsViewModel.setCameraTime(formattedTime);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        timePickerDialog.show();
        timePickerDialog.setCancelable(false);
        popUpCameraSettingsViewModel.setShowTimePicker(true);
        timePickerDialog.setOnCancelListener(dialog -> {
            popUpCameraSettingsViewModel.setCameraTime(null);
            popUpCameraSettingsViewModel.setShowTimePicker(false);
            timePickerDialog.dismiss();
            Log.d(TAG, "showtimePickerDialog: on cancel");
        });
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

        popUpTimeZoneLayoutBinding.opsinLocalDate.setText(getString(R.string.colon_split, automaticFormattedDate));
        if (isTimeFormat24Hr(requireContext())) {
            Log.d("Time Format", "24-hour format");
            String automatic24HrTimeFormat = simple24HourTimeFormat().format(setAutomaticModeDateTime.getTime());
            popUpTimeZoneLayoutBinding.opsinLocalTime.setText(getString(R.string.colon_split, automatic24HrTimeFormat));
            popUpTimeZoneLayoutBinding.opsinLocalTimeFormat.setText(getString(R.string.colon_split, getString(R.string.hour_24)));
        } else {
            Log.d("Time Format", "12-hour format");
            String automatic12HrTimeFormat = simple12HourTimeFormat().format(setAutomaticModeDateTime.getTime());
            popUpTimeZoneLayoutBinding.opsinLocalTime.setText(getString(R.string.colon_split, automatic12HrTimeFormat).toUpperCase());
            popUpTimeZoneLayoutBinding.opsinLocalTimeFormat.setText(getString(R.string.colon_split, getString(R.string.hour_12)));
        }

        DateFormat date = new SimpleDateFormat("z", Locale.getDefault());
        String localTimeZone = date.format(setAutomaticModeDateTime.getTime());
        popUpTimeZoneLayoutBinding.opsinLocalTimeZone.setText(getString(R.string.colon_split, localTimeZone));
        Log.d(TAG, "setAutomaticModeDateAndTime: " + automaticFormattedTime);
    }

    @Override
    public void onPause() {
        super.onPause();
        /*
        while rotating save the selected tab position of video mode if confirmation dialog appear
         */
        //true tab position not change
        if (!PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog() && popUpCameraSettingsViewModel.isConfirmationDialogShown()) {
            Log.e(TAG, "onPause: POPUP " + "seltab " + binding.tabVideoMode.getSelectedTabPosition());
            popUpCameraSettingsViewModel.setVideoModePosition(binding.tabVideoMode.getSelectedTabPosition());
        }

        /* if app goes to background dismiss dialog*/
        if (datePickerDialog != null && datePickerDialog.isShowing()) {
            datePickerDialog.dismiss();
        }
        if (timePickerDialog != null && timePickerDialog.isShowing()) {
            timePickerDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        switch (currentCameraSsid) {
            case NIGHTWAVE:
                if (popUpCameraSettingsViewModel.getPleaseWaitAlertDialog() != null) {
                    if (popUpCameraSettingsViewModel.getPleaseWaitAlertDialog().isShowing()) {
                        if (PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog())
                            showWiFiVideoModeProgressbar();
                        else
                            hideWiFiVideoModeProgressbar();
                    }
                }
        /*
        when rotating the device save the selected tab position of video mode while confirmation dialog shown
         */
                if (popUpCameraSettingsViewModel.isConfirmationDialogShown() || PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                    try {
                        int tab_position = popUpCameraSettingsViewModel.getVideoModePosition();
                        binding.tabVideoMode.getTabAt(tab_position).select();
                        Log.e(TAG, "onResume: isDialogShown " + tab_position);
                    } catch (Exception e) {
                        Log.e(TAG, "onResume: " + e.getLocalizedMessage());
                    }
                }
                break;
            case OPSIN:
                if (popUpCameraSettingsViewModel.isShowDatePicker())
                    showDatePickerDialog();

            if (popUpCameraSettingsViewModel.isShowTimePicker())
                showTimePickerDialog();

                TabLayout.Tab tabAt = popUpTimeZoneLayoutBinding.opsinTabDateTimeMode.getTabAt(popUpCameraSettingsViewModel.getDateTimeModePosition());
                if (tabAt != null)
                    tabAt.select();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"onResume : NW_Digital");
                break;
        }
    }

}
