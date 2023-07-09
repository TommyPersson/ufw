package io.tpersson.ufw.managed.guice

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.multibindings.Multibinder
import io.github.classgraph.ClassGraph
import io.tpersson.ufw.managed.Managed
import io.tpersson.ufw.managed.ManagedRunner

public class ManagedGuiceModule(
    private val scanPackages: List<String>,
) : Module {

    override fun configure(binder: Binder) {
        with(binder) {
            // TODO centralize class scanning in just one location
            val scanResult = ClassGraph()
                .enableClassInfo()
                .acceptPackages(*scanPackages.toTypedArray(), "io.tpersson.ufw")
                .scan()

            val classes = scanResult.allClasses
                .filter { it.extendsSuperclass(Managed::class.java) }
                .filter { !it.isAbstract }
                .loadClasses()
                .map { it as Class<out Managed> }

            Multibinder.newSetBinder(binder, Managed::class.java).also {
                for (clazz in classes) {
                    it.addBinding().to(clazz)
                }
            }

            bind(ManagedRunner::class.java)
        }
    }
}

