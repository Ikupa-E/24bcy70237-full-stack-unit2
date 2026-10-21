# Post Scheduler - Full Stack Project

## Experiment 1: REST APIs with Spring Boot
Run: `mvn spring-boot:run`  (Java 17+, Maven)
Base URL: http://localhost:8080/api/v1/posts
H2 console: http://localhost:8080/h2-console  (jdbc:h2:mem:postdb, user: sa, no password)

| Method | URL                           | Purpose                |
|--------|-------------------------------|------------------------|
| POST   | /api/v1/posts                 | Create post            |
| GET    | /api/v1/posts?status=DRAFT    | List (optional filter) |
| GET    | /api/v1/posts/{id}            | Get one                |
| PUT    | /api/v1/posts/{id}            | Update                 |
| DELETE | /api/v1/posts/{id}            | Delete                 |
| PATCH  | /api/v1/posts/{id}/schedule   | Schedule a post        |
| PATCH  | /api/v1/posts/{id}/unschedule | Back to draft          |

### Sample requests (Postman: Body > raw > JSON)
POST /api/v1/posts
{ "title": "Launch day", "content": "We are live!", "author": "Asha" }

POST with a schedule (use a future date)
{ "title": "Launch day", "content": "We are live!", "author": "Asha", "scheduledAt": "2030-01-15T10:30:00" }

PATCH /api/v1/posts/1/schedule
{ "scheduledAt": "2030-02-01T09:00:00" }

Invalid request (title too short, author blank) returns 400:
{ "success": false, "message": "Validation failed",
  "errors": { "title": "Title must be between 3 and 100 characters", "author": "Author is required" },
  "timestamp": "..." }

---

## Experiment 2: Global Exception Handling and Structured Logging

New/changed files
- NEW     filter/CorrelationIdFilter.java          (assigns X-Correlation-Id, stores it in MDC)
- NEW     interceptor/RequestLoggingInterceptor.java (logs request, status, duration)
- NEW     config/WebConfig.java                    (registers the interceptor)
- NEW     exception/ApiException.java, ErrorCode.java, DuplicateResourceException.java, InvalidOperationException.java
- NEW     controller/DiagnosticsController.java    (demo: triggers a 500)
- NEW     resources/logback-spring.xml             (console + rolling file, includes correlation ID)
- CHANGED exception/GlobalExceptionHandler.java, exception/ResourceNotFoundException.java
- CHANGED response/ApiResponse.java (adds correlationId), NEW response/ErrorResponse.java
- CHANGED service/PostService.java, repository/PostRepository.java, config/CorsConfig.java
- CHANGED resources/application.properties (show-sql off)

Error response shape
{
  "success": false, "status": 404, "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND", "message": "Post not found with id 999",
  "path": "/api/v1/posts/999", "correlationId": "…", "timestamp": "…"
}

Logs are written to the console and to logs/post-scheduler.log.

---

## Experiment 3: Pagination and Sorting

New/changed files
- NEW     dto/PageResponse.java, dto/PostSummaryResponse.java
- NEW     repository/PostSpecifications.java      (dynamic filters)
- NEW     config/PaginationProperties.java        (default page size, sort, max size)
- NEW     config/DataSeeder.java                  (50 sample posts at startup)
- NEW     exception/InvalidQueryParameterException.java
- CHANGED model/Post.java (indexes), repository/PostRepository.java,
          service/PostService.java, controller/PostController.java, application.properties

GET /api/v1/posts parameters (all optional)
  page (0-based, default 0), size (1-50, default 10), sortBy (id, title, author, status,
  scheduledAt, createdAt, updatedAt; default createdAt), direction (asc|desc, default desc),
  status, author, keyword (searches title and content)

Example: /api/v1/posts?page=1&size=5&sortBy=title&direction=asc&status=DRAFT
The list returns PostSummaryResponse (content preview only); GET /{id} returns the full post.

---

## Experiment 4: Query Optimization and Caching

New/changed files
- CHANGED pom.xml (spring-boot-starter-cache, JCache API, Ehcache 3, MySQL driver)
- NEW     model/Comment.java; CHANGED model/Post.java (one-to-many comments, lazy)
- NEW     repository/CommentRepository.java; CHANGED repository/PostRepository.java
          (JOIN FETCH queries + native SQL aggregation)
- NEW     config/CacheConfig.java (Ehcache caches "posts" and "postStats", TTL + size limit)
- NEW     service/PerformanceService.java, controller/PerformanceController.java
- NEW     dto/CommentResponse, PostWithCommentsResponse, PerformanceReport, PostStatsResponse, AuthorStatusCount
- CHANGED service/PostService.java (@Cacheable, @CachePut, @CacheEvict), controller/PostController.java (/stats),
          dto/PostResponse.java (Serializable), config/DataSeeder.java (comments),
          resources/application.properties; NEW resources/application-mysql.properties
