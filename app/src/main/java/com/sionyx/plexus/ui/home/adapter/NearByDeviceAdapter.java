package com.sionyx.plexus.ui.home.adapter;

import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.locationLatitude;
import static com.dome.librarynightwave.utils.Constants.locationLongitude;
import static com.sionyx.plexus.ui.MainActivity.hasAlreadyAddedInDialogTag;
import static com.sionyx.plexus.ui.camera.CameraFragment.isChangeCameraNameButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isFactoryRestButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isInfoButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isSettingButtonPressed;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpFwUpdateCheck;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpInfo;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpSettings;
import static com.sionyx.plexus.ui.home.HomeFragment.isUpdatingCameraName;
import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.setWiFiHistory;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
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

public class NearByDeviceAdapter extends RecyclerView.Adapter<NearByDeviceAdapter.RecyclerViewHolder> {
    private ArrayList<WiFiHistory> nearByDeviceModels;
    private final Context mContext;
    private final HomeViewModel homeViewModel;
    private final BleWiFiViewModel bleWiFiViewModel;
    private final int row_index = -1;

    public NearByDeviceAdapter(ArrayList<WiFiHistory> nearByDeviceModels, Context mContext, HomeViewModel homeViewModel, BleWiFiViewModel bleWiFiViewModel) {
        this.nearByDeviceModels = nearByDeviceModels;
        this.mContext = mContext;
        this.homeViewModel = homeViewModel;
        this.bleWiFiViewModel = bleWiFiViewModel;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wifi_history_items_view, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, @SuppressLint("RecyclerView") int position) {
        WiFiHistory nearByDeviceModel = nearByDeviceModels.get(position);
        final String camera_ssid = nearByDeviceModel.getCamera_ssid();
        int is_wifi_connected = nearByDeviceModel.getIs_wifi_connected();

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

        holder.itemView.setOnClickListener(view -> {
            avoidDoubleClicks(holder.itemView);
            if (bleWiFiViewModel.hasAlreadyExistSSId(camera_ssid)) {
                if (camera_ssid.contains(FILTER_STRING6)){
                    setWiFiHistory(nearByDeviceModel);
                    CameraPasswordSettingViewModel._wifiHistory = nearByDeviceModel;
                    homeViewModel.setNwdCameraSelected(true);
                }
                homeViewModel.setCameraSwitched(true);
                homeViewModel.onSelectCamera(camera_ssid);
                firmwareUpdateSequence.clear();
                homeViewModel.getNoticeDialogFragments().clear();
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
                homeViewModel.setPressCancelOrBackPopUpWindow(false);
            } else {
                homeViewModel.selectedDeviceConnections(nearByDeviceModel, true);
                homeViewModel.selectNearByDevice();
                bleWiFiViewModel.setIsAutoConnectEnabled(0);
                homeViewModel.setInsertOrUpdateSsid(camera_ssid);
            }
        });

        /* for this show dot once selected BLE discovery camera connection*/
        if (bleWiFiViewModel.hasAlreadyExistSSId(camera_ssid)) {
            holder.selectedItemDot.setVisibility(View.VISIBLE);
        } else {
            holder.selectedItemDot.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return nearByDeviceModels.size();
    }

    public ArrayList<WiFiHistory> getAdapterItems() {
        return nearByDeviceModels;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setValue(ArrayList<WiFiHistory> availableDevices) {
        nearByDeviceModels = availableDevices;
        notifyDataSetChanged();
    }


    public void setValue(WiFiHistory wiFiHistory) {
        if(!nearByDeviceModels.contains(wiFiHistory)){
            nearByDeviceModels.add(wiFiHistory);
            notifyItemInserted(nearByDeviceModels.size() - 1);
        }
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private final TextView deviceName;
        private final View horizontalDivider;
        private final ImageView selectedItemDot;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            horizontalDivider = itemView.findViewById(R.id.view);
            selectedItemDot = itemView.findViewById(R.id.selected_item);

            int orientation = ((Activity) mContext).getResources().getConfiguration().orientation;
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;

            ConstraintLayout.LayoutParams newLayoutParams = (ConstraintLayout.LayoutParams) horizontalDivider.getLayoutParams();
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                double oneIconHeight = height * 0.0593;
                deviceName.getLayoutParams().height = (int) oneIconHeight;
                newLayoutParams.rightMargin = (int) oneIconHeight;
                horizontalDivider.setLayoutParams(newLayoutParams);
            } else {
                double LandscapeIconHeight = height * 0.1285;
                deviceName.getLayoutParams().height = (int) LandscapeIconHeight;
                newLayoutParams.rightMargin = (int) LandscapeIconHeight;
                horizontalDivider.setLayoutParams(newLayoutParams);
            }
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
