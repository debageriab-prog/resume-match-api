# resume-match-api

A **Spring Boot REST API** that exposes full CRUD endpoints and a Swagger UI for all entities in the
[resume-matcher](https://github.com/debageriab-prog/resume-matcher) application.
Intended to serve as the backend for a front-end application — no security layer is included at this stage.

---

## Entities

| Entity             | Table               | Description                                         |
|--------------------|---------------------|-----------------------------------------------------|
| `Assignment`       | `assignment`        | A job/assignment posting                            |
| `AssignmentIndex`  | `assignment_index`  | Tracks which assignments have been indexed          |
| `AssignmentSeeker` | `assignment_seeker` | A job seeker (user)                                 |
| `Resume`           | `resume`            | Resume belonging to an `AssignmentSeeker`           |
| `ResumeMatch`      | `resume_match`      | Match result between a `Resume` and an `Assignment` |

---

## API Endpoints

| Resource            | Base Path                  |
|---------------------|----------------------------|
| Assignments         | `/api/assignments`         |
| Assignment Indexes  | `/api/assignment-indexes`  |
| Assignment Seekers  | `/api/assignment-seekers`  |
| Resumes             | `/api/resumes`             |
| Resume Matches      | `/api/resume-matches`      |

Each resource supports `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`.
Sub-resources are available via e.g. `/api/resumes/owner/{ownerId}` and `/api/resume-matches/resume/{resumeId}`.

**Swagger UI** → `http://localhost:8080/swagger-ui.html`
**OpenAPI spec** → `http://localhost:8080/api-docs`

---

## Tech Stack

Mirrors the resume-matcher project:

| Component        | Technology                    |
|------------------|-------------------------------|
| Language         | Java 21                       |
| Framework        | Spring Boot 3.4.1             |
| Persistence      | Spring Data JPA / Hibernate   |
| Database         | MySQL 8.4                     |
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
```

### 3. Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

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
│   │   │   ├── config/        # OpenAPI configuration
│   │   │   ├── controller/    # REST controllers
│   │   │   ├── entity/        # JPA entities (mirrors resume-matcher)
│   │   │   ├── exception/     # Exception handling
│   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   └── service/       # Business logic
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
