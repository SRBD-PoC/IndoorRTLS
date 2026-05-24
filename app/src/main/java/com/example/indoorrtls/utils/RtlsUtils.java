package com.example.indoorrtls.utils;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

public class RtlsUtils {
    public static final int COMPANY_ID = 0xFFFF;

    public static String getTrackingId(ScanResult result) {
        ScanRecord record = result.getScanRecord();
        if (record != null) {
            byte[] data = record.getManufacturerSpecificData(COMPANY_ID);
            if (data != null && data.length >= 4) {
                return bytesToHex(data, 0, 4);
            }
        }
        return null;
    }

    public static byte[] getTrackingIdBytes(ScanResult result) {
        ScanRecord record = result.getScanRecord();
        if (record != null) {
            byte[] data = record.getManufacturerSpecificData(COMPANY_ID);
            if (data != null && data.length >= 4) {
                byte[] id = new byte[4];
                System.arraycopy(data, 0, id, 0, 4);
                return id;
            }
        }
        return null;
    }

    public static String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
