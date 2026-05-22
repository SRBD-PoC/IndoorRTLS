package com.example.indoorrtls.ui.main;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indoorrtls.R;

import java.util.ArrayList;
import java.util.List;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {

    private final List<ScanResult> scanResults = new ArrayList<>();

    public void setScanResults(List<ScanResult> results) {
        scanResults.clear();
        scanResults.addAll(results);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult result = scanResults.get(position);
        Context context = holder.itemView.getContext();
        
        holder.ssidText.setText(result.SSID.isEmpty() ? context.getString(R.string.hidden_network) : result.SSID);
        holder.bssidText.setText(result.BSSID);
        holder.rssiText.setText(context.getString(R.string.rssi_format, result.level));
        holder.frequencyText.setText(context.getString(R.string.frequency_format, result.frequency));
        holder.capabilitiesText.setText(result.capabilities);

        // Color coding for RSSI with high contrast
        if (result.level >= -50) {
            holder.rssiText.setTextColor(Color.parseColor("#2E7D32")); // Darker Green (Material 800)
        } else if (result.level >= -70) {
            holder.rssiText.setTextColor(Color.parseColor("#F57F17")); // Darker Amber/Yellow (Material 900)
        } else {
            holder.rssiText.setTextColor(Color.parseColor("#C62828")); // Darker Red (Material 800)
        }
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ssidText, bssidText, rssiText, frequencyText, capabilitiesText;

        ViewHolder(View itemView) {
            super(itemView);
            ssidText = itemView.findViewById(R.id.ssidText);
            bssidText = itemView.findViewById(R.id.bssidText);
            rssiText = itemView.findViewById(R.id.rssiText);
            frequencyText = itemView.findViewById(R.id.frequencyText);
            capabilitiesText = itemView.findViewById(R.id.capabilitiesText);
        }
    }
}