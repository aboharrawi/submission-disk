# Submission Disk - Kafka Pipeline Architecture

## Overview

This application implements a microservices-based submission processing system using Kafka as the event-driven backbone. Each pipeline stage is independent and processes submissions asynchronously.

## Architecture

### Technology Stack
- **Spring Boot 4.0.2** - Application framework
- **Apache Kafka** - Event streaming platform
- **PostgreSQL** - Relational database
- **Redis** - Caching layer
- **Docker Compose** - Container orchestration

### Pipeline Stages

```
┌─────────────┐     ┌────────────┐     ┌─────────┐     ┌────────────┐     ┌──────────────┐
│  Ingestion  │────▶│ Validation │────▶│ Storage │────▶│ Processing │────▶│ Notification │
└─────────────┘     └────────────┘     └─────────┘     └────────────┘     └──────────────┘
                            │                  │              │                    │
                            ▼                  ▼              ▼                    ▼
                      ┌──────────┐       ┌──────────┐  ┌──────────┐        ┌──────────┐
                      │  Failed  │       │  Failed  │  │  Failed  │        │Completed │
                      └──────────┘       └──────────┘  └──────────┘        └──────────┘
```

### Kafka Topics

| Topic | Purpose | Partitions | Producer | Consumer |
|-------|---------|------------|----------|----------|
| `submission.received` | Initial submission events | 3 | SubmissionService | - |
| `submission.validation` | Validation pipeline | 3 | SubmissionService | ValidationPipeline |
| `submission.storage` | Storage pipeline | 3 | ValidationPipeline | StoragePipeline |
| `submission.processing` | Processing pipeline | 3 | StoragePipeline | ProcessingPipeline |
| `submission.notification` | Notification events | 3 | ProcessingPipeline | NotificationPipeline |
| `submission.completed` | Completed submissions | 3 | ProcessingPipeline | NotificationPipeline |
| `submission.failed` | Failed submissions | 3 | All Pipelines | NotificationPipeline |

## Pipeline Components

### 1. Ingestion Pipeline (SubmissionService)
**Responsibility**: Accept file uploads and create submission records

**Process**:
- Validates ZIP file format
- Calculates file checksum
- Stores file to disk
- Creates database record with PENDING status
- Publishes event to `submission.validation` topic

**Event Published**:
```json
{
  "submissionId": 1,
  "fileName": "submission.zip",
  "fileSize": 1048576,
  "checksum": "abc123...",
  "status": "PENDING",
  "currentStage": "RECEIVED",
  "nextStage": "VALIDATION"
}
```

### 2. Validation Pipeline
**Responsibility**: Validate submissions and check for duplicates

**Process**:
- Checks for duplicate submissions by checksum
- Validates file integrity
- Updates status to VALIDATED or FAILED
- Publishes event to `submission.storage` or `submission.failed`

**Validation Rules**:
- No duplicate files (by checksum)
- File must be accessible
- File size within limits

### 3. Storage Pipeline
**Responsibility**: Manage file storage and backups

**Process**:
- Confirms file storage
- Can be extended for:
  - Cloud storage (S3, MinIO)
  - Backup creation
  - Metadata extraction
- Updates status to STORED
- Publishes event to `submission.processing`

**Extensibility Points**:
```java
// Move to cloud storage
cloudStorageService.upload(event.getStoragePath());

// Create backup
backupService.backup(event.getStoragePath());

// Extract metadata
metadataService.extract(event.getStoragePath());
```

### 4. Processing Pipeline
**Responsibility**: Process submission contents

**Process**:
- Extracts and analyzes file contents
- Performs business logic processing
- Can be extended for:
  - Unzipping archives
  - Running security scans
  - Validating file structure
  - Running automated tests
- Updates status to COMPLETED or FAILED
- Publishes events to `submission.notification` and `submission.completed`

**Processing Extensions**:
```java
// Unzip and analyze
fileAnalyzer.analyze(event.getStoragePath());

// Run security scan
securityScanner.scan(event.getStoragePath());

// Validate structure
structureValidator.validate(event.getStoragePath());
```

### 5. Notification Pipeline
**Responsibility**: Send notifications and alerts

**Process**:
- Sends notifications for all pipeline events
- Handles completion notifications
- Handles failure alerts
- Can be extended for:
  - Email notifications
  - Webhook callbacks
  - Slack/Teams integration
  - Mobile push notifications

**Notification Channels**:
```java
// Email notification
emailService.send(event.getSubmittedBy(), "Submission Complete");

// Webhook callback
webhookService.notify(event.getSubmissionId(), event.getStatus());

// Slack notification
slackService.sendMessage(channel, "Submission #" + event.getSubmissionId());
```

## Submission Status Flow

```
PENDING → VALIDATED → STORED → PROCESSING → COMPLETED
    ↓         ↓          ↓           ↓
  FAILED    FAILED    FAILED      FAILED
```

## Configuration

### Application Properties

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.consumer.group-id=submission-disk-group
spring.kafka.consumer.auto-offset-reset=earliest

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/submissiondb
spring.datasource.username=admin
spring.datasource.password=admin123

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# File Upload Configuration
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
submission.storage.path=uploads
```

## Running the Application

### 1. Start Infrastructure with Docker Compose

```bash
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Zookeeper (port 2181)
- Kafka (ports 9092, 29092)

