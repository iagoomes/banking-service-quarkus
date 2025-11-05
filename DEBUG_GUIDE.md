# Guia Completo de Debug - Quarkus com Gradle

> Passo a passo para debugar aplicações Quarkus na IDE

---

## Índice
- [Visão Geral](#visão-geral)
- [Método 1: Debug via Gradle quarkusDev (Recomendado)](#método-1-debug-via-gradle-quarkusdev-recomendado)
- [Método 2: Debug direto pela IDE](#método-2-debug-direto-pela-ide)
- [Desabilitar Dev Services (PostgreSQL automático)](#desabilitar-dev-services-postgresql-automático)
- [Troubleshooting](#troubleshooting)

---

## Visão Geral

Quando você executa `./gradlew quarkusDev`, o Quarkus:

1. ✅ Inicia a aplicação em **modo JVM** (não em container)
2. ✅ Habilita **debug automático na porta 5005**
3. ✅ Ativa **live reload** (hot reload de código)
4. ✅ Inicia **Dev Services** (containers automáticos para banco de dados, Kafka, etc.)

**Importante:** Sua aplicação **NÃO roda em container**. Apenas os serviços auxiliares (PostgreSQL, etc.) rodam em containers via Docker.

---

## Método 1: Debug via Gradle quarkusDev (Recomendado)

Este é o método mais simples e mantém todos os recursos do Quarkus (live reload, dev UI, etc.).

### Passo 1: Iniciar o Quarkus em Dev Mode

```bash
./gradlew quarkusDev
```

Você verá algo como:
```
Listening for transport dt_socket at address: 5005
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
banking-service-quarkus 1.0.0-SNAPSHOT on JVM started in 2.615s.
Listening on: http://localhost:8080
```

**Porta 5005** = Debug habilitado ✅

### Passo 2: Conectar a IDE ao Debugger

#### **IntelliJ IDEA**

**Opção A: Attach to Process (Mais rápido)**

1. Certifique-se que `./gradlew quarkusDev` está rodando
2. Menu: **Run → Attach to Process...** (ou `⌥⇧F5` no Mac / `Ctrl+Alt+F5` no Windows)
3. Digite "quarkus" ou "java" para filtrar
4. Selecione o processo do Quarkus (porta 5005)
5. Coloque breakpoints no código
6. Faça uma requisição (curl, Postman, etc.)
7. A execução vai parar nos breakpoints! 🎉

**Opção B: Remote JVM Debug (Configuração reutilizável)**

1. Menu: **Run → Edit Configurations...**
2. Clique em **+** (Add New Configuration)
3. Selecione **Remote JVM Debug**
4. Configure:
   - **Name**: `Debug Quarkus Dev Mode`
   - **Debugger mode**: `Attach to remote JVM`
   - **Host**: `localhost`
   - **Port**: `5005`
   - **Use module classpath**: Selecione seu módulo
5. Clique **Apply** e **OK**
6. Coloque breakpoints no código
7. Clique no ícone de Debug (🐛) e selecione "Debug Quarkus Dev Mode"
8. Faça uma requisição
9. A execução vai parar nos breakpoints! 🎉

#### **VS Code**

1. Crie o arquivo `.vscode/launch.json` (se não existir):

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Quarkus (Attach)",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

2. Certifique-se que `./gradlew quarkusDev` está rodando
3. Coloque breakpoints no código
4. Pressione **F5** ou **Run → Start Debugging**
5. Selecione "Debug Quarkus (Attach)"
6. Faça uma requisição
7. A execução vai parar nos breakpoints! 🎉

#### **Eclipse**

1. Certifique-se que `./gradlew quarkusDev` está rodando
2. Menu: **Run → Debug Configurations...**
3. Clique com botão direito em **Remote Java Application** → **New Configuration**
4. Configure:
   - **Name**: `Debug Quarkus`
   - **Project**: Selecione seu projeto
   - **Connection Type**: Standard (Socket Attach)
   - **Host**: `localhost`
   - **Port**: `5005`
5. Clique **Apply** e **Debug**
6. Coloque breakpoints e faça requisições

---

## Método 2: Debug direto pela IDE

Este método inicia o Quarkus diretamente pela IDE (sem Gradle), mas você perde algumas funcionalidades.

### IntelliJ IDEA

1. Abra a classe principal ou qualquer recurso REST
2. Clique com botão direito no código
3. Selecione **Debug 'QuarkusApplication'** ou **Run 'QuarkusApplication'** com Debug
4. Ou crie uma configuração **Quarkus** no menu **Run → Edit Configurations**

**Limitações:**
- ⚠️ Pode não ter live reload completo
- ⚠️ Dev Services podem não iniciar corretamente

### VS Code

1. Instale a extensão **Quarkus Tools for Visual Studio Code**
2. Pressione `Ctrl+Shift+P` (ou `Cmd+Shift+P` no Mac)
3. Digite: "Quarkus: Debug current Quarkus project"
4. Selecione o modo de debug

---

## Desabilitar Dev Services (PostgreSQL automático)

Se você NÃO quer que o Quarkus crie containers automáticos:

### Opção 1: Desabilitar apenas para PostgreSQL

Edite `src/main/resources/application.properties`:

```properties
# Desabilita apenas PostgreSQL Dev Services
quarkus.datasource.devservices.enabled=false

# Configure o banco manualmente
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/banking
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
```

### Opção 2: Desabilitar todos os Dev Services

```properties
# Desabilita TODOS os Dev Services
quarkus.devservices.enabled=false
```

### Opção 3: Usar banco H2 em memória (apenas para testes)

```properties
# Use H2 ao invés de PostgreSQL
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb
quarkus.hibernate-orm.database.generation=drop-and-create
```

E adicione a dependência no `build.gradle`:
```gradle
runtimeOnly 'io.quarkus:quarkus-jdbc-h2'
```

---

## Configurações Avançadas

### Mudar a porta de debug

Edite `application.properties`:
```properties
# Mudar porta de debug para 5006
quarkus.debug.port=5006
```

Ou via linha de comando:
```bash
./gradlew quarkusDev -Ddebug=5006
```

### Suspender na inicialização (aguardar debugger)

```bash
# A aplicação vai AGUARDAR você conectar o debugger antes de iniciar
./gradlew quarkusDev -Dsuspend=y
```

Útil para debugar código de inicialização.

### Desabilitar live reload

```properties
quarkus.live-reload.instrumentation=false
```

---

## Troubleshooting

### 1. Breakpoints não param a execução

**Causa:** IDE não está conectada ao debugger na porta 5005

**Solução:**
```bash
# Verifique se o Quarkus está rodando com debug habilitado
./gradlew quarkusDev

# Procure por esta linha no log:
# "Listening for transport dt_socket at address: 5005"

# Conecte a IDE na porta 5005 (veja Passo 2 acima)
```

### 2. Porta 5005 já está em uso

**Causa:** Outro processo está usando a porta de debug

**Solução:**
```bash
# Encontre o processo
lsof -i :5005

# Mate o processo (substitua PID)
kill -9 <PID>

# Ou mude a porta de debug
./gradlew quarkusDev -Ddebug=5006
```

### 3. Containers do Testcontainers não param

**Causa:** Ryuk (cleanup automático) não está funcionando

**Solução:**
```bash
# Liste containers do Testcontainers
docker ps -a | grep testcontainers

# Pare e remova manualmente
docker stop $(docker ps -q --filter "label=org.testcontainers=true")
docker rm $(docker ps -aq --filter "label=org.testcontainers=true")
```

### 4. Live reload não está funcionando

**Causa:** Mudanças não estão sendo compiladas

**Solução:**
```bash
# Certifique-se que está usando quarkusDev (não apenas 'run')
./gradlew quarkusDev

# Se ainda não funcionar, limpe e reinicie
./gradlew clean
./gradlew quarkusDev
```

### 5. IDE não encontra código-fonte do MapStruct

**Causa:** MapStruct gera código em build time

**Solução:**
```bash
# Recompile o projeto
./gradlew clean build

# Ou reinicie o Quarkus dev mode
# Ctrl+C para parar
./gradlew quarkusDev
```

### 6. Erro "Address already in use" (porta 8080)

**Causa:** Outra aplicação está usando a porta 8080

**Solução:**
```bash
# Encontre o processo
lsof -i :8080

# Mate o processo
kill -9 <PID>

# Ou mude a porta da aplicação
./gradlew quarkusDev -Dquarkus.http.port=8081
```

---

## Verificação Rápida

Para confirmar que o debug está funcionando:

### 1. Inicie o Quarkus
```bash
./gradlew quarkusDev
```

### 2. Verifique o log
Procure por:
```
Listening for transport dt_socket at address: 5005
```

### 3. Teste a aplicação
```bash
curl http://localhost:8080/q/health
```

### 4. Coloque um breakpoint
Coloque um breakpoint em qualquer endpoint REST

### 5. Conecte o debugger
Use **Run → Attach to Process** (IntelliJ) ou **F5** (VS Code)

### 6. Faça uma requisição
```bash
curl -X POST http://localhost:8080/agencia \
  -H "Content-Type: application/json" \
  -d '{"nome":"Test","cnpj":"15130254000100"}'
```

### 7. Breakpoint deve parar! ✅

---

## Comandos Úteis

```bash
# Iniciar em dev mode
./gradlew quarkusDev

# Iniciar em dev mode com porta customizada
./gradlew quarkusDev -Dquarkus.http.port=8081

# Iniciar e aguardar debugger conectar
./gradlew quarkusDev -Dsuspend=y

# Build limpo
./gradlew clean build

# Verificar containers do Testcontainers
docker ps --filter "label=org.testcontainers=true"

# Parar todos containers do Testcontainers
docker stop $(docker ps -q --filter "label=org.testcontainers=true")

# Ver processos Java rodando
jps -v | grep quarkus

# Verificar portas em uso
lsof -i :8080  # Aplicação
lsof -i :5005  # Debug
lsof -i :5432  # PostgreSQL
```

---

## Diferenças: Spring Boot vs Quarkus Debug

| Aspecto | Spring Boot DevTools | Quarkus Dev Mode |
|---------|---------------------|------------------|
| Hot Reload | Restart automático | Live reload (sem restart) |
| Debug | Precisa configurar | Habilitado por padrão (porta 5005) |
| Porta Debug | Customizável | 5005 (padrão) |
| Containers | Manual (Docker Compose) | Automático (Dev Services) |
| Comando | `./mvnw spring-boot:run` | `./gradlew quarkusDev` |
| Velocidade | Restart ~3-5s | Reload ~0.5s |

---

## Recursos Adicionais

- [Quarkus Dev UI](http://localhost:8080/q/dev/) - Console visual em dev mode
- [Quarkus Debug Guide](https://quarkus.io/guides/maven-tooling#debugging)
- [Dev Services](https://quarkus.io/guides/dev-services)
- [Continuous Testing](https://quarkus.io/guides/continuous-testing)

---

**Última atualização**: 2025-11-02