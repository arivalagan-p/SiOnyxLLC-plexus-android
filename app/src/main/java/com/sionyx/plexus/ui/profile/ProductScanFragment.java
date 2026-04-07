package com.sionyx.plexus.ui.profile;

import static com.sionyx.plexus.utils.Constants.QR_CODE_NIGHTWAVE_PREFIX;
import static com.sionyx.plexus.utils.Constants.QR_CODE_OPSIN_PREFIX;
import static com.sionyx.plexus.utils.Constants.makeToast;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentProductScanBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.EventObserver;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductScanFragment extends Fragment {
    private static final String TAG = "QRCodeScannerFragment";
    private ExecutorService cameraExecutor;

    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    private FragmentProductScanBinding binding;

    ObjectAnimator animator; // Move from top to bottom

    private ProfileViewModel profileViewModel;
    private HomeViewModel homeViewModel;
    LifecycleOwner lifecycleOwner;
    private ActivityResultLauncher<String> qrScannerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductScanBinding.inflate(inflater, container, false);
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding.setViewModel(profileViewModel);
        lifecycleOwner = this;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscribeUI();
    }

    private void subscribeUI() {
        drawBarcodeOutline();
        cameraExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (qrScannerLauncher == null)
                createQrCodeScanLauncher();
            qrScannerLauncher.launch(Manifest.permission.CAMERA);
        } else {
            setupCamera();
        }

        profileViewModel.isDeleteQrScanResult.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            profileViewModel.setQrScanModel(null);
                            profileViewModel.setDialogShowing(false);
                            binding.textureView.setVisibility(View.VISIBLE);
                            binding.scanningOverlay.setVisibility(View.VISIBLE);
                            binding.outlineView.setVisibility(View.VISIBLE);
                            binding.outlineView.setOutlineColor(Color.RED);
                            if (isAdded()) {
                                setupCamera();
                                startScanningAnimation(binding.scanningOverlay);
                            }
                        });
                    }
                }, 500);
            }
        }));

        profileViewModel.isBackToProductList.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                // Stop the camera preview
                if (previewUseCase != null) {
                    previewUseCase.setSurfaceProvider(null);
                    profileViewModel.setDialogShowing(false);
                    // Release the camera resources
                    cameraProvider.unbindAll();
                    stopScanningAnimation();
                    homeViewModel.getNavController().popBackStack();
                }
            }
        }));

        profileViewModel.isFailedUploadProduct.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            profileViewModel.setQrScanModel(null);
                            profileViewModel.setDialogShowing(false);
                            binding.textureView.setVisibility(View.VISIBLE);
                            binding.scanningOverlay.setVisibility(View.VISIBLE);
                            binding.outlineView.setVisibility(View.VISIBLE);
                            binding.outlineView.setOutlineColor(Color.RED);
                            if (isAdded()) {
                                setupCamera();
                                startScanningAnimation(binding.scanningOverlay);
                            }
                        });
                    }
                }, 500);
            }
        }));
    }

    private void createQrCodeScanLauncher() {
        qrScannerLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        setupCamera();
                    } else {
                        makeToast("Permission Denied");
                    }
                });
    }

    private void setupCamera() {
        if (!profileViewModel.isDialogShowing()) {
            binding.textureView.setVisibility(View.VISIBLE);
            final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(requireContext());

            int lensFacing = CameraSelector.LENS_FACING_BACK;
            cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindAllCameraUseCases();
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "cameraProviderFuture.addListener Error", e);
                }
            }, ContextCompat.getMainExecutor(requireContext()));
        } else {
            binding.textureView.setVisibility(View.GONE);
            binding.outlineView.setVisibility(View.GONE);
            binding.scanningOverlay.setVisibility(View.GONE);
        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        builder.setTargetRotation(getRotation());
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(binding.textureView.getSurfaceProvider());
        binding.scanningOverlay.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pure_red));
        startScanningAnimation(binding.scanningOverlay);
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind preview", e);
        }
    }

    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }

        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        Executor cameraExecutor = Executors.newSingleThreadExecutor();
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setTargetRotation(getRotation());
        builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);
        analysisUseCase = builder.build();
        analysisUseCase.setAnalyzer(cameraExecutor, this::analyze);
        binding.scanningOverlay.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pure_red));
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
        } catch (Exception e) {
            Log.e(TAG, "Error when bind analysis", e);
        }
    }

    protected int getRotation() {
        int rotation = 0;
        if (binding.textureView.getDisplay() != null) {
            rotation = binding.textureView.getDisplay().getRotation();
            Log.d(TAG, "getRotation: " + rotation);
            return rotation;
        }
        return rotation;
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy image) {
        if (image.getImage() == null) return;

        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(),
                image.getImageInfo().getRotationDegrees()
        );

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_DATA_MATRIX // Include Data Matrix format
                        )
                        .build();

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(this::onSuccessListener)
                .addOnFailureListener(e -> Log.e(TAG, "Barcode process failure", e))
                .addOnCompleteListener(task -> {
                            image.close();
                        }
                );
    }

    private void onSuccessListener(List<Barcode> barcodes) {
        if (activity != null) {
            if (!barcodes.isEmpty()) {
                activity.runOnUiThread(() -> {
                    if (isAdded()) {
                        binding.outlineView.setOutlineColor(Color.GREEN);
                        binding.scanningOverlay.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green));
                    }
                });
                QRScanModel qrScanModel = new QRScanModel();
                for (Barcode barcode : barcodes) {
                    Log.d(TAG, "Barcode Value: " + barcode.getRawValue());
                    binding.scanningOverlay.setRect(0, 0, 0, 0);
                    qrScanModel.setModel(barcode.getRawValue());
                }
                String[] lines = qrScanModel.getModel().split("\\n");
                // Iterate through each line
                try {
                    for (String line : lines) {
                        // Split each line by colon to separate key-value pairs
                        String[] keyValue = line.split(":");
                        if (keyValue.length == 2) { // Ensure it's a valid key-value pair
                            String key = keyValue[0].trim(); // Trim leading and trailing spaces from key
                            String value = keyValue[1].trim(); // Trim leading and trailing spaces from value
                            switch (key) {
                                case "Description":
                                    qrScanModel.setDescription(value);
                                case "Model":
                                    String description = qrScanModel.getDescription() != null ? qrScanModel.getDescription() : "NightWave";
                                    qrScanModel.setModel(description + " - " + value);
                                    break;
                                case "SKU":
                                    qrScanModel.setSku(value);
                                    break;
                                case "UPC":
                                    qrScanModel.setUniversalProductCode(value);
                                    break;
                                case "Classification":
                                    qrScanModel.setClassification(value);
                                    break;
                                case "SN":
                                case "S#":
                                    qrScanModel.setSerialNumber(value);
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "onSuccessListener: " + e.getMessage());
                }
                if (qrScanModel.getModel().contains(QR_CODE_OPSIN_PREFIX) || qrScanModel.getModel().contains(QR_CODE_NIGHTWAVE_PREFIX)) {
                    profileViewModel.setQrScanModel(qrScanModel); /// save to model class
                    new Handler().post(() -> {
                        previewUseCase.setSurfaceProvider(null);
                        // Release the camera resources
                        cameraProvider.unbindAll();
                        stopScanningAnimation();
                        binding.textureView.setVisibility(View.GONE);
                        binding.outlineView.setVisibility(View.GONE);
                        showQRCodeScanResultDialog("True");
                        profileViewModel.setDialogShowing(true);
                    });
                } else {
                    previewUseCase.setSurfaceProvider(null);
                    // Release the camera resources
                    cameraProvider.unbindAll();
                    stopScanningAnimation();
                    binding.textureView.setVisibility(View.GONE);
                    binding.outlineView.setVisibility(View.GONE);
                    showQRCodeScanResultDialog("False");
                    profileViewModel.setDialogShowing(true);
                }
            } else {
                binding.outlineView.setOutlineColor(Color.RED);
                if (isAdded())
                    binding.scanningOverlay.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pure_red));
            }
        }
    }

    private void showQRCodeScanResultDialog(String isResultSuccess) {
        if (activity != null) {
            activity.showDialogLoginScreen = MainActivity.ShowDialogLoginScreen.QR_CODE_SCAN_RESULT_DIALOG;
            activity.showDialogLoginScreen("Device Details", isResultSuccess);
        }
    }

    private void startScanningAnimation(View scanningOverlay) {
        animator = ObjectAnimator.ofFloat(
                scanningOverlay, "translationY",
                0, binding.textureView.getHeight() - 1);
        animator.setDuration(2000); // Adjust duration as needed
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    private void stopScanningAnimation() {
        if (animator != null && animator.isStarted()) {
            animator.cancel();
            binding.scanningOverlay.setVisibility(View.GONE);
        }
    }

    private void drawBarcodeOutline() {
        // Calculate the dimensions of the outline
        int outlineWidth = binding.textureView.getWidth();
        int outlineHeight = binding.textureView.getHeight();
        // Set the dimensions of the outline
        binding.outlineView.getLayoutParams().width = outlineWidth;
        binding.outlineView.getLayoutParams().height = outlineHeight;
        binding.outlineView.requestLayout();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }


    MainActivity activity = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }
}