package com.sionyx.plexus.ui.camera;

import static android.view.View.GONE;
import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;
import static android.view.View.*;
import static com.dome.librarynightwave.model.repository.TCPRepository.MODE_NONE;
import static com.dome.librarynightwave.model.repository.TCPRepository.commandRequested;
import static com.dome.librarynightwave.model.repository.TCPRepository.filepath;
import static com.dome.librarynightwave.model.repository.TCPRepository.fwMode;
import static com.dome.librarynightwave.model.repository.TCPRepository.imageFilePath;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.shouldReceiveUDP;
import static com.dome.librarynightwave.utils.Constants.ERR_AUTHENTICATION;
import static com.dome.librarynightwave.utils.Constants.ERR_DECODER_STREAM_01;
import static com.dome.librarynightwave.utils.Constants.ERR_DECODER_STREAM_14;
import static com.dome.librarynightwave.utils.Constants.ERR_INTERNAL_STATE_CORRUPTION;
import static com.dome.librarynightwave.utils.Constants.ERR_INVALID_RTSP_URI_06;
import static com.dome.librarynightwave.utils.Constants.ERR_INVALID_RTSP_URI_07;
import static com.dome.librarynightwave.utils.Constants.ERR_NETWORK_ISSUES_10;
import static com.dome.librarynightwave.utils.Constants.ERR_NETWORK_ISSUES_13;
import static com.dome.librarynightwave.utils.Constants.ERR_NETWORK_ISSUES_16;
import static com.dome.librarynightwave.utils.Constants.ERR_NO_VIDEO_DATA;
import static com.dome.librarynightwave.utils.Constants.ERR_PIPELINE_SETUP_FAIL_00;
import static com.dome.librarynightwave.utils.Constants.ERR_PIPELINE_SETUP_FAIL_02;
import static com.dome.librarynightwave.utils.Constants.ERR_PIPELINE_SETUP_FAIL_03;
import static com.dome.librarynightwave.utils.Constants.ERR_PIPELINE_SETUP_FAIL_04;
import static com.dome.librarynightwave.utils.Constants.ERR_PIPELINE_SETUP_FAIL_05;
import static com.dome.librarynightwave.utils.Constants.ERR_PLAYBACK_STALLED;
import static com.dome.librarynightwave.utils.Constants.ERR_SERVER_ERRORS;
import static com.dome.librarynightwave.utils.Constants.ERR_STREAM_PROGRESS_ERROR;
import static com.dome.librarynightwave.utils.Constants.ERR_STREAM_TERMINATED;
import static com.dome.librarynightwave.utils.Constants.ERR_UNKNOWN_ERROR;
import static com.dome.librarynightwave.utils.Constants.RTSP_DISCONNECTED;
import static com.dome.librarynightwave.utils.Constants.RTSP_IN_LIVE;
import static com.dome.librarynightwave.utils.Constants.RTSP_NONE;
import static com.dome.librarynightwave.utils.Constants.STREAM_QUALITY_HIGH;
import static com.dome.librarynightwave.utils.Constants.STREAM_QUALITY_LOW;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.currentStreamQuality;
import static com.dome.librarynightwave.utils.Constants.isSDKBelow13;
import static com.dome.librarynightwave.utils.Constants.rtspState;
import static com.dome.librarynightwave.utils.Constants.setIsNightWaveCamera;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP0;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP1;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP2;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP3;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP4;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP5;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP6;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP7;
import static com.dome.librarynightwave.utils.SCCPConstants.EV_STEP8;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.applyPreset;
import static com.dome.librarynightwave.viewmodel.TCPConnectionViewModel.videoSurface;
import static com.sionyx.plexus.ui.MainActivity.imageMatrixTouchHandler;
import static com.sionyx.plexus.ui.camera.CameraViewModel.cameraXValue;
import static com.sionyx.plexus.ui.camera.CameraViewModel.getCameraZoomLevel;
import static com.sionyx.plexus.ui.camera.CameraViewModel.isSelectArrow;
import static com.sionyx.plexus.ui.camera.CameraViewModel.isSelectPIP;
import static com.sionyx.plexus.ui.camera.CameraViewModel.onSelectGallery;
import static com.sionyx.plexus.ui.camera.CameraViewModel.recordButtonState;
import static com.sionyx.plexus.ui.camera.CameraViewModel.setCameraZoomLevel;
import static com.sionyx.plexus.ui.camera.CameraViewModel.videoCropState;
import static com.sionyx.plexus.ui.home.HomeViewModel.CurrentScreen;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.HOME;
import static com.sionyx.plexus.ui.home.HomeViewModel.isRecordingStarted;
import static com.sionyx.plexus.ui.home.HomeViewModel.isSdCardRecordingStarted;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.utils.Constants.ASPECT_RATIO_HEIGHT_3;
import static com.sionyx.plexus.utils.Constants.ASPECT_RATIO_HEIGHT_9;
import static com.sionyx.plexus.utils.Constants.ASPECT_RATIO_WIDTH_16;
import static com.sionyx.plexus.utils.Constants.ASPECT_RATIO_WIDTH_4;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.convertToBytes;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;
import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.Constants.isShowAllMenu1;
import static com.sionyx.plexus.utils.Constants.isShowExpandMenu1;
import static com.sionyx.plexus.utils.Constants.splitStorageValue;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Rational;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.opsinmodel.BatteryInfo;
import com.dome.librarynightwave.model.repository.opsinmodel.CompassAngles;
import com.dome.librarynightwave.model.repository.opsinmodel.GpsPosition;
import com.dome.librarynightwave.model.repository.opsinmodel.SDCardInfo;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.AspectRatioFrameLayout;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.utils.SCCPConstants;
import com.dome.librarynightwave.utils.Touch;
import com.dome.librarynightwave.utils.ZoomablePlayerView;
import com.dome.librarynightwave.utils.ZoomableTextureView;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.android.material.tabs.TabLayout;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.CameraRecordOptionLayoutBinding;
import com.sionyx.plexus.databinding.FragmentCameraBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.menus.CameraInfoViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraSettingsInfoViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraSettingsViewModel;
import com.sionyx.plexus.ui.dialog.NoticeDialogFragment;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.circulardotprogressbar.DotCircleProgressIndicator;


import org.freedesktop.gstreamer.GStreamer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraFragment extends Fragment implements Touch {
    private static final String TAG = "CameraFragment";
    public FragmentCameraBinding binding;
    public CameraRecordOptionLayoutBinding recordOptionLayoutBinding;
    CameraViewModel cameraViewModel;
    private TCPConnectionViewModel tcpConnectionViewModel;
    private CameraSettingsInfoViewModel settingsInfoViewModel;
    private CameraSettingsViewModel cameraSettingsViewModel;
    private CameraInfoViewModel cameraInfoViewModel;
    private HomeViewModel homeViewModel;
    private LifecycleOwner lifecycleOwner;

    //addition: aspectRatioState for PiP
    public static boolean aspectRatioState;

    private final Handler mHandler = new Handler();
    //addition: initializing the seconds to 0
    public int seconds = 0;
    //addition: state of the timer
    public static boolean timerState = false;
    //addition: picture in picture params
    private PictureInPictureParams pictureInPictureParams;
    public static boolean isInfoButtonPressed = false; // for this avoid same time multiple buttons pressed event to be block
    public static boolean isSettingButtonPressed = false; // for this avoid same time multiple buttons pressed event to be block
    public static boolean isChangeCameraNameButtonPressed = false; // for this avoid same time multiple buttons pressed event to be block
    public static boolean isFactoryRestButtonPressed = false; // for this avoid same time multiple buttons pressed event to be block


    private long startTime = 0L;
    private final Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    BleWiFiViewModel bleWiFiViewModel;

    private boolean isAspectRatio_16_9 = false;
    private int VIDEO_WIDTH = 720;
    private int VIDEO_HEIGHT = 540;

    int orientation;
    public static ScheduledExecutorService getNucScheduledExecutorService = null;
    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy\nHH:mm:ss");
    private final long minimumStorageForVideo = 1073741824L; // 1GB
    private final long minimumStorageForImage = 524288000L;// 500 MB
    private final long minimumSdcardStorageForVideo = 52428800L;// 50 MB
    private final long minimumSdcardStorageForImage = 10485760L;// 10 MB
    private boolean isFirstBitmapReceived = false;
    private float originalScaleX;
    private float originalScaleY;
    /*// This is for Gif
    private boolean captureGif=false;
    ArrayList<Bitmap> bitmapList=new ArrayList<>();*/
    Animation leftToRightSlideIn;
    Animation rightToLeftSlideOut;
    SDCardInfo sdCardInfo = new SDCardInfo();
    private boolean isFpsReceived = false;
    private static boolean inStreamingOpsin = false; // Bg to FG
    private ZoomablePlayerView textureView;
    private boolean isLiveEnabled = false;
    private final int NEGATIVE_BUTTON_CLICKED = 1;
    private boolean isHighQuality = false;

    public CameraFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //addition: to handle orientation
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        Log.e(TAG, "onCreateView: " + isSelectPIP);
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(requireActivity());
        lifecycleOwner = this;
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
            try {
                GStreamer.init(requireActivity());
            } catch (Exception e) {
                Log.e(TAG, "GStreamer not init ");
            }
            if (rtspState == RTSP_NONE || rtspState == RTSP_DISCONNECTED) {
                tcpConnectionViewModel.startServiceDigital();
            }
        }
        recordOptionLayoutBinding = binding.recordOptionLayout;
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        textureView = binding.videoContainer.getTextureView();
        binding.setViewModel(cameraViewModel);
        recordOptionLayoutBinding.setViewModel(cameraViewModel);
        hideNavigationBar();
        //startVideoRecordTimer();
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        settingsInfoViewModel = new ViewModelProvider(requireActivity()).get(CameraSettingsInfoViewModel.class);
        cameraSettingsViewModel = new ViewModelProvider(requireActivity()).get(CameraSettingsViewModel.class);
        cameraInfoViewModel = new ViewModelProvider(requireActivity()).get(CameraInfoViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
        binding.customProgressBar.setIndicator(new DotCircleProgressIndicator());
//        scaleGestureDetector = new ScaleGestureDetector(requireActivity(), new ZoomScalerListener());

        resizeVideo(VIDEO_WIDTH,VIDEO_HEIGHT);
        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        // to save the video from background state and handle TimerState
        if (timerState && isRecordingStarted) {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    updateRecordScreen();
                    cameraViewModel.stopRecordingVideo();
                    break;
                case OPSIN:
                    updateRecordScreen();
                    tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                    break;
                case NIGHTWAVE_DIGITAL:
//                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    if (textureView != null){
                        binding.videoContainer.removeView(textureView);
                        textureView = null;
                    }
                    // uncomment for video record and Image capture
                    updateRecordScreen();
                    tcpConnectionViewModel.stopDigitalVideoRecordLocally();
                    Log.e(TAG, "onPause called: NW_Digital");
                    break;
            }
        } else {
            updateRecordScreen();
            seconds = 0;
        }

        if (isSelectPIP) {
            if (binding != null) {
                if (binding.cameraFpsIcon.getVisibility() == VISIBLE) {
                    binding.cameraFpsIcon.setVisibility(GONE);
                }
                if (binding.cameraZoomInIcon.getVisibility() == VISIBLE) { //commenting for now
                    binding.cameraZoomInIcon.setVisibility(GONE);
                }
                if (binding.cameraZoomOutIcon.getVisibility() == VISIBLE) {
                    binding.cameraZoomOutIcon.setVisibility(GONE);
                }
                if (binding.zoomLevelText1.getVisibility() == VISIBLE) {
                    binding.zoomLevelText1.setVisibility(GONE);
                }
                binding.timerLayout.setVisibility(GONE);
                binding.startRecord.setVisibility(GONE);
                binding.cameraNucIcon.setVisibility(GONE);
                binding.cameraJpegIcon.setVisibility(GONE);
                binding.cameraExpostureIcon.setVisibility(GONE);
                binding.cameraBatteryIcon.setVisibility(GONE);
                binding.cameraMicIcon.setVisibility(GONE);
                binding.sdCardIcon.setVisibility(GONE);
                binding.sdCardLabel.setVisibility(GONE);
                binding.monochromeIcon.setVisibility(GONE);
                binding.noiseIcon.setVisibility(GONE);
                binding.roiIcon.setVisibility(GONE);
                binding.arrowRight.setVisibility(GONE);
                binding.arrowLeft.setVisibility(GONE);
            }
        }

        /* This is to not save the image when app goes background
        if (!cameraViewModel.onSaveImage) {
            if (cameraViewModel.isCapturedImage) {
                Log.d(TAG, "onPause:cameraFragment ");
                if (imageFilePath != null && !imageFilePath.isEmpty()) {
                    File file = new File(imageFilePath);
                    if (file.exists() && file.isFile() && file.canWrite()) {
                        file.delete();
                        Log.d("cameraTAG", "onPause: delete");
                    }
                }
            }
        }*/

        //this is to handle the visibility of livescreen while in pipMode
        assert binding != null;
        if (binding.cameraHomeBtn.getVisibility() == VISIBLE) {
            binding.cameraHomeBtn.setVisibility(GONE);
            binding.cameraMenuView.setVisibility(GONE);
        }

        hidemenusInSplitscreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: " + inStreamingOpsin);
        ((ZoomableTextureView) binding.textureView).reset();
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
            if (tcpConnectionViewModel != null) {
                tcpConnectionViewModel.observeCameraMode().observe(this.getViewLifecycleOwner(), observeCameraMode);
                tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
            }
        }
        /* for this pip mode exit last aspect ratio state to be set*/
        if (binding != null) {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                if (aspectRatioState) {
                    binding.cameraAspectRatio.setImageResource(R.drawable.ic_aspect_ratio4_3);
                    binding.setAspectRatio(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_aspect_ratio4_3));
                    ((ImageView) binding.image).setScaleType(ImageView.ScaleType.CENTER_CROP);
                    videoCropState = CameraViewModel.VideoCropState.CROP_ON;
                } else {
                    binding.cameraAspectRatio.setImageResource(R.drawable.ic_aspect_ratio16_9);
                    binding.setAspectRatio(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_aspect_ratio16_9));
                    ((ImageView) binding.image).setScaleType(ImageView.ScaleType.FIT_CENTER);
                    videoCropState = CameraViewModel.VideoCropState.CROP_OFF;
                }
            }
            if (onSelectGallery) {
                cameraViewModel.setIsShowAlertDialog(false);
            }
            //resetZoomState();
            if (tcpConnectionViewModel != null) {
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) && !isSelectPIP) {
                    //CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    /* for this pip mode exit again live streaming not started, now handled*/
                    if (CameraViewModel.cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        binding.recordBtn.setVisibility(GONE);
                        binding.compassView.setVisibility(VISIBLE);
                        if (inStreamingOpsin) { // on Foreground
                            tcpConnectionViewModel.stopOpsinLiveStreaming();
                            new Handler().postDelayed(() -> {
                                tcpConnectionViewModel.startOpsinLiveStreaming();
                                new Handler().postDelayed(this::triggerTimerToUpdateStatusIcons, 500);

                            }, 1000);
                        }
                    } else {
                        new Handler().postDelayed(this::triggerTimerToUpdateStatusIcons, 500);
                    }
                    //showOpsinLayout();
                } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) && !isSelectPIP) {
                    new Handler().postDelayed(() -> {
                        if (CameraViewModel.hasNewFirmware()) {
                            if (cameraViewModel.isAnalogMode())
                                tcpConnectionViewModel.startLiveView(true);
                        } else {
                            tcpConnectionViewModel.startLiveView(true);
                        }
                    }, 1000);
                }
                else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                        binding.retryTxtDigitalcamera.setVisibility(GONE);
                        bleWiFiViewModel.setConnectedSsidFromWiFiManager(homeViewModel.connectedSsid);
                    if (!isSelectPIP) {
                        Log.e(TAG,"onResume : NW_Digital");
                        new Handler().postDelayed(() -> {
                            if (rtspState == RTSP_IN_LIVE) {
                                // Video feed should be start by user and, BG to FG - stream should be play if it's streamed previously
                                isLiveEnabled = true; // to avoid flicker issue
                                tcpConnectionViewModel.setStartStreamDigitalCamera();
                            }
                        }, 2000);
                    }
                }
            }
        }
        if (isSelectPIP){
            isHighQuality = (Constants.currentStreamQuality == STREAM_QUALITY_HIGH);
            assert binding != null;
            binding.sliderTabLayout.post(() -> updateSwitchUI(false));
        }
        hidemenusInSplitscreen();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("viewTAG", "onViewCreated: ");
        orientation = requireContext().getResources().getConfiguration().orientation;
        showNightwaveOrOpsinUiLayout();

        subscribeUI();

        //addition: handling PiP state
        if (isSelectPIP) {
            binding.cameraMenuView.setVisibility(GONE);
            binding.cameraHomeBtn.setVisibility(GONE);
            if (binding.compassView.getVisibility() == VISIBLE)
                binding.compassView.setVisibility(GONE);
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                hideOpsinLayout();
            }
            Log.d(TAG, "onPictureInPictureMode " + aspectRatioState);
            if (aspectRatioState) {
                ((ImageView) binding.image).setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                ((ImageView) binding.image).setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
        } else {
            isSelectPIP = false;
        }

        bleWiFiViewModel.setConnectedSsidFromWiFiManager(homeViewModel.connectedSsid);
    }

    // private int cameraXValue;
    private MainActivity activity = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = ((MainActivity) context);
    }

    public void showNightwaveOrOpsinUiLayout() {
        switch (currentCameraSsid) {
            case OPSIN:
                /*opsin*/
                try {
                    String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv();
                    if (cameraRiscv != null) {
                        String[] aCameraRiscVersion = cameraRiscv.split("\\.");
                        CameraViewModel.cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    if (!isSelectPIP) {
                        binding.compassView.setVisibility(VISIBLE);
                        binding.recordBtn.setVisibility(GONE);
                    }
                } else {
                    binding.compassView.setVisibility(GONE);
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) binding.cameraMenuView.getLayoutParams();
                    layoutParams.topMargin = 0; // remove top margin
                    binding.cameraMenuView.setLayoutParams(layoutParams);
                }

                CameraViewModel.setHasCommandInitiate(false);
                binding.recordVideo.setVisibility(GONE);
                binding.cameraAspectRatioBtn.setVisibility(GONE);
                binding.image.setVisibility(GONE);
                binding.cameraPictureInPictureBtn.setVisibility(GONE);
                ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).setMargins(0, 0, 0, 0);
                if (CameraViewModel.cameraXValue < OPSIN_STREAMING_SUPPORTS_FROM) {
                    binding.cameraRightMenuGallery.setVisibility(GONE);
                    binding.cameraLeftMenuGallery.setVisibility(GONE);
                    binding.recordBtn.setVisibility(GONE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(GONE);
                } else {
                    binding.cameraRightMenuGallery.setVisibility(GONE);
                    binding.cameraLeftMenuGallery.setVisibility(GONE);
                    // binding.recordBtn.setVisibility(View.VISIBLE);
                }

                ((GradientDrawable) binding.cameraRightMenuView.getBackground()).setStroke(2, Color.WHITE);
                ((GradientDrawable) binding.cameraLeftMenuView.getBackground()).setStroke(2, Color.WHITE);
                binding.startRecordRectangle.setImageDrawable(AppCompatResources.getDrawable(Objects.requireNonNull(getContext()), R.drawable.ic_rectangle_svg_white));

                if (CameraViewModel.cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    binding.liveViewVideo.setVisibility(VISIBLE);
                    binding.cameraPresetBtn.setVisibility(VISIBLE);
                    binding.recordBtn.setVisibility(GONE);
                } else {
                    binding.liveViewVideo.setVisibility(GONE);
                    binding.cameraPresetBtn.setVisibility(GONE);
                }

//            binding.cameraZoomInIcon.setVisibility(View.VISIBLE);
//            binding.cameraZoomInIcon.setEnabled(false);
//            binding.cameraZoomOutIcon.setVisibility(View.VISIBLE); //commented for now
//            binding.cameraZoomOutIcon.setEnabled(false);
                binding.cameraJpegIcon.setVisibility(GONE);
                binding.cameraNucIcon.setVisibility(GONE);
                binding.cameraFpsIcon.setVisibility(GONE);
                binding.cameraExpostureIcon.setVisibility(GONE);
                binding.cameraBatteryIcon.setVisibility(GONE);
                binding.cameraMicIcon.setVisibility(GONE);
                videoCropState = CameraViewModel.VideoCropState.CROP_OFF;
                binding.cameraViewRoot.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.opsin_live_view_background));
                CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
                resetOpsinRecordUI();
                updateZoomIcon();
                tcpConnectionViewModel.setTextureView((ZoomableTextureView) binding.textureView);

                //this is for live button state to be grey out
                binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg_white));
                binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                setRecordOptionLayoutDisabled(false);
                binding.retryTxtDigitalcamera.setVisibility(GONE);
                binding.sliderTabLayout.setVisibility(GONE);
                break;
            case NIGHTWAVE:
                //Nightwave
                binding.retryTxtDigitalcamera.setVisibility(GONE);
                binding.sliderTabLayout.setVisibility(GONE);
                ((GradientDrawable) binding.cameraRightMenuView.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraLeftMenuView.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraRecordViewMenu.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraRecordViewPauseMenu.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraRecordSaveLayout.getBackground()).setStroke(0, Color.WHITE);
                Log.d("liveTAG", "Nightwave: ");
