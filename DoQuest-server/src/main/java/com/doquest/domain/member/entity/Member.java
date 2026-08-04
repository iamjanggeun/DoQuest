package com.doquest.domain.member.entity;

import com.doquest.domain.pet.entity.Pet;
import com.doquest.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "members",
        indexes = {
                @Index(name = "idx_member_email", columnList = "email", unique = true)
        }
)

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 접근 제어
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;
    @Enumerated(EnumType.STRING) // Enum을 DB에 저장 시 STRING으로 저장 (ORDINAL X)
    @Column(nullable = false, length = 20)
    private Role role;

    // Member가 연관관계의 주인 (pet_id FK 보유)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "pet_id", foreignKey = @ForeignKey(name = "fk_members_to_pets"))
    private Pet pet;

    // == Factory Method == //
    public static Member createMember(String email, String password, String nickname, Pet pet) {
        Member member = new Member();
        member.email = email;
        member.password = password;
        member.nickname = nickname;
        member.role = Role.USER;
        member.pet = pet;
        return member;
    }
}