# Banking Service - Quarkus

API RESTful para gerenciamento de agências bancárias, construída com Quarkus, OpenAPI Generator e MapStruct.

Este projeto usa Quarkus, o framework Java supersônico e subatômico.

## 🚀 Quick Start

```bash
# Executar em modo dev
./gradlew quarkusDev

# Acessar Swagger UI
open http://localhost:8080/q/swagger-ui
```

## 📋 Pré-requisitos

- Java 21
- Gradle 9.0+

## 🛠️ Tecnologias

- **Quarkus 3.27.0** - Framework principal
- **OpenAPI Generator 7.10.0** - Geração automática de código da API
- **MapStruct 1.6.3** - Mapeamento entre DTOs e Entidades
- **JAX-RS** - Especificação REST do Jakarta EE
- **SmallRye OpenAPI** - Swagger UI integrado

## 📚 Endpoints Disponíveis

### Agências

- `GET /api/v1/agencias` - Listar todas as agências
- `GET /api/v1/agencias/{id}` - Buscar agência por ID
- `POST /api/v1/agencias` - Criar nova agência
- `PUT /api/v1/agencias/{id}` - Atualizar agência
- `DELETE /api/v1/agencias/{id}` - Deletar agência

### Contas

- `GET /api/v1/contas` - Listar todas as contas
- `POST /api/v1/contas` - Criar nova conta

## 🌐 URLs Úteis

| Recurso | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/q/swagger-ui |
| **OpenAPI Spec** | http://localhost:8080/q/openapi |
| **Dev UI** | http://localhost:8080/q/dev |
| **Health Check** | http://localhost:8080/q/health |

## 📖 Exemplo de Uso

**Criar uma agência:**

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

## 🏗️ Arquitetura

Este projeto segue o padrão **API-First** com geração automática de código:

```
OpenAPI Spec (openapi.yaml) → OpenAPI Generator → Interfaces JAX-RS + DTOs
                                                           ↓
                              AgenciaApiImpl → AgenciaMapper → AgenciaService
```

**Documentação Completa**: Veja [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md) para:
- Comparação detalhada Spring Boot vs Quarkus
- Configuração do OpenAPI Generator e MapStruct
- Exemplos de código e best practices
- Guia de produção e troubleshooting

---

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/banking-service-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- Micrometer metrics ([guide](https://quarkus.io/guides/micrometer)): Instrument the runtime and your application with dimensional metrics using Micrometer.
