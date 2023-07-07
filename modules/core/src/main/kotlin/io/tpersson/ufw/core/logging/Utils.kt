package io.tpersson.ufw.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

public inline fun <reified T> T.createLogger(): Logger = LoggerFactory.getLogger(T::class.java)