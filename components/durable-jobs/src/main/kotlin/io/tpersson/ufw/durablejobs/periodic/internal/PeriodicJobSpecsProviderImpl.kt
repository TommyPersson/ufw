package io.tpersson.ufw.durablejobs.periodic.internal

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.jobDefinition
import io.tpersson.ufw.durablejobs.periodic.PeriodicJob
import jakarta.inject.Inject
import kotlin.reflect.full.findAnnotation

public class PeriodicJobSpecsProviderImpl @Inject constructor(
    private val jobHandlersProvider: DurableJobHandlersProvider,
) : PeriodicJobSpecsProvider {
    public override val periodicJobSpecs: List<PeriodicJobSpec<*>> = jobHandlersProvider.get().mapNotNull {
        val annotation = it.jobDefinition.jobClass.findAnnotation<PeriodicJob>()
            ?: return@mapNotNull null

        PeriodicJobSpec(
            handler = it,
            cronExpression = annotation.cronExpression,
            cronInstance = cronParser.parse(annotation.cronExpression).validate()
        )
    }

    public companion object {
        private val cronParser = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX).let {
            CronParser(it)
        }
    }
}