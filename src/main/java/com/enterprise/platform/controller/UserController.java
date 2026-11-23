
package com.enterprise.platform.controller;

import com.enterprise.platform.dto.UserDepartmentRequest;
import com.enterprise.platform.dto.UserRequest;
import com.enterprise.platform.dto.UserResponse;
import com.enterprise.platform.dto.UserUpdateRequest;
import com.enterprise.platform.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 회원 등록 - 누구나 가능
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody UserRequest request) {

        return userService.createUser(request);
    }

    // 전체 회원 조회 - 관리자만 가능
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {

        return userService.getAllUsers();
    }

    // 회원 상세 조회 - 본인 또는 관리자 가능
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isOwnerOrAdmin(#p0, authentication.name)")
    public UserResponse getUserById(@PathVariable Long id) {

        return userService.getUserById(id);
    }

    // 회원 정보 수정 - 본인 또는 관리자 가능
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isOwnerOrAdmin(#p0, authentication.name)")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {

        return userService.updateUser(id, request);
    }

    // 회원 소속 부서 지정/변경 - 관리자만 가능
    @PutMapping("/{id}/department")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assignDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UserDepartmentRequest request) {

        return userService.assignDepartment(id, request.getDepartmentId());
    }

    // 회원 소속 부서 해제 - 관리자만 가능
    @DeleteMapping("/{id}/department")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse removeDepartment(@PathVariable Long id) {

        return userService.removeDepartment(id);
    }

    // 회원 삭제 - 관리자만 가능
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);
    }
}
