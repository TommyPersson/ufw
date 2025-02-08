package io.tpersson.ufw.aggregates.utils

import io.tpersson.ufw.aggregates.Entity

public class EntityBag<T : Entity<TId>, TId>(
    initial: List<T>
) : Collection<T> {

    private val original = ArrayList(initial)
    private val current = original.associateTo(mutableMapOf<TId, Entry<T, TId>>()) { it.id to Entry.Existing(it) }

    private var _final: List<T> = original
    private var _added: List<T> = emptyList()
    private var _removed: List<T> = emptyList()

    private var isDirty: Boolean = false

    public fun add(element: T): Unit {
        if (current.containsKey(element.id)) {
            throw IllegalStateException("Bag already contains an entry for entity ID: ${element.id}")
        }

        current[element.id] = Entry.Added(element)
        isDirty = true
    }

    public fun addAll(elements: List<T>): Unit {
        for (element in elements) {
            add(element)
        }
    }

    public fun remove(id: TId) {
        val existing = current[id]
        if (existing != null) {
            current[id] = Entry.Removed(existing.value)
            isDirty = true
        }
    }

    private fun refreshCollections() {
        if (isDirty) {
            _final = current.values.filter { it !is Entry.Removed }.map { it.value }
            _added = current.values.filterIsInstance<Entry.Added<T, TId>>().map { it.value }
            _removed = current.values.filterIsInstance<Entry.Removed<T, TId>>().map { it.value }
            isDirty = false
        }
    }

    private val final: Collection<T>
        get() {
            refreshCollections()
            return _final
        }

    public val added: Collection<T>
        get() {
            refreshCollections()
            return _added
        }

    public val removed: Collection<T>
        get() {
            refreshCollections()
            return _removed
        }

    override val size: Int get() = final.size

    override fun isEmpty(): Boolean = final.isEmpty()

    override fun iterator(): Iterator<T> = final.iterator()

    override fun containsAll(elements: Collection<T>): Boolean = final.containsAll(elements)

    override fun contains(element: T): Boolean = final.contains(element)

    public fun containsId(id: TId): Boolean {
        val entry = current[id]
        return entry != null && entry !is Entry.Removed
    }

    public sealed class Entry<T : Entity<TId>, TId>(
        public val value: T
    ) {
        public class Added<T : Entity<TId>, TId>(value: T) : Entry<T, TId>(value)
        public class Removed<T : Entity<TId>, TId>(value: T) : Entry<T, TId>(value)
        public class Existing<T : Entity<TId>, TId>(value: T) : Entry<T, TId>(value)
    }
}