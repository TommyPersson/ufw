package io.tpersson.ufw.adapters.durablemessages.kafka.incoming

import io.github.resilience4j.kotlin.retry.RetryConfig
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.database.unitofwork.use
import io.tpersson.ufw.durablemessages.common.IncomingMessageIngester
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Duration

public class KafkaIncomingBatchProcessor(
    private val messageIngester: IncomingMessageIngester,
    private val incomingMessageConverter: KafkaIncomingMessageConverter,
    private val unitOfWorkFactory: UnitOfWorkFactory,
) {
    private val retry = Retry.of("KafkaIncomingBatchProcessor", RetryConfig {
        maxAttempts(5)
        waitDuration(Duration.ofMillis(10))
    })

    public suspend fun process(batch: RecordBatch) {
        retry.executeSuspendFunction {
            withContext(NonCancellable) {
                val messages = batch.records.mapNotNull { incomingMessageConverter.convert(it) }
                if (messages.isNotEmpty()) {
                    unitOfWorkFactory.use { uow ->
                        messageIngester.ingest(messages, unitOfWork = uow)
                    }
                }

                batch.commit()
            }
        }
    }
}