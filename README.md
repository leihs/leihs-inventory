# Leihs Inventory

## TODOs

0. Move/upgrade from depstar to tools.build
1. Properly setup static resouces with cache-busting; wrap-resource in
   leihs.inventory.server.swagger-api, see exemplary use of
   `leihs.core.http-cache-buster2` in leihs-admin;
2. Deliver SPA depending on accept headers, see leihs-admin
3. Add container build test; see leihs-admin
4. Add deploy role in this repository, madek-api-v2 for example

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
