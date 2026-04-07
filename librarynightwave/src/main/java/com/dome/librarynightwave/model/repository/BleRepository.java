package com.dome.librarynightwave.model.repository;

import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.isSdk12AndAbove;
import static com.dome.librarynightwave.utils.Constants.isSDK14;
import static com.dome.librarynightwave.utils.Constants.isSDK15;
import static com.dome.librarynightwave.utils.Constants.isSDK16AndAbove;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.receivers.BLEReceiver;
import com.dome.librarynightwave.utils.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BleRepository {
    String TAG = "BleRepository";
    private BluetoothAdapter bluetoothAdapter = null;
    private final MutableLiveData<Boolean> isBluetoothSupported = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isBluetoothDisabled = new MutableLiveData<>();

    private final MutableLiveData<Event<Boolean>> isBleScanning = new MutableLiveData<>();
    private final Application application;
    private final BLEReceiver bleReceiver;
    private static BleRepository bleRepository = null;

    private final MutableLiveData<Object> bleResult = new MutableLiveData<>();
    private final List<WiFiHistory> wifiResults = new ArrayList<>();
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning = false;
    private final Handler handler = new Handler();
    // Stops scanning after 12 seconds.
    private static final long SCAN_PERIOD = 12000;

    private final String SERVICE_UUID = "00001803-0000-1000-8000-00805f9b34fb";
    // Define a ScanFilter
    private ScanFilter filter = new ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID));
            .build();
    private final ArrayList<ScanFilter> scanFilters = new ArrayList<>();
    // Define a ScanSettings object
    private final ScanSettings settings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();

    public MutableLiveData<Event<Boolean>> getIsBleScanning() {
        return isBleScanning;
    }

    public boolean isScanning() {
        return scanning;
    }

    private void setScanning(boolean scanning) {
        this.scanning = scanning;
        isBleScanning.postValue(new Event<>(scanning));
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            getServiceUUID(result);

            BluetoothDevice device = result.getDevice();
            String SSID, MAC;
            if (isSdk12AndAbove() || isSDK16AndAbove()) {
                if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    SSID = device.getName();
                    MAC = device.getAddress();
                    makeBleList(SSID, MAC);
                } else {
                    Log.e(TAG, "onScanResult: Permission Not Granted");
                }
            } else {
                // Below Android 12
                SSID = device.getName();
                MAC = device.getAddress();
                makeBleList(SSID, MAC);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_ALREADY_STARTED");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_INTERNAL_ERROR");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_FEATURE_UNSUPPORTED");
                    break;
                case 5:
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES");
                    break;
                case 6:
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_SCANNING_TOO_FREQUENTLY");
                    break;
                default:
                    break;
            }
        }
    };

    private void getServiceUUID(ScanResult result) {
        BluetoothDevice devices = result.getDevice();

        // Get the ScanRecord object associated with the scan result
        ScanRecord scanRecord = result.getScanRecord();

        // Get the list of service UUIDs advertised by the device
        List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();

        int advertiseFlags = scanRecord.getAdvertiseFlags();
        if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            String name = devices.getName();
            if (name != null && isCamera(name)) {
//                Log.e(TAG, "advertiseFlags: " + advertiseFlags + " " + name);
            }
        }

        if (serviceUuids != null && !serviceUuids.isEmpty()) {
            for (ParcelUuid serviceUuid : serviceUuids) {
                // Do something with the service UUID
                UUID uuid = serviceUuid.getUuid();
                if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String name = devices.getName();
                    if (name != null && isCamera(name)) {
//                        Log.e(TAG, "Service UUID found: " + uuid.toString() + " " + name + " " + advertiseFlags);
                    }
                }
            }
        } else {
//            Log.d(TAG, "No service UUIDs found.");
        }
    }

    private void makeBleList(String SSID, String MAC) {
        if (SSID != null) {
//            Log.e(TAG, "onScanResult: " + SSID + " " + MAC);
            SSID = SSID.replace("\"", "");
            if (isCamera(SSID)) {
                WiFiHistory wifiResult = new WiFiHistory();
                wifiResult.setCamera_ssid(SSID);
                wifiResult.setCamera_mac_address(MAC);
                wifiResult.setIs_wifi_connected(0);
                if (!wifiResults.contains(wifiResult)) {
                    wifiResults.add(wifiResult);
                }
                bleResult.setValue(wifiResults);
            }
        }
    }

    private boolean isCamera(String SSID) {
        return SSID.contains(FILTER_STRING1) || SSID.contains(FILTER_STRING2) || SSID.contains(FILTER_STRING3) || SSID.contains(FILTER_STRING4) || SSID.contains(FILTER_STRING5) || SSID.contains(FILTER_STRING6) || SSID.startsWith(FILTER_STRING6 );
    }

    public static BleRepository getInstance(Application application) {
        if (bleRepository == null) {
            bleRepository = new BleRepository(application);
        }
        return bleRepository;
    }

    public BleRepository(Application application) {
        this.application = application;
        bluetoothAdapter = getBluetoothAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        bleReceiver = new BLEReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (isSDK14() ||  isSDK15() || isSDK16AndAbove()) {
            ContextCompat.registerReceiver(application, bleReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        } else {
            application.registerReceiver(bleReceiver, intentFilter);
        }

    }


    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public LiveData<Boolean> isBluetoothSupported() {
        isBluetoothSupported.setValue(bluetoothAdapter != null);
        return isBluetoothSupported;
    }

    public LiveData<Boolean> isBluetoothDisabled() {
        isBluetoothDisabled.setValue(bluetoothAdapter != null && !bluetoothAdapter.isEnabled());
        return isBluetoothDisabled;
    }

    public LiveData<Boolean> startObserveBleOnOffState() {
        return bleReceiver.isBluetoothOnOffLiveState();
    }

    public void startBleDiscovery() {
        Log.e(TAG, "startBleDiscovery: " );
        if (!isScanning()) {
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
            }
            // Stops scanning after a predefined scan period.
            handler.postDelayed(() -> {
                setScanning(false);
                if (isSdk12AndAbove() || isSDK16AndAbove()) {
                    if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                            bluetoothLeScanner.stopScan(leScanCallback);
                            if (wifiResults.isEmpty()) {
                                bleResult.setValue(new ArrayList<WiFiHistory>());
                            } else {
                                bleResult.setValue(false);
                            }
                        }
                    }
                } else {
                    //Below Android 12
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        bluetoothLeScanner.stopScan(leScanCallback);
                        if (wifiResults.isEmpty()) {
                            bleResult.setValue(new ArrayList<WiFiHistory>());
                        } else {
                            bleResult.setValue(false);
                        }
                    }
                }
            }, SCAN_PERIOD);
            setScanning(true);


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                filter = new ScanFilter.Builder()
//                        .setAdvertisingDataType(ScanRecord.DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL)
//                        .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                        .build();
            }

            if (isSdk12AndAbove() || isSDK16AndAbove()){
                if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        wifiResults.clear();
                        scanFilters.clear();
                        scanFilters.add(filter);
                        bluetoothLeScanner.startScan(scanFilters, settings, leScanCallback);
                        bleResult.setValue(true);
                    }
                }
            } else {
                //Below Android 12
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    wifiResults.clear();
                    scanFilters.clear();
                    scanFilters.add(filter);
                    bluetoothLeScanner.startScan(scanFilters, settings, leScanCallback);
                    bleResult.setValue(true);
                }
            }
        }
    }

    public void stopBleDiscovery() {
        if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothLeScanner.stopScan(leScanCallback);
    }

    public MutableLiveData<Object> getBleScanResult() {
        return bleResult;
    }


    public void stopObserveBleOnOffState() {
        if (bleReceiver != null)
            application.unregisterReceiver(bleReceiver);
    }

    public void getPairedDevices() {
        if (isSdk12AndAbove() || isSDK16AndAbove()) {
            if (ActivityCompat.checkSelfPermission(application, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        if (bluetoothAdapter == null)
            Log.e(TAG, "bluetoothAdapter: null");

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName != null) {
                    deviceName = deviceName.replace("\"", "");
                    if (isCamera(deviceName)) {
//                        deviceName = deviceName.replace("-", "_");
                        WiFiHistory wifiResult = new WiFiHistory();
                        wifiResult.setCamera_ssid(deviceName);
                        wifiResult.setCamera_mac_address(deviceHardwareAddress);
                        wifiResult.setIs_wifi_connected(0);
                        if (!wifiResults.contains(wifiResult)) {
                            wifiResults.add(wifiResult);
                        }
                        bleResult.setValue(wifiResults);
//                        Log.e(TAG, "pairedDevices: " + deviceName + " " + deviceName);
                    }
                }
            }
        }
    }
}
