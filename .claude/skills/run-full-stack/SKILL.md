---
name: run-full-stack
description: Start Docker infrastructure + backend services + all frontends
user-invocable: true
disable-model-invocation: true
---

# Run Full Stack

Start the entire platform: Docker infrastructure, backend services, and all 3 frontends.

## Steps

### 1. Start infrastructure
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform && \
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env up -d
```

### 2. Wait for infrastructure health checks
Wait for postgres, redis, nats, minio, and elasticsearch to be healthy:
```bash
for svc in postgres redis nats minio elasticsearch; do
  while [ "$(docker inspect --format='{{.State.Health.Status}}' auction-platform-$svc 2>/dev/null)" != "healthy" ]; do sleep 2; done
  echo "$svc: healthy"
done
```

### 3. Start backend services
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform && \
docker compose -f docker/compose/docker-compose-full.yaml --env-file docker/compose/.env up -d --build
```

### 4. Start all 3 frontends (in parallel, background)
Run each frontend dev server in the background using the Bash tool with `run_in_background: true`:
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform/frontend/buyer-web && npm run dev
```
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform/frontend/seller-portal && npm run dev
```
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform/frontend/admin-dashboard && npm run dev
```

### 5. Verify
Wait 10 seconds, then check:
- `docker ps --filter "name=auction-platform" --format "table {{.Names}}\t{{.Status}}"` — all containers healthy
- Check frontend ports (3000, 5174, 5175 or nearby) are listening

## Execution strategy
- Steps 1-3 run **sequentially** (each depends on the previous)
- Step 4 runs all 3 frontends in **parallel** as background tasks
- Step 5 verifies everything is up
