package com.example.mq.rabbitmq.constants;

/**
 * <p>
 * RabbitMQ constants.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-29 17:08
 */
public interface RabbitConsts {
    /**
     * Direct mode queue 1
     */
    String DIRECT_MODE_QUEUE_ONE = "queue.direct.1";

    /**
     * Queue 2
     */
    String QUEUE_TWO = "queue.2";

    /**
     * Queue 3
     */
    String QUEUE_THREE = "3.queue";

    /**
     * Direct mode exchange, declared explicitly rather than relying on the default exchange.
     */
    String DIRECT_MODE_EXCHANGE = "direct.mode";

    /**
     * Routing key binding queue 1 to the direct exchange. A direct exchange matches the routing key
     * as a whole string, with no wildcards.
     */
    String DIRECT_ROUTING_KEY_ONE = "direct.one";

    /**
     * Routing key binding queue 2 to the direct exchange.
     */
    String DIRECT_ROUTING_KEY_TWO = "direct.two";

    /**
     * Headers mode exchange: routes on message headers and ignores the routing key entirely.
     */
    String HEADERS_MODE_EXCHANGE = "headers.mode";

    /**
     * Queue bound to the headers exchange with x-match=all, so every declared header must match.
     */
    String HEADERS_QUEUE_ALL = "queue.headers.all";

    /**
     * Queue bound to the headers exchange with x-match=any, so one matching header is enough.
     */
    String HEADERS_QUEUE_ANY = "queue.headers.any";

    /**
     * Fanout mode exchange
     */
    String FANOUT_MODE_QUEUE = "fanout.mode";

    /**
     * Topic mode exchange
     */
    String TOPIC_MODE_QUEUE = "topic.mode";

    /**
     * Routing key 1
     */
    String TOPIC_ROUTING_KEY_ONE = "queue.#";

    /**
     * Routing key 2
     */
    String TOPIC_ROUTING_KEY_TWO = "*.queue";

    /**
     * Routing key 3
     */
    String TOPIC_ROUTING_KEY_THREE = "3.queue";

    /**
     * Dead letter exchange: where a rejected message is routed instead of being dropped.
     */
    String DEAD_LETTER_EXCHANGE = "dlx.mode";

    /**
     * Dead letter queue, holding messages that could not be handled.
     */
    String DEAD_LETTER_QUEUE = "dlq.queue";

    /**
     * Delay queue
     */
    String DELAY_QUEUE = "delay.queue";

    /**
     * Delay queue exchange
     */
    String DELAY_MODE_QUEUE = "delay.mode";
}
