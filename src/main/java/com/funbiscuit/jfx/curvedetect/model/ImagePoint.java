package com.funbiscuit.jfx.curvedetect.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class ImagePoint {
    @Getter
    @Setter
    private boolean isSnapped;

    @Getter
    private UUID id;

    @Getter
    @Setter
    private Vec2D position;

    public void setImagePos(double x, double y) {
        position.setX(x);
        position.setY(y);
    }
}
