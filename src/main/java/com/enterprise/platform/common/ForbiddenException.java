package com.enterprise.platform.common;

// 인증은 되었지만 해당 리소스에 대한 권한이 없는 경우
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
