package com.sionyx.plexus.ui.dialog;

import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.getFormattedAperture;
import static com.dome.librarynightwave.utils.Constants.getFormattedFocalLength;
import static com.sionyx.plexus.ui.camera.CameraViewModel.hasShowRecordViewMenuLayout;
import static com.sionyx.plexus.ui.camera.CameraViewModel.onSelectGallery;
import static com.sionyx.plexus.ui.dialog.GalleryAllViewModel.itemClick;
import static com.sionyx.plexus.ui.home.HomeViewModel.isRecordingStarted;

import android.annotation.SuppressLint;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentGalleryTabLayoutDialogBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.galleryliveview.GallerySelectedItemInfo;
import com.sionyx.plexus.ui.galleryliveview.GalleryViewModel;
import com.sionyx.plexus.ui.galleryliveview.livetabviewadapter.GalleryLiveTabPageAdapter;
import com.sionyx.plexus.ui.home.HomeViewModel;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryTabLayoutDialog extends DialogFragment {
    private FragmentGalleryTabLayoutDialogBinding binding;
    private GalleryAllViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private GalleryLiveTabPageAdapter manageViewAdapter;
    private String videoFilePath = "";
    private int selectedItemPosition;
    private int currentTabposition;
    private final Handler mHandler = new Handler();
    private CameraViewModel cameraViewModel;
    private GalleryViewModel galleryViewModel;
    private MediaPlayer mediaPlayer;
    private BleWiFiViewModel bleWiFiViewModel;
    private final String TAG = "GalleryTabLayoutDialog";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryTabLayoutDialogBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryAllViewModel.class);
        binding.setViewModel(viewModel);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        galleryViewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);

        binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
        binding.galleryAllViewBackButton.setVisibility(View.VISIBLE);
        viewModel.getArrayListAll().clear();
        viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());

        binding.recordingInfoViewTwitter.setVisibility(View.GONE);
        binding.recordingInfoViewFacebook.setVisibility(View.GONE);
        binding.recordingInfoViewInstagram.setVisibility(View.GONE);
        binding.recordingInfoViewYoutube.setVisibility(View.GONE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.noRecordsFoundAll.observe(lifecycleOwner, isNoRecordAll);
        viewModel.noRecordsFoundImage.observe(lifecycleOwner, isNoRecordImage);
        viewModel.noRecordsFoundVideo.observe(lifecycleOwner, isNoRecordVideo);

        subscribeUI();
    }

    private final Observer<Boolean> isNoRecordAll = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            int selectedTabPosition = binding.galleryAllViewTablayout.getSelectedTabPosition();
            Log.e("TAG", "onChanged: " + selectedTabPosition + " " + aBoolean);
            if (selectedTabPosition == 0) {
                if (aBoolean) {
                    if (binding.liveGalleryManageOptionButtons.getVisibility() == View.VISIBLE) {
                        viewModel.onManageViewBack();
                    }
                    binding.galleryAllViewManageButton.setVisibility(View.GONE);
                } else {
                    if (binding.liveGalleryManageOptionButtons.getVisibility() == View.VISIBLE) {
                        binding.galleryAllViewManageButton.setVisibility(View.GONE);
                    } else {
                        binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    };
    private final Observer<Boolean> isNoRecordImage = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            int selectedTabPosition = binding.galleryAllViewTablayout.getSelectedTabPosition();
            Log.e("TAG", "onChanged: " + selectedTabPosition + " " + aBoolean);
            if (selectedTabPosition == 1) {
                if (aBoolean) {
                    if (binding.liveGalleryManageOptionButtons.getVisibility() == View.VISIBLE) {
                        viewModel.onManageViewBack();
                    }
                    binding.galleryAllViewManageButton.setVisibility(View.GONE);
                } else {
                    if (binding.liveGalleryManageOptionButtons.getVisibility() == View.VISIBLE) {
                        binding.galleryAllViewManageButton.setVisibility(View.GONE);
                    } else {
                        binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    };
    private final Observer<Boolean> isNoRecordVideo = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            int selectedTabPosition = binding.galleryAllViewTablayout.getSelectedTabPosition();
            Log.e("TAG", "onChanged: VIDEO " + selectedTabPosition + " " + aBoolean);
            if (selectedTabPosition == 2) {
                if (aBoolean) {
                    if (binding.liveGalleryManageOptionButtons.getVisibility() == View.VISIBLE) {
                        viewModel.onManageViewBack();
                    }
                    binding.galleryAllViewManageButton.setVisibility(View.GONE);
                } else {
                    if (binding.liveGalleryManageOptionButtons.getVisibility() == View.VISIBLE) {
                        binding.galleryAllViewManageButton.setVisibility(View.GONE);
                    } else {
                        binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.noRecordsFoundAll.removeObserver(isNoRecordAll);
        viewModel.noRecordsFoundAll.removeObservers(lifecycleOwner);

        viewModel.noRecordsFoundImage.removeObserver(isNoRecordAll);
        viewModel.noRecordsFoundImage.removeObservers(lifecycleOwner);

        viewModel.noRecordsFoundVideo.removeObserver(isNoRecordAll);
        viewModel.noRecordsFoundVideo.removeObservers(lifecycleOwner);
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent);
        getDialog().getWindow().setAttributes(params);
        /*
        videoview should be paused once user get off from the screen and back to that same screen
         */
        if (binding.videoPlayerViewVideoView.getVisibility() == View.VISIBLE && !binding.videoPlayerViewVideoView.isPlaying() && viewModel.isVideoPlay()) {
            isPaused();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        onSelectGallery = false;
//        itemClick = false;
    }

    @SuppressLint("SetTextI18n")
    private void subscribeUI() {
        setCancelable(false);
        binding.galleryAllViewLayout.setVisibility(View.VISIBLE);
        viewModel.setLiveManageSelected(false);
        binding.liveGalleryManageOptionButtons.setVisibility(View.GONE);
        binding.recordingInfoLayout.setVisibility(View.GONE);
        binding.videoPlayerLayout.setVisibility(View.GONE);

        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy ");
        @SuppressLint("SimpleDateFormat") DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss z");

        /* gallery open view*/
        binding.galleryAllViewTablayout.addTab(binding.galleryAllViewTablayout.newTab().setText(getString(R.string.all)));
        binding.galleryAllViewTablayout.addTab(binding.galleryAllViewTablayout.newTab().setText(getString(R.string.photos)));
        binding.galleryAllViewTablayout.addTab(binding.galleryAllViewTablayout.newTab().setText(getString(R.string.videos)));
        binding.galleryAllViewTablayout.setTabGravity(TabLayout.GRAVITY_FILL);
        setGalleryAdapter(0, false);

        viewModel.isSelectManage.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            currentTabposition = binding.galleryAllViewViewpager.getCurrentItem();
            Log.d("ManageTAG", isRecordingStarted + "-->" + currentTabposition + "-->" + viewModel.getArrayListAll().size() + "-->" + viewModel.getArrayListVideo().size());
            if (isRecordingStarted) {
                if ((viewModel.getArrayListVideo().size() == 1 && currentTabposition == 2) || (viewModel.getArrayListAll().size() == 1 && currentTabposition == 0)) {
                    aBoolean = false;
                }
            }
            if (aBoolean) {
                viewModel.setLiveManageSelected(true);
                binding.liveGalleryManageOptionButtons.setVisibility(View.VISIBLE);
                binding.galleryAllViewManageButton.setVisibility(View.GONE);
                binding.galleryAllViewBackButton.setVisibility(View.GONE);
                resetCheckboxState();
                binding.galleryAllViewLayout.setVisibility(View.VISIBLE);
                binding.recordingInfoLayout.setVisibility(View.GONE);
                binding.videoPlayerLayout.setVisibility(View.GONE);
                viewModel.updateGalleryItemView(true);
                currentTabposition = binding.galleryAllViewViewpager.getCurrentItem();
                setGalleryAdapter(currentTabposition, true);
                viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());

                viewModel.isErasedAll = false;
                viewModel.isErasedPhoto = false;
                viewModel.isErasedVideo = false;
            }
        }));

        viewModel.isCancelGalleryAllView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                backToLiveView();
                viewModel.setLiveManageSelected(false);
                cameraViewModel.setIsShowAlertDialog(false);
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                onSelectGallery = false;
                galleryViewModel.setClosePopupAllMedia(true);
                galleryViewModel.setClosePopupImage(true);
                galleryViewModel.setClosePopupVideo(true);
                manageViewAdapter = null;
                binding.galleryAllViewViewpager.setAdapter(null);
            }
        }));

        viewModel.isBackGalleryAllView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                backToLiveView();
                Log.d("backTAG", "isBackGalleryAllView: ");
                hasShowRecordViewMenuLayout(true);
                onSelectGallery = false;

                galleryViewModel.setClosePopupAllMedia(true);
                galleryViewModel.setClosePopupImage(true);
                galleryViewModel.setClosePopupVideo(true);
                manageViewAdapter = null;
                binding.galleryAllViewViewpager.setAdapter(null);
            }
        }));

        /*gallery manage view*/
        viewModel.isManageSelectBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                viewModel.setLiveManageSelected(false);
                /*
                For clear checkbox state once click back
                 */
                if (viewModel.getArrayListAll() != null) {
                    viewModel.getArrayListAll().clear();
                }
                viewModel.getAllFiles(getActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                binding.galleryAllViewLayout.setVisibility(View.VISIBLE);
                binding.liveGalleryManageOptionButtons.setVisibility(View.GONE);
                binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
                binding.galleryAllViewBackButton.setVisibility(View.VISIBLE);
                binding.videoPlayerLayout.setVisibility(View.GONE);
                binding.recordingInfoLayout.setVisibility(View.GONE);
                viewModel.updateGalleryItemView(true);

                setGalleryAdapter(binding.galleryAllViewViewpager.getCurrentItem(), false);


            }
        }));

        viewModel.isManageSelectCancel.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                backToLiveView();
                cameraViewModel.setIsShowAlertDialog(false);
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                onSelectGallery = false;
                viewModel.setLiveManageSelected(false);
            }
        }));
        /* gallery recording info view */
        viewModel.isSelectGalleryItemView.observe(lifecycleOwner, new EventObserver<>(gallerySelectedItemInfo -> {
            if (gallerySelectedItemInfo != null) {
                binding.galleryAllViewLayout.setVisibility(View.GONE);
                binding.recordingInfoLayout.setVisibility(View.VISIBLE);
                binding.videoPlayerLayout.setVisibility(View.GONE);
                videoFilePath = gallerySelectedItemInfo.filePath;
                selectedItemPosition = gallerySelectedItemInfo.getItemPosition();
                if (gallerySelectedItemInfo.filePath.endsWith(".mp4")) {
                    itemClick = true;
                    showRecordingVideoInfolayout(gallerySelectedItemInfo);
                } else {
                    itemClick = true;
                    showCapturedImageInfo(gallerySelectedItemInfo);
                }
            }
        }));

        viewModel._isSelectRecordingInfoBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                resetCheckboxState();
                binding.galleryAllViewLayout.setVisibility(View.VISIBLE);
                binding.liveGalleryManageOptionButtons.setVisibility(View.GONE);
                binding.recordingInfoLayout.setVisibility(View.GONE);
                binding.videoPlayerLayout.setVisibility(View.GONE);
                viewModel.setLiveManageSelected(false);
                if (binding.recordingInfoViewDeleteButton.getText().equals("Delete Image")) {
                    itemClick = false;
                } else if (binding.recordingInfoViewDeleteButton.getText().equals("Delete Recording")) {
                    itemClick = false;
                }

                // binding.videoPlayerViewVideoView.setVisibility(View.GONE);
            }
        }));

        viewModel._isSelectRecordingInfoCancel.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                backToLiveView();
                viewModel.setLiveManageSelected(false);
                cameraViewModel.setIsShowAlertDialog(false);
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                onSelectGallery = false;
                galleryViewModel.setClosePopupAllMedia(true);
                galleryViewModel.setClosePopupImage(true);
                galleryViewModel.setClosePopupVideo(true);
                manageViewAdapter = null;
                binding.galleryAllViewViewpager.setAdapter(null);
            }
        }));

        viewModel._isSelectRecordingInfoDelete.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                // if ( binding.recordingInfoViewDeleteButton.getText().equals("Delete Image")) {
                if (videoFilePath != null && !videoFilePath.isEmpty()) {
                    showConfirmEraseGalleryItemDialog(getString(R.string.confirm_erase));
                }
            }
        }));

        viewModel.isDeleteRecordedVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                /*here saved file to be delete*/
                Log.i("TabLayout", "subscribeUI:FILEPATH > \" + videoFilePath ");
                if (!videoFilePath.isEmpty()) {
                    File file = new File(videoFilePath);
                    if (file.exists() && file.isFile() && file.canWrite()) {
                        file.delete();
//                        if (videoFilePath.endsWith(".mp4")) {
                        Toast.makeText(requireActivity(), getString(R.string.erased), Toast.LENGTH_SHORT).show();
//                        }
//                        if (videoFilePath.endsWith(".jpg")) {
//                            Toast.makeText(requireActivity(), R.string.delete_image, Toast.LENGTH_SHORT).show();
//                        }
                        viewModel.setIsMedialFileDeleted(true);
                    }

                }
                if (viewModel.getArrayListAll() != null) {
                    viewModel.getArrayListAll().clear();
                }
