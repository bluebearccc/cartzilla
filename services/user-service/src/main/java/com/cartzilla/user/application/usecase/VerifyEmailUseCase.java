package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.repository.EmailVerificationTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyEmailUseCase {
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void execute(String token) {
        var verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid email verification token"));
        if (verificationToken.isUsed() || verificationToken.isExpired()) {
            throw new BusinessException("Email verification token expired or used");
        }
        var user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.requireActive();
        user.verifyEmail();
        userRepository.save(user);
        verificationToken.markUsed();
        verificationTokenRepository.save(verificationToken);
    }
}
