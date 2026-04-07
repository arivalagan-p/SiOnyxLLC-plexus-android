package com.sionyx.plexus.ui.dialog;

import static com.dome.librarynightwave.model.repository.TCPRepository.isOpsinLiveStreamingStarted;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.utils.Constants.NWD_FW_VERSION;
import static com.dome.librarynightwave.utils.Constants.STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_DISCONNECTED;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.dome.librarynightwave.utils.Constants.mWifiState;
import static com.sionyx.plexus.ui.home.HomeFragment.isSelectPopUpFwUpdateCheck;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.model.services.TCPCommunicationService;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.EventObserver;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.sionyx.plexus.R;
import com.sionyx.plexus.ui.MainActivity;
import com.sionyx.plexus.ui.home.HomeViewModel;

import java.util.Objects;

public class NoticeDialogFragment extends DialogFragment {

    private String message = "";
    private String title = "";
    private boolean shouldHideCancel = false;
    private boolean isWarningIcon = false;
    private boolean isNotesIcon = false;
    private String release_notes;
    private boolean hasShowSaveButton = false;
    private boolean displayTitle = false;
    private boolean hasShowSDCardButton = false;
    private boolean hasShowSiOnyxAppButton = false;
    private boolean hasShowOkButton = false;
    private boolean hasShowWiFiFwUpdateDialog = false;
    private boolean hasShowSaveCameraSettingsLayout = false;
    private boolean hasShowSoftwareVersionLayout = false;
    private boolean hasShowCameraRebootLayout = false;
    private boolean hasShowPasswordResetLayout = false;
    public static boolean isShowNotes;

    private HomeViewModel homeViewModel;
    int progressStatus = 0;
    Handler handler = new Handler(Looper.getMainLooper());
    private String dialogMode;
    TextInputEditText saveSettingsEditText, specialCharacterEditText;
    LinearLayout nwd_password_reset_layout;
    ConstraintLayout fw_version_layout, specialCharacterLayout, nightwaveFwUpdateLayout, opsinFwUpdateLayout, opsinFwStatusProgressbar, presetNameSaveLayout;
    ScrollView itemScrollView, notesScrollView;
    ImageView img_info;
    ProgressBar nwd_progress_bar;
    TextView top_version_current,
            top_version_new,
            top_version_title,
            fpga_version_current,
            fpga_version_new,
            fpga_version_title,
            riscv_version_current,
            riscv_version_new,
            riscv_version_title,
            opsin_riscv_overlay_version_current,
            opsin_riscv_overlay_version_new,
            opsin_riscv_overlay_version_title,
            opsin_riscv_recovery_version_current,
            opsin_riscv_recovery_version_new,
            opsin_riscv_recovery_version_title,
            wifi_version_current,
            wifi_version_new,
            wifi_version_title,
            firmware_title,
            current_version_title,
            new_version_title,
            proceed_update_text,
            save_button_text,
            opsin_status_title,
            opsin_fpga_status,
            opsin_riscv_status,
            opsin_riscv_recovery_status,
            opsin_riscv_overlay_status,
            opsin_wifi_status,
            opsin_bluetooth_status,
            tv_message,
            opsin_progress_status_value,
            opsin_fpga_version_current,
            opsin_fpga_version_new,
            opsin_riscv_version_current,
            opsin_riscv_version_new,
            opsin_wifi_version_current,
            opsin_wifi_version_new,
            opsin_bluetooth_version_current,
            opsin_bluetooth_version_new,
            opsin_riscv_version_title,
            opsin_fpga_version_title,
            opsin_wifi_version_title,
            opsin_bluetooth_version_title,
            opsin_wifi,
            opsin_bluetooth,
            opsin_core,
            opsin_firmware,
            opsin_overlay,
            opsin_recovery,
            tv_title,
            opsin_proceed_update_text,
            txt_sdcard_btn,
            txt_sionyx_app_btn,
            opsin_firmware_title,
            nwd_firmware_title,
            nwd_firmware_version,
            nwd_firmware_message,
            nwd_camera_reboot_message,
            nwd_reset_password,
            nwd_reset_pwd_instruction,
            nwd_progress_number;

    private String cameraReleaseVersion = null,
            cameraRISCV = null,
            cameraRISCV_RECOVERY = null,
            cameraOVERLAY = null,
            cameraWIFI_rtos = null,
            cameraWIFI_BLE = null,
            cameraFPGA = null,
            appRelease = null,
            appRISCV = null,
            appRISCV_RECOVERY = null,
            appOVERLAY = null,
            appWIFI_rtos = null,
            appWIFI_BLE = null,
            appFPGA = null;

    private ImageView img_okay, img_cancel;

    private ConstraintLayout.LayoutParams layoutParams;
    private TCPConnectionViewModel tcpConnectionViewModel;

    public void setListener(HomeViewModel.NoticeDialogListener listener, HomeViewModel viewModel) {
        this.homeViewModel = viewModel;
        viewModel.setListener(listener);
    }

    @Override
    public void onResume() {
        super.onResume();
        setWidth();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Transparent1);

