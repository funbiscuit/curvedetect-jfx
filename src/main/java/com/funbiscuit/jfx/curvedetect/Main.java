package com.funbiscuit.jfx.curvedetect;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {

    public static void main(String[] args) {
        Main.launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle("com.funbiscuit.jfx.curvedetect.strings", Locale.ENGLISH);
        FXMLLoader fxmlLoader = new FXMLLoader();
        FXMLLoader popupLoader = new FXMLLoader();

        fxmlLoader.setLocation(Main.class.getResource("sample.fxml"));
        popupLoader.setLocation(Main.class.getResource("tick_popup.fxml"));
        fxmlLoader.setResources(bundle);
        popupLoader.setResources(bundle);

        Parent root = fxmlLoader.load();
        Parent tickPopup = popupLoader.load();

        root.getStylesheets().add(Main.class.getResource("main.css").toExternalForm());

        stage.setScene(new Scene(root, 1280, 720));
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setTitle(bundle.getString("title"));
        stage.show();
        MainController controller = fxmlLoader.getController();
        TickPopupController tickController = popupLoader.getController();
        controller.setTickPopup(tickPopup);
        controller.setTickPopupController(tickController);
        controller.init(stage);
    }
}
