package com.sionyx.plexus.ui.galleryliveview.livetabviewadapter;

import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.sionyx.plexus.ui.home.HomeViewModel.isRecordingStarted;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dome.librarynightwave.utils.Constants;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.dialog.GalleryAllViewModel;
import com.sionyx.plexus.ui.galleryliveview.GalleryManageModel;
import com.sionyx.plexus.ui.galleryliveview.GallerySelectedItemInfo;
import com.sionyx.plexus.ui.home.HomeViewModel;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GalleryLiveViewPageAdapter extends RecyclerView.Adapter<GalleryLiveViewPageAdapter.RecyclerViewHolder> {
    private final Context mContext;
    private ArrayList<GalleryManageModel> galleryManageModels;
    private final GalleryAllViewModel viewModel;
    private boolean isManageLayout;
    private int currentTab;
    private HomeViewModel homeViewModel;

    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    public GalleryLiveViewPageAdapter(boolean isManageClicked, Context context, ArrayList<GalleryManageModel> galleryManageModel, GalleryAllViewModel viewModel, int curTab) {
        this.mContext = context;
        this.galleryManageModels = galleryManageModel;
        this.isManageLayout = isManageClicked;
        this.viewModel = viewModel;
        this.currentTab = curTab;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.gallery_manage_item_view, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        homeViewModel = new ViewModelProvider((ViewModelStoreOwner) mContext).get(HomeViewModel.class);
        GalleryManageModel manageModel = galleryManageModels.get(position);
        if (manageModel != null) {
            String[] originalFileName = manageModel.getFileName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                int i = 1;
                int itemPosition = (holder.getBindingAdapterPosition() + i);
                String fileName = dateString.toUpperCase() + "-" + itemPosition;
                holder.filename.setText(fileName);
            }
            File file = new File(manageModel.getFilePath());

            if (manageModel.getFilePath().endsWith(".mp4")) {
                if (file.exists()) {
//                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//                    if (file.length() > 0) {
//                        retriever.setDataSource(mContext, Uri.parse(manageModel.getFilePath()));
//                    }
//                    Bitmap thumbnail = retriever.getFrameAtTime();
//                    holder.photos.setImageBitmap(thumbnail);
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
                    holder.playIcon.setVisibility(View.GONE);
//                    Bitmap bitmap = BitmapFactory.decodeFile(manageModel.getFilePath());
//                    holder.photos.setImageBitmap(bitmap);
//                    holder.playIcon.setVisibility(View.GONE);
                }
            }
            if (galleryManageModels.get(position).getFilePath().endsWith(".mp4")) {
                holder.playIcon.setVisibility(View.VISIBLE);
            }

            /*Update Recording Image in 0th position while timer running state*/
            if (isRecordingStarted) {
                if (currentTab == 0 || currentTab == 2) {
                    if (holder.getBindingAdapterPosition() == 0) {
//                    Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_recording);
//                    holder.photos.setImageBitmap(bitmap);
                        Glide.with(mContext).load(manageModel.getFilePath())
                                .placeholder(R.drawable.ic_recording)
                                .into(holder.photos);
                        holder.playIcon.setVisibility(View.GONE);
                        holder.transparent_layer.setVisibility(View.GONE);
                        holder.checkbox.setVisibility(View.GONE);
                        holder.itemView.setOnClickListener(null);
                    }
                }
            }

            if (isManageLayout) {
                /*In Recording State no Checkbox state*/
                if (isRecordingStarted && currentTab != 1 && holder.getBindingAdapterPosition() == 0) {
//                    if (currentTab == 0 || currentTab == 2) {
//                        if (holder.getBindingAdapterPosition() == 0) {
                    galleryManageModels.get(position).setChecked(false);
                    holder.transparent_layer.setVisibility(View.GONE);
                    holder.checkbox.setVisibility(View.GONE);
                    holder.checkbox.setOnCheckedChangeListener(null);
                    holder.itemView.setOnClickListener(null);
                    holder.checkbox.setChecked(false);
//                        }
//                        else {
//                            itIsManageLayout(holder, position, true);
//                        }
//                    } else {
//                        itIsManageLayout(holder, position, true);
//
//                    }
                } else {
                    itIsManageLayout(holder, position, true);
                }

            } else {
                holder.transparent_layer.setVisibility(View.GONE);
                holder.checkbox.setVisibility(View.GONE);
                /*In Recording State no item click state*/
                if (isRecordingStarted && currentTab != 1 && holder.getBindingAdapterPosition() == 0) {
//                    if (currentTab == 0 || currentTab == 2) {
//                        if (holder.getAdapterPosition() == 0) {
                    holder.itemView.setOnClickListener(null);
//                        } else {
//                            itIsManageLayout(holder, position, false);
//                        }
//                    } else {
//                        itIsManageLayout(holder, position, false);
//                    }
                } else {
                    itIsManageLayout(holder, position, false);
                }
            }
        }
    }

    /*
    Based on visibility the function will be called for checkbox enable or not and even in REC state
     */
    private void itIsManageLayout(RecyclerViewHolder holder, int position, boolean enableCheckbox) {
        if (enableCheckbox) {

            if (!isRecordingStarted) {
                holder.checkbox.setOnCheckedChangeListener(null); // Reset listener to avoid reusing incorrect state
                holder.checkbox.setChecked(false); // Reset checkbox state
            }

            holder.transparent_layer.setVisibility(View.VISIBLE);
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(galleryManageModels.get(position).isChecked());

            holder.checkbox.setOnCheckedChangeListener((compoundButton, b) -> {
                galleryManageModels.get(holder.getBindingAdapterPosition()).setChecked(b);
                int pos = viewModel.getArrayListAll().indexOf(galleryManageModels.get(holder.getBindingAdapterPosition()));
                viewModel.getArrayListAll().get(pos).setChecked(b);

                ArrayList<GalleryManageModel> tempList = new ArrayList<>();
                for (int i = 0; i < galleryManageModels.size(); i++) {
                    if (galleryManageModels.get(i).isChecked()) {
                        tempList.add(galleryManageModels.get(i));
                    }
                }
                homeViewModel.checkedItemCount = tempList.size();


                if (isRecordingStarted && currentTab != 1) {

                    Log.d("countTAG", "onCheckedChanged: " + galleryManageModels.size() + "  <-->  " + tempList.size());
                    viewModel.setSelectAllSelected(galleryManageModels.size() == (tempList.size() + 1));//21 == 20


                } else {
                    viewModel.setSelectAllSelected(galleryManageModels.size() == tempList.size());
                }


            });

            holder.itemView.setOnClickListener(v -> holder.checkbox.setChecked(!holder.checkbox.isChecked()));

            int selectCount = 0;
            for (GalleryManageModel i : galleryManageModels) {
                if (i.isChecked) {
                    selectCount++;
                }
            }

            if (isRecordingStarted && currentTab != 1) {
                Log.d("countTAG", "onCheckedChanged: >> " + galleryManageModels.size() + "  <-->  " + selectCount);
//                Log.d("countTAG1", selectCount + "-->" + galleryManageModels.size() + "-->" + isRecordingStarted + "-->" + currentTab);
//                if (currentTab != 1) {
                viewModel.setSelectAllSelected(galleryManageModels.size() == (selectCount + 1));
//                } else {
//                    viewModel.setSelectAllSelected(galleryManageModels.size() == selectCount);
//                }
            } else {
                viewModel.setSelectAllSelected(galleryManageModels.size() == selectCount);
            }

        } else {
            holder.transparent_layer.setVisibility(View.GONE);
            holder.checkbox.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                File file = new File(galleryManageModels.get(position).getFilePath());
                if (file.length() != 0) {
                    GallerySelectedItemInfo gallerySelectedItemInfo = new GallerySelectedItemInfo();
                    if (galleryManageModels.get(position).getFilePath().endsWith(".jpg")) {
                        viewModel.isSelectedFileModel = galleryManageModels.get(position);
                    }
                    gallerySelectedItemInfo.setFilePath(galleryManageModels.get(position).getFilePath());
                    gallerySelectedItemInfo.setItemPosition(holder.getAdapterPosition());
                    viewModel.onSelectGalleryItemView(gallerySelectedItemInfo);
                } else {
                    Toast.makeText(mContext, mContext.getString(R.string.corrupted_video), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return galleryManageModels.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replaceWithLatest(ArrayList<GalleryManageModel> arrayListAll, boolean hasShowCheckbox, int curTab) {
        this.galleryManageModels = arrayListAll;
        this.isManageLayout = hasShowCheckbox;
        this.currentTab = curTab;
        notifyDataSetChanged();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView filename;
        public CheckBox checkbox;
        public ImageView photos;
        public ImageView playIcon;
        public ImageView transparent_layer;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            filename = itemView.findViewById(R.id.fileName);
            photos = itemView.findViewById(R.id.photo);
            checkbox = itemView.findViewById(R.id.custom_checkbox);
            playIcon = itemView.findViewById(R.id.play_icon);
            transparent_layer = itemView.findViewById(R.id.gallery_transparent_thumbnail);
            checkbox.setVisibility(View.GONE);
            transparent_layer.setVisibility(View.GONE);
        }
    }

    public void selectAllCheckbox(int curTab, boolean isSelectAll, Handler mHandler) {
        if (!galleryManageModels.isEmpty()) {

            switch (curTab) {
                case 0:
                    for (int i = 0; i < galleryManageModels.size(); i++) {
                        GalleryManageModel manageModel = galleryManageModels.get(i);
                        if (manageModel.getFilePath().endsWith(".jpg") || manageModel.getFilePath().endsWith(".mp4")) {
                            manageModel.setChecked(isSelectAll);
                        }
                        if (viewModel.getArrayListAll().get(i).getFilePath().endsWith(".jpg") || viewModel.getArrayListAll().get(i).getFilePath().endsWith(".mp4")) {
                            viewModel.getArrayListAll().get(i).setChecked(isSelectAll);
                        }
                        Log.d("gallCheck", "selectAllCheckbox: " + galleryManageModels.size());
                    }
                    break;
                case 1:
                    for (int i = 0; i < galleryManageModels.size(); i++) {
                        GalleryManageModel manageModel = galleryManageModels.get(i);
                        if (manageModel.getFilePath().endsWith(".jpg")) {
                            manageModel.setChecked(isSelectAll);
                        }
                       /* if (viewModel.getArrayListAll().get(i).getFilePath().endsWith(".jpg")) {
                            viewModel.getArrayListAll().get(i).setChecked(isSelectAll);
                        }*/

                        Log.d("gallCheck1", "selectAllCheckbox: " + galleryManageModels.size());
                    }
                    for (GalleryManageModel i : viewModel.getArrayListAll()) {
                        if (i.getFilePath().endsWith(".jpg")) {
                            i.setChecked(isSelectAll);
                        }
                    }
                    break;
                case 2:
                    for (int i = 0; i < galleryManageModels.size(); i++) {
                        GalleryManageModel manageModel = galleryManageModels.get(i);
                        if (manageModel.getFilePath().endsWith(".mp4")) {
                            manageModel.setChecked(isSelectAll);
                        }

                        /*if (viewModel.getArrayListAll().get(i).getFilePath().endsWith(".mp4")) {
                            viewModel.getArrayListAll().get(i).setChecked(isSelectAll);
                        }*/
                    }
                    for (GalleryManageModel i : viewModel.getArrayListAll()) {
                        if (i.getFilePath().endsWith(".mp4")) {
                            i.setChecked(isSelectAll);
                        }
                    }
                    break;
            }


            mHandler.postDelayed(() -> notifyItemRangeChanged(0, galleryManageModels.size()), 250);
        }
    }

    /*
    return the boolean value check whether it has been erase or not
     */
    @SuppressLint("NotifyDataSetChanged")
    public void erased() {

        ArrayList<GalleryManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < galleryManageModels.size(); i++) {
            if (galleryManageModels.get(i).isChecked()) {
                try {
                    File file = new File(galleryManageModels.get(i).getFilePath());
                    if (file.exists() && file.isFile() && file.canWrite()) {
                        file.delete();
                        viewModel.isErase = true;
                        tempList.add(galleryManageModels.get(i));
                    }
                } catch (Exception e) {
                    viewModel.isErase = false;
                }

            } else {
                galleryManageModels.get(i).setChecked(false);
//                tempList.add(galleryManageModels.get(i));
            }
        }
//        galleryManageModels = tempList;
        if (viewModel.isErase) {
            viewModel.hasEraseLiveMedia(true);
            viewModel.hasEraseLivePhotoMedia(true);
            viewModel.hasEraseLiveVideoMedia(true);
            viewModel.setBackToGallery(true);
            viewModel.isErase = false;
            notifyDataSetChanged();
            if (tempList.size() > 1) {
                Toast.makeText(mContext, mContext.getString(R.string.multiple_erased), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.erased), Toast.LENGTH_SHORT).show();
            }

        }
    }
}
