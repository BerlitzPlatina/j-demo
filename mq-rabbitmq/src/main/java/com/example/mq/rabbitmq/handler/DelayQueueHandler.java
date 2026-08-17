package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Delay queue handler. Only active together with the delay queue itself, which needs the
 * rabbitmq_delayed_message_exchange plugin on the broker.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2019-01-04 17:42
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rabbitmq.delay.enabled", havingValue = "true")
@RabbitListener(queues = RabbitConsts.DELAY_QUEUE)
public class DelayQueueHandler extends AbstractManualAckHandler {

    @Override
    protected String queueLabel() {
        return "Delay queue";
    }

    @Override
    protected void process(MessageStruct messageStruct) {
        log.info("Delay queue, manual ack, received message: {}", messageStruct);
    }
}
