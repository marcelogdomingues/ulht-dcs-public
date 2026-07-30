# Fix Applied: Missing Prometheus Dependency

## Problem
Services showed as DOWN in Grafana because:
1. **Missing Dependency**: Services were missing `micrometer-registry-prometheus` dependency
2. **No Prometheus Endpoint**: Without this dependency, the `/actuator/prometheus` endpoint doesn't exist
3. **Network Access**: Prometheus couldn't reach services running on localhost

## Solution Applied

### 1. Added Micrometer Prometheus Dependency
Added to all service `pom.xml` files:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Services updated:**
- ✅ credential-service
- ✅ student-service  
- ✅ lusofona-service
- ✅ fulfilment-service

### 2. Updated Prometheus Configuration
Added `host.docker.internal` targets so Prometheus can reach services running on localhost (Mac/Windows).

## Next Steps

### 1. Rebuild and Restart Services

```bash
# Rebuild services with new dependency
cd credential-service && mvn clean install -DskipTests
cd ../student-service && mvn clean install -DskipTests
cd ../lusofona-service && mvn clean install -DskipTests
cd ../fulfilment-service && mvn clean install -DskipTests

# Restart your services (however you're running them)
# If using IDE, restart the Spring Boot applications
# If using Docker, rebuild and restart containers
```

### 2. Verify Prometheus Endpoint

After restarting services, verify the endpoint works:

```bash
# Should return Prometheus metrics (not 404)
curl http://localhost:8084/api/v1/actuator/prometheus | head -20
curl http://localhost:8086/api/v1/actuator/prometheus | head -20
```

### 3. Restart Prometheus

```bash
docker restart prometheus
```

### 4. Check Prometheus Targets

Go to http://localhost:9090/targets and verify all services show as UP.

### 5. Refresh Grafana Dashboard

The dashboard should now show services as UP and metrics should appear.

## Expected Result

After applying this fix:
- ✅ Services show as UP in Grafana
- ✅ `/actuator/prometheus` endpoint returns metrics
- ✅ JVM metrics appear immediately
- ✅ HTTP metrics appear after making requests

## Troubleshooting

If services still show DOWN:
1. Verify services are running: `curl http://localhost:8084/api/v1/actuator/health`
2. Check Prometheus endpoint exists: `curl http://localhost:8084/api/v1/actuator/prometheus`
3. Check Prometheus targets: http://localhost:9090/targets
4. Check service logs for errors

