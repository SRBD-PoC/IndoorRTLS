package com.example.indoorrtls.utils;

import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AdvertiseUtil {

    private static final String TAG = "AdvertiseUtil";
    private static final int COMPANY_ID = 0xFFFF;
    private static final int MAX_DEVICES_IN_PACKET = 4;
    private static byte[] mPayLoad;

    public static AdvertiseData buildAdvertiseData(List<ScanResult> results) {
        // BLE Advertising Data Packet (31 bytes total)
        // Manufacturer Specific Data header: 4 bytes (2 for length/type, 2 for Company ID)
        // Custom Device ID: 4 bytes
        // Each device entry: 4 bytes (RTLS ID) + 1 byte (RSSI) = 5 bytes.
        // 4 + (4 * 5) = 24 bytes. Total: 24 + 4 = 28 bytes.

        Log.d(TAG, "Building advertise data for " + results.size() + " devices");
        ByteBuffer buffer = ByteBuffer.allocate(4 + (MAX_DEVICES_IN_PACKET * 5));

        // 1. Put our unique Device ID first
        Log.v(TAG, "My Unique Device ID: " + bytesToHex(getOrCreateDeviceId()));
        buffer.put(getOrCreateDeviceId());

        // 2. Put scanned devices
        int count = 0;
        for (ScanResult result : results) {
            if (count >= MAX_DEVICES_IN_PACKET) break;

            byte[] remoteId = null;
            if (result.getScanRecord() != null) {
                byte[] data = result.getScanRecord().getManufacturerSpecificData(COMPANY_ID);
                if (data != null && data.length >= 4) {
                    remoteId = new byte[4];
                    System.arraycopy(data, 0, remoteId, 0, 4);
                }
            }

            if (remoteId != null) {
                byte rssiByte = (byte) result.getRssi();
                buffer.put(remoteId);
                buffer.put(rssiByte);
                count++;
            }
        }

        mPayLoad = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(mPayLoad);

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(COMPANY_ID, mPayLoad)
                .build();
    }

    public static ParsedPayload parseScanResult(ScanResult result) {
        if (result == null || result.getScanRecord() == null) {
            return null;
        }
        byte[] data = result.getScanRecord().getManufacturerSpecificData(COMPANY_ID);
        return parseAdvertiseData(data);
    }

    public static ParsedPayload createSelfPayload() {
        if (mPayLoad != null) {
            ParsedPayload parsed = parseAdvertiseData(mPayLoad);
            if (parsed != null) {
                return parsed;
            }
        }
        // If we haven't advertised yet, return a payload with just our ID and no scanned devices
        return new ParsedPayload(getOrCreateDeviceId(), new ArrayList<>());
    }

    private static ParsedPayload parseAdvertiseData(byte[] payload) {
        if (payload == null || payload.length < 4) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] senderId = new byte[4];
        buffer.get(senderId);

        List<ScannedDevice> scannedDevices = new ArrayList<>();
        while (buffer.remaining() >= 5) {
            byte[] remoteId = new byte[4];
            buffer.get(remoteId);
            int rssi = buffer.get();
            scannedDevices.add(new ScannedDevice(remoteId, rssi));
        }

        return new ParsedPayload(senderId, scannedDevices);
    }

    public static class ScannedDevice {
        public final byte[] deviceId;
        public final int rssi;

        public ScannedDevice(byte[] deviceId, int rssi) {
            this.deviceId = deviceId;
            this.rssi = rssi;
        }

        @NonNull
        @Override
        public String toString() {
            return bytesToHex(deviceId) + ": " + rssi;
        }
    }

    public static class ParsedPayload {
        public final byte[] senderDeviceId;
        public final List<ScannedDevice> scannedDevices;

        public ParsedPayload(byte[] senderDeviceId, List<ScannedDevice> scannedDevices) {
            this.senderDeviceId = senderDeviceId;
            this.scannedDevices = scannedDevices;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder string = new StringBuilder(bytesToHex(senderDeviceId) + ":\n");
            for (ScannedDevice scannedDevice : scannedDevices) {
                string.append(scannedDevice.toString()).append("\n");
            }
            return string.toString();
        }
    }

    private static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static byte[] getOrCreateDeviceId() {
        android.content.SharedPreferences prefs = AppContextUtils.getContext().getSharedPreferences("rtls_prefs", Context.MODE_PRIVATE);
        String idHex = prefs.getString("device_id", null);
        if (idHex == null) {
            // Generate a random 4-byte ID
            byte[] id = new byte[4];
            new java.util.Random().nextBytes(id);
            idHex = bytesToHex(id);
            prefs.edit().putString("device_id", idHex).apply();
            return id;
        }
        return hexToBytes(idHex);
    }
}
