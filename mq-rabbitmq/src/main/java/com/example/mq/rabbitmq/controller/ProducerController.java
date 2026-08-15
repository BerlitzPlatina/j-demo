package com.example.mq.rabbitmq.controller;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <p>
 * Sends messages, so the four listeners in this module have something to consume.
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
@Slf4j
public class ProducerController {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Straight to one queue, no exchange in between: only DirectQueueOneHandler sees it.
     */
    @PostMapping("/direct")
    public Map<String, Object> direct(@RequestParam(defaultValue = "hello direct") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.DIRECT_MODE_QUEUE_ONE, new MessageStruct(message));
        return sent("direct", RabbitConsts.DIRECT_MODE_QUEUE_ONE, message);
    }

    /**
     * Fanout copies the message to every bound queue, so queue.direct.1 and queue.2 both get it.
     */
    @PostMapping("/fanout")
    public Map<String, Object> fanout(@RequestParam(defaultValue = "hello fanout") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.FANOUT_MODE_QUEUE, "", new MessageStruct(message));
        return sent("fanout", RabbitConsts.FANOUT_MODE_QUEUE, message);
    }

    /**
     * Topic routes by pattern. A routing key decides which of the bound queues match.
     *
     * @param routingKey for example {@code queue.abc} matches queue.# , {@code 3.queue} matches *.queue
     */
    @PostMapping("/topic")
    public Map<String, Object> topic(@RequestParam(defaultValue = "queue.test") String routingKey,
                                     @RequestParam(defaultValue = "hello topic") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.TOPIC_MODE_QUEUE, routingKey, new MessageStruct(message));
        Map<String, Object> result = sent("topic", RabbitConsts.TOPIC_MODE_QUEUE, message);
        return Map.of("mode", result.get("mode"), "target", result.get("target"), "routingKey", routingKey, "message", message);
    }

    /**
     * Delayed delivery. Needs the rabbitmq_delayed_message_exchange plugin on the broker; without
     * it the exchange cannot be declared and this call fails.
     *
     * @param delayMillis how long the broker should hold the message before routing it
     */
    @PostMapping("/delay")
    public Map<String, Object> delay(@RequestParam(defaultValue = "3000") Integer delayMillis,
                                     @RequestParam(defaultValue = "hello delay") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.DELAY_MODE_QUEUE, RabbitConsts.DELAY_QUEUE, new MessageStruct(message),
                m -> {
                    m.getMessageProperties().setHeader("x-delay", delayMillis);
                    return m;
                });
        Map<String, Object> result = sent("delay", RabbitConsts.DELAY_MODE_QUEUE, message);
        return Map.of("mode", result.get("mode"), "target", result.get("target"), "delayMillis", delayMillis, "message", message);
    }

    private Map<String, Object> sent(String mode, String target, String message) {
        log.info("Sent [{}] to {}: {}", mode, target, message);
        return Map.of("mode", mode, "target", target, "message", message, "sent", true);
    }
}
