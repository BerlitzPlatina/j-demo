package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * Logs whatever ends up on the dead letter queue.
 * </p>
 * <p>
 * Two details matter here, and both were found the hard way:
 * </p>
 * <ul>
 *     <li>The payload is taken as a raw {@link Message}, not as a converted type. Messages often
 *     land here precisely because they could not be converted, and asking for a converted type
 *     would fail again on the very message we are trying to inspect.</li>
 *     <li>{@code @RabbitListener} sits on the method rather than on the class. The class-level form
 *     dispatches to {@code @RabbitHandler} methods by payload type, so it has to convert the body
 *     first - which defeats the point above.</li>
 * </ul>
 * <p>
 * Acknowledgement stays automatic: there is nowhere left to dead letter to. A real system would
 * persist these somewhere durable and raise an alert rather than only writing a log line.
 * </p>
 *
 * @author NamHoang
 */
@Slf4j
@Component
public class DeadLetterQueueHandler {

    @RabbitListener(queues = RabbitConsts.DEAD_LETTER_QUEUE, containerFactory = "dlqContainerFactory")
    public void receive(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("Dead letter received. originalQueue={} reason={} body={}",
                message.getMessageProperties().getHeader("x-first-death-queue"),
                message.getMessageProperties().getHeader("x-first-death-reason"),
                body);
    }
}
