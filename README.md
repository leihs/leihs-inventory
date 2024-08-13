# Leihs Inventory

## Development

### Start server

```sh
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

### Formatting Code

#### Clojure(Script) formatting

Use `./bin/cljfmt check` and `./bin/cljfmt fix`.

From vim you can use `:! ./bin/cljfmt fix %` to format the current file.

#### Ruby formatting

Use `standardrb` and `standardrb --fix`.

## Production build

See `./bin/build`

## Artifact-Build process

- **server.main** is used to run app/entrypoint of jar
- **server.run** creates the app

### Create dev-artifact

```bash
# :output-dir "resources/public/inventory/js"
npx shadow-cljs release frontend

clojure -X:uberjar

# ways to pass args
java -jar leihs-inventory.jar --dev-mode false --repl false run
java -jar leihs-inventory.jar --dev-mode false --repl false run "http-port=3331"
java -jar leihs-inventory.jar --dev-mode false --repl false run "http-port=3250 db-port=5415"

# ways to pass args
bin/dev-run-backend http-port=3333 db-name=my-db
bin/dev-run-backend http-port=3333

# local dev
java -jar leihs-inventory.jar --dev-mode false --repl false run http-port=3250 db-port=5415 db-user=leihs db-password=leihs
```

### Create prod-artifact

```bash
# :output-dir "resources/public/inventory/js"
npx shadow-cljs release frontend

clojure -X:uberjar

scp leihs-inventory.jar <user>@<server>:/leihs/inventory/leihs-inventory.jar

systemctl restart leihs-inventory.service
```
