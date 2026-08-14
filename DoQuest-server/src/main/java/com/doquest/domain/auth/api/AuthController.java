package com.doquest.domain.auth.api;

import com.doquest.domain.auth.dto.LoginRequest;
import com.doquest.domain.auth.dto.SignUpRequest;
import com.doquest.domain.auth.dto.TokenResponse;
import com.doquest.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 신규 회원가입 (201 Created)
     */
    @PostMapping("/signup")
    public ResponseEntity<Long> signUp(@Valid @RequestBody SignUpRequest request) {
        Long memberId = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberId);
    }

    /**
     * 로그인 & JWT 발급 (200 OK)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}