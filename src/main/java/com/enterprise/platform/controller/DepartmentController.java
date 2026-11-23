package com.enterprise.platform.controller;

import com.enterprise.platform.dto.DepartmentRequest;
import com.enterprise.platform.dto.DepartmentResponse;
import com.enterprise.platform.dto.UserSummaryResponse;
import com.enterprise.platform.service.DepartmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // 부서 등록 - 관리자만 가능
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(
            @Valid @RequestBody DepartmentRequest request) {

        return departmentService.createDepartment(request);
    }

    // 전체 부서 조회
    @GetMapping
    public List<DepartmentResponse> getAllDepartments() {

        return departmentService.getAllDepartments();
    }

    // 특정 부서 조회
    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Long id) {

        return departmentService.getDepartmentById(id);
    }

    // 부서 소속 회원 목록 조회
    @GetMapping("/{id}/users")
    public List<UserSummaryResponse> getDepartmentUsers(@PathVariable Long id) {

        return departmentService.getDepartmentUsers(id);
    }

    // 부서 수정 - 관리자만 가능
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {

        return departmentService.updateDepartment(id, request);
    }

    // 부서 삭제 - 관리자만 가능
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {

        departmentService.deleteDepartment(id);
    }
}
