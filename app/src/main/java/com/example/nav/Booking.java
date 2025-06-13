package com.example.nav;

import java.util.Objects;

public class Booking {
    private String squadName;
    private String date;
    private String startTime;
    private String endTime;
    private String purpose;
    private String responsible;
    private boolean notifyFighters;

    public Booking(String squadName, String date, String startTime, String endTime, String purpose, String responsible, boolean notifyFighters) {
        this.squadName = squadName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.responsible = responsible;
        this.notifyFighters = notifyFighters;
    }

    public String getSquadName() {
        return squadName;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getResponsible() {
        return responsible;
    }

    public boolean isNotifyFighters() {
        return notifyFighters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return notifyFighters == booking.notifyFighters &&
                Objects.equals(squadName, booking.squadName) &&
                Objects.equals(date, booking.date) &&
                Objects.equals(startTime, booking.startTime) &&
                Objects.equals(endTime, booking.endTime) &&
                Objects.equals(purpose, booking.purpose) &&
                Objects.equals(responsible, booking.responsible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(squadName, date, startTime, endTime, purpose, responsible, notifyFighters);
    }
}