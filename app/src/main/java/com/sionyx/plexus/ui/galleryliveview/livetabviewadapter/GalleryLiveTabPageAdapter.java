package com.sionyx.plexus.ui.galleryliveview.livetabviewadapter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sionyx.plexus.ui.galleryliveview.GalleryImageFragment;
import com.sionyx.plexus.ui.galleryliveview.GalleryVideoFragment;
import com.sionyx.plexus.ui.galleryliveview.GalleyAllViewsFragment;

import java.util.ArrayList;

public class GalleryLiveTabPageAdapter extends FragmentStateAdapter {
    private final ArrayList<Fragment> fragmentArrayList;

    public GalleryLiveTabPageAdapter(@NonNull Fragment fragment, ArrayList<Fragment> fragmentArrayList) {
        super(fragment);
        this.fragmentArrayList = fragmentArrayList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
         switch (position){
            case 0:
                return new GalleyAllViewsFragment();
            case 1:
                return new GalleryImageFragment();
            case 2:
                return new GalleryVideoFragment();
            default:
                Log.e("LiveTabAdapter","Invalida exception ");
                return new GalleyAllViewsFragment();
        }
//        return fragmentArrayList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentArrayList.size();
    }
}
