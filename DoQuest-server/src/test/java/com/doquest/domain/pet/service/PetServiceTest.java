package com.doquest.domain.pet.service;

import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.entity.Role;
import com.doquest.domain.pet.entity.Pet;
import com.doquest.domain.pet.repository.PetRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @InjectMocks
    private PetService petService;

    @Mock
    private PetRepository petRepository;

    @Test
    @DisplayName("회원 ID로 펫을 성공적으로 조회한다")
    void getPetByMemberId_success() {
        // given
        Long memberId = 1L;
        Pet pet = Pet.createDefaultPet("나비");
        Member member = Member.createMember("test@email.com", "pass", "닉네임", pet);
        given(petRepository.findByMemberId(memberId)).willReturn(Optional.of(pet));

        // when
        Pet result = petService.getPetByMemberId(memberId);

        // then
        assertThat(result.getName()).isEqualTo("나비");
        assertThat(result.getLevel()).isEqualTo(1);
        verify(petRepository).findByMemberId(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 펫 조회 시 PET_NOT_FOUND 예외가 발생한다")
    void getPetByMemberId_notFound() {
        // given
        Long memberId = 999L;
        given(petRepository.findByMemberId(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> petService.getPetByMemberId(memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PET_NOT_FOUND.getMessage());
    }
}