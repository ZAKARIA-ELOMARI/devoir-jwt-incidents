package com.enset.incidents.service;

import com.enset.incidents.dto.Dtos.StatisticsDto;
import com.enset.incidents.dto.Dtos.UserDto;
import com.enset.incidents.entity.Priority;
import com.enset.incidents.entity.Role;
import com.enset.incidents.entity.Status;
import com.enset.incidents.entity.User;
import com.enset.incidents.exception.ApiException;
import com.enset.incidents.repository.IncidentRepository;
import com.enset.incidents.repository.RefreshTokenRepository;
import com.enset.incidents.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminService(UserRepository userRepository, IncidentRepository incidentRepository,
                        RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtAsc().stream().map(Mapper::toDto).toList();
    }

    public UserDto setEnabled(Long id, boolean enabled, String adminEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        if (!enabled && user.getEmail().equals(adminEmail)) {
            throw ApiException.badRequest("Vous ne pouvez pas desactiver votre propre compte");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.deleteByUser(user); // coupe aussi le renouvellement de session
        }
        return Mapper.toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public StatisticsDto getStatistics() {
        return new StatisticsDto(
                userRepository.count(),
                userRepository.countByRole(Role.EMPLOYEE),
                userRepository.countByRole(Role.TECHNICIAN),
                userRepository.countByRole(Role.ADMIN),
                incidentRepository.count(),
                incidentRepository.countByStatus(Status.OPEN),
                incidentRepository.countByStatus(Status.ASSIGNED),
                incidentRepository.countByStatus(Status.IN_PROGRESS),
                incidentRepository.countByStatus(Status.RESOLVED),
                incidentRepository.countByStatus(Status.CLOSED),
                incidentRepository.countByPriority(Priority.CRITICAL));
    }
}
