package com.dome.librarynightwave.model.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.persistence.DomeRoomDatabase;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistoryDao;


import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DigitalCameraInfoRepository {

    private static final String TAG = "DigitalCameraInfoRepository";
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private static DigitalCameraInfoRepository digitalCameraInfoRepository;
    private final NightwaveDigitalWiFiHistoryDao nwdCameraInfoDao;


    public static DigitalCameraInfoRepository getInstance(Application application) {
        if (digitalCameraInfoRepository == null) {
            digitalCameraInfoRepository = new DigitalCameraInfoRepository(application);
        }
        return digitalCameraInfoRepository;
    }

    public DigitalCameraInfoRepository(Application application) {
        DomeRoomDatabase db = DomeRoomDatabase.getInstance(application);
        nwdCameraInfoDao = db.nwdCameraInfoDao();
    }

    //Get Loading State
    public MutableLiveData<Boolean> getIsLoading(){
        return isLoading;
    }

    public Single<NightwaveDigitalWiFiHistory> checkNWDSsidIsExit(String ssid) {
        return  nwdCameraInfoDao.checkSsidIsExit(ssid);
    }

    public Single<String> getCameraPassword(String ssid) {
        return nwdCameraInfoDao.getCameraPassword(ssid);
    }

    /* NWD Camera Info*/
    public void insertNWDCamera(NightwaveDigitalWiFiHistory cameraInfo) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.insertCamera(cameraInfo)).subscribeOn(Schedulers.io())
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

    public void updateLastFWDisplayDate(String ssid, long lastDisplayDate) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.updateLastFWDisplayDate(ssid,lastDisplayDate)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateLastFWDisplayDate Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }

    public void deleteNWDSsid(String ssid) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.delete(ssid)).subscribeOn(Schedulers.io())
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

    public void updateCameraAutoConnectStateAndPassword(String ssid, String pwd,int state) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.updateCameraConnectionStateAndPassword(ssid,pwd,state)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraAutoConnectStateAndPassword Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }

    public void updateCameraAutoConnect(String ssid, int state) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.updateCameraAutoConnect(ssid,state)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraAutoConnect Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }

    public void updateCameraPassword(String ssid, String pwd) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.updateCameraPassword(ssid,pwd)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraPassword Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }

    public void updateCameraFirmwareVersion(String ssid, String firmware) {
        isLoading.setValue(true);
        Completable.fromAction(() -> nwdCameraInfoDao.updateCameraFirmwareVersion(ssid,firmware)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: Called" + " " + d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete:updateCameraFirmwareVersion Called ");
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }
                });
    }
}
