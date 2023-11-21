---
title: Installation
---

# Key-Value Store: Installation

## Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-key-value-store" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-key-value-store</artifactId>
    <version>${ufw-version}</version> <!-- Disregard when using the BOM -->
</dependency>
```

## Normal Setup

### Configuration

Use the config DSL to enable the Key-Value Store.

```kotlin title="YourApp.kt" linenums="1" hl_lines="2-4"
ufw = UFW.build {
    keyValueStore {
        expiredEntryReapingInterval = Duration.fromSeconds(30)
    }
}
```

## Guice Setup

### Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-key-value-store-guice" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-key-value-store-guice</artifactId>
    <version>${ufw-version}</version> <!-- Disregard when using the BOM -->
</dependency>
```

### Configuration

Add an instance of `KeyValueStoreGuiceModule` to the Guice injector.

```kotlin title="YourGuiceApp.kt" linenums="1" hl_lines="11"
val injector = Guice.createInjector(
    Module {
        it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())
        it.bind(DataSource::class.java).toInstance(MyDataSource())
    },
    CoreGuiceModule(
        scanPackages = setOf("com.example.myguiceapp"),
    ),
    ManagedGuiceModule(),
    DatabaseGuiceModule(),
    KeyValueStoreGuiceModule(...),
)
```
