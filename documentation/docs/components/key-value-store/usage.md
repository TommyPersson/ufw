---
title: Usage
---

# Key-Value Store: Usage

## Reading and Writing values

First, define a `KeyValueStore.Key<T>` for your entry, then use the `KeyValueStore` interfaces' `get` and `put` methods
to read and write values for your entry.

The Key-Value Store can read and write any JSON serializable (using Jackson) value. The type of value is determined by
the type provided when creating the key.

```kotlin title="Example.kt: Reading and Writing" linenums="1"
fun example() {
    val keyValueStore: KeyValueStore = ufw.keyValueStore.keyValueStore // Or @Inject using Guice

    val myKey = KeyValueStore.Key.of<Int>("my-key")

    keyValueStore.put(myKey, 123)

    val entry: Entry<Int>? = keyValueStore.get(myKey)

    println(entry?.value) // "123" 
}
```

## Listing entries

The `KeyValueStore.list(prefix, limit, offset)` methods allows for listing any entries whose key names match the given
prefix. The resulting entries are unparsed and will require a call to `UnparsedEntry.parseAs(KClass)` to get the actual
value.

```kotlin title="Example.kt: Listing Entries" linenums="1"
fun example() {
    val keyValueStore: KeyValueStore = ufw.keyValueStore.keyValueStore // Or @Inject using Guice

    val counterAKey = KeyValueStore.Key.of<Int>("counter:a")
    val counterBKey = KeyValueStore.Key.of<Int>("counter:b")

    keyValueStore.put(counterAKey, 1)
    keyValueStore.put(counterBKey, 2)

    val counterEntries: List<UnparsedEntry> = keyValueStore.list("counter:", limit = 100)

    val counterValues = counterEntries.map { it.parseAs(Int::class) }

    println(counterValues) // "[1, 2]" 
}
```

## Transactions with `UnitOfWork`

`KeyValueStore.put` accepts an optional `UnitOfWork` parameter. Any failed writes will cause the `UnitOfWork` to
rollback.

```kotlin title="Example.kt: UnitOfWork"
fun example() {
    val keyValueStore: KeyValueStore = ufw.keyValueStore.keyValueStore // Or @Inject using Guice

    val counterAKey = KeyValueStore.Key.of<Int>("counter:a")
    val counterBKey = KeyValueStore.Key.of<Int>("counter:b")

    val unitOfWork = unitOfWorkFactory.create()

    keyValueStore.put(counterAKey, 1, unitOfWork = unitOfWork)
    keyValueStore.put(counterBKey, 2, unitOfWork = unitOfWork)

    unitOfWork.commit()
}
```

## Entry Versioning

`KeyValueStore.put` accepts an optional `expectedVersion` parameter. If this version does not match the current entry,
then any `put` operation will fail. The exception type thrown is `VersionMismatchException`.

This is useful to prevent concurrent writes overwriting the same entry.

```kotlin title="Example.kt: Versioning"
fun example() {
    val keyValueStore: KeyValueStore = ufw.keyValueStore.keyValueStore // Or @Inject using Guice

    val myKey = KeyValueStore.Key.of<Int>("my-key")

    keyValueStore.put(myKey, 123) // succeeds
    keyValueStore.put(myKey, 456, expectedVersion = 2) // throws, since version is currently `1`
}
```

## Entry Expiration

`KeyValueStore.put` accepts an optional `ttl` parameter. If specified, the Key-Value Store will automatically delete the
entry after the time has passed.

This can be useful for implementing caches shared between application instances.

```kotlin title="Example.kt: Expiration" linenums="1"
fun example() {
    val keyValueStore: KeyValueStore = ufw.keyValueStore.keyValueStore // Or @Inject using Guice

    val myKey = KeyValueStore.Key.of<Int>("my-key")

    keyValueStore.put(myKey, 123, ttl = Duration.ofMinutes(10))

    // 10 minutes later

    val entry: Entry<Int>? = keyValueStore.get(myKey)

    println(entry?.value) // null 
}
```

!!! note

    The actual entry deletion is performed by a background process executing according to `KeyValueStoreConfig.expiredEntryReapingInterval`.
    This means an entry may not have actually been deleted immediately after the supplied `ttl` value.
    
    A timestamp is therefore checked on each read to prevent these stale entries from being read.