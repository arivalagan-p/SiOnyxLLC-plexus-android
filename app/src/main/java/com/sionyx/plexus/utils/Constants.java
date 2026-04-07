package com.sionyx.plexus.utils;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;
import static com.dome.librarynightwave.utils.Constants.FILE_NOT_FOUND;
import static com.dome.librarynightwave.utils.Constants.ON_SUCCESS;
import static com.dome.librarynightwave.utils.Constants.SERVER_ERROR;
import static com.dome.librarynightwave.utils.Constants.UNAUTHORISED;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;
import static com.sionyx.plexus.ApplicationClass.appContext;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sionyx.plexus.R;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constants {
    public static final int WIFI_NOT_CONNECTED = -1;
    public static final int WIFI_AVAILABLE = 0;
    public static final int WIFI_CONNECTED = 1;

    public static final int WI_FI_PANEL_RESULT_CODE = 100;
    public static final int REQUEST_ENABLE_BT = 200;
    public static final int REQUEST_BATTERY_OPTIMIZATION = 300;
    public static final int REQUEST_ENABLE_GPS = 400;


    public static final int NONE = 0;
    public static final int HOME = 1;
    public static final int LIVE_VIEW = 2;
    public static final int CAMERA_SETTINGS = 3;
    public static volatile int mSocketState = NONE;

    private int getState() {
        return mSocketState;
    }

    public static final MutableLiveData<Boolean> _isShowExpandMenu1 = new MutableLiveData<>();
    public static LiveData<Boolean> isShowExpandMenu1 = _isShowExpandMenu1;

    public static final MutableLiveData<Boolean> _isShowAllMenu1 = new MutableLiveData<>();
    public static LiveData<Boolean> isShowAllMenu1 = _isShowAllMenu1;

    public static ArrayList<String> firmwareUpdateSequence = new ArrayList<>();

    public static final int ASPECT_RATIO_WIDTH_4 = 4;
    public static final int ASPECT_RATIO_HEIGHT_3 = 3;
    public static final int ASPECT_RATIO_WIDTH_16 = 16;
    public static final int ASPECT_RATIO_HEIGHT_9 = 9;
    public static final int OPSIN_STREAMING_SUPPORTS_FROM = 23;
    public static final String OPSIN_RISCV_RTC_SUPPORTS_FROM = "24.1.0";
    public static final String OPSIN_WIFI_RTC_SUPPORTS_FROM = "2.4.0";
    public static final String OPSIN_RECORDING_STATUS_SUPPORTS_FROM = "22.4.12";
    public static final int OPSIN_FIRMWARE_UPGRADE_BATTERY_THRESHOLD = 50;
    public static final String OPSIN_RISCV_VERSION = "24.2.1";
    public static final String OPSIN_WIFI_RTOS_VERSION = "2.4.2";
    public static final float OPSIN_CAMERA_FPGA_VERSION = 206.2F;
    public static boolean isLoginEnable = false; //  set this to true if you want handle sign-in

    /*
     * Start Of Osprey Base Version.
     * Used to display the Top version as 2.0.0 if RISCV is 23.2.1, FPGA is 206.2, Wi-Fi is 2.3.9 and Overlay is 1.1.4
     */
    public static final String RISCV_BASE_VERSION_TO_DISPLAY_TOP = "23.2.1";
    public static final String FPGA_BASE_VERSION_TO_DISPLAY_TOP = "206.2";
    public static final String WIFI_BASE_VERSION_TO_DISPLAY_TOP = "2.3.9";
    public static final String OVERLAY_BASE_VERSION_TO_DISPLAY_TOP = "1.1.4";
    /*End Of Osprey Base Version */

    /*AWS*/
    public static final String REGISTERATION_BASE_URL = "https://sionyx-development.s3.us-east-2.amazonaws.com/Signup.html";
    public static final String FORGOT_PASSWORD_URL = "https://z5fy7y6yx9.execute-api.us-east-2.amazonaws.com/Sionyx/";
    public static final String CHANGE_NEW_PASSWORD_URL = "https://gxrvlg26f2.execute-api.us-east-2.amazonaws.com/Sionyx/";
    public static final String PROFILE_URL = "  https://fp4lv4288j.execute-api.us-east-2.amazonaws.com/Sionyx/";


    /* for this split storage volume values*/
    private static final long KB_TO_BYTES = 1024L;
    private static final long BYTES = 1L;
    private static final long MB_TO_BYTES = KB_TO_BYTES * 1024L;
    private static final long GB_TO_BYTES = MB_TO_BYTES * 1024L;
    private static final long TB_TO_BYTES = GB_TO_BYTES * 1024L;

    public static final String QR_CODE_OPSIN_PREFIX = "MDV";
    public static final String QR_CODE_NIGHTWAVE_PREFIX = "CRV";

    public static SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
    }

    public static SimpleDateFormat simpleTimeFormat() {
        return new SimpleDateFormat("HH:mm a", Locale.getDefault());
    }
    public static SimpleDateFormat simple24HourTimeFormat() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public static SimpleDateFormat simple12HourTimeFormat() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    public static long getAvailableInternalStorageSize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    public static void makeToast(final String message) {
        runOnUiThread(() -> {
            if (message != null)
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
        });
    }


    /* for this sd card storage value split storage value E.g 22GB -> numeric -> '22' and unit 'GB'*/
    public static String[] splitStorageValue(String storageValue) {
        // Define the pattern to match the numeric value and the unit
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(\\S+)");
        Matcher matcher = pattern.matcher(storageValue.trim());
        // Initialize an array to store the numeric value and the unit
        String[] result = new String[2];
        // Check if the pattern matches the input string
        if (matcher.matches()) {
            // Extract the numeric value and the unit
            result[0] = matcher.group(1); // Numeric value
            result[1] = matcher.group(2); // Unit
        }
        return result;
    }

    /* after split value pass to covert into bytes and return long value*/
    public static long convertToBytes(double value, String unit) {
        switch (unit.toLowerCase()) {
            case "b":
                return (long) (value * BYTES);
            case "kb":
                return (long) (value * KB_TO_BYTES);
            case "mb":
                return (long) (value * MB_TO_BYTES);
            case "gb":
                return (long) (value * GB_TO_BYTES);
            case "tb":
                return (long) (value * TB_TO_BYTES);
            default:
                throw new IllegalArgumentException("Invalid unit: " + unit);
        }
    }

    public static String convert12to24Hrs(String time12hr) {
        String convertTime = "";
        try {
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm a", Locale.getDefault());
            Date date = sdf12.parse(time12hr);
            if (date != null) {
                convertTime = sdf24.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return convertTime;
    }

    public static String convert24to12Hrs(String time24hr) {
        String convertTime = "";
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm a", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf24.parse(time24hr);
            if (date != null) {
                convertTime = sdf12.format(date).toUpperCase();
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return convertTime;
    }

    public static boolean isTimeFormat24Hr(Context context) {
        String timeFormat = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
        return "24".equals(timeFormat);
    }
    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // Convert the entire string to lowercase
        input = input.toLowerCase();
        // Capitalize the first letter
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String getFile() {
        String currentCamera = "";
        switch (currentCameraSsid) {
            case OPSIN:
                currentCamera = "Opsin";
                break;
            case NIGHTWAVE:
                currentCamera = "Nightwave_Analog";
                break;
            case NIGHTWAVE_DIGITAL:
                currentCamera = "Nightwave_Digital";
                break;
        }
        return currentCamera;
    }

    public static void apiErrorMessage(Context context, int responseCode){
        try {
            String errMsg = "";

            switch (responseCode){
                case ON_SUCCESS:
                    errMsg = context.getString(R.string.api_on_success_msg);
                    break;
                case UNAUTHORISED:
                    errMsg = context.getString(R.string.api_unauthorized_msg);
                    break;
                case FILE_NOT_FOUND:
                    errMsg = context.getString(R.string.api_file_not_found_msg);;
                    break;
                case SERVER_ERROR:
                    errMsg = context.getString(R.string.api_server_error_msg);
                    break;

                default:
                    errMsg = context.getString(R.string.api_error_msg);
                    break;
            }

            Log.d("Constants", "apiErrorMessage: " + responseCode);
            Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            Log.d("Constants", "apiErrorMessage: " + e.getMessage());
        }



    }

}
