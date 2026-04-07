package com.sionyx.plexus.ui.home.gallerybottomview.bottomtabviewadapter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomAllFragment;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomImageFragment;
import com.sionyx.plexus.ui.home.gallerybottomview.GalleryBottomVideoFragment;

import java.util.ArrayList;

public class GalleryBottomTabPageAdapter extends FragmentStateAdapter {
    private final ArrayList<Fragment> fragmentArrayList;

    public GalleryBottomTabPageAdapter(@NonNull Fragment fragment, ArrayList<Fragment> fragmentArrayList) {
        super(fragment);
        this.fragmentArrayList = fragmentArrayList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                return new GalleryBottomAllFragment();
            case 1:
                return new GalleryBottomImageFragment();
            case 2:
                return new GalleryBottomVideoFragment();
            default:
                Log.e("BottomTabAdapter","Invalid exception ");
                return new GalleryBottomAllFragment();
        }
//        return fragmentArrayList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentArrayList.size();
    }
}