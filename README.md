# SEOlytics – Automated SEO Technical Auditor

Full-stack technical SEO crawler: Spring Boot 3 / Java 21 backend, React + Vite frontend, MySQL storage, JWT auth, downloadable HTML/PDF reports.

SEOlytics scores are an internal technical health metric. They are **not** Google’s ranking algorithm.

## Features

- Registration, login, bcrypt passwords, JWT-protected APIs
- Bounded public-web crawls with robots.txt, crawl-delay, sitemap discovery, and link discovery
- SSRF controls: no localhost, private IPs, or credentialed URLs
- On-page checks: titles, meta descriptions, canonicals, H1s, viewport, HTTPS, image alt, status codes, redirect chains, sampled broken links, GA4/gtag/GTM snippet detection
- Dashboard charts from stored crawl data
- Audit compare, history delete, HTML + PDF reports
- Optional Selenium rendering for JS-heavy homepages (`SEOLYTICS_USE_SELENIUM=true`)

## GA4 limitation

A detected `G-` measurement ID, `gtag`, or GTM snippet only means the markup was present in HTML. SEOlytics does **not** verify that analytics hits are collected.

## Selenium limitation

Headless Chrome is **off by default**. Enable it only if Chrome is installed. Without it, crawls use HTTP + JSoup (server-rendered HTML).

## Requirements

- Java 21+
- Maven 3.9+
- Node.js 20+
- MySQL 8 (or Docker)

## MySQL

```bash
docker compose up -d
```

Or create database `seolytics` and user `seolytics` / `seolytics`. Optional reference schema: `backend/src/main/resources/db/mysql-schema.sql`. Hibernate `ddl-auto=update` also creates tables.

## Environment

Copy `.env.example` and export variables for the backend process (or set them in your IDE). At minimum:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` (32+ characters in production)
- `CORS_ALLOWED_ORIGINS` for production frontend origins, for example `https://seolytics-ai.onrender.com`

For the hosted Vite frontend, set:

- `VITE_API_URL` to the deployed backend HTTPS URL, for example `https://seolytics-api.onrender.com`

## Run backend

```bash
cd backend
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

For local Maven runs, the backend uses the `dev` profile with an H2 database and starts on `http://localhost:8081`.

API: `http://localhost:8081`

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` (proxies `/api` to the backend).

## Render deployment

The backend deploys as a separate Render Web Service from `backend/Dockerfile`. The Docker build runs:

```bash
mvn clean package -DskipTests
```

The container starts the Spring Boot jar with:

```bash
java -jar app.jar
```

Render must provide these backend environment variables:

- `DB_URL` or `DATABASE_URL` as a JDBC MySQL URL
- `DB_USERNAME` or `DATABASE_USERNAME`
- `DB_PASSWORD` or `DATABASE_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS=https://seolytics-ai.onrender.com`
- `SEOLYTICS_USE_SELENIUM=false`

The backend reads Render's `PORT` automatically and binds to `0.0.0.0`.

## Demo workflow

1. Register and sign in
2. New audit → public `https://` URL (start with 15–20 pages)
3. Watch crawl progress, then review issues, page metadata, and score breakdown
4. Download HTML/PDF reports
5. Run a second crawl and compare

## REST APIs

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Login |
| GET | `/api/auth/me` | Current user |
| POST | `/api/audits` | Start crawl `{ url, maxPages, maxDepth }` |
| GET | `/api/audits` | History |
| GET | `/api/audits/{id}` | Results (`query`, `severity`, `type`, `status`) |
| GET | `/api/audits/{id}/progress` | Progress |
| GET | `/api/dashboard` | Dashboard stats |
| GET | `/api/audits/{id}/compare/{otherId}` | Compare |
| DELETE | `/api/audits/{id}` | Delete |
| GET | `/api/audits/{id}/report.html` | HTML report |
| GET | `/api/audits/{id}/report.pdf` | PDF report |

## Crawler safety

- Hard caps on pages, depth, HTML size, redirect hops, and broken-link samples
- Delay between requests (and robots.txt crawl-delay when higher)
- Failures produce `FAILED` or `PARTIAL` status with stored pages

## Tests

```bash
cd backend
mvn test
```

Covers URL safety, robots.txt, scoring, and registration/JWT.
