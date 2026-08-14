package com.doquest.domain.auth.service;

import com.doquest.domain.auth.dto.LoginRequest;
import com.doquest.domain.auth.dto.SignUpRequest;
import com.doquest.domain.auth.dto.TokenResponse;
import com.doquest.domain.member.entity.Member;
import com.doquest.domain.member.repository.MemberRepository;
import com.doquest.domain.pet.entity.Pet;
import com.doquest.global.config.security.JwtProvider;
import com.doquest.global.error.BusinessException;
import com.doquest.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입
     * 1. 이메일 중복 체크 (DUPLICATE_EMAIL -> M002)
     * 2. 비밀번호 단방향 암호화
     * 3. 기본 펫 생성 및 회원 영속화 (Cascade)
     */
    @Transactional
    public Long signUp(SignUpRequest request) {
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Pet defaultPet = Pet.createDefaultPet(request.petName());
        Member member = Member.createMember(
                request.email(),
                encodedPassword,
                request.nickname(),
                defaultPet
        );

        Member savedMember = memberRepository.save(member);
        return savedMember.getId();
    }

    /**
     * 로그인
     * 1. 회원 조회 및 비밀번호 일치 검증 (통합 에러 INVALID_LOGIN_CREDENTIALS -> A001)
     * 2. JWT Access Token 발급
     */
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .filter(m -> passwordEncoder.matches(request.password(), m.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        String accessToken = jwtProvider.createToken(member.getId());
        return TokenResponse.of(accessToken);
    }
}