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

## Code Generation & Mapping

### OpenAPI Generator Plugin

Este projeto utiliza o **OpenAPI Generator** para gerar automaticamente a camada de API a partir de uma especificação OpenAPI.

#### Configuração

```gradle
plugins {
    id 'org.openapi.generator' version '7.10.0'
}

dependencies {
    // Dependências necessárias para código gerado
    implementation 'io.swagger.core.v3:swagger-annotations:2.2.26'
    implementation 'org.openapitools:jackson-databind-nullable:0.2.6'
    implementation 'jakarta.validation:jakarta.validation-api:3.1.0'
}

openApiGenerate {
    generatorName = "jaxrs-spec"
    inputSpec = "$rootDir/src/main/resources/openapi/openapi.yaml"
    outputDir = "${layout.buildDirectory.get()}/generated/openapi"
    apiPackage = "br.com.iagoomes.app.api"
    modelPackage = "br.com.iagoomes.app.api.model"

    configOptions = [
        dateLibrary: "java8",
        useBeanValidation: "true",
        interfaceOnly: "true",
        returnResponse: "true",
        useJakartaEe: "true",
        delegatePattern: "true"  // Padrão Delegate
    ]
}

// Gera código antes da compilação
compileJava.dependsOn tasks.openApiGenerate
```

#### Estrutura de Arquivos

```
src/main/resources/openapi/
└── openapi.yaml                    # Especificação OpenAPI

build/generated/openapi/src/main/java/
└── br/com/iagoomes/app/api/
    ├── AgenciaApi.java            # Interface gerada
    ├── ContaApi.java              # Interface gerada
    └── model/
        ├── AgenciaRequest.java    # DTOs gerados
        ├── AgenciaResponse.java
        └── ...
```

#### Comandos

```bash
# Gerar código da API
./gradlew openApiGenerate

# Limpar e regenerar
./gradlew clean openApiGenerate

# Build completo (inclui geração)
./gradlew build
```

#### Padrão Delegate

Com `delegatePattern: true`, o generator cria interfaces JAX-RS que você implementa:

**Interface Gerada** (`AgenciaApi.java`):
```java
@Path("/api/v1/agencias")
public interface AgenciaApi {

    @GET
    @Path("/{id}")
    @Produces({"application/json"})
    Response buscarAgenciaPorId(@PathParam("id") Long id);

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Response criarAgencia(@Valid AgenciaRequest request);
}
```

**Implementação** (`AgenciaApiImpl.java`):
```java
@ApplicationScoped
public class AgenciaApiImpl implements AgenciaApi {

    @Inject
    AgenciaService agenciaService;

    @Inject
    AgenciaMapper agenciaMapper;

    @Override
    public Response buscarAgenciaPorId(Long id) {
        return agenciaService.buscarPorId(id)
            .map(agenciaMapper::toResponse)
            .map(response -> Response.ok(response).build())
            .orElse(Response.status(Status.NOT_FOUND).build());
    }

    @Override
    public Response criarAgencia(AgenciaRequest request) {
        var agencia = agenciaMapper.toEntity(request);
        var criada = agenciaService.criar(agencia);
        var response = agenciaMapper.toResponse(criada);
        return Response.status(Status.CREATED).entity(response).build();
    }
}
```

**Vantagens:**
- ✅ API sempre sincronizada com a especificação OpenAPI
- ✅ Validações automáticas via Bean Validation
- ✅ Separação clara: Interface (contrato) vs Implementação (lógica)
- ✅ Type-safe: Erros de compilação se a spec mudar
- ✅ Documentação automática via Swagger

---

### MapStruct

**MapStruct** é usado para mapeamento entre DTOs (gerados) e entidades de domínio.

#### Configuração

```gradle
dependencies {
    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
}

// Configuração para processar anotações
tasks.withType(JavaCompile).configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor
}
```

#### Uso Básico

```java
@Mapper(
    componentModel = "cdi",  // Integração com CDI
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AgenciaMapper {

    // Request -> Entity
    Agencia toEntity(AgenciaRequest request);

    // Entity -> Response
    AgenciaResponse toResponse(Agencia agencia);

    // Update entity (ignora nulls)
    void updateEntityFromRequest(
        AgenciaRequest request,
        @MappingTarget Agencia agencia
    );
}
```

