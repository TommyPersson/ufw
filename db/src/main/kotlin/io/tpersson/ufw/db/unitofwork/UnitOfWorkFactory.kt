package io.tpersson.ufw.db.unitofwork

public interface UnitOfWorkFactory {
    public fun create(): UnitOfWork
}