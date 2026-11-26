package com.DormiShare.service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;

import com.DormiShare.model.*;
import com.DormiShare.util.LocalDateAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonStorageService {
    
        public static final String DATA_FILE = "dormishare_data.json";

    private final Path path;
    private final Gson gson;
    private AppData data;

    public JsonStorageService() {
        this(Paths.get(DATA_FILE));
    }

    public JsonStorageService(Path path) {
        this.path = path;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();
    }

    /** Loads from disk; seeds and saves sample data if the file doesn't exist or is empty/corrupt. */
    public AppData load() {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                AppData loaded = gson.fromJson(reader, AppData.class);
                if (loaded != null && loaded.getLocations() != null) {
                    data = loaded;
                    return data;
                }
            } catch (Exception e) {
                // Keep the unreadable file as a backup instead of silently destroying it.
                try {
                    Files.move(path, path.resolveSibling(DATA_FILE + ".corrupt"),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) { }
            }
        }
        data = createSampleData();
        save();
        return data;
    }

    public AppData getData() {
        if (data == null) load();
        return data;
    }

    /** write to a temp file, tas saka move over the real one. */
    public void save() {
        if (data == null) return;
        Path tmp = path.resolveSibling(DATA_FILE + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + tmp, e);
        }
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to save " + path, ex);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save " + path, e);
        }
    }

    public Path getPath() { return path; }

    // ---------------------------------------------------------------- sample data

    private AppData createSampleData() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        AppData app = new AppData();

        // Location 1: electric sub-meters only
        Location kuya = new Location("Bahay ni Kuya", true, false);

        Room pearl = new Room("Pearl", 4, 2500);
        pearl.setLastElectricReading(1200);
        Tenant juan = new Tenant("Juan Dela Cruz", today.minusMonths(6), null);
        Tenant maria = new Tenant("Maria Santos", today.minusMonths(4), null);
        // Maria went home for a week this month (not billed for utilities)
        maria.getAwayPeriods().add(new DateRange(firstOfMonth.plusDays(9), firstOfMonth.plusDays(15)));
        maria.setRentStatus(PaymentStatus.PAID);
        Tenant pedro = new Tenant("Pedro Reyes", firstOfMonth.plusDays(10), null); // mid-month move-in
        pearl.getTenants().add(juan);
        pearl.getTenants().add(maria);
        pearl.getTenants().add(pedro);

        Room garnet = new Room("Garnet", 3, 3000);
        garnet.setLastElectricReading(800);
        Tenant ana = new Tenant("Ana Villanueva", today.minusMonths(8), null);
        ana.setRentStatus(PaymentStatus.PAID);
        ana.setUtilityStatus(PaymentStatus.PAID);
        Tenant liza = new Tenant("Liza Bautista", today.minusMonths(2), null);
        garnet.getTenants().add(ana);
        garnet.getTenants().add(liza);

        kuya.getRooms().add(pearl);
        kuya.getRooms().add(garnet);

        // Location 2: no sub-meters (pure tenant-day proration)
        Location compound = new Location("Compound", false, false);

        Room unitA = new Room("Unit A", 2, 3500);
        unitA.getTenants().add(new Tenant("Carlo Mendoza", today.minusMonths(5), null));
        unitA.getTenants().add(new Tenant("Jomar Aquino", today.minusMonths(3), null));

        Room unitB = new Room("Unit B", 4, 2000);
        Tenant rico = new Tenant("Rico Garcia", today.minusYears(1), null);
        rico.setRentStatus(PaymentStatus.PAID);
        Tenant nina = new Tenant("Nina Cruz", today.minusMonths(7), firstOfMonth.plusDays(19)); // moves out mid-month
        unitB.getTenants().add(rico);
        unitB.getTenants().add(nina);

        compound.getRooms().add(unitA);
        compound.getRooms().add(unitB);

        app.getLocations().add(kuya);
        app.getLocations().add(compound);
        return app;
    }
}
