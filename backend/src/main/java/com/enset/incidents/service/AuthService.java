package com.enset.incidents.service;

import com.enset.incidents.dto.Dtos.*;
import com.enset.incidents.entity.RefreshToken;
import com.enset.incidents.entity.Role;
import com.enset.incidents.entity.User;
import com.enset.incidents.exception.ApiException;
import com.enset.incidents.repository.RefreshTokenRepository;
import com.enset.incidents.repository.UserRepository;
import com.enset.incidents.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshExpirationMs;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email deja utilise");
        }
        // Le role est TOUJOURS EMPLOYEE a l'inscription (impossible de le choisir)
        User user = new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), Role.EMPLOYEE);
        return Mapper.toDto(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApiException.unauthorized("Email ou mot de passe incorrect");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Compte desactive : contactez l'administrateur");
        }
        return buildResponse(user, createRefreshToken(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> ApiException.unauthorized("Refresh token invalide"));

        if (stored.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw ApiException.unauthorized("Refresh token expire : reconnectez-vous");
        }
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Compte desactive : contactez l'administrateur");
        }
        return buildResponse(user, stored.getToken());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        // Deconnexion : le refresh token est supprime, il ne peut plus renouveler l'access token
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user); // un seul refresh token actif par utilisateur
        refreshTokenRepository.flush();
        RefreshToken rt = new RefreshToken(UUID.randomUUID().toString(), user,
                Instant.now().plusMillis(refreshExpirationMs));
        return refreshTokenRepository.save(rt).getToken();
    }

    private AuthResponse buildResponse(User user, String refreshToken) {
        return new AuthResponse(jwtService.generateToken(user), refreshToken, user.getRole(),
                user.getFullName(), user.getEmail());
    }
}
