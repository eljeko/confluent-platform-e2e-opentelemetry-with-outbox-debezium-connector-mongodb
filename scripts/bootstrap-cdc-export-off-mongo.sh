#!/bin/bash

sh scripts/tear-down-cdc-mongo.sh

echo "Starting kafka docker containers..."
docker compose -f docker-compose-export-off.yml --env-file .env up -d

echo "Done"