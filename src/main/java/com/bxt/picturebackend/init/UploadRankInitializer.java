package com.bxt.picturebackend.init;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bxt.picturebackend.constant.RedisKeyConstant;
import com.bxt.picturebackend.dto.picture.UserUploadCountDTO;
import com.bxt.picturebackend.mapper.PictureMapper;

import com.bxt.picturebackend.model.entity.Picture;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
@Component
@Slf4j
public class UploadRankInitializer implements ApplicationRunner {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("开始初始化上传排行榜（QueryWrapper 方式）");

            // 1. 获取所有 userId
            QueryWrapper<Picture> qw = new QueryWrapper<>();
            qw.select("DISTINCT userId")
                    .eq("isDeleted", 0);

            List<Long> userIds = pictureMapper.selectObjs(qw)
                    .stream()
                    .map(obj -> (Long) obj)
                    .toList();

            // 2. 按 userId 逐个统计
            for (Long userId : userIds) {
                QueryWrapper<Picture> countQw = new QueryWrapper<>();
                countQw.eq("userId", userId)
                        .eq("isDeleted", 0);

                Long count = pictureMapper.selectCount(countQw);
                if (count > 0) {
                    stringRedisTemplate.opsForZSet().add(
                            RedisKeyConstant.PICTURE_UPLOAD_RANK,
                            userId.toString(),
                            count
                    );
                }
            }

            log.info("排行榜初始化完成，共 {} 个用户", userIds.size());
        } catch (Exception e) {
            log.error("排行榜初始化失败（不影响启动）", e);
        }
    }
}
