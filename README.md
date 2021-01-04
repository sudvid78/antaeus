## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

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
 
My Approach 😁

## Functional Business Requirements realized/assumed
*   Charge customers of different markets(US,EUR etc) on Day one of every month
*   Customers with different currency involved here which means scheduling to be trigerred according to different timezones
*   If charging fails on Day one of month , then attempt a retry operation for certain number of times
*   Send reminder mails during between an unsuccessful charging until say, next 14 days or so
*   Send reminder mail for next month billing for invoices with PAID status 

## Approach
````
Start the timer for 2 timezones, America and Europe market and all customers in Europe zone are mapped to Denmark time zone and 
American customers to America timezone. Both timer runs once for every 24 hours and process invoices asyncronously in the order below,
##On DAY==1, 
    1. fetch all Invoices with status = PENDING in batch and submit to the Runnable class 'charge'. The class 'charge' process the invoices in below order:      
    the Runnable task 'charge' 2. check for
        1. check for low balance and abort the operation if low balance and update the invoice status to Retry  
        2. attempts to charge the invoice and update the status as PAID
        3. If failed to charge, update the status to RETRY if retry count is <= maxretrycount else mark the status as Failed 
           which prevents from picking up the invoice for next billing. this might help to take some action for those invoices.
        4. While attempting to charge, guard with client idempotency key to make sure the exactly once semantics
    

##On DAY == 25,
        This is to prepare all the PAID invoices for next month billing. 
    1. fetch all the invoice with status = PAID , send a reminder mail for next month and update the status to PENDING , 
        so that the scheduler can start charging
##From DAY ==2 until DAY ==14,
    Fetch the invoices with status = RETRY , send a mail that payment is due and attempt to charge for the next 14 days or so.

refer io.pleo.antaeus.core.services.helper.kt for RUnnable task charge and reminder   
````

## Code Compilation and Execution Status - Successfully executed
 
##Tests
Successfully passed test scripts for pending invoices and retry invoices

## Note - Things simplified for demo purpose 
Most of the configuration like, mapping the currency to timezone, Retry count, batchSize etc are 
hardcoded / simplified for demo purposes. Ideally these vars are to be updated in DM via an admin console
Similarly generating idempotency key and sending email is very much simplified for demo purpose 

## Area for improvement/ Non-functional requirements. 
1. Submitting the tasks for charging could be scaled by posting the submit tasks messages
   to an Distributed/Enterprise level pub/sub Queue such as KAFKA
2. Implement an Alerting system like Prometheus to raise Functional alerts during failure billing or Invoices getting FAILED 
    even after retried after certain number of times 
3. Multiple payment provider could be deployed to speed up the Charging operations    

