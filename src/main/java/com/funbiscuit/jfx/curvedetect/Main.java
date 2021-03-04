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

    public static ResourceBundle getResourceBundle() {
        return ResourceBundle.getBundle("com.funbiscuit.jfx.curvedetect.strings", Locale.ENGLISH);
    }

    @Override
    public void start(Stage stage) throws IOException {
        ResourceBundle bundle = getResourceBundle();

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(Main.class.getResource("sample.fxml"));
        fxmlLoader.setResources(bundle);

        Parent root = fxmlLoader.load();

        stage.setScene(new Scene(root, 1280, 720));
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setTitle(bundle.getString("title"));
        stage.show();
    }
}
