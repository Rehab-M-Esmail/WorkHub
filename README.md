# WorkHub
Engineering and Operating a **multi-tenant SaaS backend** built with Spring Boot.

## ⚙️ Prerequisites

Make sure you have the following installed:

- Java 21
- Maven
- Git


## 📥 1. Clone the Repository

```bash
git clone https://github.com/Rehab-M-Esmail/workhub.git
cd workhub
```

## 2. Install Dependencies
```bash
  mvn clean install
```

## ▶️ 3. Run the Application
```bash
mvn spring-boot:run
```
## 4. Access The Application
http://localhost:8080

## Run with Docker (App + Kafka)

This starts:
- `workhub-app` (Spring Boot API on `http://localhost:8080`)
- `workhub-redpanda` (Kafka-compatible broker on `localhost:9092`)

```bash
docker compose up --build
```

Then open:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
