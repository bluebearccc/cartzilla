package com.cartzilla.gateway.filter;

import com.cartzilla.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * GatewayFilter validate JWT, inject X-User-Id / X-User-Role cho downstream.
 * Khai báo trong route bằng:  filters: [ JwtAuth ]
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public static class Config {}

    /** Tên dùng để tham chiếu trong route config: filters: [ JwtAuth ]. */
    @Override
    public String name() {
        return "JwtAuth";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return unauthorized(exchange);
            }
            String token = auth.substring(7);
            if (!jwtTokenProvider.isValid(token)) {
                return unauthorized(exchange);
            }
            Claims claims = jwtTokenProvider.parse(token);
            Object role = claims.get("role");
            if (claims.getSubject() == null || claims.getSubject().isBlank() || role == null) {
                return unauthorized(exchange);
            }
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                    })
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", String.valueOf(role))
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private reactor.core.publisher.Mono<Void> unauthorized(
            org.springframework.web.server.ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
