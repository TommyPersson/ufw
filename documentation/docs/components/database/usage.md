---
title: Usage
---

# Database: Usage

## Typed queries

As mentioned in the [Introduction](./index.md), the Database component does not depend on any specific database library.
Instead, it provides its own set of minimal constructs to make querying easier. They are somewhat limited at the moment,
so you should not hesitate to bring a more full-featured library if necessary.

Queries are implemented by inheriting one of the following base classes. Each is responsible for translating the 
high-level query and parameters to an executable JDCB `PreparedStatement` upon execution. Their advantage over using
`PreparedStatement`s directly is that they can be passed around without an active `Connection` and provide a easier way 
to specify query parameters.

### Typed query variants

* `TypedSelectSingle<T>`
: Returns `T?`.     

* `TypedSelectList<T>`
: Returns `List<T>`.

* `TypedUpdate`
: Returns `Int` for the number of affected rows.<br /> 
  Can be included in a `UnitOfWork`.<br /> 
  Allows a `minimumAffectedRows` to be specified.<br />

* `TypedUpdateReturningSingle<T>`
: Supports the `UPDATE ... RETURNING` syntax. <br />
  Returns `T?`. <br />
  Allows a `minimumAffectedRows` to be specified.<br />

* `TypedupdateReturningList<T>`
: Supports the `UPDATE ... RETURNING` syntax. <br />
  Returns `List<T>`.<br />
  Allows a `minimumAffectedRows` to be specified.<br />

If the `minimumAffectedRows` (default: `1`) is not reached for a update query, then a `MiniumumAffectedRowsException`
is thrown. 

### Examples

```kotlin title="Example: Using TypedSelectSingle" linenums="1"
data class MyExampleResult(
    val thing: String
)

data class MyExampleSelect(
    val param1: String
) : TypedSelectSingle<MyExampleResult>(
    "SELECT * FROM stuff WHERE column = :param1"
)

fun example() {
    val database: Database = ufw.database.database

    val result: MyExampleResult? = database.select(MyExampleSelect("param1Value"))
}
```

```kotlin title="Example: Using TypedUpdate" linenums="1"
data class MyExampleUpdate(
    val param1: String
) : TypedUpdate(
    "INSERT INTO stuff (stuff) VALUES (:param1)"
)

fun example() {
    val database: Database = ufw.database.database

    val affectedRows: Int = database.update(MyExampleUpdate("param1Value"))
}
```

### High-level SQL

TODO

## Transactions using `UnitOfWorkFactory` and `UnitOfWork`

The `UnitOfWorkFactory` is responsible for creating `UnitOfWork` instances. The `UnitOfWork` instances may be populated
with a set of update queries, either `TypedUpdate`s or functions with the signature `(Connection) -> PreparedStatement`.
The update statements are then executed by calling `UnitOfWork.commit`. If any statements fail, the transaction is
automatically rolled back and any `SQLException` is rethrown. The transaction is also aborted if any of the updates
throw a `MinimumAffectedRowsException`.

```kotlin title="Example: Using UnitOfWorkFactory"
fun example() {
    val unitOfWorkFactory = ufw.database.unitOfWorkFactory
    val unitOfWork = unitOfWorkFactory.create()

    unitOfWork.add(MyUpdate1("param"))
    unitOfWork.add(MyUpdate2("param"))
    unitOfWork.add(MyUpdate3("param"))

    unitOfWork.commit()
}
```

The `UnitOfWorkFactory` also has a `use`-method, which can be used instead of separate `create` and `commit`.

```kotlin title="Example: UnitOfWorkFactory.use"
fun example() {
    val unitOfWorkFactory = ufw.database.unitOfWorkFactory
    unitOfWorkFactory.use { uow ->
        uow.add(MyUpdate1("param"))
        uow.add(MyUpdate2("param"))
        uow.add(MyUpdate3("param"))
    }
}
```

Additionally, `UnitOfWork` as a `addPostCommitHook`-method that can be used to schedule a lambda to execute upon
successful commit.

```kotlin title="Example: UnitOfWork.addPostCommitHook"
fun example() {
    val unitOfWorkFactory = ufw.database.unitOfWorkFactory
    unitOfWorkFactory.use { uow ->
        uow.add(MyUpdate1("param"))
        uow.add(MyUpdate2("param"))
        uow.add(MyUpdate3("param"))
        uow.addPostCommitHook {
            println("Updated all items!")
        }
    }
}
```

## Database locks with `DatabaseLocks`

`DatabaseLocks` provides a way to get database-backed locks that can only be acquired a by a single `instanceId` at a
time. This can be useful to implement locking patterns across application instances by giving each a unique ID.

Each lock has an associated timestamp for when the lock was last acquired or refreshed. When acquiring a lock, you can
specify a parameter which will allow the lock to be stolen if the timestamp is sufficiently old.

Calling `LockHandle.refresh` will update this timestamp and prevent the lock from being stolen. If the lock has already
been lost, then the `refresh`-method returns false.

```kotlin title="Example: Using DatabaseLocks" linenums="1"
val appInstanceId = UUID.randomUUID().toString()

fun example() {
    val databaseLocks: DatabaseLocks = ufw.database.locks

    val myLock: DatabaseLock = databaseLocks.create("my-lock", instanceId = appInstanceId)
    val lockHandle: DatabaseLockHandle = myLock.tryAcquire(stealIfOlderThan = Duration.ofMinutes(5))

    for (i in 1..100) {
        if (!lockHandle.refresh()) {
            // We lost the lock
            break
        }

        /* do task */
    }

    lockHandle.release()
}
```