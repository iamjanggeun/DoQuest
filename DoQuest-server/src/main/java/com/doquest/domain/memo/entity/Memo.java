package com.doquest.domain.memo.entity;

import com.doquest.domain.member.entity.Member;
import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(
        name = "memos",
        indexes = {
                // 특정 회원의 메모 목록 최신순 조회 인덱스 최적화
                @Index(name = "idx_memos_member_created", columnList = "member_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "memo_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_memos_to_members"), nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isParsed; // AI 파싱 완료 여부

    // == 정적 생성 메서드 == //
    public static Memo createMemo(Member member, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메모 내용은 공백일 수 없습니다.");
        }
        Memo memo = new Memo();
        memo.member = member;
        memo.content = content;
        memo.isParsed = false;
        return memo;
    }

    // == 비즈니스 로직 == //
    public void updateContent(String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("수정할 메모 내용은 공백일 수 없습니다.");
        }
        this.content = newContent;
    }

    public void markAsParsed() {
        this.isParsed = true;
    }

    public void resetParsed() {
        this.isParsed = false;
    }
}
