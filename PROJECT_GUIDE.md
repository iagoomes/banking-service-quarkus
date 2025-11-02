# Banking Service Quarkus - Guia do Projeto

> Guia de referência rápida para desenvolvimento com Quarkus + Gradle

---

## Informações do Projeto

- **Nome**: banking-service-quarkus
- **Versão**: 1.0.0-SNAPSHOT
- **Grupo**: br.com.iagoomes
- **Framework**: Quarkus 3.27.0
- **Build Tool**: Gradle 9.0.0
- **Java Version**: 21

---

## Comandos Essenciais

### Desenvolvimento
```bash
# Iniciar em modo desenvolvimento (com live reload)
./gradlew quarkusDev

# ⚠️ IMPORTANTE: Gradle exige pelo menos uma classe Java em src/main/java
# Caso contrário, você receberá o erro:
# "At least one source directory should contain sources before starting Quarkus in dev mode"
```

### Build
```bash
# Build padrão
./gradlew build

# Build uber-jar
./gradlew build -Dquarkus.package.jar.type=uber-jar

# Executar aplicação buildada
java -jar build/quarkus-app/quarkus-run.jar
```

### Testes
```bash
# Executar testes
./gradlew test

# Executar testes em modo contínuo
./gradlew quarkusTest
```

### Native Build
```bash
# Compilação nativa (requer GraalVM)
./gradlew build -Dquarkus.native.enabled=true

# Compilação nativa via container
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Executar nativo
./build/banking-service-quarkus-1.0.0-SNAPSHOT-runner
```

---

## Diferenças: Spring Boot vs Quarkus (neste projeto)

### 1. Estrutura de Projeto

**Spring Boot:**
```
src/main/java/
  └── com/example/demo/
      ├── DemoApplication.java          # Classe main com @SpringBootApplication
      ├── controller/
      │   └── UserController.java       # @RestController
      ├── service/
      │   └── UserService.java          # @Service
      ├── repository/
      │   └── UserRepository.java       # extends JpaRepository
      └── model/
          └── User.java                 # @Entity
```

**Quarkus:**
```
src/main/java/
  └── br/com/iagoomes/
      ├── # Não há classe Application necessária!
      ├── resource/
      │   └── UserResource.java         # @Path (JAX-RS)
      ├── service/
      │   └── UserService.java          # @ApplicationScoped
      ├── repository/
      │   └── UserRepository.java       # extends PanacheRepository
      └── entity/
          └── User.java                 # @Entity
```

### 2. REST Endpoints

**Spring Boot:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userService.save(user));
    }
}
```

**Quarkus (JAX-RS):**
```java
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") Long id) {
        return Response.ok(userService.findById(id)).build();
    }

    @POST
    public Response createUser(User user) {
        return Response.status(Response.Status.CREATED)
            .entity(userService.save(user))
            .build();
    }
}
```

### 3. Dependency Injection

**Spring Boot:**
```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ou via constructor injection (recomendado)
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**Quarkus (CDI):**
```java
@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    // ou via constructor injection
    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

### 4. Repository/Data Access

**Spring Boot (Spring Data JPA):**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByName(String name);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
```

**Quarkus (Panache Repository Pattern):**
```java
@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public List<User> findByName(String name) {
        return list("name", name);
    }

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
```

**Quarkus (Panache Active Record - Alternativa):**
```java
@Entity
public class User extends PanacheEntity {
    public String name;
    public String email;

    // Métodos estáticos
    public static List<User> findByName(String name) {
        return list("name", name);
    }

    public static Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}

// Uso:
User.findByName("John");
User.findByEmail("john@example.com");
```

### 5. Configuração (application.properties)

**Spring Boot:**
```properties
server.port=8080
spring.application.name=my-app

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Quarkus:**
```properties
quarkus.http.port=8080
quarkus.application.name=banking-service

# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=user
quarkus.datasource.password=pass

# Hibernate
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
```

### 6. Inicialização da Aplicação

**Spring Boot:**
```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**Quarkus:**
```java
// ✨ Não é necessário classe main!
// Quarkus detecta automaticamente suas classes anotadas
// O framework cuida da inicialização

// Se precisar executar código na inicialização:
@ApplicationScoped
public class Startup {

    void onStart(@Observes StartupEvent ev) {
        System.out.println("Application starting...");
    }
}
```

### 7. Exception Handling

**Spring Boot:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

**Quarkus:**
```java
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse(ex.getMessage()))
            .build();
    }
}
```

### 8. Validation

