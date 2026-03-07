# NOURA Platform

NOURA is an enterprise commerce platform with:
- A Spring Boot backend API (`backend/`) — core e-commerce with integrated commerce modules
- An enterprise admin frontend (`frontend/src/admin/`) — React 19 + TypeScript
- A management admin panel (`frontend/admin/`) — React 18 + Vite
- A customer storefront (`frontend/storefront/`) — Next.js 15
- A hardware bridge (`bridge/hardware-bridge/`) — Node.js
- Local infrastructure via Docker Compose (`docker-compose.yml`)

The backend supports authentication, catalog management, cart/checkout, orders, B2B approvals, notifications, and admin operations.  
The frontend currently boots the admin dashboard code path by default (`src/admin` alias).

Enterprise category/taxonomy guidance (hierarchy, governance, attribute model, channel mapping, localization, analytics) is documented in [ARCHITECTURE.md](./ARCHITECTURE.md).

## Documentation Map

- [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md): Full developer documentation (overview, setup, architecture, security, workflows)
- [ARCHITECTURE.md](./ARCHITECTURE.md): Detailed architecture and data flows
- [API_REFERENCE.md](./API_REFERENCE.md): Endpoint catalog and examples
- [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md): Day-to-day contributor workflows

## Quick Start (Docker)

1. Set a secure JWT secret (minimum 32 chars):

```cmd
set JWT_SECRET=replace_with_a_long_random_secret_value_at_least_32_chars
```

2. Start the stack:

```cmd
docker compose up --build
```

3. Access services:
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui`
- Enterprise Admin: `http://localhost:8081`
- Admin Panel: `http://localhost:5173`
- Storefront: `http://localhost:3000`

Optional profiles:
- Kafka/Zookeeper: `docker compose --profile eventing up --build`
- Elasticsearch: `docker compose --profile search up --build`

## Quick Start (Local Development)

### Backend

```cmd
cd backend
set JWT_SECRET=replace_with_a_long_random_secret_value_at_least_32_chars
mvnw.cmd spring-boot:run
```

### Frontend

```cmd
cd frontend
npm install
npm run dev
```

Default local URL: `http://localhost:5173`

## Useful Commands

### Backend

```cmd
cd backend
mvnw.cmd test
```

### Frontend

```cmd
cd frontend
npm run typecheck
npm run build
npm test -- --runInBand
```
