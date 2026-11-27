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

    /** Renders comma-separated tokens as pills: Paid = green, Pending = amber, anything else = neutral gray. */
    public static <T> TableColumn<T, String> badgeCol(String title, Function<T, String> f, double width) {
        TableColumn<T, String> c = textCol(title, f, width);
        c.setCellFactory(col -> new TableCell<T, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER_LEFT);
                for (String token : item.split(",")) {
                    String t = token.trim();
                    Label badge = new Label(t);
                    badge.getStyleClass().add("badge");
                    switch (t) {
                        case "Paid" -> badge.getStyleClass().add("badge-paid");
                        case "Pending" -> badge.getStyleClass().add("badge-pending");
                        default -> badge.getStyleClass().add("badge-neutral");
                    }
                    box.getChildren().add(badge);
                }
                setGraphic(box);
                setText(null);
            }
        });
        return c;
    }

        // ---- small outline icons (24x24 grid) ----
    public static final String ICON_PIN = "M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z M12 7a3 3 0 1 0 0 6a3 3 0 1 0 0-6Z";
    public static final String ICON_BOLT = "M13 2L3 14h9l-1 8l10-12h-9l1-8z";
    public static final String ICON_SEARCH = "M3 11a8 8 0 1 0 16 0a8 8 0 1 0-16 0 M21 21l-4.3-4.3";
    public static final String ICON_CHEVRON = "M9 18l6-6-6-6";

    public static Node icon(String svg, double size) {
        SVGPath p = new SVGPath();
        p.setContent(svg);
        p.getStyleClass().add("icon");
        double scale = size / 24.0;
        p.setScaleX(scale);
        p.setScaleY(scale);
        StackPane box = new StackPane(new Group(p));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    private static void fixSize(Region r, double w, double h) {
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
    }

    /** Column that shows "5 / 7" as a small progress bar plus the text. */
    public static <T> TableColumn<T, String> occupancyCol(String title, Function<T, String> f, double width) {
        TableColumn<T, String> c = textCol(title, f, width);
        c.setCellFactory(col -> new TableCell<T, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item.contains("/")) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String[] parts = item.split("/");
                int occ = Integer.parseInt(parts[0].trim());
                int cap = Integer.parseInt(parts[1].trim());
                double ratio = cap <= 0 ? 0 : Math.min(1.0, (double) occ / cap);

                StackPane track = new StackPane();
                fixSize(track, 96, 6);
                track.getStyleClass().add("occ-track");
                Region fill = new Region();
                fixSize(fill, 96 * ratio, 6);
                fill.getStyleClass().add("occ-fill");
                StackPane.setAlignment(fill, Pos.CENTER_LEFT);
                track.getChildren().add(fill);

                Label count = new Label(item);
                count.getStyleClass().add("occ-count");
                HBox box = new HBox(12, track, count);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
        return c;
    }

    /** Last column: a small gray button with a ">" arrow. */
    public static <T> TableColumn<T, Void> chevronCol(Consumer<T> onClick) {
        TableColumn<T, Void> c = new TableColumn<>("");
        c.setMinWidth(90);
        c.setPrefWidth(90);
        c.setMaxWidth(90);
        c.setSortable(false);
        c.setCellFactory(col -> new TableCell<T, Void>() {
            private final Button b = new Button();
            {
                b.setGraphic(icon(ICON_CHEVRON, 16));
                b.getStyleClass().add("chevron-btn");
                b.setOnAction(e -> onClick.accept(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : b);
            }
        });
        return c;
    }

    public static TableView<TenantBillShare> shareTable() {
        TableView<TenantBillShare> t = new TableView<>();
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Tenant", TenantBillShare::getTenantName, 120));
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Room", TenantBillShare::getRoomName, 70));
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Days / Basis",
                s -> s.getBillableDays() + " / " + s.getTotalBasisDays(), 90));
        t.getColumns().add(UiKit.<TenantBillShare>textCol("Share", s -> peso(s.getAmountPeso()), 90));
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        t.setPlaceholder(new Label("No preview yet."));
        t.setPrefHeight(260);
        t.setMinHeight(200);
        return t;
    }
}
