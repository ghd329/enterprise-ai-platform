package com.enterprise.platform.service;

import com.enterprise.platform.dto.DepartmentRequest;
import com.enterprise.platform.dto.DepartmentResponse;
import com.enterprise.platform.entity.Department;
import com.enterprise.platform.repository.DepartmentRepository;
import com.enterprise.platform.common.ResourceNotFoundException;
import com.enterprise.platform.common.DuplicateResourceException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // 부서 등록
    public DepartmentResponse createDepartment(DepartmentRequest request) {

        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("이미 존재하는 부서명입니다.");
        }

        Department department = new Department();

        department.setName(request.getName());
        department.setDescription(request.getDescription());

        Department savedDepartment = departmentRepository.save(department);

        return new DepartmentResponse(savedDepartment);
    }

    // 전체 부서 조회
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {

        return departmentRepository.findAll()
                .stream()
                .map(DepartmentResponse::new)
                .toList();
    }

    // 특정 부서 조회
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("부서를 찾을 수 없습니다."));

        return new DepartmentResponse(department);
    }

    // 부서 수정
    public DepartmentResponse updateDepartment(
            Long id,
            DepartmentRequest request) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("부서를 찾을 수 없습니다."));

        if (departmentRepository.existsByNameAndIdNot(
                request.getName(), id)) {
            throw new DuplicateResourceException("이미 존재하는 부서명입니다.");
        }

        department.setName(request.getName());
        department.setDescription(request.getDescription());

        return new DepartmentResponse(department);
    }

    // 부서 삭제
    public void deleteDepartment(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("부서를 찾을 수 없습니다."));

        departmentRepository.delete(department);
    }
}
