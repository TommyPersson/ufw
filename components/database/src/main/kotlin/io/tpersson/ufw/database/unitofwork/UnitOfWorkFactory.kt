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