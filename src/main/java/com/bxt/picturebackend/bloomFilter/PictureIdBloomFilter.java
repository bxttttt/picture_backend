package com.bxt.picturebackend.bloomFilter;

import com.bxt.picturebackend.mapper.PictureMapper;
import com.bxt.picturebackend.model.entity.Picture;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class PictureIdBloomFilter {

    @Autowired
    private PictureMapper pictureMapper;

    /** 对外提供查询的 BloomFilter */
    private final AtomicReference<BloomFilter<String>> activeFilter = new AtomicReference<>();

    /** 后台 rebuild 使用 */
    private final AtomicReference<BloomFilter<String>> standbyFilter = new AtomicReference<>();

    /** 假阳性率（图片量大，建议更保守） */
    private static final double FPP = 0.01;

    /** 兜底容量，防止空表 */
    private static final int DEFAULT_CAPACITY = 1000;

    @PostConstruct
    public void init() {
        rebuildBloomFilter(true);
    }

    /**
     * 查询：无锁读
     */
    public boolean mightContain(Long id) {
        BloomFilter<String> filter = activeFilter.get();
        return filter != null && filter.mightContain(String.valueOf(id));
    }

    /**
     * 新图片写入（上传成功后调用）
     */
    public void add(Long id) {
        BloomFilter<String> filter = activeFilter.get();
        if (filter != null) {
            filter.put(String.valueOf(id));
        }
    }

    /**
     * 定期重建（建议图片表 3~6 小时一次）
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void scheduledRebuild() {
        rebuildBloomFilter(false);
    }

    /**
     * 双 BloomFilter rebuild + 原子切换
     */
    private void rebuildBloomFilter(boolean firstInit) {
        List<Picture> pictures = pictureMapper.selectList(null);
        int expectedInsertions = Math.max(pictures.size(), DEFAULT_CAPACITY);

        BloomFilter<String> newFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                FPP
        );

        for (Picture picture : pictures) {
            newFilter.put(String.valueOf(picture.getId()));
        }

        // 放入 standby
        standbyFilter.set(newFilter);

        // 原子切换
        activeFilter.set(standbyFilter.get());
        standbyFilter.set(null);

        log.info("PictureId BloomFilter rebuild completed, size={}, fpp={}",
                pictures.size(), FPP);

        if (firstInit) {
            log.info("PictureId BloomFilter initialized");
        }
    }
}
