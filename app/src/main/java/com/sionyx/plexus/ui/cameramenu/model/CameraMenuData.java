package com.sionyx.plexus.ui.cameramenu.model;

public class CameraMenuData {
    String title;
    int icon;
    public CameraMenuData(String title, int icon)    {
        this.icon =icon;
        this.title = title;
    }

    public void setIcon(Integer icon) {
        this.icon = icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }
}