**Spring Boot:**
```java
@RestController
public class UserController {

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        // Spring valida automaticamente
        return ResponseEntity.ok(userService.save(user));
    }
}

@Entity
public class User {
    @NotBlank
    private String name;

    @Email
    private String email;
}
```

**Quarkus:**
```java
@Path("/users")
public class UserResource {

    @POST
    public Response create(@Valid User user) {
        // Quarkus valida automaticamente
        return Response.ok(userService.save(user)).build();
    }
}

@Entity
public class User {
    @NotBlank
    public String name;

    @Email
    public String email;
}
```

### 9. Testing

**Spring Boot:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk());
    }
}
```

**Quarkus:**
```java
@QuarkusTest
class UserResourceTest {

    @Test
    void shouldGetUser() {
        given()
            .when().get("/api/users/1")
            .then()
            .statusCode(200);
    }
}
```

---

## Dependências do Projeto

```gradle
dependencies {
    // REST
    implementation 'io.quarkus:quarkus-rest'
    implementation 'io.quarkus:quarkus-rest-jackson'

    // REST Client
    implementation 'io.quarkus:quarkus-rest-client-jackson'

    // Database
    implementation 'io.quarkus:quarkus-hibernate-orm-panache'
    implementation 'io.quarkus:quarkus-jdbc-postgresql'

    // Observability
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation 'io.quarkus:quarkus-micrometer'

    // CDI
    implementation 'io.quarkus:quarkus-arc'

    // Testing
    testImplementation 'io.quarkus:quarkus-junit5'
}
```

**Equivalentes Spring Boot:**
- `quarkus-rest` → `spring-boot-starter-web`
- `quarkus-rest-jackson` → incluído em `spring-boot-starter-web`
- `quarkus-hibernate-orm-panache` → `spring-boot-starter-data-jpa`
- `quarkus-jdbc-postgresql` → `postgresql` driver
- `quarkus-smallrye-health` → `spring-boot-starter-actuator`
- `quarkus-micrometer` → `spring-boot-starter-actuator` + `micrometer`

---

## Dev Services

Uma das features mais poderosas do Quarkus é o **Dev Services**. Em modo dev, o Quarkus automaticamente inicia containers para serviços como bancos de dados, Kafka, etc.

**Exemplo com PostgreSQL:**
```properties
# Não precisa configurar URL em dev mode!
# Quarkus inicia automaticamente um PostgreSQL via Docker

# Em produção:
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://prod-db:5432/banking
%prod.quarkus.datasource.username=${DB_USER}
%prod.quarkus.datasource.password=${DB_PASSWORD}
```

**Spring Boot equivalente:**
Você precisaria configurar manualmente um Docker Compose ou usar Testcontainers explicitamente.

---

## Live Reload vs DevTools

**Spring Boot DevTools:**
- Monitora mudanças nos arquivos
- **Reinicia** a aplicação (fast restart, mas ainda é restart)
- Requer configuração adicional em alguns IDEs

**Quarkus Dev Mode:**
- Monitora mudanças nos arquivos
- **Não reinicia** - recarrega classes em tempo real
- Build incremental
- Funciona out-of-the-box

---

## Health Checks

**Spring Boot Actuator:**
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up().withDetail("service", "operational").build();
    }
}
```

Endpoint: `http://localhost:8080/actuator/health`

**Quarkus SmallRye Health:**
```java
@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("banking-service");
    }
}
```

Endpoints:
- Liveness: `http://localhost:8080/q/health/live`
- Readiness: `http://localhost:8080/q/health/ready`

---

## Configurações de Produção

### Profile Management

**Spring Boot:**
```properties
# application.properties
spring.application.name=banking-service

# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/banking_dev
server.port=8080
logging.level.root=DEBUG

# application-prod.properties
spring.datasource.url=jdbc:postgresql://prod-db:5432/banking
server.port=8080
logging.level.root=INFO
```

**Ativação:**
```bash
# Via argumento
java -jar app.jar --spring.profiles.active=prod

# Via variável de ambiente
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

**Quarkus:**
```properties
# application.properties (comum a todos)
quarkus.application.name=banking-service

# Dev mode (sem prefixo ou %dev)
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/banking_dev
%dev.quarkus.log.level=DEBUG
%dev.quarkus.http.port=8080

# Test mode
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:test

# Production (sempre usar %prod prefix)
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://prod-db:5432/banking
%prod.quarkus.log.level=INFO
%prod.quarkus.http.port=8080
```

**Ativação:**
```bash
# Via argumento
java -jar app.jar -Dquarkus.profile=prod

