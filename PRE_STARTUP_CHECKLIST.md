# ✅ PRE-STARTUP CHECKLIST

## 🔧 Prérequis Système

### Java & Maven
- [ ] Java 17+ installé
  ```powershell
  java -version
  # Attendu: openjdk 17.x ou supérieur
  ```

- [ ] Maven 3.8+ installé
  ```powershell
  mvn --version
  # Attendu: Apache Maven 3.8.x ou supérieur
  ```

### Dépendances Externes

- [ ] PostgreSQL 15+ démarré et accessible
  ```powershell
  # Tester la connexion
  psql -h localhost -U postgres -d wallet_db -c "SELECT 1"
  # Ou créer la BD si elle n'existe pas:
  psql -h localhost -U postgres -c "CREATE DATABASE wallet_db;"
  ```

- [ ] Kafka 3.0+ démarré et accessible
  ```powershell
  # Tester la connexion
  # Ou vérifier que Zookeeper (2181) et Kafka (9092) écoutent
  netstat -ano | findstr "2181\|9092"
  ```

---

## 🛠️ Compilation & Préparation

- [ ] Projet compilé avec succès
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn clean compile -q -DskipTests
  # Attendu: BUILD SUCCESS
  ```

- [ ] Configurations mises à jour
  - [ ] `auth/src/main/resources/application.properties` ✅
  - [ ] `transaction-service/src/main/resources/application.properties` ✅
  - [ ] `user-service/src/main/resources/application.properties` ✅
  - [ ] `wallet-service/src/main/resources/application.yml` ✅

- [ ] Docker Compose actualisé
  - [ ] `docker/docker-compose.yml` avec Kafka ✅

---

## 📂 Fichiers de Démarrage

- [ ] Scripts `.bat` créés et accessibles:
  - [ ] `start-eureka.bat` ✅
  - [ ] `start-config-server.bat` ✅
  - [ ] `start-auth-service.bat` ✅
  - [ ] `start-wallet-service.bat` ✅
  - [ ] `start-transaction-service.bat` ✅
  - [ ] `start-notification-service.bat` ✅

---

## 📚 Documentation

- [ ] Documentation lue:
  - [ ] `RESUME_EXECUTIF.md` ✅
  - [ ] `PLAN_TEST_MICROSERVICES.md` ✅
  - [ ] `COMMANDES_RAPIDES.md` ✅

---

## 🖥️ Environnement Exécution

- [ ] Ports disponibles (non utilisés):
  ```powershell
  # Vérifier que ces ports sont libres:
  $ports = @(8761, 8888, 8081, 8082, 8083, 8084, 5432, 9092, 2181)
  $ports | ForEach-Object {
    $conn = New-Object System.Net.Sockets.TcpClient
    try {
      $conn.Connect("localhost", $_)
      Write-Host "Port $_ : OCCUPÉ (utilisateur)"
      $conn.Close()
    } catch {
      Write-Host "Port $_ : LIBRE ✓"
    }
  }
  ```

- [ ] 8+ GB RAM disponibles
  ```powershell
  Get-ComputerInfo | Select-Object @{
    Label="RAM Total (GB)"
    Expression={[math]::Round($_.CsPhyicallyInstalledSystemMemory/1GB,2)}
  }
  ```

- [ ] 5+ GB espace disque libre
  ```powershell
  Get-Volume -DriveLetter C | Select-Object SizeRemaining
  ```

---

## 🚀 Plan de Démarrage

### Phase 1: Infrastructure (5-10 min)
- [ ] PostgreSQL démarré
  ```powershell
  # Windows Service ou via installer
  net start postgresql-x64-15
  # Ou via WSL/Docker
  docker run -d -e POSTGRES_PASSWORD=1234 postgres:15-alpine
  ```

- [ ] Kafka démarré
  ```powershell
  # Via Docker Compose:
  docker-compose -f docker/docker-compose.yml up -d zookeeper kafka
  # Ou installation locale
  ```

- [ ] Database `wallet_db` créée
  ```sql
  CREATE DATABASE wallet_db;
  ```

### Phase 2: Microservices (ordre strict)

#### ✅ Terminal 1 - Eureka Server (8761)
- [ ] Ouvrir Terminal 1 (PowerShell)
- [ ] Exécuter:
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn -pl service-registry spring-boot:run
  ```
- [ ] Attendre: "Tomcat started on port(s): 8761"
- [ ] Vérifier: http://localhost:8761 ✓
- [ ] Procédez au service suivant

#### ✅ Terminal 2 - Config Server (8888)
- [ ] Ouvrir Terminal 2 (PowerShell)
- [ ] Attendre 5 secondes
- [ ] Exécuter:
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn -pl config-server spring-boot:run
  ```
- [ ] Attendre: "Tomcat started on port(s): 8888"
- [ ] Vérifier: `Invoke-WebRequest http://localhost:8888/health`
- [ ] Procédez au service suivant

#### ✅ Terminal 3 - Auth Service (8081)
- [ ] Ouvrir Terminal 3 (PowerShell)
- [ ] Attendre 5 secondes
- [ ] Exécuter:
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn -pl auth spring-boot:run
  ```
- [ ] Attendre: "Started AuthApplication in"
- [ ] Vérifier: `Invoke-WebRequest http://localhost:8081/actuator/health`
- [ ] Procédez au service suivant

