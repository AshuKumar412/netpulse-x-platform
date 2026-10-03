# NetPulse X — Self-Healing Network Infrastructure & Adaptive Traffic Routing Platform

NetPulse X is a high-performance, resilient network infrastructure platform built with modern distributed systems design principles.

---

## Phase Summary & Status

- **Phase 1: Modular Foundation & Architecture** — `COMPLETE`
- **Phase 2: Network Topology Graph Engine & Simulation Lifecycle** — `COMPLETE`
- **Phase 3: Real-Time Telemetry & Heartbeat Monitoring Engine** — `COMPLETE`
- **Phase 4: Adaptive Traffic Routing & Load Balancing** — `COMPLETE`
- **Phase 5: Automatic Failover / Self-Healing** — `COMPLETE`
- **Phase 6: Chaos Engineering + Email OTP Verification** — `COMPLETE`
- **Phase 7: OS Process Management + CPU Scheduling Engine** — `COMPLETE`
- **Phase 8: What-If Infrastructure Simulator + Decision Explainability** — `COMPLETE`
- **Phase 9: Predictive Congestion Detection & ML Intelligence** — `COMPLETE`

---

## Phase 9: Predictive Congestion Detection & ML Intelligence

Phase 9 introduces machine learning and predictive intelligence to NetPulse X, transforming the platform to `PREDICT → PREVENT → OPTIMIZE → RECOVER → PROVE`:

- **Real Telemetry Feature Engineering**:
  - Extracts real historical telemetry time series (`TelemetryRecordRepository`).
  - Calculates 1st derivatives (Rate of Change $\Delta/\Delta t$ per second for CPU, Memory, Latency, Packet Loss, and Connection Growth).
  - Computes 5-point moving rolling averages for smoothing.
  - Generates 16-dimensional feature vectors (`PredictiveFeatureVector`).
- **Deterministic ML Algorithms**:
  - `MULTINOMIAL_LOGISTIC_REGRESSION`: Softmax probability distribution with L2 regularized gradient descent.
  - `DECISION_TREE_CLASSIFIER`: Deterministic CART algorithm optimizing Gini impurity splits with MDI feature importance.
- **Model Lifecycle & Versioning**:
  - Full model metadata tracking: `modelVersion`, `accuracy`, `precisionScore`, `recallScore`, `f1Score`, and `confusionMatrix`.
  - Serialized weight storage in PostgreSQL.
- **5-Minute Congestion Forecasting & Explainability**:
  - Classifies nodes into `NORMAL`, `WARNING`, `CONGESTION_LIKELY`, `CONGESTED`, `INSUFFICIENT_DATA`, `MODEL_UNAVAILABLE`.
  - Transparent "Why?" explainability breakdown attributing decisions to leading metric trends and threshold proximities.
- **Historical Outcome Verification**:
  - Automatically evaluates past 5-minute predictions against subsequently recorded real telemetry.
- **Cross-Phase Integrations**:
  - Phase 4 Predictive Routing: Optional predictive congestion penalty weight applied to candidate scores.
  - Phase 5 Predictive Failover: Early warning advisory alerts.
  - Phase 8 What-If: Isolated hypothetical prediction evaluation on cloned scenario state.

---

## Phase 8: What-If Infrastructure Simulator & Explainability

Phase 8 enables operators to simulate complex failure scenarios, traffic surges, resource spikes, and network partitions on isolated, read-only system snapshots with complete decision explainability.

---

## Phase 7: OS Process Management & CPU Scheduling Engine

Phase 7 simulates real operating system process lifecycle and multi-core CPU scheduling (FCFS, SJF, SRTF, Priority, Round Robin, Multilevel Queue, and CFS-inspired scheduling) across infrastructure nodes.

---

## Phase 6: Chaos Engineering & Email OTP Verification

Phase 6 provides 7 controlled failure injection experiments (Node Failure, High Latency, Packet Loss, CPU Spike, Memory Spike, Traffic Spike, Network Partition) along with secure SMTP email OTP registration and welcome notifications.

---

## Phase 5: Self-Healing Failover & Recovery Verification

Phase 5 delivers automatic failure detection, heartbeat timeout classification, traffic rerouting, node recovery orchestration, and post-recovery health verification.

---

## Phase 4: Adaptive Traffic Routing Engine Overview

Phase 4 delivers legitimate, explainable, multi-strategy traffic dispatch and load balancing across live network nodes based on dynamic telemetry and operational health:

