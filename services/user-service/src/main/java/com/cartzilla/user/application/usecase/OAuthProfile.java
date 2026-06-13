package com.cartzilla.user.application.usecase;

public record OAuthProfile(
        String providerUserId,
        String email,
        String displayName,
        String avatarUrl,
        boolean emailVerified) {
}
