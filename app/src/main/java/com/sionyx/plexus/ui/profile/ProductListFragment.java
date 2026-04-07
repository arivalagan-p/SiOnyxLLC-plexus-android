package com.sionyx.plexus.ui.profile;

import static android.content.Context.MODE_PRIVATE;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_MANAGE_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_RECORDED_VIDEO_PLAYER_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.GALLERY_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_SETTINGS_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.utils.Constants.capitalizeFirstLetter;
import static com.sionyx.plexus.utils.Constants.makeToast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentProductListBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.api.interfaces.ProfileInterface;
import com.sionyx.plexus.ui.api.requestModel.RequestProfileDeviceModel;
import com.sionyx.plexus.ui.api.responseModel.ProfileDevices;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.EventObserver;
import com.sionyx.plexus.utils.NetworkUtils;

import java.util.ArrayList;

public class ProductListFragment extends Fragment {
    private static final String TAG = "ProductListFragment";
    private ProfileViewModel profileViewModel;
    private HomeViewModel homeViewModel;

    private FragmentProductListBinding binding;
    private LifecycleOwner lifecycleOwner;
    private QRCodeScanAdapter qrCodeScanAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        lifecycleOwner = this;
        binding.setViewModel(profileViewModel);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscribeUI();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void subscribeUI() {
        if (profileViewModel.getProfileDeviceArrayList != null && profileViewModel.getProfileDeviceArrayList.size() > 0) {
            binding.qrRecyclerItemView.setVisibility(View.VISIBLE);
            binding.qrRecyclerItemViewEmpty.setVisibility(View.GONE);
            qrCodeScanAdapter = new QRCodeScanAdapter(requireContext(), profileViewModel.getProfileDeviceArrayList, profileViewModel);
            binding.qrRecyclerItemView.setAdapter(qrCodeScanAdapter);
        } else {
            binding.qrRecyclerItemViewEmpty.setVisibility(View.VISIBLE);
            binding.qrRecyclerItemView.setVisibility(View.GONE);
        }
        if (isAdded())
            getListOfProfileDevice();

        profileViewModel.isSelectQrCodeScanner.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (binding.contentLoadingProgressbar.getVisibility() != View.VISIBLE) {
                    profileViewModel.setDialogShowing(false); // for this scanner view show dialog view value reset
                    profileViewModel.setQrScanModel(null);
                    homeViewModel.getNavController().navigate(R.id.productScanFragment);
                }
            }
        }));

        profileViewModel.isLoadingProgressbar.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                binding.contentLoadingProgressbar.setVisibility(View.VISIBLE);
            } else {
                binding.contentLoadingProgressbar.setVisibility(View.GONE);
            }
        }));

        profileViewModel.isCancelProductListView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                onBackPressed();
            }
        }));

        profileViewModel.isSelectDeleteProductItem.observe(lifecycleOwner, new EventObserver<>(deleteProductModel -> {
            if (deleteProductModel != null) {
                if (binding.contentLoadingProgressbar.getVisibility() != View.VISIBLE) {
                    new Handler().post(() -> {
                        NetworkUtils.pingServer(isSuccessful -> {
                            if (isSuccessful) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    profileViewModel.setDeleteProductModel(deleteProductModel);
                                    showProductDeleteAlertDialog(deleteProductModel.getProfileDevices(), "False");
                                });
                            } else {
                                showInternetConnectionFailedDialog();
                            }
                        });
                    });
                }
            }
        }));

        profileViewModel.isDeleteProductOnAWS.observe(lifecycleOwner, new EventObserver<>(deleteProductModel -> {
            if (deleteProductModel != null) {
                profileViewModel.hasShowProgressbar(true);
                try {
                    String loginUsername = getLoginUsername();
                    RequestProfileDeviceModel requestProfileDeviceModel = new RequestProfileDeviceModel();
                    if (loginUsername != null) {
                        requestProfileDeviceModel.setUserName(loginUsername);
                        String cameraName = deleteProductModel.getProfileDevices().getCamera() != null ? deleteProductModel.getProfileDevices().getCamera() : "Nightwave";
                        requestProfileDeviceModel.setCamera(capitalizeFirstLetter(cameraName));
                        requestProfileDeviceModel.setModel(deleteProductModel.getProfileDevices().getModel());
                        requestProfileDeviceModel.setSerialNumber(deleteProductModel.getProfileDevices().getSerialNumber());
                        requestProfileDeviceModel.setsKU(deleteProductModel.getProfileDevices().getsKU());
                        requestProfileDeviceModel.setClassification(deleteProductModel.getProfileDevices().getClassification());
                        Log.d(TAG, "onDialogDeleteProductOkClick: " + deleteProductModel.getProfileDevices().model);
                    }

                    // call api
                    profileViewModel.deleteProductOnAws(requestProfileDeviceModel, new ProfileInterface() {
                        @Override
                        public void onSuccess(String message, ArrayList<ProfileDevices> profileDevicesArrayList) {
                            if (message != null) {
                                if (profileViewModel.getProfileDeviceArrayList != null && !profileViewModel.getProfileDeviceArrayList.isEmpty()) {
                                    ProfileDevices profileDevices = new ProfileDevices();
                                    profileDevices.setCamera(deleteProductModel.getProfileDevices().getCamera());
                                    profileDevices.setModel(deleteProductModel.getProfileDevices().getModel());
                                    profileDevices.setClassification(deleteProductModel.getProfileDevices().getClassification());
                                    profileDevices.setsKU(deleteProductModel.getProfileDevices().getsKU());
                                    profileDevices.setSerialNumber(deleteProductModel.getProfileDevices().getSerialNumber());

                                    profileViewModel.getProfileDeviceArrayList.remove(deleteProductModel.getItemPosition());
                                    qrCodeScanAdapter.notifyDataSetChanged();
                                }
                                makeToast(getString(R.string.the_product_details_have_been_successfully_deleted));
                                profileViewModel.hasShowProgressbar(false);
                                /// here also remove local array list that item and update view
                                profileViewModel.setDeleteProductModel(null);
                                if (!profileViewModel.getProfileDeviceArrayList.isEmpty()) {
                                    binding.qrRecyclerItemViewEmpty.setVisibility(View.GONE);
                                    binding.qrRecyclerItemView.setVisibility(View.VISIBLE);
                                }
                                else {
                                    binding.qrRecyclerItemViewEmpty.setVisibility(View.VISIBLE);
                                    binding.qrRecyclerItemView.setVisibility(View.GONE);
                                }
                            }
                        }

                        @Override
                        public void onFailure(String error, String message) {
                            profileViewModel.hasShowProgressbar(false);
                            profileViewModel.setDeleteProductModel(null);
                            if (message != null)
                                Toast.makeText(requireContext(), "" + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));

        activity.getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    onBackPressed();
                }
            });
    }

    private void onBackPressed() {
        if (screenType == GALLERY_SCREEN || screenType == GALLERY_MANAGE_SCREEN || screenType == GALLERY_RECORDED_VIDEO_INFO_SCREEN || screenType == GALLERY_RECORDED_VIDEO_PLAYER_SCREEN) {
            Log.e(TAG, "showHomeScreen: " + screenType.name());
            homeViewModel.onCancelGalleryView();
        }
        if (screenType == INFO_SCREEN) {
            Log.e(TAG, "showHomeScreen: " + screenType.name());
            homeViewModel.onCancelInfoView();
        }
        if (screenType == HomeViewModel.ScreenType.ADD_SCREEN) {
            homeViewModel.cancelNearByDeviceView();
        }
        if (screenType == POP_UP_INFO_SCREEN || screenType == POP_UP_SETTINGS_SCREEN)
            homeViewModel.onPopUpViewCancel();

        homeViewModel.getNavController().navigate(R.id.homeFragment);
    }

    private void showProductDeleteAlertDialog(ProfileDevices profileDevices, String isResultSuccess) {
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.DELETE_PRODUCT_ALERT_DIALOG;
            activity.showDialogLoginScreen(profileDevices.getModel(), isResultSuccess);
        }
    }

    private void showInternetConnectionFailedDialog() {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.NO_INTERNET_CONNECTION_DIALOG;
            activity.showDialogLoginScreen("Network Unavailable", "");
        }
    }

    private void getListOfProfileDevice() {
        try {
            String loginUsername = getLoginUsername();
            if (loginUsername != null) {
                profileViewModel.hasShowProgressbar(true);
                binding.profileName.setText(loginUsername);
                profileViewModel.getListOfProductDevices(loginUsername, new ProfileInterface() {
                    @Override
                    public void onSuccess(String message, ArrayList<ProfileDevices> profileDevices) {
                        profileViewModel.hasShowProgressbar(false);
                        if (profileDevices != null && !profileDevices.isEmpty()) {
                            binding.qrRecyclerItemView.setVisibility(View.VISIBLE);
                            binding.qrRecyclerItemViewEmpty.setVisibility(View.GONE);
                            qrCodeScanAdapter = new QRCodeScanAdapter(requireContext(), profileDevices, profileViewModel);
                            binding.qrRecyclerItemView.setAdapter(qrCodeScanAdapter);
                            profileViewModel.getProfileDeviceArrayList = profileDevices;
                        } else {
                            binding.qrRecyclerItemViewEmpty.setVisibility(View.VISIBLE);
                            binding.qrRecyclerItemView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(String error, String message) {
                        profileViewModel.hasShowProgressbar(false);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLoginUsername() {
        String userName = null;
        try {
            if (activity != null) {
                SharedPreferences loginSharedPreferences = activity.getSharedPreferences("LoginState", MODE_PRIVATE);
                userName = loginSharedPreferences.getString("userName", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userName;
    }

    MainActivity activity = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }
}