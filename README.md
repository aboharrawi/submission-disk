# 🚀 Submission Disk - Kafka Pipeline Architecture

A modern, event-driven submission processing system built with Spring Boot and Apache Kafka. Each submission flows through independent, scalable pipeline stages for validation, storage, processing, and notification.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Quick Start](#quick-start)
- [Pipeline Stages](#pipeline-stages)
- [API Documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Documentation](#documentation)
- [Technology Stack](#technology-stack)

## 🏗️ Architecture Overview

```
Client Upload
     ↓
Ingestion (Immediate Response)
     ↓
submission.validation → ValidationPipeline
     ↓
submission.storage → StoragePipeline
     ↓
submission.processing → ProcessingPipeline
     ↓
submission.notification → NotificationPipeline
     ↓
Completed / Failed
```

### Key Features

✅ **Asynchronous Processing** - Non-blocking pipeline execution  
✅ **Independent Scaling** - Scale each pipeline stage separately  
✅ **Fault Tolerant** - Isolated failure handling per stage  
✅ **Event-Driven** - Complete audit trail in Kafka  
✅ **Extensible** - Easy to add new pipeline stages  

## 🚀 Quick Start

### Prerequisites

- Java 25
- Docker & Docker Compose
- Gradle (included via wrapper)

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL** (port 5432) - Database
- **Redis** (port 6379) - Caching
- **Kafka** (port 29092) - Event streaming
- **Zookeeper** (port 2181) - Kafka coordination

### 2. Build Application

```bash
./gradlew clean build
```

### 3. Run Application

```bash
./gradlew bootRun
```

The application starts on **http://localhost:8080**

### 4. Test the Pipeline

Run the automated test script:

```bash
./test-pipeline.sh
```

Or manually submit a file:

```bash
curl -X POST http://localhost:8080/api/submissions \
  -F "file=@test.zip" \
  -F "submittedBy=user@example.com" \
  -F "description=Test submission"
```

## 🔄 Pipeline Stages

### 1️⃣ Validation Pipeline

**Topic**: `submission.validation`  
**Responsibility**: Validate submissions and check for duplicates

- Checks for duplicate files by checksum
- Validates file integrity
- Updates status to `VALIDATED` or `FAILED`
- Publishes to: `submission.storage` or `submission.failed`

### 2️⃣ Storage Pipeline

**Topic**: `submission.storage`  
**Responsibility**: Manage file storage and backups

- Confirms file storage
- Can be extended for cloud storage (S3, MinIO)
- Can create backups
- Updates status to `STORED`
- Publishes to: `submission.processing`

### 3️⃣ Processing Pipeline

**Topic**: `submission.processing`  
**Responsibility**: Process submission contents

- Processes file contents
- Can unzip and analyze files
- Can run security scans
- Can execute automated tests
- Updates status to `COMPLETED` or `FAILED`
- Publishes to: `submission.notification` and `submission.completed`

### 4️⃣ Notification Pipeline

**Topics**: `submission.notification`, `submission.completed`, `submission.failed`  
**Responsibility**: Send notifications and alerts

- Sends completion notifications
- Handles failure alerts
- Can be extended for email, Slack, webhooks

## 📊 Submission Status Flow

```
PENDING → VALIDATED → STORED → PROCESSING → COMPLETED
   ↓         ↓          ↓           ↓
 FAILED    FAILED    FAILED      FAILED
```

## 🌐 API Documentation

### Submit a File

```http
POST /api/submissions
Content-Type: multipart/form-data

Parameters:
- file: ZIP file (required, max 100MB)
- description: string (optional)
- submittedBy: string (required)

Response: 200 OK
{
  "id": 1,
  "fileName": "submission.zip",
  "fileSize": 1048576,
  "status": "PENDING",
  "submittedAt": "2026-02-15T10:30:00",
  "submittedBy": "user@example.com"
}
```

### Get Submission

```http
GET /api/submissions/{id}

Response: 200 OK
{
  "id": 1,
  "fileName": "submission.zip",
  "status": "COMPLETED",
  "submittedAt": "2026-02-15T10:30:00",
  "processedAt": "2026-02-15T10:30:15"
}
```

### List All Submissions

```http
GET /api/submissions

Response: 200 OK
[
  { ... },
  { ... }
]
```

### Get by Status

```http
GET /api/submissions/status/{status}

Statuses: PENDING, VALIDATED, STORED, PROCESSING, COMPLETED, FAILED

Response: 200 OK
[...]
```

### Get by User

```http
GET /api/submissions/user/{submittedBy}

Response: 200 OK
[...]
```

### Delete Submission

```http
DELETE /api/submissions/{id}

Response: 204 No Content
```

## 📈 Monitoring

### Check Kafka Topics

```bash
docker exec submission-disk-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list
```

### Monitor Consumer Groups

```bash
docker exec submission-disk-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group submission-disk-group \
  --describe
```

### View Database

```bash
docker exec -it submission-disk-postgres \
  psql -U admin -d submissiondb \
  -c "SELECT id, file_name, status FROM submissions ORDER BY id DESC LIMIT 10;"
```

### Application Logs

```bash
# Watch pipeline execution
./gradlew bootRun | grep "PIPELINE"

# Expected output:
# VALIDATION PIPELINE: Processing submission ID: 1
# STORAGE PIPELINE: Processing submission ID: 1
# PROCESSING PIPELINE: Processing submission ID: 1
# NOTIFICATION PIPELINE: Notification sent for submission 1
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [KAFKA_PIPELINE_README.md](./KAFKA_PIPELINE_README.md) | Comprehensive architecture documentation |
| [PIPELINE_QUICKSTART.md](./PIPELINE_QUICKSTART.md) | Quick start guide with examples |
| [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) | Implementation details and summary |
| [DOCKER_README.md](./DOCKER_README.md) | Docker infrastructure setup |

## 🛠️ Technology Stack

### Backend
- **Spring Boot 4.0.2** - Application framework
- **Spring Kafka** - Kafka integration
- **Spring Data JPA** - Database access
- **Spring Data Redis** - Caching

### Infrastructure
- **Apache Kafka 7.6.0** - Event streaming
- **PostgreSQL 16** - Relational database
- **Redis 7** - In-memory cache
- **Docker Compose** - Container orchestration

### Build & Tools
- **Gradle 9.3** - Build automation
- **Lombok** - Boilerplate reduction
- **Jackson** - JSON serialization

## 🔧 Configuration

### Application Properties

```properties
# Kafka
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.consumer.group-id=submission-disk-group

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/submissiondb
spring.datasource.username=admin
spring.datasource.password=admin123

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# File Upload
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
submission.storage.path=uploads
```

## 🧪 Testing

### Automated Test

```bash
./test-pipeline.sh
```

### Manual Testing

```bash
# Create test file
echo "Test content" > test.txt
zip test.zip test.txt

# Submit
curl -X POST http://localhost:8080/api/submissions \
  -F "file=@test.zip" \
  -F "submittedBy=test@example.com"

# Check status (replace {id} with actual ID)
curl http://localhost:8080/api/submissions/{id}
```

## 🔍 Troubleshooting

### Kafka Connection Issues

```bash
# Check Kafka is running
docker-compose ps kafka

# View Kafka logs
docker-compose logs kafka

# Test connectivity
docker exec submission-disk-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

### Application Won't Start

1. Check if ports are available (8080, 5432, 6379, 9092)
2. Verify Docker containers are healthy
3. Check application logs for errors
4. Ensure Java 25 is installed

### Pipeline Not Processing

1. Check Kafka topics exist
2. Verify consumer group is active
3. Check application logs for exceptions
4. Ensure database is accessible

## 📦 Project Structure

```
submission-disk/
├── src/main/java/com/mharawi/submissiondisk/
│   ├── config/           # Kafka configuration
│   │   ├── KafkaTopicConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   └── KafkaConsumerConfig.java
│   ├── pipeline/         # Pipeline components
│   │   ├── SubmissionProducer.java
│   │   ├── ValidationPipeline.java
│   │   ├── StoragePipeline.java
│   │   ├── ProcessingPipeline.java
│   │   └── NotificationPipeline.java
│   ├── event/            # Event models
│   │   └── SubmissionEvent.java
│   ├── entity/           # Database entities
│   ├── service/          # Business logic
│   ├── controller/       # REST endpoints
│   └── repository/       # Database access
├── docker-compose.yml    # Infrastructure setup
├── build.gradle          # Dependencies
├── test-pipeline.sh      # Test script
└── *.md                  # Documentation
```

## 🚧 Extending the Pipeline

### Add Custom Validation

```java
// ValidationPipeline.java
customValidator.validate(event.getStoragePath());
```

### Add Cloud Storage

```java
// StoragePipeline.java
s3Client.upload(event.getStoragePath());
```

### Add Email Notifications

```java
// NotificationPipeline.java
emailService.send(event.getSubmittedBy(), "Submission Complete");
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

MIT License - see LICENSE file for details

## 👥 Support

For issues and questions:
- Check documentation in the `docs` folder
- Review [KAFKA_PIPELINE_README.md](./KAFKA_PIPELINE_README.md)
- Open an issue on GitHub

## 🎯 Next Steps

1. ✅ **Immediate**: Run `./test-pipeline.sh` to verify setup
2. 📧 **Short-term**: Add email notification service
3. ☁️ **Medium-term**: Integrate cloud storage (S3/MinIO)
4. 📊 **Long-term**: Add Prometheus/Grafana monitoring

---

**Built with ❤️ using Spring Boot, Kafka, and Event-Driven Architecture**

Last Updated: February 15, 2026

