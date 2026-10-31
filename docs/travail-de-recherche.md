# Travail de recherche (reponses courtes, a reprendre dans le rapport)

## 1. Relations JPA entre User et Incident (section 4)

| Choix | Justification |
|---|---|
| `Incident.createdBy` : `@ManyToOne(LAZY, optional = false)` | Plusieurs incidents appartiennent a un seul createur ; un incident a toujours un createur. |
| `Incident.assignedTo` : `@ManyToOne(LAZY)` (nullable) | Un technicien peut traiter plusieurs incidents ; tant que l'incident n'est pas affecte, la valeur est `null`. |
| Pas de `@OneToMany` dans `User` | Relation unidirectionnelle : on evite de charger (ou de serialiser) toute la liste des incidents d'un utilisateur, et deux listes (`created` / `assigned`) seraient ambigues. Les listes sont obtenues par requete : `findByCreatedBy...`, `findByAssignedTo...`. |
| `@OneToMany(mappedBy = "incident", cascade = ALL)` dans `Incident` pour `history` et `comments` | Ces objets n'existent que dans le contexte d'un incident : on les cree/supprime avec lui (`cascade`, `orphanRemoval`). |
| `LAZY` partout | `EAGER` charge systematiquement l'entite liee (jointures inutiles, risque de N+1 et de boucles). Avec `LAZY`, la donnee n'est lue que si on y accede. |
| `@EntityGraph` sur les listes d'incidents | Quand la liste affiche le createur et le technicien, on les charge en **une seule requete** (evite le probleme N+1). |
| `spring.jpa.open-in-view=false` + DTO | Les entites ne sortent jamais du service : on les convertit en DTO dans la transaction (pas de `LazyInitializationException`, pas de fuite du mot de passe). |

## 2. JWT (section 6)

- **Claim** : une information (paire cle/valeur) contenue dans le payload du JWT. Claims standards : `sub` (sujet),
  `iat` (date de creation), `exp` (expiration). On peut ajouter des claims personnalises.
- **Ajouter le role** : `Jwts.builder().subject(email).claim("role", "EMPLOYEE")...`
- **Lire un claim** : `Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("role", String.class)`
- **Verifier l'expiration** : le parseur lance `ExpiredJwtException` si `exp` est depasse (verifie aussi la signature) ;
  le filtre l'attrape et Spring Security repond `401`.
- Un JWT est **signe**, pas chiffre : on ne met jamais de mot de passe dedans.
- Dans ce projet, le role utilise pour les autorisations est relu en base a chaque requete : un compte desactive ou
  dont le role change est pris en compte immediatement, sans attendre l'expiration du token.

## 3. Spring Security (sections 7, 9)

- `SessionCreationPolicy.STATELESS` : aucune session serveur ; l'identite vient uniquement du JWT.
- `hasRole("ADMIN")` attend l'autorite `ROLE_ADMIN` (le prefixe `ROLE_` est ajoute automatiquement) ;
  `hasAuthority("ROLE_ADMIN")` compare la chaine exacte.
- `@PreAuthorize("hasRole('ADMIN')")` protege une **methode** (necessite `@EnableMethodSecurity`) ; les regles d'URL
  (`/api/admin/**`) protegent un **chemin**. Le projet utilise les deux (defense en profondeur).
- CSRF desactive : pas de cookie de session, le JWT est envoye dans un en-tete, une page tierce ne peut donc pas le
  joindre automatiquement.

## 4. Controle du proprietaire (section 12)

`Authentication auth` (injecte dans le controleur) fournit l'email de l'utilisateur identifie par le JWT
(equivalent a `SecurityContextHolder.getContext().getAuthentication()`). Le service charge l'utilisateur, puis compare
`incident.getCreatedBy().getId()` a `user.getId()` (ou `assignedTo` pour un technicien) et lance une
`403 Forbidden` sinon. Le client ne fournit **jamais** son identite : elle vient du token.

## 5. Codes HTTP (section 14)

| Code | Sens | Exemple dans le projet |
|---|---|---|
| 400 Bad Request | Requete invalide | Titre vide, mauvais ancien mot de passe, incident CLOSED |
| 401 Unauthorized | **Non authentifie** (identite inconnue) | JWT absent, invalide ou expire, mauvais mot de passe |
| 403 Forbidden | Authentifie mais **non autorise** | EMPLOYEE sur `/api/admin/**`, incident d'un autre, compte desactive |
| 404 Not Found | Ressource inexistante | Incident ou utilisateur introuvable |
| 409 Conflict | Conflit avec l'etat existant | Email deja utilise |

Les erreurs sont centralisees dans `GlobalExceptionHandler` (`@RestControllerAdvice` + `@ExceptionHandler`) et ont
toutes le format `{ "status": 404, "message": "Incident introuvable" }`.

## 6. Validation (section 13)

`@NotBlank`, `@Email`, `@Size`, `@NotNull` sur les DTO + `@Valid` sur le `@RequestBody`. En cas d'echec Spring lance
`MethodArgumentNotValidException`, convertie en `400` avec le detail champ par champ.

## 7. Access Token et Refresh Token (section 23)

| | Access Token | Refresh Token |
|---|---|---|
| Role | Authentifier chaque requete `/api/**` | Obtenir un nouvel access token |
| Duree | Courte (5 min ici) | Longue (7 jours ici) |
| Contenu | JWT signe (email, role, exp) | Valeur aleatoire (UUID) stockee en base |
| Envoye | A chaque requete | Uniquement a `/auth/refresh` et `/auth/logout` |

- Renouvellement : `POST /auth/refresh {"refreshToken": "..."}` -> nouvel access token.
- Deconnexion : `POST /auth/logout` supprime le refresh token en base (il ne peut plus servir). L'access token deja
  emis reste valable jusqu'a son expiration (quelques minutes) : c'est la raison de sa courte duree.
- Cote React, le renouvellement automatique est desactive par defaut (`VITE_ENABLE_REFRESH=false`) pour respecter la
  section 17 (401 -> retour au Login). Mettre `true` dans `frontend/.env` pour l'activer.
