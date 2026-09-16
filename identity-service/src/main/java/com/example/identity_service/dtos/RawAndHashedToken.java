package com.example.identity_service.dtos;

// small carrier — raw goes to client, hashed goes to DB
public record RawAndHashedToken(
        String raw,
        String hashed
) {}