package io.tpersson.ufw.core

public interface AppInfoProvider {
    public suspend fun get(): AppInfo

    public companion object {
        public fun simple(
            name: String = "unknown",
            version: String = "unknown",
            environment: String = "unknown"
        ): AppInfoProvider {
            return SimpleAppInfoProvider(AppInfo(name, version, environment))
        }
    }
}
