# ULHT DCS Makefile
# Provides convenient commands for managing the application stack

.PHONY: help start stop restart logs status clean
.PHONY: start-distributed stop-distributed restart-distributed status-distributed test-distributed
.PHONY: project-start project-stop project-restart project-logs project-status
.PHONY: infra-start infra-stop infra-restart infra-logs infra-status

# Default target
help:
	@echo "ULHT DCS Management Commands"
	@echo "============================"
	@echo ""
	@echo "Full Stack Commands:"
	@echo "  start          - Start the full application stack (Infra + Apps)"
	@echo "  stop           - Stop the full application stack"
	@echo "  restart        - Restart the full application stack"
	@echo "  logs           - Show logs from all services"
	@echo "  status         - Show status of all services"
	@echo "  clean          - Stop and remove all containers, volumes, and networks"
	@echo ""
	@echo "Distributed Microservices Commands:"
	@echo "  start-distributed    - Start distributed microservices with service discovery"
	@echo "  stop-distributed     - Stop distributed microservices"
	@echo "  restart-distributed  - Restart distributed microservices"
	@echo "  status-distributed   - Show distributed services status"
	@echo "  test-distributed     - Test distributed architecture"
	@echo ""
	@echo "Development Environment Commands:"
	@echo "  dev-start      - Start complete dev environment (Infra + Apps)"
	@echo "  dev-stop       - Stop complete dev environment"
	@echo "  dev-restart    - Restart complete dev environment"
	@echo ""
	@echo "Infrastructure Commands (Kafka, Zookeeper):"
	@echo "  infra-start    - Start only the infrastructure (Kafka, Zookeeper, Kafka-UI)"
	@echo "  infra-stop     - Stop only the infrastructure"
	@echo "  infra-restart  - Restart only the infrastructure"
	@echo "  infra-logs     - Show infrastructure logs"
	@echo "  infra-status   - Show infrastructure status"
	@echo ""
	@echo "Project Commands (Spring Boot Services):"
	@echo "  project-start   - Start only the Spring Boot services (ulht-waltid-proxy, ulht-credential-service)"
	@echo "  project-stop    - Stop only the Spring Boot services"
	@echo "  project-restart - Restart only the Spring Boot services"
	@echo "  project-logs    - Show Spring Boot services logs"
	@echo "  project-status  - Show Spring Boot services status"
	@echo ""
	@echo "Development Commands:"
	@echo "  build          - Build all application images"
	@echo "  rebuild        - Rebuild all application images (no cache)"
	@echo "  shell          - Open shell in a specific service"
	@echo ""

# Application stack commands
start:
	@echo "🚀 Starting ULHT DCS full stack..."
	docker-compose up -d
	@echo "✅ Full stack started!"
	@echo "📊 Services available at:"
	@echo "   Kafka UI: http://localhost:8081"
	@echo "   ulht-waltid-proxy: http://localhost:8085"
	@echo "   ulht-credential-service: http://localhost:8086"

stop:
	@echo "🛑 Stopping ULHT DCS full stack..."
	docker-compose down
	@echo "✅ Full stack stopped!"

restart:
	@echo "🔄 Restarting ULHT DCS full stack..."
	docker-compose down
	docker-compose up -d
	@echo "✅ Full stack restarted!"

logs:
	@echo "📝 Showing logs from all services..."
	docker-compose logs -f

status:
	@echo "📊 Service Status:"
	docker-compose ps

clean:
	@echo "🧹 Cleaning up all containers, volumes, and networks..."
	docker-compose down -v --remove-orphans
	docker system prune -f
	@echo "✅ Cleanup completed!"


# Development commands
build:
	@echo "🔨 Building application images..."
	docker-compose build
	@echo "✅ Build completed!"

rebuild:
	@echo "🔨 Rebuilding application images (no cache)..."
	docker-compose build --no-cache
	@echo "✅ Rebuild completed!"

shell:
	@echo "🐚 Available services for shell access:"
	@echo "   make shell SERVICE=ulht-credential-service"
	@echo "   make shell SERVICE=ulht-waltid-proxy"
	@echo "   make shell SERVICE=kafka"
	@if [ -n "$(SERVICE)" ]; then \
		echo "Opening shell in $(SERVICE)..."; \
		docker-compose exec $(SERVICE) /bin/bash; \
	else \
		echo "Please specify a SERVICE parameter"; \
	fi

# Quick commands for common tasks
kafka-logs:
	@echo "📝 Showing Kafka logs..."
	docker-compose logs -f kafka

app-logs:
	@echo "📝 Showing application logs..."
	docker-compose logs -f ulht-credential-service ulht-waltid-proxy


