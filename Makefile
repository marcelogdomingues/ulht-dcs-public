# DCS — Makefile
# Convenience wrappers around the Docker Compose stacks (Docker Compose v2).
# See docs/DEPLOYMENT.md and docs/GETTING_STARTED.md for full details.

.PHONY: help \
        demo demo-stop demo-logs demo-status \
        up down restart logs ps build rebuild clean shell \
        obs-up obs-down

# Primary application stack = microservices + (optional) local override.
# The override is git-ignored; it is included automatically when present.
OVERRIDE := $(wildcard docker-compose.override.yml)
COMPOSE  := docker compose -f docker-compose.microservices.yml $(if $(OVERRIDE),-f docker-compose.override.yml,)
# Observability + infra stack (Prometheus / Grafana / Loki / Promtail / exporters).
COMPOSE_OBS := docker compose -f docker-compose.infrastructure.yml

help:
	@echo "DCS — Make targets"
	@echo ""
	@echo "Demo (self-contained: mock walt.id + SIS, no .env needed):"
	@echo "  demo         Build & start the one-command demo stack"
	@echo "  demo-stop    Stop and remove the demo stack"
	@echo "  demo-logs    Follow the credential + sis service logs"
	@echo "  demo-status  Show demo service status"
	@echo ""
	@echo "Full stack (microservices + override; needs .env — see docs/GETTING_STARTED.md):"
	@echo "  up           Build & start the full stack"
	@echo "  down         Stop the full stack"
	@echo "  restart      Restart the full stack"
	@echo "  logs         Follow logs from all services"
	@echo "  ps           Show service status"
	@echo "  build        Build all service images"
	@echo "  rebuild      Build all service images (no cache)"
	@echo "  clean        Stop & remove containers, volumes, and networks"
	@echo "  shell SERVICE=<name>   Open a shell in a running service"
	@echo ""
	@echo "Observability (Prometheus / Grafana / Loki / Promtail):"
	@echo "  obs-up       Start the observability + infra stack"
	@echo "  obs-down     Stop the observability + infra stack"

# --- Demo -------------------------------------------------------------------
demo:
	@echo "🎬 Starting self-contained DCS demo (mock walt.id + SIS)..."
	docker compose -f docker-compose.demo.yml up -d --build
	@echo "✅ Demo starting. Poll: make demo-status"
	@echo "   Entry point: http://localhost:8084/api/v1/student/issue (apikey: demo-key)"
	@echo "   See docs/DEMO.md for issue/poll/fetch/verify curl commands."

demo-stop:
	docker compose -f docker-compose.demo.yml down

demo-logs:
	docker compose -f docker-compose.demo.yml logs -f credential-service sis-service

demo-status:
	docker compose -f docker-compose.demo.yml ps

# --- Full application stack --------------------------------------------------
up:
	@echo "🚀 Starting the full DCS stack..."
	$(COMPOSE) up -d --build
	@echo "✅ Started. Kafka UI · Consul :8500 · Kong :8000 · services :8084-8087"

down:
	$(COMPOSE) down

restart: down up

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

build:
	$(COMPOSE) build

rebuild:
	$(COMPOSE) build --no-cache

clean:
	@echo "🧹 Removing containers, volumes, and networks..."
	$(COMPOSE) down -v --remove-orphans

shell:
	@if [ -n "$(SERVICE)" ]; then \
		$(COMPOSE) exec $(SERVICE) /bin/sh; \
	else \
		echo "Usage: make shell SERVICE=credential-service"; \
	fi

# --- Observability + infra --------------------------------------------------
obs-up:
	$(COMPOSE_OBS) up -d

obs-down:
	$(COMPOSE_OBS) down
