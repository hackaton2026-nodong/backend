package com.kworkerharmony.backend.user;

import com.kworkerharmony.backend.global.response.ApiResponse;
import com.kworkerharmony.backend.global.security.UserPrincipal;
import com.kworkerharmony.backend.user.dto.request.UpdateMyProfileRequest;
import com.kworkerharmony.backend.user.dto.response.MyProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(userService.getMyProfile(userPrincipal));
    }

    @PatchMapping("/me")
    public ApiResponse<MyProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateMyProfileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(userService.updateMyProfile(request, userPrincipal));
    }
}
