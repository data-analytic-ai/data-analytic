# Data Bridge — STARTUP & DEVELOPMENT GUIDE

> Single-source, copy‑pasteable guide for running and debugging the project locally or in a prod‑like Docker setup.

---

## Architecture Overview

Data Bridge is a Spring Boot application that orchestrates data migration between multiple databases:

- **Source Database**: Oracle XE 21c (reads from here)
- **Target Database**: PostgreSQL 15 (writes here)
- **Job Repository**: PostgreSQL 15 (Spring Batch metadata)
- **State Store**: MongoDB 6 (application state / CMS‑style storage)

---

## Prerequisites

- **Docker** & **Docker Compose v2** (`docker compose ...`)
- **Java 21 JDK** (for local IntelliJ development)
- **IntelliJ IDEA** (recommended) or another Java IDE
- **Maven 3.8+** (bundled with IntelliJ is fine)

---

## Project Structure

```
data-bridge/
├── src/                                  # Spring Boot source code
├── infra/
│   └── databases/
│       ├── docker-compose.yml            # Databases only (Oracle, Postgres x2, Mongo)
│       └── schema/                       # Initialization scripts
├── docker-compose.yml                    # Root: app service (merges with infra compose)
├── Dockerfile                            # App image
├── src/main/resources/
│   ├── application.properties            # Base (common) settings — no hostnames
│   ├── application-docker.properties     # App-in-Docker profile
│   └── application-localdev.properties   # IntelliJ-on-host profile
└── README/STARTUP docs                   # This file (STARTUP.md)
```

---

## Configuration Profiles

### `localdev` (IntelliJ on host)
- App runs on your **machine**.
- App connects to Dockerized databases via **localhost** published ports.
- Ideal for daily development/debugging with breakpoints.

### `docker` (App runs in Docker)
- App runs inside the **Docker network**.
- App connects to databases by **service DNS** (e.g., `job-repo-db`).
- Prod‑like, great for full‑stack verification. JDWP remote debug supported.

> The **base** `application.properties` contains only common settings. Host-specific URLs live in profile files.

---

## Connection Matrix

| Database        | IntelliJ (localdev → localhost) | App-in-Docker (docker → service DNS) | Credentials            |
|-----------------|----------------------------------|--------------------------------------|------------------------|
| **Job Repo (PG)** | `localhost:5433`                 | `job-repo-db:5432`                   | `postgres / postgres`  |
| **Target (PG)** | `localhost:5432`                 | `target-db:5432`                     | `postgres / postgres`  |
| **State (Mongo)** | `localhost:27017`                | `state-store:27017`                  | `root / password`      |
| **Source (Oracle)** | `localhost:1521`               | `source-db:1521`                     | `DATAANALYTIC / oracle`|

---

## Option A — Run from IntelliJ (Recommended for Development)

### 1) Start database services

```bash
# from project root
cd infra/databases

docker compose up -d

docker compose ps
```

Healthy example output:

```
NAME               STATUS          PORTS
postgres-target    Up (healthy)    0.0.0.0:5432->5432/tcp
postgres-job-repo  Up (healthy)    0.0.0.0:5433->5432/tcp
mongo-storage      Up (healthy)    0.0.0.0:27017->27017/tcp
oracle-source      Up (healthy)    0.0.0.0:1521->1521/tcp
```

### 2) IntelliJ Run/Debug configuration

1. **Run → Edit Configurations… → + → Spring Boot**
2. **Name**: `DataAnalyticApplication`
3. **Main class**: `ai.dataanalytic.DataAnalyticApplication`
4. **Active profiles**: `localdev`
5. **Environment variables** (copy one line):
   - **Windows** (semicolon `;` separated):
     ```
     BRIDGE_SOURCE_JDBC_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1;BRIDGE_SOURCE_USERNAME=DATAANALYTIC;BRIDGE_SOURCE_PASSWORD=oracle;BRIDGE_TARGET_JDBC_URL=jdbc:postgresql://localhost:5432/data_analytic_target;BRIDGE_TARGET_USERNAME=postgres;BRIDGE_TARGET_PASSWORD=postgres;BRIDGE_JOB_REPO_JDBC_URL=jdbc:postgresql://localhost:5433/data_analytic_batch;BRIDGE_JOB_REPO_USERNAME=postgres;BRIDGE_JOB_REPO_PASSWORD=postgres;BRIDGE_STATE_STORE_URI=mongodb://root:password@localhost:27017/dataAnalytic?authSource=admin
     ```
   - **macOS/Linux** (colon `:` separated):
     ```
     BRIDGE_SOURCE_JDBC_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1:BRIDGE_SOURCE_USERNAME=DATAANALYTIC:BRIDGE_SOURCE_PASSWORD=oracle:BRIDGE_TARGET_JDBC_URL=jdbc:postgresql://localhost:5432/data_analytic_target:BRIDGE_TARGET_USERNAME=postgres:BRIDGE_TARGET_PASSWORD=postgres:BRIDGE_JOB_REPO_JDBC_URL=jdbc:postgresql://localhost:5433/data_analytic_batch:BRIDGE_JOB_REPO_USERNAME=postgres:BRIDGE_JOB_REPO_PASSWORD=postgres:BRIDGE_STATE_STORE_URI=mongodb://root:password@localhost:27017/dataAnalytic?authSource=admin
     ```
6. **Apply → OK**
7. **Run/Debug** the configuration.

App will start at <http://localhost:8081>.

### 3) Stop databases when done

