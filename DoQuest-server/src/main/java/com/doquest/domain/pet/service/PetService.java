package com.doquest.domain.pet.service;

import com.doquest.domain.pet.entity.Pet;
import com.doquest.domain.pet.repository.PetRepository;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;

    /*
    * 회원 아이디로 펫 단건 조회 (대시보드 서빙용)
    * */
    public Pet getPetByMemberId(Long memberId) {
        return petRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_NOT_FOUND));
    }
}
