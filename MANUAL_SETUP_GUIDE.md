# Guia de Setup Manual - Quarkus com Gradle

> Como criar um projeto Quarkus do zero sem acesso ao quarkus.io

Este guia mostra como montar manualmente um projeto Quarkus com Gradle em ambientes com restrições de acesso à internet ou ao site quarkus.io.

---

## 📋 Pré-requisitos

- Java 21 instalado
- Gradle 9.0+ instalado (ou use o Gradle Wrapper)
- Acesso ao Maven Central (ou repositório corporativo com mirror)

---

## 🔍 Descobrindo a Versão LTS do Quarkus

### Opção 1: Maven Central (mais confiável)

Acesse o Maven Central via navegador ou curl:

```bash
# Via navegador
https://central.sonatype.com/artifact/io.quarkus.platform/quarkus-bom

# Via curl (se tiver acesso)
curl -s "https://search.maven.org/solrsearch/select?q=g:io.quarkus.platform+AND+a:quarkus-bom&rows=5&wt=json" | grep -o '"latestVersion":"[^"]*"'
```

**Última versão LTS conhecida:** `3.27.0` (Janeiro 2025)

### Opção 2: Repositório Corporativo

Se sua empresa tem um Nexus/Artifactory, verifique lá:

```bash
# Exemplo com Nexus
https://seu-nexus.empresa.com/#browse/browse:maven-public:io/quarkus/platform/quarkus-bom
```

### Opção 3: GitHub Releases (via proxy corporativo)

```bash
https://github.com/quarkusio/quarkus/releases
```

### Opção 4: Verificar localmente (se já tem projetos Quarkus)

```bash
# Procurar em projetos existentes
grep -r "quarkusPlatformVersion" ~/projetos/*/gradle.properties
```

---

## 📁 Estrutura de Diretórios Manual

Crie esta estrutura de pastas:

```bash
mkdir -p seu-projeto/{src/{main/{java/com/empresa/app,resources},test/java/com/empresa/app},gradle/wrapper}
cd seu-projeto
```

Estrutura completa:

```
seu-projeto/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar      # (baixar do Gradle oficial)
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/empresa/app/
│   │   │       └── GreetingResource.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/empresa/app/
│               └── GreetingResourceTest.java
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── .gitignore
```

---

## 📝 Arquivos de Configuração

### 1. `settings.gradle`

```gradle
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // Se usar repositório corporativo:
        // maven { url = uri("https://seu-nexus.empresa.com/repository/maven-public/") }
    }

    plugins {
        id 'io.quarkus' version "${quarkusPluginVersion}"
    }
}

rootProject.name = 'seu-projeto'
```

### 2. `gradle.properties`

```properties
# Versões do Quarkus (AJUSTAR CONFORME NECESSÁRIO)
quarkusPluginVersion=3.27.0
quarkusPlatformGroupId=io.quarkus.platform
quarkusPlatformArtifactId=quarkus-bom
quarkusPlatformVersion=3.27.0

# Configurações Gradle
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

### 3. `build.gradle`

```gradle
plugins {
    id 'java'
    id 'io.quarkus'
}

repositories {
    mavenCentral()
    mavenLocal()

    // Se usar repositório corporativo:
    // maven {
    //     url = uri("https://seu-nexus.empresa.com/repository/maven-public/")
    //     credentials {
    //         username = project.findProperty("nexusUser") ?: System.getenv("NEXUS_USER")
    //         password = project.findProperty("nexusPassword") ?: System.getenv("NEXUS_PASSWORD")
    //     }
    // }
}

dependencies {
    // BOM do Quarkus (gerencia versões)
    implementation enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")

    // Extensões mínimas para começar
    implementation 'io.quarkus:quarkus-rest'
    implementation 'io.quarkus:quarkus-rest-jackson'
    implementation 'io.quarkus:quarkus-arc'

    // Testes
    testImplementation 'io.quarkus:quarkus-junit5'
    testImplementation 'io.rest-assured:rest-assured'
}

group = 'com.empresa'
version = '1.0.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

test {
    systemProperty "java.util.logging.manager", "org.jboss.logmanager.LogManager"
}

compileJava {
    options.encoding = 'UTF-8'
    options.compilerArgs << '-parameters'
}

compileTestJava {
    options.encoding = 'UTF-8'
}
```

### 4. `src/main/resources/application.properties`

```properties
# Configuração HTTP
quarkus.http.port=8080

# Log level
quarkus.log.level=INFO
quarkus.log.console.enable=true
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n

# Banner customizado (opcional)
quarkus.banner.enabled=true
```

### 5. `src/main/java/com/empresa/app/GreetingResource.java`

```java
package com.empresa.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
```

### 6. `src/test/java/com/empresa/app/GreetingResourceTest.java`

```java
package com.empresa.app;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from Quarkus REST"));
    }
}
```

### 7. `.gitignore`

```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.classpath
.project
.settings/
bin/

# OS
.DS_Store
Thumbs.db

