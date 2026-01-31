package com.bxt.picturebackend.listener;

import com.bxt.picturebackend.dto.picture.PictureFileDeleteEvent;
import com.bxt.picturebackend.manager.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import static com.bxt.picturebackend.config.RabbitMQConfig.PICTURE_FILE_DELETE_QUEUE;

@Slf4j
@Component
public class PictureFileDeleteListener {

    @Resource
    private FileManager fileManager;

    @RabbitListener(queues = PICTURE_FILE_DELETE_QUEUE)
    public void handleDelete(PictureFileDeleteEvent event) {
        try {
            log.info("收到图片文件删除事件，key={}", event.getCosKey());
            fileManager.deleteOriginPicture(event.getCosKey());
        } catch (Exception e) {
            log.error("处理图片文件删除事件失败，event={}", event, e);
            // ❗这里先不抛异常，避免消息反复重试
        }
    }
}
