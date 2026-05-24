package com.example.indoorrtls.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.indoorrtls.utils.PermissionUtils;
import com.example.indoorrtls.utils.RtlsUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BluetoothScanner {
    private static final String TAG = "BluetoothScanner";

    public interface OnBluetoothScanResultListener {
        void onResultsAvailable(List<ScanResult> results);
        void onScanFailed(int errorCode);
        void onPermissionRequired(String[] permissions);
    }

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private final OnBluetoothScanResultListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isScanning = false;
    private final Map<String, ScanResultTimestamp> currentResults = new ConcurrentHashMap<>();

    private static class ScanResultTimestamp {
        ScanResult result;
        long timestamp;

        ScanResultTimestamp(ScanResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleNewResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                handleNewResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            listener.onScanFailed(errorCode);
        }
    };

    private void handleNewResult(ScanResult result) {
        String trackingId = RtlsUtils.getTrackingId(result);
        if (trackingId != null) {
            currentResults.put(trackingId, new ScanResultTimestamp(result));
        }
    }

    public BluetoothScanner(Context context, OnBluetoothScanResultListener listener) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
        this.listener = listener;
    }

    public void start() {
        if (isScanning || bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }

        String[] permissions = PermissionUtils.getBluetoothScanPermissions();
        if (!PermissionUtils.hasPermissions(context, permissions)) {
            listener.onPermissionRequired(permissions);
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            isScanning = true;
            currentResults.clear();

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder()
                    .setManufacturerData(RtlsUtils.COMPANY_ID, new byte[0], new byte[0])
                    .build());

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            handler.post(reportResultsRunnable);
        }
    }

    public void stop() {
        if (!isScanning) return;
        isScanning = false;
        if (bluetoothLeScanner != null && bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(scanCallback);
            }
        }
        handler.removeCallbacks(reportResultsRunnable);
    }

    private final Runnable reportResultsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                long now = System.currentTimeMillis();
                List<ScanResult> filteredList = new ArrayList<>();

                Iterator<Map.Entry<String, ScanResultTimestamp>> it = currentResults.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, ScanResultTimestamp> entry = it.next();
                    if (now - entry.getValue().timestamp > 10000) {
                        it.remove();
                    } else {
                        filteredList.add(entry.getValue().result);
                    }
                }

                listener.onResultsAvailable(filteredList);
                handler.postDelayed(this, 2000);
            }
        }
    };
}
