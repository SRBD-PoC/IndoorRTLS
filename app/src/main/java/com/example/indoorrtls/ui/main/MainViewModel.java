package com.example.indoorrtls.ui.main;

import android.bluetooth.le.ScanResult;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<List<ScanResult>> _bluetoothScanResults = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<ScanResult>> getBluetoothScanResults() {
        return _bluetoothScanResults;
    }

    public void updateBluetoothScanResults(List<ScanResult> results) {
        _bluetoothScanResults.setValue(results);
    }
}
