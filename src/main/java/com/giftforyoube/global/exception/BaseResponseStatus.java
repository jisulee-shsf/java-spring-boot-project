package com.giftforyoube.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseResponseStatus {

    /**
     * 1. 요청이 성공한 경우(2000)
     */

    // 0. 공통
    SUCCESS(true, 2000, "요청에 성공하였습니다."),

    // 1-1. 회원가입 / 로그인 / 로그아웃 / 회원탈퇴
    REGISTER_ACCOUNT_SUCCESS(true, 2000, "회원가입이 완료되었습니다"),
    LOGIN_SUCCESS(true, 2000, "로그인이 완료되었습니다."),
    KAKAO_LOGIN_SUCCESS(true, 2000, "카카오 로그인이 완료되었습니다."),
    GOOGLE_LOGIN_SUCCESS(true, 2000, "구글 로그인이 완료되었습니다."),
    LOGOUT_SUCCESS(true, 2000, "로그아웃이 완료되었습니다."),
    DELETE_ACCOUNT_SUCCESS(true, 2000, "회원탈퇴가 완료되었습니다."),

    // 1-2 펀딩 관련
    FUNDING_ITEM_LINK_SUCCESS(true, 2000, "펀딩 아이템이 저장되었습니다."),

    // 1-3. 후원
    DONATION_READY_SUCCESS(true, 2000, "후원 결제 준비 요청이 완료되었습니다."),
    DONATION_APPROVE_SUCCESS(true, 2000, "후원 결제 승인 요청이 완료되었습니다."),

    /**
     * 2. 클라이언트 에러가 발생한 경우(4000)
     */

    // 0. 공통
    BAD_REQUEST(false, 4000, "잘못된 요청입니다."),

    // 2-1. 회원가입 / 로그인 / 로그아웃 / 회원탈퇴
    EMAIL_ALREADY_EXISTS(false, 4000, "이미 가입된 이메일입니다."),
    REGISTER_ACCOUNT_FAILED(false, 4000, "회원가입에 실패했습니다."),
    LOGIN_FAILED(false, 4000, "로그인에 실패했습니다."),
    KAKAO_LOGIN_FAILED(false, 4000, "카카오 로그인에 실패했습니다."),
    GOOGLE_LOGIN_FAILED(false, 4000, "구글 로그인에 실패했습니다."),
    USER_NOT_FOUND(false, 4000, "가입된 유저 정보가 없습니다."),
    PASSWORD_MISMATCH(false, 4000, "비밀번호가 일치하지 않습니다."),
    DELETE_ACCOUNT_FAILED(false, 4000, "회원탈퇴에 실패했습니다."),

    // 2-2. 인가 / 인증
    INVALID_BEARER_GRANT_TYPE(false, 4000, "Bearer 타입이 아닙니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND(false, 4000, "JWT 토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(false, 4000, "JWT 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(false, 4000, "JWT 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_FAILED(false, 4000, "인증에 실패했습니다", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(false, 4000, "리프레시 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_ISSUE_FAILED(false, 4000, "액세스 토큰 재발급에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USERDETAILS(false, 4000, "유저의 정보를 찾을 수 없습니다."),

    // 2-3. 알림
    NOTIFICATION_NOT_FOUND(false, 4000, "알림을 찾을 수 없습니다."),
    NOTIFICATION_NOT_READ(false, 4000, "알림을 읽지 못했습니다."),
    NOTIFICATION_NOT_DELETED(false, 4000, "알림을 삭제할 수 없습니다."),
    UNAUTHORIZED_READ_NOTIFICATION(false, 4000, "알림 조회 권한이 없습니다."),
    UNAUTHORIZED_DELETE_NOTIFICATION(false, 4000, "알림 삭제 권한이 없습니다."),
    READ_NOTIFICATION_LIST_NOT_FOUND(false, 4000, "읽은 메시지가 없습니다"),

    // 2-4. 펀딩
    UNAUTHORIZED_TO_ADD_LINK(false, 4000, "링크 추가 권한이 없습니다."),
    FUNDING_ITEM_ALREADY_EXISTS(false, 4000, "이미 등록된 펀딩 아이템입니다."),
    FUNDING_ITEM_PREVIEW_FAILED(false, 4000, "펀딩 아이템 미리보기에 실패했습니다."),
    FUNDING_ITEM_SAVE_FAILED(false, 5000, "펀딩 아이템 저장에 실패했습니다."),
    FUNDING_NOT_FOUND(false, 4000, "펀딩을 찾을 수 없습니다."),
    FUNDING_NOT_DELETED(false, 4000, "펀딩을 삭제할 수 없습니다."),
    UNAUTHORIZED_UPDATE_FUNDING(false, 4000, "펀딩 수정 권한이 없습니다."),
    UNAUTHORIZED_DELETE_FUNDING(false, 4000, "펀딩 삭제 권한이 없습니다."),
    UNAUTHORIZED_READ_FUNDING(false, 4000, "펀딩 조회 권한이 없습니다."),
    UNAUTHORIZED_FINISHED_FUNDING(false, 4000, "펀딩 종료 권한이 없습니다."),

    // 2-5. 후원
    DONATION_FAIL(false, 4000, "후원 결제에 실패했습니다."),
    DONATION_CANCEL(false, 4000, "후원 결제가 취소되었습니다."),

    /**
     * 3. 서버 에러가 발생한 경우(5000)
     */

    // 0. 공통
    INTERNAL_SERVER_ERROR(false, 5000, "서버 내부 에러가 발생했습니다."),
    UNEXPECTED_ERROR(false, 5000, "예상치 못한 에러가 발생했습니다."),
    FAIL_TO_ENCODING(false, 5000, "요청 인코딩에 실패했습니다."),
    FAIL_TO_JSON(false, 5000, "JSON 파싱 에러가 발생했습니다."),

    // 1. 이메일
    EMAIL_SEND_FAILED(false, 5000, "이메일 전송에 실패했습니다."),

    // 2. 알림
    NOTIFICATION_SEND_FAILED(false, 5000, "알림 전송에 실패했습니다."),

    // 3. 로그아웃
    LOGOUT_FAILED(false, 4000, "로그아웃에 실패했습니다."),
    UNAUTHORIZED_GET_NOTIFICATION(false, 5000, "알림을 읽을 권한이 업습니다.");

    private final boolean isSuccess;
    private final int code;
    private final String message;
    private HttpStatus httpStatus;

    BaseResponseStatus(boolean isSuccess, int code, String message) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }

    BaseResponseStatus(boolean isSuccess, int code, String message, HttpStatus httpStatus) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}