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


Development setup
--
1. Run backend: `bin/dev-run-backend`
2. Run frontend: `bin/dev-run-frontend`
   - No auto-refresh, FE-changes will be displayed after refresh