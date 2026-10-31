package com.enset.incidents.service;

import com.enset.incidents.dto.Dtos.*;
import com.enset.incidents.entity.User;
import com.enset.incidents.exception.ApiException;
import com.enset.incidents.repository.UserRepository;
import com.enset.incidents.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    private User current(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    @Transactional(readOnly = true)
    public UserDto getProfile(String email) {
        return Mapper.toDto(current(email));
    }

    @Transactional
    public ProfileUpdateResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = current(email);
        String newEmail = request.email().trim().toLowerCase();

        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw ApiException.conflict("Email deja utilise");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(newEmail);
        userRepository.save(user);

        // Le JWT contient l'email : on en emet un nouveau pour que la session reste valide
        return new ProfileUpdateResponse(Mapper.toDto(user), jwtService.generateToken(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = current(email);

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw ApiException.badRequest("Ancien mot de passe incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw ApiException.badRequest("La confirmation ne correspond pas au nouveau mot de passe");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
