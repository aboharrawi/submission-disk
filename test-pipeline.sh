#!/bin/bash

# Kafka Pipeline Test Script
# This script tests the complete submission pipeline

set -e

echo "=================================================="
echo "  Submission Disk - Kafka Pipeline Test"
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Check if Docker is running
echo -e "${YELLOW}[1/6] Checking Docker...${NC}"
if ! docker ps > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker is running${NC}"
echo ""

# Step 2: Start infrastructure
echo -e "${YELLOW}[2/6] Starting infrastructure (PostgreSQL, Redis, Kafka)...${NC}"
docker-compose up -d
echo "Waiting for services to be healthy (30 seconds)..."
sleep 30
echo -e "${GREEN}✓ Infrastructure started${NC}"
echo ""

# Step 3: Build application
echo -e "${YELLOW}[3/6] Building application...${NC}"
./gradlew clean build -x test --no-daemon
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo ""

# Step 4: Create test file
echo -e "${YELLOW}[4/6] Creating test ZIP file...${NC}"
mkdir -p test-data
echo "Test submission content" > test-data/test.txt
echo "README: This is a test submission" > test-data/README.md
cd test-data
zip -q test-submission.zip test.txt README.md
cd ..
echo -e "${GREEN}✓ Test file created: test-data/test-submission.zip${NC}"
echo ""

# Step 5: Start application in background
echo -e "${YELLOW}[5/6] Starting application...${NC}"
./gradlew bootRun > app.log 2>&1 &
APP_PID=$!
echo "Application PID: $APP_PID"
echo "Waiting for application to start (30 seconds)..."
sleep 30

# Check if app is running
if ps -p $APP_PID > /dev/null; then
    echo -e "${GREEN}✓ Application started${NC}"
else
    echo -e "${RED}✗ Application failed to start${NC}"
    echo "Last 20 lines of log:"
    tail -20 app.log
    exit 1
fi
echo ""

# Step 6: Test submission
echo -e "${YELLOW}[6/6] Testing submission pipeline...${NC}"
echo ""

# Submit file
echo "→ Submitting test file..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/submissions \
  -F "file=@test-data/test-submission.zip" \
  -F "submittedBy=test@example.com" \
  -F "description=Automated test submission")

echo "Response: $RESPONSE"
echo ""

# Extract submission ID
SUBMISSION_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

if [ -z "$SUBMISSION_ID" ]; then
    echo -e "${RED}✗ Failed to create submission${NC}"
    kill $APP_PID
    exit 1
fi

echo -e "${GREEN}✓ Submission created with ID: $SUBMISSION_ID${NC}"
echo ""

# Monitor status changes
echo "→ Monitoring pipeline progression..."
echo ""

for i in {1..10}; do
    STATUS_RESPONSE=$(curl -s http://localhost:8080/api/submissions/$SUBMISSION_ID)
    STATUS=$(echo $STATUS_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

    echo "  [$i] Status: $STATUS"

    if [ "$STATUS" == "COMPLETED" ]; then
        echo ""
        echo -e "${GREEN}✓✓✓ Pipeline completed successfully! ✓✓✓${NC}"
        break
    fi

    if [ "$STATUS" == "FAILED" ]; then
        echo ""
        echo -e "${RED}✗ Pipeline failed${NC}"
        echo "Response: $STATUS_RESPONSE"
        break
    fi

    sleep 2
done

echo ""

# Show pipeline logs
echo "→ Pipeline execution logs:"
echo ""
grep "PIPELINE" app.log | tail -20 || echo "No pipeline logs found yet"

echo ""
echo "=================================================="
echo "  Test Summary"
echo "=================================================="
echo ""
echo "Infrastructure: docker-compose.yml"
echo "  - PostgreSQL on port 5432"
echo "  - Redis on port 6379"
echo "  - Kafka on port 29092"
echo "  - Zookeeper on port 2181"
echo ""
echo "Application: http://localhost:8080"
echo "  - Submission ID: $SUBMISSION_ID"
echo "  - Final Status: $STATUS"
echo ""
echo "Kafka Topics:"
docker exec submission-disk-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep submission || echo "Could not list topics"
echo ""
echo "To view full logs: tail -f app.log"
echo "To stop application: kill $APP_PID"
echo "To stop infrastructure: docker-compose down"
echo ""

# Ask if user wants to stop
echo -e "${YELLOW}Keep application running for manual testing? (y/n)${NC}"
read -t 10 -n 1 KEEP_RUNNING || KEEP_RUNNING="n"
echo ""

if [ "$KEEP_RUNNING" != "y" ]; then
    echo "Stopping application..."
    kill $APP_PID
    echo -e "${GREEN}Application stopped${NC}"
else
    echo -e "${GREEN}Application running with PID: $APP_PID${NC}"
    echo "To stop: kill $APP_PID"
fi

echo ""
echo -e "${GREEN}Test completed!${NC}"

