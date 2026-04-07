package com.sionyx.plexus.ui.cameramenu.fragment;


import static com.dome.librarynightwave.utils.Constants.setUpdateTransferProtocol;
import static com.sionyx.plexus.R.drawable.*;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.getWiFiHistory;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;
import static com.sionyx.plexus.utils.Constants.apiErrorMessage;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.utils.Constants;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCameraMenuBottomDialogBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResponseCallback;
import com.sionyx.plexus.ui.cameramenu.adapter.CameraMenuAdapter;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.cameramenu.model.CameraMenuData;
import com.sionyx.plexus.utils.EventObserver;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** NWD Bottom sheet */
public class CameraMenuBottomDialog extends BottomSheetDialogFragment {

     private static final String TAG = "CameraMenuBottomDialog";
     private FragmentCameraMenuBottomDialogBinding cameraMenuBinding;
     private HomeViewModel homeViewModel;
     private CameraPasswordSettingViewModel cameraChangePasswordViewModel;
     private DigitalCameraInfoViewModel digitalCameraInfoViewModel;

     private Handler mHandler = new Handler(Looper.getMainLooper());

     private WiFiHistory wiFiHistory;

     private LifecycleOwner lifecycleOwner;

     private final List<CameraMenuData> cameraMenuData = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.dialog_theme);
        lifecycleOwner = this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        cameraMenuBinding = FragmentCameraMenuBottomDialogBinding.inflate(inflater, container, false);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        cameraChangePasswordViewModel = new ViewModelProvider(requireActivity()).get(CameraPasswordSettingViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(requireActivity()).get(DigitalCameraInfoViewModel.class);
        cameraMenuBinding.setViewModel(cameraChangePasswordViewModel);
        wiFiHistory = getWiFiHistory();

        cameraMenuData.add(new CameraMenuData(getString(R.string.delete), ic_delete));
//        cameraMenuData.add(new CameraMenuData("Settings", ic_camera_settings));
//        cameraMenuData.add(new CameraMenuData(getString(R.string.only_info), ic_tab_info));
        cameraMenuData.add(new CameraMenuData(getString(R.string.digital_wifi_settings), ic_wifi_settings));
        initiateRecyclerView();

        return cameraMenuBinding.getRoot();
    }

     private void initiateRecyclerView() {

         requireActivity().getOnBackPressedDispatcher().addCallback(lifecycleOwner, new OnBackPressedCallback(true) {
             @Override
             public void handleOnBackPressed() {
                 backToHome();
             }
         });
         homeViewModel.isCameraLongPressed = false;
         CameraMenuAdapter cameraMenuAdapter = new CameraMenuAdapter(cameraMenuData,cameraChangePasswordViewModel);
         cameraMenuBinding.cameraMenuRecycler.setAdapter(cameraMenuAdapter);
         cameraMenuBinding.cameraMenuRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

         cameraChangePasswordViewModel.isSelectDelete.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(aBoolean -> {
             Log.e(TAG,"isSelectDelete called " + aBoolean);
             if (aBoolean){
                 mHandler.post(this::dismiss);
                 showDeleteCameraItemDialog(getString(R.string.pop_up_delete_message));
             }
         }));

         cameraChangePasswordViewModel.hideBottomSheet.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
             homeViewModel.setHasShowProgressBar(false);
             if (isAdded()) {
                 dismissAllowingStateLoss();
             }
             if (!aBoolean) {
                 Context context = getContext();
                 if (context != null) {
                     Toast.makeText(context, getString(R.string.not_valid_camera), Toast.LENGTH_SHORT).show();
                 }
             }
         }));

         cameraChangePasswordViewModel.isSelectWifiSetting.observe(lifecycleOwner,new com.sionyx.plexus.utils.EventObserver<>(aBoolean -> {
             Log.e(TAG,"isSelectWifiSetting called " + aBoolean);

             homeViewModel.setHasShowProgressBar(true);

             if (aBoolean){
                 WiFiHistory wiFiHistory = getWiFiHistory();
                 if (wiFiHistory.getIs_wifi_connected() == WIFI_CONNECTED){
                     setUpdateTransferProtocol(Constants.UpdateTransferProtocol.https); // First try with https
                     digitalCameraInfoViewModel.getDigitalCameraInformation(new CameraResponseCallback() {
                         @Override
                         public void onSuccess(String responseStr, int code) {
                             if (code == Constants.ON_SUCCESS) {
                                 // json response
                                 JSONObject obj = null;
                                 try {
                                     obj = new JSONObject(responseStr);

                                     if (obj.getString("type").equals("done") && obj.getJSONObject("reply").getString("errno").equals("0")) {
                                         JSONObject value = obj.getJSONObject("reply").getJSONObject("value");
                                         String fwVersion = value.optString("firmwareVersion");
                                         compareSelectedCameraFirmwareVersion(wiFiHistory.getCamera_ssid(),fwVersion);
                                     }
                                 } catch (Exception e) {
                                     Log.e(TAG,"getDigitalCameraInformation " + e.getMessage());
                                 }
                             } else {
                                 mHandler.post(() -> {
                                     Context context = getContext();
                                     if (context != null) {
                                         apiErrorMessage(context, code);
                                     }
                                     cameraChangePasswordViewModel.hideBottomSheetDialog(true);
                                 });
                             }
                         }

                         @Override
                         public void onFailure(String message) {
                             mHandler.post(() -> {
                                 cameraChangePasswordViewModel.hideBottomSheetDialog(false);
                             });
                         }
                     });
                 } else {
                     mHandler.post(() -> {
                         cameraChangePasswordViewModel.hideBottomSheetDialog(true);
                         homeViewModel.getNavController().navigate(R.id.cameraWifiSettingsFragment);
                     });
                 }
             }
         }));
     }

     private void backToHome() {
         if (homeViewModel.getNavController() != null) {
             cameraChangePasswordViewModel.hideBottomSheetDialog(true);
             homeViewModel.getNavController().navigate(R.id.homeFragment);
         }
     }

     private void compareSelectedCameraFirmwareVersion(String ssid, String fwVersion) {
         digitalCameraInfoViewModel.updateCameraFirmwareVersion(ssid, fwVersion);

         DigitalCameraInfoViewModel.CompareResult result = digitalCameraInfoViewModel.compareWithBase(fwVersion);

         if (result == DigitalCameraInfoViewModel.CompareResult.LESSER || result == DigitalCameraInfoViewModel.CompareResult.EQUAL) {
             // fwVersion <= BASE_VERSION → show update
             mHandler.post(() -> cameraChangePasswordViewModel.hideBottomSheetDialog(true));
             showCameraWifiDialog(getString(R.string.firmware_update_msg));
         } else {
             mHandler.post(() -> {
                 cameraChangePasswordViewModel.hideBottomSheetDialog(true);
                 homeViewModel.getNavController().navigate(R.id.cameraWifiSettingsFragment);
             });
         }
    }
     private void showDeleteCameraItemDialog(String message) {
         MainActivity activity = ((MainActivity) getActivity());
         if (activity != null) {
             activity.showDialog = MainActivity.ShowDialog.POP_UP_DELETE_ITEM_DIALOG;
             activity.showDialog("", message, null);
         }
     }

    private void showCameraWifiDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.NWD_CAMERA_REBOOT_DIALOG;
            activity.showDialog("", message, null);
        }
    }

     @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior<View> behavior = getBottomSheetBehavior();
        if (behavior != null) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private BottomSheetBehavior<View> getBottomSheetBehavior() {
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            return BottomSheetBehavior.from(parent);
        }
        return null;
    }
    MainActivity mainActivity = null;

    @Override
    public void onAttach(@NonNull Context context) {
           super.onAttach(context);
           mainActivity = (MainActivity) context;
    }
 }