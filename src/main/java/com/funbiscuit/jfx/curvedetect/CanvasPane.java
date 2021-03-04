package com.funbiscuit.jfx.curvedetect;


import com.funbiscuit.jfx.curvedetect.model.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.UUID;

public final class CanvasPane extends Pane {

    private final Canvas canvas = new Canvas(100.0d, 100.0d);

    private GraphicsContext gc;

    @Setter
    private ImageCurve imageCurve;
    //private ImageWrapper image;

    //with this parameters image will be fit to canvas and centered
    private double defaultImageScale = 1;
    private double currentImageScale = 1;
    private Vec2D defaultImageOffset = new Vec2D(0, 0);
    private Vec2D currentImageOffset = new Vec2D(0, 0);

    @Getter
    BooleanProperty deleteMode = new SimpleBooleanProperty(false);
    @Getter
    private final ObjectProperty<MainController.WorkMode> workMode = new SimpleObjectProperty<>();

    @Setter
    private boolean drawSubdivisionMarkers = true;
    @Setter
    private boolean showImage = true;
    @Setter
    private boolean showBinarization;

    public CanvasPane() {
        getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();
    }

    protected void layoutChildren() {
        super.layoutChildren();
        double x = snappedLeftInset();
        double y = snappedTopInset();
        // Java 9 - snapSize is deprecated use snapSizeX() and snapSizeY() accordingly
        // but they are not available in Java 9
        double w = snapSize(getWidth()) - x - snappedRightInset();
        double h = snapSize(getHeight()) - y - snappedBottomInset();

        canvas.setLayoutX(x);
        canvas.setLayoutY(y);
        canvas.setWidth(w);
        canvas.setHeight(h);

        updateImageTransform();
        redrawCanvas(false);
    }


    private void cleanCanvas() {
        gc.setFill(Color.gray(0.17));
        gc.fillRect(0, 0, getWidth(), getHeight());

        drawImage();
    }

    private void drawImage() {
        ImageWrapper image = imageCurve.getImage();
        if (image == null || !showImage)
            return;
        Image img = showBinarization ? image.getBwImage() : image.getImage();

        gc.drawImage(img, currentImageOffset.getX(), currentImageOffset.getY(),
                currentImageScale * img.getWidth(), currentImageScale * img.getHeight());
    }

    private void drawPoints() {
        ArrayList<Point> allPoints = imageCurve.getAllPoints();
        ArrayList<Point> userPoints = imageCurve.getUserPoints();

        if (allPoints.size() != 0) {
            gc.setFill(Color.gray(1.0));
            gc.setStroke(Color.gray(0.5));
            gc.setLineWidth(2.0D);

            for (int i = 0; i < allPoints.size() - 1; ++i) {
                Vec2D point1 = imageToCanvas(allPoints.get(i).getPosition());
                Vec2D point2 = imageToCanvas(allPoints.get(i + 1).getPosition());

                gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
            }

            double pointSize = 6.0;

            gc.setLineWidth(1.0);
            gc.setStroke(Color.gray(0.0));
            if (drawSubdivisionMarkers) {
                for (Point point : allPoints) {
                    if (point.isSubdivisionPoint()) {
                        Vec2D pointPos = imageToCanvas(point.getPosition());
                        if (point.isSnapped()) {
                            gc.setFill(Color.gray(0.7));
                        } else {
                            gc.setFill(Color.ORANGERED);
                        }

                        gc.fillOval(pointPos.getX() - pointSize * 0.5,
                                pointPos.getY() - pointSize * 0.5, pointSize, pointSize);
                        gc.strokeOval(pointPos.getX() - pointSize * 0.5,
                                pointPos.getY() - pointSize * 0.5, pointSize, pointSize);
                    }
                }
            }

            gc.setFill(Color.LAWNGREEN);
            pointSize = 10.0;
            UUID selectedId = imageCurve.getSelectedId();
            UUID hoveredId = imageCurve.getHoveredId(ImageElement.Type.POINT);

            Vec2D highlightPosition = null;

            for (Point point : userPoints) {
                if (point.getId().equals(selectedId)) {
                    highlightPosition = point.getPosition();
                    continue;
                } else if (highlightPosition == null && point.getId().equals(hoveredId) &&
                        workMode.get() == MainController.WorkMode.POINTS) {
                    highlightPosition = point.getPosition();
                    continue;
                }

                Vec2D pointPos = imageToCanvas(point.getPosition());

                gc.setFill(Color.LAWNGREEN);
                gc.fillOval(pointPos.getX() - pointSize * 0.5,
                        pointPos.getY() - pointSize * 0.5, pointSize, pointSize);
                gc.strokeOval(pointPos.getX() - pointSize * 0.5,
                        pointPos.getY() - pointSize * 0.5, pointSize, pointSize);
            }

            if (highlightPosition != null) {
                if (selectedId != null) {
                    gc.setFill(Color.AQUAMARINE);
                } else {
                    gc.setFill(Color.WHITE);
                    if (deleteMode.get()) {
                        gc.setFill(Color.RED);
                    }
                }

                Vec2D pointPos = imageToCanvas(highlightPosition);

                gc.setLineWidth(1);
                gc.setStroke(Color.gray(0));
                gc.fillOval(pointPos.getX() - 5, pointPos.getY() - 5, 10, 10);
                gc.strokeOval(pointPos.getX() - 5, pointPos.getY() - 5, 10, 10);
            }
        }
    }

