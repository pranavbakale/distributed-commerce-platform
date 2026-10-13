# Northstar Commerce

Northstar is a small event-driven commerce system built around a real order workflow rather than a collection of disconnected demos. A customer places an order in the storefront, inventory is reserved, payment is captured, and the order is completed asynchronously. The operations screens make that flow visible without requiring anyone to tail logs or run database queries.

The repository contains three Spring Boot services, a responsive storefront, and all of the infrastructure needed to run the system locally.

## What is in the project

- **Order service** owns checkout, order state, cancellation, and order summaries.
- **Inventory service** owns the product catalog, stock, and reservations. Product reads are cached in Redis, while JPA version columns protect stock updates from lost-update races.
- **Payment service** simulates capture and refunds. Successful receipt records are written to an S3 bucket through the AWS SDK.
- **Storefront** is a responsive shopping and operations interface served by Nginx. It includes product filtering, a persistent cart, checkout, live order tracking, inventory visibility, and payment activity.

PostgreSQL is the source of truth for each service. Kafka carries the workflow between services, Redis handles catalog caching, and LocalStack provides the local S3 API used for receipts.

## Architecture

```text
Browser :3000
    │
    └── Nginx storefront
          ├── /api/orders    ──> Order service :8081 ──> orders database
          ├── /api/products  ──> Inventory service :8082 ──> inventory database + Redis
          └── /api/payments  ──> Payment service :8083 ──> payments database + S3

Order service ── orders.created ──> Inventory service
Inventory service ── inventory.reserved ──> Payment service + Order service
Payment service ── payments.completed ──> Order service
Order service ── orders.completed ──> Inventory service
```

The four topics carry typed events with unique event IDs. Consumers keep a `processed_events` record in their own database, so replaying a message does not reserve stock or capture payment twice. Failed listener work is retried with exponential backoff. Inventory and order aggregates also use optimistic locking to guard concurrent writes.

## Run it

You need Docker Desktop with Compose v2. From the repository root:

```bash
docker compose up --build
```

The first build downloads Maven dependencies and can take a few minutes. Once the containers are healthy, open [http://localhost:3000](http://localhost:3000).

The local ports are:

| Component | Address |
| --- | --- |
| Storefront | `http://localhost:3000` |
| Order service | `http://localhost:8081` |
| Inventory service | `http://localhost:8082` |
| Payment service | `http://localhost:8083` |
| PostgreSQL, Kafka, Redis, LocalStack | Internal Compose network |

Stop the stack with `docker compose down`. Add `-v` if you also want to remove the local database, cache, and S3 volumes.

## Try the workflow

1. Add a few products to the bag and check out with a normal email address.
2. Open **Orders** to watch the order move through inventory and payment to `COMPLETED`.
3. Open **Operations** to see the reduced stock and captured payment.
4. Use an address containing `decline`, such as `decline@example.com`, to exercise the payment-failure and stock-release path.
5. Add more units than are in stock to exercise inventory rejection.

The payment processor is intentionally simulated; no card details are requested or stored.

## API surface

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/orders` | List orders |
| `POST` | `/api/orders` | Place an order |
| `GET` | `/api/orders/{id}` | Get one order |
| `POST` | `/api/orders/{id}/cancel` | Cancel an in-flight order |
| `GET` | `/api/orders/metrics/summary` | Order counts by state |
| `GET` | `/api/products` | List the catalog |
| `POST` | `/api/products` | Add a catalog item |
| `GET` | `/api/products/{sku}` | Get one product |
| `PATCH` | `/api/products/{sku}/stock` | Adjust stock |
| `GET` | `/api/inventory/reservations` | List inventory reservations |
| `GET` | `/api/payments` | List payments |
| `GET` | `/api/payments/{id}` | Get one payment |
| `POST` | `/api/payments/{id}/refund` | Refund a captured payment |
| `GET` | `/api/payments/metrics/summary` | Payment and revenue summary |

Health endpoints are available at `/actuator/health` on each service.

## Tests and delivery

Run the full suite with Java 21 and Maven 3.9 or newer:

```bash
mvn verify
```

The suite contains 12 Spring integration tests across checkout validation, totals, API errors, stock adjustment, optimistic versions, reservation idempotency, insufficient inventory, capture deduplication, declines, refunds, and revenue reporting.

GitHub Actions runs the tests and builds a container for every service and the storefront on each pull request. The service Dockerfile uses a reproducible multi-stage Maven build, and all runtime containers use non-root application users where the base image permits it.

## Configuration

The checked-in defaults are meant for local development. Copy `.env.example` to `.env` if you want to change the database or AWS test credentials. Service configuration can also be overridden directly through the environment variables used in `compose.yml`.

Receipt storage is disabled when a payment service is started on its own. Compose enables it and points the AWS SDK at LocalStack. In an AWS environment, remove `AWS_ENDPOINT`, provide the workload with an IAM role, and set `RECEIPTS_ENABLED=true`.

## What I would do differently in production

I would replace direct database-write-plus-Kafka-publish operations with a transactional outbox and CDC publisher so an application crash cannot leave state committed without its event. I would also add Schema Registry compatibility checks, dead-letter topic tooling, and trace IDs propagated through HTTP and Kafka.

The public edge would sit behind an authenticated API gateway with rate limits and request IDs. Service-to-service traffic would use workload identity and encryption, secrets would live in a managed secret store, and each database would have its own credentials and network policy. Real payments would be delegated to a PCI-compliant provider through tokenized payment methods and signed webhooks.

For operations, I would add OpenTelemetry traces, Prometheus metrics and SLO-based alerts, managed PostgreSQL/Kafka/Redis, automated backups with restore drills, and multi-AZ deployment. Load tests would set partition counts and connection-pool sizes from evidence. Finally, I would use canary delivery, database migration gates, and chaos tests around duplicate, delayed, and out-of-order events before calling the workflow production-ready.
