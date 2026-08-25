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

    // Quest - Abusing Guardrail (26.08.10 추가)
    QUEST_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "Q004", "진행 중인 퀘스트만 완료 처리할 수 있습니다."),
    QUEST_COMPLETE_TOO_FAST(HttpStatus.BAD_REQUEST, "Q005", "퀘스트 시작 후 최소 30분이 지나야 완료할 수 있습니다."),

    // Pet
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 회원의 펫을 찾을 수 없습니다."),

    // Auth & Security
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // Schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "존재하지 않는 일정입니다."),
    INVALID_SCHEDULE_DATE(HttpStatus.BAD_REQUEST, "S002", "유효하지 않은 일정 날짜 형식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
