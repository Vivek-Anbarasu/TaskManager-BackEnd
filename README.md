# Task Management Application

A RESTful Task Management API built with **Spring Boot 4**, secured with **JWT authentication**, and deployable via **Docker** and **Kubernetes (Helm)**.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [API Endpoints](#api-endpoints)
- [Roles & Permissions](#roles--permissions)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Running with Docker](#running-with-docker)
- [Configuration](#configuration)
  - [Environment Profiles](#environment-profiles)
  - [JWT Configuration](#jwt-configuration)
  - [Rate Limiting](#rate-limiting)
  - [CORS](#cors)
- [Deployment](#deployment)
  - [Helm (Kubernetes)](#helm-kubernetes)
  - [CI/CD (GitLab)](#cicd-gitlab)
- [Testing](#testing)
- [Actuator & Monitoring](#actuator--monitoring)
- [Swagger / API Docs](#swagger--api-docs)

---

## Tech Stack

| Category         | Technology                          |
|------------------|--------------------------------------|
| Language         | Java 25                              |
| Framework        | Spring Boot 4.0.3                    |
| Security         | Spring Security 7 + JWT (jjwt 0.13.0)|
| Persistence      | Spring Data JPA + Hibernate 7        |
| Database         | PostgreSQL                           |
| API Docs         | SpringDoc OpenAPI 3 (Swagger UI)     |
| Object Mapping   | MapStruct 1.6.3                      |
| Rate Limiting    | Bucket4j 8.15.0                      |
| Build Tool       | Maven                                |
| Containerisation | Docker (multi-stage build)           |
| Orchestration    | Kubernetes via Helm                  |
| CI/CD            | GitLab CI                            |
| Test Coverage    | JaCoCo (90% line coverage enforced)  |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/taskmanager/
│   │   ├── api/
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Request/Response DTOs
│   │   │   └── enums/               # Enumerations
│   │   ├── config/                  # Security, auditing config
│   │   ├── domain/
│   │   │   ├── model/               # JPA entities (Tasks, UserInfo)
│   │   │   └── repository/          # Spring Data repositories
│   │   ├── exception/               # Custom exceptions & global handler
│   │   ├── mapper/                  # MapStruct mappers
│   │   ├── security/                # JWT filter, UserDetails impl
│   │   ├── service/                 # Business logic services
│   │   ├── util/                    # Utility classes
│   │   └── validation/              # Custom validators
│   └── resources/
│       ├── application.yaml         # Default (local) config
│       ├── application-sit.yaml     # SIT config
│       ├── application-uat.yaml     # UAT config
│       └── application-prod.yaml    # Production config
├── test/                            # Unit & integration tests
helm/
└── taskmanager/                     # Helm chart
    ├── templates/                   # K8s resource templates
    ├── values.yaml                  # Default values
    ├── values-sit.yaml              # SIT overrides
    ├── values-uat.yaml              # UAT overrides
    └── values-prod.yaml             # Production overrides
```

---

## Features

- **JWT Authentication** — Stateless token-based auth with HS512 signing, issuer & audience validation
- **Token Refresh** — Dedicated endpoint to refresh JWT without re-authenticating
- **Role-Based Access Control** — `ADMIN` and `USER` roles with method-level `@PreAuthorize`
- **Rate Limiting** — Per-user request throttling using Bucket4j token bucket algorithm
- **JPA Auditing** — Automatic `created_by`, `created_date`, `last_modified_by`, `last_modified_date` on all entities
- **Input Validation** — Bean Validation (`@Valid`) on all request DTOs
- **Multi-Environment Profiles** — Separate configs for local, SIT, UAT, and PROD
- **Swagger UI** — Enabled in local/dev, disabled in SIT/UAT/PROD for security
- **Actuator** — Health probes (liveness/readiness) for Kubernetes
- **90% Test Coverage** — Enforced via JaCoCo at build time

---

## API Endpoints

### User Services (`/user`)

| Method | Endpoint              | Auth Required | Description               |
|--------|-----------------------|---------------|---------------------------|
| POST   | `/user/new-registration` | No         | Register a new user       |
| POST   | `/user/authenticate`  | No            | Login and get JWT token   |
| POST   | `/user/refresh-token` | Yes (Bearer)  | Refresh an existing token |

### Task Services (`/task`)

| Method | Endpoint     | Role          | Description              |
|--------|--------------|---------------|--------------------------|
| GET    | `/task/`     | USER or ADMIN | Get all tasks            |
| GET    | `/task/{id}` | USER or ADMIN | Get task by ID           |
| POST   | `/task/`     | ADMIN only    | Create a new task        |
| PUT    | `/task/`     | ADMIN only    | Update an existing task  |
| DELETE | `/task/{id}` | ADMIN only    | Delete a task            |

---

## Roles & Permissions

| Endpoint              | USER | ADMIN |
|-----------------------|------|-------|
| Register / Authenticate | ✅  | ✅   |
| Get all tasks         | ✅   | ✅   |
| Get task by ID        | ✅   | ✅   |
| Create task           | ❌   | ✅   |
| Update task           | ❌   | ✅   |
| Delete task           | ❌   | ✅   |

---

## Getting Started

### Prerequisites

- Java 25
- Maven 3.9+
- PostgreSQL (running locally on port `5432`)
- Docker (optional)
- Helm + kubectl (for Kubernetes deployment)

### Local Development

**1. Create the database:**
```sql
CREATE DATABASE taskdb;
```

**2. Configure credentials** in `src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/taskdb
    username: postgres
    password: your_password
```

**3. Run the application:**
```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

**4. Access Swagger UI:**

http://localhost:8080/swagger-ui.html

---

### Running with Docker

**Build and run using Docker Compose:**
```bash
docker-compose up --build
```

**Or build and run manually:**
```bash
# Build the image
docker build -t task-management-app:1.0 .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/taskdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  task-management-app:1.0
```

> The Dockerfile uses a **multi-stage build** with layered JAR extraction for optimal image size and Docker cache efficiency.

---

## Configuration

### Environment Profiles

Activate a profile using the `spring.profiles.active` property:

```bash
# SIT
mvn spring-boot:run -Dspring-boot.run.profiles=sit

# UAT
mvn spring-boot:run -Dspring-boot.run.profiles=uat

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

| Profile | Swagger | SQL Logging | Log Level       | CORS Origin                       |
|---------|---------|-------------|-----------------|-----------------------------------|
| default | ✅ ON   | ✅ ON       | DEBUG           | localhost:5173, localhost:3000     |
| sit     | ❌ OFF  | ❌ OFF      | INFO            | https://sit.taskmanager-app.com   |
| uat     | ❌ OFF  | ❌ OFF      | INFO            | https://uat.taskmanager-app.com   |
| prod    | ❌ OFF  | ❌ OFF      | WARN            | https://taskmanager-app.com       |

### JWT Configuration

```yaml
jwt:
  secret: <base64-encoded-secret>   # HS512 signing key
  issuer: https://taskmanager-app.com/user/authenticate
  audience: https://taskmanager-app.com
  expiration-ms: 900000             # 15 minutes
```

> ⚠️ Always override `jwt.secret` via environment variable or Kubernetes secret in non-local environments. Never commit real secrets.

### Rate Limiting

Configured per environment using a **token bucket** algorithm (Bucket4j):

| Setting              | Local (default) | SIT / UAT / PROD    |
|----------------------|-----------------|---------------------|
| Bucket capacity      | 60 tokens       | 100 / 1000 tokens   |
| Refill rate          | 60/min (greedy) | 1 token per 10 sec  |
| Tokens per request   | 1               | 1                   |
| HTTP response on limit | `429 Too Many Requests` | same    |

### CORS

```yaml
cors:
  allowed-origins: http://localhost:5173,http://localhost:3000
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
```

Override per environment in the respective `application-{profile}.yaml`.

---

## Deployment

### Helm (Kubernetes)

The Helm chart is located at `helm/taskmanager/` with environment-specific value files.

**Install to an environment:**
```bash
# SIT
make install-sit

# UAT
make install-uat

# Production
make install-prod
```

**Other useful Helm commands:**
```bash
make lint            # Lint the Helm chart
make template        # Render templates locally
make dry-run-sit     # Dry-run SIT deployment
make status-sit      # Check SIT pod/service/ingress status
make rollback-sit    # Rollback SIT to previous release
make history-prod    # View Production release history
make logs-prod       # Tail Production logs
make help            # List all available make targets
```

**Create Kubernetes secrets** (required before first deploy):
```bash
make create-secrets-sit
make create-secrets-uat
make create-secrets-prod
```

### CI/CD (GitLab)

The `.gitlab-ci.yml` defines the following pipeline stages:

| Stage   | Jobs                            | Trigger                        |
|---------|---------------------------------|--------------------------------|
| build   | Compile & package               | All branches                   |
| test    | Unit tests + JaCoCo coverage    | `main`, `develop`, releases    |
| package | Helm lint + chart package       | `main`, `develop`, tags        |
| deploy  | Deploy to SIT / UAT / PROD      | Manual trigger                 |

**Branch → Environment mapping:**

| Branch / Tag         | Environment |
|----------------------|-------------|
| `develop`            | SIT         |
| `release/*`          | UAT         |
| `main` / Git tags    | Production  |

**Required GitLab CI/CD variables:**

| Variable          | Description                              |
|-------------------|------------------------------------------|
| `KUBE_CONFIG_SIT` | Base64-encoded kubeconfig for SIT        |
| `KUBE_CONFIG_UAT` | Base64-encoded kubeconfig for UAT        |
| `KUBE_CONFIG_PROD`| Base64-encoded kubeconfig for Production |

---

## Testing

**Run all tests:**
```bash
mvn test
```

**Run tests with coverage report:**
```bash
mvn verify
```

Coverage report is generated at: `target/site/jacoco/index.html`

> ⚠️ The build will **fail** if line coverage drops below **90%** (enforced by JaCoCo).

---

## Actuator & Monitoring

The following actuator endpoints are exposed:

| Endpoint                  | Description                        |
|---------------------------|------------------------------------|
| `/actuator/health`        | Overall health status              |
| `/actuator/health/liveness`  | Kubernetes liveness probe       |
| `/actuator/health/readiness` | Kubernetes readiness probe      |
| `/actuator/info`          | Application build info             |
| `/actuator/metrics`       | Application metrics                |
| `/actuator/env`           | Environment properties             |

---

## Swagger / API Docs

Swagger UI is available **only in local/dev** environment:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

All `/task/**` endpoints require a **Bearer token** in the `Authorization` header. Use the `/user/authenticate` endpoint first to obtain a token, then click **Authorize** in Swagger UI and enter `Bearer <your-token>`.

> Swagger is disabled in SIT, UAT, and PROD profiles for security.

