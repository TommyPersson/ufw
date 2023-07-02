package io.tpersson.ufw.db

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

public data class DbModuleConfig(
    public val ioContext: CoroutineContext = Dispatchers.IO
) {
    public companion object {
        public val Default: DbModuleConfig = DbModuleConfig()
    }
}