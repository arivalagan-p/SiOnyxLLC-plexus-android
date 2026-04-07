package com.sionyx.plexus.ui.home.gallerybottomview;


import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.DESTINATION_FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.IS_IMAGE;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_ID;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_ID_IMG;

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
import com.sionyx.plexus.databinding.FragmentGalleryBottomPhotoViewBinding;
import com.sionyx.plexus.databinding.FragmentHomeBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.home.gallerybottomview.bottomtabviewadapter.GalleryBottomViewPageAdapter;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker;

import java.io.File;
import java.util.ArrayList;

public class GalleryBottomImageFragment extends Fragment {

    private static final String TAG = "GalleryBottomPhotoViewFragment";
    private FragmentGalleryBottomPhotoViewBinding binding;
    private FragmentHomeBinding homeBinding;
    private HomeViewModel homeViewModel;
    private boolean hasShowCheckbox = false;
    private LifecycleOwner lifecycleOwner;
    private GalleryBottomViewPageAdapter imageAdapter;
    private final boolean isVisible = true;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBottomPhotoViewBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        if (homeBinding != null)
            homeBinding.setViewModel(homeViewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel.isManageSelect.observe(lifecycleOwner, isManageSelected);
        homeViewModel.closePopupImage.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                imageAdapter = null;
                binding.galleryBottomPhotoRecylerView.setAdapter(null);
                homeViewModel.isManageSelect.removeObserver(isManageSelected);
                homeViewModel.isManageSelect.removeObservers(lifecycleOwner);
                homeViewModel.setClosePopupImage(false);
            }
        }));
    }

    private final Observer<Boolean> isManageSelected = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            hasShowCheckbox = aBoolean;
            homeViewModel.getAllFiles(getActivity());
            getGalleryImages();
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    private void getGalleryImages() {
        if (homeViewModel.getArrayListAll() != null) {
            for (int i = 0; i < homeViewModel.getArrayListAll().size(); i++) {
                GalleryBottomManageModel manageModel = homeViewModel.getArrayListAll().get(i);
                if (manageModel.getFilePath().endsWith(".jpg")) {
                    if (!homeViewModel.getArrayListPhoto().contains(manageModel)) {
                        homeViewModel.addModelPhoto(manageModel);
                    }
                }
            }
        }
        if (!homeViewModel.getArrayListPhoto().isEmpty()) {
            homeViewModel.setNoRecordsFoundImage(false);
            if (imageAdapter == null) {
                imageAdapter = new GalleryBottomViewPageAdapter(hasShowCheckbox, requireContext(), homeViewModel.getArrayListPhoto(), homeViewModel);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(), requireActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 1 : 3);
                binding.galleryBottomPhotoRecylerView.setLayoutManager(gridLayoutManager);
                binding.galleryBottomPhotoRecylerView.setAdapter(imageAdapter);
            } else {
                imageAdapter.updatedArrayList(hasShowCheckbox, homeViewModel.getArrayListPhoto());
            }
        } else {
            noRecords();
        }

        homeViewModel.isManageSelectAllPhotos.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean && isVisible) {
                if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty()) {
                    imageAdapter.selectAllCheckbox(1, true, mHandler);
                    homeViewModel.isErasedPic = false;
                    homeViewModel.onManageSelectAllPhotos(false);
                    isSelectedAllItem();
                }
            }
        }));

        homeViewModel.isMediaVideoDeleted.observe(lifecycleOwner, aBoolean -> {
            Log.i(TAG, "onChanged: IMG DELEDT");
            if (aBoolean) {
                if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty()) {
                    homeViewModel.isErasedAll = true;
                    homeViewModel.getArrayListAll().clear();
                    getGalleryImages();
                    imageAdapter.notifyDataSetChanged();
//                                homeViewModel.setIsMediaVideoDeleted(false);
                } else {
                    if (homeViewModel.getArrayListPhoto().isEmpty()) {
                        noRecords();
                    }
                }
            }
        });

        homeViewModel.isManagePhotoSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty() && isVisible) {
                    boolean isChecked = false;
                    for (GalleryBottomManageModel i : homeViewModel.getArrayListPhoto()) {
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

        homeViewModel.isErasePhoto.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty() && isVisible) {
                    int sizeOfArr;
                    sizeOfArr = imageAdapter.erased();
                    homeViewModel.isErasedPic = true;
                    if (sizeOfArr == 0) {
                        noRecords();
                    } else {
                        homeViewModel.setNoRecordsFoundImage(false);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        homeViewModel.getArrayListAll().clear();
                        homeViewModel.getArrayListPhoto().clear();
                        homeViewModel.getAllFiles(getActivity());
                        getGalleryImages();
                        imageAdapter.notifyDataSetChanged();
                        homeViewModel.hasErasePhoto(false);
                        homeViewModel.onManagePhotoSelectErase(false);
                    }, 250);

                }
            }
        }));

        homeViewModel.isManageSelectAllPhotoDownload.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (homeViewModel.getArrayListPhoto() != null && !homeViewModel.getArrayListPhoto().isEmpty()) {
                    downloadGallery();
                    homeViewModel.getAllFiles(getActivity());
                }
            }
        }));

        homeViewModel.isManagePhotoBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {

                if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty()) {
                    imageAdapter.selectAllCheckbox(1, false, mHandler);
                    homeViewModel.getAllFiles(getActivity());
                    getGalleryImages();
                    imageAdapter.notifyDataSetChanged();
                }
            }
        }));
        homeViewModel.UpdateManageLayout.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                for (GalleryBottomManageModel i : homeViewModel.getArrayListPhoto()) {
                    i.setChecked(false);
                }
                imageAdapter.notifyDataSetChanged();
            }
        }));

        homeViewModel.isClearSelectedCheckboxPhotoView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty()) {
                    imageAdapter.selectAllCheckbox(1, false, mHandler);
                    homeViewModel.getAllFiles(getActivity());
                }
            }
        }));


        homeViewModel.ManageUpdatePhotoItem.observe(lifecycleOwner, booleanEvent -> {
            if (imageAdapter != null && !homeViewModel.getArrayListPhoto().isEmpty()) {
                homeViewModel.getAllFiles(getActivity());
            }
        });

        homeViewModel.isUpdateGalleryBottomPhotoItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (!homeViewModel.getArrayListPhoto().isEmpty()) {
                    homeViewModel.getAllFiles(getActivity());
                    homeViewModel.updateGalleryBottomPhotosItemView(false);
                }
            }
        }));

        homeViewModel.hasShowGalleryImgProgressbar.observe(lifecycleOwner, aBoolean -> {
            Log.e(TAG, "hasShowGalleryImgProgressbar: " + aBoolean);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (aBoolean)
                    binding.dotProgressBar.setVisibility(View.VISIBLE);
                else
                    binding.dotProgressBar.setVisibility(View.GONE);
            });
        });
    }

    //Check whether the list of arr size 0 or not, iF it is 0 the Records not found msg will be shown
    private void noRecords() {
        homeViewModel.setNoRecordsFoundImage(true);
        binding.galleryBottomPhotoRecylerView.setVisibility(View.GONE);
        binding.tvRecordsNotFoundBottomPhotoView.setVisibility(View.VISIBLE);
    }

    private void downloadGallery() {
        homeViewModel.isPhotoDownload = false;
        long totalFileSize = 0;
        for (int i = 0; i < homeViewModel.getArrayListPhoto().size(); i++) {
            if (homeViewModel.getArrayListPhoto().get(i).isChecked()) {
                homeViewModel.isPhotoDownload = true;
                File file = new File(homeViewModel.getArrayListPhoto().get(i).getFilePath());
                totalFileSize += file.length();
            }
        }
        long availableInternalStorageSize = getAvailableInternalStorageSize();
        long minimumStorageForDownload = 1073741824L;
        Log.d("Internal Storage", "Available Internal Storage: " + availableInternalStorageSize + " TotalFileSize" + totalFileSize);
        if (totalFileSize < availableInternalStorageSize && minimumStorageForDownload < availableInternalStorageSize) {
            for (int i = 0; i < homeViewModel.getArrayListPhoto().size(); i++) {
                if (homeViewModel.getArrayListPhoto().get(i).isChecked()) {
                    if (homeViewModel.getArrayListPhoto().get(i).getFilePath().contains(".jpg")) {
                        homeViewModel.incrementImageCheckedFileCount();
                        homeViewModel.isPhotoDownload = true;
                        if (homeViewModel.getArrayListPhoto().get(i).getFilePath() != null)
                            fileDownloadToGallery(homeViewModel.getArrayListPhoto().get(i).getFilePath(), "SiOnyx", true);
                    }
                }
            }
        } else {
            if (homeViewModel.isPhotoDownload)
                showStorageAlert(getString(R.string.no_memory_to_download));
        }

        if (!homeViewModel.isPhotoDownload) {
            Toast.makeText(requireActivity(), R.string.download_unsuccessful, Toast.LENGTH_SHORT).show();
        }
        homeViewModel.clearSelectedCheckBox();
    }

    public void fileDownloadToGallery(String filePath, String destinationPath, boolean isImage) {
        Data inputData = new Data.Builder()
                .putString(FILE_PATH, filePath)
                .putString(DESTINATION_FILE_PATH, destinationPath)
                .putBoolean(IS_IMAGE, isImage)
                .putInt(NOTIFICATION_ID, 2)
                .build();
        WorkManager workManager = WorkManager.getInstance(requireContext());
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
                        homeViewModel.incrementImageFileCount();
                        if (homeViewModel.getTotalImageFileCount() == homeViewModel.getTotalCheckedImageFileCount()) {
                            homeViewModel.hasShowGalleryImgProgressbar(false);
                            homeViewModel.setHasWorkMangerRunImage(false);
                            homeViewModel.totalImageFileCompleteCount = 0;
                            homeViewModel.totalImageFileCheckedCount = 0;
                            homeViewModel.notificationBuilder("File download complete", NOTIFICATION_ID_IMG);
                            try {
                                Toast.makeText(homeViewModel.appContext, R.string.download_successful, Toast.LENGTH_SHORT).show();
                            } catch (Resources.NotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, "WorkManager: success" + homeViewModel.getTotalCheckedImageFileCount() + " //" + homeViewModel.getTotalImageFileCount());
                        break;
                    case FAILED:
                        homeViewModel.incrementImageFileCount();// for this any one file failed from the list
                        homeViewModel.setHasWorkMangerRunImage(false);
                        homeViewModel.hasShowGalleryImgProgressbar(false);
                        Log.e(TAG, "WorkManager: failed");
                        homeViewModel.notificationBuilder("File download failed", NOTIFICATION_ID_IMG);
                        break;
                    case RUNNING:
                        Log.e(TAG, "WorkManager: running");
                        if (!homeViewModel.isHasWorkMangerRunImage()) {
                            homeViewModel.setHasWorkMangerRunImage(true);
                            Toast.makeText(requireActivity(), R.string.download_in_progress, Toast.LENGTH_SHORT).show();
                        }
                        // homeViewModel.hasShowGalleryImgProgressbar(true);
                        homeViewModel.notificationBuilder("Downloading file...", NOTIFICATION_ID_IMG);
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
            if (homeViewModel != null)
                homeViewModel.isManageSelect.observe(lifecycleOwner, isManageSelected);
            if (imageAdapter != null && homeViewModel != null) {
                if (homeViewModel.getArrayListPhoto() != null) {
                    if (homeViewModel.isErasedPic) {
                        homeViewModel.getArrayListAll().clear();
                        homeViewModel.getArrayListPhoto().clear();
                        homeViewModel.getAllFiles(getActivity());
                        homeViewModel.isErasedPic = false;
                    } else {
                        homeViewModel.getArrayListPhoto().clear();
                        homeViewModel.getAllFiles(getActivity());
                    }
                    getGalleryImages();
                }
                imageAdapter.notifyDataSetChanged();
            }
        } else {
            if (homeViewModel != null) {
                homeViewModel.isManageSelect.removeObserver(isManageSelected);
                homeViewModel.isManageSelect.removeObservers(lifecycleOwner);
                homeViewModel.hasShowGalleryImgProgressbar(false);
            }
        }
    }

    //for handling the slect all Deselect all string status and function
    private void isSelectedAllItem() {
        ArrayList<GalleryBottomManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < homeViewModel.getArrayListPhoto().size(); i++) {
            if (homeViewModel.getArrayListPhoto().get(i).isChecked()) {
                tempList.add(homeViewModel.getArrayListPhoto().get(i));
            }
        }
        homeViewModel.checkedItemCount = tempList.size();
        homeViewModel.setGallerySelectAll(homeViewModel.getArrayListPhoto().size() == tempList.size());

    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
            homeViewModel.onManageSelectAllPhotos(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (homeViewModel.getArrayListPhoto().isEmpty()) {
            homeViewModel.setNoRecordsFoundImage(true);
        }
        isSelectedAllItem();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!homeViewModel.getArrayListPhoto().isEmpty()) {
            if (imageAdapter != null) {
                homeViewModel.getAllFiles(getActivity());
                getGalleryImages();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageAdapter = null;
        binding.galleryBottomPhotoRecylerView.setAdapter(null);
    }
}