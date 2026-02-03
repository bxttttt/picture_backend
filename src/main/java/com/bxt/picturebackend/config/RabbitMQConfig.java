package com.bxt.picturebackend.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PICTURE_CACHE_INVALIDATE_QUEUE = "picture.cache.invalidate.queue";

    public static final String PICTURE_FILE_DELETE_QUEUE = "picture.file.delete.queue";

    @Bean
    public Queue pictureUploadQueue() {
        return new Queue(PICTURE_CACHE_INVALIDATE_QUEUE);
    }

    @Bean
    public Queue pictureFileDeleteQueue() {
        return new Queue(PICTURE_FILE_DELETE_QUEUE, true);
    }
}
