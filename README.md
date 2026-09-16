# KBase Backend

Backend service for KBase, built with Java 21, Maven, Spring Boot, and PostgreSQL.

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL

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
