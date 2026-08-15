package com.example.mq.rabbitmq.config;

import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import com.example.mq.rabbitmq.constants.RabbitConsts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * <p>
 * RabbitMQ配置，主要是配置队列，如果提前存在该队列，可以省略本配置类
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
     * Logs the connection settings that were actually resolved, before the first connection is
     * attempted. Credentials come from the .env at the repository root, which is only found when the
     * working directory is the module or the repository root — this line says whether that worked,
     * instead of leaving a bare ACCESS_REFUSED to interpret. The password itself is never logged.
     */
    @PostConstruct
    public void logResolvedSettings() {
        log.info("RabbitMQ config: host={} port={} vhost={} username={} password={} (working directory: {})",
                host, port, virtualHost, username,
                password == null || password.isEmpty() ? "EMPTY - .env was not picked up" : "set",
                System.getProperty("user.dir"));
    }

    /**
     * Messages travel as JSON rather than as Java serialized objects. Without this the default
     * converter would use Java serialization, which Spring AMQP only accepts for explicitly
     * allow-listed classes and which no non-Java consumer could read.
     * <p>
     * The package has to be listed as trusted: a producer controls the type name written into the
     * message headers, so the converter refuses to instantiate anything outside java.util and
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
                .info("消息发送成功:correlationData({}),ack({}),cause({})", correlationData, ack, cause));
        // setReturnCallback took five arguments; setReturnsCallback takes one ReturnedMessage
        // holding the same values.
        rabbitTemplate.setReturnsCallback(returned -> log.info(
                "消息丢失:exchange({}),route({}),replyCode({}),replyText({}),message:{}", returned.getExchange(),
                returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText(), returned.getMessage()));
        return rabbitTemplate;
    }

    /**
     * 直接模式队列1
     */
    @Bean
    public Queue directOneQueue() {
        return new Queue(RabbitConsts.DIRECT_MODE_QUEUE_ONE);
    }

    /**
     * 队列2
     */
    @Bean
    public Queue queueTwo() {
        return new Queue(RabbitConsts.QUEUE_TWO);
    }

    /**
     * 队列3
     */
    @Bean
    public Queue queueThree() {
        return new Queue(RabbitConsts.QUEUE_THREE);
    }

    /**
     * 分列模式队列
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(RabbitConsts.FANOUT_MODE_QUEUE);
    }

    /**
     * 分列模式绑定队列1
     *
     * @param directOneQueue 绑定队列1
     * @param fanoutExchange 分列模式交换器
     */
    @Bean
    public Binding fanoutBinding1(Queue directOneQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(directOneQueue).to(fanoutExchange);
    }

    /**
     * 分列模式绑定队列2
     *
     * @param queueTwo       绑定队列2
     * @param fanoutExchange 分列模式交换器
     */
    @Bean
    public Binding fanoutBinding2(Queue queueTwo, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queueTwo).to(fanoutExchange);
    }

    /**
     * 主题模式队列
     * <li>路由格式必须以 . 分隔，比如 user.email 或者 user.aaa.email</li>
     * <li>通配符 * ，代表一个占位符，或者说一个单词，比如路由为 user.*，那么 user.email 可以匹配，但是 user.aaa.email
     * 就匹配不了</li>
     * <li>通配符 # ，代表一个或多个占位符，或者说一个或多个单词，比如路由为 user.#，那么 user.email
     * 可以匹配，user.aaa.email 也可以匹配</li>
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RabbitConsts.TOPIC_MODE_QUEUE);
    }

    /**
     * 主题模式绑定分列模式
     *
     * @param fanoutExchange 分列模式交换器
     * @param topicExchange  主题模式交换器
     */
    @Bean
    public Binding topicBinding1(FanoutExchange fanoutExchange, TopicExchange topicExchange) {
        return BindingBuilder.bind(fanoutExchange).to(topicExchange).with(RabbitConsts.TOPIC_ROUTING_KEY_ONE);
    }

    /**
     * 主题模式绑定队列2
     *
     * @param queueTwo      队列2
     * @param topicExchange 主题模式交换器
     */
    @Bean
    public Binding topicBinding2(Queue queueTwo, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueTwo).to(topicExchange).with(RabbitConsts.TOPIC_ROUTING_KEY_TWO);
    }

    /**
     * 主题模式绑定队列3
     *
     * @param queueThree    队列3
     * @param topicExchange 主题模式交换器
     */
    @Bean
    public Binding topicBinding3(Queue queueThree, TopicExchange topicExchange) {
        return BindingBuilder.bind(queueThree).to(topicExchange).with(RabbitConsts.TOPIC_ROUTING_KEY_THREE);
    }

    /**
     * <p>
     * 延迟队列 - only declared when the broker can actually host it.
     * </p>
     * <p>
     * The {@code x-delayed-message} exchange type comes from the community plugin
     * rabbitmq_delayed_message_exchange, which is not part of a stock RabbitMQ. Declaring it against
     * a broker without the plugin fails with {@code PRECONDITION_FAILED - unknown exchange type},
     * and that failure stops the whole application from starting. Guarding these three beans keeps
     * the rest of the module usable; switch rabbitmq.delay.enabled to true once the plugin is
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
         * 延迟队列交换器, x-delayed-type 和 x-delayed-message 固定
         */
        @Bean
        public CustomExchange delayExchange() {
            Map<String, Object> args = Maps.newHashMap();
            args.put("x-delayed-type", "direct");
            return new CustomExchange(RabbitConsts.DELAY_MODE_QUEUE, "x-delayed-message", true, false, args);
        }

        /**
         * 延迟队列绑定自定义交换器
         *
         * @param delayQueue    队列
         * @param delayExchange 延迟交换器
         */
        @Bean
        public Binding delayBinding(Queue delayQueue, CustomExchange delayExchange) {
            return BindingBuilder.bind(delayQueue).to(delayExchange).with(RabbitConsts.DELAY_QUEUE).noargs();
        }
    }

}
