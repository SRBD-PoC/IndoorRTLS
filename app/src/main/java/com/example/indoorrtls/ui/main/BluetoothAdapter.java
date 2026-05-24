package com.example.indoorrtls.ui.main;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TableLayout;
import android.widget.TableRow;

import com.example.indoorrtls.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BluetoothAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "BluetoothAdapter";
    private static final int TYPE_BROADCAST_TABLE = 0;
    private static final int TYPE_DEVICE_ITEM = 1;

    private final List<ScanResult> scanResults = new ArrayList<>();

    public void setScanResults(List<ScanResult> results) {
        Log.d(TAG, "setScanResults: Updating UI with " + results.size() + " devices");
        scanResults.clear();
        scanResults.addAll(results);
        // Sort by RSSI descending
        Collections.sort(scanResults, (o1, o2) -> Integer.compare(o2.getRssi(), o1.getRssi()));
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_BROADCAST_TABLE : TYPE_DEVICE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_BROADCAST_TABLE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_broadcast_table, parent, false);
            return new TableViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth, parent, false);
            return new DeviceViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TableViewHolder) {
            ((TableViewHolder) holder).bind(scanResults);
        } else if (holder instanceof DeviceViewHolder) {
            // position 1 in adapter maps to index 0 in results, etc.
            // But wait, if I want to show ALL results in the list below the table,
            // then position 1 maps to index 0.
            ScanResult result = scanResults.get(position - 1);
            ((DeviceViewHolder) holder).bind(result);
        }
    }

    @Override
    public int getItemCount() {
        return scanResults.isEmpty() ? 0 : scanResults.size() + 1;
    }

    static class TableViewHolder extends RecyclerView.ViewHolder {
        TableLayout tableLayout;

        TableViewHolder(View itemView) {
            super(itemView);
            tableLayout = itemView.findViewById(R.id.broadcastTable);
        }

        void bind(List<ScanResult> results) {
            // Keep header row, remove previous data rows
            int childCount = tableLayout.getChildCount();
            if (childCount > 1) {
                tableLayout.removeViews(1, childCount - 1);
            }

            int count = Math.min(results.size(), 4);
            for (int i = 0; i < count; i++) {
                ScanResult result = results.get(i);
                TableRow row = new TableRow(itemView.getContext());
                row.setPadding(4, 8, 4, 8);

                TextView indexTv = new TextView(itemView.getContext());
                indexTv.setText(String.valueOf(i + 1));
                indexTv.setPadding(8, 0, 8, 0);

                // Extract Node ID for the table
                String nodeId = "Unknown";
                if (result.getScanRecord() != null) {
                    byte[] data = result.getScanRecord().getManufacturerSpecificData(0xFFFF);
                    if (data != null && data.length >= 4) {
                        nodeId = bytesToHex(data, 0, 4);
                    }
                }

                TextView idTv = new TextView(itemView.getContext());
                idTv.setText(nodeId);
                idTv.setPadding(8, 0, 8, 0);

                TextView rssiTv = new TextView(itemView.getContext());
                rssiTv.setText(String.valueOf(result.getRssi()));
                rssiTv.setPadding(8, 0, 8, 0);
                rssiTv.setGravity(android.view.Gravity.END);

                row.addView(indexTv);
                row.addView(idTv);
                row.addView(rssiTv);

                tableLayout.addView(row);
            }
        }

        private String bytesToHex(byte[] bytes, int offset, int length) {
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < offset + length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
            return sb.toString();
        }
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, addressText, rssiText;
        View remoteDataTable;
        TableLayout remoteTableContent;

        DeviceViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            addressText = itemView.findViewById(R.id.addressText);
            rssiText = itemView.findViewById(R.id.rssiText);
            remoteDataTable = itemView.findViewById(R.id.remoteDataTable);
            remoteTableContent = itemView.findViewById(R.id.remoteTableContent);
        }

        void bind(ScanResult result) {
            Context context = itemView.getContext();

            String deviceName = null;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    deviceName = result.getDevice().getName();
                } catch (SecurityException ignored) {}
            }

            nameText.setText(deviceName != null ? deviceName : context.getString(R.string.unknown_device));
            addressText.setText(result.getDevice().getAddress());
            rssiText.setText(context.getString(R.string.rssi_format, result.getRssi()));

            // Color coding for RSSI
            if (result.getRssi() >= -60) {
                rssiText.setTextColor(Color.parseColor("#2E7D32"));
            } else if (result.getRssi() >= -80) {
                rssiText.setTextColor(Color.parseColor("#F57F17"));
            } else {
                rssiText.setTextColor(Color.parseColor("#C62828"));
            }

            // Handle Remote Data (Manufacturer Data 0xFFFF)
            if (result.getScanRecord() != null) {
                byte[] data = result.getScanRecord().getManufacturerSpecificData(0xFFFF);
                if (data != null && data.length >= 4) {
                    remoteDataTable.setVisibility(View.VISIBLE);

                    // Identify RTLS device by its custom ID
                    String rtlsId = bytesToHex(data, 0, 4);
                    Log.v(TAG, "Binding node with RTLS ID: " + rtlsId);
                    nameText.setText("RTLS Node: " + rtlsId);
                    nameText.setTextColor(Color.parseColor("#1976D2")); // Blue color for RTLS nodes

                    populateRemoteTable(data);
                } else {
                    remoteDataTable.setVisibility(View.GONE);
                }
            } else {
                remoteDataTable.setVisibility(View.GONE);
            }
        }

        private void populateRemoteTable(byte[] data) {
            remoteTableContent.removeAllViews();

            // Add Header
            TableRow header = new TableRow(itemView.getContext());
            header.setBackgroundColor(Color.parseColor("#EEEEEE"));
            header.addView(createCell("#", true));
            header.addView(createCell("MAC", true));
            header.addView(createCell("RSSI", true));
            remoteTableContent.addView(header);

            // Data starts after 4-byte Device ID
            for (int i = 4; i + 4 < data.length; i += 5) {
                TableRow row = new TableRow(itemView.getContext());

                // Index
                row.addView(createCell(String.valueOf(((i - 4) / 5) + 1), false));

                // Node ID (Formatted)
                row.addView(createCell(bytesToHex(data, i, 4), false));

                // RSSI
                row.addView(createCell(String.valueOf(data[i + 4]), false));

                remoteTableContent.addView(row);
            }
        }

        private String bytesToHex(byte[] bytes, int offset, int length) {
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < offset + length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
            return sb.toString();
        }

        private TextView createCell(String text, boolean isHeader) {
            TextView tv = new TextView(itemView.getContext());
            tv.setText(text);
            tv.setPadding(8, 4, 8, 4);
            if (isHeader) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            return tv;
        }
    }
}
