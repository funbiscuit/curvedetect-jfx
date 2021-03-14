package com.funbiscuit.jfx.curvedetect.model;

import lombok.AllArgsConstructor;
import lombok.Data;

//TODO make immutable
@Data
@AllArgsConstructor
public class Vec2D {
    private double x;
    private double y;

    public Vec2D(Vec2D point) {
        this(point.x, point.y);
    }

    public Vec2D add(Vec2D v) {
        return new Vec2D(x + v.x, y + v.y);
    }

    public Vec2D normalize() {
        double norm = getNorm();
        return new Vec2D(x / norm, y / norm);
    }

    public double getNorm() {
        return Math.sqrt(x * x + y * y);
    }
}
