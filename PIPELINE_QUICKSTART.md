# Submission Disk Pipeline - Quick Start Guide

## Pipeline Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      SUBMISSION DISK PIPELINE                            │
└─────────────────────────────────────────────────────────────────────────┘

1. CLIENT REQUEST
   ┌──────────┐
   │  Client  │
   │  (POST)  │
   └────┬─────┘
        │ Upload ZIP file
        ▼
   
2. INGESTION (SubmissionService)
   ┌─────────────────────────────┐
   │ • Validate ZIP format       │
   │ • Calculate checksum        │
   │ • Store file                │
   │ • Create DB record (PENDING)│
   │ • Publish to Kafka          │
   └────────────┬────────────────┘
                │
                ▼ submission.validation
   
3. VALIDATION PIPELINE
   ┌─────────────────────────────┐
   │ • Check for duplicates      │
   │ • Validate integrity        │
   │ • Update status (VALIDATED) │
   │ • Publish to next stage     │
   └────────────┬────────────────┘
                │
                ▼ submission.storage
   
4. STORAGE PIPELINE
   ┌─────────────────────────────┐
   │ • Confirm storage           │
   │ • [Optional] Cloud backup   │
   │ • [Optional] Metadata       │
   │ • Update status (STORED)    │
   │ • Publish to next stage     │
   └────────────┬────────────────┘
                │
                ▼ submission.processing
   
5. PROCESSING PIPELINE
   ┌─────────────────────────────┐
   │ • Process contents          │
   │ • [Optional] Unzip/analyze  │
   │ • [Optional] Security scan  │
   │ • Update status (COMPLETED) │
   │ • Publish to notification   │
   └────────────┬────────────────┘
                │
                ▼ submission.notification
   
6. NOTIFICATION PIPELINE
   ┌─────────────────────────────┐
   │ • Send notifications        │
   │ • [Optional] Email          │
   │ • [Optional] Webhook        │
   │ • [Optional] Slack/Teams    │
   │ • Log completion            │
   └─────────────────────────────┘

FAILURE HANDLING (All stages)
   ┌─────────────────────────────┐
   │ Any stage can fail and      │
   │ publish to submission.failed│
   │ for alerting and recovery   │
   └─────────────────────────────┘
```

## Quick Start Commands

### 1. Start Infrastructure
```bash
docker-compose up -d
```

### 2. Build Application
```bash
./gradlew clean build
```

### 3. Run Application
```bash
./gradlew bootRun
```

### 4. Test Submission
```bash
curl -X POST http://localhost:8080/api/submissions \
  -F "file=@test.zip" \
  -F "submittedBy=user@example.com" \
  -F "description=Test submission"
```

### 5. Check Status
```bash
curl http://localhost:8080/api/submissions/1
```

### 6. Monitor Logs
```bash
docker-compose logs -f
# Or
./gradlew bootRun | grep "PIPELINE"
```

## Pipeline Status Codes

| Status      | Description                          | Next Stage       |
|-------------|--------------------------------------|------------------|
| PENDING     | Initial submission received          | VALIDATION       |
| VALIDATED   | Validation checks passed             | STORAGE          |
| STORED      | File stored successfully             | PROCESSING       |
| PROCESSING  | Currently being processed            | COMPLETED/FAILED |
| COMPLETED   | Successfully processed               | -                |
| FAILED      | Failed at any stage                  | -                |
| REJECTED    | Rejected during validation           | -                |

## Key Features

### ✅ Asynchronous Processing
Each pipeline stage processes independently without blocking

### ✅ Scalable Architecture
Scale individual pipelines based on workload

### ✅ Fault Tolerant
Failed messages are captured and can be retried

### ✅ Auditable
Complete event trail in Kafka and database

### ✅ Extensible
Easy to add new pipeline stages or modify existing ones

## Infrastructure Services

| Service    | Port  | URL                    | Credentials         |
|------------|-------|------------------------|---------------------|
| PostgreSQL | 5432  | localhost:5432         | admin/admin123      |
| Redis      | 6379  | localhost:6379         | -                   |
| Zookeeper  | 2181  | localhost:2181         | -                   |
| Kafka      | 29092 | localhost:29092        | -                   |
| App        | 8080  | http://localhost:8080  | -                   |

## Example Event Flow

### Request
```bash
POST /api/submissions
Content-Type: multipart/form-data

file: submission.zip
submittedBy: john@example.com
description: Q1 Report
```

### Response (Immediate)
```json
{
  "id": 42,
  "fileName": "submission.zip",
  "fileSize": 2048576,
  "status": "PENDING",
  "submittedAt": "2026-02-15T14:30:00",
  "submittedBy": "john@example.com",
  "description": "Q1 Report"
}
```

### Kafka Events (Asynchronous)
```
[14:30:00] submission.validation   → {"submissionId":42, "stage":"VALIDATION"}
[14:30:01] submission.storage      → {"submissionId":42, "stage":"STORAGE"}
[14:30:02] submission.processing   → {"submissionId":42, "stage":"PROCESSING"}
[14:30:04] submission.notification → {"submissionId":42, "status":"COMPLETED"}
```

### Final Status (Query)
```bash
GET /api/submissions/42
```

```json
{
  "id": 42,
  "fileName": "submission.zip",
  "status": "COMPLETED",
  "submittedAt": "2026-02-15T14:30:00",
  "processedAt": "2026-02-15T14:30:04",
  "submittedBy": "john@example.com"
}
```

## Monitoring Commands

### Check Kafka Topics
```bash
docker exec submission-disk-kafka kafka-topics \
  --bootstrap-server localhost:9092 --list
```

### Monitor Consumer Group
```bash
docker exec submission-disk-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group submission-disk-group \
  --describe
```

### View Database Records
```bash
docker exec -it submission-disk-postgres \
  psql -U admin -d submissiondb \
  -c "SELECT id, file_name, status, submitted_at FROM submissions ORDER BY id DESC LIMIT 10;"
```

## Troubleshooting

### Pipeline Not Processing?
1. Check Kafka is running: `docker-compose ps kafka`
2. Check consumer group: `docker-compose logs app | grep "Kafka"`
3. Verify topics exist: `docker exec submission-disk-kafka kafka-topics --list`

### File Upload Fails?
1. Check file size limit in `application.properties`
2. Verify upload directory exists and is writable
3. Check logs: `./gradlew bootRun`

### Database Connection Error?
1. Verify PostgreSQL is running: `docker-compose ps postgresql`
2. Test connection: `docker exec submission-disk-postgres pg_isready`
3. Check credentials in `application.properties`

## Next Steps

1. **Extend Validation**: Add custom validation rules in `ValidationPipeline.java`
2. **Add Cloud Storage**: Integrate S3/MinIO in `StoragePipeline.java`
3. **Custom Processing**: Implement business logic in `ProcessingPipeline.java`
4. **Notification Channels**: Add email/Slack in `NotificationPipeline.java`
5. **Monitoring**: Add Prometheus/Grafana for metrics
6. **Security**: Add authentication/authorization

## Architecture Benefits

✅ **Decoupled**: Each pipeline is independent
✅ **Resilient**: Failures don't affect other stages
✅ **Scalable**: Scale pipelines individually
✅ **Maintainable**: Easy to modify or add stages
✅ **Observable**: Complete event trail
✅ **Testable**: Test each pipeline in isolation

---

For detailed documentation, see [KAFKA_PIPELINE_README.md](./KAFKA_PIPELINE_README.md)

