package pt.ulusofona.student.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ulusofona.student.kafka.KafkaDataMaskingInterceptor;
import pt.ulusofona.student.util.DataMasker;

/**
 * Configuration for data masking functionality.
 * Creates beans for DataMasker and Kafka interceptor.
 */
@Configuration
@ConditionalOnProperty(prefix = "logging.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataMaskingConfiguration {
    
    @Bean
    public DataMasker dataMasker(DataMaskerConfig config, ObjectMapper objectMapper) {
        return new DataMasker(config, objectMapper);
    }
    
    @Bean
    public KafkaDataMaskingInterceptor kafkaDataMaskingInterceptor(
            DataMasker dataMasker, 
            DataMaskerConfig config) {
        KafkaDataMaskingInterceptor interceptor = new KafkaDataMaskingInterceptor();
        interceptor.setDataMasker(dataMasker);
        interceptor.setConfig(config);
        return interceptor;
    }
}





