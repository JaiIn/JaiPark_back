package org.example.jaipark_back.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.example.jaipark_back.dto.NotificationEvent;
import java.util.HashMap;
import java.util.Map;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.core.KafkaTemplate;

@EnableKafka
@Configuration
public class KafkaConfig {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String NOTIFICATION_GROUP = "notification-group";

    @Bean
    public ProducerFactory<String, NotificationEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 프로듀서 최적화 설정
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5); // 배치 전송 대기 시간
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // 압축 설정
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 요청 타임아웃
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 최대 블록 시간
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, NotificationEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, NotificationEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, NOTIFICATION_GROUP);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                 org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                 JsonDeserializer.class);
        
        // 컨슈머 최적화 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // 한 번에 가져올 최대 레코드 수
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 세션 타임아웃
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 하트비트 간격
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 최대 폴링 간격
        
        return new DefaultKafkaConsumerFactory<>(props, 
            new org.apache.kafka.common.serialization.StringDeserializer(), 
            new JsonDeserializer<>(NotificationEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // 컨테이너 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 에러 처리 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate());
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer, new FixedBackOff(1000L, 3));
        factory.setCommonErrorHandler(errorHandler);
        
        // 동시성 설정
        factory.setConcurrency(3); // 3개의 컨슈머 스레드
        
        return factory;
    }
} 