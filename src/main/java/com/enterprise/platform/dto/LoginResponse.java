package com.enterprise.platform.dto;

import com.enterprise.platform.entity.User;

public class LoginResponse {

    private String token;
    private Long id;
    private String name;
    private String email;
    private String role;

    public LoginResponse(User user, String token) {
        this.token = token;
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    public String getToken() {
        return token;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}