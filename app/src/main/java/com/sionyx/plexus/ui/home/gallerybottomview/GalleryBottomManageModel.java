package com.sionyx.plexus.ui.home.gallerybottomview;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.util.Objects;

public class GalleryBottomManageModel {

    public ImageView photo;
    public String fileName;
    public String filePath;
    public boolean isChecked;
    public Bitmap bitmap;
    public boolean selectAll;

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public ImageView getPhoto() {
        return photo;
    }

    public void setPhoto(ImageView photo) {
        this.photo = photo;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        GalleryBottomManageModel sub_cat = (GalleryBottomManageModel) o;
        return Objects.equals(fileName, sub_cat.fileName);
    }
}