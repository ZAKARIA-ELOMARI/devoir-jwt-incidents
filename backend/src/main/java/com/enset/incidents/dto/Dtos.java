package com.enset.incidents.dto;

import com.enset.incidents.entity.Category;
import com.enset.incidents.entity.Priority;
import com.enset.incidents.entity.Role;
import com.enset.incidents.entity.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tous les DTO (objets echanges en JSON avec React) regroupes dans un seul fichier.
 * On n'expose jamais directement les entites JPA (ni le mot de passe).
 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Authentification ----------

    // Pas de champ "role" : l'utilisateur ne peut pas choisir son role (regle metier 1 et 2)
    public record RegisterRequest(
            @NotBlank(message = "Le nom complet est obligatoire") String fullName,
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 6, max = 100, message = "Le mot de passe doit contenir au moins 6 caracteres") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
            @NotBlank(message = "Le mot de passe est obligatoire") String password) {
    }

    public record RefreshRequest(@NotBlank(message = "Le refresh token est obligatoire") String refreshToken) {
    }

    public record AuthResponse(String token, String refreshToken, Role role, String fullName, String email) {
    }

    // ---------- Utilisateurs ----------

    public record UserDto(Long id, String fullName, String email, Role role, boolean enabled,
                          LocalDateTime createdAt) {
    }

    public record UserSummary(Long id, String fullName, String email) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Le nom complet est obligatoire") String fullName,
            @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email) {
    }

    // Si l'email change, le "subject" du JWT change aussi : on renvoie un nouveau token
    public record ProfileUpdateResponse(UserDto user, String token) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "L'ancien mot de passe est obligatoire") String oldPassword,
            @NotBlank(message = "Le nouveau mot de passe est obligatoire")
            @Size(min = 6, max = 100, message = "Le nouveau mot de passe doit contenir au moins 6 caracteres") String newPassword,
            @NotBlank(message = "La confirmation est obligatoire") String confirmPassword) {
    }

    // ---------- Incidents ----------

    public record CreateIncidentRequest(
            @NotBlank(message = "Le titre est obligatoire") @Size(max = 150, message = "Le titre ne doit pas depasser 150 caracteres") String title,
            @NotBlank(message = "La description est obligatoire") @Size(max = 4000, message = "La description est trop longue") String description,
            Category category,
            @NotNull(message = "La priorite est obligatoire") Priority priority) {
    }

    public record StatusRequest(@NotNull(message = "Le statut est obligatoire") Status status) {
    }

    public record CommentRequest(
            @NotBlank(message = "La note ne peut pas etre vide") @Size(max = 2000, message = "La note est trop longue") String content) {
    }

    public record IncidentDto(Long id, String title, String description, Category category, Priority priority,
                              Status status, LocalDateTime createdAt, LocalDateTime updatedAt,
                              UserSummary createdBy, UserSummary assignedTo) {
    }

    public record CommentDto(Long id, UserSummary author, String content, LocalDateTime createdAt) {
    }

    public record HistoryDto(Long id, String message, UserSummary actor, LocalDateTime createdAt) {
    }

    public record IncidentDetailDto(Long id, String title, String description, Category category, Priority priority,
                                    Status status, LocalDateTime createdAt, LocalDateTime updatedAt,
                                    UserSummary createdBy, UserSummary assignedTo,
                                    List<CommentDto> comments, List<HistoryDto> history) {
    }

    // ---------- Administration ----------

    public record StatisticsDto(long totalUsers, long employees, long technicians, long admins,
                                long totalIncidents, long open, long assigned, long inProgress,
                                long resolved, long closed, long critical) {
    }
}
