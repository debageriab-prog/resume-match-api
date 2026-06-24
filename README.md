# resume-match-api

A **Spring Boot REST API** that exposes full CRUD endpoints and a Swagger UI for all entities in the
[resume-matcher](https://github.com/debageriab-prog/resume-matcher) application.
Intended to serve as the backend for a front-end application — no security layer is included at this stage.

---

## Entities

| Entity             | Table               | Description                                         |
|--------------------|---------------------|-----------------------------------------------------|
| `Assignment`       | `assignment`        | A job/assignment posting                            |
| `AssignmentIndex`  | `assignment_index`  | Tracks which assignments have been indexed (entity only, no API) |
| `AssignmentSeeker` | `assignment_seeker` | A job seeker (user)                                 |
| `Resume`           | `resume`            | Resume belonging to an `AssignmentSeeker`           |
| `ResumeMatch`      | `resume_match`      | Match result between a `Resume` and an `Assignment` |

---

## API Endpoints

### Assignments — `/api/assignments`

| Method   | Path                  | Description                                         |
|----------|-----------------------|-----------------------------------------------------|
| `GET`    | `/`                   | List with pagination and optional filters (jobId, title, client, location, portal) |
| `GET`    | `/{id}`               | Get by ID                                           |
| `POST`   | `/`                   | Create                                              |
| `PUT`    | `/{id}`               | Update                                              |
| `DELETE` | `/{id}`               | Delete (cascades to ResumeMatch rows)               |

### Assignment Seekers — `/api/assignment-seekers`

| Method   | Path    | Description                |
|----------|---------|----------------------------|
| `GET`    | `/`     | List with pagination       |
| `GET`    | `/{id}` | Get by ID                  |
| `POST`   | `/`     | Create                     |
| `PUT`    | `/{id}` | Update                     |
| `DELETE` | `/{id}` | Delete                     |

### Resumes — `/api/resumes`

GET endpoints return a `ResumeSummaryDto` — full PDF bytes and extracted text are not exposed. The summary includes:

| Field | Source |
|---|---|
| `id` | Resume ID |
| `owner` | `AssignmentSeeker` entity |
| `managerEmail` | Resume field |
| `notificationType` | Resume field |
| `fileName` | Original uploaded file name |
| `createdAt` | Resume creation timestamp |
| `matchedCount` | Count of `ResumeMatch` rows where `decision IS NOT NULL AND decision != 'no'` |

The `PUT /{id}` endpoint returns a `ResumeUpdateDto` with only the mutable fields (`id`, `owner`, `managerEmail`, `notificationType`) — no `fileName`, `createdAt`, or `matchedCount`.

| Method   | Path                    | Description                                                      |
|----------|-------------------------|------------------------------------------------------------------|
| `GET`    | `/`                     | List with pagination (`ResumeSummaryDto`)                        |
| `GET`    | `/{id}`                 | Get summary by ID (`ResumeSummaryDto`)                           |
| `GET`    | `/owner/{ownerId}`      | List resumes for a seeker with pagination (`ResumeSummaryDto`)   |
| `GET`    | `/statistics`           | Total, today, last-week, last-month resume counts                |
| `GET`    | `/topmatched`           | Top 5 most recently uploaded resumes with at least one positive match |
| `POST`   | `/upload`               | Upload a PDF — LLM extracts owner identity, stores parsed profile |
| `PUT`    | `/{id}`                 | Update `managerEmail` and `notificationType` (returns `ResumeUpdateDto`) |
| `DELETE` | `/{id}`                 | Delete resume, its match records, and owner if no resumes remain |

**Upload flow** (`POST /api/resumes/upload`): PDFBox extracts text → OpenAI identifies seeker name/email → seeker is upserted → OpenAI builds a structured `ResumeProfileDTO` (skills, roles, tools, etc.) → all data is persisted to MySQL.

### Resume Matches — `/api/resume-matches`

| Method   | Path                          | Description                               |
|----------|-------------------------------|-------------------------------------------|
| `GET`    | `/`                           | List all matches                          |
| `GET`    | `/{id}`                       | Get by ID                                 |
| `GET`    | `/resume/{resumeId}`          | Matches for a resume, sorted by score desc|
| `GET`    | `/assignment/{assignmentId}`  | Matches for an assignment                 |
| `POST`   | `/`                           | Create                                    |
| `PUT`    | `/{id}`                       | Update score, reasons, decision           |
| `DELETE` | `/{id}`                       | Delete                                    |

**Swagger UI** → `http://localhost:8081/swagger-ui.html`
**OpenAPI spec** → `http://localhost:8081/api-docs`

---

## Cascade Delete Behaviour

| Delete on       | Also removes                                                                                    |
|-----------------|-------------------------------------------------------------------------------------------------|
| `Assignment`    | All `ResumeMatch` rows for that assignment; Elasticsearch document deleted after commit         |
| `Resume`        | All `ResumeMatch` rows for that resume; `AssignmentSeeker` owner if they have no other resumes  |

---

## Tech Stack

Mirrors the resume-matcher project:

| Component        | Technology                    |
|------------------|-------------------------------|
| Language         | Java 21                       |
| Framework        | Spring Boot 3.4.1             |
| Persistence      | Spring Data JPA / Hibernate   |
| Database         | MySQL 8.4                     |
| Search           | Elasticsearch 8.15 (Java client) |
| AI               | OpenAI Java SDK 4.12 (gpt-4o) |
| PDF parsing      | Apache PDFBox 3.0.3           |
| API Docs         | Springdoc OpenAPI 2.7.0       |
| Build            | Maven 3.9                     |
| Code style       | Spotless + Eclipse JDT        |
| Containerization | Docker / Docker Compose       |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8+ (or use Docker Compose)
- Docker & Docker Compose (optional)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/debageriab-prog/resume-match-api.git
cd resume-match-api
```

### 2. Configure environment variables

```bash
cp .env.example .env
# Edit .env with your database credentials
```

The `.env` file must define:

```env
DB_URL=jdbc:mysql://db:3306/resume_matcher?useUnicode=true&characterEncoding=utf8
DB_USER=resume_user
DB_PASSWORD=changeme
DB_NAME=resume_matcher
DB_ROOT_PASSWORD=rootpassword

ELASTIC_URL=http://elasticsearch:9200
OPENAI_API_KEY=sk-...
```

### 3. Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8081`.

### 4. Run locally

Make sure MySQL is running and the environment variables are exported, then:

```bash
mvn spring-boot:run
```

---

## Project Structure

```
resume-match-api/
├── src/
│   ├── main/
│   │   ├── java/se/debageri/api/
│   │   │   ├── config/        # OpenAPI, Elasticsearch, OpenAI configuration
│   │   │   ├── controller/    # REST controllers
│   │   │   ├── dto/           # Response DTOs (e.g. ResumeSummaryDto)
│   │   │   ├── entity/        # JPA entities (mirrors resume-matcher)
│   │   │   ├── exception/     # Domain exceptions and global handler
│   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   ├── service/       # Business logic + ElasticJobSearchService + OpenAiService
│   │   │   └── util/          # StringUtil (PDF extraction), EmailExtractor
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/se/debageri/api/
├── .dockerignore
├── .env.example
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## License

MIT License
