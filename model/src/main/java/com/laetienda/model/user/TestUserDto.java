package com.laetienda.model.user;

public class TestUserDto {

    public String userId;
    public String token;

    public TestUserDto(String username, String token) {
        this.userId = username;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String username) {
        this.userId = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
