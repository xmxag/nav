package com.example.nav;

import java.util.Objects;

public class Squad {
    private String name;
    private String direction;
    private String description;

    public Squad(String name, String direction, String description) {
        this.name = name;
        this.direction = direction;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Squad squad = (Squad) o;
        return Objects.equals(name, squad.name) &&
                Objects.equals(direction, squad.direction) &&
                Objects.equals(description, squad.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, direction, description);
    }
}