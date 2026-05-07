# 🏃 Sprint 1 — Daily Breakdown Chi Tiết
**Foundation & Project Setup | 6 ngày × 6h = 36h**

---

## 📋 Sprint Planning (Trước ngày 1 — ~30 phút)

> **Ceremony này bạn tự làm một mình.** Ngồi xuống, đọc Sprint Goal, ước tính task.

**Sprint Goal:**
> Dựng được bộ khung dự án chạy được, kết nối database, và có luồng Auth cơ bản.
> Cuối Sprint: app start được, login được, trả JWT, không crash.

**Sprint Backlog (toàn bộ task Sprint 1):**

| ID | Task | Ước tính | Ngày |
|----|------|----------|------|
| T01 | Khởi tạo Spring Boot project + dependencies | 1h | Day 1 |
| T02 | Cấu hình package structure chuẩn | 1h | Day 1 |
| T03 | Cấu hình `application.yml` (dev/prod profile) | 1h | Day 1 |
| T04 | Cấu hình kết nối MySQL, test connection | 1h | Day 1 |
| T05 | Thiết kế ERD (User, Account, Transaction, Category, Budget) | 2h | Day 1 |
| T06 | Cài đặt và cấu hình Flyway | 1h | Day 2 |
| T07 | Viết Migration V1: bảng `users` | 1.5h | Day 2 |
| T08 | Viết Migration V2: bảng `accounts` | 1h | Day 2 |
| T09 | Tạo `User` entity + `UserRepository` | 1.5h | Day 2 |
| T10 | Implement `UserDetailsService` | 1h | Day 2 |
| T11 | Implement JWT utility class (generate + validate token) | 2h | Day 3 |
| T12 | Implement `JwtAuthenticationFilter` | 2h | Day 3 |
| T13 | Cấu hình `SecurityFilterChain` | 2h | Day 3 |
| T14 | Implement `POST /api/auth/register` | 2h | Day 4 |
| T15 | Implement `POST /api/auth/login` | 2h | Day 4 |
| T16 | Global Exception Handler (`@ControllerAdvice`) | 2h | Day 4 |
| T17 | Cài đặt và cấu hình Swagger / OpenAPI | 1.5h | Day 5 |
| T18 | Viết Unit Test cho `AuthService` | 2.5h | Day 5 |
| T19 | Test toàn bộ flow với Postman | 1.5h | Day 5 |
| T20 | Viết `README.md` + `.gitignore` + cấu hình `.env` | 1.5h | Day 6 |
| T21 | Buffer: fix bug, polish, cleanup code | 2.5h | Day 6 |
| T22 | Sprint Review + Retrospective | 1h | Day 6 |

---

## 📅 DAY 1 — Project Initialization & Database Design
**Thứ Hai | 6 tiếng**

### 🌅 Daily Standup (5 phút — tự hỏi mình)
```
✅ Hôm qua làm gì? → Chưa start Sprint 1
🎯 Hôm nay làm gì? → Khởi tạo project, cấu hình, design ERD
🚧 Blocker?        → Không có
```

---

### ⏰ 09:00 – 10:00 | T01: Khởi tạo Spring Boot Project (1h)

