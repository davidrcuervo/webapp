package com.laetienda.model.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
public class ItemTypeA extends DbItem{

    @Min(18)
    private Integer age;
    private String address;

    @NotNull
    @Column(unique = true, nullable = false)
    private String username;

    public ItemTypeA(){

    }

    public ItemTypeA(String username, Integer age, String address) {
        this.age = age;
        this.address = address;
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public String getAddress() {
        return address;
    }

    public String getUsername() {
        return username;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
