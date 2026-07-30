# Log Aggregation Setup Guide

## Overview

This setup uses **Grafana Loki** for log aggregation (similar to Splunk or Datadog). Logs from all Docker containers are automatically collected and made searchable in Grafana.

## Architecture

```
Docker Containers (stdout)
    ↓
Promtail (log shipper)
    ↓
Loki (log storage)
    ↓
Grafana (log visualization)
```

## Components

### Loki
- **Port**: 3100
- **Purpose**: Log aggregation and storage
- **Retention**: 30 days (configurable)
- **Config**: `monitoring/loki/loki-config.yml`

### Promtail
- **Port**: 9080 (internal)
- **Purpose**: Collects logs from Docker containers
- **Config**: `monitoring/promtail/promtail-config.yml`
- **Access**: Docker socket (`/var/run/docker.sock`)

## Starting the Log Stack

```bash
# Start infrastructure (includes Loki and Promtail)
docker-compose -f docker-compose.infrastructure.yml up -d loki promtail

# Or start everything
docker-compose -f docker-compose.infrastructure.yml up -d
```

## Accessing Logs

### Via Grafana Dashboard

1. Go to **Grafana** → **Dashboards** → **ULHT DCS - Logs Explorer**
2. Pre-configured views:
   - All Microservices Logs
   - Individual service logs
   - Error & Warning Logs
   - Logs with Correlation ID
   - Infrastructure Logs

### Via Grafana Explore

1. Go to **Grafana** → **Explore** → Select **Loki** data source
2. Use LogQL queries to search logs

## LogQL Query Examples

### Basic Queries

```logql
# All microservices logs
{job="docker", container=~"ulht-.*"}

# Specific service
{job="docker", container=~".*student.*"}

# Error logs only
{job="docker"} |= "ERROR"

# Warning and Error logs
{job="docker"} |= "ERROR" or {job="docker"} |= "WARN"

# Logs by level (if extracted)
{job="docker", level="ERROR"}
```

### Advanced Queries

```logql
# Find logs with correlation ID
{job="docker"} | json | correlation_id != ""

# Find Kafka-related logs
{job="docker"} |= "Kafka"

# Count logs by level
sum(count_over_time({job="docker"} [1m])) by (level)

# Find logs in last 5 minutes
{job="docker"} [5m]

# Filter by container name pattern
{job="docker", container=~".*credential.*"}

# Combine filters
{job="docker", container=~".*student.*"} |= "ERROR"
```

### Service-Specific Queries

```logql
# Student Service
{job="docker", container=~".*student.*"}

# Credential Service
{job="docker", container=~".*credential.*"}

# Fulfilment Service
{job="docker", container=~".*fulfilment.*"}

# Lusofona Service
{job="docker", container=~".*lusofona.*"}
```

## Log Parsing

Promtail automatically extracts:
- **Log Level**: INFO, WARN, ERROR, DEBUG, TRACE, FATAL
- **Correlation ID**: If present in logs (UUID format)
- **Logger Name**: Java logger class name
- **Thread Name**: Thread identifier
- **Timestamp**: From log message

## Log Labels

Available labels for filtering:
- `job`: "docker", "microservices", or "infrastructure"
- `container`: Container name (e.g., "ulht-student-service")
- `service`: Service name extracted from container (e.g., "student")
- `level`: Log level (if extracted)
- `correlation_id`: Correlation ID (if present)
- `logger`: Logger class name (if extracted)
- `thread`: Thread name (if extracted)
- `stream`: "stdout" or "stderr"

## Troubleshooting

### No logs appearing

1. **Check Promtail is running**:
   ```bash
   docker ps | grep promtail
   docker logs promtail
   ```

2. **Check Loki is running**:
   ```bash
   docker ps | grep loki
   curl http://localhost:3100/ready
   ```

3. **Check Promtail can access Docker**:
   ```bash
   docker exec promtail ls -la /var/run/docker.sock
   ```

4. **Check container names match regex**:
   ```bash
   docker ps --format "{{.Names}}"
   ```
   Should match pattern: `ulht-*-service`, `prometheus`, `grafana`, etc.

### Logs not parsing correctly

1. **Check log format**: Spring Boot logs should follow pattern:
   ```
   YYYY-MM-DD HH:mm:ss.SSS [thread] LEVEL [correlationId] logger - message
   ```

2. **View raw logs in Loki**:
   ```logql
   {job="docker", container="ulht-student-service"} | json
   ```

3. **Check Promtail logs**:
   ```bash
   docker logs promtail | grep -i error
   ```

### Performance Issues

- **Reduce log volume**: Adjust log levels in `application.yml`
- **Increase retention**: Modify `loki-config.yml` retention settings
- **Filter logs**: Use Promtail relabel configs to exclude noisy logs

## Configuration Files

- **Loki**: `monitoring/loki/loki-config.yml`
  - Retention: 30 days
  - Storage: Filesystem
  - Limits: Configurable ingestion rates

- **Promtail**: `monitoring/promtail/promtail-config.yml`
  - Docker discovery: Auto-discovers containers
  - Log parsing: Extracts Spring Boot log fields
  - Label extraction: Service names, log levels, etc.

## Best Practices

1. **Use structured logging**: Include correlation IDs for tracing
2. **Set appropriate log levels**: Reduce noise in production
3. **Use LogQL filters**: Filter at query time, not ingestion
4. **Monitor log volume**: Watch Loki metrics in Grafana
5. **Set up alerts**: Alert on error rate spikes

## Integration with Metrics

Logs can be correlated with metrics:
- Use correlation IDs to trace requests across services
- Link logs to Prometheus metrics via labels
- Create dashboards combining logs and metrics

## Resources

- [Loki Documentation](https://grafana.com/docs/loki/latest/)
- [LogQL Documentation](https://grafana.com/docs/loki/latest/logql/)
- [Promtail Documentation](https://grafana.com/docs/loki/latest/clients/promtail/)

