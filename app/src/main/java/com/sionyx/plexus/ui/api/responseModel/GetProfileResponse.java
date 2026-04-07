package com.sionyx.plexus.ui.api.responseModel;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class GetProfileResponse {
        @SerializedName("UserName")
        public String userName;
        @SerializedName("Device")
        public ArrayList<ProfileDevices> profileDevices;
        public String getUserName() {
                return userName;
        }

        public void setUserName(String userName) {
                this.userName = userName;
        }

        public ArrayList<ProfileDevices> getProfileDevices() {
                return profileDevices;
        }

        public void setProfileDevices(ArrayList<ProfileDevices> profileDevices) {
                this.profileDevices = profileDevices;
        }
}