# Quarkus automaticamente detecta prod quando NÃO está em dev mode
java -jar build/quarkus-app/quarkus-run.jar
```

---

### Database Configuration (Production)

**Spring Boot (application-prod.properties):**
```properties
# Datasource
spring.datasource.url=jdbc:postgresql://${DB_HOST:prod-db}:${DB_PORT:5432}/${DB_NAME:banking}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Connection Pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# SQL Migration (Flyway/Liquibase)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

**Quarkus (application.properties):**
```properties
# Datasource (Production)
%prod.quarkus.datasource.db-kind=postgresql
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://${DB_HOST:prod-db}:${DB_PORT:5432}/${DB_NAME:banking}
%prod.quarkus.datasource.username=${DB_USER}
%prod.quarkus.datasource.password=${DB_PASSWORD}

# Connection Pool (Agroal)
%prod.quarkus.datasource.jdbc.min-size=5
%prod.quarkus.datasource.jdbc.max-size=20
%prod.quarkus.datasource.jdbc.acquisition-timeout=30
%prod.quarkus.datasource.jdbc.background-validation-interval=2M
%prod.quarkus.datasource.jdbc.idle-removal-interval=5M
%prod.quarkus.datasource.jdbc.max-lifetime=30M

# Hibernate ORM
%prod.quarkus.hibernate-orm.database.generation=validate
%prod.quarkus.hibernate-orm.log.sql=false
%prod.quarkus.hibernate-orm.log.format-sql=false
%prod.quarkus.hibernate-orm.jdbc.statement-batch-size=20

# SQL Migration (Flyway)
%prod.quarkus.flyway.migrate-at-start=true
%prod.quarkus.flyway.locations=db/migration
```

**⚠️ IMPORTANTE - Database Generation:**
- **Desenvolvimento**: `update` ou `create-drop`
- **Produção**: SEMPRE usar `validate` + Flyway/Liquibase

---

### Environment Variables & Secrets

**Spring Boot:**
```yaml
# docker-compose.yml ou Kubernetes
environment:
  SPRING_PROFILES_ACTIVE: prod
  DB_HOST: postgres-service
  DB_PORT: 5432
  DB_NAME: banking
  DB_USER: banking_user
  # Secrets via Kubernetes Secrets ou AWS Secrets Manager
  DB_PASSWORD: ${DATABASE_PASSWORD}

  # Outras configurações
  JAVA_OPTS: "-Xms512m -Xmx2048m"
```

**Quarkus:**
```yaml
# docker-compose.yml ou Kubernetes
environment:
  # Quarkus detecta prod automaticamente quando não em dev mode
  QUARKUS_PROFILE: prod
  DB_HOST: postgres-service
  DB_PORT: 5432
  DB_NAME: banking
  DB_USER: banking_user
  # Secrets via Kubernetes Secrets ou AWS Secrets Manager
  DB_PASSWORD: ${DATABASE_PASSWORD}

  # JVM Options
  JAVA_OPTS_APPEND: "-Xms512m -Xmx2048m"

  # Configurações diretas via ENV (sobrescreve application.properties)
  QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres:5432/banking
  QUARKUS_HTTP_PORT: 8080
```

**Mapeamento de variáveis:**
- Spring: `SPRING_DATASOURCE_URL` → `spring.datasource.url`
- Quarkus: `QUARKUS_DATASOURCE_JDBC_URL` → `quarkus.datasource.jdbc.url`

---

### Logging (Production)

**Spring Boot:**
```properties
# application-prod.properties
logging.level.root=INFO
logging.level.br.com.iagoomes=INFO
logging.level.org.springframework.web=WARN
logging.level.org.hibernate=WARN

# File logging
logging.file.name=/var/log/banking-service/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# JSON format (para ELK, Splunk, etc)
logging.pattern.console=%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n
```

**Quarkus:**
```properties
# application.properties
%prod.quarkus.log.level=INFO
%prod.quarkus.log.category."br.com.iagoomes".level=INFO
%prod.quarkus.log.category."io.quarkus".level=WARN
%prod.quarkus.log.category."org.hibernate".level=WARN

# Console output (JSON para produção)
%prod.quarkus.log.console.enable=true
%prod.quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n
%prod.quarkus.log.console.json=true

# File logging
%prod.quarkus.log.file.enable=true
%prod.quarkus.log.file.path=/var/log/banking-service/application.log
%prod.quarkus.log.file.rotation.max-file-size=10M
%prod.quarkus.log.file.rotation.max-backup-index=30
```

