package com.funbiscuit.jfx.curvedetect.model;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.UUID;


public class ImageCurve {
    private final ArrayList<Point> allPoints;
    private final ArrayList<Point> userPoints;
    private final ArrayList<TickPoint> xTickPoints;
    private final ArrayList<TickPoint> yTickPoints;
    private final HorizonSettings horizonSettings;
    private final double hoverZone = 14.0;
    private Point hoveredPoint;
    private Point selectedPoint;
    private TickPoint hoveredXtick;
    private TickPoint selectedXtick;
    private TickPoint hoveredYtick;
    private TickPoint selectedYtick;
    private HorizonSettings.HorizonPoint hoveredOrigin;
    private HorizonSettings.HorizonPoint selectedOrigin;
    private ImageWrapper image;
    private int subdivideIterations;


    public ImageCurve() {
        hoveredOrigin = HorizonSettings.HorizonPoint.NONE;
        selectedOrigin = HorizonSettings.HorizonPoint.NONE;
        subdivideIterations = 3;
        allPoints = new ArrayList<>();
        userPoints = new ArrayList<>();
        xTickPoints = new ArrayList<>();
        yTickPoints = new ArrayList<>();
        horizonSettings = new HorizonSettings(0.0D, 0.0D);
    }

    public UUID getSelectedId() {
        ImageElement selected = getSelectedElement();
        if (selected != null)
            return selected.getId();
        return null;
    }

    private ImageElement getSelectedElement() {
        if (selectedPoint != null)
            return selectedPoint;
        if (selectedXtick != null)
            return selectedXtick;
        if (selectedYtick != null)
            return selectedYtick;
        if (selectedOrigin == HorizonSettings.HorizonPoint.ORIGIN)
            return horizonSettings;
        if (selectedOrigin == HorizonSettings.HorizonPoint.TARGET)
            return horizonSettings.getTarget();
        return null;
    }

    public UUID getHoveredId(int type) {
        if ((type & ImageElement.Type.POINT) != 0 && hoveredPoint != null)
            return hoveredPoint.getId();
        if ((type & ImageElement.Type.X_TICK) != 0 && hoveredXtick != null)
            return hoveredXtick.getId();
        if ((type & ImageElement.Type.Y_TICK) != 0 && hoveredYtick != null)
            return hoveredYtick.getId();
        if ((type & ImageElement.Type.HORIZON) != 0 && hoveredOrigin != null) {
            if (hoveredOrigin == HorizonSettings.HorizonPoint.ORIGIN)
                return horizonSettings.getId();
            else if (hoveredOrigin == HorizonSettings.HorizonPoint.TARGET)
                return horizonSettings.getTarget().getId();
        }

        return null;
    }


    public TickPoint getSelectedTick() {
        TickPoint var10000 = this.selectedXtick;
        if (var10000 == null) {
            var10000 = this.selectedYtick;
        }

        return var10000;
    }

    public void addPoint(double x, double y) {
        selectedPoint = new Point(x, y);
        userPoints.add(selectedPoint);

        snapSelected();
        sortPoints();
    }

    public void addXtick(double x, double y) {
        selectedXtick = new TickPoint(x, y);
        selectedXtick.setNew(true);


        xTickPoints.add(selectedXtick);
        if (xTickPoints.size() > 2) {
            xTickPoints.remove(0);
        }

        snapSelected();
    }

    public void addYtick(double x, double y) {
        this.selectedYtick = new TickPoint(x, y);
        selectedYtick.setNew(true);


        yTickPoints.add(selectedYtick);
        if (yTickPoints.size() > 2) {
            yTickPoints.remove(0);
        }

        snapSelected();
    }

    public void dragSelected(double newX, double newY) {
        ImageElement selected = getSelectedElement();
        if (selected != null) {
            selected.setImagePos(newX, newY);
        }
    }

    public void snapSelected() {
        ImageElement selected = this.getSelectedElement();
        if (selected != null && image != null) {
            selected.setSnapped(image.snap(selected));
        }
    }