**Làm gì:**
1. Vào [start.spring.io](https://start.spring.io) hoặc dùng IntelliJ → New Project
2. Chọn: **Java 17+**, **Maven**, **Spring Boot 3.x**
3. Thêm dependencies:
   ```
   ✅ Spring Web
   ✅ Spring Data JPA
   ✅ Spring Security
   ✅ MySQL Driver
   ✅ Lombok
   ✅ Validation (Jakarta Validation)
   ✅ Flyway Migration
   ```
4. Group: `com.yourname`, Artifact: `finance-insight`
5. Import vào IntelliJ, chạy thử → đảm bảo `BUILD SUCCESS`

**✅ Done khi:** `mvn spring-boot:run` chạy được (dù chưa có DB)

---

### ⏰ 10:00 – 11:00 | T02: Cấu Trúc Package (1h)

**Làm gì:** Tạo cấu trúc folder theo Layer Architecture:

```
src/main/java/com/yourname/finance/
├── config/           ← SecurityConfig, SwaggerConfig, ...
├── controller/       ← REST Controllers
├── service/          ← Business logic
│   └── impl/         ← Service implementations
├── repository/       ← Spring Data JPA interfaces
├── entity/           ← JPA Entities (@Entity)
├── dto/
│   ├── request/      ← Request bodies (LoginRequest, RegisterRequest...)
│   └── response/     ← Response bodies (ApiResponse, JwtResponse...)
├── exception/        ← Custom exceptions + GlobalExceptionHandler
├── security/         ← JWT Filter, UserDetailsService
└── util/             ← JwtUtil, DateUtil, ...
```

**Tạo ngay các file placeholder:**
- `controller/AuthController.java` (empty class)
- `service/AuthService.java` (empty interface)
- `exception/GlobalExceptionHandler.java` (empty class)
- `dto/response/ApiResponse.java` (generic wrapper)

> 💡 **AI Assist:** Nhờ AI generate `ApiResponse<T>` generic wrapper class với fields: `success`, `message`, `data`

**✅ Done khi:** Package structure tạo xong, project vẫn compile được

---

### ⏰ 11:00 – 12:00 | T03 + T04: Cấu Hình application.yml + MySQL (1h)

**Làm gì:**

1. Tạo file `src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: dev

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://localhost:3306/finance_insight?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:yourpassword}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate    # ← QUAN TRỌNG: dùng validate, không phải create
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

app:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key-here-make-it-long}
    expiration: 86400000  # 24h in ms
```

2. Tạo file `.env` ở root project:
```
DB_USERNAME=root
DB_PASSWORD=yourpassword
JWT_SECRET=your-very-long-secret-key-at-least-256-bits
```

3. Thêm vào `.gitignore`:
```
.env
*.env
```

4. Tạo database MySQL: `finance_insight` (chạy lệnh SQL hoặc dùng Workbench)

**✅ Done khi:** App start, log thấy "Database connection established" (dù Flyway chưa chạy)

---

### ⏰ 13:00 – 15:00 | T05: Thiết Kế ERD (2h)

> ⚠️ **Đây là task PHẢI TỰ LÀM.** Không nhờ AI design schema vì nó không hiểu business logic của bạn.

**Làm gì:** Vẽ ERD (dùng draw.io, dbdiagram.io, hoặc giấy)

**Schema cần thiết (Sprint 1 + tương lai gần):**

```
USERS
├── id (BIGINT, PK, AUTO_INCREMENT)
├── email (VARCHAR 255, UNIQUE, NOT NULL)
├── password_hash (VARCHAR 255, NOT NULL)
├── full_name (VARCHAR 100)
├── created_at (DATETIME)
└── updated_at (DATETIME)

ACCOUNTS
├── id (BIGINT, PK)
├── user_id (FK → users.id)
├── name (VARCHAR 100)
├── type (ENUM: CASH, BANK, CREDIT)
├── balance (DECIMAL 15,2)
├── currency (VARCHAR 3, DEFAULT 'VND')
├── created_at, updated_at

CATEGORIES
├── id (BIGINT, PK)
├── user_id (FK → users.id, NULLABLE ← null = system default)
├── name (VARCHAR 100)
├── type (ENUM: INCOME, EXPENSE)
├── icon (VARCHAR 50)
└── is_default (BOOLEAN)

TRANSACTIONS
├── id (BIGINT, PK)
├── user_id (FK → users.id)
├── account_id (FK → accounts.id)
├── category_id (FK → categories.id)
├── amount (DECIMAL 15,2)
├── type (ENUM: INCOME, EXPENSE)
├── description (VARCHAR 500)
├── transaction_date (DATE)
├── is_auto_categorized (BOOLEAN, DEFAULT FALSE)  ← AI flag
├── deleted_at (DATETIME, NULLABLE)               ← soft delete
├── created_at, updated_at

BUDGETS
├── id (BIGINT, PK)
├── user_id (FK → users.id)
├── category_id (FK → categories.id)
├── amount (DECIMAL 15,2)
├── month (DATE)  ← store first day of month, e.g. 2026-04-01
└── created_at
```

**Quy tắc thiết kế quan trọng:**
- Tất cả bảng có `created_at`, `updated_at`
- Dùng `DECIMAL(15,2)` cho tiền, không dùng `DOUBLE`/`FLOAT`
- Soft delete chỉ cho `transactions` (user cần undo)
- `user_id` ở mọi bảng chính → dễ filter theo user

**✅ Done khi:** ERD vẽ xong, save file vào `docs/erd.png` hoặc `docs/schema.md`

---

### ⏰ 15:00 – 16:00 | Review & Commit Day 1

**Làm gì:**
- Review lại code đã viết
- Chạy app lần cuối, đảm bảo không có lỗi compile
- Commit:
  ```bash
  git add .
  git commit -m "feat: initial project setup with Spring Boot, MySQL config, and ERD design"
  git push origin main
  ```

**📊 Daily Progress Check:**
```
T01 ✅ Project khởi tạo
T02 ✅ Package structure
T03 ✅ application.yml
T04 ✅ MySQL connected
T05 ✅ ERD designed
```

---

## 📅 DAY 2 — Flyway Migration & User Entity
**Thứ Ba | 6 tiếng**

### 🌅 Daily Standup (5 phút)
```
✅ Hôm qua: Setup project xong, có ERD
🎯 Hôm nay: Flyway migrations + User entity + UserRepository
🚧 Blocker? → MySQL đang chạy chưa? Check lại
```

---

### ⏰ 09:00 – 10:00 | T06: Cấu Hình Flyway + Migration Đầu Tiên (1h)

**Làm gì:**
1. Tạo thư mục: `src/main/resources/db/migration/`
2. Tạo file `V1__create_users_table.sql`:

```sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

3. Chạy app → Flyway tự tạo bảng + bảng `flyway_schema_history`
4. Kiểm tra MySQL Workbench: bảng `users` đã tồn tại

> 💡 Quy tắc đặt tên file Flyway: `V{số}__{mô tả}.sql` — số phải tăng dần, không được sửa file đã chạy

**✅ Done khi:** App start, log Flyway: `"Successfully applied 1 migration"`

---

### ⏰ 10:00 – 11:00 | T07 nghiệp vụ dư: Migration V2 Bảng accounts (1h)

**Tạo `V2__create_accounts_table.sql`:**

```sql
CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type ENUM('CASH', 'BANK', 'CREDIT') NOT NULL DEFAULT 'CASH',
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**✅ Done khi:** Flyway log: `"Successfully applied 2 migrations"`

---

### ⏰ 11:00 – 12:30 | T08: Tạo User Entity (1.5h)

**Làm gì:** Tạo `entity/User.java`

```java
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Tạo `repository/UserRepository.java`:**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

**✅ Done khi:** Compile thành công, không lỗi JPA mapping

---

### ⏰ 13:30 – 14:30 | T09: Tạo DTO Classes (1h)

> 💡 **AI Assist:** Nhờ AI generate các DTO class dưới đây nhanh hơn

**Tạo các file DTO:**

`dto/request/RegisterRequest.java`:
```java
@Getter @Setter
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Tên không được để trống")
    private String fullName;
}
```

`dto/request/LoginRequest.java`:
```java
@Getter @Setter
public class LoginRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
```

`dto/response/ApiResponse.java`:
```java
@Getter @Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().success(false).message(message).build();
    }
}
```

`dto/response/JwtResponse.java`:
```java
@Getter @AllArgsConstructor
public class JwtResponse {
    private String token;
    private String tokenType = "Bearer";
    private String email;
    private String fullName;
}
```

**✅ Done khi:** Tất cả DTO compile OK

---

### ⏰ 14:30 – 15:30 | T10: Implement UserDetailsService (1h)

**Tạo `security/UserDetailsServiceImpl.java`:**

```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities("ROLE_USER")
            .build();
    }
}
```

**✅ Done khi:** Bean load được, không có circular dependency

---

### ⏰ 15:30 – 16:00 | Commit Day 2

```bash
git add .
git commit -m "feat: flyway migrations V1-V2, user entity, repositories, and DTOs"
git push
```

**📊 Daily Progress Check:**
```
T06 ✅ Flyway configured
T07 ✅ Migration V1 (users)
T08 ✅ Migration V2 (accounts)
T09 ✅ User entity + Repository
T10 ✅ UserDetailsService
```

---

## 📅 DAY 3 — JWT Security Layer
**Thứ Tư | 6 tiếng**

### 🌅 Daily Standup (5 phút)
```
✅ Hôm qua: Flyway + Entity + DTO xong
🎯 Hôm nay: JWT utility + Filter + SecurityConfig
🚧 Blocker? → Kiểm tra dependency spring-security có trong pom.xml chưa
```

> ⚠️ **Day 3 là ngày kỹ thuật nhất.** Security config hay mắc lỗi circular dependency và filter ordering. Đọc kỹ trước khi code.

---

### ⏰ 09:00 – 11:00 | T11: JWT Utility Class (2h)

> ⚠️ **Phần này PHẢI hiểu rõ, không copy paste mù quáng.** JWT là core security của app.

**Thêm dependency vào `pom.xml`:**
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**Tạo `util/JwtUtil.java`:**

```java
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

