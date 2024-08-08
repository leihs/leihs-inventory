# Leihs Inventory

## Server

### Start server

```sh
bin/dev-run-backend
```

## Client

### Start the client

Install and start:

```sh
bin/dev-run-frontend
```

Plain start, assuming it was installed before:

```sh
npm run dev
```

### Formatting Code

Use `./bin/cljfmt check` and  `./bin/cljfmt fix`.

From vim you can use `:! ./bin/cljfmt fix %` to format the current file.


#### Ruby
Use `standardrb` and  `standardrb --fix`.


Artifact-Build process
- **server.main** is used to run app/entrypoint of jar
- **server.run** creates the app
--
```bash 
clojure -X:uberjar

java -jar leihs-inventory.jar --dev-mode false --repl false run
```