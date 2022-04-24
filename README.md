## Chapter Four: Bonus
After complete the challenge, I read the book Kotlin Design Patterns and Best Practices that approach some design patterns to build coroutines. On the book, there is a chapter that compares concurrency x parallelism, and I realized that the application was optimized through parallelism but when the process to fetch page was called, the thread was awaiting the response to continues the process and that's not the desired behavior. So I decided to go back to project, and make some changes. First I used the coroutine function `useContext` to run a code block in asynchronous way. So, I changed `fetchInvoicePageByStatus` and `updateInvoice`to apply the `useContext`. After that, I notice that I was using `runBlocking` that awaits the method to finish instead to suspending the coroutine and continue, so I changed each `runBlocking` to `coroutineScope`, except in the rest layer. I also did some changes to remove the boilerplate on using the `initAsyncProcessor` function. After all, the last change was add `"-opt-in=kotlin.RequiresOptIn"` on `build.gradle.kts` to suppress the warning when uses Kotlin Coroutine Test library. Lastly, I tested the overdue service applying a `Thread.sleep(60)` on `fetchInvoicePageByStatus` and `updateInvoice` with and without `useContext` and that it was the results:

| Using                             | Time taken |
|:----------------------------------|:----------:|
| `runBlocking` only                | 9 seconds  |
| `coroutineScope` only             | 5 seconds  |
| `useContext` and `coroutineScope` | 2 seconds  |

Ps: Total invoices marked as overdue was 52 in each case.

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