- **Routing Strategies Implemented (`com.netpulse.routing.strategy`)**:
  - `ROUND_ROBIN`: Deterministic atomic cursor modulo active candidate set.
  - `LEAST_CONNECTIONS`: Selects node with lowest active connection count with deterministic tie-breaking.
  - `LEAST_LOAD`: Composite load evaluation based on normalized CPU (40%), Memory (35%), and Connection Load (25%).
  - `LATENCY_AWARE`: Dispatches traffic to the node exhibiting minimum telemetry round-trip latency.
  - `WEIGHTED`: Proportional deficit weighted round-robin based on node capacity tier (`10Gbps`: 10, `1Gbps`: 5, `100Mbps`: 1).
  - `ADAPTIVE`: Multi-factor composite cost evaluation formula:
    $$\text{Score} = (0.30 \cdot \text{CPU}) + (0.25 \cdot \text{LatencyNorm}) + (0.20 \cdot \text{RAM}) + (0.15 \cdot \text{LossNorm}) + (0.10 \cdot \text{ConnsNorm}) + \text{CapPenalty} + \text{HealthPenalty}$$
- **Routing Decision & Audit Trail (`RoutingDecisionRecord`)**:
  - Every dispatch produces an immutable JPA audit record with unique `requestId`, chosen node, score breakdown for all candidates, and human-readable `decisionReason`.
  - Persisted in PostgreSQL with configurable retention pruning and published over STOMP WebSocket `/topic/routing`.
- **Active Candidate Filtering (`RoutingCandidateService`)**:
  - Automatically excludes offline/failed nodes (`OFFLINE`, `FAILED`), unreachable/suspected nodes (`UNREACHABLE`), and degraded/warning nodes if healthy alternatives exist.
- **REST & WebSocket Endpoints**:
  - `POST /api/routing/decide`: Execute deterministic routing decision for a simulated request payload.
  - `POST /api/routing/request`: Dispatch synthetic network request and record audit log.
  - `GET /api/routing/status`: Retrieve active strategy, strategy descriptions, and eligible candidate pool.
  - `GET /api/routing/strategy`: Get active platform routing strategy.
  - `PUT /api/routing/strategy`: Dynamically update active routing strategy (persisted to Redis & PostgreSQL).
  - `GET /api/routing/config`: Retrieve strategy configuration and weights.
  - `GET /api/routing/candidates`: Inspect current eligible routing candidates with their load scores.
  - `GET /api/routing/history`: Paginated historical routing decisions audit log (`page`, `size`, `strategy`).
  - WebSocket Topic: `/topic/routing`.
- **Interactive Routing Console (`/routing`)**:
  - Dynamic strategy selector with instant server synchronization.
  - Interactive "Simulate Request Routing" dispatch with live execution telemetry.
  - Real-time decision explainability visualizer showing selected node and candidate scoring breakdown.
  - Live candidate evaluation table ranking nodes by current health, CPU, latency, connections, and calculated strategy score.
  - Historical decisions audit log with status badges and candidate breakdown inspector.

---

## Phase 3: Real-Time Telemetry & Heartbeat Monitoring Overview

Phase 3 introduces legitimate infrastructure runtime monitoring, deterministic telemetry generation, heartbeat liveness tracking, and multi-metric health evaluation:

- **Telemetry Model & JPA Entity (`TelemetryRecord`)**: Indexed by `nodeId`, `timestamp`, and `(nodeId, timestamp DESC)`. Tracks CPU Utilization (0-100%), Memory Utilization (0-100%), Latency (ms), Packet Loss (0-100%), Active Connections, and Error Rate (0-100%).
- **Deterministic Telemetry Engine (`TelemetryEngine`)**: Mathematical simulation based on node capacity factor, degree connections, operational status, and deterministic harmonic waves. Zero `Math.random()`.
- **Heartbeat & Liveness Engine (`HeartbeatService`)**:
  - Server-side timestamp tracking with configurable timeout (`heartbeat.timeout-seconds: 10`) and suspected threshold (`heartbeat.suspected-threshold-seconds: 5`).
  - Liveness classifications: `ALIVE`, `SUSPECTED`, `UNREACHABLE`.
  - Controlled operator heartbeat suppression toggle (`POST /api/heartbeat/nodes/{nodeId}/suppress`) to test timeout degradation without killing infrastructure or executing failover.
- **Node Health Evaluation Engine (`NodeHealthService`)**:
  - Rule-based multi-metric health engine (`HEALTHY`, `WARNING`, `DEGRADED`, `FAILED`, `OFFLINE`).
  - Generates human-readable primary evaluation reasons.
- **Monitoring Scheduler & Storage Strategy**:
  - PostgreSQL: Stores persistent telemetry history with bounded history retention pruning.
  - Redis: Caches latest telemetry hashes and heartbeat timestamps for ultra-low latency reads.
  - Scheduled cadence: Generates telemetry, updates heartbeat/health, and broadcasts STOMP updates.

