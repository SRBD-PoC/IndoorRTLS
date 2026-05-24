package com.example.indoorrtls.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.indoorrtls.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans for BLE devices and tracks them.
 * For RTLS nodes (devices running this app), it tracks them using a persistent Unique ID
 * instead of the rotating MAC address to prevent "ghost" duplicates.
 */
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
    // Map key: Either Persistent Unique ID (for RTLS nodes) or MAC Address (for regular devices)
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
            super.onScanResult(callbackType, result);
            handleNewResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                handleNewResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error code: " + errorCode);
            listener.onScanFailed(errorCode);
        }
    };

    private void handleNewResult(ScanResult result) {
        String trackingId = getTrackingId(result);
        if (trackingId != null) {
            Log.v(TAG, "Scanned RTLS Node: " + trackingId + " RSSI: " + result.getRssi());
            currentResults.put(trackingId, new ScanResultTimestamp(result));
        }
    }

    /**
     * Extracts a stable ID for tracking from RTLS nodes.
     * Returns null if the device is not an RTLS node.
     */
    private String getTrackingId(ScanResult result) {
        ScanRecord record = result.getScanRecord();
        if (record != null) {
            byte[] data = record.getManufacturerSpecificData(0xFFFF);
            if (data != null && data.length >= 4) {
                StringBuilder sb = new StringBuilder("RTLS_");
                for (int i = 0; i < 4; i++) {
                    sb.append(String.format("%02X", data[i]));
                }
                return sb.toString();
            }
        }
        return null;
    }

    public BluetoothScanner(Context context, OnBluetoothScanResultListener listener) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = (bluetoothManager != null) ? bluetoothManager.getAdapter() : null;
        this.listener = listener;
    }

    public void start() {
        Log.d(TAG, "Starting scanner...");
        if (isScanning || bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Cannot start scan: isScanning=" + isScanning + ", adapterEnabled=" + (bluetoothAdapter != null && bluetoothAdapter.isEnabled()));
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

            // 1. Create a filter to ONLY see RTLS nodes at the hardware/OS level
            List<ScanFilter> filters = new ArrayList<>();
            // Using a zero-length mask to match any manufacturer data with ID 0xFFFF
            filters.add(new ScanFilter.Builder()
                    .setManufacturerData(0xFFFF, new byte[0], new byte[0])
                    .build());

            // 2. Set high frequency scan mode
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            Log.d(TAG, "BLE Scan started with RTLS filters");
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            handler.post(reportResultsRunnable);
        } else {
            Log.e(TAG, "BluetoothLeScanner is null");
        }
    }

    public void stop() {
        if (!isScanning) return;
        Log.d(TAG, "Stopping scanner");
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

                // Cleanup old results (> 10 seconds)
                int removedCount = 0;
                Iterator<Map.Entry<String, ScanResultTimestamp>> it = currentResults.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, ScanResultTimestamp> entry = it.next();
                    if (now - entry.getValue().timestamp > 10000) {
                        it.remove();
                        removedCount++;
                    } else {
                        filteredList.add(entry.getValue().result);
                    }
                }

                if (removedCount > 0) Log.d(TAG, "Removed " + removedCount + " stale RTLS nodes");
                Log.d(TAG, "Reporting " + filteredList.size() + " active RTLS nodes");
                listener.onResultsAvailable(filteredList);
                handler.postDelayed(this, 2000);
            }
        }
    };
}
