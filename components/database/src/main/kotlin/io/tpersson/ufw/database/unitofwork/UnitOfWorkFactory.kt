package io.tpersson.ufw.database.unitofwork

public interface UnitOfWorkFactory {
    public fun create(): UnitOfWork
}