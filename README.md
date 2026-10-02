# Library Management System - Desktop Client

JavaFX desktop application for the librarians of a small network of lending libraries (Open Library Network).
It works on a union catalog shared by every library, kept in a central Strapi backend, and stores patron data
and borrow records locally in an encrypted database.

Each library runs its own instance of this client; all instances talk to the same Strapi server.
The system has three parts:

| Part | Repository | Role |
|------|------------|------|
| Strapi backend | `library-strapi` | Union catalog, copies, borrow/return, cataloguer admin |
| Public website | `oln-astro-frontend` | Catalog and availability for the public |
| **Desktop client** | **this repository** | Librarian workstation |

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Catalog Model (Strapi)](#catalog-model-strapi)
- [Local Data Model](#local-data-model)
- [Features](#features)
- [Security](#security)
- [Setup](#setup)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Development](#development)
- [License](#license)

---

## Overview

The application is the librarian's daily tool:

- **Catalog browsing** of the union catalog: books, brochures, magazines and their issues, authors and publishers,
  each limited to what the librarian's library holds
- **Adding publications**: books by ISBN (from the network's catalog, or from Biblionet.gr when it is not there yet),
  brochures, and magazine issues by ISSN (National Library of Greece catalogue); a local record when no source has it
- **Copy management**: add, edit the condition of, and delete the physical copies of the librarian's library
- **Borrow and return**, atomic on the server, so a copy can never be lent twice
- **Patron management**, local only (personal data stays on the library's computer)
- **Borrow tracking** with overdue detection and per-patron history

Catalog data comes from Strapi over REST. Patrons and borrow records live in an AES-encrypted H2 database on the
library's computer.

---

## Architecture

```
+--------------------------------------------------------------------+
|                    Desktop Client (this application)               |
+--------------------------------------------------------------------+
|                                                                    |
|  +------------------+    +-------------------+    +--------------+ |
|  |  JavaFX UI       |    |  Controllers      |    | FXML Views   | |
|  |  (25.0.4)        |<-->|  wizards, pickers |<-->| (29 files)   | |
|  +------------------+    +-------------------+    +--------------+ |
|           |                       |                                |
|           v                       v                                |
|  +-------------------+   +------------------+                      |
|  | CatalogService    |   | BorrowService    |                      |
|  | StrapiApiClient   |   | (Strapi + H2)    |                      |
|  | (HTTP/1.1 + JSON) |   +------------------+                      |
|  +-------------------+            |                                |
|           |                       v                                |
|  +------------------+    +------------------+                      |
|  | AuthService      |    | H2 Database      |                      |
|  | (JWT + Keystore) |    | (AES, local)     |                      |
|  +------------------+    +------------------+                      |
|           |                       |                                |
+-----------|--------------------+--+--------------------------------+
            |                    |
            v                    v
   +------------------+    +----------+
   |  Strapi 5        |    | OS       |
   |  (REST API)      |    | Keystore |
   +------------------+    +----------+
   | Books (all types)|    | DEK      |
   | Persons, roles   |    | JWT      |
   | Publishers       |    | Library  |
   | Subjects         |    | Server   |
   | Magazines        |    +----------+
   | Copies, Libraries|
   +------------------+
```

**Operational modes:**

| Operation | Strapi reachable | Strapi unreachable |
|-----------|------------------|--------------------|
| Browse the catalog, add publications | Yes | No |
| Borrow / return | Yes | No (needs the server) |
| View borrow history | Yes | Yes (local H2) |
| Manage patrons | Yes | Yes (local H2) |

A saved session starts even when the server is unreachable; the status bar then shows **● Offline**.

---

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 25 (LTS) | Runtime and build (the build requires 25) |
| JavaFX | 25.0.4 | Desktop UI |
| Spring Boot | 4.1.1 | Dependency injection, JPA, configuration |
| Hibernate / H2 | 7.x / 2.4 (managed by Spring Boot) | Local encrypted storage |
| Jackson | 3.x (`tools.jackson`) | JSON for the Strapi API |
| java-keyring | 1.0.4 | OS keystore (DEK, session) |
| FontAwesomeFX | 4.7.0-9.1.2 | Icons |
| Lombok | managed by Spring Boot | JPA entities |
| JUnit 5, Mockito, jqwik | managed / 1.7.4 | Tests (unit, integration, property-based) |
| Maven wrapper | 3.9.16 | Build |

---

## Catalog Model (Strapi)

The client follows the Strapi 5 API: flat responses (`{ data, meta }`, no `attributes`), and every record is
addressed by its **`documentId`** (paths, filters and request bodies). Numeric ids are not used.

- **Publication** (`book`) with a `type`: «Βιβλίο» (book), «Μπροσούρα» (brochure) or «Περιοδικό» (magazine issue)
- **Contributors**: an ordered list of person + role (author, translator, …); the author role is recognised by its
  Biblionet type id, never by its name
- **Publisher**, **subjects** (DDC), **copies** (per library, with condition and availability)
- **Magazine** (title, ISSN, publisher) with its **issues**: publications of type «Περιοδικό» with an issue number
  and/or a period, shown in reading order
- **Local records** (entered by a librarian when no source has them) are marked for review by a cataloguer in the
  Strapi admin; duplicates are refused by the server and offered back to the librarian as candidates

---

## Local Data Model

Only patrons and borrow records are stored locally. Catalog data is fetched from Strapi.

```
+---------------------------+
|        USER (H2)          |
+---------------------------+
| userId (PK, auto)         |
| firstname                 |
| lastname                  |
| email                     |
| phone                     |
| activeBorrowCount (trans) |
+---------------------------+
         | 1:N
         v
+--------------------------------------+
|             BORROW (H2)              |
+--------------------------------------+
| id (PK, auto)                        |
| user_id (FK -> User)                 |
|--------------------------------------|
| strapiCopyDocumentId (String)        |  --> Strapi copy documentId
| strapiPublicationDocumentId (String) |  --> Strapi book documentId
| copyNumber (Integer)                 |
|--------------------------------------|
| publicationTitle (cached)            |
| publicationType (cached)             |  «Βιβλίο» | «Μπροσούρα» | «Περιοδικό»
| isbn (cached, null for brochures)    |
| authorName (cached)                  |
|--------------------------------------|
| borrowDate, dueDate, returnDate      |
| returned (boolean)                   |
+--------------------------------------+
```

**Design decisions:**
- Borrows reference Strapi records by `documentId` (no JPA relation to remote data)
- Cached fields (`publicationTitle`, `isbn`, …) let lists show without REST calls
- Strapi stays the source of truth for availability; local records are history and display
- The schema is managed by Hibernate (`ddl-auto=update`); a database file from an older H2 version does not open
  (the application then says so in a dialog)

---

## Features

### Catalog
- Sidebar modules: Dashboard, Patrons, Borrows, Books, Brochures, Magazines, Authors, Publishers
- Books, brochures and magazines list only what the librarian's library holds, with search and paging
- Publication details window: contributors, publisher, subjects, cover (Biblionet), copies
- Authors and publishers of the library, each with their publications in the library

### Adding publications (wizards)
- **Book**: ISBN → network catalog → Biblionet.gr → otherwise a local record form
- **Brochure**: search the network's brochures → otherwise a local record form
- **Magazine issue**: ISSN or serial barcode → network catalog → National Library of Greece → otherwise a new local
  magazine; then the issue (number and/or period)
- Pickers for contributors (person + role, ordered), publisher and subjects; new person and new publisher dialogs
- Duplicates are offered as candidates instead of creating a second record
- Copies are added in the same flow (count and condition), numbered after the library's existing copies

### Copies
- Add, change condition (NEW, GOOD, FAIR, POOR) and delete copies of the librarian's library;
  a borrowed copy cannot be deleted

### Borrow operations
- Choose a patron, a publication and one of its available copies
- Configurable due date (default 14 days)
- Atomic borrow and return on the server (a copy can never be lent twice across libraries)

### Patrons
- Local create, edit and delete; active borrow count; borrow history per patron

### Dashboard
- Total publications of the library, active loans, overdue loans, registered patrons
- Most borrowed publications with their covers

### Usability
- Greek and English, switched at runtime
- Keyboard shortcuts: Ctrl+1 … Ctrl+5 (books, patrons, borrows, authors, publishers); in management screens
  Ctrl+N (new), Ctrl+E (edit), Delete, F5 (refresh)
- **● Online / ● Offline** indicator; Logout in the sidebar

---

## Security

### Authentication
- Login via Strapi (`POST /api/auth/local`); **only accounts with the Librarian role** and an assigned library can
  log in
- The server URL must use **https**; plain http is accepted only for a server on the same computer
  (`localhost`, `127.0.0.1`, `[::1]`). A saved session that uses plain http to a remote server is cleared
- The JWT, the library's `documentId` and the server URL are kept in the OS keystore; the password is never stored
- A saved session is restored at startup; a token the server rejects (401) leads to the login screen

### Data encryption
- H2 database encrypted with AES; the 256-bit Data Encryption Key (DEK) is generated on first run and kept in the
  OS keystore, never sent over the network
- When the database cannot open (older H2 version, already in use, key that does not open it), the application
  shows the reason in a dialog instead of failing silently

### Backup and recovery
> Implemented in `BackupService` but not yet available in the application: there is no menu or schedule that runs it.
- Hybrid encryption: AES-256-GCM (data) + RSA-2048-OAEP (session key)
- RSA public key stored locally (encrypts backups); RSA private key held offline by an administrator
- Recovery possible even if the DEK is lost

### OS keystore
- Linux: GNOME Keyring / KDE Wallet (Secret Service API)
- macOS: Keychain
- Windows: Credential Store
- If no keystore is available, secrets fall back to memory (not persistent, not secure: development only)

---

## Setup

### Prerequisites

- JDK 25 (the build stops with a clear message on an older Java)
- JavaFX 25 comes through Maven; nothing to install
- A running Strapi backend (see the `library-strapi` README)
- A Strapi user with the **Librarian** role and an assigned library (created by an administrator; public
  registration is closed)

### Build

```bash
./mvnw clean package -DskipTests
```

### Run

```bash
./mvnw javafx:run
```

In IntelliJ, run the Maven goal `javafx:run` (project SDK 25). `spring-boot:run`, or running
`LibraryManagementFXApplication` directly with JavaFX on the classpath, does not work: the Java launcher refuses a
JavaFX `Application` main class without the JavaFX modules.

After packaging, the fat jar also runs:

```bash
java -jar target/librarymanagementsystemdesktop-0.0.1-SNAPSHOT.jar
```

It prints "Unsupported JavaFX configuration" because JavaFX is loaded from the classpath; this goes away with a
packaged build that bundles JavaFX as modules (planned).

### First run

1. The login screen asks for the server URL (e.g. `https://catalog.example.org`, or `http://localhost:1337` for a
   local server) and the librarian's credentials
2. On success the main window opens on the dashboard
3. A DEK is generated and stored in the OS keystore
4. The encrypted H2 database is created in `./data/`

---

## Configuration

### application.properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.h2.encryption.enabled` | `true` | AES encryption of the local database |
| `app.h2.path` | `./data/library` | Path of the H2 database file (without extension) |
| `borrow.default.due.days` | `14` | Default borrow duration in days |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management |

### Test profile

Every test runs with the `test` profile (set by the Maven surefire configuration), defined in
`src/test/resources/application-test.properties`: an in-memory H2 database without encryption and an in-memory
keystore (`app.keystore.in-memory=true`). Tests also use their own Java preferences root (`target/test-prefs`).
They never touch `./data/`, the OS keystore or the saved language of the running application. These switches
exist only in the test profile; `MainConfigTest` checks that the real configuration does not use them.

---

## Project Structure

```
src/main/java/net/gizmolab/library/librarymanagementsystemdesktop/
├── LibraryManagementSystemApplication.java   # Spring Boot configuration
├── LibraryManagementFXApplication.java       # JavaFX entry point (starts Spring, shows startup failures)
├── config/
│   ├── DataSourceConfig.java                 # Encrypted H2 DataSource (opens the database at startup)
│   └── FXMLLoaderFactory.java                # Spring-aware FXML loader
├── controller/
│   ├── base/                                 # BaseController, BaseManagementController (tables, paging)
│   ├── picker/                               # Contributor, publisher and subject pickers
│   ├── LoginController.java
│   ├── MainNavigationController.java         # Sidebar, modules, language, logout, status bar
│   ├── DashboardViewController.java
│   ├── BookManagementController.java         # Books list
│   ├── BrochureManagementController.java     # Brochures list
│   ├── MagazineManagementController.java     # Magazines and their issues
│   ├── PublicationListController.java        # Shared publication list behaviour
│   ├── PublicationDetailWindow.java, PublicationDetailModalController.java, PublicationSummaryController.java
│   ├── AddPublicationWizardController.java   # Shared wizard flow
│   ├── AddBookWizardController.java, AddBrochureWizardController.java, AddIssueWizardController.java
│   ├── LocalPublicationFormController.java   # Local record form (book, brochure, issue)
│   ├── NewPersonDialogController.java, NewPublisherDialogController.java, NewMagazineDialogController.java
│   ├── CopiesInputController.java, CopyManagementModalController.java
│   ├── AuthorManagementController.java, AuthorBooksModalController.java
│   ├── PublisherManagementController.java, PublisherPublicationsModalController.java
│   ├── BorrowManagementController.java, BorrowFormControllerNew.java, BorrowHistoryModalController.java
│   └── UserManagementController.java, UserFormController.java
├── dto/                                      # Strapi records (documentId) and local BorrowDTO, UserDTO
│   └── draft/                                # Publication, person, publisher and magazine drafts (payloads)
├── model/                                    # JPA entities: User, Borrow (H2)
├── repository/                               # Spring Data JPA: UserRepository, BorrowRepository
├── service/
│   ├── AuthService.java                      # Login (Librarian only, https), session in the keystore
│   ├── KeyStoreService.java                  # OS keystore (DEK, session), in-memory for tests
│   ├── StrapiApiClient.java                  # HTTP/1.1 client for the Strapi REST API
│   ├── CatalogService.java                   # Lookups, local records, duplicates, copies, issues
│   ├── AddPublicationFlow.java               # Create a publication and add its copies
│   ├── BorrowServiceImpl.java, UserServiceImpl.java (+ interfaces)
│   ├── BackupService.java                    # Encrypted backups (not wired to the UI yet)
│   ├── I18nManager.java, IconProvider.java, AlertManager.java, AnimationHelper.java
│   ├── GlobalExceptionHandler.java
│   ├── exceptions/
│   └── utilities/                            # DTOConverter (Strapi JSON -> DTO), keyboard and layout helpers
├── util/                                     # Formatting, filters, issue order, paging, popular list,
│                                             # background tasks, user messages, startup failure messages
└── validation/
    └── ISBNValidator.java

src/main/resources/
├── fxml/                                     # 29 FXML views
├── css/modern-theme.css                      # Application stylesheet
├── static/assets/logo.png
├── messages.properties                       # Default texts (English)
├── messages_el.properties                    # Greek
├── messages_en.properties                    # English
└── application.properties                    # Spring Boot configuration

src/test/resources/
├── application-test.properties               # Test profile (in-memory database and keystore)
└── strapi-fixtures/                          # Real Strapi 5 responses (contract fixtures)

docs/                                         # Manual test checklists
tools/check-fxml.py                           # Checks every fx:id and handler against its controller
```

---

## Development

### Build and run the tests

```bash
./mvnw clean compile
./mvnw test
```

- Tests are isolated from the running application (see [Test profile](#test-profile)); they pass while the
  application is open.
- Some tests load FXML and CSS with JavaFX and need a display (they do not run headless).
- `strapi-fixtures/*.json` are real responses written by the Strapi contract tests (`library-strapi`,
  `tests/integration/contract-fixtures*.test.js`); regenerate them there when the API changes.
- `MessagesBundleTest` checks that the three message files have the same keys and that every text the UI asks for
  exists.

### FXML check

```bash
python3 tools/check-fxml.py
```

### Manual checklists

`docs/` holds the checklists used after larger changes: read-only catalog, adding publications, magazines,
the Strapi 5 migration and the Java 25 upgrade.

### Code conventions

- Controllers are Spring-managed; form and dialog controllers use `@Scope("prototype")`
- Work off the FX thread through `BackgroundTasks` / `javafx.concurrent.Task`
- All Strapi communication goes through `StrapiApiClient` (and `CatalogService` for catalog flows); controllers make
  no HTTP calls
- Strapi records are identified by `documentId`; only local H2 entities have numeric ids
- DTOs are plain Java objects; JPA entities use Lombok `@Data`
- User-visible texts come from the message files, except startup failures shown before Spring is up

---

## License

[MIT](LICENSE) © 2026 gizmo_lab
