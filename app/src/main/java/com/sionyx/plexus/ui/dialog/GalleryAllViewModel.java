package com.sionyx.plexus.ui.dialog;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dome.librarynightwave.utils.Event;
import com.sionyx.plexus.ui.galleryliveview.GalleryManageModel;
import com.sionyx.plexus.ui.galleryliveview.GallerySelectedItemInfo;
import com.sionyx.plexus.utils.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class GalleryAllViewModel extends ViewModel {
    //    public GalleryAllViewModel(@NonNull Application application) {
//        super(application);
//    }
    /*gallery all view live data observe*/
    public String galleryFilePath;
    public MutableLiveData<Event<Boolean>> isSelectManage = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> _isCancelGalleryAllView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancelGalleryAllView = _isCancelGalleryAllView;

    private final MutableLiveData<Event<Boolean>> _isBackGalleryAllView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isBackGalleryAllView = _isBackGalleryAllView;

    private final MutableLiveData<Event<GallerySelectedItemInfo>> _isSelectGalleryItemView = new MutableLiveData<>();
    public LiveData<Event<GallerySelectedItemInfo>> isSelectGalleryItemView = _isSelectGalleryItemView;

    /*gallery manage view live data observe*/
//    public MutableLiveData<Event<Boolean>> isManageSelectAll = new MutableLiveData<>();
    public MutableLiveData<Boolean> isManageSelectAll = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> isManageSelectAll2 = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> isManageSelectDownload = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> isManageSelectErase = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllView = _isManageSelectAllView;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllPhotos = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllPhotos = _isManageSelectAllPhotos;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllVideos = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllVideos = _isManageSelectAllVideos;

    private final MutableLiveData<Event<Boolean>> _isManageAllSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageAllSelectErase = _isManageAllSelectErase;

    private final MutableLiveData<Event<Boolean>> _isManagePhotoSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManagePhotoSelectErase = _isManagePhotoSelectErase;

    private final MutableLiveData<Event<Boolean>> _isManageVideoSelectErase = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageVideoSelectErase = _isManageVideoSelectErase;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllDownload = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllDownload = _isManageSelectAllDownload;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllPhotoDownload = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllPhotoDownload = _isManageSelectAllPhotoDownload;

    private final MutableLiveData<Event<Boolean>> _isManageSelectAllVideoDownload = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isManageSelectAllVideoDownload = _isManageSelectAllVideoDownload;

    private final MutableLiveData<Event<Boolean>> _isClearSelectedCheckboxAllView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isClearSelectedCheckboxAllView = _isClearSelectedCheckboxAllView;

    private final MutableLiveData<Event<Boolean>> _isClearSelectedCheckboxPhotoView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isClearSelectedCheckboxPhotoView = _isClearSelectedCheckboxPhotoView;

    private final MutableLiveData<Event<Boolean>> _isClearSelectedCheckboxVideoView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isClearSelectedCheckboxVideoView = _isClearSelectedCheckboxVideoView;

    public MutableLiveData<Event<Boolean>> isManageSelectCancel = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> isManageSelectBack = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> clearSelectedCheckBox = new MutableLiveData<>();

    private final MutableLiveData<Boolean> _noRecordsFoundAll = new MutableLiveData<>();
    public LiveData<Boolean> noRecordsFoundAll = _noRecordsFoundAll;

    private final MutableLiveData<Boolean> _noRecordsFoundImage = new MutableLiveData<>();
    public LiveData<Boolean> noRecordsFoundImage = _noRecordsFoundImage;

    private final MutableLiveData<Boolean> _noRecordsFoundVideo = new MutableLiveData<>();
    public LiveData<Boolean> noRecordsFoundVideo = _noRecordsFoundVideo;

    /*gallery recording info view observer*/
    public MutableLiveData<Event<Boolean>> _isSelectRecordingInfoBack = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectRecordingInfoCancel = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectRecordingInfoDelete = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectRecordingInfoPlay = new MutableLiveData<>();

    /*gallery recording video player view observer*/
    public MutableLiveData<Event<Boolean>> _isSelectVideoPlayerBack = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectVideoPlayerCancel = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectStopVideo = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectPauseVideo = new MutableLiveData<>();
    public MutableLiveData<Event<Boolean>> _isSelectPlayVideo = new MutableLiveData<>();

    public MutableLiveData<Event<Boolean>> _isUpdateGalleryItemView = new MutableLiveData<>();

    /* for this recording info viw delete button click refresh item view*/
    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryAllItemView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateGalleryAllItemView = _isUpdateGalleryAllItemView;

    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryPhotosItemView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateGalleryPhotosItemView = _isUpdateGalleryPhotosItemView;

    private final MutableLiveData<Event<Boolean>> _isUpdateGalleryVideosItemView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isUpdateGalleryVideosItemView = _isUpdateGalleryVideosItemView;

    /*gallery all view binding methods*/
    public void onSelectManage() {
        isSelectManage.setValue(new Event<>(true));
    }

    public void onSelectGalleryBack() {
        _isBackGalleryAllView.setValue(new Event<>(true));
    }

    public void onCancelGalleryAllView() {
        _isCancelGalleryAllView.setValue(new Event<>(true));
    }

    public void onSelectGalleryItemView(GallerySelectedItemInfo gallerySelectedItemInfo) {
        _isSelectGalleryItemView.setValue(new Event<>(gallerySelectedItemInfo));
    }

    /*gallery manage view binding methods*/
    public void onManageSelectAll() {
        isManageSelectAll2.setValue(new Event<>(true));
        // isManageSelectAll.setValue(true);
    }

    public void onManageSelectAllView(boolean isSelectAll) {
        _isManageSelectAllView.setValue(new Event<>(isSelectAll));
    }

    public void onManageSelectAllPhotos(boolean isSelectAll) {
        _isManageSelectAllPhotos.setValue(new Event<>(isSelectAll));
    }

    public void onManageSelectAllVideos(boolean isSelectAll) {
        _isManageSelectAllVideos.setValue(new Event<>(isSelectAll));
    }

    public void onManageSelectDownload() {
        isManageSelectDownload.setValue(new Event<>(true));
    }

    public void onManageSelectErase() {
        isManageSelectErase.setValue(new Event<>(true));
    }

    public void onManageAllSelectErase(boolean isErased) {
        _isManageAllSelectErase.setValue(new Event<>(isErased));
    }

    public void onManagePhotoSelectErase(boolean isErased) {
        _isManagePhotoSelectErase.setValue(new Event<>(isErased));
    }

    public void onManageVideoSelectErase(boolean isErased) {
        _isManageVideoSelectErase.setValue(new Event<>(isErased));
    }

    public void onManageSelectAllDownload(boolean isDownload) {
        _isManageSelectAllDownload.setValue(new Event<>(isDownload));
    }

    public void onManageSelectAllPhotoDownload(boolean isDownload) {
        _isManageSelectAllPhotoDownload.setValue(new Event<>(isDownload));
    }

    public void onManageSelectAllVideoDownload(boolean isDownload) {
        _isManageSelectAllVideoDownload.setValue(new Event<>(isDownload));
    }

    public void onClearSelectedCheckboxAllView(boolean isClear) {
        _isClearSelectedCheckboxAllView.setValue(new Event<>(isClear));
    }

    public void onClearSelectedCheckboxPhotoView(boolean isClear) {
        _isClearSelectedCheckboxPhotoView.setValue(new Event<>(isClear));
    }

    public void onClearSelectedCheckboxVideoView(boolean isClear) {
        _isClearSelectedCheckboxVideoView.setValue(new Event<>(isClear));
    }

    public void onManageViewBack() {
        isManageSelectBack.setValue(new Event<>(true));
    }

    public void onManageViewCancel() {
        isManageSelectCancel.setValue(new Event<>(true));
    }

    public void clearSelectedCheckBox() {
        clearSelectedCheckBox.setValue(new Event<>(true));
    }

    public void setNoRecordsFoundAll(boolean noRecordsFoundAll) {
        this._noRecordsFoundAll.setValue(noRecordsFoundAll);
    }

    public void setNoRecordsFoundImage(boolean noRecordsFoundImage) {
        this._noRecordsFoundImage.setValue(noRecordsFoundImage);
    }

    public void setNoRecordsFoundVideo(boolean noRecordsFoundVideo) {
        this._noRecordsFoundVideo.setValue(noRecordsFoundVideo);
    }

    /*gallery recording view binding methods*/
    public void onSelectRecordingInfoCancel() {
        _isSelectRecordingInfoCancel.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoDelete() {
        _isSelectRecordingInfoDelete.setValue(new Event<>(true));
    }

    public void onSelectRecordInfoBack() {
        _isSelectRecordingInfoBack.setValue(new Event<>(true));
    }

    public void onSelectRecordingInfoPlay() {
        _isSelectRecordingInfoPlay.setValue(new Event<>(true));
    }

    /*gallery video player view binding methods*/
    public void onRecordingInfoVideoPlayerBack() {
        _isSelectVideoPlayerBack.setValue(new Event<>(true));
    }

    public void onRecordingInfoVideoPlayerCancel() {
        _isSelectVideoPlayerCancel.setValue(new Event<>(true));
    }

    public void onRecordingInfoVideoPlayerPlay() {
        _isSelectPlayVideo.setValue(new Event<>(true));
    }

    public void onRecordingInfoVideoStop() {
        _isSelectStopVideo.setValue(new Event<>(true));
    }

    public void onRecordingInfoVideoPasue() {
        _isSelectPauseVideo.setValue(new Event<>(true));
    }

    public void updateGalleryItemView(boolean isUpdate) {
        _isUpdateGalleryItemView.setValue(new Event<>(isUpdate));
    }

    public void updateGalleryAllItemView(boolean isUpdateItemView) {
        _isUpdateGalleryAllItemView.setValue(new Event<>(isUpdateItemView));
    }

    public void updateGalleryPhotosItemView(boolean isUpdateItemView) {
        _isUpdateGalleryPhotosItemView.setValue(new Event<>(isUpdateItemView));
    }

    public void updateGalleryVideosItemView(boolean isUpdateItemView) {
        _isUpdateGalleryVideosItemView.setValue(new Event<>(isUpdateItemView));
    }

    public boolean isVideoPause;
    public boolean isVideoPlay;
    public int playbackPosition;

    public boolean isVideoPause() {
        return isVideoPause;
    }

    public void setVideoPause(boolean videoPause) {
        isVideoPause = videoPause;
    }

    public boolean isVideoPlay() {
        return isVideoPlay;
    }

    public void setVideoPlay(boolean videoPlay) {
        isVideoPlay = videoPlay;
    }

    public int getPlaybackPosition() {
        return playbackPosition;
    }

    public void setPlaybackPosition(int playbackPosition) {
        this.playbackPosition = playbackPosition;
    }

    public boolean isLiveManageSelected() {
        return isLiveManageSelected;
    }

    public void setLiveManageSelected(boolean liveManageSelected) {
        isLiveManageSelected = liveManageSelected;
    }

    private boolean isLiveManageSelected = false;

    private final ArrayList<GalleryManageModel> arrayListAll = new ArrayList<>();

    private final ArrayList<GalleryManageModel> arrayListPhoto = new ArrayList<>();

    private final ArrayList<GalleryManageModel> arrayListVideo = new ArrayList<>();

    public ArrayList<GalleryManageModel> getArrayListAll() {
        return arrayListAll;
    }

    public ArrayList<GalleryManageModel> getArrayListPhoto() {
        return arrayListPhoto;
    }

    public ArrayList<GalleryManageModel> getArrayListVideo() {
        return arrayListVideo;
    }

    public void addAllItem(GalleryManageModel model) {
        this.getArrayListAll().add(model);
    }

    public void addPics(GalleryManageModel model) {
        this.getArrayListPhoto().add(model);
    }

    public void addVideos(GalleryManageModel model) {
        this.getArrayListVideo().add(model);
    }

    public void getAllFiles(FragmentActivity activity, String connectedSsidFromWiFiManager) {
        try {
            File photosDirectory = createAppFolderForImage(activity, "SiOnyx", connectedSsidFromWiFiManager);
            File videosDirectory = createAppFolderForVideo(activity, "SiOnyx", connectedSsidFromWiFiManager);

            File[] videoFile = videosDirectory != null ? videosDirectory.listFiles() : new File[0];
            File[] imageFiles = photosDirectory != null ? photosDirectory.listFiles() : new File[0];

            File[] combinedFiles = new File[videoFile.length +  imageFiles.length];
            // copy the files from directory1 into the combined array
            System.arraycopy(videoFile, 0, combinedFiles, 0, videoFile.length);
            // copy the files from directory2 into the combined array, starting at the end of the files1 array
            System.arraycopy(imageFiles, 0, combinedFiles, videoFile.length, imageFiles.length);

            Arrays.sort(combinedFiles, (file1, file2) -> {
                long lastModified1 = file1.lastModified();
                long lastModified2 = file2.lastModified();

                if (lastModified1 > lastModified2) {
                    return -1;
                } else if (lastModified1 < lastModified2) {
                    return 1;
                } else {
                    return 0;
                }
            });

            for (File file : combinedFiles) {
                if (file.isFile()) {
                    String path = file.getAbsolutePath();
                    if ((path.endsWith(".mp4")) || (path.endsWith(".jpg"))) {
                        GalleryManageModel manageModel = new GalleryManageModel();
                        manageModel.setFilePath(path);
                        manageModel.setFileName(file.getName());
                        if (!getArrayListAll().contains(manageModel)) {
                            addAllItem(manageModel);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Exception all files >> " + e.getMessage());
        }
    }

    private File createAppFolderForImage(Context context, String folderName, String SSID) {

        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalFilesDir != null) {
            File folder = new File(externalFilesDir, folderName);
            File subFolder = new File(folder, Constants.getFile());
            File ssid_folder = new File(subFolder, SSID);
            if (!ssid_folder.exists()) {
                boolean folderCreated = ssid_folder.mkdir();
                if (!folderCreated) {
                    Log.e("TCPRepository", "Failed to create folder: " + ssid_folder.getAbsolutePath());
                    return null;
                }
            }
            return ssid_folder;
        } else {
            Log.e("TCPRepository ", "External files directory is null.");
            return null;
        }
    }

    private File createAppFolderForVideo(Context context, String folderName, String connectedSsidFromWiFiManager) {
        File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (externalFilesDir != null) {
            File folder = new File(externalFilesDir, folderName);
            File ssidFolder = new File(folder, Constants.getFile());
            File subFolder = new File(ssidFolder, connectedSsidFromWiFiManager);
            if (!subFolder.exists()) {
                boolean folderCreated = subFolder.mkdirs();
                if (!folderCreated) {
                    Log.e("TCPRepository", "Failed to create folder: " + subFolder.getAbsolutePath());
                    return null;
                }
            }
            return subFolder;
        } else {
            Log.e("TCPRepository ", "External files directory is null.");
            return null;
        }
    }

    public MutableLiveData<Event<Boolean>> isSelectAllSelected = new MutableLiveData<>();

    public void setSelectAllSelected(boolean isSelectAll) {
        this.isSelectAllSelected.setValue(new Event<>(isSelectAll));
    }

    public boolean isErasedAll;
    public boolean isErasedPhoto;
    public boolean isErasedVideo;

    public MutableLiveData<Event<Boolean>> isMedialFileDeleted = new MutableLiveData<>();

    public void setIsMedialFileDeleted(boolean isDeleted) {
        this.isMedialFileDeleted.setValue(new Event<>(isDeleted));
    }

    public MutableLiveData<Event<Boolean>> isBackToGallery = new MutableLiveData<>();

    public void setBackToGallery(boolean backToGallery) {
        this.isBackToGallery.setValue(new Event<>(backToGallery));
    }

    public boolean isErase;
    public boolean isDownloaded;
    public boolean isPhotoDownload;
    public boolean isVideoDownload;

    public int isCurrentTab;

    public void setCurrentTabScreen(int curTab) {
        this.isCurrentTab = curTab;
    }

    private final MutableLiveData<Event<Boolean>> _isEraseLiveMedia = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isEraseLiveMedia = _isEraseLiveMedia;

    public void hasEraseLiveMedia(Boolean value) {
        _isEraseLiveMedia.setValue(new Event<>(value));
    }

    public void hasEraseLiveVideoMedia(Boolean value) {
        _isEraseLiveVideoMedia.setValue(new Event<>(value));
    }

    public void hasEraseLivePhotoMedia(Boolean value) {
        _isEraseLivePhotoMedia.setValue(new Event<>(value));
    }

    private final MutableLiveData<Event<Boolean>> _isEraseLiveVideoMedia = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isEraseLiveVideoMedia = _isEraseLiveVideoMedia;

    private final MutableLiveData<Event<Boolean>> _isEraseLivePhotoMedia = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isEraseLivePhotoMedia = _isEraseLivePhotoMedia;

    private final MutableLiveData<Event<Boolean>> _isDeleteRecordedVideo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDeleteRecordedVideo = _isDeleteRecordedVideo;

    private final MutableLiveData<Event<Boolean>> _isSelectImageInfo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectImageInfo = _isSelectImageInfo;

    public void onSelectImageInfo() {
        _isSelectImageInfo.setValue(new Event<>(true));
    }

    public void hasDeleteRecordedVideo(Boolean recordedVideo) {
        _isDeleteRecordedVideo.setValue(new Event<>(recordedVideo));
    }

    private final MutableLiveData<Event<Boolean>> _isDeleteCapturedImage = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDeleteCapturedImage = _isDeleteCapturedImage;

    public void hasDeleteCapturedImage(Boolean recordedVideo) {
        _isDeleteCapturedImage.setValue(new Event<>(recordedVideo));
    }

    public static boolean itemClick = false;

    public int isSelectedImagePosition;

    public ArrayList<GalleryManageModel> filterImageList = new ArrayList<>();

    public GalleryManageModel isSelectedFileModel;


}
