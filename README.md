# resume-match-api

A **Java 21 / Spring Boot 3** REST API that exposes full CRUD access to all
entities of the [Resume Matcher](https://github.com/srbhr/Resume-Matcher)
application. Designed to be consumed by a future front-end application.

Swagger UI is served at `http://localhost:8080/swagger-ui.html` — no
authentication is required in this first release.

---

## Entities

| Entity | Table | Description |
|---|---|---|
| **Resume** | `resumes` | Master and tailored resume documents |
| **Job** | `jobs` | Job description documents |
| **Improvement** | `improvements` | LLM tailoring results (resume ↔ job) |
| **Application** | `applications` | Kanban job-application tracker cards |
| **ApiKey** | `api_keys` | Encrypted LLM-provider API keys |

---

## API Endpoints

| Resource | Base path |
|---|---|
| Resumes | `GET/POST /api/v1/resumes` |
| Resume by ID | `GET/PUT/DELETE /api/v1/resumes/{resumeId}` |
| Master resume | `GET /api/v1/resumes/master` |
| Tailored resumes | `GET /api/v1/resumes/{resumeId}/tailored` |
| Jobs | `GET/POST /api/v1/jobs` |
| Job by ID | `GET/PUT/DELETE /api/v1/jobs/{jobId}` |
| Jobs by resume | `GET /api/v1/jobs/by-resume/{resumeId}` |
| Improvements | `GET/POST /api/v1/improvements` |
| Improvement by ID | `GET/PUT/DELETE /api/v1/improvements/{requestId}` |
| Improvements by resume | `GET /api/v1/improvements/by-resume/{resumeId}` |
| Improvements by job | `GET /api/v1/improvements/by-job/{jobId}` |
| Applications | `GET/POST /api/v1/applications` |
| Application by ID | `GET/PUT/DELETE /api/v1/applications/{applicationId}` |
| Applications by status | `GET /api/v1/applications/by-status/{status}` |
| Applications by job | `GET /api/v1/applications/by-job/{jobId}` |
| Applications by resume | `GET /api/v1/applications/by-resume/{resumeId}` |
| API Keys | `GET /api/v1/api-keys` |
| API Key by provider | `GET/PUT/DELETE /api/v1/api-keys/{provider}` |
| Health | `GET /api/v1/health` |

Full interactive documentation is available in Swagger UI.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| JSON column type | Hypersistence Utils (`@Type(JsonType.class)`) |
| API docs | SpringDoc OpenAPI 2 + Swagger UI |
| Build | Maven 3.9 (wrapper included) |
| Containerisation | Docker (multi-stage) + Docker Compose |

---

## Getting Started

### Prerequisites

- Java 21+
- Docker & Docker Compose

### Run with Docker Compose (recommended)

```bash
# 1. Copy the environment template and fill in values
cp .env.example .env

# 2. Start PostgreSQL + the API
docker compose up --build

# 3. Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Run locally

```bash
# 1. Start only PostgreSQL
docker compose up postgres -d

# 2. Copy and edit the environment file
cp .env.example .env

# 3. Export environment variables (or use your IDE's run config)
export $(grep -v '^#' .env | xargs)

# 4. Build and run
./mvnw spring-boot:run
```

### Run tests

```bash
./mvnw test
```

Tests use an H2 in-memory database — no external services required.

---

## Configuration

All configuration is done via environment variables (see `.env.example`):

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL hostname |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `resume_match` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `SERVER_PORT` | `8080` | Application port |
| `LOG_LEVEL` | `INFO` | Root log level (`DEBUG`, `INFO`, `WARNING`, `ERROR`) |
| `CORS_ORIGINS` | `*` | Allowed CORS origins (`*` or comma-separated list) |

---

## Project Structure

```
src/
└── main/
    ├── java/com/resumematch/api/
    │   ├── ResumeMatchApiApplication.java   # Entry point
    │   ├── config/
    │   │   ├── OpenApiConfig.java           # Swagger / OpenAPI metadata
    │   │   └── WebConfig.java               # CORS configuration
    │   ├── controller/                      # REST controllers
    │   │   ├── ResumeController.java
    │   │   ├── JobController.java
    │   │   ├── ImprovementController.java
    │   │   ├── ApplicationController.java
    │   │   ├── ApiKeyController.java
    │   │   └── HealthController.java
    │   ├── dto/                             # Request/response DTOs
    │   ├── entity/                          # JPA entity classes
    │   ├── exception/                       # Exception types + global handler
    │   ├── repository/                      # Spring Data JPA repositories
    │   └── service/                         # Business logic
    └── resources/
        └── application.yml                  # Spring configuration
```

---

## License

Apache License 2.0 — see [LICENSE](https://www.apache.org/licenses/LICENSE-2.0).
