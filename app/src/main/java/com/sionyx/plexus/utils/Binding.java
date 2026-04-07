package com.sionyx.plexus.utils;

import android.view.View;

import androidx.databinding.BindingAdapter;

public class  Binding {
    @BindingAdapter("showIf")
    public  static void setVisiblity(View view, boolean aBoolean) {
        if (aBoolean) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
}
