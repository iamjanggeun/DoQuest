package com.doquest.domain.pet.repository;

import com.doquest.domain.member.entity.Member;

import java.util.Optional;

public interface PetRepository {
    Optional<Member> findByMemberId(Long memberId);
}


