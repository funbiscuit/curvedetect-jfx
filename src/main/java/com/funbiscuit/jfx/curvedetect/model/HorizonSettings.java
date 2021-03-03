package com.funbiscuit.jfx.curvedetect.model;

import lombok.Getter;
import lombok.Setter;

public final class HorizonSettings {
    @Getter
    private final ImagePoint origin;

    @Getter
    private final ImagePoint target;

    @Getter
    @Setter
    private boolean isValid;

    public HorizonSettings(double imageX, double imageY) {
        origin = new ImagePoint(imageX, imageY);
        target = new ImagePoint(imageX + 100, imageY);
    }

    public final Vec2D getVerticalDirection() {
        Vec2D originPos = origin.getPosition();
        Vec2D targetPos = target.getPosition();

        Vec2D dir = new Vec2D(targetPos.getY() - originPos.getY(),
                -targetPos.getX() + originPos.getX());
        double norm = Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY());
        if (norm > 2.0) {
            dir.setX(dir.getX() / norm);
            dir.setY(dir.getY() / norm);
        } else {
            dir.setX(1.0);
            dir.setY(0.0);
        }

        return dir;
    }

    public final Vec2D getHorizontalDirection() {
        Vec2D originPos = origin.getPosition();
        Vec2D targetPos = target.getPosition();

        Vec2D dir = new Vec2D(targetPos.getX() - originPos.getX(),
                targetPos.getY() - originPos.getY());
        double norm = Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY());
        if (norm > 2.0) {
            dir.setX(dir.getX() / norm);
            dir.setY(dir.getY() / norm);
        } else {
            dir.setX(0.0);
            dir.setY(-1.0);
        }

        return dir;
    }

    public enum HorizonPoint {
        NONE,
        ORIGIN,
        TARGET;
    }
}
