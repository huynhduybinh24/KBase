# KBase Backend

Backend service for KBase, built with Java 21, Maven, Spring Boot, and PostgreSQL.

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL
- MinIO (or another S3-compatible object store)

## Configuration

The defaults in `src/main/resources/application.properties` target a local PostgreSQL database named `kbase`. For other environments, set:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/kbase` | JDBC connection URL |
| `DB_USERNAME` | Required | Database user |
| `DB_PASSWORD` | Required | Database password |
| `JPA_DDL_AUTO` | `validate` | Hibernate schema action |
| `JWT_SECRET` | Required | JWT signing key (Base64, at least 256 bits) |
| `JWT_EXPIRATION_MS` | `3600000` | Access-token lifetime in milliseconds |
| `MINIO_ENDPOINT` | `http://localhost:9000` | S3-compatible API endpoint |
| `MINIO_ACCESS_KEY` | Required | Object-storage access key |
| `MINIO_SECRET_KEY` | Required | Object-storage secret key |
| `MINIO_BUCKET` | `kbase-documents` | Document bucket, created on first upload if absent |
| `MAX_UPLOAD_SIZE` | `52428800` | Application upload limit in bytes (50 MiB) |
| `RAG_CHUNK_SIZE` | `800` | Approximate tokens per chunk |
| `RAG_CHUNK_OVERLAP` | `100` | Approximate overlapping tokens |
| `EMBEDDING_PROVIDER` | `dev` | `dev` or `openai-compatible` |
| `EMBEDDING_BASE_URL` | `http://localhost:11434/v1` | OpenAI-compatible provider base URL |
| `EMBEDDING_API_KEY` | Empty | Provider credential; never commit production values |
| `EMBEDDING_MODEL` | `kbase-dev-hash-v1` | Embedding model identifier |
| `EMBEDDING_DIMENSION` | `128` | Deterministic DEV embedding dimensions |
| `RAG_TOP_K` | `5` | Chunks retrieved for each chat question |
| `RAG_MAX_HISTORY_MESSAGES` | `10` | Recent private-session messages sent to the LLM |
| `RAG_MAX_CONTEXT_CHUNKS` | `10` | Maximum ranked chunks included in a prompt |
| `RAG_MAX_CONTEXT_CHARS` | `20000` | Maximum document-context characters per prompt |
| `LLM_PROVIDER` | `dev` | `dev` or `openai-compatible` |
| `LLM_BASE_URL` | `http://localhost:11434/v1` | OpenAI-compatible chat API base URL |
| `LLM_API_KEY` | Empty | Provider credential supplied only through the environment |
| `LLM_MODEL` | `kbase-dev-context-v1` | Chat model identifier |
| `LLM_TEMPERATURE` | `0.1` | Production provider sampling temperature |
| `LLM_MAX_OUTPUT_TOKENS` | `800` | Provider output limit |
| `LLM_TIMEOUT_SECONDS` | `60` | Connect and response timeout |

For local development, create the database before starting the application:

```sql
CREATE DATABASE kbase;
```

## Build and run

```shell
mvn clean verify
mvn spring-boot:run
```

Once the application is running, the OpenAPI document is available at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`.

Flyway applies the PostgreSQL schema migrations when the application starts.

For local MinIO, set matching credentials and start the provided service:

```shell
docker compose up -d minio
```

The S3 API is exposed on port `9000` and the MinIO console on port `9001`. With
the Compose defaults, use `MINIO_ACCESS_KEY=minioadmin` and
`MINIO_SECRET_KEY=minioadmin123` for the backend. Production deployments must
provide strong secrets rather than these local-development defaults.

For a pgvector-enabled development database on port `5433`:

```shell
docker compose up -d pgvector
```

Set `DB_URL=jdbc:postgresql://localhost:5433/kbase`, `DB_USERNAME=kbase`, and
`DB_PASSWORD` to the configured `PGVECTOR_PASSWORD`. The Compose password is a
development fallback only and must be overridden outside local development.

## Authentication

| Method | Endpoint | Authentication | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Register a new account with the `USER` role |
| `POST` | `/api/auth/login` | Public | Authenticate and receive a JWT access token |
| `GET` | `/api/users/me` | Bearer JWT | Return the currently authenticated user |

Send the returned token to protected endpoints using:

```http
Authorization: Bearer <access-token>
```

The supported roles are `ADMIN`, `OWNER`, and `USER`. Public registration always assigns `USER`; privileged roles must be assigned through a controlled administrative workflow.

## Projects