        if (getArguments() != null) {
            message = getArguments().getString("message");
            title = getArguments().getString("title");
            shouldHideCancel = getArguments().getBoolean("disable_cancel_button");
            isWarningIcon = getArguments().getBoolean("isWarningIcon");
            isNotesIcon = getArguments().getBoolean("isNotesIcon");
            release_notes = getArguments().getString("release_notes", "");
            hasShowSaveButton = getArguments().getBoolean("hasShowSaveButton");
            displayTitle = getArguments().getBoolean("displayTitle");
            hasShowSDCardButton = getArguments().getBoolean("hasShowSDCardButton");
            hasShowSiOnyxAppButton = getArguments().getBoolean("hasShowSiOnyxAppButton");
            hasShowOkButton = getArguments().getBoolean("disable_Ok_button");
            hasShowWiFiFwUpdateDialog = getArguments().getBoolean("is_wifi_fw_update_dialog");
            hasShowSaveCameraSettingsLayout = getArguments().getBoolean("hasShowSaveCameraSettingsLayout");
            hasShowSoftwareVersionLayout = getArguments().getBoolean("hasShowSoftwareVersionLayout");
            hasShowCameraRebootLayout = getArguments().getBoolean("hasShowCameraRebootLayout");
            hasShowPasswordResetLayout = getArguments().getBoolean("hasShowPasswordResetLayout");
        }
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        /*
        For dismiss the dialog if it has been shown when app goes BG toFG
         */
        homeViewModel.dialogFragment = NoticeDialogFragment.this;


