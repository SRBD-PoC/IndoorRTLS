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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager class to handle Bluetooth Low Energy advertising.
 * It broadcasts the top scanned devices (address and RSSI) efficiently.
 */
public class BluetoothAdvertiserManager {
    private static final String TAG = "BTAdvertiserManager";
    private static final int COMPANY_ID = 0xFFFF;
    private static final int MAX_DEVICES_IN_PACKET = 4;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private boolean isAdvertising = false;
    private final byte[] deviceId; // 4-byte unique ID for this device

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
        this.deviceId = getOrCreateDeviceId();
    }

    private byte[] getOrCreateDeviceId() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("rtls_prefs", Context.MODE_PRIVATE);
        String idHex = prefs.getString("device_id", null);
        if (idHex == null) {
            // Generate a random 4-byte ID
            byte[] id = new byte[4];
            new java.util.Random().nextBytes(id);
            idHex = bytesToHex(id);
            prefs.edit().putString("device_id", idHex).apply();
            return id;
        }
        return hexToBytes(idHex);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Starts advertising the top scanned results.
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
            Collections.sort(sortedResults, (o1, o2) -> Integer.compare(o2.getRssi(), o1.getRssi()));
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build();

        AdvertiseData data = buildAdvertiseData(sortedResults);

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

    private AdvertiseData buildAdvertiseData(List<ScanResult> results) {
        // BLE Advertising Data Packet (31 bytes total)
        // Manufacturer Specific Data header: 4 bytes (2 for length/type, 2 for Company ID)
        // Custom Device ID: 4 bytes
        // Each device entry: 4 bytes (RTLS ID) + 1 byte (RSSI) = 5 bytes.
        // 4 + (4 * 5) = 24 bytes. Total: 24 + 4 = 28 bytes.

        Log.d(TAG, "Building advertise data for " + results.size() + " devices");
        ByteBuffer buffer = ByteBuffer.allocate(4 + (MAX_DEVICES_IN_PACKET * 5));

        // 1. Put our unique Device ID first
        Log.v(TAG, "My Unique Device ID: " + bytesToHex(deviceId));
        buffer.put(deviceId);

        // 2. Put scanned devices
        int count = 0;
        for (ScanResult result : results) {
            if (count >= MAX_DEVICES_IN_PACKET) break;

            byte[] remoteId = null;
            if (result.getScanRecord() != null) {
                byte[] data = result.getScanRecord().getManufacturerSpecificData(COMPANY_ID);
                if (data != null && data.length >= 4) {
                    remoteId = new byte[4];
                    System.arraycopy(data, 0, remoteId, 0, 4);
                }
            }

            if (remoteId != null) {
                byte rssiByte = (byte) result.getRssi();
                buffer.put(remoteId);
                buffer.put(rssiByte);
                count++;
            }
        }

        byte[] payload = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(payload);

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(COMPANY_ID, payload)
                .build();
    }

    private byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
