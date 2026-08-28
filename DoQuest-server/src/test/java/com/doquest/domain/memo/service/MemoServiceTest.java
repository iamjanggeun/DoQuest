package com.doquest.domain.memo.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.memo.dto.MemoResponse;
import com.doquest.domain.memo.entity.Memo;
import com.doquest.domain.memo.event.MemoCreatedEvent;
import com.doquest.domain.memo.repository.MemoRepository;
import com.doquest.domain.memo.repository.MemoAnalysisRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @InjectMocks
    private MemoService memoService;

    @Mock
    private MemoRepository memoRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemoAnalysisRepository memoAnalysisRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher; // 👈 비동기 이벤트 발행 검증용 Mock 주입

    @Test
    @DisplayName("정상적인 내용으로 메모를 생성하면 메모 ID가 반환되고 AI 파싱 이벤트가 발행된다")
    void createMemo_성공() {
        // given
        Long memberId = 1L;
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        ReflectionTestUtils.setField(member, "id", memberId);

        Memo memo = Memo.createMemo(member, "내일 2시 알고리즘 스터디");
        ReflectionTestUtils.setField(memo, "id", 100L);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memoRepository.save(any(Memo.class))).willReturn(memo);

        // when
        Long savedId = memoService.createMemo(memberId, "내일 2시 알고리즘 스터디");

        // then
        assertThat(savedId).isEqualTo(100L);
        assertThat(memo.isParsed()).isFalse();
        verify(memoRepository).save(any(Memo.class));
        verify(memoAnalysisRepository).save(any(com.doquest.domain.memo.entity.MemoAnalysis.class));

        // 👈 도메인 이벤트(MemoCreatedEvent)가 정상 발행되었는지 검증
        verify(eventPublisher).publishEvent(any(MemoCreatedEvent.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원이 메모 작성을 시도하면 MEMBER_NOT_FOUND 예외가 발생한다")
    void createMemo_회원없음_예외() {
        // given
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memoService.createMemo(memberId, "메모 내용"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원의 최신 메모 목록을 성공적으로 조회하여 DTO로 변환한다")
    void getMemosByMemberId_성공() {
        // given
        Long memberId = 1L;
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Memo memo1 = Memo.createMemo(member, "메모 1");
        Memo memo2 = Memo.createMemo(member, "메모 2");

        ReflectionTestUtils.setField(memo1, "id", 1L);
        ReflectionTestUtils.setField(memo2, "id", 2L);

        given(memberRepository.existsById(memberId)).willReturn(true);
        given(memoRepository.findByMemberIdOrderByCreatedAtDesc(memberId)).willReturn(List.of(memo2, memo1));

        // when
        List<MemoResponse> result = memoService.getMemosByMemberId(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(0).content()).isEqualTo("메모 2");
        assertThat(result.get(0).isParsed()).isFalse();

        assertThat(result.get(1).id()).isEqualTo(1L);
        assertThat(result.get(1).content()).isEqualTo("메모 1");
        assertThat(result.get(1).isParsed()).isFalse();

        verify(memoRepository).findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Test
    @DisplayName("타인의 메모를 수정하려고 하면 INVALID_INPUT_VALUE 예외가 발생한다")
    void updateMemo_타인메모_수정시도_예외() {
        // given
        Long myMemberId = 1L;
        Long ownerMemberId = 2L;
        Long memoId = 10L;

        Member owner = Member.createMember("owner@email.com", "pass", "주인", null);
        ReflectionTestUtils.setField(owner, "id", ownerMemberId);

        Memo memo = Memo.createMemo(owner, "타인의 메모 내용");

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when & then
        assertThatThrownBy(() -> memoService.updateMemo(myMemberId, memoId, "수정 시도"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_VALUE.getMessage());
    }

    @Test
    @DisplayName("AI 파싱 완료 시 isParsed 상태가 true로 변경된다")
    void completeParsing_성공() {
        // given
        Long memoId = 10L;
        Member member = Member.createMember("test@email.com", "pass", "닉네임", null);
        Memo memo = Memo.createMemo(member, "AI가 파싱할 메모");

        given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

        // when
        memoService.completeParsing(memoId);

        // then
        assertThat(memo.isParsed()).isTrue();
    }
}