        if (mWifiState == WIFI_STATE_CONNECTED && Constants.mState == STATE_CONNECTED) {
            tcpConnectionViewModel = new ViewModelProvider(this).get(TCPConnectionViewModel.class);
            isOpsinLiveStreamingStarted = true;// Just avoiding start live streaming during this fragment visible
            applyOpsinPeriodicRequest = TCPCommunicationService.OpsinPeriodicRequest.APPLY_OPSIN_PERIODIC_VALUES;
           if (isSelectPopUpFwUpdateCheck)
                tcpConnectionViewModel.clearPeriodicRequestList(); // if dialog appear clear periodic request values while save/delete settings to avoid to commented
            tcpConnectionViewModel.addOpsinPeriodicTimerCommand(TCPRepository.PERIODIC_COMMAND.KEEP_ALIVE);
        }
    }

    private void setWidth() {
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            int widthh = 0;
            int orientation = this.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                widthh = (int) (width * .800);
            } else {
                if (!release_notes.isEmpty()) {
                    widthh = (int) (width * .50);
                } else {
                    widthh = (int) (width * .35);
                }
            }
            // notesScrollView.setVerticalScrollBarEnabled(false); //here set to false temp for release notes length less
            showScrollbarInFwUpdateLayout(displayMetrics, orientation);
            Dialog dialog = getDialog();
            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.width = widthh;
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    window.setAttributes(params);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showScrollbarInFwUpdateLayout(DisplayMetrics displayMetrics, int orientation) {
        // for this function add scroll view in landscape mode view mobile only
        float yInches = displayMetrics.heightPixels / displayMetrics.ydpi;
        float xInches = displayMetrics.widthPixels / displayMetrics.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        if (diagonalInches >= 6.5) {
            // 6.5inch devices and above(Tablet)
            layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topToBottom = opsin_firmware_title.getId();
            itemScrollView.setLayoutParams(layoutParams);
            itemScrollView.setVerticalScrollBarEnabled(false);
            notesScrollView.setVerticalScrollBarEnabled(false);
        } else {
            //Smart Phone
            if (!isShowNotes)
                notesScrollView.setVerticalScrollBarEnabled(false);

            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                itemScrollView.setVerticalScrollBarEnabled(false);
                if (isShowNotes) {
                    notesScrollView.setVerticalScrollBarEnabled(false);
                }
            } else {
                layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (int) (102 * getResources().getDisplayMetrics().density));
                layoutParams.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
                itemScrollView.setVerticalScrollBarEnabled(true);
                // if large notes to uncomment this line
              /*  if (isShowNotes) {
                    if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN))
                        notesScrollView.setVerticalScrollBarEnabled(true);
                }*/
            }
            layoutParams.topToBottom = opsin_firmware_title.getId();
            itemScrollView.setLayoutParams(layoutParams);
        }
    }

    private void showScrollbarInNotesLayout(boolean isShowNotesScrollView) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int orientation = this.getResources().getConfiguration().orientation;

        // for this function add scroll view in landscape mode view mobile only
        float yInches = displayMetrics.heightPixels / displayMetrics.ydpi;
        float xInches = displayMetrics.widthPixels / displayMetrics.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        if (diagonalInches >= 6.5) {
            // 6.5inch devices and above(Tablet)
            layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topToBottom = tv_title.getId();
            notesScrollView.setLayoutParams(layoutParams);
            notesScrollView.setVerticalScrollBarEnabled(false);
        } else {
            //Smart Phone
            if (isShowNotesScrollView) {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    notesScrollView.setVerticalScrollBarEnabled(false);
                } else {
                    // Temp commented to use if notes length more than 5 line use scroll view
                    /*if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.OPSIN)) {
                        layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, (int) (120 * getResources().getDisplayMetrics().density));
                        layoutParams.rightMargin = (int) (8 * getResources().getDisplayMetrics().density);
                        notesScrollView.setVerticalScrollBarEnabled(true);
                    } else {
                        layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                        notesScrollView.setVerticalScrollBarEnabled(false);
                    }*/

                    // use minimum notes length use this
                    layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    notesScrollView.setVerticalScrollBarEnabled(false);
                }
            } else {
                // for this remove height and set to wrap_content for text_message_view.
                layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                notesScrollView.setVerticalScrollBarEnabled(false);
            }

            layoutParams.topToBottom = tv_title.getId();
            notesScrollView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View dialog = inflater.inflate(R.layout.dialog_info, container, false);
        tv_title = dialog.findViewById(R.id.tv_title);
        tv_message = dialog.findViewById(R.id.tv_message);
        img_okay = dialog.findViewById(R.id.img_okay);
        img_cancel = dialog.findViewById(R.id.img_cancel);
        img_info = dialog.findViewById(R.id.img_info);
        ImageView img_notes = dialog.findViewById(R.id.img_notes);
        specialCharacterEditText = dialog.findViewById(R.id.edit_name_text);
        saveSettingsEditText = dialog.findViewById(R.id.edit_preset_name_text);
        itemScrollView = dialog.findViewById(R.id.item_scroll_view);
        notesScrollView = dialog.findViewById(R.id.notes_scroll_view);
        opsin_firmware_title = dialog.findViewById(R.id.opsin_firmware_title);
        nwd_firmware_title = dialog.findViewById(R.id.NWD_software_update_title);
        nwd_firmware_version = dialog.findViewById(R.id.NWD_current_version);
        nwd_firmware_message = dialog.findViewById(R.id.NWD_software_update_message);
        nwd_camera_reboot_message = dialog.findViewById(R.id.NWD_camera_reboot_msg);
        nwd_reset_password = dialog.findViewById(R.id.tvTitle);
        nwd_reset_pwd_instruction = dialog.findViewById(R.id.tvInstructions);
        nwd_progress_bar = dialog.findViewById(R.id.progressBar);
        nwd_progress_number = dialog.findViewById(R.id.tvPercentage);
        nwd_password_reset_layout = dialog.findViewById(R.id.pwd_reset_layout);

        tv_message.setMovementMethod(LinkMovementMethod.getInstance());
        tv_message.setLinkTextColor(Color.BLUE);

        specialCharacterLayout = dialog.findViewById(R.id.special_character_layout);
        presetNameSaveLayout = dialog.findViewById(R.id.preset_name_save_layout);
        fw_version_layout = dialog.findViewById(R.id.fw_update_layout);
        nightwaveFwUpdateLayout = dialog.findViewById(R.id.nw_fw_update_layout);
        opsinFwUpdateLayout = dialog.findViewById(R.id.opsin_fw_update_layout);
        opsinFwStatusProgressbar = dialog.findViewById(R.id.opsin_fw_status_bar);

        top_version_current = dialog.findViewById(R.id.top_version_current);
        top_version_new = dialog.findViewById(R.id.top_version_new);
        top_version_title = dialog.findViewById(R.id.top_version);
        firmware_title = dialog.findViewById(R.id.firmware_title);
        current_version_title = dialog.findViewById(R.id.current_version_title);
        new_version_title = dialog.findViewById(R.id.new_version_title);
        proceed_update_text = dialog.findViewById(R.id.proceed_update_text);
        save_button_text = dialog.findViewById(R.id.txt_save_btn);
        opsin_status_title = dialog.findViewById(R.id.opsin_status_title);
        opsin_proceed_update_text = dialog.findViewById(R.id.opsin_proceed_update_text);

        //nightwave
        fpga_version_current = dialog.findViewById(R.id.fpga_version_current);
        fpga_version_new = dialog.findViewById(R.id.fpga_version_new);
        fpga_version_title = dialog.findViewById(R.id.fpga_version);

        wifi_version_current = dialog.findViewById(R.id.wifi_version_current);
        wifi_version_new = dialog.findViewById(R.id.wifi_version_new);
        wifi_version_title = dialog.findViewById(R.id.wifi_version);

        riscv_version_current = dialog.findViewById(R.id.riscv_version_current);
        riscv_version_new = dialog.findViewById(R.id.riscv_version_new);
        riscv_version_title = dialog.findViewById(R.id.riscv_version);

        //Opsin
        txt_sionyx_app_btn = dialog.findViewById(R.id.txt_sionyx_app_btn);
        txt_sdcard_btn = dialog.findViewById(R.id.txt_sdcard_btn);
        opsin_progress_status_value = dialog.findViewById(R.id.opsin_progress_status_value);

        opsin_wifi_version_current = dialog.findViewById(R.id.opsin_wifi_version_current);
        opsin_wifi_version_new = dialog.findViewById(R.id.opsin_wifi_version_new);
        opsin_wifi_version_title = dialog.findViewById(R.id.opsin_wifi_version);
        opsin_wifi_status = dialog.findViewById(R.id.opsin_wifi_status);

        opsin_bluetooth_version_current = dialog.findViewById(R.id.opsin_bluetooth_version_current);
        opsin_bluetooth_version_new = dialog.findViewById(R.id.opsin_bluetooth_version_new);
        opsin_bluetooth_version_title = dialog.findViewById(R.id.opsin_bluetooth_version);
        opsin_bluetooth_status = dialog.findViewById(R.id.opsin_bluetooth_status);

        opsin_fpga_version_current = dialog.findViewById(R.id.opsin_fpga_version_current);
        opsin_fpga_version_new = dialog.findViewById(R.id.opsin_fpga_version_new);
        opsin_fpga_version_title = dialog.findViewById(R.id.opsin_fpga_version);
        opsin_fpga_status = dialog.findViewById(R.id.opsin_fpga_status);

        opsin_riscv_version_current = dialog.findViewById(R.id.opsin_riscv_version_current);
        opsin_riscv_version_new = dialog.findViewById(R.id.opsin_riscv_version_new);
        opsin_riscv_version_title = dialog.findViewById(R.id.opsin_riscv_version);
        opsin_riscv_status = dialog.findViewById(R.id.opsin_riscv_status);

        opsin_riscv_recovery_version_current = dialog.findViewById(R.id.opsin_riscv_recovery_version_current);
        opsin_riscv_recovery_version_new = dialog.findViewById(R.id.opsin_riscv_recovery_version_new);
        opsin_riscv_recovery_version_title = dialog.findViewById(R.id.opsin_riscv_recovery_version);
        opsin_riscv_recovery_status = dialog.findViewById(R.id.opsin_riscv_recovery_status);

        opsin_riscv_overlay_version_current = dialog.findViewById(R.id.opsin_riscv_overlay_version_current);
        opsin_riscv_overlay_version_new = dialog.findViewById(R.id.opsin_riscv_overlay_version_new);
        opsin_riscv_overlay_version_title = dialog.findViewById(R.id.opsin_riscv_overlay_version);
        opsin_riscv_overlay_status = dialog.findViewById(R.id.opsin_riscv_overlay_status);

        //opsin progressbar status
        opsin_wifi = dialog.findViewById(R.id.wifi);
        opsin_bluetooth = dialog.findViewById(R.id.ble);
        opsin_core = dialog.findViewById(R.id.core);
        opsin_firmware = dialog.findViewById(R.id.riscv);
        opsin_overlay = dialog.findViewById(R.id.overlay);
        opsin_recovery = dialog.findViewById(R.id.recovery);

        dialogMode = homeViewModel.getDialogMode();

        if (!release_notes.isEmpty()) {
            if (dialogMode.equals("NOTES")) {
                fw_version_layout.setVisibility(View.GONE);
                hideOpsinFwChangesLayout();
                hideNightwaveFwUpdateLayout();
                isShowNotes = true;
            } else {
                isShowNotes = false;
                fw_version_layout.setVisibility(View.VISIBLE);
                switch (currentCameraSsid) {
                    case NIGHTWAVE:
                        nightwaveFwUpdateLayout.setVisibility(View.VISIBLE);
                        proceed_update_text.setText(getString(R.string.proceed_with_update_nightwave));
                        break;
                    case OPSIN:
                        showOpsinFwUpdateLayout();
                        opsin_proceed_update_text.setText(getString(R.string.proceed_with_update));
                        break;
                    case NIGHTWAVE_DIGITAL:
                        //update text
                        break;
                }
                getFirmwareVersions();
            }
        } else {
            isShowNotes = false;
            fw_version_layout.setVisibility(View.GONE);
            hideOpsinFwChangesLayout();
            hideNightwaveFwUpdateLayout();
        }

        /* for this show special character edit text layout*/
        if (!hasShowSaveButton) {
            specialCharacterLayout.setVisibility(View.GONE);
            save_button_text.setVisibility(View.GONE);
            img_okay.setVisibility(View.VISIBLE);
        } else {
            specialCharacterLayout.setVisibility(View.VISIBLE);
            save_button_text.setVisibility(View.VISIBLE);
            img_okay.setVisibility(View.GONE);
        }

        /* for this show camera settings save edit text layout*/
        if (!hasShowSaveCameraSettingsLayout) {
            presetNameSaveLayout.setVisibility(View.GONE);
            // save_button_text.setVisibility(View.GONE);
            img_okay.setVisibility(View.VISIBLE);
        } else {
            presetNameSaveLayout.setVisibility(View.VISIBLE);
            save_button_text.setVisibility(View.GONE);
            img_okay.setVisibility(View.VISIBLE);

            applyInputFilters();

            saveSettingsEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not used, but required by interface
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not used, but required by interface
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String filtered = removeEmojiAndWhitespace(s.toString());

                    if (!s.toString().equals(filtered)) {
                        saveSettingsEditText.setText(filtered);
                        saveSettingsEditText.setSelection(filtered.length()); // Move cursor to end
                    }
                }
            });


        }


        if (!hasShowSDCardButton) {
            txt_sdcard_btn.setVisibility(View.GONE);
        } else {
            txt_sdcard_btn.setVisibility(View.VISIBLE);
            if (hasShowWiFiFwUpdateDialog)
                txt_sdcard_btn.setText(getString(R.string.repeat));
            else
                txt_sdcard_btn.setText(getString(R.string.sd_card));
        }

        if (!hasShowSiOnyxAppButton) {
            txt_sionyx_app_btn.setVisibility(View.GONE);
        } else {
            txt_sionyx_app_btn.setVisibility(View.VISIBLE);
            if (hasShowWiFiFwUpdateDialog)
                txt_sionyx_app_btn.setText(getString(R.string.skip));
            else
                txt_sionyx_app_btn.setText(getString(R.string.siOnyx_app));
        }

        if (hasShowOkButton)
            img_okay.setVisibility(View.GONE);

        if (shouldHideCancel)
            img_cancel.setVisibility(View.GONE);

        if (!isNotesIcon)
            img_notes.setVisibility(View.GONE);

        if (isWarningIcon)
            img_info.setImageResource(R.drawable.ic_baseline_warning);
        else {
            if (hasShowSaveCameraSettingsLayout)
                img_info.setVisibility(View.GONE);
            else
                img_info.setImageResource(R.mipmap.ic_info);
        }
        MainActivity activity = ((MainActivity) getActivity());
        if (activity != null) {
            Log.e("NoticeDialogFragment","WIFI_DISCONNECT: " + activity.showDialog + " mWifiState"+mWifiState);
            if (Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL.equals(currentCameraSsid)) {
                if (activity.showDialog == MainActivity.ShowDialog.POP_UP_DELETE_ITEM_DIALOG) {
                    img_cancel.setVisibility(View.VISIBLE);
                } else if (activity.showDialog == MainActivity.ShowDialog.WIFI_DISCONNECT) {
                    img_cancel.setVisibility(View.GONE);
                } else if (mWifiState == WIFI_STATE_DISCONNECTED) {
                    img_cancel.setVisibility(View.GONE);
                }
            }
        }
        if (!message.equals("")) {
            tv_message.setText(message);
            tv_message.setMovementMethod(LinkMovementMethod.getInstance());
            tv_message.setLinkTextColor(Color.BLUE);
        }

        if (!displayTitle) {
            tv_title.setVisibility(View.GONE);
        } else if (!title.equals("")) {
            tv_title.setVisibility(View.VISIBLE);
            tv_title.setText(title);
        }

        if (hasShowSoftwareVersionLayout && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
            Log.e("NoticeDialogFragment","NWD_FW_VERSION: " + NWD_FW_VERSION);
            String fwVersion = NWD_FW_VERSION != null ? NWD_FW_VERSION : " ";//get current camera firmware version
            nwd_firmware_version.setText(String.format(getString(R.string.software_update_version), fwVersion));
            nwd_firmware_message.setText(Html.fromHtml(getString(R.string.software_update_message), Html.FROM_HTML_MODE_LEGACY));
            nwd_firmware_message.setMovementMethod(LinkMovementMethod.getInstance());
            nwd_firmware_title.setVisibility(View.VISIBLE);
            nwd_firmware_version.setVisibility(View.VISIBLE);
            nwd_firmware_message.setVisibility(View.VISIBLE);
            img_okay.setVisibility(View.GONE);
            img_info.setVisibility(View.GONE);
            tv_message.setVisibility(View.GONE);
        }

        if (hasShowCameraRebootLayout && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){
            nwd_camera_reboot_message.setVisibility(View.GONE);
            img_okay.setVisibility(View.VISIBLE);
            img_info.setVisibility(View.GONE);
            tv_message.setVisibility(View.VISIBLE);
        }
       /* NWD password reset 10sec delay logic*/
