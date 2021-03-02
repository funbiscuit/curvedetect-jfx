package com.funbiscuit.jfx.curvedetect;

import com.funbiscuit.jfx.curvedetect.model.Vec2D;

import java.util.UUID;

public final class TickPoint implements ImageElement {
    private UUID id = UUID.randomUUID();
    private Point point;
    private boolean isSnapped;
    private double tickValue;
    private boolean isNew;
    private TickPoint backup;

    public TickPoint(double imageX, double imageY) {
        point = new Point(imageX, imageY);
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

    public final double getTickValue() {
        return tickValue;
    }

    public final void setTickValue(double tickValue) {
        this.tickValue = tickValue;
    }

    public final boolean isNew() {
        return isNew;
    }

    public final void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public Vec2D getImagePos() {
        return point.getImagePos();
    }

    public void setImagePos(double x, double y) {
        point.setImagePos(x, y);
    }

    public final double distanceTo(ImageElement point, Vec2D tickDirection) {
        double dx = tickDirection.getX();
        double dy = tickDirection.getY();
        double norm = Math.sqrt(dx * dx + dy * dy);
        if (norm < 0.1D) {
            return -1.0D;
        } else {
            Vec2D thisPos = this.point.getImagePos();
            Vec2D thatPos = point.getImagePos();
            double normDy = dy / norm;
            double normDx = dx / norm;
            double extraShift = -normDy * thisPos.getX() + normDx * thisPos.getY();
            return Math.abs(normDy * thatPos.getX() - normDx * thatPos.getY() + extraShift);
        }
    }

    public final void makeBackup() {
        backup = new TickPoint(this.getImagePos().getX(), this.getImagePos().getY());
        backup.tickValue = tickValue;
    }

    public final void restoreBackup() {
        if (backup != null) {
            point.setImagePos(backup.getImagePos().getX(), backup.getImagePos().getY());
            tickValue = backup.tickValue;
            backup = null;
        }
    }
}
