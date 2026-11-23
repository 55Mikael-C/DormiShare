package com.DormiShare.service;


import com.DormiShare.model.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

// Receipt printer
// button on the side of the name of tenant to export and other things

public class PdfExportService {

    // color pallete - used later to build the doc
    private static final Color INK = new Color(0x0F, 0x17, 0x2A);
    private static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color LINE = new Color(0xE2, 0xE8, 0xF0);
    private static final Color SOFT = new Color(0xF8, 0xFA, 0xFC);
    private static final Color ACCENT = new Color(0x25, 0x63, 0xEB);


    // The latest billing period that includes this tenant, with its electric and water bills (either may be null).
    public record Statement(String fileName, LocalDate start, LocalDate end,
                            BillRecord electric, TenantBillShare electricShare,
                            BillRecord water, TenantBillShare waterShare) {}

    

    public Optional<Statement> buildStatement(Location loc, Tenant tenant) {
        /*Whats going to this tenant's receipt, no PDF yet.

            looks through the location's saved bills and ignores everything na walang share.
            pick yung bill na may latest end date. yun yung magiging  "period".
            finds the electric bill and the water bill for that exact period, using pick.
            It makes the file name, replace mga spaces and symbols with underscores so the name is safe.
            Return Optional para sa mga people na wlang nalolog na utility bills yet
        */
        BillRecord latest = null;
        for (BillRecord b : loc.getBillHistory()) {
            if (findShare(b, tenant) == null) continue;
            if (latest == null || b.getPeriodEnd().isAfter(latest.getPeriodEnd())) latest = b;
        }
        if (latest == null) return Optional.empty();

        LocalDate s = latest.getPeriodStart(), e = latest.getPeriodEnd();
        BillRecord el = pick(loc, tenant, UtilityType.ELECTRIC, s, e);
        BillRecord wa = pick(loc, tenant, UtilityType.WATER, s, e);
        String name = "Statement_" + tenant.getFullName().trim().replaceAll("[^A-Za-z0-9]+", "_")
                + "_" + s + "_to_" + e + ".pdf";
        return Optional.of(new Statement(name, s, e,
                el, el == null ? null : findShare(el, tenant),
                wa, wa == null ? null : findShare(wa, tenant)));
    }

    public void export(Location loc, Room room, Tenant tenant, Statement st, Path target) throws IOException {
        /*Export PDF
            counts the days in the period, inclusive para kunyare Nov 1 to Nov 30 is 30 days.
            It creates an A4 page with 48-point margins and opens the file at the place you chose.
            It writes the title ("DormiShare" and "Billing Statement").
            It adds the info table: Property, Room, Tenant, Billing period.
            It calls addUtility once for electricity and once for water.
            It builds the Summary table: rent + electric share + water share = TOTAL AMOUNT DUE. If a utility is missing, it counts as 0.
            It adds a small gray footnote and closes the document.
        */

        long periodDays = ChronoUnit.DAYS.between(st.start(), st.end()) + 1;
        Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
        try (OutputStream os = Files.newOutputStream(target)) {
            PdfWriter.getInstance(doc, os);
            doc.open();

            doc.add(new Paragraph("DormiShare", font(22, true, ACCENT)));
            doc.add(new Paragraph("Billing Statement", font(12, false, MUTED)));
            doc.add(new Paragraph(" ", font(8, false, MUTED)));

            PdfPTable info = kv();
            kvRow(info, "Property", loc.getName(), false);
            kvRow(info, "Room", room.getName(), false);
            kvRow(info, "Tenant", tenant.getFullName(), false);
            kvRow(info, "Billing period", st.start() + " to " + st.end() + " (" + periodDays + " days)", false);
            doc.add(info);

            addUtility(doc, "Electricity", "kWh", st.electric(), st.electricShare(), periodDays);
            addUtility(doc, "Water", "m3", st.water(), st.waterShare(), periodDays);

            double electric = st.electricShare() == null ? 0 : st.electricShare().getAmountPeso();
            double water = st.waterShare() == null ? 0 : st.waterShare().getAmountPeso();
            double rent = room.getMonthlyRent();

            doc.add(new Paragraph(" ", font(8, false, MUTED)));
            doc.add(new Paragraph("Summary", font(12, true, INK)));
            PdfPTable sum = kv();
            kvRow(sum, "Monthly rent", peso(rent), false);
            kvRow(sum, "Electric share", peso(electric), false);
            kvRow(sum, "Water share", peso(water), false);
            sum.addCell(cell("TOTAL AMOUNT DUE", font(12, true, INK), Element.ALIGN_LEFT, SOFT));
            sum.addCell(cell(peso(rent + electric + water), font(12, true, ACCENT), Element.ALIGN_RIGHT, SOFT));
            doc.add(sum);

            doc.add(new Paragraph(" ", font(8, false, MUTED)));
            doc.add(new Paragraph("Utility shares are computed from the exact amounts printed on the utility bills, "
                    + "split by the days each tenant was billable (days present minus days away). "
                    + "Generated on " + LocalDate.now() + ".", font(8, false, MUTED)));

            doc.close();
        } catch (DocumentException e) {
            throw new IOException("Could not build PDF", e);
        }
    }