    public void unsnapSelected(double newX, double newY) {
        ImageElement selected = this.getSelectedElement();
        if (selected != null) {
            selected.setImagePos(newX, newY);
        }
    }

    public void sortPoints() {
        sortPointsArray(userPoints);
        updateSubdivision();
    }

    private void updateSubdivision() {
        if (image == null)
            return;

        int pointsNum = userPoints.size();

        //TODO we are able to reuse old data, so don't clear it
        allPoints.clear();

        if (userPoints.size() < 2) {
            if (userPoints.size() == 1)
                allPoints.add(userPoints.get(0));

            return;
        }

        //extra points for each two user points
        int extraPoints = (1 << subdivideIterations) - 1; //=(2^S)-1
        int totalPointNum = (extraPoints + 1) * (pointsNum - 1) + 1; //subdivided+user

        allPoints.ensureCapacity(totalPointNum);

        for (int i = 0; i < totalPointNum; i++) {
            allPoints.add(new Point(0.0D, 0.0D));
        }

        //copy existing points to array
        for (int i = 0; i < userPoints.size(); i++) {
            allPoints.set(i * (extraPoints + 1), userPoints.get(i));
        }

        //number of points that we need to movee from one border to mid point (excluding)
        int extraPointsHalf = 1 << subdivideIterations - 1; //=(2^S-1)-1

        for (int i = 0; i < userPoints.size() - 1; i++) {
            //TODO check if we need to subdivide this segment

            int step = extraPoints + 1;
            int leftIndex = i * step;
            int rightIndex = leftIndex + step;

            for (int j = 1; j <= extraPointsHalf; j++) {
                Vec2D leftPointPos = allPoints.get(leftIndex).getImagePos();
                Vec2D rightPointPos = allPoints.get(rightIndex).getImagePos();

                Point nextLeft = allPoints.get(leftIndex + 1);
                Point nextRight = allPoints.get(rightIndex - 1);

                //TODO is this possible?
                if (nextLeft == null || nextRight == null)
                    continue;

                nextLeft.setImagePos(leftPointPos.getX() + (rightPointPos.getX() - leftPointPos.getX()) * (1.0D / (double) (rightIndex - leftIndex)), leftPointPos.getY() + (rightPointPos.getY() - leftPointPos.getY()) * (1.0D / (double) (rightIndex - leftIndex)));
                nextLeft.setSnapped(image.snap(nextLeft));
                nextLeft.setSubdivisionPoint(true);

                if (j != extraPointsHalf) {
                    nextRight.setImagePos(rightPointPos.getX() - (rightPointPos.getX() - leftPointPos.getX()) * (1.0D / (double) (rightIndex - leftIndex)), rightPointPos.getY() - (rightPointPos.getY() - leftPointPos.getY()) * (1.0D / (double) (rightIndex - leftIndex)));
                    nextRight.setSnapped(image.snap(nextRight));
                    nextRight.setSubdivisionPoint(true);
                }

                leftIndex++;
                rightIndex--;
            }

        }

        sortPointsArray(allPoints);
    }

    private void sortPointsArray(ArrayList<Point> array) {
        Vec2D dir = horizonSettings.getHorizontalDirection();
        Vec2D horizonPos = horizonSettings.getImagePos();
        double dy = dir.getY();
        double mDx = -dir.getX();
        int gridInvertion = 1;

        if (xTickPoints.size() > 1) {
            TickPoint left;
            TickPoint right;

            if (xTickPoints.get(0).getTickValue() < xTickPoints.get(1).getTickValue()) {
                left = xTickPoints.get(0);
                right = xTickPoints.get(1);
            } else {
                left = xTickPoints.get(1);
                right = xTickPoints.get(0);
            }

            Vec2D p1image = left.getImagePos();
            Vec2D p2image = right.getImagePos();
            double p1Projection = -mDx * (p1image.getX() - horizonPos.getX()) + dy * (p1image.getY() - horizonPos.getY());
            double p2Projection = -mDx * (p2image.getX() - horizonPos.getX()) + dy * (p2image.getY() - horizonPos.getY());
            if (p1Projection > p2Projection) {
                gridInvertion = -1;
            }
        }

        int gridInv = gridInvertion;

        array.sort((p1, p2) -> {
            Vec2D p1image = p1.getImagePos();
            Vec2D p2image = p2.getImagePos();
            double p1Projection = -mDx * (p1image.getX() - horizonPos.getX()) + dy * (p1image.getY() - horizonPos.getY());
            double p2Projection = -mDx * (p2image.getX() - horizonPos.getX()) + dy * (p2image.getY() - horizonPos.getY());
            return p1Projection < p2Projection ? -gridInv : (p1Projection > p2Projection ? gridInv : 0);
        });

    }

