# Leihs Inventory

## Development

### Endpoints
- http://localhost:3260/
- http://localhost:3260/inventory/api-docs/index.html


### Start server
```sh
bin/dev-run-backend
```

### Start client

Install and start:

```sh
bin/dev-run-frontend

# build fe-artefacts
npm run build --emptyOutDir
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

### Create artifact & deploy manually

```bash
./bin/build

scp leihs-inventory.jar <user>@<server>:/leihs/inventory/leihs-inventory.jar

systemctl restart leihs-inventory.service
```