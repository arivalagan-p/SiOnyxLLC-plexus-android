package com.sionyx.plexus.ui.home.gallerybottomview.bottomtabviewadapter;

import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomManageModel;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomSelectedItemInfo;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GalleryBottomViewPageAdapter extends RecyclerView.Adapter<GalleryBottomViewPageAdapter.RecyclerViewHolder> {
    private final Context mContext;
    private ArrayList<GalleryBottomManageModel> galleryBottomManageModel;
    private HomeViewModel homeViewModel;
    private static final String TAG = "BottomAllAdapter";
    private Boolean isCheckBoxShown;
    private int itemPosition;
    @SuppressLint("SimpleDateFormat")
    private final DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    public GalleryBottomViewPageAdapter(boolean isManageLayout, Context mContext, ArrayList<GalleryBottomManageModel> galleryBottomManageModel, HomeViewModel homeViewModel) {
        this.mContext = mContext;
        this.galleryBottomManageModel = galleryBottomManageModel;
        this.homeViewModel = homeViewModel;
        this.isCheckBoxShown = isManageLayout;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatedArrayList(boolean isManageLayout, ArrayList<GalleryBottomManageModel> galleryBottomManageModel) {
        this.isCheckBoxShown = isManageLayout;
        this.galleryBottomManageModel = galleryBottomManageModel;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.gallery_bottom_manage_item_view, parent, false);
        return new RecyclerViewHolder(view);
    }


    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        GalleryBottomManageModel manageModel = galleryBottomManageModel.get(position);
        homeViewModel = new ViewModelProvider((ViewModelStoreOwner) mContext).get(HomeViewModel.class);
        if (manageModel != null) {
            String[] originalFileName = manageModel.getFileName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                int i = 1;
                itemPosition = (holder.getBindingAdapterPosition() + i);
                String fileName = dateString.toUpperCase() + "-" + itemPosition;
                holder.filename.setText(fileName);
            }
            File file = new File(manageModel.getFilePath());
            if (manageModel.getFilePath().endsWith(".mp4")) {
                if (file.exists()) {
                    switch (currentCameraSsid){
                        case NIGHTWAVE:
                            Glide.with(mContext).load(manageModel.getFilePath())
                                    .placeholder( R.drawable.ic_camera_background )
                                    .into(holder.photos);
                            break;
                        case OPSIN:
                            Glide.with(mContext).load(manageModel.getFilePath())
                                    .placeholder( R.drawable.opsin_live_view_background )
                                    .into(holder.photos);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Glide.with(mContext).load(manageModel.getFilePath())
                                    .placeholder( R.drawable.ic_nw_digital_background )
                                    .into(holder.photos);
                            break;
                    }
                }
            } else if (manageModel.getFilePath().endsWith(".jpg")) {
                if (file.exists()) {
                    switch (currentCameraSsid){
                        case NIGHTWAVE:
                            Glide.with(mContext).load(manageModel.getFilePath())
                                    .placeholder( R.drawable.ic_camera_background )
                                    .into(holder.photos);
                            break;
                        case OPSIN:
                            Glide.with(mContext).load(manageModel.getFilePath())
                                    .placeholder( R.drawable.opsin_live_view_background )
                                    .into(holder.photos);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Glide.with(mContext).load(manageModel.getFilePath())
                                    .placeholder( R.drawable.ic_nw_digital_background )
                                    .into(holder.photos);
                            break;
                    }
                }
            }
            if (galleryBottomManageModel.get(position).getFilePath().endsWith(".mp4")) {
                holder.playIcon.setVisibility(View.VISIBLE);
            }
            if (galleryBottomManageModel.get(position).getFilePath().endsWith(".jpg")) {
                holder.playIcon.setVisibility(View.GONE);
            }
            //Manage
            if (isCheckBoxShown) {

                holder.checkbox.setOnCheckedChangeListener(null); // Reset listener to avoid reusing incorrect state.
                holder.checkbox.setClickable(false); // Reset checkbox state

                holder.checkbox_layout.setVisibility(View.VISIBLE);
                holder.checkbox.setVisibility(View.VISIBLE);
                holder.checkbox.setChecked(galleryBottomManageModel.get(position).isChecked());
                holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    galleryBottomManageModel.get(holder.getBindingAdapterPosition()).setChecked(isChecked);
                    int pos = homeViewModel.getArrayListAll().indexOf(galleryBottomManageModel.get(holder.getBindingAdapterPosition()));
                    homeViewModel.getArrayListAll().get(pos).setChecked(isChecked);

                    //for handling the select all Deselect all string status and function
                    ArrayList<GalleryBottomManageModel> tempList = new ArrayList<>();
                    for (int i = 0; i < galleryBottomManageModel.size(); i++) {
                        if (galleryBottomManageModel.get(i).isChecked()) {
                            tempList.add(galleryBottomManageModel.get(i));
                        }
                    }
                    homeViewModel.checkedItemCount = tempList.size();
                    homeViewModel.setGallerySelectAll(galleryBottomManageModel.size() == tempList.size());
                });
                int selectCount = 0;
                for (GalleryBottomManageModel manage : galleryBottomManageModel) {
                    if (manage.isChecked) {
                        selectCount++;
                    }
                }
                if (galleryBottomManageModel.size() == selectCount) {
                    homeViewModel.setGallerySelected(true);
                }
                //homeViewModel.setGallerySelectAll(galleryBottomManageModel.size() == selectCount);
                holder.itemView.setOnClickListener(v -> holder.checkbox.setChecked(!holder.checkbox.isChecked()));
            } else {
                holder.checkbox_layout.setVisibility(View.GONE);
                holder.checkbox.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> {
                    if (file.length() != 0) {
                        GalleryBottomSelectedItemInfo galleryBottomSelectedItemInfo = new GalleryBottomSelectedItemInfo();
                        int i = 1;
                        int itemPosition_ = (holder.getBindingAdapterPosition() + i);
                        if (manageModel.getFilePath().endsWith("jpg")) {
                            homeViewModel.isSelectedFilePath = manageModel;
                        }
                        galleryBottomSelectedItemInfo.setFilePath(manageModel.getFilePath());
                        galleryBottomSelectedItemInfo.setItemPosition(holder.getAdapterPosition());
                        homeViewModel.onSelectGalleryBottomItemView(galleryBottomSelectedItemInfo);
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.corrupted_video), Toast.LENGTH_SHORT).show();
                    }

                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return galleryBottomManageModel.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView filename;
        public CheckBox checkbox;
        public ImageView photos;
        public ImageView playIcon;
        public ImageView transparent_layer;
        public ConstraintLayout checkbox_layout;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            filename = itemView.findViewById(R.id.bottom_view_gallery_title);
            photos = itemView.findViewById(R.id.bottom_view_gallery_thumbnail);
            checkbox = itemView.findViewById(R.id.custom_checkbox);
            playIcon = itemView.findViewById(R.id.bottom_view_gallery_play_icon);
            transparent_layer = itemView.findViewById(R.id.bottom_view_gallery_transparent_thumbnail);
            checkbox_layout = itemView.findViewById(R.id.checkbox_layer);
            checkbox.setVisibility(View.GONE);
        }
    }

    public void selectAllCheckbox(int curtab, boolean isSelectAll, Handler mHandler) {
        if (!galleryBottomManageModel.isEmpty() && !homeViewModel.getArrayListAll().isEmpty()) {
            switch (curtab) {
                case 0:
                    for (int i = 0; i < galleryBottomManageModel.size(); i++) {
                        GalleryBottomManageModel manageModel = galleryBottomManageModel.get(i);
                        if (manageModel.getFilePath().endsWith(".jpg") || manageModel.getFilePath().endsWith(".mp4")) {
                            manageModel.setChecked(isSelectAll);
                        }
                        if (homeViewModel.getArrayListAll().size() > i) {
                            if (homeViewModel.getArrayListAll().get(i).getFilePath().endsWith(".jpg") || homeViewModel.getArrayListAll().get(i).getFilePath().endsWith(".mp4")) {
                                homeViewModel.getArrayListAll().get(i).setChecked(isSelectAll);
                            }
                        }
                    }
                    break;
                case 1:
                    for (int i = 0; i < galleryBottomManageModel.size(); i++) {
                        GalleryBottomManageModel manageModel = galleryBottomManageModel.get(i);
                        if (manageModel.getFilePath().endsWith(".jpg")) {
                            manageModel.setChecked(isSelectAll);
                        }
                        if (homeViewModel.getArrayListAll().size() > i) {
                            if (homeViewModel.getArrayListAll().get(i).getFilePath().endsWith(".jpg")) {
                                homeViewModel.getArrayListAll().get(i).setChecked(isSelectAll);
                            }
                        }
                    }
                    for (GalleryBottomManageModel i : homeViewModel.getArrayListAll()) {
                        if (i.getFilePath().endsWith(".jpg")) {
                            i.setChecked(isSelectAll);
                        }
                    }
                    break;
                case 2:
                    for (int i = 0; i < galleryBottomManageModel.size(); i++) {
                        GalleryBottomManageModel manageModel = galleryBottomManageModel.get(i);
                        if (manageModel.getFilePath().endsWith(".mp4")) {
                            manageModel.setChecked(isSelectAll);
                        }
                        if (homeViewModel.getArrayListAll().size() > i) {
                            if (homeViewModel.getArrayListAll().get(i).getFilePath().endsWith(".mp4")) {
                                homeViewModel.getArrayListAll().get(i).setChecked(isSelectAll);
                            }
                        }
                    }
                    for (GalleryBottomManageModel i : homeViewModel.getArrayListAll()) {
                        if (i.getFilePath().endsWith(".mp4")) {
                            i.setChecked(isSelectAll);
                        }
                    }
                    break;
            }

        }
        mHandler.postDelayed(() -> notifyItemRangeChanged(0, galleryBottomManageModel.size()), 250);
    }

    //return the size of array after erased the selected item from the filepath
    @SuppressLint("NotifyDataSetChanged")
    public int erased() {
        boolean isErased = false;
        ArrayList<GalleryBottomManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < galleryBottomManageModel.size(); i++) {
            if (galleryBottomManageModel.get(i).isChecked()) {
                File file = new File(galleryBottomManageModel.get(i).getFilePath());
                if (file.exists() && file.isFile() && file.canWrite()) {
                    file.delete();
                    isErased = true;
                    tempList.add(galleryBottomManageModel.get(i));
                }
            } else {
                galleryBottomManageModel.get(i).setChecked(false);
            }
        }
        if (isErased) {
            homeViewModel.setBackToGallery(true);
            notifyDataSetChanged();
            if (tempList.size() > 1) {
                Toast.makeText(mContext, mContext.getString(R.string.multiple_erased), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.erased), Toast.LENGTH_SHORT).show();
            }
        }
        return galleryBottomManageModel.size();
    }
}
