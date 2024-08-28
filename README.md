# Leihs Inventory


## TODOs

0. Move/upgrade from depstar to tools.build
0. Properly setup static resouces with cache-busting; wrap-resource in
   leihs.inventory.server.swagger-api, see exemplary use of
   `leihs.core.http-cache-buster2` in leihs-admin;
0. Deliver SPA depending on accept headers, see leihs-admin
0. Add container build test; see leihs-admin
0. Add deploy role in this repository, madek-api-v2 for example



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

### Create artifact & deploy manually

```bash
./bin/build

scp leihs-inventory.jar <user>@<server>:/leihs/inventory/leihs-inventory.jar

systemctl restart leihs-inventory.service
```



