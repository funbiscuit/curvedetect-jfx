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
    private final Vec2D position;

    public ImagePoint(double x, double y) {
        position = new Vec2D(x, y);
    }

    public void setImagePos(double x, double y) {
        position.setX(x);
        position.setY(y);
    }
}
