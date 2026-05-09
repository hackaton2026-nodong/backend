package com.kworkerharmony.backend.auth;

import com.kworkerharmony.backend.auth.dto.request.LoginRequest;
import com.kworkerharmony.backend.auth.dto.request.LogoutRequest;
import com.kworkerharmony.backend.auth.dto.request.ReissueRequest;
import com.kworkerharmony.backend.auth.dto.request.SignupRequest;
import com.kworkerharmony.backend.auth.dto.response.LoginResponse;
import com.kworkerharmony.backend.auth.dto.response.ReissueResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth API", description = "Authentication and Authorization")
@RestController
@RequestMapping({"/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Signup", description = "Register a new user")
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.empty();
    }

    @Operation(summary = "Login", description = "Login and receive JWT tokens")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "Reissue", description = "Reissue tokens with Refresh Token")
    @PostMapping("/reissue")
    public ApiResponse<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return ApiResponse.success(authService.reissue(request));
    }

    @Operation(summary = "Logout", description = "Logout and invalidate tokens")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.empty();
    }
}
