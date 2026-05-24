package com.example.indoorrtls.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.indoorrtls.models.NodePosition;

import java.util.ArrayList;
import java.util.List;

public class RtlsMapView extends View {

    private final List<NodePosition> nodes = new ArrayList<>();
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float padding = 100f;

    public RtlsMapView(Context context) {
        this(context, null);
    }

    public RtlsMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(2f);
    }

    public void setNodes(List<NodePosition> newNodes) {
        this.nodes.clear();
        if (newNodes != null) {
            this.nodes.addAll(newNodes);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (nodes.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        // Find bounds to scale
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        for (NodePosition node : nodes) {
            minX = Math.min(minX, node.getX());
            maxX = Math.max(maxX, node.getX());
            minY = Math.min(minY, node.getY());
            maxY = Math.max(maxY, node.getY());
        }

        float rangeX = maxX - minX;
        float rangeY = maxY - minY;
        
        // Avoid division by zero
        if (rangeX == 0) rangeX = 1;
        if (rangeY == 0) rangeY = 1;

        float scaleX = (width - 2 * padding) / rangeX;
        float scaleY = (height - 2 * padding) / rangeY;
        float scale = Math.min(scaleX, scaleY);

        // Draw nodes
        for (NodePosition node : nodes) {
            float canvasX = padding + (node.getX() - minX) * scale;
            float canvasY = padding + (node.getY() - minY) * scale;

            if (node.isSelf()) {
                nodePaint.setColor(Color.BLUE);
            } else {
                nodePaint.setColor(Color.RED);
            }

            canvas.drawCircle(canvasX, canvasY, 20f, nodePaint);
            canvas.drawText(node.getId() + (node.isSelf() ? " (Me)" : ""), canvasX, canvasY - 30f, textPaint);
        }
    }
}
