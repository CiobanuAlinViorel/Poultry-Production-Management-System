#!/usr/bin/env bash
# Starts each service against a scratch DB on the local docker Postgres and reports startup / schema validation errors.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PG="docker exec -i ppms-postgres psql -U postgres -v ON_ERROR_STOP=1"
SERVICES=${*:-admin farm hatchery parent-flock slaughterhouse notification reporting identity}
for svc in $SERVICES; do
  db="validate_${svc//-/_}"
  $PG -d postgres -qc "DROP DATABASE IF EXISTS $db;" >/dev/null 2>&1
  $PG -d postgres -qc "CREATE DATABASE $db;" >/dev/null
  echo "=== $svc ==="
  timeout 120 java -jar "$ROOT/backend/$svc-service/target/$svc-service-1.0.0.jar" \
    --spring.datasource.url="jdbc:postgresql://localhost:5433/$db" \
    --spring.datasource.username=postgres --spring.datasource.password=postgres \
    --spring.kafka.bootstrap-servers=localhost:9092 \
    --server.port=0 --spring.profiles.active=docker \
    --ppms.jwt.public-key-file=$ROOT/infra/compose/secrets/jwt-public.pem \
    --ppms.jwt.private-key-file=$ROOT/infra/compose/secrets/jwt-private.pem \
    > "/tmp/validate-$svc.log" 2>&1 &
  pid=$!
  for i in $(seq 1 120); do
    grep -qE "Started .*Application|Application run failed|APPLICATION FAILED TO START" "/tmp/validate-$svc.log" 2>/dev/null && break
    sleep 1
  done
  sleep 3  # let schedulers (outbox poller etc.) run at least once
  kill $pid 2>/dev/null; wait $pid 2>/dev/null
  grep -E "Started .*Application|Schema-validation|Migration .* failed|APPLICATION FAILED" "/tmp/validate-$svc.log" | head -3
  grep "Caused by" "/tmp/validate-$svc.log" | tail -1
  echo "runtime ERROR lines: $(grep -c ' ERROR ' "/tmp/validate-$svc.log")"
done
