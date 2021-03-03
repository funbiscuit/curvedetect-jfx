package com.funbiscuit.jfx.curvedetect.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public final class TickPoint {
    @Getter
    private final ImagePoint imagePoint;

    @Getter
    @Setter
    private double tickValue;

    @Getter
    @Setter
    private boolean isNew;

    private TickPoint backup;

    public TickPoint(double imageX, double imageY) {
        imagePoint = new ImagePoint(imageX, imageY);
    }

    public Vec2D getPosition() {
        return imagePoint.getPosition();
    }

    public UUID getId() {
        return imagePoint.getId();
    }

    public final double distanceTo(Vec2D point, Vec2D tickDirection) {
        double dx = tickDirection.getX();
        double dy = tickDirection.getY();
        double norm = Math.sqrt(dx * dx + dy * dy);
        if (norm < 0.1D) {
            return -1.0D;
        } else {
            Vec2D thisPos = imagePoint.getPosition();
            double normDy = dy / norm;
            double normDx = dx / norm;
            double extraShift = -normDy * thisPos.getX() + normDx * thisPos.getY();
            return Math.abs(normDy * point.getX() - normDx * point.getY() + extraShift);
        }
    }

    public final void makeBackup() {
        backup = new TickPoint(imagePoint.getPosition().getX(), imagePoint.getPosition().getY());
        backup.tickValue = tickValue;
    }

    public final void restoreBackup() {
        if (backup != null) {
            imagePoint.setImagePos(backup.getImagePoint().getPosition().getX(),
                    backup.getImagePoint().getPosition().getY());
            tickValue = backup.tickValue;
            backup = null;
        }
    }
}
