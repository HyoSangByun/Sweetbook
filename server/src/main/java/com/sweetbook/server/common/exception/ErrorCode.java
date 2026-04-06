package com.sweetbook.server.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_004", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_005", "사용자를 찾을 수 없습니다."),
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTIVITY_001", "운동 기록을 찾을 수 없습니다."),
    CSV_IMPORT_FAILED(HttpStatus.BAD_REQUEST, "ACTIVITY_002", "CSV 적재 중 오류가 발생했습니다."),
    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALBUM_001", "앨범을 찾을 수 없습니다."),
    ALBUM_ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ALBUM_002", "앨범에서 선택된 운동 기록을 찾을 수 없습니다."),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "PHOTO_001", "사진을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "FILE_001", "파일 처리 중 오류가 발생했습니다."),
    SWEETBOOK_CALL_FAILED(HttpStatus.BAD_GATEWAY, "SWEETBOOK_001", "Sweetbook API 호출에 실패했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

