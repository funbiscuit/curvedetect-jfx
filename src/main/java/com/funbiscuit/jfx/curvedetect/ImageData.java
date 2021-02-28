package com.funbiscuit.jfx.curvedetect;

import javafx.application.Platform;
import javafx.scene.image.*;

import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageData {
    private Image image;
    private WritableImage imageBW;
    private int[] originalPixels = new int[0];
    private int[] transientPixels = new int[0];
    private int threshold = 127;
    private final AtomicBoolean isBinarizationCalculating = new AtomicBoolean(false);
    private boolean isRecalculationNeeded;

    public Image getImage() {
        return image;
    }

    public WritableImage getImageBW() {
        return imageBW;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void loadImage(File file) {
        image = new Image("file:///" + file.getPath());
        updateImagePixels();
        updateBinarization(null);
    }

    private void updateImagePixels() {
        if (image == null) {
            return;
        }
        PixelReader reader = image.getPixelReader();
        if (reader != null) {
            int imw = (int) image.getWidth();
            int imh = (int) image.getHeight();

            if (originalPixels.length != imw * imh) {
                originalPixels = new int[imw * imh];
            }

            if (transientPixels.length != imw * imh) {
                transientPixels = new int[imw * imh];
            }

            reader.getPixels(0, 0, imw, imh,
                    getPixelFormat(image), originalPixels, 0, imw);
        }
    }

    private WritablePixelFormat<IntBuffer> getPixelFormat(Image img) {
        if (img.getPixelReader().getPixelFormat().getType() == PixelFormat.Type.INT_ARGB_PRE) {
            return PixelFormat.getIntArgbPreInstance();
        } else {
            return PixelFormat.getIntArgbInstance();
        }
    }

    public void updateBinarization(Runnable callback) {
        if (image == null) {
            return;
        }

        //only create new image if size has changed (or image is null)
        if (imageBW == null || imageBW.getWidth() != image.getWidth() ||
                imageBW.getHeight() != imageBW.getHeight())
            imageBW = new WritableImage((int) image.getWidth(), (int) image.getHeight());

        // do not start new thread if we're already calculating
        if (isBinarizationCalculating.get()) {
            isRecalculationNeeded = true;
            return;
        }

        isBinarizationCalculating.set(true);
        Thread t = new Thread(() -> {
            PixelWriter wr = imageBW.getPixelWriter();
            double gray;
            int counter = 0;
            int maxCounter = originalPixels.length / 100;

            for (int k = 0; k < originalPixels.length; k++) {
                ++counter;

//        *     int pixel = array[rowstart + x];
//        *
//        *     int alpha = ((pixel >> 24) & 0xff);
//        *     int red   = ((pixel >> 16) & 0xff);
//        *     int green = ((pixel >>  8) & 0xff);
//        *     int blue  = ((pixel      ) & 0xff);


                int currentPixelx = originalPixels[k];
                gray = (currentPixelx >> 16 & 0xFF) * 0.3 +
                        (currentPixelx >> 8 & 0xFF) * 0.6 +
                        (currentPixelx & 0xFF) * 0.1;
                if (counter > maxCounter) {
                    counter = 0;
                }

                if (gray < (double) getThreshold()) {
                    transientPixels[k] = 0;//black
                } else {
                    transientPixels[k] = -1;//white
                }
            }

            wr.setPixels(0, 0, (int) image.getWidth(), (int) image.getHeight(),
                    getPixelFormat(image), transientPixels, 0, (int) image.getWidth());
            isBinarizationCalculating.set(false);
            if (isRecalculationNeeded) {
                isRecalculationNeeded = false;
                updateBinarization(callback);
            }

            if (callback != null)
                Platform.runLater(callback);
        });
        t.start();
    }

    public boolean snapToCurve(ImageElement point) {
        if (image == null) {
            return false;
        }

        int halfSide = 10;
        int side = halfSide * 2 + 1;

        //don't use super small images
        if (image.getWidth() < side * 2 || image.getHeight() < side * 2)
            return false;

        Vec2D pos = point.getImagePos();
        int px = (int) pos.getX();
        int py = (int) pos.getY();

//        *     int pixel = array[rowstart + x];
//        *
//        *     int alpha = ((pixel >> 24) & 0xff);
//        *     int red   = ((pixel >> 16) & 0xff);
//        *     int green = ((pixel >>  8) & 0xff);
//        *     int blue  = ((pixel      ) & 0xff);

        int minDist = side * side;
        int closestX = -1;
        int closestY = -1;

        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                int globalRow = py - halfSide + row;
                int globalColumn = px - halfSide + column;

                boolean isBlack = this.transientPixels[globalRow * (int) image.getWidth() + globalColumn] == 0;
                if (isBlack) {
                    int dx = halfSide - column;
                    int dy = halfSide - row;
                    int dist = dx * dx + dy * dy;
                    if (dist < minDist) {
                        minDist = dist;
                        closestX = column;
                        closestY = row;
                    }
                }
            }
        }

        if (closestX == -1) {
            return false;
        }

        point.setImagePos((px + closestX - halfSide), (py + closestY - halfSide));
        return true;
    }

    public boolean snapToBary(ImageElement point) {
        if (image == null) {
            return false;
        }

        int halfSide = 4; //smaller region is used for barycenter search
        int side = halfSide * 2 + 1;

        //don't use super small images
        if (image.getWidth() < side * 2 || image.getHeight() < side * 2)
            return false;

        Vec2D pos = point.getImagePos();
        int px = (int) pos.getX();
        int py = (int) pos.getY();

        double closestX = 0;
        double closestY = 0;
        double baryMass = 0;

        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                int globalRow = py - halfSide + row;
                int globalColumn = px - halfSide + column;
                boolean isBlack = transientPixels[globalRow * (int) image.getWidth() + globalColumn] == 0;

//                if(gray<threshold)
                //TODO check if gray
                if (isBlack) {
                    baryMass += 1;//threshold-gray
                    closestX += (column - halfSide);//*(threshold-gray)
                    closestY += (row - halfSide);//*(threshold-gray)
                }
            }
        }

        if (baryMass <= 0) {
            return false;
        }

        point.setImagePos((double) px + closestX / baryMass,
                (double) py + closestY / baryMass);

        return true;
    }
}
