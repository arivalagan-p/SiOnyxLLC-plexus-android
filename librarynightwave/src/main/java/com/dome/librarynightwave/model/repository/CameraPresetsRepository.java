package com.dome.librarynightwave.model.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.persistence.DomeRoomDatabase;
import com.dome.librarynightwave.model.persistence.savesettings.SaveSettings;
import com.dome.librarynightwave.model.persistence.savesettings.SaveSettingsDao;
import com.dome.librarynightwave.utils.Event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CameraPresetsRepository {
    private static final String TAG = "CameraPresetsRepository";
    private final SaveSettingsDao saveSettingsDao;
    private static CameraPresetsRepository cameraPresetsRepository;
    private final MutableLiveData<Event<Boolean>> _isSavedSettingsSuccessfully = new MutableLiveData<>();


    public static CameraPresetsRepository getInstance(Application application) {
        if (cameraPresetsRepository == null) {
            cameraPresetsRepository = new CameraPresetsRepository(application);
        }
        return cameraPresetsRepository;
    }

    public CameraPresetsRepository(Application application) {
        DomeRoomDatabase db = DomeRoomDatabase.getInstance(application);
        saveSettingsDao = db.saveSettingsDao();
    }

    private int index = 0;

    public void saveSettingsValue(ArrayList<SaveSettings> saveSettings) {
        if (saveSettings.size() > 0 && index < saveSettings.size()) {
            SaveSettings saveSetting = saveSettings.get(index);
            Completable.fromAction(() -> saveSettingsDao.insertSettings(saveSetting))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            //Log.d(TAG, "onSubscribe: Called");
                        }

                        @Override
                        public void onComplete() {
                            //  Log.d(TAG, "onComplete: Called");
                            index++;
                            saveSettingsValue(saveSettings);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(TAG, "onError: " + e.getMessage());
                            _isSavedSettingsSuccessfully.postValue(new Event<>(false));
                        }
                    });
        } else {
            if (saveSettings.size() == index) {
                _isSavedSettingsSuccessfully.postValue(new Event<>(true));
            }
            index = 0;
        }
    }

    public LiveData<Event<Boolean>> hasSavedSettingsSuccessfully() {
        return _isSavedSettingsSuccessfully;
    }

    public void updatePreset(int setting_id, String setting_name, byte setting_value, String display_value, boolean is_nightwave, LocalDateTime datetime) {
        Completable.fromAction(() -> saveSettingsDao.updatePreset(setting_id, setting_name, setting_value, display_value, is_nightwave, datetime))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraLastConnectedTimeAndState Called ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }

    public Single<List<SaveSettings>> getSavedSettings(String preset_name, boolean is_nightwave) {
        return saveSettingsDao.getSavedSettings(preset_name, is_nightwave);
    }

    public Single<Boolean> isPresetAvailable(String preset_name, boolean isNightwave) {
        return saveSettingsDao.isPresetAvailable(preset_name, isNightwave).map(count -> count == 0);// True if name is available
    }

    public List<SaveSettings> getSavedPreSettings(boolean isNightwave) {
        return saveSettingsDao.getSavedPreSettings(isNightwave);
    }

    public List<Integer> getPresetCount(boolean isNightwave) {
        return saveSettingsDao.getPresetCount(isNightwave);
    }

    public void deletePreset(String preset_name, boolean is_nightwave) {
        Completable.fromAction(() -> saveSettingsDao.deletePreset(preset_name, is_nightwave)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: deletePreset");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
}
