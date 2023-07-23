---
title: Installation
---

# Core: Installation

## Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-database" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-database</artifactId>
    <version>${ufw-version}</version> <!-- Disregard then using the BOM -->
</dependency>
```

## Normal Setup

### Configuration

```kotlin title="YourApp.kt" linenums="1"
ufw = UFW.build {
    core {
        instantSource = Clock.systemUTC()
    }
    database {
        dataSource = MyDataSource() // Mandatory
    }
}
```

Preferably, your `DataSource` instance is some kind of JDBC connection pool.

### Initialization

Before initializing any other component, you must explicitly run any database migration scripts that they might have
registered. This is done by running the `DatabaseComponent.runMigrations` function.

```kotlin title="Example: Executing migration scripts" linenums="1"
val ufw = UFW.build { /* .. */ }
ufw.database.runMigrations()
```

## Guice Setup

### Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-database-guice" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-database-guice</artifactId>
    <version>${ufw-version}</version> <!-- Disregard then using the BOM -->
</dependency>
```

### Configuration

Add an instance of `DatabaseGuiceModule` to the Guice injector. The provide a binding for your `DataSource`.

```kotlin title="YourGuiceApp.kt" linenums="1"
val injector = Guice.createInjector(
    Module {
        it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())
        it.bind(DataSource::class.java).toInstance(MyDataSource())
    },
    CoreGuiceModule(
        scanPackages = setOf("com.example.myguiceapp"),
    ),
    DatabaseGuiceModule(),
)
```

### Initialization

Run the database migrations by first injecting an instance of the `DatabaseComponent`

```kotlin title="Example: Initialization" linenums="1"
val database = injector.getInstance(DatabaseComponent::class.java)
database.runMigrations()
```