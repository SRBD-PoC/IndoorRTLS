package com.example.indoorrtls.ui.main;

import android.net.wifi.ScanResult;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<List<ScanResult>> _scanResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ScanResult>> getScanResults() {
        return _scanResults;
    }

    public void updateScanResults(List<ScanResult> results) {
        _scanResults.setValue(results);
    }
}