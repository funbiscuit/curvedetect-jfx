package com.funbiscuit.jfx.curvedetect.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Grid of ticks. There are two "X" and two "Y" tick lines.
 * Each X tick line is orthogonal to each Y tick line.
 */
public class TickGrid {

    public static final double MIN_POS = 0.1;
    public static final double MAX_POS = 0.9;
    public static final double MIN_VAL = 0;
    public static final double MAX_VAL = 1;
    private static final UUID EMPTY_ID = new UUID(0, 0);

    @Getter
    private final List<Tick> ticks;

    private Vec2D currentBox = new Vec2D(0, 0);
    private double currentMargin = 0;

    /**
     * Distance at which tick line or anchor is considered hovered.
     */
    private double hoverDistance = 14.0;

    public TickGrid() {
        ArrayList<Tick> list = new ArrayList<>();
        Collections.addAll(list, new Tick(Tick.Type.X), new Tick(Tick.Type.X));
        Collections.addAll(list, new Tick(Tick.Type.Y), new Tick(Tick.Type.Y));

        ticks = Collections.unmodifiableList(list);
    }

    /**
     * Resets tick lines of this grid to default values for specified box size
     * Values are reset to default min and max value.
     *
     * @param boxSize - size of box that should contain ticks
     * @param margin  - margins of box
     */
    public void resetToBox(Vec2D boxSize, double margin) {
        currentBox = boxSize;
        currentMargin = margin;

        List<Tick> xTicks = ticks.stream().filter(t -> t.type == Tick.Type.X).collect(Collectors.toList());
        List<Tick> yTicks = ticks.stream().filter(t -> t.type == Tick.Type.Y).collect(Collectors.toList());

        xTicks.get(0).setValue(MIN_VAL);
        xTicks.get(1).setValue(MAX_VAL);
        xTicks.get(0).point.setPosition(new Vec2D(MIN_POS * boxSize.getX(), boxSize.getY() * 0.5));
        xTicks.get(1).point.setPosition(new Vec2D(MAX_POS * boxSize.getX(), boxSize.getY() * 0.5));

        // max and min flipped since y goes from top to bottom
        yTicks.get(0).setValue(MIN_VAL);
        yTicks.get(1).setValue(MAX_VAL);
        yTicks.get(0).point.setPosition(new Vec2D(boxSize.getX() * 0.5, MAX_POS * boxSize.getY()));
        yTicks.get(1).point.setPosition(new Vec2D(boxSize.getX() * 0.5, MIN_POS * boxSize.getY()));

        ticks.forEach(t -> t.extend(boxSize, margin));
    }

    /**
     * Set direction of Y tick lines (normally is horizontal).
     * Direction of X tick lines is updated to be 90 degrees to Y.
     * If directionY has zero norm, it is assumed to be horizontal
     */
    public void setDirectionY(Vec2D directionY) {
        if (directionY.getNorm() < 0.01) {
            directionY = new Vec2D(1, 0);
        }

        Vec2D dirY = directionY.normalize();
        Vec2D dirX = new Vec2D(dirY.getY(), -dirY.getX());

        ticks.stream().filter(t -> t.type == Tick.Type.X)
                .forEach(t -> t.direction = dirX);
        ticks.stream().filter(t -> t.type == Tick.Type.Y)
                .forEach(t -> t.direction = dirY);

        resetToBox(currentBox, currentMargin);
    }

    public void deselect() {
        ticks.forEach(Tick::deselect);
    }

    public void unhover() {
        ticks.forEach(Tick::unhover);
    }

    public void selectHovered() {
        deselect();
        ticks.stream().filter(Tick::isHovered).limit(1).forEach(Tick::select);
    }

    public void updateHovered(Vec2D position) {
        unhover();
        ticks.stream().min(Comparator.comparingDouble(t -> t.distanceTo(position)))
                .filter(t -> t.distanceTo(position) < hoverDistance)
                .ifPresent(Tick::hover);
    }

    public void dragSelected(Vec2D newPosition) {
        getSelectedTick().ifPresent(t -> t.point.setPosition(newPosition));
    }

    public void snapSelected(ImageWrapper image) {
        getSelectedTick().ifPresent(t -> t.point.snap(image));
    }

    public void unsnapSelected(double x, double y) {
        getSelectedTick().ifPresent(t -> t.point.unsnap(x, y));
    }

