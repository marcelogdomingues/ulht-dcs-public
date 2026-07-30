package pt.ulusofona.digital.wallet.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import pt.ulusofona.digital.wallet.domain.workflow.CredentialWorkflowRequest;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, CredentialWorkflowRequest> credentialWorkflowProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public KafkaTemplate<String, CredentialWorkflowRequest> credentialWorkflowKafkaTemplate() {
        return new KafkaTemplate<>(credentialWorkflowProducerFactory());
    }
    
    /**
     * Producer factory for Map-based messages (for error publishing)
     */
    @Bean
    public ProducerFactory<String, Map<String, Object>> mapProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * KafkaTemplate for Map-based messages (for error publishing)
     */
    @Bean
    public KafkaTemplate<String, Map<String, Object>> mapKafkaTemplate() {
        return new KafkaTemplate<>(mapProducerFactory());
    }
    
    /**
     * Consumer factory for Map-based messages (from Student Service)
     * Configured to deserialize JSON without type headers
     */
    @Bean
    public ConsumerFactory<String, Map<String, Object>> mapConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "lusofona-service-group");
        // Restrict trusted packages instead of "*". Messages deserialize to java.util.HashMap.
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "pt.ulusofona.digital.wallet,java.util,java.lang");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, HashMap.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(Map.class, false)
        );
    }
    
    /**
     * Kafka listener container factory for Map-based messages
     * Configured with reply template for request-reply pattern
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(mapConsumerFactory());
        // Set reply template for @SendTo support (use the mapKafkaTemplate bean)
        factory.setReplyTemplate(mapKafkaTemplate());
        return factory;
    }
} 