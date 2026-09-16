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

The document list supports `q`, `contentType`, `uploadedBy`, `from`, `to`, `page`,
`size`, and `sort` query parameters. Results are always limited to active documents in
the requested project. Page size defaults to 20 and cannot exceed 100. Sortable fields
are `createdAt`, `updatedAt`, `title`, and `fileSize`.

Text extraction runs synchronously after upload and is stored separately from document
metadata. Plain text, Markdown, PDF, and DOCX are supported. Other allowed upload types
are retained with an `UNSUPPORTED` extraction status for future processing.

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
