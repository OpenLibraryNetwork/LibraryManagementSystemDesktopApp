# Library Management System - Desktop Client

JavaFX desktop application for lending library management. Connects to a central Strapi backend for shared catalog access and provides local encrypted storage for patron data and borrow records.

Designed for deployment across 10-15 lending library branches, each running an independent instance of this client connected to the same Strapi server.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Local Data Model](#local-data-model)
- [Features](#features)
- [Security](#security)
- [Setup](#setup)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Development](#development)

---

## Overview

This application serves as the librarian workstation for daily operations:

- **Catalog browsing** of the shared publication database (books, brochures, periodical issues)
- **ISBN lookup** via Biblionet.gr API integration (automatic metadata population)
- **Copy management** (add/remove physical copies per branch)
- **Borrow/return** operations with atomic server-side enforcement
- **Patron management** (local, GDPR-compliant)
- **Borrow tracking** with overdue detection and history

All catalog data comes from the Strapi backend via REST API. Patron information and borrow records are stored locally in an AES-256 encrypted H2 database, ensuring GDPR compliance by keeping personal data on-premises.

---

## Architecture

```
+------------------------------------------------------------------+
|                    Desktop Client (this application)               |
+------------------------------------------------------------------+
|                                                                    |
|  +------------------+    +------------------+    +--------------+ |
|  |  JavaFX UI       |    |  Controllers     |    | FXML Views   | |
|  |  (25.0.4)        |<-->|  (17 screens)    |<-->| (17 files)   | |
|  +------------------+    +------------------+    +--------------+ |
|           |                       |                                |
|           v                       v                                |
|  +------------------+    +------------------+                      |
|  | StrapiApiClient  |    | BorrowService    |                      |
|  | (HTTP + Jackson)  |    | (orchestrator)   |                      |
|  +------------------+    +------------------+                      |
|           |                       |                                |
|           v                       v                                |
|  +------------------+    +------------------+                      |
|  | AuthService      |    | H2 Database      |                      |
|  | (JWT + Keystore) |    | (AES-256, local) |                      |
|  +------------------+    +------------------+                      |
|           |                       |                                |
+-----------|--------------------+--+--------------------------------+
            |                    |
            v                    v
   +------------------+    +----------+
   |  Strapi Backend  |    | OS       |
   |  (REST API)      |    | Keystore |
   +------------------+    +----------+
   | Publications     |    | DEK      |
   | Authors          |    | JWT      |
   | Publishers       |    | Config   |
   | Copies           |    +----------+
   | Libraries        |
   +------------------+
```

**Operational Modes:**

| Operation | Online (Strapi available) | Offline |
|-----------|--------------------------|---------|
| Browse catalog | Yes | No |
| Borrow / Return | Yes | No (requires connection) |
| View borrow history | Yes | Yes (local H2) |
| Manage patrons | Yes | Yes (local H2) |
| ISBN lookup (Biblionet) | Yes | No |

---

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 25 (LTS) | Runtime |
| Spring Boot | 4.1.1 | Dependency injection, JPA, configuration |
| JavaFX | 25.0.4 | Desktop UI framework |
| H2 Database | 2.x | Local encrypted storage |
| Jackson | 3.x (`tools.jackson`) | JSON parsing for Strapi responses |
| java-keyring | 1.0.4 | OS keystore access (DEK, JWT storage) |
| FontAwesome (javafx) | 4.7.0-9.1.2 | Icon library |
| Lombok | 1.18.x | Boilerplate reduction |
| Maven | 3.9.x | Build system |

---

## Local Data Model

The client stores only patron and borrow data locally. All publication/catalog data is fetched from Strapi at runtime.

```
+---------------------------+
|        USER (H2)          |
+---------------------------+
| userId (PK, auto)         |
| firstname                 |
| lastname                  |
| email                     |
| phone (unique)            |
| activeBorrowCount (trans) |
+---------------------------+
         | 1:N
         v
+---------------------------+
|       BORROW (H2)         |
+---------------------------+
| id (PK, auto)            |
| user_id (FK -> User)     |
|---------------------------|
| strapiCopyId (Long)      |  --> Strapi Copy.id
| strapiPublicationId (Long)|  --> Strapi Book.id
| copyNumber (Integer)     |
|---------------------------|
| publicationTitle (cached) |
| publicationType (cached)  |  "Book" | "Brochure" | "Periodical"
| isbn (cached, nullable)   |
| authorName (cached)       |
|---------------------------|
| borrowDate (LocalDate)    |
| dueDate (LocalDate)       |
| returnDate (nullable)     |
| returned (boolean)        |
+---------------------------+
```

**Design decisions:**
- `strapiCopyId` / `strapiPublicationId` reference Strapi entities by ID (no JPA relation)
- Cached fields (`publicationTitle`, `isbn`, etc.) avoid REST calls for list display
- Source of truth for availability remains Strapi; local records are for history/display

---

## Features

### Catalog Management
- Unified publication view (books, brochures, periodical issues) with type filter
- ISBN search with automatic Biblionet.gr metadata population
- Copy creation with condition tracking (NEW, GOOD, FAIR, POOR)
- Author and publisher management

### Borrow Operations
- Publication search with available copy display
- Patron selection from local database
- Configurable due date (default: 14 days)
- Atomic borrow via Strapi (prevents double-borrow across branches)
- Return with server-side availability update

### Patron Management
- Local CRUD operations (no server dependency)
- Active borrow count tracking
- Borrow history per patron

### Dashboard
- KPI cards: total publications, active loans, overdue items, registered patrons
- Popular publications ranking (by borrow frequency)

### Internationalization
- Greek (default) and English
- Runtime language switching without restart

---

## Security

### Authentication
- Login via Strapi (`POST /api/auth/local`)
- JWT stored in OS keystore (not filesystem, not memory-only)
- 90-day JWT expiry; automatic session restore on startup
- Password never stored locally

### Data Encryption
- H2 database encrypted with AES-256
- Data Encryption Key (DEK): 256-bit random, stored in OS keystore
- DEK generated on first run, never transmitted over network
- Separate from authentication credentials

### Backup and Recovery
> Implemented in `BackupService` but not yet available in the application: there is no menu or schedule that runs it.
- Hybrid encryption: AES-256-GCM (data) + RSA-2048-OAEP (session key)
- RSA public key stored locally (encrypts backups)
- RSA private key held offline by administrator (decrypts for recovery)
- Recovery possible even if DEK is lost

### OS Keystore Integration
- Linux: GNOME Keyring / KDE Wallet (Secret Service API)
- macOS: Keychain
- Windows: Credential Store
- Fallback: in-memory (development only, not secure)

---

## Setup

### Prerequisites

- Java 25 (JDK 25); the build stops with a clear message on an older Java
- JavaFX 25 (via Maven, nothing to install)
- Maven 3.9+ (or use included `mvnw` wrapper)
- Running Strapi backend instance (see `library-strapi/` README)
- A Strapi user account with Librarian role and assigned library

### Build

```bash
./mvnw clean package -DskipTests
```

### Run

```bash
./mvnw javafx:run
```

`spring-boot:run` (or running `LibraryManagementFXApplication` directly with JavaFX on the classpath) does not work:
the Java launcher refuses a JavaFX `Application` main class without the JavaFX modules. In IntelliJ, run the Maven goal
`javafx:run`.

Or after packaging:

```bash
java -jar target/librarymanagementsystemdesktop-0.0.1-SNAPSHOT.jar
```

### First Run

1. Application starts and shows the login screen
2. Enter the Strapi server URL (e.g., `http://localhost:1337`)
3. Enter your Librarian credentials
4. On successful login, the main interface loads
5. A DEK is automatically generated and stored in the OS keystore
6. The encrypted H2 database is created in `./data/`

---

## Configuration

### application.properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.h2.encryption.enabled` | `true` | Enable/disable H2 AES encryption |
| `app.h2.path` | `./data/library` | Path to H2 database file |
| `borrow.default.due.days` | `14` | Default borrow duration in days |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management strategy |

### Development Mode

To run without OS keystore (e.g., CI environments):

```properties
# application-dev.properties
app.h2.encryption.enabled=false
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Project Structure

```
src/main/java/net/gizmolab/library/librarymanagementsystemdesktop/
├── LibraryManagementSystemApplication.java    # Spring Boot entry point
├── LibraryManagementFXApplication.java        # JavaFX application entry
├── config/
│   ├── DataSourceConfig.java                  # H2 encrypted DataSource
│   └── FXMLLoaderFactory.java                 # Spring-aware FXML loader
├── controller/
│   ├── base/
│   │   ├── BaseController.java                # Common controller utilities
│   │   └── BaseManagementController.java      # Table CRUD base class
│   ├── LoginController.java                   # Authentication screen
│   ├── MainNavigationController.java          # Primary navigation + modules
│   ├── DashboardViewController.java           # KPI dashboard
│   ├── PublicationManagementController.java   # Catalog (all types)
│   ├── PublicationFormController.java         # Add/edit publication + ISBN search
│   ├── BorrowManagementController.java        # Borrow list + return
│   ├── BorrowFormControllerNew.java           # New borrow creation
│   ├── BorrowHistoryModalController.java      # Per-patron borrow history
│   ├── AuthorManagementController.java        # Author CRUD
│   ├── AuthorFormController.java              # Author form
│   ├── AuthorBooksModalController.java        # Author's publications
│   ├── PublisherManagementController.java     # Publisher CRUD
│   ├── PublisherFormController.java           # Publisher form
│   ├── PublisherPublicationsModalController.java  # Publisher's catalog
│   ├── MagazineManagementController.java      # Magazine titles
│   ├── MagazineFormController.java            # Magazine form
│   ├── UserManagementController.java          # Patron CRUD
│   └── UserFormController.java                # Patron form
├── dto/
│   ├── PublicationDTO.java                    # Unified publication (from Strapi)
│   ├── CopyDTO.java                          # Physical copy (from Strapi)
│   ├── AuthorDTO.java                        # Author (from Strapi)
│   ├── PublisherDTO.java                     # Publisher (from Strapi)
│   ├── MagazineDTO.java                     # Magazine title (from Strapi)
│   ├── BorrowDTO.java                       # Borrow record (from local H2)
│   └── UserDTO.java                         # Patron (from local H2)
├── model/
│   ├── User.java                            # JPA entity (H2)
│   └── Borrow.java                          # JPA entity (H2)
├── repository/
│   ├── UserRepository.java                  # Spring Data JPA
│   └── BorrowRepository.java               # Spring Data JPA
├── service/
│   ├── AuthService.java                     # Strapi login, JWT management
│   ├── KeyStoreService.java                 # OS keystore (DEK, JWT, config)
│   ├── BackupService.java                   # RSA-encrypted backups (not wired to the UI yet)
│   ├── StrapiApiClient.java                 # HTTP client for Strapi REST API
│   ├── IBorrowService.java                  # Borrow operations interface
│   ├── BorrowServiceImpl.java              # Borrow orchestrator (Strapi + H2)
│   ├── IUserService.java                   # User operations interface
│   ├── UserServiceImpl.java               # User CRUD (local H2)
│   ├── AlertManager.java                   # UI alert/dialog utilities
│   ├── AnimationHelper.java               # UI animation utilities
│   ├── GlobalExceptionHandler.java        # Centralized error handling
│   ├── I18nManager.java                   # Internationalization
│   ├── IconProvider.java                  # FontAwesome icon utilities
│   ├── ValidationManager.java            # Input validation
│   └── utilities/
│       └── DTOConverter.java              # Strapi JSON -> DTO conversion
└── util/
    ├── PaginationHelper.java              # Pagination calculations
    ├── TableCellFactory.java              # TableView cell factories
    └── StylesheetHelper.java             # CSS theme application

src/main/resources/
├── fxml/                                  # 17 FXML view files
├── css/modern-theme.css                   # Application stylesheet
├── messages.properties                    # Default (English) i18n
├── messages_el.properties                 # Greek i18n
├── messages_en.properties                 # English i18n
└── application.properties                 # Spring Boot configuration
```

---

## Development

### Build

```bash
./mvnw clean compile
```

### Run Tests

```bash
./mvnw test
```

### Package (fat JAR)

```bash
./mvnw clean package -DskipTests
```

### Code Conventions

- Controllers annotated with `@Controller` or `@Component` (Spring-managed)
- Form controllers use `@Scope("prototype")` (new instance per dialog)
- Background operations use `javafx.concurrent.Task` (off FX thread)
- All Strapi communication goes through `StrapiApiClient`
- No direct HTTP calls from controllers
- DTOs are plain Java objects (no Lombok, explicit getters/setters)
- JPA entities use Lombok `@Data`