**O MapStruct gera automaticamente**:

```java
@ApplicationScoped
public class AgenciaMapperImpl implements AgenciaMapper {

    @Override
    public Agencia toEntity(AgenciaRequest request) {
        Agencia agencia = new Agencia();
        agencia.setNumero(request.getNumero());
        agencia.setNome(request.getNome());
        // ... mapeia todos os campos automaticamente
        return agencia;
    }

    // ... outras implementações
}
```

#### Mapeamentos Customizados

```java
@Mapper(componentModel = "cdi")
public interface AgenciaMapper {

    // Mapeamento com transformação customizada
    @Mapping(target = "situacaoCadastral",
             expression = "java(mapSituacao(request))")
    Agencia toEntity(AgenciaRequest request);

    default String mapSituacao(AgenciaRequest request) {
        // Lógica customizada
        return "ATIVA";
    }
}
```

#### Vantagens vs Mapeamento Manual

**Manual** (Evite!):
```java
public AgenciaResponse toResponse(Agencia agencia) {
    AgenciaResponse response = new AgenciaResponse();
    response.setId(agencia.getId());
    response.setNumero(agencia.getNumero());
    response.setNome(agencia.getNome());
    // ... 20 linhas de boilerplate
    return response;
}
```

**MapStruct**:
```java
@Mapper(componentModel = "cdi")
public interface AgenciaMapper {
    AgenciaResponse toResponse(Agencia agencia);
    // ✨ Implementação gerada automaticamente!
}
```

**Vantagens:**
- ✅ Zero boilerplate
- ✅ Type-safe em tempo de compilação
- ✅ Performance (sem reflection)
- ✅ Fácil manutenção
- ✅ Suporte a mapeamentos complexos

---

### Bean Validation com Hibernate Validator

**Bean Validation** permite validar automaticamente os dados de entrada da API usando anotações Jakarta.

#### Configuração

```gradle
dependencies {
    // Validação automática de requests
    implementation 'io.quarkus:quarkus-hibernate-validator'

    // API de validação (já incluída pelo OpenAPI Generator)
    implementation 'jakarta.validation:jakarta.validation-api:3.1.0'
}
```

#### Como Funciona com OpenAPI Generator

Quando você define validações no `openapi.yaml`:

```yaml
components:
  schemas:
    AgenciaRequest:
      required:
        - numero
        - nome
        - endereco
      properties:
        numero:
          type: string
          minLength: 4
          maxLength: 4
          description: Número da agência
        nome:
          type: string
          minLength: 3
          maxLength: 100
        telefone:
          type: string
          pattern: '^\(\d{2}\) \d{4,5}-\d{4}$'
```

O OpenAPI Generator automaticamente gera as anotações:

```java
public class AgenciaRequest {
    @NotNull
    @Size(min=4, max=4)
    private String numero;

    @NotNull
    @Size(min=3, max=100)
    private String nome;

    @Pattern(regexp="^\\(\\d{2}\\) \\d{4,5}-\\d{4}$")
    private String telefone;

    @NotNull
    @Valid
    private EnderecoRequest endereco;
}
```

E a interface da API já inclui `@Valid`:

```java
@Path("/api/v1/agencias")
public interface AgenciaApi {

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Response criarAgencia(@Valid @NotNull AgenciaRequest agenciaRequest);
}
```

#### ⚠️ IMPORTANTE: Não Redefina @Valid na Implementação

**ERRADO** ❌:
```java
@ApplicationScoped
public class AgenciaApiImpl implements AgenciaApi {

    @Override
    public Response criarAgencia(@Valid AgenciaRequest request) {  // ❌ NÃO FAÇA ISSO!
        // ...
    }
}
```

**Erro que ocorre:**
```
jakarta.validation.ConstraintDeclarationException: HV000151:
A method overriding another method must not redefine the parameter
constraint configuration, but method AgenciaApiImpl#criarAgencia(AgenciaRequest)
redefines the configuration of AgenciaApi#criarAgencia(AgenciaRequest).
```

