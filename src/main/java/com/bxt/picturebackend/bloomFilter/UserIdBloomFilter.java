package com.bxt.picturebackend.bloomFilter;

import com.bxt.picturebackend.mapper.UserMapper;
import com.bxt.picturebackend.model.entity.User;
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
public class UserIdBloomFilter {

    @Autowired
    private UserMapper userMapper;

    /** 对外提供查询的 BloomFilter */
    private final AtomicReference<BloomFilter<String>> activeFilter = new AtomicReference<>();

    /** 后台 rebuild 使用 */
    private final AtomicReference<BloomFilter<String>> standbyFilter = new AtomicReference<>();

    /** 假阳性率（比 0.05 更稳） */
    private static final double FPP = 0.01;

    /** 兜底容量 */
    private static final int DEFAULT_CAPACITY = 1000;

    @PostConstruct
    public void init() {
        rebuildBloomFilter(true);
    }

    /**
     * 查询：无锁，直接读 activeFilter
     */
    public boolean mightContain(Long id) {
        BloomFilter<String> filter = activeFilter.get();
        return filter != null && filter.mightContain(String.valueOf(id));
    }

    /**
     * 新用户写入（允许少量误差）
     */
    public void add(Long id) {
        BloomFilter<String> filter = activeFilter.get();
        if (filter != null) {
            filter.put(String.valueOf(id));
        }
    }

    /**
     * 定期重建（每 6 小时）
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void scheduledRebuild() {
        rebuildBloomFilter(false);
    }

    /**
     * 双 BloomFilter rebuild + 原子切换
     */
    private void rebuildBloomFilter(boolean firstInit) {
        List<User> users = userMapper.selectList(null);
        int expectedInsertions = Math.max(users.size(), DEFAULT_CAPACITY);

        BloomFilter<String> newFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                FPP
        );

        for (User user : users) {
            newFilter.put(String.valueOf(user.getId()));
        }

        // 放入 standby
        standbyFilter.set(newFilter);

        // 原子切换
        activeFilter.set(standbyFilter.get());
        standbyFilter.set(null);

        log.info("UserId BloomFilter rebuild completed, size={}, fpp={}",
                users.size(), FPP);

        if (firstInit) {
            log.info("UserId BloomFilter initialized");
        }
    }
}
