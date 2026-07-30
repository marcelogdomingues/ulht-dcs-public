package pt.ulusofona.ulht.credential.config;

import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import pt.ulusofona.ulht.credential.kafka.KafkaDataMaskingInterceptor;

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
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory(kafkaProperties));
        return factory;
    }

    /**
     * Listener container factory for workflow messages (credential/verification workflow)
     * Used by CredentialWorkflowConsumer and VerificationWorkflowConsumer
     * Configured with reply template for request-reply pattern
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> workflowKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            @org.springframework.beans.factory.annotation.Qualifier("workflowKafkaTemplate") 
            org.springframework.kafka.core.KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workflowConsumerFactory(kafkaProperties));
        // Set reply template for @SendTo support
        factory.setReplyTemplate(workflowKafkaTemplate);
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
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "pt.ulusofona.ulht.credential,java.util,java.lang");
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
