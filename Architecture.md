# distops — Implementation & Architecture Document

## 1. Overview

**distops** (distribution operations) is a comprehensive operations and maintenance platform for managing multiple [CNCF Distribution](https://github.com/distribution/distribution) (Docker/OCI Registry) instances. It is a Java 21 web application built on the lightweight **Solon** micro-framework, providing:

- Multi-instance Docker Registry management (start, stop, restart, enable, disable)
- Configurable private registries and proxy (pull-through cache) registries
- An embedded authentication server supporting three auth backends: **None**, **htpasswd** (Basic Auth), and **Token** (JWT-based)
- Dynamic JWKS (JSON Web Key Set) key pair generation for EC, RSA, and HMAC algorithms
- Dynamic read-only account creation with fine-grained ACL (glob/regex-based access control)
- Webhook-based event ingestion for tracking images, tags, and manifests across registries
- A web-based administration dashboard with internationalization (English / 简体中文)

**Group ID / Artifact:** `com.dyrnq:distops` v1.0.0  
**Main class:** `com.dyrnq.distops.WebApp`  
**Default port:** `12680`  
**Build tool:** Maven (with Maven Wrapper)

---

## 2. Technology Stack

| Layer                   | Technology                                                                        |
|-------------------------|-----------------------------------------------------------------------------------|
| **Language**            | Java 21                                                                           |
| **Framework**           | [Solon](https://solon.noear.org/) 3.10.4 (lightweight alternative to Spring Boot) |
| **ORM**                 | [Wood](https://github.com/noear/wood) (lightweight ORM by Noear)                  |
| **Templating**          | [FreeMarker](https://freemarker.apache.org/) for views and config generation      |
| **Database**            | H2 (embedded, default), SQLite (embedded), MySQL, PostgreSQL                      |
| **Migration**           | [Flyway](https://flywaydb.org/) 12.x                                              |
| **Connection Pool**     | [HikariCP](https://github.com/brettwooldridge/HikariCP) 7.x                       |
| **JWT (App)**           | [JJWT](https://github.com/jwtk/jjwt) 0.13.0 (io.jsonwebtoken)                     |
| **JWT (Registry Auth)** | [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt) 10.9           |
| **Password**            | [password4j](https://github.com/Password4j/password4j) 1.8.4 + Hutool BCrypt      |
| **CLI**                 | [Picocli](https://picocli.info/) 4.7.7                                            |
| **ID Gen**              | [TSID Creator](https://github.com/f4b6a3/tsid-creator) 5.2.6 (Snowflake-like)     |
| **YAML**                | [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/) + Jackson YAML            |
| **Crypto**              | [Bouncy Castle](https://www.bouncycastle.org/) 1.84 (bcprov, bcpkix)              |
| **HTTP Client**         | [OkHttp](https://square.github.io/okhttp/) 5.3.2                                  |
| **Build Info**          | `build-info-maven-plugin` (embeds git revision and build datetime)                |
| **Container**           | s6-overlay (process supervisor + init), supervisord (registry process mgmt)       |
| **Frontend**            | [Layui](https://layui.dev/) 2.x + [ECharts](https://echarts.apache.org/)          |

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
                          ┌─────────────────────┐
                          │   Admin Web UI      │
                          │  (Layui + ECharts)  │
                          └──────────┬──────────┘
                                     │ HTTP
                          ┌──────────▼──────────┐
                          │   Solon Web Server  │
                          │     (port 12680)     │
                          ├─────────────────────┤
                          │  Filters & Interceptors
                          │  ├─ AppFilter        │
                          │  ├─ I18nFilter       │
                          │  ├─ JwtInterceptor   │
                          │  └─ AppExceptionFilter│
                          ├─────────────────────┤
                          │  Controllers         │
                          │  ├─ /admin (UI pages) │
                          │  ├─ /api/* (REST API) │
                          │  ├─ /auth (Registry Auth) │
                          │  ├─ /event (Webhooks)│
                          │  └─ /token (Login)   │
                          ├─────────────────────┤
                          │  Services            │
                          │  ├─ InstService      │
                          │  ├─ AuthService      │
                          │  ├─ BusinessLogic    │
                          │  └─ CronTab          │
                          ├─────────────────────┤
                          │  DSO / Mappers       │
                          │  (Wood ORM)          │
                          └──────────┬──────────┘
                                     │
                          ┌──────────▼──────────┐
                          │   Database          │
                          │  (H2/SQLite/MySQL/  │
                          │   PostgreSQL)        │
                          └─────────────────────┘

    Supervisor Control
    ───────────────────
    distops ──supervisorctl──► supervisord
                                  │
                                  ├── registry-default (port 5000)
                                  ├── registry-staging (port 5001)
                                  └── ...
```

### 3.2 Container Architecture

When deployed in Docker, the container uses **s6-overlay** as the process supervisor, which manages three services:

```
s6-overlay (/init)
  ├── distops (longrun)    — Java application JAR
  ├── redis (longrun)      — Redis for caching/sessions
  └── supervisor (longrun) — supervisord managing registry processes
       ├── registry-default
       ├── registry-*
       └── ...
```

Key container components:
- **CNCF Distribution** (registry binary) — the actual Docker/OCI registry server
- **skopeo** — image copying and inspection
- **regctl** (regclient) — registry API client
- **nginx** — reverse proxy (available but not configured by default)
- **Redis** — append-only persistence mode

---

## 4. Request Processing Pipeline

### 4.1 Filter Chain

Every HTTP request passes through this filter/interceptor pipeline:

```
Request
  │
  ├─[1] AppFilter (global)
  │     Sets context attributes: cookie names, project name,
  │     frontend config, base URL, version info, locale
  │
  ├─[2] I18nFilter (global)
  │     Loads all i18n message properties as context attributes
  │
  ├─[3] JwtInterceptor (path-specific: /admin/*, /api/*)
  │     Validates JWT cookie for authenticated routes
  │
  ├─[4] Controller
  │
  └─[5] AppExceptionFilter (global, index=0 — highest priority)
        Catches unhandled exceptions, renders error responses
```

### 4.2 Authentication Flow

**Admin UI login:**
1. POST `/token/getToken` with Base64-encoded `name` and `pass`
2. `BusinessLogic.login()` decodes credentials, validates BCrypt hash
3. A JWT (JJWT HS512) is issued with subject=username, 10-day expiration
4. The JWT is stored in a cookie (name configurable, default `TOKEN`)
5. `JwtInterceptor` reads this cookie on every `/admin/*` and `/api/*` request

**Docker Registry token auth:**
1. Docker client makes an unauthenticated request → receives `401` with `WWW-Authenticate`
2. Docker client requests a token from `POST /auth/token/` with Basic Auth credentials
3. `AuthController` validates credentials against the `account` table (BCrypt)
4. ACL rules are evaluated (glob/regex matching) to determine granted actions
5. A JWT is signed using the instance-configured key (EC, RSA, or HMAC via Nimbus JOSE)
6. The JWT is returned with scope-limited access to the requested repositories

### 4.3 I18N / Internationalization

- Messages are defined in `src/main/resources/i18n/messages*.properties` and `src/main/native2ascii/*.msg`
- The locale is resolved via a cookie (`SOLON.LOCALE`) through `LocaleResolverCookie`
- `I18nFilter` loads all message key-value pairs into context attributes
- Frontend JavaScript reads i18n strings from context attributes
- The locale can be switched via `POST /token/i18n`

---

## 5. Core Domain Model

### 5.1 Entity Relationship

```
Inst (Registry Instance)
  │
  ├── 1:N ── Repo (Image Repository)
  │             │
  │             ├── 1:N ── Artifact (Tag → Manifest reference)
  │             │             │
  │             │             └── N:1 ── Manifest (OCI manifest metadata)
  │             │                           │
  │             │                           └── self-referencing: parent_digest
  │             │
  │             └── 1:N ── Manifest (directly)
  │
  ├── 1:N ── Account (Registry auth user)
  │
  └── (via templates) ── GlobalConfig
```

### 5.2 Database Tables

| Table           | Description                                                              |
|-----------------|--------------------------------------------------------------------------|
| `inst`          | Registry instance configuration (port, auth, proxy, env)                 |
| `repo`          | Image repositories per instance                                          |
| `manifest`      | OCI/Docker manifest metadata (digest, media type, platform, annotations) |
| `artifact`      | Image tags referencing manifests                                         |
| `account`       | Docker Registry user accounts with BCrypt hashes and ACL                 |
| `user`          | System admin users (for the web UI)                                      |
| `global_config` | Key-value store for runtime configuration templates                      |

### 5.3 Database Views

| View                          | Purpose                                                        |
|-------------------------------|----------------------------------------------------------------|
| `artifact_manifest_view`      | Flattened JOIN of artifact + manifest for tag-centric queries  |
| `artifact_manifest_oci_view`  | Multi-arch manifest list JOIN (OCI index → child manifests)    |

### 5.4 Inst — The Central Entity

The `inst` table is the most complex entity, containing all configuration for a single registry instance:

**Core fields:** `name`, `port`, `log_level`, `os_arch`, `extra_yaml`, `autostart`, `autorestart`, `enabled`, `pid`, `env`

**Auth fields (`token` auth type):**
- `auth` — auth type: `none`, `htpasswd`, or `token`
- `auth_realm` — the realm displayed to clients (e.g., `http://host:port/auth`)
- `auth_service` — the service identifier
- `auth_issuer` — the JWT issuer
- `auth_private_key` — private key (PEM) for JWT signing
- `auth_public_key` — public key (PEM)
- `auth_jwks_json` — JWKS JSON document
- `auth_key_type` — `EC`, `RSA`, or `HMAC`
- `auth_key_alg` — e.g., `ES256`, `RS256`, `HS256`

**Proxy fields (pull-through cache):**
- `proxy_remoteurl` — upstream registry URL
- `proxy_username` / `proxy_password` — upstream credentials
- `proxy_ttl` — cache TTL (e.g., `168h`)

---

## 6. Registry Lifecycle Management

### 6.1 How Instances Run

Each registry instance runs as a **supervisord-managed process**. The lifecycle is:

```
enable()                    disable()
  │                           │
  ├─ Generate config.yml     ├─ supervisorctl stop
  ├─ Generate supervisor.ini  ├─ Delete supervisor.ini
  ├─ Write JWKS file         ├─ supervisorctl update
  ├─ Write htpasswd file     └─ Set enabled=0
  ├─ supervisorctl update
  └─ Set enabled=1
```

### 6.2 Configuration File Generation

Registry configurations are generated from **FreeMarker templates** stored in the `global_config` table:

**Template: `registry/config.yml.tpl`**

The template generates the CNCF Distribution `config.yml` with:
- **Storage:** filesystem backend at `{home}/registry/{instName}/data`, upload purging enabled (168h age, 24h interval)
- **Auth:** configured per instance type:
  - *token:* JWKS-based, realm pointing to the distops auth endpoint
  - *htpasswd:* Basic auth with htpasswd file
  - *none/silly:* trivial auth for development
- **HTTP:** CORS-enabled for Docker clients, HTTP/2 support
- **Notifications:** webhook events sent to `http://127.0.0.1:{distopsPort}/event/{instName}`
- **Proxy:** injected if the instance has proxy configuration (remote URL, credentials, TTL)

**Template: `supervisor/registry.ini.tpl`**

Generates a supervisord program definition:
- Program name: `registry-{instName}`
- Command: `/usr/local/bin/registry serve {configPath}`
- Runs as user `dist`
- Autostart, autorestart, graceful stop (TERM, 30s timeout)

### 6.3 Startup Bootstrapping

`RegistrySuperGen` is a `@Component` with an `@Init` method that runs at application startup:
1. Seeds the `global_config` table with default templates (if they don't exist):
   - `registry_config_yml_template` (ID 10000)
   - `registry_supervisor_template` (ID 10001)
2. Iterates all `Inst` records — if `enabled == 1`, calls `instService.enable()` to regenerate config and restart

### 6.4 Health Monitoring

`CronTab` runs every **3 seconds** (`@Scheduled(fixedRate = 3000)`):
1. Queries all enabled `Inst` records
2. Runs `supervisorctl status registry-{name}` for each
3. If RUNNING, queries the PID via `supervisorctl pid` and updates the database
4. If stopped, sets `pid = 0`

---

## 7. Authentication Server

### 7.1 Auth Types

| Type       | Description                                                            |
|------------|------------------------------------------------------------------------|
| `none`     | No authentication (development only)                                   |
| `htpasswd` | Basic HTTP authentication backed by htpasswd file                      |
| `token`    | JWT Bearer token authentication (Docker v2)                            |
| `silly`    | The silly authentication provider is only appropriate for development. |

### 7.2 Token Authentication Flow

```
Docker Client          distops (/auth)        Account DB      Key Material
     │                       │                     │                │
     │── GET /v2/ ──────────►│                     │                │
     │◄─ 401 WWW-Authenticate│                     │                │
     │                       │                     │                │
     │── POST /auth/token ──►│                     │                │
     │   (Basic: user:pass)  │                     │                │
     │                       │── lookup account ──►│                │
     │                       │◄── account + ACL ───│                │
     │                       │── BCrypt check      │                │
     │                       │── evaluate ACL rules│                │
     │                       │── load key ────────────────────────►│
     │                       │◄── signer (EC/RSA/HMAC) ────────────│
     │                       │── create JWT        │                │
     │◄── { token, expires }─│                     │                │
     │                       │                     │                │
     │── GET /v2/ (Bearer)──►│  [Registry validates JWT signature independently]  │
```

### 7.3 JWT Signing Key Types

Three asymmetric/asymmetric signing backends are supported, all implemented via Nimbus JOSE+JWT:

| Key Type | Algorithms            | Implementation Class       | JWS Algorithm |
|----------|-----------------------|----------------------------|----------------|
| EC       | ES256, ES384, ES512   | `ECTokenServiceImpl`       | ECDSA         |
| RSA      | RS256, RS384, RS512, PS256, PS384, PS512 | `RSATokenServiceImpl` | RSA / RSA-PSS |
| HMAC     | HS256, HS384, HS512   | `HMACTokenServiceImpl`     | HMAC-SHA      |

### 7.4 Key Pair Generation

`KeyPairManager` orchestrates key generation via strategy pattern:

- **EC:** Uses BouncyCastle `ECKeyPairGenerator` with NIST curves (`secp256r1`/`secp384r1`/`secp521r1`). JWKS includes `kty: "EC"`, `crv`, `x`, `y`. `kid` is computed via JWK Thumbprint (RFC 7638, SHA-256 truncated to 43 chars).
- **RSA:** Uses BouncyCastle `RSAKeyPairGenerator` with key sizes 2048/3072/4096 bits. JWKS includes `kty: "RSA"`, `n`, `e`.
- **HMAC:** Uses Java `KeyGenerator` (`HmacSHA256`/`HmacSHA384`/`HmacSHA512`). JWKS includes `kty: "oct"`, `k` (Base64-encoded secret).

### 7.5 ACL (Access Control List)

Each `Account` has an `acl` field containing a JSON array of rules:

```json
[
  {
    "match": {
      "type": "repository",
      "name": "${account}/*",
      "account": "myuser"
    },
    "actions": ["pull", "push"],
    "comment": "Full access to own repos"
  },
  {
    "match": {
      "type": "repository",
      "name": "public/*"
    },
    "actions": ["pull"],
    "comment": "Read-only access to public repos"
  }
]
```

**Match evaluation (`AuthService`):**
- **`type`:** resource type (`repository`, `registry`, `namespace`)
- **`name`:** supports exact match, glob patterns (`*`, `?`, `[chars]`), and regex (`/pattern/`)
- **`account`:** specific account name or pattern
- **`${account}` variable expansion:** `name` patterns can reference the authenticating username
- **`ip`** and **`service`:** stubbed for future implementation

---

## 8. Event Processing (Webhook Ingestion)

### 8.1 Overview

CNCF Distribution can be configured to send webhook notifications on push/pull events. The `EventController` at `/event/{instName}` receives these notifications and populates the database.

### 8.2 Processing Pipeline

```
Registry Push/Pull Event
     │
     ▼
POST /event/{instName}
     │
     ├── Save raw JSON to {home}/tmp/event/{instName}/event_{tsid}.json
     │
     └── processEvent(json, instName)
           │
           ├── For each event in events[]:
           │     │
           │     ├── Lookup Inst by name
           │     ├── Extract: repository, digest, tag, size, mediaType, timestamp
           │     │
           │     ├── Create/update Repo record
           │     ├── Create/update Manifest record
           │     ├── If tag present: create/update Artifact record
           │     │
           │     └── If mediaType is manifest list (multi-arch):
           │           └── processReferences()
           │                 │
           │                 └── For each reference:
           │                       ├── Skip attestation manifests
           │                       ├── Extract platform (os, arch, variant)
           │                       ├── Create/update child Manifest (parent_digest link)
           │                       └── Use org.opencontainers.image.created annotation
```

### 8.3 Registry Notification Configuration

The generated `config.yml` template includes:

```yaml
notifications:
  events:
    includereferences: true
  endpoints:
    - url: http://127.0.0.1:12680/event/${inst.name}
      timeout: 1s
      threshold: 10
      backoff: 1s
```

---

## 9. Database Architecture

### 9.1 Multi-Database Support

`DataSourceEmbed` supports four database backends, configured via `app.yml`:

| Backend     | Configuration Key       | Default JDBC URL                                    |
|-------------|-------------------------|-----------------------------------------------------|
| H2          | `spring.active: h2`     | `jdbc:h2:file:{home}/db;MODE=MySQL;DB_CLOSE_DELAY=-1` |
| SQLite      | `spring.active: sqlite` | `jdbc:sqlite:{home}/db`                             |
| MySQL       | `spring.active: mysql`  | Configured via `spring.datasource.url`              |
| PostgreSQL  | `spring.active: postgresql` | Configured via `spring.datasource.url`          |

### 9.2 H2 Version Migration

A notable feature is the **automatic H2 database format migration**:
- On startup, `H2FormatVersionChecker` reads the database file header to detect format version
- If upgrading from H2 2.1.214 to 2.2.224: exports via `Script` tool, deletes old DB, imports via `RunScript`
- If downgrading: same process in reverse
- Both JARs are downloaded from a Tencent mirror at runtime
- Rollback is attempted on failure

### 9.3 Flyway Migrations

Flyway migrations are stored per database type:
- `classpath:db/migration/h2/`
- `classpath:db/migration/sqlite/`
- `classpath:db/migration/mysql/`
- `classpath:db/migration/postgresql/`

Configuration: `baselineOnMigrate=true`, `cleanDisabled=true`, `mixed=true`. Can be disabled with `spring.flyway.enabled: false`.

### 9.4 Seed Data

The initial migration (`V20260218__001.sql`) seeds:
- One admin user (`admin@hello.com` / bcrypt hash)
- One default registry instance (name `default`, port `5000`, token auth, issuer `docker-auth-server`)
- Three registry accounts:
  - `admin` — full access (all repositories, all actions)
  - `test` — full access
  - `read` — pull-only access

---

## 10. REST API Design

### 10.1 API Structure

All API endpoints are under `/api/` and extend `ApiController`. Responses use Solon's `Result<T>` pattern with pagination via `PageResult<T>`.

### 10.2 Endpoint Summary

| Resource   | Base URL       | Operations                                              |
|------------|----------------|---------------------------------------------------------|
| Inst       | `/api/inst`    | CRUD, start, stop, restart, enable, disable, config, keypair |
| Account    | `/api/account` | CRUD, enable/disable, ACL get/update                    |
| Repo       | `/api/repo`    | CRUD                                                    |
| Artifact   | `/api/artifact`| Query, get, del, add, update, byRepo, byManifest, queryOciByManifest |
| Manifest   | `/api/manifest`| Query, get, getByDigest, del, children, byRepo, byArch  |
| User       | `/api/user`    | CRUD, changePass                                        |

### 10.3 Key Instance Operations

- **`enable`** — generates config files from FreeMarker templates, writes JWKS/htpasswd, registers with supervisord, starts the registry process
- **`disable`** — stops the supervisord service, removes config, sets enabled=0
- **`restart`** — calls `enable()` then `supervisorctl restart`
- **`keypair`** — generates EC/RSA/HMAC key pairs and stores them on the instance record

### 10.4 Account ↔ htpasswd Synchronization

When an account is created, updated, enabled, or disabled, the system regenerates the htpasswd file for the associated instance by:
1. Querying all enabled accounts for that instance
2. Writing BCrypt-hashed username:password entries to the htpasswd file
3. The registry picks up changes on next request (htpasswd is read on each auth attempt)

---

## 11. Admin Web UI

### 11.1 Structure

The admin interface is served as FreeMarker HTML templates from `WEB-INF/view/admin/`:

| Page                | Template            | Function                                          |
|---------------------|---------------------|----------------------------------------------------|
| Dashboard (no auth) | `index-noauth.html` | Landing page when not logged in                    |
| Dashboard (auth)    | `index-auth.html`   | Dashboard with charts/health info after login      |
| Instances           | `inst.html`         | List, manage registry instances                    |
| Instance Config     | `instConfig.html`   | View generated config files (YAML, INI, JWKS, htpasswd) |
| Instance Edit       | `instEdit.html`     | Create/edit instance settings                      |
| Repositories        | `repo.html`         | List tracked image repositories                    |
| Artifacts           | `artifact.html`     | List image tags/artifacts                          |
| OCI Multi-arch      | `oci.html`          | View multi-arch manifest list children             |
| Accounts            | `account.html`      | Manage Docker Registry user accounts               |
| Users               | `user.html`         | Manage system/admin users                          |
| Login               | `login.html`        | Login form                                         |
| About               | `about.html`        | Version and system info                            |

### 11.2 Frontend Technology

- **Layui 2.13.5** — UI framework (tables, forms, modals, navigation)
- **ECharts** — dashboard charts
- **ACE Editor** — YAML editor for instance extra_yaml field
- **jQuery** — DOM manipulation (bundled with Layui)
- I18N strings are injected as JavaScript variables via Freemarker context attributes
- All API calls use Layui's `admin.req()` helper, which includes the JWT cookie automatically

---

## 12. Configuration Management

### 12.1 Application Configuration (`app.yml`)

Key settings:
```yaml
server.port: 12680
server.request.maxBodySize: 2048mb
server.request.maxHeaderSize: 20mb
server.session.timeout: 7200

jwt:
  name: TOKEN
  secret: <Base64-encoded HS512 key>
  expiration: 864000000   # 10 days in ms
  allowExpire: true
  allowAutoIssue: false
  allowHeader: true

solon:
  app.name: distops
  logging.logger.undertow: WARN
  serialization.json.longAsString: true

spring:
  active: sqlite
  datasource:
    url: ""
    username: ""
    password: ""
```

### 12.2 Environment Variable Override

`WebApp.main()` iterates all Solon configuration property names and checks for corresponding environment variables using three naming conventions:
- Direct uppercase: `my-prop` → `MYPROP`
- Snake case: `my-prop` → `MY_PROP`
- CamelCase-to-snake: `myPropName` → `MYPROPNAME`, `MY_PROP_NAME`

Any matching environment variable overrides the file-based configuration.

### 12.3 Template-Based Instance Configuration

Instance-specific configuration is generated from Freemarker templates stored in `global_config`:
- `registry_config_yml_template` — the registry `config.yml` template
- `registry_supervisor_template` — the supervisor `.ini` template

Templates can be modified at runtime via the `GlobalConfigMapper`, allowing operators to update configuration without code changes.

---

## 13. Deployment

### 13.1 Docker Container

The Docker image bundles:

```
eclipse-temurin:25-jdk-noble (Ubuntu Noble + Java 25)
  ├── s6-overlay v3.2.2.0 (init + process supervisor)
  ├── CNCF Distribution v3.0.0 (registry binary)
  ├── skopeo v1.22.2 (image tool)
  ├── regctl v0.11.3 (registry API client)
  ├── supervisord (registry process manager)
  ├── nginx (reverse proxy)
  ├── Redis (caching)
  └── distops JAR
```

Entrypoint: `docker-entrypoint.sh` → `s6-overlay /init` → manages distops/redis/supervisor

### 13.2 Vagrant Development Environment

A `Vagrantfile` provisions a development VM:
- Ubuntu 24.04, 2 CPUs, 4GB RAM, 500GB disk, IP `192.168.66.125`
- `scripts/provision.sh` performs: system tuning (sysctl, limits), Docker installation, Java 21, MySQL client, skopeo, regclient, crictl, and editor tools

### 13.3 Build & CI

```
./mvnw clean package -Dmaven.test.skip=true
```

- Maven compiler target: Java 21 (plugin configured to 17 — annotation processing only)
- `build-info-maven-plugin` embeds git revision and build datetime at package time
- `native2ascii` runs during `compile` phase for i18n message files
- GitHub Actions CI (`maven.yml`): JDK 21, builds on push/PR to `main`
- POM uses `solon-maven-plugin` for packaging

---

## 14. Utility Infrastructure

### 14.1 ID Generation

All primary keys use **TSID** (Time-Sorted Unique Identifier) via `IDUtils`:
- 64-bit IDs combining timestamp + random bits
- Similar properties to Snowflake: globally unique, roughly time-ordered
- Both `Long` and `String` representations available

### 14.2 Password Hashing

Three password encoding approaches coexist:
- **BcryptUtils** (password4j) — used for registry account passwords, cost factor 12, detects hash version (`$2a$`/`$2b$`/`$2x$`/`$2y$`)
- **BCryptPasswordEncoder** (Hutool) — alternative encoder, same cost factor
- **Pbkdf2PasswordEncoder** — PBKDF2 with HMAC-SHA256, used for legacy or specific scenarios

### 14.3 JWT Utilities

Two JWT libraries serve different purposes:
- **JJWT** (`JwtUtils`) — used for admin UI login tokens (HMAC-SHA512, prefix support, expiration handling)
- **Nimbus JOSE+JWT** (`NimbusJwtService`) — used for Docker Registry token auth (EC, RSA, HMAC signing)

### 14.4 CLI Tool

A Picocli-based CLI (`cli.java`) with a `jwt` subcommand that generates HS512 keys and prints them in environment variable and config file formats.

---

## 15. Key Design Decisions

1. **Solon over Spring Boot:** Chosen for its lightweight footprint and faster startup time, appropriate for a container-native operations tool.
2. **Wood ORM over MyBatis/JPA:** Follows the Solon ecosystem conventions, using `BaseMapperWrap<T>` for type-safe CRUD with minimal boilerplate.
3. **supervisord for registry processes:** Each registry instance runs as a supervisord-managed child process, allowing distops to manage lifecycle independently of its own JVM.
4. **FreeMarker for config generation:** Templates stored in the database enable runtime customization of registry configurations without code changes.
5. **TSID over auto-increment:** Time-sorted unique IDs avoid database-specific sequence concerns and support multi-database portability.
6. **Dual JWT libraries:** JJWT for internal admin auth (simpler, already a dependency), Nimbus for registry token auth (needed for EC/RSA signing support).
7. **H2 format migration:** The custom H2 version migration system ensures embedded databases survive framework upgrades, crucial for persistent local deployments.
8. **Event webhook ingestion:** Rather than polling the registry API, the system leverages CNCF Distribution's native webhook notifications for real-time artifact tracking.

---

## 16. Package Structure

```
com.dyrnq.distops
├── WebApp.java                    # Main entry point, Solon bootstrap
├── Config.java                    # Bean configuration (HomeDir, CfgExtractor, LocaleResolver)
├── DbConfig.java                  # DbContext bean
├── DataSourceEmbed.java           # Multi-DB DataSource with Flyway + H2 migration
├── Constants.java                 # Application constants
├── CookieName.java                # Cookie name constants
├── HomeDir.java                   # Home directory paths
├── CfgExtractor.java              # Token cookie name record
├── ShowBanner.java                # Startup banner
├── TokenExpiredException.java     # Custom exception
├── H2FormatVersionChecker.java   # H2 format version detection
│
├── controller/
│   ├── BaseController.java        # Base controller (empty)
│   ├── ApiController.java         # API base (prefix: /api)
│   ├── IndexController.java       # Root redirect (/)
│   ├── AdminController.java       # Admin UI pages (/admin)
│   ├── EventController.java       # Registry webhook receiver (/event)
│   ├── TokenController.java       # Login + i18n switch (/token)
│   ├── TestController.java        # Debug/testing endpoints
│   ├── PageResult.java            # Paginated response wrapper
│   └── api/
│       ├── InstController.java    # Instance CRUD + operations
│       ├── AccountController.java # Registry account CRUD + ACL
│       ├── ArtifactController.java# Artifact query/manage
│       ├── ManifestController.java# Manifest query/manage
│       ├── RepoController.java    # Repository CRUD
│       └── UserController.java    # System user CRUD
│
├── filter/
│   ├── AppFilter.java             # Global request context setup
│   ├── AppExceptionFilter.java    # Global exception handler
│   ├── I18nFilter.java            # i18n message loader
│   ├── JwtInterceptor.java       # JWT auth guard
│   └── Message.java               # i18n message DTO
│
├── service/
│   ├── BusinessLogic.java         # User login, password change
│   ├── CronTab.java               # Scheduled registry health check
│   ├── InstService.java           # Core instance lifecycle management
│   ├── RegistrySuperGen.java      # Startup template seeding
│   └── dto/                       # Query parameter DTOs
│
├── model/                         # ORM entities (Inst, User, Repo, Manifest, Artifact, Account, GlobalConfig, Views)
├── dso/                           # Data access mappers (Wood ORM BaseMapperWrap)
│
├── registry/
│   ├── RegistryProxy.java         # Proxy/remote registry config model
│   └── auth/
│       ├── controller/            # AuthController, KeyPairController, KeyPairTestController
│       ├── service/               # AuthService, ITokenService, TokenServiceImpls
│       ├── model/                 # AuthRequest, JWTHeader, JWTPayload, TokenResponse, AclConfig
│       ├── KeyPairManager.java    # Unified key pair generation facade
│       ├── KeyPairInfo.java       # Key pair info DTO
│       ├── ECKeyPairGenerator.java
│       ├── RSAKeyPairGenerator.java
│       ├── HMACKeyGenerator.java
│       ├── KeyPairGeneratorService.java
│       └── util/NimbusJwtService.java
│
└── utils/                         # AddressUtils, BCryptPasswordEncoder, BcryptUtils, IDUtils,
                                   # JwtUtils, Md5Util, PathUtils, Pbkdf2PasswordEncoder,
                                   # TarUtils, VersionUtils, X500NameConverter, X509Holder
```

---

## 17. Testing

### 17.1 Test Coverage

Tests are located in `src/test/java/com/dyrnq/distops/`:

| Test Class              | Focus                                                         |
|-------------------------|---------------------------------------------------------------|
| `AccountMapperTest`     | Solon integration test — inserts 1000 accounts, tests BCrypt  |
| `KeyPairGeneratorTest`  | Standalone EC key pair generation to PEM files                |
| `PrivateKeyTest`        | PEM private key loading via BouncyCastle                      |
| `SecurityUtils`         | BCrypt hashing/verification demo                              |
| `YamlTest`              | YAML parsing, merging, and dumping with SnakeYAML             |

### 17.2 Integration Test Scripts

The `scripts/` directory contains end-to-end shell scripts:

| Script                  | Tests                                               |
|-------------------------|-----------------------------------------------------|
| `test-login-token.sh`   | Docker token auth flow (catalog → challenge → token → authorized request) |
| `test-login-htpasswd.sh`| htpasswd Basic auth                                 |
| `test-login-regctl.sh`  | regctl login + manifest retrieval                   |
| `test-pull-push.sh`     | Docker pull/push + skopeo cross-registry copy       |
| `test-readonly.sh`      | Read-only account enforcement                       |
| `test-algorithms.sh`    | Full cycle: generate EC/RSA/HMAC keys, rebuild, restart, test pull/push |
| `test-crictl.sh`        | containerd/crictl against the registry              |
| `provision.sh`          | Vagrant provisioning (OS tuning, Docker, Java, tools) |
| `install.sh`            | Deploy DB containers (MySQL, PostgreSQL, MinIO, Nginx, Distribution) |
| `ch-docker-daemon.sh`   | Configure Docker daemon mirrors + insecure registries |
| `bump-layui.sh`         | Update Layui frontend library                       |

---

## 18. Build Output Structure

The Maven build produces a JAR with embedded FreeMarker views and static assets:

```
distops.jar
├── com/dyrnq/distops/**        # Application classes
├── WEB-INF/
│   ├── view/
│   │   ├── index.html
│   │   └── admin/              # 18 admin HTML templates
│   └── static/
│       ├── css/
│       ├── js/
│       ├── img/
│       └── lib/                # 12 frontend libraries (layui, echarts, etc.)
├── templates/
│   ├── registry/config.yml.tpl
│   └── supervisor/registry.ini.tpl
├── i18n/messages*.properties
├── db/migration/*/
├── app.yml
├── banner.txt
└── build.info
```
