## Chapter Three: Resilience Optimization
I will create a retry policy on failures invoices that the charge was declined or that thrown NetworkException. The other ones I will mark them as `uncollectible`. At the beginning of the second day of month I will execute another process that will mark declined charges as `overdue`.

## 10 April 2022 - 3 hours
I started the day, reviewing the current solution, trying to find any points of improvement before start the retry policy solution. I notice that the current solution was holding the http connection waiting for response instead of dispatch job and let the execution happen on background, so I added a solution that I found on Javalin documentation. Then I marked invoice charges that thrown CustomerNotFoundException and CurrencyMismatchException as uncollectible, as there's no way of charge this kind of invoices on future retries. And in the end, I refactored the existing process to handle coroutines to reuse algorithm on overdue service.     

## 11 April 2022 - 5 hours
I refactored the FailureNotificator to be an EventNotificator that dispatches any important changes to interest parts. Then I triggered events to notify any status changes on invoices. When I finished the refactor, I notice that the BillingService was having too many responsibilities, so I moved the async iterator over pending invoices to AntaeusDal. Continuing the refactoring, I created a new class called InvoiceDomainService that now contains the code that notify events and the code that handle invoice status changes. With that big change, I needed to move tests to your respective layers and needed to rewrite some others.   

## Resume of chapter
It was simple to implement the retry policy logic, it only needed to dispatch the scheduler expression to execute 3 times on a day on AWS Event Bridge Rule. Therefore, we needed to create another rule to mark invoices as `overdue` to handle with invoices that could not be charged because it happened a soft problem like, lack of customer account balance or the Payment Provider that becomes unavailable on the day. I also provided another endpoint to retry charge a specific invoice to handle overdue scenarios as told before. I believe that the service is ready to production, and it has a good design to support future features and improvements.

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
