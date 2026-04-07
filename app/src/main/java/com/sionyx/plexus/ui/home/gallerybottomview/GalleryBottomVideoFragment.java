package com.sionyx.plexus.ui.home.gallerybottomview;


import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.DESTINATION_FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.IS_IMAGE;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_ID;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_ID_VIDEO;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.work.WorkManager;

import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentGalleryBottomVideoViewBinding;
import com.sionyx.plexus.databinding.FragmentHomeBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.home.gallerybottomview.bottomtabviewadapter.GalleryBottomViewPageAdapter;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker;

import java.io.File;
import java.util.ArrayList;

public class GalleryBottomVideoFragment extends Fragment {

    private static final String TAG = "GalleryBottomVideoViewFragment";
    private FragmentGalleryBottomVideoViewBinding binding;
    private FragmentHomeBinding homeBinding;
    private HomeViewModel homeViewModel;
    private LifecycleOwner lifecycleOwner;
    private boolean hasShowCheckbox;
    private GalleryBottomViewPageAdapter videoAdapter;
    private final boolean isVisible = true;
    private final Handler mHandler = new Handler();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentGalleryBottomVideoViewBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        if (homeBinding != null)
            homeBinding.setViewModel(homeViewModel);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel.isManageSelect.observe(lifecycleOwner, isManageSelected);

