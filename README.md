# DDNS FullStack

## Server Configuration

| Parameter | Default | Required | Description                                                                          |
|-----------|---------|----------|--------------------------------------------------------------------------------------|
| MODE      | client  | false    | Describes mode in which application running, set to `server` to run as IP server     |
| HEADER    | none    | false    | Defines which header will contain IP value. Otherwise IP from request will be taken. |
| PORT      | 8080    | false    | Port on which application will be listening                                          |
| HOST      | 0.0.0.0 | false    | Host on which application will be listening                                          |

## Client Configuration

| Parameter  | Default | Required | Description                                                                                |
|------------|---------|----------|--------------------------------------------------------------------------------------------|
| MODE       | client  | false    | Describes mode in which application running, omit or set to `client` to run as DNS updater |
| SERVER_URL | none    | true     | URL to endpoint that returns IP                                                            |
| TOKEN      | none    | true     | Token to access DigitalOcean API                                                           |
