# Backend testing

From `backend/`, run `mvn test` (or `mvnw test` if a wrapper is added). The unit suite verifies the exact Stage 1 BCrypt hash, JWT issuance, tamper rejection, and expiry rejection. Web/security coverage exercises login success/failure, validation, `/me` without authentication, and STUDENT/ADMIN role boundaries; the role endpoints provide a stable smoke target. The bearer filter rejects malformed/expired tokens by leaving the request unauthenticated, after which Spring Security returns 401.

Stage 3 also contains focused calculation tests for zero-class attendance, the 75% threshold formulas, future scheduled occurrences/holidays, provisional CGPA rounding, and undefined SGPA. The source-level student queries constrain records by the authenticated student's database ID; finance/document queries similarly constrain ownership. These protections still require integration execution against MySQL.

**Execution status:** `mvn test` was attempted from `backend/` and was blocked because Maven is not available on PATH; no Maven wrapper exists. Java 24 is installed, but Java 21 compatibility was not proven by compilation. The MySQL80 Windows service is running and the MySQL 8 client exists at `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`, but a non-interactive connection as `root` was rejected because no password was supplied. Therefore application startup, Hibernate `ddl-auto=validate`, SQL execution, and MySQL-backed API flows were not executed. No credentials were requested or recorded.

### Required local verification

```powershell
Set-Location .\backend
mvn test
$env:SLCM_DB_URL = "jdbc:mysql://localhost:3306/slcm_portal?useSSL=false&serverTimezone=UTC"
$env:SLCM_DB_USERNAME = "<your MySQL user>"
$env:SLCM_DB_PASSWORD = "<your MySQL password>"
$env:SLCM_JWT_SECRET = "<random value of at least 32 bytes>"
mvn spring-boot:run
```

Use the existing Workbench-created database; do not rerun seed scripts for this verification. Hibernate validates only the mapped `users`/`students` JPA entities. Tables accessed by `JdbcTemplate` are not automatically schema-validated by Hibernate, so execute `database/99_verify_queries.sql` separately and exercise the endpoint workflows.
