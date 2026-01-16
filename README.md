# Spring Boot Email Service (RabbitMQ & Elasticsearch)

A microservice for centralized email distribution. Receives email messages asynchronously via RabbitMQ, stores them in Elasticsearch and attempts to send them via SMTP. A background task automatically resends failed mails every 5 minutes.

## Tech Stack

- Java 21
- Spring Boot 3.5.8
- RabbitMQ
- Elasticsearch
- Docker & Docker Compose 

## Running the Application

### Start services with Docker Compose

```bash
docker-compose up -d
```

This command will start all required services:
- **Elasticsearch** - Database - http://localhost:9200
- **RabbitMQ** - Message Broker - http://localhost:15672 (username: `rabbit`, password: `s@cr3t`)
- **Kibana** - Elasticsearch UI - http://localhost:5601
- **Email Service** - This microservice

#### Check container status

```bash
docker-compose ps
```

#### View service logs

```bash
docker-compose logs -f email-service
```

### Running Tests

```bash
mvn clean test
```

## Usage
### Integration to Spring application example: 
[Anime Spring REST API](https://github.com/KaterynaKunieva/anime-spring-rest-api/)

### Message Format
To send an email, publish a message to the `email-notifications-exchange` exchange in the following JSON format:
```json
{
  "recipient": "recipient@example.com",
  "subject": "Example email subject",
  "body": "Example email body"
}
```

### Email Statuses

| Status | Description |
|--------|-------------|
| `PENDING` | Message received, not yet sent |
| `SENT` | Successfully sent |
| `ERROR` | Error during sending, awaiting retry |

## Stopping Services

```bash
docker-compose down
```

To remove all data (including Elasticsearch volume):

```bash
docker-compose down -v
```