//            ((ImageView) binding.image).setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_camera_background));
                binding.image.setImageResource(R.drawable.ic_camera_background);

                CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                videoCropState = CameraViewModel.VideoCropState.CROP_ON;
                binding.recordVideo.setVisibility(GONE);
                binding.cameraPresetBtn.setVisibility(VISIBLE);
                binding.cameraLeftMenuGallery.setVisibility(GONE);
                binding.cameraRightMenuGallery.setVisibility(GONE);
                binding.recordBtn.setVisibility(GONE);

                binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg));
                binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                setRecordOptionLayoutDisabled(false);

                if (CameraViewModel.hasNewFirmware()) {
                    Log.d(TAG, "subscribeUI: isNewFirmware " + CameraViewModel.hasNewFirmware());
                    //cameraViewModel.clearCameraModeState();
                    tcpConnectionViewModel.getCameraMode();
                } else {
                    /*Start LiveView*/
                    Log.d(TAG, "subscribeUI: isNewFirmware" + CameraViewModel.hasNewFirmware());
                    tcpConnectionViewModel.startLiveView(true);
                    binding.customProgressBar.setVisibility(VISIBLE);
                    binding.liveViewVideo.setVisibility(VISIBLE);
                }
                hideOpsinLayout();
                setCropState();
                CameraViewModel.setHasPressedSettingCancelIcon(false); // for this condition to avoid stop live view on cancel settings dialog screen

                //Pinch Zoom for Nightwave
                imageMatrixTouchHandler.setListener(this);
                binding.image.setOnTouchListener(imageMatrixTouchHandler);
                binding.analysisBtn.setVisibility(GONE);
                binding.analyticsLayout.setVisibility(GONE);
                break;
            case NIGHTWAVE_DIGITAL:
                ((GradientDrawable) binding.cameraRightMenuView.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraLeftMenuView.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraRecordViewMenu.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraRecordViewPauseMenu.getBackground()).setStroke(0, Color.WHITE);
                ((GradientDrawable) binding.cameraRecordSaveLayout.getBackground()).setStroke(0, Color.WHITE);
                Log.d("liveTAG", "Nightwave Digital: ");
                binding.image.setImageResource(R.drawable.ic_nw_digital_background);

                binding.recordVideo.setVisibility(GONE);
                binding.cameraPresetBtn.setVisibility(GONE);
                binding.cameraLeftMenuGallery.setVisibility(GONE);
                binding.cameraRightMenuGallery.setVisibility(GONE);
                binding.recordBtn.setVisibility(GONE);


//                tcpConnectionViewModel.setVideoContainer(binding.videoContainer.getTextureView());
                // Media icons
                setRecordOptionLayoutDisabled(false);// only streaming
//                setIconsAreEnabled(false);
                hideOpsinLayout();
                setCropState();
                CameraViewModel.setHasPressedSettingCancelIcon(false); // for this condition to avoid stop live view on cancel settings dialog screen

                binding.analysisBtn.setVisibility(GONE);
                binding.analyticsLayout.setVisibility(GONE);

                break;
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void subscribeUI() {
        /*if clicked info icon*/
        hasShowExpandMenu();

        if (cameraViewModel.isShowAllMenu()) {
            showAutohideLayout();
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToHome();
            }
        });
        Log.e(TAG, "subscribeUI: " + currentCameraSsid);
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
            /*opsin*/
            binding.retryTxtDigitalcamera.setVisibility(GONE);
            binding.sliderTabLayout.setVisibility(GONE);
            if (isSelectPIP) {
                hideOpsinLayout();
            } else {
                if (CameraViewModel.cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    tcpConnectionViewModel.getOpsinRecordingStatus();
                    binding.customProgressBar.setVisibility(VISIBLE);
                    binding.recordBtn.setVisibility(GONE);
                    new Handler(Looper.getMainLooper()).postDelayed(this::triggerTimerToUpdateStatusIcons, 1000);
                } else {
                    triggerTimerToUpdateStatusIcons();
                }
            }

            cameraViewModel.isResetOpsinUI.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    resetOpsinRecordUI();
                }
            }));

            binding.recordPause.setOnClickListener(v -> {
                if (CameraViewModel.opsinRecordButtonState == CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Start) {
                    tcpConnectionViewModel.stopOpsinCameraRecord();
                    if (homeViewModel.menuState) {
                        binding.startRecord.setVisibility(GONE);
                    }
                }
            });

            //this is to handle start & stop live button state
            binding.liveViewVideo.setOnClickListener(v -> {
                /*OPSIN START STOP LIVE STREAMING*/
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    binding.image.setVisibility(VISIBLE);
                    binding.textureView.setVisibility(GONE);
                    tcpConnectionViewModel.stopOpsinLiveStreaming();
                } else {
                    binding.image.setVisibility(GONE);
                    binding.textureView.setVisibility(VISIBLE);
                    tcpConnectionViewModel.getOpsinRecordingStatus();
                }
            });
            // camera sd card record
            binding.startRecord.setOnClickListener(v -> {
                if (CameraViewModel.opsinRecordButtonState == CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop) {
                    //    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    if (sdCardInfo.isMounted()) {
                        boolean hasSdCardStorageFull = hasAvailableSdcardStorageSpace(sdCardInfo, true);
                        if (hasSdCardStorageFull) {
                            showCameraSdcardStorageFullAlertDialog(getString(R.string.camera_sd_card_full_video_recording_msg));
                        } else {
                            Log.d(TAG, "startRecord: sd card space available");
                            tcpConnectionViewModel.startOpsinCameraRecord();
                        }
                    } else {
                        tcpConnectionViewModel.startOpsinCameraRecord();
                    }
                }
            });

            tcpConnectionViewModel.observeOpsinCameraRecordingState().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (!aBoolean) {
                    tcpConnectionViewModel.startOpsinLiveStreaming();
                } else {
                    if (shouldReceiveUDP) {
                        CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                        binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg_white));
                        binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                        ((ImageView) binding.image).setImageResource(R.drawable.opsin_live_view_background);
                        setRecordOptionLayoutDisabled(false);
                        binding.textureView.setVisibility(GONE);
                        ((ImageView) binding.image).setVisibility(VISIBLE);
                        //on Live Stop state -Button state has been changed
                        binding.recordBtn.setVisibility(GONE);
                        binding.cameraPictureInPictureBtn.setVisibility(GONE);
                        ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
                        tcpConnectionViewModel.stopOpsinStreamingStates();

                        //Show dialog here about recording is initiated by camera knob and reset the default background
                        showOpsinCameraStreamingToStopRecordDialog(getString(R.string.opsin_recording_started_during_streaming));
                        if (binding.customProgressBar.getVisibility() == VISIBLE) {
                            binding.customProgressBar.setVisibility(GONE);
                        }

                        // to save the video from live streaming while stopped itself
                        if (timerState && isRecordingStarted) {
                            updateRecordScreen();
//                            cameraViewModel.stopRecordingVideo();
                            tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                        } else {
                            updateRecordScreen();
                            seconds = 0;
                        }
                    } else {
                        //show dialog here whether to stop recording
                        Log.e(TAG, "OPSIN RECORDING STATE IS TRUE");
                        CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                        showOpsinCameraRecordInProgressDialog(getString(R.string.opsin_start_streaming_msg_while_recording));
                        binding.textureView.setVisibility(GONE);
                        binding.image.setVisibility(VISIBLE);
                        binding.image.setImageResource(R.drawable.opsin_live_view_background);

                        if (binding.customProgressBar.getVisibility() == VISIBLE) {
                            binding.customProgressBar.setVisibility(GONE);
                        }
                    }
                }
            }));


            cameraViewModel.isSelectZoomIn.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean && !isSelectPIP) {
                    Log.e(TAG, "Zoom in level" + getCameraZoomLevel());
                    if (getCameraZoomLevel() == 1) {
                        setCameraZoomLevel(2);
                        updateZoomIcon();
                        //tcpConnectionViewModel.setDigitalZoom(LEVEL2.getValue());
                    } else if (getCameraZoomLevel() == 2) {
                        setCameraZoomLevel(3);
                        updateZoomIcon();
                        //tcpConnectionViewModel.setDigitalZoom(LEVEL3.getValue());
                    }
                }
            }));

            cameraViewModel.isSelectZoomOut.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean && !isSelectPIP) {
                    Log.e(TAG, "Zoom out level" + getCameraZoomLevel());
                    if (getCameraZoomLevel() == 2) {
                        setCameraZoomLevel(1);
                        updateZoomIcon();
                        // tcpConnectionViewModel.setDigitalZoom(LEVEL1.getValue());
                    } else if (getCameraZoomLevel() == 3) {
                        setCameraZoomLevel(2);
                        updateZoomIcon();
                        // tcpConnectionViewModel.setDigitalZoom(LEVEL2.getValue());
                    }
                }
            }));
            cameraViewModel.triggerTimerTOUpdateStatusIcons.observe(lifecycleOwner, new EventObserver<>(isTriggered -> {
                if (isTriggered) {
                    triggerTimerToUpdateStatusIcons();
                } else {
                    stopPeriodicTimer();
                }
            }));

            homeViewModel.isOpsinRecordInProgress.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(aBoolean -> {
                Log.e(TAG, "isOpsinRecordInProgress" + aBoolean);
                if (aBoolean) {
                    cameraViewModel.setHasShowProgress(false);
                    tcpConnectionViewModel.resetOpsinLiveStreamingState(); // for this after sd card record stop to start live stream reset state
                    tcpConnectionViewModel.stopOpsinCameraRecord();
                    /*after sd card recording stopped to start live streaming*/
                    new Handler().postDelayed(() -> tcpConnectionViewModel.getOpsinRecordingStatus(), 1000);
                } else {
                    cameraViewModel.setHasShowProgress(false);
                    binding.textureView.setVisibility(GONE);
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg_white));
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                    ((ImageView) binding.image).setImageResource(R.drawable.opsin_live_view_background);
                    setRecordOptionLayoutDisabled(false);
                    //on Live Stop state -Button state has been changed
                    binding.recordBtn.setVisibility(GONE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(GONE);
                    binding.cameraPictureInPictureBtn.setVisibility(GONE);
                    ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
                }
            }));

            homeViewModel.isOpsinStreamStopRecord.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    Log.e(TAG, "isOpsinStreamStopRecord" + aBoolean);
                    cameraViewModel.setHasShowProgress(false);
                    binding.textureView.setVisibility(GONE);
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg_white));
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                    ((ImageView) binding.image).setImageResource(R.drawable.opsin_live_view_background);
                    //on Live Stop state -Button state has been changed
                    binding.recordBtn.setVisibility(GONE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(GONE);
                    binding.cameraPictureInPictureBtn.setVisibility(GONE);
                    ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
                }
            }));

            homeViewModel.opsinBatteryInfo.observe(lifecycleOwner, new EventObserver<>(objectEvent -> {
                if (homeViewModel.currentScreen == CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == CurrentScreen.CAMERA_SETTINGS_DIALOG_SCREEN || homeViewModel.currentScreen == CurrentScreen.CAMERA_INFO_DIALOG_SCREEN) {
                    if ((BatteryInfo) objectEvent != null && !isSelectPIP) {
                        binding.cameraBatteryIcon.setVisibility(VISIBLE);
                        switch (((BatteryInfo) objectEvent).getState()) {
                            case 0:
                            case 5:
                            case 10:
                            case 15:
                            case 20:
                            case 25:
                                binding.cameraBatteryIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_battery_25));
                                break;
                            case 50:
                                binding.cameraBatteryIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_battery_50));
                                break;
                            case 75:
                                binding.cameraBatteryIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_battery_75));
                                break;
                            case 100:
                                binding.cameraBatteryIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_battery_100));
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + ((BatteryInfo) objectEvent).getState());
                        }
                        Log.e(TAG, "observeOpsinGetBatteryInfo : " + ((BatteryInfo) objectEvent).getState());
//                        tcpConnectionViewModel.getNUC();
                    }
                } else {
                    Log.e(TAG, "observeOpsinGetBatteryInfo : else");
                }
            }));

            tcpConnectionViewModel.observeOpsinCameraErrorMessage().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    showOpsinCameraNotResponding(requireContext().getString(R.string.socket_disconnected_messgae));
                    isRecordingStarted = false;
                    isSdCardRecordingStarted = false;
                    shouldReceiveUDP = false;
                    cameraViewModel.setShowExpandMenu(false);
                    cameraViewModel.setShowAllMenu(false);
                    screenType = HOME;
                    firmwareUpdateSequence.clear();
                    fwMode = MODE_NONE;
                    isChangeCameraNameButtonPressed = false;
                    isFactoryRestButtonPressed = false;
                    isSettingButtonPressed = false;
                    isInfoButtonPressed = false;
                    isFirstBitmapReceived = false;
                    resetOpsinRecordUI();
                }
            }));

            tcpConnectionViewModel.observeOpsinCommandError().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null && !isSelectPIP) {
                    CommandError commandError = (CommandError) o;
                    String error = commandError.getError();
                    if (error.equalsIgnoreCase("SCCP_CMD_CAMERA_START_RECORDING : SCCP_ACK_FAILURE")) {
                        showOpsinCameraNotResponding(getString(R.string.failed_to_start_record));
                    } else {
                        showOpsinCameraNotResponding(getString(R.string.failed_to_start_record));
                    }
                    Log.e(TAG, "opsin_command_error: " + commandError.getError());
                }
            }));

            tcpConnectionViewModel.observeOpsinCameraStartStreaming().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                Log.e(TAG, "observeOpsinCameraStartStreaming: " + aBoolean);
                if (aBoolean) {
                    //  cameraViewModel.setHasDismissPleaseWaitProgressDialog(true);
                    try {
                        binding.image.setVisibility(VISIBLE);
                        binding.image.setImageResource(R.color.black);
                        inStreamingOpsin = true;
                        CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED;
                        binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_live_view_white));
                        binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                        setRecordOptionLayoutDisabled(true);
                        //on Live Start state -Button state has been changed
                        //   binding.recordBtn.setVisibility(View.VISIBLE);
                        cameraViewModel.onSelectRecordOptionsCancel();
                        binding.cameraPictureInPictureBtn.setVisibility(VISIBLE);
                        ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                        binding.textureView.setVisibility(VISIBLE);
                        if (binding.customProgressBar.getVisibility() == VISIBLE) {
                            binding.customProgressBar.setVisibility(GONE);
                        }
                        if (tcpConnectionViewModel.observeHasRecordStoppedDuetoNoLivePackets().hasObservers()) {
                            tcpConnectionViewModel.observeHasRecordStoppedDuetoNoLivePackets().removeObservers(lifecycleOwner);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (binding.customProgressBar.getVisibility() == VISIBLE) {
                        binding.customProgressBar.setVisibility(GONE);
                    }
                }
            }));

            tcpConnectionViewModel.observeOpsinCameraStopStreaming().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                Log.e(TAG, "observeOpsinCameraStopStreaming: " + aBoolean);
                if (aBoolean) {
                    inStreamingOpsin = false;
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg_white));
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                    setRecordOptionLayoutDisabled(false);
                    ((ImageView) binding.image).setImageResource(R.drawable.opsin_live_view_background);
                    //on Live Stop state -Button state has been changed
                    binding.recordBtn.setVisibility(GONE);
                    cameraViewModel.onSelectRecordOptionsCancel();
                    binding.cameraPictureInPictureBtn.setVisibility(GONE);
                    ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
                    cameraViewModel.setHasShowProgress(false);
                    if (binding.customProgressBar.getVisibility() == VISIBLE) {
                        binding.customProgressBar.setVisibility(GONE);
                    }
                }
            }));

            tcpConnectionViewModel.observeOpsinCameraStartRecord().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                Log.d(TAG, "start record:" + aBoolean + " /" + CameraViewModel.opsinRecordButtonState);
                if (aBoolean && !isSelectPIP) {
                    CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Start;
                    startTime = SystemClock.uptimeMillis();
                    customHandler.postDelayed(updateTimerThread, 0);
                    binding.timerLayout.setVisibility(VISIBLE);
                    binding.startRecord.setVisibility(GONE);
                    binding.cameraMenuView.setVisibility(GONE);
                    isSdCardRecordingStarted = true;
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM)
                        binding.compassView.setVisibility(GONE);
                }
            }));

            tcpConnectionViewModel.observeOpsinCameraStopRecord().observe(requireActivity(), aBoolean -> {
                Log.d(TAG, "stop record:" + aBoolean);
                if (aBoolean && !isSelectPIP) {
                    isSdCardRecordingStarted = false;
                    CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
                    resetOpsinRecordUI();
                    binding.image.setVisibility(GONE);
                    binding.textureView.setVisibility(VISIBLE);
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        new Handler().postDelayed(() -> {
                            tcpConnectionViewModel.getOpsinRecordingStatus();
                        }, 750);
                    }
                }
            });

            tcpConnectionViewModel.getValueOpsinLiveViewStats().observe(lifecycleOwner, opsinRTPStats -> {
                if (opsinRTPStats != null) {
                    /*for this Nw*/
                    binding.goodPacket.setVisibility(GONE);
                    binding.size.setVisibility(GONE);
                    binding.fps.setVisibility(GONE);
                    binding.receivedUdpPacket.setVisibility(GONE);
                    binding.rtpSkippedPacket.setVisibility(GONE);
                    binding.jpegBadStart.setVisibility(GONE);
                    binding.jpegBadEnd.setVisibility(GONE);
                    binding.jpegCorrupted.setVisibility(GONE);

                    binding.opsinFrameSize.setVisibility(VISIBLE);
                    binding.opsinFps.setVisibility(VISIBLE);
                    binding.opsinOutOfOrder.setVisibility(VISIBLE);
                    binding.opsinRtpReceived.setVisibility(VISIBLE);

                    /*for this opsin*/
                    int opsin_fps = opsinRTPStats.getOpsin_Fps();
                    if (opsin_fps > 0) {
                        String fps = getString(R.string.fps) + opsin_fps;
                        binding.opsinFps.setText(fps);
                    }
                    binding.opsinFrameSize.setText(getString(R.string.frame_size) + opsinRTPStats.getOpsin_frame_size());
                    binding.opsinOutOfOrder.setText(getString(R.string.out_of_order) + opsinRTPStats.getOpsin_out_of_order());
                    binding.opsinRtpReceived.setText(getString(R.string.rtp_received) + opsinRTPStats.getOpsin_rtp_received());
                }
            });

           /* tcpConnectionViewModel.observeOpsinGetDigitalZoom().observe(lifecycleOwner, new EventObserver<>(object -> {
                if (object != null && !isSelectPIP) {
                    if (object instanceof CommandError) {
                        Log.e(TAG, "observeOpsinGetDigitalZoom: " + ((CommandError) object).getError());
                    } else {
                        if ((byte) object == 1) {
                            binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out_fade));
                            binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in));
                            binding.zoomLevelText1.setText(R.string._1_x);
                            setCameraZoomLevel(1);
                        } else if ((byte) object == 2) {
                            setCameraZoomLevel(2);
                            binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out));
                            binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in));
                            binding.zoomLevelText1.setText(R.string._2_x);
                        } else {
                            binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in_fade));
                            binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out));
                            setCameraZoomLevel(3);
                            binding.zoomLevelText1.setText(R.string._3_x);
                        }
                        //if (!CameraViewModel.hasCommandInitiated())
                        getNUCState();

                        Log.e(TAG, "observeOpsinGetDigitalZoom : " + object + " " + getCameraZoomLevel() + "/" + CameraViewModel.hasCommandInitiated());
                    }
                }
            }));

            tcpConnectionViewModel.observeOpsinSetDigitalZoom().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null && !isSelectPIP) {
                    if (getCameraZoomLevel() == 1) {
                        binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out_fade));
                        binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in));
                        binding.zoomLevelText1.setText(getString(R.string._1_x));
                    } else if (getCameraZoomLevel() == 2) {
                        binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out));
                        binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in));
                        binding.zoomLevelText1.setText(getString(R.string._2_x));
                    } else {
                        binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in_fade));
                        binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out));
                        binding.zoomLevelText1.setText(getString(R.string._3_x));
                    }
                    Log.e(TAG, "observeOpsinSetDigitalZoom Level : " + getCameraZoomLevel());
                }
            }));*/


            tcpConnectionViewModel.observeOpsinGetNUCState().observe(lifecycleOwner, value -> {
                if (value != null && !isSelectPIP) {
                    //  binding.cameraNucIcon.setVisibility(View.VISIBLE);
                    cameraSettingsViewModel.observeOpsinNUCValue(value);
                    switch ((int) value) {
                        case 0: // disabled --red
                        case 2:
                            binding.cameraNucIcon.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_nuc_off));
                            break;
                        case 1: // enabled-- green
                            binding.cameraNucIcon.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_nuc_on));
                            break;
                    }
                    Log.e(TAG, "observeOpsinGetNUCState : " + value + "/ " + CameraViewModel.hasCommandInitiated());
