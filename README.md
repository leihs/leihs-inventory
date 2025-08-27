# Leihs Inventory

## Development

### Start server

```sh
# to build latest FE and run BE
npm inst
npm run build

bin/dev-run-backend
```

### Start client

Install and start:

```sh
bin/dev-run-frontend
```

For a quicker start when already installed before:

```sh
npm run dev
```

### Sync Locales

```sh
npx i18next-locales-sync -p de -s en -l resources/public/inventory/assets/locales/ --spaces 2 --useEmptyString true
```

**Then discard changes in zod.json files**

### Formatting Code

#### Clojure(Script) formatting

Use `./bin/cljfmt check` and `./bin/cljfmt fix`.

From vim you can use `:! ./bin/cljfmt fix %` to format the current file.

#### Ruby formatting

Use `standardrb` and `standardrb --fix`.

## Production build

See `./bin/build`

### Development-Setups

#### Frontend Development

1. `/inventory`  
   http://localhost:8080/

   ```bash
   # leihs/inventory
   ./bin/dev-run-backend
   ```

#### Standalone

1. `/inventory`  
   http://localhost:3260/  
   See `SESSION_HANDLING_ACTIVATED?`
   ```bash
   # leihs/inventory
   ./bin/dev-run-backend
   ```

#### Setup with proxy/my/inventory

1. Proxy  
   http://localhost:3200/
   ```bash
   # leihs/integration-tests
   ./bin/start-reverse-proxy
   ```
2. `/inventory`  
   http://localhost:3260/
   ```bash
   # leihs/inventory
   ./bin/dev-run-backend
   ```
3. `/my` (provides simple login)  
   http://localhost:3240/
   ```bash
   # leihs/my
   ./bin/dev-run-backend
   ```
4. Legacy (not required)
   ```bash
   # leihs/legacy
   ./bin/rails server -p 3210
   ```

#### Backend Coding Guidelines

1. _always a dedicated handler per route_ (no reuse of handlers between multiple routes)  
2. _mapping of routes to file structure and namespaces_ (example: `/inventory/:pool-id/models/:model-id/entitlements` -> `/inventory/inventory-pool/models/model/entitlements.clj` ) 
3. _maintain parity between frontend and backend routes as far as possible_ (in frontend as well as in legacy all views/resources are scoped under `:pool-id`-> do the same for backend routes -> no global resources like e.g. `/inventory/models` but `/inventory/:pool-id/models`) 
4. _no configs in yml files_. following system is setup in other leihs apps which has to be followed like this: 
  * default in namespace
  * ENV overrides default in namespace
  * CLI parameter overrides ENV and namespace default
5. _no temporary personal debug stuff under main namespace_ `/inventory/inventory-pool/...` (if desired than place it under another namespace and don't include it in production build) 
6. _no versioning of routes until prod release_ (afterwards ok). keep always the actual version depending on the specification and test. 
7. _no grouping of routes in swagger_. all routes are listed flat and alphabetically sorted. 
8. _don't use native clojure print facilities_ (like `println`) for debug log statements. they are active always irrespective of log level! use `debug`, `warn` etc. instead always making an adequate judgement what and what not to have in prod log. we don't want to clutter prod log (performance and readability). debug statements are safe in general even if one forgets them in a particular file: they don't clutter prod log and even development log unless the respective namespaces is activated for debug logging. 
9. _eliminate dead code_ (unused vars, etc.). also keep number of unused `:requires` low (you can use clojure-lsp "clean ns" command)
10. _keep field definitions in context of entities_; don't create a centralized field registry:
  * `utils/coercion/spec_alpha_definition*.clj`
  * `utils/helper.clj` -> `convert-map-if-exist`
11. _use canonical routes_

### Create artifact & deploy manually

```bash
./bin/build

scp leihs-inventory.jar <user>@<server>:/leihs/inventory/leihs-inventory.jar

systemctl restart leihs-inventory.service
```

### Clojure tests

```bash
# additional clojure-tests can be triggered by:
clojure -X:test
```
