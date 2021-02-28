package com.funbiscuit.jfx.curvedetect;

import java.util.UUID;

public interface ImageElement {
    boolean isSnapped();

    void setSnapped(boolean snapped);

    UUID getId();

    Vec2D getImagePos();

    void setImagePos(double x, double y);

    final class Type {
        public static final int POINT = 1;
        public static final int X_TICK = 2;
        public static final int Y_TICK = 4;
        public static final int HORIZON = 8;
        public static final int ALL = 15;
    }
}
