# Keystone Backend

Keystone is a sophisticated, multi-tenant Work Order Management System designed with Spring Boot and PostgreSQL. It streamlines scheduling, dispatching, and field operations across a varied workforce of technicians and dispatchers, while keeping customers fully informed.

## Tech Stack
- **Java 17** & **Spring Boot 3**
- **Spring Data JPA** & **Hibernate**
- **PostgreSQL** with Flyway Migrations
- **Spring Security** (Stateless JWT)
- **AWS S3** / **Local Disk** (Multipart file uploads)
- **Docker** & **Maven**

## Local Setup

### Requirements
- JDK 17
- Docker (for PostgreSQL via `docker-compose`)
- Maven

### Running the App
1. Start the database:
   ```bash
   docker-compose up -d
   ```
2. Build and run the app:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
Flyway will automatically run all migrations (`V1` to `V12`) on startup.

## Environment Variables

| Variable | Description | Default |
| -------- | ----------- | ------- |
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/keystone` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASS` | Database password | `postgres` |
| `JWT_SECRET` | Secret key for JWT signing | `c3VwZXJfc2VjcmV0X2tleV9mb3JfZGV2ZWxvcG1lbnRfcHVycG9zZXM=` |
| `JWT_EXPIRATION` | JWT validity in milliseconds | `86400000` (24h) |
| `STORAGE_PROVIDER` | `local` or `s3` | `local` |
| `LOCAL_STORAGE_DIR` | Absolute path for `local` storage | `/tmp/keystone-uploads` |
| `S3_BUCKET` | AWS S3 Bucket Name | - |
| `S3_REGION` | AWS Region | - |
| `S3_ACCESS_KEY` | AWS Access Key | - |
| `S3_SECRET_KEY` | AWS Secret Key | - |

## Database Seed & Migrations
The database is heavily versioned via Flyway inside `src/main/resources/db/migration`.
The seed file (`V2__seed.sql`) automatically provisions customers, sites, skills, parts, and users.

**Seed Logins (Password is always `password`):**
1. **MANAGER**: `admin@keystone.com`
2. **DISPATCHER**: `dispatcher@keystone.com`
3. **TECHNICIAN**: `tech1@keystone.com`
4. **CUSTOMER**: `contact@acme.com`

## Architecture Summary
Keystone is heavily domain-driven:
- **Work Orders**: The core domain model. It dictates the full lifecycle (NEW -> ASSIGNED -> IN_PROGRESS -> COMPLETED -> CLOSED). The state machine is strictly enforced, and transition history is automatically audited.
- **Role-Based Isolation**: Tenant segregation is built deep into the data repositories. A `CUSTOMER` can never query or manipulate a work order that does not belong to their company site.
- **Reporting Engine**: A highly optimized JPQL projection layer handles metrics natively, eliminating N+1 queries.
- **Full Text Search**: Powered by native PostgreSQL `TSVECTOR` and `GIN` indices, providing blazing-fast textual lookups.

## API Documentation & Health
- **Swagger UI**: Access the full interactive OpenAPI schema at `http://localhost:8080/swagger-ui.html`
- **Actuator Health**: Ready for Kubernetes and deployment checks at `http://localhost:8080/actuator/health`
