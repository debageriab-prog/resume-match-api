# resume-match-api

A **Spring Boot REST API** for the [Resume Matcher](https://github.com/debageriab-prog/resume-matcher) project.
Provides full CRUD operations for all entities and exposes a Swagger UI for easy exploration and testing.

---

## Features

- RESTful API with full CRUD for all Resume Matcher entities
- Swagger UI available at `/swagger-ui.html`
- OpenAPI 3 specification at `/api-docs`
- PostgreSQL database via Spring Data JPA
- Docker & Docker Compose support
- Validation and structured error responses

---

## Entities

| Entity        | Description                                      |
|---------------|--------------------------------------------------|
| `Candidate`   | Job seeker profile (name, email, location, etc.) |
| `Resume`      | Resume/CV document belonging to a candidate      |
| `Skill`       | Skill tag (e.g., Java, Python, SQL)              |
| `Experience`  | Work experience entry for a candidate            |
| `Education`   | Educational background entry for a candidate     |
| `Company`     | Company that posts job listings                  |
| `JobPosting`  | A job listing posted by a company                |
| `Match`       | Match result linking a resume to a job posting   |

---

## API Endpoints

| Resource        | Base Path              |
|-----------------|------------------------|
| Candidates      | `/api/candidates`      |
| Resumes         | `/api/resumes`         |
| Skills          | `/api/skills`          |
| Experiences     | `/api/experiences`     |
| Educations      | `/api/educations`      |
| Companies       | `/api/companies`       |
| Job Postings    | `/api/job-postings`    |
| Matches         | `/api/matches`         |

Each resource supports: `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`.
Sub-resources (e.g. resumes per candidate) are accessible via `/api/resumes/candidate/{candidateId}`.

---

## Tech Stack

| Component       | Technology                    |
|-----------------|-------------------------------|
| Language        | Java 21                       |
| Framework       | Spring Boot 3.3.5             |
| Persistence     | Spring Data JPA / Hibernate   |
| Database        | PostgreSQL 16                 |
| API Docs        | Springdoc OpenAPI 2 (Swagger) |
| Build           | Maven 3.9                     |
| Containerization| Docker / Docker Compose       |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or use Docker Compose)
- Docker & Docker Compose (optional, for containerized setup)

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

The `.env` file should contain:

```env
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=resume_matcher
DB_USERNAME=postgres
DB_PASSWORD=changeme
```

### 3. Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

### 4. Run locally (without Docker)

Make sure PostgreSQL is running and the `.env` variables are exported, then:

```bash
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn clean package -DskipTests
java -jar target/resume-match-api-0.0.1-SNAPSHOT.jar
```

---

## Swagger UI

Once the application is running, open your browser and navigate to:

```
http://localhost:8080/swagger-ui.html
```

The OpenAPI JSON spec is available at:

```
http://localhost:8080/api-docs
```

---

## Project Structure

```
resume-match-api/
├── src/
│   ├── main/
│   │   ├── java/com/resumematcher/api/
│   │   │   ├── config/          # Spring and OpenAPI configuration
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── entity/          # JPA entities
│   │   │   │   └── enums/       # Enum types
│   │   │   ├── exception/       # Exception handling
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/resumematcher/api/
├── .env.example                 # Environment variable template
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## License

MIT License
