package com.sionyx.plexus.ui.camera.menus;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.utils.Event;

public class CameraSettingsInfoViewModel extends AndroidViewModel {
    private int evPosition;
    private boolean isEvSeekbarSelected = false;
    private boolean isBrightnessSeekbarSelected = false;
    private final MutableLiveData<Object> _observeOpsinEvValue = new MutableLiveData<>();
    LiveData<Object> observeOpsinEvValue = _observeOpsinEvValue;

    private final MutableLiveData<Event<Object>> _observeOpsinBrightnessValue = new MutableLiveData<>();
    LiveData<Event<Object>> observeOpsinBrightnessValue = _observeOpsinBrightnessValue;

    public int getEvPosition() {
        return evPosition;
    }

    public void setEvPosition(int evPosition) {
        this.evPosition = evPosition;
    }

    public boolean isEvSeekbarSelected() {
        return !isEvSeekbarSelected;
    }

    public void setEvSeekbarSelected(boolean evSeekbarSelected) {
        isEvSeekbarSelected = evSeekbarSelected;
    }

    public boolean isBrightnessSeekbarSelected() {
        return !isBrightnessSeekbarSelected;
    }

    public void setBrightnessSeekbarSelected(boolean evSeekbarSelected) {
        isBrightnessSeekbarSelected = evSeekbarSelected;
    }

    public void observeOpsinEvValue(Object observeOpsinEvValue){
        _observeOpsinEvValue.setValue(observeOpsinEvValue);
    }

    public void observeOpsinBrightnessValue(Object observeOpsinEvValue){
        _observeOpsinBrightnessValue.postValue(new Event<>(observeOpsinEvValue));
    }

    public CameraSettingsInfoViewModel(@NonNull Application application) {
        super(application);
    }
}
