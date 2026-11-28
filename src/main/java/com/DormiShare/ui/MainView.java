package com.DormiShare.ui;

import javafx.css.PseudoClass;
import javafx.scene.shape.Circle;
import com.DormiShare.model.*;
import com.DormiShare.service.BillingCalculatorService;
import com.DormiShare.service.JsonStorageService;
import com.DormiShare.service.PdfExportService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainView {
    private enum Level { LOCATIONS, ROOMS, TENANTS, BILLS }

    private record BillRow(Location loc, BillRecord bill) {}

    private final JsonStorageService storage;
    private final AppData data;
    private final BillingCalculatorService calc = new BillingCalculatorService();
    private final PdfExportService pdf = new PdfExportService();

    private final BorderPane root = new BorderPane();
    private final HBox breadcrumb = new HBox(4);
    private final TextField search = new TextField();
    private final VBox workspace = new VBox(14);
    private final Drawer drawer = new Drawer();
    

    private Level level = Level.LOCATIONS;
    private Location currentLocation;
    private Room currentRoom;
    private TableView<Tenant> tenantTable;
    
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");
    private final VBox sidebar = new VBox(4);

    public MainView(JsonStorageService storage) {
        this.storage = storage;
        this.data = storage.getData();

        root.getStyleClass().add("root-pane");
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        BorderPane.setMargin(sidebar, new Insets(14, 0, 0, 0));
        workspace.getStyleClass().add("workspace");
        root.setCenter(workspace);
        root.setRight(drawer);

        search.textProperty().addListener((o, a, b) -> render());
        showLocations();
    }

    public BorderPane getRoot() { return root; }

    // ================================================================ chrome

    private Node buildTopBar() {
        Circle dot = new Circle(12);
        dot.getStyleClass().add("brand-dot");
        Label title = new Label("DormiShare");
        title.getStyleClass().add("app-title");
        HBox brand = new HBox(10, dot, title);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        breadcrumb.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Search");
        search.setFocusTraversable(false);
        search.setPrefWidth(280);
        search.getStyleClass().add("search");
        HBox searchBox = new HBox(10, UiKit.icon(UiKit.ICON_SEARCH, 18), search);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("search-box");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(24, brand, breadcrumb, spacer, searchBox);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private Node buildSidebar() {
        sidebar.getStyleClass().add("sidebar-box");
        sidebar.setPrefWidth(270);
        sidebar.setMinWidth(270);
        sidebar.setMaxHeight(Double.MAX_VALUE);
        rebuildSidebar();
        return sidebar;
    }

    private void rebuildSidebar() {
        sidebar.getChildren().clear();
        Label heading = new Label("Manage");
        heading.getStyleClass().add("sidebar-heading");
        sidebar.getChildren().add(heading);
        sidebar.getChildren().add(navItem("locations", "Locations", UiKit.ICON_PIN, this::showLocations));
        for (Location l : data.getLocations())
            sidebar.getChildren().add(navItem(l.getId(), l.getName(), null, () -> showRooms(l)));
        sidebar.getChildren().add(navItem("bills", "Utilities & Bills", UiKit.ICON_BOLT, this::showBills));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Node exitItem = navItem("exit", "Exit", UiKit.ICON_EXIT, () -> {
            storage.save();
            javafx.application.Platform.exit();
        });
        exitItem.getStyleClass().add("nav-exit");
        sidebar.getChildren().add(exitItem);
        updateSidebar();
    }

    private Node navItem(String key, String text, String iconSvg, Runnable action) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(key);
        row.getStyleClass().add(iconSvg == null ? "nav-sub" : "nav-item");
        if (iconSvg != null) row.getChildren().add(UiKit.icon(iconSvg, 20));
        row.getChildren().add(new Label(text));
        row.setOnMouseClicked(e -> action.run());
        return row;
    }

    private void updateSidebar() {
        String active = switch (level) {
            case LOCATIONS -> "locations";
            case BILLS -> "bills";
            case ROOMS, TENANTS -> currentLocation == null ? "" : currentLocation.getId();
        };
        for (Node n : sidebar.getChildren())
            n.pseudoClassStateChanged(CURRENT, active.equals(n.getUserData()));
    }

    private void updateBreadcrumb() {
        boolean deep = level == Level.ROOMS || level == Level.TENANTS;
        breadcrumb.setVisible(deep);
        breadcrumb.setManaged(deep);
        breadcrumb.getChildren().clear();
        if (!deep) return;

        crumb("Locations", this::showLocations);
        sep();
        Location loc = currentLocation;
        crumb(loc.getName(), level == Level.ROOMS ? null : () -> showRooms(loc));
        if (level == Level.TENANTS) {
            sep();
            crumb(currentRoom.getName(), null);
        }
    }

    private void crumb(String text, Runnable action) {
        if (action == null) {
            Label l = new Label(text);
            l.getStyleClass().add("crumb-current");
            breadcrumb.getChildren().add(l);
        } else {
            Hyperlink h = new Hyperlink(text);
            h.getStyleClass().add("crumb");
            h.setOnAction(e -> action.run());
            breadcrumb.getChildren().add(h);
        }
    }

    private void sep() {
        Label s = new Label("/");
        s.getStyleClass().add("crumb-sep");
        breadcrumb.getChildren().add(s);
    }

    // ================================================================ navigation

    private void showLocations() { level = Level.LOCATIONS; currentLocation = null; currentRoom = null; drawer.hide(); render(); }
    private void showRooms(Location l) { level = Level.ROOMS; currentLocation = l; currentRoom = null; drawer.hide(); render(); }
    private void showTenants(Room r) { level = Level.TENANTS; currentRoom = r; drawer.hide(); render(); }
    private void showBills() { level = Level.BILLS; currentLocation = null; currentRoom = null; drawer.hide(); render(); }

    private void render() {
        updateBreadcrumb();
        updateSidebar();
        workspace.getChildren().clear();
        switch (level) {
            case LOCATIONS -> renderLocations();
            case ROOMS -> renderRooms();
            case TENANTS -> renderTenants();
            case BILLS -> renderBills();
        }
    }

    private void persist() { storage.save(); }

    // ================================================================ level 1: locations

    private void renderLocations() {
        TableView<Location> table = newTable();
        table.getColumns().addAll(List.of(
                UiKit.<Location>textCol("Name", Location::getName, 200),
                UiKit.<Location>textCol("Rooms", l -> String.valueOf(l.getRooms().size()), 100),
                UiKit.<Location>occupancyCol("Occupied / Capacity", l -> occupied(l) + " / " + capacity(l), 200),
                UiKit.<Location>badgeCol("Sub-meters", this::meterBadges, 200),
                UiKit.<Location>badgeCol("Status", this::locationStatus, 120),
                UiKit.<Location>chevronCol(this::showRooms)));
        table.getColumns().get(0).getStyleClass().add("name-col");
        table.getItems().setAll(data.getLocations().stream().filter(l -> matches(l.getName())).toList());
        installRows(table, this::showRooms, this::openLocationForm, this::deleteLocation, Map.of());

        Button add = UiKit.primary("+ New Location");
        add.setOnAction(e -> openLocationForm(null));
        setPage("Locations", new Node[]{add}, table);
    }

    private String meterBadges(Location l) {
        List<String> parts = new ArrayList<>();
        if (l.isHasElectricSubMeter()) parts.add("Electric");
        if (l.isHasWaterSubMeter()) parts.add("Water");
        return parts.isEmpty() ? "None" : String.join(",", parts);
    }

    private String locationStatus(Location l) {
        List<Tenant> all = tenantsOf(l);
        String rent = aggregate(all, Tenant::getRentStatus);
        String util = aggregate(all, Tenant::getUtilityStatus);
        if (rent.equals("—")) return "—";
        return rent.equals("Paid") && util.equals("Paid") ? "Paid" : "Pending";
    }

    private void openLocationForm(Location existing) {
        TextField name = new TextField(existing == null ? "" : existing.getName());
        CheckBox el = new CheckBox("Electric sub-meter per room");
        CheckBox wa = new CheckBox("Water sub-meter per room");
        el.setSelected(existing != null && existing.isHasElectricSubMeter());
        wa.setSelected(existing != null && existing.isHasWaterSubMeter());

        Button save = UiKit.primary(existing == null ? "Create Location" : "Save Changes");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            String n = name.getText().trim();
            if (n.isEmpty()) { UiKit.error("Location name is required."); return; }
            Location l = existing != null ? existing : new Location();
            l.setName(n);
            l.setHasElectricSubMeter(el.isSelected());
            l.setHasWaterSubMeter(wa.isSelected());
            if (existing == null) data.getLocations().add(l);
            persist();
            rebuildSidebar();
            drawer.hide();
            render();
        });
        drawer.show(existing == null ? "New Location" : "Location Properties",
                new VBox(14, UiKit.field("Location name", name), el, wa, save));
    }

    private void deleteLocation(Location l) {
        if (!UiKit.confirm("Delete \"" + l.getName() + "\" with all its rooms, tenants and bill history?")) return;
        data.getLocations().remove(l);
        persist();
        rebuildSidebar();
        render();
    }

    // ================================================================ level 2: rooms

    private void renderRooms() {
        TableView<Room> table = newTable();
        table.getColumns().addAll(List.of(
                UiKit.<Room>textCol("Room", Room::getName, 160),
                UiKit.<Room>textCol("Tenants", r -> occupied(r) + " / " + r.getCapacity(), 90),
                UiKit.<Room>textCol("Monthly Rate", r -> UiKit.peso(r.getMonthlyRent()), 120),
                UiKit.<Room>badgeCol("Rent Status", r -> aggregate(r.getTenants(), Tenant::getRentStatus), 120),
                UiKit.<Room>badgeCol("Utilities Status", r -> aggregate(r.getTenants(), Tenant::getUtilityStatus), 140)));
        table.getItems().setAll(currentLocation.getRooms().stream().filter(r -> matches(r.getName())).toList());
        installRows(table, this::showTenants, this::openRoomForm, this::deleteRoom, Map.of());

        Button add = UiKit.primary("+ New Room");
        add.setOnAction(e -> openRoomForm(null));
        Button bill = UiKit.secondary("Log Utility Bill");
        bill.setOnAction(e -> openBillForm());
        Button props = UiKit.secondary("Location Properties");
        props.setOnAction(e -> openLocationForm(currentLocation));
        setPage(currentLocation.getName() + " · Rooms", new Node[]{add, bill, props}, table);
    }

    private void openRoomForm(Room existing) {
        Location loc = currentLocation;
        TextField name = new TextField(existing == null ? "" : existing.getName());
        Spinner<Integer> cap = new Spinner<>(1, 20, existing == null ? 4 : existing.getCapacity());
        cap.setMaxWidth(Double.MAX_VALUE);
        TextField rent = new TextField(existing == null ? "" : UiKit.num(existing.getMonthlyRent()));
        rent.setPromptText("Monthly rent per tenant");
        TextField elec = new TextField(existing == null ? "0" : UiKit.num(existing.getLastElectricReading()));
        TextField water = new TextField(existing == null ? "0" : UiKit.num(existing.getLastWaterReading()));

        VBox form = new VBox(14, UiKit.field("Room name", name), UiKit.field("Capacity", cap),
                UiKit.field("Monthly rent (₱)", rent));
        if (loc.isHasElectricSubMeter()) form.getChildren().add(UiKit.field("Starting electric reading (kWh)", elec));
        if (loc.isHasWaterSubMeter()) form.getChildren().add(UiKit.field("Starting water reading (m³)", water));

        Button save = UiKit.primary(existing == null ? "Create Room" : "Save Changes");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            String n = name.getText().trim();
            Double rentVal = UiKit.parseNumber(rent.getText());
            if (n.isEmpty() || rentVal == null || rentVal < 0) {
                UiKit.error("Enter a room name and a valid monthly rent.");
                return;
            }
            Room r = existing != null ? existing : new Room();
            r.setName(n);
            r.setCapacity(cap.getValue());
            r.setMonthlyRent(rentVal);
            Double ev = UiKit.parseNumber(elec.getText());
            Double wv = UiKit.parseNumber(water.getText());
            if (loc.isHasElectricSubMeter() && ev != null) r.setLastElectricReading(ev);
            if (loc.isHasWaterSubMeter() && wv != null) r.setLastWaterReading(wv);
            if (existing == null) loc.getRooms().add(r);
            persist();
            drawer.hide();
            render();
        });
        form.getChildren().add(save);
        drawer.show(existing == null ? "New Room" : "Edit Room", form);
    }

    private void deleteRoom(Room r) {
        if (!UiKit.confirm("Delete room \"" + r.getName() + "\" and its tenants?")) return;
        currentLocation.getRooms().remove(r);
        persist();
        render();
    }

    private void openBillForm() {
        boolean hasTenants = currentLocation.getRooms().stream().anyMatch(r -> !r.getTenants().isEmpty());
        if (!hasTenants) { UiKit.info("Add rooms and tenants before logging a bill."); return; }
        drawer.show("Log Utility Bill", new LogBillForm(currentLocation, calc, storage, () -> {
            drawer.hide();
            render();
            UiKit.info("Bills saved. Open a room and use Download PDF on a tenant.");
        }));
    }

    // ================================================================ level 3: tenants

    private void renderTenants() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = YearMonth.now().atEndOfMonth();

        TableView<Tenant> table = newTable();
        tenantTable = table;
        table.getColumns().addAll(List.of(
                UiKit.<Tenant>textCol("Name", Tenant::getFullName, 170),
                UiKit.<Tenant>textCol("Stay Dates", t -> t.getMoveInDate() + " → "
                        + (t.getMoveOutDate() == null ? "present" : t.getMoveOutDate()), 200),
                UiKit.<Tenant>textCol("Billable Days (this month)",
                        t -> calc.billableDays(t, monthStart, monthEnd) + " / " + monthEnd.getDayOfMonth(), 170),
                UiKit.<Tenant>badgeCol("Rent", t -> label(t.getRentStatus()), 100),
                UiKit.<Tenant>badgeCol("Utilities", t -> label(t.getUtilityStatus()), 110)));

        TableColumn<Tenant, Void> pdfCol = new TableColumn<>("Statement");
        pdfCol.setPrefWidth(130);
        pdfCol.setCellFactory(c -> new TableCell<Tenant, Void>() {
            private final Button btn = UiKit.secondary("");
            {
                btn.setGraphic(UiKit.icon(UiKit.ICON_DOWNLOAD, 18));
                btn.getStyleClass().addAll("icon-btn", "btn-small");
                btn.setTooltip(new Tooltip("Download PDF Statement"));
                btn.setOnAction(e -> exportPdf(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().add(pdfCol);

        table.getItems().setAll(currentRoom.getTenants().stream().filter(t -> matches(t.getFullName())).toList());

        Map<String, Consumer<Tenant>> extras = new LinkedHashMap<>();
        extras.put("Toggle rent status", t -> { t.setRentStatus(flip(t.getRentStatus())); persist(); render(); });
        extras.put("Toggle utility status", t -> { t.setUtilityStatus(flip(t.getUtilityStatus())); persist(); render(); });
        extras.put("Manage away dates", this::openAwayForm);
        installRows(table, null, this::openTenantForm, this::deleteTenant, extras);

        Button add = UiKit.primary("+ Add Tenant");
        add.setOnAction(e -> openTenantForm(null));
        Button away = UiKit.secondary("Manage Away Dates");
        away.setOnAction(e -> {
            Tenant sel = tenantTable.getSelectionModel().getSelectedItem();
            if (sel == null) UiKit.info("Select a tenant in the table first.");
            else openAwayForm(sel);
        });
        setPage(currentRoom.getName() + " · Tenants", new Node[]{add, away}, table);
    }

    private void openTenantForm(Tenant existing) {
        Room room = currentRoom;
        TextField name = new TextField(existing == null ? "" : existing.getFullName());
        DatePicker moveIn = new DatePicker(existing == null ? LocalDate.now() : existing.getMoveInDate());
        DatePicker moveOut = new DatePicker(existing == null ? null : existing.getMoveOutDate());
        moveIn.setMaxWidth(Double.MAX_VALUE);
        moveOut.setMaxWidth(Double.MAX_VALUE);
        Hyperlink clear = new Hyperlink("Still staying (clear move-out date)");
        clear.setOnAction(e -> moveOut.setValue(null));
        ChoiceBox<PaymentStatus> rent = statusBox(existing == null ? PaymentStatus.PENDING : existing.getRentStatus());
        ChoiceBox<PaymentStatus> util = statusBox(existing == null ? PaymentStatus.PENDING : existing.getUtilityStatus());

        Button save = UiKit.primary(existing == null ? "Add Tenant" : "Save Changes");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            String n = name.getText().trim();
            if (n.isEmpty() || moveIn.getValue() == null) {
                UiKit.error("Enter the tenant's name and move-in date.");
                return;
            }
            if (moveOut.getValue() != null && moveOut.getValue().isBefore(moveIn.getValue())) {
                UiKit.error("Move-out date can't be before move-in date.");
                return;
            }
            if (existing == null && moveOut.getValue() == null && occupied(room) >= room.getCapacity()
                    && !UiKit.confirm("This room is already at capacity. Add anyway?")) return;
            Tenant t = existing != null ? existing : new Tenant();
            t.setFullName(n);
            t.setMoveInDate(moveIn.getValue());
            t.setMoveOutDate(moveOut.getValue());
            t.setRentStatus(rent.getValue());
            t.setUtilityStatus(util.getValue());
            if (existing == null) room.getTenants().add(t);
            persist();
            drawer.hide();
            render();
        });
        drawer.show(existing == null ? "Add Tenant" : "Edit Tenant", new VBox(14,
                UiKit.field("Full name", name), UiKit.field("Move-in date", moveIn),
                UiKit.field("Move-out date (optional)", moveOut), clear,
                UiKit.field("Rent status", rent), UiKit.field("Utility status", util), save));
    }

    private void openAwayForm(Tenant t) {
        ListView<DateRange> list = new ListView<>(FXCollections.observableArrayList(t.getAwayPeriods()));
        list.setPrefHeight(170);
        list.setPlaceholder(new Label("No away periods."));
        DatePicker from = new DatePicker();
        DatePicker to = new DatePicker();
        from.setMaxWidth(Double.MAX_VALUE);
        to.setMaxWidth(Double.MAX_VALUE);

        Button add = UiKit.primary("Add away period");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> {
            if (from.getValue() == null || to.getValue() == null || to.getValue().isBefore(from.getValue())) {
                UiKit.error("Pick a valid start and end date (end on or after start).");
                return;
            }
            DateRange r = new DateRange(from.getValue(), to.getValue());
            t.getAwayPeriods().add(r);
            list.getItems().add(r);
            from.setValue(null);
            to.setValue(null);
            persist();
            render();
        });
        Button remove = UiKit.secondary("Remove selected");
        remove.setOnAction(e -> {
            DateRange sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            t.getAwayPeriods().remove(sel);
            list.getItems().remove(sel);
            persist();
            render();
        });
        Label hint = new Label("Days in these ranges are not billed for utilities.");
        hint.getStyleClass().add("hint");
        drawer.show("Away dates · " + t.getFullName(), new VBox(14, hint, list, remove,
                UiKit.field("Away from", from), UiKit.field("Back on", to), add));
    }

    private void deleteTenant(Tenant t) {
        if (!UiKit.confirm("Remove " + t.getFullName() + " from this room?")) return;
        currentRoom.getTenants().remove(t);
        persist();
        render();
    }

    private void exportPdf(Tenant t) {
        Optional<PdfExportService.Statement> st = pdf.buildStatement(currentLocation, t);
        if (st.isEmpty()) {
            UiKit.info("No utility bills have been logged for " + t.getFullName() + " yet.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save statement");
        fc.setInitialFileName(st.get().fileName());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(root.getScene().getWindow());
        if (f == null) return;
        try {
            pdf.export(currentLocation, currentRoom, t, st.get(), f.toPath());
            UiKit.info("Saved " + f.getName());
        } catch (Exception ex) {
            UiKit.error("Could not create the PDF: " + ex.getMessage());
        }
    }

    // ================================================================ bills overview

    private void renderBills() {
        List<BillRow> rows = new ArrayList<>();
        for (Location l : data.getLocations())
            for (BillRecord b : l.getBillHistory())
                if (matches(l.getName(), b.getType().name())) rows.add(new BillRow(l, b));
        rows.sort(Comparator.comparing((BillRow r) -> r.bill().getPeriodEnd()).reversed());

        TableView<BillRow> table = newTable();
        table.getColumns().addAll(List.of(
                UiKit.<BillRow>textCol("Location", r -> r.loc().getName(), 160),
                UiKit.<BillRow>textCol("Utility", r -> r.bill().getType() == UtilityType.ELECTRIC ? "Electric" : "Water", 90),
                UiKit.<BillRow>textCol("Period", r -> r.bill().getPeriodStart() + " → " + r.bill().getPeriodEnd(), 200),
                UiKit.<BillRow>textCol("Total Bill", r -> UiKit.peso(r.bill().getTotalBillPeso()), 120),
                UiKit.<BillRow>textCol("Tenants billed", r -> String.valueOf(r.bill().getTenantShares().size()), 110)));
        table.getItems().setAll(rows);
        installRows(table, this::showBillDetails, null, this::deleteBill, Map.of());
        setPage("Utilities & Bills", new Node[0], table);
    }

    private void showBillDetails(BillRow r) {
        TableView<TenantBillShare> shares = UiKit.shareTable();
        shares.getItems().setAll(r.bill().getTenantShares());
        Label l1 = new Label(r.loc().getName() + " · " + (r.bill().getType() == UtilityType.ELECTRIC ? "Electric" : "Water"));
        l1.getStyleClass().add("section-label");
        Label l2 = new Label(r.bill().getPeriodStart() + " → " + r.bill().getPeriodEnd()
                + "   |   Total " + UiKit.peso(r.bill().getTotalBillPeso()));
        drawer.show("Bill details", new VBox(12, l1, l2, shares));
    }

    private void deleteBill(BillRow r) {
        if (!UiKit.confirm("Delete this bill record? (Room meter readings are not rolled back.)")) return;
        r.loc().getBillHistory().remove(r.bill());
        persist();
        render();
    }

    // ================================================================ table plumbing

    private <T> TableView<T> newTable() {
        TableView<T> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        t.setPlaceholder(new Label("Nothing here yet."));
        VBox.setVgrow(t, Priority.ALWAYS);
        return t;
    }

    /**
     * onClick != null: single-click a row to drill in. onClick == null: double-click edits.
     * Right-click gives a context menu (Edit / extras / Delete).
     */
    private <T> void installRows(TableView<T> table, Consumer<T> onClick, Consumer<T> onEdit,
                                 Consumer<T> onDelete, Map<String, Consumer<T>> extras) {
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            if (onEdit != null) cm.getItems().add(menuItem("Edit", () -> onEdit.accept(row.getItem())));
            extras.forEach((label, action) -> cm.getItems().add(menuItem(label, () -> action.accept(row.getItem()))));
            if (onDelete != null) {
                if (!cm.getItems().isEmpty()) cm.getItems().add(new SeparatorMenuItem());
                cm.getItems().add(menuItem("Delete", () -> onDelete.accept(row.getItem())));
            }
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(cm));
            row.setOnMouseClicked(e -> {
                if (row.isEmpty() || e.getButton() != MouseButton.PRIMARY) return;
                if (onClick != null && e.getClickCount() == 1) onClick.accept(row.getItem());
                else if (onClick == null && e.getClickCount() == 2 && onEdit != null) onEdit.accept(row.getItem());
            });
            return row;
        });
    }

    private MenuItem menuItem(String text, Runnable action) {
        MenuItem m = new MenuItem(text);
        m.setOnAction(e -> action.run());
        return m;
    }

    private void setPage(String title, Node[] actions, Node table) {
        Label h = new Label(title);
        h.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, h, spacer);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getChildren().addAll(actions);

        VBox card = new VBox(table);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(4));
        VBox.setVgrow(card, Priority.ALWAYS);
        workspace.getChildren().addAll(head, card);
    }

    // ================================================================ small helpers

    private boolean matches(String... texts) {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        if (q.isEmpty()) return true;
        for (String t : texts) if (t != null && t.toLowerCase().contains(q)) return true;
        return false;
    }

    private int occupied(Room r) {
        LocalDate today = LocalDate.now();
        return (int) r.getTenants().stream().filter(t -> BillingCalculatorService.isActiveOn(t, today)).count();
    }

    private int occupied(Location l) { return l.getRooms().stream().mapToInt(this::occupied).sum(); }
    private int capacity(Location l) { return l.getRooms().stream().mapToInt(Room::getCapacity).sum(); }

    private List<Tenant> tenantsOf(Location l) {
        List<Tenant> all = new ArrayList<>();
        for (Room r : l.getRooms()) all.addAll(r.getTenants());
        return all;
    }

    /** "Paid" if every current tenant is paid, "Pending" otherwise, "—" if nobody lives there. */
    private String aggregate(Collection<Tenant> tenants, Function<Tenant, PaymentStatus> f) {
        LocalDate today = LocalDate.now();
        List<Tenant> active = tenants.stream().filter(t -> BillingCalculatorService.isActiveOn(t, today)).toList();
        if (active.isEmpty()) return "—";
        return active.stream().allMatch(t -> f.apply(t) == PaymentStatus.PAID) ? "Paid" : "Pending";
    }

    private String label(PaymentStatus s) { return s == PaymentStatus.PAID ? "Paid" : "Pending"; }
    private PaymentStatus flip(PaymentStatus s) { return s == PaymentStatus.PAID ? PaymentStatus.PENDING : PaymentStatus.PAID; }

    private ChoiceBox<PaymentStatus> statusBox(PaymentStatus value) {
        ChoiceBox<PaymentStatus> b = new ChoiceBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
        b.setValue(value);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }
}
