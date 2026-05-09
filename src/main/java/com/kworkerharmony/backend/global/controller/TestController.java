package com.kworkerharmony.backend.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Test API", description = "For testing Swagger setup")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Operation(summary = "Hello", description = "Return hello message")
    @GetMapping("/hello")
    public String hello() {
        return "Hello, K-Worker Harmony!";
    }
}
