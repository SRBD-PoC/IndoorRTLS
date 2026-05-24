package com.example.indoorrtls.ui.main;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.indoorrtls.algorithms.MdsEngine;
import com.example.indoorrtls.models.NodePosition;
import com.example.indoorrtls.utils.RtlsUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<List<NodePosition>> _nodePositions = new MutableLiveData<>();
    private String myNodeId;
    
    private Map<String, double[]> lastPositionsMap = new HashMap<>();

    private static final double A = -60.0;
    private static final double N = 2.5;

    public LiveData<List<NodePosition>> getNodePositions() {
        return _nodePositions;
    }

    public void setMyNodeId(String id) {
        this.myNodeId = id;
    }

    public void updateBluetoothScanResults(List<ScanResult> results) {
        computePositions(results);
    }

    private void computePositions(List<ScanResult> results) {
        if (myNodeId == null) return;

        Set<String> allNodes = new HashSet<>();
        allNodes.add(myNodeId);

        Map<String, Map<String, Integer>> rssiMap = new HashMap<>();
        Map<String, Integer> phoneMeasurements = new HashMap<>();

        for (ScanResult result : results) {
            String sourceId = RtlsUtils.getTrackingId(result);
            if (sourceId == null) continue;

            allNodes.add(sourceId);
            phoneMeasurements.put(sourceId, result.getRssi());

            Map<String, Integer> seenBySource = new HashMap<>();
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                byte[] data = record.getManufacturerSpecificData(RtlsUtils.COMPANY_ID);
                if (data != null && data.length >= 4) {
                    for (int i = 4; i + 4 < data.length; i += 5) {
                        String targetId = RtlsUtils.bytesToHex(data, i, 4);
                        int rssi = data[i + 4];
                        allNodes.add(targetId);
                        seenBySource.put(targetId, rssi);
                    }
                }
            }
            rssiMap.put(sourceId, seenBySource);
        }
        rssiMap.put(myNodeId, phoneMeasurements);

        List<String> sortedNodes = new ArrayList<>(allNodes);
        Collections.sort(sortedNodes);
        sortedNodes.remove(myNodeId);
        sortedNodes.add(0, myNodeId); // Me is index 0

        int size = sortedNodes.size();
        if (size < 2) {
            if (size == 1) {
                _nodePositions.setValue(Collections.singletonList(new NodePosition(myNodeId, 0, 0, true)));
            } else {
                _nodePositions.setValue(new ArrayList<>());
            }
            return;
        }

        double[][] distanceMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            String src = sortedNodes.get(i);
            Map<String, Integer> srcMeasurements = rssiMap.get(src);
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 0;
                    continue;
                }
                String dest = sortedNodes.get(j);
                Integer rssi = (srcMeasurements != null) ? srcMeasurements.get(dest) : null;
                if (rssi == null) {
                    Map<String, Integer> destMeasurements = rssiMap.get(dest);
                    rssi = (destMeasurements != null) ? destMeasurements.get(src) : null;
                }
                
                if (rssi != null) {
                    distanceMatrix[i][j] = rssiToDistance(rssi);
                } else {
                    // Heuristic for missing distances: 
                    // Use a value that allows a triangle, e.g., slightly less than sum of others
                    // or just a reasonable room-scale default.
                    distanceMatrix[i][j] = 5.0; 
                }
            }
        }

        double[][] initialCoords = new double[size][2];
        boolean hasLast = true;
        for (int i = 0; i < size; i++) {
            double[] last = lastPositionsMap.get(sortedNodes.get(i));
            if (last != null) {
                initialCoords[i] = last;
            } else {
                hasLast = false;
            }
        }

        double[][] finalCoords;
        if (!hasLast || lastPositionsMap.size() != size) {
            finalCoords = MdsEngine.calculateClassicalMDS(distanceMatrix);
        } else {
            finalCoords = MdsEngine.calculateIterativeMDS(distanceMatrix, initialCoords, 10);
        }

        // Anchor: Shift everything so "Self" (index 0) is at (0,0)
        double offsetX = finalCoords[0][0];
        double offsetY = finalCoords[0][1];

        List<NodePosition> positions = new ArrayList<>();
        Map<String, double[]> nextPositionsMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String id = sortedNodes.get(i);
            float x = (float) (finalCoords[i][0] - offsetX);
            float y = (float) (finalCoords[i][1] - offsetY);
            positions.add(new NodePosition(id, x, y, id.equals(myNodeId)));
            nextPositionsMap.put(id, new double[]{x, y});
        }
        lastPositionsMap = nextPositionsMap;
        _nodePositions.setValue(positions);
    }

    private double rssiToDistance(int rssi) {
        return Math.pow(10, (A - rssi) / (10 * N));
    }
}
