package com.example.indoorrtls.ui.main;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorrtls.R;

import java.util.ArrayList;
import java.util.List;

public class BluetoothAdapter extends RecyclerView.Adapter<BluetoothAdapter.ViewHolder> {

    private final List<ScanResult> scanResults = new ArrayList<>();

    public void setScanResults(List<ScanResult> results) {
        scanResults.clear();
        scanResults.addAll(results);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult result = scanResults.get(position);
        Context context = holder.itemView.getContext();

        String deviceName = result.getDevice().getName();
        holder.nameText.setText(deviceName != null ? deviceName : context.getString(R.string.unknown_device));
        holder.addressText.setText(result.getDevice().getAddress());
        holder.rssiText.setText(context.getString(R.string.rssi_format, result.getRssi()));

        // Color coding for RSSI
        if (result.getRssi() >= -60) {
            holder.rssiText.setTextColor(Color.parseColor("#2E7D32"));
        } else if (result.getRssi() >= -80) {
            holder.rssiText.setTextColor(Color.parseColor("#F57F17"));
        } else {
            holder.rssiText.setTextColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, addressText, rssiText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            addressText = itemView.findViewById(R.id.addressText);
            rssiText = itemView.findViewById(R.id.rssiText);
        }
    }
}
