package com.sionyx.plexus.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dome.librarynightwave.model.persistence.savesettings.SaveSettings;
import com.dome.librarynightwave.viewmodel.CameraPresetsViewModel;
import com.sionyx.plexus.R;

import java.util.ArrayList;

public class CameraPresetAdapter extends RecyclerView.Adapter<CameraPresetAdapter.RecyclerViewHolder> {

    private final ArrayList<SaveSettings> settingsArrayList;
    private final CameraPresetsViewModel presetsViewModel;

    public static SaveSettings saveSettings;
    public int selectedPosition = -1;

    private RadioButton selected = null;

    public CameraPresetAdapter(Context context, ArrayList<SaveSettings> saveSettingsArrayList, CameraPresetsViewModel viewModel) {
        this.settingsArrayList = saveSettingsArrayList;
        this.presetsViewModel = viewModel;
    }

    @NonNull
    @Override
    public CameraPresetAdapter.RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.camera_presettings_view, parent, false);
        return new RecyclerViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull CameraPresetAdapter.RecyclerViewHolder holder, int position) {
        SaveSettings saveSettings = settingsArrayList.get(position);
        holder.presetname.setText(saveSettings.getPreset_name());

        // for this radio button default state handle
        if (position == getItemCount() - 1) {
            if (selected == null) {
                holder.radioButton.setChecked(false);
                selected = holder.radioButton;
            }
        }

        holder.itemView.setOnClickListener(view -> {
            if (selected != null) {
                selected.setChecked(false);
            }
            holder.radioButton.setChecked(true);
            selected = holder.radioButton;
            // selected item position data get
            selectedPosition = holder.getAdapterPosition();
            presetsViewModel.hasShowApplyOption(true);
            presetsViewModel.setSaveSettings(saveSettings);
        });
        holder.radioButton.setOnClickListener(view -> {
            if (selected != null) {
                selected.setChecked(false);
            }
            holder.radioButton.setChecked(true);
            selected = holder.radioButton;
            // selected item position data get
            selectedPosition = holder.getAdapterPosition();
            presetsViewModel.hasShowApplyOption(true);
            presetsViewModel.setSaveSettings(saveSettings);
        });
    }

    public void updateRadioButtonState(){
        if (selected != null) {
            selected.setChecked(false);
        }
    }
    @Override
    public int getItemCount() {
        return settingsArrayList.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView presetname;
        RadioButton radioButton;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            presetname = itemView.findViewById(R.id.preset_name);
            radioButton = itemView.findViewById(R.id.select_preset_checkbox);
        }
    }

    public static SaveSettings getSaveSettings() {
        return saveSettings;
    }

    public static void setSaveSettings(SaveSettings saveSettings) {
        CameraPresetAdapter.saveSettings = saveSettings;
    }
}



