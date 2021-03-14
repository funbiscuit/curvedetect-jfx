package com.funbiscuit.jfx.curvedetect;

import com.funbiscuit.jfx.curvedetect.model.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class MainController {
    private final MenuItem openImageItem;
    private final MenuItem pointsItem;
    private final MenuItem horizonItem;
    private final MenuItem itemGrid;
    private final Vec2D mousePosition;
    private final Vec2D previousMousePosition;
    private final ImageCurve imageCurve;
    private final ObjectProperty<WorkMode> workMode = new SimpleObjectProperty<>();
    private final BooleanProperty deleteMode = new SimpleBooleanProperty(false);
    public TickPopupController tickPopupController;
    private ImageWrapper image;
    private ContextMenu contextMenu;
    private Stage tickDialog;
    private boolean ctrlPressed;
    private int subdivideIterations = 3;
    private boolean isSaveReady;
    private String decimalSeparator = ".";


    private final ObjectProperty<Window> window= new SimpleObjectProperty<>();

    @FXML
    public HBox rootComponent;
    @FXML
    private Slider subdivisionSlider;
    @FXML
    private Label subdivisionValueLabel;
    @FXML
    private Slider binarizationSlider;
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
    private TextField columnSeparatorValueField;
    @FXML
    private TextField lineEndingValueField;
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
        workMode.set(WorkMode.NONE);
        openImageItem = new MenuItem("open");
        pointsItem = new MenuItem("points");
        horizonItem = new MenuItem("horizon");
        itemGrid = new MenuItem("grid");
        mousePosition = new Vec2D(0.0D, 0.0D);
        previousMousePosition = new Vec2D(0.0D, 0.0D);
        imageCurve = new ImageCurve();
    }

    private void setupBindings() {
        // setup binding of window property so we can create modal dialogs
        rootComponent.sceneProperty().addListener(o -> {
            Scene scene = rootComponent.getScene();
            if(scene == null)
                return;
            if(window.isBound())
                window.unbind();
            window.bind(scene.windowProperty());
            //TODO will not work if windows change
            window.addListener((obj, old, newVal) -> createWindowEventHandlers());
        });

        mainCanvas.getDeleteMode().bind(deleteMode);
        mainCanvas.getWorkMode().bind(workMode);
    }

    private void addPropertyListeners() {
        subdivideIterations = (int) subdivisionSlider.getValue();

        subdivisionSlider.valueProperty().addListener((o, old, newValue) -> {
            if (subdivideIterations != newValue.intValue()) {
                subdivideIterations = newValue.intValue();
                subdivisionValueLabel.setText(Integer.toString(subdivideIterations));
                subdivisionSlider.setValue(subdivideIterations);
                imageCurve.setSubdivision(subdivideIterations);
                mainCanvas.redrawCanvas();
            }
        });

        binarizationSlider.valueProperty().addListener((o, old, newValue) -> {
            if (image != null && image.getThreshold() != newValue.intValue()) {
                image.setThreshold(newValue.intValue());
                image.updateBinarization(() -> Platform.runLater(() -> mainCanvas.redrawCanvas()));
            }
            binarizationValueLabel.setText(Integer.toString(newValue.intValue()));
        });

        drawSubMarkers.selectedProperty().addListener((o, old, newValue) -> {
            mainCanvas.setDrawSubdivisionMarkers(newValue);
            mainCanvas.redrawCanvas(false);
        });

        showImageToggle.selectedProperty().addListener((o, old, newValue) -> {
            mainCanvas.setShowImage(newValue);
            mainCanvas.redrawCanvas(false);
        });

        showBinarizationToggle.selectedProperty().addListener((o, old, newValue) -> {
            mainCanvas.setShowBinarization(newValue);
            mainCanvas.redrawCanvas(false);
        });

        decimalSeparatorComboBox.getSelectionModel().selectedIndexProperty().addListener(
                (o, old, newValue) -> decimalSeparator = newValue.equals(1) ? "," : "."
        );
    }

    private void exportPoints() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Mat", "*.mat"));
        chooser.setInitialDirectory(new File("."));

        File file = chooser.showSaveDialog(window.get());
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

        //TODO
        if (tickPopupController.getWasCanceled()) {
            imageCurve.resetSelectedTick();
        } else {
            if (tickPopupController.getDeleteTick()) {
//                imageCurve.deleteSelectedTick();
            } else {
                imageCurve.makeTickInput(tickPopupController.getTickValue());
            }
        }

        if (imageCurve.isXgridReady() && !imageCurve.isYgridReady()) {
            workMode.set(WorkMode.TICK_GRID);
        } else if (imageCurve.isYgridReady() && !imageCurve.isXgridReady()) {
            workMode.set(WorkMode.TICK_GRID);
        }

        ctrlPressed = false;
        Vec2D imagePos = mainCanvas.canvasToImage(mousePosition);
        imageCurve.updateHoveredElement(imagePos.getX(), imagePos.getY());
        mainCanvas.redrawCanvas(false);
        updateControls();
    }

    private void showTickInput() {
        //TODO
//        TickPoint selected = imageCurve.getSelectedTick();
//
//        if (selected == null) {
//            mainCanvas.redrawCanvas(false);
//            return;
//        }
//
//        tickPopupController.setTickValue(selected.getTickValue(), selected.isNew());
//
//        if(tickDialog.getOwner() == null)
//            tickDialog.initOwner(window.get());
//
//        tickDialog.show();
//        Window stage = window.get();
//        tickDialog.setX(stage.getX() + stage.getWidth() * 0.5 - tickDialog.getWidth() * 0.5);
//        tickDialog.setY(stage.getY() + stage.getHeight() * 0.5 - tickDialog.getHeight() * 0.5);
    }

    private void createWindowEventHandlers() {
        window.get().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.CONTROL) {
                onCtrlInput(false);
            } else if (e.getCode() == KeyCode.SHIFT) {
                onShiftInput(false);
            } else {
                return;
            }

            e.consume();
            mainCanvas.redrawCanvas(false);
        });

        window.get().addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.CONTROL) {
                onCtrlInput(true);
            } else if (e.getCode() == KeyCode.SHIFT) {
                onShiftInput(true);
            } else {
                return;
            }

            e.consume();
            mainCanvas.redrawCanvas(false);
        });
    }

    private void createEventHandlers() {
        tickDialog.setOnHiding(it -> handleTickInput());

        mainCanvas.setOnScroll(it -> {
            double zoomFactor = 1.3;
            if (ctrlPressed) {
                zoomFactor = 1.15;
            }

            if (it.getDeltaY() < 0) {
                zoomFactor = 2.0 - zoomFactor;
            }

            mainCanvas.zoomImage(mousePosition, zoomFactor);
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

    private void onCtrlInput(boolean released) {
        if (released) {
            ctrlPressed = false;
            Vec2D imagePos = mainCanvas.canvasToImage(mousePosition);
            imageCurve.unsnapSelected(imagePos.getX(), imagePos.getY());
        } else {
            ctrlPressed = true;
            imageCurve.snapSelected();
        }
        mainCanvas.redrawCanvas();
        updateControls();
    }

    private void onShiftInput(boolean released) {
        if (released) {
            deleteMode.set(false);
        } else if (imageCurve.getSelectedId() == null) {
            deleteMode.set(true);
        }

        updateControls();
    }

    private void onMouseDrag(MouseButton button) {
        if (button == MouseButton.PRIMARY) {
            switch (workMode.get()) {
                case POINTS:
                case TICK_GRID:
                case HORIZON:
                    Vec2D imagePos = mainCanvas.canvasToImage(mousePosition);
                    imageCurve.dragSelected(imagePos.getX(), imagePos.getY());

                    deleteMode.set(false);
                    if (ctrlPressed) {
                        imageCurve.snapSelected();
                    }
                    if (workMode.get() == WorkMode.POINTS ||
                            workMode.get() == WorkMode.HORIZON) {
                        imageCurve.sortPoints();
                    }
                    break;
            }
            updateControls();
            mainCanvas.redrawCanvas(false);
        } else if (button == MouseButton.MIDDLE) {
            mainCanvas.panImage(mousePosition.getX() - previousMousePosition.getX(),
                    mousePosition.getY() - previousMousePosition.getY());
        }
    }

    private boolean onMousePress(MouseButton button) {
        boolean consumed = false;

        if (button == MouseButton.PRIMARY) {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            } else {
                Vec2D imagePos = mainCanvas.canvasToImage(mousePosition);

                imageCurve.updateHoveredElement(imagePos.getX(), imagePos.getY());
                switch (workMode.get()) {
                    case NONE:
                        break;
                    case POINTS:
                        if (ctrlPressed) {
                            imageCurve.addPoint(imagePos.getX(), imagePos.getY());
                        } else if (imageCurve.selectHovered(ImageElement.Type.POINT)) {
                            imageCurve.dragSelected(imagePos.getX(), imagePos.getY());
                        }
                        break;
                    case TICK_GRID:
                        if (ctrlPressed) {
                            //imageCurve.addXtick(imagePos.getX(), imagePos.getY());
                        } else if (imageCurve.selectHovered(ImageElement.Type.TICK_GRID)) {
                            imageCurve.backupSelectedTick();

//                            selectedXtick?.x=e.x
//                            selectedXtick?.y=e.y
                        }
                        break;
                    case HORIZON:
                        if (imageCurve.selectHovered(ImageElement.Type.HORIZON)) {
                            imageCurve.dragSelected(imagePos.getX(), imagePos.getY());
                            imageCurve.sortPoints();
                        }
                        break;

                }
                mainCanvas.redrawCanvas(false);
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
        pointsItem.setDisable(image == null || workMode.get() == MainController.WorkMode.POINTS);
        itemGrid.setDisable(image == null || workMode.get() == MainController.WorkMode.TICK_GRID);
        horizonItem.setDisable(image == null || workMode.get() == MainController.WorkMode.HORIZON);


        contextMenu.show(mainCanvas, x, y);
    }

    private void onMouseRelease(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
//            updateHoveredElement(e.x,e.y)
            switch (workMode.get()) {
                case TICK_GRID:
                    imageCurve.deselectAll();
//                    if (deleteMode.get()) {
//                        imageCurve.deleteSelected();
//                    } else if (imageCurve.getSelectedTick() != null) {
//                        showTickInput();
//                    }
                    break;
                case POINTS:
                case HORIZON:
                    if (deleteMode.get()) {
                        //by using delete selected we can make sure that horizon
                        // will be reset only if it is selected
                        imageCurve.deleteSelected();
                    } else
                        imageCurve.deselectAll();
                    break;
            }

            deleteMode.set(e.isShiftDown());
        }

        updateControls();
        mainCanvas.redrawCanvas(false);
    }

    private void onMouseMove() {
        Vec2D imagePos = mainCanvas.canvasToImage(mousePosition);
        imageCurve.updateHoveredElement(imagePos.getX(), imagePos.getY());
        mainCanvas.redrawCanvas(false);
    }

    private void openImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("pictures", "*.jpg", "*.png", "*.bmp"));
        chooser.setInitialDirectory(new File("."));

        File file = chooser.showOpenDialog(window.get());
        if (file == null) {
            return;
        }
        image = new ImageWrapper(file.toPath());
        image.setThreshold((int) binarizationSlider.getValue());
        imageCurve.setImage(image);
        imageCurve.resetPoints();
        mainCanvas.updateImageTransform();
        workMode.set(WorkMode.POINTS);
        imageCurve.resetHorizon();
        mainCanvas.fitImage(false);
        mainCanvas.redrawCanvas(false);
        updateControls();
    }

    private void updateControls() {
        //check what controls are available and what should be disabled

        updateSaveReady();

        copyToClipboardButton.setDisable(!isSaveReady);
        exportButton.setDisable(!isSaveReady);

        //TODO update tips
    }

    private void initTickDialog() {
        // create tick popup
        FXMLLoader popupLoader = new FXMLLoader();
        popupLoader.setLocation(Main.class.getResource("tick_popup.fxml"));
        popupLoader.setResources(Main.getResourceBundle());
        Parent tickPopup;
        try {
            tickPopup = popupLoader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create tick popup", e);
        }
        tickPopupController = popupLoader.getController();

        //TODO use Stage(StageStyle.TRANSPARENT)
        tickDialog = new Stage();
        tickDialog.setResizable(false);
        tickDialog.initModality(Modality.WINDOW_MODAL);
        tickDialog.setTitle(resources.getString("tickDialogTitle"));
        tickDialog.setScene(new Scene(tickPopup, 230, 140));
    }

    /**
     * Called after FXML finished loading and all properties are ready
     */
    public void initialize() {
        rootComponent.getStylesheets().add(MainController.class
                .getResource("main.css").toExternalForm());

        mainCanvas.setImageCurve(imageCurve);

        initTickDialog();

        setupBindings();

        contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(openImageItem, pointsItem, horizonItem, itemGrid);

        openImageButton.addEventHandler(ActionEvent.ACTION, it -> openImage());

        copyToClipboardButton.addEventHandler(ActionEvent.ACTION, it -> copyPoints());

        exportButton.addEventHandler(ActionEvent.ACTION, it -> exportPoints());
        openImageItem.setOnAction((it -> openImage()));
        pointsItem.setOnAction((it -> {
            workMode.set(WorkMode.POINTS);
            updateControls();
        }));
        horizonItem.setOnAction((it -> {
            workMode.set(WorkMode.HORIZON);
            updateControls();
        }));
        itemGrid.setOnAction((it -> {
            workMode.set(WorkMode.TICK_GRID);
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

    public enum WorkMode {
        NONE,
        POINTS,
        TICK_GRID,
        HORIZON;
    }
}
