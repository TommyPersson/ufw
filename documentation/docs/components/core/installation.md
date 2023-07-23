---
title: Installation
---

# Core: Installation

## Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-job-core" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-job-core</artifactId>
    <version>${ufw-version}</version> <!-- Disregard then using the BOM -->
</dependency>
```

## Normal Setup

### Configuration

```kotlin title="YourApp.kt" linenums="1"

import java.time.Clockval

ufw = UFW.build {
    core {
        clock = Clock.systemUTC()         // Mandatory
        meterRegistry = MyMeterRegistry() // Optional

        objectMapper {
            /* Jackson ObjectMapper configuration */
        }
    }
}
```

## Guice Setup

### Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-core-guice" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-core-guice</artifactId>
    <version>${ufw-version}</version> <!-- Disregard then using the BOM -->
</dependency>
```

### Configuration

Add an instance of `CoreGuiceModule` to the Guice injector. Specify the `scanPackages` parameter to allow the other UFW
components to scan for their classes. Specify the `configureObjectMapper` to customize the `ObjectMapper`.

Additionally, you must provide a binding for an `InstantSource`. Optionally,
provide a binding for a `MeterRegistry`.

```kotlin title="YourGuiceApp.kt" linenums="1"
val injector = Guice.createInjector(
    Module {
        it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())

        OptionalBinder.newOptionalBinder(it, MeterRegistry::class.java)
            .setBinding().toInstance(/* MeterRegistry instance */)
    },
    CoreGuiceModule(
        scanPackages = setOf("com.example.myguiceapp"),
        configureObjectMapper = {
            /* Jackson ObjectMapper configuration */
        }
    ),
)
```

