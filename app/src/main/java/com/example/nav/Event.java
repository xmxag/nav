package com.example.nav;

import java.util.Objects;

public class Event {
    private String name;
    private String location;
    private String date;
    private String startTime;
    private String endTime;
    private String squadName;
    private String responsible;

    public Event(String name, String location, String date, String startTime, String endTime, String squadName, String responsible) {
        this.name = name;
        this.location = location;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.squadName = squadName;
        this.responsible = responsible;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
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

    public String getSquadName() {
        return squadName;
    }

    public String getResponsible() {
        return responsible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(name, event.name) &&
                Objects.equals(location, event.location) &&
                Objects.equals(date, event.date) &&
                Objects.equals(startTime, event.startTime) &&
                Objects.equals(endTime, event.endTime) &&
                Objects.equals(squadName, event.squadName) &&
                Objects.equals(responsible, event.responsible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location, date, startTime, endTime, squadName, responsible);
    }
}