**CORRETO** ✅:
```java
@ApplicationScoped
public class AgenciaApiImpl implements AgenciaApi {

    @Override
    public Response criarAgencia(AgenciaRequest request) {  // ✅ Sem @Valid
        // As validações são aplicadas automaticamente pela interface
        var agencia = agenciaMapper.toEntity(request);
        return Response.status(Status.CREATED).entity(agencia).build();
    }
}
```

**Por quê?** A interface `AgenciaApi` já tem `@Valid`. Adicionar novamente na implementação viola as regras do Hibernate Validator (HV000151).

#### Testando as Validações

**Requisição com dados inválidos:**
```bash
curl -X POST http://localhost:8080/api/v1/agencias \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "001",
    "nome": "AB",
    "telefone": "11-3333-4444"
  }'
```

**Resposta automática (HTTP 400):**
```json
{
  "title": "Constraint Violation",
  "status": 400,
  "violations": [
    {
      "field": "criarAgencia.agenciaRequest.numero",
      "message": "tamanho deve ser entre 4 e 4"
    },
    {
      "field": "criarAgencia.agenciaRequest.nome",
      "message": "tamanho deve ser entre 3 e 100"
    },
    {
      "field": "criarAgencia.agenciaRequest.telefone",
      "message": "deve corresponder a \"^\\(\\d{2}\\) \\d{4,5}-\\d{4}$\""
    },
    {
      "field": "criarAgencia.agenciaRequest.endereco",
      "message": "não deve ser nulo"
    }
  ]
}
```

**Requisição válida:**
```bash
curl -X POST http://localhost:8080/api/v1/agencias \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "0001",
    "nome": "Agência Centro",
    "endereco": {
      "cep": "01310-100",
      "logradouro": "Avenida Paulista",
      "numero": "1000",
      "bairro": "Bela Vista",
      "cidade": "São Paulo",
      "estado": "SP"
    },
    "telefone": "(11) 3333-4444",
    "gerente": "João Silva"
  }'
```

**Resposta (HTTP 201):**
```json
{
  "id": 1,
  "numero": "0001",
  "nome": "Agência Centro",
  "endereco": {
    "cep": "01310-100",
    "logradouro": "Avenida Paulista",
    "numero": "1000",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP"
  },
  "telefone": "(11) 3333-4444",
  "gerente": "João Silva",
  "dataCadastro": "2025-11-04T22:26:02.255984",
  "dataAtualizacao": "2025-11-04T22:26:02.25599"
}
```

#### Anotações Comuns de Validação

| Anotação | OpenAPI | Descrição |
|----------|---------|-----------|
| `@NotNull` | `required: true` | Campo obrigatório |
| `@NotBlank` | - | Não nulo e não vazio |
| `@Size(min, max)` | `minLength`, `maxLength` | Tamanho de strings |
| `@Min`, `@Max` | `minimum`, `maximum` | Valores numéricos |
| `@Pattern` | `pattern` | Regex de validação |
| `@Email` | `format: email` | Formato de email |
| `@Valid` | Objetos aninhados | Validação em cascata |

**Vantagens:**
- ✅ Validação automática antes do código executar
- ✅ Respostas de erro padronizadas
- ✅ Zero código de validação manual
- ✅ Sincronizado com especificação OpenAPI
- ✅ Type-safe em tempo de compilação

---

### Workflow Completo: OpenAPI + MapStruct

```
1. Definir API          →  src/main/resources/openapi/openapi.yaml
   (OpenAPI Spec)
                            ↓
2. Gerar Interfaces     →  ./gradlew openApiGenerate
   (OpenAPI Generator)
                            ↓
3. Criar Mapper         →  AgenciaMapper.java (interface)
   (MapStruct)
                            ↓
4. Implementar API      →  AgenciaApiImpl.java
   (Delegate Pattern)       implements AgenciaApi
                            ↓
5. Lógica de Negócio    →  AgenciaService.java
   (Service Layer)
```

**Exemplo de Fluxo:**

