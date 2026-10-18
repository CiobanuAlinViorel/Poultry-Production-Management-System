package com.example.identity_service.application;

import com.example.identity_service.services.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
public class PublicController {

    private final TokenService tokenService;

    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(tokenService.getJwkSet());
    }
}