//                if (viewModel.getArrayListPhoto() != null) {
//                    viewModel.getArrayListPhoto().clear();
//                }
//                if (viewModel.getArrayListVideo() != null) {
//                    viewModel.getArrayListVideo().clear();
//                }
//                viewModel.getAllFiles(requireActivity());
                binding.recordingInfoLayout.setVisibility(View.GONE);
                binding.videoPlayerLayout.setVisibility(View.GONE);
                binding.liveGalleryManageOptionButtons.setVisibility(View.GONE);
                viewModel.updateGalleryItemView(true);
                binding.galleryAllViewLayout.setVisibility(View.VISIBLE);

                binding.galleryAllViewBackButton.setVisibility(View.VISIBLE);
                Log.d("sizeTAG", "subscribeUI: " + viewModel.getArrayListAll().size());
                if (viewModel.getArrayListAll().size() == 0) {
                    binding.galleryAllViewManageButton.setVisibility(View.GONE);
                } else {
                    binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
                }

            }
        }));

        viewModel._isSelectRecordingInfoPlay.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                // here show video player view
                binding.galleryAllViewLayout.setVisibility(View.GONE);
                binding.recordingInfoLayout.setVisibility(View.GONE);
                binding.videoPlayerLayout.setVisibility(View.VISIBLE);
                binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
                binding.videoPlayerViewStopIconOuter.setAlpha(1.0f);
                binding.videoPlayerViewPlayIconOuter.setAlpha(1.0f);


                if (!videoFilePath.isEmpty()) {
                    binding.videoPlayerViewVideoView.setVideoPath(videoFilePath);
                    binding.videoPlayerViewVideoView.seekTo(100);
                    File file = new File(videoFilePath);
                    String[] originalFileName = file.getName().split("_");
                    int index = originalFileName[2].lastIndexOf('.');
                    if (index != -1) {
                        String substring = originalFileName[2].substring(0, index);
                        long millisecond = Long.parseLong(substring);
                        String dateString = dateFormat.format(new Date(millisecond));
                        binding.videoPlayerViewVideoTitle.setText(dateString.toUpperCase() + "-" + (selectedItemPosition + 1));
                        mHandler.postDelayed(mUpdateTimeTask, 100);
                    }
                }
                binding.videoPlayerViewRecordingInfoText.setText("Recording Info");
                binding.videoPlayerViewPauseIconOuter.setVisibility(View.VISIBLE);
                binding.videoPlayerImageUpdate.setVisibility(View.GONE);
                binding.videoPlayerViewSeekbar.setVisibility(View.VISIBLE);
                binding.videoPlayerViewPlayIconOuter.setImageResource(R.drawable.ic_play_outline);
                binding.videoPlayerViewStopIconOuter.setImageResource(R.drawable.ic_stop_outline);
                binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
                binding.recordingVideoPlayerImageView.setVisibility(View.GONE);
                binding.videoPlayerPlayIcon.setVisibility(View.VISIBLE);
                binding.videoPlayerViewVideoCurrentDuration.setVisibility(View.VISIBLE);
                binding.videoPlayerViewVideoTotalDuration.setVisibility(View.VISIBLE);
            }
        }));

        viewModel.isSelectImageInfo.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (videoFilePath.endsWith(".jpg")) {
                    Log.i("TabLayout", "subscribeUI: " + viewModel.filterImageList.indexOf(viewModel.isSelectedFileModel));
                    viewModel.isSelectedImagePosition = viewModel.filterImageList.indexOf(viewModel.isSelectedFileModel);
                    videoFilePath = viewModel.filterImageList.get(viewModel.isSelectedImagePosition).filePath;
                    binding.galleryAllViewLayout.setVisibility(View.GONE);
                    binding.recordingInfoLayout.setVisibility(View.GONE);
                    binding.videoPlayerLayout.setVisibility(View.VISIBLE);
                    binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
                    binding.videoPlayerViewRecordingInfoText.setText("Image Info");
                    binding.videoPlayerViewPauseIconOuter.setVisibility(View.GONE);
                    binding.videoPlayerViewSeekbar.setVisibility(View.GONE);
                    binding.videoPlayerViewPlayIconOuter.setImageResource(R.drawable.ic_arrow_left);
                    binding.videoPlayerViewStopIconOuter.setImageResource(R.drawable.ic_arrow_right);
                    binding.videoPlayerViewVideoView.setVisibility(View.GONE);
                    binding.recordingVideoPlayerImageView.setVisibility(View.VISIBLE);
                    binding.videoPlayerPlayIcon.setVisibility(View.GONE);
                    binding.videoPlayerViewVideoCurrentDuration.setVisibility(View.GONE);
                    binding.videoPlayerViewVideoTotalDuration.setVisibility(View.GONE);

                    if (!videoFilePath.isEmpty()) {
                        File file = new File(videoFilePath);
                        String[] originalFileName = file.getName().split("_");
                        int index = originalFileName[2].lastIndexOf('.');
                        if (index != -1) {
                            String substring = originalFileName[2].substring(0, index);
                            long millisecond = Long.parseLong(substring);
                            String dateString = dateFormat.format(new Date(millisecond));
                            binding.videoPlayerViewVideoTitle.setVisibility(View.VISIBLE);
                            binding.videoPlayerViewVideoTitle.setText(dateString.toUpperCase() + "-" + (viewModel.isSelectedImagePosition + 1));
                        }
                    }
                    switch (currentCameraSsid){
                        case NIGHTWAVE:
                            Glide.with(requireActivity()).load(videoFilePath)
                                    .placeholder(R.drawable.ic_camera_background )
                                    .into((ImageView) binding.recordingVideoPlayerImageView);
                            break;
                        case OPSIN:
                            Glide.with(requireActivity()).load(videoFilePath)
                                    .placeholder(R.drawable.opsin_live_view_background )
                                    .into((ImageView) binding.recordingVideoPlayerImageView);
                            break;
                        case NIGHTWAVE_DIGITAL:
                            Glide.with(requireActivity()).load(videoFilePath)
                                    .placeholder(R.drawable.ic_nw_digital_background )
                                    .into((ImageView) binding.recordingVideoPlayerImageView);
                            break;
                    }


                    binding.videoPlayerViewPlayIconOuter.setAlpha(1.0f);
                    binding.videoPlayerViewStopIconOuter.setAlpha(1.0f);

                    binding.videoPlayerImageUpdate.setVisibility(View.VISIBLE);
                    binding.videoPlayerImageUpdate.setText((viewModel.isSelectedImagePosition + 1) + "/" + viewModel.filterImageList.size());//0 1 2 3 (3+1)/4

                    if (viewModel.filterImageList.size() == 1) {//5/5
                        binding.videoPlayerViewPlayIconOuter.setAlpha(0.10f);
                        binding.videoPlayerViewStopIconOuter.setAlpha(0.10f);
                    } else if ((viewModel.isSelectedImagePosition + 1) == 1) {//1/5
                        binding.videoPlayerViewPlayIconOuter.setAlpha(0.10f);
                        binding.videoPlayerViewStopIconOuter.setAlpha(1.0f);
                    } else if ((viewModel.isSelectedImagePosition + 1) == viewModel.filterImageList.size()) {//5/1
                        binding.videoPlayerViewStopIconOuter.setAlpha(0.10f);
                        binding.videoPlayerViewPlayIconOuter.setAlpha(1.0f);
                    }
                }
            }
        }));

        /* for this observe erase button click event observe for currently visible fragment*/
        viewModel.isManageSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
                    viewModel.onManageAllSelectErase(true);
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
                    viewModel.onManagePhotoSelectErase(true);
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
                    viewModel.onManageVideoSelectErase(true);
                }
            }
            viewModel.updateGalleryItemView(true);
        }));

        viewModel.isBackToGallery.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.i("tablayout", "subscribeUI: back to gallery ");
                viewModel.setLiveManageSelected(false);
                resetCheckboxState();
                binding.galleryAllViewLayout.setVisibility(View.VISIBLE);
                binding.liveGalleryManageOptionButtons.setVisibility(View.GONE);
                binding.galleryAllViewManageButton.setVisibility(View.VISIBLE);
                binding.galleryAllViewBackButton.setVisibility(View.VISIBLE);
                binding.videoPlayerLayout.setVisibility(View.GONE);
                binding.recordingInfoLayout.setVisibility(View.GONE);
                viewModel.updateGalleryItemView(false);

                if (viewModel.getArrayListAll() != null) {
                    viewModel.getArrayListAll().clear();
//                    viewModel.getArrayListPhoto().clear();
                }
                viewModel.getAllFiles(getActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());

                setGalleryAdapter(binding.galleryAllViewViewpager.getCurrentItem(), false);
            }
        }));

        viewModel.isSelectAllSelected.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            Log.i("TABLAYOUTDIALOG", "allSelected >> " + aBoolean);
            if (aBoolean) {
                binding.galleryManageViewSelectAllButton.setText(getText(R.string.unselect_all));
            } else {
                binding.galleryManageViewSelectAllButton.setText(getText(R.string.select_all));

            }
        }));

        //selectAll button event based on Manage View ViewPager
        viewModel.isManageSelectAll2.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryManageViewSelectAllButton.getText().toString().contains(getString(R.string.select_all))) {
                    if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
                        viewModel.onManageSelectAllView(true);
                    } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
                        viewModel.onManageSelectAllPhotos(true);
                    } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
                        viewModel.onManageSelectAllVideos(true);
                    }
