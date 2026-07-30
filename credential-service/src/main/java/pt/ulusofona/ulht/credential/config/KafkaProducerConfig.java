package pt.ulusofona.ulht.credential.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pt.ulusofona.ulht.credential.kafka.KafkaDataMaskingInterceptor;
import pt.ulusofona.ulht.credential.model.LusofonaUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaProducerConfig {

    @Autowired(required = false)
    private KafkaDataMaskingInterceptor kafkaDataMaskingInterceptor;

    @Bean
    public KafkaTemplate<String, LusofonaUser> kafkaTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> kafkaPropertiesMap = kafkaProperties.buildProducerProperties();
        // Add interceptor if available
        if (kafkaDataMaskingInterceptor != null) {
            addProducerInterceptor(kafkaPropertiesMap);
        }
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaPropertiesMap));
    }

    /**
     * KafkaTemplate for workflow messages (credential/verification workflow)
     * Used by CredentialWorkflowConsumer and VerificationWorkflowConsumer
     */
    @Bean
    public KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> kafkaPropertiesMap = kafkaProperties.buildProducerProperties();
        // Add interceptor if available
        if (kafkaDataMaskingInterceptor != null) {
            addProducerInterceptor(kafkaPropertiesMap);
        }
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaPropertiesMap));
    }

    private void addProducerInterceptor(Map<String, Object> props) {
        String interceptorClass = kafkaDataMaskingInterceptor.getClass().getName();
        Object existingInterceptors = props.get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
        
        if (existingInterceptors == null) {
            props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptorClass);
        } else if (existingInterceptors instanceof String) {
            // Single interceptor as string
            if (!existingInterceptors.equals(interceptorClass)) {
                List<String> interceptors = new ArrayList<>();
                interceptors.add((String) existingInterceptors);
                interceptors.add(interceptorClass);
                props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
            }
        } else if (existingInterceptors instanceof List) {
            // Multiple interceptors as list
            @SuppressWarnings("unchecked")
            List<String> interceptors = (List<String>) existingInterceptors;
            if (!interceptors.contains(interceptorClass)) {
                interceptors.add(interceptorClass);
            }
            props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
        }
    }

}