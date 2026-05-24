package com.example.indoorrtls.ui.main;

import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.util.Log;
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
import com.example.indoorrtls.scanner.BluetoothAdvertiserManager;
import com.example.indoorrtls.scanner.BluetoothScanner;
import com.example.indoorrtls.utils.PermissionUtils;

import java.util.List;

public class BluetoothFragment extends Fragment implements BluetoothScanner.OnBluetoothScanResultListener {

    private static final String TAG = "BluetoothFragment";
    private MainViewModel mViewModel;
    private BluetoothAdapter adapter;
    private BluetoothScanner bluetoothScanner;
    private BluetoothAdvertiserManager bluetoothAdvertiserManager;

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
                    Toast.makeText(getContext(), "Permissions denied. Cannot scan Bluetooth.", Toast.LENGTH_SHORT).show();
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
        Log.d(TAG, "onStart: Initializing Bluetooth flow");
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        checkBluetoothAndStart();
        // Start advertising our ID immediately so others can see us
        if (bluetoothAdvertiserManager != null) {
            Log.d(TAG, "Triggering initial advertisement");
            bluetoothAdvertiserManager.startAdvertising(null);
        }
    }

    private void checkBluetoothAndStart() {
        Log.d(TAG, "Checking permissions and Bluetooth status");
        // On Android 12+, ACTION_REQUEST_ENABLE requires BLUETOOTH_CONNECT permission.
        String[] permissions = PermissionUtils.getBluetoothScanPermissions();
        if (!PermissionUtils.hasPermissions(requireContext(), permissions)) {
            Log.d(TAG, "Requesting missing permissions");
            requestPermissionLauncher.launch(permissions);
            return;
        }

        android.bluetooth.BluetoothAdapter btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth");
            Toast.makeText(getContext(), "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!btAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth disabled, requesting user to enable");
            Intent enableBtIntent = new Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        } else {
            Log.d(TAG, "Bluetooth is enabled, starting scanner");
            bluetoothScanner.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Stopping scanning and advertising");
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        bluetoothScanner.stop();
        bluetoothAdvertiserManager.stopAdvertising();
    }

    // BluetoothScanner.OnBluetoothScanResultListener Implementation

    @Override
    public void onResultsAvailable(List<ScanResult> results) {
        Log.d(TAG, "onResultsAvailable: Received " + results.size() + " RTLS results");
        mViewModel.updateBluetoothScanResults(results);
        if (bluetoothAdvertiserManager != null) {
            bluetoothAdvertiserManager.startAdvertising(results);
        }
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
