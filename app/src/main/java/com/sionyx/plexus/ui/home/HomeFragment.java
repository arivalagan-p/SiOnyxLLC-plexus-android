package com.sionyx.plexus.ui.home;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.networkCapability;
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
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.shouldReceiveUDP;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.FPGA;
import static com.dome.librarynightwave.utils.Constants.OPSIN_FACTORY;
import static com.dome.librarynightwave.utils.Constants.OPSIN_FULL_IMAGE;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RESTART;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_FPGA;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_OVERLAY;
import static com.dome.librarynightwave.utils.Constants.OPSIN_RISCV_RECOVERY;
import static com.dome.librarynightwave.utils.Constants.OPSIN_WIFI_BLE;
import static com.dome.librarynightwave.utils.Constants.RISCV;
import static com.dome.librarynightwave.utils.Constants.STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.USB_VIDEO_NOT_SUPPORTED_VERSION;
import static com.dome.librarynightwave.utils.Constants.WIFI_RTOS;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.getFormattedAperture;
import static com.dome.librarynightwave.utils.Constants.getFormattedFocalLength;
import static com.dome.librarynightwave.utils.Constants.isSDK10;
import static com.dome.librarynightwave.utils.Constants.isSDKBelow13;
import static com.dome.librarynightwave.utils.Constants.mState;
import static com.dome.librarynightwave.utils.Constants.mWifiState;
import static com.dome.librarynightwave.utils.Constants.opsinWiFiOldFirmwareVersion;
import static com.dome.librarynightwave.utils.Constants.rtspState;
import static com.dome.librarynightwave.utils.Constants.strUnknown;
import static com.google.android.material.tabs.TabLayout.GRAVITY_FILL;
import static com.sionyx.plexus.ui.MainActivity.hasAlreadyAddedInDialogTag;
import static com.sionyx.plexus.ui.camera.CameraFragment.avoidDoubleClicks;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.ADD_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.FW_UPDATE_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.HOME;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.hasShowProgressBar;
import static com.sionyx.plexus.ui.home.HomeViewModel.isShowTimeZoneLayout;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.ui.home.HomeViewModel.setSelectedTimeZone;
import static com.sionyx.plexus.ui.home.HomeViewModel.setShowTimeZoneLayout;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.getWiFiHistory;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.popupWindow;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.setWiFiHistory;
import static com.sionyx.plexus.utils.Constants.WIFI_AVAILABLE;
import static com.sionyx.plexus.utils.Constants.WIFI_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;
import static com.sionyx.plexus.utils.Constants.WIFI_NOT_CONNECTED;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;
import static com.sionyx.plexus.utils.Constants.isLoginEnable;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.repository.opsinmodel.OpsinVersionDetails;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.SCCPConstants;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentHomeBinding;
import com.sionyx.plexus.databinding.FragmentPopUpLayoutBinding;
import com.sionyx.plexus.databinding.PopUpTimeZoneListViewBinding;
import com.sionyx.plexus.databinding.RecordingInfoScreenBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraInfoViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.dialog.BottomSheetLogoutDialog;
import com.sionyx.plexus.ui.dialog.CameraTimeZoneAdapter;
import com.sionyx.plexus.ui.dialog.GalleryAllViewModel;
import com.sionyx.plexus.ui.dialog.NoticeDialogFragment;
import com.sionyx.plexus.ui.dialog.TimeZoneViewModel;
import com.sionyx.plexus.ui.home.adapter.NearByDeviceAdapter;
import com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter;
import com.sionyx.plexus.ui.home.adapter.SelectedDeviceHistoryAdpter;
import com.sionyx.plexus.ui.home.decoration.HorizontalSpaceItemDecoration;
import com.sionyx.plexus.ui.home.decoration.VerticalSpaceItemDecoration;
import com.sionyx.plexus.ui.home.gallerybottomview.bottomtabviewadapter.GalleryBottomTabPageAdapter;
import com.sionyx.plexus.ui.info.InfoViewTabLayoutAdapter;
import com.sionyx.plexus.ui.login.LoginViewModel;
import com.sionyx.plexus.ui.model.CurrentFwVersion;
import com.sionyx.plexus.ui.model.Manifest;
import com.sionyx.plexus.ui.popup.PopUpCameraInfoAdapter;
import com.sionyx.plexus.ui.popup.PopUpCameraSettingsViewModel;
import com.sionyx.plexus.ui.popup.PopUpSettingsAdapter;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.TooltipHelper;
import com.sionyx.plexus.utils.circulardotprogressbar.DotCircleProgressIndicator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment {
    private final String TAG = "HomeFragment";

    private static final String POSITION_KEY = "position_key";
    // public static boolean pauseState = false;
    //public static boolean stopState = false;
    private static int selectedItemPosition;
    private HomeViewModel homeViewModel;
    private BleWiFiViewModel bleWiFiViewModel;
    private DigitalCameraInfoViewModel digitalCameraInfoViewModel;
    private TCPConnectionViewModel tcpConnectionViewModel;
    private CameraInfoViewModel cameraInfoViewModel;
    private CameraViewModel cameraViewModel;
    private TimeZoneViewModel timeZoneViewModel;
    private LoginViewModel loginViewModel;
    private RecordingInfoScreenBinding recordingInfoScreenBinding;
    private FragmentPopUpLayoutBinding fragmentPopUpLayoutBinding;
    private PopUpTimeZoneListViewBinding popUpTimeZoneListViewBinding;
    private String videoFilePath = "";
    private int playbackPosition;
    private final String intialState = "0:00:00";
    private FragmentHomeBinding binding;
    private SelectDeviceAdapter adapter;
    private LifecycleOwner lifecycleOwner;
    private int orientation;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    // private String connectedSsid;
    private NearByDeviceAdapter nearByDeviceAdapter;
    private SelectedDeviceHistoryAdpter selectedDeviceHistoryAdpter;
    public static String galleryFilePath;
    private final int StopPlayBack = -1;
    //button state: to handle orientation of gallery Layout
    private final int GALLERY_LAYOUT = 0;
    private final int MANAGE_LAYOUT = 1;
    private final int RECORDING_VIDEO_INFO_LAYOUT = 2;
    private final int RECORDING_VIDEO_PLAYER_LAYOUT = 3;
    private final int GALLERY_CANCEL = 4;
    private final int BACK_STACK_GALLERY_SCREEN = 5;
    private final int BACK_STACK_MANAGE_SCREEN = 6;
    private final int BACK_STACK_RECORDED_VIDEO_INFO_SCREEN = 7;
    private final int BACK_STACK_RECORDED_VIDEO_PLAYER_SCREEN = 8;
    private final int INFO_LAYOUT_BACK_STACKING = 10;
    private final int SETTINGS_LAYOUT_BACK_STACKING = 12;
    private final int INFO_LAYOUT = 9;
    private final int SETTINGS_LAYOUT = 11;
    private final int NEGATIVE_BUTTON_CLICKED = 0;
    private final int NEUTRAL_BUTTON_CLICKED = 1;
    private final int POSITIVE_BUTTON_CLICKED = 2;
    private static final String popUpInfoScreen = "popUpInfoScreen";
    private static final String popUpSettingsScreen = "popUpSettingsScreen";
    private static final String popUpNone = "popUpNone";

    public static boolean isSelectPopUpSettings;
    public static boolean isSelectPopUpInfo;
    public static boolean isSelectPopUpFwUpdateCheck;
    public static boolean backButton;

    private String sCameraWiFiRtos, sCameraRiscv, sCameraRiscvRecovery, sCameraOverlay, sCameraFpga, sCameraBle, sMasterVersion = null, sAppRiscv = null, sAppRiscvRecovery = null, sAppOverlay = null, sAppWiFiRtos = null, sAppBle = null, sAppFpga = null, sReleaseNotes = null;
    private WifiManager mWifiManager;
    private ConnectivityManager connectivityManager;
    private AlertDialog pleaseWaitAlertDialog;
    private TextView progressTextView;
    public static boolean isUpdatingCameraName;
    private final String regex = "^[a-zA-Z0-9!@#$%&*]*$";
    private GalleryBottomTabPageAdapter manageViewTabAdapter;
    private GalleryAllViewModel galleryAllViewModel;
    private MediaPlayer mediaPlayer;
    private PopUpCameraInfoAdapter popUpCameraInfoAdapter;
    private PopUpCameraSettingsViewModel popUpCameraSettingsViewModel;
    private int seekState;
    private boolean isUnknownWiFiVersion = false;
    private float fCameraFpga = 0;

    private SharedPreferences toolTipSharedPreference;
    private TooltipHelper tooltipHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        orientation = requireContext().getResources().getConfiguration().orientation;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        mWifiManager = (WifiManager) requireActivity().getSystemService(Context.WIFI_SERVICE);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(requireActivity()).get(DigitalCameraInfoViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        cameraInfoViewModel = new ViewModelProvider(requireActivity()).get(CameraInfoViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        popUpCameraSettingsViewModel = new ViewModelProvider(requireActivity()).get(PopUpCameraSettingsViewModel.class);
        timeZoneViewModel = new ViewModelProvider(this).get(TimeZoneViewModel.class);
        binding.setViewModel(homeViewModel);
        binding.dotProgressBar.setIndicator(new DotCircleProgressIndicator());
        binding.customProgressBar.setIndicator(new DotCircleProgressIndicator());
        binding.cameraConnectProgressBar.setIndicator(new DotCircleProgressIndicator());
        binding.fwDotProgressBar.setIndicator(new DotCircleProgressIndicator());
        mWifiManager = (WifiManager) requireActivity().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        /*included -recording Info Layout in Home Fragment*/
        recordingInfoScreenBinding = binding.recordingInfoLayout;
        recordingInfoScreenBinding.setViewModel(homeViewModel);
        /*included -Pop UP Layout Fragment in Home Fragment*/
        fragmentPopUpLayoutBinding = binding.popUpWindowLayout;
        popUpTimeZoneListViewBinding = binding.popUpWindowLayout.popUpTimeZoneListLayout;
        fragmentPopUpLayoutBinding.setViewModel(homeViewModel);
        galleryAllViewModel = new ViewModelProvider(requireActivity()).get(GalleryAllViewModel.class);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        hideNavigationBar();
        toolTipSharedPreference = activity.getSharedPreferences("ToolTipPromptState", MODE_PRIVATE);
        tooltipHelper = new TooltipHelper(requireActivity());

        binding.galleryShareButton.setVisibility(View.GONE);
        binding.recordingInfoLayout.recordingInfoSocialMediaLayout.setVisibility(View.GONE);

        homeViewModel.getEnterExitMultiWindowMode().observe(lifecycleOwner, new EventObserver<>(isMultiWindowModeActivated -> {
            Log.e(TAG, "getEnterExitMultiWindowMode: " + isMultiWindowModeActivated);
            if (isMultiWindowModeActivated) {
                handleSplitScreenEnabled();
                homeViewModel.hasEnableMultiWindowMode();
            } else {
                handleSplitScreenDisabled();
            }
        }));
        binding.loginLogoutIcon.setVisibility(isLoginEnable ? View.VISIBLE : View.GONE);

        binding.fwDotProgressBar.setVisibility(GONE);
        return binding.getRoot();
    }

    private void handleSplitScreenDisabled() {
        binding.bottomContainer.setVisibility(VISIBLE);
       // binding.loginLogoutIcon.setVisibility(VISIBLE);

        if (HomeViewModel.screenType == ADD_SCREEN)
            binding.selectedDeviceHistoryLayout.setVisibility(VISIBLE);

        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Guideline guideLine = binding.getRoot().findViewById(R.id.recycler_view_guideline_bottom);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
            params.guidePercent = 0.8082f;
            guideLine.setLayoutParams(params);

            Guideline guideLine1 = binding.getRoot().findViewById(R.id.recycler_container_start_view_guideline_bottom);
            ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) guideLine1.getLayoutParams();
            params1.guidePercent = 0.6681f;
            guideLine1.setLayoutParams(params1);
        } else {
            Guideline guideLine = binding.getRoot().findViewById(R.id.select_device_guideline_vertical_recyclerView_start);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
            params.guideBegin = (int) (170 * getResources().getDisplayMetrics().density);

            guideLine.setLayoutParams(params);

            Guideline guideLine1 = binding.getRoot().findViewById(R.id.nearby_device_recycler_view_guideline_vertical_end);
            ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) guideLine1.getLayoutParams();
            params1.guidePercent = 0.7818f;
            guideLine1.setLayoutParams(params1);
        }

    }

    private void handleSplitScreenEnabled() {
        if (activity.isInMultiWindowMode()) {
            binding.loginLogoutIcon.setVisibility(GONE);
            binding.bottomContainer.setVisibility(View.GONE);
            if (HomeViewModel.screenType == POP_UP_SETTINGS_SCREEN || HomeViewModel.screenType == POP_UP_INFO_SCREEN) {
                homeViewModel.onPopUpViewCancel();
            }
            if (HomeViewModel.screenType == INFO_SCREEN) {
                homeViewModel.onCancelInfoView();
            }

            if (HomeViewModel.screenType == GALLERY_SCREEN) {
                homeViewModel.onCancelGalleryView();
            }

            if (screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                homeViewModel.setGalleryVideoPlayerSelected(false);
                homeViewModel.setGalleryVideoRecordedInfoSelected(false);
                homeViewModel.onCancelGalleryView();
            }

            if (HomeViewModel.screenType == ADD_SCREEN) {
                binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            }

            int orientation = this.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Guideline guideLine = binding.getRoot().findViewById(R.id.recycler_view_guideline_bottom);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                params.guidePercent = 0.99f;
                guideLine.setLayoutParams(params);

                Guideline guideLine1 = binding.getRoot().findViewById(R.id.recycler_container_start_view_guideline_bottom);
                ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) guideLine1.getLayoutParams();
                params1.guidePercent = 0.99f;
                guideLine1.setLayoutParams(params1);
            } else {
                Guideline guideLine = binding.getRoot().findViewById(R.id.select_device_guideline_vertical_recyclerView_start);
                if (guideLine != null) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                    params.guideBegin = 10;
                    params.guidePercent = 0.0864f;
                    guideLine.setLayoutParams(params);

                    Guideline guideLine1 = binding.getRoot().findViewById(R.id.nearby_device_recycler_view_guideline_vertical_end);
                    ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) guideLine1.getLayoutParams();
                    params1.guidePercent = 0.9135f;
                    guideLine1.setLayoutParams(params1);
                }
            }

        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        binding.settingsImageView.setVisibility(View.GONE);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ((ConstraintLayout.LayoutParams) binding.galleryImageView.getLayoutParams()).horizontalBias = 0.0f;
        }
        shouldReceiveUDP = false;


        if (homeViewModel.isInfoSelected()) {
            binding.infoImageView.setImageResource(R.drawable.ic_info_white_background);
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        } else {
            if (binding != null) {
                binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            }
        }

        //addition: Gallery-Icon Selected
        if (homeViewModel.isGallerySelected()) {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_white_background);
            binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        } else {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
            // showHomeScreen();
        }

        //Manage-Button State
        if (homeViewModel.isGalleryManageSelected()) {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_white_background);
            binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        } else {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
        }

        if (homeViewModel.isGalleryVideoRecordedInfoSelected()) {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_white_background);
            binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        } else {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
        }

        if (homeViewModel.isGalleryVideoPlayerSelected()) {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_white_background);
            binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        } else {
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
        }

        //Settings-Icon Selected
        if (homeViewModel.isSettingsSelected()) {
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_white_background);
            binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
        } else {
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        }

        if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
            binding.infoImageView.setClickable(false);
            binding.galleryImageView.setClickable(false);
        }

        /* this state is to stop the clicking state of info and gallery in home screen while pop-ip is enabled*/
       /* if (isSelectPopUpSettings || isSelectPopUpInfo) {
            binding.infoImageView.setClickable(false);
            binding.galleryImageView.setClickable(false);
        }*/

        try {
            if (!homeViewModel.isPermissionChecked()) {
                try {
                    ((MainActivity) requireActivity()).checkPermissions();
                    ((MainActivity) requireActivity()).checkGpsOnOffState();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (HomeViewModel.screenType == HOME) {
                    if (mWifiManager.isWifiEnabled()) {
                        homeViewModel.connectedSsid = getWiFiSSID(mWifiManager);
                        if (homeViewModel.connectedSsid != null) {
                            Log.e(TAG, "onViewCreated: CONNECTED_SSID " + homeViewModel.connectedSsid);
                            if (!homeViewModel.connectedSsid.equals("") && !homeViewModel.connectedSsid.equals("<unknown ssid>")) {
                                if (isCamera()) {
                                    bleWiFiViewModel.setConnectedSsidFromWiFiManager(homeViewModel.connectedSsid);
//                                    if (!bleWiFiViewModel.hasAlreadyExistSSId(homeViewModel.connectedSsid)) {
                                    homeViewModel.setInsertOrUpdateSsid(homeViewModel.connectedSsid);
                                    notifyAdapters();
//                                    }
                                }
                            }
                        }
                        Log.d("Connected WiFi SSID", homeViewModel.connectedSsid);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        subscribeUI();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void subscribeUI() {
        viewGroupStateHandling();
        addButtonStateHandling();
        selectDeviceOrientationHandling();
        Log.e(TAG, "subscribeUI(): " + HomeViewModel.screenType.name());

        /*handled back press for layout group */
        requireActivity().getOnBackPressedDispatcher().addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        });

        /* for this AWS login and logout */
        homeViewModel.isSignInOrSignOutAccount.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    BottomSheetLogoutDialog bottomSheetLogoutDialog = new BottomSheetLogoutDialog();
                    if (activity != null)
                        bottomSheetLogoutDialog.show(activity.getSupportFragmentManager(), bottomSheetLogoutDialog.getTag());
                });
            }
        }));
        /* AWS function end */

      /*for this when wifi disconnect show wifi disconnect dialog if press `yes` or `no` go to home screen
        default(IF current screen `firmware update` then dialog select `X`button reset to fw update all version show dialog ui default value(false)*/
        homeViewModel.isSelectWifiDialogPositiveButton.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
//            showOpsinCameraPowerOffDialog(getString(R.string.after_wifi_upgrade)); // for this opsin camera power cycle dialog after wifi disconnect
            Log.e(TAG, "isSelectWifiDialogPositiveButton: " + aBoolean);

            if (screenType == HomeViewModel.ScreenType.FW_UPDATE_SCREEN) {
                tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
            }

            if (aBoolean) {
                if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.HOME) {
                    showHomeScreen();
                    /*hide progressbar(when tcp socket connection timeout dialog dismiss but show progressbar) */
                    hideProgressBar();
                }
            } else {
                showHomeScreen();
                /*hide progressbar(when tcp socket connection timeout dialog dismiss but show progressbar) */
                hideProgressBar();
                hideWiFiDisconnectProgressbar();
                homeViewModel.setHasShowFwDialog(false);/* for this show fw update dialog with all fw_version in ui. after complete fw update ui hide and show home screen */
            }
        }));

        /*when fw update user goes to background an come to foreground this event trigger*/
        homeViewModel.isUpdateFirmwareBGToFG.observe(lifecycleOwner, new EventObserver<>(hasUpdateFirmwareBgToFg -> {
            if (hasUpdateFirmwareBgToFg) {
                Log.e(TAG, "Update state: " + HomeViewModel.screenType);
                showHomeScreen();
            }
        }));

        /*for this fw update mode check is golden or normal*/
        homeViewModel.isUpdateFirmwareRecoverMode.observe(lifecycleOwner, new EventObserver<>(hasUpdateFirmwareRecoverMode -> {
            if (hasUpdateFirmwareRecoverMode) {
                homeViewModel.setAbnormalFwUpdateSequenceIndex(0);
                homeViewModel.setAbnormalFwUpdate(true);
                startAbnormalBehaviourFwUpdateSequence();
                homeViewModel.startFirmwareRecoverMode(false);/* for this when rotate an click ok button set to default*/
                hideProgressBar(); /*for this when select ok in dialog progressbar dismiss*/
                Log.e(TAG, "hideProgressBar:4 ");
            }
        }));

        homeViewModel.hasUpgradeWifiFirmwareOnlyIfWiFiVersionIsNull.observe(lifecycleOwner, new EventObserver<>(hasUpdateFirmwareRecoverMode -> {
            if (hasUpdateFirmwareRecoverMode) {
                //start wifi firmware upgrade...
                homeViewModel.getCurrentFwVersion().setWifiRtos(opsinWiFiOldFirmwareVersion);
                Constants.setOpsinContainsOldWiFiFirmware(true);
                firmwareUpdateSequence.clear();
                firmwareUpdateSequence.add(WIFI_RTOS);
                tcpConnectionViewModel.getBatteryInfo();
            } else {
                Object object = tcpConnectionViewModel.observeOpsinCameraVersionInfo().getValue().peekContent();
                if (object != null) {
                    firmwareUpdateSequence.clear();
                    opsinFirmwareUpgradeCheck((OpsinVersionDetails) object, true);
                } else {
                    showFwUpdateDialog();
                }
            }
        }));

        /*for this when select `ok` button in dialog for(fw update sequence) progressbar dismiss*/
        homeViewModel.isShowProgressBar.observe(lifecycleOwner, new EventObserver<>(hasShowProgressBar -> {
            if (!hasShowProgressBar) {
                hideProgressBar();
                homeViewModel.setHasShowProgressBar(false);
                binding.dotProgressBar.setVisibility(GONE);
                Log.e(TAG, "hideProgressBar:5 ");
                if (screenType == POP_UP_SETTINGS_SCREEN)
                    popUpCameraSettingsViewModel.setHasShowProgress(false);
            }
        }));
        /*for this when wifi disconnect show wifi disconnect dialog if press `yes` or `no` go to home screen and stop firmware update progress in UI */
        homeViewModel.isStopFwUIProgress.observe(lifecycleOwner, new EventObserver<>(hasShowProgressBar -> {
            if (hasShowProgressBar) {
                tcpConnectionViewModel.setFirmwareUpdateCompleted(true);
                homeViewModel.setAbnormalFwUpdate(false);
                homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) tcpConnectionViewModel.getRiscBootMode();
                tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
                tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
                hideProgressBar(); /*for this when select ok in dialog progressbar dismiss*/
                hideWiFiDisconnectProgressbar();/* for this wifi dialog fw update complete after few minutes later to wifi disconnect*/
                Log.e(TAG, "hideProgressBar:6");
            }
        }));
        /*for this when firmware update failed show dialog if press `yes` reset fw update sequence and retry fw update sequence and `no` go to home screen and stop firmware update progress in UI */
        homeViewModel.isFWUpdateFailed.observe(lifecycleOwner, new EventObserver<>(hasUpdateFailed -> {
            if (hasUpdateFailed) {
                resetFirmwareUpdateFailed();
                showHomeScreen();
                homeViewModel.onSelectCamera(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
            } else {
                resetFirmwareUpdateFailed();
                showHomeScreen();
                homeViewModel.setHasShowFwDialog(false);/* for this show fw update dialog with all fw_version in ui. after complete fw update ui hide and show home screen */
            }
        }));

        homeViewModel.isSelectInfo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (HomeViewModel.screenType == POP_UP_INFO_SCREEN) {
                hasShowProgressBar = false;
            }
            if (homeViewModel.hasShowProgressBar()) {
                if (HomeViewModel.screenType == HOME) {
                    binding.infoImageView.setClickable(false);
                    Log.e(TAG, "subscribeUI: true" + HomeViewModel.screenType.name());
                }
                /* this state is to stop the clicking state of info and gallery in home screen while pop-ip is enabled*/
//            } else if (isSelectPopUpSettings || isSelectPopUpInfo) {
////                binding.infoImageView.setClickable(false);
////                binding.galleryImageView.setClickable(false);
            } else {
                // for this reset state while app in pop up settings screen to press bottom info icon button
                popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.MANUAL;
                popUpCameraSettingsViewModel.setDateTimeModePosition(0);

                homeViewModel.setSettingsSelected(false);
                homeViewModel.setGallerySelected(false);
                binding.infoImageView.setClickable(true);
                avoidDoubleClicks(binding.infoImageView);
                if (aBoolean) {
                    if (HomeViewModel.screenType != FW_UPDATE_SCREEN) {
                        if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
                            homeViewModel.onCancelGalleryView();
                        }
                        popUpCameraInfoAdapter = null;
                        homeViewModel.popUpSettingsAdapter = null;
                        fragmentPopUpLayoutBinding.popUpViewPager.setAdapter(null);
                        fragmentPopUpLayoutBinding.popUpSettingsViewPager.setAdapter(null);

                        homeViewModel.hasSelectPopUpSettings(false);
                        homeViewModel.hasSelectPopUpInfo(false);
                        homeViewModel.closePopupSettingFragment(true);
                        homeViewModel.closePopupCameraAdjusmentFragment(true);
                        homeViewModel.closePopupCameraInfoFragment(true);

                        HomeViewModel.screenType = HomeViewModel.ScreenType.INFO_SCREEN;
                        homeViewModel.setBottomLayoutType(INFO_LAYOUT);
                        binding.viewPager.setCurrentItem(0);
                        /* for this pop up (info / settings) screen view to click app info icon show info view and then press back if tcp already connected go to live view so, to avoid this case */
                        if (tcpConnectionViewModel != null) {
                            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                            if (value != null && value == Constants.STATE_CONNECTED) {
                                disConnectCamera();
                            }
                        }
                    }
                }
            }
        }));
        homeViewModel.isSelectGallery.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.d(TAG, "pop_TAG_selectGallery" + HomeViewModel.screenType.name() + " " + homeViewModel.hasShowProgressBar() + hasShowProgressBar);
            if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                return;
            }
            if (HomeViewModel.screenType == POP_UP_INFO_SCREEN) {
                hasShowProgressBar = false;
            }
            if (homeViewModel.hasShowProgressBar()) {
                if (HomeViewModel.screenType == HomeViewModel.ScreenType.HOME) {
                    binding.galleryImageView.setClickable(false);
                    Log.e(TAG, "subscribeUI: true" + HomeViewModel.screenType.name());
                    homeViewModel.setCurrentManageTabPosition(0);
                }
            } else {
                // for this reset state while app in pop up settings screen to press bottom gallery icon button
                popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.MANUAL;
                popUpCameraSettingsViewModel.setDateTimeModePosition(0);
                avoidDoubleClicks(binding.galleryImageView);
                binding.galleryImageView.setClickable(true);
                if (aBoolean) {
                    if (HomeViewModel.screenType != FW_UPDATE_SCREEN) {
                        if (screenType == INFO_SCREEN) {
                            homeViewModel.onCancelInfoView();
                        }
                        popUpCameraInfoAdapter = null;
                        homeViewModel.popUpSettingsAdapter = null;
                        fragmentPopUpLayoutBinding.popUpViewPager.setAdapter(null);
                        fragmentPopUpLayoutBinding.popUpSettingsViewPager.setAdapter(null);

                        homeViewModel.hasSelectPopUpSettings(false);
                        homeViewModel.hasSelectPopUpInfo(false);
                        homeViewModel.closePopupSettingFragment(true);
                        homeViewModel.closePopupCameraAdjusmentFragment(true);
                        homeViewModel.closePopupCameraInfoFragment(true);
                        homeViewModel.setCurrentManageTabPosition(0);
                        if (screenType == HOME || screenType == INFO_SCREEN || screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN || screenType == ADD_SCREEN) {
                            homeViewModel.setBottomLayoutType(GALLERY_LAYOUT);
                        }
                        homeViewModel.setGallerySelected(true);
                        homeViewModel.setGalleryManageSelected(true);
                        homeViewModel.setGalleryVideoPlayerSelected(true);
                        homeViewModel.setGalleryVideoRecordedInfoSelected(true);
                        homeViewModel.setInfoSelected(false);
                        homeViewModel.setSettingsSelected(false);
                        HomeViewModel.screenType = HomeViewModel.ScreenType.GALLERY_SCREEN;
                        binding.galleryImageView.setImageResource(R.drawable.ic_image_white_background);
                        binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
                        binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
                        binding.selectDeviceLayout.setVisibility(View.GONE);
                        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
                        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
                        binding.infoTabLayout.setVisibility(View.GONE);
                        binding.settingsTabLayout.setVisibility(View.GONE);
                        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
                        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);

                        /* for this pop up (info / settings) screen view to click app info icon show info view and then press back if tcp already connected go to live view so, to avoid this case */
                        if (tcpConnectionViewModel != null) {
                            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                            if (value != null && value == Constants.STATE_CONNECTED) {
                                disConnectCamera();
                            }
                        }
                    }
                }
            }
        }));

        homeViewModel.isSelectSettings.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (homeViewModel.hasShowProgressBar()) {
                if (HomeViewModel.screenType == HomeViewModel.ScreenType.HOME) {
                    binding.settingsImageView.setClickable(false);
                }
            } else {
                binding.settingsImageView.setClickable(true);
                if (aBoolean) {
                    if (HomeViewModel.screenType != FW_UPDATE_SCREEN) {
                        homeViewModel.setSettingsSelected(true);
                        homeViewModel.setGallerySelected(false);
                        homeViewModel.setGalleryVideoPlayerSelected(false);
                        homeViewModel.setGalleryVideoRecordedInfoSelected(false);
                        homeViewModel.setInfoSelected(false);
                        binding.settingsImageView.setImageResource(R.drawable.ic_settings_white_background);
                        binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
                        binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
                        HomeViewModel.screenType = HomeViewModel.ScreenType.SETTINGS_SCREEN;
                        binding.selectDeviceLayout.setVisibility(View.GONE);
                        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
                        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
                        binding.infoTabLayout.setVisibility(View.GONE);
                        binding.galleryTabLayout.setVisibility(View.GONE);
                        binding.settingsTabLayout.setVisibility(VISIBLE);
                        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
                        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
                        /* for this pop up (info / settings) screen view to click app info icon show info view and then press back if tcp already connected go to live view so, to avoid this case */
                        if (tcpConnectionViewModel != null) {
                            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                            if (value != null && value == Constants.STATE_CONNECTED) {
                                disConnectCamera();
                            }
                        }
                    }
                }
            }
        }));

        homeViewModel.loadSelectedDevices.observe(lifecycleOwner, selectDeviceModels -> {
            if (homeViewModel.adapterNotifyState == HomeViewModel.AdapterNotifyState.NOTIFY_ALL) {
//                Log.e(TAG, "subscribeUI: NOTIFY_ALL ");
                homeViewModel.getSelectDeviceModelss().clear();
                homeViewModel.getSelectDeviceModelss().addAll(selectDeviceModels);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                homeViewModel.getDeviceHistoryModelss().clear();
                homeViewModel.getDeviceHistoryModelss().addAll(selectDeviceModels);
                homeViewModel.getDeviceHistoryModelss().remove(0);
                if (selectedDeviceHistoryAdpter != null) {
                    selectedDeviceHistoryAdpter.notifyDataSetChanged();
                }
                homeViewModel.adapterNotifyState = HomeViewModel.AdapterNotifyState.NOTIFY_NONE;
            } else if (homeViewModel.adapterNotifyState == HomeViewModel.AdapterNotifyState.NOTIFY_ADDED) {
//                Log.e(TAG, "subscribeUI: NOTIFY_ADDED ");
                notifyAdded(selectDeviceModels);
            } else if (homeViewModel.adapterNotifyState == HomeViewModel.AdapterNotifyState.NOTIFY_MOVED) {
//                Log.e(TAG, "subscribeUI: NOTIFY_MOVED ");
                notifyAdded(selectDeviceModels);
            } else if (homeViewModel.adapterNotifyState == HomeViewModel.AdapterNotifyState.NOTIFY_REMOVED) {
//                Log.e(TAG, "subscribeUI: NOTIFY_REMOVED ");
                homeViewModel.getSelectDeviceModelss().forEach(wiFiHistory -> {
                    if (!selectDeviceModels.contains(wiFiHistory)) {
                        int pos = homeViewModel.getSelectDeviceModelss().indexOf(wiFiHistory);
                        homeViewModel.setAddedRemovedPos(pos);
                    }
                });
                if (!homeViewModel.getSelectDeviceModelss().isEmpty()) {
                    if (homeViewModel.getAddedRemovedPos() != -1) {
                        homeViewModel.getSelectDeviceModelss().remove(homeViewModel.getAddedRemovedPos());
                        if (adapter != null) {
                            adapter.notifyItemRemoved(homeViewModel.getAddedRemovedPos());
                            homeViewModel.setAddedRemovedPos(-1);
                        }
                    }
                }


                homeViewModel.getDeviceHistoryModelss().forEach(wiFiHistory -> {
                    if (!selectDeviceModels.contains(wiFiHistory)) {
                        int pos = homeViewModel.getDeviceHistoryModelss().indexOf(wiFiHistory);
                        homeViewModel.setAddedRemovedPos(pos);
                    }
                });
                if (!homeViewModel.getDeviceHistoryModelss().isEmpty()) {
                    if (homeViewModel.getAddedRemovedPos() != -1) {
                        homeViewModel.getDeviceHistoryModelss().remove(homeViewModel.getAddedRemovedPos());
                        if (selectedDeviceHistoryAdpter != null) {
                            selectedDeviceHistoryAdpter.notifyItemRemoved(homeViewModel.getAddedRemovedPos());
                        }
                        homeViewModel.setAddedRemovedPos(-1);
                    }
                }
                homeViewModel.adapterNotifyState = HomeViewModel.AdapterNotifyState.NOTIFY_NONE;
            }
        });


        homeViewModel.isAddDevice.observe(lifecycleOwner, new EventObserver<>(hasAddDevice -> {
            if (!homeViewModel.isPermissionChecked()) {
                ((MainActivity) requireActivity()).checkPermissions();
            }
            ((MainActivity) requireActivity()).checkGpsOnOffState();
            ((MainActivity) requireActivity()).checkBluetoothIsSupported(true); // for this only ask permission on wifi discovery screen

            if (!hasShowProgressBar) {
                Log.e(TAG, "hasShowProgressBar: false");
                if (HomeViewModel.screenType == HOME) {
                    if (hasAddDevice) {
                        HomeViewModel.screenType = HomeViewModel.ScreenType.ADD_SCREEN;
                        binding.selectDeviceLayout.setVisibility(View.GONE);
                        binding.nearByDevicesHistoryLayout.setVisibility(VISIBLE);
                        if (activity.isInMultiWindowMode()) {
                            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
                        } else {
                            binding.selectedDeviceHistoryLayout.setVisibility(View.VISIBLE);
                        }
                        binding.infoTabLayout.setVisibility(View.GONE);
                        binding.galleryTabLayout.setVisibility(View.GONE);
                        binding.settingsTabLayout.setVisibility(View.GONE);
                        binding.firmwareUpdateLayout.setVisibility(View.GONE);
                        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
                        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
                        if (homeViewModel.isPermissionChecked()) startBleDiscovery(true);
                    }
                }
            }
        }));


        homeViewModel.isSelectDevice.observe(lifecycleOwner, new EventObserver<>(hasSelectDiscoverDevice -> {
            if (hasSelectDiscoverDevice) {
                showHomeScreen();
            }
        }));

        homeViewModel.isCancel.observe(lifecycleOwner, new EventObserver<>(hasCancel -> {
            if (hasCancel) {
                showHomeScreen();
            }
        }));

        homeViewModel.loadNearByDeviceConnections.observe(lifecycleOwner, nearByDeviceModels -> {
            if (nearByDeviceModels != null) {
                setNearByDeviceAdapter(nearByDeviceModels);
            }
        });


        /*scan bluetooth device discover*/
        if (bleWiFiViewModel.isScanning()) {
            binding.setIsLoading(true);
            binding.nearByDeviceViewRefreshIcon.setVisibility(View.GONE);
        } else {
            binding.setIsLoading(false);
            binding.nearByDeviceViewRefreshIcon.setVisibility(VISIBLE);
        }

        homeViewModel.isRefreshWifiConnection.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                bleWiFiViewModel.startBleDiscovery();
                startBleDiscovery(true);
            }
        }));

        /*Get Camera History*/
        getWiFiHistoryCallback();

        /*BLE ViewModel*/
        bleWiFiViewModel.getShouldStartToGetWifiHistory().observe(requireActivity(), aBoolean -> {
            if (aBoolean) {
                startBleDiscovery(true);
                bleWiFiViewModel.setShouldStartToGetWifiHistory(false);
            }
        });

        bleWiFiViewModel.getBleScanResult().observe(requireActivity(), observer);

        bleWiFiViewModel.getShouldStartBleDiscovery().observe(requireActivity(), aBoolean -> {
            if (aBoolean) {
                bleWiFiViewModel.startBleDiscovery();
//                bleWiFiViewModel.getPairedDevices();
                startBleDiscovery(false);
            }
        });
        bleWiFiViewModel.getIsBleScanning().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(aBoolean -> {
            if (!aBoolean) {
                if (nearByDeviceAdapter != null) {
                    ArrayList<WiFiHistory> original = new ArrayList<>(homeViewModel.getBleResults());
                    homeViewModel.getBleResults().forEach(wiFiHistory -> {
                        try {
                            String camera_ssid = wiFiHistory.getCamera_ssid();
                            WiFiHistory wiFiHistorySingle = bleWiFiViewModel.checkSsidDiscoverable(camera_ssid).blockingGet();
                            if (wiFiHistorySingle != null) {
                                Log.e(TAG, "subscribeUI: " + wiFiHistorySingle.getCamera_ssid() + " " + wiFiHistorySingle.getIs_wifi_connected());
                                if (wiFiHistorySingle.getIs_wifi_connected() == -1) {
                                    original.remove(wiFiHistory);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    homeViewModel.getBleResults().clear();
                    homeViewModel.getBleResults().addAll(original);
                    nearByDeviceAdapter.setValue(homeViewModel.getBleResults());
                }
            }
        }));
        homeViewModel.getInsertOrUpdateSsid().observe(requireActivity(), selectedSsid -> {
            Log.e(TAG, "subscribeUI: getInsertOrUpdateSsid() " + selectedSsid);
            if (!selectedSsid.isEmpty()) {
                insertOrUpdate(selectedSsid);
                homeViewModel.setInsertOrUpdateSsid("");
            } else {
                hideProgressBar();
                bleWiFiViewModel.updateCameraConnectionState(selectedSsid,WIFI_NOT_CONNECTED);
            }
        });

        /* info view layout */
        homeViewModel.isInfoViewCancel.observe(lifecycleOwner, new EventObserver<>(hasCancel -> {
            if (hasCancel) {
                hideProgressBar();
                HomeViewModel.screenType = HOME;
                showHomeScreen();
                homeViewModel.setSelectedCamera("");
                homeViewModel.setBottomLayoutType(INFO_LAYOUT_BACK_STACKING);
                homeViewModel.setInfoSelected(false);
                homeViewModel.setGallerySelected(false);
                homeViewModel.setGalleryManageSelected(false);
                homeViewModel.setGalleryVideoPlayerSelected(false);
                homeViewModel.setGalleryVideoRecordedInfoSelected(false);
                homeViewModel.setSettingsSelected(false);
                binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
                binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
                binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);

                /* for this pop up (info / settings) screen view to click app info icon show info view and then press back if tcp already connected go to live view so, to avoid this case */
                if (tcpConnectionViewModel != null) {
                    Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                    if (value != null && value == Constants.STATE_CONNECTED) {
                        disConnectCamera();
                    }
                }
            }
        }));

        // popup open time zone list view
        homeViewModel.isOpenTimeZoneListView.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                setShowTimeZoneLayout(true);
                fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
                fragmentPopUpLayoutBinding.popUpTimeZoneListLayout.popUpTimeZoneList.setVisibility(VISIBLE);
                showTimeZoneListViewLayout();
            }
        }));
        // cancel icon for time zone cancel
        fragmentPopUpLayoutBinding.popUpTimeZoneListLayout.timeZoneCancelIcon.setOnClickListener(v -> {
            setShowTimeZoneLayout(false);
            setSelectedTimeZone(false);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(VISIBLE);
            fragmentPopUpLayoutBinding.popUpTimeZoneListLayout.popUpTimeZoneList.setVisibility(View.GONE);
            setCameraSettingAdapter(); // for this refresh layout
        });

        // selected time zone to set text view
        timeZoneViewModel.isSelectTimeZone.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(timeZoneValue -> {
            if (timeZoneValue != -1) {
                setSelectedTimeZone(true);
                fragmentPopUpLayoutBinding.popUpTimeZoneListLayout.popUpTimeZoneList.setVisibility(View.GONE);
                fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(VISIBLE);
                fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setVisibility(VISIBLE);
                fragmentPopUpLayoutBinding.popUpSettingsViewPager.setVisibility(VISIBLE);
                setCameraSettingAdapter(); // for this refresh layout
                homeViewModel.hasSetAndClosedTimeZoneListLayout(timeZoneValue);
                setShowTimeZoneLayout(false);
            }
        }));

        //addition : /* gallery view layout cancel button */>>>>>>>>>>>>>>>>>>>>>>>>
        homeViewModel.isGalleryViewCancel.observe(lifecycleOwner, new EventObserver<>(hasCancel -> {
            if (hasCancel) {
                homeViewModel.setSelectedCamera("");
                hideProgressBar();
                handleCloseFromGalleryInfoScreen();
                if(homeViewModel.playVideo) {
                    homeViewModel.stopState = true;
                    homeViewModel.playVideo = false;
                }
            }
        }));

        //addition: /* setting view layout cancel button*/
        homeViewModel.isSettingsViewCancel.observe(lifecycleOwner, new EventObserver<>(hasCancel -> {
            if (hasCancel) {
                showHomeScreen();
                homeViewModel.setSelectedCamera("");
                homeViewModel.setGallerySelected(false);
                homeViewModel.setGalleryManageSelected(false);
                homeViewModel.setGalleryVideoPlayerSelected(false);
                homeViewModel.setGalleryVideoRecordedInfoSelected(false);
                homeViewModel.setInfoSelected(false);
                homeViewModel.setSettingsSelected(false);
                binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
                binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
                binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);

                /* for this pop up (info / settings) screen view to click app info icon show info view and then press back if tcp already connected go to live view so, to avoid this case */
                if (tcpConnectionViewModel != null) {
                    Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                    if (value != null && value == Constants.STATE_CONNECTED) {
                        disConnectCamera();
                    }
                }
            }
        }));

        /*info tablayout*/
        binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_tab_info));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.contact)));
        binding.tabLayout.setTabGravity(GRAVITY_FILL);

        InfoViewTabLayoutAdapter infoViewTabLayoutAdapter = new InfoViewTabLayoutAdapter(this, binding.tabLayout.getTabCount());
        binding.viewPager.setAdapter(infoViewTabLayoutAdapter);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
        }).attach();
        binding.tabLayout.getTabAt(0).setIcon(R.drawable.ic_tab_info);
        binding.tabLayout.getTabAt(1).setText(getString(R.string.contact));

        //addition: /* gallery tablayout */
        if (Objects.requireNonNull(getActivity()).getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.gallerytabLayout.addTab(binding.gallerytabLayout.newTab().setText(getString(R.string.all)));
            binding.gallerytabLayout.addTab(binding.gallerytabLayout.newTab().setIcon(R.drawable.ic_gallery));
            binding.gallerytabLayout.addTab(binding.gallerytabLayout.newTab().setIcon(R.drawable.ic_video_icon));
        } else {
            binding.gallerytabLayout.addTab(binding.gallerytabLayout.newTab().setText(getString(R.string.all)));
            binding.gallerytabLayout.addTab(binding.gallerytabLayout.newTab().setText(getString(R.string.photos)));
            binding.gallerytabLayout.addTab(binding.gallerytabLayout.newTab().setText(getString(R.string.videos)));
        }


        //manage button
        homeViewModel.isSelectManage.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                homeViewModel.setGallerySelectAll(false);
                homeViewModel.setCurrentManageTabPosition(binding.galleryViewPager.getCurrentItem());
                HomeViewModel.selectedtab = "MANAGE";
                homeViewModel.setGalleryManageSelected(true);

                homeViewModel.setIsMediaVideoDeleted(false);
                homeViewModel.isErasedAll = false;
                homeViewModel.isErasedPic = false;
                homeViewModel.isErasedVideo = false;

                homeViewModel.setBottomLayoutType(MANAGE_LAYOUT);
            }
        }));

        //manage layout: back button
        homeViewModel.isManageSelectBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                homeViewModel.setCurrentManageTabPosition(binding.galleryViewPager.getCurrentItem());
                homeViewModel.setGalleryManageSelected(false);
                resetCheckBoxState();
                homeViewModel.setBottomLayoutType(GALLERY_LAYOUT);
            }
        }));

        //back to gallery after download and erase is clicked
        homeViewModel.isBackToGallery.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                homeViewModel.setGalleryManageSelected(false);
