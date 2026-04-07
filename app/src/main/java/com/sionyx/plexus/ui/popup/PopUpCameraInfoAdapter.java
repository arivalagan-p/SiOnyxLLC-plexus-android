package com.sionyx.plexus.ui.popup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class PopUpCameraInfoAdapter extends FragmentStateAdapter {
    private final ArrayList<Fragment> fragmentArrayList;

    public PopUpCameraInfoAdapter(@NonNull Fragment fragment, ArrayList<Fragment> fragmentArrayList) {
        super(fragment);
        this.fragmentArrayList = fragmentArrayList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentArrayList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentArrayList.size();
    }
}
