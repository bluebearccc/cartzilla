package com.cartzilla.user.api.dto;

public class OAuthDtos {
    private OAuthDtos() {}

    public record AuthorizationResponse(String authorizationUrl, String state) {}

    public record OAuthLoginResponse(String accessToken, String refreshToken, String email, String role) {}
}
