package com.doquest.domain.pet.repository;

import com.doquest.domain.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * Member가 Pet의 FK(pet_id)를 갖는 단방향 연관관계이므로
     * Member와 Join하여 해당 회원의 펫을 조회
     */
    @Query("SELECT m.pet FROM Member m WHERE m.id = :memberId")
    Optional<Pet> findByMemberId(@Param("memberId") Long memberId);
}


