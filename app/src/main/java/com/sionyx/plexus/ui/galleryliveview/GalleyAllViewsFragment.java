package com.sionyx.plexus.ui.galleryliveview;

import static com.sionyx.plexus.ui.home.HomeViewModel.isRecordingStarted;
import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.DESTINATION_FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.IS_IMAGE;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.NOTIFICATION_ID;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.NOTIFICATION_ID_ALL_LIVE;

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
import com.sionyx.plexus.databinding.FragmentGalleyAllViewsBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.dialog.GalleryAllViewModel;
import com.sionyx.plexus.ui.galleryliveview.livetabviewadapter.GalleryLiveViewPageAdapter;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker;

import java.io.File;
import java.util.ArrayList;

public class GalleyAllViewsFragment extends Fragment {
    private static final String TAG = "GalleyAllViewsFragment";
    private FragmentGalleyAllViewsBinding binding;
    private GalleryAllViewModel viewModel;

    private HomeViewModel homeViewModel;
    private GalleryViewModel galleryViewModel;
    private LifecycleOwner lifecycleOwner;
    private boolean hasShowCheckbox;
    private GalleryLiveViewPageAdapter manageAllViewAdapter;
    private boolean isVisible;
    private final Handler mHandler = new Handler();
    BleWiFiViewModel bleWiFiViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView: ");
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryAllViewModel.class);
        galleryViewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);

        binding = FragmentGalleyAllViewsBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        binding.setViewModel(viewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e(TAG, "onViewCreated: ");
        galleryViewModel.isManageSelected.observe(lifecycleOwner, isManageAllSelected);


        galleryViewModel.closePopupAllMedia.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                Log.e(TAG, "closePopupAllMedia: ");
                manageAllViewAdapter = null;
//                viewModel = null;
                binding.galleryManageRecylerView.setAdapter(null);
                galleryViewModel.isManageSelected.removeObserver(isManageAllSelected);
                galleryViewModel.isManageSelected.removeObservers(lifecycleOwner);

            }
        }));

    }

    private final Observer<Boolean> isManageAllSelected = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            Log.e(TAG, "isManageAllSelected");
            hasShowCheckbox = aBoolean;
            subscribeUI();
        }
    };

    private void subscribeUI() {
        new Handler().post(this::getGalleryVideosAndImages);
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void getGalleryVideosAndImages() {
        if (isAdded() && viewModel != null) {
            viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());

            viewModel.filterImageList.clear();
            for (GalleryManageModel i : viewModel.getArrayListAll()) {
                if (i.getFilePath().endsWith(".jpg")) {
//                if (!homeViewModel.filterImageList.contains())
                    viewModel.filterImageList.add(i);
                }
            }

            binding.setViewModel(viewModel);
            if (!viewModel.getArrayListAll().isEmpty()) {
                viewModel.setNoRecordsFoundAll(false);
                if (manageAllViewAdapter == null) {
                    manageAllViewAdapter = new GalleryLiveViewPageAdapter(hasShowCheckbox, requireActivity(), viewModel.getArrayListAll(), viewModel, 0);
                    GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(), 3);
                    binding.galleryManageRecylerView.setLayoutManager(gridLayoutManager);
                    binding.galleryManageRecylerView.setAdapter(manageAllViewAdapter);
                } else {
                    manageAllViewAdapter.replaceWithLatest(viewModel.getArrayListAll(), hasShowCheckbox, 0);
                }
                viewModel.setCurrentTabScreen(0);
            } else {
                noRecordsFound();
            }


            viewModel.isMedialFileDeleted.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    viewModel.isErasedAll = true;
                    viewModel.getArrayListAll().clear();
                    getGalleryVideosAndImages();
                    manageAllViewAdapter.notifyDataSetChanged();
                }
            }));
            /*for this observe select all button press*/
            viewModel.isManageSelectAllView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean && isVisible) {
                    if (manageAllViewAdapter != null && !viewModel.getArrayListAll().isEmpty()) {
                        manageAllViewAdapter.selectAllCheckbox(0, true, mHandler);
                        viewModel.onManageSelectAllView(false);
                        viewModel.isErasedAll = false;
                        viewModel.hasEraseLiveMedia(false);
                        isSelectAll();
                    }
                }
            }));

            /*for this observe erase button press*/
            viewModel.isManageAllSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    if (manageAllViewAdapter != null && !viewModel.getArrayListAll().isEmpty() && isVisible) {
                        boolean isChecked = false;
                        for (GalleryManageModel i : viewModel.getArrayListAll()) {
                            if ((i.getFilePath().endsWith(".mp4")) || (i.getFilePath().endsWith(".jpg"))) {
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

            viewModel.isEraseLiveMedia.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    if (manageAllViewAdapter != null && !viewModel.getArrayListAll().isEmpty() && isVisible) {
                        manageAllViewAdapter.erased();
                        viewModel.isErasedAll = true;
                        viewModel.onManageAllSelectErase(false);
                        viewModel.hasEraseLivePhotoMedia(false);
                        viewModel.hasEraseLiveVideoMedia(false);
                        manageAllViewAdapter.notifyDataSetChanged();
                    }
                }
            }));

            /*for this observe download button press*/
            viewModel.isManageSelectAllDownload.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    if (viewModel.getArrayListAll() != null && !viewModel.getArrayListAll().isEmpty()) {
                        downloadGallery();
                    }
                }
            }));

            /* for this after selected check box cleared while tab swipe and after file download button press cleared*/
            viewModel.isClearSelectedCheckboxAllView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean) {
                    if (manageAllViewAdapter != null && !viewModel.getArrayListAll().isEmpty()) {
                        manageAllViewAdapter.selectAllCheckbox(0, false, mHandler);
                    }
                }
            }));


            /* for this only after delete recording info gallery item to refresh adapter*/
            viewModel.isUpdateGalleryAllItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
                if (aBoolean && viewModel.getArrayListAll() != null) {
                    viewModel.getAllFiles(getActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    viewModel.updateGalleryAllItemView(false);
                }
            }));

            galleryViewModel.hasShowGalleryAllProgressbar.observe(lifecycleOwner, aBoolean -> requireActivity().runOnUiThread(() -> {
                if (aBoolean)
                    binding.dotProgressBar.setVisibility(View.VISIBLE);
                else
                    binding.dotProgressBar.setVisibility(View.GONE);
            }));
        } else {
            Log.e(TAG, "getGalleryVideosAndImages: NOT attached");
        }
    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (viewModel.getArrayListAll().isEmpty()) {
                viewModel.setNoRecordsFoundAll(true);
            }
            if (viewModel.getArrayListAll() != null) {
                isSelectAll();
            }
        } catch (Exception e) {
            Log.d(TAG, "onResume: "+ e.getMessage());
        }
    }

    private void isSelectAll() {
        ArrayList<GalleryManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < viewModel.getArrayListAll().size(); i++) {
            if (viewModel.getArrayListAll().get(i).isChecked()) {
                tempList.add(viewModel.getArrayListAll().get(i));
            }
        }
        Log.i(TAG, "onResume: all " + viewModel.getArrayListAll().size() + "selected " + tempList.size());
        homeViewModel.checkedItemCount = tempList.size();
        if (isRecordingStarted) {
            Log.i(TAG, "onResume: all REC " + viewModel.getArrayListAll().size() + "selected " + tempList.size());
            viewModel.setSelectAllSelected(viewModel.getArrayListAll().size() == (tempList.size() + 1));//23 == 22
        } else {
            viewModel.setSelectAllSelected(viewModel.getArrayListAll().size() == tempList.size());
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onPause() {
        super.onPause();
        /*this is to notify adapter from background to foreground state*/
        if (manageAllViewAdapter != null) {
            getGalleryVideosAndImages();
            manageAllViewAdapter.notifyDataSetChanged();
        }
    }

    /*
    Records not found function has been created for handling the visibility of views
     */
    private void noRecordsFound() {
        /*viewModel.setBackToGallery(true);*/
        binding.tvNoRecordFoundAllViews.setVisibility(View.VISIBLE);
        binding.galleryManageRecylerView.setVisibility(View.GONE);
        viewModel.setNoRecordsFoundAll(true);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            isVisible = true;
            if (galleryViewModel != null)
                galleryViewModel.isManageSelected.observe(lifecycleOwner, isManageAllSelected);
            if (manageAllViewAdapter != null && viewModel != null) {
                if (!viewModel.getArrayListAll().isEmpty()) {
                    Log.e(TAG, "setMenuVisibility");
                    if (viewModel.isErasedAll) {
                        viewModel.getArrayListAll().clear();
                        viewModel.isErasedAll = false;
                    }
                    getGalleryVideosAndImages();
                }
                manageAllViewAdapter.notifyDataSetChanged();
            }
        } else {
            isVisible = false;
            if (galleryViewModel != null) {
                galleryViewModel.isManageSelected.removeObserver(isManageAllSelected);
                galleryViewModel.isManageSelected.removeObservers(lifecycleOwner);
                galleryViewModel.hasShowGalleryAllProgressbar(false);
            }
        }
    }

    private void downloadGallery() {
        viewModel.isDownloaded = false;
        long totalFileSize = 0;
        for (int i = 0; i < viewModel.getArrayListAll().size(); i++) {
            if (viewModel.getArrayListAll().get(i).isChecked()) {
                viewModel.isDownloaded = true;
                File file = new File(viewModel.getArrayListAll().get(i).getFilePath());
                totalFileSize += file.length();
            }
        }
        long availableInternalStorageSize = getAvailableInternalStorageSize();
        Log.d("Internal Storage", "Available Internal Storage: " + availableInternalStorageSize + " TotalFileSize" + totalFileSize);
        long minimumStorageForDownload = 1073741824L; // 1GB
        if (totalFileSize < availableInternalStorageSize && minimumStorageForDownload < availableInternalStorageSize) {
            for (int i = 0; i < viewModel.getArrayListAll().size(); i++) {
                if (viewModel.getArrayListAll().get(i).isChecked()) {
                    if (viewModel.getArrayListAll().get(i).getFilePath().contains(".mp4")) {
                        viewModel.isDownloaded = true;
                        galleryViewModel.incrementAllCheckedFileCount(); // for this dynamically file select count increase reason for user choose some time not select all file
                        fileDownloadToGallery(viewModel.getArrayListAll().get(i).getFilePath(), "SiOnyx", false);
                    } else if (viewModel.getArrayListAll().get(i).getFilePath().contains(".jpg")) {
                        galleryViewModel.incrementAllCheckedFileCount(); // for this dynamically file select count increase reason for user choose some time not select all file
                        viewModel.isDownloaded = true;
                        fileDownloadToGallery(viewModel.getArrayListAll().get(i).getFilePath(), "SiOnyx", true);
                    }
                }
            }
        } else {
            if (viewModel.isDownloaded)
                showStorageAlert(getString(R.string.no_memory_to_download));
        }

        if (!viewModel.isDownloaded) {
            Toast.makeText(getActivity(), R.string.download_unsuccessful, Toast.LENGTH_SHORT).show();
        }
        viewModel.clearSelectedCheckBox();
    }

    public void fileDownloadToGallery(String filePath, String destinationPath, boolean isImage) {
        new Handler().post(() -> {
            if (filePath != null) {
                Data inputData = new Data.Builder()
                        .putString(FILE_PATH, filePath)
                        .putString(DESTINATION_FILE_PATH, destinationPath)
                        .putBoolean(IS_IMAGE, isImage)
                        .putInt(NOTIFICATION_ID, 4)
                        .build();
                OneTimeWorkRequest downloadRequest = new OneTimeWorkRequest.Builder(LiveViewFileDownloadWorker.class)
                        .setInputData(inputData)
                        .build();
                Log.e(TAG, "fileDownloadToGallery: " + downloadRequest.getId());
                galleryViewModel.workManager.enqueue(downloadRequest);

                galleryViewModel.workManager.getWorkInfoByIdLiveData(downloadRequest.getId()).observeForever(workInfo -> {
                    if (workInfo != null) {
                        switch (workInfo.getState()) {
                            case SUCCEEDED:
                                Data outputData = workInfo.getOutputData();
                                int result = outputData.getInt("resultKey", 0);
                                galleryViewModel.incrementAllFileCount();
                                if (galleryViewModel.getTotalAllFileCompleteCount() == galleryViewModel.getTotalCheckedAllFileCount()) {
                                    galleryViewModel.hasShowGalleryAllProgressbar(false);
                                    galleryViewModel.setHasWorkMangerRunAll(false);
                                    galleryViewModel.totalAllFileCompleteCount = 0;
                                    galleryViewModel.totalAllFileCheckedCount = 0;
                                    galleryViewModel.notificationBuilder("File download complete", NOTIFICATION_ID_ALL_LIVE);
                                    try {
                                        Toast.makeText(galleryViewModel.appContext, R.string.download_successful, Toast.LENGTH_SHORT).show();
                                    } catch (Resources.NotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.e(TAG, "WorkManager: success" + galleryViewModel.getTotalCheckedAllFileCount() + " //" + galleryViewModel.getTotalAllFileCompleteCount());
                                break;
                            case FAILED:
                                Log.e(TAG, "WorkManager: failed");
                                galleryViewModel.incrementAllFileCount();
                                galleryViewModel.hasShowGalleryAllProgressbar(false);
                                galleryViewModel.setHasWorkMangerRunAll(false);
                                galleryViewModel.notificationBuilder("File download failed", NOTIFICATION_ID_ALL_LIVE);
                                break;
                            case RUNNING:
                                Log.e(TAG, "WorkManager: running");
                                if (!galleryViewModel.isHasWorkMangerRunAll()) {
                                    galleryViewModel.setHasWorkMangerRunAll(true);
                                    Toast.makeText(galleryViewModel.appContext, R.string.download_in_progress, Toast.LENGTH_SHORT).show();
                                }
                                //  galleryViewModel.hasShowGalleryAllProgressbar(true);
                                galleryViewModel.notificationBuilder("Downloading file...", NOTIFICATION_ID_ALL_LIVE);
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