package com.example.mq.rabbitmq.controller;

import com.example.mq.rabbitmq.constants.RabbitConsts;
import com.example.mq.rabbitmq.message.MessageStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    @Value("${rabbitmq.delay.enabled}")
    private boolean delayEnabled;

    /**
     * Straight to one queue, no exchange in between: only DirectQueueOneHandler sees it.
     */
    @PostMapping("/direct")
    public Map<String, Object> direct(@RequestParam(defaultValue = "hello direct") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.DIRECT_MODE_QUEUE_ONE, new MessageStruct(message));
        return sent("direct", RabbitConsts.DIRECT_MODE_QUEUE_ONE, message);
    }

    /**
     * Same direct routing, but through an exchange that was declared for it. The routing key is
     * matched as a whole string, no wildcards: {@code direct.one} reaches queue.direct.1,
     * {@code direct.two} reaches queue.2, anything else routes nowhere and comes back through the
     * returns callback.
     */
    @PostMapping("/direct-exchange")
    public Map<String, Object> directExchange(@RequestParam(defaultValue = RabbitConsts.DIRECT_ROUTING_KEY_ONE) String routingKey,
                                              @RequestParam(defaultValue = "hello direct exchange") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.DIRECT_MODE_EXCHANGE, routingKey, new MessageStruct(message));
        Map<String, Object> result = sent("directExchange", RabbitConsts.DIRECT_MODE_EXCHANGE, message);
        return Map.of("mode", result.get("mode"), "target", result.get("target"), "routingKey", routingKey, "message", message);
    }

    /**
     * Headers mode ignores the routing key and matches on headers instead. queue.headers.all is
     * bound with x-match=all so it needs type=report <em>and</em> format=pdf; queue.headers.any is
     * bound with x-match=any so either one on its own is enough.
     *
     * @param type   value for the {@code type} header, blank to leave the header off
     * @param format value for the {@code format} header, blank to leave the header off
     */
    @PostMapping("/headers")
    public Map<String, Object> headers(@RequestParam(defaultValue = "report") String type,
                                       @RequestParam(defaultValue = "pdf") String format,
                                       @RequestParam(defaultValue = "hello headers") String message) {
        rabbitTemplate.convertAndSend(RabbitConsts.HEADERS_MODE_EXCHANGE, "", new MessageStruct(message),
                m -> {
                    // A header set to an empty value would still count as present and fail to match,
                    // so a blank parameter means the header is not sent at all.
                    if (!type.isBlank()) {
                        m.getMessageProperties().setHeader("type", type);
                    }
                    if (!format.isBlank()) {
                        m.getMessageProperties().setHeader("format", format);
                    }
                    return m;
                });
        Map<String, Object> result = sent("headers", RabbitConsts.HEADERS_MODE_EXCHANGE, message);
        return Map.of("mode", result.get("mode"), "target", result.get("target"), "type", type, "format", format, "message", message);
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
        // Without the guard the send would look like it worked: the exchange does not exist, so the
        // broker drops the message and only the returns callback notices, while the caller still
        // gets sent=true.
        if (!delayEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Delay queue is disabled. Install the rabbitmq_delayed_message_exchange plugin and set rabbitmq.delay.enabled=true");
        }

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
