package com.sionyx.plexus.ui.info;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sionyx.plexus.ContactFragment;
import com.sionyx.plexus.InfoFragment;

public class InfoViewTabLayoutAdapter extends FragmentStateAdapter {
    private final int tabCount;

    public InfoViewTabLayoutAdapter(@NonNull Fragment fragment, int tabCount) {
        super(fragment);
        this.tabCount = tabCount;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new InfoFragment();
            case 1:
                return new ContactFragment();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return tabCount;
    }
}
