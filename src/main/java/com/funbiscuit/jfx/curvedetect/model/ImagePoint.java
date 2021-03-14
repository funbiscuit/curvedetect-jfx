package com.funbiscuit.jfx.curvedetect.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class ImagePoint {
    @Getter
    @Setter
    private boolean isSnapped;

    @Getter
    private final UUID id = UUID.randomUUID();

    @Getter
    @Setter
    private Vec2D position;

    public ImagePoint(double x, double y) {
        position = new Vec2D(x, y);
    }

    public void setImagePos(double x, double y) {
        position.setX(x);
        position.setY(y);
    }

    public void snap(ImageWrapper imageWrapper) {
        Vec2D snappedPosition = imageWrapper.snap(position);
        isSnapped = snappedPosition != null;
        if (isSnapped) {
            position = snappedPosition;
        }
    }

    public void unsnap(double x, double y) {
        isSnapped = false;
        position = new Vec2D(x, y);
    }

    @Override
    public String toString() {
        return "ImagePoint{" +
                "isSnapped=" + isSnapped +
                ", id=" + id +
                ", position=" + position +
                '}';
    }
}
