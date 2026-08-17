package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Queue 2 handler.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2019-01-04 15:42
 */
@Slf4j
@RabbitListener(queues = RabbitConsts.QUEUE_TWO)
@Component
public class QueueTwoHandler extends AbstractManualAckHandler {

    @Override
    protected String queueLabel() {
        return "Queue 2";
    }

    @Override
    protected void process(MessageStruct messageStruct) {
        log.info("Queue 2, manual ack, received message: {}", messageStruct);
    }
}
