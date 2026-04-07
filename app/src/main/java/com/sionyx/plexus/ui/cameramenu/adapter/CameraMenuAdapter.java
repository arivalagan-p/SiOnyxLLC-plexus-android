package com.sionyx.plexus.ui.cameramenu.adapter;

import static com.sionyx.plexus.ui.home.adapter.SelectDeviceAdapter.getWiFiHistory;
import static com.sionyx.plexus.utils.Constants.WIFI_CONNECTED;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.cameramenu.model.CameraMenuData;
import com.sionyx.plexus.ui.cameramenu.model.CameraPasswordSettingViewModel;

import java.util.List;

public class CameraMenuAdapter extends RecyclerView.Adapter<CameraMenuAdapter.ViewHolder> {

    private static String TAG = "CameraMenuAdapter";

    private final CameraPasswordSettingViewModel changePasswordViewModel;


    private final List<CameraMenuData> cameraMenuDataList;

    public CameraMenuAdapter(List<CameraMenuData> options, CameraPasswordSettingViewModel viewModel) {
        this.cameraMenuDataList = options;
        this.changePasswordViewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.camera_menu_item, parent, false);
        Log.d(TAG,"onCreateViewHolder: "+ view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        CameraMenuData menu = cameraMenuDataList.get(position);
        Log.d(TAG, "onBindViewHolder: " + menu);
        holder.title.setText(menu.getTitle());
        int iconResId = menu.getIcon();
        if (iconResId != 0) {
            holder.icon.setImageResource(iconResId);
        } else {
            holder.icon.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (position == 0){
                  changePasswordViewModel.onSelectDelete();
              } else if (position == 1){
                  changePasswordViewModel.onSelectWifiSetting();
              }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cameraMenuDataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;
        View viewLine;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.camera_menu_txt);
            icon = itemView.findViewById(R.id.camera_menu_icon);
            viewLine = itemView.findViewById(R.id.view_line);
        }
    }
}