All project endpoints require a Bearer JWT.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/projects` | Create a project |
| `GET` | `/api/projects` | List accessible projects |
| `GET` | `/api/projects/{projectId}` | Get a project as a member |
| `PUT` | `/api/projects/{projectId}` | Update a project as its owner |
| `DELETE` | `/api/projects/{projectId}` | Delete a project as its owner |
| `GET` | `/api/projects/{projectId}/members` | List project members |
| `POST` | `/api/projects/{projectId}/members` | Add a member by email as owner |
| `DELETE` | `/api/projects/{projectId}/members/{userId}` | Remove a non-owner member as owner |

## Documents

Document metadata is stored in PostgreSQL and file bytes are stored in MinIO. All endpoints require a Bearer JWT and project membership. Uploads accept `multipart/form-data` fields named `file`, `title`, and optional `description`.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/projects/{projectId}/documents` | Upload a file and create metadata |
| `GET` | `/api/projects/{projectId}/documents` | List active documents |
| `GET` | `/api/projects/{projectId}/documents/{documentId}` | Get an active document |
| `GET` | `/api/projects/{projectId}/documents/{documentId}/download` | Download file as an attachment |
| `GET` | `/api/projects/{projectId}/documents/{documentId}/preview` | Preview PDF, PNG, JPEG, or plain text inline |
| `PUT` | `/api/projects/{projectId}/documents/{documentId}` | Update title and description as uploader or project owner |
| `DELETE` | `/api/projects/{projectId}/documents/{documentId}` | Soft-delete as uploader or project owner |
| `GET` | `/api/projects/{projectId}/documents/{documentId}/content` | Read extracted text and extraction status |
| `POST` | `/api/projects/{projectId}/documents/{documentId}/extract` | Retry extraction as uploader or project owner |
| `POST` | `/api/projects/{projectId}/documents/{documentId}/index` | Rebuild chunks and embeddings as uploader or project owner |
| `GET` | `/api/projects/{projectId}/search/semantic` | Retrieve project chunks by cosine similarity |

The document list supports `q`, `contentType`, `uploadedBy`, `from`, `to`, `page`,
`size`, and `sort` query parameters. Results are always limited to active documents in
the requested project. Page size defaults to 20 and cannot exceed 100. Sortable fields
are `createdAt`, `updatedAt`, `title`, and `fileSize`.

Text extraction runs synchronously after upload and is stored separately from document
metadata. Plain text, Markdown, PDF, and DOCX are supported. Other allowed upload types
are retained with an `UNSUPPORTED` extraction status for future processing.

Completed text is deterministically split in order using a conservative four-characters-
per-token approximation. Paragraph and sentence boundaries are preferred, and configurable
overlap is retained between chunks. Embeddings are stored in PostgreSQL using pgvector with
variable dimensions. Exact cosine search is used; no ANN index is created because embedding
model and dimensions are configurable.

The default `dev` embedding provider is deterministic hashing for local tests only. It is
not a production semantic model. Set `EMBEDDING_PROVIDER=openai-compatible` and supply the
provider URL, key, and model to use a production embeddings endpoint.

## Grounded project chat

All chat endpoints require project membership, and sessions are private to their creator.
Project owners do not receive access to another member's chat history.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/projects/{projectId}/chat/sessions` | Create a private chat session |
| `GET` | `/api/projects/{projectId}/chat/sessions` | List the current user's sessions |
| `GET` | `/api/projects/{projectId}/chat/sessions/{sessionId}` | Get private session metadata |
| `GET` | `/api/projects/{projectId}/chat/sessions/{sessionId}/messages` | Get chronological messages and structured sources |
| `POST` | `/api/projects/{projectId}/chat/sessions/{sessionId}/messages` | Ask the grounded project assistant |
| `DELETE` | `/api/projects/{projectId}/chat/sessions/{sessionId}` | Delete the session and its history |

The prompt builder keeps system instructions separate from retrieved document text, clearly
delimits every untrusted context chunk, limits history and context size, and preserves the
highest-ranked chunks first. With no usable context, KBase returns a fixed insufficient-context
answer without calling the LLM. The deterministic DEV provider never contacts an external
service and is only for pipeline testing, not real language quality. Set `LLM_PROVIDER` to
`openai-compatible` for a production provider. Chat generation and embedding providers remain
independently configured.

## Project structure

```text
src/
├── main/
│   ├── java/com/kbase/backend/
│   │   └── KBaseApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/kbase/backend/
        └── KBaseApplicationTests.java
```