//                    if (!isRecordingStarted)
//                        viewModel.setSelectAllSelected(true);
                } else if (binding.galleryManageViewSelectAllButton.getText().toString().contains(getString(R.string.unselect_all))) {
                    if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
                        viewModel.onClearSelectedCheckboxAllView(true);
                    } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
                        viewModel.onClearSelectedCheckboxPhotoView(true);
                    } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
                        viewModel.onClearSelectedCheckboxVideoView(true);
                    }
//                    if (!isRecordingStarted)
//                        viewModel.setSelectAllSelected(false);
                }
                viewModel.updateGalleryItemView(false);
            }
        }));

        viewModel._isUpdateGalleryItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
                    viewModel.updateGalleryAllItemView(true);
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
                    viewModel.updateGalleryPhotosItemView(true);
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
                    viewModel.updateGalleryVideosItemView(true);
                }
                viewModel.updateGalleryItemView(false);
            }
        }));

        viewModel.isManageSelectDownload.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
                    viewModel.onManageSelectAllDownload(true);
                    if (viewModel.isDownloaded) {
                        viewModel.setBackToGallery(true);
                    }
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
                    viewModel.onManageSelectAllPhotoDownload(true);
                    if (viewModel.isPhotoDownload) {
                        viewModel.setBackToGallery(true);
                    }
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
                    viewModel.onManageSelectAllVideoDownload(true);
                    if (viewModel.isVideoDownload) {
                        viewModel.setBackToGallery(true);
                    }
                }
                viewModel.updateGalleryItemView(false);
            }
        }));

        viewModel.clearSelectedCheckBox.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
                    viewModel.onClearSelectedCheckboxAllView(true);
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
                    viewModel.onClearSelectedCheckboxPhotoView(true);
                } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
                    viewModel.onClearSelectedCheckboxVideoView(true);
                }
                viewModel.updateGalleryItemView(false);
            }
        }));

        /* gallery video player view */
        binding.videoPlayerViewSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
                    try {
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(i, MediaPlayer.SEEK_CLOSEST);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.videoPlayerViewVideoView.setOnPreparedListener(mp -> {
            mediaPlayer = mp;
            binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
            binding.videoPlayerViewVideoView.seekTo(0);
            mHandler.postDelayed(mUpdateTimeTask, 100);
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(viewModel.getPlaybackPosition(), MediaPlayer.SEEK_CLOSEST);
            }
            binding.videoPlayerViewSeekbar.setMin(0);
            binding.videoPlayerViewSeekbar.setMax(binding.videoPlayerViewVideoView.getDuration());

            /* for this get media player status get*/
            new Handler().postDelayed(() -> {
                try {
                    if (binding.videoPlayerViewVideoView.isPlaying()) {
                        binding.videoPlayerPlayIcon.setVisibility(View.INVISIBLE);
                    } else {
                        binding.videoPlayerPlayIcon.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1000);

            // Start the timer to update the seek bar position
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (binding.videoPlayerViewVideoView.isPlaying()) {
                            binding.videoPlayerViewSeekbar.setProgress(binding.videoPlayerViewVideoView.getCurrentPosition());
                            viewModel.setPlaybackPosition(binding.videoPlayerViewVideoView.getCurrentPosition());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 100);
        });

        binding.videoPlayerViewVideoView.setOnErrorListener((mp, what, extra) -> {
            Log.d("video__", "setOnErrorListener ");
            return true;
        });

        binding.videoPlayerViewVideoView.setOnCompletionListener(mp -> {
            mediaPlayer = mp;
            binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
            //if (binding.videoPlayerViewVideoView.isPlaying())
            binding.videoPlayerViewVideoView.seekTo(0);
            binding.videoPlayerViewVideoView.pause();
            viewModel.setPlaybackPosition(0);
            // binding.videoPlayerViewVideoView.stopPlayback();
            binding.videoPlayerPlayIcon.setVisibility(View.VISIBLE);
            viewModel.setVideoPlay(false);
            viewModel.setVideoPause(false);
            binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_outline));
        });
       /* binding.videoPlayerViewVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d("video", "setOnErrorListener ");
                return true;
            }
        });*/

        viewModel._isSelectPlayVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.videoPlayerViewRecordingInfoText.getText().equals("Image Info")) {

                    if (viewModel.filterImageList != null) {

                        if (viewModel.isSelectedImagePosition > 0) { //3 > 0
                            viewModel.isSelectedImagePosition--;//2
                            nextImage(viewModel.isSelectedImagePosition);
                        }
                    }

                } else {
                    binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
                    isPlaying();
                    if (!binding.videoPlayerViewVideoView.isPlaying() && !viewModel.isVideoPlay()) {
                        binding.videoPlayerViewVideoView.start();
                        viewModel.setVideoPlay(true);
                        viewModel.setVideoPause(false);
                        binding.videoPlayerPlayIcon.setVisibility(View.INVISIBLE);
                        binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_filled));
                    } else if (viewModel.isVideoPause()) {
                        binding.videoPlayerViewVideoView.resume();
                        viewModel.setVideoPlay(true);
                        viewModel.setVideoPause(false);
                        binding.videoPlayerPlayIcon.setVisibility(View.VISIBLE);
                        binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_outline));
                    } else {
                        Log.e("TAG", "subscribeUI: video play else");
                        binding.videoPlayerViewVideoView.start();
                        viewModel.setVideoPlay(true);
                        viewModel.setVideoPause(false);
                        isPlaying();

                    }
                }
            }
        }));

        viewModel._isSelectPauseVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.videoPlayerViewVideoView.isPlaying() && !viewModel.isVideoPause() || binding.videoPlayerViewVideoView.canPause()) {
                    binding.videoPlayerViewVideoView.pause();
                    viewModel.setVideoPause(true);
                    viewModel.setVideoPlay(false);
                    isPaused();
                }
            }
        }));

        viewModel._isSelectStopVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.videoPlayerViewRecordingInfoText.getText().equals("Image Info")) {
                    if (viewModel.filterImageList != null) {
                        int endIndex = viewModel.filterImageList.size() - 1;
                        if (viewModel.isSelectedImagePosition < endIndex) { //3 > 0
                            viewModel.isSelectedImagePosition++;//2
                            nextImage(viewModel.isSelectedImagePosition);
                        }
                    }
                } else {
                    if (binding.videoPlayerViewVideoView.isPlaying() || (viewModel.isVideoPlay() || viewModel.isVideoPause() || binding.videoPlayerViewVideoView.canPause())) {
                        binding.videoPlayerViewVideoView.seekTo(0);
                        binding.videoPlayerViewVideoView.pause();
                        viewModel.setVideoPlay(false);
                        /*viewModel.setVideoPause(false);*/
                        viewModel.setPlaybackPosition(0);
                        isStopped();
                    }
                }
            }
        }));

        viewModel._isSelectVideoPlayerBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (videoFilePath.endsWith(".mp4")) {
                    binding.videoPlayerViewPlayIconOuter.setAlpha(1.0f);
                    binding.videoPlayerViewStopIconOuter.setAlpha(1.0f);
                    binding.galleryAllViewLayout.setVisibility(View.GONE);
                    binding.recordingInfoLayout.setVisibility(View.VISIBLE);
                    binding.videoPlayerLayout.setVisibility(View.GONE);
                    binding.videoPlayerViewVideoView.setVisibility(View.GONE);

                    if (binding.videoPlayerViewVideoView.isPlaying())
                        binding.videoPlayerViewVideoView.stopPlayback();

                    viewModel.setVideoPlay(false);
                    viewModel.setVideoPause(false);
                    viewModel.setPlaybackPosition(0);
                    binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_outline));
                    binding.videoPlayerViewPauseIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_pause_outline));
                    binding.videoPlayerViewStopIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_outline));
                    mHandler.removeCallbacks(mUpdateTimeTask);

