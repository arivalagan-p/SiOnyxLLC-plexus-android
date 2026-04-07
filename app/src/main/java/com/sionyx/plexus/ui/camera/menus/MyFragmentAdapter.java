package com.sionyx.plexus.ui.camera.menus;


import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dome.librarynightwave.utils.Constants;
import com.sionyx.plexus.ui.camera.CameraViewModel;

public class MyFragmentAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public MyFragmentAdapter(@NonNull Fragment fragment, int tabCount) {
        super(fragment);
        this.tabCount = tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // here only new firmware to show settings screen on nightwave camera
                if (CameraViewModel.hasNewFirmware() && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE)) {
                    return new CameraSettingsInfoFragment();
                } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                    return new CameraSettingsInfoFragment();
                } else if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
                    //return setting fragment
                }
            case 1:
                return new CameraSettingsFragment();
            default:
                return null;
        }
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
