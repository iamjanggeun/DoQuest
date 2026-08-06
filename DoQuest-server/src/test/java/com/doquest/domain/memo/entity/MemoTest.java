package com.doquest.domain.memo.entity;

import com.doquest.domain.member.entity.Member;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class MemoTest {

    @Test
    @DisplayName("메모 생성 및 수정이 정상적으로 된다.")
    public void 메모생성테스트() throws Exception {
        //given
        Member member = Member.createMember("asd@naver.com", "password", "nick", null);
        //when
        Memo memo = Memo.createMemo(member, "스파이더맨 재밌다");
        memo.updateContent("톰스파가 최고의 스파이디다");
        //then
        assertThat(memo.getMember()).isEqualTo(member);
        assertThat(memo.getContent()).isEqualTo("톰스파가 최고의 스파이디다");
    }

    @Test
    @DisplayName("메모 생성을 빈 값으로 시도하면 IllegalArgumentException 예외가 발생한다.")
    public void 메모생성시공백테스트() throws Exception {
        //given
        Member member = Member.createMember("asd@naver.com", "pass", "nick", null);
        //when & then
        assertThatThrownBy(() -> Memo.createMemo(member, "     "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("메모 내용은 공백일 수 없습니다.");
    }

    @Test
    @DisplayName("메모 수정 시에도 빈 값은 예외가 발생한다.")
    public void 메모수정시공백테스트() throws Exception {
        //given
        Member member = Member.createMember("sdf@naver.com", "pass", "nick", null);
        Memo memo = Memo.createMemo(member, "gooooood");
        //when & then
        assertThatThrownBy(() -> memo.updateContent("    "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수정할 메모 내용은 공백일 수 없습니다.");
    }
}