# Infrastructure (Kafka, Zookeeper) commands
infra-start:
	@echo "🏗️  Starting infrastructure services..."
	docker-compose up -d zookeeper kafka kafka-ui
	@echo "✅ Infrastructure services started!"
	@echo "📊 Services available at:"
	@echo "   Kafka: localhost:9092 (internal), localhost:29092 (external)"
	@echo "   Zookeeper: localhost:2181"
	@echo "   Kafka UI: http://localhost:8081"

infra-stop:
	@echo "🛑 Stopping infrastructure services..."
	docker-compose stop zookeeper kafka kafka-ui
	@echo "✅ Infrastructure services stopped!"

infra-restart:
	@echo "🔄 Restarting infrastructure services..."
	docker-compose restart zookeeper kafka kafka-ui
	@echo "✅ Infrastructure services restarted!"

infra-logs:
	@echo "📝 Showing infrastructure logs..."
	docker-compose logs -f zookeeper kafka kafka-ui

infra-status:
	@echo "📊 Infrastructure Services Status:"
	docker-compose ps zookeeper kafka kafka-ui

# Project (Spring Boot services) commands
project-start:
	@echo "🚀 Starting Spring Boot services..."
	docker-compose up -d ulht-waltid-proxy ulht-credential-service
	@echo "✅ Spring Boot services started!"
	@echo "📊 Services available at:"
	@echo "   ulht-waltid-proxy: http://localhost:8085"
	@echo "   ulht-credential-service: http://localhost:8086"

project-stop:
	@echo "🛑 Stopping Spring Boot services..."
	docker-compose stop ulht-waltid-proxy ulht-credential-service
	@echo "✅ Spring Boot services stopped!"

project-restart:
	@echo "🔄 Restarting Spring Boot services..."
	docker-compose restart ulht-waltid-proxy ulht-credential-service
	@echo "✅ Spring Boot services restarted!"

project-logs:
	@echo "📝 Showing Spring Boot services logs..."
	docker-compose logs -f ulht-waltid-proxy ulht-credential-service

project-status:
	@echo "📊 Spring Boot Services Status:"
	docker-compose ps ulht-waltid-proxy ulht-credential-service 

# Development environment commands
dev-start:
	@echo "🚀 Starting complete development environment..."
	@echo "📦 Starting infrastructure..."
	@make infra-start
	@echo "⏳ Waiting for Kafka to be ready..."
	@sleep 10
	@echo "🚀 Starting Spring Boot services..."
	@make project-start
	@echo ""
	@echo "✅ Complete development environment started!"
	@echo "📊 All services available at:"
	@echo "   Infrastructure:"
	@echo "     Kafka: localhost:9092 (internal), localhost:29092 (external)"
	@echo "     Zookeeper: localhost:2181"
	@echo "     Kafka UI: http://localhost:8081"
	@echo "   Applications:"
	@echo "     ulht-waltid-proxy: http://localhost:8085"
	@echo "     ulht-credential-service: http://localhost:8086"

dev-stop:
	@echo "🛑 Stopping complete development environment..."
	@make project-stop
	@make infra-stop
	@echo "✅ Complete development environment stopped!"

dev-restart:
	@echo "🔄 Restarting complete development environment..."
	@make dev-stop
	@make dev-start

# =============================================================================
# DISTRIBUTED MICROSERVICES COMMANDS
# =============================================================================

# Start distributed microservices with service discovery
start-distributed:
	@echo "🚀 Starting distributed microservices architecture..."
	docker-compose -f docker-compose.yml -f docker-compose.microservices.yml up -d
	@echo "✅ Distributed microservices started!"
	@echo "📊 Consul UI: http://localhost:8500"
	@echo "🌐 Kong Gateway: http://localhost:8000"
	@echo "🔧 Kong Admin: http://localhost:8001"

# Stop distributed microservices
stop-distributed:
	@echo "🛑 Stopping distributed microservices..."
	docker-compose -f docker-compose.yml -f docker-compose.microservices.yml down
	@echo "✅ Distributed microservices stopped!"

# Restart distributed microservices
restart-distributed: stop-distributed start-distributed
	@echo "🔄 Distributed microservices restarted!"

# Show distributed services status
status-distributed:
	@echo "📊 Distributed Microservices Status:"
	@echo "=================================="
	docker-compose -f docker-compose.yml -f docker-compose.microservices.yml ps

# Test distributed architecture
test-distributed:
	@echo "🧪 Testing distributed microservices architecture..."
	@chmod +x test-distributed-architecture.sh
	@./test-distributed-architecture.sh

# ... existing code ... 