# Quarkus
target/
```

---

## 🔧 Gradle Wrapper (Opcional mas Recomendado)

### Opção A: Gerar com Gradle Instalado

```bash
gradle wrapper --gradle-version 9.0
```

### Opção B: Download Manual do Wrapper

Se não tiver Gradle instalado, baixe de outro projeto ou do site oficial:

1. Baixe `gradle-wrapper.jar` de:
   ```
   https://github.com/gradle/gradle/raw/v9.0.0/gradle/wrapper/gradle-wrapper.jar
   ```

2. Coloque em `gradle/wrapper/gradle-wrapper.jar`

3. Crie `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

4. Crie scripts `gradlew` e `gradlew.bat` (copie de outro projeto ou do template oficial)

**gradlew** (Linux/Mac):
```bash
#!/bin/sh
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
```

```bash
chmod +x gradlew
```

---

## ✅ Testando o Setup

### 1. Verificar Versões

```bash
./gradlew --version

# Deve mostrar:
# Gradle 9.0
# JVM: 21.x.x
```

### 2. Listar Dependências (verifica acesso ao repositório)

```bash
./gradlew dependencies --configuration runtimeClasspath
```

**⚠️ Problema Comum:** Se falhar aqui, verifique:
- Acesso ao Maven Central ou repositório corporativo
- Proxy corporativo configurado
- Credenciais corretas (se usar Nexus privado)

### 3. Compilar o Projeto

```bash
./gradlew build
```

**Esperado:** `BUILD SUCCESSFUL`

### 4. Executar em Modo Dev

```bash
./gradlew quarkusDev
```

**Esperado:**
```
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/

INFO  [io.quarkus] Quarkus 3.27.0 on JVM started in 1.234s. Listening on: http://localhost:8080
```

### 5. Testar Endpoint

```bash
curl http://localhost:8080/hello
# Esperado: Hello from Quarkus REST
```

### 6. Executar Testes

```bash
./gradlew test
```

---

## 🔐 Configuração de Proxy Corporativo

Se sua empresa usa proxy, configure:

### gradle.properties (adicionar)

```properties
systemProp.http.proxyHost=proxy.empresa.com
systemProp.http.proxyPort=8080
systemProp.http.proxyUser=seu-usuario
systemProp.http.proxyPassword=sua-senha
systemProp.http.nonProxyHosts=localhost|127.0.0.1

systemProp.https.proxyHost=proxy.empresa.com
systemProp.https.proxyPort=8080
systemProp.https.proxyUser=seu-usuario
systemProp.https.proxyPassword=sua-senha
systemProp.https.nonProxyHosts=localhost|127.0.0.1
```

**⚠️ Segurança:** Não commite senhas! Use variáveis de ambiente:

```properties
systemProp.http.proxyUser=${PROXY_USER}
systemProp.http.proxyPassword=${PROXY_PASSWORD}
```

---

## 📦 Adicionando Extensões Quarkus

### Via Gradle (Recomendado)

Edite `build.gradle` e adicione na seção `dependencies`:

```gradle
dependencies {
    // ... dependências existentes

    // Database
    implementation 'io.quarkus:quarkus-hibernate-orm-panache'
    implementation 'io.quarkus:quarkus-jdbc-postgresql'

    // Validation
    implementation 'io.quarkus:quarkus-hibernate-validator'

    // OpenAPI / Swagger
    implementation 'io.quarkus:quarkus-smallrye-openapi'

    // REST Client
    implementation 'io.quarkus:quarkus-rest-client-jackson'

    // Health & Metrics
    implementation 'io.quarkus:quarkus-smallrye-health'
    implementation 'io.quarkus:quarkus-micrometer'
}
```

### Lista de Extensões Comuns

| Extensão | Dependência Gradle |
|----------|-------------------|
| REST | `io.quarkus:quarkus-rest` |
| Jackson | `io.quarkus:quarkus-rest-jackson` |
| Hibernate ORM | `io.quarkus:quarkus-hibernate-orm-panache` |
| PostgreSQL | `io.quarkus:quarkus-jdbc-postgresql` |
| MySQL | `io.quarkus:quarkus-jdbc-mysql` |
| Validation | `io.quarkus:quarkus-hibernate-validator` |
| OpenAPI | `io.quarkus:quarkus-smallrye-openapi` |
| Health | `io.quarkus:quarkus-smallrye-health` |
| Metrics | `io.quarkus:quarkus-micrometer` |
| REST Client | `io.quarkus:quarkus-rest-client-jackson` |
| Kafka | `io.quarkus:quarkus-kafka-client` |
| Redis | `io.quarkus:quarkus-redis-client` |
| Security JWT | `io.quarkus:quarkus-smallrye-jwt` |

**Descobrir mais extensões:**
```bash
./gradlew listExtensions
```

---

## 🚀 Evolução do Projeto Base

Depois de ter o projeto base funcionando, evolua para o projeto completo:

### 1. Adicionar OpenAPI Generator

