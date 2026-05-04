# Kafka Async Report Workflow Implementation Report

## Overview

This feature introduces an asynchronous report workflow backed by Kafka-compatible messaging.
Clients enqueue report jobs through an API, and a background consumer processes them with at-least-once delivery guarantees.

## Implemented Scope

### 1. API Endpoints

- `POST /reports`
  - Creates a new report job in `QUEUED` state.
  - Publishes a `ReportRequestedEvent` to Kafka.
- `GET /reports/{jobId}/status`
  - Returns the current job lifecycle state (`QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`).

## 2. Messaging and Reliability

- Added Kafka producer via `ReportEventProducer`.
- Added Kafka consumer via `ReportRequestedConsumer`.
- Implemented at-least-once safety with `processed_messages` dedup table:
  - Uniqueness on `(message_id, consumer_name)`.
  - Duplicate messages are safely ignored.

## 3. Persistence Model

- `ReportJob` entity:
  - `id`, `reportType`, `tenantId`, `status`
  - `resultMessage`, `failureReason`
  - `createdAt`, `startedAt`, `completedAt`
- `ProcessedMessage` entity:
  - `messageId`, `consumerName`, `processedAt`

## 4. Configuration

- Added Kafka app settings in `application.properties`:
  - Bootstrap servers, serializers/deserializers, consumer group, listener ack mode.
  - Topic name: `workhub.report.requested.v1`
- Added explicit producer bean wiring in `KafkaProducerConfig`:
  - `ProducerFactory<String, ReportRequestedEvent>`
  - `KafkaTemplate<String, ReportRequestedEvent>`

## 5. Demo Assets

- Postman collection updated with `Report Async Workflow` folder:
  - Enqueue request
  - Status polling request
- Added script demos:
  - `scripts/demo/report_async_demo.ps1`
  - `scripts/demo/report_async_demo.sh`

## 6. Docker Support

- Added root `Dockerfile` (Java 21 build + runtime).
- Added root `docker-compose.yml`:
  - `workhub-app`
  - `workhub-redpanda` (Kafka-compatible broker)
- App is wired through:
  - `KAFKA_BOOTSTRAP_SERVERS=redpanda:9092`

## 7. Run and Test

### Start

```bash
docker compose up --build
```

### Test flow

1. Create tenant (if needed): `POST /tenant`
2. Enqueue report: `POST /reports`
3. Poll status: `GET /reports/{jobId}/status` until `COMPLETED` or `FAILED`

## Notes

- Local Maven test execution currently depends on Java 21 toolchain availability.
- During development, build output under `target/` changed; these artifacts are not part of the feature source deliverable.
