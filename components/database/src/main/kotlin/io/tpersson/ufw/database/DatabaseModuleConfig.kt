package io.tpersson.ufw.database

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

public data class DatabaseModuleConfig(
    public val ioContext: CoroutineContext = Dispatchers.IO
) {
    public companion object {
        public val Default: DatabaseModuleConfig = DatabaseModuleConfig()
    }
}