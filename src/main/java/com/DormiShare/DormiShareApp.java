package com.DormiShare;

import com.DormiShare.service.JsonStorageService;
import com.DormiShare.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DormiShareApp extends Application{
    
    @Override
    public void start(Stage stage) {
        JsonStorageService storage = new JsonStorageService();
        storage.load();

        MainView view = new MainView(storage);
        Scene scene = new Scene(view.getRoot(), 1280, 780);
        scene.getStylesheets().add(getClass().getResource("/css/DormiShare.css").toExternalForm());

        stage.setTitle("DormiShare");
        stage.setMinWidth(980);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
