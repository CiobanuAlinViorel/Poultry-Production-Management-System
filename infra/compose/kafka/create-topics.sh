#!/bin/sh
# Idempotent topic bootstrap for local dev.
# Usage: create-topics.sh <bootstrap-server>

BOOTSTRAP="${1:-localhost:9092}"

TOPICS=$(cat <<'EOF'
ppms.identity.user:3
ppms.admin.role-assignment:3
ppms.admin.employee:3:compact
ppms.admin.settings:3
ppms.admin.catalog:3:compact
ppms.admin.roster:3
ppms.site.directory:3:compact
ppms.parent-flock.flock:3
ppms.parent-flock.production:3
ppms.parent-flock.egg-transfer:3
ppms.hatchery.egg-reception:3
ppms.hatchery.incubation:3
ppms.hatchery.chick-transfer:3
ppms.farm.chick-reception:3
ppms.farm.lot:3
ppms.farm.daily-ops:3
ppms.farm.delivery:3
ppms.slaughterhouse.reception:3
ppms.slaughterhouse.processing:3
ppms.slaughterhouse.cold-chain:3
ppms.slaughterhouse.sales:3
ppms.inventory:3
ppms.alerts:3
ppms.notification:3
ppms.audit:3
EOF
)

echo "$TOPICS" | while IFS=':' read -r topic partitions cleanup; do
  [ -z "$topic" ] && continue
  partitions=${partitions:-3}
  cleanup=${cleanup:-delete}
  echo "Creating topic $topic (partitions=$partitions, cleanup=$cleanup)"
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --create \
    --if-not-exists \
    --partitions "$partitions" \
    --replication-factor 1 \
    --config "cleanup.policy=$cleanup" \
    --topic "$topic" || true
done

echo "Topics created."
