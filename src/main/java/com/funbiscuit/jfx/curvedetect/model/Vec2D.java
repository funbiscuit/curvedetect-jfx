package com.funbiscuit.jfx.curvedetect.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Vec2D {
    private double x;
    private double y;

    public Vec2D(Vec2D point) {
        this(point.x, point.y);
    }
}