/*        if (hasShowPasswordResetLayout && currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)){

            nwd_password_reset_layout.setVisibility(View.VISIBLE);
            nwd_reset_password.setVisibility(View.VISIBLE);
            nwd_reset_pwd_instruction.setVisibility(View.VISIBLE);
            nwd_progress_bar.setVisibility(View.VISIBLE);
            nwd_progress_number.setVisibility(View.VISIBLE);

            img_okay.setVisibility(View.GONE);
            img_info.setVisibility(View.GONE);
            tv_message.setVisibility(View.GONE);

            homeViewModel.getProgress().observe(requireActivity(), progress -> {
                nwd_progress_bar.setProgress(progress);
                nwd_progress_number.setText(progress + "%");
                if (progress == 100){
                    if (isVisible()){
                        Log.e("NoticeDialogfragment", "onCreateView: dismiss called" );
                        nwd_progress_bar.setProgress(0);
                        nwd_progress_number.setText(0 + "%");
                        homeViewModel.resetProgress();
                        homeViewModel.showDialog(true);
                        dismiss();
                    }

                }
            });

            homeViewModel.startProgress();

        }*/

        /* Cause of dismiss here - Sometimes the NWD_FWV Prompt is not dismissed from main activity when rotate the screen*/
        homeViewModel.firmwarePromptDismissed().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) {
                // FWD FW version prompt should be dismissed
                if (isVisible()) { // Need to validate
                    homeViewModel.resetTimer();
                    dismiss();
                    handler.postDelayed(() -> homeViewModel.navigateLiveScreen.postValue(true),500);
                }
            }
        });

        switch (dialogMode) {
            case "NOTES":
                if (!release_notes.isEmpty()) {
                    tv_message.setText(release_notes);
                    tv_message.setMovementMethod(LinkMovementMethod.getInstance());
                    tv_message.setLinkTextColor(Color.BLUE);
                    showScrollbarInNotesLayout(true);
                    isShowNotes = true;
                }

                img_cancel.setVisibility(View.GONE);
                img_notes.setVisibility(View.GONE);
                img_okay.setVisibility(View.VISIBLE);
                homeViewModel.setDialogMode("NOTES");
                dialogMode = homeViewModel.getDialogMode();
                getFirmwareVersions();
                break;

            case "NORMAL":
                homeViewModel.setDialogMode("NORMAL");
                dialogMode = homeViewModel.getDialogMode();
                showScrollbarInNotesLayout(false);
                isShowNotes = false;
                break;
        }

        img_okay.setOnClickListener(v -> {
            Log.e("NoticeDialogFragment", "dialogMode: "+ homeViewModel.getDialogMode());
            if (currentCameraSsid.equals(Constants.CURRENT_CAMERA_SSID.NIGHTWAVE_DIGITAL)) {
//                if (mWifiState == WIFI_STATE_DISCONNECTED || dialogMode.equals("SWITCH")) {
//                    try {
//                        homeViewModel.getListener().onDialogNegativeClick(NoticeDialogFragment.this);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }  else {
                    homeViewModel.getListener().onDialogPositiveClick(NoticeDialogFragment.this);

//                }
            }
            else {
                switch (dialogMode) {
                    case "NORMAL":
                        isShowNotes = false;
                        // for this save camera settings save layout view event call
                        if (hasShowSaveCameraSettingsLayout) {
                            try {
                                String cameraPresetName = saveSettingsEditText.getText().toString().trim();
                                if (cameraPresetName.isEmpty())
                                    saveSettingsEditText.setError(getString(R.string.preset_name_length));
                                if (!cameraPresetName.isEmpty()) {
                                    if (isValidPresetName(cameraPresetName)) {
                                        hideKeyboard(presetNameSaveLayout);
                                        homeViewModel.getListener().onDialogSaveClick(NoticeDialogFragment.this, cameraPresetName, false);
                                    } else {
                                        Toast.makeText(requireContext(), getString(R.string.preset_name_length), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                homeViewModel.getListener().onDialogPositiveClick(NoticeDialogFragment.this);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "NOTES":
                        if (!message.equals(""))
                            tv_message.setText(message);
                        if (isShowNotes)
                            isShowNotes = false;
                        img_cancel.setVisibility(View.VISIBLE);
                        img_notes.setVisibility(View.VISIBLE);
                        img_okay.setVisibility(View.VISIBLE);
                        homeViewModel.setDialogMode("NORMAL");
                        showVersionInformationDialog();
                        // hide and show defalut height and with for test message
                        int orientation = this.getResources().getConfiguration().orientation;
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                            showScrollbarInNotesLayout(false);
                        else
                            showScrollbarInNotesLayout(false);
                        dialogMode = homeViewModel.getDialogMode();
                        break;
                    case "SWITCH":
                        try {
                            homeViewModel.getListener().onDialogNegativeClick(NoticeDialogFragment.this);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        });

        specialCharacterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String allowedCharacters = "^[a-zA-Z0-9!@#$%&*]*$";
                    if (!s.toString().matches(allowedCharacters)) {
                        // If the entered text does not match the pattern, remove the last entered character
                        String filteredText = s.toString().replaceAll("[^a-zA-Z0-9!@#$%&*]", "");
                        specialCharacterEditText.setText(filteredText);
                        specialCharacterEditText.setSelection(Objects.requireNonNull(specialCharacterEditText.getText()).length());
                        Toast.makeText(requireContext(), getString(R.string.camera_name_error), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        save_button_text.setOnClickListener(v -> {
            try {
                String deviceName = specialCharacterEditText.getText().toString();
                if (deviceName.isEmpty())
                    specialCharacterEditText.setError(getString(R.string.device_name_cannot_blank));
                if (!deviceName.isEmpty()) {
                    if (isValidUsername(deviceName)) {
                        hideKeyboard(specialCharacterLayout);
                        homeViewModel.getListener().onDialogSaveClick(NoticeDialogFragment.this, deviceName, true);
                    } else {
                        specialCharacterEditText.setError(getString(R.string.camera_name_length));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        txt_sdcard_btn.setOnClickListener(v -> {
            try {
                homeViewModel.getListener().onDialogSdCardButtonClick(NoticeDialogFragment.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        txt_sionyx_app_btn.setOnClickListener(v -> {
            try {
                homeViewModel.getListener().onDialogSiOnyxAppButtonClick(NoticeDialogFragment.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        img_cancel.setOnClickListener(v -> {
            try {
                homeViewModel.getListener().onDialogNegativeClick(NoticeDialogFragment.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        img_notes.setOnClickListener(v -> {
            try {
                if (!release_notes.equals(""))
                    tv_message.setText(release_notes);

                img_cancel.setVisibility(View.GONE);
                img_notes.setVisibility(View.GONE);
                img_okay.setVisibility(View.VISIBLE);
                homeViewModel.setDialogMode("NOTES");
                hideVersionInformationDialog();
                dialogMode = homeViewModel.getDialogMode();

                int orientation = this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    showScrollbarInNotesLayout(true);
                else
                    showScrollbarInNotesLayout(false);
                isShowNotes = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        homeViewModel.isUpdateOkIcon.observe(this, aBoolean -> {
            if (aBoolean) {
                img_okay.setEnabled(true);
                img_okay.setClickable(true);
                img_okay.setAlpha(1.0f);
            } else {
                /* for this condition while select popup fw update check*/
                if (percentage >= 100) {
                    if (isSelectPopUpFwUpdateCheck) {
                        if (dialogMode.equals("NORMAL")) {
                            img_okay.setEnabled(false);
                            img_okay.setClickable(false);
                            img_okay.setAlpha(0.5f);
                        } else {
                            img_okay.setEnabled(true);
                            img_okay.setClickable(true);
                            img_okay.setAlpha(1.0f);
                        }
                    } else {
                        img_okay.setEnabled(true);
                        img_okay.setClickable(true);
                        img_okay.setAlpha(1.0f);
                    }
                } else {
                    if (isSelectPopUpFwUpdateCheck) {
                        img_okay.setEnabled(true);
                        img_okay.setClickable(true);
                        img_okay.setAlpha(1.0f);
                    }
                }
            }
        });

        homeViewModel.isEnableMultiWindowMode.observe(this, new EventObserver<>(aBoolean -> {
            if (aBoolean) {
                Log.e("1", "getEnterExitMultiWindowMode: " + aBoolean);
                dismiss();
            }
        }));

        return dialog;
    }


    private boolean isValidPresetName(String presetName) {
        return presetName.length() >= 3 && presetName.length() <= 32;
    }

    private boolean isValidUsername(String userName) {
        return userName.length() >= 8 && userName.length() < 32;
    }

    private void hideKeyboard(ConstraintLayout constraintLayout) {
        try {
            //  if (specialCharacterLayout.getId() == R.id. special_character_layout) {
            if (constraintLayout != null) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(constraintLayout.getWindowToken(), 0);
            }
            //   }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideVersionInformationDialog() {
        Log.e("hideVersionInformationDialog: ", "true");
        fw_version_layout.setVisibility(View.GONE);
        hideNightwaveFwUpdateLayout();

        new Handler().postDelayed(() -> homeViewModel.hasUpdateOkIcon(true), 200);
        hideOpsinFwChangesLayout();
    }

    private void showVersionInformationDialog() {
        Log.e("showVersionInformationDialog: ", "true");
        fw_version_layout.setVisibility(View.VISIBLE);
        switch (currentCameraSsid) {
            case NIGHTWAVE:
                nightwaveFwUpdateLayout.setVisibility(View.VISIBLE);
                proceed_update_text.setText(getString(R.string.proceed_with_update_nightwave));
                break;
            case OPSIN:
                percentage = 0;
                showOpsinFwUpdateLayout();
                opsin_proceed_update_text.setText(getString(R.string.proceed_with_update));
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e("NoticeDialogFragment","showVersionInformationDialog: ");
                break;
        }
        getFirmwareVersions();
    }

    private void getFirmwareVersions() {
        if (homeViewModel.getManifest() != null) {
            cameraReleaseVersion = homeViewModel.getCurrentFwVersion().getReleaseVersion();
            if (cameraReleaseVersion == null)
                cameraReleaseVersion = "UNKNOWN";

            appRelease = homeViewModel.getManifest().getVersions().getRelease();
            appFPGA = homeViewModel.getManifest().getVersions().getFpga();
            appRISCV = homeViewModel.getManifest().getVersions().getRiscv();
            appOVERLAY = homeViewModel.getManifest().getVersions().getOverlay();
            appRISCV_RECOVERY = homeViewModel.getManifest().getVersions().getRiscvRecovery();
            appWIFI_rtos = homeViewModel.getManifest().getVersions().getWiFiRtos();
            appWIFI_BLE = homeViewModel.getManifest().getVersions().getwIFI_BLE();
        }
        // not required
//        if (homeViewModel.getManifest() != null) {
//            appRelease = homeViewModel.getManifest().getVersions().getRelease();
//            appFPGA = homeViewModel.getManifest().getVersions().getFpga();
//            appRISCV = homeViewModel.getManifest().getVersions().getrISCV();
//            appWIFI_rtos = homeViewModel.getManifest().getVersions().getwIFI_RTOS();
//        }

        if (homeViewModel.getCurrentFwVersion() != null) {
            cameraFPGA = homeViewModel.getCurrentFwVersion().getFpga();
            cameraRISCV = homeViewModel.getCurrentFwVersion().getRiscv();
            cameraRISCV_RECOVERY = homeViewModel.getCurrentFwVersion().getRiscvRecovery();
            cameraOVERLAY = homeViewModel.getCurrentFwVersion().getOverlay();
            cameraWIFI_rtos = homeViewModel.getCurrentFwVersion().getWiFiRtos();
            cameraWIFI_BLE = homeViewModel.getCurrentFwVersion().getBle();
        }

        switch (currentCameraSsid) {
            case NIGHTWAVE:
                nightwaveFwUpdateLayout.setVisibility(View.VISIBLE);
                fpga_version_current.setText(cameraFPGA);
                fpga_version_new.setText(appFPGA);
                riscv_version_current.setText(cameraRISCV);
                riscv_version_new.setText(appRISCV);
                wifi_version_current.setText(cameraWIFI_rtos);
                wifi_version_new.setText(appWIFI_rtos);
                top_version_current.setText(cameraReleaseVersion);
                top_version_new.setText(appRelease);
                hideOpsinFwChangesLayout();
                break;
            case OPSIN:
                hideNightwaveFwUpdateLayout();
                opsinFwUpdateLayout.setVisibility(View.VISIBLE);
                opsinFwStatusProgressbar.setVisibility(View.VISIBLE);
                opsin_progress_status_value.setVisibility(View.VISIBLE);

            opsin_fpga_version_current.setText(cameraFPGA);
            opsin_fpga_version_new.setText(appFPGA);
            opsin_riscv_version_current.setText(cameraRISCV);
            opsin_riscv_version_new.setText(appRISCV);
            if (cameraWIFI_rtos == null || cameraWIFI_rtos.equalsIgnoreCase(""))
                cameraWIFI_rtos = getString(R.string.unknown);

            if (cameraWIFI_rtos.equalsIgnoreCase(getString(R.string.unknown))) {
                opsin_wifi_version_current.setText(cameraWIFI_rtos);
            } else {
                opsin_wifi_version_current.setText(cameraWIFI_rtos);
            }
            opsin_wifi_version_new.setText(appWIFI_rtos);

            /*for this opsin*/
            opsin_riscv_overlay_version_current.setText((cameraOVERLAY != null && !cameraOVERLAY.isEmpty()) ? cameraOVERLAY.trim() : getString(R.string.unknown));
            opsin_riscv_overlay_version_new.setText((appOVERLAY != null && !appOVERLAY.isEmpty()) ? appOVERLAY.trim() : getString(R.string.unknown));

            String camera_recovery_version_split = (cameraRISCV_RECOVERY != null && !cameraRISCV_RECOVERY.isEmpty()) ? cameraRISCV_RECOVERY.trim() : getString(R.string.unknown);
            String[] camera_riscv_recovery_version = camera_recovery_version_split.split("-");
            opsin_riscv_recovery_version_current.setText(camera_riscv_recovery_version[0]);

            String app_recovery_version_split = (appRISCV_RECOVERY != null && !appRISCV_RECOVERY.isEmpty()) ? appRISCV_RECOVERY.trim() : getString(R.string.unknown);
            String[] app_riscv_recovery_version = app_recovery_version_split.split("-");
            opsin_riscv_recovery_version_new.setText(app_riscv_recovery_version[0]);

            opsin_bluetooth_version_current.setText(cameraWIFI_BLE);
            opsin_bluetooth_version_new.setText(appWIFI_BLE);

                showFwUpdateStatus();
                break;
            case NIGHTWAVE_DIGITAL:
                Log.e("NoticeDialogFragment","getFirmwareVersions : NW_Digital");
                break;
        }
    }

    double percentage = 0;

    public void showFwUpdateStatus() {
        cameraWIFI_rtos = homeViewModel.getCurrentFwVersion().getWiFiRtos();
        cameraFPGA = homeViewModel.getCurrentFwVersion().getFpga();
        cameraRISCV = homeViewModel.getCurrentFwVersion().getRiscv();
        cameraOVERLAY = homeViewModel.getCurrentFwVersion().getOverlay();
        cameraRISCV_RECOVERY = homeViewModel.getCurrentFwVersion().getRiscvRecovery();
        cameraWIFI_BLE = homeViewModel.getCurrentFwVersion().getBle();

        appWIFI_rtos = homeViewModel.getManifest().getVersions().getWiFiRtos();
        appFPGA = homeViewModel.getManifest().getVersions().getFpga();
        appRISCV = homeViewModel.getManifest().getVersions().getRiscv();
        appOVERLAY = homeViewModel.getManifest().getVersions().getOverlay();
        appRISCV_RECOVERY = homeViewModel.getManifest().getVersions().getRiscvRecovery();
        appWIFI_BLE = homeViewModel.getManifest().getVersions().getwIFI_BLE();
        String[] split = appRISCV_RECOVERY.split("-");

        if (!appWIFI_rtos.equalsIgnoreCase(cameraWIFI_rtos)) {
            opsin_wifi_status.setText(getString(R.string.pending));
            opsin_wifi.setBackgroundResource(R.color.white);
        } else {
            percentage = percentage + 16.67;
            opsin_wifi_status.setText(getString(R.string.complete));
            opsin_wifi.setBackgroundResource(R.color.light_gray);
            opsin_wifi.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        if (!appWIFI_BLE.equalsIgnoreCase(cameraWIFI_BLE)) {
            opsin_bluetooth_status.setText(getString(R.string.pending));
            opsin_bluetooth.setBackgroundResource(R.color.white);
        } else {
            percentage = percentage + 16.67;
            opsin_bluetooth_status.setText(getString(R.string.complete));
            opsin_bluetooth.setBackgroundResource(R.color.light_gray);
            opsin_bluetooth.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        if (!appFPGA.equalsIgnoreCase(cameraFPGA)) {
            opsin_fpga_status.setText(getString(R.string.pending));
            opsin_core.setBackgroundResource(R.color.white);
        } else {
            percentage = percentage + 16.67;
            opsin_fpga_status.setText(getString(R.string.complete));
            opsin_core.setBackgroundResource(R.color.light_gray);
            opsin_core.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        if (!appRISCV.equalsIgnoreCase(cameraRISCV)) {
            opsin_riscv_status.setText(getString(R.string.pending));
            opsin_firmware.setBackgroundResource(R.color.white);
        } else {
            percentage = percentage + 16.67;
            opsin_riscv_status.setText(getString(R.string.complete));
            opsin_firmware.setBackgroundResource(R.color.light_gray);
            opsin_firmware.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        if (!appOVERLAY.equalsIgnoreCase(cameraOVERLAY)) {
            opsin_overlay.setBackgroundResource(R.color.white);
            opsin_riscv_overlay_status.setText(getString(R.string.pending));
        } else {
            percentage = percentage + 16.67;
            opsin_riscv_overlay_status.setText(getString(R.string.complete));
            opsin_overlay.setBackgroundResource(R.color.light_gray);
            opsin_overlay.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        try {
            String[] cameraRecovery = cameraRISCV_RECOVERY.split("-");
            if (!cameraRecovery[0].equalsIgnoreCase(split[0])) {
                opsin_riscv_recovery_status.setText(getString(R.string.pending));
                opsin_recovery.setBackgroundResource(R.color.white);
            } else {
                percentage = percentage + 16.67;
                opsin_riscv_recovery_status.setText(getString(R.string.complete));
                opsin_recovery.setBackgroundResource(R.color.light_gray);
                opsin_recovery.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String status_Value = Math.round(percentage) + "%";
            opsin_progress_status_value.setText(status_Value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        new Handler().postDelayed(() -> homeViewModel.hasUpdateOkIcon(false), 200);

    }

    public void hideOpsinFwChangesLayout() {
        //need to hide status progressbar,value,opsin fw update layout
        opsinFwUpdateLayout.setVisibility(View.GONE);
        opsinFwStatusProgressbar.setVisibility(View.GONE);
        opsin_progress_status_value.setVisibility(View.GONE);
        opsin_proceed_update_text.setVisibility(View.GONE);
        percentage = 0;
    }

    public void hideNightwaveFwUpdateLayout() {
        nightwaveFwUpdateLayout.setVisibility(View.GONE);
    }

    public void showOpsinFwUpdateLayout() {
        opsinFwUpdateLayout.setVisibility(View.VISIBLE);
        opsin_progress_status_value.setVisibility(View.VISIBLE);
        opsinFwStatusProgressbar.setVisibility(View.VISIBLE);
        opsin_proceed_update_text.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /* NWD password reset 10sec delay logic*/
/*        if (homeViewModel != null){
            homeViewModel.getProgress().removeObservers(getViewLifecycleOwner());
            homeViewModel.getShowDialog().removeObservers(getViewLifecycleOwner());
        }*/

        if (homeViewModel != null){
            homeViewModel.firmwarePromptDismissed().removeObservers(getViewLifecycleOwner());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    private void applyInputFilters() {
        InputFilter blockEmojiFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                int type = Character.getType(source.charAt(i));

                if (type == Character.SURROGATE ||
                        type == Character.OTHER_SYMBOL ||
                        type == Character.NON_SPACING_MARK ||
                        Character.isWhitespace(source.charAt(i))) {
                    return "";
                }


                if (source.charAt(i) > 0x7F) {
                    return "";
                }
            }
            return null;
        };
        saveSettingsEditText.setFilters(new InputFilter[]{blockEmojiFilter});
    }

    private String removeEmojiAndWhitespace(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int type = Character.getType(c);

            boolean isEmoji = type == Character.SURROGATE || type == Character.OTHER_SYMBOL || c > 0x7F;
            if (!Character.isWhitespace(c) && !isEmoji) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isAllowedCharacter(char c) {
        // Check if the character is a number, alphabet, special characters
        // Add any additional special characters you want to allow
        String allowedCharacters = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
        return Character.isLetterOrDigit(c) || allowedCharacters.contains(String.valueOf(c));
    }
}

