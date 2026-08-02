# DCS Monitoring Stack

This directory contains the monitoring and observability stack for the Digital Credential System.

## Overview

The monitoring stack includes:

- **Prometheus** - Metrics collection and storage
- **Grafana** - Visualization and dashboards
- **Loki** - Log aggregation (like Splunk/Datadog)
- **Promtail** - Log shipper (collects logs from containers)
- **Kafka Exporter** - Kafka-specific metrics
- **Spring Boot Actuator** - Application metrics from all microservices

## Architecture

```
┌─────────────────┐
│   Microservices │
│  (4 services)   │
│  Actuator       │
└────────┬────────┘
         │ /actuator/prometheus
         ▼
┌─────────────────┐     ┌─────────────────┐
│   Prometheus    │────▶│    Grafana      │
│   :9090         │     │     :3000       │
└────────┬────────┘     └─────────────────┘
         │                    ▲
         │                    │
    ┌────┴────┐              │
    │         │              │
┌───▼───┐  ┌──▼────────┐    │
│ Kafka │  │  Consul   │    │
│Exporter│  │  Metrics  │    │
└───────┘  └───────────┘    │
                            │
┌─────────────────┐         │
│ Docker Containers│        │
│  (stdout logs)  │         │
└────────┬────────┘         │
         │                  │
         ▼                  │
┌─────────────────┐         │
│    Promtail     │─────────┘
│  (log shipper)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│      Loki       │──────┐
│     :3100       │      │
└─────────────────┘      │
                          │
                          ▼
                  ┌───────────────┐
                  │    Grafana    │
                  │  (Logs View)  │
                  └───────────────┘
```

## Services Monitored

### Microservices
- **Student Service** (port 8084)
- **Sis Service** (port 8085)
- **Credential Service** (port 8086)
- **Fulfilment Service** (port 8087)

### Infrastructure
- **Kafka** (via Kafka Exporter on port 9308)
- **Zookeeper** (via JMX - optional)
- **Consul** (port 8500)
- **Kong Gateway** (via Prometheus plugin - optional)

## Quick Start

### 1. Start Infrastructure with Monitoring

```bash
docker-compose -f docker-compose.infrastructure.yml up -d
```

This will start:
- Prometheus on http://localhost:9090
- Grafana on http://localhost:3000 (admin/admin)
- Loki on http://localhost:3100 (log aggregation)
- Promtail (log shipper - no UI)
- Kafka Exporter on http://localhost:9308

### 2. Start Microservices

```bash
docker-compose -f docker-compose.microservices.yml up -d
```

### 3. Access Dashboards

- **Grafana**: http://localhost:3000
  - Username: `admin`
  - Password: `admin`
  
- **Prometheus**: http://localhost:9090
  - Query interface: http://localhost:9090/graph
  - Targets status: http://localhost:9090/targets

## Metrics Endpoints

All microservices expose Prometheus metrics at:
- `/api/v1/actuator/prometheus`

Example:
```bash
curl http://localhost:8084/api/v1/actuator/prometheus
curl http://localhost:8085/api/v1/actuator/prometheus
curl http://localhost:8086/api/v1/actuator/prometheus
curl http://localhost:8087/api/v1/actuator/prometheus
```

## Key Metrics Tracked

### Application Metrics (per service)
- **HTTP Request Rate** - Requests per second
- **HTTP Response Time** - P50, P95, P99 latencies
- **HTTP Error Rate** - 4xx and 5xx errors
- **JVM Memory** - Heap and non-heap usage
- **JVM GC** - Garbage collection stats
- **Thread Count** - Active and daemon threads
- **Kafka Consumer Lag** - Message processing lag
- **Kafka Producer** - Messages sent per second

### Kafka Metrics
- **Topic Size** - Messages per topic
- **Consumer Lag** - Lag per consumer group
- **Broker Info** - Broker status and metadata
- **Partition Info** - Partition offsets and sizes

### Infrastructure Metrics
- **Service Health** - Health check status
- **Consul** - Service discovery metrics

## Grafana Dashboards

### Pre-configured Dashboards (Auto-loaded)

Dashboards are automatically loaded on Grafana startup via provisioning:

1. **Microservices Overview** - General service metrics (Request rate, Error rate, Response time, JVM Memory, Health status)
2. **Kafka Overview** - Kafka-specific metrics (Topic size, Consumer lag, Broker count)
3. **Kafka Detailed** - Detailed Kafka metrics (Topics, partitions, consumer groups, lag)
4. **JVM Metrics** - Detailed JVM metrics (Heap memory, GC stats, Threads, CPU)
5. **Infrastructure & System** - Infrastructure health (Prometheus, Consul, Kafka)
6. **API Performance** - API-level performance metrics (endpoints, response times, error rates)
7. **Logs Explorer** - Centralized log viewing (all microservices and infrastructure logs)

All dashboards are located in `monitoring/grafana/dashboards/` and are automatically provisioned when Grafana starts.

