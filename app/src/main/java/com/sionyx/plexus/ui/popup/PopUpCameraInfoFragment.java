package com.sionyx.plexus.ui.popup;

import static com.dome.librarynightwave.model.repository.TCPRepository.isOpsinLiveStreamingStarted;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.sionyx.plexus.ui.camera.CameraViewModel.cameraXValue;
import static com.sionyx.plexus.ui.home.HomeViewModel.ScreenType.POP_UP_INFO_SCREEN;
import static com.sionyx.plexus.ui.home.HomeViewModel.screenType;
import static com.sionyx.plexus.utils.Constants.FPGA_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.OPSIN_STREAMING_SUPPORTS_FROM;
import static com.sionyx.plexus.utils.Constants.OVERLAY_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.RISCV_BASE_VERSION_TO_DISPLAY_TOP;
import static com.sionyx.plexus.utils.Constants.WIFI_BASE_VERSION_TO_DISPLAY_TOP;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.repository.opsinmodel.OpsinVersionDetails;
import com.dome.librarynightwave.model.repository.opsinmodel.SDCardInfo;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.CommandError;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.gson.Gson;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentPopUpCameraInfoBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.menus.CameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.ui.model.Manifest;

import java.io.InputStream;

public class PopUpCameraInfoFragment extends Fragment {
    private static final String TAG = "PopUpCameraInfoFragment";
    private FragmentPopUpCameraInfoBinding binding;
    public TCPConnectionViewModel tcpConnectionViewModel;
    private BleWiFiViewModel bleWiFiViewModel;
    private CameraInfoViewModel infoViewModel;
    private HomeViewModel homeViewModel;
    private LifecycleOwner lifecycleOwner;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPopUpCameraInfoBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        lifecycleOwner = this;
        bleWiFiViewModel = new ViewModelProvider(requireActivity()).get(BleWiFiViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(this).get(TCPConnectionViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        infoViewModel = new ViewModelProvider(this).get(CameraInfoViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (screenType == POP_UP_INFO_SCREEN) {
            infoViewModel.cameraSSId = bleWiFiViewModel.getConnectedSsidFromWiFiManager();
            subscribeUI();
        }
        homeViewModel.closePopupCameraInfoFragment.observe(lifecycleOwner, new com.sionyx.plexus.utils.EventObserver<>(vboolean -> {
            if (vboolean) {
                removeObservers();
            }
        }));
    }

    private void subscribeUI() {
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                hideOpsinVersionDetails();
                new Handler().postDelayed(() -> tcpConnectionViewModel.getCameraName(), 500);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getReleasePkgVer(), 1000);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getRiscVersion(), 1500);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getFPGAVersion(), 2000);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getCameraInfo(), 2500);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getWiFiPassword(), 3000);


            tcpConnectionViewModel.observeCameraName().observe(lifecycleOwner, cameraNameObserver);

            tcpConnectionViewModel.observeCameraPassword().observe(lifecycleOwner, cameraPasswordObserver);

            tcpConnectionViewModel.observeCameraInfo().observe(lifecycleOwner, cameraInfoObserver);

            tcpConnectionViewModel.observeFpgaVersion().observe(lifecycleOwner, cameraFpgaVersionObserver);

            tcpConnectionViewModel.observeRisvVersion().observe(lifecycleOwner, cameraRisvVersion);

            tcpConnectionViewModel.observeReleaseVersion().observe(lifecycleOwner, cameraReleaseVersion);

                break;
            case OPSIN: //Opsin
                new Handler().postDelayed(() -> tcpConnectionViewModel.getOpsinCameraName(), 100);

            binding.popSsid.setText(infoViewModel.cameraSSId);
            hideNightwaveVersionDetails();

            if (cameraXValue >= OPSIN_STREAMING_SUPPORTS_FROM) {
                binding.popSdCardInfoLayout.setVisibility(View.VISIBLE);
                binding.popSerialNumber.setVisibility(View.VISIBLE);
                binding.popSerialNumberLabel.setVisibility(View.VISIBLE);
                binding.popBleLabel.setVisibility(View.VISIBLE);
                binding.popBleVersion.setVisibility(View.VISIBLE);
                binding.popSdCardHeaderLabel.setVisibility(View.GONE);

                new Handler().postDelayed(() -> tcpConnectionViewModel.getOpsinSdCardInfomation(), 500);
                new Handler().postDelayed(() -> tcpConnectionViewModel.getSerialNumber(), 1000);
            } else {
                binding.popSdCardInfoLayout.setVisibility(View.GONE);
                binding.popSerialNumber.setVisibility(View.GONE);
                binding.popSerialNumberLabel.setVisibility(View.GONE);
                binding.popBleLabel.setVisibility(View.GONE);
                binding.popBleVersion.setVisibility(View.GONE);
            }

            homeViewModel.opsinPopupCameraName.observe(lifecycleOwner, name -> {
                Log.d(TAG, "subscribeUI: " + name);
                binding.popCameraName.setText(name);
            });

            homeViewModel.opsinPopupProductVersionDetails.observe(lifecycleOwner, objectEvent -> {
                if (objectEvent != null) {
                    OpsinVersionDetails opsinVersionDetails = (OpsinVersionDetails) objectEvent;
                    binding.popApplicationFw.setText(opsinVersionDetails.getRiscv());
                    binding.popFpgaFw.setText(opsinVersionDetails.getFpga());
                    binding.popIspVersion.setText(opsinVersionDetails.getIsp() != null && !opsinVersionDetails.getIsp().isEmpty() ? opsinVersionDetails.getIsp() : getString(R.string.unknown));
                    binding.popOverlayVersion.setText(opsinVersionDetails.getOverlay() != null && !opsinVersionDetails.getOverlay().isEmpty() ? opsinVersionDetails.getOverlay() : getString(R.string.unknown));
                    binding.popRecoveryVersion.setText(opsinVersionDetails.getRiscvRecovery() != null && !opsinVersionDetails.getRiscvRecovery().isEmpty() ? opsinVersionDetails.getRiscvRecovery() : getString(R.string.unknown));
                    String sCameraWiFiRtos = opsinVersionDetails.getWifi();
                    try {
                        if (sCameraWiFiRtos != null && !sCameraWiFiRtos.isEmpty()) {
                            String[] discardWiFiVersion = sCameraWiFiRtos.split("-");
                            binding.popSystemFw.setText(discardWiFiVersion[0]);
                        } else {
                            binding.popSystemFw.setText(requireContext().getString(R.string.unknown));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                   // binding.popSystemFw.setText(opsinVersionDetails.getWifi() != null && !opsinVersionDetails.getWifi().isEmpty() ? opsinVersionDetails.getWifi() : getString(R.string.unknown));
                    binding.popBleVersion.setText(opsinVersionDetails.getBle() != null && !opsinVersionDetails.getBle().isEmpty() ? opsinVersionDetails.getBle() : getString(R.string.unknown));
                    showTopVersionUiText(opsinVersionDetails);
                }
            });

            tcpConnectionViewModel.getOpsinSdCardInfo().observe(lifecycleOwner, opsinSdCardInfoObserver);
            tcpConnectionViewModel.observeOpsinSerialNumber().observe(lifecycleOwner, opsinSerialNumberObserver);

                isOpsinLiveStreamingStarted = true;// Just avoiding start live streaming during this fragment visible

                applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
                tcpConnectionViewModel.clearPeriodicRequestList();
                tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e(TAG,"subscribeUI : NW_Digital");
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private final EventObserver<Object> opsinSdCardInfoObserver = new EventObserver<>(sdCardInfo -> {
        if (sdCardInfo instanceof CommandError) {
            Log.e(TAG, "opsinSdCardInfo_Observer: " + ((CommandError) sdCardInfo).getError());
        } else {
            SDCardInfo sdCard = (SDCardInfo) sdCardInfo;
            binding.popSdCardHeaderLabel.setVisibility(View.VISIBLE);
            binding.popSdCardHeader.setText(sdCard.isMounted() ? requireContext().getString(R.string.mounted) : requireContext().getString(R.string.unmounted));
            if (sdCard.isMounted()) {
                binding.sdCardDetailsLayout.setVisibility(View.VISIBLE);
                binding.popFileCount.setText(String.valueOf(sdCard.getFileCount()));
                binding.popUsedSize.setText(formatKiloBytes(sdCard.getUsedSize()));
                binding.popAvailableSize.setText(formatKiloBytes(sdCard.getAvailableSize()));
                binding.popTotalSize.setText(formatKiloBytes(sdCard.getTotalSize()));
                binding.popUsedPercentage.setText(getSdCardUsePercentage(sdCard.getAvailableSize(), sdCard.getTotalSize()) + "%");
                //   Log.d(TAG, "opsinSdCardInfo_Observer: "+ sdCard.getAvailableSize() + "\n"+ sdCard.getTotalSize());
            } else {
                binding.sdCardDetailsLayout.setVisibility(View.GONE);
            }
        }
    });

    private int getSdCardUsePercentage(long availableSizeInKilobytes, long totalSizeInKilobytes) {
        return (int) (100 - ((double) (availableSizeInKilobytes) / (double) totalSizeInKilobytes) * 100);
    }

    public String formatKiloBytes(long size) {
        long Bytes = size * 1024; // convert kb to bytes
        return Formatter.formatFileSize(requireContext(), Bytes);
    }

    private final EventObserver<Object> opsinSerialNumberObserver = new EventObserver<>(serialNumber -> {
        if (serialNumber instanceof CommandError) {
            Log.e(TAG, "opsinSerialNumberObserver: " + ((CommandError) serialNumber).getError());
        } else {
            if (serialNumber != null) {
                String allowedCharacters = "^[a-zA-Z0-9!@#$%&*]*$";
                if (!serialNumber.toString().matches(allowedCharacters)) {
                    // If the entered text does not match the pattern, remove the last entered character
                    String filteredText = serialNumber.toString().replaceAll("[^a-zA-Z0-9!@#$%&*]", "");
                    if (filteredText.length() > 0)
                        binding.popSerialNumber.setText(filteredText.trim());
                    else
                        binding.popSerialNumber.setText("---");
                }
            }
        }
    });

    private final Observer<Object> cameraNameObserver = new Observer<Object>() {
        @Override
        public void onChanged(Object object) {
            if (object != null) {
                if (object instanceof CommandError) {
                    Log.e(TAG, "cameraName: " + ((CommandError) object).getError());
                    binding.popCameraName.setText(infoViewModel.cameraSSId);
                } else {
                    String response = object.toString();
                    if (!response.equals("")) {
                        binding.popCameraName.setText(response.trim());
                    } else {
                        Log.e(TAG, "onChanged: " + response + " " + infoViewModel.cameraSSId);
                        binding.popCameraName.setText(infoViewModel.cameraSSId);
                    }
                }
            }
        }
    };

    private final Observer<Object> cameraPasswordObserver = new Observer<Object>() {
        @Override
        public void onChanged(Object object) {
            if (object != null) {
                if (object instanceof CommandError) {
                    Log.e("TAG", " cameraPassword: " + ((CommandError) object).getError());
                    binding.popPassword.setText(getString(R.string.unknown));
                } else {
                    String response = object.toString();
                    if (!response.equals("")) {
                        binding.popPassword.setText(response.trim());
                    }
                }
            }
        }
    };

    private final Observer<Object> cameraInfoObserver = new Observer<Object>() {
        @Override
        public void onChanged(Object object) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeCameraInfo: " + ((CommandError) object).getError());
                binding.popMacAddress.setText(getString(R.string.unknown));
                binding.popSsid.setText(getString(R.string.unknown));
                binding.popSystemFw.setText(getString(R.string.unknown));
            } else {
                try {
                    String response = object.toString();
                    String cameraName = response.substring(0, 32);
                    String releaseVersion = response.substring(64, 80);
                    String wifiRtosVersion = response.substring(80, 128);
                    String wifiMac = response.substring(176, 194);
                    String ssid = response.substring(212, (int) response.length());
                    ssid = ssid.replace("\"", "");

                    binding.popMacAddress.setText(wifiMac);
                    binding.popSystemFw.setText(wifiRtosVersion);

                    String text = !cameraName.trim().equals("") ? cameraName : infoViewModel.cameraSSId;
                    if (!text.equals(""))
                        binding.popCameraName.setText(text);
                    else
                        binding.popCameraName.setText(infoViewModel.cameraSSId);

                    binding.popSsid.setText(ssid);
                } catch (Exception e) {
                    binding.popMacAddress.setText(getString(R.string.unknown));
                    binding.popSsid.setText(getString(R.string.unknown));
                    binding.popSystemFw.setText(getString(R.string.unknown));
                    e.printStackTrace();
                }
            }
        }
    };

    private final Observer<Object> cameraFpgaVersionObserver = new Observer<Object>() {
        @Override
        public void onChanged(Object object) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeFpgaVersion: " + ((CommandError) object).getError());
                binding.popFpgaFw.setText(getString(R.string.unknown));
            } else {
                try {
                    String response = object.toString();
                    binding.popFpgaFw.setText(response.trim());
                } catch (Exception e) {
                    binding.popFpgaFw.setText(getString(R.string.unknown));
                    e.printStackTrace();
                }
            }
        }
    };

    private final Observer<Object> cameraRisvVersion = new Observer<Object>() {
        @Override
        public void onChanged(Object object) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeFpgaVersion: " + ((CommandError) object).getError());
                binding.popApplicationFw.setText(getString(R.string.unknown));
            } else {
                try {
                    String response = object.toString();
                    binding.popApplicationFw.setText(response.trim());
                } catch (Exception e) {
                    binding.popApplicationFw.setText(getString(R.string.unknown));
                    e.printStackTrace();
                }
            }
        }
    };

    private final Observer<Object> cameraReleaseVersion = new Observer<Object>() {
        @Override
        public void onChanged(Object object) {
            if (object instanceof CommandError) {
                Log.e(TAG, "observeFpgaVersion: " + ((CommandError) object).getError());
                binding.popTopVerison.setText(getString(R.string.unknown));
            } else {
                try {
                    String response = object.toString();
                    binding.popTopVerison.setText(response.trim());
                } catch (Exception e) {
                    binding.popTopVerison.setText(getString(R.string.unknown));
                    e.printStackTrace();
                }
            }
        }
    };

    private void removeObservers() {
        tcpConnectionViewModel.observeCameraName().removeObserver(cameraNameObserver);
        tcpConnectionViewModel.observeCameraName().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeCameraPassword().removeObserver(cameraPasswordObserver);
        tcpConnectionViewModel.observeCameraPassword().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeCameraInfo().removeObserver(cameraInfoObserver);
        tcpConnectionViewModel.observeCameraInfo().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeFpgaVersion().removeObserver(cameraFpgaVersionObserver);
        tcpConnectionViewModel.observeFpgaVersion().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeRisvVersion().removeObserver(cameraRisvVersion);
        tcpConnectionViewModel.observeRisvVersion().removeObservers(lifecycleOwner);
        tcpConnectionViewModel.observeReleaseVersion().removeObserver(cameraReleaseVersion);
        tcpConnectionViewModel.observeReleaseVersion().removeObservers(lifecycleOwner);
//        tcpConnectionViewModel.getOpsinSdCardInfo().removeObserver(opsinSdCardInfoObserver);
//        tcpConnectionViewModel.getOpsinSdCardInfo().removeObservers(lifecycleOwner);
//        tcpConnectionViewModel.observeOpsinSerialNumber().removeObserver(opsinSerialNumberObserver);
//        tcpConnectionViewModel.observeOpsinSerialNumber().removeObservers(lifecycleOwner);
    }

    private void hideOpsinVersionDetails() {
        binding.popIspVersionTitle.setVisibility(View.GONE);
        binding.popIspVersion.setVisibility(View.GONE);
        binding.popRecoveryVersionTitle.setVisibility(View.GONE);
        binding.popRecoveryVersion.setVisibility(View.GONE);
        binding.popOverlayVersion.setVisibility(View.GONE);
        binding.popOverlayVersionTitle.setVisibility(View.GONE);
        binding.sdCardDetailsLayout.setVisibility(View.GONE);
        binding.popSdCardInfoLayout.setVisibility(View.GONE);
        binding.popSerialNumber.setVisibility(View.GONE);
        binding.popSerialNumberLabel.setVisibility(View.GONE);
        binding.popBleLabel.setVisibility(View.GONE);
        binding.popBleVersion.setVisibility(View.GONE);
    }

    private void hideNightwaveVersionDetails() {
        binding.popPasswordLabel.setVisibility(View.GONE);
        binding.popPassword.setVisibility(View.GONE);
        binding.popMacAddressLabel.setVisibility(View.GONE);
        binding.popMacAddress.setVisibility(View.GONE);
        binding.popTopVerisonLabel.setVisibility(View.GONE);
        binding.popTopVerison.setVisibility(View.GONE);
    }

    private Manifest readManifest() {
        String tContents = "";
        Manifest manifest = null;
        try {
            InputStream stream = null;
            MainActivity activity = ((MainActivity) getActivity());
            if (activity != null) {
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        stream = activity.getAssets().open("nightwave/manifest.txt");
                        break;
                    case OPSIN:
                        stream = activity.getAssets().open("opsin/manifest.txt");
                        break;
                    case NIGHTWAVE_DIGITAL:
                        Log.e(TAG,"readManifest : NW_Digital");
                        break;
                }
            }

            int size = stream.available();
            byte[] buffer = new byte[size];
            int read = stream.read(buffer);
            stream.close();
            if (read > 0) {
                tContents = new String(buffer);
                Gson gson = new Gson();
                manifest = gson.fromJson(tContents, Manifest.class);
                Log.e(TAG, "readManifest: " + tContents);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return manifest;
    }

    private String readManifestFile() {
        Manifest manifest = readManifest();
        return manifest.getVersions().getRelease();
    }

    private void showTopVersionUiText(OpsinVersionDetails opsinVersionDetails) {
        try {
            String fpga = opsinVersionDetails.getFpga();
            String wifi = opsinVersionDetails.getWifi();
            String riscv = opsinVersionDetails.getRiscv();
            String cameraOverlay = opsinVersionDetails.getOverlay();

            int fpgaComparisonResult = -1;
            int wifiComparisonResult = -1;
            int riscvComparisonResult = -1;
            int overlayComparisonResult = -1;
            boolean isEngineeringFirmware = false;

            if (fpga != null) {
                fpgaComparisonResult = compareVersions(fpga, FPGA_BASE_VERSION_TO_DISPLAY_TOP);
            }

            if (wifi != null && containsFourthDigit(wifi)) {
                String removedWifi = removeFourthDigit(wifi);
                wifiComparisonResult = compareVersions(removedWifi, WIFI_BASE_VERSION_TO_DISPLAY_TOP);
                isEngineeringFirmware = true;
            } else if (wifi != null) {
                wifiComparisonResult = compareVersions(wifi, WIFI_BASE_VERSION_TO_DISPLAY_TOP);
            }

            if (riscv != null && containsFourthDigit(riscv)) {
                String removedRiscv = removeFourthDigit(riscv);
                riscvComparisonResult = compareVersions(removedRiscv, RISCV_BASE_VERSION_TO_DISPLAY_TOP);
                isEngineeringFirmware = true;
            } else if (riscv != null) {
                riscvComparisonResult = compareVersions(riscv, RISCV_BASE_VERSION_TO_DISPLAY_TOP);
            }

            if (cameraOverlay != null && containsFourthDigit(cameraOverlay)) {
                String removedCameraOverlay = removeFourthDigit(cameraOverlay);
                overlayComparisonResult = compareVersions(removedCameraOverlay, OVERLAY_BASE_VERSION_TO_DISPLAY_TOP);
                isEngineeringFirmware = true;
            } else if (cameraOverlay != null) {
                overlayComparisonResult = compareVersions(cameraOverlay, OVERLAY_BASE_VERSION_TO_DISPLAY_TOP);
            }


            binding.popTopVerison.setVisibility(View.VISIBLE);
            binding.popTopVerisonLabel.setVisibility(View.VISIBLE);
            if (fpgaComparisonResult >= 0 && wifiComparisonResult >= 0 && riscvComparisonResult >= 0 && overlayComparisonResult >= 0) {
                //-1 lesser, 1 greater, 0 equal
                Manifest manifest = readManifest();
                String topVersion = manifest.getVersions().getRelease();
                String manifestRiscv = manifest.getVersions().getRiscv();
                String manifestFpga = manifest.getVersions().getFpga();
                String manifestWifi = manifest.getVersions().getWiFiRtos();
                String manifestOverlay = manifest.getVersions().getOverlay();
                String manifestBle = manifest.getVersions().getwIFI_BLE();

                if (topVersion != null && manifestFpga.equals(fpga) && manifestRiscv.equals(riscv) && manifestWifi.equals(wifi) && manifestOverlay.equals(cameraOverlay)) {
                    if (isEngineeringFirmware) {
                        binding.popTopVerison.setText(topVersion);
                    } else {
                        String[] splitTopVersion = topVersion.split("-");
                        binding.popTopVerison.setText(splitTopVersion[0]);
                    }
                } else {
                    binding.popTopVerison.setText("-");
                }
            } else {
                binding.popTopVerison.setText("-");
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public boolean containsFourthDigit(String version) {
        String[] parts = version.split("\\.");

        return parts.length >= 4;
    }

    public String removeFourthDigit(String version) {
        String[] parts = version.split("\\.");

        if (parts.length >= 4) {
            parts[3] = ""; // Remove the fourth part
            return String.join(".", parts);
        } else {
            return version; // No fourth part, return original version
        }
    }

    public int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = Integer.parseInt(parts1[i]);
            int num2 = Integer.parseInt(parts2[i]);
            if (num1 < num2) {
                return -1;
            } else if (num1 > num2) {
                return 1;
            }
        }

        if (parts1.length < parts2.length) {
            return -1;
        } else if (parts1.length > parts2.length) {
            return 1;
        } else {
            return 0;
        }
    }

}