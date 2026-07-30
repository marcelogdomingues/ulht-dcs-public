# Monitoring Troubleshooting Guide

## Issue: HTTP Metrics Empty (`http_server_requests_seconds_count`)

### Problem
The dashboard shows empty results for HTTP metrics even though services are running and `up{job="service-name"}` returns 1.

### Why This Happens

1. **No HTTP Requests Yet**: Spring Boot only creates HTTP metrics when requests are made. If no requests have been made to the service, the metrics won't exist.

2. **Metric Name Differences**: In some Spring Boot versions, metric names might differ slightly.

### Solutions

#### 1. Generate Some Traffic

Make some HTTP requests to your services to generate metrics:

```bash
# Health check
curl http://localhost:8084/api/v1/actuator/health

# Student service
curl http://localhost:8084/api/v1/student/status/test-id

# Credential service
curl http://localhost:8086/api/v1/actuator/health
```

#### 2. Check Available Metrics in Prometheus

Go to http://localhost:9090/graph and try these queries:

```promql
# Check what HTTP metrics exist
{__name__=~"http.*"}

# Check all metrics from a service
{job="student-service"}

# List all available metrics
{job="student-service", __name__!=""}
```

#### 3. Verify Metric Names

The metric names in Spring Boot 3.x should be:
- `http_server_requests_seconds_count` - Request counter
- `http_server_requests_seconds_sum` - Total request time
- `http_server_requests_seconds_bucket` - Histogram buckets

If these don't exist, check:
- Is the service receiving requests?
- Is Actuator Prometheus endpoint enabled?
- Check service logs for errors

#### 4. Alternative Metric Names

If `http_server_requests_seconds_count` doesn't work, try:

```promql
# Alternative names (depends on Spring Boot version)
http_server_requests_seconds_total
http_server_requests_seconds
http_server_requests_count
```

#### 5. Check Prometheus Targets

Verify services are being scraped successfully:

1. Go to http://localhost:9090/targets
2. Check if all services show "UP" and have no errors
3. Look for any scrape errors in the UI

#### 6. Test Metrics Endpoint Directly

```bash
# Check what metrics are actually exposed
curl http://localhost:8084/api/v1/actuator/prometheus | grep http

# If empty, the service hasn't received requests yet
# Make a request first, then check again
curl http://localhost:8084/api/v1/student/status/test
curl http://localhost:8084/api/v1/actuator/prometheus | grep http
```

## Issue: Services Show as DOWN

### Problem
Services are running but dashboard shows them as DOWN.

### Solution

The `up` metric checks if Prometheus can scrape the target. If it shows DOWN:

1. **Check Prometheus Targets**: http://localhost:9090/targets
   - Look for scrape errors
   - Verify endpoints are correct

2. **Check Network**: Ensure Prometheus can reach services
   ```bash
   # From Prometheus container
   docker exec prometheus wget -O- http://ulht-student-service:8084/api/v1/actuator/prometheus
   ```

3. **Check Service Logs**:
   ```bash
   docker logs ulht-student-service | tail -50
   ```

4. **Verify Metrics Path**: Check if `/api/v1/actuator/prometheus` is correct
   ```bash
   curl http://localhost:8084/api/v1/actuator/prometheus
   ```

## Issue: Dashboard Shows No Data

### Common Causes

1. **No Traffic**: Services need to receive requests to generate HTTP metrics
2. **Wrong Metric Names**: Metric names might differ in your Spring Boot version
3. **Time Range**: Make sure you're looking at a time range when data exists
4. **Prometheus Not Scraping**: Check Prometheus targets page

### Quick Fix

1. Generate traffic to services
2. Wait 15-30 seconds for Prometheus to scrape
3. Refresh Grafana dashboard
4. Check Prometheus directly: http://localhost:9090/graph

## Useful Prometheus Queries

```promql
# Check if service is up
up{job="student-service"}

# List all available metrics
{job="student-service"}

# Check HTTP metrics exist
http_server_requests_seconds_count{job="student-service"}

# JVM metrics (should always exist if service is running)
jvm_memory_used_bytes{job="student-service"}

# All metrics with "http" in name
{__name__=~"http.*", job="student-service"}
```

## Getting Help

1. Check Prometheus targets: http://localhost:9090/targets
2. Check Prometheus graph: http://localhost:9090/graph
3. Check service logs: `docker logs <service-name>`
4. Check Prometheus logs: `docker logs prometheus`

