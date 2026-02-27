# Quality Evaluator API

API REST para **avaliação de qualidade de projetos de software**, com base em métricas técnicas, regras de negócio e critérios de classificação.  

O sistema permite criar avaliações de projetos, filtrar resultados, exportar dados em CSV e gerar métricas de observabilidade.

---

## 📌 Funcionalidades

- Criar avaliações de projetos com pontuação automática baseada em:
  - Linguagem de programação
  - Linhas de código
  - Complexidade
  - Presença de testes
  - Uso de Git
- Filtrar avaliações por:
  - Nome do projeto
  - Linguagem
  - Score mínimo e máximo
  - Classificação
  - Período de criação
- Exportar avaliações filtradas para **CSV**
- Métricas e observabilidade via **Spring Boot Actuator e Micrometer**
- Endpoints de Health e Info para monitoramento
- Documentação automática via **OpenAPI / Swagger**
- Autenticação com **JWT**
- Controle de acesso baseado em **roles** 
- Dashboard analítico com **métricas agregadas**
- Cache com **Spring Cache**

---

## 🛠 Tecnologias

- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Boot Validation
- Spring Boot Actuator
- Springdoc OpenAPI
- H2 Database (em memória)
- Logstash Logback Encoder
- Maven
- Spring Security
- JWT Authentication
- Spring Cache
  
---

## 🚀 Rodando o projeto localmente

### Pré-requisitos

- Java 17 instalado
- Maven 3.8+ instalado

### Passos

1. Clone o repositório:

```bash
git clone https://github.com/marceloscoleso/quality-evaluator-api.git
cd quality-evaluator-api
```

2. Execute o projeto:

```bash
mvn spring-boot:run
```

3. A API estará disponível em:

```arduino
http://localhost:8080
```

4. Documentação Swagger (OpenAPI UI):

```arduino
http://localhost:8080/swagger-ui/index.html
```

---

## 📄 Endpoints principais

### Criar avaliação

```bash
POST /api/evaluations
Content-Type: application/json
```

### Request Body Exemplo:

```json
{
  "projectName": "Quality Evaluator API",
  "language": "JAVA",
  "linesOfCode": 250,
  "complexity": 2,
  "hasTests": true,
  "usesGit": true,
  "analyzedBy": "Marcelo"
}
```

### Response esperado:

```json
{
  "id": 1,
  "projectName": "Quality Evaluator API",
  "language": "JAVA",
  "score": 90,
  "classification": "EXCELENTE",
  "analyzedBy": "Marcelo",
  "createdAt": "2026-02-10T05:55:44.409"
}
```

### Listar todas as avaliações

```http
GET /api/evaluations?page=0&size=10&sort=createdAt,desc
```

### Buscar avaliação por ID

```http
GET /api/evaluations/{id}
```

### Filtrar avaliações

```http
GET /api/evaluations/filter?projectName=quality&language=JAVA&minScore=60&maxScore=90&classification=BOM&startDate=2024-01-01&endDate=2024-12-31
```

**Filtros Disponíveis:**

- **projectName** → nome do projeto (parcial)
- **language** → linguagem do projeto (JAVA, CSHARP, JAVASCRIPT, etc.)
- **minScore e maxScore** → intervalo de pontuação
- **classification** → EXCELENTE, BOM, REGULAR, RUIM
- **startDate e endDate** → período de criação (yyyy-MM-dd)

### Exportar avaliações em CSV

```http
GET /api/evaluations/export/csv?projectName=quality&language=JAVA&minScore=60&maxScore=90&classification=BOM&startDate=2024-01-01&endDate=2024-12-31
```

**Resposta:** arquivo CSV com as avaliações filtradas.

**Colunas:** Projeto,Linguagem,Nota,Classificacao,Data

---

## ⚙️ Modelo de dados

### Linguagens suportadas

- JAVA, CSHARP, JAVASCRIPT, TYPESCRIPT, PYTHON, KOTLIN, GO, PHP, RUBY, SWIFT, C, CPP, RUST, DART, OTHER

### Classificações possíveis

- EXCELENTE
- BOM
- REGULAR
- RUIM

---

## 📊 Health Check e Métricas

### Endpoints do Spring Actuator

- **Health Check:** GET /actuator/health
  
  Retorna status de saúde da aplicação.
  
- **Info:** GET /actuator/info
  
  Informações do projeto, versão, autor, etc.

- **Métricas:** GET /actuator/metrics
  
  Métricas de performance, contadores e timers (integrado com Micrometer).

### Endpoints amigáveis para humanos

- **Health Check:** GET /health
  
  Página web simples mostrando o status da aplicação,
  
- **Info:** GET /info
  
  Informações do projeto, versão, autor, etc.
  
- **Métricas:** GET /metrics
  
  Página web mostrando métricas de performance em formato legível.
  
---

## 🏗 Arquitetura

A aplicação segue arquitetura em camadas:

- Controller → Camada de entrada HTTP
- Service → Regras de negócio
- Repository → Persistência com JPA
- DTO → Transferência de dados
- Security → Autenticação e autorização JWT
- Monitoring → Actuator + métricas customizadas

A separação de responsabilidades garante:
- Manutenibilidade
- Testabilidade
- Escalabilidade

  ---

## 🔒 Segurança

- Senhas criptografadas com BCrypt
- Autenticação baseada em JWT
- Controle de acesso via @PreAuthorize
- Proteção por usuário autenticado (isolamento de dados por usuário)

  ---

## 📂 Estrutura do projeto

```css
src/
├─ main/
│  ├─ java/
│  │  └─ br/com/marceloscoleso/quality_evaluator_api/
│  │      ├─ controller/
│  │      ├─ dto/
│  │      ├─ exception/
│  │      ├─ model/
│  │      ├─ repository/
│  │      ├─ service/
│  │      └─ util/
│  └─ resources/
│      └─ application.properties
```

---

## ⚖️ Licença

Este projeto está sob a MIT License.

**Desenvolvedor:** Marcelo Scoleso

**GitHub:** https://github.com/marceloscoleso


