package io.tpersson.ufw.core

public class SimpleAppInfoProvider(
    private val info: AppInfo
) : AppInfoProvider {
    override suspend fun get(): AppInfo {
        return info
    }
}