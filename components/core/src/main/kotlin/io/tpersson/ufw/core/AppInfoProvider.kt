package io.tpersson.ufw.core

import java.util.UUID

public interface AppInfoProvider {
    public fun get(): AppInfo

    public companion object {
        public fun simple(
            name: String = "unknown",
            version: String = "unknown",
            environment: String = "unknown",
            instanceId: String = UUID.randomUUID().toString(),
        ): AppInfoProvider {
            return SimpleAppInfoProvider(AppInfo(name, version, environment, instanceId))
        }
    }


}
