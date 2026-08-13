package com.doquest.domain.memo.dto;

import jakarta.validation.constraints.NotBlank;

public record MemoCreateRequest(
        @NotBlank(message = "메모 내용은 공백일 수 없습니다.")
        String content
) {
}
