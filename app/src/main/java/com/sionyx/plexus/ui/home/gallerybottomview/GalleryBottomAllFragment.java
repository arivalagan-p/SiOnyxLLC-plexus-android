package com.sionyx.plexus.ui.home.gallerybottomview;


import static com.sionyx.plexus.utils.Constants.getAvailableInternalStorageSize;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.DESTINATION_FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.FILE_PATH;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.IS_IMAGE;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_ID;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_ID_ALL;

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

import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentGalleryBottomAllViewBinding;
import com.sionyx.plexus.databinding.FragmentHomeBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.home.gallerybottomview.bottomtabviewadapter.GalleryBottomViewPageAdapter;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class GalleryBottomAllFragment extends Fragment {

    private static final String TAG = "GalleryBottomAllViewFragment";
    private FragmentGalleryBottomAllViewBinding binding;
    FragmentHomeBinding fragmentHomeBinding;
    private HomeViewModel homeViewModel;
    private LifecycleOwner lifecycleOwner;
    public boolean hasShowCheckbox = false;
    private GalleryBottomViewPageAdapter galleryBottomManageAllViewAdapter;
    private final boolean isVisible = true;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBottomAllViewBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        if (fragmentHomeBinding != null)
            fragmentHomeBinding.setViewModel(homeViewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel.isManageSelect.observe(lifecycleOwner, isManageSelected);

        homeViewModel.closePopupAllMedia.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                galleryBottomManageAllViewAdapter = null;
                binding.galleryBottomAllRecylerView.setAdapter(null);
                homeViewModel.isManageSelect.removeObserver(isManageSelected);
                homeViewModel.isManageSelect.removeObservers(lifecycleOwner);
                homeViewModel.setClosePopupAllMedia(false);
            }
        }));
    }

    private final Observer<Boolean> isManageSelected = aBoolean -> {
        hasShowCheckbox = aBoolean;
        getGalleryVideosAndImages();
    };

    @SuppressLint("NotifyDataSetChanged")
    private void getGalleryVideosAndImages() {
        homeViewModel.getAllFiles(getActivity());
        homeViewModel.filterImageList.clear();
        for (GalleryBottomManageModel i : homeViewModel.getArrayListAll()) {
            if (i.getFilePath().endsWith(".jpg")) {
                homeViewModel.filterImageList.add(i); //this is for image info screen
            }
        }

        if (!homeViewModel.getArrayListAll().isEmpty()) {
            homeViewModel.setNoRecordsFoundAll(false);
            if (galleryBottomManageAllViewAdapter == null) {
                galleryBottomManageAllViewAdapter = new GalleryBottomViewPageAdapter(hasShowCheckbox, requireContext(), homeViewModel.getArrayListAll(), homeViewModel);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(requireActivity(), requireActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 1 : 3);
                binding.galleryBottomAllRecylerView.setLayoutManager(gridLayoutManager);
                binding.galleryBottomAllRecylerView.setAdapter(galleryBottomManageAllViewAdapter);
            } else {
                galleryBottomManageAllViewAdapter.updatedArrayList(hasShowCheckbox, homeViewModel.getArrayListAll());
            }
        } else {
            noRecords();
        }

        homeViewModel.isManageSelectAllView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean && isVisible) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty()) {
                    galleryBottomManageAllViewAdapter.selectAllCheckbox(0, true, mHandler);
                    homeViewModel.isErasedAll = false;
                    homeViewModel.onManageSelectAllView(false);
                    isSelectAllItems();
                }
            }
        }));

        homeViewModel.isMediaVideoDeleted.observe(lifecycleOwner, aBoolean -> {
            if (aBoolean) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty()) {
                    homeViewModel.isErasedAll = true;
                    homeViewModel.getArrayListAll().clear();
                    getGalleryVideosAndImages();
                    galleryBottomManageAllViewAdapter.notifyDataSetChanged();
                } else {
                    if (homeViewModel.getArrayListAll().isEmpty()) {
                        noRecords();
                    }
                }
            }
        });

        homeViewModel.isManageAllSelectErase.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty() && isVisible) {

                    boolean isChecked = false;
                    for (GalleryBottomManageModel i : homeViewModel.getArrayListAll()) {
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

        homeViewModel.isEraseMedia.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty() && isVisible) {
                    int sizeOfArr = galleryBottomManageAllViewAdapter.erased();
                    homeViewModel.isErasedAll = true;
                    if (sizeOfArr == 0) {
                        noRecords();
                    } else {
                        homeViewModel.setNoRecordsFoundAll(false);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        homeViewModel.getArrayListAll().clear();
                        getGalleryVideosAndImages();
                        galleryBottomManageAllViewAdapter.updatedArrayList(hasShowCheckbox, homeViewModel.getArrayListAll());
                        galleryBottomManageAllViewAdapter.notifyDataSetChanged();
                        homeViewModel.hasEraseMedia(false);
                        homeViewModel.onManageAllSelectErase(false);
                    }, 250);

                }
            }
        }));

        homeViewModel.isManageSelectAllDownload.observe(lifecycleOwner, new EventObserver<>(hasSelect -> {
            if (hasSelect) {
                if (homeViewModel.getArrayListAll() != null && !homeViewModel.getArrayListAll().isEmpty()) {
                    downloadGallery();
                    getGalleryVideosAndImages();
                }
            }
        }));

        homeViewModel.isManageAllBack.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty()) {
                    galleryBottomManageAllViewAdapter.selectAllCheckbox(0, false, mHandler);
                    getGalleryVideosAndImages();
                    galleryBottomManageAllViewAdapter.notifyDataSetChanged();
                }
            }
        }));

        homeViewModel.UpdateManageLayout.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                   /* HomeViewModel.onRotate = false;
                    for(GalleryBottomManageModel i:arrayList){
                        i.setChecked(false);
                    }
                    galleryBottomManageAllViewAdapter.notifyDataSetChanged();*/
            }
        }));

        homeViewModel.isClearSelectedCheckboxAllView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty()) {
                    galleryBottomManageAllViewAdapter.selectAllCheckbox(0, false, mHandler);
                    homeViewModel.getAllFiles(getActivity());
                }
            }
        }));


        homeViewModel.ManageUpdateAllItem.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (galleryBottomManageAllViewAdapter != null && !homeViewModel.getArrayListAll().isEmpty()) {
                    getGalleryVideosAndImages();
                }
            }
        }));

        homeViewModel.isUpdateGalleryBottomAllItemView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (!homeViewModel.getArrayListAll().isEmpty()) {
                    homeViewModel.getAllFiles(getActivity());
                    homeViewModel.isUpdateGalleryBottomAllItemView(false);
                }
            }
        }));

        homeViewModel.hasShowGalleryAllProgressbar.observe(lifecycleOwner, aBoolean -> requireActivity().runOnUiThread(() -> {
            if (aBoolean)
                binding.dotProgressBar.setVisibility(View.VISIBLE);
            else
                binding.dotProgressBar.setVisibility(View.GONE);
        }));
    }

    /*
    Check whether the list of arr size 0 or not, iF it is 0 the Records not found msg will be shown
     */
    private void noRecords() {
        homeViewModel.setNoRecordsFoundAll(true);
        binding.galleryBottomAllRecylerView.setVisibility(View.GONE);
        binding.tvRecordsNotFoundBottomAllview.setVisibility(View.VISIBLE);
    }

    private void downloadGallery() {
        homeViewModel.isDownloaded = false;
        long totalFileSize = 0;
        for (int i = 0; i < homeViewModel.getArrayListAll().size(); i++) {
            if (homeViewModel.getArrayListAll().get(i).isChecked()) {
                homeViewModel.isDownloaded = true;
                File file = new File(homeViewModel.getArrayListAll().get(i).getFilePath());
                totalFileSize += file.length();
            }
        }
        long availableInternalStorageSize = getAvailableInternalStorageSize();
        long minimumStorageForDownload = 1073741824L;
        // read all file size and sum
        //double totalFileSize = Double.parseDouble(formatSize(finalTotalSize));
        Log.d("Internal Storage", "Available Internal Storage: " + availableInternalStorageSize + " TotalFileSize" + totalFileSize);
        if (totalFileSize < availableInternalStorageSize && minimumStorageForDownload < availableInternalStorageSize) {
            for (int i = 0; i < homeViewModel.getArrayListAll().size(); i++) {
                if (homeViewModel.getArrayListAll().get(i).isChecked()) {
                    if (homeViewModel.getArrayListAll().get(i).getFilePath().contains(".mp4")) {
                        Log.d(TAG, "Available Internal Storage: in .mp4" + availableInternalStorageSize);
                        homeViewModel.isDownloaded = true;
                        if (homeViewModel.getArrayListAll().get(i).getFilePath() != null) {
                            homeViewModel.incrementAllCheckedFileCount(); // for this dynamically file select count increase reason for user choose some time not select all file
                            fileDownloadToGallery(homeViewModel.getArrayListAll().get(i).getFilePath(), "SiOnyx", false);
                        }
                    } else if (homeViewModel.getArrayListAll().get(i).getFilePath().contains(".jpg")) {
                        Log.d(TAG, "Available Internal Storage: in .jpg" + availableInternalStorageSize);
                        homeViewModel.isDownloaded = true;
                        if (homeViewModel.getArrayListAll().get(i).getFilePath() != null) {
                            homeViewModel.incrementAllCheckedFileCount();
                            fileDownloadToGallery(homeViewModel.getArrayListAll().get(i).getFilePath(), "SiOnyx", true);
                        }
                    }
                }
            }
        } else {
            if (homeViewModel.isDownloaded)
                showStorageAlert(getString(R.string.no_memory_to_download));
        }

        if (!homeViewModel.isDownloaded) {
            Toast.makeText(requireActivity(), R.string.download_unsuccessful, Toast.LENGTH_SHORT).show();
        }
        homeViewModel.clearSelectedCheckBox();
    }

    private double getFileSize(File file) {
        long fileSizeBytes = file.length();
        double fileSizeKB = fileSizeBytes / 1024.0;
        double fileSizeMB = fileSizeKB / 1024.0;
        return fileSizeMB / 1024.0;
    }

    private static String formatDecimal(double number, int decimalPlaces) {
        StringBuilder pattern = new StringBuilder("#.");
        for (int i = 0; i < decimalPlaces; i++) {
            pattern.append("#");
        }
        DecimalFormat decimalFormat = new DecimalFormat(pattern.toString());
        return decimalFormat.format(number);
    }

    private String formatSize(long size) {
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double sizeInDouble = size;

        while (sizeInDouble >= 1024 && unitIndex < units.length - 1) {
            sizeInDouble /= 1024;
            unitIndex++;
        }
        return String.format("%.2f", sizeInDouble, units[unitIndex]);
        // return String.format("%.2f%s", sizeInDouble, units[unitIndex]); // for this reutrn size with unit
    }

    public void fileDownloadToGallery(String filePath, String destinationPath, boolean isImage) {
        Data inputData = new Data.Builder()
                .putString(FILE_PATH, filePath)
                .putString(DESTINATION_FILE_PATH, destinationPath)
                .putBoolean(IS_IMAGE, isImage)
                .putInt(NOTIFICATION_ID, 1)
                .build();

        OneTimeWorkRequest downloadRequest = new OneTimeWorkRequest.Builder(HomeFileDownloadWorker.class)
                .setInputData(inputData)
                .build();
        homeViewModel.workManager.enqueue(downloadRequest);

        homeViewModel.workManager.getWorkInfoByIdLiveData(downloadRequest.getId()).observeForever(workInfo -> {
            if (workInfo != null) {
                switch (workInfo.getState()) {
                    case SUCCEEDED:
                        Data data = workInfo.getOutputData();
                        data.getInt("resultKey", 0);
                        homeViewModel.incrementAllFileCount();
                        if (homeViewModel.getTotalAllFileCompleteCount() == homeViewModel.getTotalCheckedAllFileCount()) {
                            homeViewModel.hasShowGalleryAllProgressbar(false);
                            homeViewModel.setHasWorkMangerRunAll(false);
                            homeViewModel.totalAllFileCompleteCount = 0; // for reset all task complete count
                            homeViewModel.totalAllFileCheckedCount = 0; //for reset all checked files count
                            homeViewModel.notificationBuilder("File download complete", NOTIFICATION_ID_ALL);
                            try {
                                Toast.makeText(homeViewModel.appContext, R.string.download_successful, Toast.LENGTH_SHORT).show();
                            } catch (Resources.NotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, "WorkManager: success" + homeViewModel.getTotalAllFileCompleteCount() + "//" + homeViewModel.getTotalCheckedAllFileCount());
                        break;
                    case FAILED:
                        homeViewModel.incrementAllFileCount();
                        homeViewModel.hasShowGalleryAllProgressbar(false);
                        homeViewModel.setHasWorkMangerRunAll(false);
                        homeViewModel.notificationBuilder("File download failed", NOTIFICATION_ID_ALL);
                        Log.e(TAG, "WorkManager: failed");
                        break;
                    case RUNNING:
                        if (!homeViewModel.isHasWorkMangerRunAll()) {
                            homeViewModel.setHasWorkMangerRunAll(true);
                            Toast.makeText(homeViewModel.appContext, R.string.download_in_progress, Toast.LENGTH_SHORT).show();
                        }
                        // homeViewModel.hasShowGalleryAllProgressbar(true);
                        homeViewModel.notificationBuilder("Downloading file...", NOTIFICATION_ID_ALL);
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
            if (galleryBottomManageAllViewAdapter != null && homeViewModel != null) {
                if (homeViewModel.getArrayListAll() != null) {
                    if (homeViewModel.isErasedAll) {
                        homeViewModel.getArrayListAll().clear();
                        homeViewModel.isErasedAll = false;
                    }
                    getGalleryVideosAndImages();
                }
                galleryBottomManageAllViewAdapter.notifyDataSetChanged();
            }
        } else {
            if (homeViewModel != null) {
                homeViewModel.isManageSelect.removeObserver(isManageSelected);
                homeViewModel.isManageSelect.removeObservers(lifecycleOwner);
                homeViewModel.hasShowGalleryAllProgressbar(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (homeViewModel.getArrayListAll().isEmpty()) {
            homeViewModel.setNoRecordsFoundAll(true);
        }
        isSelectAllItems();
    }

    //for handling the select all Deselect all string status and function
    private void isSelectAllItems() {
        ArrayList<GalleryBottomManageModel> tempList = new ArrayList<>();
        for (int i = 0; i < homeViewModel.getArrayListAll().size(); i++) {
            if (homeViewModel.getArrayListAll().get(i).isChecked()) {
                tempList.add(homeViewModel.getArrayListAll().get(i));
            }
        }
        homeViewModel.checkedItemCount = tempList.size();
        homeViewModel.setGallerySelectAll(homeViewModel.getArrayListAll().size() == tempList.size());
    }

    private void showConfirmEraseGalleryItemDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.CONFIRM_ERASE;
            activity.showDialog("", message, null);
            homeViewModel.onManageAllSelectErase(false);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onPause() {
        super.onPause();
        if (!homeViewModel.getArrayListAll().isEmpty()) {
            if (galleryBottomManageAllViewAdapter != null) {
                getGalleryVideosAndImages();
                galleryBottomManageAllViewAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        galleryBottomManageAllViewAdapter = null;
        binding.galleryBottomAllRecylerView.setAdapter(null);
    }
}