    public Optional<Tick> getSelectedTick() {
        return ticks.stream().filter(Tick::isSelected)
                .findFirst();
    }

    public boolean isHovered() {
        return ticks.stream().anyMatch(Tick::isHovered);
    }

    public boolean isSelected() {
        return ticks.stream().anyMatch(Tick::isSelected);
    }

    public Vec2D imageToReal(Vec2D imagePoint) {
        //this function should be called after check that
        //we can really calculate real points
        //so all x and y ticks are defined

        Vec2D realPoint = new Vec2D(0.0D, 0.0D);
        Vec2D x0pos = ticks.get(0).getPosition();
        Vec2D x1pos = ticks.get(1).getPosition();
        Vec2D y0pos = ticks.get(2).getPosition();
        Vec2D y1pos = ticks.get(3).getPosition();
//        Vec2D horizon0pos = horizonSettings.getOrigin().getPosition();
//        Vec2D horizon1pos = horizonSettings.getTarget().getPosition();
        // direction of Y line, normally horizontal
        Vec2D dirTickY = ticks.get(2).direction;
        double det1 = (x0pos.getX() - x1pos.getX()) * dirTickY.getX() +
                (x0pos.getY() - x1pos.getY()) * dirTickY.getY();
        double det2 = (y0pos.getX() - y1pos.getX()) * (-dirTickY.getY()) -
                (y0pos.getY() - y1pos.getY()) * (-dirTickY.getX());

        double a = (ticks.get(0).getValue() - ticks.get(1).getValue()) *
                dirTickY.getX() / det1;
        double b = (ticks.get(0).getValue() - ticks.get(1).getValue()) *
                dirTickY.getY() / det1;
        double c = (ticks.get(2).getValue() - ticks.get(3).getValue()) *
                (-dirTickY.getY()) / det2;
        double d = -(ticks.get(2).getValue() - ticks.get(3).getValue()) *
                (-dirTickY.getX()) / det2;

        double e = ticks.get(0).getValue() - a * x0pos.getX() - b * x0pos.getY();
        double f = ticks.get(2).getValue() - c * y0pos.getX() - d * y0pos.getY();

        realPoint.setX(a * imagePoint.getX() + b * imagePoint.getY() + e);
        realPoint.setY(c * imagePoint.getX() + d * imagePoint.getY() + f);

        return realPoint;
    }

    public Comparator<? super Point> getPointsComparator() {
        Vec2D origin = ticks.get(2).getPosition();
        Vec2D tickYDirection = ticks.get(2).direction;
        double dy = tickYDirection.getY();
        double mDx = tickYDirection.getX();

        return (p1, p2) -> {
            Vec2D p1image = p1.getPosition();
            Vec2D p2image = p2.getPosition();
            double p1Projection = -mDx * (p1image.getX() - origin.getX()) + dy * (p1image.getY() - origin.getY());
            double p2Projection = -mDx * (p2image.getX() - origin.getX()) + dy * (p2image.getY() - origin.getY());
            return Double.compare(p1Projection, p2Projection);
        };
    }

    public List<Anchor> getAnchors() {
        return Collections.emptyList();
    }

    /**
     * Single tick line
     */
    public static class Tick {
        @Getter
        private final Type type;

        @Getter
        private final UUID id = UUID.randomUUID();

        private Vec2D direction;

        @Getter
        @Setter
        private ImagePoint point = new ImagePoint(0, 0);

        @Getter
        private Vec2D start = new Vec2D(0, 0);

        @Getter
        private Vec2D end = new Vec2D(0, 0);

        @Getter
        @Setter
        private double value;

        @Getter
        private boolean isSelected;

        @Getter
        private boolean isHovered;

        private Tick(Type type) {
            this.type = type;
            if (type == Type.X)
                direction = new Vec2D(0, -1);
            else // Type.Y
                direction = new Vec2D(1, 0);
        }

        public Vec2D getPosition() {
            return point.getPosition();
        }

        public void select() {
            isSelected = true;
        }

        public void deselect() {
            isSelected = false;
        }

        public void hover() {
            isHovered = true;
        }

        public void unhover() {
            isHovered = false;
        }

