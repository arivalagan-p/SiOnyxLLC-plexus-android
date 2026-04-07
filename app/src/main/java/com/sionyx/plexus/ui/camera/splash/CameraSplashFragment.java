package com.sionyx.plexus.ui.camera.splash;

import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCameraSplashBinding;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;

public class CameraSplashFragment extends Fragment {
    private static final String TAG = "CameraSplashFragment";
    private FragmentCameraSplashBinding binding;
    private HomeViewModel homeViewModel;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        binding = FragmentCameraSplashBinding.inflate(inflater, container, false);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* for this block back navigation */
            requireActivity().getOnBackPressedDispatcher().addCallback(requireActivity(), new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    onBackPressed();
                }
            });
    }

    private void onBackPressed() {
        if (binding.nightWaveSplashVideo.isPlaying()) {
            if (homeViewModel.getNavController().getCurrentDestination().getId() != R.id.cameraSplashFragment) {
                homeViewModel.getNavController().popBackStack();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            switch (currentCameraSsid) {
                case OPSIN:
                    binding.nightwaveSplash.setVisibility(View.GONE);
                    binding.opsinSplash.setVisibility(View.VISIBLE);
                    binding.opsinSplashVideo.setVideoURI(Uri.parse("android.resource://" + requireActivity().getPackageName() + "/" + R.raw.opsin_splash_landscape));
                    binding.opsinSplashVideo.start();
                    binding.opsinSplashVideo.setOnCompletionListener(mp -> homeViewModel.getNavController().navigate(R.id.cameraFragment));
                    break;
                case NIGHTWAVE:
                    binding.nightwaveSplash.setVisibility(View.VISIBLE);
                    binding.opsinSplash.setVisibility(View.GONE);
                    binding.nightWaveSplashVideo.setVideoURI(Uri.parse("android.resource://" + requireActivity().getPackageName() + "/" + R.raw.nightwave_splash_landscape));
                    binding.nightWaveSplashVideo.start();
                    CameraViewModel.setHasPressedSettingCancelIcon(false);
                    CameraViewModel.setHasPressedLiveViewButton(false);
                    binding.nightWaveSplashVideo.setOnCompletionListener(mp -> homeViewModel.getNavController().navigate(R.id.cameraFragment));
                    break;
                case NIGHTWAVE_DIGITAL:
                    Log.e(TAG,"onResume : NW_Digital");
                    //splash video will be updated for NW_Digital
                    binding.nightwaveSplash.setVisibility(View.VISIBLE);
                    binding.opsinSplash.setVisibility(View.GONE);
                    binding.nightWaveSplashVideo.setVideoURI(Uri.parse("android.resource://" + requireActivity().getPackageName() + "/" + R.raw.nw_digital_splash_landscape));
                    binding.nightWaveSplashVideo.start();
                    CameraViewModel.setHasPressedSettingCancelIcon(false);
                    CameraViewModel.setHasPressedLiveViewButton(false);
                    binding.nightWaveSplashVideo.setOnCompletionListener(mp -> homeViewModel.getNavController().navigate(R.id.cameraFragment));
                    break;
            }
        }
    }
}