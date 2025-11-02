# Comparação de Tecnologias - Guia de Referência

## Maven vs Gradle

### Maven
- **Arquivo de configuração**: `pom.xml` (XML)
- **Estrutura**: Declarativa, mais verbosa
- **Convenções**: Fortemente baseado em convenções
- **Ciclo de vida**: Fases bem definidas (compile, test, package, install, deploy)
- **Sintaxe de comandos**: `mvn goal:task` ou `mvn fase`
- **Gerenciamento de dependências**: Via tags XML `<dependencies>`
- **Performance**: Builds podem ser mais lentos em projetos grandes
- **Curva de aprendizado**: Mais simples para iniciantes
- **Adoção**: Mais tradicional, amplamente usado em empresas

**Exemplo de comando:**
```bash
./mvnw clean install
./mvnw quarkus:dev
```

### Gradle
- **Arquivo de configuração**: `build.gradle` (Groovy ou Kotlin DSL)
- **Estrutura**: Programática, mais concisa
- **Convenções**: Flexível, permite customizações complexas
- **Ciclo de vida**: Tasks personalizáveis
- **Sintaxe de comandos**: `gradle taskName`
- **Gerenciamento de dependências**: Via DSL Groovy/Kotlin
- **Performance**: Builds incrementais, cache, melhor performance
- **Curva de aprendizado**: Mais complexo inicialmente
- **Adoção**: Crescente, especialmente em projetos Android

**Exemplo de comando:**
```bash
./gradlew clean build
./gradlew quarkusDev
```

### Principais Diferenças na Prática

| Aspecto | Maven | Gradle |
|---------|-------|--------|
| Formato | XML | Groovy/Kotlin DSL |
| Verbosidade | Alta | Baixa |
| Performance | Moderada | Alta (cache, incremental) |
| Flexibilidade | Limitada | Alta |
| Popularidade Enterprise | Muito alta | Crescente |

---

## Spring Boot vs Quarkus

### Spring Boot
- **Filosofia**: Framework tradicional Java, maduro e completo
- **Startup time**: Mais lento (segundos)
- **Footprint de memória**: Maior consumo
- **Reflection**: Uso intensivo em runtime
- **Compilação**: JIT (Just-In-Time)
- **Cloud Native**: Suporte, mas não foi projetado para isso
- **Ecossistema**: Muito maduro, vasta biblioteca de integrações
- **Developer Experience**: Excelente, muita documentação
- **Adoção**: Dominante no mercado empresarial
- **Ideal para**: Aplicações tradicionais, monólitos, microservices convencionais

**Características:**
- Auto-configuração extensiva
- Grande quantidade de starters
- Spring Data, Spring Security, Spring Cloud
- Suporte robusto para bancos de dados
- Curva de aprendizado moderada

### Quarkus
- **Filosofia**: Cloud-native, container-first, otimizado para Kubernetes
- **Startup time**: Muito rápido (milissegundos)
- **Footprint de memória**: Muito menor
- **Reflection**: Mínimo, processado em build time
- **Compilação**: AOT (Ahead-Of-Time) com GraalVM native
- **Cloud Native**: Projetado especificamente para cloud
- **Ecossistema**: Em crescimento, focado em padrões Jakarta EE
- **Developer Experience**: Excelente, dev mode com live reload
- **Adoção**: Crescente em novos projetos cloud-native
- **Ideal para**: Microservices, serverless, containers, Kubernetes

**Características:**
- Build time processing (otimizações em tempo de compilação)
- Dev mode com live reload automático
- Suporte nativo a GraalVM (native executables)
- Suporte a reactive e imperative
- Foco em padrões (Jakarta EE, MicroProfile)
- Panache para simplificação de Hibernate

### Comparação Técnica

| Aspecto | Spring Boot | Quarkus |
|---------|-------------|---------|
| Startup (JVM) | 3-5s | 0.5-1s |
| Startup (Native) | N/A | 0.01-0.05s |
| Memória (JVM) | 200-500 MB | 100-200 MB |
| Memória (Native) | N/A | 20-50 MB |
| Hot Reload | DevTools (restart) | Live reload (sem restart) |
| Padrões | Spring próprio | Jakarta EE, MicroProfile |
| Reactive | WebFlux | Mutiny, Vert.x |
| Maturidade | Muito alta | Moderada |

### Quando escolher cada um?

**Spring Boot:**
- Projetos enterprise tradicionais
- Equipe já familiarizada com Spring
- Necessidade de integrações complexas já prontas
- Aplicações que não rodam em containers
- Projetos onde startup time não é crítico

**Quarkus:**
- Microservices em Kubernetes
- Aplicações serverless (AWS Lambda, etc)
- Projetos que exigem otimização de recursos
- Necessidade de startup extremamente rápido
- Aplicações cloud-native desde o início
- Quando você quer executáveis nativos

---

## Nuances Importantes

### 1. Comandos Maven vs Gradle no Quarkus

**Maven** usa dois-pontos:
```bash
./mvnw quarkus:dev
```

**Gradle** usa camelCase:
```bash
./gradlew quarkusDev
```

### 2. Comportamento de Dev Mode

**Maven (`mvn quarkus:dev`)**
- ✅ Permite iniciar sem código fonte
- Inicia normalmente mesmo com `src/main/java` vazio
- Você pode começar a codificar com hot reload

**Gradle (`./gradlew quarkusDev`)**
- ❌ Exige pelo menos uma classe Java para iniciar
- Erro: "At least one source directory should contain sources before starting Quarkus in dev mode when using Gradle"
- Validação mais rigorosa antes de iniciar

**Solução para Gradle:**
Sempre criar pelo menos uma classe antes de rodar `quarkusDev`, ou gerar o projeto com código de exemplo incluído.

---

## Referências

- [Maven Documentation](https://maven.apache.org/)
- [Gradle Documentation](https://docs.gradle.org/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Quarkus Documentation](https://quarkus.io/)
- [Quarkus vs Spring Boot Comparison](https://quarkus.io/blog/quarkus-vs-spring/)
