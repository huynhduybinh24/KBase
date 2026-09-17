# KBase Frontend

React 19 + Vite + TypeScript frontend for KBase. Milestone 1 includes authentication,
protected routing, and the initial application shell. Projects, documents, and chat are
intentionally represented only as upcoming features.

## Run locally

```shell
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`. Set `VITE_API_BASE_URL` in a local `.env`
when the backend is not at `http://localhost:8080`. Swagger is available at
`http://localhost:8080/swagger-ui/index.html`.

```shell
npm run test
npm run build
```

The access token is currently stored through one local-storage abstraction. A production
architecture should prefer secure HttpOnly cookies if backend cookie authentication is
added later.
