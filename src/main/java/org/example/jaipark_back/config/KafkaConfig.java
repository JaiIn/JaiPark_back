package org.example.jaipark_back.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.example.jaipark_back.dto.NotificationEvent;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@EnableKafka
@EnableAsync
@Configuration
public class KafkaConfig {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String NOTIFICATION_GROUP = "notification-group";
    
    // 알림 관련 토픽 이름 정의
    private static final String NOTIFICATION_TOPIC = "notification";
    private static final String LIKE_TOPIC = "notification-like";
    private static final String COMMENT_TOPIC = "notification-comment";
    private static final String FOLLOW_TOPIC = "notification-follow";
    private static final String BATCH_TOPIC = "notification-batch";
    private static final String RETRY_TOPIC = "notification-retry";
    private static final String DLQ_TOPIC = "notification-dlq"; // Dead Letter Queue

    /**
     * 비동기 작업을 위한 스레드 풀 설정
     */
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("kafka-async-");
        executor.initialize();
        return executor;
    }
    
    /**
     * Kafka 토픽 생성
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return new KafkaAdmin(configs);
    }
    
    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(NOTIFICATION_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic likeTopic() {
        return TopicBuilder.name(LIKE_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic commentTopic() {
        return TopicBuilder.name(COMMENT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic followTopic() {
        return TopicBuilder.name(FOLLOW_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic batchTopic() {
        return TopicBuilder.name(BATCH_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic retryTopic() {
        return TopicBuilder.name(RETRY_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(DLQ_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * 단일 알림 이벤트 Producer 설정
     */
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
    
    /**
     * 배치 알림 이벤트 Producer 설정
     */
    @Bean
    public ProducerFactory<String, List<NotificationEvent>> batchProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 배치 처리를 위한 최적화 설정
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768); // 32KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 배치 전송 대기 시간
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, List<NotificationEvent>> batchKafkaTemplate() {
        return new KafkaTemplate<>(batchProducerFactory());
    }

    /**
     * 단일 알림 Consumer 설정
     */
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
    
    /**
     * 배치 알림 Consumer 설정
     */
    @Bean
    public ConsumerFactory<String, List<NotificationEvent>> batchConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "notification-batch-group");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                 org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                 JsonDeserializer.class);
        
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50); // 배치 처리를 위해 더 적은 수의 배치를 가져옴
        
        return new DefaultKafkaConsumerFactory<>(props, 
            new org.apache.kafka.common.serialization.StringDeserializer(), 
            new JsonDeserializer<>(List.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // 컨테이너 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 에러 처리 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate(), 
            (record, exception) -> {
                // 에러 발생 시 DLQ 토픽으로 메시지 전송
                return new org.apache.kafka.common.TopicPartition(DLQ_TOPIC, 0);
            });
            
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer, new FixedBackOff(1000L, 3));
        factory.setCommonErrorHandler(errorHandler);
        
        // 동시성 설정
        factory.setConcurrency(3); // 3개의 컨슈머 스레드
        
        return factory;
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, List<NotificationEvent>> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, List<NotificationEvent>> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(batchConsumerFactory());
        
        // 배치 처리 설정
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 3)));
        factory.setConcurrency(2);
        
        return factory;
    }
}