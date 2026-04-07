package com.sionyx.plexus.ui.home.adapter;

import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.locationLatitude;
import static com.dome.librarynightwave.utils.Constants.locationLongitude;
import static com.sionyx.plexus.ui.MainActivity.hasAlreadyAddedInDialogTag;
import static com.sionyx.plexus.ui.camera.CameraFragment.isChangeCameraNameButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isFactoryRestButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isInfoButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isSettingButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraViewModel.isSelectArrow;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpFwUpdateCheck;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpInfo;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpSettings;
import static com.sionyx.plexus.ui.home.HomeFragment.isUpdatingCameraName;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.setWiFiHistory;
import static com.sionyx.plexus.utils.Constants.WIFI_AVAILABLE;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;
import static com.sionyx.plexus.utils.Constants.WIFI_NOT_CONNECTED;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SelectedDeviceHistoryAdpter extends RecyclerView.Adapter<SelectedDeviceHistoryAdpter.RecyclerViewHolder> {
    private final ArrayList<WiFiHistory> selectDeviceModelArrayList;
    private final Context mContext;
    private final HomeViewModel homeViewModel;
    private final BleWiFiViewModel bleWiFiViewModel;

    public SelectedDeviceHistoryAdpter(ArrayList<WiFiHistory> selectDeviceModels, Context context, HomeViewModel homeViewModel, BleWiFiViewModel bleWiFiViewModel) {
        this.selectDeviceModelArrayList = selectDeviceModels;
        this.mContext = context;
        this.homeViewModel = homeViewModel;
        this.bleWiFiViewModel = bleWiFiViewModel;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_items_view, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        WiFiHistory deviceModel = selectDeviceModelArrayList.get(position);
        String camera_ssid = deviceModel.getCamera_ssid();
        final int wifiState = deviceModel.getIs_wifi_connected();
        String camera_name = deviceModel.getCamera_name();

        bleWiFiViewModel.getCameraName(camera_ssid).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(String name) {
                        holder.deviceName.setText(!name.isEmpty() ? name : camera_ssid);
                    }

                    @Override
                    public void onError(Throwable e) {
                        holder.deviceName.setText(camera_ssid);
                    }
                });

        if (camera_ssid.contains(FILTER_STRING1)
                || camera_ssid.contains(FILTER_STRING2)
                || camera_ssid.contains(FILTER_STRING3)) {
            switch (wifiState) {
                case WIFI_AVAILABLE:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_AVAILABLE"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_nw_analog_available);
                    break;
                case WIFI_CONNECTED:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_CONNECTED"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_nw_analog_connected);
                    break;
                case WIFI_NOT_CONNECTED:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_NOT_CONNECTED"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_nw_analog_disconnected);
                    break;
            }
        }


        if (camera_ssid.contains(FILTER_STRING4) || camera_ssid.contains(FILTER_STRING5)) {
            switch (wifiState) {
                case WIFI_AVAILABLE:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_AVAILABLE"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_opsin_available);
                    break;
                case WIFI_CONNECTED:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_CONNECTED"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.opsin_connected);
                    break;
                case WIFI_NOT_CONNECTED:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_NOT_CONNECTED"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_opsin_disconnect);
                    break;
            }
        }

        if (camera_ssid.contains(FILTER_STRING6)) {
            switch (wifiState) {
                case WIFI_AVAILABLE:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_AVAILABLE"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_nw_digital_available);
                    break;
                case WIFI_CONNECTED:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_CONNECTED"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_nw_digital_connected);
                    break;
                case WIFI_NOT_CONNECTED:
                    holder.imageView.setContentDescription(wifiState + ":"+"WIFI_NOT_CONNECTED"+":"+camera_ssid);
                    holder.imageView.setImageResource(R.drawable.ic_nw_digital_disconnected);
                    break;
            }
        }

        holder.itemView.setOnClickListener(view -> {
            // Constants.hasNightWaveCamera = false;
            avoidDoubleClicks(holder.itemView);
            firmwareUpdateSequence.clear();
            homeViewModel.getNoticeDialogFragments().clear();
            if (camera_ssid.contains(FILTER_STRING6)){
                setWiFiHistory(deviceModel);
                CameraPasswordSettingViewModel._wifiHistory = deviceModel;
                homeViewModel.setNwdCameraSelected(true);
            }
            homeViewModel.setHasShowProgressBar(true);
            homeViewModel.setCameraSwitched(true);
            homeViewModel.onSelectCamera(camera_ssid);
            isInfoButtonPressed = false;
            isSettingButtonPressed = false;
            isChangeCameraNameButtonPressed = false;
            isFactoryRestButtonPressed = false;
            isUpdatingCameraName = false;
            hasAlreadyAddedInDialogTag = false; // fot this while select camera and rotate before show dialog value not update so,we use this
            locationLatitude = 0.0f;
            locationLongitude = 0.0f;

            /* for this if select pop window but not connect camera and try again to connect camera connect but not go to live view here show last selected pop up state , so avoid use this variable  */
            isSelectPopUpInfo = false;
            isSelectPopUpSettings = false;
            isSelectPopUpFwUpdateCheck = false;
            isSelectArrow = false;
            homeViewModel.setPressCancelOrBackPopUpWindow(false);
        });
    }

    @Override
    public int getItemCount() {
        return selectDeviceModelArrayList.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName;
        public ImageView imageView;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.name);
            imageView = itemView.findViewById(R.id.camera_icon);
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
}
