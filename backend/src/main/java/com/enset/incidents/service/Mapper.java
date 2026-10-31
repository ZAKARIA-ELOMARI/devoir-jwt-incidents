package com.enset.incidents.service;

import com.enset.incidents.dto.Dtos.*;
import com.enset.incidents.entity.*;

/** Conversion entites -> DTO. A appeler dans une transaction (relations LAZY). */
public final class Mapper {

    private Mapper() {
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }

    public static UserSummary summary(User u) {
        return u == null ? null : new UserSummary(u.getId(), u.getFullName(), u.getEmail());
    }

    public static IncidentDto toDto(Incident i) {
        return new IncidentDto(i.getId(), i.getTitle(), i.getDescription(), i.getCategory(), i.getPriority(),
                i.getStatus(), i.getCreatedAt(), i.getUpdatedAt(), summary(i.getCreatedBy()),
                summary(i.getAssignedTo()));
    }

    public static IncidentDetailDto toDetailDto(Incident i) {
        return new IncidentDetailDto(i.getId(), i.getTitle(), i.getDescription(), i.getCategory(), i.getPriority(),
                i.getStatus(), i.getCreatedAt(), i.getUpdatedAt(), summary(i.getCreatedBy()),
                summary(i.getAssignedTo()),
                i.getComments().stream()
                        .map(c -> new CommentDto(c.getId(), summary(c.getAuthor()), c.getContent(), c.getCreatedAt()))
                        .toList(),
                i.getHistory().stream()
                        .map(h -> new HistoryDto(h.getId(), h.getMessage(), summary(h.getActor()), h.getCreatedAt()))
                        .toList());
    }
}
