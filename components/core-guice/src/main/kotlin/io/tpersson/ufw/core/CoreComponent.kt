package io.tpersson.ufw.core

import com.google.inject.Binder
import com.google.inject.Module

public class CoreGuiceModule : Module {
    override fun configure(binder: Binder) {
        binder.bind(CoreComponent::class.java)
    }
}
