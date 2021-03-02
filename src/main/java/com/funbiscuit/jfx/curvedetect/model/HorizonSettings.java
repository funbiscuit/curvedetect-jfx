package com.funbiscuit.jfx.curvedetect.model;

import java.util.UUID;

public final class HorizonSettings implements ImageElement {
    private UUID id = UUID.randomUUID();

    private Point point;
    private Point targetPoint;

    private boolean isSnapped;
    private boolean isValid;

    public HorizonSettings(double imageX, double imageY) {
        point = new Point(imageX, imageY);
        targetPoint = new Point(imageX + 100, imageY);
    }

    public boolean isSnapped() {
        return isSnapped;
    }

    public void setSnapped(boolean isSnapped) {
        this.isSnapped = isSnapped;
    }

    public UUID getId() {
        return id;
    }

    public final boolean isValid() {
        return isValid;
    }

    public final void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public final ImageElement getTarget() {
        return targetPoint;
    }

    public Vec2D getImagePos() {
        return point.getImagePos();
    }

    public void setImagePos(double x, double y) {
        point.setImagePos(x, y);
    }

    public final Vec2D getVerticalDirection() {
        Vec2D originPos = point.getImagePos();
        Vec2D targetPos = targetPoint.getImagePos();

        Vec2D dir = new Vec2D(targetPos.getY() - originPos.getY(),
                -targetPos.getX() + originPos.getX());
        double norm = Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY());
        if (norm > 2.0D) {
            dir.setX(dir.getX() / norm);
            dir.setY(dir.getY() / norm);
        } else {
            dir.setX(1.0D);
            dir.setY(0.0D);
        }

        return dir;
    }

    public final Vec2D getHorizontalDirection() {
        Vec2D originPos = point.getImagePos();
        Vec2D targetPos = targetPoint.getImagePos();

        Vec2D dir = new Vec2D(targetPos.getX() - originPos.getX(),
                targetPos.getY() - originPos.getY());
        double norm = Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY());
        if (norm > 2.0D) {
            dir.setX(dir.getX() / norm);
            dir.setY(dir.getY() / norm);
        } else {
            dir.setX(0.0D);
            dir.setY(-1.0D);
        }

        return dir;
    }

    public enum HorizonPoint {
        NONE,
        ORIGIN,
        TARGET;
    }
}
