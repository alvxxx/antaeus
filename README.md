## Talking about the solution I want to achieve
I will delegate the schedule process execution responsibility to an external service, the AWS Event Bridge. The billing service will listen an event and will dispatch a request to a billing endpoint that will dispatch the process execution. With asynchronous process handlers in mind, I need to ensure idempotency, achieve a good failure handling and have a well-designed retry policy.

## How I will manage the development process?
I will separate the development in chapters each one with your own branch and the main branch will contain the final solution.

---
## Chapter One: MVP
This chapter goal is to obtain a simple solution, creating as many tests as possible to reach stability for future improvements, as performance and resilience. As I am still learning about the billing process, and I don't know in first-hand which are the load that this system will handle.  

## 6 April 2022 - 3hs
Before start the development I studied Kotlin to recognize which features that could help me with the solution. I build some poc's with Flows, Channels, Streams. I also studied the docs of Exposed and MockK to understand how to create unit tests with Kotlin. After that I started creating some tests and the first behaviors.

## 7 April 2022 - 3hs
I finalized the development of a simple solution of the Billing Service. All the process is synchronous, because in the first moment I just want to build a great battery of tests to improve the solution in the next chapters. I also designed a failure notificator service, that will notify any interested part on errors. For now, the failure notificator only logs the error happened inside the billing process. I decided in first place to only update charged invoices successfully to PAID, and leaves the declined charges to be handled by the failure notificator because I want to retry charge declined invoices and invoices that thrown NetworkException. This retry feature only will be developed in the end.

## 8 April 2022 - 4hs
First, I updated project dependencies, and I mounted the billing service on the main class. I also created an endpoint that triggers the billing charges. I took this decision, to enables the possibility to manually trigger the process in case of any failure and to become easier triggers the cron job via AWS Event Bridge. To finalize the work, I mocked a simple AWS cloud environment in a container using localstack, I used this to be able to test the AWS Event Bridge events triggering the billing endpoint. On the end, I validated successfully the simple billing service created and in the next chapter I will try to optimize the algorithm using asynchronous process handlers using Kotlin coroutines and flows.

## Resume of chapter
Took about 10 hours to develop the MVP of the solution. After finalize the development, on daily work I would deploy this solution on production as it is, but before I would add some observability tools like Datadog, Grafana and Logstash to collect information of this service in production. I also would prefer to create an infrastructure as a code with Terraform or Pulumi to be easier apply any infrastructure changes on future development cycles.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker and Docker Compose is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker, docker-compose and make for your platform

```
make start      # to starts the application
make stop      # to stop the application
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
