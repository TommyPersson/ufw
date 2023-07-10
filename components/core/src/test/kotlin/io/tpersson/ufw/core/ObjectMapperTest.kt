package io.tpersson.ufw.core

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class ObjectMapperTest {
    @Test
    fun `Can serialize and deserialize Instants as ISO-8601`() {
        val objectMapper = UFWObjectMapper.default.objectMapper

        val expectedString = "2020-02-02T12:02:02.002Z"
        val expectedInstant = Instant.parse(expectedString)

        val actual = objectMapper.writeValueAsString(expectedInstant)

        assertThat(actual).isEqualTo("\"$expectedString\"")

        val deserialized = objectMapper.readValue<Instant>(actual)
        
        assertThat(deserialized).isEqualTo(expectedInstant)
    }
}