**✅ Done khi:** Unit test nhỏ: generate token → extract email → đúng không

---

### ⏰ 11:00 – 13:00 | T12: JWT Authentication Filter (2h)

**Tạo `security/JwtAuthenticationFilter.java`:**

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                        );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalid → tiếp tục, không set authentication
        }

        filterChain.doFilter(request, response);
    }
}
```

**✅ Done khi:** Filter compile, không có import error

---

### ⏰ 14:00 – 16:00 | T13: SecurityFilterChain Config (2h)

**Tạo `config/SecurityConfig.java`:**

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> ⚠️ **Common Bug:** Nếu gặp lỗi `Circular dependency` → đảm bảo `PasswordEncoder` bean nằm trong `SecurityConfig`, không phải inject từ chỗ khác.

**Test nhanh:** Start app → không có lỗi startup

**Commit Day 3:**
```bash
git commit -m "feat: JWT utility, authentication filter, and security configuration"
```

**📊 Daily Progress Check:**
```
T11 ✅ JwtUtil
T12 ✅ JwtAuthenticationFilter
T13 ✅ SecurityFilterChain
```

---

## 📅 DAY 4 — Auth Endpoints & Error Handling
**Thứ Năm | 6 tiếng**

### 🌅 Daily Standup (5 phút)
```
✅ Hôm qua: JWT security layer hoàn chỉnh
🎯 Hôm nay: Register, Login endpoint + Global Exception Handler
🚧 Blocker? → Security config đã OK chưa? (test ngay đầu ngày)
```

---

### ⏰ 09:00 – 11:00 | T14: Register Endpoint (2h)

**Tạo `service/AuthService.java` (interface):**

```java
public interface AuthService {
    ApiResponse<JwtResponse> register(RegisterRequest request);
    ApiResponse<JwtResponse> login(LoginRequest request);
}
```

**Tạo `service/impl/AuthServiceImpl.java`:**

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public ApiResponse<JwtResponse> register(RegisterRequest request) {
        // 1. Check email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email đã được đăng ký: " + request.getEmail());
        }

        // 2. Tạo user mới
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .build();

        userRepository.save(user);

        // 3. Generate JWT
        String token = jwtUtil.generateToken(user.getEmail());

        JwtResponse jwtResponse = new JwtResponse(token, "Bearer", user.getEmail(), user.getFullName());
        return ApiResponse.success("Đăng ký thành công", jwtResponse);
    }

    @Override
    public ApiResponse<JwtResponse> login(LoginRequest request) {
        // Spring Security tự validate credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());

        JwtResponse jwtResponse = new JwtResponse(token, "Bearer", user.getEmail(), user.getFullName());
        return ApiResponse.success("Đăng nhập thành công", jwtResponse);
    }
}
```

