.PHONY: infra-up up obs-up down build test-it fe-dev seed keys logs

infra-up:
	cd infra/compose && docker compose -f docker-compose.yml up -d

up:
	cd infra/compose && docker compose -f docker-compose.yml -f docker-compose.services.yml up -d

obs-up:
	cd infra/compose && docker compose -f docker-compose.yml --profile observability up -d

down:
	cd infra/compose && docker compose -f docker-compose.yml -f docker-compose.services.yml --profile observability --profile backup down

build:
	cd backend && ./mvnw -T1C verify

test-it:
	cd backend && ./mvnw -T1C verify -Pintegration

fe-dev:
	cd frontend && pnpm install && pnpm dev

seed:
	infra/scripts/seed-demo.sh

keys:
	infra/scripts/gen-jwt-keys.sh

logs:
	cd infra/compose && docker compose -f docker-compose.yml -f docker-compose.services.yml logs -f
