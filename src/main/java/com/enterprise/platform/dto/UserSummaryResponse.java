package com.enterprise.platform.dto;

import com.enterprise.platform.entity.User;

public class UserSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String role;

    public UserSummaryResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}