package com.bxt.picturebackend.listener;

import com.bxt.picturebackend.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class PictureCacheInvalidateListener {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;



    @RabbitListener(queues = RabbitMQConfig.PICTURE_CACHE_INVALIDATE_QUEUE)
    public void handleCacheInvalidate(Long pictureId) {
        try {
            String hashKey = DigestUtils.md5DigestAsHex(pictureId.toString().getBytes(StandardCharsets.UTF_8));
            String redisKey = "picture:id:" + hashKey;

            // 删除 Redis 缓存
            stringRedisTemplate.delete(redisKey);
            log.info("缓存已被清除，pictureId={}", pictureId);
        } catch (Exception e) {
            log.error("缓存清除异常，pictureId=" + pictureId, e);
        }
    }
}
