package com.prography.backend.domain.auth.controller;

import com.prography.backend.domain.auth.dto.AuthRequestDTO;
import com.prography.backend.domain.auth.dto.AuthResponseDTO;
import com.prography.backend.global.common.response.ApiResponse;
import com.prography.backend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponseDTO.LoginResultDTO> login(@Valid @RequestBody AuthRequestDTO.LoginRequestDTO request) {
        return ApiResponse.success(authService.login(request.getLoginId(), request.getPassword()));
    }
}
