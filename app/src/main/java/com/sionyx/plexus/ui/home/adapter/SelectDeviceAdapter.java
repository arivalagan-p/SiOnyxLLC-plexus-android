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
import static com.sionyx.plexus.utils.Constants.WIFI_AVAILABLE;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;
import static com.sionyx.plexus.utils.Constants.WIFI_NOT_CONNECTED;
import static com.sionyx.plexus.utils.Constants.firmwareUpdateSequence;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.cameramenu.fragment.CameraMenuBottomDialog;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.TooltipHelper;

import java.util.ArrayList;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SelectDeviceAdapter extends RecyclerView.Adapter<SelectDeviceAdapter.RecyclerViewHolder> {

    private String TAG = "SelectDeviceAdapter";
    private final ArrayList<WiFiHistory> selectDeviceModelList;
    private HomeViewModel homeViewModel;
    private Context context;
    public static PopupWindow popupWindow;
    private BleWiFiViewModel bleWiFiViewModel;
    private FragmentActivity fragmentActivity;
    private boolean tooltipShown;
    private boolean hasShownTooltip;
    private final DigitalCameraInfoViewModel digitalCameraInfoViewModel;;

    public SelectDeviceAdapter(ArrayList<WiFiHistory> recyclerDataArrayList, FragmentActivity mcontext, HomeViewModel homeViewModel, FragmentActivity fragmentActivity, boolean isTooltipShown) {
        this.selectDeviceModelList = recyclerDataArrayList;
        this.homeViewModel = homeViewModel;
        this.context = mcontext;
        bleWiFiViewModel = new ViewModelProvider(mcontext).get(BleWiFiViewModel.class);
        digitalCameraInfoViewModel = new ViewModelProvider(mcontext).get(DigitalCameraInfoViewModel.class);
        this.fragmentActivity = fragmentActivity;
        homeViewModel.popupPos = -1;
        this.tooltipShown = isTooltipShown;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_device_items_view, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, @SuppressLint("RecyclerView") int itemPosition) {
        WiFiHistory wiFiHistory = selectDeviceModelList.get(itemPosition);
        final String camera_ssid = wiFiHistory.getCamera_ssid();
        String camera_name = wiFiHistory.getCamera_name();
        int wifiState = wiFiHistory.getIs_wifi_connected();

        if (camera_ssid.equalsIgnoreCase("Add Device")) {
            try {
                if (camera_name == null) {
                    camera_name = camera_ssid;
                }
                holder.name.setText(camera_name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            bleWiFiViewModel.getCameraName(camera_ssid).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<String>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(String name) {
                            holder.name.setText(!name.isEmpty() ? name : camera_ssid);
                        }

                        @Override
                        public void onError(Throwable e) {
                            holder.name.setText(camera_ssid);
                        }
                    });
        }
// for this clear If previously added camera is there in cache while install app first time
//        new Handler().post(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (camera_ssid.contains(FILTER_STRING4) || camera_ssid.contains(FILTER_STRING5)) {
//                        if (itemPosition != 0) {
//                            bleWiFiViewModel.deleteSsid(wiFiHistory.getCamera_ssid());
//                            homeViewModel.removeSelectedDeviceConnections(wiFiHistory); //  home screen grid view items to remove
//                            homeViewModel.removeConnectedDeviceHistory(wiFiHistory);
//                        }
//                    } else {
//                        Log.d("camera_ssid", "" + wiFiHistory.getCamera_ssid());
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });

        if (itemPosition == 0) {
            holder.imageView.setContentDescription("-1:WIFI_NOT_CONNECTED:Add Device");
            holder.imageView.setImageResource(R.drawable.ic_add);
        } else {
            if (camera_ssid.contains(FILTER_STRING1)
                    || camera_ssid.contains(FILTER_STRING2)
                    || camera_ssid.contains(FILTER_STRING3)) {
                switch (wifiState) {
                    case WIFI_AVAILABLE:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_AVAILABLE" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_nw_analog_available);
                        break;
                    case WIFI_CONNECTED:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_CONNECTED" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_nw_analog_connected);
                        break;
                    case WIFI_NOT_CONNECTED:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_NOT_CONNECTED" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_nw_analog_disconnected);
                        break;
                }
            }

            if (camera_ssid.contains(FILTER_STRING4) || camera_ssid.contains(FILTER_STRING5)) {
                switch (wifiState) {
                    case WIFI_AVAILABLE:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_AVAILABLE" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_opsin_available);
                        break;
                    case WIFI_CONNECTED:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_CONNECTED" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.opsin_connected);
                        break;
                    case WIFI_NOT_CONNECTED:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_NOT_CONNECTED" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_opsin_disconnect);
                        break;
                }
            }

            if (camera_ssid.contains(FILTER_STRING6)) {
                switch (wifiState) {
                    case WIFI_AVAILABLE:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_AVAILABLE" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_nw_digital_available);
                        break;
                    case WIFI_CONNECTED:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_CONNECTED" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_nw_digital_connected);
                        break;
                    case WIFI_NOT_CONNECTED:
                        holder.imageView.setContentDescription(wifiState + ":" + "WIFI_NOT_CONNECTED" + ":" + camera_ssid);
                        holder.imageView.setImageResource(R.drawable.ic_nw_digital_disconnected);
                        break;
                }
            }

//          the background overlay is sometimes displaying between of the camera so, used delay for avoid that issue
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Only show for first matching item
                    if (itemPosition == 1 && !tooltipShown && !hasShownTooltip) {
                        Log.i(TAG, "onBindViewHolder: toolTipValue : " + tooltipShown +" "+hasShownTooltip+" "+ itemPosition);
                        hasShownTooltip = true;
                        holder.itemView.postDelayed(() -> {
                            TooltipHelper tooltipHelper = new TooltipHelper(context);
                            tooltipHelper.showToolTip(holder.itemView);
                        }, 0);
                    }
                }
            },500);

        }

        holder.itemView.setOnClickListener(view -> {
            avoidDoubleClicks(holder.itemView);
            if (itemPosition != 0) {
                CameraPasswordSettingViewModel._wifiHistory = wiFiHistory;
                setWiFiHistory(wiFiHistory);
                avoidDoubleClicks(holder.itemView);
                firmwareUpdateSequence.clear();
                homeViewModel.getNoticeDialogFragments().clear();
                homeViewModel.setCameraSwitched(true);
                homeViewModel.setHasShowProgressBar(true);
                homeViewModel.onSelectCamera(camera_ssid);
                homeViewModel.hasShowProgressBar(true);
                isInfoButtonPressed = false;
                isSettingButtonPressed = false;
                isChangeCameraNameButtonPressed = false;
                homeViewModel.setNwdCameraSelected(true);
                homeViewModel.isCameraLongPressed = false;
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
            } else {
                homeViewModel.onAddDevice();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (itemPosition != 0) {
                // fot this block to show dialog while connected camera and loading progressbar
                if (/*wifiState != WIFI_CONNECTED && */!homeViewModel.hasShowProgressBar() && !fragmentActivity.isInMultiWindowMode()) {
                    setWiFiHistory(wiFiHistory);
                    // Opsin and Nightwave Analog
                    if (!camera_ssid.contains(FILTER_STRING6)) {
                        homeViewModel.hasSelectPopUpDelete(true);
                        setSelectedItemPosition(itemPosition);

                    } else {
                        // Nightwave Digital Change pwd Implementation
                        Log.d(TAG, "setOnLongClickListener: ");
                        homeViewModel.setNwdCameraSelected(false);
                        homeViewModel.isCameraLongPressed = true;
//                        if (wiFiHistory.getIs_wifi_connected() == WIFI_CONNECTED) {
//                            isSelectedSsidDataIsExist(wiFiHistory.getCamera_ssid());
//                        } else {
                            // check whether the ip address is null or not
                            if (fragmentActivity != null) {
                                CameraMenuBottomDialog bottomSheetDialog = new CameraMenuBottomDialog();
                                bottomSheetDialog.show(fragmentActivity.getSupportFragmentManager(), "CameraMenu");
                            }
//                        }

                    }
                    notifyItemRangeChanged(0, getItemCount());
                }
            }
            return true;
        });
        if (popupWindow != null) {
            popupWindow.setOnDismissListener(() -> {
                homeViewModel.popupPos = -1;
                notifyItemRangeChanged(0, getItemCount());
            });
        }
        if (itemPosition == homeViewModel.popupPos) {
            holder.imageView.setAlpha(1F);
            holder.name.setAlpha(1F);
        } else {
            holder.imageView.setAlpha(0.5F);
            holder.name.setAlpha(0.5F);
        }

        if (itemPosition == 0 || homeViewModel.popupPos == -1) {
            holder.imageView.setAlpha(1F);
            holder.name.setAlpha(1F);
        }
    }

    private void isSelectedSsidDataIsExist(String ssid) {
        Disposable subscribe = digitalCameraInfoViewModel.checkNWDSsidIsExit(ssid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null) {
                                Log.e(TAG, "isSelectedSsidDataIsExist onSuccess " + result.getCamera_ssid());
                                new CameraMenuBottomDialog().show(
                                        fragmentActivity.getSupportFragmentManager(), "CameraMenu"
                                );
                            }
                        },
                        e -> {
                            Log.e(TAG, "isSelectedSsidDataIsExist onError ");
                            homeViewModel.setCameraSwitched(true);
                            homeViewModel.onSelectCamera(wiFiHistory.getCamera_ssid());
                        }
                );
    }
    private void avoidDoubleClicks(final View view) {
        final long DELAY_IN_MS = 900;
        if (!view.isClickable()) {
            return;
        }
        view.setClickable(false);
        view.postDelayed(() -> view.setClickable(true), DELAY_IN_MS);
    }

    @Override
    public int getItemCount() {
        return selectDeviceModelList.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        public ImageView imageView;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            imageView = itemView.findViewById(R.id.camera_icon);
        }
    }

    // Long press popup has removed as per the Ticket ID - 438
    public void showPopUpMenu(View v, WiFiHistory wiFiHistory) {
        popupWindow = popupDisplay(wiFiHistory);
        DisplayMetrics metrics = new DisplayMetrics();
        fragmentActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        try {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
            if (popupWindow != null) {
                popupWindow.showAsDropDown(v, (v.getWidth() - popupWindow.getWidth()) / 2, -(v.getHeight() + popupWindow.getHeight() + 20));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PopupWindow popupDisplay(WiFiHistory wiFiHistory) {
        popupWindow = new PopupWindow(context);
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.pop_up_layout, null);
        ImageView pop_delete = (ImageView) view.findViewById(R.id.pop_delete);
        ImageView pop_settings = (ImageView) view.findViewById(R.id.pop_settings);
        ImageView pop_info = (ImageView) view.findViewById(R.id.pop_info);
        ImageView pop_fw_update = (ImageView) view.findViewById(R.id.pop_fw_update);
        setWiFiHistory(wiFiHistory);

        if (wiFiHistory.getIs_wifi_connected() == WIFI_CONNECTED) {
            pop_delete.setAlpha(0.5f);
            pop_delete.setEnabled(false);
        } else {
            pop_delete.setEnabled(true);
        }

        if (wiFiHistory.getCamera_ssid().contains(FILTER_STRING4) || wiFiHistory.getCamera_ssid().contains(FILTER_STRING5)) {
            pop_fw_update.setVisibility(View.VISIBLE);
        } else {
            pop_fw_update.setVisibility(View.GONE);
        }

        pop_delete.setOnClickListener(v -> {
            homeViewModel.hasSelectPopUpDelete(true);
            popupWindow.dismiss();
        });

        pop_settings.setOnClickListener(v -> {
            if (wiFiHistory.getCamera_ssid().contains(FILTER_STRING6)){
                popupWindow.dismiss();
            } else {
                homeViewModel.hasSelectPopUpSettings(true);
                homeViewModel.setCameraSwitched(true);
                homeViewModel.onSelectCamera(getWiFiHistory().getCamera_ssid());
                popupWindow.dismiss();
                homeViewModel.setPressCancelOrBackPopUpWindow(false);
            }
        });

        pop_info.setOnClickListener(v -> {
            if (wiFiHistory.getCamera_ssid().contains(FILTER_STRING6)){
                popupWindow.dismiss();
            } else {
                homeViewModel.hasSelectPopUpInfo(true);
                homeViewModel.setCameraSwitched(true);
                homeViewModel.onSelectCamera(getWiFiHistory().getCamera_ssid());
                popupWindow.dismiss();
                homeViewModel.setPressCancelOrBackPopUpWindow(false);
            }
        });


        pop_fw_update.setOnClickListener(v -> {
            homeViewModel.hasSelectPopUpFwUpdateCheck(true);
            homeViewModel.setCameraSwitched(true);
            homeViewModel.onSelectCamera(getWiFiHistory().getCamera_ssid());
            popupWindow.dismiss();
            homeViewModel.setPressCancelOrBackPopUpWindow(false);
        });

        popupWindow.setWidth((int) (150 * context.getResources().getDisplayMetrics().density));
        popupWindow.setHeight((int) (35 * context.getResources().getDisplayMetrics().density));
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setContentView(view);
        popupWindow.setBackgroundDrawable(AppCompatResources.getDrawable(context,R.drawable.pop_up_window_background));
        return popupWindow;
    }

    public static int selectedItemPosition = -1;

    public static int getSelectedItemPosition() {
        return selectedItemPosition;
    }

    public static void setSelectedItemPosition(int selectedItemPosition) {
        SelectDeviceAdapter.selectedItemPosition = selectedItemPosition;
    }

    public static WiFiHistory wiFiHistory;

    public static WiFiHistory getWiFiHistory() {
        return wiFiHistory;
    }

    public static void setWiFiHistory(WiFiHistory wiFiHistory) {
        SelectDeviceAdapter.wiFiHistory = wiFiHistory;
    }
}
