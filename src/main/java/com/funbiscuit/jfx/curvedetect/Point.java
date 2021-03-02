package com.funbiscuit.jfx.curvedetect;

import com.funbiscuit.jfx.curvedetect.model.Vec2D;

import java.util.UUID;

public class Point implements ImageElement {
    private UUID id = UUID.randomUUID();
    private boolean isSnapped;
    private Vec2D imagePosition;
    private boolean isSubdivisionPoint;

    public Point(double imageX, double imageY) {
        imagePosition = new Vec2D(imageX, imageY);
    }

    public boolean isSnapped() {
        return isSnapped;
    }

    public void setSnapped(boolean isSnapped) {
        this.isSnapped = isSnapped;
    }

    public UUID getId() {
        return id;
    }

    public Vec2D getImagePos() {
        return imagePosition;
    }

    public void setImagePos(double x, double y) {
        imagePosition.setX(x);
        imagePosition.setY(y);
    }

    public final boolean isSubdivisionPoint() {
        return isSubdivisionPoint;
    }

    public final void setSubdivisionPoint(boolean isSubdivisionPoint) {
        this.isSubdivisionPoint = isSubdivisionPoint;
    }
}
