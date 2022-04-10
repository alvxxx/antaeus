## Chapter Two: MVP
In this chapter I will optimize the billing service algorithm, the current process works well and solve the problem, but in at certain quantity of invoices to charge, the process will take too long to charge all records. I also need to test the algorithm in a way near to the reality and now our billing service is mocked and the database is located on the same environment as the application, therefore on production usually we have a remote database that have some latency between read and write operations. With that said, I will mock some delay inside this two services, to emulate this environment. I also will measure the algorithm efficiency during this development, to know that I am going on the right way.

## 9 April 2022 - 5 hours
I added some threading blocking waits on PaymentProvider and on Dal layer as well to notice the Billing Service degrade with service latencies near to reality. After that, I deepened the knowledge in coroutines in Kotlin and read about the features like Flow, Channels, Suspended functions and SharedMemory to have more tools to solve the problem. I read the Exposed documentation trying to find any feature that could enable to have asynchronous calls on database, but I didn't find a result. So the option that I chose it was to transform the method that get pending invoices in database into a paginated version. With that I was able to dispatch multiple coroutines, each fetching one page to process billing charges. With that I was able to have a huge performance optimization on the billing process, but I created another, ensure that we have strictly one fetch to a page, without that, it will occur invoices being charged multiple times. The solution of this problem it was the usage of AtomicInteger, and done! After did that I decided to put all development in a separate branch called `spike` and I will recreate the algorithm using TDD to have a better understanding of the problem.

## Resume of chapter

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
