#!/bin/bash

echo "Stopping kafka docker containers..."
docker compose -f docker-compose.yml --env-file .env down --volumes
