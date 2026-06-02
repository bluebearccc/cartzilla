package com.cartzilla.user.application.usecase;

import com.cartzilla.security.JwtTokenProvider;
import com.cartzilla.user.application.command.AuthCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public record Result(String accessToken, String email, String role) {}

    public Result execute(AuthCommand.Login cmd) {
        User user = userRepository.findByEmail(cmd.email())
                .orElseThrow(() -> new BusinessException("Email hoặc mật khẩu không đúng"));
        if (!passwordEncoder.matches(cmd.password(), user.getPasswordHash())) {
            throw new BusinessException("Email hoặc mật khẩu không đúng");
        }
        String token = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        return new Result(token, user.getEmail(), user.getRole().name());
    }
}
