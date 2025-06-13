package com.example.nav;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String login;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String dateOfBirth;
    private String studyPlace;
    private String phone;
    private String role;
    private List<Squad> squads;
    private String email;
    private String socialLink;
    private String passportSeries;
    private String passportNumber;
    private String passportIssuedBy;
    private String passportIssuedDate;

    public User(String login, String firstName, String lastName, String role, List<Squad> squads) {
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.squads = squads != null ? squads : new ArrayList<>();
    }

    public String getLogin() { return login; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getStudyPlace() { return studyPlace; }
    public void setStudyPlace(String studyPlace) { this.studyPlace = studyPlace; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<Squad> getSquads() { return squads; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSocialLink() { return socialLink; }
    public void setSocialLink(String socialLink) { this.socialLink = socialLink; }
    public String getPassportSeries() { return passportSeries; }
    public void setPassportSeries(String passportSeries) { this.passportSeries = passportSeries; }
    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public String getPassportIssuedBy() { return passportIssuedBy; }
    public void setPassportIssuedBy(String passportIssuedBy) { this.passportIssuedBy = passportIssuedBy; }
    public String getPassportIssuedDate() { return passportIssuedDate; }
    public void setPassportIssuedDate(String passportIssuedDate) { this.passportIssuedDate = passportIssuedDate; }
}