### Importing Additional Dashboards

You can import dashboards from Grafana's dashboard library:

1. Go to Grafana → Dashboards → Import
2. Enter dashboard ID:
   - **Spring Boot 2.1 Statistics**: 11378
   - **JVM (Micrometer)**: 4701
   - **Kafka Exporter Overview**: 7589
   - **Kafka Dashboard**: 721

3. Select Prometheus as data source
4. Import and customize as needed

## Prometheus Queries

### Service Request Rate
```promql
sum(rate(http_server_requests_seconds_count[5m])) by (service)
```

### Service Error Rate
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
```

### Response Time (p95)
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))
```

### JVM Memory Usage
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### Kafka Consumer Lag
```promql
kafka_consumer_lag_sum
```

### Service Health Status
```promql
up{job=~"student-service|sis-service|credential-service|fulfilment-service"}
```

## Alerting (Future)

Alert rules can be added to `prometheus/alert_rules.yml` and configured in Prometheus.

Example alert rules:
- High error rate (> 5% for 5 minutes)
- High response time (p95 > 1s for 5 minutes)
- Service down (up == 0)
- High memory usage (> 80% for 5 minutes)
- High Kafka consumer lag (> 1000 messages)

## Log Aggregation with Loki

### Overview

Loki is Grafana's log aggregation system (similar to Splunk or Datadog) that collects logs from all Docker containers and makes them searchable in Grafana.

### How It Works

1. **Promtail** - Collects logs from Docker containers via Docker socket
2. **Loki** - Stores and indexes logs
3. **Grafana** - Visualizes logs with powerful querying capabilities

### Log Collection

Logs are automatically collected from:
- All microservices (`dcs-*-service`)
- Infrastructure services (Kafka, Consul, Prometheus, Grafana, etc.)
- Logs are parsed to extract:
  - Log level (INFO, WARN, ERROR, etc.)
  - Correlation IDs (if present)
  - Logger names
  - Thread names
  - Timestamps

### Accessing Logs in Grafana

1. Go to Grafana → Explore → Select "Loki" data source
2. Use LogQL queries to filter logs:
   ```logql
   # All microservices logs
   {job="microservices"}
   
   # Specific service
   {job="microservices", service="student-service"}
   
   # Error logs only
   {job="microservices"} |= "ERROR"
   
   # Logs with correlation ID
   {job="microservices"} | json | correlation_id != ""
   
   # Logs by level
   {job="microservices", level="ERROR"}
   ```

3. Or use the **Logs Explorer** dashboard for pre-configured views

### Log Retention

- Logs are retained for **30 days** (720 hours)
- Configurable in `monitoring/loki/loki-config.yml`

### LogQL Examples

```logql
# Find all errors in the last hour
{job="microservices"} |= "ERROR"

# Find logs for a specific correlation ID
{job="microservices"} | json | correlation_id="abc-123-def"

# Find Kafka-related logs
{job="microservices"} |= "Kafka"

# Count logs by level
sum(count_over_time({job="microservices"} [1m])) by (level)

# Find slow requests (>1s)
{job="microservices"} | json | duration > 1000
```

## Troubleshooting

### Prometheus not scraping services

1. Check if services are running:
   ```bash
   docker ps | grep dcs
   ```

2. Check Prometheus targets:
   http://localhost:9090/targets

3. Verify metrics endpoint is accessible:
   ```bash
   curl http://localhost:8084/api/v1/actuator/prometheus
   ```

4. Check service logs:
   ```bash
   docker logs dcs-student-service
   ```

### Grafana not showing data

1. Check Grafana data source:
   - Go to Configuration → Data Sources
   - Verify Prometheus URL: http://prometheus:9090
   - Test connection

2. Check Prometheus has data:
   - Go to Prometheus → Graph
   - Query: `up`

3. Check Grafana logs:
   ```bash
   docker logs grafana
   ```

### Kafka Exporter not working

1. Verify Kafka is running:
   ```bash
   docker ps | grep kafka
   ```

2. Check Kafka Exporter logs:
   ```bash
   docker logs kafka-exporter
   ```

3. Test Kafka Exporter endpoint:
   ```bash
   curl http://localhost:9308/metrics
   ```

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)
- [Kafka Exporter](https://github.com/danielqsj/kafka-exporter)

## Ports Summary

| Service | Port | Purpose |
|---------|------|---------|
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Visualization |
| Loki | 3100 | Log aggregation |
| Promtail | 9080 | Log shipper (internal) |
| Kafka Exporter | 9308 | Kafka metrics |

## Next Steps

1. **Add Alerting**: Configure Alertmanager for notifications
2. ✅ **Add Logging**: Integrated Loki for centralized logging
3. **Add Tracing**: Integrate Jaeger or Zipkin for distributed tracing
4. **Custom Dashboards**: Create business-specific dashboards
5. **Performance Tuning**: Optimize Prometheus retention and scrape intervals
6. **Log Parsing**: Enhance log parsing for better structured logging

