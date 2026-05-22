package com.example.indoorrtls.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import com.example.indoorrtls.utils.AppContextUtils;
import com.example.indoorrtls.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanner {

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
    private final List<ScanResult> currentResults = new ArrayList<>();

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            updateResults(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                updateResults(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            listener.onScanFailed(errorCode);
        }
    };

    public BluetoothScanner(Context context, OnBluetoothScanResultListener listener) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.listener = listener;
    }

    public void start() {
        if (isScanning || bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;

        String[] permissions = PermissionUtils.getBluetoothScanPermissions();
        if (!PermissionUtils.hasPermissions(context, permissions)) {
            listener.onPermissionRequired(permissions);
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            isScanning = true;
            currentResults.clear();
            if (ActivityCompat.checkSelfPermission(AppContextUtils.getContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothLeScanner.startScan(scanCallback);
            // Periodically report results to the listener
            handler.post(reportResultsRunnable);
        }
    }

    public void stop() {
        if (!isScanning) return;
        isScanning = false;
        if (bluetoothLeScanner != null && bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(AppContextUtils.getContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
        }
        handler.removeCallbacks(reportResultsRunnable);
    }

    private void updateResults(ScanResult result) {
        synchronized (currentResults) {
            int index = -1;
            for (int i = 0; i < currentResults.size(); i++) {
                if (currentResults.get(i).getDevice().getAddress().equals(result.getDevice().getAddress())) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                currentResults.set(index, result);
            } else {
                currentResults.add(result);
            }
        }
    }

    private final Runnable reportResultsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                synchronized (currentResults) {
                    listener.onResultsAvailable(new ArrayList<>(currentResults));
                }
                handler.postDelayed(this, 2000); // Update every 2 seconds
            }
        }
    };
}
