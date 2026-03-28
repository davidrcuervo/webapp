package com.laetienda.model.kc;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import net.minidev.json.annotate.JsonIgnore;

public class KcUser {

    @NotNull private String id;
    @NotNull private String username;

    @Email
    @NotNull private String email;

    private boolean emailVerified;
    private String firstName;
    private String lastName;
    private boolean enabled;

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonIgnore
    public String getFullName(){
        return getFirstName() + " " + getLastName();
    }
}
