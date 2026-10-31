# Diagrammes

Les diagrammes ci-dessous sont en **Mermaid** : ils s'affichent directement sur GitHub / GitLab / VS Code
(extension "Markdown Preview Mermaid Support"). Pour les inserer dans le rapport PDF, collez le code sur
<https://mermaid.live> puis exportez en PNG ou SVG.

## 1. Architecture generale

```mermaid
flowchart TB
    subgraph React["Frontend React (http://localhost:5173)"]
        UI["Login - Dashboard - Incidents - Users"]
        AX["Axios + interceptors<br/>Authorization: Bearer JWT"]
        UI --> AX
    end

    AX -- "HTTP / JSON" --> CORS

    subgraph Spring["Backend Spring Boot (http://localhost:8080)"]
        CORS["CORS"] --> JF["JwtAuthFilter<br/>verifie le JWT"]
        JF --> SS["Spring Security<br/>regles d'URL + @PreAuthorize"]
        SS --> RC["REST Controllers"]
        RC --> S["Services<br/>regles metier + controle du proprietaire"]
        S --> R["Repositories (Spring Data JPA)"]
    end

    R --> DB[("H2 / MySQL")]
```

## 2. Diagramme des entites

```mermaid
erDiagram
    USERS ||--o{ INCIDENTS : "cree (created_by_id)"
    USERS |o--o{ INCIDENTS : "traite (assigned_to_id)"
    INCIDENTS ||--o{ INCIDENT_HISTORY : "historique"
    INCIDENTS ||--o{ INCIDENT_COMMENTS : "notes"
    USERS ||--o{ INCIDENT_COMMENTS : "ecrit (author_id)"
    USERS |o--o{ INCIDENT_HISTORY : "action de (actor_id)"
    USERS ||--o| REFRESH_TOKENS : "possede"

    USERS {
        Long id PK
        String fullName
        String email UK
        String password "BCrypt"
        Role role "EMPLOYEE | TECHNICIAN | ADMIN"
        boolean enabled
        LocalDateTime createdAt
    }
    INCIDENTS {
        Long id PK
        String title
        String description
        Category category
        Priority priority
        Status status "OPEN | ASSIGNED | IN_PROGRESS | RESOLVED | CLOSED"
        LocalDateTime createdAt
        LocalDateTime updatedAt
        Long created_by_id FK
        Long assigned_to_id FK "nullable"
    }
    INCIDENT_HISTORY {
        Long id PK
        Long incident_id FK
        String message
        Long actor_id FK
        LocalDateTime createdAt
    }
    INCIDENT_COMMENTS {
        Long id PK
        Long incident_id FK
        Long author_id FK
        String content
        LocalDateTime createdAt
    }
    REFRESH_TOKENS {
        Long id PK
        String token UK
        Long user_id FK
        Instant expiryDate
    }
```

## 3. Sequence d'une requete protegee

```mermaid
sequenceDiagram
    participant R as React (Axios)
    participant F as JwtAuthFilter
    participant S as Spring Security
    participant C as Controller / Service
    R->>F: GET /api/incidents/12 (Authorization: Bearer JWT)
    F->>F: verifie signature + expiration
    alt JWT absent / invalide / expire
        F-->>R: 401 Unauthorized
    else JWT valide
        F->>S: Authentication (email + role) dans le SecurityContext
        S->>C: @PreAuthorize (role) puis controle du proprietaire
        alt role interdit ou incident d'un autre
            C-->>R: 403 Forbidden
        else autorise
            C-->>R: 200 + JSON
        end
    end
    Note over R: sur 401 : suppression du JWT, redirection vers /login, message "session expiree"
```
