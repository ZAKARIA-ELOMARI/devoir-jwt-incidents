package com.enset.incidents.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_comments")
public class IncidentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public IncidentComment() {
    }

    public IncidentComment(Incident incident, String content, User author) {
        this.incident = incident;
        this.content = content;
        this.author = author;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
