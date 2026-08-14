package com.doquest.domain.memo.api;

import com.doquest.domain.memo.dto.MemoCreateRequest;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.dto.MemoUpdateRequest;
import com.doquest.domain.memo.entity.Memo;
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
     * 자유 메모 등록
     */
    @PostMapping
    public ResponseEntity<MemoResponse> createMemo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemoCreateRequest request) {

        Long memoId = memoService.createMemo(memberId, request.content());

        // 메모 단건 생성 결과 DTO 조립하여 201 Created 응답
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MemoResponse(memoId, request.content(), false, null));
    }

    /**
     * 회원의 최신 메모 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<MemoResponse>> getMemos(
            @AuthenticationPrincipal Long memberId
    ) {

        List<Memo> memos = memoService.getMemosByMemberId(memberId);
        List<MemoResponse> response = memos.stream()
                .map(MemoResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 메모 내용 수정
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
     * 메모 삭제
     */
    @DeleteMapping("/{memoId}")
    public ResponseEntity<Void> deleteMemo(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long memoId) {

        memoService.deleteMemo(memberId, memoId);
        return ResponseEntity.ok().build();
    }
}