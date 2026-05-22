package com.example.indoorrtls.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.example.indoorrtls.utils.PermissionUtils;

import java.util.List;

public class WifiScanner {

    public interface OnScanResultListener {
        void onResultsAvailable(List<ScanResult> results);
        void onScanFailed();
        void onPermissionRequired(String[] permissions);
    }

    private final Context context;
    private final WifiManager wifiManager;
    private final OnScanResultListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private boolean isScanning = false;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                handleSuccess();
            } else {
                handleFailure();
            }
        }
    };

    public WifiScanner(Context context, OnScanResultListener listener) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.listener = listener;
    }

    public void start() {
        if (isScanning) return;
        isScanning = true;
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);
        
        performScan();
    }

    public void stop() {
        isScanning = false;
        try {
            context.unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            // Not registered
        }
        handler.removeCallbacksAndMessages(null);
    }

    public void performScan() {
        String[] permissions = PermissionUtils.getWifiScanPermissions();
        if (!PermissionUtils.hasPermissions(context, permissions)) {
            listener.onPermissionRequired(permissions);
            return;
        }

        boolean success = wifiManager.startScan();
        if (!success) {
            handleFailure();
        }
    }

    private void handleSuccess() {
        List<ScanResult> results = wifiManager.getScanResults();
        listener.onResultsAvailable(results);
        
        if (isScanning) {
            performScan(); // Immediate next scan
        }
    }

    private void handleFailure() {
        listener.onScanFailed();
        
        if (isScanning) {
            // Delay retry on failure to avoid tight loop
            handler.postDelayed(this::performScan, 1000);
        }
    }
}
