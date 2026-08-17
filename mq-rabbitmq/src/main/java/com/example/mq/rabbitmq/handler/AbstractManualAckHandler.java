package com.example.mq.rabbitmq.handler;

import com.example.mq.rabbitmq.message.MessageStruct;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;

import java.io.IOException;

/**
 * <p>
 * Shared receive-process-acknowledge loop for the queue handlers.
 * </p>
 * <p>
 * Acknowledgement is manual, so exactly one of these has to happen for every delivery:
 * </p>
 * <ul>
 *     <li>{@code basicAck} - the work succeeded, the broker may drop the message</li>
 *     <li>{@code basicNack} - the work failed, the broker routes the message to the dead letter
 *     queue because it is nacked without requeue</li>
 * </ul>
 * <p>
 * Neither happening is the dangerous case: the delivery stays unacknowledged, it counts against the
 * prefetch limit, and it is only released when the connection drops.
 * </p>
 *
 * @author NamHoang
 */
@Slf4j
public abstract class AbstractManualAckHandler {

    /**
     * Name used in the logs to tell the handlers apart.
     */
    protected abstract String queueLabel();

    /**
     * Does the actual work for one message. Throwing from here nacks the delivery.
     *
     * @param messageStruct the received payload
     */
    protected abstract void process(MessageStruct messageStruct);

    @RabbitHandler
    public void receive(MessageStruct messageStruct, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            process(messageStruct);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // Covers both a failure in process() and a failure of the ack itself.
            log.error("{}: handling failed for delivery {}, sending it to the dead letter queue", queueLabel(), deliveryTag, e);
            nack(channel, deliveryTag);
        }
    }

    /**
     * Rejects a single delivery without requeueing it.
     * <p>
     * Requeueing would put the message straight back at the head of the queue, and a message that
     * fails for a permanent reason - a malformed payload, a row that does not exist - would then be
     * redelivered forever, burning cpu and filling the log. Sending it to the dead letter queue
     * instead keeps it for inspection and lets the queue drain.
     * <p>
     * This is also why {@code basicNack} is used rather than {@code basicRecover}: recover
     * redelivers <em>every</em> unacknowledged message on the channel, including the ones other
     * deliveries are still working on, while nack addresses only this delivery.
     */
    private void nack(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            // Nothing left to try: the broker will redeliver once this channel closes.
            log.error("{}: could not nack delivery {}", queueLabel(), deliveryTag, e);
        }
    }
}
