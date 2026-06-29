package com.fidely.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic campaignTopic() {
        return TopicBuilder.name("campaign-notifications")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
