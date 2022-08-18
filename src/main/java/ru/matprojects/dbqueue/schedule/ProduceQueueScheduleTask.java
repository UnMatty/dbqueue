package ru.matprojects.dbqueue.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;
import ru.yoomoney.tech.dbqueue.api.impl.ShardingQueueProducer;
import ru.yoomoney.tech.dbqueue.spring.dao.SpringDatabaseAccessLayer;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProduceQueueScheduleTask {
    private final QueueProducer<String> firstQueueProducer;
    private final QueueProducer<String> secondQueueProducer;
    private AtomicInteger firstTaskCounter = new AtomicInteger(0);
    private AtomicInteger secondTaskCounter = new AtomicInteger(0);

    @Scheduled(fixedDelay = 5000)
    public void produceMessageToFirstQueue() {
        firstQueueProducer.enqueue(EnqueueParams.create(String.valueOf(firstTaskCounter.incrementAndGet())));
    }

    @Scheduled(fixedDelay = 3000)
    public void produceMessageToSecondQueue() {
        secondQueueProducer.enqueue(EnqueueParams.create(String.valueOf(secondTaskCounter.incrementAndGet())));
    }
}
