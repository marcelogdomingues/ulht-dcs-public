-- Runs once on first Postgres initialization (files in /docker-entrypoint-initdb.d).
-- The primary database (fulfilment) is created by the container's POSTGRES_DB env var;
-- credential-service uses its own database on the same container, created here.
CREATE DATABASE credential;
GRANT ALL PRIVILEGES ON DATABASE credential TO ulht;
