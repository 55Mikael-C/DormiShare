package com.DormiShare.model;

import java.time.LocalDate;

/** Inclusive date range (e.g., a period the tenant was away). */
public class DateRange {
    private LocalDate start;
    private LocalDate end;

    public DateRange() {}

    public DateRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate getStart() { return start; }
    public void setStart(LocalDate start) { this.start = start; }
    public LocalDate getEnd() { return end; }
    public void setEnd(LocalDate end) { this.end = end; }

    @Override
    public String toString() { return start + " → " + end; }
}