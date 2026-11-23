package com.enterprise.platform.dto;

import jakarta.validation.constraints.NotNull;

// 관리자가 회원의 소속 부서를 지정할 때 사용하는 요청
public class UserDepartmentRequest {

    @NotNull(message = "부서 ID는 필수 입력값입니다.")
    private Long departmentId;

    public UserDepartmentRequest() {}

    public Long getDepartmentId() { return departmentId; }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