//                resetCheckBoxState();
                homeViewModel.setBottomLayoutType(GALLERY_LAYOUT);
                binding.galleryManageSelectAllButton.setText(getString(R.string.select_all));
                binding.galleryViewPager.setCurrentItem(homeViewModel.getCurrentManageTabPosition());
            }
        }));

        //recording Info layout: delete button
        homeViewModel.isSelectRecordingInfoDelete.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                showConfirmEraseGalleryItemDialog(getString(R.string.confirm_erase));
            }
        }));

        homeViewModel.isDeleteRecordedVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.i(TAG, "subscribeUI:FILEPATH > \" + videoFilePath ");

                if (!videoFilePath.isEmpty()) {
                    File file = new File(videoFilePath);
                    if (file.exists() && file.isFile() && file.canWrite()) {
                        file.delete();
                        galleryFilePath = null;
                        Toast.makeText(activity, getString(R.string.erased), Toast.LENGTH_SHORT).show();
                      /*  if (videoFilePath.endsWith(".mp4")) {
                            Toast.makeText(activity, R.string.delete_videos, Toast.LENGTH_SHORT).show();
                        }
                        if (videoFilePath.endsWith(".jpg")) {
                            Toast.makeText(activity, R.string.delete_image, Toast.LENGTH_SHORT).show();
                        }*/

                    }
                }
                if (homeViewModel != null) {
                    homeViewModel.noRecordsFoundAll.removeObserver(isNoRecordAll);
                    homeViewModel.noRecordsFoundAll.removeObservers(lifecycleOwner);

                    homeViewModel.noRecordsFoundImage.removeObserver(isNoRecordImage);
                    homeViewModel.noRecordsFoundImage.removeObservers(lifecycleOwner);

                    homeViewModel.noRecordsFoundVideo.removeObserver(isNoRecordVideo);
                    homeViewModel.noRecordsFoundVideo.removeObservers(lifecycleOwner);
                }
                assert homeViewModel != null;
                if (homeViewModel.getArrayListPhoto() != null) {
                    homeViewModel.getArrayListPhoto().clear();
                }
                if (homeViewModel.getArrayListAll() != null) {
                    homeViewModel.getArrayListAll().clear();
                }
                if (homeViewModel.getArrayListVideo() != null) {
                    homeViewModel.getArrayListVideo().clear();
                }
                homeViewModel.getAllFiles(getActivity());
                screenType = GALLERY_SCREEN;
                homeViewModel.setBottomLayoutType(GALLERY_LAYOUT);
                resetCheckBoxState();
                homeViewModel.updateGalleryBottomItemView(true);
                binding.galleryManageOptionButtons.setVisibility(View.GONE);
                recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                binding.galleryTabLayout.setVisibility(VISIBLE);
                binding.galleryManageButton.setVisibility(VISIBLE);
                binding.galleryShareButton.setVisibility(View.GONE);
            }
        }));


        //manage layout: erase button
        homeViewModel.isManageSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryViewPager.getCurrentItem() == 0) {
                    homeViewModel.onManageAllSelectErase(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 1) {
                    homeViewModel.onManagePhotoSelectErase(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 2) {
                    homeViewModel.onManageVideoSelectErase(true);
                }
                homeViewModel.updateGalleryBottomItemView(true);
            }
        }));

        homeViewModel.isGallerySelectAllMedia.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.galleryManageSelectAllButton.setText(getText(R.string.unselect_all));
            } else {
                binding.galleryManageSelectAllButton.setText(getText(R.string.select_all));
            }
        }));

        //manage layout: select all
        homeViewModel.isManageSelectSelectAll.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (binding.galleryManageSelectAllButton.getText().toString().contains(getString(R.string.select_all))) {
                    if (binding.galleryViewPager.getCurrentItem() == 0) {
                        homeViewModel.onManageSelectAllView(true);

                    } else if (binding.galleryViewPager.getCurrentItem() == 1) {
                        homeViewModel.onManageSelectAllPhotos(true);

                    } else if (binding.galleryViewPager.getCurrentItem() == 2) {
                        homeViewModel.onManageSelectAllVideo(true);
                    }
                    homeViewModel.setGallerySelectAll(true);

                } else if (binding.galleryManageSelectAllButton.getText().toString().contains(getString(R.string.unselect_all))) {
                    if (binding.galleryViewPager.getCurrentItem() == 0) {
                        homeViewModel.onManageAllSelectBack(true);
                        homeViewModel.setGallerySelectAll(false);
                    } else if (binding.galleryViewPager.getCurrentItem() == 1) {
                        homeViewModel.onManagePhotoSelectBack(true);
                    } else if (binding.galleryViewPager.getCurrentItem() == 2) {
                        homeViewModel.onManageVideoSelectBack(true);
                    }
                    homeViewModel.setGallerySelectAll(false);
                }
            }

            homeViewModel.updateGalleryBottomItemView(false);
        }));
        homeViewModel._isUpdateGalleryItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryViewPager.getCurrentItem() == 0) {
                    homeViewModel.isUpdateGalleryBottomAllItemView(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 1) {
                    homeViewModel.updateGalleryBottomPhotosItemView(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 2) {
                    homeViewModel.updateGalleryBottomVideosItemView(true);
                }
                homeViewModel.updateGalleryBottomItemView(false);
            }
        }));

        //manage layout: download button
        homeViewModel.isManageSelectDownload.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (binding.galleryViewPager.getCurrentItem() == 0) {
                    homeViewModel.onManageSelectAllDownload(true);
                    if (homeViewModel.isDownloaded) homeViewModel.setBackToGallery(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 1) {
                    homeViewModel.onManageSelectAllPhotoDownload(true);
                    if (homeViewModel.isPhotoDownload) homeViewModel.setBackToGallery(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 2) {
                    homeViewModel.onManageSelectAllVideoDownload(true);
                    if (homeViewModel.isVideoDownload) homeViewModel.setBackToGallery(true);
                }
                homeViewModel.updateGalleryBottomItemView(false);
            }
        }));

        //manage layout: clear checkBox state handling
        homeViewModel.clearSelectedCheckBox.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryViewPager.getCurrentItem() == 0) {
                    homeViewModel.onClearSelectedCheckboxAllView(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 1) {
                    homeViewModel.onClearSelectedCheckboxPhotoView(true);
                } else if (binding.galleryViewPager.getCurrentItem() == 2) {
                    homeViewModel.onClearSelectedCheckboxVideoView(true);
                }
                homeViewModel.updateGalleryBottomItemView(false);
            }
        }));

        //gallery layout: item selecting video info with delete option
        homeViewModel.isSelectGalleryBottomItemView.observe(lifecycleOwner, new EventObserver<>(selectedBottomVideoModel -> {
            homeViewModel.setGalleryVideoRecordedInfoSelected(true);
            homeViewModel.setCurrentManageTabPosition(binding.galleryViewPager.getCurrentItem());
            if (selectedBottomVideoModel.filePath != null) {
                homeViewModel.setBottomLayoutType(RECORDING_VIDEO_INFO_LAYOUT);
                galleryFilePath = selectedBottomVideoModel.filePath;
                selectedItemPosition = selectedBottomVideoModel.getItemPosition();
                if (galleryFilePath.endsWith(".mp4")) {
                    showRecordingVideoInfolayout(selectedItemPosition);
                } else {
                    showCapturedImageInfo(selectedItemPosition);
                }
            }
        }));

        //recording info layout: play button
        homeViewModel.isSelectRecordingInfoPlay.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                homeViewModel.setGalleryVideoPlayerSelected(true);
                homeViewModel.setBottomLayoutType(RECORDING_VIDEO_PLAYER_LAYOUT);
                showRecordedVideoPlayerLayout(selectedItemPosition);
            }
        }));


        //image info layout:
        homeViewModel.isSelectImageInfo.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (galleryFilePath.endsWith(".jpg")) {
                    Log.i(TAG, "subscribeUI: SELECT IMAGE POS " + homeViewModel.filterImageList.indexOf(homeViewModel.isSelectedFilePath));
                    homeViewModel.isSelectedItemPosition = homeViewModel.filterImageList.indexOf(homeViewModel.isSelectedFilePath);
                    homeViewModel.setGalleryVideoPlayerSelected(true);
                    homeViewModel.setBottomLayoutType(RECORDING_VIDEO_PLAYER_LAYOUT);
                    showCapturedImageLayout(homeViewModel.isSelectedItemPosition);
                }
            }
        }));


        //recorded video player layout: videoview
        recordingInfoScreenBinding.recordingVideoPlayerVideoView.setOnPreparedListener(mp -> {
            mediaPlayer = mp;
            homeViewModel.mCurrentState = true;
            if (homeViewModel.playVideo)
                mHandler.postDelayed(mUpdateTimeTask, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp.seekTo(homeViewModel.pos, MediaPlayer.SEEK_CLOSEST);
            } else {
                mp.seekTo(homeViewModel.pos);
            }
            Log.d(TAG, "onPrepared: " + homeViewModel.pos);
            if (homeViewModel.pos > 0) {
                if (homeViewModel.playVideo) {
                    mp.start();
                    recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.INVISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_filled);
                    recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                    recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                } else {

                    if (homeViewModel.isSeekbarSeeked) {
                        homeViewModel.pauseState = true;
                        recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_filled);
                    }
                    recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);

                }
                if (homeViewModel.isSeekbarSeeked) {
                    recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.INVISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_filled);
                    recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                    recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                }

            } else {
                recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setMin(seekState);
            }

            recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setMin(0);
            recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setMax(recordingInfoScreenBinding.recordingVideoPlayerVideoView.getDuration());

            if (homeViewModel.pauseState) {
                recordingInfoScreenBinding.recordingVideoPlayerVideoView.pause();
                recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
                recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
                recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_filled);
            }

            if (homeViewModel.stopState) {
                recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
                recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
                recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_filled);
            }
        });

        recordingInfoScreenBinding.recordingVideoPlayerVideoView.setOnErrorListener((mp, what, extra) -> {
            Log.d(TAG, "onError: Video ");
            return true;
        });


        //recorded video player layout: on compeletion- video state handling
        recordingInfoScreenBinding.recordingVideoPlayerVideoView.setOnCompletionListener(mp -> {
            int totalDuration = recordingInfoScreenBinding.recordingVideoPlayerVideoView.getDuration();
            String totalDurationString = getDurationString(totalDuration);
            recordingInfoScreenBinding.recordingVideoPlayerVideoTotalDuration.setText(totalDurationString);
            recordingInfoScreenBinding.recordingVideoPlayerVideoCurrentDuration.setText(intialState);
            recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setProgress(0);
            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
            recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
            recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
            if (mp != null) {
                mp.seekTo(0, MediaPlayer.SEEK_CLOSEST);
            }
        });

        //recording info layout: back button
        homeViewModel.isSelectRecordingInfoBack.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                homeViewModel.isDeleteRecording = false;
                homeViewModel.setCurrentManageTabPosition(homeViewModel.getCurrentManageTabPosition());
                binding.galleryTabLayout.setVisibility(VISIBLE);
                binding.galleryManageButton.setVisibility(VISIBLE);
                binding.galleryShareButton.setVisibility(View.GONE);
                binding.galleryManageOptionButtons.setVisibility(View.GONE);
                recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                homeViewModel.setBottomLayoutType(GALLERY_LAYOUT);
            }
        }));

        //recorded video player layout: video view's centre play button
        homeViewModel.isSelectRecordingInfoVideoPlayButton.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                homeViewModel.playVideo = true;
                homeViewModel.pauseState = false;
                homeViewModel.stopState = false;
                homeViewModel.isSeekbarSeeked = false;
                mHandler.postDelayed(mUpdateTimeTask, 100);
                recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
                recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.INVISIBLE);
                if (!recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying()) {
                    if (recordingInfoScreenBinding.recordingVideoPlayerVideoView.getDuration() == StopPlayBack) {
                        recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVideoPath(galleryFilePath);
                    }
                    recordingInfoScreenBinding.recordingVideoPlayerVideoView.start();
                    recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_filled);
                    recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                    recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                }
            }
        }));

        //recorded video player layout: pause button
        homeViewModel.isSelectRecordingInfoVideoPause.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.d(TAG, "subscribeUI:1111 " + seekState);
                homeViewModel.isSeekbarSeeked = false;
                if (recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying() || seekState > 0 || recordingInfoScreenBinding.recordingVideoPlayerVideoView.canPause()) {
                    homeViewModel.pauseState = true;
                    homeViewModel.stopState = false;
                    recordingInfoScreenBinding.recordingVideoPlayerVideoView.pause();
                    recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_filled);
                    recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
                    recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                }
            }
        }));

        //recorded video player layout: play button
        homeViewModel.isSelectRecordingInfoVideoPlay.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (recordingInfoScreenBinding.recordingVideoPlayerHeading.getText().equals("Image Info")) {
                    // video start button changed to left arrow for gallery swipe in left way
                    Log.e(TAG, "subscribeUI: load pre image");
                    if (homeViewModel.filterImageList != null) {

                        if (homeViewModel.isSelectedItemPosition > 0) { //3 > 0
                            homeViewModel.isSelectedItemPosition--;//2
                            nextImage(homeViewModel.isSelectedItemPosition);
                        }

                        selectedItemPosition = homeViewModel.isSelectedItemPosition;
                        Log.e(TAG, "subscribeUI: ITEM POS " + selectedItemPosition);
                    }
                } else {
                   /* if (mediaPlayer != null) {
                        mediaPlayer.seekTo(0, MediaPlayer.SEEK_CLOSEST);
                        mediaPlayer.seekTo(0);
                    }*/
                    homeViewModel.isSeekbarSeeked = false;
                    homeViewModel.playVideo = true;
                    homeViewModel.pauseState = false;
                    homeViewModel.stopState = false;
                    mHandler.postDelayed(mUpdateTimeTask, 100);
                    recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.INVISIBLE);
                    if (!recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying()) {
                        if (recordingInfoScreenBinding.recordingVideoPlayerVideoView.getDuration() == StopPlayBack) {
                            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVideoPath(galleryFilePath);
                        } else {
                            recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.INVISIBLE);
                        }
                        recordingInfoScreenBinding.recordingVideoPlayerVideoView.start();
                        recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_filled);
                        recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                        recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                    }
                }
            }
        }));


        //recorded video player layout: stop button
        homeViewModel.isSelectRecordingInfoVideoStop.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (recordingInfoScreenBinding.recordingVideoPlayerHeading.getText().equals("Image Info")) {
                    // video stop button changed to right arrow for gallery swipe in right way
                    Log.e(TAG, "subscribeUI: load next image");
                    if (homeViewModel.filterImageList != null) {

                        int endIndex = homeViewModel.filterImageList.size() - 1;
                        if (homeViewModel.isSelectedItemPosition < endIndex) {
                            homeViewModel.isSelectedItemPosition++;
                            nextImage(homeViewModel.isSelectedItemPosition);
                        }
                        selectedItemPosition = homeViewModel.isSelectedItemPosition;

                    }
                } else {
                    if (recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying() || recordingInfoScreenBinding.recordingVideoPlayerVideoView.canPause()) {
                        homeViewModel.isSeekbarSeeked = false;
                        recordingInfoScreenBinding.recordingVideoPlayerVideoView.seekTo(0);
                        recordingInfoScreenBinding.recordingVideoPlayerVideoView.pause();
                        homeViewModel.stopState = true;
                        recordingInfoScreenBinding.recordingVideoPlayerVideoCurrentDuration.setText(intialState);
                        recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setProgress(0);
                        mHandler.removeCallbacks(mUpdateTimeTask);
                        recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_filled);
                        recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                        recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
                        recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
                    }
                }
            }
        }));
        /*
        for this observe dismiss video mode change confirmation dialog while bg to fg
         */
        popUpCameraSettingsViewModel.observeHasDismissConfirmationDialog().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(aBoolean -> {
            Log.d(TAG, "observeHasDismissConfirmationDialog: " + aBoolean + " act " + screenType);
            if (aBoolean) {
                try {
                    if (screenType == POP_UP_SETTINGS_SCREEN) {
                        new Handler().postDelayed(() -> {
                            try {
                                DialogFragment dialogFragment = new NoticeDialogFragment();
                                if (dialogFragment.getShowsDialog()) {
                                    Log.e(TAG, "run: observeHasDismissConfirmationDialog");
                                    dialogFragment.dismiss();
                                } else {
                                    Log.e(TAG, "run: observeHasDismissConfirmationDialog else");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // hideShowDialog();
                            // popUpCameraSettingsViewModel.hasShowVideoModeWifiDialog(false); // simply dialog close and revert button to USB mode
                            // popUpCameraSettingsViewModel.hasShowVideoModeUSBDialog(false);//dialog show and select yes switch from Wifi to USB mode
                        }, 1000);

                    } else {
                        Log.e(TAG, "observeHasDismissConfirmationDialog: else");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                // popUpCameraSettingsViewModel.setDismissConfirmationDialog(false);
            }
        }));

//        homeViewModel.noRecordsFoundAll.observe(lifecycleOwner, isNoRecordAll);
//        homeViewModel.noRecordsFoundImage.observe(lifecycleOwner, isNoRecordImage);
//        homeViewModel.noRecordsFoundVideo.observe(lifecycleOwner, isNoRecordVideo);

        ////recorded video player layout: back button
        homeViewModel.isSelectRecordingInfoVideoBack.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {

                if (recordingInfoScreenBinding.recordingVideoPlayerHeading.getText().equals("Image Info")) {
//                    binding.galleryTabLayout.setVisibility(View.VISIBLE);
//                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
//                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    homeViewModel.setBottomLayoutType(GALLERY_LAYOUT);
                } else {
                    homeViewModel.pauseState = false;
                    homeViewModel.stopState = false;
                    recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setProgress(seekState);
                    if (galleryFilePath.endsWith(".mp4")) {
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(0, MediaPlayer.SEEK_CLOSEST);
                            mediaPlayer.seekTo(0);
                        }
                    }
                    homeViewModel.setBottomLayoutType(RECORDING_VIDEO_INFO_LAYOUT);
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(VISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    if (recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying()) {
                        recordingInfoScreenBinding.recordingVideoPlayerVideoView.stopPlayback();
                    }
                    recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                    recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
                    recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
                    mHandler.removeCallbacks(mUpdateTimeTask);
                }
            }
        }));

        //recorded video player layout: seekbar state handling
        recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (b) {
                    recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(progress, MediaPlayer.SEEK_CLOSEST);
                    }

                    seekState = progress;

                    Log.d(TAG, "onProgressChanged: " + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                homeViewModel.isSeekbarSeeked = true;
                if (!homeViewModel.stopState) {
                    homeViewModel.stopState = false;
                }
                if (!homeViewModel.playVideo) {
                    homeViewModel.playVideo = false;
                }
                if (!homeViewModel.pauseState) {
                    homeViewModel.pauseState = false;
                }
//                homeViewModel.playVideo = false;
                // pauseState = false;
                //     stopState = false;


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                homeViewModel.isSeekbarSeeked = true;
               /* if (!stopState) {
                    stopState = false;
                }*/
                if (!homeViewModel.stopState) {
                    homeViewModel.stopState = false;
                }
                if (!homeViewModel.playVideo) {
                    homeViewModel.playVideo = false;
                }
                if (!homeViewModel.pauseState) {
                    homeViewModel.pauseState = false;
                }
//                homeViewModel.playVideo = false;
//                pauseState = false;
                // stopState = false;

            }
        });

        //handling orientation for each layout using this observer
        homeViewModel.getBottomLayoutType().observe(lifecycleOwner, integer -> {
            switch (integer) {
                case GALLERY_LAYOUT:

                    homeViewModel.noRecordsFoundAll.observe(lifecycleOwner, isNoRecordAll);
                    homeViewModel.noRecordsFoundImage.observe(lifecycleOwner, isNoRecordImage);
                    homeViewModel.noRecordsFoundVideo.observe(lifecycleOwner, isNoRecordVideo);
                    // The getarraylistall not updated once erased the file in live view
                    if (homeViewModel.getArrayListAll() != null) {
                        homeViewModel.getArrayListAll().clear();
                        homeViewModel.getAllFiles(getActivity());
                    }
                    binding.galleryTabLayout.setVisibility(VISIBLE);
                    binding.galleryManageButton.setVisibility(VISIBLE);
                    binding.galleryShareButton.setVisibility(View.GONE);
                    binding.galleryManageOptionButtons.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        ConstraintSet constraintSet = new ConstraintSet();
                        ConstraintLayout constraintLayout = binding.galleryTabLayout;
                        constraintSet.clone(constraintLayout);
                        constraintSet.connect(binding.galleryViewPager.getId(), ConstraintSet.TOP, binding.galleryManageButtonHorizontalBottomGuideline.getId(), ConstraintSet.TOP, 30);
                        constraintSet.connect(binding.galleryViewPager.getId(), ConstraintSet.BOTTOM, binding.gallerytabLayout.getId(), ConstraintSet.TOP, 30);
                        constraintSet.setMargin(binding.galleryViewPager.getId(), ConstraintSet.BOTTOM, 28);
                        constraintSet.applyTo(constraintLayout);
                    }
                    homeViewModel.setGallerySelected(true);
                    homeViewModel.setGalleryManageSelected(false);
                    galleryAdapterView(false);
                    setShowTimeZoneLayout(false);
                    setSelectedTimeZone(false);
                    break;
                case MANAGE_LAYOUT:
                    if (homeViewModel.isGalleryManageSelected()) {
                        boolean all = homeViewModel.noRecordsFoundAll.hasObservers();
                        if (!all) {
                            homeViewModel.noRecordsFoundAll.observe(lifecycleOwner, isNoRecordAll);
                            homeViewModel.noRecordsFoundImage.observe(lifecycleOwner, isNoRecordImage);
                            homeViewModel.noRecordsFoundVideo.observe(lifecycleOwner, isNoRecordVideo);
                        }
                        binding.galleryTabLayout.setVisibility(VISIBLE);
                        binding.galleryManageOptionButtons.setVisibility(VISIBLE);
                        binding.galleryManageButton.setVisibility(View.GONE);
                        binding.galleryShareButton.setVisibility(View.GONE);
                        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                        homeViewModel.setGalleryManageSelected(true);
                        HomeViewModel.screenType = HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN;
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            ConstraintSet constraintSet = new ConstraintSet();
                            ConstraintLayout constraintLayout = binding.galleryTabLayout;
                            constraintSet.clone(constraintLayout);
                            constraintSet.connect(binding.galleryViewPager.getId(), ConstraintSet.TOP, binding.galleryCancelIcon.getId(), ConstraintSet.BOTTOM, 0);
                            constraintSet.connect(binding.galleryViewPager.getId(), ConstraintSet.BOTTOM, binding.galleryManageBtnsGuidelineTop.getId(), ConstraintSet.TOP, 30);
                            constraintSet.setMargin(binding.galleryViewPager.getId(), ConstraintSet.BOTTOM, 10);
                            constraintSet.applyTo(constraintLayout);
                        }
                        galleryAdapterView(true);
                    } else {
                        binding.galleryManageOptionButtons.setVisibility(View.GONE);
                        homeViewModel.setGalleryManageSelected(false);
                    }
                    break;
                case RECORDING_VIDEO_INFO_LAYOUT:
                    homeViewModel.isDeleteRecording = true;
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    binding.galleryManageOptionButtons.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(VISIBLE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    HomeViewModel.screenType = HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN;
                    if (galleryFilePath != null && galleryFilePath.endsWith(".mp4")) {
                        showRecordingVideoInfolayout(selectedItemPosition);
                    } else {
                        showCapturedImageInfo(homeViewModel.isSelectedItemPosition);
                    }
                    break;
                case RECORDING_VIDEO_PLAYER_LAYOUT:
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(VISIBLE);
                    HomeViewModel.screenType = HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN;
                    if (galleryFilePath != null && galleryFilePath.endsWith(".mp4")) {
                        showRecordedVideoPlayerLayout(selectedItemPosition);
                    } else {
                        showCapturedImageLayout(homeViewModel.isSelectedItemPosition);
                    }

                    break;
                case GALLERY_CANCEL:
                    homeViewModel.isDeleteRecording = false;
                    homeViewModel.pos = 0;
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    if (screenType == GALLERY_SCREEN) {
                        binding.selectDeviceLayout.setVisibility(VISIBLE);
                    }
                    homeViewModel.setGalleryManageSelected(false);
                    break;
                case BACK_STACK_GALLERY_SCREEN:
                    showHomeScreen();
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    binding.selectDeviceLayout.setVisibility(VISIBLE);
                    binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
                    homeViewModel.setGallerySelected(false);
                    break;
                case BACK_STACK_MANAGE_SCREEN:
                    backToGallery();
                    break;
                case BACK_STACK_RECORDED_VIDEO_INFO_SCREEN:
                    homeViewModel.isDeleteRecording = false;
                    backToGallery();
                    break;
                case BACK_STACK_RECORDED_VIDEO_PLAYER_SCREEN:
                    if (galleryFilePath != null && galleryFilePath.endsWith(".mp4")) {
                        showRecordedVideoInfoLayout();
                    } else {
                        backToGallery();
                    }
                    break;
                case INFO_LAYOUT:
                    showInfoLayout();
                    break;
                case INFO_LAYOUT_BACK_STACKING:
                    if (screenType == INFO_SCREEN) {
                        isSelectPopUpInfo = false;
                        isSelectPopUpSettings = false;
                        homeViewModel.setPressCancelOrBackPopUpWindow(true);
                        cancelInfoLayout();
                    }
                    break;
                case SETTINGS_LAYOUT:
                    showSettingsLayout();
                    break;
                case SETTINGS_LAYOUT_BACK_STACKING:
                    isSelectPopUpInfo = false;
                    isSelectPopUpSettings = false;
                    cancelSettingsLayout();
                    homeViewModel.setPressCancelOrBackPopUpWindow(true);
                    break;
            }
        });

        //bottom setting tablayout
        binding.settingstabLayout.addTab(binding.settingstabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
        binding.settingstabLayout.setTabGravity(GRAVITY_FILL);
        //new TabLayoutMediator(binding.settingstabLayout, binding.settingsViewPager,(tab, position) -> {}).attach();
        // binding.settingsViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.settingstabLayout));
       /* binding.settingstabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.settingsViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });*/


        /* popUp view*/
/*
        homeViewModel.isSelectPopUpInfo.observe(lifecycleOwner, new EventObserver<>(hasSelectDiscoverDevice -> {
            if (hasSelectDiscoverDevice) {
                isSelectPopUpInfo = true;
                isSelectPopUpSettings = false;
                isSelectPopUpFwUpdateCheck = false;
                bleWiFiViewModel.setShouldAutoConnectionEnabled(true);
                fragmentPopUpLayoutBinding.popUpViewPager.setAdapter(null);
                homeViewModel.setGallerySelected(true);
                popUpCameraInfoAdapter = null;
            }
        }));

        homeViewModel.isSelectPopUpSettings.observe(lifecycleOwner, hasSelectDiscoverDevice -> {
            if (hasSelectDiscoverDevice) {
                isSelectPopUpSettings = true;
                isSelectPopUpInfo = false;
                isSelectPopUpFwUpdateCheck = false;
                bleWiFiViewModel.setShouldAutoConnectionEnabled(true);
                fragmentPopUpLayoutBinding.popUpSettingsViewPager.setAdapter(null);
                homeViewModel.popUpSettingsAdapter = null;
                //  navigateToNextScreen(popUpSettingsScreen);
            }
        });

        homeViewModel.isSelectPopUpFwUpdateCheck.observe(lifecycleOwner,new EventObserver<>(hasSelectDiscoverDevice -> {
            if (hasSelectDiscoverDevice) {
                isSelectPopUpSettings = false;
                isSelectPopUpInfo = false;
                isSelectPopUpFwUpdateCheck = true;
                bleWiFiViewModel.setShouldAutoConnectionEnabled(true);
            }
        }));
*/

        homeViewModel.isSelectPopUpDelete.observe(lifecycleOwner, new EventObserver<>(hasCancel -> {
            if (hasCancel) {
                if (isSDK10()){
                    if (activity != null) {
                        String message = getString(R.string.delete_camera_popup_message);
                        activity.showDialog = MainActivity.ShowDialog.CAMERA_CLOSURE_DIALOG;
                        activity.showDialog("", message, null);
                    }
                }else{
                    showDeleteCameraItemDialog(getString(R.string.pop_up_delete_message));
                }
            }
        }));

        homeViewModel.isPopUpInfoViewCancel.observe(lifecycleOwner, new EventObserver<>(hasCancel -> {
            if (hasCancel) {
                homeViewModel.setSelectedCamera("");
                popUpCameraInfoAdapter = null;
                homeViewModel.popUpSettingsAdapter = null;
                fragmentPopUpLayoutBinding.popUpViewPager.setAdapter(null);
                fragmentPopUpLayoutBinding.popUpSettingsViewPager.setAdapter(null);
                // for this reset rtc mode state pop up view closed usecase
                popUpCameraSettingsViewModel.rtcMode = PopUpCameraSettingsViewModel.RTC_MODE.MANUAL;
                popUpCameraSettingsViewModel.setDateTimeModePosition(0);

                homeViewModel.hasSelectPopUpSettings(false);
                homeViewModel.hasSelectPopUpInfo(false);
                homeViewModel.closePopupSettingFragment(true);
                homeViewModel.closePopupCameraAdjusmentFragment(true);
                homeViewModel.closePopupCameraInfoFragment(true);
                disConnectCamera();
                hideProgressBar();
                showHomeScreen();
                isSelectPopUpInfo = false;
                isSelectPopUpSettings = false;
                binding.infoImageView.setClickable(true);
                binding.galleryImageView.setClickable(true);
                homeViewModel.setPressCancelOrBackPopUpWindow(true);
                homeViewModel.getNavController().popBackStack(R.id.popUpSettingsFragment, true);
                HomeViewModel.screenType = HOME;
                /* for this after close dialog remove observer*/
                if (CameraViewModel.hasNewFirmware()) {
                    tcpConnectionViewModel.observeCameraMode().removeObserver(observeCameraMode);
                    tcpConnectionViewModel.observeCameraMode().removeObservers(this.getViewLifecycleOwner());
                    tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
                    tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
                }
            }
        }));

        fragmentPopUpLayoutBinding.popUpTabLayout.addTab(fragmentPopUpLayoutBinding.popUpTabLayout.newTab().setIcon(R.drawable.ic_tab_info));
        fragmentPopUpLayoutBinding.popUpTabLayout.setTabGravity(GRAVITY_FILL);
        /* for this initiate tab layout and while rotate device update adapter*/
        // initPopupSettingsTabLayout();
        setCameraSettingAdapter();

        /* WHEN LONG PRESS CAMERA ICON SHOW POP UP DIALOG TO DELETE CAMERA ITEM FROM THE LIST*/
        homeViewModel.isDeleteCameraItem.observe(lifecycleOwner, new EventObserver<>(hasSelectDiscoverDevice -> {
            if (hasSelectDiscoverDevice) {
                String camera_ssid = getWiFiHistory().getCamera_ssid();
                bleWiFiViewModel.deleteSsid(camera_ssid);
                digitalCameraInfoViewModel.deleteNWDSsid(camera_ssid); // delete nwd data also
                homeViewModel.removeSelectedDeviceConnections(getWiFiHistory()); //  home screen grid view items to remove
                if (getWiFiHistory().getIs_wifi_connected() == WIFI_CONNECTED){
                    connectivityManager.bindProcessToNetwork(null);
                    if (activity != null) {
                        activity.unregisterNetworkCallback();
                    }
                    networkCapability = null;
                }
                try {
                    if (activity != null) {
                        boolean isFirmwareSkipped = activity.getIsFirmwareSkipped(camera_ssid);
                        if (isFirmwareSkipped) {
                            activity.setFirmwareSkipped(camera_ssid, false);
                        }
                    }

                    if (selectedDeviceHistoryAdpter != null) {
                        selectedDeviceHistoryAdpter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));

        homeViewModel.hideFwScreenAndShowHomeScreen.observe(lifecycleOwner, new EventObserver<>(vboolean -> {
            if (vboolean) {
                displayHomeScreen();
            }
        }));

        // for this pop up settings bg to fg to show home screen
        homeViewModel.hidePopUpWindowAndShowHomeScreen.observe(lifecycleOwner, new EventObserver<>(vboolean -> {
            if (vboolean) {
                // for this pop up setting custom progress dialog while showing app goes to bg to fg
                // to dismiss progress dialog
                Log.e(TAG, "subscribeUI: hidePopUpWindowAndShowHomeScreen ");
                popUpCameraInfoAdapter = null;
                homeViewModel.popUpSettingsAdapter = null;
                hideProgressBar();
                screenType = HOME;
                showHomeScreen();
                popUpCameraSettingsViewModel.setShowDatePicker(false);
                popUpCameraSettingsViewModel.setShowTimePicker(false);
                homeViewModel.getNavController().popBackStack(R.id.popUpSettingsFragment, true);
                /* for this after close dialog remove observer*/
//                if (CameraViewModel.hasNewFirmware()) {
                tcpConnectionViewModel.observeCameraMode().removeObserver(observeCameraMode);
                tcpConnectionViewModel.observeCameraMode().removeObservers(this.getViewLifecycleOwner());
                tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
                tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
//                }
                popUpCameraSettingsViewModel.setDismissCustomProgressDialog(true);
                popUpCameraSettingsViewModel.setConfirmationDialogShown(false);
                //avoid live stream after bg to fg and remove camera observer in popup setting screen
                tcpConnectionViewModel.disconnectSocket();
                tcpConnectionViewModel.setProgramatically(false);
                setShowTimeZoneLayout(false);
                setSelectedTimeZone(false);
            }
        }));

        tcpConnectionViewModel.isSocketConnectedEvent().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(mState -> {
            if (mState != null) {
                if (!tcpConnectionViewModel.isFirmwareUpdateChecked()) {
                    if (mState == Constants.STATE_CONNECTED) {
                        Log.e(TAG, "subscribeUI: STATE_CONNECTED");
                        //  showProgressBar();
                        readManifestFile();
                        fwMode = MODE_NONE;
                        registerObserverNightwaveOrOpsin();
                    }
                }
            }
        }));
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                registerObserverNightwave();
                break;
            case OPSIN:
                registerObserverOpsin();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"registerCamera : NW_Digital");
                break;
        }
        /* When special character save button press observe */
        homeViewModel.isUpdateCameraName.observe(lifecycleOwner, new EventObserver<>(cameraName -> {
            if (cameraName != null && !cameraName.isEmpty()) {
                // here update camera name and show custom progress dialog
                boolean b = bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                if (b) {
                    Log.e(TAG, "subscribeUI: cameraName" + cameraName);
                    if (tcpConnectionViewModel != null) {
                        Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                        if (value != null && value == Constants.STATE_CONNECTED) {
                            showProgressBar();
                            switch (currentCameraSsid) {
                                case NIGHTWAVE:
                                    tcpConnectionViewModel.setCameraName(cameraName);
                                    break;
                                case OPSIN:
                                    tcpConnectionViewModel.setOpsinCameraName(cameraName);
                                    break;
                                case NIGHTWAVE_DIGITAL:
                                    Log.e(TAG,"isUpdateCameraName Observer : NW_Digital");
                                    break;
                            }

                            isUpdatingCameraName = true;
                            new Handler().postDelayed(() -> {
                                hideProgressBar();
                                bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), cameraName);
                                switch (currentCameraSsid) {
                                    case NIGHTWAVE:
                                        tcpConnectionViewModel.getCameraName();
                                        break;
                                    case OPSIN:
                                        tcpConnectionViewModel.getOpsinCameraName();
                                        break;
                                    case NIGHTWAVE_DIGITAL:
                                        Log.e(TAG,"UpdateCameraName : NW_Digital");
                                        break;
                                }
                            }, 5000);
                        }
                    } else {
                        isUpdatingCameraName = false;
                        Toast.makeText(homeViewModel.appContext, getString(R.string.camera_update_failed), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }));

        homeViewModel.getNavigateToNextScreen().observe(requireActivity(), new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                String connectedSsid = getWiFiSSID(mWifiManager);
                showProgressBar();
                if (!connectedSsid.equals("") && !connectedSsid.equals(activity.getString(R.string.unknown_ssid))) {
                    Log.e(TAG, "subscribeUI: getNavigateToNextScreen() " + connectedSsid);
                    tcpConnectionViewModel.setSSIDSelectedManually(true);
                    tcpConnectionViewModel.setFirmwareUpdateCompleted(false);
                    mWifiState = WIFI_STATE_CONNECTED;
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE) || currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                        if (mState == Constants.STATE_CONNECTED) {
                            registerObserverNightwaveOrOpsin();
                        } else {
                            new Handler().postDelayed(() -> tcpConnectionViewModel.connectSocket(), 5000);
                        }
                    } else {
                        if (!isSelectPopUpFwUpdateCheck && !isSelectPopUpInfo && !isSelectPopUpSettings){
                            MainActivity activity = ((MainActivity) getActivity());
                            if (activity != null) {
                                Log.e(TAG,"rtspState " + rtspState);
                                try {
                                    if (activity.showDialog != MainActivity.ShowDialog.NONE) {
                                        hideShowDialog();
                                        hasAlreadyAddedInDialogTag = false;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                homeViewModel.getNavController().navigate(R.id.cameraSplashFragment);
                                homeViewModel.setNavigateToNextScreen(false);
                            }
                        } else {
                            hideProgressBar();
                        }
                    }
                } else {
                    hideProgressBar();
                    Log.e(TAG, "hideProgressBar:8 ");
                }
            }
        }));

        tcpConnectionViewModel.observeCircleProgressIndicator().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(isShown -> {
            if (screenType.equals(FW_UPDATE_SCREEN) && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                if (isShown) {
                    if (binding.fwDotProgressBar.getVisibility() == GONE) {
                        Log.e(TAG, "subscribeUI: observeCircleProgressIndicator VISIBLE");
                        requireActivity().runOnUiThread(() -> {
                            binding.fwDotProgressBar.setVisibility(VISIBLE);
                            binding.fwDotProgressBar.smoothToShow();
                        });
                    }
                } else {
                    if (binding.fwDotProgressBar.getVisibility() == VISIBLE) {
                        binding.fwDotProgressBar.setVisibility(GONE);
                        binding.fwDotProgressBar.smoothToHide();
                    }
                }
            }
        }));


        tcpConnectionViewModel.observeFwUpdateProgress().observe(lifecycleOwner, fwProgress);
        /*When firmware update socket timeout during fw update show this dialog*/
        tcpConnectionViewModel.observeFwUpdateFailed().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                    Log.e(TAG, "subscribeUI: observeFwUpdateFailed " + object );
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                        showFwRetryDialogMessage(getString(R.string.firmware_update_failed_retry));
                    } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)){
                        hideProgressBar();
                        if (object.equals("NO_RESPONSE_TIMEOUT")) {
                            showFwReBootModeDialog(requireContext().getString(R.string.display_user_contact_customer_care_service));
                        }
                    }
                }
            }
        }));

        homeViewModel.isUpdateFirmware.observe(lifecycleOwner, new EventObserver<>(value -> {
            switch (value) {
                case NEGATIVE_BUTTON_CLICKED:
                    Log.e(TAG, "subscribeUI: Negative" + HomeViewModel.screenType.name());
                    if (activity != null) {
                        String wiFiSSID = getWiFiSSID(mWifiManager);
                        if (!isSelectPopUpFwUpdateCheck) // for this avoid fw update skipped selected ssid while long press
                            activity.setFirmwareSkipped(wiFiSSID, true);
                    }

                    homeViewModel.setShowFwDialogNone(false);/* for this only show/hide the pop window layouts*/
                    /*for this condition fw update dialog show and click negative button show related ui*/
                    if (isSelectPopUpFwUpdateCheck) {
                        isSelectPopUpFwUpdateCheck = false;
                        firmwareUpdateSequence.clear();
                        applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.NONE;
                        homeViewModel.setSelectedCamera("");
                        disConnectCamera();
                    } else {
                        navigateToNextScreen(popUpNone);
                    }

//                    if (HomeViewModel.screenType != POP_UP_INFO_SCREEN && HomeViewModel.screenType != POP_UP_SETTINGS_SCREEN) {
//                        navigateToNextScreen(popUpNone);
//                    } else {
//                        if (HomeViewModel.screenType == POP_UP_INFO_SCREEN) {
//                            navigateToNextScreen(popUpInfoScreen);
//                        }
//                        if (HomeViewModel.screenType == POP_UP_SETTINGS_SCREEN) {
//                            navigateToNextScreen(popUpSettingsScreen);
//                        }
//                    }
                    break;
                case NEUTRAL_BUTTON_CLICKED:
                    Log.e(TAG, "subscribeUI: Neutral");
                    break;
                case POSITIVE_BUTTON_CLICKED:
                    if (activity != null) {
                        activity.setFirmwareSkipped(homeViewModel.connectedSsid, false);
                    }
                    /* reason is when click notes icon then press positive button  some case goto fw update UI*/
                    Log.e(TAG, "subscribeUI: " + homeViewModel.getDialogMode());
                    if (!homeViewModel.getDialogMode().equals("NOTES")) {
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                // If Golden update required in home screen then uncomment
                                if (HomeViewModel.isGoldenUpdate) {
                                startFwUpdate();
                                }
                                Log.e(TAG, "dialog: fw: nightwave");
                                break;
                            case OPSIN:
                                for (int i = 0; i < firmwareUpdateSequence.size(); i++) {
                                    String sequenceName = firmwareUpdateSequence.get(i);
                                    if (i > 0) {
                                        if (sequenceName.equalsIgnoreCase("WAIT")) {
                                            String previousSeq = firmwareUpdateSequence.get(i - 1);
                                            switch (previousSeq) {
                                                case RISCV:
                                                case OPSIN_RISCV_RECOVERY:
                                                    firmwareUpdateSequence.set(i, "WAIT 20");
                                                    break;
                                                case FPGA:
                                                    firmwareUpdateSequence.set(i, "WAIT 30");
                                                    break;
                                                case OPSIN_RISCV_OVERLAY:
                                                    firmwareUpdateSequence.set(i, "WAIT 40");
                                                    break;
                                                case OPSIN_FULL_IMAGE:
                                                case OPSIN_RISCV_FPGA:
                                                    firmwareUpdateSequence.set(i, "WAIT 60");
                                                    break;
                                                case OPSIN_RESTART:
                                                case OPSIN_FACTORY:
                                                    break;
                                                default:
                                                    Log.e(TAG, "subscribeUI: ");
                                            }
                                        }
                                    }
                                }
                                startOpsinFwUpdate();
                                Log.e(TAG, " dialog: fw: opsin");
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG,"positive button : NW_Digital");
                                break;
                        }
                    }
                    break;
                default:
                    break;
            }

        }));

        homeViewModel.showProgressbarLive.observe(lifecycleOwner, aBoolean -> {
            if (aBoolean) {
                new Handler().post(() -> {
                    if (binding.dotProgressBar.getVisibility() == GONE) {
                        binding.dotProgressBar.setVisibility(VISIBLE);
                    }
                });
            } else {
                if (binding.dotProgressBar.getVisibility() == VISIBLE) {
                    binding.dotProgressBar.setVisibility(GONE);
                }
            }

            if (HomeViewModel.screenType == HomeViewModel.ScreenType.ADD_SCREEN)
                binding.setIsCameraConnect(aBoolean); // For this history camera select load progressbar
            Log.e(TAG, "_showProgressbarLive: " + " ///" + binding.dotProgressBar.getVisibility() + "///" + aBoolean);
        });

        tcpConnectionViewModel.observeOpsinGetCameraName().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            String opsinConnectedSsid = bleWiFiViewModel.getConnectedSsidFromWiFiManager().replace("\"", "");
            if (object != null) {
                if (object instanceof CommandError) {
                    fragmentPopUpLayoutBinding.popUpCameraName.setText(opsinConnectedSsid);
                    fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(opsinConnectedSsid);
                    fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.opsin_connected);
                    fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.opsin_connected);
                    boolean b = bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    if (b) {
                        if (fwMode == MODE_NONE && !homeViewModel.isHasShowFwDialog()) {
                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                                Log.e(TAG, "observeOpsinGetCameraName Error: " + ((CommandError) object).getError());
                                if (homeViewModel.connectedSsid.contains(FILTER_STRING4) || homeViewModel.connectedSsid.contains(FILTER_STRING5))
                                    bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                                isUpdatingCameraName = false;
                                tcpConnectionViewModel.getProductVersion();
                            }
                        }
                    }
                } else {
                    String response = object.toString().trim();
                    Log.e(TAG, "observeOpsinGetCameraName: " + response.trim() + " " + bleWiFiViewModel.getConnectedSsidFromWiFiManager());
//                    if (!response.equals("")) {

                    String camName = response.isEmpty() ? bleWiFiViewModel.getConnectedSsidFromWiFiManager() : response.trim();
                    fragmentPopUpLayoutBinding.popUpCameraName.setText(camName);
                    fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(camName);
                    homeViewModel.observePopupOpsinCameraName(camName);

                    fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.opsin_connected);
                    fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.opsin_connected);

                    boolean b = bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    if (b) {
                        if (!response.isEmpty() && !response.matches(regex)) {
                            Log.e(TAG, "observeOpsinGetCameraName exsist onChanged1: " + response.trim());
                            hideProgressBar();
                            if (tcpConnectionViewModel != null) {
                                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                                // fort his condition after save button press and rotate device avoid to show again this dialog
                                if (value != null && value == Constants.STATE_CONNECTED && !isUpdatingCameraName) {
                                    showSpecialCharacterDialog(getString(R.string.un_supported_character_message));
                                }
                            }
                        } else {
                            if (homeViewModel.connectedSsid.contains(FILTER_STRING4) || homeViewModel.connectedSsid.contains(FILTER_STRING5)) {
                                if (fwMode == MODE_NONE && !homeViewModel.isHasShowFwDialog()) {
                                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                                        bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), response.trim());
                                        tcpConnectionViewModel.getProductVersion();
                                        homeViewModel.observePopupOpsinCameraName(camName);
                                    }
                                }
                            }
                        }
                    }
