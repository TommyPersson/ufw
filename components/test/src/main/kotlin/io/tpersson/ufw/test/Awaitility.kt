package io.tpersson.ufw.test

import kotlinx.coroutines.runBlocking
import org.awaitility.core.ConditionFactory

public fun ConditionFactory.suspendingUntil(conditionEvaluator: suspend () -> Boolean): Unit {
    return until {
        runBlocking { conditionEvaluator() }
    }
}