package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.web.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateStore {
    private final TokenGenerator tokenGenerator;
    private final Map<String, Entry> states = new ConcurrentHashMap<>();

    @Value("${oauth.state-ttl-seconds:600}")
    private long ttlSeconds = 600;

    public OAuthStateStore(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    public String issue(OAuthProvider provider) {
        if (provider == null) {
            throw new BusinessException("OAuth provider is required");
        }
        cleanupExpired();
        String state = tokenGenerator.generateUrlSafeToken();
        states.put(state, new Entry(provider, Instant.now().plusSeconds(ttlSeconds)));
        return state;
    }

    public void validateAndConsume(OAuthProvider provider, String state) {
        if (provider == null) {
            throw new BusinessException("OAuth provider is required");
        }
        if (state == null || state.isBlank()) {
            throw new BusinessException("OAuth state is required");
        }
        Entry entry = states.remove(state);
        if (entry == null || entry.expiresAt().isBefore(Instant.now()) || entry.provider() != provider) {
            throw new BusinessException("Invalid OAuth state");
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        states.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record Entry(OAuthProvider provider, Instant expiresAt) {}
}
