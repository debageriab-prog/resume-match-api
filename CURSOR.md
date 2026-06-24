# resume-match-api — AI Navigation Guide

Spring Boot 3.4.1 / Java 21 REST API that ingests resumes (PDF), parses them with OpenAI, stores them in MySQL, and matches them against job assignments indexed in Elasticsearch.

---

## Key facts

| Item | Value |
|---|---|
| Group / Artifact | `se.debageri` / `resume-match-api` |
| Root package | `se.debageri.api` |
| Server port | `8081` |
| MySQL (local dev) | `127.0.0.1:3307` / db `assignment_db` (from `.env`) |
| Elasticsearch | `ELASTIC_URL` env var, index `assignments_v1` |
| RabbitMQ | `RABBIT_HOST` env var, exchange `events.topic` |
| OpenAI model | `gpt-4o` (constant `OpenAiService.PROFILE_MODEL`) |
| Code formatter | Spotless / Eclipse JDT — run `mvn spotless:apply` before committing |

---

## Package map

```
src/main/java/se/debageri/api/
├── ResumeMatchApiApplication.java   # entry point
│
├── config/
│   ├── AppConfig.java               # beans: OpenAIClient, RestClient, ElasticsearchClient
│   └── OpenApiConfig.java           # Swagger/springdoc customisation
│
├── controller/
│   ├── AssignmentController.java    # CRUD for assignments
│   ├── AssignmentSeekerController.java
│   ├── ResumeController.java        # resume upload + CRUD
│   └── ResumeMatchController.java   # match query endpoints
│
├── service/
│   ├── AssignmentService.java       # assignment CRUD + Elastic delete + Rabbit publish
│   ├── AssignmentSeekerService.java
│   ├── ResumeService.java           # PDF ingest flow (parse → LLM → upsert seeker → save)
│   ├── ResumeMatchService.java      # match read/delete
│   ├── OpenAiService.java           # LLM calls: extractAssignmentSeekerInfo, extractStructuredProfile
│   └── ElasticJobSearchService.java # bulk delete from Elasticsearch
│
├── repository/
│   ├── AssignmentRepository.java
│   ├── AssignmentSpecification.java # JPA Specification filters (jobId, title, client, location, portal)
│   ├── AssignmentIndexRepository.java  # Elasticsearch index repo
│   ├── AssignmentSeekerRepository.java
│   ├── ResumeRepository.java
│   └── ResumeMatchRepository.java
│
├── entity/
│   ├── Assignment.java              # table: assignment (unique on job_id)
│   ├── AssignmentSeeker.java        # table: assignment_seeker (unique on email)
│   ├── AssignmentIndex.java         # Elasticsearch document
│   ├── Resume.java                  # table: resume (FK owner_id → assignment_seeker)
│   ├── ResumeMatch.java             # table: resume_match (unique on resume_id + assignment_id)
│   ├── HighMatchRow.java            # projection / query result
│   └── NotificationType.java        # enum: USER, MANAGER, BOTH (controls notification routing)
│
├── dto/
│   ├── AssignmentSeekerInfoDTO.java # firstName, lastName, email — extracted by LLM
│   ├── ResumeProfileDTO.java        # structured profile: skills, tools, roles, etc.
│   ├── ResumeSummaryDto.java        # GET response: id, owner, managerEmail, notificationType, fileName, createdAt, matchedCount
│   ├── ResumeUpdateDto.java         # PUT response: id, owner, managerEmail, notificationType (no fileName/createdAt/matchedCount)
│   ├── ResumeUpdateRequest.java     # record: managerEmail, notificationType
│   ├── ResumeTopMatchedDto.java     # top-matched list item: id, fileName, ownerName, createdAt, matchCount
│   ├── ResumeMatchTopMatchedDto.java# used by ResumeMatchController
│   └── StatisticsResponse.java      # totalCount, todayCount, lastWeekCount, lastMonthCount
│
├── rabbit/
│   ├── RabbitConfig.java            # TopicExchange "events.topic"
│   ├── AssignmentEventPublisher.java # publishes "assignment.upserted" after TX commit
│   └── EventEnvelope.java           # wrapper sent over the wire
│
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice — all error responses shaped here
│   ├── AssignmentNotFoundException.java
│   ├── ResumeNotFoundException.java
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
│
└── util/
    ├── StringUtil.java              # extractTextFromPDF, safeLimit, stripJsonCodeFences, isBlank
    └── EmailExtractor.java          # regex fallback for email extraction from resume text
```

---

## Core flows

### 1 — Resume upload
`POST /resumes` (multipart PDF)
1. `StringUtil.extractTextFromPDF` — Apache PDFBox → plain text
2. `OpenAiService.extractAssignmentSeekerInfo` — LLM → `AssignmentSeekerInfoDTO` (name + email)
3. `EmailExtractor` regex fallback if LLM missed email
4. `ResumeService.upsertAssignmentSeeker` — find-by-email or create in `assignment_seeker`
5. `OpenAiService.extractStructuredProfile` — LLM → `ResumeProfileDTO` (skills, tools, roles …)
6. Persist `Resume` row (PDF bytes, extracted text, profile JSON)

