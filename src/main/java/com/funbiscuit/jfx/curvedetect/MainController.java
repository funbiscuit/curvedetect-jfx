package com.funbiscuit.jfx.curvedetect;

import com.funbiscuit.jfx.curvedetect.model.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class MainController {
    private final Vec2D defaultImageOffset = new Vec2D(0, 0);
    private final Vec2D currentImageOffset = new Vec2D(0, 0);
    private final MenuItem openImageItem;
    private final MenuItem pointsItem;
    private final MenuItem horizonItem;
    private final MenuItem itemGrid;
    private final Vec2D mousePosition;
    private final Vec2D previousMousePosition;
    private final ImageCurve imageCurve;
    public Parent tickPopup;
    public TickPopupController tickPopupController;
    private ImageWrapper image;
    private MainController.WorkMode currentWorkMode;
    private GraphicsContext gc;
    //with this parameters image will be fit to canvas and centered
    private double defaultImageScale = 1;
    private double currentImageScale = 1;
    private ContextMenu contextMenu;
    private Stage tickDialog;
    private boolean deleteOnClick;
    private boolean ctrlPressed;
    private int subdivideIterations = 3;
    private boolean drawSubdivisionMarkers = true;
    private boolean showImage = true;
    private boolean isSaveReady;
    private boolean showBinarization;
    private String decimalSeparator = ".";
    private Stage stage;
    @FXML
    private Slider subdivisionSlider;
    @FXML
    private Label subdivisionLabel;
    @FXML
    private Label subdivisionValueLabel;
    @FXML
    private Slider binarizationSlider;
    @FXML
    private Label binarizationLabel;
    @FXML
    private Label binarizationValueLabel;
    @FXML
    private CheckBox drawSubMarkers;
    @FXML
    private CheckBox showImageToggle;
    @FXML
    private CheckBox showBinarizationToggle;
    @FXML
    private Button openImageButton;
    @FXML
    private Button resetAllButton;
    @FXML
    private Button copyToClipboardButton;
    @FXML
    private Button exportButton;
    @FXML
    private Label columnSeparatorLabel;
    @FXML
    private Label lineEndingLabel;
    @FXML
    private TextField columnSeparatorValueField;
    @FXML
    private TextField lineEndingValueField;
    @FXML
    private Label decimalSeparatorLabel;
    @FXML
    private Label tipsLabel;
    @FXML
    private VBox exportReadyVBox;
    @FXML
    private Label pointsReadyLabel;
    @FXML
    private Label xTicksReadyLabel;
    @FXML
    private Label yTicksReadyLabel;
    @FXML
    private ComboBox<String> decimalSeparatorComboBox;
    @FXML
    private CanvasPane mainCanvas;
    @FXML
    private ResourceBundle resources;

    public MainController() {
        currentWorkMode = MainController.WorkMode.NONE;
        openImageItem = new MenuItem("open");
        pointsItem = new MenuItem("points");
        horizonItem = new MenuItem("horizon");
        itemGrid = new MenuItem("grid");
        mousePosition = new Vec2D(0.0D, 0.0D);
        previousMousePosition = new Vec2D(0.0D, 0.0D);
        imageCurve = new ImageCurve();
    }

    public void setTickPopup(Parent tickPopup) {
        this.tickPopup = tickPopup;
    }

    public void setTickPopupController(TickPopupController tickPopupController) {
        this.tickPopupController = tickPopupController;
    }

    public void init(Stage stage) {
        this.stage = stage;

        redrawCanvas(false);

        //TODO use Stage(StageStyle.TRANSPARENT)
        tickDialog = new Stage();
        tickPopupController.init(tickDialog);
        tickDialog.setResizable(false);
        tickDialog.initModality(Modality.WINDOW_MODAL);
        tickDialog.initOwner(stage);
        tickDialog.setTitle(resources.getString("tickDialogTitle"));

        tickDialog.setScene(new Scene(tickPopup, 230, 140));
        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(openImageItem, pointsItem, horizonItem, itemGrid);

        openImageButton.addEventHandler(ActionEvent.ACTION, it -> openImage());

        copyToClipboardButton.addEventHandler(ActionEvent.ACTION, it -> copyPoints());

        exportButton.addEventHandler(ActionEvent.ACTION, it -> exportPoints());
        openImageItem.setOnAction((it -> openImage()));
        pointsItem.setOnAction((it -> {
            currentWorkMode = WorkMode.POINTS;
            updateControls();
        }));
        horizonItem.setOnAction((it -> {
            currentWorkMode = WorkMode.HORIZON;
            updateControls();
        }));
        itemGrid.setOnAction((it -> {
            currentWorkMode = imageCurve.isXgridReady() && !imageCurve.isYgridReady() ? WorkMode.Y_TICKS : WorkMode.X_TICKS;
            updateControls();
        }));
        decimalSeparatorComboBox.getItems().addAll(
                resources.getString("decimalSeparator.dot"),
                resources.getString("decimalSeparator.comma")
        );
        decimalSeparatorComboBox.getSelectionModel().select(0);
        addPropertyListeners();
        createEventHandlers();
        updateControls();
    }

    private void addPropertyListeners() {
        subdivideIterations = (int) subdivisionSlider.getValue();

        subdivisionSlider.valueProperty().addListener((o, old, newValue) -> {
            if (subdivideIterations != newValue.intValue()) {
                subdivideIterations = newValue.intValue();
                subdivisionValueLabel.setText(Integer.toString(subdivideIterations));
                subdivisionSlider.setValue(subdivideIterations);
                imageCurve.setSubdivision(subdivideIterations);
                redrawCanvas();
            }
        });

        binarizationSlider.valueProperty().addListener((o, old, newValue) -> {
            if (image != null && image.getThreshold() != newValue.intValue()) {
                image.setThreshold(newValue.intValue());
                image.updateBinarization(() -> Platform.runLater(this::redrawCanvas));
            }
            binarizationValueLabel.setText(Integer.toString(newValue.intValue()));
        });

        drawSubMarkers.selectedProperty().addListener((o, old, newValue) -> {
            drawSubdivisionMarkers = newValue;
            redrawCanvas(false);
        });

        showImageToggle.selectedProperty().addListener((o, old, newValue) -> {
            showImage = newValue;
            redrawCanvas(false);
        });

        showBinarizationToggle.selectedProperty().addListener((o, old, newValue) -> {
            showBinarization = newValue;
            redrawCanvas(false);
        });

        decimalSeparatorComboBox.getSelectionModel().selectedIndexProperty().addListener(
                (o, old, newValue) -> decimalSeparator = newValue.equals(1) ? "," : "."
        );
    }

    private void redrawCanvas() {
        redrawCanvas(true);
    }

    private void redrawCanvas(boolean sortPoints) {
        cleanCanvas();
        if (sortPoints) {
            this.imageCurve.sortPoints();
        }

        drawElements();
    }

    private void exportPoints() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mat", "*.mat"));
        chooser.setInitialDirectory(new File("."));

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            String s = file.toString();
            if (s.endsWith(".txt")) {
                exportToText(file);
            } else if (s.endsWith(".mat")) {
                exportToMatFile(file);
            } else {

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export fail");
                alert.setHeaderText("Unsupported");
                alert.setContentText("Unsupported description");
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.getStylesheets().add(this.getClass().getResource("modena_dark.css").toExternalForm());
                alert.showAndWait();
            }
        }
    }

    private void exportToMatFile(File file) {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            MatlabFileHelper mat = new MatlabFileHelper();
            mat.writeMatHeader(outputStream);
            this.imageCurve.sortPoints();
            int allPointsNum = this.imageCurve.getAllPointsCount();
            int userPointsNum = this.imageCurve.getUserPointsCount();
            double[] exportAllPoints = new double[allPointsNum * 2];
            double[] exportAllPointsImage = new double[allPointsNum * 2];
            double[] exportUserPoints = new double[userPointsNum * 2];
            double[] exportUserPointsImage = new double[userPointsNum * 2];
            this.imageCurve.writeRealPoints(exportAllPoints, exportUserPoints);
            this.imageCurve.writeImagePoints(exportAllPointsImage, exportUserPointsImage);
            mat.writeMatrix(outputStream, "AllPointsReal", exportAllPoints, allPointsNum, 2);
            mat.writeMatrix(outputStream, "AllPointsPixels", exportAllPointsImage, allPointsNum, 2);
            mat.writeMatrix(outputStream, "UserPointsReal", exportUserPoints, userPointsNum, 2);
            mat.writeMatrix(outputStream, "UserPointsPixels", exportUserPointsImage, userPointsNum, 2);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void exportToText(File file) {

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(getExportString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getExportString() {
        imageCurve.sortPoints();
        ArrayList<Vec2D> exportPoints = new ArrayList<>();
        imageCurve.writeRealPoints(exportPoints, null);
        StringBuilder builder = new StringBuilder();

        for (Vec2D point : exportPoints) {
            vec2DtoText(point, builder);
        }

        return builder.toString();
    }

    private void copyPoints() {
        if (isSaveReady) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(getExportString());
            clipboard.setContent(content);
        }
    }

    private void vec2DtoText(Vec2D point, StringBuilder builder) {
        if (!decimalSeparator.equals(".")) {
            String strValue = String.valueOf(point.getX()).replace(".", decimalSeparator);
            builder.append(strValue);
            builder.append("\t");
            strValue = String.valueOf(point.getY()).replace(".", decimalSeparator);
            builder.append(strValue);
        } else {
            builder.append(point.getX());
            builder.append("\t");
            builder.append(point.getY());
        }
        builder.append("\n");
    }

    private void showSaveReadyLabel(Label label, int position) {
        if (!label.isVisible()) {
            exportReadyVBox.getChildren().add(position, label);
            label.setVisible(true);
        }
    }

    private void hideSaveReadyLabel(Label label) {
        if (label.isVisible()) {
            label.setVisible(false);
            exportReadyVBox.getChildren().remove(label);
        }
    }

    private void updateSaveReady() {
        int insertPosition = 0;
        int exportReadyStatus = imageCurve.getExportStatus();
        isSaveReady = exportReadyStatus == 0;
        if ((exportReadyStatus & ImageCurve.ExportStatus.NO_IMAGE) == 0) {

            if ((exportReadyStatus & ImageCurve.ExportStatus.NO_POINTS) == 0 &&
                    (exportReadyStatus & ImageCurve.ExportStatus.ONE_POINT) == 0) {
                hideSaveReadyLabel(pointsReadyLabel);
            } else {
                showSaveReadyLabel(pointsReadyLabel, insertPosition);

                pointsReadyLabel.setText("no points");
                ++insertPosition;
            }

            if ((exportReadyStatus & ImageCurve.ExportStatus.NO_X_GRID_LINES) != 0) {
                showSaveReadyLabel(xTicksReadyLabel, insertPosition);

                xTicksReadyLabel.setText("no vertical");
                ++insertPosition;
            } else if ((exportReadyStatus & ImageCurve.ExportStatus.ONE_X_GRID_LINE) != 0) {
                showSaveReadyLabel(xTicksReadyLabel, insertPosition);

                xTicksReadyLabel.setText("one vertical");
                ++insertPosition;
            } else if ((exportReadyStatus & ImageCurve.ExportStatus.VALUE_OVERLAP_X_GRID) != 0) {
                showSaveReadyLabel(xTicksReadyLabel, insertPosition);

                xTicksReadyLabel.setText("vertical value overlap");
                ++insertPosition;
            } else if ((exportReadyStatus & ImageCurve.ExportStatus.PIXEL_OVERLAP_X_GRID) != 0) {
                showSaveReadyLabel(xTicksReadyLabel, insertPosition);

                xTicksReadyLabel.setText("vertical pixel overlap");
                ++insertPosition;
            } else {
                hideSaveReadyLabel(xTicksReadyLabel);
            }

            if ((exportReadyStatus & ImageCurve.ExportStatus.NO_Y_GRID_LINES) != 0) {
                showSaveReadyLabel(yTicksReadyLabel, insertPosition);

                yTicksReadyLabel.setText("no horizontal");
            } else if ((exportReadyStatus & ImageCurve.ExportStatus.ONE_Y_GRID_LINE) != 0) {
                showSaveReadyLabel(yTicksReadyLabel, insertPosition);

                yTicksReadyLabel.setText("one horiz");
            } else if ((exportReadyStatus & ImageCurve.ExportStatus.VALUE_OVERLAP_Y_GRID) != 0) {
                showSaveReadyLabel(yTicksReadyLabel, insertPosition);

                yTicksReadyLabel.setText("value horiz overlap");
            } else if ((exportReadyStatus & ImageCurve.ExportStatus.PIXEL_OVERLAP_Y_GRID) != 0) {
                showSaveReadyLabel(yTicksReadyLabel, insertPosition);

                yTicksReadyLabel.setText("horiz pixel overlap");
            } else {
                hideSaveReadyLabel(yTicksReadyLabel);
            }
        } else {
            hideSaveReadyLabel(pointsReadyLabel);
            hideSaveReadyLabel(xTicksReadyLabel);
            hideSaveReadyLabel(yTicksReadyLabel);
        }
    }

    private void handleTickInput() {

        if (tickPopupController.getWasCanceled()) {
            imageCurve.resetSelectedTick();
        } else {
            if (tickPopupController.getDeleteTick()) {
                imageCurve.deleteSelectedTick();
            } else {
                imageCurve.makeTickInput(tickPopupController.getTickValue());
            }
        }

        if (imageCurve.isXgridReady() && !imageCurve.isYgridReady()) {
            currentWorkMode = MainController.WorkMode.Y_TICKS;
        } else if (imageCurve.isYgridReady() && !imageCurve.isXgridReady()) {
            currentWorkMode = MainController.WorkMode.X_TICKS;
        }

        ctrlPressed = false;
        Vec2D imagePos = canvasToImage(mousePosition);
        imageCurve.updateHoveredElement(imagePos.getX(), imagePos.getY());
        redrawCanvas(false);
        updateControls();
    }

    private Vec2D canvasToImage(Vec2D canvasPosition) {
        return new Vec2D((canvasPosition.getX() - currentImageOffset.getX()) / currentImageScale,
                (canvasPosition.getY() - currentImageOffset.getY()) / currentImageScale);
    }

    private Vec2D imageToCanvas(Vec2D imagePosition) {
        return new Vec2D(imagePosition.getX() * currentImageScale + currentImageOffset.getX(),
                imagePosition.getY() * currentImageScale + currentImageOffset.getY());
    }

    private void showTickInput() {
        TickPoint selected = imageCurve.getSelectedTick();

        if (selected == null) {
            this.redrawCanvas(false);
            return;
        }

        tickPopupController.setTickValue(selected.getTickValue(), selected.isNew());

        tickDialog.show();
        tickDialog.setX(stage.getX() + stage.getWidth() * 0.5 - tickDialog.getWidth() * 0.5);
        tickDialog.setY(stage.getY() + stage.getHeight() * 0.5 - tickDialog.getHeight() * 0.5);
    }

    private void createEventHandlers() {
        tickDialog.setOnHiding(it -> handleTickInput());

        stage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.CONTROL) {
                onCtrlInput(false);
            } else if (e.getCode() == KeyCode.SHIFT) {
                onShiftInput(false);
            } else {
                return;
            }

            e.consume();
            redrawCanvas(false);
        });

        stage.addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.CONTROL) {
                onCtrlInput(true);
            } else if (e.getCode() == KeyCode.SHIFT) {
                onShiftInput(true);
            } else {
                return;
            }

            e.consume();
            redrawCanvas(false);
        });
        mainCanvas.setOnScroll(it -> {
            double zoomFactor = 1.3;
            if (ctrlPressed) {
                zoomFactor = 1.15;
            }

            if (it.getDeltaY() < (double) 0) {
                zoomFactor = 2.0 - zoomFactor;
            }

            zoomImage(zoomFactor);
        });

        mainCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            mousePosition.setX(e.getX());
            mousePosition.setY(e.getY());

            onMouseDrag(e.getButton());

            previousMousePosition.setX(e.getX());
            previousMousePosition.setY(e.getY());
        });

        mainCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            mousePosition.setX(e.getX());
            mousePosition.setY(e.getY());

            onMouseMove();

            previousMousePosition.setX(e.getX());
            previousMousePosition.setY(e.getY());
        });

        mainCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            mousePosition.setX(e.getX());
            mousePosition.setY(e.getY());
            previousMousePosition.setX(e.getX());
            previousMousePosition.setY(e.getY());

            if (onMousePress(e.getButton())) {
                e.consume();
            }
        });

        mainCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, (e -> {
            mousePosition.setX(e.getX());
            mousePosition.setY(e.getY());

            onMouseRelease(e);

            previousMousePosition.setX(e.getX());
            previousMousePosition.setY(e.getY());
        }));
    }

    private void zoomImage(double zoomFactor) {
        double maxZoom = 5.0D;
        Vec2D currentImagePoint = canvasToImage(mousePosition);
        currentImageScale *= zoomFactor;
        if (currentImageScale > maxZoom) {
            currentImageScale = maxZoom;
        }

        Vec2D newCanvasPosition = imageToCanvas(currentImagePoint);
        currentImageOffset.setX(currentImageOffset.getX() - (newCanvasPosition.getX() - mousePosition.getX()));
        currentImageOffset.setY(currentImageOffset.getY() - (newCanvasPosition.getY() - mousePosition.getY()));
        if (currentImageScale < defaultImageScale) {
            fitImage(false);
        } else {
            checkImageOffset();
        }

        redrawCanvas(false);
    }

    private void checkImageOffset() {
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

        if (currentImageOffset.getX() < mainCanvas.getWidth() - defaultImageOffset.getX() - img.getWidth() * currentImageScale) {
            currentImageOffset.setX(mainCanvas.getWidth() - defaultImageOffset.getX() - img.getWidth() * currentImageScale);
        }

        if (currentImageOffset.getY() < mainCanvas.getHeight() - defaultImageOffset.getY() - img.getHeight() * currentImageScale) {
            currentImageOffset.setY(mainCanvas.getHeight() - defaultImageOffset.getY() - img.getHeight() * currentImageScale);
        }
    }

    private void fitImage(boolean redraw) {
        currentImageScale = defaultImageScale;
        currentImageOffset.setX(defaultImageOffset.getX());
        currentImageOffset.setY(defaultImageOffset.getY());

        if (redraw)
            redrawCanvas(false);
    }

    private void panImage(double deltaX, double deltaY) {
        currentImageOffset.setX(currentImageOffset.getX() + deltaX);
        currentImageOffset.setY(currentImageOffset.getY() + deltaY);
        checkImageOffset();
        redrawCanvas(false);
    }

    private void onCtrlInput(boolean released) {
        if (released) {
            ctrlPressed = false;
            Vec2D imagePos = this.canvasToImage(mousePosition);
            imageCurve.unsnapSelected(imagePos.getX(), imagePos.getY());
        } else {
            ctrlPressed = true;
            imageCurve.snapSelected();
        }
        redrawCanvas();
        updateControls();
    }

    private void onShiftInput(boolean released) {
        if (released) {
            deleteOnClick = false;
        } else if (imageCurve.getSelectedElement() == null) {
            deleteOnClick = true;
        }

        updateControls();
    }

    private void onMouseDrag(MouseButton button) {
        if (button == MouseButton.PRIMARY) {
            switch (currentWorkMode) {
                case POINTS:
                case X_TICKS:
                case Y_TICKS:
                case HORIZON:
                    Vec2D imagePos = canvasToImage(mousePosition);
                    imageCurve.dragSelected(imagePos.getX(), imagePos.getY());

                    deleteOnClick = false;
                    if (ctrlPressed) {
                        imageCurve.snapSelected();
                    }
                    if (currentWorkMode == WorkMode.POINTS ||
                            currentWorkMode == WorkMode.HORIZON) {
                        imageCurve.sortPoints();
                    }
                    break;
            }
            updateControls();
            redrawCanvas(false);
        } else if (button == MouseButton.MIDDLE) {
            panImage(mousePosition.getX() - previousMousePosition.getX(),
                    mousePosition.getY() - previousMousePosition.getY());
        }
    }

    private boolean onMousePress(MouseButton button) {
        boolean consumed = false;

        if (button == MouseButton.PRIMARY) {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            } else {
                Vec2D imagePos = canvasToImage(mousePosition);

                imageCurve.updateHoveredElement(imagePos.getX(), imagePos.getY());
                switch (currentWorkMode) {
                    case NONE:
                        break;
                    case POINTS:
                        if (ctrlPressed) {
                            imageCurve.addPoint(imagePos.getX(), imagePos.getY());
                        } else if (imageCurve.selectHovered(ImageElement.Type.POINT)) {
                            imageCurve.dragSelected(imagePos.getX(), imagePos.getY());
                        }
                        break;
                    case X_TICKS:
                        if (ctrlPressed) {
                            imageCurve.addXtick(imagePos.getX(), imagePos.getY());
                        } else if (imageCurve.selectHovered(ImageElement.Type.X_TICK | ImageElement.Type.Y_TICK)) {
                            imageCurve.backupSelectedTick();

//                            selectedXtick?.x=e.x
//                            selectedXtick?.y=e.y
                        }
                        break;
                    case Y_TICKS:
                        if (ctrlPressed) {
                            imageCurve.addYtick(imagePos.getX(), imagePos.getY());
                        } else if (imageCurve.selectHovered(ImageElement.Type.X_TICK | ImageElement.Type.Y_TICK)) {
                            imageCurve.backupSelectedTick();
//                            selectedYtick?.x=e.x
//                            selectedYtick?.y=e.y
                        }
                        break;
                    case HORIZON:
                        if (imageCurve.selectHovered(ImageElement.Type.HORIZON)) {
                            imageCurve.dragSelected(imagePos.getX(), imagePos.getY());
                            imageCurve.sortPoints();
                        }
                        break;

                }
                redrawCanvas(false);
            }

            consumed = true;
        } else if (button == MouseButton.SECONDARY) {
            Point2D screenPoint = mainCanvas.localToScreen(
                    mousePosition.getX(), mousePosition.getY());
            if (screenPoint != null) {
                showContextMenu(screenPoint.getX(), screenPoint.getY());
                consumed = true;
            }
        }
        updateControls();

        return consumed;
    }

    private void showContextMenu(double x, double y) {
        openImageItem.setVisible(image == null);
        pointsItem.setDisable(image == null || currentWorkMode == MainController.WorkMode.POINTS);
        itemGrid.setDisable(image == null || currentWorkMode == MainController.WorkMode.X_TICKS || currentWorkMode == MainController.WorkMode.Y_TICKS);
        horizonItem.setDisable(image == null || currentWorkMode == MainController.WorkMode.HORIZON);


        contextMenu.show(mainCanvas, x, y);
    }

    private void onMouseRelease(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
//            updateHoveredElement(e.x,e.y)
            switch (currentWorkMode) {
                case X_TICKS:
                case Y_TICKS:
                    if (deleteOnClick) {
                        imageCurve.deleteSelected();
                    } else if (imageCurve.getSelectedTick() != null) {
                        showTickInput();
                    }
                    break;
                case POINTS:
                case HORIZON:
                    if (deleteOnClick) {
                        //by using delete selected we can make sure that horizon
                        // will be reset only if it is selected
                        imageCurve.deleteSelected();
                    } else
                        imageCurve.deselectAll();
                    break;
            }

            deleteOnClick = e.isShiftDown();
        }

        updateControls();
        redrawCanvas(false);
    }

    private void onMouseMove() {
        Vec2D imagePos = canvasToImage(mousePosition);
        imageCurve.updateHoveredElement(imagePos.getX(), imagePos.getY());
        this.redrawCanvas(false);
    }

    private void openImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("pictures", "*.jpg", "*.png", "*.bmp"));
        chooser.setInitialDirectory(new File("."));

        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        image = new ImageWrapper(file.toPath());
        image.setThreshold((int) binarizationSlider.getValue());
        imageCurve.setImage(image);
        imageCurve.resetPoints();
        updateImageTransform();
        currentWorkMode = MainController.WorkMode.POINTS;
        imageCurve.resetHorizon();
        fitImage(false);
        redrawCanvas(false);
        updateControls();
    }

    private void updateControls() {
        //check what controls are available and what should be disabled

        updateSaveReady();

        copyToClipboardButton.setDisable(!isSaveReady);
        exportButton.setDisable(!isSaveReady);

        //TODO update tips
    }

    private void cleanCanvas() {
        gc.setFill(Color.gray(0.17));
        gc.fillRect(0.0D, 0.0D, mainCanvas.getWidth(), mainCanvas.getHeight());

        drawImage();
    }

    private void updateImageTransform() {
        //called when image scale and offset have changed

        if (image == null) {
            return;
        }

        Image img = image.getImage();

        double w = img.getWidth();
        double h = img.getHeight();
        double imgAspect = w / h;


        double canvasAspect = mainCanvas.getWidth() / mainCanvas.getHeight();
        if (imgAspect < canvasAspect) {
            w *= mainCanvas.getHeight() / h;
            h = mainCanvas.getHeight();
        } else {
            h *= mainCanvas.getWidth() / w;
            w = mainCanvas.getWidth();
        }

        defaultImageScale = w / img.getWidth();
        defaultImageOffset.setX((mainCanvas.getWidth() - w) * 0.5);
        defaultImageOffset.setY((mainCanvas.getHeight() - h) * 0.5);
    }

    private void drawImage() {
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
                Vec2D point1 = imageToCanvas(((ImageElement) allPoints.get(i)).getImagePos());
                Vec2D point2 = imageToCanvas(((ImageElement) allPoints.get(i + 1)).getImagePos());

                gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
            }

            double pointSize = 6.0;

            gc.setLineWidth(1.0);
            gc.setStroke(Color.gray(0.0));
            if (drawSubdivisionMarkers) {
                for (Point point : allPoints) {
                    if (point.isSubdivisionPoint()) {
                        Vec2D pointPos = this.imageToCanvas(point.getImagePos());
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
            ImageElement selectedPoint = imageCurve.getSelectedElement();
            ImageElement hoveredPoint = imageCurve.getHoveredElement(ImageElement.Type.POINT);


            for (ImageElement point : userPoints) {
                if (selectedPoint != null && point.getId().equals(selectedPoint.getId()))
                    continue;

                Vec2D pointPos = imageToCanvas(point.getImagePos());

                gc.setFill(Color.LAWNGREEN);
                gc.fillOval(pointPos.getX() - pointSize * 0.5,
                        pointPos.getY() - pointSize * 0.5, pointSize, pointSize);
                gc.strokeOval(pointPos.getX() - pointSize * 0.5,
                        pointPos.getY() - pointSize * 0.5, pointSize, pointSize);
            }

            ImageElement point = selectedPoint;

            gc.setFill(Color.AQUAMARINE);
            if (selectedPoint == null && currentWorkMode == MainController.WorkMode.POINTS) {
                point = hoveredPoint;

                gc.setFill(Color.WHITE);
                if (deleteOnClick) {
                    gc.setFill(Color.RED);
                }
            }

            if (point != null) {
                Vec2D pointPos = imageToCanvas(point.getImagePos());

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
            Vec2D regionBR = new Vec2D(mainCanvas.getWidth() - margin, mainCanvas.getHeight() - margin);
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
        TickPoint selected = imageCurve.getSelectedTick();
        ImageElement hovered = imageCurve.getHoveredElement(6);

        for (TickPoint xTick : xTickPoints) {
            gc.setStroke(Color.gray(0.0));

            if (currentWorkMode == MainController.WorkMode.X_TICKS ||
                    currentWorkMode == MainController.WorkMode.Y_TICKS) {
                if (selected != null && xTick.getId().equals(selected.getId())) {
                    gc.setStroke(Color.GREEN);
                } else if (hovered != null && xTick.getId().equals(hovered.getId())) {
                    if (deleteOnClick) {
                        gc.setStroke(Color.RED);
                    } else {
                        gc.setStroke(Color.LAWNGREEN);
                    }
                }
            }

            Vec2D point1 = imageToCanvas(xTick.getImagePos());
            Vec2D point2 = new Vec2D(point1);
            point2.setX(point1.getX() + horizon.getVerticalDirection().getX() * 100);
            point2.setY(point1.getY() + horizon.getVerticalDirection().getY() * 100);

            extendCanvasLine(point1, point2, 10.0);
            gc.strokeLine(point1.getX(), point1.getY(), point2.getX(), point2.getY());
        }


        ArrayList<TickPoint> yTickPoints = imageCurve.getYticks();

        for (TickPoint yTick : yTickPoints) {
            gc.setStroke(Color.gray(0.0));

            if (currentWorkMode == MainController.WorkMode.X_TICKS || currentWorkMode == MainController.WorkMode.Y_TICKS) {
                if (selected != null && yTick.getId().equals(selected.getId())) {
                    gc.setStroke(Color.GREEN);
                } else if (hovered != null && yTick.getId().equals(hovered.getId())) {
                    if (deleteOnClick) {
                        gc.setStroke(Color.RED);
                    } else {
                        gc.setStroke(Color.LAWNGREEN);
                    }
                }
            }

            Vec2D point1 = imageToCanvas(yTick.getImagePos());
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
            Vec2D pos = imageToCanvas(tick.getImagePos());
            gc.fillText(String.valueOf(tick.getTickValue()), pos.getX(), pos.getY());
        }

        for (TickPoint tick : yTickPoints) {
            Vec2D pos = imageToCanvas(tick.getImagePos());
            gc.fillText(String.valueOf(tick.getTickValue()), pos.getX(), pos.getY());
        }
    }

    private void drawHorizon() {
        HorizonSettings horizon = imageCurve.getHorizon();

        if (!horizon.isValid() || currentWorkMode != MainController.WorkMode.HORIZON) {
            return;
        }

        gc.setLineWidth(2.0);
        gc.setStroke(Color.gray(0.0));

        Vec2D origin = imageToCanvas(horizon.getImagePos());
        Vec2D target = imageToCanvas(horizon.getTarget().getImagePos());

        gc.strokeLine(origin.getX(), origin.getY(), target.getX(), target.getY());
        ImageElement selected = imageCurve.getSelectedElement();
        ImageElement hovered = imageCurve.getHoveredElement(ImageElement.Type.HORIZON);

        gc.setLineWidth(1.0D);
        gc.setStroke(Color.gray(0.0D));
        gc.setFill(Color.gray(0.7D));

        //draw origin point
        if (selected != null && horizon.getId().equals(selected.getId())) {
            gc.setFill(Color.AQUAMARINE);
        } else if (hovered != null && horizon.getId().equals(hovered.getId())) {
            gc.setFill(Color.WHITE);
            //deleting origin will reset horizon
            if (deleteOnClick) {
                gc.setFill(Color.RED);
            }
        }

        gc.fillOval(origin.getX() - 5, origin.getY() - 5, 10, 10);
        gc.strokeOval(origin.getX() - 5, origin.getY() - 5, 10, 10);
        gc.setFill(Color.gray(0.7));

        if (selected != null && horizon.getTarget().getId().equals(selected.getId())) {
            gc.setFill(Color.AQUAMARINE);
        } else if (hovered != null && horizon.getTarget().getId().equals(hovered.getId())) {
            gc.setFill(Color.WHITE);
            if (this.deleteOnClick) {
                gc.setFill(Color.RED);
            }
        }

        gc.fillOval(target.getX() - 5, target.getY() - 5, 10, 10);
        gc.strokeOval(target.getX() - 5, target.getY() - 5, 10, 10);
    }

    private void drawElements() {
        this.drawTickLines();
        this.drawHorizon();
        this.drawPoints();
        this.drawTickValues();
    }

    public void initialize() {
        gc = mainCanvas.getCanvas().getGraphicsContext2D();

        mainCanvas.setOnResizeListener(() -> {
            updateImageTransform();
            redrawCanvas(false);
        });
    }

    public enum WorkMode {
        NONE,
        POINTS,
        X_TICKS,
        Y_TICKS,
        HORIZON;
    }
}
