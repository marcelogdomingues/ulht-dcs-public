# IntelliJ IDEA Setup Guide for ULHT Digital Credentials System

## ✅ Prerequisites Checklist

Before running the services in IntelliJ, ensure you have:

### 1. Infrastructure Services Running (Docker)
All these services **MUST** be running before starting any Spring Boot service in IntelliJ:

```bash
# Check if infrastructure is running
docker-compose -f docker-compose.infrastructure.yml ps
```

You should see:
- ✅ **Zookeeper** (localhost:2181) - HEALTHY
- ✅ **Kafka** (localhost:29092) - HEALTHY  
- ✅ **Consul** (localhost:8500) - HEALTHY
- ✅ **API Gateway / Kong** (localhost:8000, 8001) - HEALTHY
- ✅ **Kafka UI** (localhost:8081) - RUNNING

**Start infrastructure if not running:**
```bash
cd /Users/marcelodomingues/Developments/ulht-dcs
docker-compose -f docker-compose.infrastructure.yml up -d
```

### 2. Walt.id Services Running (Docker)
The credential service needs these external services:

- ✅ **Walt.id Issuer API** (localhost:7002)
- ✅ **Walt.id Verifier API** (localhost:7003)
- ✅ **Walt.id Wallet** (localhost:7001)

**Check walt.id services:**
```bash
docker ps | grep -E "issuer|verifier|wallet"
```

**If not running, start them from their docker-compose location:**
```bash
cd /Users/marcelodomingues/Developments/University/waltid-identity/docker-compose
docker-compose up -d
```

### 3. Java & Maven Setup
- ✅ **JDK 25** (openjdk-25) - Already configured in IntelliJ
- ✅ **Maven** - Ensure Maven is properly imported

## 🚀 Running Services in IntelliJ

### Option 1: Run Individual Services

I've created IntelliJ run configurations for all services. To run them:

1. **Open IntelliJ IDEA**
2. **Reload the project** (File → Reload All from Disk) to pick up new configurations
3. **Look for the run configurations** in the top-right dropdown:
   - `StudentService` (Port 8084)
   - `CredentialService` (Port 8086)
   - `FulfilmentService` (Port 8087)
   - `LusofonaService` (Port 8085)

4. **Select a service** from the dropdown and click the green ▶️ Run button

### Option 2: Run All Services at Once

1. Select **"All Services"** from the run configurations dropdown
2. Click the green ▶️ Run button
3. All 4 services will start in parallel

## 📋 Service Startup Order (Recommended)

While the compound configuration runs all services together, you may want to start them in this order for debugging:

1. **First:** `LusofonaService` (Port 8085)
   - Provides student data from ULHT API
   
2. **Second:** `CredentialService` (Port 8086)
   - Handles credential issuance and verification
   - Depends on walt.id services
   
3. **Third:** `FulfilmentService` (Port 8087)
   - Tracks workflow progress
   
4. **Last:** `StudentService` (Port 8084)
   - Entry point for all requests
   - Depends on FulfilmentService

## 🔍 Verifying Services are Running

Once services start, check:

### Health Endpoints
- Student Service: http://localhost:8084/api/v1/actuator/health
- Credential Service: http://localhost:8086/api/v1/actuator/health
- Fulfilment Service: http://localhost:8087/api/v1/actuator/health
- Lusofona Service: http://localhost:8085/api/v1/actuator/health

### Swagger UI (API Documentation)
- Student Service: http://localhost:8084/api/v1/swagger-ui.html
- Credential Service: http://localhost:8086/api/v1/swagger-ui.html

### Consul Service Discovery
- Consul UI: http://localhost:8500
- Check that services are registering with Consul

### Kafka Topics
- Kafka UI: http://localhost:8081
- Verify topics are created and messages are flowing

## ❌ Common Issues & Solutions

### Issue 1: "Cannot connect to Kafka"
**Solution:** Ensure Kafka infrastructure is running:
```bash
docker-compose -f docker-compose.infrastructure.yml ps kafka
```

