package com.sionyx.plexus.ui.dialog;

import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.viewmodel.CameraPresetsViewModel.applyPreset;
import static com.sionyx.plexus.ui.camera.CameraFragment.isInfoButtonPressed;
import static com.sionyx.plexus.ui.camera.CameraFragment.isSettingButtonPressed;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dome.librarynightwave.model.persistence.savesettings.SaveSettings;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;
import com.sionyx.plexus.R;
import com.sionyx.plexus.databinding.FragmentCameraPresetDialogBinding;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.camera.CameraViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CameraPresetDialogFragment extends DialogFragment {
    private static final String TAG = "CameraPresetDialogFragment";
    private FragmentCameraPresetDialogBinding binding;
    private CameraPresetsViewModel cameraPresetsViewModel;
    private CameraViewModel cameraViewModel;
    private LifecycleOwner lifecycleOwner;
    private CameraPresetAdapter cameraPresetAdapter;
    private List<SaveSettings> savedPreSettings;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCameraPresetDialogBinding.inflate(inflater, null, false);
        lifecycleOwner = this;
        binding.setLifecycleOwner(lifecycleOwner);
        cameraPresetsViewModel = new ViewModelProvider(requireActivity()).get(CameraPresetsViewModel.class);
        cameraViewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);
        binding.setViewModel(cameraPresetsViewModel);
        binding.cameraPresetLayoutContainer.setBackgroundResource(R.drawable.ic_tab_layout_background_with_border);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        subscribeUI();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void subscribeUI() {
        setCancelable(false);

        savedPreSettings = cameraPresetsViewModel.getSavedPreSettings(currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE));
        if (savedPreSettings != null && !savedPreSettings.isEmpty()) {
            cameraPresetAdapter = new CameraPresetAdapter(requireContext(), (ArrayList<SaveSettings>) savedPreSettings, cameraPresetsViewModel);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            binding.presetRecylerView.setLayoutManager(layoutManager);
            binding.presetRecylerView.setAdapter(cameraPresetAdapter);
            binding.emptyView.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.VISIBLE);
        }
        // for this disable default apply and delete button
        binding.savePreset.setEnabled(false);
        binding.deletePreset.setEnabled(false);
        binding.savePreset.setAlpha(.5f);
        binding.deletePreset.setAlpha(.5f);

        cameraPresetsViewModel.isCancelPresetView.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
                cameraViewModel.setSelectPreset(false);
            }
        }));

        cameraPresetsViewModel.isApplyPreset.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (cameraPresetAdapter != null) {
                    if (applyPreset == CameraPresetsViewModel.ApplyPreset.NONE && !cameraPresetsViewModel.isSelectDeletePreset()) {
                        int selectedPosition = cameraPresetAdapter.selectedPosition;
                        SaveSettings saveSettings = savedPreSettings.get(selectedPosition);
                        Log.d(TAG, "subscribeUI: " + saveSettings.getPreset_name() + " ///" + selectedPosition);
                        applyPresetSettings(saveSettings.getPreset_name(), saveSettings.isIs_nightwave());
                    } else {
                        Log.e(TAG, "subscribeUI: apply preset value in-progress");
                        Toast.makeText(requireContext(), getString(R.string.please_wait), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }));

        //for this observe click event on button
        cameraPresetsViewModel.isDeletePreset.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.NONE) {
                    cameraPresetsViewModel.setSelectDeletePreset(true);
                    showCameraSettingsDeleteDialog(getString(R.string.save_settings_delete_message));
                }else {
                    Log.e(TAG, "subscribeUI: apply preset value in-progress");
                    Toast.makeText(requireContext(), getString(R.string.please_wait), Toast.LENGTH_SHORT).show();
                }
            }
        }));

        // for this observer dialog click event
        cameraPresetsViewModel.hasDeletePreset.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            cameraPresetsViewModel.setSelectDeletePreset(false);
            if (aBoolean) {
                if (cameraPresetAdapter != null) {
                    int selectedPosition = cameraPresetAdapter.selectedPosition;
                    SaveSettings saveSettings = savedPreSettings.get(selectedPosition);
                    Log.d(TAG, "isDeletePreset: " + saveSettings.getPreset_name() + selectedPosition);
                    cameraPresetsViewModel.deletePreset(saveSettings.getPreset_name(), saveSettings.isIs_nightwave());
                    savedPreSettings.remove(selectedPosition);
                    cameraPresetsViewModel.hasShowApplyOption(false); // for this disable apply and delete options
                    if (cameraPresetAdapter != null) {
                        cameraPresetAdapter.updateRadioButtonState();
                        cameraPresetAdapter.notifyDataSetChanged();
                    }
                    if (savedPreSettings.size() == 0) {
                        binding.emptyView.setVisibility(View.VISIBLE);
                        binding.savePreset.setEnabled(false);
                        binding.deletePreset.setEnabled(false);
                        binding.savePreset.setAlpha(.5f);
                        binding.deletePreset.setAlpha(.5f);
                    }
                }
            }
        }));


        // for this when select radio button then it enable apply and delete buttons
        cameraPresetsViewModel.hasShowApplyOption.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.savePreset.setEnabled(true);
                binding.deletePreset.setEnabled(true);
                binding.savePreset.setAlpha(1.0f);
                binding.deletePreset.setAlpha(1.0f);
            } else {
                binding.savePreset.setEnabled(false);
                binding.deletePreset.setEnabled(false);
                binding.savePreset.setAlpha(.5f);
                binding.deletePreset.setAlpha(.5f);
            }
        }));

        cameraPresetsViewModel.isApplySettingsSuccessfully.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                binding.presetProgressBar.setVisibility(View.GONE);
                Toast.makeText(cameraPresetsViewModel.mContext, getString(R.string.settings_loaded_successfully), Toast.LENGTH_LONG).show();
                backToLiveScreen();
                cameraViewModel.setShowExpandMenu(false);
                cameraViewModel.setShowAllMenu(true);
                applyPreset = CameraPresetsViewModel.ApplyPreset.NONE;
                cameraPresetsViewModel.setSelectDeletePreset(false);
                cameraViewModel.setSelectPreset(false);
            }
        }));

        if (requireActivity().isInMultiWindowMode()) {
            cameraPresetsViewModel.onCancelPresetView();
        }
        cameraViewModel.isEnableMultiWindowMode.observe(lifecycleOwner, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.e(TAG, "getEnterExitMultiWindowMode: " + aBoolean);
                cameraPresetsViewModel.onCancelPresetView();
            }
        }));


    }

    private void showCameraSettingsDeleteDialog(String message) {
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            activity.showDialog = MainActivity.ShowDialog.DELETE_SAVE_CAMERA_SETTINGS_ALERT_DIALOG;
            activity.showDialog("", message, null);
        }
    }

    private void applyPresetSettings(String presetName, boolean isNightwave) {
        cameraPresetsViewModel.getSavedSettings(presetName, isNightwave)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<SaveSettings>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(List<SaveSettings> saveSettings) {
                        updateUI(saveSettings);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(cameraPresetsViewModel.mContext, getString(R.string.settings_loaded_failed), Toast.LENGTH_LONG).show();
                        binding.presetProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private final Handler handler = new Handler();

    private void updateUI(List<SaveSettings> saveSettings) {
        requireActivity().runOnUiThread(() -> {
            binding.presetProgressBar.setVisibility(View.VISIBLE);
            cameraPresetsViewModel.hasShowApplyOption(false); // for this disable apply and delete options
        });

        new Thread(() -> {
            applyPreset = CameraPresetsViewModel.ApplyPreset.APPLY_PRESET_VALUES;
            cameraPresetsViewModel.applySettings((ArrayList<SaveSettings>) saveSettings);
        }).start();
    }

    private void backToLiveScreen() {
        isInfoButtonPressed = false;
        isSettingButtonPressed = false;
        cameraPresetsViewModel.setSelectDeletePreset(false);
        if (binding.getRoot().getVisibility() == View.VISIBLE) {
            cameraViewModel.setIsShowAlertDialog(false);
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