package pt.ulusofona.student.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Request-Reply Configuration
 * 
 * Implements the Request-Reply pattern for reliable Kafka messaging:
 * 1. Send a request message to a request topic
 * 2. Wait for a reply message on a reply topic
 * 3. Match request and reply using correlation ID
 * 
 * Benefits:
 * - Guaranteed delivery confirmation
 * - Know when workflow actually starts
 * - Dynamic timeout based on actual processing
 * - Better error handling
 */
@Configuration
public class KafkaReplyingConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.reply.topics.student-login-reply:student.login.reply}")
    private String studentLoginReplyTopic;

    @Value("${kafka.reply.topics.verification-reply:verification.reply}")
    private String verificationReplyTopic;

    @Value("${kafka.reply.timeout:30000}")
    private long replyTimeout;

    /**
     * Producer factory for replying template
     */
    @Bean
    public ProducerFactory<String, Object> replyingProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Consumer factory for reply messages
     */
    @Bean
    public ConsumerFactory<String, Object> replyConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "student-service-reply-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Restrict trusted packages instead of "*". Messages deserialize to java.util.HashMap.
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "pt.ulusofona.student,java.util,java.lang");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Reply container for student login replies
     */
    @Bean
    public ConcurrentMessageListenerContainer<String, Object> studentLoginReplyContainer(
            ConsumerFactory<String, Object> replyConsumerFactory) {
        
        ContainerProperties containerProperties = new ContainerProperties(studentLoginReplyTopic);
        containerProperties.setGroupId("student-login-reply-group");
        
        ConcurrentMessageListenerContainer<String, Object> container =
                new ConcurrentMessageListenerContainer<>(replyConsumerFactory, containerProperties);
        container.setAutoStartup(true);
        
        return container;
    }

    /**
     * Reply container for verification replies
     */
    @Bean
    public ConcurrentMessageListenerContainer<String, Object> verificationReplyContainer(
            ConsumerFactory<String, Object> replyConsumerFactory) {
        
        ContainerProperties containerProperties = new ContainerProperties(verificationReplyTopic);
        containerProperties.setGroupId("verification-reply-group");
        
        ConcurrentMessageListenerContainer<String, Object> container =
                new ConcurrentMessageListenerContainer<>(replyConsumerFactory, containerProperties);
        container.setAutoStartup(true);
        
        return container;
    }

    /**
     * Replying Kafka Template for student login
     * 
     * Usage:
     * <pre>
     * RequestReplyFuture<String, Object, Object> future = 
     *     replyingTemplate.sendAndReceive(record);
     * ConsumerRecord<String, Object> reply = future.get(30, TimeUnit.SECONDS);
     * </pre>
     */
    @Bean
    public ReplyingKafkaTemplate<String, Object, Object> studentLoginReplyingTemplate(
            ProducerFactory<String, Object> replyingProducerFactory,
            ConcurrentMessageListenerContainer<String, Object> studentLoginReplyContainer) {
        
        ReplyingKafkaTemplate<String, Object, Object> template =
                new ReplyingKafkaTemplate<>(replyingProducerFactory, studentLoginReplyContainer);
        
        template.setDefaultReplyTimeout(Duration.ofMillis(replyTimeout));
        template.setSharedReplyTopic(true);
        
        return template;
    }

    /**
     * Replying Kafka Template for verification
     */
    @Bean
    public ReplyingKafkaTemplate<String, Object, Object> verificationReplyingTemplate(
            ProducerFactory<String, Object> replyingProducerFactory,
            ConcurrentMessageListenerContainer<String, Object> verificationReplyContainer) {
        
        ReplyingKafkaTemplate<String, Object, Object> template =
                new ReplyingKafkaTemplate<>(replyingProducerFactory, verificationReplyContainer);
        
        template.setDefaultReplyTimeout(Duration.ofMillis(replyTimeout));
        template.setSharedReplyTopic(true);
        
        return template;
    }
}

