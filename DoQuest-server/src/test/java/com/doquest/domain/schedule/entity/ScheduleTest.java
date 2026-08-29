package com.doquest.domain.schedule.entity;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.memo.entity.Memo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Schedule 도메인 단위 테스트")
class ScheduleTest {

    private Member createTestMember() {
        Member member = Member.createMember("test@test.com", "password", "testUser", null);
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Memo createTestMemo(Member member) {
        Memo memo = Memo.createMemo(member, "이번주 금요일 코딩테스트 응시");
        ReflectionTestUtils.setField(memo, "id", 100L);
        return memo;
    }

    @Nested
    @DisplayName("일정 생성(createSchedule) 테스트")
    class CreateScheduleTest {

        @Test
        @DisplayName("[성공] 필수 정보와 메모 연관관계를 포함해 일정을 정상 생성한다.")
        void createSchedule_Success() {
            // given
            Member member = createTestMember();
            Memo memo = createTestMemo(member);
            String title = "코딩테스트";
            LocalDate scheduledAt = LocalDate.of(2026, 8, 28);
            String location = "온라인";
            String summaryInfo = "프로그래머스 환경";

            // when
            Schedule schedule = Schedule.createSchedule(member, memo, title, scheduledAt, location, summaryInfo);

            // then
            assertThat(schedule.getMember()).isEqualTo(member);
            assertThat(schedule.getMemo()).isEqualTo(memo);
            assertThat(schedule.getTitle()).isEqualTo(title);
            assertThat(schedule.getScheduledAt()).isEqualTo(scheduledAt);
            assertThat(schedule.getLocation()).isEqualTo(location);
            assertThat(schedule.getSummaryInfo()).isEqualTo(summaryInfo);
            assertThat(schedule.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("[성공] 메모 연관관계가 없는 수동 등록 일정(memo=null)도 정상 생성된다.")
        void createSchedule_WithoutMemo_Success() {
            // given
            Member member = createTestMember();

            // when
            Schedule schedule = Schedule.createSchedule(
                    member, null, "운동하기", LocalDate.of(2026, 8, 28), "헬스장", "하체 루틴"
            );

            // then
            assertThat(schedule.getMemo()).isNull();
            assertThat(schedule.getTitle()).isEqualTo("운동하기");
        }

        @Test
        @DisplayName("[예외] 제목이 null이거나 공백이면 IllegalArgumentException이 발생한다.")
        void createSchedule_NullOrEmptyTitle_ThrowsException() {
            // given
            Member member = createTestMember();
            LocalDate scheduledAt = LocalDate.of(2026, 8, 28);

            // when & then
            assertThatThrownBy(() -> Schedule.createSchedule(member, null, "", scheduledAt, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("일정 제목은 필수 입력값입니다.");
        }

        @Test
        @DisplayName("[예외] 일정 날짜가 null이면 IllegalArgumentException이 발생한다.")
        void createSchedule_NullDate_ThrowsException() {
            // given
            Member member = createTestMember();

            // when & then
            assertThatThrownBy(() -> Schedule.createSchedule(member, null, "일정 제목", null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("일정 날짜는 필수 입력값입니다.");
        }
    }

    @Nested
    @DisplayName("일정 상태 및 정보 변경 테스트")
    class UpdateAndToggleTest {

        @Test
        @DisplayName("[성공] changeCompletion 호출 시 요청한 완료 상태로 변경된다.")
        void changeCompletion_Success() {
            // given
            Member member = createTestMember();
            Schedule schedule = Schedule.createSchedule(
                    member, null, "자격증 시험", LocalDate.of(2026, 9, 1), null, null
            );

            // when & then
            assertThat(schedule.isCompleted()).isFalse();

            schedule.changeCompletion(true);
            assertThat(schedule.isCompleted()).isTrue();

            schedule.changeCompletion(false);
            assertThat(schedule.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("[성공] update 메서드로 일정 정보를 수정한다.")
        void update_Success() {
            // given
            Member member = createTestMember();
            Schedule schedule = Schedule.createSchedule(
                    member, null, "원래 제목", LocalDate.of(2026, 9, 1), "강남", "메모 요약"
            );

            // when
            schedule.update("변경된 제목", LocalDate.of(2026, 9, 2), "판교", "수정된 요약");

            // then
            assertThat(schedule.getTitle()).isEqualTo("변경된 제목");
            assertThat(schedule.getScheduledAt()).isEqualTo(LocalDate.of(2026, 9, 2));
            assertThat(schedule.getLocation()).isEqualTo("판교");
            assertThat(schedule.getSummaryInfo()).isEqualTo("수정된 요약");
        }
    }
}