---

### Observability & Monitoring (Production)

**Spring Boot Actuator:**
```properties
# application-prod.properties
management.endpoints.web.exposure.include=health,metrics,prometheus,info
management.endpoints.web.base-path=/actuator
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true

# Info endpoint
management.info.env.enabled=true
info.app.name=Banking Service
info.app.version=@project.version@
```

**Endpoints:**
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

**Quarkus:**
```properties
# application.properties
%prod.quarkus.smallrye-health.root-path=/health

# Metrics (Micrometer)
%prod.quarkus.micrometer.enabled=true
%prod.quarkus.micrometer.export.prometheus.enabled=true
%prod.quarkus.micrometer.export.prometheus.path=/metrics

# Application Info
%prod.quarkus.application.name=Banking Service
%prod.quarkus.application.version=${quarkus.application.version}
```

**Endpoints:**
- Liveness: `/q/health/live` ou `/health/live`
- Readiness: `/q/health/ready` ou `/health/ready`
- Metrics: `/q/metrics` ou `/metrics`

---

### Security Headers & CORS (Production)

**Spring Boot:**
```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy("default-src 'self'")
                .frameOptions().deny()
                .xssProtection().and()
                .contentTypeOptions().and()
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.example.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

**Quarkus:**
```properties
# application.properties
%prod.quarkus.http.cors=true
%prod.quarkus.http.cors.origins=https://app.example.com
%prod.quarkus.http.cors.methods=GET,POST,PUT,DELETE
%prod.quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with
%prod.quarkus.http.cors.exposed-headers=Content-Disposition
%prod.quarkus.http.cors.access-control-max-age=24H

# Security Headers
%prod.quarkus.http.header."X-Frame-Options".value=DENY
%prod.quarkus.http.header."X-Content-Type-Options".value=nosniff
%prod.quarkus.http.header."X-XSS-Protection".value=1; mode=block
%prod.quarkus.http.header."Strict-Transport-Security".value=max-age=31536000; includeSubDomains
```

---

### Resource Limits & Performance Tuning

**Spring Boot (JVM Options):**
```bash
# Dockerfile ou start script
JAVA_OPTS="-Xms512m \
           -Xmx2048m \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -XX:+HeapDumpOnOutOfMemoryError \
           -XX:HeapDumpPath=/var/log/heapdump.hprof \
           -Djava.security.egd=file:/dev/./urandom"

java $JAVA_OPTS -jar application.jar
```

**Quarkus (JVM Mode):**
```bash
# Dockerfile
JAVA_OPTS_APPEND="-Xms512m \
                  -Xmx2048m \
                  -XX:+UseG1GC \
                  -XX:MaxGCPauseMillis=200 \
                  -XX:+HeapDumpOnOutOfMemoryError \
                  -Djava.security.egd=file:/dev/./urandom"

java $JAVA_OPTS_APPEND -jar quarkus-run.jar
```

**Quarkus (Native Mode):**
```properties
# build.gradle - configuração de build nativo
quarkus {
    native {
        containerBuild = true
        builderImage = "quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21"

        // Otimizações
        enableHttpUrlHandler = true
        enableHttpsUrlHandler = true
        additionalBuildArgs = [
            "-H:+ReportExceptionStackTraces",
            "-H:ResourceConfigurationFiles=resources-config.json"
        ]
    }
}
```

**Resource limits (Kubernetes):**
```yaml
resources:
  requests:
    # Spring Boot JVM
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "2000m"

---
resources:
  requests:
    # Quarkus JVM
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"

---
resources:
  requests:
    # Quarkus Native
    memory: "64Mi"
    cpu: "100m"
  limits:
    memory: "256Mi"
    cpu: "500m"
```

---

### Container Configuration

**Spring Boot - Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criar usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY build/libs/*.jar application.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx2048m", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "application.jar"]
```

**Quarkus JVM - Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /work

# Criar usuário não-root
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus
USER quarkus:quarkus

