#!/bin/sh
set -e

mkdir -p /app/uploads/shared-media
chown -R appuser:appgroup /app/uploads

exec su-exec appuser java -jar /app/app.jar
