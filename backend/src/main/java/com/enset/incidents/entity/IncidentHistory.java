package com.enset.incidents.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_history")
public class IncidentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    // Exemple : "OPEN -> IN_PROGRESS", "Affecte a Technicien A", "Incident cree"
    @Column(nullable = false, length = 500)
    private String message;

    // Utilisateur ayant effectue l'action
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public IncidentHistory() {
    }

    public IncidentHistory(Incident incident, String message, User actor) {
        this.incident = incident;
        this.message = message;
        this.actor = actor;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public String getMessage() { return message; }
    public User getActor() { return actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
