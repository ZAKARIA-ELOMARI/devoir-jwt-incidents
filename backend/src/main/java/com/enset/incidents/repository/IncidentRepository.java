package com.enset.incidents.repository;

import com.enset.incidents.entity.Incident;
import com.enset.incidents.entity.Priority;
import com.enset.incidents.entity.Status;
import com.enset.incidents.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // EntityGraph : charge createdBy et assignedTo en une seule requete (evite le probleme N+1)
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findByAssignedToOrderByCreatedAtDesc(User assignedTo);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findAllByOrderByCreatedAtDesc();

    long countByStatus(Status status);

    long countByPriority(Priority priority);
}
