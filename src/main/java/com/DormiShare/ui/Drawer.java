package com.DormiShare.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class Drawer extends VBox{
    private final Label title = new Label();
    private final VBox body = new VBox();

    public Drawer() {
        getStyleClass().add("drawer");
        setPrefWidth(440);
        setMinWidth(440);
        setMaxWidth(440);

        title.getStyleClass().add("drawer-title");
        Button close = new Button("✕");
        close.getStyleClass().add("icon-btn");
        close.setOnAction(e -> hide());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, close);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("drawer-header");

        body.getStyleClass().add("drawer-body");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("drawer-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, scroll);
        hide();
    }

    public void show(String heading, Node content) {
        title.setText(heading);
        body.getChildren().setAll(content);
        setVisible(true);
        setManaged(true);
    }

    public void hide() {
        body.getChildren().clear();
        setVisible(false);
        setManaged(false);
    }
}