### Issue 2: "Port already in use"
**Solution:** Check what's using the port:
```bash
lsof -i :8084  # Or whichever port is conflicting
```
Kill the process or change the port in `application.yml`

### Issue 3: "Bean creation failed" or "Cannot resolve configuration property"
**Solution:** 
1. Reimport Maven projects: Right-click on `pom.xml` → Maven → Reload Project
2. Rebuild: Build → Rebuild Project
3. Invalidate caches: File → Invalidate Caches / Restart

### Issue 4: "Could not find or load main class"
**Solution:**
1. Run Maven clean and compile:
   ```bash
   cd student-service
   mvn clean compile
   ```
2. Rebuild in IntelliJ: Build → Rebuild Project
3. Check that the module is properly recognized: File → Project Structure → Modules

### Issue 5: Compilation error in StudentController
**Already Fixed!** The enum conversion issue has been resolved. If you still see it, pull the latest changes.

## 🔧 Service Ports Summary

| Service            | Port | Context Path | Description                    |
|--------------------|------|--------------|--------------------------------|
| Student Service    | 8084 | /api/v1      | Entry point for all requests   |
| Lusofona Service   | 8085 | /api/v1      | ULHT API integration           |
| Credential Service | 8086 | /api/v1      | W3C credential management      |
| Fulfilment Service | 8087 | /api/v1      | Workflow tracking              |
| Kafka              | 29092| -            | Message broker (external)      |
| Kafka (internal)   | 9092 | -            | Message broker (container)     |
| Zookeeper          | 2181 | -            | Kafka coordination             |
| Consul             | 8500 | -            | Service discovery              |
| Kong Gateway       | 8000 | -            | API Gateway                    |
| Kong Admin         | 8001 | -            | Gateway administration         |
| Kafka UI           | 8081 | -            | Kafka management UI            |
| Walt.id Wallet     | 7001 | -            | Wallet service                 |
| Walt.id Issuer     | 7002 | -            | Credential issuer              |
| Walt.id Verifier   | 7003 | -            | Credential verifier            |

## 📝 Testing the Complete Flow

Once all services are running, test the complete credential issuance flow:

1. **Issue Credentials:**
   ```bash
   curl -X POST http://localhost:8084/api/v1/student/issue \
     -H "Content-Type: application/json" \
     -d '{
       "userName": "test-student",
       "password": "test-password"
     }'
   ```

2. **Monitor Progress:**
   - Check Kafka UI: http://localhost:8081
   - Check service logs in IntelliJ
   - Use Postman collections in `/postman` directory

3. **Check Consul:**
   - Open http://localhost:8500
   - Verify all services are registered

## 🛠️ Development Tips

### Hot Reload
- IntelliJ supports Spring Boot DevTools for hot reload
- Changes to Java files will auto-restart the service

### Debug Mode
- Use the 🐛 Debug button instead of ▶️ Run
- Set breakpoints in controllers, services, or Kafka consumers

### View Logs
- Each service has its own console tab in IntelliJ
- Logs are colored and formatted for easy reading
- Search logs with Ctrl+F (Cmd+F on Mac)

### Profile Configuration
- Default profile uses `localhost` for all services
- Docker profile uses container names
- Current setup is optimized for local development

## 📚 Additional Resources

- Main README: `/README.md`
- Postman Collections: `/postman/`
- Architecture Diagrams: `CompleteArchitecture.png`, `SimpleArchitecture.png`
- Service READMEs:
  - `/student-service/README.md`
  - `/credential-service/README.md`
  - `/fulfilment-service/README.md`
  - `/lusofona-service/README.md`

## 🎯 Next Steps

1. ✅ Ensure infrastructure is running
2. ✅ Ensure walt.id services are running
3. ✅ Reload IntelliJ to pick up new run configurations
4. ✅ Run "All Services" or individual services
5. ✅ Test health endpoints
6. ✅ Test with Postman collections

---

**Need help?** Check the service logs in IntelliJ console tabs or Kafka UI for message flow debugging.