**Tạo `controller/AuthController.java`:**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

**✅ Done khi:** Test với Postman:
- `POST /api/auth/register` → 201 Created, nhận JWT
- `POST /api/auth/register` lần 2 email cũ → báo lỗi rõ ràng

---

### ⏰ 11:00 – 13:00 | T15 + T16: Login + Global Exception Handler (2h)

**Tạo Custom Exceptions:**

`exception/EmailAlreadyExistsException.java`:
```java
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
```

`exception/ResourceNotFoundException.java`:
```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

**Tạo `exception/GlobalExceptionHandler.java`:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Dữ liệu không hợp lệ"));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Email hoặc mật khẩu không đúng"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    // Catch all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Có lỗi xảy ra, vui lòng thử lại"));
    }
}
```

**Test Login:**
- `POST /api/auth/login` đúng credentials → nhận JWT ✅
- `POST /api/auth/login` sai password → 401 Unauthorized ✅
- `POST /api/auth/register` thiếu email → 400 Bad Request ✅

---

### ⏰ 14:00 – 16:00 | Test Protected Endpoint + Commit (2h)

**Tạo endpoint test `GET /api/users/me`:**

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Map to response DTO
        return ResponseEntity.ok(ApiResponse.success(new UserProfileResponse(user)));
    }
}
```

**Test flow đầy đủ:**
1. Register → nhận token
2. Gọi `GET /api/users/me` với header `Authorization: Bearer {token}` → nhận thông tin user ✅
3. Gọi không có token → 403 Forbidden ✅

**Commit Day 4:**
```bash
git commit -m "feat: auth endpoints (register/login), global exception handler, protected endpoint"
```

**📊 Daily Progress Check:**
```
T14 ✅ Register endpoint
T15 ✅ Login endpoint
T16 ✅ Global Exception Handler
```

---

## 📅 DAY 5 — Swagger, Testing & Polish
**Thứ Sáu | 6 tiếng**

### 🌅 Daily Standup (5 phút)
```
✅ Hôm qua: Auth API hoàn chỉnh, flow Login → JWT → Protected endpoint OK
🎯 Hôm nay: Swagger UI + Unit tests + Postman collection
🚧 Blocker? → Không có, momentum đang ổn
```

---

### ⏰ 09:00 – 10:30 | T17: Swagger / OpenAPI Setup (1.5h)

**Thêm dependency:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Tạo `config/SwaggerConfig.java`:**

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Personal Finance Insight API")
                .description("API cho hệ thống quản lý tài chính cá nhân có AI")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

**Thêm Swagger annotations vào Controller:**
```java
@Operation(summary = "Đăng ký tài khoản mới")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
    @ApiResponse(responseCode = "409", description = "Email đã tồn tại")
})
@PostMapping("/register")
public ResponseEntity<ApiResponse<JwtResponse>> register(...) { ... }
```

**Test:** Mở `http://localhost:8080/swagger-ui.html` → thấy UI đẹp với lock icon cho JWT

