## Talking about the final solution
I delegate the responsibility to schedule the process execution to an external service, the AWS Event Bridge. The billing service is triggered via http request, and the process starts the asynchronous execution. I decided to follow this approach because it's possible to manually trigger the endpoint if it is necessary. Therefore, there are some cares that needs to be taken. The endpoints exposed for manage billings is an administrative endpoint and should not be exposed to external clients of the api. This concern is simply handled with an api gateway. All the endpoints of billing is async, i.e: the request is responded with status code 202 only and the process execution continues on background. The billing process is executed on first weekday of each month at 6am, 9am and 3pm. All the invoices that couldn't be charged due a lack of account balance or NetworkException in these three attempts will be marked as `overdue`. Invoices that have an issue that needs human intervention to be executed successfully, like due of a currency mismatch between invoice and customer, is marked as `uncollectable`. The development process is organized in chapter, and each chapter has the description of what I did in each day.

| Chapters | Description                                                                                                                                                                                                       |                              Branch                              |
|:--------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------:|
|   one    | I was learning about the billing process and kotlin as well. I focused in create a simple solution of the problem with a good tests battery to support future changes.                                            |  **[Link](https://github.com/alvxxx/antaeus/tree/chapter-one)**  |
|   two    | I focused to optimize the algorithm performance using coroutines and shared variables to handle racing conditions.                                                                                                |  **[Link](https://github.com/alvxxx/antaeus/tree/chapter-two)**  |
|  three   | Optimized the resilience of the billing process, implementing a simple retry policy and process to handle inconsistency of status, i.e: mark invoices that could not be charged to `uncollectable` or `overdued`. | **[Link](https://github.com/alvxxx/antaeus/tree/chapter-three)** |
|  spike   | An auxiliary branch that helped me to find a solution for the performance optimization using coroutines before implementing the solution of the chapter two using *TDD*.                                          |     **[Link](https://github.com/alvxxx/antaeus/tree/spike)**     |
|   four   | A bonus chapter that I optimized the billing service for concurrency, after read the book "Kotlin Design Patterns and Best Practices".                                                                            | **[Link](https://github.com/alvxxx/antaeus/tree/chapter-four)**  |

All development process was developed using the following:

> #### Principles
- Small commits
- Single Responsibility Principle (SRP)
- Interface Segregation Principle (ISP)
- Dependency Inversion Principle (DIP)
- Don't Repeat Yourself (DRY)
- Keep It Simple, Silly (KISS)

> #### Methodologies and Design
- *TDD*
- Clean Architecture
- DDD (Some concepts approached on book, like Domain service)
- Conventional Commits

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
* [Localstack](https://localstack.cloud/) - A local AWS stack to develop and test services locally

Happy hacking 😁!