    // ------------------------------------------------------------ sections

    private void addUtility(Document doc, String title, String unit, BillRecord b, TenantBillShare s, 
                            long periodDays) throws DocumentException {
        /* 
        Water and Electricity Section

        If there's no bill: it writes "No electricity bill was logged for this period." and stops.
        If hindi, add rows:
            Total property bill: the exact peso amount from the bill.
            Sub-metered properties only: the room's previous → current reading and units used, the total property consumption (all rooms added up), and the room's peso share.
            Your billable days: for example "27 of 30 days in period".
            Total tenant-days: the number everyone's days were divided by. The label says "in room" for sub-metered properties and "in property" otherwise.
            Your share: the tenant's final amount, in bold.
        */

        Paragraph h = new Paragraph(title, font(12, true, INK));
        h.setSpacingBefore(14);
        h.setSpacingAfter(4);
        doc.add(h);

        if (b == null || s == null) {
            doc.add(new Paragraph("No " + title.toLowerCase() + " bill was logged for this period.",
                    font(10, false, MUTED)));
            return;
        }
        PdfPTable t = kv();
        kvRow(t, "Total property bill", peso(b.getTotalBillPeso()), false);

        boolean subMetered = b.getRoomReadings().containsKey(s.getRoomId());
        if (subMetered) {
            double prev = b.getRoomPreviousReadings().getOrDefault(s.getRoomId(), 0.0);
            double curr = b.getRoomReadings().get(s.getRoomId());
            double propertyCons = 0;
            for (var e : b.getRoomReadings().entrySet())
                propertyCons += e.getValue() - b.getRoomPreviousReadings().getOrDefault(e.getKey(), 0.0);
            kvRow(t, "Room sub-meter reading",
                    fmt(prev) + " to " + fmt(curr) + " (" + fmt(curr - prev) + " " + unit + ")", false);
            kvRow(t, "Total property consumption", fmt(propertyCons) + " " + unit, false);
            kvRow(t, "Room share", peso(s.getRoomPesoShare()), false);
        }
        kvRow(t, "Your billable days", s.getBillableDays() + " of " + periodDays + " days in period", false);
        kvRow(t, subMetered ? "Total tenant-days in room" : "Total tenant-days in property",
                String.valueOf(s.getTotalBasisDays()), false);
        kvRow(t, "Your " + title.toLowerCase() + " share", peso(s.getAmountPeso()), true);
        doc.add(t);

    }

    // ------------------------------------------------------------ helpers

    private BillRecord pick(Location loc, Tenant tenant, UtilityType type, LocalDate s, LocalDate e) {
        // finds the bill of a given type (electric or water) for an exact period that includes this tenant. It checks the bills one by one and returns the first match, or null or ("nothing") if walang matches.
        for (BillRecord b : loc.getBillHistory()) {
            if (b.getType() == type && s.equals(b.getPeriodStart()) && e.equals(b.getPeriodEnd())
                    && findShare(b, tenant) != null) return b;
        }
        return null;   
    }

    private TenantBillShare findShare(BillRecord b, Tenant tenant) {
        // inside one bill, finds this tenant's line. It compares the tenant's UUID against each line's tenant ID (this is where those IDs are used) and returns the matching line, or null.
        for (TenantBillShare s : b.getTenantShares())
            if (tenant.getId().equals(s.getTenantId())) return s;
        return null;
    }

    private PdfPTable kv() throws DocumentException {
        // empty two-column table, left - labels , right - values. It takes the full page width, and the left column is a bit wider (3 to 2.4). "kv" ay key-value, like a label and its value.
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{3f, 2.4f});
        return t;
    }

    private void kvRow(PdfPTable t, String label, String value, boolean bold) {
        // adds one row to a table: the label on the left and the value on the right. Normal rows use gray label text. If bold is true, both sides turn dark, and the value turns bold.
        t.addCell(cell(label, font(10, bold, bold ? INK : MUTED), Element.ALIGN_LEFT, null));
        t.addCell(cell(value, font(10, bold, INK), Element.ALIGN_RIGHT, null));
    }

    private PdfPCell cell(String text, Font f, int align, Color bg) {
        // builds a single table cell. It sets the text, the alignment (left or right), a thin light line along the bottom only, 6 points of inner padding and, optionally, a background color. The Total Due row uses the pale background.
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(LINE);
        c.setBorderWidth(0.5f);
        c.setPadding(6);
        if (bg != null) c.setBackgroundColor(bg);
        return c;

    }

    private Font font(float size, boolean bold, Color color) {
        // makes a font in Helvetica with the given size, weight and color. It exists so the rest of the code can say font(10, false, MUTED) imbes na mahabang library call.
        return FontFactory.getFont(FontFactory.HELVETICA, size, bold ? Font.BOLD : Font.NORMAL, color);
    }

    private String peso(double v) {
        // formats money for the PDF
        return "PHP " + String.format("%,.2f", v);
    }

    private String fmt(double v) {
        // formats meter readings and units
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }

}