//                    } else {
//                        // for this below condition after opsin camera factory reset select camera name not showing issue to be avoid in home screen.
//                        Log.d("opsinCheckTAG opsinConnectedSsid", ""+opsinConnectedSsid);
//                        fragmentPopUpLayoutBinding.popUpCameraName.setText(opsinConnectedSsid);
//                        fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(opsinConnectedSsid);
//                        fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.opsin_connected);
//                        fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.opsin_connected);
//                        boolean b = bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
//                        if (b) {
//                            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
//                                Log.e(TAG, "observeOpsinGetCameraName onChanged2: " + response.trim());
//                                bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
//                            }
//                        }
//                    }
                }
            } else {
                Log.e(TAG, "observeOpsinGetCameraName: Object Null");
            }
        }));

/// for this get camera name rom db in pop up setting screen
        if (homeViewModel.connectedSsid != null) {
            bleWiFiViewModel.getCameraName(homeViewModel.connectedSsid).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<String>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onSuccess(String name) {
                    String ss = !name.isEmpty() ? name : homeViewModel.connectedSsid;

                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            fragmentPopUpLayoutBinding.popUpCameraName.setText(ss);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(ss);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                            break;
                        case OPSIN:
                            fragmentPopUpLayoutBinding.popUpCameraName.setText(ss);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(ss);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.opsin_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.opsin_connected);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"onSuccess : NW_Digital");
                            fragmentPopUpLayoutBinding.popUpCameraName.setText(ss);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(ss);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_digital_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_digital_connected);
                            break;
                    }
                    Log.e(TAG, "onSuccess: getCameraName" + ss);
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "onError: getCameraName" + homeViewModel.connectedSsid);
                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            fragmentPopUpLayoutBinding.popUpCameraName.setText(homeViewModel.connectedSsid);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(homeViewModel.connectedSsid);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                            break;
                        case OPSIN:
                            fragmentPopUpLayoutBinding.popUpCameraName.setText(homeViewModel.connectedSsid);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(homeViewModel.connectedSsid);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.opsin_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.opsin_connected);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Log.e(TAG,"onError : NW_Digital");
                            fragmentPopUpLayoutBinding.popUpCameraName.setText(homeViewModel.connectedSsid);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(homeViewModel.connectedSsid);
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_digital_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_digital_connected);
                            break;
                    }
                }
            });
        }

    }

    private void onBackPressed() {
        Log.e(TAG, "handleOnBackPressed: " + HomeViewModel.screenType.name());
        if (HomeViewModel.screenType == HOME) {
            HomeViewModel.addButtonState = HomeViewModel.AddButtonState.INIT;
            ((MainActivity) Objects.requireNonNull(getActivity())).exitFromApp();
            binding.galleryTabLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.INFO_SCREEN) {
            homeViewModel.setBottomLayoutType(INFO_LAYOUT_BACK_STACKING);
            handleCloseFromGalleryInfoScreen();
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_SCREEN) {
            homeViewModel.setBottomLayoutType(BACK_STACK_GALLERY_SCREEN);
            homeViewModel.setGallerySelected(false);
            homeViewModel.setGalleryManageSelected(false);
            homeViewModel.setGalleryVideoPlayerSelected(false);
            homeViewModel.setGalleryVideoRecordedInfoSelected(false);
            handleCloseFromGalleryInfoScreen();
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN) {
            homeViewModel.setBottomLayoutType(BACK_STACK_MANAGE_SCREEN);
            homeViewModel.setGalleryManageSelected(false);
            homeViewModel.setGalleryVideoPlayerSelected(false);
            homeViewModel.setGalleryVideoRecordedInfoSelected(false);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN) {
            homeViewModel.setBottomLayoutType(BACK_STACK_RECORDED_VIDEO_INFO_SCREEN);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
            homeViewModel.setBottomLayoutType(BACK_STACK_RECORDED_VIDEO_PLAYER_SCREEN);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.SETTINGS_SCREEN) {
            homeViewModel.setBottomLayoutType(SETTINGS_LAYOUT_BACK_STACKING);
        } else if ((HomeViewModel.screenType == FW_UPDATE_SCREEN) && (homeViewModel.firmwareStatus == HomeViewModel.FirmwareStatus.FW_UPDATE_STARTED || homeViewModel.firmwareStatus == HomeViewModel.FirmwareStatus.FW_UPDATE_FAILED || homeViewModel.firmwareStatus == HomeViewModel.FirmwareStatus.FW_UPDATE_NONE)) {
            /* Don't do anything here*/
            Log.e(TAG, "handleOnBackPressed: Fw_status :" + homeViewModel.firmwareStatus.name());
        } else if (HomeViewModel.screenType == FW_UPDATE_SCREEN && homeViewModel.firmwareStatus == HomeViewModel.FirmwareStatus.FW_UPDATE_COMPLETED) {
            showHomeScreen();
            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_NONE;
            hideProgressBar();
        } else if (HomeViewModel.screenType == POP_UP_INFO_SCREEN || HomeViewModel.screenType == POP_UP_SETTINGS_SCREEN) {
            /* for this while back press on either popup info & settings screen reset ths variable, avoid while after close popup window*/
            isSelectPopUpInfo = false;
            isSelectPopUpSettings = false;
            binding.infoImageView.setClickable(true);
            binding.galleryImageView.setClickable(true);
            homeViewModel.setPressCancelOrBackPopUpWindow(true);// for this avoid after cancel or back pop up window and then rotate device navigate to live view issue
            disConnectCamera();
            hideProgressBar();
            showHomeScreen();
        } else {
            showHomeScreen();
            homeViewModel.setInfoSelected(false);
            homeViewModel.setGallerySelected(false);
//                    homeViewModel.setGalleryManageSelected(false);
            homeViewModel.setGalleryVideoPlayerSelected(false);
            homeViewModel.setGalleryVideoRecordedInfoSelected(false);
            homeViewModel.setSettingsSelected(false);
            binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
            binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
            binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        }
    }

    private void showTimeZoneListViewLayout() {
        ArrayList<String> timeZonelist = new ArrayList<>(Arrays.asList(timeZoneViewModel.settingsArrayList));
        CameraTimeZoneAdapter cameraTimeZoneAdapter = new CameraTimeZoneAdapter(timeZonelist, timeZoneViewModel);
        popUpTimeZoneListViewBinding.timeZoneRecylerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        popUpTimeZoneListViewBinding.timeZoneRecylerView.setAdapter(cameraTimeZoneAdapter);
    }

    private void handleCloseFromGalleryInfoScreen() {
        showHomeScreen();
        resetCheckBoxState();
        homeViewModel.pos = 0;
        backButton = true;
        homeViewModel.setBottomLayoutType(GALLERY_CANCEL);
        HomeViewModel.screenType = HOME;
        binding.galleryTabLayout.setVisibility(View.GONE);
        homeViewModel.setGallerySelected(false);
        homeViewModel.setGalleryManageSelected(false);
        homeViewModel.setGalleryVideoPlayerSelected(false);
        homeViewModel.setGalleryVideoRecordedInfoSelected(false);
        homeViewModel.setInfoSelected(false);
        homeViewModel.setSettingsSelected(false);
        binding.galleryViewPager.setCurrentItem(0);
        binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
        binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
        binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        homeViewModel.pauseState = false;
        homeViewModel.stopState = false;
        recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
        recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
        recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);

        homeViewModel.setClosePopupAllMedia(true);
        homeViewModel.setClosePopupImage(true);
        homeViewModel.setClosePopupVideo(true);
        manageViewTabAdapter = null;
        binding.galleryViewPager.setAdapter(null);

        /* for this pop up (info / settings) screen view to click app info icon show info view and then press back if tcp already connected go to live view so, to avoid this case */
        if (tcpConnectionViewModel != null) {
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                disConnectCamera();
            }
        }
    }

    private void setCameraSettingAdapter() {
        if (HomeViewModel.screenType == HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN) {
            fragmentPopUpLayoutBinding.popUpSettingsViewPager.invalidate();
            initPopupSettingsTabLayout();
            fragmentPopUpLayoutBinding.popUpSettingsLayout.requestLayout();
            if (homeViewModel.popUpSettingsAdapter == null) {
                homeViewModel.popUpSettingsAdapter = new PopUpSettingsAdapter(this, homeViewModel.getListOfFragments());
                fragmentPopUpLayoutBinding.popUpSettingsViewPager.setAdapter(homeViewModel.popUpSettingsAdapter);
                new TabLayoutMediator(fragmentPopUpLayoutBinding.popUpSettingsTabLayout, fragmentPopUpLayoutBinding.popUpSettingsViewPager, (tab, position) -> {
                }).attach();
            } else {
                homeViewModel.popUpSettingsAdapter.notifyDataSetChanged();
            }
            try {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        if (CameraViewModel.hasNewFirmware()) {
                            fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getTabAt(0).setIcon(R.drawable.ic_camera_info);
                            fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getTabAt(1).setIcon(R.drawable.ic_camera_settings);
                        } else {
                            fragmentPopUpLayoutBinding.popUpSettingsLayout.requestLayout();
                            fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getTabAt(0).setIcon(R.drawable.ic_camera_settings);
                        }
                        break;
                    case OPSIN:
                        fragmentPopUpLayoutBinding.popUpSettingsLayout.requestLayout();
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getTabAt(0).setIcon(R.drawable.ic_camera_info);
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getTabAt(1).setIcon(R.drawable.ic_camera_settings);
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"setting tabIcon : NW_Digital");
                        break;
                }
                fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setTabGravity(GRAVITY_FILL);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initPopupSettingsTabLayout() {
        requireActivity().runOnUiThread(() -> {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    if (CameraViewModel.hasNewFirmware()) {
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.addTab(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.newTab().setIcon(R.drawable.ic_camera_info));
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.addTab(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(fragmentPopUpLayoutBinding.popUpSettingsLayout);

                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            constraintSet.connect(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getId(), ConstraintSet.END, fragmentPopUpLayoutBinding.popUpSettingsTabLayoutVerticalGuidelineEnd.getId(), ConstraintSet.END);
                        } else {
                            // newLayoutParams.rightMargin = 30;
                            if (fragmentPopUpLayoutBinding.popUpSettingsTabViewVerticalGuidelineEnd != null) {
                                constraintSet.connect(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getId(), ConstraintSet.END, fragmentPopUpLayoutBinding.popUpSettingsTabViewVerticalGuidelineEnd.getId(), ConstraintSet.END);
                            }
                        }
                        constraintSet.applyTo(fragmentPopUpLayoutBinding.popUpSettingsLayout);
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setBackground(null);
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setTabGravity(GRAVITY_FILL);
                    } else {
                        ConstraintLayout.LayoutParams newLayoutParams = (ConstraintLayout.LayoutParams) fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getLayoutParams();
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.addTab(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
                        newLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                        if (orientation == Configuration.ORIENTATION_PORTRAIT)
                            fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setBackground(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_tab_rectangle_port));
                        else
                            fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setBackground(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_tab_rectangle));
                        fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setLayoutParams(newLayoutParams);
                    }
                    fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setTabGravity(GRAVITY_FILL);
                    break;
                case OPSIN:
                    fragmentPopUpLayoutBinding.popUpSettingsLayout.requestLayout();
                    fragmentPopUpLayoutBinding.popUpSettingsTabLayout.addTab(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.newTab().setIcon(R.drawable.ic_camera_info));
                    fragmentPopUpLayoutBinding.popUpSettingsTabLayout.addTab(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(fragmentPopUpLayoutBinding.popUpSettingsLayout);

                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        constraintSet.connect(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getId(), ConstraintSet.END, fragmentPopUpLayoutBinding.popUpSettingsTabLayoutVerticalGuidelineEnd.getId(), ConstraintSet.END);
                    } else {
                        // newLayoutParams.rightMargin = 30;
                        if (fragmentPopUpLayoutBinding.popUpSettingsTabViewVerticalGuidelineEnd != null) {
                            constraintSet.connect(fragmentPopUpLayoutBinding.popUpSettingsTabLayout.getId(), ConstraintSet.END, fragmentPopUpLayoutBinding.popUpSettingsTabViewVerticalGuidelineEnd.getId(), ConstraintSet.END);
                        }
                    }
                    constraintSet.applyTo(fragmentPopUpLayoutBinding.popUpSettingsLayout);
                    fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setBackground(null);
                    fragmentPopUpLayoutBinding.popUpSettingsTabLayout.setTabGravity(GRAVITY_FILL);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"initPopupSettingsTabLayout : NW_Digital");
                    break;
            }
        });
    }

    private void setCameraInfoAdapter() {
        if (HomeViewModel.screenType == HomeViewModel.ScreenType.POP_UP_INFO_SCREEN) {
            if (popUpCameraInfoAdapter == null) {
                popUpCameraInfoAdapter = new PopUpCameraInfoAdapter(this, homeViewModel.getListOfInfoFragments());
                fragmentPopUpLayoutBinding.popUpViewPager.setAdapter(popUpCameraInfoAdapter);
            } else {
                popUpCameraInfoAdapter.notifyDataSetChanged();
            }
            new TabLayoutMediator(fragmentPopUpLayoutBinding.popUpTabLayout, fragmentPopUpLayoutBinding.popUpViewPager, (tab, position) -> {
            }).attach();
            fragmentPopUpLayoutBinding.popUpTabLayout.getTabAt(0).setIcon(R.drawable.ic_tab_info);
        }

    }

    //To display Settings Layout
    private void showSettingsLayout() {

    }

    //Dismiss Setting Layout
    private void cancelSettingsLayout() {
        showHomeScreen();
        homeViewModel.setSettingsSelected(false);
        binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        binding.selectDeviceLayout.setVisibility(VISIBLE);
        binding.infoTabLayout.setVisibility(View.GONE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
    }

    //Display Info Layout
    private void showInfoLayout() {
        homeViewModel.setInfoSelected(true);
        homeViewModel.setGallerySelected(false);
        homeViewModel.setGalleryManageSelected(false);
        homeViewModel.setGalleryVideoPlayerSelected(false);
        homeViewModel.setGalleryVideoRecordedInfoSelected(false);
        homeViewModel.setSettingsSelected(false);
        binding.infoImageView.setImageResource(R.drawable.ic_info_white_background);
        binding.galleryImageView.setImageResource(R.drawable.ic_image_icon);
        binding.settingsImageView.setImageResource(R.drawable.ic_settings_icon);
        HomeViewModel.screenType = HomeViewModel.ScreenType.INFO_SCREEN;
        binding.selectDeviceLayout.setVisibility(View.GONE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        binding.infoTabLayout.setVisibility(VISIBLE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        popUpTimeZoneListViewBinding.popUpTimeZoneList.setVisibility(View.GONE);
        setShowTimeZoneLayout(false);
        setSelectedTimeZone(false);
    }

    //Dismiss Info Layuot
    private void cancelInfoLayout() {
        showHomeScreen();
        homeViewModel.setInfoSelected(false);
        binding.infoImageView.setImageResource(R.drawable.ic_info_icon);
        binding.selectDeviceLayout.setVisibility(VISIBLE);
        binding.infoTabLayout.setVisibility(View.GONE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
    }

    //method to resetting the manage -layout item's checkbox state
    private void resetCheckBoxState() {
//        if (binding.galleryViewPager.getCurrentItem() == 0) {
        homeViewModel.onManageAllSelectBack(true);
//        } else if (binding.galleryViewPager.getCurrentItem() == 1) {
        homeViewModel.onManagePhotoSelectBack(true);
//        } else if (binding.galleryViewPager.getCurrentItem() == 2) {
        homeViewModel.onManageVideoSelectBack(true);
//        }
    }

    //method for recording info layout- data handling
    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy ");
    @SuppressLint("SimpleDateFormat")
    DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss z");

    private void showRecordingVideoInfolayout(int itemPosition) {
        if (galleryFilePath != null) {
            selectedItemPosition = itemPosition;
            binding.galleryTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);

            recordingInfoScreenBinding.recordingInfoVideoPlayButton.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingInfoHeading.setText(getString(R.string.recording_info));
            recordingInfoScreenBinding.recordingInfoDeleteButton.setText(getString(R.string.delete_recording));
            recordingInfoScreenBinding.recordingInfoScrollView.setVisibility(VISIBLE);
            recordingInfoScreenBinding.imageInfoScrollView.setVisibility(View.GONE);


            File file = new File(galleryFilePath);
            Date lastModifiedDate = new Date(file.lastModified());

            recordingInfoScreenBinding.recordingInfoDetailHolderVideoDate.setText(dateFormat.format(lastModifiedDate).toUpperCase());
            recordingInfoScreenBinding.recordingInfoDetailHolderVideoTime.setText(timeFormat.format(lastModifiedDate));

            MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(galleryFilePath));
            if (mediaPlayer != null) {
                recordingInfoScreenBinding.recordingInfoDetailHolderVideoDuration.setText(getTotalVideoDuration(mediaPlayer.getDuration()));
            }
//            recordingInfoScreenBinding.recordingInfoDetailHolderLocation.setText("INDIA");
            String[] originalFileName = file.getName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                String videoTitle = dateString.toUpperCase() + "-" + (selectedItemPosition + 1);
                recordingInfoScreenBinding.recordingInfoVideoTitle.setText(videoTitle);
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            if (file.length() > 0) {
                retriever.setDataSource(requireContext(), Uri.parse(galleryFilePath));
            }
            switch (currentCameraSsid){
                case NIGHTWAVE:
                    Glide.with(requireActivity()).load(galleryFilePath)
                            .placeholder( R.drawable.ic_camera_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingInfoVideoThumbnail);
                    break;
                case OPSIN:
                    Glide.with(requireActivity()).load(galleryFilePath)
                            .placeholder( R.drawable.opsin_live_view_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingInfoVideoThumbnail);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Glide.with(requireActivity()).load(galleryFilePath)
                            .placeholder( R.drawable.ic_nw_digital_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingInfoVideoThumbnail);
                    break;
            }

            videoFilePath = galleryFilePath;

            String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
            if (location != null) {
                Pattern pattern = Pattern.compile("^([+|-]\\d{1,2}(?:\\.\\d+)?)([+|-]\\d{1,3}(?:\\.\\d+)?)");
                // create a matcher object with the location string
                Matcher matcher = pattern.matcher(location);
                // check if the matcher finds a match
                if (matcher.find()) {
                    // get the latitude and longitude values from the matcher groups
                    double latitude = Double.parseDouble(Objects.requireNonNull(matcher.group(1)));
                    double longitude = Double.parseDouble(Objects.requireNonNull(matcher.group(2)));
                    if (latitude != 0.0 && longitude != 0.0)
                        recordingInfoScreenBinding.recordingInfoDetailHolderVideoLocation.setText("Lat: " + latitude + "\nLon: " + longitude);
                    else
                        recordingInfoScreenBinding.recordingInfoDetailHolderVideoLocation.setText(getString(R.string.location_not_found));

                } else {
                    recordingInfoScreenBinding.recordingInfoDetailHolderVideoLocation.setText(getString(R.string.location_not_found));
                }
            } else {
                recordingInfoScreenBinding.recordingInfoDetailHolderVideoLocation.setText(getString(R.string.location_not_found));
            }

            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
            if (!videoFilePath.isEmpty()) {
                recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVideoPath(videoFilePath);
            }
            if (!recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying()) {
                // recordingInfoScreenBinding.recordingVideoPlayerVideoView.start();
                recordingInfoScreenBinding.recordingVideoPlayerVideoView.stopPlayback();
                homeViewModel.pos = 0;
                recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
                recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
                recordingInfoScreenBinding.recordingVideoPlayerPause.setImageResource(R.drawable.ic_pause_outline);
            }
        }
    }

    private final DecimalFormat df = new DecimalFormat("#.####");

    private void showCapturedImageInfo(int itemPosition) {

        homeViewModel.isSelectedItemPosition = itemPosition;
        if (homeViewModel.isSelectedFilePath != null) {
            binding.galleryTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);

            recordingInfoScreenBinding.recordingInfoVideoPlayButton.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoHeading.setText(getString(R.string.image_info));
            recordingInfoScreenBinding.recordingInfoDeleteButton.setText(getString(R.string.delete_image_text));
            recordingInfoScreenBinding.recordingInfoScrollView.setVisibility(View.GONE);
            recordingInfoScreenBinding.imageInfoScrollView.setVisibility(VISIBLE);

            File file = new File(homeViewModel.isSelectedFilePath.filePath);
            Date lastModifiedDate = new Date(file.lastModified());
            videoFilePath = homeViewModel.isSelectedFilePath.filePath;
            try {
                ExifInterface exifInterface = new ExifInterface(file);
                int[] resolution = getResolution(exifInterface);
                String resolutionFormat = String.format(getString(R.string.resolution_format), resolution[0], resolution[1]);

                String lat = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                String longg = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                String lat_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                String long_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

                String latLon = getString(R.string.location_not_found);
                if (lat != null && longg != null && lat_ref != null && long_ref != null) {
                    try {
                        String[] splitLat = lat.split(",");
                        String[] splitLong = longg.split(",");

                        String[] latDegree = splitLat[0].split("/");
                        String[] latMinutes = splitLat[1].split("/");
                        String[] latSeconds = splitLat[2].split("/");

                        String[] longDegree = splitLong[0].split("/");
                        String[] longMinutes = splitLong[1].split("/");
                        String[] longSeconds = splitLong[2].split("/");

                        // Example latitude in DMS format: 37° 45' 30" N
                        String degrees = latDegree[0];
                        String minutes = latMinutes[0];
                        String seconds = latSeconds[0];
                        String direction = lat_ref; // 'N' for North, 'S' for South

                        double latitude = convertDMSToDD(degrees, minutes, seconds, direction);
                        Log.e(TAG, "Latitude in Decimal Degrees:: " + latitude);
                        String roundedValueLat = df.format(latitude);
                        double roundedLatitude = Double.parseDouble(roundedValueLat);


                        // Example longitude in DMS format: 122° 15' 45" W
                        degrees = longDegree[0];
                        minutes = longMinutes[0];
                        seconds = longSeconds[0];
                        direction = long_ref; // 'W' for West, 'E' for East

                        double longitude = convertDMSToDD(degrees, minutes, seconds, direction);
                        Log.e(TAG, "Longitude in Decimal Degrees:: " + longitude);
                        String roundedValue = df.format(longitude);
                        double roundedLongitude = Double.parseDouble(roundedValue);

                        if (roundedLongitude != 0.0 && roundedLatitude != 0.0)
                            latLon = String.format(getString(R.string.lat_lon), String.valueOf(roundedLatitude), String.valueOf(roundedLongitude));
                        else
                            latLon = requireContext().getString(R.string.location_not_found);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                String focalLengthFormatted = getFormattedFocalLength(exifInterface);
                String apertureFormatted = getFormattedAperture(exifInterface);
                recordingInfoScreenBinding.recordingInfoDetailHolderLocation.setText(latLon);
                recordingInfoScreenBinding.recordingInfoDetailHolderModel.setText(exifInterface.getAttribute(ExifInterface.TAG_MODEL));
                recordingInfoScreenBinding.recordingInfoDetailHolderResolution.setText(resolutionFormat);
                recordingInfoScreenBinding.recordingInfoDetailHolderSoftware.setText(exifInterface.getAttribute(ExifInterface.TAG_SOFTWARE));
                recordingInfoScreenBinding.recordingInfoDetailHolderAperture.setText(apertureFormatted);
                recordingInfoScreenBinding.recordingInfoDetailHolderFocalLength.setText(focalLengthFormatted);
                recordingInfoScreenBinding.recordingInfoDetailHolderDate.setText(dateFormat.format(lastModifiedDate).toUpperCase());
                recordingInfoScreenBinding.recordingInfoDetailHolderTime.setText(timeFormat.format(lastModifiedDate));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] originalFileName = file.getName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                String imageTitle = dateString.toUpperCase() + "-" + (itemPosition + 1);
                recordingInfoScreenBinding.recordingInfoVideoTitle.setText(imageTitle);
            }


            switch (currentCameraSsid){
                case NIGHTWAVE:
                    Glide.with(requireActivity()).load(homeViewModel.isSelectedFilePath.filePath)
                            .placeholder( R.drawable.ic_camera_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingInfoVideoThumbnail);
                    break;
                case OPSIN:
                    Glide.with(requireActivity()).load(homeViewModel.isSelectedFilePath.filePath)
                            .placeholder( R.drawable.opsin_live_view_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingInfoVideoThumbnail);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Glide.with(requireActivity()).load(homeViewModel.isSelectedFilePath.filePath)
                            .placeholder( R.drawable.ic_nw_digital_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingInfoVideoThumbnail);
                    break;
            }
//            videoFilePath = galleryFilePath;
        }
    }

    public double convertDMSToDD(String degrees, String minutes, String seconds, String
            direction) {
        try {
            double d = Double.parseDouble(degrees);
            double m = Double.parseDouble(minutes);
            double s = Double.parseDouble(seconds);

            double decimal = d + (m / 60.0) + (s / 3600.0);

            // Adjust for negative values (South for latitude, West for longitude)
            if (direction.equals("S") || direction.equals("W")) {
                decimal = -decimal;
            }

            return decimal;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0.0; // Handle invalid input gracefully
        }
    }

    public int[] getResolution(ExifInterface exifInterface) throws IOException {
        int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
        int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
        Log.e(TAG, "getWidthAndHeight1: " + width + " " + height);
        return new int[]{width, height};
    }


    //Display Gallery Layout
    private void backToGallery() {
        if (homeViewModel.getArrayListAll() != null) {
            homeViewModel.getArrayListAll().clear();
            homeViewModel.getAllFiles(getActivity());
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ConstraintSet constraintSet = new ConstraintSet();
            ConstraintLayout constraintLayout = binding.galleryTabLayout;
            constraintSet.clone(constraintLayout);
            constraintSet.connect(binding.galleryViewPager.getId(), ConstraintSet.TOP, binding.galleryManageButtonHorizontalBottomGuideline.getId(), ConstraintSet.TOP, 30);
            constraintSet.connect(binding.galleryViewPager.getId(), ConstraintSet.BOTTOM, binding.gallerytabLayout.getId(), ConstraintSet.TOP, 30);
            constraintSet.setMargin(binding.galleryViewPager.getId(), ConstraintSet.BOTTOM, 28);
            constraintSet.applyTo(constraintLayout);
        }
        binding.galleryTabLayout.setVisibility(VISIBLE);
        binding.galleryManageOptionButtons.setVisibility(View.GONE);
        binding.galleryManageButton.setVisibility(VISIBLE);
        binding.galleryShareButton.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        binding.galleryImageView.setImageResource(R.drawable.ic_image_white_background);
        HomeViewModel.screenType = GALLERY_SCREEN;
        homeViewModel.setGalleryManageSelected(false);
        galleryAdapterView(false);
    }

    //Display Recorded_Video_Info Layout
    private void showRecordedVideoInfoLayout() {
        binding.galleryTabLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(VISIBLE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        binding.selectDeviceLayout.setVisibility(View.GONE);
        if (galleryFilePath != null) {
            homeViewModel.setBottomLayoutType(RECORDING_VIDEO_INFO_LAYOUT);
        }
    }

    //handling recording video player layout's data
    private void showRecordedVideoPlayerLayout(int curImagePos) {
        if (galleryFilePath.endsWith(".mp4")) {
            recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(1.0f);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(1.0f);
            recordingInfoScreenBinding.recordingVideoPlayerHeading.setText(getString(R.string.recording_info));
            recordingInfoScreenBinding.recordingVideoPlayerPause.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingImageUpdate.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_play_outline);
            recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_stop_outline);
            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerImageView.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerVideoCurrentDuration.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerVideoTotalDuration.setVisibility(VISIBLE);

            binding.galleryTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
            //recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.VISIBLE);
            //  recordingInfoScreenBinding.recordingVideoPlayerVideoView.stopPlayback();
//        if (!galleryFilePath.isEmpty()) {
            File file = new File(galleryFilePath);
            String[] originalFileName = file.getName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                String videoTitle = dateString.toUpperCase() + "-" + (selectedItemPosition + 1);
                recordingInfoScreenBinding.recordingVideoPlayerVideoTitle.setText(videoTitle);
            }

            if (galleryFilePath.endsWith(".mp4")) {
                recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVideoPath(galleryFilePath);
                if(homeViewModel.playVideo)
                    mHandler.postDelayed(mUpdateTimeTask, 100);
                recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setProgress(0);
            }
        }
    }

    //handling captured image layout with slider
    private void showCapturedImageLayout(int curImagePos) {
        if (galleryFilePath.endsWith(".jpg")) {
            homeViewModel.isSelectedItemPosition = curImagePos;//homeViewModel.filterImageList.indexOf(homeViewModel.isSelectedFilePath);
            selectedItemPosition = curImagePos;
            galleryFilePath = homeViewModel.filterImageList.get(curImagePos).filePath;//homeViewModel.isSelectedItemPosition
            recordingInfoScreenBinding.recordingVideoPlayerHeading.setText(getString(R.string.image_info));
            recordingInfoScreenBinding.recordingVideoPlayerPause.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setImageResource(R.drawable.ic_arrow_left);
            recordingInfoScreenBinding.recordingVideoPlayerStop.setImageResource(R.drawable.ic_arrow_right);
            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerImageView.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerPlayButton.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerVideoCurrentDuration.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerVideoTotalDuration.setVisibility(View.GONE);


            switch (currentCameraSsid){
                case NIGHTWAVE:
                    Glide.with(requireActivity()).load(galleryFilePath)
                            .placeholder( R.drawable.ic_camera_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingVideoPlayerImageView);
                    break;
                case OPSIN:
                    Glide.with(requireActivity()).load(galleryFilePath)
                            .placeholder( R.drawable.opsin_live_view_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingVideoPlayerImageView);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Glide.with(requireActivity()).load(galleryFilePath)
                            .placeholder( R.drawable.ic_nw_digital_background )
                            .into((ImageView) recordingInfoScreenBinding.recordingVideoPlayerImageView);
                    break;
            }

            recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(1.0f);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(1.0f);

            recordingInfoScreenBinding.recordingImageUpdate.setVisibility(VISIBLE);
            String imageUpdate = (curImagePos + 1) + "/" + homeViewModel.filterImageList.size();
            recordingInfoScreenBinding.recordingImageUpdate.setText(imageUpdate);//0 1 2 3 (3+1)/4
            if (homeViewModel.filterImageList.size() == 1) {//5/5
                recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(0.10f);
                recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(0.10f);
            } else if ((curImagePos + 1) == 1) {
                recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(0.10f);
                recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(1.0f);
            } else if ((curImagePos + 1) == homeViewModel.filterImageList.size()) {//
                recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(0.10f);
                recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(1.0f);
            }

            binding.galleryTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);

            File file = new File(galleryFilePath);
            String[] originalFileName = file.getName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                String videoTitle = dateString.toUpperCase() + "-" + (selectedItemPosition + 1);
                recordingInfoScreenBinding.recordingVideoPlayerVideoTitle.setText(videoTitle);
            }
        }
    }

    private void nextImage(int imagePos) {
        String filepath = homeViewModel.filterImageList.get(imagePos).filePath;

        switch (currentCameraSsid){
            case NIGHTWAVE:
                Glide.with(requireActivity()).load(filepath)
                        .placeholder( R.drawable.ic_camera_background )
                        .into((ImageView) recordingInfoScreenBinding.recordingVideoPlayerImageView);
            case OPSIN:
                Glide.with(requireActivity()).load(filepath)
                        .placeholder( R.drawable.opsin_live_view_background )
                        .into((ImageView) recordingInfoScreenBinding.recordingVideoPlayerImageView);
            case NIGHTWAVE_DIGITAL:
                Glide.with(requireActivity()).load(filepath)
                        .placeholder( R.drawable.ic_nw_digital_background )
                        .into((ImageView) recordingInfoScreenBinding.recordingVideoPlayerImageView);
        }
        Log.i(TAG, "nextImage: IMAGE SLIDER >> " + (imagePos + 1) + "/" + homeViewModel.filterImageList.size());
        String imageUpdate = (imagePos + 1) + "/" + homeViewModel.filterImageList.size();

        recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(1.0f);
        recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(1.0f);

        recordingInfoScreenBinding.recordingImageUpdate.setVisibility(VISIBLE);
        recordingInfoScreenBinding.recordingImageUpdate.setText(imageUpdate);

        if (homeViewModel.filterImageList.size() == 1) {//1/1
            recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(0.10f);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(0.10f);
        } else if ((imagePos + 1) == 1) {//1/4
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(0.10f);
            recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(1.0f);
        } else if ((imagePos + 1) == homeViewModel.filterImageList.size()) {
            recordingInfoScreenBinding.recordingVideoPlayerStop.setAlpha(0.10f);
            recordingInfoScreenBinding.recordingVideoPlayerPlay.setAlpha(1.0f);
        }

        homeViewModel.isSelectedItemPosition = imagePos;

        if (!filepath.isEmpty()) {
            File file = new File(filepath);
            String[] originalFileName = file.getName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                String videoTitle = dateString.toUpperCase() + "-" + (imagePos + 1);
                recordingInfoScreenBinding.recordingVideoPlayerVideoTitle.setText(videoTitle);
            }
        }
    }

    private void galleryAdapterView(boolean isManage) {
        homeViewModel.setIsManageSelect(isManage);
        if (manageViewTabAdapter == null) {
            manageViewTabAdapter = new GalleryBottomTabPageAdapter(this, homeViewModel.getFragments());
            binding.galleryViewPager.setAdapter(manageViewTabAdapter);
            binding.galleryViewPager.setCurrentItem(homeViewModel.getCurrentManageTabPosition(), true);
            binding.galleryViewPager.setOffscreenPageLimit(3);
            new TabLayoutMediator(binding.gallerytabLayout, binding.galleryViewPager, (tab, position) -> {
            }).attach();

            if (Objects.requireNonNull(getActivity()).getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                binding.gallerytabLayout.getTabAt(0).setText(getString(R.string.all));
                binding.gallerytabLayout.getTabAt(1).setIcon(R.drawable.ic_gallery);
                binding.gallerytabLayout.getTabAt(2).setIcon(R.drawable.ic_video_icon);
            } else {
                binding.gallerytabLayout.getTabAt(0).setText(getString(R.string.all));
                binding.gallerytabLayout.getTabAt(1).setText(getString(R.string.photos));
                binding.gallerytabLayout.getTabAt(2).setText(getString(R.string.videos));
            }

            binding.gallerytabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    homeViewModel.tabPos = tab.getPosition();
                    homeViewModel.setCurrentManageTabPosition(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

        }
    }

    private final Observer<Boolean> isNoRecordAll = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            Log.e(TAG, "onChanged: ALL " + aBoolean);
            int selectedTabPosition = binding.gallerytabLayout.getSelectedTabPosition();
            if (selectedTabPosition == 0) {
                if (aBoolean) {
                    if (binding.galleryManageOptionButtons.getVisibility() == VISIBLE) {
                        homeViewModel.onManageSelectBack();
                    }
                    binding.galleryManageButton.setVisibility(View.GONE);
                } else {
                    if (binding.galleryManageOptionButtons.getVisibility() == VISIBLE) {
                        binding.galleryManageButton.setVisibility(View.GONE);
                    } else {
                        binding.galleryManageButton.setVisibility(VISIBLE);
                    }
                }
            }
        }
    };
    private final Observer<Boolean> isNoRecordImage = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            Log.e(TAG, "onChanged: IMG " + aBoolean);
            int selectedTabPosition = binding.gallerytabLayout.getSelectedTabPosition();
            if (selectedTabPosition == 1) {
                if (aBoolean) {
                    if (binding.galleryManageOptionButtons.getVisibility() == VISIBLE) {
                        homeViewModel.onManageSelectBack();
                    }
                    binding.galleryManageButton.setVisibility(View.GONE);
                } else {
                    if (binding.galleryManageOptionButtons.getVisibility() == VISIBLE) {
                        binding.galleryManageButton.setVisibility(View.GONE);
                    } else {
                        binding.galleryManageButton.setVisibility(VISIBLE);
                    }
                }
            }
        }
    };
    private final Observer<Boolean> isNoRecordVideo = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            Log.e(TAG, "onChanged: VIDEO " + aBoolean);
            int selectedTabPosition = binding.gallerytabLayout.getSelectedTabPosition();
            if (selectedTabPosition == 2) {
                if (aBoolean) {
                    if (binding.galleryManageOptionButtons.getVisibility() == VISIBLE) {
                        homeViewModel.onManageSelectBack();
                    }
                    binding.galleryManageButton.setVisibility(View.GONE);

                } else {
                    if (binding.galleryManageOptionButtons.getVisibility() == VISIBLE) {
                        binding.galleryManageButton.setVisibility(View.GONE);
                    } else {
                        binding.galleryManageButton.setVisibility(VISIBLE);
                    }
                }
            }

        }
    };


    private final Handler mHandler = new Handler();

    private void notifyAdded(ArrayList<WiFiHistory> selectDeviceModels) {
        selectDeviceModels.forEach(wiFiHistory -> {
            /*Select Device Adapter*/
            if (adapter != null && homeViewModel.getSelectDeviceModelss().contains(wiFiHistory)) {
                int index = homeViewModel.getSelectDeviceModelss().indexOf(wiFiHistory);
                int is_wifi_connected = wiFiHistory.getIs_wifi_connected();
                String ssidHistory = wiFiHistory.getCamera_ssid();
//                Log.e(TAG, "notifyAdded: " + is_wifi_connected + " " + index);

                if (is_wifi_connected == 1) {
                    RecyclerView.ViewHolder viewHolderForAdapterPosition = binding.selectedDeviceRecyclerView.findViewHolderForAdapterPosition(index);
                    if (viewHolderForAdapterPosition != null) {
                        View itemView = viewHolderForAdapterPosition.itemView;
                        View viewById = itemView.findViewById(R.id.camera_icon);
                        CharSequence contentDescription = viewById.getContentDescription();
                        if (contentDescription != null) {
                            String strContentDescription = contentDescription.toString();
                            String[] split = strContentDescription.split(":");
                            String stateId = split[0];
                            String stateDescription = split[1];
                            String ssid = split[2];

//                            Log.e(TAG, "notifyAdded: "
//                                    + "\nstateId: " + stateId
//                                    + "\nstateDescription: " + stateDescription
//                                    + "\nssid: " + ssid
//                                    + "\nssidHistory: " + ssidHistory
//                                    + "\nis_wifi_connected: " + is_wifi_connected
//                                    + "\nindex: " + index);

                            if (!stateDescription.contains("WIFI_CONNECTED") || !ssid.equalsIgnoreCase(ssidHistory)) {
                                homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                Collections.swap(homeViewModel.getSelectDeviceModelss(), index, 1);
                                adapter.notifyItemMoved(index, 1);
                                adapter.notifyItemChanged(index);
                                adapter.notifyItemChanged(1);
                            }
                        } else {
                            homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                            Collections.swap(homeViewModel.getSelectDeviceModelss(), index, 1);
                            adapter.notifyItemMoved(index, 1);
                            adapter.notifyItemChanged(index);
                            adapter.notifyItemChanged(1);
                        }
                    }
                } else {
                    RecyclerView.ViewHolder viewHolderForAdapterPosition = binding.selectedDeviceRecyclerView.findViewHolderForAdapterPosition(index);
                    try {
                        if (viewHolderForAdapterPosition != null) {
                            View itemView = viewHolderForAdapterPosition.itemView;
                            View viewById = itemView.findViewById(R.id.camera_icon);
                            CharSequence contentDescription = viewById.getContentDescription();
                            if (contentDescription != null) {
                                String strContentDescription = contentDescription.toString();
//                                Log.e(TAG, "notifyAdded: " + strContentDescription);
                                String[] split = strContentDescription.split(":");
                                String stateId = split[0];
                                String stateDescription = split[1];
                                String ssid = split[2];

                                //                            Log.e(TAG, "notifyAdded: else "
                                //                                    + "\nstateId: " + stateId
                                //                                    + "\nstateDescription: " + stateDescription
                                //                                    + "\nssid: " + ssid
                                //                                    + "\nssidHistory: " + ssidHistory
                                //                                    + "\nis_wifi_connected: " + is_wifi_connected
                                //                                    + "\nindex: " + index);

                                if ((stateDescription.contains("WIFI_AVAILABLE") && is_wifi_connected != 0) || !ssid.equalsIgnoreCase(ssidHistory)) {
                                    homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                    adapter.notifyItemChanged(index);
                                } else if ((stateDescription.contains("WIFI_NOT_CONNECTED") && is_wifi_connected != -1) || !ssid.equalsIgnoreCase(ssidHistory)) {
                                    homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                    adapter.notifyItemChanged(index);
                                } else if (stateDescription.contains("WIFI_CONNECTED") || !ssid.equalsIgnoreCase(ssidHistory)) {
                                    homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                    adapter.notifyItemChanged(index);
                                }
                            } else {
                                homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                adapter.notifyItemChanged(index);
                            }
                        } else {
                            homeViewModel.getSelectDeviceModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                            adapter.notifyItemChanged(index);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                homeViewModel.getSelectDeviceModelss().add(wiFiHistory);
                if (homeViewModel.getSelectDeviceModelss().size() == 1) {
                    adapter.notifyItemInserted(0);
                } else {
                    int addedIndex = homeViewModel.getSelectDeviceModelss().indexOf(wiFiHistory);
                    if (addedIndex == 1) {
                        adapter.notifyItemInserted(1);
                    } else {
                        adapter.notifyItemInserted(addedIndex);

                        mHandler.postDelayed(() -> {
                            WiFiHistory wiFiHistory1 = homeViewModel.getSelectDeviceModelss().get(1);
                            if (wiFiHistory1.getIs_wifi_connected() == 1 && addedIndex > 2) {
                                Collections.swap(homeViewModel.getSelectDeviceModelss(), addedIndex, 2);
                                adapter.notifyItemMoved(addedIndex, 2);
                            } else {
                                WiFiHistory wiFiHistoryFromPos = homeViewModel.getSelectDeviceModelss().get(1);
                                String connectedWiFiSSID = getWiFiSSID(mWifiManager);
                                if (!connectedWiFiSSID.isEmpty() && connectedWiFiSSID.equalsIgnoreCase(wiFiHistoryFromPos.getCamera_ssid())) {
                                    homeViewModel.getSelectDeviceModelss().get(1).setIs_wifi_connected(WIFI_CONNECTED);
                                    adapter.notifyItemChanged(1);
                                } else {
                                    Collections.swap(homeViewModel.getSelectDeviceModelss(), addedIndex, 1);
                                    adapter.notifyItemMoved(addedIndex, 1);
                                }
                            }
                        }, 50);
                    }
                    Log.e(TAG, "subscribeUI: Index Of " + addedIndex);
                }
            }

            /*History Adapter*/
            if (!wiFiHistory.getCamera_ssid().contains("Add Device")) {
                if (selectedDeviceHistoryAdpter != null && homeViewModel.getDeviceHistoryModelss().contains(wiFiHistory)) {
                    int index = homeViewModel.getDeviceHistoryModelss().indexOf(wiFiHistory);
                    int is_wifi_connected = wiFiHistory.getIs_wifi_connected();
                    String ssidHistory = wiFiHistory.getCamera_ssid();
                    if (is_wifi_connected == 1) {
                        homeViewModel.getDeviceHistoryModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                        Collections.swap(homeViewModel.getDeviceHistoryModelss(), index, 0);
                        selectedDeviceHistoryAdpter.notifyItemMoved(index, 0);
                        selectedDeviceHistoryAdpter.notifyItemChanged(0);
                    } else {
                        RecyclerView.ViewHolder viewHolderForAdapterPosition = binding.selectedItemHistory.findViewHolderForAdapterPosition(index);
                        if (viewHolderForAdapterPosition != null) {
                            View itemView = viewHolderForAdapterPosition.itemView;
                            View viewById = itemView.findViewById(R.id.camera_icon);
                            CharSequence contentDescription = viewById.getContentDescription();
                            if (contentDescription != null) {
                                String strContentDescription = contentDescription.toString();
                                String[] split = strContentDescription.split(":");
                                String stateId = split[0];
                                String stateDescription = split[1];
                                String ssid = split[2];

//                                Log.e(TAG, "notifyAdded: " + "\nstateId: " + stateId + "\nstateDescription: " + stateDescription + "\nssid: " + ssid + "\nssidHistory: " + ssidHistory + "\nis_wifi_connected: " + is_wifi_connected + "\nindex: " + index);

                                if ((stateDescription.contains("WIFI_AVAILABLE") && is_wifi_connected != 0) || !ssid.equalsIgnoreCase(ssidHistory)) {
                                    homeViewModel.getDeviceHistoryModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                    selectedDeviceHistoryAdpter.notifyItemChanged(index);
                                } else if ((stateDescription.contains("WIFI_NOT_CONNECTED") && is_wifi_connected != -1) || !ssid.equalsIgnoreCase(ssidHistory)) {
                                    homeViewModel.getDeviceHistoryModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                    selectedDeviceHistoryAdpter.notifyItemChanged(index);
                                } else if (stateDescription.contains("WIFI_CONNECTED") || !ssid.equalsIgnoreCase(ssidHistory)) {
                                    homeViewModel.getDeviceHistoryModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                    selectedDeviceHistoryAdpter.notifyItemChanged(index);
                                }
                            } else {
                                homeViewModel.getDeviceHistoryModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                                selectedDeviceHistoryAdpter.notifyItemChanged(index);
                            }
                        } else {
                            homeViewModel.getDeviceHistoryModelss().get(index).setIs_wifi_connected(is_wifi_connected);
                            selectedDeviceHistoryAdpter.notifyItemChanged(index);
                        }
                    }
                } else {
                    homeViewModel.getDeviceHistoryModelss().add(wiFiHistory);
                    if (homeViewModel.getDeviceHistoryModelss().size() == 1) {
                        selectedDeviceHistoryAdpter.notifyItemInserted(0);
                    } else {
                        int addedIndex = homeViewModel.getDeviceHistoryModelss().indexOf(wiFiHistory);
                        if (addedIndex == 0) {
                            selectedDeviceHistoryAdpter.notifyItemInserted(0);
                        } else {
                            adapter.notifyItemInserted(addedIndex);
                            mHandler.postDelayed(() -> {
                                WiFiHistory wiFiHistory1 = homeViewModel.getDeviceHistoryModelss().get(0);
                                if (wiFiHistory1.getIs_wifi_connected() == 1 && addedIndex > 1) {
                                    Collections.swap(homeViewModel.getDeviceHistoryModelss(), addedIndex, 1);
                                    selectedDeviceHistoryAdpter.notifyItemMoved(addedIndex, 1);
                                } else {
                                    Collections.swap(homeViewModel.getDeviceHistoryModelss(), addedIndex, 0);
                                    selectedDeviceHistoryAdpter.notifyItemMoved(addedIndex, 0);
                                }
                            }, 50);
                        }
                        Log.e(TAG, "subscribeUI: Index Of " + addedIndex);
                    }
                }
            }
            homeViewModel.adapterNotifyState = HomeViewModel.AdapterNotifyState.NOTIFY_NONE;
        });
    }

    private void readManifestFile() {
        Manifest manifest = readManifest();
        CurrentFwVersion currentFwVersion = new CurrentFwVersion();
        homeViewModel.setManifest(manifest);
        homeViewModel.setCurrentFwVersion(currentFwVersion);
    }

    private void displayHomeScreen() {
        if (screenType == FW_UPDATE_SCREEN) {
            if (getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                // Safe to access views, update UI
                showHomeScreen();
            }
        }
        /*hide progressbar(when tcp socket connection timeout dialog dismiss but show progressbar) */
        hideProgressBar();
        hideWiFiDisconnectProgressbar();
    }

    private void resetFirmwareUpdateFailed() {
        Log.e(TAG, "resetFirmwareUpdateFailed");
        tcpConnectionViewModel.setFirmwareUpdateCompleted(false);// changed true to false
        homeViewModel.setAbnormalFwUpdate(false);
        homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                tcpConnectionViewModel.getRiscBootMode();
                break;
            case OPSIN:
                //here get opsin boot mode
                Log.e(TAG, "resetFirmwareUpdateFailed: opsin");
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"resetFirmwareUpdateFailed : NW_Digital");
                break;
        }
        tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
        tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
        hideProgressBar(); /*for this when select ok in dialog progressbar dismiss*/
    }

    private void registerObserverNightwaveOrOpsin() {
        resetToDefault();
        readManifestFile();
        switch (currentCameraSsid) {
            case NIGHTWAVE: {
                registerObserverNightwave();
                MainActivity activity = ((MainActivity) getActivity());
                if (activity != null) {
                    if (fwMode == MODE_NONE) {
                        if (mState != STATE_CONNECTED) {
                            tcpConnectionViewModel.connectSocket();
                        }

                        new Handler().postDelayed(() -> {
                            tcpConnectionViewModel.getRiscBootMode();
                            homeViewModel.setBootModeCheckCount(homeViewModel.getBootModeCheckCount() + 1);
                        }, 500);
                    }
                }
                Log.e(TAG, "subscribeUI: nightwave");
                break;
            }
            case OPSIN: {
                resetToDefault();
                Constants.setOpsinContainsOldWiFiFirmware(false);
                registerObserverOpsin();
                MainActivity activity = ((MainActivity) getActivity());
                if (activity != null) {
                    if (fwMode == MODE_NONE) {
                        new Handler().postDelayed(() -> {
                            homeViewModel.setBootModeCheckCount(homeViewModel.getBootModeCheckCount() + 1);
                            tcpConnectionViewModel.getOpsinImageState();//1st Check
                        }, 500);
                    }
                }
                //opsinInitialCommand();
                Log.e(TAG, "subscribeUI: opsin");
                break;
            }
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"registerObserverNightwaveOrOpsin : NW_Digital");
                if (!isSelectPopUpFwUpdateCheck && !isSelectPopUpInfo && !isSelectPopUpSettings) {
                MainActivity activity = ((MainActivity) getActivity());
                if (activity != null) {
                    if (fwMode == MODE_NONE) {
                        homeViewModel.getNavController().navigate(R.id.cameraSplashFragment);
                    }
                }
            }
                break;
        }
    }

    private void resetToDefault() {
        fwMode = MODE_NONE;
        homeViewModel.setOpsinFwUpdateSequenceIndex(0);
        homeViewModel.setBootModeCheckCount(0);
        tcpConnectionViewModel.setFirmwareUpdateCompleted(false);
        homeViewModel.setHasShowFwDialog(false);
        homeViewModel.setShowFwDialogNone(false);
        /* for this handle bottom info button press and then rotate screen show home screen so, avoid use this */
        if (screenType != INFO_SCREEN && screenType != GALLERY_SCREEN && screenType != POP_UP_SETTINGS_SCREEN) {
            HomeViewModel.screenType = HOME;
            showHomeScreen();
        }
    }

    private void registerObserverNightwave() {
        /*for this while rotate device again observe to be blocked on pop up settings screen*/
        if (screenType == POP_UP_SETTINGS_SCREEN || screenType == POP_UP_INFO_SCREEN) {
        } else {
            /* for this condition while loading video mode progress dialog blocked sent other commands while rotate deivce*/
            if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                tcpConnectionViewModel.observeRiscBootMode().observe(lifecycleOwner, observeBootMode);
                tcpConnectionViewModel.observeReleaseVersion().observe(lifecycleOwner, observeReleaseVersion);
                tcpConnectionViewModel.observeFpgaVersion().observe(lifecycleOwner, observeFpgeVersion);
                tcpConnectionViewModel.observeRisvVersion().observe(lifecycleOwner, observeRiscVersion);
                tcpConnectionViewModel.observeCameraInfo().observe(lifecycleOwner, observeCameraInfo);
            }
        }
        /* for this condition while loading video mode progress dialog blocked sent other commands while rotate deivce*/
        if (!popUpCameraSettingsViewModel.isConfirmationDialogShown() && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog())
            tcpConnectionViewModel.observeCameraName().observe(lifecycleOwner, observeCameraName);
    }

    private final Handler mHandlerForUpgrade = new Handler(Looper.getMainLooper());

    private void registerObserverOpsin() {
        if (screenType == POP_UP_SETTINGS_SCREEN || screenType == POP_UP_INFO_SCREEN) {
            tcpConnectionViewModel.getOpsinCameraName();
        }

        tcpConnectionViewModel.observeOpsinCameraErrorMessage().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(aBoolean -> {
            if (aBoolean) {
                /*
                It should be observed for only opsin
                 */
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    hideProgressBar();
                    showHomeScreen();
                    tcpConnectionViewModel.stopOpsinFWTimers();
                    if (bleWiFiViewModel.getOnAvailableSSID().equals("")) {
                        Log.e(TAG, "registerObserverOpsin: WIFI_DISCONNECTED");
                        notifyAdded(homeViewModel.getselectDeviceModelArrayList());
                        try {
                            bleWiFiViewModel.setNetworkCapability();
                            tcpConnectionViewModel.disconnectSocket();
                        } catch (StackOverflowError | Exception e) {
                            e.printStackTrace();
                        }
                        //Display WiFi connection broken here
                    } else {
                        if (fwMode != MODE_NONE) {
                            fwMode = MODE_NONE;
                            // HomeViewModel.screenType = HomeViewModel.ScreenType.HOME;
                            firmwareUpdateSequence.clear();

                            MainActivity activity = ((MainActivity) getActivity());
                            if (activity != null) {
                                showHomeScreen();
                                activity.showDialog = MainActivity.ShowDialog.OPSIN_RECYCLE_THE_CAMERA;
                                activity.showDialog("", getString(R.string.after_wifi_upgrade), null);
                            }
                        } else {
                            Log.e(TAG, "registerObserverOpsin: " + bleWiFiViewModel.getOnAvailableSSID());
                            MainActivity activity = ((MainActivity) getActivity());
                            if (activity != null) {
                                //Assume WiFi is fluctuated and displaying wifi powercycle the camera
                                firmwareUpdateSequence.clear();
                                if (fwMode == MODE_WIFI_DIALOG) {
                                    tcpConnectionViewModel.wifiUpdateComplete();
                                    tcpConnectionViewModel.stopPleaseWaitProgressUpdate();
                                } else {
                                    if (isAdded()) {
                                        activity.showDialog = MainActivity.ShowDialog.SOCKET_FAILED;
                                        activity.showDialog("", getString(R.string.socket_disconnected_messgae), null);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }));

        tcpConnectionViewModel.observeOpsinGetImageState().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if (object instanceof Byte) {
                    int opsinImageStateCount = getOpsinImageStateCount(homeViewModel.connectedSsid);
                    byte imageState = (byte) object;
                    // imageState = 0x03; // for this new seq opsin always golden
                    if (imageState == 0x01 || imageState == 0x02) {
                        if (opsinImageStateCount >= 2) {
                            storeOpsinImageStateCountWhenAttempting2ndTime(0);
                        }
                        startFirmwareUpgradeSequence("normal");
                    } else {
                        Log.d(TAG, "OPSIN_IMAGE_STATE: GOLDEN " + imageState);
                        showFwReBootModeDialog(requireContext().getString(R.string.display_user_contact_customer_care_service_opsin));
//                        if (opsinImageStateCount >= 2) {
//                            showFwReBootModeDialog(requireContext().getString(R.string.display_user_contact_customer_care_service_opsin));
//                            Log.e(TAG, "Display a dialog to the user to contact customer care service");
//                            homeViewModel.setAbnormalFwUpdate(false);
//                        } else {
//                            startFirmwareUpgradeSequence("golden");
//                        }
                    }

                    //2 Time Golden old sequence with full image update
//                    if (homeViewModel.getBootModeCheckCount() == 3) {
//                        startFirmwareUpgradeSequence("normal");
//                    } else {
//                        startFirmwareUpgradeSequence("golden");
//                    }

                    //Always Golden old sequence with full image update
//                    int opsinImageStateCount = getOpsinImageStateCount(connectedSsid);
//                    if (opsinImageStateCount >= 2) {
//                        showFwReBootModeDialog(requireContext().getString(R.string.display_user_contact_customer_care_service_opsin));
//                        Log.e(TAG, "Display a dialog to the user to contact customer care service");
//                        homeViewModel.setAbnormalFwUpdate(false);
//                    } else {
//                        startFirmwareUpgradeSequence("golden");
//                    }

                }
            }
        }));

        tcpConnectionViewModel.observeOpsinCameraRestart().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if ((boolean) object) {
                    Log.d(TAG, "observeOpsinCameraRestart: Camera restated ");
                    if (homeViewModel.getBootModeCheckCount() == 1) {
                        new Handler().postDelayed(() -> {
                            homeViewModel.setBootModeCheckCount(homeViewModel.getBootModeCheckCount() + 1);
                            tcpConnectionViewModel.getOpsinImageState();//2nd check
                        }, 5000);
                    }
                }
            }
        }));

        tcpConnectionViewModel.observeOpsinSetMasterVersion().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if (object instanceof CommandError) {
                    Log.d(TAG, "observeOpsinSetMasterVersion Error: " + ((CommandError) object).getError());
                }
                if ((boolean) object) {
                    Log.d(TAG, "observeOpsinSetMasterVersion:" + object);
                }
            }
        }));

        homeViewModel.getWifiCompleted().observe(lifecycleOwner, new EventObserver<>(object -> {
            if (object instanceof Boolean) {
                wifiCompleted();
            } else {
                otherFwCompleted(object.toString());
            }
        }));
        homeViewModel.getNotifyProgress().observe(lifecycleOwner, new EventObserver<>(object -> {
            if (object instanceof String) {
                String message = object.toString();
                if (message.equalsIgnoreCase("UPGRADE_DATA_COMPLETE_WAIT")) {
                    if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                        hideWiFiDisconnectProgressbar();
                        showWiFiDisconnectProgressbar();
                    } else {
                        Log.e(TAG, "registerObserverOpsin: not FW_UPDATE_SCREEN");
                    }
                } else if (message.equalsIgnoreCase("UPGRADE_DATA_COMPLETE_WAIT_CANCEL")) {
                    hideWiFiDisconnectProgressbar();
                } else if (message.equalsIgnoreCase("POWER_CYCLE_CAMERA")) {
                    powerCycleTheCamera();
                }
            }
        }));
        tcpConnectionViewModel.observerUpgradeComplete().observeForever(new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if(currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)){
                    String message = object.toString();
                    if (message.contains("UPGRADE_COMPLETE_OTHER")) {
                        Log.e(TAG, "UPGRADE_COMPLETE_OTHER: " + message + " " + fwMode);

                        if (fwMode != MODE_WIFI_DIALOG) {
                            homeViewModel.setWifiCompleted(message);
                        } else {
                            if (message.equalsIgnoreCase("WIFI:UPGRADE_COMPLETE_OTHER")) {
                                homeViewModel.setWifiCompleted(true);
                            } else {
                                Log.e(TAG, "registerObserverOpsin: Not in WIFI Mode");
                            }
                            fwMode = MODE_NONE;
                        }

                    } else if (message.equalsIgnoreCase("UPGRADE_COMPLETE_WIFI")) {
                        Log.e(TAG, "UPGRADE_COMPLETE_WIFI");
                        if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                            binding.loadingText.setText(getString(R.string.completed));
                            binding.firmwareCameraUpload.setVisibility(View.GONE);
                            binding.firmwareCameraUploadDone.setVisibility(VISIBLE);
                        }
                    } else if (message.equalsIgnoreCase("UPGRADE_DATA_COMPLETE_WAIT")) {
                        Log.e(TAG, "UPGRADE_DATA_COMPLETE_WAIT");
                        homeViewModel.setNotifyProgress("UPGRADE_DATA_COMPLETE_WAIT");
                    } else if (message.equalsIgnoreCase("UPGRADE_DATA_COMPLETE_WAIT_CANCEL")) {
                        Log.e(TAG, "UPGRADE_DATA_COMPLETE_WAIT_CANCEL");
                        homeViewModel.setNotifyProgress("UPGRADE_DATA_COMPLETE_WAIT_CANCEL");
                    } else if (message.equalsIgnoreCase("POWER_CYCLE_CAMERA")) {
                        homeViewModel.setNotifyProgress("POWER_CYCLE_CAMERA");
                    }
                }
            }
        }));
    }

    private void otherFwCompleted(String message) {
        String progressText = 100 + "%";
        binding.progressText.setText(progressText);
        binding.progressBar.setProgress(100);
        binding.loadingText.setText(getString(R.string.completed));
        binding.firmwareCameraUpload.setVisibility(View.GONE);
        binding.firmwareCameraUploadDone.setVisibility(VISIBLE);

        if (!homeViewModel.isAbnormalFwUpdate()) {
            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
            if (message.equalsIgnoreCase("RISCV:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("FPGA:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("BLE:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("RECOVERY:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(() -> {
                    homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                    startOpsinFwUpdate();
                }, 1000);
            }else if (message.equalsIgnoreCase("RECOVERY_WIFI_DISCONNECT:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("FULL:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("RISCV_FPGA:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("RISCV_OVERLAY:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
            } else if (message.equalsIgnoreCase("RESTART:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::startOpsinFwUpdate, 1000);
            } else if (message.equalsIgnoreCase("FACTORY:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(() -> {
                    if (tcpConnectionViewModel != null) {
                        firmwareUpdateSequence.clear();
                        bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), "");
                        tcpConnectionViewModel.setFirmwareUpdateCompleted(true);
                        homeViewModel.setAbnormalFwUpdate(false);
                        homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
                        tcpConnectionViewModel.getOpsinImageState();
                        tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
                        tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
                        homeViewModel.setHasShowFwDialog(false);
                        tcpConnectionViewModel.setSSIDSelectedManually(false);
                        showHomeScreen();
                    }
                }, 1000);
            } else if (message.equalsIgnoreCase("WAIT:UPGRADE_COMPLETE_OTHER")) {
                mHandlerForUpgrade.postDelayed(this::startOpsinFwUpdate, 1000);
            }
        } else {
            Log.e(TAG, "registerObserverOpsin: UPGRADE_COMPLETE_OTHER");
            int nextIndex = homeViewModel.getAbnormalFwUpdateSequenceIndex() + 1;
            homeViewModel.setAbnormalFwUpdateSequenceIndex(nextIndex);
            startAbnormalBehaviourFwUpdateSequence();
        }
    }

    private void wifiCompleted() {
        String progressText = 100 + "%";
        binding.progressText.setText(progressText);
        binding.progressBar.setProgress(100);
        binding.loadingText.setText(getString(R.string.completed));
        binding.firmwareCameraUpload.setVisibility(View.GONE);
        binding.firmwareCameraUploadDone.setVisibility(VISIBLE);
        mHandlerForUpgrade.postDelayed(this::powerCycleTheCamera, 1000);
    }

    private void powerCycleTheCamera() {
        Log.e(TAG, "POWER_CYCLE_CAMERA");
        HomeViewModel.hasShowPowerCycleDialog = true;
        if (isAdded()) {
            showOpsinCameraPowerOffDialog(getString(R.string.after_wifi_upgrade));
            bleWiFiViewModel.resetWifiState();
            /*Un register network call back*/
            bleWiFiViewModel.updateCameraConnectionState(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), WIFI_NOT_CONNECTED);
            homeViewModel.setCameraSwitched(false);
            connectivityManager.bindProcessToNetwork(null);
            if (activity != null) {
                activity.unregisterNetworkCallback();
            }
            networkCapability = null;
        }

        hideProgressBar();
        hideWiFiDisconnectProgressbar();
        homeViewModel.setHasShowFwDialog(false);
        fwMode = MODE_NONE;
        tcpConnectionViewModel.stopPleaseWaitProgressUpdate();

//        mHandlerForUpgrade.postDelayed(this::showHomeScreen, 1000);
        mHandlerForUpgrade.postDelayed(() -> {
            if (getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                // Safe to access views, update UI
                showHomeScreen();
            }
        },1000);
    }

    /* below method call inside get image type observer*/
    private void getOpsinProductVersionDetails() {
        /// showProgressBar();
        if (tcpConnectionViewModel != null) {
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                new Handler().postDelayed(() -> {
                    Log.e(TAG, "getOpsinProductVersionDetails: getOpsinCameraName");
                    tcpConnectionViewModel.getOpsinCameraName();
                }, 500);
            }
        }
        assert tcpConnectionViewModel != null;
        tcpConnectionViewModel.observeOpsinCameraVersionInfo().observe(lifecycleOwner, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            if (object != null) {
                if (object instanceof CommandError) {
                    Log.e(TAG, "observeOpsinCameraVersionInfo: " + ((CommandError) object).getError());
                } else {
                    Log.e(TAG, "observeOpsinCameraVersionInfo ");
                    opsinFirmwareUpgradeCheck((OpsinVersionDetails) object, false);
                    // for this observe camera info dialog view
                    cameraInfoViewModel.observeCameraVersionDetails(object);
                    /* for this observe version details on pop up window opsin settings*/
                    homeViewModel.observePopupCameraVersionDetails(object);
                    // homeViewModel.setOpsinVersionDetails((OpsinVersionDetails)object);
                }
            }
        }));
    }

    private final Observer<Object> observeCameraName = object -> {
        String cameraSsid = bleWiFiViewModel.getConnectedSsidFromWiFiManager().replace("\"", "");
        if (object != null) {
            hideProgressBar();
            if (object instanceof CommandError) {
                Log.e("TAG", "observeCameraName Error: " + ((CommandError) object).getError());
                boolean b = bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                if (b) {
                    /* for this show camera ssid on get camera name observe failed*/
                    fragmentPopUpLayoutBinding.popUpCameraName.setText(cameraSsid);
                    fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(cameraSsid);
                    if (homeViewModel.connectedSsid.contains(FILTER_STRING1) || homeViewModel.connectedSsid.contains(FILTER_STRING2) || homeViewModel.connectedSsid.contains(FILTER_STRING3)) {
                        fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                        fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                        bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    }

                    isUpdatingCameraName = false;
                    if (fwMode == MODE_NONE && !homeViewModel.isHasShowFwDialog()) {
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getReleasePkgVer(), 1000);
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getFPGAVersion(), 1500);
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getRiscVersion(), 2000);
                        new Handler().postDelayed(() -> tcpConnectionViewModel.getCameraInfo(), 2500);
                    }
                }
            } else {
                String response = object.toString();
                Log.e(TAG, "observeCameraName: " + response + " " + bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                /* for this show camera ssid on get camera name observe failed*/
                response = response.replace("\"", "");
                fragmentPopUpLayoutBinding.popUpCameraName.setText(response);
                fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(response);
                if (homeViewModel.connectedSsid.contains(FILTER_STRING1) || homeViewModel.connectedSsid.contains(FILTER_STRING2) || homeViewModel.connectedSsid.contains(FILTER_STRING3)) {
                    fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                    fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                }
                if (!response.equals("")) {
                    if (homeViewModel.connectedSsid.contains(FILTER_STRING1) || homeViewModel.connectedSsid.contains(FILTER_STRING2) || homeViewModel.connectedSsid.contains(FILTER_STRING3)) {
                        boolean b = bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                        if (b) {
                            if (!response.trim().matches(regex)) {
                                hideProgressBar();
                                if (tcpConnectionViewModel != null) {
                                    Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                                    // fort his condition after save button press and rotate device avoid to show again this dialog
                                    if (value != null && value == Constants.STATE_CONNECTED && !isUpdatingCameraName) {
                                        Log.e(TAG, "observeCameraName exsist onChanged1: " + response.trim() + " " + isUpdatingCameraName + " " + hasAlreadyAddedInDialogTag);
                                        showSpecialCharacterDialog(getString(R.string.un_supported_character_message));
                                    }
                                }
                            } else {
                                if (fwMode == MODE_NONE && !homeViewModel.isHasShowFwDialog() && screenType != POP_UP_INFO_SCREEN) { //&& screenType != POP_UP_SETTINGS_SCREEN) {
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getReleasePkgVer(), 1000);
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getFPGAVersion(), 1500);
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getRiscVersion(), 2000);
                                    new Handler().postDelayed(() -> tcpConnectionViewModel.getCameraInfo(), 2500);
                                    Log.e(TAG, "observeCameraName1: " + response.trim());
                                    bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), response.trim());
                                }
                            }
                        }
                    }
                } else {
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                        /* for this show camera ssid on get camera name observe failed*/
                        fragmentPopUpLayoutBinding.popUpCameraName.setText(cameraSsid);
                        fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(cameraSsid);
                        if (homeViewModel.connectedSsid.contains(FILTER_STRING1) || homeViewModel.connectedSsid.contains(FILTER_STRING2) || homeViewModel.connectedSsid.contains(FILTER_STRING3)) {
                            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
                            bleWiFiViewModel.updateCameraName(bleWiFiViewModel.getConnectedSsidFromWiFiManager(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                        }
//                        Log.e(TAG, "observeCameraName2: " + bleWiFiViewModel.getConnectedSsidFromWiFiManager() + " " + connectedSsid);
                    }
                }
            }
        }
    };
    boolean ifWeAreSimulatingGolden = false;
    private final Observer<Object> observeBootMode = bootMode -> {
        if (bootMode instanceof CommandError) {
            Log.e(TAG, "observeBootMode: " + ((CommandError) bootMode).getError());
            hideProgressBar();
            Log.e(TAG, "hideProgressBar:9 ");

            if (HomeViewModel.screenType != HomeViewModel.ScreenType.INFO_SCREEN && HomeViewModel.screenType != HomeViewModel.ScreenType.ADD_SCREEN && HomeViewModel.screenType != HomeViewModel.ScreenType.HOME) {
                new Handler().postDelayed(() -> {
                    homeViewModel.setFwUpdateSequenceIndex(0);
                    homeViewModel.setBootModeCheckCount(0);
                    showHomeScreen();
                    tcpConnectionViewModel.setSSIDSelectedManually(false);
                }, 2000);
            }
        } else {
            if (!homeViewModel.isHasShowFwDialog() && fwMode == MODE_NONE) {
                    startFirmwareUpgradeSequence((String) bootMode);
            }
        }
    };

    private void startFirmwareUpgradeSequence(String bootMode) {
        Log.e(TAG, "observeBootMode: " + bootMode + " getBootModeCheckCount: " + homeViewModel.getBootModeCheckCount());
        try {

            /*
             * One times golden
             * 1 time golden --> RESET FPGA, 2 time Golden --> RESET and RISCV Update
             * */
//            if (homeViewModel.getBootModeCheckCount() == 1 && homeViewModel.getFwCompletedCount() == 0) {
//                bootMode = getString(R.string.is_golden);
//                if (((String) bootMode).equalsIgnoreCase(getString(R.string.is_golden))) {
//                    ifWeAreSimulatingGolden = true;//Even when we are simulating Golden, if camera returns boot mode as golden, then we should pass true to reset fpga
//                } else {
//                    ifWeAreSimulatingGolden = false;
//                }
//            }

            /*
             * Two times golden
             * */
//            if (homeViewModel.getFwCompletedCount() == 0) {
//                if (homeViewModel.getBootModeCheckCount() == 1 || homeViewModel.getBootModeCheckCount() == 2) {
//                    bootMode = getString(R.string.is_golden);
//                    if (((String) bootMode).equalsIgnoreCase(getString(R.string.is_golden))) {
//                        ifWeAreSimulatingGolden = true;//Even when we are simulating Golden, if camera returns boot mode as golden, then we should pass true to reset fpga
//                    } else {
//                        ifWeAreSimulatingGolden = false;
//                    }
//                }
//            } else if (homeViewModel.getFwCompletedCount() == 1 && homeViewModel.getBootModeCheckCount() == 2) {
//                bootMode = getString(R.string.is_golden);
//            }


            /*
             * Always Golden*/
//            bootMode = getString(R.string.is_golden);
//            if (((String) bootMode).equalsIgnoreCase(getString(R.string.is_golden))) {
//                ifWeAreSimulatingGolden = true;//Even when we are simulating Golden, if camera returns boot mode as golden, then we should pass true to reset fpga
//            } else {
//                ifWeAreSimulatingGolden = false;
//            }


            if (homeViewModel.getCurrentFwVersion() != null && bootMode != null) {
                homeViewModel.getCurrentFwVersion().setWifiBootMode(bootMode);
                /*Start FW Sequence*/
                if (tcpConnectionViewModel.isFirmwareUpdateCompleted()) {
                    homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_COMPLETED;
                    if (bootMode.equalsIgnoreCase(requireContext().getString(R.string.is_golden))) {
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                if (homeViewModel.getFwCompletedCount() == 1 && !homeViewModel.isHasShowFwDialog()) {
                                    showFwReBootModeDialog(requireContext().getString(R.string.display_power_cycle_camera));
                                    Log.e(TAG, "Display a dialog to the user by asking to power cycle the camera");
                                    handleComplete();
                                } else if (homeViewModel.getFwCompletedCount() >= 2 && !homeViewModel.isHasShowFwDialog()) {
                                    showFwReBootModeDialog(requireContext().getString(R.string.display_user_contact_customer_care_service));
                                    Log.e(TAG, "Display a dialog to the user to contact customer care service");
                                    homeViewModel.setAbnormalFwUpdate(false);
                                    handleComplete();
                                }
                                break;
                            case OPSIN:
                                //3rd Check :Contact customer care service if 3rd check response is Golden
                                if (homeViewModel.getBootModeCheckCount() == 3 && !homeViewModel.isHasShowFwDialog()) {//Need to check whether 3rd image state response received here
                                    showFwReBootModeDialog(requireContext().getString(R.string.display_user_contact_customer_care_service_opsin));
                                    Log.e(TAG, "Display a dialog to the user to contact customer care service");
                                    homeViewModel.setAbnormalFwUpdate(false);
                                    handleComplete();
                                } else {
                                    Log.d(TAG, "startFirmwareUpgradeSequence opsin: completed");
                                    handleComplete();
                                }
                                break;
                            case NIGHTWAVE_DIGITAL:
                                Log.e(TAG,"isFirmwareUpdateCompleted : NW_Digital");
                                break;
                        }
                    } else {
                        hideProgressBar();
                        switch (currentCameraSsid) {
                            case NIGHTWAVE:
                                if (screenType != GALLERY_SCREEN && screenType != INFO_SCREEN) {
                                    new Handler().postDelayed(() -> {
                                        Log.e(TAG, "isHasShowFwDialog: " + homeViewModel.isHasShowFwDialog());
                                        if (!homeViewModel.isHasShowFwDialog()) {
                                            homeViewModel.setFwUpdateSequenceIndex(0);
                                            homeViewModel.setBootModeCheckCount(0);
                                            hideProgressBar();
                                            showHomeScreen();
                                        }
                                    }, 500);

                                    homeViewModel.setAbnormalFwUpdate(false);
                                    tcpConnectionViewModel.setFirmwareUpdateChecked(false);
                                    getAllFwDetails();
                                }
                                break;
                            case OPSIN:
                                //Do recovery in 3rd check if image state response is normal
                                if (homeViewModel.getBootModeCheckCount() == 3) {
                                    homeViewModel.setAbnormalFwUpdateSequenceIndex(0);
                                    startAbnormalBehaviourFwUpdateSequence();
                                } else {
                                    new Handler().postDelayed(() -> {
                                        Log.e(TAG, "isHasShowFwDialog: " + homeViewModel.isHasShowFwDialog());
                                        if (!homeViewModel.isHasShowFwDialog()) {
                                            homeViewModel.setOpsinFwUpdateSequenceIndex(0);
                                            homeViewModel.setBootModeCheckCount(0);
                                            showHomeScreen();
                                        }
                                    }, 500);

                                    homeViewModel.setAbnormalFwUpdate(false);
                                    tcpConnectionViewModel.setFirmwareUpdateChecked(false);
                                    getAllFwDetails();
                                }
                                break;
                            case NIGHTWAVE_DIGITAL:
                                //golden
                                break;
                        }
                    }
                } else {
                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            // Golden mode
                            if (bootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                // As per new requirement
                                hideProgressBar();
                                if (!homeViewModel.hasShowRecoverModeDialog()) {
                                    showFwRecoverDialogMessage(requireContext().getString(R.string.display_user_recover_mode));
                                    homeViewModel.setHasShowRecoverModeDialog(true);
                                    Log.e(TAG, "Display dialog to the user that explains about recover mode with okay button");
                                }
                            /*    if (homeViewModel.getBootModeCheckCount() == 1) {
                                    showProgressBar();
                                    tcpConnectionViewModel.resetFPGA(!ifWeAreSimulatingGolden);
                                    new Handler().postDelayed(() -> {
                                        homeViewModel.setBootModeCheckCount(homeViewModel.getBootModeCheckCount() + 1);
                                        tcpConnectionViewModel.getRiscBootMode();
                                        hideProgressBar();
                                    }, 15000);
                                } else {
                                    if (!homeViewModel.hasShowRecoverModeDialog()) {
                                        showFwRecoverDialogMessage(requireContext().getString(R.string.display_user_recover_mode));
                                        homeViewModel.setHasShowRecoverModeDialog(true);
                                        Log.e(TAG, "Display dialog to the user that explains about recover mode with okay button");
                                    }
                                }*/

                                HomeViewModel.isGoldenUpdate = true;
                            } else if (bootMode.equalsIgnoreCase(getString(R.string.is_normal))) {//2
                                getAllFwDetails();
                                HomeViewModel.isGoldenUpdate = false;
                            }
                            break;
                        case OPSIN:
                            if (bootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                if (homeViewModel.getBootModeCheckCount() == 1) {//1st check response
                                    tcpConnectionViewModel.restartOpsinCamera();
                                } else if (homeViewModel.getBootModeCheckCount() == 2) {//2nd check response
                                    if (!homeViewModel.hasShowRecoverModeDialog()) {
                                        showFwRecoverDialogMessage(requireContext().getString(R.string.display_user_recover_mode_opsin));
                                        homeViewModel.setHasShowRecoverModeDialog(true);
                                        Log.e(TAG, "Display dialog to the user that explains about recover mode with okay button");
                                    }
                                }
                            } else if (bootMode.equalsIgnoreCase(requireContext().getString(R.string.is_normal))) {
                                Log.e(TAG, "startFirmwareUpgradeSequence: isNormal");
                                getAllFwDetails();
                            }
                            hideProgressBar();
                            break;
                        case NIGHTWAVE_DIGITAL:
                            //
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleComplete() {
        if (tcpConnectionViewModel != null) {
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                    String appRelease = homeViewModel.getManifest().getVersions().getRelease();
                    tcpConnectionViewModel.setTopReleaseVersion(appRelease);
                }
                tcpConnectionViewModel.setFirmwareUpdateCompleted(false);

                tcpConnectionViewModel.disconnectSocket();
                tcpConnectionViewModel.resetSocketState();
            }
        }

        new Handler().postDelayed(() -> {
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    homeViewModel.setFwUpdateSequenceIndex(0);
                    break;
                case OPSIN:
                    homeViewModel.setOpsinFwUpdateSequenceIndex(0);
                    break;
                case NIGHTWAVE_DIGITAL:
                    break;
            }
            homeViewModel.setBootModeCheckCount(0);
            // hideProgressBar();
            tcpConnectionViewModel.setSSIDSelectedManually(false);
            showHomeScreen();
        }, 2000);
    }

    private void getAllFwDetails() {
        if (!tcpConnectionViewModel.isFirmwareUpdateChecked()) {
            //  showProgressBar();
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    getAllFwVersionDetails();
                    break;
                case OPSIN:
                    Log.e(TAG, "startFirmwareUpgradeSequence: Opsin command");
                    getOpsinProductVersionDetails();
                    break;
                case NIGHTWAVE_DIGITAL:
                    //
                    break;
            }
        }
    }

    private void getAllFwVersionDetails() {
        if (tcpConnectionViewModel != null) {
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                new Handler().postDelayed(() -> {
                    if (fwMode == MODE_NONE) {
                        tcpConnectionViewModel.getCameraName();
                    }
                }, 500);
            } else {
                hideProgressBar();
//                showMessage(getString(R.string.please_try_again));
            }
        }
    }

    private final Observer<Object> observeReleaseVersion = aString -> {
        if (aString != null) {
            if (aString instanceof CommandError) {
                Log.e(TAG, "observeReleaseVersion: " + ((CommandError) aString).getError());
                hideProgressBar();
            } else {
                Log.e(TAG, "observeReleaseVersion: " + aString);
                if (homeViewModel.getCurrentFwVersion() != null) {
                    showProgressBar();
                    homeViewModel.getCurrentFwVersion().setReleaseVersion((String) aString);
                }
            }
        }
    };

    private final Observer<Object> observeFpgeVersion = aString -> {
        if (aString != null) {
            if (aString instanceof CommandError) {
                hideProgressBar();
                Log.e(TAG, "observeFpgaVersion: " + ((CommandError) aString).getError());
            } else {
                Log.e(TAG, "observeFpgaVersion: " + aString);
                if (homeViewModel.getCurrentFwVersion() != null) {
                    showProgressBar();
                    homeViewModel.getCurrentFwVersion().setFpga((String) aString);
                }
            }
        }
    };

    private final Observer<Object> observeRiscVersion = aString -> {
        if (aString != null) {
            if (aString instanceof CommandError) {
                hideProgressBar();
                Log.e(TAG, "observeRisvVersion: " + ((CommandError) aString).getError());
            } else {
                Log.e(TAG, "observeRisvVersion: " + aString);
                if (homeViewModel.getCurrentFwVersion() != null) {
                    showProgressBar();
                    homeViewModel.getCurrentFwVersion().setRiscv((String) aString);
                }
            }
        }
    };

    public boolean onlyAlphabets(String str, int n) {

        // Return false if the string
        // has empty or null
        if (str == null || str == "") {
            return false;
        }

        // Traverse the string from
        // start to end
        boolean isCharFound = false;
        for (int i = 0; i < n; i++) {
            // Check if the specified
            // character is not a letter then
            // return false,
            // else return true
            if (Character.isLetter(str.charAt(i))) {
                isCharFound = true;
            }
        }
        return isCharFound;
    }

    private final Observer<Object> observeCameraInfo = aString -> {
        if (aString != null) {
            if (aString instanceof CommandError) {
                Log.e(TAG, "observeCameraInfo: " + ((CommandError) aString).getError());
                hideProgressBar();
                if (homeViewModel.connectedSsid != null) {
                    fragmentPopUpLayoutBinding.popUpCameraName.setText(homeViewModel.connectedSsid);
                    fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(homeViewModel.connectedSsid);
                }
            } else {
                nightWaveFirmwareUpdateCheck(aString);
            }
        }
    };

    private void nightWaveFirmwareUpdateCheck(Object aString) {
        String response = aString.toString();
        Log.e(TAG, "nightWaveFirmwareUpdateCheck RESPONSE : " + response);
        int length = response.length();
        String cameraName = response.substring(0, 32);
        String cameraModel = response.substring(32, 64);
        String releaseVersion = response.substring(64, 80);
        String wifiRtosVersion = response.substring(80, 128);
        String wifiBootVersion = response.substring(128, 176);
        String wifiMac = response.substring(176, 194);
        String bleMac = response.substring(194, 212);
        String ssid = response.substring(212, length);
        if (homeViewModel.connectedSsid != null)
            homeViewModel.connectedSsid = homeViewModel.connectedSsid.replace("\"", "");
        String selectedCameraSsid = ssid.replace("\"", "");
        String cameraReleaseVersion = null, cameraRISCV = null, cameraWIFI_rtos = null, cameraWIFI_boot = null, cameraFPGA = null, cameraBootMode = null, appRelease = null, appRISCV = null, appWIFI_rtos = null, appWIFI_boot = null, appFPGA = null, release_notes = null;
        if (homeViewModel.connectedSsid != null) {
            String connectedSsid1 = ssid.replace("\"", "");
            String text = !cameraName.trim().equals("") ? cameraName : connectedSsid1;
            fragmentPopUpLayoutBinding.popUpCameraName.setText(text);
            fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText(text);
        }

        /* for this show connected icon */
        if (selectedCameraSsid.contains(FILTER_STRING1) || selectedCameraSsid.contains(FILTER_STRING2) || selectedCameraSsid.contains(FILTER_STRING3)) {
            fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
            fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.ic_nw_analog_connected);
            try {
                fragmentPopUpLayoutBinding.popUpCameraName.setText((cameraName.trim() != null && !cameraName.trim().isEmpty()) ? cameraName.trim() : selectedCameraSsid);
                fragmentPopUpLayoutBinding.popUpSettingsCameraName.setText((cameraName.trim() != null && !cameraName.trim().isEmpty()) ? cameraName.trim() : selectedCameraSsid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*for this condition while first select fw update nightwave camera and select opsin camera and again select another nightwave camera with no fw update ,now show previous connected nightwave camera fw update dialog so,avoid that usecase*/
        if (homeViewModel.connectedSsid != null && homeViewModel.connectedSsid.equals(selectedCameraSsid.trim())) {
            showProgressBar();
            bleWiFiViewModel.updateCameraName(selectedCameraSsid, cameraName.trim());

            if (homeViewModel.getCurrentFwVersion() != null) {
                homeViewModel.getCurrentFwVersion().setWifiRtos(wifiRtosVersion);
                homeViewModel.getCurrentFwVersion().setWifiBoot(wifiBootVersion);
                homeViewModel.getCurrentFwVersion().setCameraName(cameraName);
                homeViewModel.getCurrentFwVersion().setWifiMac(wifiMac);
                homeViewModel.getCurrentFwVersion().setSsid(ssid);
            }

            if (homeViewModel.getCurrentFwVersion() != null) {
                cameraReleaseVersion = homeViewModel.getCurrentFwVersion().getReleaseVersion();
                cameraFPGA = homeViewModel.getCurrentFwVersion().getFpga();
                cameraRISCV = homeViewModel.getCurrentFwVersion().getRiscv();
                cameraWIFI_rtos = homeViewModel.getCurrentFwVersion().getWiFiRtos();
                cameraWIFI_boot = homeViewModel.getCurrentFwVersion().getWifiBoot();
                cameraBootMode = homeViewModel.getCurrentFwVersion().getWifiBootMode();
            }

            if (homeViewModel.getManifest() != null) {
                appRelease = homeViewModel.getManifest().getVersions().getRelease();
                appFPGA = homeViewModel.getManifest().getVersions().getFpga();
                appRISCV = homeViewModel.getManifest().getVersions().getRiscv();
                appWIFI_rtos = homeViewModel.getManifest().getVersions().getWiFiRtos();
                appWIFI_boot = homeViewModel.getManifest().getVersions().getwIFI_BOOT();
                release_notes = homeViewModel.getManifest().getReleaseNotes();
            }

            if (cameraReleaseVersion != null)
                cameraReleaseVersion = cameraReleaseVersion.trim();

            if (cameraFPGA != null) cameraFPGA = cameraFPGA.trim();

            if (cameraRISCV != null) cameraRISCV = cameraRISCV.trim();

            if (cameraWIFI_rtos != null) cameraWIFI_rtos = cameraWIFI_rtos.trim();

            if (cameraWIFI_boot != null) cameraWIFI_boot = cameraWIFI_boot.trim();

            if (cameraBootMode != null) cameraBootMode = cameraBootMode.trim();


            if (appRelease != null) appRelease = appRelease.trim();

            if (appFPGA != null) appFPGA = appFPGA.trim();

            if (appRISCV != null) appRISCV = appRISCV.trim();

            if (appWIFI_rtos != null) appWIFI_rtos = appWIFI_rtos.trim();

            if (appWIFI_boot != null) appWIFI_boot = appWIFI_boot.trim();

            if (release_notes != null) release_notes = release_notes.trim();


            Log.e(TAG, "observeCameraInfo: " + "\nappRelease :" + appRelease + "\nappRISCV :" + appRISCV + "\nappWIFI_rtos :" + appWIFI_rtos + "\nappWIFI_boot :" + appWIFI_boot + "\nappFPGA :" + appFPGA);

            Log.e(TAG, "observeCameraInfo: " + "\ncameraReleaseVersion:" + cameraReleaseVersion + "\ncameraBootMode: " + cameraBootMode
                    // + "\ncameraRISCV: " + cameraRISCV
                    + "\ncameraWIFI_rtos: " + cameraWIFI_rtos + "\ncameraWIFI_boot: " + cameraWIFI_boot + "\ncameraFPGA: " + cameraFPGA);

            try {
                String[] part;
                if (cameraRISCV != null) {
                    cameraRISCV = cameraRISCV.replaceAll("([A-Za-z])", "");
                    float f2 = 0;
                    try {
                        f2 = Float.parseFloat(cameraRISCV);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (f2 > USB_VIDEO_NOT_SUPPORTED_VERSION) {
                        Log.d(TAG, "is new firmware version: " + screenType.name());
                        CameraViewModel.setIsNewFirmware(true);
                        setCameraSettingAdapter(); /* for this update tab layout ui*/
                    } else {
                        Log.d(TAG, "is old firmware version: " + screenType.name());
                        CameraViewModel.setIsNewFirmware(false);
                        setCameraSettingAdapter();
                    }
                }

            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }

            Log.e(TAG, "CHECK " + tcpConnectionViewModel.isFirmwareUpdateChecked() + " " + " " + tcpConnectionViewModel.isSSIDSelectedManually());
            if (tcpConnectionViewModel.isSSIDSelectedManually()) {
                boolean isFpgaUpdateAvailable = false;
                boolean isRiscUpdateAvailable = false;
                boolean isWiFiRtosUpdateAvailable = false;
                ArrayList<String> sequence = homeViewModel.getManifest().getSequence();

                if (appFPGA != null && cameraFPGA != null && !cameraFPGA.equals("") && !appFPGA.equals(cameraFPGA) && !appFPGA.equals("")) {
                    try {
                        int app_fpga = Integer.parseInt(appFPGA, 16); //Converting Hex into Integer
                        int camera_fpga = Integer.parseInt(cameraFPGA, 16); //Converting Hex into Integer
                        if (app_fpga > camera_fpga) {
                            Log.e(TAG, "app_fpga > camera_fpga");
                            if (sequence.contains("FPGA")) {
                                isFpgaUpdateAvailable = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (appRISCV != null && cameraRISCV != null && !cameraRISCV.equals("") && !appRISCV.equals(cameraRISCV) && !appRISCV.equals("")) {
                    try {
                        float f1 = Float.parseFloat(appRISCV);
                        float f2 = Float.parseFloat(cameraRISCV);
                        if (Float.compare(f1, f2) == 0) {
                            Log.e(TAG, "appRISCV = cameraRISCV");
                        } else if (Float.compare(f1, f2) < 0) {
                            Log.e(TAG, "appRISCV < cameraRISCV");
                        } else {
                            Log.e(TAG, "appRISCV > cameraRISCV");
                            if (sequence.contains("RISCV")) {
                                isRiscUpdateAvailable = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (appWIFI_rtos != null && cameraWIFI_rtos != null && !cameraWIFI_rtos.equals("") && !appWIFI_rtos.equals(cameraWIFI_rtos) && !appWIFI_rtos.equals("")) {
                    try {
                        String[] vAppWIFI_RTOS = appWIFI_rtos.split("-");
                        String[] vCameraWIFI_rtos = cameraWIFI_rtos.split("-");

                        if (vCameraWIFI_rtos.length == 2 && vAppWIFI_RTOS.length == 2) {
                            String[] vAppLeft = vAppWIFI_RTOS[0].split("\\.");
                            String[] vAppRight = vAppWIFI_RTOS[1].split("\\.");

                            String[] vCameraLeft = vCameraWIFI_rtos[0].split("\\.");
                            String[] vCameraRight = vCameraWIFI_rtos[1].split("\\.");

                            String modified = cameraWIFI_rtos.replaceAll("\\.", "");
                            String modified1 = modified.replaceAll("-", "");
                            boolean matches = onlyAlphabets(modified1, modified1.length());

                            if (!matches) {
                                if (sequence.contains("WIFI_RTOS")) {
                                    int appRightLength = vAppRight.length;
                                    int cameraRightLength = vCameraRight.length;

                                    if (!vAppWIFI_RTOS[0].equals(vCameraWIFI_rtos[0])) {
                                        if (vAppLeft.length == vCameraLeft.length) {
                                            for (int i = 0; i < vAppLeft.length; i++) {
                                                int appValue = Integer.parseInt(vAppLeft[i]);
                                                int cameraValue = Integer.parseInt(vCameraLeft[i]);
                                                if (appValue == cameraValue) {
                                                    Log.e(TAG, "RTOS LEFT IS EQUAL");
                                                } else if (appValue > cameraValue) {
                                                    isWiFiRtosUpdateAvailable = true;
                                                    Log.e(TAG, "RTOS LEFT IS GREATER");
                                                    break;
                                                } else {
                                                    Log.e(TAG, "RTOS LEFT IS LESSER");
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (!vAppWIFI_RTOS[1].equals(vCameraWIFI_rtos[1]) && !isWiFiRtosUpdateAvailable) {
                                        if (vAppRight.length == vCameraRight.length) {
                                            for (int i = 0; i < vAppRight.length; i++) {
                                                int appValue = Integer.parseInt(vAppRight[i]);
                                                int cameraValue = Integer.parseInt(vCameraRight[i]);
                                                if (appValue == cameraValue) {
                                                    Log.e(TAG, "RTOS RIGHT IS EQUAL");
                                                } else if (appValue > cameraValue) {
                                                    isWiFiRtosUpdateAvailable = true;
                                                    Log.e(TAG, "RTOS RIGHT IS GREATER");
                                                    break;
                                                } else {
                                                    Log.e(TAG, "RTOS RIGHT IS LESSER");
                                                    break;
                                                }
                                            }
                                        } else if (appRightLength > cameraRightLength) {
                                            boolean isAppGreater = false;
                                            boolean isAppLesser = false;
                                            for (int i = 0; i < appRightLength; i++) {
                                                int appValue = Integer.parseInt(vAppRight[i]);
                                                int cameraValue = 0;

                                                if (i < cameraRightLength) {
                                                    cameraValue = Integer.parseInt(vCameraRight[i]);
                                                    if (appValue > cameraValue) {
                                                        isWiFiRtosUpdateAvailable = true;
                                                        isAppGreater = true;
                                                        break;
                                                    } else if (appValue == cameraValue) {
                                                        Log.e(TAG, "FOURTH DIGIT " + appValue + " " + cameraValue);
                                                    } else {
                                                        isAppLesser = true;
                                                        break;
                                                    }
                                                } else if (!isWiFiRtosUpdateAvailable) {
                                                    if (!isAppGreater && !isAppLesser) {
                                                        isWiFiRtosUpdateAvailable = true;
                                                    }
                                                }
                                            }
                                        } else {
                                            for (int i = 0; i < cameraRightLength; i++) {
                                                int cameraValue = Integer.parseInt(vCameraRight[i]);
                                                int appValue = 0;
                                                if (i < appRightLength) {
                                                    appValue = Integer.parseInt(vAppRight[i]);
                                                    if (appValue == cameraValue) {
                                                        Log.e(TAG, "RTOS RIGHT IS EQUAL");
                                                    } else if (appValue > cameraValue) {
                                                        isWiFiRtosUpdateAvailable = true;
                                                        Log.e(TAG, "RTOS RIGHT IS GREATER");
                                                        break;
                                                    } else {
                                                        Log.e(TAG, "RTOS RIGHT IS LESSER");
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                }
                if (cameraReleaseVersion == null) {
                    cameraReleaseVersion = "UNKNOWN";
                }

                if (!cameraReleaseVersion.equals(appRelease)) {
                    if (isFpgaUpdateAvailable || isRiscUpdateAvailable || isWiFiRtosUpdateAvailable) {
                        if (!tcpConnectionViewModel.isFirmwareUpdateChecked()) {
                            if (/*!tcpConnectionViewModel.isFirmwareSkipped()*/activity != null && !activity.getIsFirmwareSkipped(homeViewModel.connectedSsid)) {
                                if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                    // for here added '!homeViewModel.hasShowRecoverModeDialog()' this method to avoid to show fw dialog because while showing software recover dialog
                                    if (activity.isInMultiWindowMode()) {
                                        Toast.makeText(activity, R.string.firmware_dialog_in_split_screen, Toast.LENGTH_LONG).show();
                                        hideProgressBar();
                                    } else if (!homeViewModel.isHasShowFwDialog() && !homeViewModel.hasShowRecoverModeDialog() && fwMode == MODE_NONE && HomeViewModel.screenType != FW_UPDATE_SCREEN) {/*for this condition show firmware versions dialog only one time create when rotate device not create */
                                        Log.e(TAG, "nightWaveFirmwareUpdateCheck: 2");
                                        if (!HomeViewModel.isGoldenUpdate) {
                                            navigateToNextScreen(popUpNone);
                                            cameraInfoViewModel.setFirmwareDialogNeeded(true);
                                        }
                                        /*showFwUpdateDialog(release_notes);
                                        homeViewModel.setHasShowFwDialog(true);
                                        homeViewModel.setShowFwDialogNone(true);*/
                                    }
                                }
                            } else {
                                try {
                                    if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                        if (!homeViewModel.hasShowRecoverModeDialog()) {
                                            navigateToNextScreen(popUpNone);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            Log.e(TAG, "ELSE " + tcpConnectionViewModel.isFirmwareUpdateChecked());
                            hideProgressBar();
                        }
                    } else {
                        try {
                            if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                if (!homeViewModel.hasShowRecoverModeDialog()) {
                                    tcpConnectionViewModel.setTopReleaseVersion(appRelease);
                                    navigateToNextScreen(popUpNone);

                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        hideProgressBar();
                    }
                } else if (isFpgaUpdateAvailable || isRiscUpdateAvailable || isWiFiRtosUpdateAvailable) {
                    if (!tcpConnectionViewModel.isFirmwareUpdateChecked()) {
                        if (/*!tcpConnectionViewModel.isFirmwareSkipped()*/activity != null && !activity.getIsFirmwareSkipped(homeViewModel.connectedSsid)) {
                            // below for this camera in only golden image while checking that time device to rotate to show fw update dialog. so avoid use this condition
                            if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                if (activity.isInMultiWindowMode()) {
                                    Toast.makeText(activity, R.string.firmware_dialog_in_split_screen, Toast.LENGTH_LONG).show();
                                    hideProgressBar();
                                } else if (!homeViewModel.isHasShowFwDialog() && !homeViewModel.hasShowRecoverModeDialog() && fwMode == MODE_NONE && HomeViewModel.screenType != FW_UPDATE_SCREEN) {/*for this condition show firmware versions dialog only one time create when rotate device not create */
                                    // for here added '!homeViewModel.hasShowRecoverModeDialog()' this method to avoid to show fw dialog because while showing software recover dialog
                                    Log.e(TAG, "nightWaveFirmwareUpdateCheck: 3");
                                    if (!HomeViewModel.isGoldenUpdate) {
                                        navigateToNextScreen(popUpNone);
                                        cameraInfoViewModel.setFirmwareDialogNeeded(true);
                                    }
                                    /*showFwUpdateDialog(release_notes);
                                    homeViewModel.setHasShowFwDialog(true);
                                    homeViewModel.setShowFwDialogNone(true);*/
                                }
                            }
                        } else {
                            if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                                if (!homeViewModel.hasShowRecoverModeDialog()) {
                                    Log.e(TAG, "nightWaveFirmwareUpdateCheck1: " + screenType.name());
                                    navigateToNextScreen(popUpNone);
                                    cameraInfoViewModel.setFirmwareDialogNeeded(false);
                                }
                            }
                            hideProgressBar();
                        }
                    } else {
                        Log.e(TAG, "ELSE ELSE " + tcpConnectionViewModel.isFirmwareUpdateChecked());
                        hideProgressBar();
                    }
                } else {
                    try {
                        if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                            if (!homeViewModel.hasShowRecoverModeDialog()) {
                                Log.e(TAG, "nightWaveFirmwareUpdateCheck: " + isSelectPopUpSettings + "/" + isSelectPopUpInfo);
                                navigateToNextScreen(popUpNone);
                                cameraInfoViewModel.setFirmwareDialogNeeded(false);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    hideProgressBar();
                }
            }
            hideProgressBar();
        } else {
            if (cameraBootMode != null) cameraBootMode = cameraBootMode.trim();
            Log.e(TAG, "nightWaveFirmwareUpdateCheck: connected ssid null" + cameraBootMode);
            try {
                if (cameraBootMode != null && !cameraBootMode.equalsIgnoreCase(getString(R.string.is_golden))) {
                    navigateToNextScreen(popUpNone);
                    cameraInfoViewModel.setFirmwareDialogNeeded(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        hideProgressBar();
    }

    private void opsinFirmwareUpgradeCheck(OpsinVersionDetails opsinVersionDetails, boolean isWifiSkipped) {
        showProgressBar();

        sCameraFpga = opsinVersionDetails.getFpga();
        sCameraRiscv = opsinVersionDetails.getRiscv();
        sCameraWiFiRtos = opsinVersionDetails.getWifi();
        sCameraBle = opsinVersionDetails.getBle();
        if (sCameraWiFiRtos != null) {
            String[] discardWiFiVersion = sCameraWiFiRtos.split("-");
            sCameraWiFiRtos = discardWiFiVersion[0];
        }
        if (sCameraBle != null) {
            String[] discardBleVersion = sCameraBle.split("-");
            sCameraBle = discardBleVersion[0];
        }
        sCameraRiscvRecovery = opsinVersionDetails.getRiscvRecovery();
        sCameraOverlay = opsinVersionDetails.getOverlay();

        fragmentPopUpLayoutBinding.popUpSettingsCameraIcon.setImageResource(R.drawable.opsin_connected);
        fragmentPopUpLayoutBinding.popUpCameraIcon.setImageResource(R.drawable.opsin_connected);


        Log.e(TAG, "opsinFirmwareUpgradeCheck: FPGA: " + sCameraFpga + " RISCV: " + sCameraRiscv + " WIFI: " + sCameraWiFiRtos + " BLE: " + sCameraBle + " RECOVERY: " + sCameraRiscvRecovery + " OVERLAY: " + sCameraOverlay);
        if (homeViewModel.getCurrentFwVersion() != null) {
            homeViewModel.getCurrentFwVersion().setRiscv(sCameraRiscv);
            homeViewModel.getCurrentFwVersion().setOverlay(sCameraOverlay);
            homeViewModel.getCurrentFwVersion().setRiscvRecovery(sCameraRiscvRecovery);
            homeViewModel.getCurrentFwVersion().setFpga(sCameraFpga);
            homeViewModel.getCurrentFwVersion().setWifiRtos(sCameraWiFiRtos);
            homeViewModel.getCurrentFwVersion().setBle(sCameraBle);
            /*for this save cameraX value in view model*/
            try {
                if (sCameraRiscv != null) {
                    String[] aCameraRiscVersion = sCameraRiscv.split("\\.");
                    CameraViewModel.cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (homeViewModel.getManifest() == null) {
            readManifest();
        }
        if (homeViewModel.getManifest() != null) {
            sMasterVersion = homeViewModel.getManifest().getVersions().getRelease();
            sAppFpga = homeViewModel.getManifest().getVersions().getFpga();
            sAppRiscv = homeViewModel.getManifest().getVersions().getRiscv();
            sAppOverlay = homeViewModel.getManifest().getVersions().getOverlay();
            sAppRiscvRecovery = homeViewModel.getManifest().getVersions().getRiscvRecovery();
            sAppWiFiRtos = homeViewModel.getManifest().getVersions().getWiFiRtos();
            sAppBle = homeViewModel.getManifest().getVersions().getwIFI_BLE();
            sReleaseNotes = homeViewModel.getManifest().getReleaseNotes();
        }
        ArrayList<String> sequence = null;
        try {
            sequence = homeViewModel.getManifest().getSequence();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sequence != null && sequence.isEmpty()) {
            readManifestFile();
            sequence = homeViewModel.getManifest().getSequence();
        }

        if (tcpConnectionViewModel.isSSIDSelectedManually() && sequence != null) {
            if (sAppFpga != null && sAppRiscv != null && sAppRiscvRecovery != null && sAppWiFiRtos != null && sAppOverlay != null && sAppBle != null) {
                sAppRiscv = sAppRiscv.trim();
                sAppFpga = sAppFpga.trim();
                sAppWiFiRtos = sAppWiFiRtos.trim();
                sAppOverlay = sAppOverlay.trim();
                sAppRiscvRecovery = sAppRiscvRecovery.trim();
                sAppBle = sAppBle.trim();

                String[] aAppRiscVersion = sAppRiscv.split("\\.");
                String[] aCameraRiscVersion = sCameraRiscv.split("\\.");

                float fAppFpga = Float.parseFloat(sAppFpga);
                fCameraFpga = Float.parseFloat(sCameraFpga);

                /*Creating sequence based on Manifest and internal conditions*/
                sequence.forEach(sequenceName -> {
                    if (sequenceName.contains("WAIT")) {
                        firmwareUpdateSequence.add(sequenceName);
                    } else {
                        switch (sequenceName) {
                            case "RISCV":
                                int appXValue = Integer.parseInt(aAppRiscVersion[0]);
                                int cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);

                                int appYValue = Integer.parseInt(aAppRiscVersion[1]);
                                int cameraYValue = Integer.parseInt(aCameraRiscVersion[1]);

                                int appZValue = Integer.parseInt(aAppRiscVersion[2]);
                                int cameraZValue = Integer.parseInt(aCameraRiscVersion[2]);


                                /*This is new*/
                                if (aAppRiscVersion.length == 3 && aCameraRiscVersion.length == 3) {
                                    if (appXValue > cameraXValue || appYValue > cameraYValue || appZValue > cameraZValue) {
                                        Log.e(TAG, "opsinFirmwareUpgradeCheck: appXValue > cameraXValue || appYValue > cameraYValue || appZValue > cameraZValue");
                                        compareAndCreateSequenceIfCameraContainsRiscvAs22xx(fAppFpga);
                                    } else {
                                        // opsin Falcon firmware upgrade validation
                                      /*  if (sCameraRiscv != null && sCameraWiFiRtos != null) {
                                            if (sCameraRiscv.equals(OPSIN_RISCV_VERSION) && sCameraWiFiRtos.equals(OPSIN_WIFI_RTOS_VERSION) &&
                                                    fCameraFpga == OPSIN_CAMERA_FPGA_VERSION) {
                                                compareAndCreateSequenceIfCameraContainsRiscvAs22xx(fAppFpga);
                                            }
                                        }*/
                                    }
                                } else if (aAppRiscVersion.length == 4 && aCameraRiscVersion.length == 4) {
                                    if (appXValue > cameraXValue || appYValue > cameraYValue || appZValue > cameraZValue) {
                                        Log.e(TAG, "opsinFirmwareUpgradeCheck: appXValue > cameraXValue || appYValue > cameraYValue || appZValue > cameraZValue");
                                        compareAndCreateSequenceIfCameraContainsRiscvAs22xx(fAppFpga);
                                    } else {
                                        boolean containsOnlyNumbers = aCameraRiscVersion[3].matches("[0-9]+");
                                        if (containsOnlyNumbers) {
                                            int appIDValue = Integer.parseInt(aAppRiscVersion[3]);
                                            int cameraIDValue = Integer.parseInt(aCameraRiscVersion[3]);
                                            if (appIDValue > cameraIDValue) {
                                                compareAndCreateSequenceIfCameraContainsRiscvAs22xx(fAppFpga);
                                            }
                                        }
                                    }
                                } else if (aAppRiscVersion.length == 4 && aCameraRiscVersion.length == 3 || aAppRiscVersion.length == 3 && aCameraRiscVersion.length == 4) {
                                    Log.e(TAG, "startOpsinFwUpdate: aAppRiscVersion length 4 aCameraRiscVersion 3");
                                    compareAndCreateSequenceIfCameraContainsRiscvAs22xx(fAppFpga);
                                }



                                /*This is for later Opsery*/
//                                if (aAppRiscVersion.length == 3 && aCameraRiscVersion.length == 3) {
//                                    Log.e(TAG, "startOpsinFwUpdate: aAppRiscVersion length 3 aCameraRiscVersion 3");
//                                    compareAndCreateSequence(aAppRiscVersion, aCameraRiscVersion, fAppFpga, fCameraFpga);
//                                } else if (aAppRiscVersion.length == 4 && aCameraRiscVersion.length == 4) {
//                                    Log.e(TAG, "startOpsinFwUpdate: aAppRiscVersion length 4 aCameraRiscVersion 4");
//                                    if (appXValue == cameraXValue && appYValue == cameraYValue && appZValue == cameraZValue) {
//                                        boolean containsOnlyNumbers = aCameraRiscVersion[3].matches("[0-9]+");
//                                        if (containsOnlyNumbers) {
//                                            int appIDValue = Integer.parseInt(aAppRiscVersion[3]);
//                                            int cameraIDValue = Integer.parseInt(aCameraRiscVersion[3]);
//                                            if (appIDValue > cameraIDValue) {
//                                                firmwareUpdateSequence.add(OPSIN_FULL_IMAGE);
//                                                firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//                                            }
//                                        } else {
//                                            firmwareUpdateSequence.add(OPSIN_FULL_IMAGE);
//                                            firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//                                        }
//                                    } else {
//                                        compareAndCreateSequence(aAppRiscVersion, aCameraRiscVersion, fAppFpga, fCameraFpga);
//                                    }
//                                } else if (aAppRiscVersion.length == 4 && aCameraRiscVersion.length == 3 || aAppRiscVersion.length == 3 && aCameraRiscVersion.length == 4) {
//                                    Log.e(TAG, "startOpsinFwUpdate: aAppRiscVersion length 4 aCameraRiscVersion 3");
//                                    if (appXValue == cameraXValue && appYValue == cameraYValue && appZValue == cameraZValue) {
//                                        firmwareUpdateSequence.add(OPSIN_FULL_IMAGE);
//                                        firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//                                    } else {
//                                        compareAndCreateSequence(aAppRiscVersion, aCameraRiscVersion, fAppFpga, fCameraFpga);
//                                    }
//                                }
                                break;
                            case "RISCV_RECOVERY":
                                if (sCameraRiscvRecovery.equals(strUnknown)) {
                                    firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
                                } else {
                                    String[] discardCameraRecovery = sCameraRiscvRecovery.split("-");
                                    String[] discardAppRecovery = sAppRiscvRecovery.split("-");
                                    String[] aCameraRecovery = discardCameraRecovery[0].split("\\.");
                                    String[] aAppRecovery = discardAppRecovery[0].split("\\.");
                                    boolean isRecoveryUpdateAvailable = false;

                                    if (aAppRecovery.length == aCameraRecovery.length || aAppRecovery.length < aCameraRecovery.length) {
                                        for (int i = 0; i < aAppRecovery.length; i++) {
                                            int appValue = Integer.parseInt(aAppRecovery[i]);
                                            int cameraValue = Integer.parseInt(aCameraRecovery[i]);
                                            if (appValue > cameraValue) {
                                                isRecoveryUpdateAvailable = true;
                                                Log.d(TAG, "RECOVERY RIGHT IS GREATER");
                                                break;
                                            }
                                        }
                                        if (isRecoveryUpdateAvailable) {
                                            Log.e(TAG, "startOpsinFwUpdate: Recovery Update available");
                                            firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
                                        }
                                    } else {
                                        for (int i = 0; i < aCameraRecovery.length; i++) {
                                            try {
                                                int appValue = Integer.parseInt(aAppRecovery[i]);
                                                int cameraValue = Integer.parseInt(aCameraRecovery[i]);
                                                if (appValue > cameraValue) {
                                                    isRecoveryUpdateAvailable = true;
                                                    break;
                                                }
                                            } catch (NumberFormatException e) {
                                                isRecoveryUpdateAvailable = true;
                                                e.printStackTrace();
                                            }
                                        }
                                        if (isRecoveryUpdateAvailable) {
                                            firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
                                        }
                                    }
                                }
                                break;
                            case "FACTORY":
                                firmwareUpdateSequence.add(OPSIN_FACTORY);
                                break;
                            case "WIFI_RTOS":
                                if (!isWifiSkipped) {
                                    sAppWiFiRtos = sAppWiFiRtos.trim();
                                    if (sCameraWiFiRtos != null) {
                                        sCameraWiFiRtos = sCameraWiFiRtos.trim();
                                    } else {
                                        sCameraWiFiRtos = "";
                                    }
                                    Log.e(TAG, "opsinFirmwareUpgradeCheck: " + sAppWiFiRtos + " " + sCameraWiFiRtos + " " + !sAppWiFiRtos.equals(sCameraWiFiRtos) + " " + !sCameraWiFiRtos.equals("") + " " + !sAppWiFiRtos.equals(""));
                                    if (!sCameraWiFiRtos.equals("") && !sAppWiFiRtos.equals("") && !sAppWiFiRtos.equals(sCameraWiFiRtos)) {
                                        String[] vAppWiFiVersion = sAppWiFiRtos.split("\\.");
                                        String[] vCameraWiFIVersion = sCameraWiFiRtos.split("\\.");
                                        boolean isWiFiRtosUpdateAvailable = false;
                                        if (vAppWiFiVersion.length == vCameraWiFIVersion.length || vAppWiFiVersion.length < vCameraWiFIVersion.length) {
                                            for (int i = 0; i < vAppWiFiVersion.length; i++) {
                                                int appValue = Integer.parseInt(vAppWiFiVersion[i]);
                                                int cameraValue = Integer.parseInt(vCameraWiFIVersion[i]);
                                                if (appValue > cameraValue) {
                                                    isWiFiRtosUpdateAvailable = true;
                                                    Log.d(TAG, "RTOS RIGHT IS GREATER");
                                                    break;
                                                }
                                            }
                                            if (isWiFiRtosUpdateAvailable) {
                                                Log.e(TAG, "startOpsinFwUpdate: Wi-Fi version available");
                                                firmwareUpdateSequence.add(WIFI_RTOS);
                                            }
                                        } else {
                                            for (int i = 0; i < vCameraWiFIVersion.length; i++) {
                                                int appValue = Integer.parseInt(vAppWiFiVersion[i]);
                                                int cameraValue = Integer.parseInt(vCameraWiFIVersion[i]);
                                                if (appValue > cameraValue) {
                                                    isWiFiRtosUpdateAvailable = true;
                                                    Log.d(TAG, "RTOS RIGHT IS GREATER");
                                                    break;
                                                }
                                            }
                                            if (isWiFiRtosUpdateAvailable) {
                                                Log.e(TAG, "startOpsinFwUpdate: Wi-Fi version available");
                                                firmwareUpdateSequence.add(WIFI_RTOS);
                                            }
                                        }
                                    } else if (sCameraWiFiRtos.equals("")) {
                                        Log.e(TAG, "startOpsinFwUpdate: Wi-Fi version NULL");
                                        isUnknownWiFiVersion = true;
                                    } else {
                                        Log.e(TAG, "startOpsinFwUpdate: Wi-Fi version equals");
                                    }
                                }
                                break;
                            case "RESTART":
                                firmwareUpdateSequence.add(OPSIN_RESTART);
                                break;
                            case "WIFI_BLE":
                                int wifiComparisonResult = -1;
                                if (sCameraWiFiRtos != null && !sCameraWiFiRtos.equals("") && !sCameraWiFiRtos.equals(getString(R.string.unknown))) {
                                    if (sCameraWiFiRtos != null && containsFourthDigit(sCameraWiFiRtos)) {
                                        String removedWifi = removeFourthDigit(sCameraWiFiRtos);
                                        wifiComparisonResult = compareVersions(removedWifi, WIFI_BASE_VERSION_TO_DISPLAY_TOP);
                                    } else if (sCameraWiFiRtos != null) {
                                        wifiComparisonResult = compareVersions(sCameraWiFiRtos, WIFI_BASE_VERSION_TO_DISPLAY_TOP);
                                    }
                                }
                                if (wifiComparisonResult >= 0) {
                                    String[] vAppBleVersion = sAppBle.split("\\.");
                                    String[] vCameraBleVersion = sCameraBle.split("\\.");
                                    boolean isBleUpdateAvailable = false;
                                    if (vAppBleVersion.length == vCameraBleVersion.length || vAppBleVersion.length < vCameraBleVersion.length) {
                                        for (int i = 0; i < vAppBleVersion.length; i++) {
                                            int appValue = Integer.parseInt(vAppBleVersion[i]);
                                            int cameraValue = Integer.parseInt(vCameraBleVersion[i]);
                                            if (appValue > cameraValue) {
                                                isBleUpdateAvailable = true;
                                                break;
                                            }
                                        }
                                        if (isBleUpdateAvailable) {
                                            firmwareUpdateSequence.add(OPSIN_WIFI_BLE);
                                        }
                                    } else {
                                        for (int i = 0; i < vCameraBleVersion.length; i++) {
                                            int appValue = Integer.parseInt(vAppBleVersion[i]);
                                            int cameraValue = Integer.parseInt(vCameraBleVersion[i]);
                                            if (appValue > cameraValue) {
                                                isBleUpdateAvailable = true;
                                                break;
                                            }
                                        }
                                        if (isBleUpdateAvailable) {
                                            firmwareUpdateSequence.add(OPSIN_WIFI_BLE);
                                        }
                                    }
                                }

                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + sequenceName);
                        }
                        if (isUnknownWiFiVersion) {
                        }
                    }
                });
            }
            if (!isUnknownWiFiVersion) {
                if (activity.isInMultiWindowMode()) {
                    Toast.makeText(activity, R.string.firmware_dialog_in_split_screen, Toast.LENGTH_LONG).show();
                    hideProgressBar();
                    disConnectCamera();
                }  else
                    showFwUpdateDialog();
            } else {
                if (isSelectPopUpSettings || isSelectPopUpInfo) {
                    firmwareUpdateSequence.clear();
                    if (activity.isInMultiWindowMode()) {
                        Toast.makeText(activity, R.string.firmware_dialog_in_split_screen, Toast.LENGTH_LONG).show();
                        hideProgressBar();
                        disConnectCamera();
                    }else
                        showFwUpdateDialog();
                } else {
                    isUnknownWiFiVersion = false;
                    activity.showDialog = MainActivity.ShowDialog.WIFI_FW_UPDATE_AVAILABLE_DIALOG;
                    activity.showDialog(getString(R.string.wifi_upgrade_suggestion_due_to_wifi_null_title), getString(R.string.wifi_upgrade_suggestion_due_to_wifi_null_message), null);
                }

            }
        } else {
            hideProgressBar();
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

    public void showFwUpdateDialog() {
        if (!firmwareUpdateSequence.isEmpty()) {
            Log.e(TAG, "showFwUpdateDialog: " + firmwareUpdateSequence.size() + " " + firmwareUpdateSequence.get(0));
            if (firmwareUpdateSequence.contains(RISCV) || firmwareUpdateSequence.contains(FPGA) || firmwareUpdateSequence.contains(OPSIN_FULL_IMAGE) || firmwareUpdateSequence.contains(OPSIN_RISCV_FPGA) || firmwareUpdateSequence.contains(OPSIN_RISCV_OVERLAY) || firmwareUpdateSequence.contains(OPSIN_RISCV_RECOVERY) || firmwareUpdateSequence.contains(WIFI_RTOS) || firmwareUpdateSequence.contains(OPSIN_WIFI_BLE)) {
                if (!tcpConnectionViewModel.isFirmwareUpdateChecked()) {
                    if (activity != null && !activity.getIsFirmwareSkipped(homeViewModel.connectedSsid)) {
                        if (!homeViewModel.isHasShowFwDialog() && fwMode == MODE_NONE && HomeViewModel.screenType != FW_UPDATE_SCREEN) {/*for this condition show firmware versions dialog only one time create when rotate device not create */
                            homeViewModel.getNoticeDialogFragments().clear();
                            showFwUpdateDialog(sReleaseNotes);
                            homeViewModel.setHasShowFwDialog(true);
                            homeViewModel.setShowFwDialogNone(true);
                        }
                    } else {
                        if (!homeViewModel.hasShowRecoverModeDialog()) {
                            if (isSelectPopUpFwUpdateCheck) {
                                showOpsinFwUpdateDialog();
                            } else {
                                navigateToNextScreen(popUpNone);
                            }
                        }
                        hideProgressBar();
                    }
                } else {
                    hideProgressBar();
                }
            } else {
                Log.e(TAG, "opsinFirmwareUpgradeCheck: ELSE");
                if (!homeViewModel.hasShowRecoverModeDialog()) {
                 if (isSelectPopUpFwUpdateCheck) {
                        showOpsinFwUpdateDialog();
                    } else {
                        Log.e(TAG, "opsinFirmwareUpgradeCheck: " + homeViewModel.isPressCancelOrBackPopUpWindow());
                        if (!homeViewModel.isPressCancelOrBackPopUpWindow()) {
                            navigateToNextScreen(popUpNone);
                        }
                    }
                }
                hideProgressBar();
            }
        } else {
            if (!homeViewModel.hasShowRecoverModeDialog()) {
                if (isSelectPopUpFwUpdateCheck) {
                    showOpsinFwUpdateDialog();
                } else {
                    navigateToNextScreen(popUpNone);
                }
            }
            hideProgressBar();
        }
    }

    //   for this show only long press pop up fw update available icon press
    private void showOpsinFwUpdateDialog() {
        if (fwMode == MODE_NONE && HomeViewModel.screenType != FW_UPDATE_SCREEN) {
            homeViewModel.getNoticeDialogFragments().clear();
            showFwUpdateDialog(sReleaseNotes);
            homeViewModel.setHasShowFwDialog(true);
            homeViewModel.setShowFwDialogNone(true);
        }
    }

    private void setOpsinMasterVersion() {
        if (sMasterVersion != null) {
            String[] aMasterVersion = sMasterVersion.split("\\.");
            if (aMasterVersion.length == 3) {
                String sMajor = aMasterVersion[0];
                String sMinor = aMasterVersion[1];
                String sPatch = aMasterVersion[2];
                tcpConnectionViewModel.setOpsinMasterVersion(sMajor, sMinor, sPatch);
            }
        }
    }

    private void compareAndCreateSequenceIfCameraContainsRiscvAs22xx(float fAppFpga) {
        boolean isOverlayUpdateAvailable = false;
        if (sCameraOverlay != null && !sCameraOverlay.equals(strUnknown)) {
            String[] vAppOverlayVersion = sAppOverlay.split("\\.");
            String[] vCameraOverlayVersion = sCameraOverlay.split("\\.");
            if (vAppOverlayVersion.length == vCameraOverlayVersion.length) {
                for (int i = 0; i < vAppOverlayVersion.length; i++) {
                    int appValue = Integer.parseInt(vAppOverlayVersion[i]);
                    int cameraValue = Integer.parseInt(vCameraOverlayVersion[i]);
                    if (appValue > cameraValue) {
                        isOverlayUpdateAvailable = true;
                        Log.d(TAG, "OVERLAY RIGHT IS GREATER");
                        break;
                    }
                }
            }
        } else {
            isOverlayUpdateAvailable = true;
        }

        if (fAppFpga > fCameraFpga) {
            Log.e(TAG, "startOpsinFwUpdate: Display a dialog");
            homeViewModel.setIsFpgaUpgradeAvailable(true);
            // This is for dummy to display a customer contact dialog, will be cleared once dialog appeared
            firmwareUpdateSequence.add(FPGA);
        } else if (isOverlayUpdateAvailable) {
            Log.e(TAG, "startOpsinFwUpdate: RISCV OVERLAY");
            firmwareUpdateSequence.add(OPSIN_RISCV_OVERLAY);
            firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
        } else {
            Log.e(TAG, "startOpsinFwUpdate: RISCV RISCV");
            firmwareUpdateSequence.add(RISCV);
        }
    }

//    private void compareAndCreateSequence(String[] aAppRiscVersion, String[] aCameraRiscVersion, float fAppFpga, float fCameraFpga) {
//        int appXValue = Integer.parseInt(aAppRiscVersion[0]);
//        int cameraXValue = Integer.parseInt(aCameraRiscVersion[0]);
//
//        int appYValue = Integer.parseInt(aAppRiscVersion[1]);
//        int cameraYValue = Integer.parseInt(aCameraRiscVersion[1]);
//
//        int appZValue = Integer.parseInt(aAppRiscVersion[2]);
//        int cameraZValue = Integer.parseInt(aCameraRiscVersion[2]);
//
//        if (appXValue > cameraXValue) {
//            Log.e(TAG, "startOpsinFwUpdate: RISCV X");
//            firmwareUpdateSequence.add(OPSIN_FULL_IMAGE);
//            firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//        } else if (appYValue > cameraYValue || appZValue > cameraZValue) {
//            Log.e(TAG, "startOpsinFwUpdate: RISCV Y or RISC Z");
//
//            boolean isOverlayUpdateAvailable = false;
//            if (sCameraOverlay != null) {
//                if (!sCameraOverlay.equals(strUnknown)) {
//                    String[] vAppOverlayVersion = sAppOverlay.split("\\.");
//                    String[] vCameraOverlayVersion = sCameraOverlay.split("\\.");
//                    if (vAppOverlayVersion.length == vCameraOverlayVersion.length) {
//                        for (int i = 0; i < vAppOverlayVersion.length; i++) {
//                            int appValue = Integer.parseInt(vAppOverlayVersion[i]);
//                            int cameraValue = Integer.parseInt(vCameraOverlayVersion[i]);
//                            if (appValue > cameraValue) {
//                                isOverlayUpdateAvailable = true;
//                                Log.d(TAG, "OVERLAY RIGHT IS GREATER");
//                                break;
//                            }
//                        }
//                    }
//                } else {
//                    isOverlayUpdateAvailable = true;
//                }
//
//
//                if (isOverlayUpdateAvailable && fAppFpga > fCameraFpga) {
//                    Log.e(TAG, "startOpsinFwUpdate: RISCV OVERLAY AND FPGA");
//                    firmwareUpdateSequence.add(OPSIN_FULL_IMAGE);
//                    firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//                } else if (isOverlayUpdateAvailable) {
//                    Log.e(TAG, "startOpsinFwUpdate: RISCV OVERLAY");
//                    firmwareUpdateSequence.add(OPSIN_RISCV_OVERLAY);
//                    firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//                } else if (fAppFpga > fCameraFpga) {
//                    Log.e(TAG, "startOpsinFwUpdate: RISCV FPGA");
//                    firmwareUpdateSequence.add(OPSIN_RISCV_FPGA);
//                    firmwareUpdateSequence.add(OPSIN_RISCV_RECOVERY);
//                } else {
//                    Log.e(TAG, "startOpsinFwUpdate: RISCV RISCV");
//                    firmwareUpdateSequence.add(RISCV);
//                }
//            } else {
//                Log.e(TAG, "startOpsinFwUpdate: OVERLAY IS NULL. SO, RISCV");
//                firmwareUpdateSequence.add(RISCV);
//            }
//
//        }
//    }

    private void navigateToNextScreen(String popUpScreenName) {
        if (tcpConnectionViewModel != null) {
            Log.e(TAG, "navigateToNextScreen: " + popUpScreenName + "/" + HomeViewModel.screenType.name());

            /* mHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideProgressBar();
                }
            });*/
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                if (!homeViewModel.hasShowFwDialogNone()) {
                    if (popUpScreenName.equals(popUpInfoScreen) || screenType == POP_UP_INFO_SCREEN) {
                        if (fwMode == MODE_NONE && !homeViewModel.hasShowRecoverModeDialog()) {
                            showPopUpCameraInfoScreen();
                            binding.selectDeviceLayout.setVisibility(View.GONE);
                            removeObserver();
                            //  homeViewModel.closePopupCameraInfoFragment(true);
                        }
                    } else if (popUpScreenName.equals(popUpSettingsScreen) || screenType == POP_UP_SETTINGS_SCREEN) {
                        /*
                        for this firmware update resume while rotating the device,
                        go to home screen and stopped FW update to avoid this condition added
                         */
                        //(!popUpCameraSettingsViewModel.isConfirmationDialogShown && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) -> that condition while rotate pop up settings screen in bw sent camera mode command that one to be blocked
                        if (fwMode == MODE_NONE && !homeViewModel.hasShowRecoverModeDialog() && !popUpCameraSettingsViewModel.isConfirmationDialogShown && !PopUpCameraSettingsViewModel.isHasDismissPleaseWaitProgressDialog()) {
                            tcpConnectionViewModel.getCameraMode();
                            tcpConnectionViewModel.observeCameraMode().observe(this.getViewLifecycleOwner(), observeCameraMode);
                            tcpConnectionViewModel.observeCameraVideoMode().observe(this.getViewLifecycleOwner(), observeCameraVideoMode);
                            showPopUpCameraSettingsScreen();
                            binding.selectDeviceLayout.setVisibility(View.GONE);
                            removeObserver();
                        }

                        /* For this after popup window appear removed camera mode observer */
//                        if (CameraViewModel.hasNewFirmware()) {
//                            tcpConnectionViewModel.observeCameraMode().removeObserver(observeCameraMode);
//                            tcpConnectionViewModel.observeCameraMode().removeObservers(this.getViewLifecycleOwner());
//                            tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
//                            tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
//                        }
                    } else {
                        navigateToCameraSplash(popUpScreenName);
                    }
                } else {
                    Log.e(TAG, "navigateToNextScreen: hasShowFwDialogNone : false");
                    navigateToCameraSplash(popUpScreenName);
                }
            } else {
                // Dialog shown for while updating the FW the screen rotation the progress has been paused due to failed
                if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
                    Log.e(TAG, "navigateToNextScreen: fwFailedDialog");
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN))
                        showFwRetryDialogMessage(getString(R.string.firmware_update_failed_retry));
                }
                Log.e(TAG, "navigateToNextScreen: socket not connect");
            }
        }
    }

    private void removeObserver() {
        tcpConnectionViewModel.observeReleaseVersion().removeObserver(observeBootMode);
        tcpConnectionViewModel.observeRiscBootMode().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeCameraInfo().removeObserver(observeCameraInfo);
        tcpConnectionViewModel.observeCameraInfo().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeRisvVersion().removeObserver(observeRiscVersion);
        tcpConnectionViewModel.observeRisvVersion().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeFpgaVersion().removeObserver(observeFpgeVersion);
        tcpConnectionViewModel.observeFpgaVersion().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeReleaseVersion().removeObserver(observeReleaseVersion);
        tcpConnectionViewModel.observeReleaseVersion().removeObservers(lifecycleOwner);

        tcpConnectionViewModel.observeCameraName().removeObserver(observeCameraName);
        tcpConnectionViewModel.observeCameraName().removeObservers(lifecycleOwner);
    }

    private final com.dome.librarynightwave.utils.EventObserver<Object> observeCameraMode = new com.dome.librarynightwave.utils.EventObserver<>(object -> {
        if (object instanceof CommandError) {
            Log.e(TAG, "observeCameraMode: " + ((CommandError) object).getError());
        } else {
            //  if (homeViewModel.currentScreen == HomeViewModel.CurrentScreen.LIVE_VIEW || homeViewModel.currentScreen == HomeViewModel.CurrentScreen.CAMERA_INFO_DIALOG_SCREEN || CameraViewModel.isHasVisibleSettingsInfoView()) {
            int response = (int) object;
            Log.d(TAG, "observeCameraMode: " + object);
            if (response == SCCPConstants.SCCP_CAMERA_MODE.SCCP_CAMERA_MODE_USB.getValue()) {
                tcpConnectionViewModel.getVideoMode();
                cameraViewModel.setAnalogMode(false);
                Log.d(TAG, "observeCameraMode : USB ");
            } else if (response == SCCPConstants.SCCP_CAMERA_MODE.SSCP_CAMERA_MODE_ANALOG.getValue()) {
                cameraViewModel.setAnalogMode(true);
                Log.d(TAG, "observeCameraMode : Analog");
            }
            tcpConnectionViewModel.observeCameraMode().removeObservers(this.getViewLifecycleOwner());
            // } else {
            //     Log.d(TAG, "observeCameraMode: not in live view");
            //}
        }
    });

    private final com.dome.librarynightwave.utils.EventObserver<Object> observeCameraVideoMode = new com.dome.librarynightwave.utils.EventObserver<>(object -> {
        if (object instanceof CommandError) {
            Log.e(TAG, "observeCameraVideoMode: " + ((CommandError) object).getError());
        } else {
            int response = (int) object;
            Log.d(TAG, "observeCameraVideoMode: " + object);
            if (response == SCCPConstants.SCCP_VIDEO_MODE.SCCP_VIDEO_MODE_USB.getValue()) {
                CameraViewModel.setIsLiveScreenVideoModeWIFI(false);
                CameraViewModel.setIsLiveScreenVideoModeUVC(true);
                // Show dialog with wave image to enable video mode to wifi
                Log.d(TAG, "observeCameraVideoMode : UVC ");
            } else if (response == SCCPConstants.SCCP_VIDEO_MODE.SSCP_VIDEO_MODE_WIFI.getValue()) {
                CameraViewModel.setIsLiveScreenVideoModeWIFI(true);
                CameraViewModel.setIsLiveScreenVideoModeUVC(false);
                Log.d(TAG, "observeCameraVideoMode : WIFI");
            }
//            homeViewModel.setCameraVideoModeResponse(object);
//            homeViewModel.isUpdateCameraVideoMode.removeObservers(lifecycleOwner);
            popUpCameraSettingsViewModel.isUpdateVideoModeState(object);
            tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
        }
    });

    private void hideShowDialog() {
        Log.e(TAG, "hideShowDialog: called");
        if (!homeViewModel.getFragmentManager().isDestroyed()) {
            if (!homeViewModel.getNoticeDialogFragments().isEmpty()) {
                for (int i = 0; i < homeViewModel.getNoticeDialogFragments().size(); i++) {
                    NoticeDialogFragment dialogFragment = homeViewModel.getNoticeDialogFragments().get(i);
                    if (dialogFragment.isVisible()) {
                        String tag = dialogFragment.getTag();
                        Log.e(TAG, "showDialog: " + tag);
                        new Handler().post(() -> {
                            hasAlreadyAddedInDialogTag = false;
                            dialogFragment.dismiss();
                        });
                    }
                }
            } else {
                new NoticeDialogFragment().dismissNow();
                hasAlreadyAddedInDialogTag = false;
            }
            homeViewModel.getNoticeDialogFragments().clear();
        }
    }

    private void navigateToCameraSplash(String popUpScreenName) {
        if (popUpScreenName.equals(popUpNone) && !hasAlreadyAddedInDialogTag && (HomeViewModel.screenType != POP_UP_INFO_SCREEN && HomeViewModel.screenType != POP_UP_SETTINGS_SCREEN) && HomeViewModel.screenType != FW_UPDATE_SCREEN) {
            // Exist logic should not affect OPSIN
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                homeViewModel.getManifest().getSequence().clear();
                firmwareUpdateSequence.clear();
                homeViewModel.getOpsinAbnormalSequence().clear();
                homeViewModel.getAbnormalSequence().clear();
                homeViewModel.setFwCompletedCount(0);
                homeViewModel.setFwUpdateSequenceIndex(0);
                homeViewModel.setOpsinFwUpdateSequenceIndex(0);
            }
            try {
                if (activity != null && activity.showDialog != MainActivity.ShowDialog.NONE) {
                    hideShowDialog();
                    activity.showDialog = MainActivity.ShowDialog.NONE;
                    hasAlreadyAddedInDialogTag = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e(TAG, "navigateToCameraSplash called : ");
            if (tooltipHelper != null) {
                tooltipHelper.dismissIfShowing();
            }
            removeObserver();
            homeViewModel.setSelectedCamera("");
            homeViewModel.getNavController().navigate(R.id.cameraSplashFragment);
            homeViewModel.setNavigateToNextScreen(false);
            hasShowProgressBar = false;
            homeViewModel.setHasShowProgressBar(false);
        }
    }

    private void startFwUpdate() {
        if (homeViewModel.getManifest() != null && homeViewModel.getCurrentFwVersion() != null) {
            ArrayList<String> sequence = homeViewModel.getManifest().getSequence();
            int fwUpdateSequenceIndex = homeViewModel.getFwUpdateSequenceIndex();
            Log.e(TAG, "startFwUpdate: " + fwUpdateSequenceIndex + " " + Arrays.toString(sequence.toArray(new String[0])));

            if (fwUpdateSequenceIndex == 0)
                tcpConnectionViewModel.observeFwUpdateProgress().observe(lifecycleOwner, fwProgress);

            if (!sequence.isEmpty()) {
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
                            homeViewModel.setFwUpdateSequenceIndex(nextIndex);
                            startFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(Constants.FPGA)) {
                        String camerafpga = homeViewModel.getCurrentFwVersion().getFpga();
                        if (camerafpga != null) camerafpga = camerafpga.trim();
                        String appVersion = homeViewModel.getManifest().getVersions().getFpga().trim();
                        if (camerafpga != null && !appVersion.equals(camerafpga) && !appVersion.equals("")) {
                            Log.e(TAG, "startFwUpdate: fpga inside");
                            tcpConnectionViewModel.startFwUpdate(sequences);
                            showFirmwareUpdateScreen();
                        } else {
                            int nextIndex = homeViewModel.getFwUpdateSequenceIndex() + 1;
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
                        homeViewModel.setHasShowFwDialog(false);/* for this show fw update dialog with all fw_version in ui. after complete fw update ui hide and show home screen */
                        tcpConnectionViewModel.setSSIDSelectedManually(false);
                    }
                }
            }
        }
    }

    private void startOpsinFwUpdate() {
        if (homeViewModel.getManifest() != null && homeViewModel.getCurrentFwVersion() != null) {
            int fwUpdateSequenceIndex = homeViewModel.getOpsinFwUpdateSequenceIndex();

            Log.e(TAG, "startOpsinFwUpdate: Sequence Size: " + firmwareUpdateSequence.size() + " Sequence Index:" + fwUpdateSequenceIndex);

            if (fwUpdateSequenceIndex == 0)
                tcpConnectionViewModel.observeFwUpdateProgress().observe(lifecycleOwner, fwProgress);

            if (!firmwareUpdateSequence.isEmpty()) {
                if (fwUpdateSequenceIndex < firmwareUpdateSequence.size()) {
                    String sequences = firmwareUpdateSequence.get(fwUpdateSequenceIndex);
                    homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_STARTED;

                    if (sequences.equalsIgnoreCase(RISCV)) {
                        binding.estimationTime.setText(getString(R.string.estimated_time, "2"));

                        String cameraRiscv = homeViewModel.getCurrentFwVersion().getRiscv().trim();
                        String appRisc = homeViewModel.getManifest().getVersions().getRiscv().trim();
                        if (!appRisc.equals(cameraRiscv) && !appRisc.equals("")) {
                            Log.e(TAG, "startOpsinFwUpdate: RISC-V inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(RISCV);
                            showFirmwareUpdateScreen();
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: RISC-V Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(FPGA)) {
                        binding.estimationTime.setText(getString(R.string.estimated_time, "15"));

                        String cameraFpga = homeViewModel.getCurrentFwVersion().getFpga().trim();
                        String appFpga = homeViewModel.getManifest().getVersions().getFpga().trim();
                        if (!appFpga.equals(cameraFpga) && !appFpga.equals("")) {
                            Log.e(TAG, "startOpsinFwUpdate: FPGA-V inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(FPGA);
                            showFirmwareUpdateScreen();
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: FPGA-V Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(OPSIN_FULL_IMAGE)) {
                        binding.estimationTime.setText(getString(R.string.estimated_time, "15"));

                        Log.e(TAG, "startOpsinFwUpdate: FULL_IMAGE inside");
                        tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_FULL_IMAGE);
                        showFirmwareUpdateScreen();
                    } else if (sequences.equalsIgnoreCase(OPSIN_RISCV_FPGA)) {
                        binding.estimationTime.setText(getString(R.string.estimated_time, "15"));

                        String cameraFpga = homeViewModel.getCurrentFwVersion().getFpga();
                        String appFpga = homeViewModel.getManifest().getVersions().getFpga().trim();
                        if (!appFpga.equals(cameraFpga) && !appFpga.equals("")) {
                            Log.e(TAG, "startOpsinFwUpdate: OPSIN_RISCV_FPGA inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_RISCV_FPGA);
                            showFirmwareUpdateScreen();
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: OPSIN_RISCV_FPGA Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(OPSIN_RISCV_OVERLAY)) {
                        binding.estimationTime.setText(getString(R.string.estimated_time, "6"));

                        String cameraOverlay = homeViewModel.getCurrentFwVersion().getOverlay();
                        String appOverlay = homeViewModel.getManifest().getVersions().getOverlay().trim();
                        if (!appOverlay.equals(cameraOverlay) || cameraOverlay.equals(strUnknown)) {
                            Log.e(TAG, "startOpsinFwUpdate: OVERLAY inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_RISCV_OVERLAY);
                            showFirmwareUpdateScreen();
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: OVERLAY Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(OPSIN_RISCV_RECOVERY)) {
                        binding.estimationTime.setText(getString(R.string.estimated_time, "2"));

                        String cameraRecovery = homeViewModel.getCurrentFwVersion().getRiscvRecovery();
                        String appRecovery = homeViewModel.getManifest().getVersions().getRiscvRecovery().trim();
                        if (!appRecovery.equals(cameraRecovery) || cameraRecovery.equals(strUnknown)) {
                            Log.e(TAG, "startOpsinFwUpdate: RECOVERY inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_RISCV_RECOVERY);
                            showFirmwareUpdateScreen();
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: RECOVERY Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(WIFI_RTOS)) {
                        String cameraWiFi = homeViewModel.getCurrentFwVersion().getWiFiRtos();
                        if (cameraWiFi != null) cameraWiFi = cameraWiFi.trim();
                        String appWiFi = homeViewModel.getManifest().getVersions().getWiFiRtos().trim();
                        if (cameraWiFi != null && !appWiFi.equals(cameraWiFi)) {
                            Log.e(TAG, "startOpsinFwUpdate: WIFI_RTOS inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(WIFI_RTOS);
                            showFirmwareUpdateScreen();
                            binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: WIFI_RTOS Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(OPSIN_WIFI_BLE)) {
                        String cameraBle = homeViewModel.getCurrentFwVersion().getBle();
                        if (cameraBle != null) cameraBle = cameraBle.trim();
                        String appBle = homeViewModel.getManifest().getVersions().getwIFI_BLE().trim();
                        if (cameraBle != null && !appBle.equals(cameraBle)) {
                            Log.e(TAG, "startOpsinFwUpdate: WIFI_BLE inside");
                            tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_WIFI_BLE);
                            showFirmwareUpdateScreen();
                            binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                        } else {
                            Log.e(TAG, "startOpsinFwUpdate: WIFI_BLE Skipped");
                            int nextIndex = homeViewModel.getOpsinFwUpdateSequenceIndex() + 1;
                            homeViewModel.setOpsinFwUpdateSequenceIndex(nextIndex);
                            startOpsinFwUpdate();
                        }
                    } else if (sequences.equalsIgnoreCase(OPSIN_RESTART)) {
                        binding.estimationTime.setVisibility(View.GONE);
                        tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_RESTART);
                        showFirmwareUpdateScreen();
                    } else if (sequences.equalsIgnoreCase(OPSIN_FACTORY)) {
                        binding.estimationTime.setVisibility(View.GONE);
                        tcpConnectionViewModel.startOpsinFwUpdate(OPSIN_FACTORY);
                        showFirmwareUpdateScreen();
                    } else if (sequences.contains("WAIT")) {
                        binding.estimationTime.setVisibility(View.GONE);
                        tcpConnectionViewModel.startOpsinFwUpdate(sequences);
                        showFirmwareUpdateScreen();
                    }
                } else {
                    Log.e(TAG, "startOpsinFwUpdate: completed");
                    if (tcpConnectionViewModel != null) {
                        Log.e(TAG, "startOpsinFwUpdate: completed inside");
                        tcpConnectionViewModel.setFirmwareUpdateCompleted(true);
                        homeViewModel.setAbnormalFwUpdate(false);
                        homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
                        tcpConnectionViewModel.getOpsinImageState();
                        tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
                        tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
                        homeViewModel.setHasShowFwDialog(false);/* for this show fw update dialog with all fw_version in ui. after complete fw update ui hide and show home screen */
                        tcpConnectionViewModel.setSSIDSelectedManually(false);
//                        showHomeScreen();
                    }
                }
            } else {
                navigateToNextScreen(popUpNone);
            }
        }
    }

    private void startAbnormalBehaviourFwUpdateSequence() {
        if (homeViewModel.getManifest() != null && homeViewModel.getCurrentFwVersion() != null) {
            int abnormalFwUpdateSequenceIndex = homeViewModel.getAbnormalFwUpdateSequenceIndex();
            homeViewModel.firmwareStatus = HomeViewModel.FirmwareStatus.FW_UPDATE_STARTED;
            Log.e(TAG, "fwupdateProgress startAbnormalBehaviourFwUpdateSequence: " + HomeViewModel.screenType.name());

            if (abnormalFwUpdateSequenceIndex == 0)
                tcpConnectionViewModel.observeFwUpdateProgress().observe(lifecycleOwner, fwProgress);

            changeProgressCenterCameraIcon();
            switch (currentCameraSsid) {
                case NIGHTWAVE:
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
                    break;
                case OPSIN: //Opsin
                    ArrayList<String> opsinAbnormalSequence = homeViewModel.getOpsinAbnormalSequence();
                    if (!opsinAbnormalSequence.isEmpty()) {
                        Log.e(TAG, "fwupdateProgress opsinAbnormalSequence: " + HomeViewModel.screenType.name());
                        if (abnormalFwUpdateSequenceIndex < opsinAbnormalSequence.size()) {
                            String sequences = opsinAbnormalSequence.get(abnormalFwUpdateSequenceIndex);
                            if (sequences.equalsIgnoreCase(OPSIN_FULL_IMAGE)) {
                                tcpConnectionViewModel.startOpsinFwUpdate(sequences);
                                showFirmwareUpdateScreen();

                                //Store count
                                storeOpsinImageStateCountWhenAttempting2ndTime(2);
                            }
                            if (sequences.equalsIgnoreCase(RISCV)) {
                                tcpConnectionViewModel.startOpsinFwUpdate(sequences);
                                showFirmwareUpdateScreen();
                            }
                            if (sequences.equalsIgnoreCase(OPSIN_RISCV_RECOVERY)) {
                                tcpConnectionViewModel.startOpsinFwUpdate(sequences);
                                showFirmwareUpdateScreen();
                            }
                            if (sequences.contains(Constants.WAIT)) {
                                tcpConnectionViewModel.startOpsinFwUpdate(sequences);
                                showFirmwareUpdateScreen();
                            }
                        } else {
                            if (tcpConnectionViewModel != null) {
                                Log.e(TAG, "startAbnormalBehaviourFwUpdateSequence: 3rd check");
                                tcpConnectionViewModel.setFirmwareUpdateCompleted(true);
                                homeViewModel.setFwCompletedCount(homeViewModel.getFwCompletedCount() + 1);
                                homeViewModel.setBootModeCheckCount(homeViewModel.getBootModeCheckCount() + 1);
                                tcpConnectionViewModel.getOpsinImageState();//3rd Check
                                tcpConnectionViewModel.observeFwUpdateProgress().removeObserver(fwProgress);
                                tcpConnectionViewModel.observeFwUpdateProgress().removeObservers(lifecycleOwner);
                                homeViewModel.setHasShowFwDialog(false);
                                tcpConnectionViewModel.setSSIDSelectedManually(false);
                            }
                        }
                    }
                    break;
                case NIGHTWAVE_DIGITAL:
                    break;
            }
        }
    }

    private void storeOpsinImageStateCountWhenAttempting2ndTime(int count) {
        try {
            String connectedSsidFromWiFiManager = bleWiFiViewModel.getConnectedSsidFromWiFiManager();
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("ImageState", MODE_PRIVATE);
            SharedPreferences.Editor myEdit = sharedPreferences.edit();
            myEdit.putInt("ImageStateCount", count);
            myEdit.putString("SSID", connectedSsidFromWiFiManager);
            myEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getOpsinImageStateCount(String connectedSsid) {
        int count = 0;
        try {
            SharedPreferences sh = requireActivity().getSharedPreferences("ImageState", MODE_PRIVATE);
            String ssid = sh.getString("SSID", "");
            int ImageStateCount = sh.getInt("ImageStateCount", 0);
            if (connectedSsid != null && connectedSsid.equalsIgnoreCase(ssid)) {
                count = ImageStateCount;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
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
                    Log.d(TAG, "Home_Completed: " + value);
                    switch (currentCameraSsid) {
                        case NIGHTWAVE:
                            if (HomeViewModel.screenType == FW_UPDATE_SCREEN && HomeViewModel.isGoldenUpdate) {
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
                            break;
                        case OPSIN:
                            if (fwMode == MODE_RISCV) {
                                binding.loadingText.setText(getString(R.string.updating_riscv));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else if (fwMode == MODE_FPGA) {
                                binding.loadingText.setText(getString(R.string.updating_fpga));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "15"));
                            } else if (fwMode == MODE_OPSIN_RISCV_OVERLAY) {
                                binding.loadingText.setText(getString(R.string.updating_riscv_overlay));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "6"));
                            } else if (fwMode == MODE_WIFI_DIALOG) {
                                binding.loadingText.setText(getString(R.string.updating_wifi));
                                if (Constants.isOpsinContainsOldWiFiFirmware()) {
                                    binding.estimationTime.setText(getString(R.string.estimated_time, "4"));
                                } else {
                                    binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                                }

                            } else if (fwMode == MODE_OPSIN_BLE) {
                                binding.loadingText.setText(getString(R.string.updating_ble));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else if (fwMode == MODE_OPSIN_RECOVERY) {
                                binding.loadingText.setText(getString(R.string.updating_recovery));
                                binding.estimationTime.setText(getString(R.string.estimated_time, "2"));
                            } else {
                                binding.estimationTime.setText("");
                                binding.loadingText.setText(getString(R.string.updating));
                            }
                            changeProgressCenterCameraIcon();
                            String percentage1 = String.format(getString(R.string.percentage), value);
                            binding.progressText.setText(percentage1);
                            binding.progressBar.setProgress(value);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            //
                            break;
                    }
                }
            }
        }
    };

    private void changeProgressCenterCameraIcon() {
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                binding.firmwareCamera.setImageResource(R.drawable.ic_firmware_upload_camera);
                break;
            case OPSIN:
                binding.firmwareCamera.setImageResource(R.drawable.ic_opsin_white_round);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"changeProgressCenterCameraIcon : NW_Digital");
                break;
        }

    }

    private void showFwRetryDialogMessage(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.FW_RETRY_DIALOG_MESSAGE;
            activity.showDialog("", message, null);
        }
    }

    private void showFwUpdateDialog(String release_notes) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.FW_UPDATE_AVAILABLE_DIALOG;
            switch (currentCameraSsid) {
                case NIGHTWAVE:
                    activity.showDialog("", requireContext().getString(R.string.frimware_update_available), release_notes);
                    break;
                case OPSIN:
                    activity.showDialog("", requireContext().getString(R.string.frimware_update_status), release_notes);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"showFwUpdateDialog : NW_Digital");
                    break;
            }
            hideProgressBar();
            if (!isSelectPopUpFwUpdateCheck) tcpConnectionViewModel.setFirmwareUpdateChecked(true);
        }
    }

    private void showFwReBootModeDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.FW_REBOOT_MODE_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void showFwRecoverDialogMessage(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.FW_RECOVER_DIALOG_MESSAGE;
            activity.showDialog("", message, null);
            /*
            for this while recover dialog showing rotate the device to avoid show this dialog on pop setting screen
             */
            showHomeScreen();
        }
    }

    private void showDeleteCameraItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.POP_UP_DELETE_ITEM_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void showOpsinCameraPowerOffDialog(String message) {
        hideWiFiDisconnectProgressbar();
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            if (HomeViewModel.hasShowPowerCycleDialog) {
                activity.showDialog = MainActivity.ShowDialog.OPSIN_RECYCLE_THE_CAMERA;
                activity.showDialog("", message, null);
            } else {
                Log.d(TAG, "showOpsinCameraPowerOffDialog: not show ");
            }
        }
    }

    private void showSpecialCharacterDialog(String message) {
        if (screenType == HOME) {
            MainActivity activity = ((MainActivity) getActivity());
            if (activity != null) {
                activity.showDialog = MainActivity.ShowDialog.SPECIAL_CHARACTER_DIALOG;
                activity.showDialog("", message, null);
            }
        }
    }


    private void showMessage(String message) {
        if (activity != null && !activity.isFinishing())
            Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void showProgressBar() {
        if (!hasShowProgressBar) {
            hasShowProgressBar = true;
            requireActivity().runOnUiThread(() -> homeViewModel.setHasShowProgressBar(true));
        }
    }

    private void hideProgressBar() {
        Log.e(TAG, "hideProgressBar: ");
        hasShowProgressBar = false;
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                binding.dotProgressBar.setVisibility(GONE);
                binding.dotProgressBar.smoothToHide();
                homeViewModel.setHasShowProgressBar(false);
            });
        }
        // binding.setIsProgressLoading(hasShowProgressBar);
        binding.infoImageView.setClickable(true);
        binding.galleryImageView.setClickable(true);
    }

    //addition: handled gallery and settings tablayout
    private void showHomeScreen() {

        Log.e(TAG, "showHomeScreen: isAddedOrNot " + isAdded() );
        if (!isAdded()) {
            // Safe to access views, update UI
            return;
        }
        if (fwMode == MODE_NONE) {
            firmwareUpdateSequence.clear();
            binding.progressBar.setProgress(0);
            binding.progressText.setText(getString(R.string.intial_percentage));
        }
        hideWiFiDisconnectProgressbar();
        hideProgressBar();

        Log.e(TAG, "showHomeScreen: " + screenType.name());
        /* for this condition while rotate device not set current screen to home */
        if (screenType != POP_UP_INFO_SCREEN && screenType != POP_UP_SETTINGS_SCREEN && screenType != INFO_SCREEN && screenType != GALLERY_SCREEN) {
            HomeViewModel.screenType = HOME;
        } else if (screenType == GALLERY_SCREEN) {
            Log.e(TAG, "showHomeScreen: " + screenType.name());
            homeViewModel.onCancelGalleryView();

        } else if (screenType == INFO_SCREEN) {
            Log.e(TAG, "showHomeScreen: " + screenType.name());
            homeViewModel.onCancelInfoView();
        } else if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN) {
            if (tcpConnectionViewModel.isSocketConnected().getValue() == STATE_CONNECTED) {
                homeViewModel.onPopUpViewCancel();
            }
        }
        /* for this after fw update via long press popup window device rotate fw update ui flicker, so avoid use this, now use this blocked code fw update ui */
//        if (screenType == FW_UPDATE_SCREEN && isSelectPopUpSettings || isSelectPopUpInfo)
//            HomeViewModel.screenType = HOME;

        // homeViewModel.currentScreen = HomeViewModel.CurrentScreen.HOME;
      // binding.loginLogoutIcon.setVisibility(VISIBLE);
        binding.selectDeviceLayout.setVisibility(VISIBLE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        binding.infoTabLayout.setVisibility(View.GONE);
        binding.firmwareUpdateLayout.setVisibility(View.GONE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        popUpTimeZoneListViewBinding.popUpTimeZoneList.setVisibility(View.GONE);

        if (homeViewModel != null) {
            homeViewModel.noRecordsFoundAll.removeObserver(isNoRecordAll);
            homeViewModel.noRecordsFoundAll.removeObservers(lifecycleOwner);

            homeViewModel.noRecordsFoundImage.removeObserver(isNoRecordImage);
            homeViewModel.noRecordsFoundImage.removeObservers(lifecycleOwner);

            homeViewModel.noRecordsFoundVideo.removeObserver(isNoRecordVideo);
            homeViewModel.noRecordsFoundVideo.removeObservers(lifecycleOwner);
        }
    }

    private void showFirmwareUpdateScreen() {
        Log.e(TAG, "showFirmwareUpdateScreen: called" );
        if (fwMode == MODE_NONE) {
            binding.progressBar.setProgress(0);
            binding.progressText.setText(getString(R.string.intial_percentage));
        }
        binding.loginLogoutIcon.setVisibility(GONE);
        HomeViewModel.screenType = FW_UPDATE_SCREEN;
        binding.selectDeviceLayout.setVisibility(View.GONE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        binding.infoTabLayout.setVisibility(View.GONE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
        recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
        binding.firmwareUpdateLayout.setVisibility(VISIBLE);
        binding.firmwareCameraUpload.setVisibility(VISIBLE);
        binding.firmwareCameraUploadDone.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
    }

    private Manifest readManifest() {
        showProgressBar();
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

    private void startBleDiscovery(boolean value) {
        try {
            if (value) {
                homeViewModel.getBleResults().clear();
                if (nearByDeviceAdapter != null) nearByDeviceAdapter.notifyDataSetChanged();
            }

            bleWiFiViewModel.setShouldStartBleDiscovery(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getWiFiHistoryCallback() {
        Disposable disposable = bleWiFiViewModel.getCameraList().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(wiFiHistories -> {
            if (wiFiHistories != null && !wiFiHistories.isEmpty()) {
                for (int j = 0; j < wiFiHistories.size(); j++) {
                    WiFiHistory wiFiHistory = wiFiHistories.get(j);
                    if (!bleWiFiViewModel.getCombinedCameraList().contains(wiFiHistory)) {
                        Log.e(TAG, "getWiFiHistoryCallback: IF " + wiFiHistory.getIs_wifi_connected() + " " + wiFiHistory.getCamera_ssid());
                        bleWiFiViewModel.getCombinedCameraList().add(wiFiHistory);
                        homeViewModel.selectedDeviceConnections(wiFiHistory, false);
                    } else {
//                        Log.e(TAG, "getWiFiHistoryCallback: ELSE " + wiFiHistory.getIs_wifi_connected() + " " + wiFiHistory.getCamera_ssid());
                        String wiFiSSID = getWiFiSSID(mWifiManager);
                        String camera_ssid = wiFiHistory.getCamera_ssid();
                        if (!wiFiSSID.equalsIgnoreCase(camera_ssid)) {
                            try {
                                Object object = bleWiFiViewModel.getBleScanResult().getValue();
                                if (object instanceof ArrayList) {
                                    ArrayList<WiFiHistory> value = (ArrayList<WiFiHistory>) object;
                                    boolean contains = false;
                                    if (value != null) {
                                        contains = value.contains(wiFiHistory);
                                    }
                                    if (contains) {
                                        wiFiHistory.setIs_wifi_connected(WIFI_AVAILABLE);
                                    } else {
                                        wiFiHistory.setIs_wifi_connected(WIFI_NOT_CONNECTED);
                                    }
                                } else {
                                    Log.e(TAG, "getWiFiHistoryCallback: " + object);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        int isWifiConnected = wiFiHistory.getIs_wifi_connected();
//                        Log.e(TAG, "getWiFiHistoryCallback: " + isWifiConnected + " " + camera_ssid);
                        for (int i = 0; i < bleWiFiViewModel.getCombinedCameraList().size(); i++) {
                            String camera_ssid1 = bleWiFiViewModel.getCombinedCameraList().get(i).getCamera_ssid();
                            if (camera_ssid1.equals(camera_ssid)) {
                                bleWiFiViewModel.getCombinedCameraList().get(i).setIs_wifi_connected(isWifiConnected);
                                homeViewModel.selectedDeviceConnections(wiFiHistory, false);
                            }
                        }
                    }
                }
                handleAutoConnect(wiFiHistories);
            } else {
                Log.e("TAG", "Camera History Not Available");
            }
        });
        compositeDisposable.add(disposable);
    }

    private void handleAutoConnect(List<WiFiHistory> wiFiHistories) {
        if (bleWiFiViewModel.getIsAutoConnectEnabled() == -1) {
            WiFiHistory wiFiHistory = wiFiHistories.get(0);
//            Log.e("TAG", "AUTOCONNECT TRUE");
            bleWiFiViewModel.setIsAutoConnectEnabled(0);
            String camera_ssid = wiFiHistory.getCamera_ssid();
            homeViewModel.setCameraSwitched(true);
            homeViewModel.selectedDeviceConnections(wiFiHistory, false);

            Log.e(TAG, "handleAutoConnect " + camera_ssid);
            if (isSDKBelow13()) {
                homeViewModel.connectedSsid = mWifiManager.getConnectionInfo().getSSID();
                if (homeViewModel.connectedSsid != null) {
                    homeViewModel.connectedSsid = homeViewModel.connectedSsid.replace("\"", "");
                }
                Log.d(TAG, "WiFiSSID: 12 and below devices");
            } else {
                if (bleWiFiViewModel.getNetworkCapability() != null) {
                    final TransportInfo transportInfo = ((NetworkCapabilities) bleWiFiViewModel.getNetworkCapability()).getTransportInfo();
                    if (transportInfo instanceof WifiInfo) {
                        final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                        homeViewModel.connectedSsid = wifiInfo.getSSID();
                        homeViewModel.connectedSsid = homeViewModel.connectedSsid.replace("\"", "");
                        Log.d(TAG, "WiFiSSID:  13 devices: " + homeViewModel.connectedSsid);
                    }
                } else {
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (networkCapabilities != null) {
                        final TransportInfo transportInfo = networkCapabilities.getTransportInfo();
                        if (transportInfo instanceof WifiInfo) {
                            final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                            String ssid = wifiInfo.getSSID();
                            homeViewModel.connectedSsid = wifiInfo.getSSID();
                            homeViewModel.connectedSsid = homeViewModel.connectedSsid.replace("\"", "");
                            Log.d(TAG, "WiFiSSID:  13 devices" + homeViewModel.connectedSsid);
                        }
                    }
                }
            }
            /* here handled for this condition while connect camera on settings screen and come back app to show last connected camera wifi connect request dialog(auto connect) and connect,
             so avoid that case and wifi connected camera.*/
//            if (wiFiHistories.size() == 1) {//Allow Auto connect only if wifi history has one camera
//                if (!currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
//                    bleWiFiViewModel.setShouldAutoConnectionEnabled(true);
//                }
//            }

            // To avoid 2 time wifi connection request only for NWD
            // New requirement - Removed autoconnect
           /* if (bleWiFiViewModel.isAutoConnectionEnabled() && !currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
                try {
                    if (homeViewModel.connectedSsid != null && !homeViewModel.connectedSsid.equals("") && !homeViewModel.connectedSsid.equals("<unknown ssid>")) {
                        if (isCamera()) {
                            if (homeViewModel.connectedSsid.equals(camera_ssid)) {
                                Log.e("TAG", "AUTOCONNECT LAST CONNECTED SSID TRUE " + camera_ssid);
                                bleWiFiViewModel.setSsidToRequestWiFiConnection(camera_ssid);
                            } else {
                                Log.e("TAG", "AUTOCONNECT CURRENTLY CONNECTED SSID--- LAST CONNECTED: " + wiFiHistory.getCamera_ssid() + " CURRENTLY CONNECTED SSID: " + homeViewModel.connectedSsid);
                                bleWiFiViewModel.setSsidToRequestWiFiConnection(homeViewModel.connectedSsid);
                            }
                        } else {
                            Log.e("TAG", "AUTOCONNECT LAST CONNECTED SSID TRUE " + camera_ssid);
                            bleWiFiViewModel.setSsidToRequestWiFiConnection(camera_ssid);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                notifyAdapters();
            }*/
        } else {
//            try {
//                connectedSsid = getWiFiSSID(mWifiManager);
//                connectedSsid = connectedSsid.replace("\"", "");
//                if (!connectedSsid.equals("") && !connectedSsid.equals("<unknown ssid>")) {
//                    if (isCamera()) {
//                        Log.e("TAG", "AUTOCONNECT FALSE");
//                        if (!bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager())) {
//                            homeViewModel.setInsertOrUpdateSsid(connectedSsid);
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }

    private List<WiFiHistory> wiFiHistories = new ArrayList<>();

    private final Observer<Object> observer = result -> {
        if (result != null) {
            if (result instanceof List) {
                wiFiHistories = (List<WiFiHistory>) result;
//                Log.e("TAG", "wiFiHistories Size " + wiFiHistories.size());
                homeViewModel.connectedSsid = getWiFiSSID(mWifiManager);
                if (!homeViewModel.connectedSsid.equals("") && !homeViewModel.connectedSsid.equals("<unknown ssid>")) {
                    if (isCamera()) {
                        if (!bleWiFiViewModel.hasAlreadyExistSSId(bleWiFiViewModel.getConnectedSsidFromWiFiManager())) {
                            homeViewModel.setInsertOrUpdateSsid(homeViewModel.connectedSsid);
                        }
                    }
                }
                if (!wiFiHistories.isEmpty()) {
                    for (int i = 0; i < wiFiHistories.size(); i++) {
                        WiFiHistory bleResult = wiFiHistories.get(i);
                        if (bleWiFiViewModel.getCombinedCameraList().contains(bleResult)) {
                            String camera_ssid = bleResult.getCamera_ssid();
                            if (!homeViewModel.connectedSsid.isEmpty() && isCamera()) {
                                if (camera_ssid.equals(homeViewModel.connectedSsid)) {
//                                    Log.e(TAG, "SSID WIFI_CONNECTED " + camera_ssid);
                                    bleWiFiViewModel.updateCameraConnectionState(bleResult.getCamera_ssid(), WIFI_CONNECTED);
                                    bleResult.setIs_wifi_connected(WIFI_CONNECTED);
                                    if (screenType == ADD_SCREEN) {
                                        homeViewModel.loadWifiHistory(bleResult);
                                    }
                                } else {
//                                    Log.e(TAG, "SSID WIFI_AVAILABLE1 " + camera_ssid);
                                    bleWiFiViewModel.updateCameraConnectionState(bleResult.getCamera_ssid(), WIFI_AVAILABLE);
                                    bleResult.setIs_wifi_connected(WIFI_AVAILABLE);
                                    if (screenType == ADD_SCREEN) {
                                        homeViewModel.loadWifiHistory(bleResult);
                                    }
                                    if (homeViewModel.getCameraLookupState().isLooking() && homeViewModel.getLookUpSSID().equalsIgnoreCase(bleResult.getCamera_ssid())) {
                                        homeViewModel.getCameraLookupState().transitionToFound();
                                        homeViewModel.setIsLookUpSSIDFound(true);
                                        Log.e(TAG, "setIsLookUpSSIDFound true");
                                    }

                                }
                            } else {
//                                Log.e(TAG, "SSID WIFI_AVAILABLE2 " + camera_ssid);
                                bleWiFiViewModel.updateCameraConnectionState(bleResult.getCamera_ssid(), WIFI_AVAILABLE);
                                bleResult.setIs_wifi_connected(WIFI_AVAILABLE);
                                if (screenType == ADD_SCREEN) {
                                    homeViewModel.loadWifiHistory(bleResult);
                                }
                                if (homeViewModel.getCameraLookupState().isLooking() && homeViewModel.getLookUpSSID().equalsIgnoreCase(bleResult.getCamera_ssid())) {
                                    homeViewModel.getCameraLookupState().transitionToFound();
                                    homeViewModel.setIsLookUpSSIDFound(true);
                                    Log.e(TAG, "setIsLookUpSSIDFound true");
                                }
                            }
                        } else {
//                            Log.e(TAG, "SSID WIFI_AVAILABLE3 " + bleResult.getCamera_ssid());
                            bleWiFiViewModel.updateCameraConnectionState(bleResult.getCamera_ssid(), WIFI_AVAILABLE);
                            bleResult.setIs_wifi_connected(WIFI_AVAILABLE);
                            bleWiFiViewModel.getCombinedCameraList().add(bleResult);
                            if (screenType == ADD_SCREEN) {
                                homeViewModel.loadWifiHistory(bleResult);
                            }
                            if (homeViewModel.getCameraLookupState().isLooking() && homeViewModel.getLookUpSSID().equalsIgnoreCase(bleResult.getCamera_ssid())) {
                                homeViewModel.getCameraLookupState().transitionToFound();
                                homeViewModel.setIsLookUpSSIDFound(true);
                                Log.e(TAG, "setIsLookUpSSIDFound true");
                            }
                        }
                    }
                    /*Get the difference between scanned result and db value*/
                    ArrayList<WiFiHistory> combinedCameraList = bleWiFiViewModel.getCombinedCameraList();
                    List<WiFiHistory> differences = combinedCameraList.stream().filter(element -> !wiFiHistories.contains(element)).collect(Collectors.toList());
                    for (int i = 0; i < differences.size(); i++) {
                        WiFiHistory wiFiHistory = differences.get(i);
                        if (!wiFiHistory.getCamera_ssid().equals(homeViewModel.connectedSsid)) {
                            wiFiHistory.setIs_wifi_connected(WIFI_NOT_CONNECTED);
                            bleWiFiViewModel.updateCameraConnectionState(wiFiHistory.getCamera_ssid(), WIFI_NOT_CONNECTED);
                        } else {
                            wiFiHistory.setIs_wifi_connected(WIFI_CONNECTED);
                            bleWiFiViewModel.updateCameraConnectionState(wiFiHistory.getCamera_ssid(), WIFI_CONNECTED);
                        }
                    }
                } else {
                    homeViewModel.loadWifiHistory(null);
                    binding.setIsLoading(false);
                    binding.nearByDeviceViewRefreshIcon.setVisibility(VISIBLE);
                    bleWiFiViewModel.updateAllCameraConnectionState(WIFI_NOT_CONNECTED);
                }
            } else if (result instanceof Boolean) {
                boolean startStopState = (boolean) result;
                binding.setIsLoading(startStopState);
                if (startStopState) {
                    binding.nearByDeviceViewRefreshIcon.setVisibility(View.GONE);
                } else {
                    binding.nearByDeviceViewRefreshIcon.setVisibility(VISIBLE);
                    if (homeViewModel.getCameraLookupState().isLooking()) {
                        homeViewModel.getCameraLookupState().transitionToNotFound();
                        homeViewModel.setIsLookUpSSIDFound(false);
                        Log.e(TAG, "setIsLookUpSSIDFound false");
                    }
                }
            }

        } else {
            Log.e(TAG, "BleObserver is Null ");
        }
    };

    private void insertOrUpdate(String ssid) {
        Log.e(TAG, "insertOrUpdate: " + ssid);
        bleWiFiViewModel.checkSsidIsExit(ssid).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<WiFiHistory>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSuccess(WiFiHistory wiFiHistory) {
                try {
                    String connectedSSSID = getWiFiSSID(mWifiManager);
                    String camera_ssid = wiFiHistory.getCamera_ssid();
                    Log.e(TAG, "onSuccess: " + connectedSSSID + " " + camera_ssid);
                    if (connectedSSSID.equals(camera_ssid)) {
                        Log.e(TAG, "insertOrUpdate: onSuccess " + connectedSSSID + " " + camera_ssid);
                        if (!homeViewModel.isNwdCameraPressed())
                            setWiFiHistory(wiFiHistory);
                        wiFiHistory.setIs_wifi_connected(WIFI_CONNECTED);
                        long last_connected_date_time = System.currentTimeMillis();
                        bleWiFiViewModel.updateCameraLastConnectedTimeAndState(camera_ssid, last_connected_date_time, WIFI_CONNECTED);
                        bleWiFiViewModel.updateLastConnectedCameraState(camera_ssid);
                    } else {
                        Log.e(TAG, "insertOrUpdate: onSuccess " + wiFiHistory.getCamera_ssid());
                        wiFiHistory.setIs_wifi_connected(WIFI_AVAILABLE);
                        long last_connected_date_time = System.currentTimeMillis();
                        bleWiFiViewModel.updateCameraLastConnectedTimeAndState(camera_ssid, last_connected_date_time, WIFI_AVAILABLE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "insertOrUpdate: onError " + ssid);
                String camera_mac_address = "";
                String camera_fw_version = "";
                String camera_password = "";
                long last_connected_date_time = System.currentTimeMillis();

                String connectedSSSID = getWiFiSSID(mWifiManager);
                updateLastConnectedSsid();
                WiFiHistory wiFiHistory;
                if(connectedSSSID.equals(ssid)){
                    wiFiHistory = new WiFiHistory(ssid, ssid, camera_mac_address, camera_fw_version, camera_password, last_connected_date_time, WIFI_CONNECTED);
                }else {
                    wiFiHistory = new WiFiHistory(ssid, ssid, camera_mac_address, camera_fw_version, camera_password, last_connected_date_time, WIFI_AVAILABLE);
                }
//                WiFiHistory wiFiHistory = new WiFiHistory(ssid, ssid, camera_mac_address, camera_fw_version, camera_password, last_connected_date_time, WIFI_AVAILABLE);
                bleWiFiViewModel.insertCamera(wiFiHistory);
            }
        });

    }

    private void updateLastConnectedSsid(){
        String ssid = bleWiFiViewModel.getOnAvailableSSID();
        Log.e(TAG, "updateLastConnectedSsid: " + ssid);
        if (ssid != null){
            bleWiFiViewModel.checkSsidIsExit(ssid).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<WiFiHistory>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onSuccess(WiFiHistory wiFiHistory) {
                    if (wiFiHistory != null){
                        Log.e(TAG, "onSuccess: " + wiFiHistory.getCamera_ssid());
                        bleWiFiViewModel.updateCameraConnectionState(wiFiHistory.getCamera_ssid(),WIFI_AVAILABLE);
                    }
                }

                @Override
                public void onError(Throwable e) {

                }
            });
        }
    }


    private void setNearByDeviceAdapter(WiFiHistory nearByDeviceModels) {
        if (!homeViewModel.getBleResults().contains(nearByDeviceModels)) {
            if (nearByDeviceAdapter != null) {
                nearByDeviceAdapter.setValue(nearByDeviceModels);
            }
        }
        if (nearByDeviceAdapter == null) {
            if (!homeViewModel.getBleResults().contains(nearByDeviceModels)) {
                homeViewModel.getBleResults().add(nearByDeviceModels);
            }
            nearByDeviceAdapter = new NearByDeviceAdapter(homeViewModel.getBleResults(), requireContext(), homeViewModel, bleWiFiViewModel);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            binding.rvHistory.setLayoutManager(layoutManager);
            binding.rvHistory.setAdapter(nearByDeviceAdapter);
        }
    }

    public String getWiFiSSID(WifiManager mWifiManager) {
        String wifiSSID = "";
        if (isSDKBelow13()) {
            wifiSSID = mWifiManager.getConnectionInfo().getSSID();
            wifiSSID = wifiSSID.replace("\"", "");
//            Log.d(TAG, "getWiFiSSID: 12 and below devices");
        } else {
            if (bleWiFiViewModel.getNetworkCapability() != null) {
                final TransportInfo transportInfo = ((NetworkCapabilities) bleWiFiViewModel.getNetworkCapability()).getTransportInfo();
                if (transportInfo instanceof WifiInfo) {
                    final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                    wifiSSID = wifiInfo.getSSID();
                    wifiSSID = wifiSSID.replace("\"", "");
//                    Log.d(TAG, "getWiFiSSID:  13 devices" + wifiSSID);
                }
            }
        }
        return wifiSSID;
    }

    private boolean isCamera() {
        return homeViewModel.connectedSsid.contains(FILTER_STRING1) || homeViewModel.connectedSsid.contains(FILTER_STRING2) || homeViewModel.connectedSsid.contains(FILTER_STRING3) || homeViewModel.connectedSsid.contains(FILTER_STRING4) || homeViewModel.connectedSsid.contains(FILTER_STRING5) || homeViewModel.connectedSsid.contains(FILTER_STRING6);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void notifyAdapters() {
        try {
            if (nearByDeviceAdapter != null) nearByDeviceAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            Log.d("EraseDElete", "showConfirmEraseGalleryItemDialog: ");
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
        }
    }

    private void showPopUpCameraInfoScreen() {
        HomeViewModel.screenType = HomeViewModel.ScreenType.POP_UP_INFO_SCREEN;
        setCameraInfoAdapter();
        binding.selectDeviceLayout.setVisibility(View.GONE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        binding.infoTabLayout.setVisibility(View.GONE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(VISIBLE);
        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        binding.galleryImageView.setClickable(true);
        binding.infoImageView.setClickable(true);
        hasShowProgressBar = false;
        mHandler.post(this::hideProgressBar);

        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

    }

    private void showPopUpCameraSettingsScreen() {
        HomeViewModel.screenType = HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
        setCameraSettingAdapter();
        binding.selectDeviceLayout.setVisibility(View.GONE);
        binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
        binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
        binding.infoTabLayout.setVisibility(View.GONE);
        binding.galleryTabLayout.setVisibility(View.GONE);
        binding.settingsTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
        fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(VISIBLE);
        binding.galleryImageView.setClickable(true);
        binding.infoImageView.setClickable(true);
        hasShowProgressBar = false;
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        mHandler.post(this::hideProgressBar);
    }

    private void disConnectCamera() {
        if (tcpConnectionViewModel != null) {
            Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
            if (value != null && value == Constants.STATE_CONNECTED) {
                tcpConnectionViewModel.disconnectSocket();
                tcpConnectionViewModel.isSocketConnected().removeObservers(lifecycleOwner);
            }
        }
    }

    private void addButtonStateHandling() {
        if (HomeViewModel.addButtonState == HomeViewModel.AddButtonState.INIT) {
            homeViewModel.initiateSelectDeviceView();
            HomeViewModel.addButtonState = HomeViewModel.AddButtonState.INITIATE_FINISH;
        }
    }

    //addition: added screen type setting and gallery screen
    private void viewGroupStateHandling() {
        if (HomeViewModel.screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(VISIBLE);
            if (activity.isInMultiWindowMode()) {
                binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            } else {
                binding.selectedDeviceHistoryLayout.setVisibility(View.VISIBLE);
            }
            binding.infoTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(View.GONE);
            binding.settingsTabLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HOME) {
            binding.selectDeviceLayout.setVisibility(VISIBLE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(View.GONE);
            binding.settingsTabLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.INFO_SCREEN) {
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(View.GONE);
            binding.settingsTabLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(VISIBLE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_SCREEN) {
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(VISIBLE);
            binding.settingsTabLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN) {
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(VISIBLE);
            binding.galleryManageOptionButtons.setVisibility(VISIBLE);
            binding.galleryManageButton.setVisibility(View.GONE);
            binding.galleryShareButton.setVisibility(View.GONE);

            binding.settingsTabLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN) {
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(VISIBLE);
            binding.settingsTabLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(VISIBLE);
            binding.settingsTabLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == HomeViewModel.ScreenType.SETTINGS_SCREEN) {
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(View.GONE);
            binding.settingsTabLayout.setVisibility(VISIBLE);
            binding.firmwareUpdateLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == FW_UPDATE_SCREEN) {
            recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
            recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
            binding.selectDeviceLayout.setVisibility(View.GONE);
            binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
            binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
            binding.infoTabLayout.setVisibility(View.GONE);
            binding.galleryTabLayout.setVisibility(View.GONE);
            binding.settingsTabLayout.setVisibility(View.GONE);
            binding.firmwareUpdateLayout.setVisibility(VISIBLE);
            fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
            fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
        } else if (HomeViewModel.screenType == POP_UP_INFO_SCREEN && !homeViewModel.hasShowFwDialogNone()) {
            if (tcpConnectionViewModel != null) {
                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                if (value != null && value == Constants.STATE_CONNECTED) {
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    binding.selectDeviceLayout.setVisibility(View.GONE);
                    binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
                    binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
                    binding.infoTabLayout.setVisibility(View.GONE);
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    binding.settingsTabLayout.setVisibility(View.GONE);
                    binding.firmwareUpdateLayout.setVisibility(View.GONE);
                    fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(VISIBLE);
                    fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
                }
            }
        } else if (HomeViewModel.screenType == POP_UP_SETTINGS_SCREEN && !homeViewModel.hasShowFwDialogNone()) {
            if (tcpConnectionViewModel != null) {
                Integer value = tcpConnectionViewModel.isSocketConnected().getValue();
                if (value != null && value == Constants.STATE_CONNECTED) {
                    binding.selectDeviceLayout.setVisibility(View.GONE);
                    binding.nearByDevicesHistoryLayout.setVisibility(View.GONE);
                    binding.selectedDeviceHistoryLayout.setVisibility(View.GONE);
                    binding.infoTabLayout.setVisibility(View.GONE);
                    binding.galleryTabLayout.setVisibility(View.GONE);
                    binding.settingsTabLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingInfoLayout.setVisibility(View.GONE);
                    recordingInfoScreenBinding.recordingVideoPlayerLayout.setVisibility(View.GONE);
                    binding.firmwareUpdateLayout.setVisibility(View.GONE);
                    fragmentPopUpLayoutBinding.popUpInfoTabLayout.setVisibility(View.GONE);
                    fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(VISIBLE);

                    /* For this after popup window appear removed camera mode observer */
                    if (CameraViewModel.hasNewFirmware()) {
                        tcpConnectionViewModel.observeCameraMode().removeObserver(observeCameraMode);
                        tcpConnectionViewModel.observeCameraMode().removeObservers(lifecycleOwner);
                        tcpConnectionViewModel.observeCameraVideoMode().removeObserver(observeCameraVideoMode);
                        tcpConnectionViewModel.observeCameraVideoMode().removeObservers(this.getViewLifecycleOwner());
                    }
                } else {
                    Log.e(TAG, "viewGroupStateHandling: socket closed " + screenType);
                }
            }
        }
    }

    private void selectDeviceOrientationHandling() {

        boolean isTooltipShown = toolTipSharedPreference.getBoolean("isToolTipShown", false);
        Log.e(TAG, "selectDeviceOrientationHandling: isTooltipShown : " + isTooltipShown + " onForeground : " + CameraViewModel.orientationHandled);


        if (adapter == null) {
            adapter = new SelectDeviceAdapter(homeViewModel.getSelectDeviceModelss(), requireActivity(), homeViewModel, requireActivity(), isTooltipShown);
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                int verticalSpace = getResources().getInteger(R.integer.port_recycler_view_container_items_vertical_space);
                try {
                    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
                    binding.selectedDeviceRecyclerView.setLayoutManager(layoutManager);
                    VerticalSpaceItemDecoration verticalSpaceItemDecoration = new VerticalSpaceItemDecoration(verticalSpace);
                    binding.selectedDeviceRecyclerView.addItemDecoration(verticalSpaceItemDecoration);
                    binding.selectedDeviceRecyclerView.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                int verticalSpace = getResources().getInteger(R.integer.land_recycler_view_container_items_vertical_space);

                try {
                    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 6);
                    if (activity.isInMultiWindowMode()) {
                        layoutManager = new GridLayoutManager(requireContext(), 4);
                    }
                    binding.selectedDeviceRecyclerView.setLayoutManager(layoutManager);
                    HorizontalSpaceItemDecoration horizontalSpaceItemDecoration = new HorizontalSpaceItemDecoration(verticalSpace);
                    binding.selectedDeviceRecyclerView.addItemDecoration(horizontalSpaceItemDecoration);
                    binding.selectedDeviceRecyclerView.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (selectedDeviceHistoryAdpter == null) {
            selectedDeviceHistoryAdpter = new SelectedDeviceHistoryAdpter(homeViewModel.getDeviceHistoryModelss(), requireContext(), homeViewModel, bleWiFiViewModel);
            LinearLayoutManager layoutManager;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
            } else {
                layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
            }
            binding.selectedItemHistory.setLayoutManager(layoutManager);
            binding.selectedItemHistory.setAdapter(selectedDeviceHistoryAdpter);
        }
    }

    private MainActivity activity = null;

    private void showWiFiDisconnectProgressbar() {
        Log.e(TAG, "showWiFiDisconnectProgressbar: ");
        if (homeViewModel.getNavController() != null) {
            homeViewModel.getNavController().navigate(R.id.customProgressDialogFragment);
        }
    }

    private void hideWiFiDisconnectProgressbar() {
        try {
            if (homeViewModel.getNavController() != null && homeViewModel.getNavController().getCurrentDestination().getId() == R.id.customProgressDialogFragment) {
                homeViewModel.getNavController().popBackStack();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = ((MainActivity) context);
    }

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


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView: called " );
        fragmentPopUpLayoutBinding.popUpSettingsViewPager.setAdapter(null);
        fragmentPopUpLayoutBinding.popUpViewPager.setAdapter(null);

        if (tcpConnectionViewModel != null) {
            tcpConnectionViewModel.observeReleaseVersion().removeObserver(observeBootMode);
            tcpConnectionViewModel.observeRiscBootMode().removeObservers(lifecycleOwner);

            tcpConnectionViewModel.observeCameraInfo().removeObserver(observeCameraInfo);
            tcpConnectionViewModel.observeCameraInfo().removeObservers(lifecycleOwner);

            tcpConnectionViewModel.observeRisvVersion().removeObserver(observeRiscVersion);
            tcpConnectionViewModel.observeRisvVersion().removeObservers(lifecycleOwner);

            tcpConnectionViewModel.observeFpgaVersion().removeObserver(observeFpgeVersion);
            tcpConnectionViewModel.observeFpgaVersion().removeObservers(lifecycleOwner);

            tcpConnectionViewModel.observeReleaseVersion().removeObserver(observeReleaseVersion);
            tcpConnectionViewModel.observeReleaseVersion().removeObservers(lifecycleOwner);

            tcpConnectionViewModel.observeCameraName().removeObserver(observeCameraName);
            tcpConnectionViewModel.observeCameraName().removeObservers(lifecycleOwner);
        }
        compositeDisposable.dispose();
    }

    //runnable for gallery
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (homeViewModel.mCurrentState) {
                recordingInfoScreenBinding.recordingVideoPlayerVideoView.setVisibility(VISIBLE);
                int currentDuration = recordingInfoScreenBinding.recordingVideoPlayerVideoView.getCurrentPosition();
                String currentDurationString = getDurationString(currentDuration);
                int totalDuration = recordingInfoScreenBinding.recordingVideoPlayerVideoView.getDuration();
                String totalDurationString = getDurationString(totalDuration);
                recordingInfoScreenBinding.recordingVideoPlayerVideoCurrentDuration.setText(currentDurationString);
                recordingInfoScreenBinding.recordingVideoPlayerVideoTotalDuration.setText(totalDurationString);
                recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setProgress(currentDuration);
                Log.i(TAG, "onPrepared: mState " + mState);
                mHandler.postDelayed(this, 100);
            }
        }
    };

    //recording video player layout: Total Video duration- calculation
    @SuppressLint("DefaultLocale")
    public String getTotalVideoDuration(long value) {
        String videoTotalTime;
        long s = value / 1000;
        videoTotalTime = String.format("%dh %02dm %02ds", s / 3600, (s % 3600) / 60, (s % 60));
        return videoTotalTime;
    }

    @SuppressLint("DefaultLocale")
    public String getVideoDuration(long value) {
        String VideoTotalDuration;
        long s = value / 1000;
        VideoTotalDuration = String.format("%dh %02dm %02ds", s / 3600, (s % 3600) / 60, (s % 60));
        return VideoTotalDuration;
    }

    //recording video player layout: Current Video duration-calculation
    @SuppressLint("DefaultLocale")
    public String getDurationString(int value) {
        String videoCurrentDuration;
        long s = value / 1000;
        videoCurrentDuration = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
        return videoCurrentDuration;
    }

    @Override
    public void onPause() {
        super.onPause();
//        backButton = true;
        homeViewModel.pos = recordingInfoScreenBinding.recordingVideoPlayerVideoView.getCurrentPosition();
        homeViewModel.mCurrentState = recordingInfoScreenBinding.recordingVideoPlayerVideoView.isPlaying();
        Log.d("HOMETAG", "onPause: " + homeViewModel.pos + " " + homeViewModel.mCurrentState);
        if (manageViewTabAdapter != null) {
            HomeViewModel.selectedtab = "GALLERY";
            homeViewModel.setCurrentManageTabPosition(binding.galleryViewPager.getCurrentItem());
        }
        if (seekState == 0 && mediaPlayer != null) {
            Log.d(TAG, "onPause:seekstate " + seekState);
            recordingInfoScreenBinding.recordingVideoPlayerVideoView.seekTo(seekState);
            recordingInfoScreenBinding.recordingVideoPlayerSeekbar.setProgress(seekState);
            //mediaPlayer.seekTo(0, MediaPlayer.SEEK_CLOSEST);
            // mediaPlayer.seekTo(0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        handleSplitScreenEnabled();
        /*
        while fw updating function to rotate the device
        shown popup setting screen for avoiding the condition used
         */
        Log.e(TAG, "onResume: HOME " + screenType.name());
        if (!activity.isInMultiWindowMode()) {
            if (screenType == FW_UPDATE_SCREEN)
                binding.loginLogoutIcon.setVisibility(GONE);
           // else
           //     binding.loginLogoutIcon.setVisibility(VISIBLE);
        }

        if (fwMode == MODE_NONE) {
            if (screenType == POP_UP_SETTINGS_SCREEN) {
                if (!isShowTimeZoneLayout()) {
                    fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(VISIBLE);
                    showPopUpCameraSettingsScreen();
                    popUpTimeZoneListViewBinding.popUpTimeZoneList.setVisibility(GONE);
                } else {
                    fragmentPopUpLayoutBinding.popUpSettingsLayout.setVisibility(View.GONE);
                    fragmentPopUpLayoutBinding.popUpTimeZoneListLayout.popUpTimeZoneList.setVisibility(VISIBLE);
                    showTimeZoneListViewLayout();
                }
            }

            if (screenType == POP_UP_INFO_SCREEN) showPopUpCameraInfoScreen();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (homeViewModel != null) {
            homeViewModel.isSeekbarSeeked = false;
        }
        Log.d(TAG, "Output Got:onDestroy: ");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "Output Got:onStop: ");
    }
}