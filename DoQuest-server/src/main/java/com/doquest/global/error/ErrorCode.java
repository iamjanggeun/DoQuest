package com.doquest.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "적절하지 않은 요청 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    //Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 존재하는 이메일입니다."),

    //Quest
    QUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "존재하지 않는 퀘스트입니다."),
    QUEST_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "Q002", "이미 완료된 퀘스트입니다."),
    DUPLICATE_QUEST_EXISTS(HttpStatus.CONFLICT, "Q003", "유사한 퀘스트가 이미 존재합니다."),

    // Pet
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 회원의 펫을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
