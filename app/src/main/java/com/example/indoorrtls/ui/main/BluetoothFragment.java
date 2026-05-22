package com.example.indoorrtls.ui.main;

import android.bluetooth.le.ScanResult;
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
import com.example.indoorrtls.scanner.BluetoothScanner;

import java.util.List;

public class BluetoothFragment extends Fragment implements BluetoothScanner.OnBluetoothScanResultListener {

    private MainViewModel mViewModel;
    private BluetoothAdapter adapter;
    private BluetoothScanner bluetoothScanner;

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
                    bluetoothScanner.start();
                } else {
                    Toast.makeText(getContext(), "Permissions denied. Cannot scan Bluetooth.", Toast.LENGTH_SHORT).show();
                }
            });

    public static BluetoothFragment newInstance() {
        return new BluetoothFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        bluetoothScanner = new BluetoothScanner(requireContext(), this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        setupRecyclerView(view);
        view.findViewById(R.id.scanFab).setOnClickListener(v -> bluetoothScanner.start());

        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.bluetoothList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BluetoothAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.getBluetoothScanResults().observe(getViewLifecycleOwner(), results -> adapter.setScanResults(results));
    }

    @Override
    public void onStart() {
        super.onStart();
        bluetoothScanner.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        bluetoothScanner.stop();
    }

    // BluetoothScanner.OnBluetoothScanResultListener Implementation

    @Override
    public void onResultsAvailable(List<ScanResult> results) {
        mViewModel.updateBluetoothScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {
        // Handle scan failure
    }

    @Override
    public void onPermissionRequired(String[] permissions) {
        requestPermissionLauncher.launch(permissions);
    }
}