> 💡 **AI Assist:** Nhờ AI generate `@Operation` và `@ApiResponse` annotations cho tất cả endpoints nhanh hơn

**✅ Done khi:** Swagger UI load được, có thể test API trực tiếp trên Swagger

---

### ⏰ 10:30 – 13:00 | T18: Unit Tests (2.5h)

**Tạo `test/service/AuthServiceTest.java`:**

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthServiceImpl authService;

    // =================== REGISTER ===================

    @Test
    @DisplayName("Đăng ký thành công với email mới")
    void register_success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");
        request.setFullName("Nguyen Van A");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(jwtUtil.generateToken(anyString())).thenReturn("mock_jwt_token");

        // Act
        ApiResponse<JwtResponse> response = authService.register(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("mock_jwt_token", response.getData().getToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại khi email đã tồn tại")
    void register_emailAlreadyExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@gmail.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // =================== LOGIN ===================

    @Test
    @DisplayName("Đăng nhập thành công")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        User mockUser = User.builder()
            .email("test@gmail.com")
            .fullName("Test User")
            .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken("test@gmail.com")).thenReturn("valid_token");

        ApiResponse<JwtResponse> response = authService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("valid_token", response.getData().getToken());
    }
}
```

**Chạy tests:**
```bash
mvn test
```

**✅ Done khi:** Tất cả tests GREEN ✅

---

### ⏰ 14:00 – 15:30 | T19: Postman Collection (1.5h)

**Tạo Postman Collection "Finance Insight API":**

```
📁 Finance Insight API
  └── 📁 Auth
        ├── POST Register
        │     Body: {"email": "user@test.com", "password": "pass1234", "fullName": "Test User"}
        ├── POST Login
        │     Body: {"email": "user@test.com", "password": "pass1234"}
        │     Test script: pm.environment.set("token", pm.response.json().data.token)
        └── GET Current User
              Header: Authorization: Bearer {{token}}
