package com.sionyx.plexus.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sionyx.plexus.ui.api.AWSCommunication;
import com.sionyx.plexus.ui.api.interfaces.ProductUploadResultCallback;
import com.sionyx.plexus.ui.api.interfaces.ProfileDevicesResultCallback;
import com.sionyx.plexus.ui.api.interfaces.ProfileInterface;
import com.sionyx.plexus.ui.api.requestModel.RequestProfileDeviceModel;
import com.sionyx.plexus.ui.api.responseModel.GetProfileResponse;
import com.sionyx.plexus.ui.api.responseModel.ProfileDevices;
import com.sionyx.plexus.utils.Event;

import java.util.ArrayList;

public class ProfileViewModel extends AndroidViewModel {

    /* for this qr code scanned result show in dialog now rotate device again visible qr scanner in bg so, avoid to hide qr scanner use this variable*/
    public boolean isDialogShowing;
    public boolean isAlreadyExistQRData;

    public boolean isAlreadyExistQRData() {
        return isAlreadyExistQRData;
    }

    public void setAlreadyExistQRData(boolean alreadyExistQRData) {
        isAlreadyExistQRData = alreadyExistQRData;
    }

    public boolean isDialogShowing() {
        return isDialogShowing;
    }

    public void setDialogShowing(boolean dialogShowing) {
        isDialogShowing = dialogShowing;
    }

    /* for this save after scanned result upload to aws server and delete from from server also update ui in locally*/
    public ArrayList<ProfileDevices> getProfileDeviceArrayList = new ArrayList<>();

    /* for this delete icon press currently selected item details hold*/
    public DeleteProductModel deleteProductModel = new DeleteProductModel();

    public DeleteProductModel getDeleteProductModel() {
        return deleteProductModel;
    }

    public void setDeleteProductModel(DeleteProductModel deleteProductModel) {
        this.deleteProductModel = deleteProductModel;
    }
    public ProfileViewModel(@NonNull Application application) {
        super(application);
    }

    private final MutableLiveData<Event<Boolean>> _isSelectQrCodeScanner = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isSelectQrCodeScanner = _isSelectQrCodeScanner;

    private final MutableLiveData<Event<Boolean>> _isDeleteQrScanResult = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDeleteQrScanResult = _isDeleteQrScanResult;

    private final MutableLiveData<Event<Boolean>> _isBackToProductList = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isBackToProductList = _isBackToProductList;

    private final MutableLiveData<Event<Boolean>> _isCancelProductListView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancelProductListView = _isCancelProductListView;

    /* for this alert dialog ok clicked event observe*/
    private final MutableLiveData<Event<DeleteProductModel>> _isDeleteProductOnAWS = new MutableLiveData<>();
    public LiveData<Event<DeleteProductModel>> isDeleteProductOnAWS = _isDeleteProductOnAWS;

    /* for this delete icon press event observe*/
    private final MutableLiveData<Event<DeleteProductModel>> _isSelectDeleteProductItem= new MutableLiveData<>();
    public LiveData<Event<DeleteProductModel>> isSelectDeleteProductItem = _isSelectDeleteProductItem;

    private final MutableLiveData<Event<Boolean>> _isFailedUploadProduct = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isFailedUploadProduct = _isFailedUploadProduct;

    private final MutableLiveData<Event<Boolean>> _isLoadingProgressbar = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isLoadingProgressbar = _isLoadingProgressbar;

    /* for this after scan result success hold scan data for sent to aws service*/
    private QRScanModel qrScanModel = new QRScanModel();

    public QRScanModel getQrScanModel() {
        return qrScanModel;
    }

    public void setQrScanModel(QRScanModel qrScanModel) {
        this.qrScanModel = qrScanModel;
    }

    public void onSelectQrCodeScanner() {
        _isSelectQrCodeScanner.postValue(new Event<>(true));
    }

    public void deleteProductItem(DeleteProductModel deleteProductModel) {
        _isSelectDeleteProductItem.postValue(new Event<>(deleteProductModel));
    }

    public void deleteProductOnAws(DeleteProductModel deleteProductModel) {
        _isDeleteProductOnAWS.postValue(new Event<>(deleteProductModel));
    }

    public void hasShowProgressbar(boolean hasShowProgressbar) {
        _isLoadingProgressbar.postValue(new Event<>(hasShowProgressbar));
    }

    public void hasFailedUploadProduct() {
        _isFailedUploadProduct.postValue(new Event<>(true));
    }

    public void deleteQrScanResult() {
        _isDeleteQrScanResult.postValue(new Event<>(true));
    }

    public void onBackProductList() {
        _isBackToProductList.postValue(new Event<>(true));
    }

    public void onCancelProductListView() {
        _isCancelProductListView.postValue(new Event<>(true));
    }

    private AWSCommunication awsCommunication = null;

    private AWSCommunication getAwsCommunication() {
        if (awsCommunication == null) {
            awsCommunication = new AWSCommunication();
        }
        return awsCommunication;
    }

    public void getListOfProductDevices(String userName, ProfileInterface profileInterface) {
        getAwsCommunication().getListOfProductDevices(userName, new ProfileDevicesResultCallback() {
            @Override
            public void onSuccess(GetProfileResponse getProfileResponse) {
                if (getProfileResponse != null) {
                    profileInterface.onSuccess("", getProfileResponse.getProfileDevices());
                }
            }

            @Override
            public void onFailure(String error, String message) {
                profileInterface.onFailure(error, message);
            }
        });
    }

    public void uploadProductToAws(RequestProfileDeviceModel profileDeviceModel, ProfileInterface profileInterface) {
        getAwsCommunication().uploadProductToAws(profileDeviceModel, new ProductUploadResultCallback() {
            @Override
            public void onSuccess(String forgotPasswordResponse) {
                if (forgotPasswordResponse != null) {
                    profileInterface.onSuccess(forgotPasswordResponse, null);
                }
            }

            @Override
            public void onFailure(String error, String message) {
                profileInterface.onFailure(error, message);
            }
        });
    }

    public void deleteProductOnAws(RequestProfileDeviceModel profileDeviceModel, ProfileInterface profileInterface) {
        getAwsCommunication().deleteProductOnAws(profileDeviceModel, new ProductUploadResultCallback() {
            @Override
            public void onSuccess(String forgotPasswordResponse) {
                if (forgotPasswordResponse != null) {
                    profileInterface.onSuccess(forgotPasswordResponse, null);
                }
            }

            @Override
            public void onFailure(String error, String message) {
                profileInterface.onFailure(error, message);
            }
        });
    }


}
