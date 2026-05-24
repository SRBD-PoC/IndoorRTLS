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

import com.example.indoorrtls.utils.DeviceUtils;
import com.example.indoorrtls.utils.RtlsUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BluetoothAdvertiserManager {
    private static final String TAG = "BTAdvertiserManager";
    private static final int MAX_DEVICES_IN_PACKET = 4;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private boolean isAdvertising = false;
    private final byte[] deviceId;

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
        this.deviceId = DeviceUtils.getOrCreateDeviceIdBytes(this.context);
    }

    public void startAdvertising(List<ScanResult> scanResults) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || !bluetoothAdapter.isMultipleAdvertisementSupported()) {
            return;
        }

        if (advertiser == null) {
            advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }

        if (advertiser == null) return;

        List<ScanResult> sortedResults = new ArrayList<>();
        if (scanResults != null) {
            sortedResults.addAll(scanResults);
            Collections.sort(sortedResults, (o1, o2) -> Integer.compare(o2.getRssi(), o1.getRssi()));
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        AdvertiseData data = buildAdvertiseData(sortedResults);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (isAdvertising) {
            advertiser.stopAdvertising(advertiseCallback);
        }

        advertiser.startAdvertising(settings, data, advertiseCallback);
        isAdvertising = true;
    }

    public void stopAdvertising() {
        if (isAdvertising && advertiser != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                advertiser.stopAdvertising(advertiseCallback);
            }
            isAdvertising = false;
        }
    }

    private AdvertiseData buildAdvertiseData(List<ScanResult> results) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + (MAX_DEVICES_IN_PACKET * 5));
        buffer.put(deviceId);

        int count = 0;
        for (ScanResult result : results) {
            if (count >= MAX_DEVICES_IN_PACKET) break;
            byte[] remoteId = RtlsUtils.getTrackingIdBytes(result);
            if (remoteId != null) {
                buffer.put(remoteId);
                buffer.put((byte) result.getRssi());
                count++;
            }
        }

        byte[] payload = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(payload);

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(RtlsUtils.COMPANY_ID, payload)
                .build();
    }
}
