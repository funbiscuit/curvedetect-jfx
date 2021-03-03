package com.funbiscuit.jfx.curvedetect.model;

public interface ImageElement {
    final class Type {
        public static final int POINT = 1;
        public static final int X_TICK = 2;
        public static final int Y_TICK = 4;
        public static final int HORIZON = 8;
        public static final int ALL = 15;
    }
}
