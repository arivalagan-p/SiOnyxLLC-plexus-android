package com.sionyx.plexus.ui.dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentTimeZoneDialogBinding;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.utils.EventObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class TimeZoneDialogFragment extends DialogFragment {
    private static final String TAG = "TimeZoneDialogFragment";
    private FragmentTimeZoneDialogBinding timeZoneDialogBinding;
    private LifecycleOwner lifecycleOwner;
    private TimeZoneViewModel timeZoneViewModel;
    private CameraViewModel cameraViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        timeZoneDialogBinding = FragmentTimeZoneDialogBinding.inflate(inflater, container, false);
        lifecycleOwner = this;
        timeZoneDialogBinding.setLifecycleOwner(lifecycleOwner);
        timeZoneViewModel = new ViewModelProvider(requireActivity()).get(TimeZoneViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        timeZoneDialogBinding.setViewModel(timeZoneViewModel);
        timeZoneDialogBinding.timeZoneLayoutContainer.setBackgroundResource(R.drawable.ic_tab_layout_background_with_border);
        return timeZoneDialogBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscribeUI();
    }

    private void subscribeUI() {
        setCancelable(false);
        ArrayList<String> timeZonelist = new ArrayList<>(Arrays.asList(timeZoneViewModel.settingsArrayList));
        CameraTimeZoneAdapter cameraTimeZoneAdapter = new CameraTimeZoneAdapter(timeZonelist, timeZoneViewModel);
        timeZoneDialogBinding.timeZoneRecylerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        timeZoneDialogBinding.timeZoneRecylerView.setAdapter(cameraTimeZoneAdapter);

        timeZoneViewModel.isSelectTimeZoneCancelIcon.observe(lifecycleOwner, new EventObserver<Boolean>(aBoolean -> {
            if (aBoolean) {
                cameraViewModel.hasShowSettingsDialog(true);
                backToSettings();
            }
        }));

        timeZoneViewModel.isSelectTimeZone.observe(lifecycleOwner, new EventObserver<>(integer -> {
            if (integer != -1) {
                Log.d(TAG, "selectedTime Zone: "+ integer);
                backToSettings();
            }
        }));
    }

    private void backToSettings() {
        if (timeZoneDialogBinding.getRoot().getVisibility() == View.VISIBLE) {
            dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        assert this.getDialog() != null;
        WindowManager.LayoutParams params = Objects.requireNonNull(this.getDialog().getWindow()).getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent);
        getDialog().getWindow().setAttributes(params);
    }
}