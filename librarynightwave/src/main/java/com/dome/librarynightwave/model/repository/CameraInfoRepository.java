package com.dome.librarynightwave.model.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.room.EmptyResultSetException;


import com.dome.librarynightwave.model.persistence.DomeRoomDatabase;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistoryDao;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistoryDao;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CameraInfoRepository {

    private static final String TAG = "CameraInfoRepository";
    private final WiFiHistoryDao cameraInfoDao;
    private final NightwaveDigitalWiFiHistoryDao nwdCameraInfoDao;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private static CameraInfoRepository cameraInfoRepository;


    public static CameraInfoRepository getInstance(Application application) {
        if (cameraInfoRepository == null) {
            cameraInfoRepository = new CameraInfoRepository(application);
        }
        return cameraInfoRepository;
    }

    public CameraInfoRepository(Application application) {
        DomeRoomDatabase db = DomeRoomDatabase.getInstance(application);
        cameraInfoDao = db.cameraInfoDao();
        nwdCameraInfoDao = db.nwdCameraInfoDao();
    }

    //Get Loading State
    public MutableLiveData<Boolean> getIsLoading(){
        return isLoading;
    }

    /*Camera Info*/
    public void insertCamera(WiFiHistory cameraInfo) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.insertCamera(cameraInfo)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called");
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Called");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: "+e.getMessage());
                    }
                });
    }
    public void updateCameraLastConnectedTimeAndState(String ssid, long timeStamp,int state) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.updateCameraLastConnectedTimeAndState(ssid,timeStamp,state)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraLastConnectedTimeAndState Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }

    public void updateLastConnectedCameraState(String ssid) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.updateLastConnectedCameraState(ssid)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraLastConnectedTimeAndState Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
    public void updateCameraLastConnectedTime(String ssid, long timeStamp) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.updateCameraLastConnectedTime(ssid,timeStamp)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraLastConnectedTime Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
    public void updateCameraConnectionState(String ssid, int state) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.updateCameraConnectionState(ssid,state)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
//                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
//                        Log.d(TAG, "onComplete:updateCameraConnectionState Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
    public void updateAllCameraConnectionState(int state) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.updateAllCameraConnectionState(state)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
    public void updateCameraConnectionState(int state) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.updateCameraConnectionState(state)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
    public void deleteSsid(String ssid) {
        isLoading.setValue(true);
        Completable.fromAction(() -> cameraInfoDao.delete(ssid)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
    public Flowable<List<WiFiHistory>> getCameraList() {
        return  cameraInfoDao.getCameraList();
    }
    public Single<String> getCameraName(String ssid) {
        return cameraInfoDao.getCameraName(ssid);
    }

    public Single<List<WiFiHistory>> getAll() {
        return cameraInfoDao.getAll();
    }
    public Single<WiFiHistory> checkSsidIsExit(String ssid) {
        return  cameraInfoDao.checkSsidIsExit(ssid);
    }
    public Maybe<WiFiHistory> checkSsidDiscoverable(String ssid) {
        Maybe<WiFiHistory> wiFiHistorySingle = null;
        try {
            wiFiHistorySingle = cameraInfoDao.checkSsidDiscoverable(ssid);
        } catch (EmptyResultSetException e) {
            e.printStackTrace();
        }
        return  wiFiHistorySingle;
    }
    public boolean hasAlreadyExistSSId(String ssid) {
        return cameraInfoDao.hasAlreadyExistSSId(ssid);
    }

    public void updateCameraName(String ssid, String cameraName) {
        cameraInfoDao.updateCameraName(ssid, cameraName);
    }
}
