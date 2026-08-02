package com.example.dcs.credential.config;

import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;
import com.example.dcs.credential.kafka.KafkaDataMaskingInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
@AllArgsConstructor
public class KafkaConsumerConfig {

    @Autowired(required = false)
    private KafkaDataMaskingInterceptor kafkaDataMaskingInterceptor;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaProperties kafkaProperties, DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory(kafkaProperties));
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    /**
     * Reliability error handler shared by all credential-service listener factories.
     *
     * <p>Retries a failing record with exponential backoff (1s, 2s, 4s capped at 10s,
     * ~4 attempts) and, once retries are exhausted, publishes the record to
     * {@code <originalTopic>.DLT} via the {@link DeadLetterPublishingRecoverer}. DLT topics
     * are auto-created. Bad payloads (deserialization / illegal arguments) skip retries and
     * are sent straight to the DLT.</p>
     *
     * <p>Uses the {@code workflowKafkaTemplate} (Map value serializer) for publishing; the
     * failed record's raw bytes are re-published so the value type is irrelevant.</p>
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Qualifier("workflowKafkaTemplate")
            KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            workflowKafkaTemplate,
            (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(
            DeserializationException.class,
            IllegalArgumentException.class);
        return errorHandler;
    }

    /**
     * Listener container factory for workflow messages (credential/verification workflow)
     * Used by CredentialWorkflowConsumer and VerificationWorkflowConsumer
     * Configured with reply template for request-reply pattern
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> workflowKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            @Qualifier("workflowKafkaTemplate")
            KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workflowConsumerFactory(kafkaProperties));
        // Set reply template for @SendTo support
        factory.setReplyTemplate(workflowKafkaTemplate);
        // Wire in retry/backoff + dead-letter publishing for reliability
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    private ConsumerFactory<String, String> defaultConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProps = kafkaProperties.buildConsumerProperties();
        // Add interceptor if available
        if (kafkaDataMaskingInterceptor != null) {
            addConsumerInterceptor(consumerProps);
        }
        return new DefaultKafkaConsumerFactory<>(consumerProps);
    }

    /**
     * Consumer factory for workflow messages with proper JsonDeserializer configuration
     */
    private ConsumerFactory<String, Map<String, Object>> workflowConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProps = new HashMap<>(kafkaProperties.buildConsumerProperties());
        
        // Override deserializer configuration to use ErrorHandlingDeserializer
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                         ErrorHandlingDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                         ErrorHandlingDeserializer.class);
        consumerProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, 
                         StringDeserializer.class);
        consumerProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, 
                         JsonDeserializer.class.getName());
        
        // Configure JsonDeserializer via properties (not setters).
        // Restrict trusted packages instead of "*" to avoid deserialization gadget attacks.
        // Messages are deserialized to java.util.HashMap (see VALUE_DEFAULT_TYPE below).
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.dcs.credential,java.util,java.lang");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");
        
        // Add interceptor if available
        if (kafkaDataMaskingInterceptor != null) {
            addConsumerInterceptor(consumerProps);
        }
        
        return new DefaultKafkaConsumerFactory<>(consumerProps);
    }

    private void addConsumerInterceptor(Map<String, Object> props) {
        String interceptorClass = kafkaDataMaskingInterceptor.getClass().getName();
        Object existingInterceptors = props.get(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
        
        if (existingInterceptors == null) {
            props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptorClass);
        } else if (existingInterceptors instanceof String) {
            // Single interceptor as string
            if (!existingInterceptors.equals(interceptorClass)) {
                List<String> interceptors = new ArrayList<>();
                interceptors.add((String) existingInterceptors);
                interceptors.add(interceptorClass);
                props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
            }
        } else if (existingInterceptors instanceof List) {
            // Multiple interceptors as list
            @SuppressWarnings("unchecked")
            List<String> interceptors = (List<String>) existingInterceptors;
            if (!interceptors.contains(interceptorClass)) {
                interceptors.add(interceptorClass);
            }
            props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
        }
    }
}
