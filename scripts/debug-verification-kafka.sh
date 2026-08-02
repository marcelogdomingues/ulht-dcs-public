#!/bin/bash
# Script to debug verification Kafka topic and consumer group

echo "🔍 Debugging Verification Kafka Topic"
echo "======================================"
echo ""

# Find Kafka container
KAFKA_CONTAINER=$(docker ps --format "{{.Names}}" | grep -i kafka | head -1)

if [ -z "$KAFKA_CONTAINER" ]; then
    echo "❌ Kafka container not found. Is Kafka running?"
    echo "Available containers:"
    docker ps --format "{{.Names}}"
    exit 1
fi

echo "✅ Found Kafka container: $KAFKA_CONTAINER"
echo ""

# 1. Check if topic exists
echo "1️⃣ Checking if topic 'verification.requested' exists..."
docker exec "$KAFKA_CONTAINER" kafka-topics --list --bootstrap-server localhost:29092 2>/dev/null | grep -i verification || echo "   ⚠️  Topic not found!"
echo ""

# 2. Describe topic
echo "2️⃣ Topic details:"
docker exec "$KAFKA_CONTAINER" kafka-topics --describe --bootstrap-server localhost:29092 --topic verification.requested 2>/dev/null || echo "   ⚠️  Could not describe topic"
echo ""

# 3. Check consumer group
echo "3️⃣ Consumer group 'credential-service-verification-group' status:"
docker exec "$KAFKA_CONTAINER" kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --group credential-service-verification-group \
  --describe 2>/dev/null || echo "   ⚠️  Consumer group not found or no members"
echo ""

# 4. Check latest messages in topic
echo "4️⃣ Latest messages in topic (last 5):"
docker exec "$KAFKA_CONTAINER" kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic verification.requested \
  --from-beginning \
  --max-messages 5 \
  --timeout-ms 5000 2>/dev/null || echo "   ⚠️  No messages found or timeout"
echo ""

# 5. Check topic offsets
echo "5️⃣ Topic partition offsets:"
docker exec "$KAFKA_CONTAINER" kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:29092 \
  --topic verification.requested 2>/dev/null || echo "   ⚠️  Could not get offsets"
echo ""

echo "======================================"
echo "💡 Next steps:"
echo "   1. Check student service logs for '📤 Publishing verification request'"
echo "   2. If consumer group exists, reset offsets: ./reset-verification-consumer-group.sh"
echo "   3. Restart credential service after resetting offsets"

