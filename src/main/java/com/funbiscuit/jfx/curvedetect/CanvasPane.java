package com.funbiscuit.jfx.curvedetect;


import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

public final class CanvasPane extends Pane {

    private final Canvas canvas = new Canvas(100.0d, 100.0d);
    private OnResizeListener resizeListener;

    public CanvasPane() {
        getChildren().add(canvas);
    }

    public final Canvas getCanvas() {
        return canvas;
    }

    public final void setOnResizeListener(OnResizeListener listener) {
        resizeListener = listener;
    }

    protected void layoutChildren() {
        super.layoutChildren();
        double x = snappedLeftInset();
        double y = snappedTopInset();
        // Java 9 - snapSize is depricated used snapSizeX() and snapSizeY() accordingly
        double w = snapSize(getWidth()) - x - snappedRightInset();
        double h = snapSize(getHeight()) - y - snappedBottomInset();

        canvas.setLayoutX(x);
        canvas.setLayoutY(y);
        canvas.setWidth(w);
        canvas.setHeight(h);

        if (resizeListener != null) {
            resizeListener.onResize();
        }
    }

    public interface OnResizeListener {
        void onResize();
    }
}
