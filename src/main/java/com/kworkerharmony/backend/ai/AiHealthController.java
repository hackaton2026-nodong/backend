package com.kworkerharmony.backend.ai;

import com.kworkerharmony.backend.ai.dto.response.AiHealthResponse;
import com.kworkerharmony.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiHealthController {

    private final AiHealthService aiHealthService;

    @GetMapping("/health")
    public ApiResponse<AiHealthResponse> getHealth() {
        return ApiResponse.success(aiHealthService.getHealth());
    }
}
