---
name: kill-all
description: Stop and remove all project-related services, stop frontend and kill testing browser
user-invocable: true
disable-model-invocation: false
---

# Kill All — Stop Everything

Stop and remove all project-related services in parallel:

## Steps (run in parallel)

### 1. Docker containers
Stop and remove all containers from both compose files. Only remove volumes if the argument `volumes` was passed (e.g. `/kill-all volumes`). Otherwise volumes persist.
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform && \
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env down --remove-orphans 2>/dev/null; \
docker compose -f docker/compose/docker-compose-full.yaml --env-file docker/compose/.env down --remove-orphans 2>/dev/null; \
echo "Docker containers stopped and removed"
```
If the argument includes `volumes`, add `-v` flag to both `down` commands:
```bash
cd /home/radionica/Radionica/Tradex/Tradex/eu-auction-platform && \
docker compose -f docker/compose/docker-compose-infrastructure.yaml --env-file docker/compose/.env down -v --remove-orphans 2>/dev/null; \
docker compose -f docker/compose/docker-compose-full.yaml --env-file docker/compose/.env down -v --remove-orphans 2>/dev/null; \
echo "Docker containers stopped and removed (volumes deleted)"
```

### 2. Frontend dev servers
Kill all frontend dev server processes by port. Do NOT use `pkill -f` with path patterns — it matches the bash process itself and causes exit code 144.
```bash
for port in 3000 3001 3002 5174 5175 5176 5177; do
  pid=$(lsof -ti :$port 2>/dev/null)
  if [ -n "$pid" ]; then kill -9 $pid 2>/dev/null && echo "Killed PID $pid on port $port"; fi
done; \
echo "Frontend dev servers killed"
```

### 3. Quarkus dev mode services
Kill any running Gradle/Quarkus dev processes:
```bash
pkill -f "quarkusDev" 2>/dev/null; \
pkill -f "quarkus:dev" 2>/dev/null; \
pkill -f "gradlew" 2>/dev/null; \
# Kill Java processes on service ports 8080-8092
for port in $(seq 8080 8092); do
  pid=$(lsof -ti :$port 2>/dev/null)
  [ -n "$pid" ] && kill -9 $pid 2>/dev/null
done; \
echo "Quarkus dev services killed"
```

### 4. Chrome DevTools MCP browser
If a Chrome DevTools MCP-controlled browser is open, close all its pages and kill it:
- First try: use `mcp__chrome-devtools__list_pages` to check if browser is connected, then close pages
- Then kill any headless/remote-debugging Chrome processes:
```bash
pkill -f "remote-debugging" 2>/dev/null; \
pkill -f "chrome.*--remote-debugging-port" 2>/dev/null; \
pkill -f "chromium.*--remote-debugging-port" 2>/dev/null; \
echo "Chrome DevTools browser killed"
```

## Execution strategy
Run steps 1–4 in **parallel** using separate Bash tool calls. Report results when all complete.
