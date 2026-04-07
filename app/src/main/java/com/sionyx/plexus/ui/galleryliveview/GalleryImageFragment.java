package com.sionyx.plexus.ui.galleryliveview;

import static android.view.View.SCROLLBAR_POSITION_LEFT;
import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.DESTINATION_FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.IS_IMAGE;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.NOTIFICATION_ID;
import static com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker.NOTIFICATION_ID_IMG_LIVE;

import android.annotation.SuppressLint;
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

import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentGalleryImageBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.dialog.GalleryAllViewModel;
import com.sionyx.plexus.ui.galleryliveview.livetabviewadapter.GalleryLiveViewPageAdapter;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.workmanager.LiveViewFileDownloadWorker;

import java.io.File;
import java.util.ArrayList;

public class GalleryImageFragment extends Fragment {
    private static final String TAG = "GalleryImageFragment";
    private FragmentGalleryImageBinding binding;
    private GalleryAllViewModel viewModel;
    private GalleryViewModel galleryViewModel;
    private boolean hasShowCheckbox;
    private LifecycleOwner lifecycleOwner;
    private GalleryLiveViewPageAdapter managePhotoViewAdapter;
    private boolean isVisible;
    private HomeViewModel homeViewModel;
    private BleWiFiViewModel bleWiFiViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryAllViewModel.class);
        galleryViewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryImageBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        binding.setViewModel(viewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        galleryViewModel.isManageSelected.observe(lifecycleOwner, isManageAllSelected);

        galleryViewModel.closePopupImage.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                Log.e(TAG, "closePopupImage: ");
                managePhotoViewAdapter = null;
                binding.galleryImageRecylerView.setAdapter(null);
                galleryViewModel.isManageSelected.removeObserver(isManageAllSelected);
                galleryViewModel.isManageSelected.removeObservers(lifecycleOwner);
            }
        }));
    }

    private final Observer<Boolean> isManageAllSelected = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            hasShowCheckbox = aBoolean;
            subscribeUI();
        }
    };

    private void subscribeUI() {
        new Handler().post(this::getGalleryImages);
    }

    private final Handler mHandler = new Handler();

    @SuppressLint("NotifyDataSetChanged")
    private void getGalleryImages() {

        binding.setViewModel(viewModel);

        if (viewModel.getArrayListAll() != null) {
            for (int i = 0; i < viewModel.getArrayListAll().size(); i++) {
                GalleryManageModel manageModel = viewModel.getArrayListAll().get(i);
                if (manageModel.getFilePath().endsWith(".jpg")) {
                    if (!viewModel.getArrayListPhoto().contains(manageModel)) {
                        viewModel.addPics(manageModel);
                    }
                }
            }
        }


        /*
        Initialy check the list of size, if it is 0(else) the Records not found tv will be visible on center of screen
         */
        if (!viewModel.getArrayListPhoto().isEmpty()) {
            viewModel.setNoRecordsFoundImage(false);
            if (managePhotoViewAdapter == null) {
                managePhotoViewAdapter = new GalleryLiveViewPageAdapter(hasShowCheckbox, getActivity(), viewModel.getArrayListPhoto(), viewModel, 1);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
                binding.galleryImageRecylerView.setLayoutManager(gridLayoutManager);
                binding.galleryImageRecylerView.setAdapter(managePhotoViewAdapter);
                binding.galleryImageRecylerView.setVerticalScrollbarPosition(SCROLLBAR_POSITION_LEFT);
            } else {
                managePhotoViewAdapter.replaceWithLatest(viewModel.getArrayListPhoto(), hasShowCheckbox, 1);
            }
        } else {
            noRecordsFound();
        }

        /*for this observe select all button press*/
        viewModel.isManageSelectAllPhotos.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean && isVisible) {
                if (managePhotoViewAdapter != null && !viewModel.getArrayListPhoto().isEmpty()) {
                    managePhotoViewAdapter.selectAllCheckbox(1, true, mHandler);
                    viewModel.onManageSelectAllPhotos(false);
                    viewModel.isErasedPhoto = false;
                    viewModel.hasEraseLivePhotoMedia(false);
                    isSelectAll();
                }
            }
        }));

        /*for this observe erase button press*/
        viewModel.isManagePhotoSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (managePhotoViewAdapter != null && !viewModel.getArrayListPhoto().isEmpty() && isVisible) {
                    boolean isChecked = false;
                    for (GalleryManageModel i : viewModel.getArrayListPhoto()) {
                        if (i.getFilePath().endsWith(".jpg")) {
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

        viewModel.isMedialFileDeleted.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                viewModel.isErasedVideo = true;
                viewModel.getArrayListAll().clear();
                viewModel.getArrayListVideo().clear();
                viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                getGalleryImages();
                managePhotoViewAdapter.notifyDataSetChanged();
            }
        }));


        viewModel.isEraseLivePhotoMedia.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (managePhotoViewAdapter != null && !viewModel.getArrayListPhoto().isEmpty() && isVisible) {
                    managePhotoViewAdapter.erased();
                    viewModel.isErasedPhoto = true;
                    viewModel.getArrayListPhoto().clear();
                    viewModel.getAllFiles(getActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    getGalleryImages();
                    managePhotoViewAdapter.notifyDataSetChanged();
                    viewModel.onManagePhotoSelectErase(false);
                    viewModel.hasEraseLivePhotoMedia(false);
                }
            }
        }));

        /*for this observe download button press*/
        viewModel.isManageSelectAllPhotoDownload.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (viewModel.getArrayListPhoto() != null && !viewModel.getArrayListPhoto().isEmpty()) {
                    downloadGallery();
                }
            }
        }));

        /* for this after selected check box cleared while tab swipe and after file download button press cleared*/
        viewModel.isClearSelectedCheckboxPhotoView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (managePhotoViewAdapter != null && !viewModel.getArrayListPhoto().isEmpty()) {
                    managePhotoViewAdapter.selectAllCheckbox(1, false, mHandler);
                }
            }
        }));

        /* for this only after delete gallery item to refresh adapter*/
        viewModel.isUpdateGalleryPhotosItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (!viewModel.getArrayListPhoto().isEmpty()) {
                    viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                    viewModel.updateGalleryPhotosItemView(false);
                }
            }
        }));

        galleryViewModel.hasShowGalleryImgProgressbar.observe(lifecycleOwner, aBoolean -> {
            Log.e(TAG, "hasShowGalleryImgProgressbar: " + aBoolean);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (aBoolean)
                    binding.dotProgressBar.setVisibility(View.VISIBLE);
                else
                    binding.dotProgressBar.setVisibility(View.GONE);
            });
        });

    }

    /*
    Visibility handle function for Records Not found on selected tab
     */
    private void noRecordsFound() {
        /*viewModel.setBackToGallery(true);*/
        viewModel.setNoRecordsFoundImage(true);
        binding.galleryImageRecylerView.setVisibility(View.GONE);
        binding.tvNoRecordFoundPhotosViews.setVisibility(View.VISIBLE);
        binding.tvNoRecordFoundPhotosViews.setText(getResources().getString(R.string.record_not_found));
    }


    private void isSelectAll() {
        ArrayList<GalleryManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < viewModel.getArrayListPhoto().size(); i++) {
            if (viewModel.getArrayListPhoto().get(i).isChecked()) {
                tempList.add(viewModel.getArrayListPhoto().get(i));
            }
        }
        homeViewModel.checkedItemCount = tempList.size();
        viewModel.setSelectAllSelected(viewModel.getArrayListPhoto().size() == tempList.size());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel.getArrayListPhoto() != null) {
            Log.i(TAG, "setMenuVisibility onResume: Image");
            isSelectAll();
        }
        if (viewModel.getArrayListPhoto().isEmpty()) {
            viewModel.setNoRecordsFoundImage(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        /*this is to notify adapter from background to foreground state*/
        if (managePhotoViewAdapter != null) {
            Log.i(TAG, "setMenuVisibility onPause: IMAGE");
            viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
            getGalleryImages();
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
            if (managePhotoViewAdapter != null && viewModel != null) {
                if (viewModel.getArrayListPhoto() != null) {
                    if (viewModel.isErasedPhoto) {
                        // viewModel.getArrayListAll().clear();
                        viewModel.getArrayListPhoto().clear();
                        viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
                        viewModel.isErasedPhoto = false;
                    }
//                    else {
                    viewModel.getArrayListPhoto().clear();
                    viewModel.getAllFiles(requireActivity(), bleWiFiViewModel.getConnectedSsidFromWiFiManager());
//                    }
                    getGalleryImages();
                }
                managePhotoViewAdapter.notifyDataSetChanged();

            }

        } else {
            isVisible = false;
            if (galleryViewModel != null) {
                galleryViewModel.isManageSelected.removeObserver(isManageAllSelected);
                galleryViewModel.isManageSelected.removeObservers(lifecycleOwner);
                galleryViewModel.hasShowGalleryImgProgressbar(false);
            }
        }
    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
        }
    }

    private void downloadGallery() {
        viewModel.isPhotoDownload = false;
        long totalFileSize = 0;
        for (int i = 0; i < viewModel.getArrayListPhoto().size(); i++) {
            if (viewModel.getArrayListPhoto().get(i).isChecked()) {
                viewModel.isPhotoDownload = true;
                File file = new File(viewModel.getArrayListPhoto().get(i).getFilePath());
                totalFileSize += file.length();
            }
        }
        long availableInternalStorageSize = getAvailableInternalStorageSize();
        long minimumStorageForDownload = 1073741824L;
        Log.d("Internal Storage", "Available Internal Storage: " + availableInternalStorageSize + " TotalFileSize" + totalFileSize);
        if (totalFileSize < availableInternalStorageSize && minimumStorageForDownload < availableInternalStorageSize) {
            for (int i = 0; i < viewModel.getArrayListPhoto().size(); i++) {
                if (viewModel.getArrayListPhoto().get(i).isChecked()) {
                    if (viewModel.getArrayListPhoto().get(i).getFilePath().contains(".jpg")) {
                        galleryViewModel.incrementImageCheckedFileCount();
                        viewModel.isPhotoDownload = true;
                        fileDownloadToGallery(viewModel.getArrayListPhoto().get(i).getFilePath(), "SiOnyx", true);
                    }
                }
            }
        } else {
            if (viewModel.isPhotoDownload)
                showStorageAlert(getString(R.string.no_memory_to_download));
        }

        if (!viewModel.isPhotoDownload) {
            Toast.makeText(requireActivity(), R.string.download_unsuccessful, Toast.LENGTH_SHORT).show();
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
                        .putInt(NOTIFICATION_ID, 5)
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
                                galleryViewModel.incrementImageFileCount();
                                if (galleryViewModel.getImageFileCount() == galleryViewModel.getTotalCheckedImageFileCount()) {
                                    galleryViewModel.hasShowGalleryImgProgressbar(false);
                                    galleryViewModel.setHasWorkMangerRunImage(false);
                                    galleryViewModel.totalImageFileCompleteCount = 0;
                                    galleryViewModel.totalImageFileCheckedCount = 0;
                                    galleryViewModel.notificationBuilder("File download complete", NOTIFICATION_ID_IMG_LIVE);
                                    try {
                                        Toast.makeText(galleryViewModel.appContext, R.string.download_successful, Toast.LENGTH_SHORT).show();
                                    } catch (Resources.NotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                                Log.e(TAG, "WorkManager: success" + galleryViewModel.getTotalCheckedImageFileCount() + " //" + galleryViewModel.getImageFileCount());
                                break;
                            case FAILED:
                                galleryViewModel.incrementImageFileCount();
                                galleryViewModel.setHasWorkMangerRunImage(false);
                                galleryViewModel.hasShowGalleryImgProgressbar(false);
                                Log.e(TAG, "WorkManager: failed");
                                galleryViewModel.notificationBuilder("File download failed", NOTIFICATION_ID_IMG_LIVE);
                                break;
                            case RUNNING:
                                Log.e(TAG, "WorkManager: running");
                                if (!galleryViewModel.isHasWorkMangerRunImage()) {
                                    //  galleryViewModel.setHasWorkMangerRunImage(true);
                                    Toast.makeText(galleryViewModel.appContext, R.string.download_in_progress, Toast.LENGTH_SHORT).show();
                                }
                                // galleryViewModel.hasShowGalleryImgProgressbar(true);
                                galleryViewModel.notificationBuilder("Downloading file...", NOTIFICATION_ID_IMG_LIVE);
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