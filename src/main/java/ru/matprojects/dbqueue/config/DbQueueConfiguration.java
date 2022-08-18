package ru.matprojects.dbqueue.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.matprojects.dbqueue.queue.FirstQueueConsumer;
import ru.matprojects.dbqueue.queue.SecondQueueConsumer;
import ru.yoomoney.tech.dbqueue.api.impl.NoopPayloadTransformer;
import ru.yoomoney.tech.dbqueue.api.impl.ShardingQueueProducer;
import ru.yoomoney.tech.dbqueue.api.impl.SingleQueueShardRouter;
import ru.yoomoney.tech.dbqueue.config.*;
import ru.yoomoney.tech.dbqueue.config.impl.LoggingTaskLifecycleListener;
import ru.yoomoney.tech.dbqueue.config.impl.LoggingThreadLifecycleListener;
import ru.yoomoney.tech.dbqueue.settings.*;
import ru.yoomoney.tech.dbqueue.spring.dao.SpringDatabaseAccessLayer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;

@Configuration
@RequiredArgsConstructor
public class DbQueueConfiguration {
    private final static String DB_QUEUE_NAME = "queue_tasks";
    private final static int QUEUE_THREAD_COUNT = 10;
    private final static String FIRST_QUEUE_ID = "first_queue";
    private final static String SECOND_QUEUE_ID = "second_queue";

    @Bean
    QueueShard<SpringDatabaseAccessLayer> queueShard(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        var databaseAccessLayer = new SpringDatabaseAccessLayer(
                DatabaseDialect.POSTGRESQL, QueueTableSchema.builder().build(),
                jdbcTemplate,
                transactionTemplate);

        return new QueueShard<>(new QueueShardId("main"), databaseAccessLayer);
    }

    @Bean
    QueueSettings queueSettings() {
        return QueueSettings.builder()
                .withProcessingSettings(ProcessingSettings.builder()
                        .withProcessingMode(ProcessingMode.SEPARATE_TRANSACTIONS)
                        .withThreadCount(QUEUE_THREAD_COUNT).build())
                .withPollSettings(PollSettings.builder()
                        .withBetweenTaskTimeout(Duration.ofMillis(100))
                        .withFatalCrashTimeout(Duration.ofSeconds(1))
                        .withNoTaskTimeout(Duration.ofMillis(100)).build())
                .withFailureSettings(FailureSettings.builder().withRetryInterval(Duration.ofMinutes(1))
                        .withRetryType(FailRetryType.GEOMETRIC_BACKOFF).build())
                .withReenqueueSettings(ReenqueueSettings.builder()
                        .withRetryType(ReenqueueRetryType.MANUAL).build())
                .withExtSettings(ExtSettings.builder().withSettings(new HashMap<>()).build())
                .build();
    }

    @Bean(name = "firstQueueConfig")
    QueueConfig firstQueueConfig(QueueSettings queueSettings) {
        var queueId = new QueueId(FIRST_QUEUE_ID);

        return new QueueConfig(QueueLocation.builder()
                .withTableName(DB_QUEUE_NAME)
                .withQueueId(queueId)
                .build(), queueSettings);
    }

    @Bean(name = "secondQueueConfig")
    QueueConfig secondQueueConfig(QueueSettings queueSettings) {
        var queueId = new QueueId(SECOND_QUEUE_ID);

        return new QueueConfig(QueueLocation.builder()
                .withTableName(DB_QUEUE_NAME)
                .withQueueId(queueId)
                .build(), queueSettings);
    }

    @Bean(name = "firstQueueProducer")
    ShardingQueueProducer<String, SpringDatabaseAccessLayer> firstQueueProducer(
            QueueConfig firstQueueConfig,
            QueueShard<SpringDatabaseAccessLayer> queueShard) {
        return new ShardingQueueProducer<>(
                firstQueueConfig,
                NoopPayloadTransformer.getInstance(),
                new SingleQueueShardRouter<>(queueShard));
    }

    @Bean(name = "secondQueueProducer")
    ShardingQueueProducer<String, SpringDatabaseAccessLayer> secondQueueProducer(
            QueueConfig secondQueueConfig,
            QueueShard<SpringDatabaseAccessLayer> queueShard) {
        return new ShardingQueueProducer<>(
                secondQueueConfig,
                NoopPayloadTransformer.getInstance(),
                new SingleQueueShardRouter<>(queueShard));
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    QueueService queueService(QueueShard<SpringDatabaseAccessLayer> queueShard,
                              FirstQueueConsumer firstQueueConsumer,
                              SecondQueueConsumer secondQueueConsumer) {
        QueueService queueService = new QueueService(Collections.singletonList(queueShard),
                new LoggingThreadLifecycleListener(),
                new LoggingTaskLifecycleListener());

        queueService.registerQueue(firstQueueConsumer);
        queueService.registerQueue(secondQueueConsumer);
        return queueService;
    }
}
