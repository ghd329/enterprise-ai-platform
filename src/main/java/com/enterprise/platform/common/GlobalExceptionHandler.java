
package com.enterprise.platform.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authorization.AuthorizationDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 회원을 찾을 수 없는 경우
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {

        ApiErrorResponse response =
                new ApiErrorResponse(404, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    // 중복 데이터가 발생한 경우
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateResourceException ex) {

        ApiErrorResponse response =
                new ApiErrorResponse(409, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // 잘못된 입력값인 경우
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException ex) {

        ApiErrorResponse response =
                new ApiErrorResponse(400, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 잘못된 입력값 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        ApiErrorResponse response = new ApiErrorResponse(400, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 로그인 인증 실패
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException e) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        e.getMessage()
                ));
    }

    // 리소스 접근 권한 없음 (예: 다른 사람의 문서 수정)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            ForbiddenException e) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        e.getMessage()
                ));
    }

    // 권한 부족
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException e) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        "접근 권한이 없습니다."
                ));
    }

    // 그 외 예상하지 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "서버 내부 오류가 발생했습니다."
                ));
    }
}
