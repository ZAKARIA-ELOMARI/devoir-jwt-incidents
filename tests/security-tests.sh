#!/usr/bin/env bash
# Scenario complet (section 24) + tests de securite obligatoires (section 25) avec curl.
# Pre-requis : backend demarre sur http://localhost:8080 (base H2 vide au demarrage)
# Utilisation  : bash tests/security-tests.sh
# (Windows : executer dans Git Bash. Sinon utilisez la collection Postman.)

API="${API:-http://localhost:8080}"
EXPIRED_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJlbXAxQGVuc2V0Lm1hIiwicm9sZSI6IkVNUExPWUVFIiwiaWF0IjoxNzYwMDAwMDAwLCJleHAiOjE3NjAwMDAzMDB9.jTJ22l6G09A9SzsVJi38HrexOyDClNQiQCUwsIVGuQQ"
PASS=0; FAIL=0

# call METHOD PATH TOKEN [JSON]  -> remplit $CODE et $BODY
call() {
  local method="$1" path="$2" token="$3" data="$4"
  local args=(-s -w '\n%{http_code}' -X "$method" "$API$path" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-d "$data")
  local out; out=$(curl "${args[@]}")
  CODE=$(echo "$out" | tail -n1)
  BODY=$(echo "$out" | sed '$d')
}

# expect "description" CODE_ATTENDU
expect() {
  if [ "$CODE" = "$2" ]; then PASS=$((PASS+1)); echo "  [OK]   $1 -> $CODE"
  else FAIL=$((FAIL+1)); echo "  [FAIL] $1 -> attendu $2, recu $CODE : $BODY"; fi
}

token_of() { echo "$BODY" | sed -E 's/.*"token":"([^"]+)".*/\1/'; }
first_id() { echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }

echo "=== Connexion des comptes de test ==="
call POST /auth/login "" '{"email":"admin@enset.ma","password":"Admin123!"}';  expect "login ADMIN" 200; ADMIN=$(token_of)
call POST /auth/login "" '{"email":"tech1@enset.ma","password":"Tech123!"}';  expect "login TECHNICIAN" 200; TECH=$(token_of)
call POST /auth/login "" '{"email":"tech2@enset.ma","password":"Tech123!"}';  expect "login TECHNICIAN 2" 200; TECH2=$(token_of)
call POST /auth/login "" '{"email":"emp1@enset.ma","password":"Emp123!"}';    expect "login EMPLOYEE 1" 200; EMP1=$(token_of)
call POST /auth/login "" '{"email":"emp2@enset.ma","password":"Emp123!"}';    expect "login EMPLOYEE 2" 200; EMP2=$(token_of)

echo; echo "=== Scenario (section 24) ==="
call POST /api/incidents "$EMP1" '{"title":"PC infecte","description":"Ecran de rancon affiche","category":"MALWARE","priority":"CRITICAL"}'
expect "EMPLOYEE 1 declare l'incident 1" 201; INC1=$(first_id)
call POST /api/incidents "$EMP1" '{"title":"Email suspect","description":"Lien de phishing recu","category":"PHISHING","priority":"HIGH"}'
expect "EMPLOYEE 1 declare l'incident 2" 201
call POST /api/incidents "$EMP2" '{"title":"Panne reseau","description":"Plus de connexion au 2e etage","category":"NETWORK","priority":"MEDIUM"}'
expect "EMPLOYEE 2 declare l'incident 3" 201; INC3=$(first_id)

call GET "/api/incidents/$INC3" "$EMP1";           expect "EMPLOYEE 1 ne voit pas l'incident de EMPLOYEE 2" 403
call GET /api/incidents "$ADMIN";                  expect "ADMIN consulte tous les incidents" 200

# id du technicien A (premier utilisateur TECHNICIAN de la liste)
call GET /api/admin/users "$ADMIN"
TECH_ID=$(echo "$BODY" | tr '{' '\n' | grep 'tech1@enset.ma' | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
call PUT "/api/incidents/$INC1/assign/$TECH_ID" "$ADMIN"; expect "ADMIN affecte l'incident 1 au technicien" 200
call GET /api/incidents/assigned "$TECH";          expect "TECHNICIAN consulte ses incidents" 200
call PUT "/api/incidents/$INC1/status" "$TECH" '{"status":"IN_PROGRESS"}'; expect "TECHNICIAN : -> IN_PROGRESS" 200
call PUT "/api/incidents/$INC1/status" "$TECH" '{"status":"RESOLVED"}';    expect "TECHNICIAN : IN_PROGRESS -> RESOLVED" 200
call POST "/api/incidents/$INC1/comments" "$TECH" '{"content":"Le poste a ete isole du reseau et le malware supprime."}'
expect "TECHNICIAN ajoute une note" 200
call GET "/api/incidents/$INC1" "$EMP1";           expect "EMPLOYEE 1 consulte l'etat + historique" 200
echo "$BODY" | grep -o '"message":"[^"]*"' | sed 's/^/        historique : /'

echo; echo "=== Tests de securite obligatoires (section 25) ==="
call GET /api/profile "";                          expect "1. API protegee sans JWT" 401
call GET /api/profile "abc.def.invalide";          expect "2. JWT invalide" 401
call GET /api/profile "$EXPIRED_TOKEN";            expect "3. JWT expire" 401
call GET /api/admin/users "$EMP1";                 expect "4. EMPLOYEE sur une route ADMIN" 403
call GET "/api/incidents/$INC3" "$EMP1";           expect "5. EMPLOYEE sur l'incident d'un autre" 403
call PUT "/api/incidents/$INC3/status" "$TECH2" '{"status":"IN_PROGRESS"}'
expect "6. TECHNICIAN sur un incident non affecte" 403

# 7. utilisateur desactive
call POST /auth/register "" '{"fullName":"Compte Test","email":"desactive@enset.ma","password":"secret123"}'; expect "   (creation du compte a desactiver)" 201
NEWID=$(first_id)
call PUT "/api/admin/users/$NEWID/disable" "$ADMIN"; expect "   (ADMIN desactive le compte)" 200
call POST /auth/login "" '{"email":"desactive@enset.ma","password":"secret123"}'; expect "7. utilisateur desactive ne peut pas se connecter" 403

call POST /auth/register "" '{"fullName":"Doublon","email":"emp1@enset.ma","password":"secret123"}'; expect "8. inscription avec un email existant" 409
call POST /api/incidents "$EMP1" '{"title":"","description":"","priority":null}'; expect "9. incident avec donnees invalides" 400

echo; echo "=== Regles metier supplementaires ==="
call POST /auth/register "" '{"fullName":"Pirate","email":"pirate@enset.ma","password":"secret123","role":"ADMIN"}'
expect "inscription avec role=ADMIN (champ ignore)" 201
echo "$BODY" | grep -q '"role":"EMPLOYEE"' && echo "        role attribue : EMPLOYEE" || { FAIL=$((FAIL+1)); echo "  [FAIL] le role n'est pas EMPLOYEE"; }
call PUT "/api/incidents/$INC1/status" "$ADMIN" '{"status":"CLOSED"}'; expect "ADMIN cloture l'incident 1" 200
call PUT "/api/incidents/$INC1/status" "$TECH" '{"status":"IN_PROGRESS"}'; expect "incident CLOSED non modifiable" 400
call PUT "/api/incidents/$INC1/assign/$TECH_ID" "$EMP1"; expect "EMPLOYEE ne peut pas affecter" 403

echo; echo "Resultat : $PASS reussis, $FAIL echoues"
[ "$FAIL" -eq 0 ]
