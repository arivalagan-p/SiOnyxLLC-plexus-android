package com.sionyx.plexus.ui.galleryliveview;

import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.FILE_DOWNLOAD;
import static com.sionyx.plexus.utils.workmanager.HomeFileDownloadWorker.NOTIFICATION_CHANNEL_ID;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.WorkManager;

import com.sionyx.plexus.R;
import com.sionyx.plexus.utils.Event;

import java.util.ArrayList;

public class GalleryViewModel extends AndroidViewModel {
    private final Application application;
    public Context appContext;

    private final MutableLiveData<Boolean> _isManageSelected = new MutableLiveData<>();
    public LiveData<Boolean> isManageSelected = _isManageSelected;

    private final MutableLiveData<Boolean> _isSelectAllSelectedForImage = new MutableLiveData<>();
    public LiveData<Boolean> isSelectAllSelectedForImage = _isSelectAllSelectedForImage;

    private final MutableLiveData<Boolean> _isSelectAllSelectedForVideo = new MutableLiveData<>();
    public LiveData<Boolean> isSelectAllSelectedForVideo = _isSelectAllSelectedForVideo;

    private final MutableLiveData<Boolean> _isSelectAllSelectedForAllMedia = new MutableLiveData<>();
    public LiveData<Boolean> isSelectAllSelectedForAllMedia = _isSelectAllSelectedForAllMedia;

    private final MutableLiveData<Event<Boolean>> _closePopupAllMedia = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupAllMedia = _closePopupAllMedia;

    private final MutableLiveData<Event<Boolean>> _closePopupImage = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupImage = _closePopupImage;

    private final MutableLiveData<Event<Boolean>> _closePopupVideo = new MutableLiveData<>();
    public LiveData<Event<Boolean>> closePopupVideo = _closePopupVideo;

    private final GalleyAllViewsFragment galleyAllViewsFragment = new GalleyAllViewsFragment();
    private final GalleryImageFragment galleryImageFragment = new GalleryImageFragment();
    private final GalleryVideoFragment galleryVideoFragment = new GalleryVideoFragment();
    private final ArrayList<Fragment> fragmentArrayList = new ArrayList<>();
    public WorkManager workManager = WorkManager.getInstance(getApplication().getApplicationContext());

    public ArrayList<Fragment> getFragments() {
        if (fragmentArrayList.isEmpty()) {
            fragmentArrayList.add(galleyAllViewsFragment);
            fragmentArrayList.add(galleryImageFragment);
            fragmentArrayList.add(galleryVideoFragment);
        }
        return fragmentArrayList;
    }