#### ✅ Terminal 4 - Wallet Service (8082)
- [ ] Ouvrir Terminal 4 (PowerShell)
- [ ] Attendre 10 secondes
- [ ] Exécuter:
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn -pl wallet-service spring-boot:run
  ```
- [ ] Attendre: "Started WalletApplication in"
- [ ] Vérifier: `Invoke-WebRequest http://localhost:8082/actuator/health`
- [ ] Procédez au service suivant

#### ✅ Terminal 5 - Transaction Service (8083)
- [ ] Ouvrir Terminal 5 (PowerShell)
- [ ] Attendre 10 secondes
- [ ] Exécuter:
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn -pl transaction-service spring-boot:run
  ```
- [ ] Attendre: "Started TransactionApplication in"
- [ ] Vérifier: `Invoke-WebRequest http://localhost:8083/actuator/health`
- [ ] Procédez au service suivant

#### ✅ Terminal 6 - Notification Service (8084)
- [ ] Ouvrir Terminal 6 (PowerShell)
- [ ] Attendre 10 secondes
- [ ] Exécuter:
  ```powershell
  cd C:\Users\HP\Downloads\zaphira-15-12-2025
  mvn -pl notification-service spring-boot:run
  ```
- [ ] Attendre: "Started NotificationApplication in"
- [ ] Vérifier: `Invoke-WebRequest http://localhost:8084/actuator/health`
- [ ] Procédez aux tests

---

## 🧪 Tests Rapides Immédiats

### Test 1: Vérifier tous les services
```powershell
# Dans n'importe quel Terminal (pas les 6 actifs)

# Eureka Dashboard
Invoke-WebRequest http://localhost:8761

# Santé de tous les services
@(8761, 8888, 8081, 8082, 8083, 8084) | ForEach-Object {
  Write-Host "Port $_:"
  Invoke-WebRequest http://localhost:$_/actuator/health -ErrorAction SilentlyContinue
}
```

### Test 2: Créer un utilisateur
```powershell
$body = @{
    email = "test@example.com"
    password = "Test123!"
    firstName = "Test"
    lastName = "User"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8081/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $body
```

### Test 3: Login
```powershell
$loginBody = @{
    email = "test@example.com"
    password = "Test123!"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type" = "application/json"} `
  -Body $loginBody

$response.Content | ConvertFrom-Json
```

---

## 🔄 Ordre de Fermeture (à la fin)

Pour fermer proprement (inverse de l'ouverture):

1. [ ] Ctrl+C dans Terminal 6 (Notification Service)
2. [ ] Ctrl+C dans Terminal 5 (Transaction Service)
3. [ ] Ctrl+C dans Terminal 4 (Wallet Service)
4. [ ] Ctrl+C dans Terminal 3 (Auth Service)
5. [ ] Ctrl+C dans Terminal 2 (Config Server)
6. [ ] Ctrl+C dans Terminal 1 (Eureka Server)
7. [ ] Arrêter Kafka: `docker-compose down`
8. [ ] Arrêter PostgreSQL: `net stop postgresql-x64-15`

---

## ⚠️ Problèmes Courants

### Port déjà utilisé
```powershell
# Trouver le processus
Get-NetTCPConnection -LocalPort 8081 | Select-Object OwningProcess

# Tuer le processus
Stop-Process -Id {PID} -Force
```

### PostgreSQL Connection Error
```
SOLUTION:
1. Vérifier que PostgreSQL est lancé
2. Vérifier que la base wallet_db existe
3. Vérifier les credentials (postgres/1234)
```

### Kafka Connection Error
```
SOLUTION:
1. Vérifier que Kafka est lancé
2. Vérifier que Zookeeper est lancé (2181)
3. Vérifier bootstrap.servers=localhost:9092
```

### Eureka Service Not Registered
```
SOLUTION:
Attendre 30-60 secondes après le démarrage
L'enregistrement est asynchrone
```

---

## 📋 Checklist Finale

Avant de démarrer les tests complets:

- [ ] Tous les 6 services démarrés et affichent "Tomcat started"
- [ ] Eureka Dashboard visible sur http://localhost:8761
- [ ] Tous les services affichent "UP" sur /actuator/health
- [ ] PostgreSQL accessible et BD wallet_db existe
- [ ] Kafka/Zookeeper accessibles
- [ ] Au moins 3 GB RAM libres
- [ ] Terminal 1-6 ne montrent pas d'erreurs
- [ ] Vous avez les 3 documents principaux ouverts:
  - RESUME_EXECUTIF.md
  - PLAN_TEST_MICROSERVICES.md
  - COMMANDES_RAPIDES.md

---

## ✅ PRÊT À COMMENCER!

Si tous les checkmarks sont cochés ✓, vous êtes prêt!

**Prochaine étape:** Suivez le scénario "Test Flux Complet E2E" dans [PLAN_TEST_MICROSERVICES.md](./PLAN_TEST_MICROSERVICES.md)

---

**Document généré:** 19 Décembre 2025
**Statut:** ✅ SYSTÈME PRÊT POUR TEST
