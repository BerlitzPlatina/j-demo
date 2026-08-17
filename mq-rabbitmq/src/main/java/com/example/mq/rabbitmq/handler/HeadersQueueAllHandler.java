package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Handler for the headers exchange queue bound with x-match=all.
 * </p>
 *
 * @author NamHoang
 */
@Slf4j
@RabbitListener(queues = RabbitConsts.HEADERS_QUEUE_ALL)
@Component
public class HeadersQueueAllHandler extends AbstractManualAckHandler {

    @Override
    protected String queueLabel() {
        return "Headers queue (match all)";
    }

    @Override
    protected void process(MessageStruct messageStruct) {
        log.info("Headers queue (match all), manual ack, received message: {}", messageStruct);
    }
}
