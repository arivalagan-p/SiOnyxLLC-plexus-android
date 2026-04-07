package com.sionyx.plexus.ui.profile;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.api.responseModel.ProfileDevices;

import java.util.ArrayList;

public class QRCodeScanAdapter extends RecyclerView.Adapter<QRCodeScanAdapter.RecyclerViewHolder> {
    private final Context mContext;
    private final ArrayList<ProfileDevices> profileDevicesArrayList;
    private final ProfileViewModel profileViewModel;

    public QRCodeScanAdapter(Context context, ArrayList<ProfileDevices> devicesArrayList, ProfileViewModel viewModel) {
        this.mContext = context;
        this.profileDevicesArrayList = devicesArrayList;
        this.profileViewModel = viewModel;
    }

    @NonNull
    @Override
    public QRCodeScanAdapter.RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.qr_code_item_layout, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QRCodeScanAdapter.RecyclerViewHolder holder, int position) {
        ProfileDevices profileDevices = profileDevicesArrayList.get(position);
        if (profileDevices.camera.equals("Opsin")) {
            holder.qrCameraIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.opsin_connected));
        } else {
            holder.qrCameraIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_nw_analog_connected));
            // Apply corner radius to the ImageView
            RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(holder.qrCameraIcon.getResources(), ((BitmapDrawable) holder.qrCameraIcon.getDrawable()).getBitmap());
            roundedDrawable.setCornerRadius(200f);
            holder.qrCameraIcon.setImageDrawable(roundedDrawable);
        }

        holder.descriptionModelName.setText(profileDevices.getModel());
        holder.serialNumber.setText(profileDevices.getSerialNumber());

        holder.deleteProductIcon.setOnClickListener(v -> {
            profileViewModel.setDeleteProductModel(null); // for this previously hold any value to reset while select delete icon
            DeleteProductModel deleteProductModel = new DeleteProductModel();
            deleteProductModel.setProfileDevices(profileDevices);
            deleteProductModel.setItemPosition(position);
            profileViewModel.deleteProductItem(deleteProductModel);
        });
    }

    public void updateItemList(ProfileDevices profileDevices) {
        profileDevicesArrayList.add(profileDevices);
    }

    @Override
    public int getItemCount() {
        return profileDevicesArrayList.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionModelName, serialNumber;
        AppCompatImageView qrCameraIcon, deleteProductIcon;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            serialNumber = itemView.findViewById(R.id.serial_number);
            descriptionModelName = itemView.findViewById(R.id.description_model_name);
            qrCameraIcon = itemView.findViewById(R.id.qr_camera_icon);
            deleteProductIcon = itemView.findViewById(R.id.delete_profile);
        }
    }
}