//                    tcpConnectionViewModel.getEv();
                }
            });

            tcpConnectionViewModel.observeOpsinGetEv().observe(lifecycleOwner, new EventObserver<>(object -> {
                if (object != null && !isSelectPIP) {
                    Log.e(TAG, "observeOpsinGetEv : " + (byte) object);
                    settingsInfoViewModel.observeOpsinEvValue(object);
                    switch ((byte) object) {
                        case EV_STEP0:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_minus_25));
                            break;
                        case EV_STEP1:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_minus2));
                            break;
                        case EV_STEP2:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_minus15));
                            break;
                        case EV_STEP3:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_minus1));
                            break;
                        case EV_STEP4:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_minus05));
                            break;
                        case EV_STEP5:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ev0));
                            break;
                        case EV_STEP6:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_05));
                            break;
                        case EV_STEP7:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_1));
                            break;
                        case EV_STEP8:
                            binding.cameraExpostureIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_exposer_15));
                            break;

                        default:
                            Log.e(TAG, "onChanged: UnExpected Value");
                    }
//                    tcpConnectionViewModel.getFrameRate();
                }
            }));

            tcpConnectionViewModel.observeOpsinGetFrameRate().observe(lifecycleOwner, new EventObserver<>(object -> {
                if (object != null && !isSelectPIP) {
                    cameraSettingsViewModel.observeOpsinFPSValue(object);
                    if ((short) object == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue()) {
                        binding.cameraFpsIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_30_fps));
                    } else if ((short) object == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue()) {
                        binding.cameraFpsIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_60_fps));
                    } else {
                        binding.cameraFpsIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_90_fps));
                    }
                    Log.e(TAG, "observeOpsinGetFrameRate : " + object);
//                    tcpConnectionViewModel.getMicState();
                }
            }));
            tcpConnectionViewModel.observeOpsinGetMICState().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null && !isSelectPIP) {
                    cameraSettingsViewModel.observeOpsinMICState(o);
                    binding.cameraMicIcon.setVisibility(VISIBLE);
                    if ((byte) o == SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue()) {
                        binding.cameraMicIcon.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_mic_on_green));
                    } else {
                        binding.cameraMicIcon.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_mic_off));
                    }
                    Log.e(TAG, "observeOpsinCameraMICState : " + o);
                    if (!CameraViewModel.hasCommandInitiated()) {
                        CameraViewModel.setHasCommandInitiate(true);
                    }

                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
//                        tcpConnectionViewModel.getOpsinCompassValue();
                    }
                }
            }));

            tcpConnectionViewModel.getOpsinCompassState().observe(lifecycleOwner, new EventObserver<>(object -> {
                if (object != null && !isSelectPIP) {
                    CompassAngles compassAngles = (CompassAngles) object;
                    Log.d(TAG, "observeOpsinGetCompassState: " + compassAngles.getYaw());
                    boolean calibrated = compassAngles.isCalibrated();
                    byte calibratedLevel = compassAngles.getCalibratedLevel();

                    if (calibratedLevel <= 50) {
                        switch (calibratedLevel) {
                            case 0:
                                binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_calibrated_0));
                                break;
                            case 25:
                                binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_calibrated_25));
                                break;
                            case 50:
                                binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_calibrated_50));
                                break;
                        }

                    } else {
                        // here need to change icons and add value in sccp constants
                        if ((compassAngles.getYaw() >= 338 && compassAngles.getYaw() <= 359) || (compassAngles.getYaw() >= 0 && compassAngles.getYaw() <= 11)) {
                            //"N"
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_n));
                        } else if (compassAngles.getYaw() >= 11 && compassAngles.getYaw() <= 33) {
                            // "NNE";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_nne));
                        } else if (compassAngles.getYaw() >= 34 && compassAngles.getYaw() <= 78) {
                            // "ENE";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_ene));
                        } else if (compassAngles.getYaw() >= 79 && compassAngles.getYaw() <= 101) {
                            // "E";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_e));
                        } else if (compassAngles.getYaw() >= 102 && compassAngles.getYaw() <= 123) {
                            // "ESE";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_ese));
                        } else if (compassAngles.getYaw() >= 124 && compassAngles.getYaw() <= 168) {
                            // "SSE";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_sse));
                        } else if (compassAngles.getYaw() >= 169 && compassAngles.getYaw() <= 191) {
                            // "S";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_s));
                        } else if (compassAngles.getYaw() >= 192 && compassAngles.getYaw() <= 213) {
                            // "SSW";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_ssw));
                        } else if (compassAngles.getYaw() >= 214 && compassAngles.getYaw() <= 258) {
                            // "WSW";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_wsw));
                        } else if (compassAngles.getYaw() >= 259 && compassAngles.getYaw() <= 281) {
                            // "W";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_w));
                        } else if (compassAngles.getYaw() >= 282 && compassAngles.getYaw() <= 303) {
                            // "WNW";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_wnw));
                        } else if (compassAngles.getYaw() >= 304 && compassAngles.getYaw() <= 348) {
                            // "NNW";
                            binding.compassIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_nnw));
                        }
                    }
//                    tcpConnectionViewModel.getOpsinMonochromaticState();
                }
            }));

            tcpConnectionViewModel.getOpsinSdCardInfo().observe(lifecycleOwner, new EventObserver<>(object -> {
                if (object != null && !isSelectPIP) {
                    sdCardInfo = (SDCardInfo) object;
                    Log.d(TAG, "observegetOpsinSdCardInfo: " + sdCardInfo.isMounted() + " " + isSdCardRecordingStarted);
                    cameraInfoViewModel.observeOpsinSdCardInfo(object);
                    if (sdCardInfo.isMounted()) {
                        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM)
                            binding.sdCardLabel.setVisibility(VISIBLE);
                        else
                            binding.sdCardLabel.setVisibility(GONE);

                        binding.sdCardLabel.setText(String.valueOf(sdCardInfo.getFileCount()));
                        binding.sdCardIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_sdcard_mount));
                        recordOptionLayoutBinding.cameraRecordBtn.setEnabled(true);
                        recordOptionLayoutBinding.cameraRecordBtn.setAlpha(1.0f);
                        // for this while start sd card recording during this time storage full
                        if (isSdCardRecordingStarted) {
                            boolean hasSdCardStorageFull = hasAvailableSdcardStorageSpace(sdCardInfo, true);
                            if (hasSdCardStorageFull) {
                                tcpConnectionViewModel.stopOpsinCameraRecord();
                                showCameraSdcardStorageFullAlertDialog(getString(R.string.camera_sd_card_full_video_recording_msg));
                            } else {
                                Log.d(TAG, "getOpsinSdCardInfo: sdcard space available");
                            }
                        }
                    } else {
                        binding.sdCardLabel.setVisibility(GONE);
                        binding.sdCardIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_sdcard_un_mount));
                        recordOptionLayoutBinding.cameraRecordBtn.setEnabled(false);
                        recordOptionLayoutBinding.cameraRecordBtn.setAlpha(0.5f);
                        /* if sd card recording during this time */
                        if (isSdCardRecordingStarted && !isSelectPIP) {
                            isSdCardRecordingStarted = false;
                            CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
                            resetOpsinRecordUI();
                            binding.image.setVisibility(GONE);
                            binding.textureView.setVisibility(VISIBLE);
                            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                                new Handler().postDelayed(() -> {
                                    tcpConnectionViewModel.getOpsinRecordingStatus();
                                }, 750);
                            }
                        }
                    }
//                    tcpConnectionViewModel.getOpsinMonochromaticState();
                }
            }));

            tcpConnectionViewModel.getOpsinMonochromeState().observe(lifecycleOwner, new EventObserver<>(monochromeState -> {
                if (monochromeState != null && !isSelectPIP) {
                    Log.d(TAG, "getOpsinMonochromeState: " + monochromeState);
                    cameraSettingsViewModel.observeOpsinMonochromeState(monochromeState);
                    if ((byte) monochromeState == SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.ENABLED.getValue()) {
                        binding.monochromeIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_monochrome_white));
                    } else {
                        binding.monochromeIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_monochrome_on));
                    }
//                    tcpConnectionViewModel.getOpsinNoiseReductionState();
                }
            }));

            tcpConnectionViewModel.getOpsinNoiseState().observe(lifecycleOwner, new EventObserver<>(noiseState -> {
                if (noiseState != null && !isSelectPIP) {
                    Log.d(TAG, "getOpsinNoiseState: " + noiseState);
                    cameraSettingsViewModel.observeOpsinNoiseState(noiseState);
                    if ((byte) noiseState == SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.ENABLED.getValue()) {
                        binding.noiseIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_noice_on));
                    } else {
                        binding.noiseIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_noice_off));
                    }
//                    tcpConnectionViewModel.getOpsinROI();
                }
            }));

            tcpConnectionViewModel.getOpsinROIState().observe(lifecycleOwner, new EventObserver<>(roiState -> {
                if (roiState != null && !isSelectPIP) {
                    Log.d(TAG, "getOpsinROIState: " + roiState);
                    cameraSettingsViewModel.observeOpsinRoiState(roiState);
                    if ((byte) roiState == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue()) {
                        binding.roiIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_roi_green_30));
                    } else if ((byte) roiState == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue()) {
                        binding.roiIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_roi_green_50));
                    } else {
                        binding.roiIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_roi_off));
                    }
//                    tcpConnectionViewModel.getGpsPosition();
                }
            }));


            tcpConnectionViewModel.observeOpsinGetGpsLocation().observe(lifecycleOwner, new EventObserver<>(o -> {
                if (o != null) {
                    if (o instanceof CommandError) {
                        Log.e(TAG, "observeOpsinGetGpsLocation: " + ((CommandError) o).getError());
                    } else {
                        Log.e(TAG, "observeOpsinGetGpsLocation : " + o);
                        GpsPosition gpsPosition = (GpsPosition) o;
                        boolean powerStateOn = gpsPosition.isPowerStateOn();
                        boolean valid = gpsPosition.isValid();
                        double latitude = gpsPosition.getLatitude();
                        double longitude = gpsPosition.getLongitude();
                        char ns = gpsPosition.getNs();
                        char ew = gpsPosition.getEw();
                        int satelliteCount = gpsPosition.getSatelliteCount();
                        double mslAltitude = gpsPosition.getMslAltitude();
                        String time = gpsPosition.getTime();
                        String date = gpsPosition.getDate();
                        String degreeFormatLatitude = gpsPosition.getDegreeFormatLatitude();
                        String degreeFormatLongitude = gpsPosition.getDegreeFormatLongitude();

                        String value = "POWER_STATE: " + powerStateOn + "\n"
                                + " VALID: " + valid + "\n"
                                + " LATITUDE: " + latitude + "\n"
                                + " LONGITUDE: " + longitude + "\n"
                                + " N_S: " + ns + "\n"
                                + " E_W: " + ew + "\n"
                                + " NB_SATELLITE: " + satelliteCount + "\n"
                                + " MSL_ALTITUDE: " + mslAltitude + "\n"
                                + " TIME: " + time + "\n"
                                + " DATE: " + date + "\n"
                                + " DegreeFormatLatitude: " + degreeFormatLatitude + "\n"
                                + " DegreeFormatLongitude: " + degreeFormatLongitude;
                        if (latitude != 0 && longitude != 0) {
                            String latLan = getResources().getString(R.string.latitude_longitude, degreeFormatLatitude, degreeFormatLongitude);
                            binding.tvOpsinLatLong.setText(latLan);
                            // showLocationUI();
                        } else {
                            hideLocationUI();
                        }
                    }
                }
            }));


            //Pinch Zoom for Opsin
            binding.textureView.setListener((Touch) this);
            if (!isSelectArrow) {
                binding.monochromeIcon.setVisibility(GONE);
                binding.noiseIcon.setVisibility(GONE);
                binding.roiIcon.setVisibility(GONE);
                binding.arrowLeft.setVisibility(GONE);
                // here validate to show animation layout RISC-v v23 and above
                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    binding.arrowRight.setVisibility(VISIBLE);
                    binding.sdCardIcon.setVisibility(GONE);
                    binding.sdCardLabel.setVisibility(GONE);
                    binding.cameraNucIcon.setVisibility(GONE);
                    binding.cameraExpostureIcon.setVisibility(GONE);
                    binding.cameraFpsIcon.setVisibility(VISIBLE);
                    binding.scrollView.setVisibility(GONE);
                    leftToRightSlideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right);
                    rightToLeftSlideOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left);
                } else {
                    binding.arrowRight.setVisibility(GONE);
                    binding.sdCardIcon.setVisibility(GONE);
                    binding.sdCardLabel.setVisibility(GONE);
                    binding.cameraNucIcon.setVisibility(VISIBLE);
                    binding.cameraExpostureIcon.setVisibility(VISIBLE);
                    binding.cameraFpsIcon.setVisibility(VISIBLE);
                    binding.scrollView.setVisibility(VISIBLE);
                }
            } else {
                // here validate to show animation layout RISC-v v23 and above
                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    binding.cameraNucIcon.setVisibility(VISIBLE);
                    binding.cameraExpostureIcon.setVisibility(VISIBLE);
                    binding.cameraFpsIcon.setVisibility(VISIBLE);
                    binding.sdCardIcon.setVisibility(VISIBLE);
                    binding.sdCardLabel.setVisibility(VISIBLE);
                    binding.monochromeIcon.setVisibility(VISIBLE);
                    binding.noiseIcon.setVisibility(VISIBLE);
                    binding.roiIcon.setVisibility(VISIBLE);
                    binding.arrowRight.setVisibility(GONE);
                    binding.arrowLeft.setVisibility(VISIBLE);
                    binding.scrollView.setVisibility(VISIBLE);
                    leftToRightSlideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right);
                    rightToLeftSlideOut = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left);
                } else {
                    binding.arrowRight.setVisibility(GONE);
                    binding.sdCardIcon.setVisibility(GONE);
                    binding.sdCardLabel.setVisibility(GONE);
                    binding.cameraNucIcon.setVisibility(VISIBLE);
                    binding.cameraExpostureIcon.setVisibility(VISIBLE);
                    binding.cameraFpsIcon.setVisibility(VISIBLE);
                    binding.scrollView.setVisibility(VISIBLE);
                }
                Log.d(TAG, "isSelectArrow: called");
            }
            binding.arrowRight.setOnClickListener(view -> {
                if (!isSelectArrow) {
                    isSelectArrow = true;
                    binding.cameraNucIcon.setVisibility(VISIBLE);
                    binding.cameraExpostureIcon.setVisibility(VISIBLE);
                    binding.cameraFpsIcon.setVisibility(VISIBLE);
                    binding.sdCardIcon.setVisibility(VISIBLE);
                    binding.sdCardLabel.setVisibility(VISIBLE);
                    binding.monochromeIcon.setVisibility(VISIBLE);
                    binding.noiseIcon.setVisibility(VISIBLE);
                    binding.roiIcon.setVisibility(VISIBLE);
                    binding.arrowRight.setVisibility(GONE);
                    binding.arrowLeft.setVisibility(GONE);
                    binding.scrollView.setVisibility(VISIBLE);
                }
                leftToRightSlideIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        binding.arrowRight.setVisibility(GONE);
                        binding.arrowLeft.setVisibility(GONE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        binding.arrowLeft.setVisibility(VISIBLE);
                        binding.scrollView.setVisibility(VISIBLE);
                        binding.arrowRight.setVisibility(GONE);
                        isSelectArrow = true;
                        animation.reset();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                binding.animLayout.startAnimation(leftToRightSlideIn);
            });

            binding.arrowLeft.setOnClickListener(view -> {
                if (isSelectArrow) {
                    isSelectArrow = false;
                }
                rightToLeftSlideOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        binding.arrowLeft.setVisibility(GONE);
                        binding.arrowRight.setVisibility(GONE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        isSelectArrow = false;
                        binding.cameraNucIcon.setVisibility(GONE);
                        binding.cameraExpostureIcon.setVisibility(GONE);
                        //binding.cameraFpsIcon.setVisibility(View.GONE);
                        binding.sdCardIcon.setVisibility(GONE);
                        binding.sdCardLabel.setVisibility(GONE);
                        binding.monochromeIcon.setVisibility(GONE);
                        binding.noiseIcon.setVisibility(GONE);
                        binding.roiIcon.setVisibility(GONE);
                        binding.scrollView.setVisibility(GONE);

                        binding.arrowLeft.setVisibility(GONE);
                        binding.arrowRight.setVisibility(VISIBLE);
                        animation.reset();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                binding.animLayout.startAnimation(rightToLeftSlideOut);
            });
        } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)){
            //Nightwave
            binding.sliderTabLayout.setVisibility(GONE);
            binding.retryTxtDigitalcamera.setVisibility(GONE);
            binding.liveViewVideo.setOnClickListener(v -> {
                if (CameraViewModel.hasNewFirmware()) {
                    CameraViewModel.setHasPressedLiveViewButton(true); // for this condition to avoid stop live view on cancel settings dialog screen
                    CameraViewModel.setHasPressedSettingCancelIcon(false); // for this condition to avoid stop live view on cancel settings dialog screen
                    CameraViewModel.setHasVisibleSettingsInfoView(false); // for this condition some time not usb mode dialog
                    tcpConnectionViewModel.getCameraMode(); // here check camera mode

                    tcpConnectionViewModel.observeCameraMode().observe(this.getViewLifecycleOwner(), observeCameraMode);
                    tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
                } else {
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                        tcpConnectionViewModel.stopLiveView();
                    } else {
                        tcpConnectionViewModel.startLiveView(true);
                        binding.customProgressBar.setVisibility(VISIBLE);
                    }
                }
                setCropState();
            });
        } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
