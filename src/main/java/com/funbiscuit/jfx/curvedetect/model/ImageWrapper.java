package com.funbiscuit.jfx.curvedetect.model;

import com.funbiscuit.jfx.curvedetect.ImageElement;
import javafx.scene.image.*;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageWrapper {
    /**
     * Format in which pixels are stored inside imagePixels and bwPixels arrays
     */
    private final WritablePixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();

    /**
     * Flag that indicates that new binarization is calculating
     */
    private boolean isBinarizationCalculating;

    /**
     * Indicates that binarization must be recalculated.
     * Used as a flag for background thread to continue recalculating
     */
    private boolean isRecalculationNeeded;

    @Getter
    private Image image;

    /**
     * Image which stores binarization version of current image
     */
    @Getter
    private WritableImage bwImage;

    /**
     * Original pixels of image (stored as 0xAARRGGBB int values)
     */
    private int[] imagePixels = new int[0];

    /**
     * black-white pixels of image with current binarization threshold
     * 0x00 - black, 0xFFFFFFFF - white
     */
    private int[] bwPixels = new int[0];

    /**
     * Threshold to calculate binarization.
     * If pixel luminosity (0..255) is greater than this value, pixel is
     * considered white, otherwise it is considered black.
     */
    @Getter
    @Setter
    private int threshold = 127;

    public ImageWrapper(Path path) {
        System.out.println(path);
        try (InputStream stream = Files.newInputStream(path)) {
            init(new Image(stream));
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't load image: " + path);
        }
    }

    private void init(Image image) {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0)
            throw new IllegalArgumentException("Image can't be empty");

        this.image = image;
        bwImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());

        updateImagePixels();
        updateBinarization();
    }

    /**
     * Initializes imagePixels array to actual pixels of image.
     * Resizes bwPixels to size of imagePixels
     * Called only in constructor
     */
    private void updateImagePixels() {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        imagePixels = new int[width * height];
        bwPixels = new int[width * height];

        image.getPixelReader().getPixels(0, 0, width, height,
                pixelFormat, imagePixels, 0, width);
    }

    /**
     * Update binarization (both bwImage and bwPixels) in the background thread
     * with current value of threshold
     * @param callback - runnable to run after binarization is complete (can be null)
     */
    public void updateBinarization(Runnable callback) {
        synchronized (this) {
            // do not start calculation if we're already calculating
            // just set flag that we need to update binarization again for new threshold
            if (isBinarizationCalculating) {
                isRecalculationNeeded = true;
                return;
            }
            isBinarizationCalculating = true;
        }

        double thresholdValue = threshold;

        new Thread(() -> {
            while (true) {
                isRecalculationNeeded = false;
                PixelWriter pixelWriter = bwImage.getPixelWriter();

                for (int i = 0; i < imagePixels.length; i++) {
                    int pixelRgb = imagePixels[i];

                    // coefficients from formula for luminance (E'y) in Rec.ITU-R BT.601-7
                    double gray = (pixelRgb >> 16 & 0xFF) * 0.299 +
                            (pixelRgb >> 8 & 0xFF) * 0.587 +
                            (pixelRgb & 0xFF) * 0.114;

                    if (gray > thresholdValue)
                        bwPixels[i] = 0xFFFFFFFF;   // white
                    else
                        bwPixels[i] = 0x00000000;   // black
                }

                pixelWriter.setPixels(0, 0, (int) image.getWidth(), (int) image.getHeight(),
                        pixelFormat, bwPixels, 0, (int) image.getWidth());

                if (callback != null)
                    callback.run();

                synchronized (ImageWrapper.this) {
                    // threshold changed and we need to recalculate binarization
                    if (isRecalculationNeeded)
                        continue;
                    isBinarizationCalculating = false;
                    break;
                }
            }
        }).start();
    }

    public void updateBinarization() {
        updateBinarization(null);
    }

    /**
     * Performs snapping of provided point to nearest black pixel
     * and then performs snapping to barycenter of nearby region
     * @param point to snap
     * @return true if point was snapped or false if no black pixel was found nearby
     */
    public boolean snap(ImageElement point) {
        return snapToCurve(point) && snapToBary(point);
    }

    /**
     * Snaps given point to nearest black pixel (changes its coordinates)
     * @param point to snap
     * @return true if point was snapped or false if no black pixel was found nearby
     */
    private boolean snapToCurve(ImageElement point) {
        // rectangular region that defines region of snapping
        int halfSide = 10;
        int side = halfSide * 2 + 1;

        // do not snap on small images
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        if (width < side * 2 || height < side * 2)
            return false;

        Vec2D pos = point.getImagePos();
        int px = (int) pos.getX();
        int py = (int) pos.getY();


        int minDist = side * side;
        int closestX = -1;
        int closestY = -1;

        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                int globalRow = py - halfSide + row;
                int globalColumn = px - halfSide + column;

                boolean isBlack = bwPixels[globalRow * width + globalColumn] == 0;
                if (!isBlack)
                    continue;

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

        if (closestX == -1)
            return false;

        point.setImagePos(px + closestX - halfSide, py + closestY - halfSide);
        return true;
    }

    /**
     * Snaps given point to barycenter of nearby region
     * @param point to snap
     * @return true if snap was successful, or false if there are no black pixels nearby
     */
    private boolean snapToBary(ImageElement point) {
        // smaller region is used for barycenter calculation
        int halfSide = 4;
        int side = halfSide * 2 + 1;

        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        // don't snap on small images
        if (width < side * 2 || height < side * 2)
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
                boolean isBlack = bwPixels[globalRow * width + globalColumn] == 0;

                //TODO use grayscale
                if (isBlack) {
                    baryMass += 1;//threshold-gray
                    closestX += (column - halfSide);//*(threshold-gray)
                    closestY += (row - halfSide);//*(threshold-gray)
                }
            }
        }

        if (baryMass <= 0)
            return false;

        point.setImagePos(px + closestX / baryMass, py + closestY / baryMass);

        return true;
    }
}
