package com.funbiscuit.jfx.curvedetect;

import javafx.geometry.Point2D;

public class Vec2D {
    private double x;
    private double y;

    public Vec2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2D(Vec2D point) {
        this(point.x, point.y);
    }

    public Point2D toPoint2D() {
        return new Point2D(x, y);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
