package io.tpersson.ufw.durablecaches

import org.intellij.lang.annotations.Language
import java.time.Duration
import kotlin.reflect.KClass

public data class DurableCacheDefinition<TValue : Any>(
    public val id: String,
    public val title: String,
    @Language("Markdown")
    public val description: String,
    public val valueType: KClass<out TValue>,
    public val expiration: Duration?,
    public val inMemoryExpiration: Duration, // TODO use
)