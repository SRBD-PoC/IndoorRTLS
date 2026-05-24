package com.example.indoorrtls.ui.main;

import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.indoorrtls.R;
import com.example.indoorrtls.scanner.BluetoothAdvertiserManager;
import com.example.indoorrtls.scanner.BluetoothScanner;
import com.example.indoorrtls.ui.views.RtlsMapView;
import com.example.indoorrtls.utils.DeviceUtils;
import com.example.indoorrtls.utils.PermissionUtils;
import com.example.indoorrtls.utils.RtlsUtils;

import java.util.Collections;
import java.util.List;

public class BluetoothFragment extends Fragment implements BluetoothScanner.OnBluetoothScanResultListener {

    private MainViewModel mViewModel;
    private BluetoothScanner bluetoothScanner;
    private BluetoothAdvertiserManager bluetoothAdvertiserManager;
    private RtlsMapView mapView;

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
                    checkBluetoothAndStart();
                } else {
                    Toast.makeText(getContext(), "Permissions denied.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> enableBtLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (bluetoothScanner != null) {
                    bluetoothScanner.start();
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
        bluetoothAdvertiserManager = new BluetoothAdvertiserManager(requireContext());

        String myNodeId = RtlsUtils.bytesToHex(DeviceUtils.getOrCreateDeviceIdBytes(requireContext()), 0, 4);
        mViewModel.setMyNodeId(myNodeId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        mapView = view.findViewById(R.id.mapView);
        view.findViewById(R.id.scanFab).setOnClickListener(v -> bluetoothScanner.start());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.getNodePositions().observe(getViewLifecycleOwner(), positions -> {
            if (mapView != null) {
                mapView.setNodes(positions);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        checkBluetoothAndStart();
        if (bluetoothAdvertiserManager != null) {
            bluetoothAdvertiserManager.startAdvertising(Collections.emptyList());
        }
    }

    private void checkBluetoothAndStart() {
        String[] permissions = PermissionUtils.getBluetoothScanPermissions();
        if (!PermissionUtils.hasPermissions(requireContext(), permissions)) {
            requestPermissionLauncher.launch(permissions);
            return;
        }

        android.bluetooth.BluetoothAdapter btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) return;

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        } else {
            bluetoothScanner.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        bluetoothScanner.stop();
        bluetoothAdvertiserManager.stopAdvertising();
    }

    // BluetoothScanner.OnBluetoothScanResultListener

    @Override
    public void onResultsAvailable(List<ScanResult> results) {
        mViewModel.updateBluetoothScanResults(results);
        if (bluetoothAdvertiserManager != null) {
            bluetoothAdvertiserManager.startAdvertising(results);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        // Handle scan failed
    }

    @Override
    public void onPermissionRequired(String[] permissions) {
        requestPermissionLauncher.launch(permissions);
    }
}