### 2. Build the Application

```bash
./gradlew clean build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

Or run the JAR:
```bash
java -jar build/libs/submission-disk-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Submit a File
```bash
POST /api/submissions
Content-Type: multipart/form-data

Parameters:
- file: ZIP file (required)
- description: string (optional)
- submittedBy: string (required)

Response:
{
  "id": 1,
  "fileName": "submission.zip",
  "fileSize": 1048576,
  "status": "PENDING",
  "submittedAt": "2026-02-15T10:30:00",
  "submittedBy": "user@example.com"
}
```

### Get Submission Status
```bash
GET /api/submissions/{id}

Response:
{
  "id": 1,
  "fileName": "submission.zip",
  "status": "COMPLETED",
  "submittedAt": "2026-02-15T10:30:00",
  "processedAt": "2026-02-15T10:30:15"
}
```

### List All Submissions
```bash
GET /api/submissions

Response: Array of submission objects
```

### Get Submissions by Status
```bash
GET /api/submissions/status/{status}

Statuses: PENDING, VALIDATED, STORED, PROCESSING, COMPLETED, FAILED

Response: Array of submission objects
```

### Get Submissions by User
```bash
GET /api/submissions/user/{submittedBy}

Response: Array of submission objects
```

### Delete Submission
```bash
DELETE /api/submissions/{id}

Response: 204 No Content
```

## Monitoring and Observability

### Kafka Consumer Groups

Monitor consumer lag:
```bash
docker exec submission-disk-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group submission-disk-group \
  --describe
```

### Application Logs

Each pipeline logs its processing:
```
VALIDATION PIPELINE: Processing submission ID: 1
VALIDATION PIPELINE: Submission 1 validated successfully
STORAGE PIPELINE: Processing submission ID: 1
STORAGE PIPELINE: Storage confirmed for submission 1
PROCESSING PIPELINE: Processing submission ID: 1
PROCESSING PIPELINE: Submission 1 processed successfully
NOTIFICATION PIPELINE: Notification sent for submission 1
```

### Database Queries

Check submission status:
```sql
SELECT id, file_name, status, submitted_at, processed_at 
FROM submissions 
ORDER BY submitted_at DESC;
```

## Scalability

### Horizontal Scaling

Each pipeline can be scaled independently:

```yaml
# docker-compose.override.yml
services:
  app-validation:
    image: submission-disk:latest
    environment:
      - KAFKA_CONSUMER_GROUP=validation-group
      - PIPELINE_ENABLED=validation
    scale: 3

  app-processing:
    image: submission-disk:latest
    environment:
      - KAFKA_CONSUMER_GROUP=processing-group
      - PIPELINE_ENABLED=processing
    scale: 5
```

### Kafka Partitions

Increase partitions for higher throughput:
```java
@Bean
public NewTopic submissionProcessingTopic() {
    return TopicBuilder.name(SUBMISSION_PROCESSING)
            .partitions(10)  // Increased from 3
            .replicas(1)
            .build();
}
```

## Error Handling

### Retry Logic

Failed messages can be retried:
```java
@KafkaListener(
    topics = SUBMISSION_PROCESSING,
    containerFactory = "kafkaListenerContainerFactory",
    errorHandler = "kafkaErrorHandler"
)
```

### Dead Letter Queue

Failed messages after retries go to failed topic:
```java
submissionProducer.sendEvent(KafkaTopicConfig.SUBMISSION_FAILED, event);
```

## Future Enhancements

### 1. Idempotency
Add idempotency keys to prevent duplicate processing

### 2. Saga Pattern
Implement distributed transactions with compensation

### 3. Circuit Breaker
Add resilience patterns for external service calls

### 4. Event Sourcing
Store all events for complete audit trail

### 5. CQRS
Separate read and write models for better performance

### 6. Schema Registry
Add Confluent Schema Registry for event schema management

### 7. Kafka Streams
Use Kafka Streams for complex event processing

## Testing

### Integration Tests

```bash
./gradlew test
```

### Manual Testing

```bash
# Submit a file
curl -X POST http://localhost:8080/api/submissions \
  -F "file=@test-submission.zip" \
  -F "submittedBy=test@example.com" \
  -F "description=Test submission"

# Check status
curl http://localhost:8080/api/submissions/1

# Watch logs
docker-compose logs -f
```

## Troubleshooting

### Kafka Connection Issues
```bash
# Check Kafka is running
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka

# Test Kafka connectivity
docker exec submission-disk-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Database Connection Issues
```bash
# Check PostgreSQL is running
docker-compose ps postgresql

# Connect to database
docker exec -it submission-disk-postgres psql -U admin -d submissiondb
```

### Redis Connection Issues
```bash
# Check Redis is running
docker-compose ps redis

# Test Redis connectivity
docker exec submission-disk-redis redis-cli ping
```

## License

MIT License

## Contributors

- Your Name <your.email@example.com>

## Support

For issues and questions, please open an issue on GitHub.

