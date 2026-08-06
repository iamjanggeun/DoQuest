package com.doquest.domain.memo.entity;

import com.doquest.domain.member.entity.Member;
import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "memos",
        indexes = {
                @Index(name = "idx_memos_member", columnList = "member_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long id;

    //Quest 대신 Member와의 단방향 연관관계로 정정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "fk_memos_to_members"), nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // == 정적 생성 메서드 == //
    public static Memo createMemo(Member member, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메모 내용은 공백일 수 없습니다.");
        }
        Memo memo = new Memo();
        memo.member = member;
        memo.content = content;
        return memo;
    }

    // == 비즈니스 로직 == //
    /**
     * 메모 내용 수정
     */
    public void updateContent(String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("수정할 메모 내용은 공백일 수 없습니다.");
        }
        this.content = newContent;
    }
}