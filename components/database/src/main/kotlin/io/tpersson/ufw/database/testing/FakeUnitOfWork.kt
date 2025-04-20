package io.tpersson.ufw.database.testing

import io.tpersson.ufw.database.typedqueries.TypedUpdate
import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.sql.Connection
import java.sql.PreparedStatement

public class FakeUnitOfWork : UnitOfWork {
    private val preCommitHooks = HashSet<suspend () -> Unit>()
    private val postCommitHooks = HashSet<suspend () -> Unit>()

    override fun add(minimumAffectedRows: Int, block: (Connection) -> PreparedStatement) {
    }

    override fun add(update: TypedUpdate, exceptionMapper: (Exception) -> Exception) {
    }

    override fun addPreCommitHook(block: suspend () -> Unit) {
        preCommitHooks += block
    }

    override fun addPostCommitHook(block: suspend () -> Unit) {
        postCommitHooks += block
    }

    override suspend fun commit() {
        preCommitHooks.forEach { it() }
        postCommitHooks.forEach { it() }
    }
}