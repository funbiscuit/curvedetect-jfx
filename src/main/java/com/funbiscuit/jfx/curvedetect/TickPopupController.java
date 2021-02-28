package com.funbiscuit.jfx.curvedetect;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class TickPopupController {
    @FXML
    private TextField tickValueText;
    @FXML
    private Label tickTipLabel;
    @FXML
    private Button tickApprove;
    @FXML
    private Button tickCancel;
    @FXML
    private Button tickDelete;
    private Stage stage;

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

    public void init(Stage stage) {
        this.stage = stage;
        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, it -> {
            tickValueText.requestFocus();
            tickValueText.selectAll();
        });
    }

    public void initialize() {
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
            stage.close();
        });

        tickApprove.addEventHandler(ActionEvent.ACTION, it -> {
            wasCanceled = false;
            deleteTick = false;
            stage.close();
        });

        tickCancel.addEventHandler(ActionEvent.ACTION, it -> {
            wasCanceled = !isNew;
            deleteTick = isNew;
            stage.close();
        });

        tickDelete.addEventHandler(ActionEvent.ACTION, it -> {
            wasCanceled = false;
            deleteTick = true;
            stage.close();
        });
    }
}