        homeViewModel.closePopupVideo.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                videoAdapter = null;
                binding.galleryBottomVideoRecylerView.setAdapter(null);
                homeViewModel.isManageSelect.removeObserver(isManageSelected);
                homeViewModel.isManageSelect.removeObservers(lifecycleOwner);
                homeViewModel.setClosePopupVideo(false);
            }
        }));
    }

    private final Observer<Boolean> isManageSelected = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            hasShowCheckbox = aBoolean;
            homeViewModel.getAllFiles(getActivity());
            getGalleryVideo();
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    private void getGalleryVideo() {
        if (homeViewModel.getArrayListAll() != null) {
            for (int i = 0; i < homeViewModel.getArrayListAll().size(); i++) {
                GalleryBottomManageModel manageModel = homeViewModel.getArrayListAll().get(i);
                if (manageModel.getFilePath().endsWith(".mp4")) {
                    if (!homeViewModel.getArrayListVideo().contains(manageModel)) {
                        homeViewModel.addVideo(manageModel);
                    }
                }
            }
        }
        if (!homeViewModel.getArrayListVideo().isEmpty()) {
            homeViewModel.setNoRecordsFoundVideo(false);
            if (videoAdapter == null) {
                videoAdapter = new GalleryBottomViewPageAdapter(hasShowCheckbox, requireContext(), homeViewModel.getArrayListVideo(), homeViewModel);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(), requireActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 1 : 3);
                binding.galleryBottomVideoRecylerView.setLayoutManager(gridLayoutManager);
                binding.galleryBottomVideoRecylerView.setAdapter(videoAdapter);
            } else {
                videoAdapter.updatedArrayList(hasShowCheckbox, homeViewModel.getArrayListVideo());
            }
        } else {
            noRecords();
        }

        homeViewModel.isManageSelectAllVideos.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect && isVisible) {
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty()) {
                    videoAdapter.selectAllCheckbox(2, true, mHandler);
                    homeViewModel.isErasedVideo = false;
                    homeViewModel.onManageSelectAllVideo(false);
                    isSelectAllItems();
                }
            }
        }));


        homeViewModel.isMediaVideoDeleted.observe(lifecycleOwner, aBoolean -> {
            if (aBoolean) {
                Log.i(TAG, "getGalleryVideo: SIZE " + homeViewModel.getArrayListVideo().size());
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty()) {
                    homeViewModel.isErasedVideo = true;
                    homeViewModel.getArrayListAll().clear();
                    homeViewModel.getArrayListVideo().clear();
                    homeViewModel.getAllFiles(getActivity());
                    getGalleryVideo();
                    videoAdapter.notifyDataSetChanged();
//                                homeViewModel.setIsMediaVideoDeleted(false);
                } else {
                    if (homeViewModel.getArrayListVideo().isEmpty()) {
                        noRecords();
                    }
                }
            }
        });

        homeViewModel.isManageVideoSelectErase.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty() && isVisible) {
                    boolean isChecked = false;
                    for (GalleryBottomManageModel i : homeViewModel.getArrayListVideo()) {
                        if (i.getFilePath().endsWith(".mp4")) {
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

        homeViewModel.isEraseVideo.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty() && isVisible) {
                    int sizeOfArr;
                    sizeOfArr = videoAdapter.erased();
                    homeViewModel.isErasedVideo = true;
                    if (sizeOfArr == 0) {
                        noRecords();
                    } else {
                        homeViewModel.setNoRecordsFoundVideo(false);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        homeViewModel.getArrayListAll().clear();
                        homeViewModel.getArrayListVideo().clear();
                        homeViewModel.getAllFiles(getActivity());
                        getGalleryVideo();
                        videoAdapter.notifyDataSetChanged();
                        homeViewModel.hasEraseVideo(false);
                        homeViewModel.onManageVideoSelectErase(false);
                    }, 250);

                }
            }
        }));

        homeViewModel.isManageSelectAllVideoDownload.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (homeViewModel.getArrayListVideo() != null && !homeViewModel.getArrayListVideo().isEmpty()) {
                    downloadGallery();
                }
            }
        }));

        homeViewModel.isManageVideoBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty()) {
                    videoAdapter.selectAllCheckbox(2, false, mHandler);
                    homeViewModel.getAllFiles(getActivity());
                    videoAdapter.notifyDataSetChanged();
                }
            }
        }));

        homeViewModel.UpdateManageLayout.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                for (GalleryBottomManageModel i : homeViewModel.getArrayListVideo()) {
                    i.setChecked(false);
                }
                videoAdapter.notifyDataSetChanged();
            }
        }));

        homeViewModel.isClearSelectedCheckboxVideoView.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty()) {
                    videoAdapter.selectAllCheckbox(2, false, mHandler);
                    homeViewModel.getAllFiles(getActivity());
                }
            }
        }));


        homeViewModel.ManageUpdateVideoItem.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (videoAdapter != null && !homeViewModel.getArrayListVideo().isEmpty()) {
                    homeViewModel.getAllFiles(getActivity());
                }
            }
        }));

        homeViewModel.isUpdateGalleryBottomVideosItemView.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (!homeViewModel.getArrayListVideo().isEmpty()) {
                    homeViewModel.getAllFiles(getActivity());
                    homeViewModel.updateGalleryBottomVideosItemView(false);
                }
            }
        }));

        homeViewModel.hasShowGalleryVideoProgressbar.observe(lifecycleOwner, aBoolean -> {
            Log.e(TAG, "hasShowGalleryVideoProgressbar: " + aBoolean);
            if (aBoolean)
                binding.dotProgressBar.setVisibility(View.VISIBLE);
            else
                binding.dotProgressBar.setVisibility(View.GONE);
        });
    }

    //Check whether the list of arr size 0 or not, iF it is 0 the Records not found msg will be shown
    private void noRecords() {
        homeViewModel.setNoRecordsFoundVideo(true);
        binding.galleryBottomVideoRecylerView.setVisibility(View.GONE);
        binding.tvRecordsNotFoundBottomVideoView.setVisibility(View.VISIBLE);
    }

    private void downloadGallery() {
        homeViewModel.isVideoDownload = false;
        long totalFileSize = 0;
        for (int i = 0; i < homeViewModel.getArrayListVideo().size(); i++) {
            if (homeViewModel.getArrayListVideo().get(i).isChecked()) {
                homeViewModel.isVideoDownload = true;
                File file = new File(homeViewModel.getArrayListVideo().get(i).getFilePath());
                totalFileSize += file.length();
            }
        }

        long availableInternalStorageSize = getAvailableInternalStorageSize();
        long minimumStorageForDownload = 1073741824L;
        Log.d("Internal Storage", "Available Internal Storage: " + availableInternalStorageSize + " TotalFileSize" + totalFileSize);
        if (totalFileSize < availableInternalStorageSize && minimumStorageForDownload < availableInternalStorageSize) {
            for (int i = 0; i < homeViewModel.getArrayListVideo().size(); i++) {
                if (homeViewModel.getArrayListVideo().get(i).isChecked()) {
                    if (homeViewModel.getArrayListVideo().get(i).getFilePath().contains(".mp4")) {
                        homeViewModel.incrementVideoCheckedFileCount();
                        homeViewModel.isVideoDownload = true;
                        if (homeViewModel.getArrayListVideo().get(i).getFilePath() != null)
                            fileDownloadToGallery(homeViewModel.getArrayListVideo().get(i).getFilePath(), "SiOnyx", false);
                    }
                }
            }
        } else {
            if (homeViewModel.isVideoDownload)
                showStorageAlert(getString(R.string.no_memory_to_download));
        }

        if (!homeViewModel.isVideoDownload) {
            Toast.makeText(requireActivity(), R.string.download_unsuccessful, Toast.LENGTH_SHORT).show();
        }
        homeViewModel.clearSelectedCheckBox();
    }

    public void fileDownloadToGallery(String filePath, String destinationPath, boolean isImage) {
        Data inputData = new Data.Builder()
                .putString(FILE_PATH, filePath)
                .putString(DESTINATION_FILE_PATH, destinationPath)
                .putBoolean(IS_IMAGE, isImage)
                .putInt(NOTIFICATION_ID, 3)
                .build();
        WorkManager workManager = WorkManager.getInstance(requireActivity());
        OneTimeWorkRequest downloadRequest = new OneTimeWorkRequest.Builder(HomeFileDownloadWorker.class)
                .setInputData(inputData)
                .build();

        Log.e(TAG, "fileDownloadToGallery: " + downloadRequest.getId());
        workManager.enqueue(downloadRequest);

        workManager.getWorkInfoByIdLiveData(downloadRequest.getId()).observeForever(workInfo -> {
            if (workInfo != null) {
                switch (workInfo.getState()) {
                    case SUCCEEDED:
                        Data outputData = workInfo.getOutputData();
                        int result = outputData.getInt("resultKey", 0);
                        homeViewModel.incrementVideoFileCount();
                        if (homeViewModel.getTotalVideoFileCompleteCount() == homeViewModel.getTotalCheckedVideoFileCount()) {
                            homeViewModel.setHasWorkMangerRunVideo(false);
                            homeViewModel.hasShowGalleryVideoProgressbar(false);
                            homeViewModel.totalVideoFileCompleteCount = 0;
                            homeViewModel.totalVideoFileCheckedCount = 0;
                            homeViewModel.notificationBuilder("File download complete", NOTIFICATION_ID_VIDEO);
                            try {
                                Toast.makeText(homeViewModel.appContext, R.string.download_successful, Toast.LENGTH_SHORT).show();
                            } catch (Resources.NotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, "WorkManager: success" + homeViewModel.getTotalCheckedVideoFileCount() + " //" + homeViewModel.getTotalVideoFileCompleteCount());
                        break;
                    case FAILED:
                        homeViewModel.incrementVideoFileCount();
                        homeViewModel.setHasWorkMangerRunVideo(false);
                        homeViewModel.hasShowGalleryVideoProgressbar(false);
                        Log.e(TAG, "WorkManager: failed");
                        homeViewModel.notificationBuilder("File download failed", NOTIFICATION_ID_VIDEO);
                        break;
                    case RUNNING:
                        Log.e(TAG, "WorkManager: running");
                        if (!homeViewModel.isHasWorkMangerRunVideo()) {
                            homeViewModel.setHasWorkMangerRunVideo(true);
                            Toast.makeText(requireActivity(), R.string.download_in_progress, Toast.LENGTH_SHORT).show();
                        }
                        //  homeViewModel.hasShowGalleryVideoProgressbar(true);
                        homeViewModel.notificationBuilder("Downloading file...", NOTIFICATION_ID_VIDEO);
                        break;
                }
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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        if (menuVisible) {
            if (homeViewModel != null) {
                homeViewModel.isManageSelect.observe(lifecycleOwner, isManageSelected);
            }
            if (videoAdapter != null && homeViewModel != null) {
                if (homeViewModel.getArrayListVideo() != null) {
                    if (homeViewModel.isErasedVideo) {
                        homeViewModel.getArrayListVideo().clear();
                        homeViewModel.getArrayListAll().clear();
                        homeViewModel.getAllFiles(getActivity());
                        homeViewModel.isErasedVideo = false;
                    } else {
                        homeViewModel.getArrayListVideo().clear();
                        homeViewModel.getAllFiles(getActivity());
                    }
                    getGalleryVideo();
                }

                videoAdapter.notifyDataSetChanged();
            }
        } else {
            if (homeViewModel != null) {
                homeViewModel.isManageSelect.removeObserver(isManageSelected);
                homeViewModel.isManageSelect.removeObservers(lifecycleOwner);
                homeViewModel.hasShowGalleryVideoProgressbar(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "getGalleryVideo: VIDEO FRAG ");
        if (homeViewModel.getArrayListVideo().isEmpty()) {
            homeViewModel.setNoRecordsFoundVideo(true);
        }
        isSelectAllItems();
    }

    //for handling the slect all Deselect all string status and function
    private void isSelectAllItems() {
        ArrayList<GalleryBottomManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < homeViewModel.getArrayListVideo().size(); i++) {
            if (homeViewModel.getArrayListVideo().get(i).isChecked()) {
                tempList.add(homeViewModel.getArrayListVideo().get(i));
            }
        }
        homeViewModel.checkedItemCount = tempList.size();
        homeViewModel.setGallerySelectAll(homeViewModel.getArrayListVideo().size() == tempList.size());
    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
            homeViewModel.onManageVideoSelectErase(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!homeViewModel.getArrayListVideo().isEmpty()) {
            if (videoAdapter != null) {
                homeViewModel.getAllFiles(getActivity());
                getGalleryVideo();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        videoAdapter = null;
        binding.galleryBottomVideoRecylerView.setAdapter(null);
    }
}