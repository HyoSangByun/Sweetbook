package com.sweetbook.server.auth.controller;

import com.sweetbook.server.auth.dto.AuthTokenResponse;
import com.sweetbook.server.auth.dto.LoginRequest;
import com.sweetbook.server.auth.dto.MeResponse;
import com.sweetbook.server.auth.dto.SignupRequest;
import com.sweetbook.server.auth.service.AuthService;
import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원가입, 로그인, 내 정보 조회 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다.")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호 로그인 후 JWT를 발급합니다.")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 인증된 사용자 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(principal)));
    }
}