---

## System Architecture

```
                      +-----------------------------+
                      |     React Client (SPA)      |
                      |   (Vite + Tailwind CSS)     |
                      +--------------+--------------+
                                     |
                         HTTP / REST | WebSocket (STOMP)
                                     v
                      +-----------------------------+
                      |   Spring Boot 3.3 Backend   |
                      |        (Modular Core)       |
                      +-------+-------------+-------+
                              |             |
                     JPA/JDBC |             | Lettuce Driver
                              v             v
             +--------------------+     +-------------------+
             | PostgreSQL 16 (DB) |     |  Redis 7 (Cache)  |
             +--------------------+     +-------------------+
```

---

## Technology Stack

| Layer | Component | Version / Specification |
|---|---|---|
| **Runtime** | Java | 25 LTS (Modular VM) |
| **Framework** | Spring Boot | 3.3.4 |
| **Security** | Spring Security & JJWT | 0.12.6 (HMAC-SHA) |
| **Persistence** | Spring Data JPA / Hibernate | 6.5.3 (PostgreSQL dialect) |
| **Database** | PostgreSQL | 16-alpine |
| **Cache Store** | Redis & Spring Data Redis | 7-alpine / Lettuce |
| **Real-Time** | Spring WebSocket & STOMP | RFC 6455 / STOMP 1.2 |
| **API Docs** | SpringDoc OpenAPI Starter UI | 2.6.0 (OpenAPI 3.0.1) |
| **Diagnostics** | Spring Boot Actuator | Health, Info, Metrics |
| **Frontend UI** | React / Vite / Tailwind | React 18, Vite 8, Tailwind CSS |
| **Containerization**| Docker Compose | Multi-container composition |
| **Automated Tests** | JUnit 5 / Mockito | 86 Automated Tests (100% Passing) |

---

## Directory Structure

```
NETPULSE X/
├── backend/
│   ├── src/main/java/com/netpulse/
│   │   ├── auth/            # Authentication & Registration Services
│   │   ├── common/          # Standardized API response models
│   │   ├── config/          # OpenAPI, Redis, WebSocket configs
│   │   ├── exception/       # Centralized GlobalExceptionHandler
│   │   ├── health/          # Health evaluation models & service
│   │   ├── heartbeat/       # Heartbeat tracking & suppression service
│   │   ├── monitoring/      # MonitoringScheduler & engine control
│   │   ├── node/            # NetworkNode entity, DTOs, Service, Controller
│   │   ├── routing/         # Routing strategies, candidate filter, RoutingService, RoutingController
│   │   ├── security/        # JWT filter, Token provider, SecurityConfig, RBAC
│   │   ├── telemetry/       # TelemetryRecord entity, TelemetryEngine, TelemetryService
│   │   ├── topology/        # NetworkLink, TopologyEngine, TopologyService
│   │   ├── user/            # User entity, DTOs, Service, Controller
│   │   └── websocket/       # WebSocket test controllers & endpoints
│   ├── src/main/resources/
│   │   └── application.yml  # Environment-backed configuration
│   ├── src/test/java/       # Comprehensive JUnit 5 & Mockito test suite (86 tests)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # Common UI (Buttons, Inputs, Cards, Badges, Modals, Layout, Routing)
│   │   ├── context/         # AuthContext, WebSocketContext
│   │   ├── pages/           # Dashboard, Nodes, Topology, Routing, Login, Register
│   │   ├── services/        # Axios API client, authService, nodeService, telemetryService, routingService
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml       # Production/local multi-container setup
├── .env.example             # Configuration template (no credentials)
└── .gitignore               # Secrets, build artifacts, node_modules ignored
```

---

## Quick Start & Verification

### 1. Environment Configuration

Copy `.env.example` to `.env` and set your local credentials:
```bash
cp .env.example .env
```

### 2. Run Backend Tests
```bash
cd backend
mvn test
```

### 3. Run Frontend
```bash
cd frontend
npm install
npm run build
npm run dev
```

### 4. Run Full Stack via Docker Compose
```bash
docker compose up --build
```

---

## Security & Operational Policy

- **Zero Fake / Mock Operational Data**: NetPulse X strictly relies on real backend-persisted records and deterministic telemetry models. When no records exist, the UI renders clean, professional empty states rather than fake statistics or placeholder charts.
- **Deterministic Routing**: Every load balancing decision is explainable, repeatable, and audited with complete scoring breakdowns.
- **Credential Protection**: Passwords are encrypted using BCrypt. Secrets are loaded strictly through environment variables and never exposed in client bundles or version control.
- **Safe Error Reporting**: Internal database errors and stack traces are captured on the server and returned to clients as sanitized, user-friendly error codes.
