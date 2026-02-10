# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Leihs Inventory is a full-stack application for managing inventory, built with:
- **Backend**: Clojure with Reitit for routing, Ring for HTTP, HoneySQL for database queries
- **Frontend**: ClojureScript with React (via UIX), TailwindCSS, React Query
- **Database**: PostgreSQL with JSONB fields for flexible data structures

## Development Commands

### Backend
```bash
bin/dev-run-backend          # Start backend server (port 3260)
clojure -X:test              # Run Clojure tests
bin/cljfmt check             # Check Clojure formatting
bin/cljfmt fix               # Fix Clojure formatting
```

### Frontend
```bash
bin/dev-run-frontend         # Install and start frontend dev server
npm run dev                  # Quick start (when already installed)
npm run build                # Production build
npm run format               # Format frontend code with Prettier
npm run format:check         # Check frontend formatting
```

### Database
The application connects to PostgreSQL database `leihs_dev` (test: `leihs_test`).

### Full Development Setup
For integrated development with proxy:
1. Start reverse proxy: `leihs/integration-tests/bin/start-reverse-proxy` (port 3200)
2. Start inventory backend: `bin/dev-run-backend` (port 3260)
3. Start my service: `leihs/my/bin/dev-run-backend` (port 3240)

### Production Build
```bash
./bin/build                  # Creates leihs-inventory.jar
```

### Locales Management
```bash
npm run locales:sync         # Sync translation files
# Then manually discard changes in zod.json files
```

## Architecture

### Backend Structure

#### Routing and Request Flow
The application uses Reitit for routing with a comprehensive middleware stack defined in `server/main_handler.clj`:
1. Request flows through ordered middleware (authentication, CSRF, database transactions, coercion, etc.)
2. Routes are defined in `server/resources/routes.clj` which imports all sub-route modules
3. All routes are scoped under `/:pool_id` (inventory pool context)

#### Route Organization
Routes follow a strict file structure mapping:
- URL: `/inventory/:pool-id/models/:model-id/entitlements`
- File: `server/resources/pool/models/model/entitlements.clj`

Each route module has:
- `routes.clj` - Route definitions with Swagger docs, parameter schemas, handlers
- `main.clj` - Handler functions with business logic
- `types.clj` - Schema definitions for requests/responses (when needed)

#### Database Access
- Database connection available via `:tx` key in request (injected by `db/wrap-tx` middleware)
- Uses HoneySQL for query building: `(sql/select ...) (sql/from ...) (sql/where ...)`
- Execute with `jdbc/query` or `jdbc/execute-one!`
- Many entities store dynamic data in JSONB `data` column (e.g., fields table)

#### Handler Pattern
```clojure
(defn get-resource [request]
  (try
    (let [tx (:tx request)
          {:keys [param]} (query-params request)
          query (-> (sql/select :*) (sql/from :table))
          results (jdbc/query tx (sql-format query))]
      (response results))
    (catch Exception e
      (error "Error message" e)
      (bad-request {:error "..." :details (.getMessage e)}))))
```

#### Schema Validation
- Uses Clojure Spec for coercion: `reitit.coercion.spec/coercion`
- Schema validation with `schema.core` for request/response types
- UUID conversion is done via spec coercion in `routes.clj` - declare fields as `s/Uuid` in parameter schemas
- Example: `:parameters {:path {:pool_id s/Uuid} :query {:type (s/enum "item" "license")} :body {:id s/Uuid :owner_id s/Uuid}}`

### Frontend Structure

#### Component Organization
- `client/components/ui/` - Reusable Radix UI components (shadcn-style)
- `client/components/customized/` - Custom business components
- `client/components/form/` - Form-specific components
- `client/routes/` - Route-specific page components

#### State Management
- React Query (TanStack Query) for server state
- React hooks for local state
- Form state managed by React Hook Form with Zod validation

#### API Communication
- Axios with cache interceptor for API calls
- Base URL configuration for different environments
- CSRF token handling via headers

### Key Files
- `server/main_handler.clj` - Middleware stack and router initialization
- `server/resources/routes.clj` - Central route registry
- `server/resources/pool/fields/main.clj` - Example of JSONB field handling
- `deps.edn` - Clojure dependencies and aliases
- `package.json` - Frontend dependencies and scripts

## Backend Coding Guidelines (from README)

1. **One handler per route** - No handler reuse between routes
2. **Route-to-file mapping** - Strict correspondence between URL structure and file paths
3. **Pool-scoped routes** - All resources under `/:pool-id`, no global resources
4. **No YAML configs** - Use namespace defaults, overridable by ENV, then CLI params
5. **No debug code in main** - Keep temporary/personal debug code out of main namespace
6. **No route versioning** - Until production release
7. **Flat Swagger routes** - No grouping, alphabetically sorted
8. **Use structured logging** - `debug`, `warn`, `error` from `taoensso.timbre`, never `println`
9. **Eliminate dead code** - Remove unused vars and requires
10. **Context-local field definitions** - Keep schemas near their usage, not centralized
11. **Use canonical routes** - Follow established routing patterns
12. **Prefer threading macros** - Use `->` and `->>` threading macros instead of nested function calls when nesting level > 2 in `.clj`, `.cljs`, and `.cljc` files
13. **Debug logging** - For `.clj` and `.cljs` files debugging purposes only, use `debug` statement from `taoensso.timbre` library. You may need to add the required lib require in the namespace declaration (e.g., `[taoensso.timbre :refer [debug]]`). Also add the respective namespace to `logging.cljc` file in `shared-clj` project. Server must be restarted after changes.
14. **Middlewares location** - Always place middlewares in `src/leihs/inventory/server/middlewares`
15. **Utils location** - Always place utilities/helpers in `src/leihs/inventory/server/utils`
16. **Format before commit** - Always run `./bin/cljfmt fix` after modifying any Clojure backend code (`.clj` and `.cljc` files) and before committing

## Database Schema Notes

The `fields` table uses JSONB for flexible field definitions:
- `id` - Field identifier
- `active` - Boolean flag
- `position` - Display order
- `data` - JSONB containing field metadata (type, label, group, permissions, etc.)

Fields can have:
- `target_type` - Restricts field to "item" or "license"
- `forPackage` - When true, field applies to packages
- Visibility dependencies - Fields can show/hide based on other field values

## Testing

Clojure tests in `test/` directory. Run with:
```bash
clojure -X:test              # All tests
```

Feature and API specs/tests are run with:
```bash
bin/rspec                    # Run all feature and API tests
bin/rspec spec/features/models/create_spec.rb:65  # Run specific test at line
```

Note: For `.cljs` (frontend) files, the shadow-cljs watcher handles hot reloading automatically.

## Database Migrations

PostgreSQL schema managed separately. Check `shared-clj/` for shared database utilities.