    public GalleryViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.appContext = application.getApplicationContext();
    }

    public void setIfManageSelected(boolean ifManageSelected) {
        _isManageSelected.setValue(ifManageSelected);
    }

    public void setIfSelectAllSelectedForImage(boolean ifSelectAllSelectedForImage) {
        _isSelectAllSelectedForImage.setValue(ifSelectAllSelectedForImage);
    }

    public void setIfSelectAllSelectedForVideo(boolean ifSelectAllSelectedForVideo) {
        _isSelectAllSelectedForVideo.setValue(ifSelectAllSelectedForVideo);
    }

    public void setIfSelectAllSelectedForAllMedia(boolean ifSelectAllSelectedForAllMedia) {
        _isSelectAllSelectedForAllMedia.setValue(ifSelectAllSelectedForAllMedia);
    }

    public void setClosePopupAllMedia(Boolean value) {
        _closePopupAllMedia.setValue(new Event<>(value));
    }

    public void setClosePopupImage(Boolean value) {
        _closePopupImage.setValue(new Event<>(value));
    }

    public void setClosePopupVideo(Boolean value) {
        _closePopupVideo.setValue(new Event<>(value));
    }

    public void notificationBuilder(String message, int notificationId) {
        NotificationCompat.Builder completeNotificationBuilder =
                new NotificationCompat.Builder(application.getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(FILE_DOWNLOAD)
                        .setContentText(message)
                        .setSmallIcon(R.drawable.image_sionyx);

        if (ActivityCompat.checkSelfPermission(application.getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(application.getApplicationContext()).notify(notificationId, completeNotificationBuilder.build());
    }

    public boolean hasWorkMangerRunAll;
    public boolean hasWorkMangerRunImage;
    public boolean hasWorkMangerRunVideo;

    public int totalImageFileCompleteCount = 0;
    public int totalVideoFileCompleteCount = 0;
    public int totalAllFileCompleteCount = 0;
    public int totalImageFileCheckedCount = 0;
    public int totalVideoFileCheckedCount = 0;
    public int totalAllFileCheckedCount = 0;


    public int getTotalCheckedImageFileCount() {
        return totalImageFileCheckedCount;
    }

    public void incrementImageCheckedFileCount() {
        this.totalImageFileCheckedCount++;
    }

    public int getTotalCheckedVideoFileCount() {
        return totalVideoFileCheckedCount;
    }

    public void incrementVideoCheckedFileCount() {
        this.totalVideoFileCheckedCount++;
    }


    public int getTotalCheckedAllFileCount() {
        return totalAllFileCheckedCount;
    }

    public void incrementAllCheckedFileCount() {
        this.totalAllFileCheckedCount++;
    }


    public int getTotalVideoFileCompleteCount() {
        return totalVideoFileCompleteCount;
    }

    public void incrementVideoFileCount() {
        this.totalVideoFileCompleteCount++;
    }

    public int getTotalAllFileCompleteCount() {
        return totalAllFileCompleteCount;
    }

    public void incrementAllFileCount() {
        this.totalAllFileCompleteCount++;
    }

    public void incrementImageFileCount() {
        totalImageFileCompleteCount++;
    }

    public int getImageFileCount() {
        return totalImageFileCompleteCount;
    }

    private final MutableLiveData<Boolean> _hasShowGalleryAllProgressbar = new MutableLiveData<>();
    public LiveData<Boolean> hasShowGalleryAllProgressbar = _hasShowGalleryAllProgressbar;

    private final MutableLiveData<Boolean> _hasShowGalleryImgProgressbar = new MutableLiveData<>();
    public LiveData<Boolean> hasShowGalleryImgProgressbar = _hasShowGalleryImgProgressbar;

    private final MutableLiveData<Boolean> _hasShowGalleryVideoProgressbar = new MutableLiveData<>();
    public LiveData<Boolean> hasShowGalleryVideoProgressbar = _hasShowGalleryVideoProgressbar;

    public boolean isHasWorkMangerRunAll() {
        return hasWorkMangerRunAll;
    }

    public void setHasWorkMangerRunAll(boolean hasWorkMangerRunAll) {
        this.hasWorkMangerRunAll = hasWorkMangerRunAll;
    }

    public boolean isHasWorkMangerRunImage() {
        return hasWorkMangerRunImage;
    }

    public void setHasWorkMangerRunImage(boolean hasWorkMangerRunImage) {
        this.hasWorkMangerRunImage = hasWorkMangerRunImage;
    }

    public boolean isHasWorkMangerRunVideo() {
        return hasWorkMangerRunVideo;
    }

    public void setHasWorkMangerRunVideo(boolean hasWorkMangerRunVideo) {
        this.hasWorkMangerRunVideo = hasWorkMangerRunVideo;
    }

    public void hasShowGalleryAllProgressbar(boolean hasShowProgressBar) {
        _hasShowGalleryAllProgressbar.setValue(hasShowProgressBar);
    }

    public void hasShowGalleryImgProgressbar(boolean hasShowProgressBar) {
        _hasShowGalleryImgProgressbar.setValue(hasShowProgressBar);
    }

    public void hasShowGalleryVideoProgressbar(boolean hasShowProgressBar) {
        _hasShowGalleryVideoProgressbar.setValue(hasShowProgressBar);
    }
}
