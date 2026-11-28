package com.DormiShare;

import com.DormiShare.service.JsonStorageService;
import com.DormiShare.ui.MainView;
import com.DormiShare.ui.UiKit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DormiShareApp extends Application{
    
    @Override
    public void start(Stage stage) {
        JsonStorageService storage = new JsonStorageService();
        storage.load();

        MainView view = new MainView(storage);
        StackPane root = UiKit.wrapWithVideo(view.getRoot(), "/videos/background.mp4");
        
        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(getClass().getResource("/css/DormiShare.css").toExternalForm());

        stage.setTitle("DormiShare");
        stage.setMinWidth(980);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
