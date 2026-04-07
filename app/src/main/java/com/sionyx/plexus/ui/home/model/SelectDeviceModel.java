package com.sionyx.plexus.ui.home.model;

public class SelectDeviceModel {
    public String name;
    public int imageId;

    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imgid) {
        this.imageId = imgid;
    }


    public SelectDeviceModel(String name, int imageId) {
        this.name = name;
        this.imageId = imageId;
    }
}
