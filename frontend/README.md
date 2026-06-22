# Cartzilla Frontend

React 18 + TypeScript + Vite storefront & dashboard for the Cartzilla microservices
backend. Implements all 27 screens from **SRS §7** / `docs/FRONTEND-STITCH-PROMPTS.md`,
wired to the API Gateway (`http://localhost:8080`).

## Stack

- **Vite + React 18 + TypeScript**
- **React Router** — routes match SRS §7
- **TanStack Query** — server state, caching, loading/error
- **Axios** — single instance, token + envelope unwrap + 401 refresh interceptors
- **React Hook Form + Zod** — form state & validation
- **Tailwind CSS** — design tokens from "Prompt 0" (indigo `#4F46E5`, Inter/Manrope)
- **Recharts** — admin reports charts

## Run

```bash
cd frontend
npm install
cp .env.example .env      # optional; defaults to gateway on :8080
npm run dev               # http://localhost:5173
```

The dev server proxies `/api/**` to the gateway, so start the backend first
(`eureka → config → services → gateway`). The gateway already allows the
`http://localhost:5173` origin in CORS.

```bash
npm run build             # type-check + production bundle to dist/
npm run preview
```

## Structure

```
src/
├── components/
│   ├── layout/    Storefront header/footer, Account & Dashboard layouts, Auth layout
│   ├── product/   ProductCard
│   └── ui/        Button, Input, Modal, Toast, StatusChip, Pagination, States, ...
├── lib/           api (axios), auth-storage, format (₫/date), order helpers, cn
├── pages/         storefront · auth · account · purchase · orders · staff · admin
├── routes/        guards (ProtectedRoute, RoleRoute)
├── services/      typed API clients per domain
├── store/         auth + cart contexts
└── types/         API envelope + domain models
```

## Backend integration notes

- **Envelope**: every response is `{ success, message, data, timestamp }`; `api.*`
  helpers return `data.data`. Business errors come back as HTTP 4xx with a Vietnamese
  `message` surfaced directly into toasts/inline alerts.
- **Auth**: access + refresh tokens in `localStorage`; `Authorization: Bearer` added
  per request; a 401 triggers a single-flight refresh via `/users/refresh-token`.
  The gateway derives `X-User-Id`/`X-User-Role` from the JWT; checkout sends `userId`
  decoded from the token's `sub` claim.
- **Shipping snapshot**: checkout serializes the selected address to a JSON string;
  order detail parses it back for display.
- **VNPay**: `/checkout/payment` creates the payment URL and redirects; the result
  page polls `GET /api/payments/{orderId}` until `PAID`/`FAILED`.

## Design references

`design/stitch/` holds the original Google Stitch output (HTML + screenshots) used as
the visual reference for each screen, plus the generation scripts (`gen-all.ps1`).
```
