package com.doquest.domain.pet.repository;

import com.doquest.domain.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByMemberId(Long memberId);
}