```

**Thiết lập Postman Environment:**
- `base_url`: `http://localhost:8080`
- `token`: (tự động set khi login)

**Export collection → save vào `postman/Finance_Insight.postman_collection.json`**

**Commit Day 5:**
```bash
git commit -m "feat: Swagger UI config, unit tests for AuthService, Postman collection"
```

---

## 📅 DAY 6 — Buffer, Sprint Review & Retrospective
**Thứ Bảy | 6 tiếng**

### 🌅 Daily Standup (5 phút)
```
✅ Hôm qua: Swagger + tests + Postman xong
🎯 Hôm nay: Bug fixes, README, Sprint Review
🚧 Blocker? → Liệt kê bug còn tồn đọng
```

---

### ⏰ 09:00 – 10:00 | T20: README + .env Security (1h)

**Viết `README.md` đầy đủ:**

```markdown
# Personal Finance Insight AI

Hệ thống quản lý tài chính cá nhân tích hợp AI.

## Tech Stack
- Spring Boot 3.x + Java 17
- MySQL 8
- JWT Authentication
- OpenAI API (Sprint 4+)

## Prerequisites
- Java 17+
- MySQL 8
- Maven

## Chạy local

1. Clone repo
2. Tạo database MySQL:
   CREATE DATABASE finance_insight;

3. Copy file cấu hình:
   cp .env.example .env
   # Điền DB_USERNAME, DB_PASSWORD, JWT_SECRET

4. Chạy app:
   mvn spring-boot:run

5. Swagger UI: http://localhost:8080/swagger-ui.html

## API Overview
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | /api/auth/register | Đăng ký |
| POST | /api/auth/login | Đăng nhập |
| GET | /api/users/me | Thông tin cá nhân |
```

**Tạo `.env.example`:**
```
DB_USERNAME=root
DB_PASSWORD=yourpassword
JWT_SECRET=your-256-bit-secret-key-here
```

---

### ⏰ 10:00 – 12:30 | T21: Buffer — Bug Fix & Code Cleanup (2.5h)

**Checklist cleanup:**
- [ ] Xóa tất cả `System.out.println()` → dùng `@Slf4j` + `log.info()`
- [ ] Review tất cả TODO comments
- [ ] Kiểm tra: không có hardcoded password/secret trong code
- [ ] Đảm bảo `.env` có trong `.gitignore`
- [ ] Chạy lại tất cả unit tests → còm xanh hết
- [ ] Test lại toàn bộ flow với Postman

**Common bug cần check:**
```
❌ JWT secret ngắn quá (< 32 chars) → SignatureException
❌ Flyway migration fail do charset → thêm utf8mb4 vào SQL
❌ BCrypt encode trong filter (performance) → encode phải trong ServiceImpl
❌ CORS chưa cấu hình → browser không gọi được API
```

