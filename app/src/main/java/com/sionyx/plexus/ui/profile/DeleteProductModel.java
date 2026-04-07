package com.sionyx.plexus.ui.profile;

import com.sionyx.plexus.ui.api.responseModel.ProfileDevices;

public class DeleteProductModel {
    public ProfileDevices profileDevices;
    public int itemPosition;

    public ProfileDevices getProfileDevices() {
        return profileDevices;
    }

    public void setProfileDevices(ProfileDevices profileDevices) {
        this.profileDevices = profileDevices;
    }

    public int getItemPosition() {
        return itemPosition;
    }

    public void setItemPosition(int itemPosition) {
        this.itemPosition = itemPosition;
    }
}