**build.gradle:**
```gradle
plugins {
    id 'java'
    id 'io.quarkus'
    id 'org.openapi.generator' version '7.10.0'
}

dependencies {
    // OpenAPI Generator dependencies
    implementation 'io.swagger.core.v3:swagger-annotations:2.2.26'
    implementation 'org.openapitools:jackson-databind-nullable:0.2.6'
    implementation 'jakarta.validation:jakarta.validation-api:3.1.0'
}

// Configuração do OpenAPI Generator
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
        useSwaggerAnnotations: "false"
    ]
}

sourceSets {
    main {
        java {
            srcDir "$generatedSourcesDir/src/main/java"
        }
    }
}

tasks.named('compileJava') {
    dependsOn tasks.named('openApiGenerate')
}
```

### 2. Adicionar MapStruct

**build.gradle:**
```gradle
dependencies {
    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
}

tasks.withType(JavaCompile).configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor
}
```

### 3. Configurar Database

**application.properties:**
```properties
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/banking

# Hibernate
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
```

---

## 🐛 Troubleshooting

### Problema 1: "Could not resolve io.quarkus:quarkus-rest"

**Causa:** Sem acesso ao Maven Central ou versão incorreta

**Solução:**
```bash
# 1. Verificar acesso ao repositório
curl -I https://repo1.maven.org/maven2/

# 2. Verificar versão no gradle.properties
cat gradle.properties | grep quarkusPlatformVersion

# 3. Tentar download manual
./gradlew build --refresh-dependencies
```

### Problema 2: "At least one source directory should contain sources"

**Causa:** Sem nenhuma classe Java em src/main/java

**Solução:** Criar pelo menos uma classe Java (exemplo: GreetingResource.java)

### Problema 3: Gradle Wrapper não funciona

**Causa:** gradle-wrapper.jar não baixado ou corrompido

**Solução:**
```bash
# Baixar novamente
wget https://github.com/gradle/gradle/raw/v9.0.0/gradle/wrapper/gradle-wrapper.jar -O gradle/wrapper/gradle-wrapper.jar

# Ou usar Gradle instalado
gradle wrapper --gradle-version 9.0
```

### Problema 4: Proxy bloqueia downloads

**Causa:** Configuração de proxy incorreta

**Solução:** Verificar proxy em `gradle.properties` e testar:
```bash
# Testar acesso via curl
curl -x http://proxy.empresa.com:8080 https://repo1.maven.org/maven2/

# Definir proxy via linha de comando
./gradlew build -Dhttp.proxyHost=proxy.empresa.com -Dhttp.proxyPort=8080
```

### Problema 5: Versão LTS não encontrada no repositório corporativo

**Causa:** Repositório corporativo desatualizado

**Solução:**
1. Solicitar ao time de DevOps para sincronizar com Maven Central
2. Usar versão anterior disponível
3. Como último recurso, usar arquivo `.jar` local via `mavenLocal()`

---

## 📚 Recursos Offline

Se você precisa trabalhar completamente offline:

### 1. Cachear Dependências Localmente

```bash
# Primeiro download (online)
./gradlew build --refresh-dependencies

# Gradle armazena em:
ls ~/.gradle/caches/modules-2/files-2.1/io.quarkus/
```

### 2. Criar Repositório Local

```bash
# Copiar dependências para repositório local
./gradlew build --offline

# Ou criar um maven-local.zip com todas as dependências
# e distribuir para time offline
```

### 3. Documentação Offline

Baixe a documentação do Quarkus:
```bash
# Clone do repositório (se tiver Git access)
git clone https://github.com/quarkusio/quarkus.git
cd quarkus/docs
```

Ou baixe versão zipada das releases.

---

## ✅ Checklist Final

Antes de começar a desenvolver:

- [ ] Java 21 instalado e configurado
- [ ] Gradle 9.0 funcionando (wrapper ou instalado)
- [ ] Projeto base criado e compila (`./gradlew build`)
- [ ] Servidor dev mode funciona (`./gradlew quarkusDev`)
- [ ] Endpoint /hello responde
- [ ] Testes passam (`./gradlew test`)
- [ ] Acesso ao repositório Maven (Central ou corporativo) confirmado
- [ ] Proxy configurado (se necessário)
- [ ] Extensões necessárias adicionadas
- [ ] `.gitignore` configurado
- [ ] README.md do projeto criado

---

## 🎯 Próximos Passos

Agora que você tem um projeto Quarkus base funcionando:

1. ✅ Adicione suas extensões necessárias (OpenAPI, MapStruct, Database)
2. ✅ Crie a especificação OpenAPI em `src/main/resources/openapi/openapi.yaml`
3. ✅ Configure o OpenAPI Generator
4. ✅ Implemente as entidades de domínio
5. ✅ Crie os mappers com MapStruct
6. ✅ Implemente os services
7. ✅ Implemente as APIs (implementação das interfaces geradas)
8. ✅ Configure o banco de dados
9. ✅ Adicione testes

**Referências:**
- [PROJECT_GUIDE.md](PROJECT_GUIDE.md) - Guia completo do projeto
- [README.md](README.md) - Quick start e exemplos
- [DEBUG_GUIDE.md](DEBUG_GUIDE.md) - Debugging e troubleshooting

---

**Criado por:** Claude Code
**Última atualização:** 2025-11-04