    public void resetSelectedTick() {
        //cancel all changes that were made to selected tick since it was selected
        TickPoint selected = selectedXtick;
        if (selected == null) {
            selected = selectedYtick;
        }

        if (selected != null) {
            selected.setNew(false);
            selected.restoreBackup();
        }

        selectedXtick = null;
        selectedYtick = null;
    }

    public void deleteSelectedTick() {
        TickPoint selected = this.selectedXtick;
        if (selected == null) {
            selected = this.selectedYtick;
        }

        if (selected != null) {
            xTickPoints.remove(selected);
            yTickPoints.remove(selected);
        }

        selectedXtick = null;
        selectedYtick = null;
    }

    private void updateHoveredPoint(double x, double y) {
        double minDist = hoverZone * hoverZone;
        hoveredPoint = null;

        for (Point point : userPoints) {
            Vec2D pos = point.getImagePos();
            double dx = pos.getX() - x;
            double dy = pos.getY() - y;
            double dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                hoveredPoint = point;
            }
        }
    }

    private void updateHoveredXtick(double x, double y) {
        double minDist = hoverZone;
        hoveredXtick = null;

        Vec2D tickDirection = horizonSettings.getVerticalDirection();
        Point point = new Point(x, y);

        for (TickPoint xTick : xTickPoints) {
            double dist = xTick.distanceTo(point, tickDirection);
            if (dist < minDist) {
                minDist = dist;
                hoveredXtick = xTick;
            }
        }
    }

    private void updateHoveredYtick(double x, double y) {
        double minDist = hoverZone;
        hoveredYtick = null;
        Vec2D tickDirection = horizonSettings.getHorizontalDirection();
        Point point = new Point(x, y);

        for (TickPoint yTick : yTickPoints) {
            double dist = yTick.distanceTo(point, tickDirection);
            if (dist < minDist) {
                minDist = dist;
                hoveredYtick = yTick;
            }
        }
    }

    private void updateHoveredHorizon(double x, double y) {
        hoveredOrigin = HorizonSettings.HorizonPoint.NONE;
        if (!horizonSettings.isValid())
            return;

        double minDist = hoverZone * hoverZone;

        Vec2D originPos = horizonSettings.getImagePos();
        Vec2D tagetPos = horizonSettings.getTarget().getImagePos();

        double dx = originPos.getX() - x;
        double dy = originPos.getY() - y;
        double dist = dx * dx + dy * dy;

        dx = tagetPos.getX() - x;
        dy = tagetPos.getY() - y;
        double dist2 = dx * dx + dy * dy;

        if (dist < dist2 && dist < minDist) {
            hoveredOrigin = HorizonSettings.HorizonPoint.ORIGIN;
        } else if (dist2 < minDist) {
            hoveredOrigin = HorizonSettings.HorizonPoint.TARGET;
        }
    }

    public void backupSelectedTick() {
        TickPoint selectedTick = getSelectedTick();
        if (selectedTick != null) {
            selectedTick.makeBackup();
        }
    }

    public void deselectAll() {
        selectedPoint = null;
        selectedOrigin = HorizonSettings.HorizonPoint.NONE;
        selectedXtick = null;
        selectedYtick = null;
    }

    public void deleteSelected() {
        if (selectedPoint != null) {
            userPoints.remove(selectedPoint);
            selectedPoint = null;
            hoveredPoint = null;
            sortPoints();
        } else {
            if (selectedXtick != null) {
                xTickPoints.remove(selectedXtick);
                hoveredXtick = null;
                selectedXtick = null;
            } else if (selectedYtick != null) {
                yTickPoints.remove(selectedYtick);
                hoveredYtick = null;
                selectedYtick = null;
            } else if (selectedOrigin != HorizonSettings.HorizonPoint.NONE) {
                resetHorizon();
            }
        }
    }

    public void setImage(ImageWrapper image) {
        this.image = image;
    }

    public void setSubdivision(int value) {
        subdivideIterations = value;
    }

    public void resetHorizon() {
        horizonSettings.setValid(false);
        if (image == null)
            return;
        Image img = image.getImage();

        horizonSettings.setImagePos(img.getWidth() * 0.1, img.getHeight() * 0.5);
        horizonSettings.getTarget().setImagePos(img.getWidth() * 0.9, img.getHeight() * 0.5);
        selectedOrigin = HorizonSettings.HorizonPoint.NONE;
        hoveredOrigin = HorizonSettings.HorizonPoint.NONE;
        horizonSettings.setValid(true);
    }

    public void resetPoints() {
        allPoints.clear();
        userPoints.clear();
        xTickPoints.clear();
        yTickPoints.clear();
        horizonSettings.setValid(false);
        selectedOrigin = HorizonSettings.HorizonPoint.NONE;
        selectedYtick = null;
        selectedXtick = null;
        selectedPoint = null;
        hoveredPoint = null;
    }

    public boolean selectHovered(int type) {

        if ((type & ImageElement.Type.POINT) != 0 && hoveredPoint != null) {
            deselectAll();
            selectedPoint = hoveredPoint;
        } else if ((type & ImageElement.Type.X_TICK) != 0 && hoveredXtick != null) {
            deselectAll();
            selectedXtick = hoveredXtick;
        } else if ((type & ImageElement.Type.Y_TICK) != 0 && hoveredYtick != null) {
            deselectAll();
            selectedYtick = hoveredYtick;
        } else if ((type & ImageElement.Type.HORIZON) != 0 && hoveredOrigin != HorizonSettings.HorizonPoint.NONE) {
            deselectAll();
            selectedOrigin = hoveredOrigin;
        } else {
            return false;
        }

        return true;
    }

    public void updateHoveredElement(double x, double y) {
        updateHoveredElement(x, y, ImageElement.Type.ALL);
    }

    public void updateHoveredElement(double x, double y, int type) {
        if ((type & ImageElement.Type.POINT) != 0)
            updateHoveredPoint(x, y);

        if ((type & ImageElement.Type.X_TICK) != 0)
            updateHoveredXtick(x, y);

        if ((type & ImageElement.Type.Y_TICK) != 0)
            updateHoveredYtick(x, y);

        if ((type & ImageElement.Type.HORIZON) != 0)
            updateHoveredHorizon(x, y);
    }

    public void makeTickInput(double newValue) {
        TickPoint selected = selectedXtick;
        if (selected == null) {
            selected = selectedYtick;
        }

        if (selected != null) {
            selected.setNew(false);
            selected.setTickValue(newValue);
        }

        selectedXtick = null;
        selectedYtick = null;
    }

    public boolean isXgridReady() {
        return xTickPoints.size() > 1 && (getGridOverlapState() &
                (ExportStatus.VALUE_OVERLAP_X_GRID |
                        ExportStatus.PIXEL_OVERLAP_X_GRID)) == 0;
    }

    public boolean isYgridReady() {
        return yTickPoints.size() > 1 && (getGridOverlapState() &
                (ExportStatus.VALUE_OVERLAP_Y_GRID |
                        ExportStatus.PIXEL_OVERLAP_Y_GRID)) == 0;
    }

    public int getExportStatus() {
        int status = ExportStatus.READY;
        if (image == null) {
            status |= ExportStatus.NO_IMAGE;
            return status;
        }

        switch (allPoints.size()) {
            case 0:
                status |= ExportStatus.NO_POINTS;
                break;
            case 1:
                status |= ExportStatus.ONE_POINT;
        }


        for (int i = 0; i <= 1; ++i) {
            ArrayList<TickPoint> ticks = i == 0 ? xTickPoints : yTickPoints;
            int noLinesFlag = i == 0 ? ExportStatus.NO_X_GRID_LINES : ExportStatus.NO_Y_GRID_LINES;
            int oneLineFlag = i == 0 ? ExportStatus.ONE_X_GRID_LINE : ExportStatus.ONE_Y_GRID_LINE;
            int ticksEntered = 0;

            for (TickPoint tick : ticks) {
                if (!tick.isNew()) {
                    ++ticksEntered;
                }
            }

            switch (ticksEntered) {
                case 0:
                    status |= noLinesFlag;
                    break;
                case 1:
                    status |= oneLineFlag;
            }
        }

        status |= getGridOverlapState();

        return status;
    }

    private int getGridOverlapState() {
        int result = 0;

        //TODO remove magic numbers
        double minPixelDist = 5.0;
        double minValueDist = 1.0e-8;

        Vec2D tickDirection;
        if (xTickPoints.size() > 1) {
            tickDirection = horizonSettings.getVerticalDirection();
            TickPoint tick0 = xTickPoints.get(0);
            TickPoint tick1 = xTickPoints.get(1);

            if (tick0.distanceTo(tick1, tickDirection) < minPixelDist) {
                result |= ExportStatus.PIXEL_OVERLAP_X_GRID;
            }

            if (Math.abs(tick0.getTickValue() - tick1.getTickValue()) < minValueDist) {
                result |= ExportStatus.VALUE_OVERLAP_X_GRID;
            }
        }

        if (yTickPoints.size() > 1) {
            tickDirection = horizonSettings.getHorizontalDirection();
            TickPoint tick0 = yTickPoints.get(0);
            TickPoint tick1 = yTickPoints.get(1);

            if (tick0.distanceTo(tick1, tickDirection) < minPixelDist) {
                result |= ExportStatus.PIXEL_OVERLAP_Y_GRID;
            }

            if (Math.abs(tick0.getTickValue() - tick1.getTickValue()) < minValueDist) {
                result |= ExportStatus.VALUE_OVERLAP_Y_GRID;
            }
        }

        return result;
    }

    private Vec2D imageToReal(Vec2D imagePoint) {
        //this function should be called after check that
        //we can really calculate real points
        //so all x and y ticks are defined

        Vec2D realPoint = new Vec2D(0.0D, 0.0D);
        Vec2D x0pos = xTickPoints.get(0).getImagePos();
        Vec2D x1pos = xTickPoints.get(1).getImagePos();
        Vec2D y0pos = yTickPoints.get(0).getImagePos();
        Vec2D y1pos = yTickPoints.get(1).getImagePos();
        Vec2D horizon0pos = horizonSettings.getImagePos();
        Vec2D horizon1pos = horizonSettings.getTarget().getImagePos();
        double det1 = (x0pos.getX() - x1pos.getX()) * (horizon1pos.getX() - horizon0pos.getX()) +
                (x0pos.getY() - x1pos.getY()) * (horizon1pos.getY() - horizon0pos.getY());
        double det2 = (y0pos.getX() - y1pos.getX()) * (horizon0pos.getY() - horizon1pos.getY()) -
                (y0pos.getY() - y1pos.getY()) * (horizon0pos.getX() - horizon1pos.getX());

        double a = (xTickPoints.get(0).getTickValue() - xTickPoints.get(1).getTickValue()) *
                (horizon1pos.getX() - horizon0pos.getX()) / det1;
        double b = (xTickPoints.get(0).getTickValue() - xTickPoints.get(1).getTickValue()) *
                (horizon1pos.getY() - horizon0pos.getY()) / det1;
        double c = (yTickPoints.get(0).getTickValue() - yTickPoints.get(1).getTickValue()) *
                (horizon0pos.getY() - horizon1pos.getY()) / det2;
        double d = -(yTickPoints.get(0).getTickValue() - yTickPoints.get(1).getTickValue()) *
                (horizon0pos.getX() - horizon1pos.getX()) / det2;

        double e = xTickPoints.get(0).getTickValue() - a * x0pos.getX() - b * x0pos.getY();
        double f = yTickPoints.get(0).getTickValue() - c * y0pos.getX() - d * y0pos.getY();

        realPoint.setX(a * imagePoint.getX() + b * imagePoint.getY() + e);
        realPoint.setY(c * imagePoint.getX() + d * imagePoint.getY() + f);

        return realPoint;
    }

    public HorizonSettings getHorizon() {
        return horizonSettings;
    }

    public ArrayList<TickPoint> getXticks() {
        return xTickPoints;
    }

    public ArrayList<TickPoint> getYticks() {
        return yTickPoints;
    }

    public ArrayList<Point> getAllPoints() {
        return allPoints;
    }

    public ArrayList<Point> getUserPoints() {
        return userPoints;
    }

    public int getAllPointsCount() {
        return allPoints.size();
    }

    public int getUserPointsCount() {
        return userPoints.size();
    }

    public void writeRealPoints(ArrayList<Vec2D> allPoints, ArrayList<Vec2D> userPoints) {
        if (allPoints != null) {
            convertPoints(this.allPoints, allPoints);
        }

        if (userPoints != null) {
            convertPoints(this.userPoints, userPoints);
        }
    }

    public void writeRealPoints(double[] allPoints, double[] userPoints) {
        if (allPoints != null) {
            this.convertPoints(this.allPoints, allPoints);
        }

        if (userPoints != null) {
            this.convertPoints(this.userPoints, userPoints);
        }
    }

    public void writeImagePoints(double[] allPoints, double[] userPoints) {
        if (allPoints != null) {
            this.copyPoints(this.allPoints, allPoints);
        }

        if (userPoints != null) {
            this.copyPoints(this.userPoints, userPoints);
        }
    }

    private void convertPoints(ArrayList<? extends ImageElement> imagePoints, ArrayList<Vec2D> out_realPoints) {
        if (out_realPoints.size() > 0) {
            out_realPoints.clear();
        }

        out_realPoints.ensureCapacity(imagePoints.size());

        for (ImageElement point : imagePoints) {
            Vec2D imagePos = point.getImagePos();
            Vec2D exportPoint = imageToReal(imagePos);
            out_realPoints.add(exportPoint);
        }
    }

    private void convertPoints(ArrayList<? extends ImageElement> imagePoints, double[] out_realPoints) {
        //don't do anything if size is not enough
        if (out_realPoints.length < imagePoints.size() * 2)
            return;

        for (int i = 0; i < imagePoints.size(); i++) {
            ImageElement point = imagePoints.get(i);
            Vec2D imagePos = point.getImagePos();
            Vec2D exportPoint = this.imageToReal(imagePos);
            out_realPoints[2 * i] = exportPoint.getX();
            out_realPoints[2 * i + 1] = exportPoint.getY();
        }
    }

    private void copyPoints(ArrayList<? extends ImageElement> points, double[] out_Points) {
        //don't do anything if size is not enough
        if (out_Points.length < points.size() * 2)
            return;

        for (int i = 0; i < points.size(); i++) {
            Vec2D point = points.get(i).getImagePos();
            out_Points[2 * i] = point.getX();
            out_Points[2 * i + 1] = point.getY();
        }
    }

    public static final class ExportStatus {
        public static final int READY = 0;
        public static final int NO_POINTS = 1;
        public static final int ONE_POINT = 1 << 1;
        public static final int NO_X_GRID_LINES = 1 << 2;
        public static final int ONE_X_GRID_LINE = 1 << 3;
        public static final int VALUE_OVERLAP_X_GRID = 1 << 4;
        public static final int PIXEL_OVERLAP_X_GRID = 1 << 5;
        public static final int NO_Y_GRID_LINES = 1 << 6;
        public static final int ONE_Y_GRID_LINE = 1 << 7;
        public static final int VALUE_OVERLAP_Y_GRID = 1 << 8;
        public static final int PIXEL_OVERLAP_Y_GRID = 1 << 9;
        public static final int NO_IMAGE = 1 << 10;
    }
}

