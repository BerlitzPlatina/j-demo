package com.example.mq.rabbitmq.config;

import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import com.example.mq.rabbitmq.constants.RabbitConsts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * <p>
 * RabbitMQ configuration: declares the queues, exchanges and bindings. It can
 * be skipped
 * entirely when the queues already exist on the broker.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-29 17:03
 */
@Slf4j
@Configuration
public class RabbitMqConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    /**
     * Logs the connection settings that were actually resolved, before the first
     * connection is
     * attempted. Credentials come from the .env at the repository root, which is
     * only found when the
     * working directory is the module or the repository root — this line says
     * whether that worked,
     * instead of leaving a bare ACCESS_REFUSED to interpret. The password itself is
     * never logged.
     */
    @PostConstruct
    public void logResolvedSettings() {
        log.info("RabbitMQ config: host={} port={} vhost={} username={} password={} (working directory: {})",
                host, port, virtualHost, username,
                password == null || password.isEmpty() ? "EMPTY - .env was not picked up" : "set",
                System.getProperty("user.dir"));
    }

    /**
     * Messages travel as JSON rather than as Java serialized objects. Without this
     * the default
     * converter would use Java serialization, which Spring AMQP only accepts for
     * explicitly
     * allow-listed classes and which no non-Java consumer could read.
     * <p>
     * The package has to be listed as trusted: a producer controls the type name
     * written into the
     * message headers, so the converter refuses to instantiate anything outside
     * java.util and
     * java.lang unless it is told which packages are safe.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.example.mq.rabbitmq.message");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        // setPublisherConfirms(boolean) was replaced by a mode: CORRELATED carries the
        // CorrelationData through to the confirm callback below.
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> log
                .info("Message published: correlationData({}), ack({}), cause({})", correlationData, ack, cause));
        // setReturnCallback took five arguments; setReturnsCallback takes one
        // ReturnedMessage
        // holding the same values.
        rabbitTemplate.setReturnsCallback(returned -> log.info(
                "Message returned undelivered: exchange({}), route({}), replyCode({}), replyText({}), message: {}",
                returned.getExchange(),
                returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText(), returned.getMessage()));
        return rabbitTemplate;
    }

    /**
     * Listener factory for the dead letter queue, deliberately without the JSON
     * converter.
     * <p>
     * The listener adapter converts the body before it binds handler parameters, so
     * a listener
     * running on the default factory would fail conversion on exactly the malformed
     * messages the
     * dead letter queue exists to capture - and the message would be discarded
     * unread. Using
     * {@link SimpleMessageConverter} here means the body is handed over untouched.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory dlqContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        // Nothing to dead letter to from here, so let the container acknowledge.
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    /**
     * Exchange a rejected message is routed to. Without a dead letter exchange, a
     * message nacked
     * without requeue is discarded silently.
     */
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(RabbitConsts.DEAD_LETTER_EXCHANGE, true, false);
    }

    /**
     * Holds every message the handlers could not process, so it can be inspected
     * and replayed
     * rather than lost.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitConsts.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);
    }

    /**
     * Direct mode queue 1.
     */
    @Bean
    public Queue directOneQueue() {
        return withDeadLetter(RabbitConsts.DIRECT_MODE_QUEUE_ONE);
    }

    /**
     * Queue 2.
     */
    @Bean
    public Queue queueTwo() {
        return withDeadLetter(RabbitConsts.QUEUE_TWO);
    }

    /**
     * Queue 3.
     */
    @Bean
    public Queue queueThree() {
        return withDeadLetter(RabbitConsts.QUEUE_THREE);
    }

    /**
     * Queue for the headers exchange, x-match=all binding.
     */
    @Bean
    public Queue headersAllQueue() {
        return withDeadLetter(RabbitConsts.HEADERS_QUEUE_ALL);
    }

    /**
     * Queue for the headers exchange, x-match=any binding.
     */
    @Bean
    public Queue headersAnyQueue() {
        return withDeadLetter(RabbitConsts.HEADERS_QUEUE_ANY);
    }

    /**
     * Builds a queue whose rejected messages end up on the dead letter exchange.
     * <p>
     * Note that queue arguments are immutable once a queue exists: adding this to a
     * queue already
     * declared without it fails with {@code PRECONDITION_FAILED}, and the queue has
     * to be deleted
     * and redeclared.
     *
     * @param name queue name
     * @return the queue definition
     */
    private Queue withDeadLetter(String name) {
        return QueueBuilder.durable(name).deadLetterExchange(RabbitConsts.DEAD_LETTER_EXCHANGE).build();
    }

    /**
     * Direct mode exchange.
     * <p>
     * Sending to a queue name with no exchange already routes through the broker's
     * default
     * exchange, which is a direct exchange every queue is implicitly bound to under
     * its own name.
     * Declaring one explicitly is what lets a routing key differ from the queue
     * name, and lets
     * several queues share a key.
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(RabbitConsts.DIRECT_MODE_EXCHANGE);
    }

    /**
     * Binds queue 1 to the direct exchange under {@code direct.one}.
     */
    @Bean
    public Binding directBinding1(Queue directOneQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(directOneQueue).to(directExchange).with(RabbitConsts.DIRECT_ROUTING_KEY_ONE);
    }

    /**
     * Binds queue 2 to the direct exchange under {@code direct.two}.
     */
    @Bean
    public Binding directBinding2(Queue queueTwo, DirectExchange directExchange) {
        return BindingBuilder.bind(queueTwo).to(directExchange).with(RabbitConsts.DIRECT_ROUTING_KEY_TWO);
    }

    /**
     * Headers mode exchange: the routing key is ignored, the binding arguments are
     * matched against
     * the message headers instead.
     */
    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(RabbitConsts.HEADERS_MODE_EXCHANGE);
    }

    /**
     * x-match=all: a message reaches this queue only when it carries <em>both</em>
     * headers with
     * these exact values. Extra headers on the message are ignored.
     */
    @Bean
    public Binding headersAllBinding(Queue headersAllQueue, HeadersExchange headersExchange) {
        return BindingBuilder.bind(headersAllQueue).to(headersExchange)
                .whereAll(Map.of("type", "report", "format", "pdf")).match();
    }

    /**
     * x-match=any: one matching header is enough.
     */
    @Bean
    public Binding headersAnyBinding(Queue headersAnyQueue, HeadersExchange headersExchange) {
        return BindingBuilder.bind(headersAnyQueue).to(headersExchange)
                .whereAny(Map.of("type", "report", "format", "pdf")).match();
    }

    /**
     * Fanout mode exchange.
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(RabbitConsts.FANOUT_MODE_QUEUE);
    }

    /**
     * Binds queue 1 to the fanout exchange.
     *
     * @param directOneQueue queue 1
     * @param fanoutExchange fanout exchange
     */
    @Bean
    public Binding fanoutBinding1(Queue directOneQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(directOneQueue).to(fanoutExchange);
    }

    /**
     * Binds queue 2 to the fanout exchange.
     *
     * @param queueTwo       queue 2
     * @param fanoutExchange fanout exchange
     */
    @Bean
    public Binding fanoutBinding2(Queue queueTwo, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queueTwo).to(fanoutExchange);
    }

    /**
     * Topic mode exchange.
     * <li>Routing keys are dot separated, for example user.email or
     * user.aaa.email</li>
     * <li>The * wildcard stands for exactly one word: user.* matches user.email but
     * not
     * user.aaa.email</li>
     * <li>The # wildcard stands for zero or more words: user.# matches both
     * user.email
     * and user.aaa.email</li>
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RabbitConsts.TOPIC_MODE_QUEUE);
    }

    /**
     * Binds the fanout exchange to the topic exchange, so a matching key fans out
     * to both queues.
     *
     * @param fanoutExchange fanout exchange
     * @param topicExchange  topic exchange
     */
    @Bean
    public Binding topicBinding1(FanoutExchange fanoutExchange, TopicExchange topicExchange) {
        return BindingBuilder.bind(fanoutExchange).to(topicExchange).with(RabbitConsts.TOPIC_ROUTING_KEY_ONE);
    }

    /**
     * Binds queue 2 to the topic exchange.
     *
     * @param queueTwo      queue 2
     * @param topicExchange topic exchange
     */
    @Bean
    public Binding topicBinding2(Queue queueTwo, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueTwo).to(topicExchange).with(RabbitConsts.TOPIC_ROUTING_KEY_TWO);
    }

    /**
     * Binds queue 3 to the topic exchange.
     *
     * @param queueThree    queue 3
     * @param topicExchange topic exchange
     */
    @Bean
    public Binding topicBinding3(Queue queueThree, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueThree).to(topicExchange).with(RabbitConsts.TOPIC_ROUTING_KEY_THREE);
    }

    /**
     * <p>
     * Delay queue - only declared when the broker can actually host it.
     * </p>
     * <p>
     * The {@code x-delayed-message} exchange type comes from the community plugin
     * rabbitmq_delayed_message_exchange, which is not part of a stock RabbitMQ.
     * Declaring it against
     * a broker without the plugin fails with
     * {@code PRECONDITION_FAILED - unknown exchange type},
     * and that failure stops the whole application from starting. Guarding these
     * three beans keeps
     * the rest of the module usable; switch rabbitmq.delay.enabled to true once the
     * plugin is
     * installed.
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(name = "rabbitmq.delay.enabled", havingValue = "true")
    public static class DelayQueueConfig {

        @Bean
        public Queue delayQueue() {
            return new Queue(RabbitConsts.DELAY_QUEUE, true);
        }

        /**
         * Delay exchange. The x-delayed-type argument and the x-delayed-message type
         * are fixed.
         */
        @Bean
        public CustomExchange delayExchange() {
            Map<String, Object> args = Maps.newHashMap();
            args.put("x-delayed-type", "direct");
            return new CustomExchange(RabbitConsts.DELAY_MODE_QUEUE, "x-delayed-message", true, false, args);
        }

        /**
         * Binds the delay queue to the custom exchange.
         *
         * @param delayQueue    queue
         * @param delayExchange delay exchange
         */
        @Bean
        public Binding delayBinding(Queue delayQueue, CustomExchange delayExchange) {
            return BindingBuilder.bind(delayQueue).to(delayExchange).with(RabbitConsts.DELAY_QUEUE).noargs();
        }
    }

}
