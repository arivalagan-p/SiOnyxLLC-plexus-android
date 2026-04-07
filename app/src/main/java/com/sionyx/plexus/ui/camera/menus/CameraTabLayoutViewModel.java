package com.sionyx.plexus.ui.camera.menus;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.utils.Event;

public class CameraTabLayoutViewModel extends AndroidViewModel {
    public CameraTabLayoutViewModel(@NonNull Application application) {
        super(application);
    }

    private final MutableLiveData<Event<Boolean>> _isCancel = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancel = _isCancel;

    private final MutableLiveData<Event<Boolean>> _isSaveSettings = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSaveSettings = _isSaveSettings;

    public void onCancel() {
        _isCancel.setValue(new Event<>(true));
    }

    public void onSaveSettings() {
        _isSaveSettings.setValue(new Event<>(true));
    }

    public static final MutableLiveData<Event<Boolean>> showCircleProgress = new MutableLiveData<>();

    public LiveData<Event<Boolean>> onShowCircleProgress(){
        return showCircleProgress;
    }

    public void setShowCircleProgress(boolean isShow){
        showCircleProgress.postValue(new Event<>(isShow));
    }


}
