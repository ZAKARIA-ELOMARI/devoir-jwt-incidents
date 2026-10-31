package com.enset.incidents.controller;

import com.enset.incidents.dto.Dtos.*;
import com.enset.incidents.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Authentication : l'utilisateur identifie par le JWT (jamais fourni par le client dans l'URL)
    @GetMapping
    public UserDto getProfile(Authentication auth) {
        return profileService.getProfile(auth.getName());
    }

    @PutMapping
    public ProfileUpdateResponse update(Authentication auth, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(auth.getName(), request);
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(auth.getName(), request);
        return Map.of("message", "Mot de passe modifie");
    }
}