    private void extendCanvasLine(Vec2D point1, Vec2D point2, double margin) {
        double dx = point1.getX() - point2.getX();
        double dy = point1.getY() - point2.getY();
        double norm = Math.sqrt(dx * dx + dy * dy);
        if (!(norm < 1.0D)) {
            double extraShift = -dy * point1.getX() + dx * point1.getY();
            Vec2D regionTL = new Vec2D(margin, margin);
            Vec2D regionBR = new Vec2D(getWidth() - margin, getHeight() - margin);
            double righty;
            double lefty;
            if (Math.abs(dx) > 1 && Math.abs(dy) > 1) {
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
                if (Math.abs(dx) < 2 && Math.abs(dy) > 1) {
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
                } else if (Math.abs(dy) < 2 && Math.abs(dx) > 1) {
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
        }
    }

    private void drawTickLines() {
        gc.setLineWidth(2.0);
        HorizonSettings horizon = imageCurve.getHorizon();
        ArrayList<TickPoint> xTickPoints = imageCurve.getXticks();
        UUID selectedId = imageCurve.getSelectedId();
        UUID hoveredId = imageCurve.getHoveredId(ImageElement.Type.X_TICK | ImageElement.Type.Y_TICK);

        for (TickPoint xTick : xTickPoints) {
            gc.setStroke(Color.gray(0.0));

            if (workMode.get() == MainController.WorkMode.X_TICKS ||
                    workMode.get() == MainController.WorkMode.Y_TICKS) {
                if (xTick.getId().equals(selectedId)) {
                    gc.setStroke(Color.GREEN);
                } else if (xTick.getId().equals(hoveredId)) {
                    if (deleteMode.get()) {
                        gc.setStroke(Color.RED);
                    } else {
                        gc.setStroke(Color.LAWNGREEN);
                    }
                }
            }

            Vec2D point1 = imageToCanvas(xTick.getPosition());
            Vec2D point2 = new Vec2D(point1);
            point2.setX(point1.getX() + horizon.getVerticalDirection().getX() * 100);
            point2.setY(point1.getY() + horizon.getVerticalDirection().getY() * 100);

            extendCanvasLine(point1, point2, 10.0);
            gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
        }


        ArrayList<TickPoint> yTickPoints = imageCurve.getYticks();

        for (TickPoint yTick : yTickPoints) {
            gc.setStroke(Color.gray(0.0));

            if (workMode.get() == MainController.WorkMode.X_TICKS || workMode.get() == MainController.WorkMode.Y_TICKS) {
                if (yTick.getId().equals(selectedId)) {
                    gc.setStroke(Color.GREEN);
                } else if (yTick.getId().equals(hoveredId)) {
                    if (deleteMode.get()) {
                        gc.setStroke(Color.RED);
                    } else {
                        gc.setStroke(Color.LAWNGREEN);
                    }
                }
            }

            Vec2D point1 = imageToCanvas(yTick.getPosition());
            Vec2D point2 = new Vec2D(point1);
            point2.setX(point2.getX() + horizon.getHorizontalDirection().getX() * 100);
            point2.setY(point2.getY() + horizon.getHorizontalDirection().getY() * 100);
            extendCanvasLine(point1, point2, 10);

            gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
        }
    }

    private void drawTickValues() {
        gc.setFill(Color.gray(0.0D));

        gc.setFont(new Font(16.0D));
        ArrayList<TickPoint> xTickPoints = this.imageCurve.getXticks();
        ArrayList<TickPoint> yTickPoints = this.imageCurve.getYticks();

        for (TickPoint tick : xTickPoints) {
            Vec2D pos = imageToCanvas(tick.getPosition());
            gc.fillText(String.valueOf(tick.getTickValue()), pos.getX(), pos.getY());
        }

        for (TickPoint tick : yTickPoints) {
            Vec2D pos = imageToCanvas(tick.getPosition());
            gc.fillText(String.valueOf(tick.getTickValue()), pos.getX(), pos.getY());
        }
    }

    private void drawHorizon() {
        HorizonSettings horizon = imageCurve.getHorizon();

        if (!horizon.isValid() || workMode.get() != MainController.WorkMode.HORIZON) {
            return;
        }

        gc.setLineWidth(2.0);
        gc.setStroke(Color.gray(0.0));

        Vec2D origin = imageToCanvas(horizon.getOrigin().getPosition());
        Vec2D target = imageToCanvas(horizon.getTarget().getPosition());

        gc.strokeLine(origin.getX(), origin.getY(), target.getX(), target.getY());
        UUID selectedId = imageCurve.getSelectedId();
        UUID hoveredId = imageCurve.getHoveredId(ImageElement.Type.HORIZON);

        gc.setLineWidth(1.0D);
        gc.setStroke(Color.gray(0.0D));
        gc.setFill(Color.gray(0.7D));

        //draw origin point
        if (horizon.getOrigin().getId().equals(selectedId)) {
            gc.setFill(Color.AQUAMARINE);
        } else if (horizon.getOrigin().getId().equals(hoveredId)) {
            gc.setFill(Color.WHITE);
            //deleting origin will reset horizon
            if (deleteMode.get()) {
                gc.setFill(Color.RED);
            }
        }

        gc.fillOval(origin.getX() - 5, origin.getY() - 5, 10, 10);
        gc.strokeOval(origin.getX() - 5, origin.getY() - 5, 10, 10);
        gc.setFill(Color.gray(0.7));

        if (horizon.getTarget().getId().equals(selectedId)) {
            gc.setFill(Color.AQUAMARINE);
        } else if (horizon.getTarget().getId().equals(hoveredId)) {
            gc.setFill(Color.WHITE);
            if (deleteMode.get()) {
                gc.setFill(Color.RED);
            }
        }

        gc.fillOval(target.getX() - 5, target.getY() - 5, 10, 10);
        gc.strokeOval(target.getX() - 5, target.getY() - 5, 10, 10);
    }

    private void drawElements() {
        drawTickLines();
        drawHorizon();
        drawPoints();
        drawTickValues();
    }

    public void redrawCanvas() {
        redrawCanvas(true);
    }

    public void redrawCanvas(boolean sortPoints) {
        cleanCanvas();
        if (sortPoints) {
            //TODO maybe put somewhere else
            imageCurve.sortPoints();
        }

        drawElements();
    }

    public Vec2D canvasToImage(Vec2D canvasPosition) {
        return new Vec2D((canvasPosition.getX() - currentImageOffset.getX()) / currentImageScale,
                (canvasPosition.getY() - currentImageOffset.getY()) / currentImageScale);
    }

    public Vec2D imageToCanvas(Vec2D imagePosition) {
        return new Vec2D(imagePosition.getX() * currentImageScale + currentImageOffset.getX(),
                imagePosition.getY() * currentImageScale + currentImageOffset.getY());
    }

    public void zoomImage(Vec2D zoomPosition, double zoomFactor) {
        double maxZoom = 5.0D;
        Vec2D currentImagePoint = canvasToImage(zoomPosition);
        currentImageScale *= zoomFactor;
        if (currentImageScale > maxZoom) {
            currentImageScale = maxZoom;
        }

        Vec2D newCanvasPosition = imageToCanvas(currentImagePoint);
        currentImageOffset.setX(currentImageOffset.getX() - (newCanvasPosition.getX() - zoomPosition.getX()));
        currentImageOffset.setY(currentImageOffset.getY() - (newCanvasPosition.getY() - zoomPosition.getY()));
        if (currentImageScale < defaultImageScale) {
            fitImage(false);
        } else {
            checkImageOffset();
        }

        redrawCanvas(false);
    }

    private void checkImageOffset() {
        ImageWrapper image = imageCurve.getImage();
        if (image == null) {
            return;
        }
        Image img = image.getImage();
        if (currentImageOffset.getX() > defaultImageOffset.getX()) {
            currentImageOffset.setX(defaultImageOffset.getX());
        }

        if (currentImageOffset.getY() > defaultImageOffset.getY()) {
            currentImageOffset.setY(defaultImageOffset.getY());
        }

        if (currentImageOffset.getX() < getWidth() - defaultImageOffset.getX() - img.getWidth() * currentImageScale) {
            currentImageOffset.setX(getWidth() - defaultImageOffset.getX() - img.getWidth() * currentImageScale);
        }

        if (currentImageOffset.getY() < getHeight() - defaultImageOffset.getY() - img.getHeight() * currentImageScale) {
            currentImageOffset.setY(getHeight() - defaultImageOffset.getY() - img.getHeight() * currentImageScale);
        }
    }

    public void fitImage(boolean redraw) {
        currentImageScale = defaultImageScale;
        currentImageOffset.setX(defaultImageOffset.getX());
        currentImageOffset.setY(defaultImageOffset.getY());

        if (redraw)
            redrawCanvas(false);
    }

    public void panImage(double deltaX, double deltaY) {
        currentImageOffset.setX(currentImageOffset.getX() + deltaX);
        currentImageOffset.setY(currentImageOffset.getY() + deltaY);
        checkImageOffset();
        redrawCanvas(false);
    }

    public void updateImageTransform() {
        //called when image scale and offset have changed

        if (imageCurve.getImage() == null) {
            return;
        }

        Image img = imageCurve.getImage().getImage();

        double w = img.getWidth();
        double h = img.getHeight();
        double imgAspect = w / h;


        double canvasAspect = getWidth() / getHeight();
        if (imgAspect < canvasAspect) {
            w *= getHeight() / h;
            h = getHeight();
        } else {
            h *= getWidth() / w;
            w = getWidth();
        }

        defaultImageScale = w / img.getWidth();
        defaultImageOffset.setX((getWidth() - w) * 0.5);
        defaultImageOffset.setY((getHeight() - h) * 0.5);
    }
}
