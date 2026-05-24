package com.example.indoorrtls.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.indoorrtls.utils.AdvertiseUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager class to handle Bluetooth Low Energy advertising.
 * It broadcasts the top scanned devices (address and RSSI) efficiently.
 */
public class BluetoothAdvertiserManager {
    private static final String TAG = "BTAdvertiserManager";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private boolean isAdvertising = false;

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "BLE Advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "BLE Advertising failed with error code: " + errorCode);
            isAdvertising = false;
        }
    };

    public BluetoothAdvertiserManager(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
    }

    /**
     * Starts advertising the top scanned results.
     *
     * @param scanResults The list of current scan results.
     */
    public void startAdvertising(List<ScanResult> scanResults) {
        Log.d(TAG, "Starting advertising with " + (scanResults != null ? scanResults.size() : 0) + " results");
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || !bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.w(TAG, "BLE Advertising not supported or Bluetooth disabled");
            return;
        }

        if (advertiser == null) {
            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }

        if (advertiser == null) {
            Log.e(TAG, "Failed to get BluetoothLeAdvertiser");
            return;
        }

        // Prepare the top results sorted by RSSI descending
        List<ScanResult> sortedResults = new ArrayList<>();
        if (scanResults != null && !scanResults.isEmpty()) {
            sortedResults.addAll(scanResults);
            sortedResults.sort((o1, o2) -> Integer.compare(o2.getRssi(), o1.getRssi()));
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build();

        AdvertiseData data = AdvertiseUtil.buildAdvertiseData(sortedResults);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing BLUETOOTH_ADVERTISE permission");
            return;
        }

        // If already advertising, stop it first to refresh data
        if (isAdvertising) {
            advertiser.stopAdvertising(advertiseCallback);
        }

        advertiser.startAdvertising(settings, data, advertiseCallback);
        isAdvertising = true;
    }

    /**
     * Stops BLE advertising.
     */
    public void stopAdvertising() {
        if (isAdvertising && advertiser != null) {
            Log.d(TAG, "Stopping BLE advertising");
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                advertiser.stopAdvertising(advertiseCallback);
            }
            isAdvertising = false;
        }
    }
}
