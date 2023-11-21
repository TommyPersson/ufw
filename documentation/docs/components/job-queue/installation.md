---
title: Installation
---

# Job Queue: Installation

## Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-job-queue" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-job-queue</artifactId>
    <version>${ufw-version}</version> <!-- Disregard when using the BOM -->
</dependency>
```

## Normal Setup

### Configuration

```kotlin title="YourApp.kt" hl_lines="11-16" linenums="1"
val ufw = UFW.build {
    // Configuration of other components are not shown
    jobQueue {
        configure {
            /* ... set configuration options ... */
        }
        handlers = setOf(/* ... your job handler instances ... */)
    }
}
```

In addition to specifying the `JobHandler`s inside of the `jobQueue` builder, you may also register them after
construction by using the `JobQueueComponent.register` function.

```kotlin title="Example: Registering a JobHandler after construction" linenums="1"
val myJobHandler: JobHandler = MyJobHandler()
ufw.jobQueue.register(myJobHandler)
```

### Initialization

Initialize the Job Queue and the rest of the UFW components by running the database migrator and the starting all
the `Managed` instances.

```kotlin title="Example: Initialization" linenums="1"
ufw.database.runMigrations()
ufw.managed.startAll(addShutdownHook = true) // default = true
```

!!! warning

    You cannot register additional `JobHandler`s after you've initialized all `Managed` instances
    with `ufw.managed.startAll()`.

## Guice Setup

### Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-job-queue-guice" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-job-queue-guice</artifactId>
    <version>${ufw-version}</version> <!-- Disregard when using the BOM -->
</dependency>
```

### Configuration

Add an instance of `JobQueueGuiceModule` to the Guice injector. The Guice-module will automatically scan
for `JobHandler` types in the packages that you set in `CoreGuiceModule.scanPackages`.

```kotlin title="YourGuiceApp.kt" hl_lines="11-15" linenums="1"
val injector = Guice.createInjector(
    Module {
        it.bind(DataSource::class.java).toInstance(/* your DataSource */)
        it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())
    },
    CoreGuiceModule(
        scanPackages = setOf("com.example.myguiceapp"),
    ),
    ManagedGuiceModule(),
    DatabaseGuiceModule(),
    JobQueueGuiceModule(
        config = JobQueueConfig(
            /* configuration options */
        )
    ),
)
```

### Initialization

Initialize the Job Queue and the rest of the UFW components by running the database migrator and the starting all
the `Managed` instances.

```kotlin title="Example: Initialization" linenums="1"
val database = injector.getInstance(DatabaseComponent::class.java)
database.runMigrations()

val managed = injector.getInstance(ManagedComponent::class.java)
managed.startAll(addShutdownHook = true)
```