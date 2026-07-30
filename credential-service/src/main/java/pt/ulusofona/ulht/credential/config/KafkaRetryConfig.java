package pt.ulusofona.ulht.credential.config;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Base Kafka configuration with retry support.
 * Independent configuration for the credential service.
 */
public abstract class KafkaRetryConfig {
    
    protected final KafkaProperties kafkaProperties;
    
    public KafkaRetryConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }
    
    /**
     * Creates a typed Kafka listener container factory with retry and DLT support
     */
    protected <T> ConcurrentKafkaListenerContainerFactory<String, T> createTypedKafkaListenerContainerFactory(Class<T> messageType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        
        ConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(
            kafkaProperties.buildConsumerProperties()
        );
        
        factory.setConsumerFactory(consumerFactory);
        
        // Configure acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Configure concurrency
        factory.setConcurrency(kafkaProperties.getListener().getConcurrency());
        
        return factory;
    }

    /**
     * Creates a typed Kafka template
     */
    protected <T> org.springframework.kafka.core.KafkaTemplate<String, T> createTypedKafkaTemplate(Class<T> messageType) {
        return new org.springframework.kafka.core.KafkaTemplate<>(
            new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties()
            )
        );
    }

    /**
     * Creates a typed consumer factory
     */
    protected <T> ConsumerFactory<String, T> createTypedConsumerFactory(Class<T> messageType) {
        return new DefaultKafkaConsumerFactory<>(
            kafkaProperties.buildConsumerProperties()
        );
    }
}