- NEW     jmeter/post-scheduler-load-test.jmx

Endpoints
  GET    /api/v1/performance/posts-with-comments/naive      N+1 problem (reports SQL statement count)
  GET    /api/v1/performance/posts-with-comments/optimized  JOIN FETCH (reports SQL statement count)
  GET    /api/v1/posts/stats                                native SQL aggregation, cached
  GET    /api/v1/posts/{id}                                 cached by id
  DELETE /api/v1/performance/cache                          clear all caches (between benchmark runs)

Switch caching off for a benchmark: set spring.cache.type=none in application.properties.
Print every SQL statement: set logging.level.org.hibernate.SQL=DEBUG.
Use MySQL: mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"

JMeter: open jmeter/post-scheduler-load-test.jmx (4 thread groups run one after another,
20 users x 25 loops each) and read the Summary Report / Aggregate Report.

---

## Experiment 5: JWT Authentication and Role-Based Access Control

Default accounts (created at startup): admin / Admin@123 (ADMIN) and user / User@123 (USER).
Every endpoint except /auth/register and /auth/login now needs the header
  Authorization: Bearer <token>

New/changed files
- CHANGED pom.xml (spring-boot-starter-security, jjwt 0.12.6)
- NEW     model/Role.java, model/AppUser.java, repository/AppUserRepository.java
- NEW     security/JwtService.java, JwtAuthenticationFilter.java, RestSecurityErrorHandler.java, CustomUserDetailsService.java
- NEW     config/SecurityConfig.java (filter chain, URL rules, @EnableMethodSecurity), config/SecuritySeeder.java
- NEW     service/AuthService.java, service/UserService.java
- NEW     controller/AuthController.java, controller/AdminController.java
- NEW     dto/RegisterRequest, LoginRequest, AuthResponse, UserResponse, RoleUpdateRequest
- CHANGED exception/GlobalExceptionHandler.java (401 / 403), exception/ErrorCode.java, exception/ResourceNotFoundException.java
- CHANGED controller/PostController.java (DELETE = ADMIN), PerformanceController + DiagnosticsController (ADMIN)
- CHANGED logback-spring.xml (adds [user=...]), application.properties, jmeter/post-scheduler-load-test.jmx

Access rules
  Public ........ POST /api/v1/auth/register, POST /api/v1/auth/login
  Any user ...... all other endpoints under /api/v1/posts, GET /api/v1/auth/me
  ADMIN only .... DELETE /api/v1/posts/{id}, /api/v1/admin/**, /api/v1/performance/**, /api/v1/diagnostics/**

---

## Experiment 6: Encryption, Secure Credential Storage and Token Lifecycle

No new dependencies (AES comes from the JDK). Tables social_accounts and refresh_tokens are created automatically.

New/changed files
- NEW     security/AesEncryptionService.java (AES-256-GCM, random IV per encryption, versioned format)
- NEW     model/SocialAccount.java, model/Platform.java, repository/SocialAccountRepository.java
- NEW     service/SocialAccountService.java, controller/SocialAccountController.java, dto/SocialAccount*, TokenUpdateRequest, ConnectionStatusResponse
- NEW     model/RefreshToken.java, repository/RefreshTokenRepository.java, service/RefreshTokenService.java, config/SchedulingConfig.java
- NEW     controller/CryptoDemoController.java (admin demo), dto/CryptoTextRequest.java
- NEW     exception/InvalidTokenException.java, exception/CryptoException.java
- CHANGED service/AuthService.java, controller/AuthController.java (/refresh, /logout), dto/AuthResponse.java
          (accessToken + refreshToken), exception/ErrorCode.java, config/SecurityConfig.java, application.properties

Endpoints
  POST   /api/v1/auth/refresh                     public: exchange a refresh token for a new pair (rotation)
  POST   /api/v1/auth/logout                      public: revoke the refresh token family
  POST   /api/v1/social-accounts                  link an account (tokens encrypted)
  GET    /api/v1/social-accounts[/{id}]           list / read own accounts (masked token hint only)
  PUT    /api/v1/social-accounts/{id}/tokens      replace tokens
  POST   /api/v1/social-accounts/{id}/test-connection  decrypt in memory and check expiry
  DELETE /api/v1/social-accounts/{id}
  POST   /api/v1/admin/crypto/encrypt|decrypt     admin demo of AES-GCM

Token strategy: access token = JWT, 15 minutes. Refresh token = random 256-bit value, 7 days, stored only as a
SHA-256 hash, single use (rotation) with reuse detection that revokes the whole token family.
