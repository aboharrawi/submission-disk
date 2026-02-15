# Submission Disk - Docker Setup

This project uses Docker Compose to run PostgreSQL, Redis, and Kafka services.

## Prerequisites

- Docker
- Docker Compose

## Services

The Docker Compose setup includes:

- **PostgreSQL** (port 5432): Database service
  - Database: `submissiondb`
  - Username: `admin`
  - Password: `admin123`

- **Redis** (port 6379): Cache and session storage

- **Kafka** (ports 9092, 29092): Message broker
  - Internal port: 9092 (for inter-container communication)
  - External port: 29092 (for host machine connections)

- **Zookeeper** (port 2181): Kafka dependency

## How to Use

### Start all services

```bash
docker-compose up -d
```

### Check service status

```bash
docker-compose ps
```

### View logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f postgresql
docker-compose logs -f kafka
docker-compose logs -f redis
```

### Stop all services

```bash
docker-compose down
```

### Stop and remove volumes (clean slate)

```bash
docker-compose down -v
```

## Running the Spring Boot Application

After starting the Docker services, you can run the Spring Boot application:

```bash
./gradlew bootRun
```

Or build and run:

```bash
./gradlew build
java -jar build/libs/submission-disk-0.0.1-SNAPSHOT.jar
```

## Connection Details

The application is configured to connect to:

- **PostgreSQL**: `jdbc:postgresql://localhost:5432/submissiondb`
- **Redis**: `localhost:6379`
- **Kafka**: `localhost:29092` (from host machine)

## Testing Connections

### PostgreSQL
```bash
docker exec -it submission-disk-postgres psql -U admin -d submissiondb
```

### Redis
```bash
docker exec -it submission-disk-redis redis-cli
```

### Kafka Topics
```bash
docker exec -it submission-disk-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Troubleshooting

If services fail to start:

1. Check if ports are already in use:
   ```bash
   sudo netstat -tlnp | grep -E '5432|6379|9092|2181'
   ```

2. Check service health:
   ```bash
   docker-compose ps
   ```

3. View detailed logs:
   ```bash
   docker-compose logs
   ```

