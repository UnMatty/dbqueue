package ru.matprojects.dbqueue.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer;
import ru.yoomoney.tech.dbqueue.api.impl.NoopPayloadTransformer;
import ru.yoomoney.tech.dbqueue.settings.QueueConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecondQueueConsumer implements QueueConsumer<String> {
    private final QueueConfig secondQueueConfig;


    @Override
    public TaskExecutionResult execute(Task<String> task) {
        String payload = task.getPayloadOrThrow();
        log.info("Сообщение из второй очереди {}", payload);
        return TaskExecutionResult.finish();
    }

    @Override
    public QueueConfig getQueueConfig() {
        return secondQueueConfig;
    }

    @Override
    public TaskPayloadTransformer<String> getPayloadTransformer() {
        return NoopPayloadTransformer.getInstance();
    }
}
