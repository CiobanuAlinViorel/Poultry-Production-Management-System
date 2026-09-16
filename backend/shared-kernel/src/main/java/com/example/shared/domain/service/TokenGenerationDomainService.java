package com.example.shared.domain.service;

import com.example.shared.domain.entity.User;
import com.example.shared.domain.exception.InvalidTokenException;
import com.example.shared.domain.valueobject.JwtToken;
import com.example.shared.domain.valueobject.RefreshToken;
import com.example.shared.infrastructure.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenGenerationDomainService {

    private final TokenCryptoService tokenCryptoService;

    public JwtToken generateAccessToken(Long userId) throws InvalidTokenException {
        String tokenId = tokenCryptoService.generateTokenId();
        return JwtToken.createAccessToken(
                userId,
               tokenId
        );
    }

    public RefreshToken generateRefreshToken(User user) throws InvalidTokenException {
        String tokenId = tokenCryptoService.generateTokenId();
        return RefreshToken.create(
                user.getId(),
                user.getUsername(),
                tokenId
        );
    }

    public JwtToken refreshAccessToken(RefreshToken refreshToken) throws InvalidTokenException {
        if (refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or invalid");
        }

        return generateAccessToken(refreshToken.getUser());
    }

//    public boolean validateToken(String token) {
//        return tokenCryptoService.validateTokenSignature(token);
//    }
//
//    public String extractUsernameFromToken(String token) {
//        return tokenCryptoService.extractUsername(token);
//    }
}