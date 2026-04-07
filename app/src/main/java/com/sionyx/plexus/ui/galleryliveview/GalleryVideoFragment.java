package com.sionyx.plexus.ui.galleryliveview;

import static android.view.View.SCROLLBAR_POSITION_LEFT;
import static com.sionyx.plexus.ui.home.HomeViewModel.isRecordingStarted;
import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.DESTINATION_FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.IS_IMAGE;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.NOTIFICATION_ID;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.NOTIFICATION_ID_VIDEO_LIVE;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;

import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentGalleryVideoBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.dialog.GalleryAllViewModel;
import com.sionyx.plexus.ui.galleryliveview.livetabviewadapter.GalleryLiveViewPageAdapter;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker;

import java.io.File;
import java.util.ArrayList;

public class GalleryVideoFragment extends Fragment {
    private static final String TAG = "GalleryVideoFragment";
    private FragmentGalleryVideoBinding binding;
    private boolean hasShowCheckbox;
    private GalleryAllViewModel galleryAllViewModel;
    private GalleryViewModel galleryViewModel;
    private LifecycleOwner lifecycleOwner;
    private GalleryLiveViewPageAdapter manageVideoViewAdapter;
    private boolean isVisible;
    private final Handler mHandler = new Handler();
    private HomeViewModel homeViewModel;
    private BleWiFiViewModel bleWiFiViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        galleryAllViewModel = new ViewModelProvider(requireActivity()).get(GalleryAllViewModel.class);
        galleryViewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryVideoBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        binding.setViewModel(galleryAllViewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        galleryViewModel.isManageSelected.observe(lifecycleOwner, isManageAllSelected);
        galleryViewModel.closePopupVideo.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                Log.e(TAG, "closePopupVideo: ");
                manageVideoViewAdapter = null;
                binding.galleryVideoRecylerView.setAdapter(null);
                galleryViewModel.isManageSelected.removeObserver(isManageAllSelected);
                galleryViewModel.isManageSelected.removeObservers(lifecycleOwner);
            }
        }));

    }

    private final Observer<Boolean> isManageAllSelected = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            Log.e(TAG, "isManageAllSelected: " + aBoolean);
            hasShowCheckbox = aBoolean;
            subscribeUI();
        }
    };

    private void subscribeUI() {
        new Handler().post(this::getGalleryVideos);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getGalleryVideos() {

        binding.setViewModel(galleryAllViewModel);

        if (galleryAllViewModel.getArrayListAll() != null) {
            for (int i = 0; i < galleryAllViewModel.getArrayListAll().size(); i++) {
                GalleryManageModel manageModel = galleryAllViewModel.getArrayListAll().get(i);
                if (manageModel.getFilePath().endsWith(".mp4")) {
                    if (!galleryAllViewModel.getArrayListVideo().contains(manageModel)) {
                        galleryAllViewModel.addVideos(manageModel);
                    }
                }
            }
        }


        /*
        Check whether list of size, If it is 0 the Records not found tv will be visible
         */
        if (!galleryAllViewModel.getArrayListVideo().isEmpty()) {
            galleryAllViewModel.setNoRecordsFoundVideo(false);
            if (manageVideoViewAdapter == null) {
                manageVideoViewAdapter = new GalleryLiveViewPageAdapter(hasShowCheckbox, getActivity(), galleryAllViewModel.getArrayListVideo(), galleryAllViewModel, 2);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
                binding.galleryVideoRecylerView.setLayoutManager(gridLayoutManager);
                binding.galleryVideoRecylerView.setAdapter(manageVideoViewAdapter);
                binding.galleryVideoRecylerView.setVerticalScrollbarPosition(SCROLLBAR_POSITION_LEFT);
            } else {
                manageVideoViewAdapter.replaceWithLatest(galleryAllViewModel.getArrayListVideo(), hasShowCheckbox, 2);
            }
        } else {
            Log.i(TAG, "getGalleryVideos: >>>>>>>>>> 1");
            noRecordsFound();
        }

        /*for this observe select all button press*/
        galleryAllViewModel.isManageSelectAllVideos.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean && isVisible) {
                if (manageVideoViewAdapter != null && !galleryAllViewModel.getArrayListVideo().isEmpty()) {
                    manageVideoViewAdapter.selectAllCheckbox(2, true, mHandler);
                    galleryAllViewModel.onManageSelectAllVideos(false);
                    galleryAllViewModel.isErasedVideo = false;
                    galleryAllViewModel.hasEraseLiveVideoMedia(false);
                    isSelectAll();

                }
            }
        }));

        galleryAllViewModel.isMedialFileDeleted.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                galleryAllViewModel.isErasedVideo = true;
                galleryAllViewModel.getArrayListAll().clear();
                galleryAllViewModel.getArrayListVideo().clear();
                galleryAllViewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                getGalleryVideos();
                manageVideoViewAdapter.notifyDataSetChanged();
            }
        }));

        /*for this observe erase button press*/
        galleryAllViewModel.isManageVideoSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (manageVideoViewAdapter != null && !galleryAllViewModel.getArrayListVideo().isEmpty() && isVisible) {
                    boolean isChecked = false;
                    for (GalleryManageModel i : galleryAllViewModel.getArrayListVideo()) {
                        if ((i.getFilePath().endsWith(".mp4"))) {
                            if (i.isChecked) {
                                isChecked = true;
                                break;
                            }
                        }
                    }
                    if (isChecked) {
                        String confirmMsg;
                        if (homeViewModel.checkedItemCount > 1) {
                            confirmMsg = getString(R.string.multiple_confirm_erase);
                        } else {
                            confirmMsg = getString(R.string.confirm_erase);
                        }
                        showConfirmEraseGalleryItemDialog(confirmMsg);
                    } else {
                        Toast.makeText(getActivity(), R.string.not_erased, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }));

        galleryAllViewModel.isEraseLiveVideoMedia.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (manageVideoViewAdapter != null && !galleryAllViewModel.getArrayListVideo().isEmpty() && isVisible) {
                    Log.d("KReeTAG", "getGalleryVideos: ");
                    manageVideoViewAdapter.erased();
                    galleryAllViewModel.isErasedVideo = true;
                    galleryAllViewModel.getArrayListVideo().clear();
                    galleryAllViewModel.getAllFiles(getActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    getGalleryVideos();
                    manageVideoViewAdapter.notifyDataSetChanged();
                    galleryAllViewModel.onManageVideoSelectErase(false);
                    galleryAllViewModel.hasEraseLivePhotoMedia(false);
                }
            }
        }));

        /*for this observe download button press*/
        galleryAllViewModel.isManageSelectAllVideoDownload.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (galleryAllViewModel.getArrayListVideo() != null && !galleryAllViewModel.getArrayListVideo().isEmpty()) {
                    downloadGallery();
                }
            }
        }));

        /* for this after selected check box cleared while tab swipe and after file download button press cleared*/
        galleryAllViewModel.isClearSelectedCheckboxVideoView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (manageVideoViewAdapter != null && !galleryAllViewModel.getArrayListVideo().isEmpty()) {
                    manageVideoViewAdapter.selectAllCheckbox(2, false, mHandler);
                }
            }
        }));

        /* for this only after delete gallery item to refresh adapter*/
        galleryAllViewModel.isUpdateGalleryVideosItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (!galleryAllViewModel.getArrayListVideo().isEmpty()) {
                    galleryAllViewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    galleryAllViewModel.updateGalleryVideosItemView(false);
                }
            }
        }));

        galleryViewModel.hasShowGalleryVideoProgressbar.observe(lifecycleOwner, aBoolean -> {
            Log.e(TAG, "hasShowGalleryVideoProgressbar: " + aBoolean);
            if (aBoolean)
                binding.dotProgressBar.setVisibility(View.VISIBLE);
            else
                binding.dotProgressBar.setVisibility(View.GONE);
        });
    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
        }
    }

    /*
    Records not found function has been created for handling the visibility views
     */
    @SuppressLint("SetTextI18n")
    private void noRecordsFound() {
        /* galleryAllViewModel.setBackToGallery(true);*/
        binding.galleryVideoRecylerView.setVisibility(View.GONE);
        binding.tvNoRecordFoundVideoViews.setVisibility(View.VISIBLE);
        galleryAllViewModel.setNoRecordsFoundVideo(true);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "getGalleryVideos: >>>>>>>>>> 2");
        if (galleryAllViewModel.getArrayListVideo() != null) {
            isSelectAll();
        }
        if (galleryAllViewModel.getArrayListVideo().isEmpty()) {
            galleryAllViewModel.setNoRecordsFoundVideo(true);
        }
        if (isRecordingStarted && galleryAllViewModel.getArrayListVideo().size() == 1) {
            galleryAllViewModel.onManageViewBack();
        }
    }

    private void isSelectAll() {
        ArrayList<GalleryManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < galleryAllViewModel.getArrayListVideo().size(); i++) {
            if (galleryAllViewModel.getArrayListVideo().get(i).isChecked()) {
                tempList.add(galleryAllViewModel.getArrayListVideo().get(i));
            }
        }
        homeViewModel.checkedItemCount = tempList.size();
        Log.i(TAG, "onResume: video " + galleryAllViewModel.getArrayListVideo().size() + "selected " + tempList.size());
        if (isRecordingStarted) {
            galleryAllViewModel.setSelectAllSelected(galleryAllViewModel.getArrayListVideo().size() == (tempList.size() + 1));
        } else {
            galleryAllViewModel.setSelectAllSelected(galleryAllViewModel.getArrayListVideo().size() == tempList.size());
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "getGalleryVideos: >>>>>>>>>> 3");
        /*this is to notify adapter from background to foreground state*/
        if (manageVideoViewAdapter != null) {
            galleryAllViewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
            getGalleryVideos();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            isVisible = true;
            if (galleryViewModel != null)
                galleryViewModel.isManageSelected.observe(lifecycleOwner, isManageAllSelected);
            if (manageVideoViewAdapter != null && galleryAllViewModel != null) {
                if (galleryAllViewModel.getArrayListVideo() != null) {
                    if (galleryAllViewModel.isErasedVideo) {
                        galleryAllViewModel.getArrayListVideo().clear();
                        // galleryAllViewModel.getArrayListAll().clear();
                        galleryAllViewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                        galleryAllViewModel.isErasedVideo = false;
                    }
//                    else {
                    galleryAllViewModel.getArrayListVideo().clear();
                    galleryAllViewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
//                    }
                    getGalleryVideos();
                }
                manageVideoViewAdapter.notifyDataSetChanged();
            }
        } else {
            isVisible = false;
            if (galleryViewModel != null) {
                galleryViewModel.isManageSelected.removeObserver(isManageAllSelected);
                galleryViewModel.isManageSelected.removeObservers(lifecycleOwner);
                galleryViewModel.hasShowGalleryVideoProgressbar(false);
            }
        }
    }

    private void downloadGallery() {
        galleryAllViewModel.isVideoDownload = false;
        long totalFileSize = 0;
        for (int i = 0; i < galleryAllViewModel.getArrayListVideo().size(); i++) {
            if (galleryAllViewModel.getArrayListVideo().get(i).isChecked()) {
                galleryAllViewModel.isVideoDownload = true;
                File file = new File(galleryAllViewModel.getArrayListVideo().get(i).getFilePath());
                totalFileSize += file.length();
            }
        }

        long availableInternalStorageSize = getAvailableInternalStorageSize();
        long minimumStorageForDownload = 1073741824L;
        Log.d("Internal Storage", "Available Internal Storage: " + availableInternalStorageSize + " TotalFileSize" + totalFileSize);
        if (totalFileSize < availableInternalStorageSize && minimumStorageForDownload < availableInternalStorageSize) {
            for (int i = 0; i < galleryAllViewModel.getArrayListVideo().size(); i++) {
                if (galleryAllViewModel.getArrayListVideo().get(i).isChecked()) {
                    if (galleryAllViewModel.getArrayListVideo().get(i).getFilePath().contains(".mp4")) {
                        galleryViewModel.incrementVideoCheckedFileCount();
                        galleryAllViewModel.isVideoDownload = true;
                        fileDownloadToGallery(galleryAllViewModel.getArrayListVideo().get(i).getFilePath(), "SiOnyx", false);
                    }
                }
            }
        } else {
            if (galleryAllViewModel.isVideoDownload)
                showStorageAlert(getString(R.string.no_memory_to_download));
        }

        if (!galleryAllViewModel.isVideoDownload) {
            Toast.makeText(requireActivity(), R.string.download_unsuccessful, Toast.LENGTH_SHORT).show();
        }
        galleryAllViewModel.clearSelectedCheckBox();
    }

    public void fileDownloadToGallery(String filePath, String destinationPath, boolean isImage) {
        new Handler().post(() -> {
            if (filePath != null) {
                Data inputData = new Data.Builder()
                        .putString(FILE_PATH, filePath)
                        .putString(DESTINATION_FILE_PATH, destinationPath)
                        .putBoolean(IS_IMAGE, isImage)
                        .putInt(NOTIFICATION_ID, 6)
                        .build();
                OneTimeWorkRequest downloadRequest = new OneTimeWorkRequest.Builder(LiveViewFileDownloadWorker.class)
                        .setInputData(inputData)
                        .build();

                galleryViewModel.workManager.enqueue(downloadRequest);
                galleryViewModel.workManager.getWorkInfoByIdLiveData(downloadRequest.getId()).observeForever(workInfo -> {
                    if (workInfo != null) {
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                Data outputData = workInfo.getOutputData();
                                int result = outputData.getInt("resultKey", 0);
                                galleryViewModel.incrementVideoFileCount();
                                if (galleryViewModel.getTotalVideoFileCompleteCount() == galleryViewModel.getTotalCheckedVideoFileCount()) {
                                    galleryViewModel.totalVideoFileCompleteCount = 0;
                                    galleryViewModel.totalVideoFileCheckedCount = 0;
                                    galleryViewModel.setHasWorkMangerRunVideo(false);
                                    galleryViewModel.hasShowGalleryVideoProgressbar(false);
                                    galleryViewModel.notificationBuilder("File download complete", NOTIFICATION_ID_VIDEO_LIVE);
                                    try {
                                        Toast.makeText(galleryViewModel.appContext, R.string.download_successful, Toast.LENGTH_SHORT).show();
                                    } catch (Resources.NotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.e(TAG, "WorkManager: success" + galleryViewModel.getTotalCheckedVideoFileCount() + " //" + galleryViewModel.getTotalVideoFileCompleteCount());
                                break;
                            case FAILED:
                                galleryViewModel.incrementVideoFileCount();
                                galleryViewModel.setHasWorkMangerRunVideo(false);
                                galleryViewModel.hasShowGalleryVideoProgressbar(false);
                                Log.e(TAG, "WorkManager: failed");
                                galleryViewModel.notificationBuilder("File download failed", NOTIFICATION_ID_VIDEO_LIVE);
                                break;
                            case RUNNING:
                                Log.e(TAG, "WorkManager: running");
                                if (!galleryViewModel.isHasWorkMangerRunVideo()) {
                                    galleryViewModel.setHasWorkMangerRunVideo(true);
                                    Toast.makeText(galleryViewModel.appContext, R.string.download_in_progress, Toast.LENGTH_SHORT).show();
                                }
                                //   galleryViewModel.hasShowGalleryVideoProgressbar(true);
                                galleryViewModel.notificationBuilder("Downloading file...", NOTIFICATION_ID_VIDEO_LIVE);
                                break;
                        }
                    }

                });
            }
        });
    }

    private void showStorageAlert(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.STORAGE_ALERT;
            activity.showDialog("", message, null);
        }
    }
}