**Nếu còn thời gian — bonus task:**
- Cấu hình CORS cho phép frontend gọi API:
```java
// Thêm vào SecurityConfig
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    return config;
}))
```

---

### ⏰ 13:30 – 14:30 | 🎉 Sprint Review (1h)

> **Sprint Review** = Demo kết quả Sprint cho chính mình (và bạn bè/mentor nếu có)

**Checklist Sprint Review — Demo theo thứ tự:**

```
1. Start app → không có lỗi startup    ✅/❌
2. Flyway chạy → bảng users, accounts tồn tại  ✅/❌
3. POST /api/auth/register → 201 + JWT  ✅/❌
4. POST /api/auth/login → 200 + JWT     ✅/❌
5. POST /api/auth/login sai pass → 401  ✅/❌
6. GET /api/users/me có token → 200 + user info  ✅/❌
7. GET /api/users/me không có token → 403  ✅/❌
8. Swagger UI load được   ✅/❌
9. Tests chạy GREEN       ✅/❌
10. README dễ hiểu        ✅/❌
```

**Sprint Velocity:**
- Tổng task planned: 22
- Tổng task completed: ___
- % hoàn thành: ___

---

### ⏰ 14:30 – 15:30 | 🪞 Sprint Retrospective (30 phút)

> **Tự hỏi 3 câu:**

```
✅ What went WELL?
   - ...

⚠️  What could be IMPROVED?
   - ...

🚀 What will I do DIFFERENTLY next Sprint?
   - ...
```

**Ví dụ thực tế:**
```
✅ Went well:
   - Flyway migration cực kỳ hữu ích, sẽ dùng cả dự án
   - JWT từ jjwt-api v0.12 dễ dùng hơn version cũ

⚠️ Could improve:
   - JPA annotation bị sai gây mất 1h debug
   
🚀 Next Sprint:
   - Viết unit test SONG SONG với code (không viết tách riêng cuối ngày)
   - Setup Postman environment trước, test ngay khi xong endpoint
```

---

### ⏰ 15:30 – 16:00 | Final Commit & Push

```bash
git add .
git commit -m "chore: README, env example, code cleanup, sprint 1 complete"
git push origin main
```

**Tạo Git Tag:**
```bash
git tag -a sprint-1 -m "Sprint 1 complete: Auth system"
git push origin sprint-1
```

---

## 📊 Sprint 1 — Summary Dashboard

### Deliverables Checklist
| # | Deliverable | Status |
|---|------------|--------|
| 1 | Spring Boot app start được, kết nối MySQL | ⬜ |
| 2 | Flyway migration chạy tự động | ⬜ |
| 3 | `POST /api/auth/register` trả JWT | ⬜ |
| 4 | `POST /api/auth/login` trả JWT | ⬜ |
| 5 | Protected endpoint `/api/users/me` | ⬜ |
| 6 | Global exception handler | ⬜ |
| 7 | Swagger UI hoạt động | ⬜ |
| 8 | Unit tests GREEN | ⬜ |
| 9 | Postman collection | ⬜ |
| 10 | README đầy đủ | ⬜ |

### Thời Gian Phân Bổ Thực Tế
```
Day 1: Project Init + ERD         → 6h
Day 2: Flyway + Entity + DTO      → 6h
Day 3: JWT Security               → 6h  ← ngày khó nhất
Day 4: Auth Endpoints + Errors    → 6h
Day 5: Swagger + Tests + Postman  → 6h
Day 6: Buffer + Review + Retro    → 6h
────────────────────────────────────
Total:                              36h
```

### Nếu Bị Trễ Kế Hoạch
**Priority order khi thiếu thời gian:**
1. 🔴 PHẢI có: Register + Login hoạt động + JWT valid
2. 🟡 NÊN có: Unit tests + Global exception handler
3. 🟢 CÓ THỂ bỏ sang Sprint 2: Swagger detailed docs + CORS config

---

*Sprint 1 Complete → Sẵn sàng cho Sprint 2: Core Transaction System* 🚀
