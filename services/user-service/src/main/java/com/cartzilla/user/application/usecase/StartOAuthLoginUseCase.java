package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StartOAuthLoginUseCase {
    private final OAuthProviderGateway oauthProviderGateway;
    private final TokenGenerator tokenGenerator;

    public record Result(String authorizationUrl, String state) {}

    public Result execute(OAuthProvider provider) {
        String state = tokenGenerator.generateUrlSafeToken();
        return new Result(oauthProviderGateway.buildAuthorizationUrl(provider, state), state);
    }
}
