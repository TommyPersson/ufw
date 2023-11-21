---
title: Usage
---

# Managed: Usage

For an object to be registrable with the `ManagedComponent` it must extend either the `Managed` or `ManagedJob`
abstract classes.

## `Managed`

Extending the `Managed` class means implementing two template methods: `onStared` and `onStopped`.

```kotlin title="MyManaged.kt" linenums="1"
class MyManaged : Managed() {
    override suspend fun onStarted() {
        // start up code
    }

    override suspend fun onStopped() {
        // shutdown up code
    }
}
```

## `ManagedJob`

`ManagedJob` already extends `Managed` and overrides the `onStarted` and `onStopped` methods. In their place, it exposes
an overrideable `launch` template method. This launch method is executed within a Kotlin coroutine that is automatically
cancelled by `ManagedJob` when the instance is stopped.

It also provides a protected `isActive` property that is true as
long as the underlying coroutine is active.

```kotlin title="MyManagedJob.kt" linenums="1"

class MyManagedJob : ManagedJob() {
    override suspend fun launch() {
        while (isActive) {
            // perform recurring task
        }
    }
}
```

## Guice notes

Remember that Guice requires an `@Inject`-annotated constructor.

```kotlin title="MyGuiceManaged.kt" linenums="1"
class MyManaged @Inject constructor(): Managed() {
    ...
}
```