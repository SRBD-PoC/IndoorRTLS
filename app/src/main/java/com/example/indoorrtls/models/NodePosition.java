package com.example.indoorrtls.models;

public class NodePosition {
    private final String id;
    private final float x;
    private final float y;
    private final boolean isSelf;

    public NodePosition(String id, float x, float y, boolean isSelf) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isSelf = isSelf;
    }

    public String getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isSelf() {
        return isSelf;
    }
}
