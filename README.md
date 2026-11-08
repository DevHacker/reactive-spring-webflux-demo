# Reactive Spring WebFlux

A multi-module Gradle project that explores reactive, non-blocking microservices with **Spring WebFlux**, **Project Reactor** and **Reactive MongoDB**.

It is made of three cooperating microservices that together expose a "movie" API, plus a standalone module for learning Reactor operators.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.3-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.7.1-02303A)
![MongoDB](https://img.shields.io/badge/MongoDB-reactive-47A248)

---

## Table of Contents

- [Architecture](#architecture)
- [Modules](#modules)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Error Handling & Resilience](#error-handling--resilience)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Architecture

```
                    ┌──────────────────────────┐
     client ──────▶ │      movies-service      │  :8083
                    │  (aggregator / gateway)  │
                    └────────────┬─────────────┘
                     WebClient   │   WebClient + Resilience4j circuit breaker
              ┌──────────────────┴──────────────────┐
              ▼                                     ▼
┌──────────────────────────┐          ┌──────────────────────────┐
│    movie-info-service    │  :8080   │  movies-review-service   │  :8082
│  annotated controllers   │          │   functional endpoints   │
└────────────┬─────────────┘          └────────────┬─────────────┘
             ▼                                     ▼
        MongoDB (reactive)                    MongoDB (reactive)
```

`movies-service` fetches a movie's info from `movie-info-service`, then its reviews from `movies-review-service`, and combines both into one `Movie` response. Every call is non-blocking end to end.

---

## Modules

| Module | Port | Description |
|---|---|---|
| [`movie-info-service`](movie-info-service) | `8080` | CRUD API for movie information (name, year, cast, release date). Built with `@RestController`, Bean Validation and Reactive MongoDB. Supports `local`, `dev` and `prod` profiles. |
| [`movies-review-service`](movies-review-service) | `8082` | CRUD API for movie reviews. Built with the **functional** WebFlux style (`RouterFunction` + handler), with custom request validation and a global error handler. |
| [`movies-service`](movies-service) | `8083` | Aggregator service. Calls the two services above through a tuned `WebClient` (connection pool, timeouts, logging filters) and protects the reviews call with a **Resilience4j circuit breaker**. |
| [`reactive-programming-using-reactor`](reactive-programming-using-reactor) | — | Learning module covering `Flux` / `Mono` basics and operators (`map`, `flatMap`, `delayElements`, `merge`, `zip`, ...) with `StepVerifier` tests. |

---

## Tech Stack

- **Java 21** (Java 17 for the Reactor learning module)
- **Spring Boot 4.1.1** with Spring WebFlux, running on Netty
- **Spring Data MongoDB Reactive**
- **Spring Cloud 2025.1.3** with Circuit Breaker (Resilience4j, Reactor)
- **Project Reactor** (`reactor-core`, `reactor-test`, `reactor-tools`)
- **Jakarta Bean Validation**
- **Lombok**
- **JUnit 5**, **Mockito**, `WebTestClient`, `StepVerifier`
- **Gradle 9.7.1** (wrapper included)

---

## Prerequisites

- JDK 21 or later
- A running MongoDB instance on `localhost:27017`

Quickest way to start MongoDB:

```bash
docker run -d --name mongodb -p 27017:27017 mongo:latest
```

---

## Getting Started

### 1. Clone

```bash
git clone https://github.com/DevHacker/reactive-spring-webflux-demo.git
cd reactive-spring-webflux-demo
```

### 2. Build

```bash
./gradlew build -x test
```

### 3. Run the services

Start each service in its own terminal. Start the two downstream services before `movies-service`.

```bash
./gradlew :movie-info-service:bootRun
./gradlew :movies-review-service:bootRun
./gradlew :movies-service:bootRun
```

To run `movie-info-service` with another profile:

```bash
./gradlew :movie-info-service:bootRun --args='--spring.profiles.active=dev'
```

### 4. Seed some data

```bash
# Movie info
curl -i -H "Content-Type: application/json" \
  -d '{"name":"Batman Begins","year":2005,"cast":["Christian Bale","Michael Caine"],"releaseDate":"2005-06-15"}' \
  -X POST http://localhost:8080/api/movieinfos

# Review (use the movieInfoId returned above)
curl -i -H "Content-Type: application/json" \
  -d '{"movieInfoId":1,"comment":"Awesome Movie","rating":9.0}' \
  -X POST http://localhost:8082/api/reviews
```

More sample commands are in [`movies-service/src/main/resources/curl-commands.txt`](movies-service/src/main/resources/curl-commands.txt).

---

## API Reference

### movie-info-service — `http://localhost:8080`

| Method | Endpoint | Description | Success |
|---|---|---|---|
| `POST` | `/api/movieinfos` | Create a movie info (validated) | `201 Created` |
| `GET` | `/api/movieinfos` | List all movie infos | `200 OK` |
| `GET` | `/api/movieinfos?year={year}` | List movie infos for a year | `200 OK` |
| `GET` | `/api/movieinfos/{id}` | Get a movie info by id | `200 OK` / `404` |
| `PUT` | `/api/movieinfos/{id}` | Update a movie info | `200 OK` / `404` |
| `DELETE` | `/api/movieinfos/{id}` | Delete a movie info | `204 No Content` / `404` |

**MovieInfo**

```json
{
  "movieInfoId": "string",
  "name": "Batman Begins",
  "year": 2005,
  "cast": ["Christian Bale", "Michael Caine"],
  "releaseDate": "2005-06-15"
}
```

Validation: `name` is required. `year` is required and must be positive. `cast` entries must not be blank.

### movies-review-service — `http://localhost:8082`

| Method | Endpoint | Description | Success |
|---|---|---|---|
| `POST` | `/api/reviews` | Create a review (validated) | `201 Created` |
| `GET` | `/api/reviews` | List all reviews | `200 OK` |
| `GET` | `/api/reviews/{movieInfoId}` | List reviews for a movie | `200 OK` / `404` |
| `PUT` | `/api/reviews/{id}` | Update a review's comment and rating | `200 OK` / `404` |
| `DELETE` | `/api/reviews/{id}` | Delete a review | `200 OK` / `404` |

**Review**

```json
{
  "reviewId": "string",
  "movieInfoId": 1,
  "comment": "Awesome Movie",
  "rating": 9.0
}
```

### movies-service — `http://localhost:8083`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/v1/movies/{movieId}` | Returns the movie info together with all its reviews |

**Movie**

```json
{
  "movieInfo": { "movieInfoId": "...", "name": "Batman Begins", "year": 2005, "cast": ["..."], "releaseDate": "2005-06-15" },
  "reviewList": [ { "reviewId": "...", "movieInfoId": 1, "comment": "Awesome Movie", "rating": 9.0 } ]
}
```

Downstream base URLs are set in `movies-service/src/main/resources/application.yml`:

```yaml
restClient:
  moviesInfo:
    baseUrl: http://localhost:8080/api/movieinfos
  reviews:
    baseUrl: http://localhost:8082/api/reviews
```

---

## Error Handling & Resilience

- **Global error handlers** in each service turn exceptions into consistent HTTP responses: validation errors become `400`, missing resources become `404`.
- **Client and server errors are kept apart** in `movies-service`: a 4xx from a downstream service raises `*ClientException`, a 5xx raises `*ServerException`.
- **Circuit breaker** on the reviews call (`ResilienceConfig`):
  - opens when 50% of calls fail, over a sliding window of 10 calls
  - stays open for 2 seconds before it tries again
  - counts 5xx errors and timeouts as failures
  - **ignores** 4xx errors (`ReviewNotFoundException`, `ReviewsClientException`), so bad user input never opens the circuit
- **WebClient tuning** (`WebClientConfig`): pool of 100 connections, 3 s connect timeout, 2 s response timeout, 10 s read and write timeouts, gzip compression, and request/response logging that never logs headers.

---

## Testing

Run every test:

```bash
./gradlew test
```

Or a single module:

```bash
./gradlew :movie-info-service:test
./gradlew :movies-review-service:test
./gradlew :reactive-programming-using-reactor:test
```

| Module | Tests |
|---|---|
| `movie-info-service` | Repository integration tests, controller integration tests (`WebTestClient`), controller unit tests (mocked service), service tests |
| `movies-review-service` | Router and handler unit tests |
| `reactive-programming-using-reactor` | Operator tests with `StepVerifier` |

> The integration tests in `movie-info-service` use the `test` profile and need MongoDB running on `localhost:27017`.

---

## Project Structure

```
reactive-spring-webflux/
├── movie-info-service/                 # Movie info CRUD (annotated controllers)
│   └── src/main/java/com/gtechsolutions/movieinfoservice/
│       ├── controller/  domain/  exception/  exceptionHandler/
│       ├── repository/  service/
├── movies-review-service/              # Reviews CRUD (functional endpoints)
│   └── src/main/java/com/reactivespring/
│       ├── router/  handler/  domain/  dto/
│       ├── exception/  repository/  utils/
├── movies-service/                     # Aggregator with WebClient + circuit breaker
│   └── src/main/java/com/reactivespring/
│       ├── controller/  clients/  config/
│       ├── domain/  exception/
├── reactive-programming-using-reactor/ # Reactor learning module
├── gradle/wrapper/
├── settings.gradle  gradlew  gradlew.bat
└── README.md
```

---

## Author

**Fidèle Donvide** — [GitHub](https://github.com/DevHacker)
