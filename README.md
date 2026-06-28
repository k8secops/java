# Java Inventory Service

Spring Boot REST API for inventory management — products and orders backed by an H2 in-memory database.
Part of the **GitOps Demo** multi-service application.

## Role in the Demo

```
Browser → Node.js Frontend → Go Catalog Service
                           → Java Inventory Service (this)
```

The Node.js frontend calls this service for:
- `GET /api/v1/products` — inventory table on the dashboard
- `GET /api/v1/orders` — orders table on the dashboard
- `POST /api/v1/orders` — "Place Demo Order" button

## Tech Stack

- Java 17, Spring Boot 3.2
- Spring Data JPA + H2 (in-memory, seeded on startup)
- Spring Boot Actuator (health, metrics, info)
- Bean Validation (Jakarta)

## API Endpoints

### Products

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/products` | List all products (supports `?category=` and `?search=`) |
| GET | `/api/v1/products/{id}` | Get product by ID |
| GET | `/api/v1/products/low-stock` | Products below stock threshold (default 10) |
| POST | `/api/v1/products` | Create product |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Delete product |

### Orders

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/orders` | List all orders (supports `?email=` and `?status=`) |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| POST | `/api/v1/orders` | Create order |
| PATCH | `/api/v1/orders/{id}/status` | Update order status |
| POST | `/api/v1/orders/{id}/cancel` | Cancel order |

### Health

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Spring Boot health check |
| GET | `/actuator/info` | App info |
| GET | `/actuator/metrics` | Metrics |

### Sample — Create Order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerName": "Alice Johnson",
    "customerEmail": "alice@demo.com",
    "items": [{"productId": 1, "quantity": 2}]
  }'
```

## Seed Data

8 products are inserted on startup via `src/main/resources/data.sql`:
Electronics (Laptop, Mouse, Keyboard, Monitor, USB Hub), Furniture (Chair, Desk), Stationery (Notebooks).

## Run Locally

**Prerequisites:** Java 17+, Maven 3.8+

```bash
mvn spring-boot:run
# Service starts on :8080

curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/products
```

H2 console available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:gitopsdb`)

## Build & Test

```bash
# Run tests
mvn test

# Build jar (required before Docker build)
mvn package -DskipTests
```

## Docker

```bash
# Must build jar first
mvn package -DskipTests

docker build -t demo-java .
docker run -p 8080:8080 demo-java
```

The Dockerfile copies `target/gitops-app-1.0.0.jar` — `mvn package` must run before the Docker build. The gitops-platform compile task handles this automatically.

## Kubernetes

Manifests are in [`kubernetes/`](kubernetes/):

| File | Purpose |
|------|---------|
| `namespace.yaml` | Creates the `demo` namespace |
| `configmap.yaml` | Server port and DB config |
| `deployment.yaml` | 1 replica, 60s liveness delay (JVM startup), 256Mi RAM |
| `service.yaml` | ClusterIP on port `8080` — internal only |
| `kustomization.yaml` | Kustomize entrypoint |

Apply:
```bash
kubectl apply -k kubernetes/
```

> **Image:** Update `DOCKERHUB_USER/demo-java:latest` in `deployment.yaml` with your registry image after the CI pipeline builds and pushes it.

## CI Pipeline (gitops-platform)

When onboarding in the gitops-platform, set:
- **Language:** Java
- **Dockerfile:** `Dockerfile`
- **Build context:** `.`
- **Compile command:** `mvn package -DskipTests`
- **Test command:** `mvn test`
