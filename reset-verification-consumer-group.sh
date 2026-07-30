#!/bin/bash
# Script to reset the verification consumer group offsets
# This allows the consumer to re-read messages from the beginning

KAFKA_CONTAINER=$(docker ps --format "{{.Names}}" | grep -i kafka | head -1)

if [ -z "$KAFKA_CONTAINER" ]; then
    echo "❌ Kafka container not found. Is Kafka running?"
    exit 1
fi

echo "🔍 Found Kafka container: $KAFKA_CONTAINER"
echo "🔄 Resetting consumer group: credential-service-verification-group"
echo "📋 Topic: verification.requested"
echo ""

# Reset offsets to earliest
docker exec "$KAFKA_CONTAINER" kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --group credential-service-verification-group \
  --reset-offsets \
  --to-earliest \
  --topic verification.requested \
  --execute

echo ""
echo "✅ Consumer group offsets reset. Restart the credential service to consume from the beginning."

