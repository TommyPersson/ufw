package io.tpersson.ufw.core.utils

import org.jmolecules.ddd.annotation.Module
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

// TODO add tests

public val DefaultModule: Module = Module(
    id = "default",
    name = "Default",
    description = "The default module *(generated)*",
)

public fun KClass<*>.findModuleMolecule(): Module {
    return this.java.findModuleMolecule()
}

public fun Class<*>.findModuleMolecule(): Module {
    return moduleMoleculeForPackageCache.getOrPut(this.packageName) {
        findModuleMoleculeForPackage(this.packageName, this.classLoader) ?: DefaultModule
    }
}

public fun findModuleMoleculeForPackage(packageName: String, classLoader: ClassLoader): Module? {
    val packageInfo = try {
        classLoader.loadClass("$packageName.package-info")
    } catch (e: Exception) {
        null
    }

    val annotation = packageInfo?.getAnnotation(Module::class.java)
    if (packageInfo == null || annotation == null) {
        val parentPackageName = packageName.substringBeforeLast('.')
        if (parentPackageName == packageName) {
            return null
        }

        return findModuleMoleculeForPackage(parentPackageName, classLoader)
    }

    return annotation
}

private val moduleMoleculeForPackageCache = ConcurrentHashMap<String, Module>()