//            textureView = binding.videoContainer.getTextureView();
//            tcpConnectionViewModel.setVideoContainer(textureView);
//            binding.cameraPresetBtn.setAlpha(.5f);
//            binding.cameraPresetBtn.setEnabled(false);
            binding.image.setImageResource(R.drawable.ic_nw_digital_background);
            CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
            binding.cameraPictureInPictureBtn.setVisibility(GONE);


            setCropState();
            Log.e(TAG,"Streaming started : NW_digital");

            binding.liveViewVideo.setOnClickListener(v -> {
                if (binding.customProgressBar.getVisibility()  == VISIBLE)
                    binding.customProgressBar.setVisibility(GONE);

                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                    runOnUiThread(() -> {
                        if (binding.retryTxtDigitalcamera.getVisibility() == VISIBLE) {
                            binding.retryTxtDigitalcamera.setVisibility(GONE);
                            isLiveEnabled = false;
                            tcpConnectionViewModel.setStopRetryingCommand();
                        } else {
                            isLiveEnabled = true;
                            tcpConnectionViewModel.setStartStreamDigitalCamera();
                        }
                    });
                } else {
                    tcpConnectionViewModel.setStopStreamDigitalCamera();
                    isLiveEnabled = false;
                }
            });

            View switchRoot = binding.sliderTabLayout;
            isHighQuality = (currentStreamQuality == STREAM_QUALITY_HIGH);
            switchRoot.post(() -> updateSwitchUI(false));
            switchRoot.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float clickX = event.getX();
                    float half = v.getWidth() / 2f;
                    boolean clickedHQ = clickX > half;
                    if (clickedHQ == isHighQuality) {
                        //Same side clicked → do nothing
                        return true;
                    }
                    //Only change if different side clicked
                    isHighQuality = clickedHQ;
                    if (isHighQuality) {
                        Constants.currentStreamQuality = STREAM_QUALITY_HIGH;
                        Log.e(TAG, "Switched to HIGH resolution stream");
                    } else {
                        Constants.currentStreamQuality = STREAM_QUALITY_LOW;
                        Log.e(TAG, "Switched to LOW resolution stream");
                    }
                    if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED)
                        tcpConnectionViewModel.setChangeStreamingDigitalCamera();
                    updateSwitchUI(true);
                }
                return true;
            });

            // ui initiated as no data
            binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
            binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_start_live_view_white));
            binding.image.setVisibility(VISIBLE);
            binding.videoContainer.setVisibility(GONE);
            binding.textureView.setVisibility(GONE);
            binding.videoContainer.attachLister((Touch) this);
            binding.retryTxtDigitalcamera.setVisibility(GONE);
            binding.sliderTabLayout.setVisibility(VISIBLE);

            tcpConnectionViewModel.observeStreamingStatus().observe(lifecycleOwner, aBoolean -> {
                Log.i(TAG, "observeDigitalCameraStreamingStatus: " + aBoolean + " videoSurface is " + (videoSurface != null ? videoSurface.isValid() : "not valid" ));
                binding.image.setImageResource(R.drawable.ic_nw_digital_background);

                if (aBoolean) {
                    binding.videoContainer.setVisibility(VISIBLE);// set visible only to get surface texture for rendering
                    textureView = binding.videoContainer.getTextureView();
                    tcpConnectionViewModel.setVideoContainer(textureView);
                    binding.retryTxtDigitalcamera.setVisibility(GONE);

                    binding.image.setVisibility(VISIBLE);
                    textureView.setOpaque(false);// to avoid blcok screen

//                    if (videoSurface == null)
//                        tcpConnectionViewModel.setVideoContainer(binding.videoContainer.getTextureView());
//                    binding.retryTxtDigitalcamera.setVisibility(GONE);
                    // to avoid the black screen in texture view for very first time
//                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                        binding.videoContainer.setVisibility(GONE);
//                    }, 100);
//                    if (!isLiveEnabled){
//                        binding.videoContainer.setVisibility(VISIBLE);
//                    }
                } else {
//                    new Handler().postDelayed(() -> binding.customProgressBar.setVisibility(GONE), 500);
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_start_live_view_white));
                    binding.image.setVisibility(VISIBLE);
                    binding.videoContainer.setVisibility(GONE);
                    setRecordOptionLayoutDisabled(false);
                    binding.videoContainer.getTextureView().setAlpha(0f);//transparent
                    binding.videoContainer.reset();
                }
            });


            tcpConnectionViewModel.observeStreamingReadyToVisible().observe(lifecycleOwner,observeStreamingReadyToVisible);


            tcpConnectionViewModel.observeLiveFps().observe(lifecycleOwner, fpsValue -> {
                // print live fps value in text view
                isFpsReceived = true;

            });

            tcpConnectionViewModel.observeOnStreamingErrorCode().observe(lifecycleOwner,observeStreamingErrorCode);

            tcpConnectionViewModel.observeRetryCountData().observe(lifecycleOwner,observeRetryValue);

            tcpConnectionViewModel.observeBufferingState().observe(lifecycleOwner, isBuffering -> {
                if (isBuffering) {
                    Log.e(TAG, "subscribeUI: observeBufferingState " + isBuffering );
                    binding.customProgressBar.setVisibility(VISIBLE);
                    setSwitchEnabled(false);// HQ/STD switch disabled
                } else {
                    binding.customProgressBar.setVisibility(GONE);
                    setSwitchEnabled(true);// HQ/STD switch enabled
                }
            });

            tcpConnectionViewModel.observerNoFpsFoundForDigital().observe(lifecycleOwner, timerStopped -> {
                Log.d(TAG,"observerNoFpsFoundForDigital " + timerStopped); // no packets
               /* if (timerStopped){
                    tcpConnectionViewModel.setStopStreamDigitalCamera();
                    showCameraClosureDialog(getString(R.string.nwd_camera_closure_dialog));
                }*/
            });

            tcpConnectionViewModel.notifyDigitalMediaVideoSize().observe(lifecycleOwner,mediaSize -> {

                VIDEO_WIDTH = (int)(mediaSize & 0xFFFFFFFFL);
                VIDEO_HEIGHT = (int)((mediaSize >> 32) & 0xFFFFFFFFL);

                Log.e(TAG, "notifyDigitalMediaVideoSize: width " + VIDEO_WIDTH + " height " + VIDEO_HEIGHT);
                resizeVideo(VIDEO_WIDTH,VIDEO_HEIGHT);
            });

        }

        binding.analysisBtn.setVisibility(GONE);
        binding.analyticsLayout.setVisibility(GONE);
        cameraViewModel.isUpdateAspectRatioButtonState.observe(lifecycleOwner, eventObserver);

        cameraViewModel.enableObserver.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                cameraViewModel.isUpdateAspectRatioButtonState.observe(lifecycleOwner, eventObserver);
            }
        }));

        cameraViewModel.isSelectCameraPreset.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                cameraViewModel.setSelectPreset(true);
                binding.compassView.setVisibility(GONE);
                avoidDoubleClicks(binding.cameraPresetBtn);
                CameraViewModel.setHasAppBackgroundToForeground(false);
                cameraViewModel.setShowExpandMenu(true);
                cameraViewModel.setIsShowAlertDialog(true);
                hasShowExpandMenu();
                homeViewModel.getNavController().navigate(R.id.cameraPresetDialogFragment);
            }
        }));


        tcpConnectionViewModel.observeLiveViewErrorMessage().observe(lifecycleOwner, s -> {
            if (s != null) {
                if (s.equalsIgnoreCase("Buffering")) {
                    binding.image.setBackgroundColor(Color.parseColor("#000000"));
                    binding.customProgressBar.setVisibility(VISIBLE);
                    binding.waitingMsg.setText(s);
                } else if (s.equalsIgnoreCase("ip")) {
                    String cameraIpAddress = getCameraIpAddress(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    Log.e(TAG, "cameraIpAddress: " + cameraIpAddress);
                } else {
                    binding.error.setVisibility(VISIBLE);
                    binding.error.setText(s);
                }
            } else {
                binding.customProgressBar.setVisibility(GONE);
                binding.waitingMsg.setText("");
            }
        });

        tcpConnectionViewModel.observeStartLiveView().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o instanceof Byte) {
                Log.e(TAG, "observeStartLiveView:  " + (byte) o);
                if (isSelectPIP) {
                    binding.cameraHomeBtn.setVisibility(GONE);
                    binding.cameraMenuView.setVisibility(GONE);
                }
                if ((byte) o > 0) {
                    cameraViewModel.setHasDismissPleaseWaitProgressDialog(true);
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED;
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_live_view_white));
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                    setRecordOptionLayoutDisabled(true);
                    //on Live Start state -Button state has been changed
                    //  binding.recordBtn.setVisibility(View.VISIBLE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(VISIBLE);
                    binding.cameraAspectRatioBtn.setVisibility(VISIBLE);
                    binding.cameraPictureInPictureBtn.setVisibility(VISIBLE);
                    ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                    imageMatrixTouchHandler.animateZoomOutToFit(500);
                    removeAspectRatioObserver();
                } else {
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_start_live_view_white));
                    setRecordOptionLayoutDisabled(false);
                }
            }
        }));

        tcpConnectionViewModel.observeStopLiveView().observe(lifecycleOwner, new EventObserver<>(o -> {
            if (o instanceof Byte) {
                Log.e(TAG, "observeStopLiveView: " + (byte) o);
                cameraViewModel.setLiveViewStopped(true);
                CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_rectangle_svg));
                binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_start_live_view_white));
                binding.image.setImageResource(R.drawable.ic_camera_background);
                setRecordOptionLayoutDisabled(false);
                imageMatrixTouchHandler.animateZoomOutToFit(50);
                isFirstBitmapReceived = false;
                //on Live Stop state -Button state has been changed
                binding.recordBtn.setVisibility(GONE);
                recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(VISIBLE);
                binding.cameraAspectRatioBtn.setVisibility(GONE);
                binding.cameraPictureInPictureBtn.setVisibility(GONE);
                ((ViewGroup.MarginLayoutParams) binding.cameraPictureInPictureBtn.getLayoutParams()).topMargin = 0;
                ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
             /*   // below this condition settings screen video mode switch to usb to stop live view after live view stopped check video mode state an proceed
                if (CameraViewModel.hasNewFirmware()) {
                    cameraViewModel.setHasStopLiveViewOnSettings(true);
                }*/
                if (binding.customProgressBar.getVisibility() == VISIBLE) {
                    binding.customProgressBar.setVisibility(GONE);
                }
            }
        }));

        tcpConnectionViewModel.observeHasRecordStoppedDuetoNoLivePackets().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.e(TAG, "observeHasRecordStoppedDuetoNoLivePackets: update Recording state");
                Toast.makeText(requireActivity(), getString(R.string.recording_stopped_no_live_data), Toast.LENGTH_SHORT).show();
                updateRecordScreen();
            }
        }));

        tcpConnectionViewModel.observeHasSendLiveViewCommand().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.e(TAG, "subscribeUI: observeHasSendLiveViewCommand " + aBoolean );
            if (!aBoolean) {
                if (binding.customProgressBar.getVisibility() == VISIBLE) {
                    binding.customProgressBar.setVisibility(GONE);
                }
                if (binding.waitingMsg.getVisibility() == VISIBLE) {
                    binding.waitingMsg.setVisibility(GONE);
                }
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE))
                    showAllPacketZeroDialog(getString(R.string.noudp));
            }
        }));

        //binding.cameraSettings.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                if (CameraViewModel.analyticsButtonState == CameraViewModel.AnalyticsButtonState.Analytics_STARTED) {
//                    CameraViewModel.analyticsButtonState = CameraViewModel.AnalyticsButtonState.Analytics_STOPPED;
//                    binding.analyticsLayout.setVisibility(View.GONE);
//                } else {
//                    CameraViewModel.analyticsButtonState = CameraViewModel.AnalyticsButtonState.Analytics_STARTED;
//                    binding.analyticsLayout.setVisibility(View.VISIBLE);
//                }
//                return true;
//            }
//        });
        tcpConnectionViewModel.getValueUpdateLiveViewBitmap().observe(lifecycleOwner, bitmap -> {
            if (!currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE))
                return;

            if (bitmap != null) {
                if (binding.customProgressBar.getVisibility() == VISIBLE) {
                    binding.customProgressBar.setVisibility(GONE);
                }
                if (binding.waitingMsg.getVisibility() == VISIBLE) {
                    binding.waitingMsg.setVisibility(GONE);
                }
                binding.image.setImageBitmap(bitmap);
                if (!isFirstBitmapReceived) {
                    imageMatrixTouchHandler.animateZoomOutToFit(50);
                    isFirstBitmapReceived = true;
                }
                /*//Live stream is disabled only when GIF is started Live-stream is started
                if(captureGif){
                    binding.image.setImageBitmap(bitmap);
                    bitmapList.add(bitmap);
                }*/
            } else {
                Log.e(TAG, "subscribeUI: getValueUpdateLiveViewBitmap");
            }
        });

        cameraViewModel.isCropVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                cameraViewModel.isUpdateAspectRatioButtonState.observe(lifecycleOwner, eventObserver);
                Log.e(TAG, "subscribeUI: isCropVideo " + isSelectPIP + " ");
                //    if (!isSelectPIP) {
                if (videoCropState == CameraViewModel.VideoCropState.CROP_ON) {
                    ((ImageView) binding.image).setScaleType(ImageView.ScaleType.FIT_CENTER);
                    setCropState();
                    aspectRatioState = false;
                } else {
                    ((ImageView) binding.image).setScaleType(ImageView.ScaleType.CENTER_CROP);
                    setCropState();
                    aspectRatioState = true;
                }
                //     }
            }
        }));

        cameraViewModel.observeAllPacketZero().observe(lifecycleOwner, new EventObserver<>(hasSelect ->
        {
            if (hasSelect) {
                backToHome();
            }
        }));

        //addition: Background to Foreground UI handled
        cameraViewModel.observeHasShowLiveScreen().observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                binding.cameraMenuView.post(() -> {
                    CameraViewModel.onButtonState = true;
                    binding.cameraRecordViewScreen.setVisibility(GONE);
                    binding.cameraRecordViewPauseMenu.setVisibility(GONE);
                    binding.cameraRecordScreen.setVisibility(GONE);
                    binding.cameraMenuLayout.setVisibility(GONE);
                    if (binding.cameraMenuLayout.getVisibility() == GONE) {
                        binding.cameraHomeBtn.setVisibility(GONE);
                    }
                    if (binding.cameraMenuView.getVisibility() == VISIBLE) {
                        binding.cameraHomeBtn.setVisibility(VISIBLE);
                    }
                    seconds = 0;

                    /*here background to foreground while start recording file to be delete*/
                    if (filepath != null && !filepath.isEmpty() && timerState) {
                        timerState = false;
                        File file = new File(filepath);
                        if (file.exists() && file.isFile() && file.canWrite()) {
                            file.delete();
                            timerState = false;
                        }
                    }
                });
            }
        }));


        /* for this save recorded video while wifi disconnection if start video recording only*/
        cameraViewModel.observeHasSaveRecordedVideo().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    CameraViewModel.onButtonState = true;
                    binding.cameraRecordViewScreen.setVisibility(GONE);
                    binding.cameraRecordViewPauseMenu.setVisibility(GONE);
                    binding.cameraRecordScreen.setVisibility(GONE);
                    binding.cameraMenuLayout.setVisibility(GONE);
                    if (binding.cameraMenuLayout.getVisibility() == GONE) {
                        binding.cameraHomeBtn.setVisibility(GONE);
                    }
                    if (binding.cameraMenuView.getVisibility() == VISIBLE) {
                        binding.cameraHomeBtn.setVisibility(VISIBLE);
                    }
                    seconds = 0;
                    if (timerState) {
                        timerState = false;
                        binding.cameraRecordTimerLayout.setVisibility(GONE);
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                cameraViewModel.stopRecordingVideo();
                                break;
                            case OPSIN:
                                tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG, "stopRecording : NW_Digital");
                                // uncomment for video record and Image capture
                                tcpConnectionViewModel.stopDigitalVideoRecordLocally();
                                break;
                        }
                    }
                });
            }
        }));

        tcpConnectionViewModel.observeAllPacketZero().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.e(TAG, "subscribeUI: observeAllPacketZero " + aBoolean );
            if (aBoolean) {
                if (binding.customProgressBar.getVisibility() == VISIBLE) {
                    binding.customProgressBar.setVisibility(GONE);
                }
                if (binding.waitingMsg.getVisibility() == VISIBLE) {
                    binding.waitingMsg.setVisibility(GONE);
                }
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                    case OPSIN:
                        showAllPacketZeroDialog(getString(R.string.noudp));
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"0 UDP : NW_Digital");
                        break;
                }
            }
        }));

        cameraViewModel.isVisibleStates.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.analyticsLayout.getVisibility() != VISIBLE)
                    binding.analyticsLayout.setVisibility(VISIBLE);
                else
                    binding.analyticsLayout.setVisibility(GONE);
            }
        }));

        tcpConnectionViewModel.getValueUpdateLiveViewStats().observe(lifecycleOwner, rtpStats -> {
            if (rtpStats != null) {
                /*Opsin*/
                binding.opsinFrameSize.setVisibility(GONE);
                binding.opsinFps.setVisibility(GONE);
                binding.opsinOutOfOrder.setVisibility(GONE);
                binding.opsinRtpReceived.setVisibility(GONE);
                /*Nw*/
                String goodFrames = getString(R.string.frames) + rtpStats.getGoodFrames();
                binding.goodPacket.setText(goodFrames);
                binding.size.setText(getString(R.string.size) + rtpStats.getLastReceivedFrameSize());
                binding.fps.setText(getString(R.string.fps) + rtpStats.getFps());
                binding.receivedUdpPacket.setText(getString(R.string.received) + rtpStats.getReceivedUdpPackets());
                binding.rtpSkippedPacket.setText(getString(R.string.skipped) + rtpStats.getSkippedPackets());
                binding.jpegBadStart.setText(getString(R.string.bad_start) + rtpStats.getJpegBadStart());
                binding.jpegBadEnd.setText(getString(R.string.bad_end) + rtpStats.getJpegBadEnd());
                binding.jpegCorrupted.setText(getString(R.string.bad_offset) + rtpStats.getJpegBadOffset());
            }
        });
        binding.cameraViewRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.setOnTouchListener(this);
                showAutohideLayout();
                return true;
            }
        });

        cameraViewModel.isSelectSettings.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (!isInfoButtonPressed && !isSettingButtonPressed) {
                isSettingButtonPressed = true;
                avoidDoubleClicks(binding.cameraRightMenuSetting);
                avoidDoubleClicks(binding.cameraLeftMenuSetting);
                CameraViewModel.setHasAppBackgroundToForeground(false);
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setIsShowAlertDialog(true);
                hasShowExpandMenu();
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                    try {
                        homeViewModel.getNavController().navigate(R.id.cameraFragmentToCameraSettingFragment); // CameraSettingDialog Fragment
                    } catch (Exception e) {
                        Log.e("CameraFragment", "Exception thrown " + e.getMessage());
                    }
                } else {
                    tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
                    tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
                    homeViewModel.getNavController().navigate(R.id.cameraSettingsDialogFragment); // cameraSettingTabLayoutDialog fragment
                }
            }
        }));

        cameraViewModel.isSelectHome.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                backToHome();
            }
        }));

        cameraViewModel.isSelectInfo.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (!isInfoButtonPressed && !isSettingButtonPressed) {
                    isInfoButtonPressed = true;
                    avoidDoubleClicks(binding.cameraRightMenuInfo);
                    avoidDoubleClicks(binding.cameraLeftMenuInfo);
                    CameraViewModel.setHasAppBackgroundToForeground(false);
                    cameraViewModel.setShowExpandMenu(true);
                    cameraViewModel.setIsShowAlertDialog(true);
                    hasShowExpandMenu();
                    homeViewModel.getNavController().navigate(R.id.cameraInfoTabDialogFragment);
                }
            }
        }));

        cameraViewModel.isSelectCameraMenu.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                binding.cameraMenuLayout.setVisibility(GONE);
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(VISIBLE);
                binding.compassView.setVisibility(GONE);
            }
        }));

        //addition: Select Picture_in_Picture
        pictureInPictureParams = new PictureInPictureParams.Builder().build();
        cameraViewModel.isSelectPictureInPicture.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                Log.e(TAG, "isSelectPictureInPicture: PIP " + CameraViewModel.recordButtonState );
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    binding.textureView.reset();

                    isSelectPIP = true;
                    binding.cameraMenuView.setVisibility(GONE);
                    binding.cameraHomeBtn.setVisibility(GONE);
                    if (binding.compassView.getVisibility() == VISIBLE)
                        binding.compassView.setVisibility(GONE);
                    if (getActivity() != null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        PackageManager packageManager = getActivity().getPackageManager();
                        boolean isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
                        if (isEnabled) {
                            getActivity().enterPictureInPictureMode(pictureInPictureParams);
                        } else {
                            Log.e(TAG, "subscribeUI: PIP disabled in settings");

                        }
                    }
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
                }
            }
        }));

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        //addition: Select Menu
        cameraViewModel.isSelectMenuView.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    homeViewModel.menuState = true;
                }
                binding.cameraMenuLayout.setVisibility(VISIBLE);
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                    /*binding.cameraRightMenuInfo.setEnabled(false);
                    binding.cameraLeftMenuInfo.setEnabled(false);
                    binding.cameraRightMenuInfo.setAlpha(.5f);
                    binding.cameraLeftMenuInfo.setAlpha(.5f);*/
                    binding.cameraRightMenuInfo.setVisibility(GONE);
                    binding.cameraLeftMenuInfo.setVisibility(GONE);
                }
                binding.cameraMenuView.setVisibility(GONE);
                mHandler.removeCallbacks(runnable);
                binding.cameraHomeBtn.setVisibility(VISIBLE);
                binding.compassView.setVisibility(GONE);

                Log.d("sharedPreftTAG", String.valueOf(sharedPref.getBoolean("leftMenuView", false)));
                if (sharedPref.getBoolean("leftMenuView", false)) {
                    binding.cameraLeftMenuView.setVisibility(VISIBLE);
                    binding.cameraRightMenuView.setVisibility(GONE);
                } else {
                    binding.cameraRightMenuView.setVisibility(VISIBLE);
                    binding.cameraLeftMenuView.setVisibility(GONE);
                }
                binding.startRecord.setVisibility(GONE);
            }
        }));

        //addition: camera live view screen
        cameraViewModel.isSelectCameraLiveViewScreen.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(GONE);
            }
        }));

        //addition: camera left menu arrow
        cameraViewModel.isSelectLeftMenuArrow.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(VISIBLE);
                binding.cameraLeftMenuView.setVisibility(GONE);
                binding.cameraRightMenuView.setVisibility(VISIBLE);
                binding.startRecord.setVisibility(GONE);
                homeViewModel.leftMenuView = false;
                editor.putBoolean("leftMenuView", false);
                editor.apply();
            }
        }));

        //addition: camera right menu arrow
        cameraViewModel.isSelectRightMenuArrow.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(VISIBLE);
                binding.cameraLeftMenuView.setVisibility(VISIBLE);
                binding.cameraRightMenuView.setVisibility(GONE);
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    binding.startRecord.setVisibility(GONE);
                }
                homeViewModel.leftMenuView = true;
                editor.putBoolean("leftMenuView", true);
                editor.apply();
            }
        }));
        //addition: camera Record start
