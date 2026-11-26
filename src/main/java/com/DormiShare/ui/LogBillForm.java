package com.DormiShare.ui;

import com.DormiShare.model.*;
import com.DormiShare.service.BillingCalculatorService;
import com.DormiShare.service.JsonStorageService;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class LogBillForm extends VBox{

    private final Location loc;
    private final BillingCalculatorService calc;
    private final JsonStorageService storage;
    private final Runnable onSaved;

    private final ChoiceBox<UtilityType> type = new ChoiceBox<>(FXCollections.observableArrayList(UtilityType.values()));
    private final DatePicker start = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker end = new DatePicker(YearMonth.now().atEndOfMonth());
    private final TextField total = new TextField();
    private final VBox readingsBox = new VBox(8);
    private final Map<String, TextField[]> readingFields = new LinkedHashMap<>();
    private final TableView<TenantBillShare> preview = UiKit.shareTable();
    private final Label message = new Label();
    private final Label summary = new Label();
    private final Button confirm = UiKit.primary("Confirm & Save Bills");

    // last valid inputs (set by recompute, used by confirm)
    private double lastBill;
    private Map<String, Double> lastPrev = new HashMap<>();
    private Map<String, Double> lastCurr = new HashMap<>();

    public LogBillForm(Location loc, BillingCalculatorService calc, JsonStorageService storage, Runnable onSaved) {
        super(14);
        this.loc = loc;
        this.calc = calc;
        this.storage = storage;
        this.onSaved = onSaved;

        type.setConverter(new StringConverter<UtilityType>() {
            @Override public String toString(UtilityType t) {
                return t == null ? "" : (t == UtilityType.ELECTRIC ? "Electric" : "Water");
            }
            @Override public UtilityType fromString(String s) { return null; }
        });
        type.setMaxWidth(Double.MAX_VALUE);
        total.setPromptText("Exact amount on the bill, e.g. 4,820.55");
        start.setPrefWidth(190);
        end.setPrefWidth(190);
        message.getStyleClass().add("hint");
        message.setWrapText(true);
        summary.getStyleClass().add("summary-label");
        confirm.setMaxWidth(Double.MAX_VALUE);
        confirm.setDisable(true);
        confirm.setOnAction(e -> save());

        Label previewLabel = new Label("Live preview");
        previewLabel.getStyleClass().add("section-label");

        getChildren().addAll(
                UiKit.field("Utility type", type),
                new HBox(10, UiKit.field("Period start", start), UiKit.field("Period end", end)),
                UiKit.field("Total bill (₱)", total),
                readingsBox,
                previewLabel, preview, summary, message, confirm);

        type.valueProperty().addListener((o, a, b) -> rebuildReadings());
        start.valueProperty().addListener((o, a, b) -> recompute());
        end.valueProperty().addListener((o, a, b) -> recompute());
        total.textProperty().addListener((o, a, b) -> recompute());
        type.setValue(UtilityType.ELECTRIC);
    }

    private void rebuildReadings() {
        readingFields.clear();
        readingsBox.getChildren().clear();
        UtilityType t = type.getValue();
        if (t != null && BillingCalculatorService.isSubMetered(loc, t)) {
            Label h = new Label("Sub-meter readings (" + (t == UtilityType.ELECTRIC ? "kWh" : "m³") + ")");
            h.getStyleClass().add("section-label");
            readingsBox.getChildren().add(h);
            readingsBox.getChildren().add(readingRow(new Label("Room"), new Label("Previous"), new Label("Current"), true));
            for (Room r : loc.getRooms()) {
                double prev = t == UtilityType.ELECTRIC ? r.getLastElectricReading() : r.getLastWaterReading();
                TextField p = new TextField(UiKit.num(prev));
                TextField c = new TextField();
                c.setPromptText("Current");
                p.textProperty().addListener((o, a, b) -> recompute());
                c.textProperty().addListener((o, a, b) -> recompute());
                readingFields.put(r.getId(), new TextField[]{p, c});
                Label name = new Label(r.getName());
                readingsBox.getChildren().add(readingRow(name, p, c, false));
            }
        }
        recompute();
    }

    private HBox readingRow(javafx.scene.Node a, javafx.scene.Node b, javafx.scene.Node c, boolean header) {
        HBox row = new HBox(8, a, b, c);
        row.setAlignment(Pos.CENTER_LEFT);
        if (a instanceof Label l) { l.setPrefWidth(100); if (header) l.getStyleClass().add("field-label"); }
        if (b instanceof Label l) { l.setPrefWidth(140); if (header) l.getStyleClass().add("field-label"); }
        if (b instanceof TextField f) f.setPrefWidth(140);
        if (c instanceof Label l) { l.setPrefWidth(140); if (header) l.getStyleClass().add("field-label"); }
        if (c instanceof TextField f) f.setPrefWidth(140);
        return row;
    }

    private void recompute() {
        preview.getItems().clear();
        confirm.setDisable(true);
        summary.setText("");

        UtilityType t = type.getValue();
        LocalDate s = start.getValue(), e = end.getValue();
        Double bill = UiKit.parseNumber(total.getText());
        if (t == null || s == null || e == null || bill == null || bill <= 0) {
            message.setText("Enter the period and the exact total bill (₱) to see the preview.");
            return;
        }
        Map<String, Double> prev = new HashMap<>(), curr = new HashMap<>();
        if (BillingCalculatorService.isSubMetered(loc, t)) {
            for (var entry : readingFields.entrySet()) {
                Double p = UiKit.parseNumber(entry.getValue()[0].getText());
                Double c = UiKit.parseNumber(entry.getValue()[1].getText());
                if (p == null || c == null) {
                    message.setText("Enter previous and current readings for every room.");
                    return;
                }
                prev.put(entry.getKey(), p);
                curr.put(entry.getKey(), c);
            }
        }
        List<TenantBillShare> shares;
        try {
            shares = calc.calculate(loc, t, s, e, bill, prev, curr);
        } catch (RuntimeException ex) {
            message.setText(ex.getMessage());
            return;
        }
        message.setText("");
        preview.getItems().setAll(shares);
        double sum = shares.stream().mapToDouble(TenantBillShare::getAmountPeso).sum();
        summary.setText("Allocated " + UiKit.peso(sum) + " of " + UiKit.peso(bill));
        lastBill = bill;
        lastPrev = prev;
        lastCurr = curr;
        confirm.setDisable(false);
    }

    private void save() {
        UtilityType t = type.getValue();
        LocalDate s = start.getValue(), e = end.getValue();
        boolean overlap = loc.getBillHistory().stream().anyMatch(b ->
                b.getType() == t && !b.getPeriodStart().isAfter(e) && !b.getPeriodEnd().isBefore(s));
        if (overlap && !UiKit.confirm("A " + (t == UtilityType.ELECTRIC ? "electric" : "water")
                + " bill already overlaps this period. Save anyway?")) return;
        try {
            calc.commit(loc, t, s, e, lastBill, lastPrev, lastCurr);
            storage.save();
            onSaved.run();
        } catch (RuntimeException ex) {
            UiKit.error(ex.getMessage());
        }
    }
}
