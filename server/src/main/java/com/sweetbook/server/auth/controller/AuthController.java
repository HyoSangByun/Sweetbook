package com.sweetbook.server.auth.controller;

import com.sweetbook.server.auth.dto.AuthTokenResponse;
import com.sweetbook.server.auth.dto.LoginRequest;
import com.sweetbook.server.auth.dto.MeResponse;
import com.sweetbook.server.auth.dto.SignupRequest;
import com.sweetbook.server.auth.service.AuthService;
import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.security.AppUserPrincipal;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(principal)));
    }
}

