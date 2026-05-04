# Observability Checklist

## 1. Application Metrics

- [ ] Screenshot of a metrics query in Prometheus showing `http_server_requests_seconds_count` or similar
      ![Requests per second count](./screenshots/Screenshot%202026-05-04%20at%2011.33.37%E2%80%AFAM.png)

## 2. Health Endpoints

- [ ] Screenshot of `http://localhost:8080/actuator/health`
      ![health](./screenshots/Screenshot%202026-05-04%20at%2011.20.41%E2%80%AFAM.png)
- [ ] Screenshot of `http://localhost:8080/actuator/health/liveness`
      ![Liveness](./screenshots/Screenshot%202026-05-04%20at%2011.25.02%E2%80%AFAM.png)
- [ ] Screenshot of `http://localhost:8080/actuator/health/readiness`
      ![Readiness](./screenshots/Screenshot%202026-05-04%20at%2011.25.19%E2%80%AFAM.png)

## 3. Prometheus Configuration

- [ ] Screenshot of Prometheus targets at `http://localhost:9090/targets`
      ![Targets](./screenshots/Screenshot%202026-05-04%20at%2011.25.44%E2%80%AFAM.png)

## 4. Grafana Dashboard

- [ ] Screenshot of Grafana dashboard with visible metrics
      ![Grafana](./screenshots/Screenshot%202026-05-04%20at%2011.31.58%E2%80%AFAM.png)

## 5. Logs and Correlation ID

- [ ] Screenshot of request/response log lines including trace IDs
      ![Trace ID](./screenshots/Screenshot%202026-05-04%20at%2011.29.32%E2%80%AFAM.png)
