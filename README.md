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

### Create artifact & deploy manually

```bash
./bin/build

scp leihs-inventory.jar <user>@<server>:/leihs/inventory/leihs-inventory.jar

systemctl restart leihs-inventory.service
```


Session
--
```clojure

leihs-user-session

session-token=value=eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyLWlkIjoibXJhZGwifQ.1lKM-8x-XiU97TbLEbASjf33PPuGNByacIPP_FOn96s&http-only=true&secure=true&max-age=3600&path=%2F


curl 'https://leihs.zhdk.ch/procure/graphql' \
-H 'Accept-Language: de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7' \
-H 'Cache-Control: no-cache' \
-H 'Connection: keep-alive' \
-H 'Cookie: leihs-anti-csrf-token=2dd79fee-e46c-4afd-93b5-dc0ff80bef95; leihs-user-session=7bbfa715-fc18-45b1-8280-06ba06967944' \
-H 'Origin: https://leihs.zhdk.ch' \
-H 'Pragma: no-cache' \
-H 'Referer: https://leihs.zhdk.ch/procure/requests' \
-H 'Sec-Fetch-Dest: empty' \
-H 'Sec-Fetch-Mode: cors' \
-H 'Sec-Fetch-Site: same-origin' \
-H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36' \
                              -H 'accept: application/json' \
                              -H 'content-type: application/json' \
                              -H 'sec-ch-ua: "Not)A;Brand";v="99", "Google Chrome";v="127", "Chromium";v="127"' \
                              -H 'sec-ch-ua-mobile: ?0' \
                              -H 'sec-ch-ua-platform: "macOS"' \
                              -H 'x-csrf-token: 2dd79fee-e46c-4afd-93b5-dc0ff80bef95' \
                              --data-raw '{"operationName":"me","variables":{},"query":"query me {\n  current_user {\n    navbarProps\n    user {\n      id\n      firstname\n      lastname\n      permissions {\n        isAdmin\n        isRequester\n        isInspectorForCategories {\n          id\n          __typename\n        }\n        isViewerForCategories {\n          id\n          __typename\n        }\n        __typename\n      }\n      __typename\n    }\n    __typename\n  }\n  settings {\n    contact_url\n    __typename\n  }\n}\n"}'

```