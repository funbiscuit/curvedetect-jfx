package com.funbiscuit.jfx.curvedetect;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class TickPopupController {
    @FXML
    private Parent rootComponent;
    @FXML
    private TextField tickValueText;
    @FXML
    private Button tickApprove;
    @FXML
    private Button tickCancel;
    @FXML
    private Button tickDelete;

    private String tickValue = "";
    private boolean wasCanceled = true;
    private boolean deleteTick;
    private int maxValueLength = 8;
    private boolean isNew;


    public double getTickValue() {
        try {
            return Double.parseDouble(tickValue);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public boolean getWasCanceled() {
        return this.wasCanceled;
    }

    public boolean getDeleteTick() {
        return this.deleteTick;
    }


    public void setTickValue(double value, boolean isNew) {
        this.isNew = isNew;
        tickValueText.setText(filterValue(String.valueOf(value)));
        tickDelete.setDisable(isNew);
    }

    private String filterValue(String value) {

        //TODO

        return value;
    }

    private void closePopup() {
        ((Stage) rootComponent.getScene().getWindow()).close();
    }

    /**
     * Called after FXML finished loading and all properties are ready
     */
    public void initialize() {
        rootComponent.sceneProperty().addListener(o -> {
            Scene scene = rootComponent.getScene();
            if (scene == null)
                return;
            //TODO will not work if windows change
            scene.windowProperty().addListener((obj, old, newVal) -> {
                newVal.addEventHandler(WindowEvent.WINDOW_SHOWN, it -> {
                    tickValueText.requestFocus();
                    tickValueText.selectAll();
                });
            });
        });


        //setting it in fxml doesn't work in native image
        tickValueText.setAlignment(Pos.CENTER);

        tickValueText.textProperty().addListener((o, old, newValue) ->
        {
            tickValue = filterValue(newValue);
            tickValueText.setText(tickValue);
        });

        tickValueText.addEventHandler(ActionEvent.ACTION, e -> {
            wasCanceled = false;
            deleteTick = false;
            closePopup();
        });

        tickApprove.addEventHandler(ActionEvent.ACTION, it -> {
            wasCanceled = false;
            deleteTick = false;
            closePopup();
        });

        tickCancel.addEventHandler(ActionEvent.ACTION, it -> {
            wasCanceled = !isNew;
            deleteTick = isNew;
            closePopup();
        });

        tickDelete.addEventHandler(ActionEvent.ACTION, it -> {
            wasCanceled = false;
            deleteTick = true;
            closePopup();
        });
    }
}

