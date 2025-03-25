package io.tpersson.ufw.aggregates.guice

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Injector
import io.github.classgraph.ScanResult
import io.tpersson.ufw.aggregates.AggregateRepository
import io.tpersson.ufw.aggregates.internal.AggregateRepositoryProvider
import io.tpersson.ufw.core.NamedBindings
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
public class GuiceAggregateRepositoryProvider @Inject constructor(
    @Named(NamedBindings.ScanResult) private val scanResult: ScanResult,
    private val injector: Injector,
) : AggregateRepositoryProvider {

    private val repositories = scanResult.allClasses
        .filter { it.implementsInterface(AggregateRepository::class.java) }
        .filter { !it.isAbstract }
        .map { it.loadClass() }
        .map { injector.getInstance(it) as AggregateRepository<*, *> }
        .toList()

    override fun add(repository: AggregateRepository<*, *>) {
        throw IllegalStateException("'add' is not available for 'GuiceAggregateRepositoryProvider'")
    }

    override fun getAll(): List<AggregateRepository<*, *>> {
        return repositories
    }
}