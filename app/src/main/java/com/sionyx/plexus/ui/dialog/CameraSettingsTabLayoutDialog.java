package com.sionyx.plexus.ui.dialog;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.URL_WEB_SETTINGS_ACTION;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.getLoadUrl;
import static com.sionyx.plexus.ui.camera.CameraFragment.isInfoButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isSettingButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraViewModel.cameraXValue;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.HOME;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCameraTabLayoutBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.camera.menus.CameraTabLayoutViewModel;
import com.sionyx.plexus.ui.camera.menus.MyFragmentAdapter;

import java.util.Objects;

public class CameraSettingsTabLayoutDialog extends DialogFragment {
    private static final String TAG = "CameraTabLayoutFragment";

    private CameraTabLayoutViewModel viewModel;
    private CameraViewModel cameraViewModel;
    private FragmentCameraTabLayoutBinding binding;
    private LifecycleOwner lifecycleOwner;
    private BleWiFiViewModel bleWiFiViewModel;
    private TCPConnectionViewModel tcpConnectionViewModel;
    private String cameraSSId = "";
    public static long clickedTimeStamp = -1;
    public static int previousPos = 0;
    public static String mStrCurrentTab = "NONE";

    private ValueCallback<Uri[]> fileUploadCallback;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::handleFilePickerResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context ctx = getActivity();
        binding = FragmentCameraTabLayoutBinding.inflate(inflater, null, false);
        lifecycleOwner = this;
        viewModel = new ViewModelProvider(requireActivity()).get(CameraTabLayoutViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(requireActivity()).get(TCPConnectionViewModel.class);
        binding.setViewModel(viewModel);

        cameraSSId = bleWiFiViewModel.getConnectedSsidFromWiFiManager();
        binding.setCameraName(cameraSSId);

        if (cameraSSId.contains(FILTER_STRING1)
                || cameraSSId.contains(FILTER_STRING2)
                || cameraSSId.contains(FILTER_STRING3)) {
            binding.cameraModelIcon.setImageResource(R.drawable.ic_nw_analog_connected);
        }

        if (cameraSSId.contains(FILTER_STRING4) || cameraSSId.contains(FILTER_STRING5)) {
            binding.cameraModelIcon.setImageResource(R.drawable.opsin_connected);
        }

        if (cameraSSId.contains(FILTER_STRING6)) {
            viewModel.setShowCircleProgress(true);
            binding.cameraModelIcon.setVisibility(VISIBLE);
            binding.cameraModelIcon.setImageResource(R.drawable.ic_nw_digital_connected);
        }

        binding.cameraSettingsLayout.setVisibility(View.VISIBLE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscribeUI();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void subscribeUI() {
        setCancelable(false);
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                viewModel.setShowCircleProgress(false);
                binding.cameraSettingNwDigitalLayout.nwDigitalWebView.setVisibility(GONE);
                binding.cameraSettingNwDigitalLayout.llWebview.setVisibility(GONE);
                binding.webCameraSettingsCancelIcon.setVisibility(GONE);
                binding.cameraSettingsLayoutContainer.setVisibility(VISIBLE);

                binding.cameraSettingsLayoutContainer.setBackgroundResource(R.drawable.ic_tab_layout_background);
                if (CameraViewModel.hasNewFirmware()) {
                    binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_camera_info));
                    binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
                } else {
                    ConstraintLayout.LayoutParams newLayoutParams = (ConstraintLayout.LayoutParams) binding.tabLayout.getLayoutParams();
                    binding.tabLayout.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_tab_rectangle));
                    binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
                    newLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                    binding.tabLayout.setLayoutParams(newLayoutParams);
                }
                break;
            case OPSIN:
                viewModel.setShowCircleProgress(false);
                binding.cameraSettingNwDigitalLayout.nwDigitalWebView.setVisibility(GONE);
                binding.cameraSettingNwDigitalLayout.llWebview.setVisibility(GONE);
                binding.webCameraSettingsCancelIcon.setVisibility(GONE);
                binding.cameraSettingsLayoutContainer.setVisibility(VISIBLE);

                binding.cameraSettingsLayoutContainer.setBackgroundResource(R.drawable.ic_tab_layout_background_with_border);
                binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_camera_info));
                binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_camera_settings));
                break;
            case NIGHTWAVE_DIGITAL:
                WebView webView = binding.cameraSettingNwDigitalLayout.nwDigitalWebView;
                binding.cameraSettingNwDigitalLayout.llWebview.setVisibility(VISIBLE);
                webView.setVisibility(VISIBLE);
                try {
                    setWebView(webView);
                    webView.loadUrl(getLoadUrl(URL_WEB_SETTINGS_ACTION));
                    loadWebCallback(webView);

                } catch (Exception e) {
                    Log.e(TAG, "Exception_thrown_webview: " + e.getMessage());
                }
                binding.cameraModelIcon.setImageResource(R.drawable.ic_nw_digital_connected);

                binding.cameraSettingsLayoutContainer.setVisibility(View.GONE);
                binding.cameraSettingsLayout.setVisibility(VISIBLE);
                binding.cameraModelIcon.setVisibility(VISIBLE);
                binding.cameraIconName.setVisibility(VISIBLE);
                binding.webCameraSettingsCancelIcon.setVisibility(VISIBLE);

                break;
        }
        binding.tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        /* for this avoid continues observer and get command response*/
        CameraViewModel.setOpsinCommandInitiateByDialogFragment(true);
        CameraViewModel.setHasCommandInitiate(true);

        MyFragmentAdapter adapter = new MyFragmentAdapter(this, binding.tabLayout.getTabCount());
        binding.viewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
        }).attach();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    onFirstTabVisibility();
                } else {
                   onSecondTabVisibility();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    onFirstTabVisibility();
                } else {
                    onSecondTabVisibility();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    onFirstTabVisibility();
                } else {
                    onSecondTabVisibility();
                }
            }
        });

        switch (currentCameraSsid) {
            case NIGHTWAVE:
                if (CameraViewModel.hasNewFirmware()) {
                    binding.tabLayout.getTabAt(0).setIcon(R.drawable.ic_camera_info);
                    binding.tabLayout.getTabAt(1).setIcon(R.drawable.ic_camera_settings);
                } else {
                    binding.tabLayout.getTabAt(0).setIcon(R.drawable.ic_camera_settings);
                    binding.preserSaveLayout.setVisibility(View.VISIBLE);
                }
                break;
            case OPSIN:
                binding.tabLayout.getTabAt(0).setIcon(R.drawable.ic_camera_info);
                binding.tabLayout.getTabAt(1).setIcon(R.drawable.ic_camera_settings);
                break;
            case NIGHTWAVE_DIGITAL:
                //tab icon visibility
                binding.cameraSettingNwDigitalLayout.nwDigitalWebView.setVisibility(VISIBLE);
                break;
        }

        viewModel.onShowCircleProgress().observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.e(TAG, "onShowCircleProgress: " + aBoolean);
            if (aBoolean) {
                binding.setIsLoading(true);
                binding.customProgressBar.smoothToShow();
            } else {
                binding.setIsLoading(false);
                binding.customProgressBar.smoothToHide();
            }
        }));

        viewModel.isCancel.observe(lifecycleOwner, new EventObserver<>(hasSelect ->
        {
            if (hasSelect) {
                clickedTimeStamp = -1;
                previousPos = 0;
                mStrCurrentTab = "NONE";
                CameraViewModel.setHasPressedSettingCancelIcon(true);
                CameraViewModel.setHasVisibleSettingsInfoView(false); // for background to foreground not shoe usb mode dialog usecase handled
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
            }
        }));

        viewModel.isSaveSettings.observe(lifecycleOwner, new EventObserver<>(hasSelect ->
        {
            if (hasSelect) {
                // here to show alert dialog with edit text view
                showCameraSettingsSaveDialog(getString(R.string.give_the_saved_settings_name_msg));
            }
        }));

        /* for this condition avoid when connect Opsin camera don't observe camera name */
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                tcpConnectionViewModel.getCameraName();
                tcpConnectionViewModel.observeCameraName().observe(requireActivity(), object -> {
                    Log.e("TAG", "observeCameraInfo: " + object);
                    if (object != null) {
                        if (object instanceof CommandError) {
                            binding.setCameraName(cameraSSId);
                        } else {
                            String response = object.toString();
                            if (!response.equals("")) {
                                binding.setCameraName(response.trim());
                            } else {
                                binding.setCameraName(cameraSSId);
                            }
                        }
                    }
                });
                break;
            case OPSIN:
                /*Opsin*/
                tcpConnectionViewModel.getOpsinCameraName();
                tcpConnectionViewModel.observeOpsinGetCameraName().observe(lifecycleOwner, new EventObserver<>(object -> {
                    if (object != null) {
                        Log.e(TAG, "observeOpsinCameraName");
                        if (object instanceof CommandError) {
                            binding.setCameraName(cameraSSId);
                        } else {
                            String response = object.toString();
                            if (!response.equals("")) {
                                binding.setCameraName(!response.trim().equals("") ? response.trim() : cameraSSId);
                            } else {
                                binding.setCameraName(cameraSSId);
                            }
                        }
                    }
                }));
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e("TAG", "observeCameraName: NW_Digital" );
                binding.setCameraName(cameraSSId);
                binding.cameraModelIcon.setImageResource(R.drawable.ic_nw_digital_connected);
                break;
        }

        if (requireActivity().isInMultiWindowMode()) {
            viewModel.onCancel();
        }
        cameraViewModel.isEnableMultiWindowMode.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.e(TAG, "getEnterExitMultiWindowMode: " + aBoolean);
                viewModel.onCancel();
            }
        }));

        // for this invisible and visible settings dialog ui while opening time zone dialog
        cameraViewModel.isShowSettingsDialog.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean)
                binding.cameraSettingsLayout.setVisibility(View.VISIBLE);
            else
                binding.cameraSettingsLayout.setVisibility(View.INVISIBLE);
        }));
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

    private void loadWebCallback(WebView webView) {
        webView.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.e(TAG, "onPageFinished " + url);
                viewModel.setShowCircleProgress(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "onReceivedError " + error);
                webPageIsNotLoading();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                webPageIsNotLoading();
                Log.e(TAG, "onReceivedHttpError " + errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String currentIp = Constants.getCameraDynamicIpAddress();
                String url = error.getUrl();
                if (url != null && currentIp != null && url.contains(currentIp)) {
                    Log.w("WebViewSSL", " Proceeding with trusted camera IP SSL" );
                    handler.proceed(); // safe for your camera
                } else {
                    handler.cancel();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                // Open file picker
                Intent intent = fileChooserParams.createIntent();
                try {
                    filePickerLauncher.launch(intent);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    return false;
                }
                return true;
            }
        });

        // if web url is not loading after 15 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (webView.getVisibility() != VISIBLE){
                webPageIsNotLoading();
            }
        }, 15000); // 15 seconds
    }

    private void webPageIsNotLoading() {
        viewModel.setShowCircleProgress(false);
        binding.cameraSettingNwDigitalLayout.tvWebViewNotReachable.setVisibility(VISIBLE);
        binding.cameraSettingNwDigitalLayout.nwDigitalWebView.setVisibility(GONE);
        binding.webCameraSettingsCancelIcon.setImageResource(R.drawable.ic_black_clr_cancel_icon);
    }

    private void handleFilePickerResult(ActivityResult result) {
        if (fileUploadCallback == null) return;

        Uri[] resultUris = null;
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            resultUris = new Uri[]{result.getData().getData()};
        }

        fileUploadCallback.onReceiveValue(resultUris);
        fileUploadCallback = null;
    }

    private void onSecondTabVisibility() {
        switch (currentCameraSsid) {
            case OPSIN:
                if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                    binding.preserSaveLayout.setVisibility(View.VISIBLE);
                } else {
                    // below code handel v23 and above to hide and show save settings alyout
                    binding.preserSaveLayout.setVisibility(GONE);
                }
                break;
            case NIGHTWAVE:
                binding.preserSaveLayout.setVisibility(View.VISIBLE);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"cameraSetting_tab2: NW_Digital");
                break;
        }
    }

    private void onFirstTabVisibility() {
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                if (CameraViewModel.hasNewFirmware())
                    binding.preserSaveLayout.setVisibility(GONE);
                else
                    binding.preserSaveLayout.setVisibility(View.VISIBLE);
                break;
            case OPSIN:
                binding.preserSaveLayout.setVisibility(GONE);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"cameraSetting_tab1: NW_Digital");
                break;
        }
    }

    private void showCameraSettingsSaveDialog(String message) {
        if (screenType == HOME) {
            MainActivity activity = ((MainActivity) getActivity());
            if (activity != null) {
                activity.showDialog = MainActivity.ShowDialog.SAVE_CAMERA_SETTINGS;
                activity.showDialog("SAVE SETTINGS", message, null);
            }
        }
    }

    private void backToLiveScreen() {
        isInfoButtonPressed = false;
        isSettingButtonPressed = false;
        if (binding.getRoot().getVisibility() == View.VISIBLE) {
            cameraViewModel.setIsShowAlertDialog(false);
            dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG,"onResume called ");
        assert getDialog() != null;
        WindowManager.LayoutParams params = Objects.requireNonNull(getDialog().getWindow()).getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent);
        getDialog().getWindow().setAttributes(params);
    }
}
