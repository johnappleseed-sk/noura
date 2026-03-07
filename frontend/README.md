# NOURA Admin Dashboard

React + Vite + TypeScript admin dashboard for the NOURA enterprise e-commerce backend.

## Stack

- React 19 + TypeScript
- Redux Toolkit
- Axios
- Ant Design
- Recharts

## Implemented Features

- Login page with JWT-based authentication
- Role-based route protection (`ADMIN`, `B2B`)
- Dashboard metrics with charts
- Product CRUD UI
- Order management table + status transitions
- Store CRUD UI
- User management
- B2B approval queue panel
- Notification sender (broadcast + user-targeted)
- Dark/light theme toggle

## Structure

```text
src/admin/
  api/
  app/
  features/
  layouts/
  pages/
  routes/
  types/
```

## Environment

```bash
VITE_APP_NAME=NOURA Admin Dashboard
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## Run

```bash
npm install
npm run dev
```

## Build

```bash
npm run typecheck
npm run build
```
