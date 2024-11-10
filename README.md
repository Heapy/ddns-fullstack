# DDNS FullStack

Simple client-server DDNS setup for personal use.

![Diagram](./docs/Fullstack%20DDNS.png)

## Prerequisites

1. Server with static public IP address or domain name, i.e `ddns.bar.com`
2. Domain used with DigitalOcean DNS, i.e `bar.com`
3. DigitalOcean API token (Obtain a token using the API section in DigitalOcean UI)
4. Home router with public, but dynamic IP (This project is not [a tunneling solution](https://github.com/anderspitman/awesome-tunneling))

## Server Configuration

| Parameter | Default | Required | Description                                                                          |
|-----------|---------|----------|--------------------------------------------------------------------------------------|
| MODE      | client  | true     | Describes mode in which application running, set to `server` to run as IP server     |
| HEADER    | none    | false    | Defines which header will contain IP value. Otherwise IP from request will be taken. |
| PORT      | 8080    | false    | Port on which application will be listening                                          |
| HOST      | 0.0.0.0 | false    | Host on which application will be listening                                          |

## Client Configuration

| Parameter               | Default | Required | Description                                                                                |
|-------------------------|---------|----------|--------------------------------------------------------------------------------------------|
| MODE                    | client  | false    | Describes mode in which application running, omit or set to `client` to run as DNS updater |
| SERVER_URL              | none    | true     | URL to endpoint that returns IP                                                            |
| CHECK_PERIOD            | 5m      | false    | Period of time between checks for IP change                                                |
| REQUEST_TIMEOUT         | 30s     | false    | Timeout for request to server                                                              |
| ATTEMPTS_BEFORE_WARNING | 5       | false    | Number of attempts to make before warning about failed request                             |

### Digitalocean DNS

| Parameter                | Default | Required | Description                                           |
|--------------------------|---------|----------|-------------------------------------------------------|
| DIGITALOCEAN_TOKEN       | none    | true     | Token to access DigitalOcean API                      |
| DIGITALOCEAN_DOMAIN_NAME | none    | true     | Domain name to update, example: `bar.com`             |
| DIGITALOCEAN_SUBDOMAIN   | none    | true     | Subdomain to update, example: `foo` (for foo.bar.com) |

### Cloudflare DNS

| Parameter              | Default | Required | Description                                   |
|------------------------|---------|----------|-----------------------------------------------|
| CLOUDFLARE_TOKEN       | none    | true     | Token to access Cloudflare API                |
| CLOUDFLARE_DOMAIN_NAME | none    | true     | Domain name to update, example: `foo.bar.com` |
| CLOUDFLARE_ZONE_ID     | none    | true     | Cloudflare Zone Id                            |

### Telegram channel for notifications

Set `TELEGRAM_TOKEN` and `TELEGRAM_CHAT_ID` environment variables to enable notifications about IP changes and warning.

| Parameter        | Default | Required | Description                                                                                                                |
|------------------|---------|----------|----------------------------------------------------------------------------------------------------------------------------|
| TELEGRAM_TOKEN   | none    | true     | Token to access Telegram API. Obtain an token using [@BotFather](https://t.me/BotFather)                                   |
| TELEGRAM_CHAT_ID | none    | true     | Chat ID to send messages to, start bot, and check https://api.telegram.org/bot[TELEGRAM_TOKEN]/getUpdates to obtain chat.  |

