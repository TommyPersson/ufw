---
title: Installation
---

# Managed: Installation

## Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-managed" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-managed</artifactId>
    <version>${ufw-version}</version> <!-- Disregard when using the BOM -->
</dependency>
```

## Normal Setup

### Configuration

You have two options for registering instances, either by providing them immediately on initial component build, or by
registering them afterwards but before they are started.

```kotlin title="YourApp.kt: Early registration" linenums="1" hl_lines="2-4"
ufw = UFW.build {
    managed {
        instances = setOf(MyManaged())
    }
}
```

```kotlin title="YourApp.kt: Late registration" linenums="1" hl_lines="3"
ufw = UFW.build { ... }

ufw.managed.register(MyManaged())
```

### Initialization

Start all instances by calling `ManagedComponent.startAll`. `startAll` will, unless configured otherwise, also register
a JVM shutdown hook to automatically call `ManagedComponent.stopAll()` upon shutdown.

```kotlin title="YourApp.kt" linenums="1" hl_lines="5-7"
ufw = UFW.build { ... }

ufw.managed.startAll(addShutdownHook = true) // default = true
```

## Guice Setup

### Maven Package

```xml title="pom.xml: io.tpersson.ufw:ufw-managed-guice" linenums="1"

<dependency>
    <groupId>io.tpersson.ufw</groupId>
    <artifactId>ufw-managed-guice</artifactId>
    <version>${ufw-version}</version> <!-- Disregard when using the BOM -->
</dependency>
```

### Configuration

Add an instance of `ManagedGuiceModule` to the Guice injector. It will automatically scan your `classpath` and register
any `Managed`-derived classes.

```kotlin title="YourGuiceApp.kt" linenums="1" hl_lines="9"
val injector = Guice.createInjector(
    Module {
        it.bind(InstantSource::class.java).toInstance(Clock.systemUTC())
        it.bind(DataSource::class.java).toInstance(MyDataSource())
    },
    CoreGuiceModule(
        scanPackages = setOf("com.example.myguiceapp"),
    ),
    ManagedGuiceModule()
)
```

### Initialization

Similar to the Guice-less variant, get the instance for your `ManagedComponent` and invoke `startAll`.

```kotlin title="Example: Initialization" linenums="1" hl_lines="2"
val managedRunner = injector.getInstance(ManagedComponent::class.java).managedRunner
managedRunner.startAll(addShutdownHook = true) // default = true
```