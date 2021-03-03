package com.funbiscuit.jfx.curvedetect.model;

import lombok.Getter;
import lombok.Setter;

public class Point extends ImagePoint {
    @Getter
    @Setter
    private boolean isSubdivisionPoint;

    public Point(double imageX, double imageY) {
        super(imageX, imageY);
    }
}