//                    showRecordingVideoInfolayout(galleryItemInfo);
                } else {
                    binding.galleryAllViewLayout.setVisibility(View.VISIBLE);
                    binding.recordingInfoLayout.setVisibility(View.GONE);
                    binding.videoPlayerLayout.setVisibility(View.GONE);
                    binding.videoPlayerViewVideoView.setVisibility(View.GONE);
//                    showCapturedImageInfo(galleryItemInfo);
                }
            }
        }));

        viewModel._isSelectVideoPlayerCancel.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
//                resetCheckboxState();
                if (binding.videoPlayerViewVideoView.isPlaying())
                    binding.videoPlayerViewVideoView.stopPlayback();
                mHandler.removeCallbacks(mUpdateTimeTask);
                backToLiveView();
                cameraViewModel.setIsShowAlertDialog(false);
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                viewModel.setVideoPlay(false);
                viewModel.setVideoPause(false);
                viewModel.setPlaybackPosition(0);
                onSelectGallery = false;

                galleryViewModel.setClosePopupAllMedia(true);
                galleryViewModel.setClosePopupImage(true);
                galleryViewModel.setClosePopupVideo(true);
                manageViewAdapter = null;
                binding.galleryAllViewViewpager.setAdapter(null);
            }
        }));
        cameraViewModel.isEnableMultiWindowMode.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.e(TAG, "getEnterExitMultiWindowMode: " + aBoolean);
                viewModel.onCancelGalleryAllView();
            }
        }));
        if (requireActivity().isInMultiWindowMode()) {
            Log.e(TAG, "subscribeUI: getEnterExitMultiWindowMode if " );
            viewModel.onCancelGalleryAllView();
        }
    }

    private void nextImage(int curImgPos) {
        String filepath = viewModel.filterImageList.get(curImgPos).filePath;
        switch (currentCameraSsid){
            case NIGHTWAVE:
                Glide.with(requireActivity()).load(filepath)
                        .placeholder( R.drawable.ic_camera_background )
                        .into((ImageView) binding.recordingVideoPlayerImageView);
                break;
            case OPSIN:
                Glide.with(requireActivity()).load(filepath)
                        .placeholder( R.drawable.opsin_live_view_background )
                        .into((ImageView) binding.recordingVideoPlayerImageView);
                break;
            case NIGHTWAVE_DIGITAL:
                Glide.with(requireActivity()).load(filepath)
                        .placeholder( R.drawable.ic_nw_digital_background )
                        .into((ImageView) binding.recordingVideoPlayerImageView);
                break;
        }

        binding.videoPlayerViewPlayIconOuter.setAlpha(1.0f);
        binding.videoPlayerViewStopIconOuter.setAlpha(1.0f);

        String imageUpdate = (curImgPos + 1) + "/" + viewModel.filterImageList.size();
        binding.videoPlayerImageUpdate.setVisibility(View.VISIBLE);
        binding.videoPlayerImageUpdate.setText(imageUpdate);

        if (viewModel.filterImageList.size() == 1) {//5/5
            binding.videoPlayerViewPlayIconOuter.setAlpha(0.10f);
            binding.videoPlayerViewStopIconOuter.setAlpha(0.10f);
        } else if ((viewModel.isSelectedImagePosition + 1) == 1) {//1/5
            binding.videoPlayerViewPlayIconOuter.setAlpha(0.10f);
            binding.videoPlayerViewStopIconOuter.setAlpha(1.0f);
        } else if ((viewModel.isSelectedImagePosition + 1) == viewModel.filterImageList.size()) {//5/1
            binding.videoPlayerViewStopIconOuter.setAlpha(0.10f);
            binding.videoPlayerViewPlayIconOuter.setAlpha(1.0f);
        }


        viewModel.isSelectedImagePosition = curImgPos;

        if (!filepath.isEmpty()) {
            File file = new File(filepath);
            String[] originalFileName = file.getName().split("_");
            int index = originalFileName[2].lastIndexOf('.');
            if (index != -1) {
                String substring = originalFileName[2].substring(0, index);
                long millisecond = Long.parseLong(substring);
                String dateString = dateFormat.format(new Date(millisecond));
                String videoTitle = dateString.toUpperCase() + "-" + (curImgPos + 1);
                binding.videoPlayerViewVideoTitle.setVisibility(View.VISIBLE);
                binding.videoPlayerViewVideoTitle.setText(videoTitle);
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy ");
    @SuppressLint("SimpleDateFormat")
    DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss z");

    @SuppressLint("SetTextI18n")
    private void showRecordingVideoInfolayout(GallerySelectedItemInfo gallerySelectedItemInfo) {
        File file = new File(gallerySelectedItemInfo.getFilePath());
        Date lastModifiedDate = new Date(file.lastModified());
        binding.recordingInfoViewPlayButton.setVisibility(View.VISIBLE);
        binding.recordingInfoViewTitleText.setText(getString(R.string.recording_info_title));
        binding.recordingInfoViewDeleteButton.setText(getString(R.string.delete_recording_text));
        binding.recordingInfoScrollView.setVisibility(View.VISIBLE);
        binding.imageInfoScrollView.setVisibility(View.GONE);
        binding.recordingInfoViewVideoDate.setText(dateFormat.format(lastModifiedDate).toUpperCase());
        binding.recordingInfoViewVideoTime.setText(timeFormat.format(lastModifiedDate));

        MediaPlayer mediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(gallerySelectedItemInfo.getFilePath()));
        if (mediaPlayer != null) {
            binding.recordingInfoViewVideoDuration.setText(getTotalVideoDuration(mediaPlayer.getDuration()));
        }

        /* for this remove file extension and ssid*/
        String[] originalFileName = file.getName().split("_");
        int index = originalFileName[2].lastIndexOf('.');
        if (index != -1) {
            String substring = originalFileName[2].substring(0, index);
            long millisecond = Long.parseLong(substring);
            String dateString = dateFormat.format(new Date(millisecond));
            String videoTitle = dateString.toUpperCase() + "-" + (gallerySelectedItemInfo.getItemPosition() + 1);
            binding.recordingInfoViewVideoTitle.setText(videoTitle);
        }
        if (file.exists()) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            // retriever.setDataSource(requireContext(), Uri.parse(filePath));
            if (file.length() > 0) {
                retriever.setDataSource(gallerySelectedItemInfo.getFilePath());
            }
            switch (currentCameraSsid){
                case NIGHTWAVE:
                    Glide.with(requireActivity()).load(gallerySelectedItemInfo.getFilePath())
                            .placeholder( R.drawable.ic_camera_background )
                            .into((ImageView) binding.recordingInfoViewVideoThumbnail);
                    break;
                case OPSIN:
                    Glide.with(requireActivity()).load(gallerySelectedItemInfo.getFilePath())
                            .placeholder( R.drawable.opsin_live_view_background )
                            .into((ImageView) binding.recordingInfoViewVideoThumbnail);
                    break;
                case NIGHTWAVE_DIGITAL:
                    Glide.with(requireActivity()).load(gallerySelectedItemInfo.getFilePath())
                            .placeholder( R.drawable.ic_nw_digital_background )
                            .into((ImageView) binding.recordingInfoViewVideoThumbnail);
                    break;
            }
//                    Bitmap thumbnail = retriever.getFrameAtTime();
//                    binding.recordingInfoViewVideoThumbnail.setImageBitmap(thumbnail);
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
                        binding.recordingInfoViewVideoLocation.setText("Lat: " + latitude + "\nLon: " + longitude);
                    else
                        binding.recordingInfoViewVideoLocation.setText(getString(R.string.location_not_found));

                } else {
                    binding.recordingInfoViewVideoLocation.setText(getString(R.string.location_not_found));
                }
            } else {
                binding.recordingInfoViewVideoLocation.setText(getString(R.string.location_not_found));
            }
        }
        videoFilePath = gallerySelectedItemInfo.getFilePath();
        selectedItemPosition = gallerySelectedItemInfo.getItemPosition();
    }

    private final DecimalFormat df = new DecimalFormat("#.####");

    private void showCapturedImageInfo(GallerySelectedItemInfo gallerySelectedItemInfo) {
        File file = new File(gallerySelectedItemInfo.getFilePath());
        Date lastModifiedDate = new Date(file.lastModified());
        binding.recordingInfoViewPlayButton.setVisibility(View.GONE);
        binding.galleryAllViewLayout.setVisibility(View.GONE);
        binding.recordingInfoLayout.setVisibility(View.VISIBLE);
        binding.videoPlayerLayout.setVisibility(View.GONE);
        binding.recordingInfoViewTitleText.setText(getString(R.string.image_info));
        binding.recordingInfoViewDeleteButton.setText(getString(R.string.delete_image_text));
        binding.recordingInfoScrollView.setVisibility(View.GONE);
        binding.imageInfoScrollView.setVisibility(View.VISIBLE);
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
            binding.recordingInfoViewLocation.setText(latLon);
            binding.recordingInfoViewModel.setText(exifInterface.getAttribute(ExifInterface.TAG_MODEL));
            binding.recordingInfoViewResolution.setText(resolutionFormat);
            binding.recordingInfoViewSoftware.setText(exifInterface.getAttribute(ExifInterface.TAG_SOFTWARE));
            binding.recordingInfoViewAperture.setText(apertureFormatted);
            binding.recordingInfoViewFocalLength.setText(focalLengthFormatted);
            binding.recordingInfoViewTime.setText(timeFormat.format(lastModifiedDate));
            binding.recordingInfoViewDate.setText(dateFormat.format(lastModifiedDate).toUpperCase());
        } catch (IOException e) {
            e.printStackTrace();
        }


        binding.recordingInfoViewDate.setText(dateFormat.format(lastModifiedDate).toUpperCase());
        binding.recordingInfoViewTime.setText(timeFormat.format(lastModifiedDate));

        String[] originalFileName = file.getName().split("_");
        int index = originalFileName[2].lastIndexOf('.');
        if (index != -1) {
            String substring = originalFileName[2].substring(0, index);
            long millisecond = Long.parseLong(substring);
            String dateString = dateFormat.format(new Date(millisecond));
            String videoTitle = dateString.toUpperCase() + "-" + (gallerySelectedItemInfo.itemPosition + 1);
            binding.recordingInfoViewVideoTitle.setText(videoTitle);
            /*binding.cameraRecordViewChoiceTitle.setText(dateString.toUpperCase());
            binding.cameraRecordViewChoiceTitle.setEllipsize(TextUtils.TruncateAt.END);*/
        }


        /*int index = file.getName().lastIndexOf('.');
        String substring = file.getName().substring(0, index);
        long millisecond = Long.parseLong(substring);
        String dateString = dateFormat.format(new Date(millisecond));*/

        switch (currentCameraSsid){
            case NIGHTWAVE:
                Glide.with(requireActivity()).load(gallerySelectedItemInfo.getFilePath())
                        .placeholder( R.drawable.ic_camera_background )
                        .into((ImageView) binding.recordingInfoViewVideoThumbnail);
                break;
            case OPSIN:
                Glide.with(requireActivity()).load(gallerySelectedItemInfo.getFilePath())
                        .placeholder( R.drawable.opsin_live_view_background )
                        .into((ImageView) binding.recordingInfoViewVideoThumbnail);
                break;
            case NIGHTWAVE_DIGITAL:
                Glide.with(requireActivity()).load(gallerySelectedItemInfo.getFilePath())
                        .placeholder( R.drawable.ic_nw_digital_background )
                        .into((ImageView) binding.recordingInfoViewVideoThumbnail);
                break;
        }

        videoFilePath = gallerySelectedItemInfo.getFilePath();
    }

    public double convertDMSToDD(String degrees, String minutes, String seconds, String direction) {
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

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
        }
    }

    private void resetCheckboxState() {
        if (binding.galleryAllViewViewpager.getCurrentItem() == 0) {
            viewModel.onClearSelectedCheckboxAllView(true);
        } else if (binding.galleryAllViewViewpager.getCurrentItem() == 1) {
            viewModel.onClearSelectedCheckboxPhotoView(true);
        } else if (binding.galleryAllViewViewpager.getCurrentItem() == 2) {
            viewModel.onClearSelectedCheckboxVideoView(true);
        }
        viewModel.updateGalleryItemView(false);
    }

    /*
    Gallery adapter
     */

    private void setGalleryAdapter(int currentTabposition, boolean isManageClicked) {
        galleryViewModel.setIfManageSelected(isManageClicked);
        if (manageViewAdapter == null) {
            manageViewAdapter = new GalleryLiveTabPageAdapter(this, galleryViewModel.getFragments());
            binding.galleryAllViewViewpager.setAdapter(manageViewAdapter);
            binding.galleryAllViewViewpager.setOffscreenPageLimit(3);
            new TabLayoutMediator(binding.galleryAllViewTablayout, binding.galleryAllViewViewpager, (tab, position) -> {
            }).attach();
            binding.galleryAllViewViewpager.setCurrentItem(currentTabposition);
            viewModel.isCurrentTab = currentTabposition;
            Objects.requireNonNull(binding.galleryAllViewTablayout.getTabAt(0)).setText(getString(R.string.all));
            Objects.requireNonNull(binding.galleryAllViewTablayout.getTabAt(1)).setText(getString(R.string.photos));
            Objects.requireNonNull(binding.galleryAllViewTablayout.getTabAt(2)).setText(getString(R.string.videos));
        }

    }

    /*
    function for icon handling when play ican has been pressed
    */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void isPlaying() {
        binding.videoPlayerPlayIcon.setVisibility(View.INVISIBLE);
        binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_filled));
        binding.videoPlayerViewPauseIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_pause_outline));
        binding.videoPlayerViewStopIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_outline));
    }

    /*
    function for icon handling when stop pause has been pressed
    */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void isPaused() {
        binding.videoPlayerPlayIcon.setVisibility(View.VISIBLE);
        binding.videoPlayerViewPauseIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_pause_filled));
        binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_outline));
        binding.videoPlayerViewStopIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_outline));
    }

    /*
    function for icon handling when stop ican has been pressed
    */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void isStopped() {
        binding.videoPlayerPlayIcon.setVisibility(View.VISIBLE);
        binding.videoPlayerViewStopIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_stop_filled));
        binding.videoPlayerViewPauseIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_pause_outline));
        binding.videoPlayerViewPlayIconOuter.setImageDrawable(AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_play_outline));
    }

    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            try {
                binding.videoPlayerViewVideoView.setVisibility(View.VISIBLE);
                int currentDuration = binding.videoPlayerViewVideoView.getCurrentPosition();
                String currentDurationString = getDurationString(currentDuration);
                int totalDuration = binding.videoPlayerViewVideoView.getDuration();
                String totalDurationString = getDurationString(totalDuration);
                binding.videoPlayerViewVideoCurrentDuration.setText(currentDurationString);
                binding.videoPlayerViewVideoTotalDuration.setText(totalDurationString);
                binding.videoPlayerViewSeekbar.setProgress(currentDuration);
                // binding.videoPlayerViewSeekbar.setProgress(currentDuration);
                mHandler.postDelayed(this, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

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

    @SuppressLint("DefaultLocale")
    public String getDurationString(int value) {
        String videoCurrentDuration;
        long s = value / 1000;
        videoCurrentDuration = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
        return videoCurrentDuration;
    }

    private void backToLiveView() {
        if (binding.getRoot().getVisibility() == View.VISIBLE) {
            cameraViewModel.setIsShowAlertDialog(false);
            dismiss();
        }
    }
}