//        cameraViewModel.isSelectCameraRecord.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
//            if (hasSelect) {
//                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
//                    // here check which button select based called below methods
//                    cameraViewModel.onSelectImageCaptureOptions();
//                    //or
//                    cameraViewModel.onSelectVideoRecordOptions();
//
//                    setRecordView();
//                    isRecordingStarted = false;
//                } else {
//                    Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
//                }
//            }
//        }));

        cameraViewModel.isSelectCameraRecordType.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            // here check which button select based called below methods
            if (hasSelect) {
                // image
       /*         if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN) {
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(View.GONE);
                    recordOptionLayoutBinding.cameraRecordOptionLayout.setVisibility(View.VISIBLE);
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                        recordOptionLayoutBinding.mobileRecordBtn.setEnabled(false);
                        recordOptionLayoutBinding.mobileRecordBtn.setAlpha(0.5f);
                    } else {
                        recordOptionLayoutBinding.mobileRecordBtn.setEnabled(true);
                        recordOptionLayoutBinding.mobileRecordBtn.setAlpha(1.0f);
                    }
                    isRecordingStarted = false;
                    CameraViewModel.hasSelectRecordVideoButton = false;
                } else {
                    // night wave capture image and save to mobile
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                        binding.cameraMenuView.setVisibility(View.GONE);
                        binding.cameraHomeBtn.setVisibility(View.GONE);
                        binding.compassView.setVisibility(View.GONE);
                        cameraViewModel.onSelectCameraCapture();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
                    }
                }*/

                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    binding.cameraMenuView.setVisibility(GONE);
                    binding.cameraHomeBtn.setVisibility(GONE);
                    binding.compassView.setVisibility(GONE);
                    cameraViewModel.onSelectCameraCapture();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
                }
            } else {
                // video
             /*   if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(View.GONE);
                    recordOptionLayoutBinding.cameraRecordOptionLayout.setVisibility(View.VISIBLE);
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                        recordOptionLayoutBinding.mobileRecordBtn.setEnabled(false);
                        recordOptionLayoutBinding.mobileRecordBtn.setAlpha(0.5f);
                    } else {
                        recordOptionLayoutBinding.mobileRecordBtn.setEnabled(true);
                        recordOptionLayoutBinding.mobileRecordBtn.setAlpha(1.0f);
                    }
                    CameraViewModel.hasSelectRecordVideoButton = true;
                    isRecordingStarted = false;
                } else {
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                        // night wave video record and save to mobile
                        cameraViewModel.onCameraLiveViewMenuVideo();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
                    }
                }*/
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    // night wave video record and save to mobile
                    cameraViewModel.onCameraLiveViewMenuVideo();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
                }
            }
        }));

        /*for this while back press galley view layout to show record view menu screen*/
        CameraViewModel.isShowRecordViewMenuLayout.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            Log.d("backTAG", "isShowRecordViewMenuLayout: ");
            if (hasSelect) {
                if (!isRecordingStarted) {
                    // setRecordView();
                    binding.cameraMenuView.setVisibility(VISIBLE);
                    binding.cameraHomeBtn.setVisibility(VISIBLE);
                    switch (currentCameraSsid) {
                        case OPSIN:
                            binding.compassView.setVisibility(VISIBLE);
                            break;
                        case NIGHTWAVE:
                            binding.compassView.setVisibility(GONE);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"RecordView : NE_Digital");
                            break;
                    }
                }
            }
        }));

        //addition: camera record view video menu - Cancel Button
        cameraViewModel.isSelectCameraRecordViewMenuCancel.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                binding.cameraMenuView.setVisibility(VISIBLE);
                visibleAutohideLayout();
                binding.cameraRecordViewScreen.setVisibility(GONE);
            }
        }));

        //addition: Camera Record View Video menu -Video Button
        cameraViewModel.isSelectCameraRecordViewMenuVideo.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    StorageManager storageManager = (StorageManager) getActivity().getSystemService(Context.STORAGE_SERVICE);
                    if (storageManager != null) {
                        long availableInternalStorageSize = getAvailableInternalStorageSize();
                        Log.d(TAG, "Available Internal/External free Space: " + availableInternalStorageSize + "GB");
                        if (availableInternalStorageSize > minimumStorageForVideo) {
                            showRecordViewLayout();
                        } else {
                            showStorageAlert(getString(R.string.video_storage));
                        }
                    }
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();

                }
            }
        }));

        //addition: Camera Record icon near timer
        cameraViewModel.isSelectRecord.observe(lifecycleOwner, new EventObserver<>(hasSelect -> showRecordScreenMenu()));

        //addition: Camera Record Menu -Pause icon is selected
        cameraViewModel.isSelectCameraRecordMenuPause.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                isRecordingStarted = false;
                cameraViewModel.setIsCapturedImageInLive(false);
                binding.cameraRecordTimerLayout.setVisibility(GONE);
                binding.cameraRecordViewPauseMenu.setVisibility(GONE);
                binding.cameraRecordSaveLayout.setVisibility(VISIBLE);
                binding.cameraRecordSaveLayoutSave.setVisibility(VISIBLE);
                binding.cameraRecordSaveLayoutDelete.setVisibility(VISIBLE);
                binding.cameraRecordSaveLayoutConfirmDelete.setVisibility(GONE);
                binding.cameraRecordSaveLayoutDuration.setVisibility(VISIBLE);
                binding.cameraRecordSaveLayoutCancel.setVisibility(GONE);

                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        cameraViewModel.stopRecordingVideo();
                        break;
                    case OPSIN:
                        tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"recordPaused : NW_Digital");
                        // uncomment for video record and Image capture
                        tcpConnectionViewModel.stopDigitalVideoRecordLocally();
                        break;
                }
                timerState = false;

                if (filepath != null && !filepath.isEmpty()) {
                    File file = new File(filepath);
                    String fileName = file.getName();
                    /* for this split camera ssid and only allow timestamp */
                    String[] originalFileName = fileName.split("_");
                    int index = originalFileName[2].lastIndexOf('.');
                    if (index != -1) {
                        String substring = originalFileName[2].substring(0, index);
                        long millisecond = Long.parseLong(substring);
                        String dateString = dateFormat.format(new Date(millisecond));
                        binding.cameraRecordViewChoiceTitle.setText(dateString.toUpperCase());
                        binding.cameraRecordViewChoiceTitle.setEllipsize(TextUtils.TruncateAt.END);
                    }
                    if (file.exists()) {
                        Log.e(TAG, "subscribeUI: " + filepath);
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                Glide.with(requireActivity()).load(filepath)
                                        .placeholder(  R.drawable.ic_camera_background )
                                        .into((ImageView) binding.cameraRecordSaveLayoutThumbnail);
                                break;
                            case OPSIN:
                                Glide.with(requireActivity()).load(filepath)
                                        .placeholder(R.drawable.opsin_live_view_background)
                                        .into((ImageView) binding.cameraRecordSaveLayoutThumbnail);
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG, "FileSaved : NW_Digital");
                                try {
                                    binding.cameraRecordSaveLayoutThumbnail.setImageResource(R.drawable.ic_nw_digital_background);
                                    MediaScannerConnection.scanFile(requireActivity(),
                                            new String[]{filepath},
                                            new String[]{"video/mp4"},
                                            (path, uri) -> {
                                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    Bitmap bitmap = getVideoThumbnail(filepath);
                                                    if (bitmap != null && !bitmap.isRecycled() && bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                                                        String frameQuality = detectDominantColor(bitmap);
                                                        Log.e(TAG, "frameQuality : " + frameQuality);
                                                        if (frameQuality.contains("VALID")) {
                                                            binding.cameraRecordSaveLayoutThumbnail.setImageBitmap(bitmap);
                                                        } else {
                                                            binding.cameraRecordSaveLayoutThumbnail.setImageBitmap(binding.videoContainer.getTextureView().getBitmap());
                                                        }
                                                    } else {
                                                        binding.cameraRecordSaveLayoutThumbnail.setImageBitmap(binding.videoContainer.getTextureView().getBitmap());
                                                    }
                                                }, 1000);
                                            });

                                } catch (Exception e) {
                                    binding.cameraRecordSaveLayoutThumbnail.setImageResource(R.drawable.ic_nw_digital_background);
                                    Log.e(TAG, "FileSaved : NW_Digital " + e.getMessage());
                                }

                                break;
                        }
                    }
                }
            }
        }));

        //addition: Camera Record Menu -Cancel icon is selected
        cameraViewModel.isSelectCameraRecordMenuCancel.observe(lifecycleOwner, new EventObserver<>(hasSelect -> binding.cameraRecordViewPauseMenu.setVisibility(GONE)));

        //addition: Camera Record with Choice layout - Save icon is selected
        cameraViewModel.isSelectCameraRecordSave.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            cameraViewModel.onSaveImage = true;
            binding.cameraRecordSaveLayoutDelete.setVisibility(GONE);
            binding.cameraRecordSaveLayoutSave.setVisibility(GONE);
            binding.cameraRecordSaveLayoutConfirmDelete.setVisibility(GONE);
            binding.cameraRecordSaveLayoutCancel.setVisibility(GONE);
            binding.cameraRecordSaveLayoutRecordingSaved.setVisibility(VISIBLE);
            mHandler.postDelayed(() -> {
                binding.cameraRecordSaveLayout.setVisibility(GONE);
                binding.cameraRecordScreen.setVisibility(GONE);
                binding.cameraRecordSaveLayoutRecordingSaved.setVisibility(GONE);
                binding.cameraRecordSaveLayoutCancel.setVisibility(GONE);
                binding.cameraMenuView.setVisibility(VISIBLE);
                showAutohideLayout();
                binding.cameraHomeBtn.setVisibility(VISIBLE);
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        binding.compassView.setVisibility(GONE);
                        break;
                    case OPSIN:
                        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                            if (!isSelectPIP)
                                binding.compassView.setVisibility(VISIBLE);
                        } else {
                            binding.compassView.setVisibility(GONE);
                        }
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"Cancel from menu : NW_digital");
                        break;
                }
                if (cameraViewModel.isCapturedImage) {
                    // cameraViewModel.captureImage(homeViewModel.getCurrentFwVersion().getRiscv());
                    Toast.makeText(getActivity(), getString(R.string.photo_capture), Toast.LENGTH_SHORT).show();
                    binding.cameraRecordViewMenu.setVisibility(GONE);
                    binding.cameraRecordScreen.setVisibility(GONE);
                    binding.cameraMenuView.setVisibility(VISIBLE);
                    cameraViewModel.setIsCapturedImageInLive(false);
                    visibleAutohideLayout();
                    binding.cameraRecordViewScreen.setVisibility(GONE);
                    if (isRecordingStarted) {
                        binding.cameraRecordTimerLayout.setVisibility(VISIBLE);
                        binding.cameraRecordScreen.setVisibility(VISIBLE);
                        binding.cameraMenuView.setVisibility(GONE);
                        binding.cameraHomeBtn.setVisibility(GONE);
                        binding.compassView.setVisibility(GONE);
                        binding.cameraRecordButton.setClickable(true);
                    } else {
                        binding.cameraMenuView.setVisibility(VISIBLE);
                        showAutohideLayout();
                        binding.cameraHomeBtn.setVisibility(VISIBLE);
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                binding.compassView.setVisibility(GONE);
                                break;
                            case OPSIN:
                                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                                    if (!isSelectPIP)
                                        binding.compassView.setVisibility(VISIBLE);
                                } else {
                                    binding.compassView.setVisibility(GONE);
                                }
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG,"captured img : NW_Digital");
                                break;
                        }
                    }
                } else {
                    timerState = false; //resetting the timer
                    seconds = 0;
                    Toast.makeText(getActivity(), getString(R.string.video_saved), Toast.LENGTH_SHORT).show();
                }

                //  cameraViewModel.stopRecordingVideo();
            }, 500);
        }));

        //addition: Camera Record with Choice layout - delete icon is selected
        cameraViewModel.isSelectCameraRecordDelete.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            binding.cameraRecordSaveLayoutDelete.setVisibility(GONE);
            binding.cameraRecordSaveLayoutSave.setVisibility(GONE);
            binding.cameraRecordSaveLayoutConfirmDelete.setVisibility(VISIBLE);
            binding.cameraRecordSaveLayoutRecordingSaved.setVisibility(GONE);
            binding.cameraRecordSaveLayoutCancel.setVisibility(VISIBLE);
            showAutohideLayout();
            if (cameraViewModel.isCapturedImage) {
                visibleAutohideLayout();
            }
        }));

        //addition: Camera Record with Choice layout - Confirm Delete icon is selected
        cameraViewModel.isSelectCameraRecordConfirmDelete.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            binding.cameraRecordSaveLayout.setVisibility(GONE);
            binding.cameraRecordScreen.setVisibility(GONE);
            binding.cameraRecordSaveLayoutRecordingSaved.setVisibility(GONE);
            binding.cameraRecordSaveLayoutCancel.setVisibility(GONE);
            binding.cameraMenuView.setVisibility(VISIBLE);
            showAutohideLayout();
            binding.cameraHomeBtn.setVisibility(VISIBLE);
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    binding.compassView.setVisibility(GONE);
                    break;
                case OPSIN:
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        if (!isSelectPIP)
                            binding.compassView.setVisibility(VISIBLE);
                    } else {
                        binding.compassView.setVisibility(GONE);
                    }
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"ConfirmDelete : NW_Digital");
                    break;
            }
            /*here saved file to be delete*/
            if (cameraViewModel.isCapturedImage) {
                if (imageFilePath != null && !imageFilePath.isEmpty()) {
                    File file = new File(imageFilePath);
                    if (file.exists() && file.isFile() && file.canWrite()) {
                        file.delete();
                        Toast.makeText(requireActivity(), getString(R.string.delete_image), Toast.LENGTH_SHORT).show();
                        cameraViewModel.setIsCapturedImageInLive(false);
                    }
                }
                /*binding.cameraMenuView.setVisibility(View.VISIBLE);
                visibleAutohideLayout();*/
                binding.cameraMenuView.setVisibility(VISIBLE);
                visibleAutohideLayout();
                binding.cameraRecordViewScreen.setVisibility(GONE);

                if (isRecordingStarted) {
                    binding.cameraRecordTimerLayout.setVisibility(VISIBLE);
                    binding.cameraRecordScreen.setVisibility(VISIBLE);
                    binding.cameraMenuView.setVisibility(GONE);
                    binding.cameraHomeBtn.setVisibility(GONE);
                    binding.compassView.setVisibility(GONE);
                    binding.cameraRecordButton.setClickable(true);
                } else {
                    binding.cameraMenuView.setVisibility(VISIBLE);
                    showAutohideLayout();
                    binding.cameraHomeBtn.setVisibility(VISIBLE);
                    switch (currentCameraSsid) {
                        case OPSIN:
                            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                                if (!isSelectPIP)
                                    binding.compassView.setVisibility(VISIBLE);
                            } else {
                                binding.compassView.setVisibility(GONE);
                            }
                            break;
                        case NIGHTWAVE:
                            binding.compassView.setVisibility(GONE);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"Captured img delete : NW_Digital");
                            break;
                    }
                    binding.cameraRecordScreen.setVisibility(GONE);
                }


            } else {
                timerState = false;
                seconds = 0;
                if (filepath != null && !filepath.isEmpty()) {
                    File file = new File(filepath);
                    if (file.exists() && file.isFile() && file.canWrite()) {
                        file.delete();
                        Toast.makeText(requireActivity(), getString(R.string.delete_videos), Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }));

        //addition: Camera Record with Choice layout - Cancel icon is selected
        cameraViewModel.isSelectCameraRecordViewCancel.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            binding.cameraRecordSaveLayoutConfirmDelete.setVisibility(GONE);
            binding.cameraRecordSaveLayoutCancel.setVisibility(GONE);
            binding.cameraRecordSaveLayoutDelete.setVisibility(VISIBLE);
            binding.cameraRecordSaveLayoutSave.setVisibility(VISIBLE);
        }));

        //addition: Camera Capture
        cameraViewModel.isSelectCameraCapture.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    StorageManager storageManager = (StorageManager) getActivity().getSystemService(Context.STORAGE_SERVICE);
                    if (storageManager != null) {
                        long availableInternalStorageSize = getAvailableInternalStorageSize();
                        Log.d(TAG, "Available Internal/External free Space: " + availableInternalStorageSize + "MB");
                        if (availableInternalStorageSize > minimumStorageForImage) {
                            binding.cameraRecordViewMenuCameraButton.setClickable(false);
                            binding.cameraRecordViewPauseMenuCameraButton.setClickable(false);
                            switch (currentCameraSsid) {
                                case NIGHTWAVE:
                                    cameraViewModel.captureImage(homeViewModel.getCurrentFwVersion().getRiscv()); //comment this to enable burst mode
                                    break;
                                case OPSIN:
                                    tcpConnectionViewModel.captureOpsinImageLocally(homeViewModel.getCurrentFwVersion().getRiscv());
                                    break;
                                case NIGHTWAVE_DIGITAL:
                                    Log.e(TAG,"captureImg : NW_Digital");
                                    // uncomment for video record and Image capture
                                    tcpConnectionViewModel.captureDigitalImageLocally();
                                    break;
                            }
                            //comment thid to enable burst mode.
                            new Handler().postDelayed(this::imageCapturedLayout, 500);
                            cameraViewModel.onSaveImage = false;
                            cameraViewModel.setIsCapturedImageInLive(true);
                        } else {
                            showStorageAlert(getString(R.string.image_storage));
                        }
                    }
                } else {
                    Toast.makeText(getActivity(), getString(R.string.enable_live), Toast.LENGTH_SHORT).show();
                }
            }
        }));
        //addition: Gallery View
        cameraViewModel.isSelectGallery.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            avoidDoubleClicks(binding.cameraRecordViewMenuGalleryButton);
            showGallery();
        }));

        isShowExpandMenu1.observe(lifecycleOwner, aBoolean -> {
            if (aBoolean && !cameraViewModel.isIsShowAlertDialog()) {
                hasShowExpandMenu();
            }
        });

        isShowAllMenu1.observe(lifecycleOwner, aBoolean -> {
            if (aBoolean && !cameraViewModel.isIsShowAlertDialog()) {
                showAutohideLayout();
            }
        });

        tcpConnectionViewModel.observeCameraMode().observe(this.getViewLifecycleOwner(), observeCameraMode);
        tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);

        cameraViewModel.observeHasShowUsbModeTextMessage().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.usbModeText.setVisibility(VISIBLE);
                CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_start_live_view_white));
                binding.image.setImageResource(R.drawable.ic_camera_background);
                binding.recordBtn.setVisibility(GONE);
                binding.cameraAspectRatioBtn.setVisibility(GONE);
                binding.cameraPictureInPictureBtn.setVisibility(GONE);
                ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
                Log.d("liveTAG", "observeHasShowUsbModeTextMessage: ");
                recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(VISIBLE);
                recordOptionLayoutBinding.cameraVideoBtn.setVisibility(GONE);
                recordOptionLayoutBinding.cameraImageBtn.setVisibility(GONE);
                recordOptionLayoutBinding.galleryBtn.setVisibility(VISIBLE);
                binding.liveViewVideo.setVisibility(GONE);
            } else {
                binding.usbModeText.setVisibility(GONE);
                binding.liveViewVideo.setVisibility(VISIBLE);
                recordOptionLayoutBinding.cameraImageBtn.setVisibility(VISIBLE);
                recordOptionLayoutBinding.cameraVideoBtn.setVisibility(VISIBLE);
                recordOptionLayoutBinding.galleryBtn.setVisibility(VISIBLE);
            }
            Log.d(TAG, "observeHasShowUsbModeTextMessage: " + aBoolean);
        }));

        cameraViewModel.observeHasDismissPleaseWaitProgressDialog().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean && !currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                binding.customProgressBar.setVisibility(VISIBLE);
                binding.liveViewVideo.setVisibility(VISIBLE);
            }
            Log.d(TAG, "observeHasDismissPleaseWaitProgressDialog: " + aBoolean);
        }));
        CameraViewModel.setHasAppBackgroundToForeground(false);
        Log.d(TAG, "SSCP_VIDEO_MODE_WIFI: " + CameraViewModel.isHasAppBackgroundToForeground());

        handleMultiWindow(requireActivity().isInMultiWindowMode());
        homeViewModel.getEnterExitMultiWindowMode().observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(isMultiWindowModeActivated -> {
            Log.e(TAG, "getEnterExitMultiWindowMode: " + isMultiWindowModeActivated);
            handleMultiWindow(isMultiWindowModeActivated);
        }));

        cameraViewModel.isSelectCameraRecordOptionsCancel.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                // here show option layout
                if (!isSelectPIP) {
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(VISIBLE);
                    recordOptionLayoutBinding.cameraRecordOptionLayout.setVisibility(GONE);
                    binding.cameraMenuView.setVisibility(VISIBLE);
                    visibleAutohideLayout();
                    binding.cameraRecordViewScreen.setVisibility(GONE);
                }
            }
        }));

        // save video/ image to mobile
        cameraViewModel.isSelectSaveToMobile.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (recordOptionLayoutBinding.mobileRecordBtn.isEnabled()) {
                    recordOptionLayoutBinding.cameraRecordOptionLayout.setVisibility(GONE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(VISIBLE);
                    binding.cameraMenuView.setVisibility(GONE);
                    binding.cameraHomeBtn.setVisibility(GONE);
                    binding.compassView.setVisibility(GONE);
                    if (CameraViewModel.hasSelectRecordVideoButton) {
                        // here record video and save to mobile
                        cameraViewModel.onCameraLiveViewMenuVideo();
                    } else {
                        // here capture image and save to mobile
                        cameraViewModel.onSelectCameraCapture();
                    }
                }
            }
        }));

        // save video/ image to camera
        cameraViewModel.isSelectSaveToCamera.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (recordOptionLayoutBinding.cameraRecordBtn.isEnabled()) {
                    recordOptionLayoutBinding.cameraRecordOptionLayout.setVisibility(GONE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(VISIBLE);
                    if (CameraViewModel.hasSelectRecordVideoButton) {
                        // here start record video and save to camera
                        binding.cameraMenuView.setVisibility(GONE);
                        binding.cameraHomeBtn.setVisibility(GONE);
                        binding.compassView.setVisibility(GONE);

                        if (CameraViewModel.opsinRecordButtonState == CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop) {
                            // here show warning dialog to stop streaming and start sd card recording
                            if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                                showOpsinCameraStreamingStopAndStartSDCardRecordDialog(getString(R.string.opsin_start_streaming_msg_while_recording));
                            } else {
                                // if live streaming stopped already here to start sd card record
                                binding.textureView.setVisibility(GONE);
                                binding.image.setVisibility(VISIBLE);
                                mHandler.removeCallbacks(runnable);
                                boolean hasCameraSdCardStorageFull = hasAvailableSdcardStorageSpace(sdCardInfo, true);
                                if (hasCameraSdCardStorageFull) {
                                    showCameraSdcardStorageFullAlertDialog(getString(R.string.camera_sd_card_full_video_recording_msg));
                                } else {
                                    tcpConnectionViewModel.startOpsinCameraRecord();
                                    Log.d(TAG, "isSelectSaveToCamera Else: sdcard space available");
                                }
                            }
                        }
                    } else {
                        // here start capture image and save to camera && check sd card minimum space 10mb to capture image
                        boolean hasSdCardStorageFull = hasAvailableSdcardStorageSpace(sdCardInfo, false);
                        if (hasSdCardStorageFull) {
                            showCameraSdcardStorageFullAlertDialog(getString(R.string.camera_sd_card_full_image_capture_msg));
                        } else {
                            Log.d(TAG, "isSelectSaveToCamera Capture Image: sdcard space available");
                            tcpConnectionViewModel.opsinCameraTakeImageToSDCard();
                        }
                    }
                }
            }
        }));

        cameraViewModel.isStopLiveStreamAndStartSdCardRecording.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    // here check sd card minimum space 50mb while start video recording
                    boolean hasCameraSdCardStorageFull = hasAvailableSdcardStorageSpace(sdCardInfo, true);
                    if (hasCameraSdCardStorageFull) {
                        showCameraSdcardStorageFullAlertDialog(getString(R.string.camera_sd_card_full_video_recording_msg));
                    } else {
                        Log.d(TAG, "isStopLiveStreamAndStartSdCardRecording: sdcard space available");
                        binding.image.setVisibility(VISIBLE);
                        binding.textureView.setVisibility(GONE);
                        binding.cameraMenuView.setVisibility(GONE);
                        mHandler.removeCallbacks(runnable);
                        tcpConnectionViewModel.stopOpsinLiveStreaming();
                        new Handler().postDelayed(() -> tcpConnectionViewModel.startOpsinCameraRecord(), 1000);
                    }
                } else {
                    binding.image.setVisibility(VISIBLE);
                    binding.textureView.setVisibility(GONE);
                    mHandler.removeCallbacks(runnable);
                    boolean hasCameraSdCardStorageFull = hasAvailableSdcardStorageSpace(sdCardInfo, true);
                    if (hasCameraSdCardStorageFull) {
                        showCameraSdcardStorageFullAlertDialog(getString(R.string.camera_sd_card_full_video_recording_msg));
                    } else {
                        Log.d(TAG, "isStopLiveStreamAndStartSdCardRecording Else: sdcard space available");
                        new Handler().postDelayed(() -> tcpConnectionViewModel.startOpsinCameraRecord(), 1000);
                    }
                }
            } else {
                cameraViewModel.onSelectRecordOptionsCancel();
            }
        }));

        tcpConnectionViewModel.observeOpsinCameraImageCaptureSDCard().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Toast.makeText(requireContext(), getString(R.string.photo_capture), Toast.LENGTH_SHORT).show();
            }
        }));
            //Nightwave firmware available dialog dismiss observer
        cameraInfoViewModel.isFwAvailableDialogDismiss.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(value -> {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                if (value == NEGATIVE_BUTTON_CLICKED) {
                    Log.e(TAG, "FwAvailableDialog dismissed" );
                    cameraViewModel.setShowExpandMenu(true);
                    cameraViewModel.setIsShowAlertDialog(true);
                    hasShowExpandMenu();
                    homeViewModel.getNavController().navigate(R.id.cameraInfoTabDialogFragment);
                }
            }
        }));

        /*//This is the start and stop button state for GIF
        binding.startButton.setOnClickListener(v->{
            bitmapList.clear();
            captureGif=true;
            Toast.makeText(getActivity(), "capturing gif", Toast.LENGTH_SHORT).show();
        });
        binding.stopButton.setOnClickListener(v->{
            captureGif=false;
            Log.d("captureTAG", "list: "+bitmapList.size());
            Toast.makeText(getActivity(), "Playing Gif: image size:"+bitmapList.size(), Toast.LENGTH_SHORT).show();
            displayGif();
        });*/

        /* //this is to button state for Burst mode.
        binding.cameraRecordViewMenuCameraButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                burstCapture();
                return false;
            }
        });*/
    }
    private void updateSwitchUI(boolean animate) {

        View switchRoot = binding.sliderTabLayout;
        if (switchRoot.getWidth() == 0) {
            switchRoot.post(() -> updateSwitchUI(animate));
            return;
        }
        ImageView switchThumb = binding.switchThumb;
        ImageView iconStd = binding.iconStd;
        ImageView iconHq = binding.iconHq;

        float targetX = isHighQuality ? switchRoot.getWidth() / 2f : 0f;

        switchThumb.setImageResource(isHighQuality ? R.drawable.ic_hq_selector : R.drawable.ic_std_selector);

        iconStd.setVisibility(isHighQuality ? View.VISIBLE : INVISIBLE);
        iconHq.setVisibility(isHighQuality ? INVISIBLE : View.VISIBLE);

        if (animate) {
            switchThumb.animate().translationX(targetX).setDuration(250).start();
        } else {
            switchThumb.setTranslationX(targetX);
        }
    }

    private void forceLowQualitySwitch() {
        if (isHighQuality) {
            isHighQuality = false;
            Constants.currentStreamQuality = STREAM_QUALITY_LOW;
            updateSwitchUI(true);
            Log.e(TAG, "Retry failed — forced LOW quality");
        }
    }

    private String detectDominantColor(Bitmap bitmap) {

        if (bitmap == null || bitmap.isRecycled()) {
            return "Invalid";
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int blackPixels = 0;
        int greenPixels = 0;
        int otherPixels = 0;

        int totalPixels = width * height;

        // Sample every Nth pixel to speed up (adjust as needed)
        int step = Math.max(1, totalPixels / 1000);

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int pixel = bitmap.getPixel(x, y);
                int red   = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue  = pixel & 0xff;

                if (red < 20 && green < 20 && blue < 20) {
                    blackPixels++;
                } else if (green > red + 30 && green > blue + 30) {
                    greenPixels++;
                } else {
                    otherPixels++;
                }
            }
        }

        if (blackPixels > greenPixels && blackPixels > otherPixels) {
            return "BLACK";
        } else if (greenPixels > blackPixels && greenPixels > otherPixels) {
            return "GREEN";
        } else {
            Log.i(TAG,"Valid frame quality");
            return "VALID";
        }
    }

    private Bitmap getVideoThumbnail(String videoFilePath){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFilePath);
            return retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST);
        } catch (Exception e) {
            Log.e("Thumbnail", "Error retrieving frame: " + e.getMessage());
            return null;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void resizeVideo(int width,int height) {
        Log.e(TAG, "resizeVideo: "+width+" "+height );
        runOnUiThread(() -> {
            binding.videoContainer.setVideoSize(width, height);
            binding.videoContainer.setResizeMode(isAspectRatio_16_9 ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
        });
    }

    private final EventObserver<Boolean> eventObserver = new EventObserver<>(aBoolean -> {
        if (aBoolean) {
       //     Log.d(TAG, "isUpdateAspectRatioButtonState: aspectRatioState: " + aspectRatioState + " videoCropState:" + CameraViewModel.videoCropState.name() + " isSelectPIP:" + isSelectPIP + " isLiveViewStopped:" + cameraViewModel.isLiveViewStopped());
            if (!cameraViewModel.isLiveViewScreenInBackground() && CameraViewModel.recordButtonState != CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED && !isSelectPIP) {
                if (!cameraViewModel.isLiveViewStopped()) {
                //    Log.d(TAG, "isUpdateAspectRatioButtonState: " + CameraViewModel.recordButtonState.name());
                    if (videoCropState == CameraViewModel.VideoCropState.CROP_ON && aspectRatioState) {
                        videoCropState = CameraViewModel.VideoCropState.CROP_OFF;
                        binding.cameraAspectRatio.setImageResource(R.drawable.ic_aspect_ratio16_9);
                        binding.setAspectRatio(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_aspect_ratio16_9));
                        aspectRatioState = false;
                    }
                } else {
                    cameraViewModel.setLiveViewStopped(false);
                    // removeAspectRatioObserver();
                }
            } else {
                cameraViewModel.setLiveViewScreenInBackground(false);
                observeAspectRatioState();
            }
        }
    });

    private void observeAspectRatioState(){
        cameraViewModel.isUpdateAspectRatioButtonState.observe(lifecycleOwner, eventObserver);
    }

    private void removeAspectRatioObserver() {
        cameraViewModel.isUpdateAspectRatioButtonState.removeObserver(eventObserver);
        cameraViewModel.isUpdateAspectRatioButtonState.removeObservers(lifecycleOwner);
    }

    private boolean hasAvailableSdcardStorageSpace(SDCardInfo sdCardInfo, boolean isVideoRecording) {
        boolean isAvailableSpace = false;
        long availableSizeBytes = sdCardInfo.getAvailableSize();
        long bytes = availableSizeBytes * 1024; // convert kb to bytes
        String storageValue = Formatter.formatFileSize(requireContext(), bytes);
        String[] splitValues = splitStorageValue(storageValue);
        if (splitValues[0] != null && splitValues[1] != null) {
            long getAvailableStorageSize = convertToBytes(Double.parseDouble(splitValues[0]), splitValues[1]);
            if (isVideoRecording) {
                if (getAvailableStorageSize < minimumSdcardStorageForVideo) {
                    Log.d(TAG, "sdcard Storage full:");
                    isAvailableSpace = true;
                }
            } else {
                if (getAvailableStorageSize < minimumSdcardStorageForImage) {
                    Log.d(TAG, "sdcard Storage full:");
                    isAvailableSpace = true;
                }
            }
            Log.d(TAG, "AvailableStorageSize: " + availableSizeBytes + "  " + getAvailableStorageSize + " \n" + "Numeric value:" + splitValues[0] + " \n" + "Unit: " + splitValues[1]);
        }
        return isAvailableSpace;
    }

    @Nullable
    private String getCameraIpAddress(String connectedSsid) {
        String cameraIpAddress = "";
        WifiManager mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = getWifiInfo(mWifiManager);
        if (wifiInfo != null) {
            try {
                int ip = wifiInfo.getIpAddress();
                byte[] buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array();
                cameraIpAddress = InetAddress.getByAddress(buffer).getHostAddress();
                setIsNightWaveCamera(connectedSsid, cameraIpAddress); // for this set port number
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return cameraIpAddress;
    }

    public WifiInfo getWifiInfo(WifiManager mWifiManager) {
        WifiInfo wifiInfo = null;
        if (isSDKBelow13()) {
            Log.d(TAG, "getWifiInfo: Android Below 13  ");
            wifiInfo = mWifiManager.getConnectionInfo();
        } else {
            Log.d(TAG, "getWifiInfo:Android 13");
            if (bleWiFiViewModel.getNetworkCapability() != null) {
                final TransportInfo transportInfo = ((NetworkCapabilities) bleWiFiViewModel.getNetworkCapability()).getTransportInfo();
                if (transportInfo instanceof WifiInfo) {
                    wifiInfo = (WifiInfo) transportInfo;
                }
            }
        }
        return wifiInfo;
    }

    private void handleMultiWindow(Boolean isMultiWindowModeActivated) {
        if (isMultiWindowModeActivated && !isSelectPIP) {
            cameraViewModel.onRecordViewCancel();
            cameraViewModel.onCameraRecordScreenMenuCancel();
            cameraViewModel.onCameraLiveViewMenuCancel();
            cameraViewModel.hasEnableMultiWindowMode();
            if (timerState && isRecordingStarted) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        updateRecordScreen();
                        cameraViewModel.stopRecordingVideo();
                        break;
                    case OPSIN:
                        updateRecordScreen();
                        tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"handleMultiWindow : NW_Digital");
                        // uncomment for video record and Image capture
                        updateRecordScreen();
                        tcpConnectionViewModel.stopDigitalVideoRecordLocally();
                        break;
                }
            } else {
                updateRecordScreen();
                seconds = 0;
            }
        } else {
            if (!isSelectPIP) {
                new Handler().postDelayed(() -> {
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                        triggerTimerToUpdateStatusIcons();
                    }
                }, 500);
                showAutohideLayout();
            }
        }
    }


   /* //this is the method used for burst mode.
   private void burstCapture() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i=0;
                long startTime=System.currentTimeMillis();
                Log.d("burstTAG", "run: ");
                while (binding.cameraRecordViewMenuCameraButton.isPressed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    i++;
                    Log.d("burstTAG", "i: "+i+" time sec:"+((System.currentTimeMillis()-startTime)/1000));
                    cameraViewModel.captureImage(homeViewModel.getCurrentFwVersion().getRiscv());
                }
            }
        }).start();
    }*/


    private void imageCapturedLayout() {
        if (isRecordingStarted) {
            binding.cameraRecordTimerLayout.setVisibility(VISIBLE);
            binding.cameraRecordButton.setClickable(false);
        } else {
            binding.cameraRecordTimerLayout.setVisibility(GONE);
        }
        binding.cameraRecordViewMenu.setVisibility(GONE);
        binding.cameraRecordScreen.setVisibility(VISIBLE);

        binding.cameraRecordViewPauseMenu.setVisibility(GONE);
        binding.cameraRecordSaveLayoutDuration.setVisibility(GONE);
        binding.cameraRecordSaveLayoutCancel.setVisibility(GONE);
        binding.cameraRecordSaveLayout.setVisibility(VISIBLE);
        binding.cameraRecordSaveLayoutSave.setVisibility(VISIBLE);
        binding.cameraRecordSaveLayoutDelete.setVisibility(VISIBLE);
        binding.cameraRecordSaveLayoutConfirmDelete.setVisibility(GONE);

        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            File file = new File(imageFilePath);
            /* for this split camera ssid and only allow timestamp */
            if (file.getName() != null) {
                String[] originalFileName = file.getName().split("_");
                int index = originalFileName[2].lastIndexOf('.');
                if (index != -1) {
                    String substring = originalFileName[2].substring(0, index);
                    long millisecond = Long.parseLong(substring);
                    String dateString = dateFormat.format(new Date(millisecond));
                    binding.cameraRecordViewChoiceTitle.setText(dateString.toUpperCase());
                    binding.cameraRecordViewChoiceTitle.setEllipsize(TextUtils.TruncateAt.END);
                }
            }
            if (file.exists()) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        Glide.with(requireActivity())
                                .load(imageFilePath)
                                .placeholder(R.drawable.ic_camera_background)
                                .into((ImageView) binding.cameraRecordSaveLayoutThumbnail);
                        break;
                    case OPSIN:
                        Glide.with(requireActivity())
                                .load(imageFilePath)
                                .placeholder(R.drawable.opsin_live_view_background)
                                .into((ImageView) binding.cameraRecordSaveLayoutThumbnail);
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"imageCapturedLayout : NW_digital");
                        Glide.with(requireActivity())
                                .load(imageFilePath)
                                .placeholder(R.drawable.ic_nw_digital_background)
                                .into((ImageView) binding.cameraRecordSaveLayoutThumbnail);
                        break;
                }

            }
        }
        binding.cameraRecordViewMenuCameraButton.setClickable(true);
        binding.cameraRecordViewPauseMenuCameraButton.setClickable(true);
    }

    private void triggerTimerToUpdateStatusIcons() {
        tcpConnectionViewModel.triggerKeepAlive(true);
        applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
        tcpConnectionViewModel.clearPeriodicRequestList();

        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);
        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_COMPASS);
        }
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.BATTERY_INFO);
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_MIC_STATE);
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_FRAME_RATE);
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_NUC);
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_SD_CARD_INFO);
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_EV);
        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_MONOCHROMATIC);
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_NOISE_REDUCTION_RATE);
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_ROI);
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.GET_GPS_POSITION);
        }
    }

    private void stopPeriodicTimer() {
        tcpConnectionViewModel.cancelOpsinPeriodicTimer();
        applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
        commandRequested = TCPRepository.COMMAND_REQUESTED.NONE;
        tcpConnectionViewModel.clearPeriodicRequestList();
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);
        tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.BATTERY_INFO);
    }

    private void updateZoomIcon() {
        if (getCameraZoomLevel() == 1) {
            binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out_fade));
            binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in));
            binding.zoomLevelText1.setText(getString(R.string._1_x));
        } else if (getCameraZoomLevel() == 2) {
            binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out));
            binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in));
            binding.zoomLevelText1.setText(getString(R.string._2_x));
        } else {
            binding.setZoomInIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_in_fade));
            binding.setZoomOutIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_zoom_out));
            binding.zoomLevelText1.setText(getString(R.string._3_x));
        }
    }

    public void resetOpsinRecordUI() {
        Log.d(TAG, "resetOpsinRecordUI: true");
        binding.timerLayout.setVisibility(GONE);
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
            if (CameraViewModel.cameraXValue < OPSIN_STREAMING_SUPPORTS_FROM) {
                binding.startRecord.setVisibility(VISIBLE);
                if (homeViewModel.menuState) {
                    binding.startRecord.setVisibility(GONE);
                }
            } else {
                binding.startRecord.setVisibility(GONE);
            }
        }
        startTime = 0;
        updatedTime = 0;
        timeInMilliseconds = 0;
        timeSwapBuff = 0;
        timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);
    }

    private void showOpsinCameraNotResponding(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_NOT_RESPONSE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void showStorageAlert(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.STORAGE_ALERT;
            activity.showDialog("", message, null);
        }
    }

    private void showOpsinCameraRecordInProgressDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_CAMERA_RECORD_IN_PROGRESS;
            activity.showDialog("", message, null);
        }
    }

    private void showOpsinCameraStreamingToStopRecordDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.OPSIN_CAMERA_STREAMING_TO_STOP_RECORD;
            activity.showDialog("", message, null);
        }
    }

    private void showOpsinCameraStreamingStopAndStartSDCardRecordDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CAMERA_SD_CARD_RECORD_START_ALERT_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void showCameraSdcardStorageFullAlertDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CAMERA_SD_CARD_STORAGE_FULL_ALERT_DIALOG;
            activity.showDialog(getString(R.string.sd_card_storage_full), message, null);
        }
    }

    private void showCameraClosureDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CAMERA_CLOSURE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void showRetryDialog(String message) {
        runOnUiThread(() -> {
            if (isRecordingStarted) {
//                Toast.makeText(requireActivity(), getString(R.string.recording_stopped_no_live_data), Toast.LENGTH_SHORT).show();
                updateRecordScreen();
                tcpConnectionViewModel.stopDigitalVideoRecordLocally();
            }

            if (binding.customProgressBar.getVisibility() == VISIBLE) {
                binding.customProgressBar.setVisibility(GONE);
            }

            binding.retryTxtDigitalcamera.setVisibility(VISIBLE);
            binding.retryTxtDigitalcamera.setText(message);
        });
    }

    private void showPowerCycleCamera(String message) {
        runOnUiThread(() -> {
            if (binding.retryTxtDigitalcamera.getVisibility() == VISIBLE) {
                binding.retryTxtDigitalcamera.setVisibility(GONE);
            }
            MainActivity activity = ((MainActivity) getActivity());
            if (activity != null) {
                activity.showDialog = MainActivity.ShowDialog.CAMERA_CLOSURE_DIALOG; //SOCKET_FAILED
                activity.showDialog("", message, null);
            }
        });
    }

    private final EventObserver<Object> observeRetryValue = new EventObserver<>(obj -> {
        runOnUiThread(() -> {
            if (obj instanceof Long) {
                long retryData = (long) obj;
                int retry = (int) (retryData & 0xFFFFFFFFL);
                int max_retry = (int) ((retryData >> 32) & 0xFFFFFFFFL);

                Log.i(TAG, "notifyRetryIsInitiated: retryCount " + retry + " maxRetry " + max_retry);
                setIconsAreEnabled(false);
                binding.liveViewVideo.setEnabled(true);
                recordOptionLayoutBinding.galleryBtn.setEnabled(true);
                binding.cameraMenuBtn.setEnabled(true);
                binding.cameraHomeBtn.setEnabled(true);
                setSwitchEnabled(false);
                binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_live_view_white));
                binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));

                showRetryDialog(String.format(getString(R.string.retrying_stream_d_d), retry, max_retry));
            } else {
                if (obj instanceof Boolean) {
                    // dismiss dialog
                    binding.retryTxtDigitalcamera.setVisibility(GONE);
                    boolean isDismissed = (boolean) obj;
                    Log.i(TAG, "showRetryDialog: view dismissed " + recordButtonState + currentStreamQuality + isDismissed );
                    if (isDismissed) {
                        // after 3 retry the streaming has changed from hq to std
                        if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED && currentStreamQuality == STREAM_QUALITY_HIGH)
                            forceLowQualitySwitch();
                    } else {
                        if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED) {
                            binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                            binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_start_live_view_white));
                        }

                        if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED)
                            setIconsAreEnabled(true);
                    }

                    if (binding.customProgressBar.getVisibility() == VISIBLE) {
                        binding.customProgressBar.setVisibility(VISIBLE);
                    }
                }
            }
        });

    });

    private void setSwitchEnabled(boolean enabled) {

        View switchRoot = binding.sliderTabLayout;
        ImageView switchThumb = binding.switchThumb;

        switchRoot.setEnabled(enabled);
        switchRoot.setClickable(enabled);
        switchRoot.setFocusable(enabled);

        float alpha = enabled ? 1f : 0.4f;
        switchRoot.setAlpha(alpha);
        switchThumb.setAlpha(alpha);
    }

    private void setIconsAreEnabled(boolean isEnable) {
        // Icons are visible but no click access when retry popup shown when gallery support is enabled
        recordOptionLayoutBinding.cameraVideoBtn.setEnabled(isEnable);
        recordOptionLayoutBinding.cameraImageBtn.setEnabled(isEnable);
        recordOptionLayoutBinding.galleryBtn.setEnabled(isEnable);
        binding.cameraPictureInPictureBtn.setEnabled(isEnable);
        binding.cameraAspectRatioBtn.setEnabled(isEnable);
        binding.sliderTabLayout.setEnabled(isEnable);
        binding.cameraMenuBtn.setEnabled(isEnable);
        binding.cameraHomeBtn.setEnabled(isEnable);
        setSwitchEnabled(isEnable);
    }

    private final EventObserver<Boolean> observeStreamingReadyToVisible = new EventObserver<>(aBoolean -> {
        if (aBoolean) {
            CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED;
            Log.e(TAG, "observeStreamingReadyToVisible:  " + rtspState);
            binding.image.setVisibility(GONE);
            if (binding.customProgressBar.getVisibility() == VISIBLE)
                binding.customProgressBar.setVisibility(GONE);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            binding.videoContainer.setVisibility(VISIBLE);
            binding.videoContainer.getTextureView().setOpaque(true);// avoid black screen
            binding.videoContainer.getTextureView().setAlpha(1f);
            binding.sliderTabLayout.setVisibility(VISIBLE);
            binding.retryTxtDigitalcamera.setVisibility(GONE);
            binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_live_view_white));
            binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
            setRecordOptionLayoutDisabled(true); // only streaming
            setIconsAreEnabled(true);
        } else {
            setIconsAreEnabled(false);
        }
    });

    private final EventObserver<Integer> observeStreamingErrorCode = new EventObserver<>(errCode -> {
        Log.e(TAG, "observeOnStreamingErrorCode:  " +  errCode + ", streaming state :  " +  recordButtonState);
        if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
            // uncomment for video record
                    if (isRecordingStarted) {
                        updateRecordScreen();
                        tcpConnectionViewModel.stopDigitalVideoRecordLocally();
                    }
        }

        if (binding.customProgressBar.getVisibility() == VISIBLE){
            binding.customProgressBar.setVisibility(GONE);
        }

        switch (errCode){
            case ERR_PIPELINE_SETUP_FAIL_00:
            case ERR_PIPELINE_SETUP_FAIL_02:
            case ERR_PIPELINE_SETUP_FAIL_03:
            case ERR_PIPELINE_SETUP_FAIL_04:
            case ERR_PIPELINE_SETUP_FAIL_05:
                showPowerCycleCamera(getString(R.string.pipeline_setup_fail));
                tcpConnectionViewModel.setStopStreamDigitalCamera();
                break;
            case ERR_INVALID_RTSP_URI_06:
            case ERR_INVALID_RTSP_URI_07:
                showPowerCycleCamera(getString(R.string.invalid_rtsp_uri));
                tcpConnectionViewModel.setStopStreamDigitalCamera();
                break;
            case ERR_PLAYBACK_STALLED:
                showPowerCycleCamera(getString(R.string.playback_stalled));
                break;
            case ERR_DECODER_STREAM_01:
            case ERR_DECODER_STREAM_14:
                showPowerCycleCamera(getString(R.string.decoder_stream_error));
                break;
            case ERR_NETWORK_ISSUES_10: //RTSP timeout (max retries reached)
            case ERR_NETWORK_ISSUES_16:
            case ERR_NETWORK_ISSUES_13: // Max reconnection attempts reached - errCode 1013
                showPowerCycleCamera(getString(R.string.network_issues));
                break;
            case ERR_SERVER_ERRORS:
                showPowerCycleCamera(getString(R.string.server_errors));
                tcpConnectionViewModel.setStopStreamDigitalCamera();
                break;
            case ERR_STREAM_TERMINATED:
                showPowerCycleCamera(getString(R.string.stream_terminated));
                break;
            case ERR_NO_VIDEO_DATA:
                showPowerCycleCamera(getString(R.string.no_video_data));
                break;
            case ERR_INTERNAL_STATE_CORRUPTION:
                showPowerCycleCamera(getString(R.string.internal_state_corruption));
                tcpConnectionViewModel.setStopStreamDigitalCamera();
                break;
            case ERR_AUTHENTICATION:
                showPowerCycleCamera(getString(R.string.authentication_error));
                break;
            case ERR_UNKNOWN_ERROR:
                showPowerCycleCamera(getString(R.string.unknown_error));
                break;
            case ERR_STREAM_PROGRESS_ERROR:
                Log.e(TAG, "notifyDigitalExceptions: default error " +  errCode);
//                        showPowerCycleCamera(getString(R.string.streaming_not_available_messgae));
                break;
            default:
                Log.e(TAG, "notifyDigitalExceptions: default error " +  errCode);
                showPowerCycleCamera(getString(R.string.streaming_not_available_messgae));
                break;
        }

        isFpsReceived = false;
    });

    private final EventObserver<Object> observeCameraMode = new EventObserver<>(object -> {
        if (object instanceof CommandError) {
            Log.e(TAG, "observeCameraMode: " + ((CommandError) object).getError());
        } else {
            if (homeViewModel.currentScreen == CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == CurrentScreen.CAMERA_INFO_DIALOG_SCREEN || CameraViewModel.isHasVisibleSettingsInfoView()) {
                int response = (int) object;
                Log.d(TAG, "observeCameraMode: " + object);
                if (response == SCCPConstants.SCCP_CAMERA_MODE.SCCP_CAMERA_MODE_USB.getValue()) {
                    tcpConnectionViewModel.getVideoMode();
                    cameraViewModel.setAnalogMode(false);
                    //on Live Stop state -Button state has been changed
                    binding.recordBtn.setVisibility(GONE);
                    recordOptionLayoutBinding.cameraRecordViewLayout.setVisibility(GONE);
                    binding.cameraAspectRatioBtn.setVisibility(GONE);
                    binding.cameraPictureInPictureBtn.setVisibility(GONE);
                    ((ViewGroup.MarginLayoutParams) binding.cameraMenuBtn.getLayoutParams()).topMargin = 0;
                    Log.d(TAG, "observeCameraMode : USB ");
                } else if (response == SCCPConstants.SCCP_CAMERA_MODE.SSCP_CAMERA_MODE_ANALOG.getValue()) {
                    hideVideoModeDialog();
                    cameraViewModel.setAnalogMode(true);
                    binding.usbModeText.setVisibility(GONE);
                    // In Live view screen sent live view command
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                        tcpConnectionViewModel.stopLiveView();
                    } else {
                        tcpConnectionViewModel.startLiveView(true);
                        CameraViewModel.setHasPressedLiveViewButton(false); //for this to avoid stop live view on entry point screen
                        binding.customProgressBar.setVisibility(VISIBLE);
                        binding.liveViewVideo.setVisibility(VISIBLE);
                    }
                    Log.d(TAG, "observeCameraMode : Analog");
                }
            } else {
                Log.d(TAG, "observeCameraMode: not in live view");
            }
        }
    });

    private final EventObserver<Object> observeCameraVideoMode = new EventObserver<>(object -> {
        if (object instanceof CommandError) {
            cameraViewModel.setHasShowProgress(false);
            cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
            Log.e(TAG, "observeCameraVideoMode: " + ((CommandError) object).getError());
        } else {
            int response = (int) object;
            cameraViewModel.setHasShowProgress(false);
            cameraSettingsViewModel.hasShowVideoModeResponseProgressbar(false);
            if (homeViewModel.currentScreen == CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == CurrentScreen.CAMERA_INFO_DIALOG_SCREEN || CameraViewModel.isHasVisibleSettingsInfoView()) {
                Log.d(TAG, "observeCameraVideoMode: " + object);
                if (response == SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                    //binding.usbModeText.setVisibility(View.VISIBLE);
                    CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                    CameraViewModel.setIsLiveScreenVideoModeUVC(true);
                    cameraViewModel.setHasShowUsbModeTextMessage(true);
                    // Show dialog with wave image to enable video mode to wifi
                    if (!CameraViewModel.isHasVisibleSettingsInfoView()) {
                        if (binding.customProgressBar.getVisibility() == VISIBLE) {
                            binding.customProgressBar.setVisibility(GONE);
                        }
                        if (!CameraViewModel.isHasPressedSettingCancelIcon()) {
                            // here not required to show dialog
                            //showVideoModeDialog(getString(R.string.enable_live_streaming_message));
                        } else {
                            CameraViewModel.setHasPressedSettingCancelIcon(false);
                        }
                        if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                            tcpConnectionViewModel.stopLiveView();
                        }
                    }
                    binding.stopLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_rectangle_svg));
                    binding.startLiveView.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_start_live_view_white));
                    binding.image.setImageResource(R.drawable.ic_camera_background);
                    setRecordOptionLayoutDisabled(false);

                    Log.d("liveTAG", "observeCameraVideoMode: ");

                    if (binding.liveViewVideo.getVisibility() == VISIBLE) {
                        binding.liveViewVideo.setVisibility(GONE);
                    }
                    Log.d(TAG, "observeCameraVideoMode : UVC ");
                } else if (response == SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                    CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                    CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                    // binding.usbModeText.setVisibility(View.GONE);
                    cameraViewModel.setHasShowUsbModeTextMessage(false);
                    if (binding.liveViewVideo.getVisibility() == GONE) {
                        binding.liveViewVideo.setVisibility(VISIBLE);
                    }
                    // In Live view screen sent live view command
                    /*Start LiveView*/
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                        if (!CameraViewModel.isHasPressedSettingCancelIcon() && CameraViewModel.isHasPressedLiveViewButton()) {
                            tcpConnectionViewModel.stopLiveView();
                            CameraViewModel.setHasPressedLiveViewButton(false); //for this to avoid stop live view on entry point screen
                        } else {
                            CameraViewModel.setHasPressedSettingCancelIcon(false);
                        }
                    } else {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!CameraViewModel.isHasPressedSettingCancelIcon()) {
                                    tcpConnectionViewModel.startLiveView(true);
                                    CameraViewModel.setHasPressedLiveViewButton(false); //for this to avoid stop live view on entry point screen
                                    binding.customProgressBar.setVisibility(VISIBLE);
                                    binding.liveViewVideo.setVisibility(VISIBLE);
                                } else {
                                    CameraViewModel.setHasPressedSettingCancelIcon(false);
                                }
                            }
                        });
                    }
                    Log.d(TAG, "observeCameraVideoMode : WIFI");
                }
            } else {
                Log.d(TAG, "observeCameraVideoMode: not in live view");
                cameraSettingsViewModel.updateVideoModeState(object);
            }
        }
    });

    private void setRecordOptionLayoutDisabled(boolean isEnabled) {
        if (isEnabled){
            recordOptionLayoutBinding.cameraVideoBtn.setEnabled(true);
            recordOptionLayoutBinding.cameraImageBtn.setEnabled(true);
            recordOptionLayoutBinding.galleryBtn.setEnabled(true);
            binding.cameraPictureInPictureBtn.setVisibility(VISIBLE);
            binding.cameraPictureInPictureBtn.setEnabled(true);
            recordOptionLayoutBinding.cameraVideoBtn.setAlpha(1.0f);
            recordOptionLayoutBinding.cameraImageBtn.setAlpha(1.0f);
            recordOptionLayoutBinding.galleryBtn.setAlpha(1.0f);
            binding.cameraPictureInPictureBtn.setAlpha(1.0f);
            binding.sliderTabLayout.setVisibility(GONE);
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                binding.cameraAspectRatioBtn.setVisibility(VISIBLE);
                binding.sliderTabLayout.setVisibility(VISIBLE); // only for NWD
            }
        } else {
            recordOptionLayoutBinding.cameraVideoBtn.setEnabled(false);
            recordOptionLayoutBinding.cameraImageBtn.setEnabled(false);
            recordOptionLayoutBinding.cameraVideoBtn.setAlpha(0.5f);
            recordOptionLayoutBinding.cameraImageBtn.setAlpha(0.5f);
            binding.cameraPictureInPictureBtn.setVisibility(VISIBLE);
            binding.cameraPictureInPictureBtn.setEnabled(false);
            binding.cameraPictureInPictureBtn.setAlpha(0.5f);

            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                if (recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED){
                    binding.cameraAspectRatioBtn.setVisibility(GONE);
                    binding.cameraPictureInPictureBtn.setVisibility(GONE);
//                    if (!isLiveEnabled) // Hq Switch handling disable and gone
//                        binding.sliderTabLayout.setVisibility(GONE);
                }
            }
        }
    }

    private void showAllPacketZeroDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.PACKET_ALL_ZERO_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void showVideoModeDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.USB_VIDEO_MODE_DIALOG;
            activity.showDialog("", message, null);
        }
    }


    private void hideVideoModeDialog() {
        for (int i = 0; i < homeViewModel.getNoticeDialogFragments().size(); i++) {
            NoticeDialogFragment dialogFragment = homeViewModel.getNoticeDialogFragments().get(i);
            if (dialogFragment.isVisible()) {
                String tag = dialogFragment.getTag();
                Log.e(TAG, "showDialog: " + tag);
                new Handler().post(dialogFragment::dismiss);
            }
        }
    }

    private void setCropState() {
        if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
            isAspectRatio_16_9 = !isAspectRatio_16_9;
            resizeVideo(VIDEO_WIDTH,VIDEO_HEIGHT);
            binding.setAspectRatio(isAspectRatio_16_9 ? AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_aspect_ratio4_3) : AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_aspect_ratio16_9));
            binding.videoContainer.reset();
            binding.image.setVisibility(GONE);

        } else {
            if (videoCropState == CameraViewModel.VideoCropState.CROP_ON) {
                binding.cameraAspectRatio.setImageResource(R.drawable.ic_aspect_ratio16_9);
                binding.setAspectRatio(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_aspect_ratio16_9));
                ((ImageView) binding.image).setScaleType(ImageView.ScaleType.FIT_CENTER);
                videoCropState = CameraViewModel.VideoCropState.CROP_OFF;
            } else if (videoCropState == CameraViewModel.VideoCropState.CROP_OFF) {
                binding.setAspectRatio(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_aspect_ratio4_3));
                ((ImageView) binding.image).setScaleType(ImageView.ScaleType.CENTER_CROP);
                videoCropState = CameraViewModel.VideoCropState.CROP_ON;
            }
        }
    }

    public void hasShowExpandMenu() {
        Log.e(TAG, "hasShowExpandMenu: " + cameraViewModel.isShowExpandMenu() + "/ " + cameraViewModel.isIsShowAlertDialog());
        if (cameraViewModel.isShowExpandMenu() && !cameraViewModel.isIsShowAlertDialog()) {
            binding.cameraMenuView.setVisibility(GONE);
            binding.cameraHomeBtn.setVisibility(GONE);
            binding.compassView.setVisibility(GONE);
            binding.cameraMenuLayout.setVisibility(VISIBLE);
        } else {
            binding.cameraMenuView.setVisibility(GONE);
            binding.cameraHomeBtn.setVisibility(GONE);
            binding.cameraMenuLayout.setVisibility(GONE);
            // binding.compassView.setVisibility(View.GONE);
        }
    }

    private void showAutohideLayout() {
        if (binding.cameraMenuView.getVisibility() == GONE) {
            binding.cameraMenuView.setVisibility(VISIBLE);
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                hideLocationUI();
                binding.compassView.setVisibility(VISIBLE);
                binding.statusIcons.setVisibility(VISIBLE);
                if (CameraViewModel.cameraXValue < OPSIN_STREAMING_SUPPORTS_FROM) {
                    if (CameraViewModel.opsinRecordButtonState == CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop) {
                        binding.startRecord.setVisibility(VISIBLE);
                    }
                    homeViewModel.menuState = false;
                }
            }
            binding.cameraHomeBtn.setVisibility(VISIBLE);
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    binding.compassView.setVisibility(GONE);
                    binding.cameraPresetBtn.setVisibility(VISIBLE);
                    break;
                case OPSIN:
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        if (!isSelectPIP) {
                            binding.compassView.setVisibility(VISIBLE);
                            binding.cameraPresetBtn.setVisibility(VISIBLE);
                        }
                    } else {
                        binding.compassView.setVisibility(GONE);
                        binding.cameraPresetBtn.setVisibility(GONE);
                    }
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"showAutohideLayout : NW_digital");
                    binding.image.setImageResource(R.drawable.ic_nw_digital_background);
                    binding.compassView.setVisibility(GONE);
                    binding.cameraPresetBtn.setVisibility(GONE);
                    if (binding.retryTxtDigitalcamera.getVisibility() == GONE && binding.customProgressBar.getVisibility() == GONE)
                        setIconsAreEnabled(true);
                    break;
            }

            binding.cameraMenuLayout.setVisibility(GONE);
            cameraViewModel.setShowExpandMenu(false);
            mHandler.removeCallbacks(runnable);
            mHandler.postDelayed(runnable, 5000);

            //to make Camera Record View Screen Visible
            if (binding.cameraRecordViewScreen.getVisibility() == VISIBLE) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(GONE);
                binding.compassView.setVisibility(GONE);
                binding.cameraLeftMenuView.setVisibility(GONE);
                binding.cameraRightMenuView.setVisibility(GONE);
                cameraViewModel.setShowExpandMenu(false);
            }

            //to make Camera Record Screen Visible
            if (binding.cameraRecordScreen.getVisibility() == VISIBLE) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(GONE);
                binding.compassView.setVisibility(GONE);
                binding.cameraLeftMenuView.setVisibility(GONE);
                binding.cameraRightMenuView.setVisibility(GONE);
                cameraViewModel.setShowExpandMenu(false);
            }
            // for this if camera sd card record starting
            if (binding.timerLayout.getVisibility() == VISIBLE) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(GONE);
                binding.compassView.setVisibility(GONE);
                binding.cameraLeftMenuView.setVisibility(GONE);
                binding.cameraRightMenuView.setVisibility(GONE);
                cameraViewModel.setShowExpandMenu(false);
            }
        }

        hidemenusInSplitscreen();
    }

    //addition: display record screen menu for 5sec
    public void showRecordScreenMenu() {
        binding.compassView.setVisibility(GONE);
        cameraViewModel.onCameraRecordMenuPause();
        mHandler.removeCallbacks(runnable);
        mHandler.postDelayed(runnable, 5000);
    }

    //addition: to make visible the auto hide layout
    public void visibleAutohideLayout() {
        if (binding.cameraMenuView.getVisibility() == VISIBLE) {
            binding.cameraMenuView.setVisibility(VISIBLE);
            binding.cameraHomeBtn.setVisibility(VISIBLE);
            switch (currentCameraSsid) {
                case OPSIN:
                    hideLocationUI();
                    if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        if (!isSelectPIP)
                            binding.compassView.setVisibility(VISIBLE);
                    } else {
                        binding.compassView.setVisibility(GONE);
                    }
                    break;
                case NIGHTWAVE:
                    binding.compassView.setVisibility(GONE);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"visibleAutohideLayout : NW_Digital");
                    binding.compassView.setVisibility(GONE);

                    break;
            }

            binding.cameraLeftMenuView.setVisibility(GONE);
            binding.cameraRightMenuView.setVisibility(GONE);
            cameraViewModel.setShowExpandMenu(false);
        } else {
            showLocationUI();
        }
        mHandler.removeCallbacks(runnable);
        mHandler.postDelayed(runnable, 5000);

        hidemenusInSplitscreen();
    }

    private void hidemenusInSplitscreen() {
        if (activity.isInMultiWindowMode()) {
            binding.cameraMenuView.setVisibility(GONE);
            binding.compassView.setVisibility(GONE);
            binding.statusIcons.setVisibility(GONE);
            if (!isSelectPIP)
                binding.cameraHomeBtn.setVisibility(VISIBLE);
            else {
                hideLocationUI();
                binding.cameraHomeBtn.setVisibility(GONE);
            }
        }
    }

    //addition: method for Record view timer to start the countdown
    public void startVideoRecordTimer() {
        Log.d("recordTAG", "startVideoRecordTimer: ");
        isRecordingStarted = true;
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                if (binding.recordScreenTimer != null && binding.cameraRecordSaveLayoutDuration != null) {
                    binding.recordScreenTimer.setText(time);
                    binding.cameraRecordSaveLayoutDuration.setText(time);
                }
                if (timerState) {
                    seconds++;
                    handler.postDelayed(this, 1000);
                } else {
                    handler.removeCallbacks(this);
                    if (binding.cameraRecordSaveLayoutDuration != null) {
                        binding.cameraRecordSaveLayoutDuration.setText(time);
                    }
                }
            }
        });
    }

    //addition: method to start timer
    public void setRecordView() {
        binding.cameraMenuView.setVisibility(GONE);
        binding.cameraHomeBtn.setVisibility(GONE);
        binding.compassView.setVisibility(GONE);
        binding.cameraRecordViewScreen.setVisibility(VISIBLE);
        binding.cameraRecordViewMenu.setVisibility(VISIBLE);
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                showLocationUI();
            }
            binding.cameraMenuView.setVisibility(GONE);
            binding.cameraHomeBtn.setVisibility(GONE);
            binding.cameraRecordViewPauseMenu.setVisibility(GONE);
        }
    };

    private void showLocationUI() {
        String latLan = binding.tvOpsinLatLong.getText().toString();
        double[] coordinates = parseCoordinates(latLan);
        if (coordinates != null) {
            if (binding.constraintLocation.getVisibility() == GONE) {
                binding.constraintLocation.setVisibility(VISIBLE);
            }

        } else {
            hideLocationUI();
        }
    }

    private void hideLocationUI() {
        if (binding.constraintLocation.getVisibility() == VISIBLE) {
            binding.constraintLocation.setVisibility(GONE);
        }
    }

    public static double[] parseCoordinates(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+)\\s*([-+]?[0-9]*\\.?[0-9]+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String latitudeStr = matcher.group(1);
            String longitudeStr = matcher.group(2);
            if (latitudeStr != null && longitudeStr != null) {
                double latitude = Double.parseDouble(latitudeStr);
                double longitude = Double.parseDouble(longitudeStr);
                return new double[]{latitude, longitude};
            }
        }
        return null;
    }

    @SuppressLint("NewApi")
    private void hideNavigationBar() {
        ComponentActivity activity = (ComponentActivity) requireActivity();
        View decorView = activity.getWindow().getDecorView();

        // Enable edge-to-edge mode for modern Android versions
        EdgeToEdge.enable(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) and above
            WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            // Android 10 (API 29) and below
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }


    public static void avoidDoubleClicks(final View view) {
        final long DELAY_IN_MS = 900;
        if (!view.isClickable()) {
            return;
        }
        view.setClickable(false);
        view.postDelayed(() -> view.setClickable(true), DELAY_IN_MS);
    }

    private void backToHome() {
        // to avoid junk files to store in media
        if (timerState && isRecordingStarted) {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    updateRecordScreen();
                    cameraViewModel.stopRecordingVideo();
                    break;
                case OPSIN:
                    updateRecordScreen();
                    tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                    break;
                case NIGHTWAVE_DIGITAL:
                    if (textureView != null){
                        binding.videoContainer.removeView(textureView);
                        textureView = null;
                    }
                    // uncomment for video record and Image capture
                    updateRecordScreen();
                    tcpConnectionViewModel.stopDigitalVideoRecordLocally();
                    Log.e(TAG, "onPause called: NW_Digital");
                    break;
            }
        } else {
            updateRecordScreen();
            seconds = 0;
        }
        HomeViewModel.hasShowProgressBar = false;
        isRecordingStarted = false;
        isSdCardRecordingStarted = false;
        shouldReceiveUDP = false;
        cameraViewModel.setShowExpandMenu(false);
        cameraViewModel.setShowAllMenu(false);
        screenType = HOME;
        firmwareUpdateSequence.clear();
        fwMode = MODE_NONE;
        isChangeCameraNameButtonPressed = false;
        isFactoryRestButtonPressed = false;
        isSettingButtonPressed = false;
        isInfoButtonPressed = false;
        isFirstBitmapReceived = false;
        cameraViewModel.setIsCapturedImageInLive(false);
        cameraViewModel.setSelectPreset(false);
        applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
        homeViewModel.setSelectedCamera("");
        homeViewModel.hasShowProgressBar(false);
        aspectRatioState = false;
        videoCropState = CameraViewModel.VideoCropState.CROP_ON;

        switch (currentCameraSsid) {
            case OPSIN:
                Log.d(TAG, "backToHome: opsin live view");
                stopPeriodicTimer();

                isSelectArrow = false;
                if (CameraViewModel.opsinRecordButtonState == CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Start) {
                    CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
                    cameraViewModel.resetOpsinRecordUI();
                    tcpConnectionViewModel.stopOpsinCameraRecord();
                } else {
                    CameraViewModel.opsinRecordButtonState = CameraViewModel.OpsinRecordButtonState.Opsin_Camera_Record_Stop;
                    cameraViewModel.resetOpsinRecordUI();
                }

                new Handler().post(() -> {
                    if (CameraViewModel.cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                        tcpConnectionViewModel.stopOpsinLiveStreaming();
                        if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                            tcpConnectionViewModel.stopOpsinVideoRecordLocally();
                            CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                        }
                    }
                });
                homeViewModel.setInsertOrUpdateSsid("");
                homeViewModel.setInsertOrUpdateNWDSsid("");
                break;
            case NIGHTWAVE:
                binding.image.setImageResource(R.drawable.ic_camera_background);
                Log.d("liveTAG", "backToHome: ");
                if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                    tcpConnectionViewModel.stopLiveView();
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    binding.cameraRecordButton.setVisibility(GONE);
                }

                if (CameraViewModel.hasNewFirmware()) {
                    tcpConnectionViewModel.observeCameraMode().removeObserver(observeCameraMode);
                    tcpConnectionViewModel.observeCameraMode().removeObservers(this.getViewLifecycleOwner());

                    CameraViewModel.setHasPressedSettingCancelIcon(false); // for this condition to avoid stop live view on cancel settings dialog screen
                    CameraViewModel.setHasVisibleSettingsInfoView(false); // for this condition some time not usb mode dialog
                    CameraViewModel.setHasPressedLiveViewButton(false);

                    tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
                    tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
                    //cameraViewModel.clearCameraModeState();
                }
                imageMatrixTouchHandler.animateZoomOutToFit(50);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"backToHome :NW_Digital");
                mHandler.postDelayed(() ->  {
                    if (CameraViewModel.recordButtonState == CameraViewModel.RecordButtonState.LIVE_VIEW_STARTED) {
                        CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                        if (textureView != null) {
                            binding.videoContainer.removeView(textureView);
                            textureView = null;
                        }
                    }
                },0);
                tcpConnectionViewModel.stopServiceDigital();
                homeViewModel.setNavigateToNextScreen(false);

                Constants.currentStreamQuality = STREAM_QUALITY_LOW;

                if (tcpConnectionViewModel.observeOnStreamingErrorCode().hasObservers()){
                    tcpConnectionViewModel.observeOnStreamingErrorCode().removeObserver(observeStreamingErrorCode);
                }
                 if (tcpConnectionViewModel.observeRetryCountData().hasObservers()){
                    tcpConnectionViewModel.observeRetryCountData().removeObserver(observeRetryValue);
                }

                 if (tcpConnectionViewModel.observeStreamingReadyToVisible().hasObservers()){
                    tcpConnectionViewModel.observeStreamingReadyToVisible().removeObserver(observeStreamingReadyToVisible);
                }
                binding.image.setImageResource(R.drawable.ic_nw_digital_background);
                break;
        }

        if (tcpConnectionViewModel != null) {
            tcpConnectionViewModel.setSSIDSelectedManually(false);
            tcpConnectionViewModel.setFirmwareUpdateCompleted(false);
            homeViewModel.setHasShowFwDialog(false);
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                tcpConnectionViewModel.disconnectSocket();
                tcpConnectionViewModel.isSocketConnected().removeObservers(lifecycleOwner);
            }
        }
        homeViewModel.setBootModeCheckCount(0);
        if (homeViewModel != null) {
            // to avoid relaunch the live when click on home icon
            homeViewModel.getNavController().popBackStack(R.id.cameraFragment, true);
            homeViewModel.getNavController().popBackStack(R.id.cameraSplashFragment, true);
            homeViewModel.getNavController().navigate(R.id.homeFragment);
        }
    }

    private void hideOpsinLayout() {
        if (binding != null) {
            binding.startRecord.setVisibility(GONE);
            binding.cameraNucIcon.setVisibility(GONE);
            binding.cameraJpegIcon.setVisibility(GONE);
            binding.cameraExpostureIcon.setVisibility(GONE);
            binding.cameraBatteryIcon.setVisibility(GONE);
            binding.cameraMicIcon.setVisibility(GONE);
            binding.cameraFpsIcon.setVisibility(GONE);
            binding.cameraZoomInIcon.setVisibility(GONE);
            binding.cameraZoomOutIcon.setVisibility(GONE);
            binding.zoomLevelText1.setVisibility(GONE);
            binding.timerLayout.setVisibility(GONE);
            binding.compassView.setVisibility(GONE);
            binding.sdCardIcon.setVisibility(GONE);
            binding.monochromeIcon.setVisibility(GONE);
            binding.noiseIcon.setVisibility(GONE);
            binding.roiIcon.setVisibility(GONE);
            binding.arrowRight.setVisibility(GONE);
            binding.arrowLeft.setVisibility(GONE);
        }
    }

    private void showOpsinLayout() {
        binding.startRecord.setVisibility(GONE);
        binding.cameraNucIcon.setVisibility(VISIBLE);
        binding.cameraJpegIcon.setVisibility(GONE);
        binding.cameraExpostureIcon.setVisibility(VISIBLE);
        binding.cameraBatteryIcon.setVisibility(VISIBLE);
        binding.cameraMicIcon.setVisibility(VISIBLE);
        binding.cameraFpsIcon.setVisibility(VISIBLE);
        binding.sdCardIcon.setVisibility(VISIBLE);
        binding.monochromeIcon.setVisibility(VISIBLE);
        binding.noiseIcon.setVisibility(VISIBLE);
        binding.roiIcon.setVisibility(VISIBLE);

        if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
            if (!isSelectPIP)
                binding.compassView.setVisibility(VISIBLE);

            if (isSelectArrow) {
                binding.arrowLeft.setVisibility(VISIBLE);
                binding.arrowRight.setVisibility(GONE);
            } else {
                binding.arrowLeft.setVisibility(GONE);
                binding.arrowRight.setVisibility(VISIBLE);
            }
        } else {
            binding.compassView.setVisibility(GONE);
        }

