package com.sionyx.plexus.ui.dialog;


import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCustomProgressDialogBinding;
import com.sionyx.plexus.ui.MainActivity;


public class CustomProgressDialogFragment extends DialogFragment {

    private FragmentCustomProgressDialogBinding binding;
    private TCPConnectionViewModel tcpConnectionViewModel;
    private MainActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent);
    }

    private void setWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        int widthh = 0;
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            widthh = (int) (width * .75);
        } else {
            widthh = (int) (width * .60);
        }
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = widthh;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCustomProgressDialogBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        setCancelable(false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tcpConnectionViewModel = new ViewModelProvider(this).get(TCPConnectionViewModel.class);
        tcpConnectionViewModel.observerUpgradeCompleteWaitMsg().observe(this, new com.dome.librarynightwave.utils.EventObserver<>(object -> {
            String message = object.toString();
            if (message.contains("UPGRADE_DATA_COMPLETE_WAIT_PERCENTAGE")) {
//                Log.e("CustomProgressDialogFragment", "UPGRADE_DATA_COMPLETE_WAIT_PERCENTAGE");
                String[] msg = message.split(" ");
                String percentage = msg[1];
                try {
                    if (activity != null) {
                        String progressText = activity.getString(R.string.please_wait) + " " + percentage;
                        binding.progressTextView.setText(progressText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;

    }

    @Override
    public void onResume() {
        super.onResume();
        setWidth();
    }
}