package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Direct queue 1 handler.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2019-01-04 15:42
 */
@Slf4j
@RabbitListener(queues = RabbitConsts.DIRECT_MODE_QUEUE_ONE)
@Component
public class DirectQueueOneHandler extends AbstractManualAckHandler {

    @Override
    protected String queueLabel() {
        return "Direct queue 1";
    }

    @Override
    protected void process(MessageStruct messageStruct) {
        log.info("Direct queue 1, manual ack, received message: {}", messageStruct);
    }
}
