package com.example.indoorrtls.ui.main;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorrtls.R;
import com.example.indoorrtls.scanner.WifiScanner;
import com.example.indoorrtls.utils.PermissionUtils;

import java.util.List;

public class MainFragment extends Fragment implements WifiScanner.OnScanResultListener {

    private MainViewModel mViewModel;
    private WifiAdapter adapter;
    private WifiScanner wifiScanner;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    wifiScanner.performScan();
                } else {
                    Toast.makeText(getContext(), "Permissions denied. Cannot scan WiFi.", Toast.LENGTH_SHORT).show();
                }
            });

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        wifiScanner = new WifiScanner(requireContext(), this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setupRecyclerView(view);
        view.findViewById(R.id.scanFab).setOnClickListener(v -> wifiScanner.performScan());

        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.wifiList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WifiAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.getScanResults().observe(getViewLifecycleOwner(), results -> adapter.setScanResults(results));
    }

    @Override
    public void onStart() {
        super.onStart();
        wifiScanner.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        wifiScanner.stop();
    }

    // WifiScanner.OnScanResultListener Implementation

    @Override
    public void onResultsAvailable(List<ScanResult> results) {
        mViewModel.updateScanResults(results);
    }

    @Override
    public void onScanFailed() {
        // Log failure or show feedback if needed
    }

    @Override
    public void onPermissionRequired(String[] permissions) {
        requestPermissionLauncher.launch(permissions);
    }
}
