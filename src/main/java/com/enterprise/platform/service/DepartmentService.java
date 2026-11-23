package com.enterprise.platform.service;

import com.enterprise.platform.dto.DepartmentRequest;
import com.enterprise.platform.dto.DepartmentResponse;
import com.enterprise.platform.dto.UserSummaryResponse;
import com.enterprise.platform.entity.Department;
import com.enterprise.platform.repository.DepartmentRepository;
import com.enterprise.platform.repository.UserRepository;
import com.enterprise.platform.repository.DocumentRepository;
import com.enterprise.platform.common.ResourceNotFoundException;
import com.enterprise.platform.common.DuplicateResourceException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
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

    // 부서 소속 회원 목록 조회
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getDepartmentUsers(Long id) {

        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("부서를 찾을 수 없습니다.");
        }

        return userRepository.findByDepartment_IdOrderByNameAsc(id)
                .stream()
                .map(UserSummaryResponse::new)
                .toList();
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

        // 소속 회원이 있는 경우 삭제 방지
        if (userRepository.existsByDepartment_Id(id)) {
            throw new DuplicateResourceException(
                    "소속 회원이 있는 부서는 삭제할 수 없습니다."
            );
        }

        // 소속 문서가 있는 경우 삭제 방지
        if (documentRepository.existsByDepartment_Id(id)) {
            throw new DuplicateResourceException(
                    "소속 문서가 있는 부서는 삭제할 수 없습니다."
            );
        }

        departmentRepository.delete(department);
    }
}