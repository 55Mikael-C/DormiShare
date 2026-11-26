package com.DormiShare.ui;

import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import java.util.function.Consumer;
import com.DormiShare.model.TenantBillShare;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Function;

public class UiKit {
    private UiKit() {}

    public static String peso(double v) { return String.format("₱%,.2f", v); }

    public static String num(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Para may style yung numbers, ex: "1,234.50"; returns null for blank/invalid. */
    public static Double parseNumber(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim().replace(",", "")); }
        catch (NumberFormatException e) { return null; }
    }

    public static VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(4, l, input);
    }

    public static Button primary(String text) { return button(text, "btn-primary"); }
    public static Button secondary(String text) { return button(text, "btn-secondary"); }
    public static Button danger(String text) { return button(text, "btn-danger"); }

    private static Button button(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        return b;
    }

    public static void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static <T> TableColumn<T, String> textCol(String title, Function<T, String> f, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue())));
        c.setPrefWidth(width);
        return c;
    }



    public static TableView<TenantBillShare> shareTable() {
        TableView<TenantBillShare> t = new TableView<>();
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Tenant", TenantBillShare::getTenantName, 120));
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Room", TenantBillShare::getRoomName, 70));
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Days / Basis",
                s -> s.getBillableDays() + " / " + s.getTotalBasisDays(), 90));
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Share", s -> peso(s.getAmountPeso()), 90));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("No preview yet."));
        t.setPrefHeight(260);
        t.setMinHeight(200);
        return t;
    }
}
