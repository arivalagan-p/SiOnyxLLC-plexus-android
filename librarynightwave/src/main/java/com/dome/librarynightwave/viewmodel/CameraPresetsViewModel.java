package com.dome.librarynightwave.viewmodel;

import static com.dome.librarynightwave.model.repository.TCPRepository.commandRequested;
import static com.dome.librarynightwave.model.services.TCPCommunicationService.applyOpsinPeriodicRequest;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_FLIP_VIDEO;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_INVERT_VIDEO;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_IRCUT;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_NW_LED;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_CLOCK_MODE;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_FPS;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_GPS;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_META_DATA;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_MIC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_MONOCHROMATIC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_NOISE_REDUCTION;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_NUC;
import static com.dome.librarynightwave.utils.SCCPConstants.SETTING_ID_OPSIN_ROI;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.persistence.savesettings.SaveSettings;
import com.dome.librarynightwave.model.repository.CameraPresetsRepository;
import com.dome.librarynightwave.model.repository.TCPRepository;
import com.dome.librarynightwave.utils.Event;
import com.dome.librarynightwave.utils.SCCPConstants;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class CameraPresetsViewModel extends AndroidViewModel {
    private final Application application;
    private static CameraPresetsRepository cameraPresetsRepository;
    private final TCPRepository tcpRepository;

    public Context mContext;

    public enum ApplyPreset {
        NONE,
        PRESET_PROCEED_NEXT_COMMAND,
        PRESET_COMMAND_PROCEEDED,
        APPLY_PRESET_VALUES,
    }

    public static ApplyPreset applyPreset = ApplyPreset.NONE;
    public boolean isSelectDeletePreset;

    public boolean isSelectDeletePreset() {
        return isSelectDeletePreset;
    }

    public void setSelectDeletePreset(boolean selectDeletePreset) {
        isSelectDeletePreset = selectDeletePreset;
    }

    public CameraPresetsViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        cameraPresetsRepository = CameraPresetsRepository.getInstance(application);
        tcpRepository = TCPRepository.getInstance(application);
        mContext = application.getApplicationContext();
    }

    public SaveSettings saveSettings;

    public SaveSettings getSaveSettings() {
        return saveSettings;
    }

    public void setSaveSettings(SaveSettings saveSettings) {
        this.saveSettings = saveSettings;
    }

    private final MutableLiveData<Event<Boolean>> _isCancelPresetView = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isCancelPresetView = _isCancelPresetView;

    private final MutableLiveData<Event<Boolean>> _isDeletePreset = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isDeletePreset = _isDeletePreset;

    private final MutableLiveData<Event<Boolean>> _hasDeletePreset = new MutableLiveData<>();
    public LiveData<Event<Boolean>> hasDeletePreset = _hasDeletePreset;

    private final MutableLiveData<Event<Boolean>> _isApplyPreset = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isApplyPreset = _isApplyPreset;

    private final MutableLiveData<Event<Boolean>> _hasShowApplyOption = new MutableLiveData<>();
    public LiveData<Event<Boolean>> hasShowApplyOption = _hasShowApplyOption;

    private final MutableLiveData<Event<Boolean>> _isApplySettingsSuccessfully = new MutableLiveData<>();
    public LiveData<Event<Boolean>> isApplySettingsSuccessfully = _isApplySettingsSuccessfully;

    public void onCancelPresetView() {
        _isCancelPresetView.setValue(new Event<>(true));
    }

    public void onDeletePreset() {
        _isDeletePreset.setValue(new Event<>(true));
    }

    public void hasDeletePreset(boolean isSelectDeletePreset) {
        _hasDeletePreset.setValue(new Event<>(isSelectDeletePreset));
    }

    public void onApplyPreset() {
        _isApplyPreset.setValue(new Event<>(true));
    }

    public void hasAppliedSettings() {
        _isApplySettingsSuccessfully.postValue(new Event<>(true));
    }

    public LiveData<Event<Boolean>> hasSavedSettingsSuccessfully() {
        return cameraPresetsRepository.hasSavedSettingsSuccessfully();
    }

    public void hasShowApplyOption(boolean hasShowDeleteOption) {
        _hasShowApplyOption.setValue(new Event<>(hasShowDeleteOption));
    }

    public Single<Boolean> isPresetAvailable(String preset_name, boolean isNightwave) {
        return cameraPresetsRepository.isPresetAvailable(preset_name, isNightwave);
    }

    public void saveSettingsValue(ArrayList<SaveSettings> saveSettings) {
        cameraPresetsRepository.saveSettingsValue(saveSettings);
    }

    public void updatePreset(int setting_id, String setting_name, byte setting_value, String display_value, boolean is_nightwave, LocalDateTime datetime) {
        cameraPresetsRepository.updatePreset(setting_id, setting_name, setting_value, display_value, is_nightwave, datetime);
    }

    public Single<List<SaveSettings>> getSavedSettings(String preset_name, boolean is_nightwave) {
        return cameraPresetsRepository.getSavedSettings(preset_name, is_nightwave);
    }

    public List<SaveSettings> getSavedPreSettings(boolean is_nightwave) {
        return cameraPresetsRepository.getSavedPreSettings(is_nightwave);
    }

    public void deletePreset(String preset_name, boolean is_nightwave) {
        cameraPresetsRepository.deletePreset(preset_name, is_nightwave);
    }

    public List<Integer> getPresetCount(boolean is_nightwave) {
        return cameraPresetsRepository.getPresetCount(is_nightwave);
    }

    /*Apply Night wave Settings*/
    private int index = 0;
    private final Handler mHandler = new Handler();

    public static final Object lockApplySettings = new Object();
    public static boolean responseReceived = false;
    public static boolean isRequestFailed = false;

    public void applySettings(ArrayList<SaveSettings> presetSettings) {
        synchronized (lockApplySettings) {
            if (presetSettings.size() > 0 && index < presetSettings.size()) {
                responseReceived = false;
                if (applyPreset == CameraPresetsViewModel.ApplyPreset.APPLY_PRESET_VALUES) {
                    SaveSettings saveSettings = presetSettings.get(index);
                    int settingId = saveSettings.getSetting_id();
                    String settingName = saveSettings.getSetting_name();
                    int settingValue = saveSettings.getSetting_value();
                    String displayValue = saveSettings.getDisplay_value();
                    boolean isNightwave = saveSettings.isIs_nightwave();
                    LocalDateTime datetime = saveSettings.getDatetime();

                    applyPreset = ApplyPreset.PRESET_COMMAND_PROCEEDED;

                    if (isNightwave) {
                        switch (settingId) {
                            case SETTING_ID_NW_LED:
                                if (settingValue == SCCPConstants.SCCP_LED.TRUE.getValue()) {
                                    tcpRepository.setLedEnableState(0);
                                } else {
                                    tcpRepository.setLedEnableState(1);
                                }
                                break;
                            case SETTING_ID_NW_INVERT_VIDEO:
                                if (settingValue == SCCPConstants.SCCP_INVERT_IMAGE.FALSE.getValue()) {
                                    tcpRepository.setInvertVideo(1);
                                } else {
                                    tcpRepository.setInvertVideo(0);
                                }
                                break;
                            case SETTING_ID_NW_FLIP_VIDEO:
                                if (settingValue == SCCPConstants.SCCP_FLIP_IMAGE.FALSE.getValue()) {
                                    tcpRepository.setFlipVideo(0);
                                } else {
                                    tcpRepository.setFlipVideo(1);
                                }
                                break;
                            case SETTING_ID_NW_IRCUT:
                                if (settingValue == SCCPConstants.SCCP_IRCUT.OUT.getValue()) {
                                    tcpRepository.setIRCut(0);
                                } else if (settingValue == SCCPConstants.SCCP_IRCUT.IN.getValue()) {
                                    tcpRepository.setIRCut(1);
                                } else {
                                    tcpRepository.setIRCut(2);
                                }
                                break;
                        }
                    } else {
                        switch (settingId) {
                            case SETTING_ID_OPSIN_NUC:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_NUC_STATE.ENABLED.getValue()) {
                                    tcpRepository.setNUC(SCCPConstants.SCCP_OPSIN_NUC_STATE.ENABLED.getValue());
                                } else {
                                    tcpRepository.setNUC(SCCPConstants.SCCP_OPSIN_NUC_STATE.DISABLED.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_MIC:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue()) {
                                    tcpRepository.setMicState(SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue());
                                } else {
                                    tcpRepository.setMicState(SCCPConstants.SCCP_OPSIN_MIC_STATE.DISABLED.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_GPS:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_GPS_STATE.ENABLED.getValue()) {
                                    tcpRepository.setGpsPower(SCCPConstants.SCCP_OPSIN_GPS_STATE.ENABLED.getValue());
                                } else {
                                    tcpRepository.setGpsPower(SCCPConstants.SCCP_OPSIN_GPS_STATE.DISABLED.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_FPS:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue()) {
                                    tcpRepository.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue());
                                } else if (settingValue == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue()) {
                                    tcpRepository.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue());
                                } else if (settingValue == SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_90.getValue()) {
                                    tcpRepository.setSetFrameRate(SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_90.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_MONOCHROMATIC:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.ENABLED.getValue()) {
                                    tcpRepository.setOpsinMonochromatic(SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.ENABLED.getValue());
                                } else {
                                    tcpRepository.setOpsinMonochromatic(SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.DISABLED.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_NOISE_REDUCTION:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.ENABLED.getValue()) {
                                    tcpRepository.setOpsinNoiseReduction(SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.ENABLED.getValue());
                                } else {
                                    tcpRepository.setOpsinNoiseReduction(SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.DISABLED.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_ROI:
                                if (settingValue == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue()) {
                                    tcpRepository.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue());
                                } else if (settingValue == SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue()) {
                                    tcpRepository.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue());
                                } else {
                                    tcpRepository.setOpsinROI(SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_OFF.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_CLOCK_MODE:
                                if (settingValue == SCCPConstants.SCCP_CLOCK_MODE.GPS.getValue()) {
                                    tcpRepository.setOpsinClockMode((byte) SCCPConstants.SCCP_CLOCK_MODE.GPS.getValue());
                                } else {
                                    tcpRepository.setOpsinClockMode((byte) SCCPConstants.SCCP_CLOCK_MODE.SYSTEM.getValue());
                                }
                                break;
                            case SETTING_ID_OPSIN_META_DATA:
                                if (settingValue == SCCPConstants.SCCP_OPSIN_METADATA_STATE.ENABLED.getValue()) {
                                    tcpRepository.setOpsinMetadata(SCCPConstants.SCCP_OPSIN_METADATA_STATE.ENABLED.getValue());
                                } else {
                                    tcpRepository.setOpsinMetadata(SCCPConstants.SCCP_OPSIN_METADATA_STATE.DISABLED.getValue());
                                }
                                break;
                        }
                    }

                    Log.e("TAG", "applySettings: " + index + " STEP1: " + applyPreset.name() + " " + applyOpsinPeriodicRequest.name() + " " + commandRequested.name());
                    try {
                        while (!responseReceived) {
                            lockApplySettings.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    applySettings(presetSettings);
                } else if (applyPreset == ApplyPreset.PRESET_PROCEED_NEXT_COMMAND) {
                    if (!isRequestFailed) {
                        index++;
                    }
                    isRequestFailed = false;
                    applyPreset = CameraPresetsViewModel.ApplyPreset.APPLY_PRESET_VALUES;
                    applySettings(presetSettings);
                } else if (applyPreset == ApplyPreset.PRESET_COMMAND_PROCEEDED) {
                    mHandler.postDelayed(() -> applySettings(presetSettings), 200);
                }

            } else {
                applyPreset = ApplyPreset.NONE;
                index = 0;
                hasAppliedSettings();
                Log.d("TAG", "applySettings: complete");
            }
        }

    }


    /*Get Night wave Settings Value Based on Tab Position*/
    public byte getInvertVideoValue(int position) {
        if (position == 0) {
            return (byte) SCCPConstants.SCCP_INVERT_IMAGE.FALSE.getValue();
        } else {
            return (byte) SCCPConstants.SCCP_INVERT_IMAGE.TRUE.getValue();
        }
    }

    public byte getFlipVideoValue(int position) {
        if (position == 0) {
            return (byte) SCCPConstants.SCCP_FLIP_IMAGE.FALSE.getValue();
        } else {
            return (byte) SCCPConstants.SCCP_FLIP_IMAGE.TRUE.getValue();
        }
    }

    public byte getIRCutValue(int position) {
        if (position == 0) {
            return (byte) SCCPConstants.SCCP_IRCUT.OUT.getValue();
        } else if (position == 1) {
            return (byte) SCCPConstants.SCCP_IRCUT.IN.getValue();
        } else {
            return (byte) SCCPConstants.SCCP_IRCUT.AUTO.getValue();
        }
    }

    public byte getLedValue(int position) {
        if (position == 0) {
            return (byte) SCCPConstants.SCCP_LED.TRUE.getValue();
        } else {
            return (byte) SCCPConstants.SCCP_LED.FALSE.getValue();
        }
    }


    /*Get Night wave Settings Display Value Based on Tab Position*/
    public String getInvertVideoDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getFlipVideoDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getIRCutDisplayValue(int position) {
        if (position == 0) {
            return "OFF";
        } else if (position == 1) {
            return "ON";
        } else {
            return "AUTO";
        }
    }

    public String getLedDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }


    /*Get OPSIN Settings Value Based on Tab Position*/
    public byte getNUCValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_NUC_STATE.ENABLED.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_NUC_STATE.DISABLED.getValue();
        }
    }

    public byte getMicValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_MIC_STATE.ENABLED.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_MIC_STATE.DISABLED.getValue();
        }
    }

    public byte getGpsValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_GPS_STATE.ENABLED.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_GPS_STATE.DISABLED.getValue();
        }
    }

    public short getFpsValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_30.getValue();
        } else if (position == 1) {
            return SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_60.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_FRAME_RATE.FRAME_RATE_90.getValue();
        }
    }

    public byte getMonochromaticValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.ENABLED.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_MONOCHROMATIC_STATE.DISABLED.getValue();
        }
    }

    public byte getNoiseReductionValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.ENABLED.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_NOISE_REDUCTION_STATE.DISABLED.getValue();
        }
    }

    public byte getRoiValue(int position) {
        if (position == 0) {
            return SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_30.getValue();
        } else if (position == 1) {
            return SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_50.getValue();
        } else {
            return SCCPConstants.OPSIN_SCCP_ROI_MODE.SCCP_ROI_OFF.getValue();
        }
    }

    public byte getClockValue(int position) {
        if (position == 0) {
            return (byte) SCCPConstants.SCCP_CLOCK_MODE.GPS.getValue();
        } else {
            return (byte) SCCPConstants.SCCP_CLOCK_MODE.SYSTEM.getValue();
        }
    }

    public byte getMetadataValue(int position) {
        if (position == 0) {
            return SCCPConstants.SCCP_OPSIN_METADATA_STATE.ENABLED.getValue();
        } else {
            return SCCPConstants.SCCP_OPSIN_METADATA_STATE.DISABLED.getValue();
        }
    }

    /*Get OPSIN Settings Display Value Based on Tab Position*/
    public String getNUCDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getMicDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getGpsDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getFpsDisplayValue(int position) {
        if (position == 0) {
            return "30";
        } else if (position == 1) {
            return "60";
        } else {
            return "90";
        }
    }

    public String getMonochromaticDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getNoiseReductionDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }

    public String getRoiDisplayValue(int position) {
        if (position == 0) {
            return "30";
        } else if (position == 1) {
            return "50";
        } else {
            return "OFF";
        }
    }

    public String getClockDisplaValue(int position) {
        if (position == 0) {
            return "GPS";
        } else {
            return "NONE";
        }
    }

    public String getMetadataDisplayValue(int position) {
        if (position == 0) {
            return "ON";
        } else {
            return "OFF";
        }
    }
}
