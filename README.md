# Gestion d'incidents - Spring Security, JWT et React

Application web permettant de declarer et suivre des incidents informatiques / cybersecurite, avec trois roles :
**EMPLOYEE**, **TECHNICIAN** et **ADMIN**.

```
devoir-jwt/
├── backend/    Spring Boot 3.3 (Java 17+), Spring Security, JWT (jjwt), JPA, H2 / MySQL
├── frontend/   React 18 + Vite, React Router, Axios
├── docs/       diagrammes (architecture, entites) + travail de recherche
└── tests/      collection Postman + script curl (scenario et tests de securite)
```

## 1. Pre-requis

| Outil | Version | Verification |
|---|---|---|
| JDK | 17 ou plus (21 OK) | `java -version` |
| Maven | 3.8+ | `mvn -v` (ou utilisez l'IDE : IntelliJ / Eclipse / VS Code) |
| Node.js | 18 ou plus | `node -v` |
| npm | 9+ | `npm -v` |
| Postman | pour les tests | - |

## 2. Lancer le backend (terminal 1)

```bash
cd backend
mvn spring-boot:run
```

Attendre le message `Started IncidentManagerApplication` puis `Comptes de test prets`.
La premiere execution telecharge les dependances Maven (quelques minutes).

Alternative (jar executable) :

```bash
cd backend
mvn clean package -DskipTests
java -jar target/incident-manager-1.0.0.jar
```

## 3. Lancer le frontend (terminal 2)

```bash
cd frontend
npm install
npm run dev
```

## 4. URLs et ports

| Service | URL | Port |
|---|---|---|
| **Application React** | <http://localhost:5173> | 5173 |
| API Spring Boot | <http://localhost:8080> | 8080 |
| Console H2 | <http://localhost:8080/h2-console> | 8080 |

Console H2 : JDBC URL `jdbc:h2:mem:incidentsdb`, utilisateur `sa`, mot de passe vide.

> La base H2 est **en memoire** : elle est videe a chaque redemarrage du backend et les comptes de test sont recrees
> automatiquement. Redemarrez le backend avant de rejouer les tests Postman / curl.

## 5. Comptes de test

Crees automatiquement au demarrage (les roles ADMIN et TECHNICIAN ne peuvent pas s'obtenir par inscription).

| Role | Email | Mot de passe |
|---|---|---|
| ADMIN | `admin@enset.ma` | `Admin123!` |
| TECHNICIAN | `tech1@enset.ma` | `Tech123!` |
| TECHNICIAN | `tech2@enset.ma` | `Tech123!` |
| EMPLOYEE | `emp1@enset.ma` | `Emp123!` |
| EMPLOYEE | `emp2@enset.ma` | `Emp123!` |

Un nouveau compte cree depuis la page **Register** est toujours EMPLOYEE.

## 6. Roles

| Role | Droits |
|---|---|
| EMPLOYEE | Profil, declarer un incident, consulter **ses** incidents et leur detail |
| TECHNICIAN | Consulter les incidents **qui lui sont affectes**, changer leur statut (`IN_PROGRESS`, `RESOLVED`), ajouter des notes |
| ADMIN | Tous les incidents, tous les utilisateurs, activer / desactiver un compte, affecter un technicien, statistiques, cloturer |

## 7. Principaux endpoints

Publics : `/auth/**` et `/h2-console/**`. Tout `/api/**` exige `Authorization: Bearer <JWT>`.

| Methode | URL | Acces |
|---|---|---|
| POST | `/auth/register` | Public (role = EMPLOYEE) |
| POST | `/auth/login` | Public -> `{token, refreshToken, role, fullName, email}` |
| POST | `/auth/refresh` | Public (refresh token) |
| POST | `/auth/logout` | Public (refresh token) |
| GET / PUT | `/api/profile` | Authentifie |
| PUT | `/api/profile/password` | Authentifie |
| POST | `/api/incidents` | EMPLOYEE |
| GET | `/api/incidents/my` | EMPLOYEE |
| GET | `/api/incidents/assigned` | TECHNICIAN |
| GET | `/api/incidents` | ADMIN |
| GET | `/api/incidents/{id}` | Proprietaire / technicien affecte / ADMIN |
| PUT | `/api/incidents/{id}/status` | TECHNICIAN (ses incidents) ou ADMIN |
| PUT | `/api/incidents/{id}/assign/{technicianId}` | ADMIN |
| POST | `/api/incidents/{id}/comments` | TECHNICIAN (ses incidents) ou ADMIN |
| GET | `/api/admin/users` | ADMIN |
| PUT | `/api/admin/users/{id}/enable` · `/disable` | ADMIN |
| GET | `/api/admin/statistics` | ADMIN |

Format des erreurs : `{ "status": 404, "message": "Incident introuvable" }`

## 8. Fonctionnalites realisees

- Inscription (role force a EMPLOYEE), connexion, mots de passe **BCrypt**
- JWT (email, role, date de creation, expiration de **5 minutes**) + filtre `JwtAuthFilter`, session `STATELESS`, CORS pour `localhost:5173`
- Controle des roles : regles d'URL (`/api/admin/**`) + `@PreAuthorize` + `@EnableMethodSecurity`
- Controle du proprietaire d'un incident cote backend (403 si l'incident n'appartient pas a l'utilisateur)
- Regles metier : compte desactive refuse (et ses tokens deviennent inutilisables), technicien limite a ses incidents, affectation reservee a l'ADMIN, incident `CLOSED` non modifiable
- Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`, `@NotNull`) et gestion globale des exceptions (`@RestControllerAdvice`)
- Historique des incidents (`IncidentHistory`) et notes de traitement (`IncidentComment`)
- Profil : modification nom / email, changement de mot de passe (verification de l'ancien)
- Refresh token (`/auth/refresh`, `/auth/logout`)
- React : `ProtectedRoute`, `RoleRoute`, interceptor Axios (ajout du Bearer, gestion du 401 -> retour au Login + message "session expiree"), dashboards par role

## 9. Scenario de demonstration (section 24)

Via l'interface (http://localhost:5173) :

1. Creer deux comptes avec **Register** (ou utiliser `emp1` / `emp2`).
2. Se connecter avec `emp1@enset.ma`, declarer 2 incidents. Se deconnecter.
3. Se connecter avec `emp2@enset.ma`, declarer 1 incident. Se deconnecter.
4. Se connecter avec `admin@enset.ma` : **Incidents** -> choisir un technicien -> **Affecter**.
5. Se connecter avec `tech1@enset.ma` : **Incidents affectes** -> ouvrir l'incident -> `IN_PROGRESS`, puis `RESOLVED`, ajouter une note.
6. Se reconnecter avec l'employe : ouvrir l'incident (nouveau statut, notes, historique).

Tests automatiques du scenario **et** des 9 tests de securite :

- **Postman** : importer `tests/Incidents-JWT.postman_collection.json`, puis *Run collection* (dossiers 0, 1, 2 dans l'ordre).
  Les tokens sont memorises automatiquement. Chaque requete contient un test (statut attendu).
  Faites vos captures d'ecran pour le rapport.
- **curl** (Git Bash / Linux / macOS) : `bash tests/security-tests.sh`

| # | Test de securite | Resultat attendu |
|---|---|---|
| 1 | API protegee sans JWT | 401 |
| 2 | JWT invalide | 401 |
| 3 | JWT expire | 401 (la collection contient un token deja expire, variable `expiredToken`) |
| 4 | EMPLOYEE sur `/api/admin/users` | 403 |
| 5 | EMPLOYEE sur l'incident d'un autre | 403 |
| 6 | TECHNICIAN sur un incident non affecte | 403 |
| 7 | Utilisateur desactive qui se connecte | 403 |
| 8 | Inscription avec un email existant | 409 |
| 9 | Incident avec donnees invalides | 400 |

Tester l'expiration dans l'interface : attendre 5 minutes (`app.jwt.expiration-ms=300000`) puis cliquer sur une page,
ou mettre temporairement `app.jwt.expiration-ms=30000` (30 s). Vous etes renvoye vers `/login` avec le message
"Votre session a expire".

## 10. Configuration utile

`backend/src/main/resources/application.properties`

| Propriete | Role |
|---|---|
| `app.jwt.secret` | Cle secrete HMAC en Base64 (a changer en production) |
| `app.jwt.expiration-ms` | Duree de l'access token (300000 = 5 min) |
| `app.jwt.refresh-expiration-ms` | Duree du refresh token (7 jours) |
| `app.cors.allowed-origins` | Origines React autorisees |
| `app.seed.enabled` | Creation des comptes de test |

**MySQL** au lieu de H2 : creer l'acces MySQL (utilisateur/mot de passe dans `application-mysql.properties`), puis

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

La base `incidents_db` est creee automatiquement.

`frontend/.env` : `VITE_API_URL` (URL du backend) et `VITE_ENABLE_REFRESH` (`true` = renouvellement automatique du token).

## 11. Problemes frequents

| Symptome | Solution |
|---|---|
| `Port 8080 was already in use` | Fermer l'autre application ou ajouter `server.port=8081` (et mettre a jour `frontend/.env`) |
| Erreur CORS dans le navigateur | Verifier que React tourne bien sur `http://localhost:5173` |
| `409 Email deja utilise` en rejouant les tests | Redemarrer le backend (base en memoire) |
| `mvn` introuvable | Ouvrir `backend/` dans IntelliJ / Eclipse et lancer `IncidentManagerApplication` |
| `release version 17 not supported` | Installer un JDK 17 ou plus |

## 12. Documentation

- `docs/diagrammes.md` : architecture generale, diagramme des entites, sequence d'une requete protegee (Mermaid)
