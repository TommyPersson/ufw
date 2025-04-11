package io.tpersson.ufw.database.unitofwork

public interface UnitOfWorkFactory {
    public fun create(): UnitOfWork
}

public suspend fun <T> UnitOfWorkFactory.use(block: suspend (UnitOfWork) -> T): T {
    val unitOfWork = create()
    val result = block(unitOfWork)
    unitOfWork.commit()
    return result
}

public suspend fun <T> UnitOfWorkFactory.extendOrCommit(unitOfWork: UnitOfWork?, block: suspend (unitOfWork: UnitOfWork) -> T): T {
    val isLocal = unitOfWork == null
    val uow = unitOfWork ?: create()

    return try {
        block(uow)
    } finally {
        if (isLocal) {
            uow.commit()
        }
    }
}