package io.tpersson.ufw.database.typedqueries

import io.tpersson.ufw.database.typedqueries.internal.RowEntityMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RowEntityMapperTest {
    @Test
    fun `Maps from snake_case row keys to camelCase properties`() {
        val row = mapOf(
            "test_property" to "a value"
        )

        data class TestEntity(val testProperty: String)

        val mapper = RowEntityMapper(TestEntity::class)

        val result = mapper.map(row)

        assertThat(result).isEqualTo(TestEntity("a value"))
    }
}