```bash
cd infra/databases

# stop containers (keep volumes)
docker compose down

# or remove volumes too (wipes data)
docker compose down -v
```

---

## Option B — Run Full Stack in Docker (Prod‑like)

This runs the app + all databases in Docker. Remote debug via JDWP is enabled on **port 5005**.

### 1) Start everything

```bash
# from project root

docker compose -f docker-compose.yml -f infra/databases/docker-compose.yml up -d --build

# follow app logs
docker compose logs -f databridge-app
```

### 2) Attach IntelliJ remote debugger (optional)

- **Run → Edit Configurations… → + → Remote JVM Debug**
- Host: `localhost`, Port: `5005`
- Click **Debug** to attach.

> The root compose sets `JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005` for the app container.

### 3) Verify

- App: <http://localhost:8081>
- Health: <http://localhost:8081/actuator/health>

### 4) Stop

```bash
# stop app+dbs

docker compose -f docker-compose.yml -f infra/databases/docker-compose.yml down

# or wipe volumes too

docker compose -f docker-compose.yml -f infra/databases/docker-compose.yml down -v
```

---

## Database Access Tools (inside containers)

### PostgreSQL (Target & Job Repo)
```bash
# Target
docker exec -it postgres-target psql -U postgres -d data_analytic_target

# Job Repo
docker exec -it postgres-job-repo psql -U postgres -d data_analytic_batch
```

### MongoDB (State Store)
```bash
docker exec -it mongo-storage mongosh -u root -p password --authenticationDatabase admin
```

### Oracle (Source)
```bash
# From host → service port
sqlplus DATAANALYTIC/oracle@//localhost:1521/XEPDB1

# From inside the container network
docker exec -it oracle-source sqlplus DATAANALYTIC/oracle@//source-db:1521/XEPDB1
```

---

## Troubleshooting

### Unknown host / Connection refused
- Ensure services are healthy:
  ```bash
  cd infra/databases
  docker compose ps
  ```
- Check logs for failing service:
  ```bash
  docker compose logs -f <service-name>
  ```
- **IntelliJ on host?** Use profile `localdev` and the **localhost** ports (e.g., job repo at `localhost:5433`).
- **App in Docker?** Use profile `docker` (service DNS like `job-repo-db`).

### Oracle takes a while
First run may take 2–5 minutes. Wait for readiness:
```bash
docker compose logs oracle-source | grep "DATABASE IS READY TO USE"
```

### Port clashes
Common conflicts:
- `5432` local Postgres
- `5433` another local DB
- `27017` local Mongo
- `1521` local Oracle

Edit the `ports:` in the compose files or stop the conflicting service.

### Spring Batch schema missing
Ensure in **any** active profile:
```properties
spring.batch.jdbc.initialize-schema=ALWAYS
```

---

## Development Workflow

**Day‑to‑day (localdev):**
1. `cd infra/databases && docker compose up -d`
2. Run/Debug from IntelliJ (profile `localdev`)
3. Iterate on code; restart app as needed
4. `docker compose down` when finished (optional)

**Prod‑like test (docker):**
1. Stop IntelliJ app
2. `docker compose -f docker-compose.yml -f infra/databases/docker-compose.yml up -d --build`
3. Optional remote debug attach (5005)
4. `... down` when done

**Schema updates:**
1. Add SQL/JS under `infra/databases/schema/...`
2. Recreate a single DB service:
   ```bash
   cd infra/databases
   docker compose stop <service>
   docker compose rm -f <service>
   docker volume rm <volume-if-any>
   docker compose up -d <service>
   ```

---

## Reference — Profile Properties

**`src/main/resources/application.properties` (base/common)**
- Common flags only (no hostnames): server port, logging, actuator, batch flags, etc.

**`src/main/resources/application-docker.properties`** (app runs in Docker)
```properties
spring.datasource.url=${BRIDGE_JOB_REPO_JDBC_URL:jdbc:postgresql://job-repo-db:5432/data_analytic_batch}
spring.datasource.username=${BRIDGE_JOB_REPO_USERNAME:postgres}
spring.datasource.password=${BRIDGE_JOB_REPO_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.data.mongodb.uri=${BRIDGE_STATE_STORE_URI:mongodb://root:password@state-store:27017/dataAnalytic?authSource=admin}
```

**`src/main/resources/application-localdev.properties`** (IntelliJ on host)
```properties
spring.datasource.url=${BRIDGE_JOB_REPO_JDBC_URL:jdbc:postgresql://localhost:5433/data_analytic_batch}
spring.datasource.username=${BRIDGE_JOB_REPO_USERNAME:postgres}
spring.datasource.password=${BRIDGE_JOB_REPO_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.data.mongodb.uri=${BRIDGE_STATE_STORE_URI:mongodb://root:password@localhost:27017/dataAnalytic?authSource=admin}
```

---

## Useful Docker Commands

```bash
# View all service logs
docker compose logs -f

# Follow specific service
docker compose logs -f postgres-job-repo

# Restart one service
docker compose restart databridge-app

# Rebuild app image & start
docker compose up --build databridge-app

# Resource usage
docker stats

# Cleanup unused images/containers
docker system prune -a
```

---

## Production Deployment Notes (High-level)

- Root compose is suitable for local/staging. For production, prefer an orchestrator (Kubernetes, ECS, etc.).
- Externalize secrets (Vault/Secrets Manager/K8s Secrets) and enable TLS.
- Use persistent volumes/backups/monitoring.
- Keep health checks, liveness/readiness probes, and structured logging.

---

**Last Updated:** October 2025

