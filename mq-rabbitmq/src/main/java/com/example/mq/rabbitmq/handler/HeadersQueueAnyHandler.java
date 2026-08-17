package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Handler for the headers exchange queue bound with x-match=any.
 * </p>
 *
 * @author NamHoang
 */
@Slf4j
@RabbitListener(queues = RabbitConsts.HEADERS_QUEUE_ANY)
@Component
public class HeadersQueueAnyHandler extends AbstractManualAckHandler {

    @Override
    protected String queueLabel() {
        return "Headers queue (match any)";
    }

    @Override
    protected void process(MessageStruct messageStruct) {
        log.info("Headers queue (match any), manual ack, received message: {}", messageStruct);
    }
}
