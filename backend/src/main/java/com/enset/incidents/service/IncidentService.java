package com.enset.incidents.service;

import com.enset.incidents.dto.Dtos.*;
import com.enset.incidents.entity.*;
import com.enset.incidents.exception.ApiException;
import com.enset.incidents.repository.IncidentRepository;
import com.enset.incidents.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    // ---------- utilitaires ----------

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private Incident findIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Incident introuvable"));
    }

    /**
     * CONTROLE DU PROPRIETAIRE (section 12) : le backend verifie lui-meme a qui appartient l'incident,
     * quel que soit ce que React affiche.
     * - ADMIN : tout voir
     * - EMPLOYEE : uniquement ses propres incidents
     * - TECHNICIAN : uniquement les incidents qui lui sont affectes
     */
    private void checkCanView(Incident incident, User user) {
        boolean allowed = switch (user.getRole()) {
            case ADMIN -> true;
            case EMPLOYEE -> incident.getCreatedBy().getId().equals(user.getId());
            case TECHNICIAN -> incident.getAssignedTo() != null
                    && incident.getAssignedTo().getId().equals(user.getId());
        };
        if (!allowed) {
            throw ApiException.forbidden("Acces interdit : cet incident ne vous appartient pas");
        }
    }

    private void checkIsAssignedTechnicianOrAdmin(Incident incident, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() != Role.TECHNICIAN || incident.getAssignedTo() == null
                || !incident.getAssignedTo().getId().equals(user.getId())) {
            throw ApiException.forbidden("Acces interdit : cet incident ne vous est pas affecte");
        }
    }

    // ---------- lectures ----------

    @Transactional(readOnly = true)
    public List<IncidentDto> getMyIncidents(String email) {
        User user = currentUser(email);
        return incidentRepository.findByCreatedByOrderByCreatedAtDesc(user).stream().map(Mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> getAssignedIncidents(String email) {
        User user = currentUser(email);
        return incidentRepository.findByAssignedToOrderByCreatedAtDesc(user).stream().map(Mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> getAllIncidents() {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream().map(Mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetailDto getIncident(Long id, String email) {
        User user = currentUser(email);
        Incident incident = findIncident(id);
        checkCanView(incident, user);
        return Mapper.toDetailDto(incident);
    }

    // ---------- ecritures ----------

    public IncidentDto create(String email, CreateIncidentRequest request) {
        User user = currentUser(email);
        Incident incident = new Incident();
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setCategory(request.category() != null ? request.category() : Category.OTHER);
        incident.setPriority(request.priority());
        incident.setStatus(Status.OPEN);
        incident.setCreatedBy(user);
        incident.addHistory("Incident cree", user);
        return Mapper.toDto(incidentRepository.save(incident));
    }

    public IncidentDto assign(Long incidentId, Long technicianId, String adminEmail) {
        User admin = currentUser(adminEmail);
        Incident incident = findIncident(incidentId);

        if (incident.getStatus() == Status.CLOSED) {
            throw ApiException.badRequest("Un incident CLOSED ne peut plus etre modifie");
        }
        User technician = userRepository.findById(technicianId)
                .filter(u -> u.getRole() == Role.TECHNICIAN)
                .orElseThrow(() -> ApiException.notFound("Technicien introuvable"));
        if (!technician.isEnabled()) {
            throw ApiException.badRequest("Ce technicien est desactive");
        }

        incident.setAssignedTo(technician);
        incident.addHistory("Affecte a " + technician.getFullName(), admin);

        // Un incident OPEN passe a ASSIGNED ; s'il est deja en cours, on garde son statut
        if (incident.getStatus() == Status.OPEN) {
            incident.setStatus(Status.ASSIGNED);
            incident.addHistory("OPEN -> ASSIGNED", admin);
        }
        return Mapper.toDto(incidentRepository.save(incident));
    }

    public IncidentDto updateStatus(Long incidentId, Status newStatus, String email) {
        User user = currentUser(email);
        Incident incident = findIncident(incidentId);

        // Regle 5 : un technicien ne modifie que SES incidents
        checkIsAssignedTechnicianOrAdmin(incident, user);

        // Regle 8 : un incident CLOSED n'est plus modifiable
        if (incident.getStatus() == Status.CLOSED) {
            throw ApiException.badRequest("Un incident CLOSED ne peut plus etre modifie");
        }
        if (newStatus == Status.ASSIGNED) {
            throw ApiException.badRequest("Utilisez l'affectation pour passer a ASSIGNED");
        }
        if (newStatus == incident.getStatus()) {
            throw ApiException.badRequest("L'incident est deja au statut " + newStatus);
        }
        // Un technicien ne peut que prendre en charge ou resoudre (la cloture est reservee a l'admin)
        if (user.getRole() == Role.TECHNICIAN && !Set.of(Status.IN_PROGRESS, Status.RESOLVED).contains(newStatus)) {
            throw ApiException.forbidden("Un technicien peut seulement passer l'incident a IN_PROGRESS ou RESOLVED");
        }

        Status old = incident.getStatus();
        incident.setStatus(newStatus);
        incident.addHistory(old + " -> " + newStatus, user);
        return Mapper.toDto(incidentRepository.save(incident));
    }

    public IncidentDetailDto addComment(Long incidentId, String content, String email) {
        User user = currentUser(email);
        Incident incident = findIncident(incidentId);
        checkIsAssignedTechnicianOrAdmin(incident, user);

        incident.addComment(content.trim(), user);
        incident.addHistory("Note de traitement ajoutee par " + user.getFullName(), user);
        incidentRepository.saveAndFlush(incident);
        return Mapper.toDetailDto(incident);
    }
}
