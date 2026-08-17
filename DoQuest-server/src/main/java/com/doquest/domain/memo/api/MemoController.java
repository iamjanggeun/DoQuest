package com.doquest.domain.memo.api;

import com.doquest.domain.memo.dto.MemoCreateRequest;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.dto.MemoUpdateRequest;
import com.doquest.domain.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    /**
     * 메모 생성 (201 Created -> 생성된 memoId 반환)
     */
    @PostMapping
    public ResponseEntity<Long> createMemo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemoCreateRequest request) {

        Long memoId = memoService.createMemo(memberId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(memoId);
    }

    /**
     * 회원의 최신 메모 목록 조회 (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<MemoResponse>> getMemos(
            @AuthenticationPrincipal Long memberId) {

        List<MemoResponse> responses = memoService.getMemosByMemberId(memberId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 메모 내용 수정 (200 OK)
     */
    @PatchMapping("/{memoId}")
    public ResponseEntity<Void> updateMemo(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long memoId,
            @Valid @RequestBody MemoUpdateRequest request) {

        memoService.updateMemo(memberId, memoId, request.content());
        return ResponseEntity.ok().build();
    }

    /**
     * 메모 삭제 (204 No Content)
     */
    @DeleteMapping("/{memoId}")
    public ResponseEntity<Void> deleteMemo(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long memoId) {

        memoService.deleteMemo(memberId, memoId);
        return ResponseEntity.noContent().build();
    }
}