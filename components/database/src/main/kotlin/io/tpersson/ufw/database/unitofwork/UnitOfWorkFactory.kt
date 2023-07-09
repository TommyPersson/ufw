package io.tpersson.ufw.database.unitofwork

public interface UnitOfWorkFactory {
    public fun create(): UnitOfWork
}

public suspend fun UnitOfWorkFactory.use(block: suspend (UnitOfWork) -> Unit) {
    val unitOfWork = create()
    block(unitOfWork)
    unitOfWork.commit()
}