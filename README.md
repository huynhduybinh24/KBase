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
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JPA_DDL_AUTO` | `validate` | Hibernate schema action |

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

Spring Security is included but intentionally has no custom authentication or authorization configuration yet. Spring Boot's default security behavior therefore applies.

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
