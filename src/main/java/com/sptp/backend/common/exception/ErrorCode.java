package com.sptp.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    //공통 예외
    BAD_REQUEST_PARAM(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    BAD_REQUEST_VALIDATION(HttpStatus.BAD_REQUEST, "검증에 실패하였습니다."),

    //회원 예외
    UNAUTHORIZED_MEMBER(HttpStatus.UNAUTHORIZED, "해당 요청은 로그인이 필요합니다."),
    UNAUTHORIZED_PASSWORD(HttpStatus.UNAUTHORIZED, "패스워드가 틀립니다."),
    UNAUTHORIZED_ID(HttpStatus.UNAUTHORIZED, "아이디가 틀립니다."),
    EXIST_USER_ID(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
    EXIST_USER_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
    NOT_MATCH_PASSWORD(HttpStatus.BAD_REQUEST, "패스워드가 일치하지 않습니다."), // 삭제 예정
    NOT_FOUND_EMAIL(HttpStatus.NOT_FOUND, "가입되지 않은 이메일 입니다."),
    NOT_MATCH_USERNAME(HttpStatus.NOT_FOUND, "잘못된 이름입니다."),
    NOT_FOUND_KEYWORD(HttpStatus.NOT_FOUND, "해당 키워드를 찾을 수 없습니다."),

    //토큰 예외
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.NOT_FOUND, "토큰이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String detail;
}