# Copiar dependências e app separadamente para melhor cache
COPY --chown=quarkus:quarkus build/quarkus-app/lib/ ./lib/
COPY --chown=quarkus:quarkus build/quarkus-app/*.jar ./
COPY --chown=quarkus:quarkus build/quarkus-app/app/ ./app/
COPY --chown=quarkus:quarkus build/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "quarkus-run.jar"]
```

**Quarkus Native - Dockerfile:**
```dockerfile
FROM registry.access.redhat.com/ubi9/ubi-minimal:latest
WORKDIR /work

# Criar usuário não-root
RUN microdnf install -y shadow-utils && \
    groupadd -r quarkus && useradd -r -g quarkus quarkus

COPY --chown=quarkus:quarkus build/*-runner /work/application

RUN chmod +x /work/application

USER quarkus:quarkus

EXPOSE 8080

ENTRYPOINT ["./application", \
    "-Dquarkus.http.host=0.0.0.0", \
    "-Dquarkus.http.port=8080"]
```

---

### Kubernetes Deployment Example

**Spring Boot:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: banking-service-spring
spec:
  replicas: 3
  selector:
    matchLabels:
      app: banking-service
  template:
    metadata:
      labels:
        app: banking-service
    spec:
      containers:
      - name: banking-service
        image: banking-service-spring:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          value: postgres-service
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

**Quarkus:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: banking-service-quarkus
spec:
  replicas: 5  # Pode ter mais réplicas com menos recursos
  selector:
    matchLabels:
      app: banking-service
  template:
    metadata:
      labels:
        app: banking-service
    spec:
      containers:
      - name: banking-service
        image: banking-service-quarkus:1.0.0-native
        ports:
        - containerPort: 8080
        env:
        - name: DB_HOST
          value: postgres-service
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: password
        resources:
          requests:
            memory: "64Mi"   # Native image usa muito menos
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 5  # Startup muito mais rápido
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 2
          periodSeconds: 5
```

---

### Build para Produção

**Spring Boot:**
```bash
# Build JAR
./gradlew clean build -Pprod

# Executar testes
./gradlew test

# Build Docker image
docker build -t banking-service-spring:1.0.0 .

# Push para registry
docker tag banking-service-spring:1.0.0 myregistry.com/banking-service:1.0.0
docker push myregistry.com/banking-service:1.0.0
```

**Quarkus JVM:**
```bash
# Build JAR
./gradlew clean build

# Executar testes
./gradlew test

# Build Docker image
docker build -f src/main/docker/Dockerfile.jvm -t banking-service-quarkus:1.0.0 .

# Push
docker push myregistry.com/banking-service:1.0.0
```

**Quarkus Native:**
```bash
# Build native (via container)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true

# Build Docker image
docker build -f src/main/docker/Dockerfile.native -t banking-service-quarkus:1.0.0-native .

# Push
docker push myregistry.com/banking-service:1.0.0-native
```

---

### Checklist de Produção

**Configurações Essenciais:**
- [ ] Database com `validate` + Flyway/Liquibase
- [ ] Connection pool configurado apropriadamente
- [ ] Logs em formato JSON para agregação
- [ ] Health checks (liveness + readiness)
- [ ] Metrics exportando para Prometheus
- [ ] Secrets via variáveis de ambiente ou Secrets Manager
- [ ] CORS configurado corretamente
- [ ] Security headers habilitados
- [ ] Resource limits definidos (CPU/Memory)
- [ ] Container rodando como usuário não-root
- [ ] Backup automático do banco de dados
- [ ] Monitoramento e alertas configurados

**Performance:**
- [ ] JVM tuning (heap size, GC)
- [ ] Connection pool otimizado
- [ ] Índices de banco de dados criados
- [ ] Cache configurado (se necessário)
- [ ] Compressão HTTP habilitada

**Segurança:**
- [ ] TLS/HTTPS habilitado
- [ ] Variáveis sensíveis em secrets
- [ ] Network policies configuradas
- [ ] RBAC configurado (Kubernetes)
- [ ] Vulnerabilidades escaneadas (Trivy, Snyk)

---

## Próximos Passos

### Áreas a explorar:
- [ ] Reactive Programming (Mutiny vs WebFlux)
- [ ] Security (Quarkus OIDC vs Spring Security)
- [ ] Messaging (Kafka, RabbitMQ)
- [ ] Caching (Infinispan vs Spring Cache)
- [ ] Observability (Tracing, Metrics)
- [ ] Kubernetes Integration
- [ ] Native Image Build

---

## Recursos Úteis

- [Quarkus Dev UI](http://localhost:8080/q/dev/) - Disponível em dev mode
- [Quarkus Cheat Sheet](https://lordofthejars.github.io/quarkus-cheat-sheet/)
- [Spring to Quarkus Migration Guide](https://quarkus.io/guides/spring-di)

---

**Última atualização**: 2025-11-01