//        binding.cameraZoomInIcon.setVisibility(View.VISIBLE);
//        binding.cameraZoomOutIcon.setVisibility(View.VISIBLE);  //commented for now
//        binding.zoomLevelText1.setVisibility(View.VISIBLE);
        binding.timerLayout.setVisibility(GONE);
    }

    private final Runnable updateTimerThread = new Runnable() {

        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int seconds = (int) (updatedTime / 1000) % 60;
            int minutes = (int) ((updatedTime / (1000 * 60)) % 60);
            int hours = (int) ((updatedTime / (1000 * 60 * 60)) % 24);

            String timerHour = String.format("%02d", hours) + " :";
            String timeMin = String.format("%02d", minutes) + " :";
            String timeMillisec = String.format("%02d", seconds);
            binding.timerHour.setText(timerHour);
            binding.timerMins.setText(timeMin);
            binding.timerMilliSec.setText(timeMillisec);
            customHandler.postDelayed(this, 0);
        }
    };

    private void resetZoomState() {
        if (binding != null) {
            binding.textureView.setScaleX(originalScaleX);
            binding.textureView.setScaleY(originalScaleY);
        }
    }

    //addition: handling pip mode in portrait and landscape
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (isInPictureInPictureMode) {
            isSelectPIP = true;
            imageMatrixTouchHandler.animateZoomOutToFit(50);
            switch (currentCameraSsid) {
                case OPSIN:
                    hideOpsinLayout();
                    break;
                case NIGHTWAVE:
                    if (binding != null) {
                        if (aspectRatioState) {
                            ((ImageView) binding.image).setScaleType(ImageView.ScaleType.CENTER_CROP);
                        } else {
                            ((ImageView) binding.image).setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                    }
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"onPictureInPictureModeChanged : NW_Digital");
                    break;
            }
            if (binding != null) {
                binding.cameraMenuView.setVisibility(GONE);
                binding.cameraHomeBtn.setVisibility(GONE);
                binding.analyticsLayout.setVisibility(GONE);
            }

            /*this is to handle aspect ratio for android verion 12, 13 and above*/
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                Rational aspectRatio = new Rational(16, 9);
                pictureInPictureParams = new PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build();
            } else {
                Rational rational;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (aspectRatioState) {
                        rational = new Rational(ASPECT_RATIO_HEIGHT_3, ASPECT_RATIO_WIDTH_4);
                    } else {
                        rational = new Rational(ASPECT_RATIO_HEIGHT_9, ASPECT_RATIO_WIDTH_16);
                    }
                } else {
                    if (aspectRatioState) {
                        rational = new Rational(ASPECT_RATIO_WIDTH_4, ASPECT_RATIO_HEIGHT_3);
                    } else {
                        rational = new Rational(ASPECT_RATIO_WIDTH_16, ASPECT_RATIO_HEIGHT_9);
                    }
                    //      isSelectPIP = true;
                }
                pictureInPictureParams = new PictureInPictureParams.Builder().setAspectRatio(rational).build();
            }
            Log.e(TAG, "onPictureInPictureModeChanged: " + aspectRatioState + "/" + videoCropState);
        } else {
            imageMatrixTouchHandler.animateZoomOutToFit(50);
            if (tcpConnectionViewModel != null) {
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    CameraViewModel.recordButtonState = CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED;
                    tcpConnectionViewModel.startOpsinLiveStreaming();
                    showOpsinLayout();
                    triggerTimerToUpdateStatusIcons();
                }
            }
        }
    }

    private void nonPipMode() {
        if (binding != null) {
            if (binding.cameraPictureInPictureBtn.getVisibility() == GONE) {
                binding.cameraPictureInPictureBtn.setVisibility(VISIBLE);
            }
            if (binding.cameraHomeBtn.getVisibility() == GONE)
                binding.cameraHomeBtn.setVisibility(VISIBLE);

        }
    }

    private void updateRecordScreen() {
        timerState = false;
        seconds = 0;
        isRecordingStarted = false;
        binding.cameraRecordScreen.setVisibility(GONE);
        binding.cameraRecordTimerLayout.setVisibility(GONE);
    }

    private void showRecordViewLayout() {
        binding.cameraMenuView.setVisibility(GONE);
        binding.cameraRecordViewScreen.setVisibility(GONE);
        binding.cameraRecordViewPauseMenu.setVisibility(GONE);
        binding.cameraRecordSaveLayout.setVisibility(GONE);
        timerState = true;
        startVideoRecordTimer();
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                cameraViewModel.startRecordingVideo();
                break;
            case OPSIN:
                tcpConnectionViewModel.startOpsinVideoRecordLocally();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"showRecordViewLayout");
                // uncomment for video record and Image capture
                tcpConnectionViewModel.startDigitalVideoRecordLocally();
                break;
        }
        binding.cameraRecordTimerLayout.setVisibility(VISIBLE);
        binding.cameraRecordScreen.setVisibility(VISIBLE);
        binding.cameraRecordButton.setClickable(true);
        binding.compassView.setVisibility(GONE);
    }

    private void showGallery() {
        onSelectGallery = true;
        Log.d("recordTAG", "subscribeUI: " + isRecordingStarted);
        if (isRecordingStarted) {
            binding.cameraRecordViewPauseMenu.setVisibility(GONE);
            binding.cameraRightMenuView.setVisibility(GONE);
            binding.cameraLeftMenuView.setVisibility(GONE);
            binding.compassView.setVisibility(GONE);
            homeViewModel.getNavController().navigate(R.id.galleryTabLayoutDialog);
            return;
        }
        //   binding.cameraRecordViewMenu.setVisibility(View.GONE);
        //Gallery in Live View
        homeViewModel.getNavController().navigate(R.id.galleryTabLayoutDialog);
        cameraViewModel.setShowExpandMenu(true);
        cameraViewModel.setIsShowAlertDialog(true);
        binding.cameraRightMenuView.setVisibility(GONE);
        binding.cameraLeftMenuView.setVisibility(GONE);

        if (binding.cameraRecordViewScreen.getVisibility() == VISIBLE) {
            binding.cameraRecordViewScreen.setVisibility(GONE);
        }
        if (binding.cameraRecordScreen.getVisibility() == VISIBLE) {
            binding.cameraRecordScreen.setVisibility(GONE);
        }
        binding.compassView.setVisibility(GONE);

        timerState = false;
        seconds = 0;
        hasShowExpandMenu();
    }


    @Override
    public void onTouchReleased() {
        Log.e(TAG, "onTouchReleased:");
        if (!cameraViewModel.isCapturedImage && !onSelectGallery && !isInfoButtonPressed && !isSettingButtonPressed && !cameraViewModel.isSelectPreset()) {
            showAutohideLayout();
        }

        if (CameraViewModel.recordButtonState != CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED && aspectRatioState && videoCropState == CameraViewModel.VideoCropState.CROP_ON){
            observeAspectRatioState();
        }

        if (cameraViewModel.isLiveViewScreenInBackground()){
            observeAspectRatioState();
        }
    }

    @Override
    public void onTouch() {
        Log.e(TAG, "onTouch: ");
        if (CameraViewModel.recordButtonState != CameraViewModel.RecordButtonState.LIVE_VIEW_STOPPED && aspectRatioState && videoCropState == CameraViewModel.VideoCropState.CROP_ON ){
            observeAspectRatioState();
        }

        if (cameraViewModel.isLiveViewScreenInBackground()){
            observeAspectRatioState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy called " + CameraViewModel.recordButtonState);
    }
}