        public final double distanceTo(Vec2D target) {
            double dx = direction.getX();
            double dy = direction.getY();
            double norm = Math.sqrt(dx * dx + dy * dy);
            Vec2D position = point.getPosition();

            double normDx = dx / norm;
            double normDy = dy / norm;
            return Math.abs(normDy * target.getX() - normDx * target.getY()
                    - normDy * position.getX() + normDx * position.getY());
        }

        /**
         * Extends start and end positions of tick line so they belong to
         * specified box boundary (with origin at 0, 0)
         *
         * @param boxSize - size of box boundary
         * @param margin  - margins of box
         */
        public void extend(Vec2D boxSize, double margin) {
            Vec2D point1 = new Vec2D(point.getPosition());
            Vec2D point2 = point1.add(direction);
            double dx = direction.getX();
            double dy = direction.getY();

            final double minDelta = 0.001;

            double extraShift = -dy * point1.getX() + dx * point1.getY();
            Vec2D regionTL = new Vec2D(margin, margin);
            Vec2D regionBR = new Vec2D(boxSize.getX() - margin, boxSize.getY() - margin);
            double righty;
            double lefty;
            if (Math.abs(dx) > minDelta && Math.abs(dy) > minDelta) {
                righty = (extraShift + dy * regionBR.getX()) / dx;
                lefty = (extraShift + dy * regionTL.getX()) / dx;
                double botx = (-extraShift + dx * regionBR.getY()) / dy;
                double topx = (-extraShift + dx * regionTL.getY()) / dy;
                point1.setX(regionTL.getX());
                point1.setY(lefty);
                if (lefty < regionTL.getY()) {
                    point1.setX(topx);
                    point1.setY(regionTL.getY());
                }

                if (lefty > regionBR.getY()) {
                    point1.setX(botx);
                    point1.setY(regionBR.getY());
                }

                point2.setX(regionBR.getX());
                point2.setY(righty);
                if (righty < regionTL.getY()) {
                    point2.setX(topx);
                    point2.setY(regionTL.getY());
                }

                if (righty > regionBR.getY()) {
                    point2.setX(botx);
                    point2.setY(regionBR.getY());
                }

                if (dx < 0) {
                    Vec2D temp = new Vec2D(point1);
                    point1.setX(point2.getX());
                    point1.setY(point2.getY());
                    point2.setX(temp.getX());
                    point2.setY(temp.getY());
                }
            } else {
                Vec2D temp;
                if (Math.abs(dy) > minDelta) {
                    righty = (-extraShift + dx * regionBR.getY()) / dy;
                    lefty = (-extraShift + dx * regionTL.getY()) / dy;
                    point1.setX(righty);
                    point1.setY(regionBR.getY());
                    point2.setX(lefty);
                    point2.setY(regionTL.getY());
                    if (dy > 0) {
                        temp = new Vec2D(point1);
                        point1.setX(point2.getX());
                        point1.setY(point2.getY());
                        point2.setX(temp.getX());
                        point2.setY(temp.getY());
                    }
                } else if (Math.abs(dx) > minDelta) {
                    righty = (extraShift + dy * regionBR.getX()) / dx;
                    lefty = (extraShift + dy * regionTL.getX()) / dx;
                    point1.setX(regionTL.getX());
                    point1.setY(lefty);
                    point2.setX(regionBR.getX());
                    point2.setY(righty);
                    if (dx < 0) {
                        temp = new Vec2D(point1);
                        point1.setX(point2.getX());
                        point1.setY(point2.getY());
                        point2.setX(temp.getX());
                        point2.setY(temp.getY());
                    }
                }
            }

            start = point1;
            end = point2;
        }

        @Override
        public String toString() {
            return "Tick{" +
                    "type=" + type +
                    ", id=" + id +
                    ", direction=" + direction +
                    ", point=" + point +
                    ", start=" + start +
                    ", end=" + end +
                    ", value=" + value +
                    ", isSelected=" + isSelected +
                    ", isHovered=" + isHovered +
                    '}';
        }

        public enum Type {
            /**
             * Tick lines that go vertically (have "x" value) for normal orientation
             */
            X,

            /**
             * Tick lines that go horizontally (have "y" value) for normal orientation
             */
            Y
        }

    }

    /**
     * Anchor represents point where two tick lines cross.
     * By dragging anchor orientation of tick lines can be changed.
     */
    public static class Anchor {

    }
}
