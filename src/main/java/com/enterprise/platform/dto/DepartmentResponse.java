package com.enterprise.platform.dto;

import com.enterprise.platform.entity.Department;
import com.enterprise.platform.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public class DepartmentResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private List<UserSummaryResponse> users;

    public DepartmentResponse(Department department) {
        this.id = department.getId();
        this.name = department.getName();
        this.description = department.getDescription();
        this.createdAt = department.getCreatedAt();

        this.users = department.getUsers()
                .stream()
                .map(UserSummaryResponse::new)
                .toList();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<UserSummaryResponse> getUsers() { return users; }
}