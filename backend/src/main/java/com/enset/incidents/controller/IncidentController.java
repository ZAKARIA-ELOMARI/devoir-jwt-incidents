package com.enset.incidents.controller;

import com.enset.incidents.dto.Dtos.*;
import com.enset.incidents.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<IncidentDto> create(Authentication auth, @Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(auth.getName(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<IncidentDto> myIncidents(Authentication auth) {
        return incidentService.getMyIncidents(auth.getName());
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<IncidentDto> assignedIncidents(Authentication auth) {
        return incidentService.getAssignedIncidents(auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<IncidentDto> allIncidents() {
        return incidentService.getAllIncidents();
    }

    // Acces selon proprietaire / role : verifie dans le service (checkCanView)
    @GetMapping("/{id}")
    public IncidentDetailDto details(@PathVariable Long id, Authentication auth) {
        return incidentService.getIncident(id, auth.getName());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public IncidentDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
                                    Authentication auth) {
        return incidentService.updateStatus(id, request.status(), auth.getName());
    }

    @PutMapping("/{id}/assign/{technicianId}")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentDto assign(@PathVariable Long id, @PathVariable Long technicianId, Authentication auth) {
        return incidentService.assign(id, technicianId, auth.getName());
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public IncidentDetailDto addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
                                        Authentication auth) {
        return incidentService.addComment(id, request.content(), auth.getName());
    }
}