### 2 — Assignment save / update
`POST /assignments` or `PUT /assignments/{id}`
1. Duplicate check on `job_id`
2. Save to MySQL
3. After TX commit → `AssignmentEventPublisher.publishAssignmentUpserted` → RabbitMQ `events.topic` / `assignment.upserted`

### 3 — Assignment delete
`DELETE /assignments/{id}`
1. Delete `resume_match` rows for this assignment
2. Delete `assignment` row (both in one TX)
3. After TX commit → Elasticsearch bulk delete + `assignment_index` row delete

---

## Configuration

| File | Purpose |
|---|---|
| `src/main/resources/application.yml` | All Spring config; env vars with defaults |
| `.env` | Local secrets — **not loaded automatically by Maven** (see below) |
| `src/test/resources/application-test.yml` | H2 overrides for tests |
| `docker-compose.yml` | Runs the app + MySQL (reads `.env` via `env_file`) |

### Running locally with `mvn spring-boot:run`

`.env` is **not** read automatically. Load it first in PowerShell:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#\s][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
    }
}
mvn spring-boot:run
```

### Required environment variables

| Variable | Used by |
|---|---|
| `DB_URL` | datasource URL (default: `localhost:3306/resume_matcher`) |
| `DB_USER` | datasource username (default: `root`) |
| `DB_PASSWORD` | datasource password (no default — app won't start without it) |
| `OPENAI_API_KEY` | `AppConfig` → `OpenAIOkHttpClient.fromEnv()` |
| `ELASTIC_URL` | Elasticsearch REST client (default: `http://localhost:9200`) |
| `RABBIT_HOST` | RabbitMQ host |
| `RABBIT_PASSWORD` | RabbitMQ password |

---

## Build & tooling

```bash
mvn spring-boot:run          # run locally (load .env first)
mvn test                     # unit + integration tests (H2)
mvn spotless:apply           # auto-format code (required before commit)
mvn spotless:check           # formatting check (runs on verify phase)
docker compose up --build    # full stack with MySQL
```

---

## Testing

### Test structure

```
src/test/
├── java/se/debageri/api/
│   ├── ResumeMatchApiApplicationTests.java         # context smoke test
│   └── controller/
│       ├── AssignmentControllerTest.java           # 20 endpoint tests
│       ├── AssignmentSeekerControllerTest.java     # 13 endpoint tests
│       ├── ResumeControllerTest.java               # 25 endpoint tests (incl. ResumeSummaryDto field coverage)
│       └── ResumeMatchControllerTest.java          # 18 endpoint tests
└── resources/
    └── application-test.yml                       # H2 + exclusions
```

### Controller test conventions

All controller tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`. The test profile:

- Replaces MySQL with **H2 in-memory** (`MODE=MySQL`, `ddl-auto: create-drop`)
- Excludes `RabbitAutoConfiguration` — no RabbitMQ needed
- Stubs `AssignmentEventPublisher` and `OpenAIClient` via `@MockBean` so RabbitMQ / OpenAI calls never happen
- Tests that touch assignment delete additionally stub `ElasticJobSearchService` to prevent real Elasticsearch calls
- Tests for resume upload (`POST /api/resumes/upload`) are intentionally omitted — they require live LLM calls; test with a real `OPENAI_API_KEY` manually

Each test class cleans its relevant tables in `@BeforeEach` to guarantee isolation.

### Seeding test data

Create entities directly via injected repositories; respect FK ordering on cleanup:
```
resume_match → resume → assignment_seeker
assignment_index → assignment
```

### Running tests

```bash
mvn test                     # all tests (no external services needed)
```

---

## CI/CD

### GitHub Actions (`.github/workflows/ci.yml`)

Two jobs run on every push and pull request to `main`:

| Job | Command | What it validates |
|---|---|---|
| `build` | `mvn package -DskipTests` | Compiles, packages the JAR |
| `test` | `mvn test` | All 77 tests against H2 in-memory DB |

The `test` job depends on `build` (via `needs: build`). Test results are uploaded as an artifact (`test-results/surefire-reports`).

---

## Conventions

- All services are `@Transactional(readOnly = true)` by default; write methods opt in with `@Transactional`.
- Side-effects that depend on external systems (Rabbit, Elastic) always run in `afterCommit()` to avoid partial failures.
- `AssignmentSeeker` is created/updated as a side-effect of resume upload — never created directly by callers.
- Error responses are centralised in `GlobalExceptionHandler`; add new exception types there.
- Spotless enforces import order `java, javax, jakarta, org, com, se` and Eclipse JDT formatting.
- **Always run `mvn spotless:apply` before pushing changes** to ensure formatting is correct.
- **All tests must follow the Given / When / Then structure** — use comments or method naming to make the three sections explicit.

```java
@Test
void shouldReturnNotFoundException_whenResumeDoesNotExist() {
    // Given
    long nonExistentId = 999L;

    // When
    Throwable thrown = catchThrowable(() -> resumeService.findById(nonExistentId));

    // Then
    assertThat(thrown).isInstanceOf(ResumeNotFoundException.class);
}
```

Use **AssertJ** (`assertThat`, `catchThrowable`, etc.) for all assertions — never JUnit's `assertEquals` / `assertThrows`.
