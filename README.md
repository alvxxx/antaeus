## Talking about the solution I want to achieve
I will delegate the schedule process execution responsibility to an external service, the AWS Event Bridge. The billing service will listen an event in AWS SQS queue, that will dispatch the process execution. With asynchronous process handlers in mind, we need to ensure idempotency and a good failure handling.

## How I will manage the development process?
I will separate the development in chapters each one with your own branch and the main branch will contain the final solution.

## Chapter One: MVP
This chapter goal is to obtain a simple solution, creating as many tests as possible to reach stability for future improvements, as performance and resilience.

## 6 April 2022 - 3hs
Before start the development I studied Kotlin to recognize which features that could help me with the solution. I build some poc's with Flows, Channels, Streams. I also studied the docs of Exposed and MockK to understand how to create unit tests with Kotlin. After that I started creating some tests and the first behaviors.

## 7 April 2022 - 3hs
I finalized the development of a simple solution of the Billing Service. All the process is synchronous, because in the first moment I just want to build a great battery of tests to improve the solution in the next chapters. I also designed a failure notificator service, that will notify any interested part on errors. For now, the failure notificator only logs the error happened inside the billing process.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