```
POST /api/v1/agencias
{
  "numero": "0001",
  "nome": "Agência Centro"
}

            ↓

[AgenciaApiImpl]
    ├─ Recebe AgenciaRequest (gerado pelo OpenAPI)
    ├─ Valida automaticamente (Bean Validation)
    ↓

[AgenciaMapper]
    ├─ toEntity(request) → Agencia
    ↓

[AgenciaService]
    ├─ criar(agencia) → lógica de negócio
    ↓

[AgenciaMapper]
    ├─ toResponse(agencia) → AgenciaResponse
    ↓

[AgenciaApiImpl]
    └─ Response.status(201).entity(response)

            ↓

HTTP 201 Created
{
  "id": 1,
  "numero": "0001",
  "nome": "Agência Centro",
  "dataCadastro": "2024-01-15T10:30:00Z"
}
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

    // Validation
    implementation 'io.quarkus:quarkus-hibernate-validator'

    // Database
    implementation 'io.quarkus:quarkus-hibernate-orm-panache'
    implementation 'io.quarkus:quarkus-jdbc-postgresql'

    // OpenAPI / Swagger UI
    implementation 'io.quarkus:quarkus-smallrye-openapi'

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
- `quarkus-hibernate-validator` → `spring-boot-starter-validation`
- `quarkus-hibernate-orm-panache` → `spring-boot-starter-data-jpa`
- `quarkus-jdbc-postgresql` → `postgresql` driver
- `quarkus-smallrye-openapi` → `springdoc-openapi`
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

## Como Usar Este Projeto

### 1. Executar em Modo Dev

```bash
./gradlew quarkusDev
```

### 2. Acessar Swagger UI

Após iniciar, acesse:

- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/q/openapi
- **Dev UI**: http://localhost:8080/q/dev
- **Health Check**: http://localhost:8080/q/health

### 3. Testar Endpoints

**Criar Agência:**
```bash
curl -X POST http://localhost:8080/api/v1/agencias \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "0001",
    "nome": "Agência Centro",
    "endereco": {
      "cep": "01310-100",
      "logradouro": "Avenida Paulista",
      "numero": "1000",
      "bairro": "Bela Vista",
      "cidade": "São Paulo",
      "estado": "SP"
    },
    "telefone": "(11) 3333-4444",
    "gerente": "João Silva"
  }'
```

**Listar Agências:**
```bash
curl http://localhost:8080/api/v1/agencias
```

**Buscar por ID:**
```bash
curl http://localhost:8080/api/v1/agencias/1
```

**Atualizar Agência:**
```bash
curl -X PUT http://localhost:8080/api/v1/agencias/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Agência Centro - Atualizada",
    "telefone": "(11) 3333-5555"
  }'
```

**Deletar Agência:**
```bash
curl -X DELETE http://localhost:8080/api/v1/agencias/1
```

### 4. Modificar a API

**Passos para adicionar novos endpoints:**

1. **Edite o OpenAPI spec**:
   ```bash
   vim src/main/resources/openapi/openapi.yaml
   ```

2. **Regenere o código**:
   ```bash
   ./gradlew clean openApiGenerate
   ```

3. **Implemente a interface gerada** em `AgenciaApiImpl.java`

4. **Build e teste**:
   ```bash
   ./gradlew build
   ./gradlew quarkusDev
   ```

### 5. Estrutura do Código Gerado

```
build/generated/openapi/src/main/java/
└── br/com/iagoomes/app/api/
    ├── AgenciaApi.java          # Interface JAX-RS (gerada)
    ├── ContaApi.java            # Interface JAX-RS (gerada)
    └── model/
        ├── AgenciaRequest.java  # DTO com Builder (gerado)
        ├── AgenciaResponse.java # DTO com Builder (gerado)
        └── ...
```

### 6. Exemplo de Uso do Builder Pattern

Os DTOs gerados incluem Builder Pattern:

```java
AgenciaRequest agencia = AgenciaRequest.builder()
    .numero("0001")
    .nome("Agência Centro")
    .telefone("(11) 3333-4444")
    .endereco(EnderecoRequest.builder()
        .cep("01310-100")
        .logradouro("Avenida Paulista")
        .numero("1000")
        .build())
    .build();
