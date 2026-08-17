package com.example.mq.kafka.config;

import com.example.mq.kafka.constants.KafkaConsts;
import com.example.mq.kafka.message.MessageStruct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Kafka configuration. The plain producer, consumer and {@code KafkaTemplate} come from Spring
 * Boot's auto configuration driven by application.properties; this class only adds what the demo
 * needs on top: the topics, a manual ack container factory, a batch container factory and a
 * JSON template/container pair.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {

    /**
     * The auto configured factories, reused so every bean below inherits the bootstrap servers and
     * the rest of the spring.kafka.* settings.
     */
    private final ConsumerFactory<String, String> consumerFactory;

    private final ProducerFactory<String, String> producerFactory;

    /**
     * The topics are declared explicitly instead of relying on the broker's auto creation, so the
     * keyed topic really gets its partitions. Auto created topics use the broker default, which is
     * usually a single partition and would make the key routing demo pointless.
     */
    @Bean
    public NewTopic simpleTopic() {
        return TopicBuilder.name(KafkaConsts.TOPIC_SIMPLE).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic keyedTopic() {
        return TopicBuilder.name(KafkaConsts.TOPIC_KEYED).partitions(KafkaConsts.DEFAULT_PARTITION_NUM).replicas(1).build();
    }

    @Bean
    public NewTopic jsonTopic() {
        return TopicBuilder.name(KafkaConsts.TOPIC_JSON).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic batchTopic() {
        return TopicBuilder.name(KafkaConsts.TOPIC_BATCH).partitions(1).replicas(1).build();
    }

    /**
     * One record per call, offsets committed by the listener through {@code Acknowledgment}.
     * Concurrency matches the partition count of the keyed topic, so the three partitions are
     * consumed in parallel.
     */
    @Bean("ackContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> ackContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(KafkaConsts.DEFAULT_PARTITION_NUM);
        return factory;
    }

    /**
     * Hands the listener the whole poll as a list. Useful when the downstream work batches well,
     * for example a bulk insert.
     */
    @Bean("batchContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }

    /**
     * Boot's auto configured KafkaTemplate is @ConditionalOnMissingBean(KafkaTemplate.class), so
     * declaring the JSON template below makes it back off. The plain String template therefore has
     * to be declared here as well; it still uses the auto configured producer factory, so the
     * spring.kafka.producer.* settings apply.
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Same broker settings as the auto configured producer, only the value serializer differs.
     */
    @Bean
    public KafkaTemplate<String, MessageStruct> jsonKafkaTemplate() {
        Map<String, Object> props = new HashMap<>(producerFactory.getConfigurationProperties());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JacksonJsonSerializer<MessageStruct>()));
    }

    /**
     * Reads the JSON topic back into {@code MessageStruct}. The target type is set explicitly and
     * type headers are ignored, so the payload deserializes even when it was produced by something
     * other than Spring Kafka.
     */
    @Bean("jsonContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, MessageStruct> jsonContainerFactory() {
        Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());

        JacksonJsonDeserializer<MessageStruct> valueDeserializer = new JacksonJsonDeserializer<>(MessageStruct.class);
        valueDeserializer.ignoreTypeHeaders();
        valueDeserializer.addTrustedPackages("com.example.mq.kafka.message");

        ConcurrentKafkaListenerContainerFactory<String, MessageStruct> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