```

---

## Arquitetura do Projeto

```
┌─────────────────────────────────────────────────────────┐
│                    OpenAPI Spec                          │
│              (openapi.yaml - fonte única)                │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │   OpenAPI Generator Plugin   │
        │   (jaxrs-spec generator)     │
        └──────────┬──────────────────┘
                   │
                   ├──► Interfaces JAX-RS (AgenciaApi)
                   └──► DTOs com Builders (AgenciaRequest/Response)

┌────────────────────┴────────────────────┐
│                                          │
▼                                          ▼
┌──────────────────┐            ┌──────────────────┐
│  AgenciaApiImpl  │            │  AgenciaMapper   │
│  (implementação) │────────────│   (MapStruct)    │
└────────┬─────────┘            └──────────────────┘
         │                               │
         ▼                               ▼
┌──────────────────┐            ┌──────────────────┐
│ AgenciaService   │────────────│  Agencia Entity  │
│ (lógica negócio) │            │  (domínio)       │
└──────────────────┘            └──────────────────┘
```

**Fluxo de Requisição:**
1. Cliente → HTTP Request
2. **AgenciaApi** (interface gerada) → valida request
3. **AgenciaApiImpl** → implementa interface
4. **AgenciaMapper** → converte DTO ↔ Entity
5. **AgenciaService** → lógica de negócio
6. **Agencia Entity** → modelo de domínio
7. Response → Cliente

---

## Tecnologias Utilizadas

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Quarkus** | 3.27.0 | Framework principal |
| **Gradle** | 9.0.0 | Build tool |
| **Java** | 21 | Linguagem |
| **OpenAPI Generator** | 7.10.0 | Geração de código da API |
| **MapStruct** | 1.6.3 | Mapeamento DTO ↔ Entity |
| **Jakarta EE** | 10+ | Especificações (JAX-RS, Bean Validation) |
| **SmallRye OpenAPI** | - | Swagger UI integrado |

---

## Próximos Passos

### Melhorias Recomendadas:
- [ ] Adicionar persistência com Panache Repository
- [ ] Implementar paginação nos endpoints de listagem
- [ ] Adicionar autenticação/autorização (JWT, OIDC)
- [ ] Criar testes unitários e de integração
- [ ] Adicionar tratamento de exceções global
- [ ] Implementar cache (Redis, Caffeine)
- [ ] Configurar CI/CD
- [ ] Adicionar Docker/Docker Compose
- [ ] Compilação nativa (GraalVM)

### Áreas a Explorar:
- [ ] Reactive Programming (Mutiny)
- [ ] Security (Quarkus OIDC)
- [ ] Messaging (Kafka, RabbitMQ)
- [ ] Observability (Tracing, Metrics)
- [ ] Kubernetes Integration

---

## Recursos Úteis

### Documentação Oficial
- [Quarkus Guides](https://quarkus.io/guides/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [MapStruct Reference](https://mapstruct.org/documentation/stable/reference/html/)

### Dev Tools
- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **Dev UI**: http://localhost:8080/q/dev
- **Health Check**: http://localhost:8080/q/health
- **Metrics**: http://localhost:8080/q/metrics

### Cheat Sheets
- [Quarkus Cheat Sheet](https://lordofthejars.github.io/quarkus-cheat-sheet/)
- [JAX-RS Annotations](https://dennis-xlc.gitbooks.io/restful-java-with-jax-rs-2-0-2rd-edition/content/en/part1/chapter3/jax_rs_injection.html)

---

## Troubleshooting

### OpenAPI Generator não gera código
```bash
./gradlew clean openApiGenerate --info
```

### Build falha com erro de MapStruct
Verifique se a ordem de processamento está correta:
```bash
./gradlew clean build
```

### Swagger UI não aparece
Certifique-se que a dependência está adicionada:
```gradle
implementation 'io.quarkus:quarkus-smallrye-openapi'
```

### Hot Reload não funciona
Reinicie o Quarkus Dev:
```bash
# Pressione 'r' no terminal do quarkusDev
# ou Ctrl+C e rode novamente
./gradlew quarkusDev
```

---

**Última atualização**: 2025-11-04
**Versão**: 1.0.0-SNAPSHOT
