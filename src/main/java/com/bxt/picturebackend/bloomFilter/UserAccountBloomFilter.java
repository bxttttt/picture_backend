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
public class UserAccountBloomFilter {

    @Autowired
    private UserMapper userMapper;

    /** 对外提供查询的 BloomFilter */
    private final AtomicReference<BloomFilter<String>> activeFilter = new AtomicReference<>();

    /** 后台构建用 */
    private final AtomicReference<BloomFilter<String>> standbyFilter = new AtomicReference<>();

    /** 假阳性率（比你原来的 0.05 明显更稳） */
    private static final double FPP = 0.01;

    /** 兜底容量 */
    private static final int DEFAULT_CAPACITY = 1000;

    @PostConstruct
    public void init() {
        rebuildBloomFilter(true);
    }

    /**
     * 查询：只走 activeFilter（无锁，极快）
     */
    public boolean mightContain(String userAccount) {
        BloomFilter<String> filter = activeFilter.get();
        return filter != null && filter.mightContain(userAccount);
    }

    /**
     * 新用户注册时写入（允许轻微误差）
     */
    public void add(String userAccount) {
        BloomFilter<String> filter = activeFilter.get();
        if (filter != null) {
            filter.put(userAccount);
        }
    }

    /**
     * 定期重建（例如每 6 小时一次）
     * cron：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void scheduledRebuild() {
        rebuildBloomFilter(false);
    }

    /**
     * 核心：双布隆 rebuild + 原子切换
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
            newFilter.put(user.getUserAccount());
        }

        // 先放到 standby
        standbyFilter.set(newFilter);

        // 原子切换
        activeFilter.set(standbyFilter.get());
        standbyFilter.set(null);

        log.info("UserAccount BloomFilter rebuild completed, size={}, fpp={}",
                users.size(), FPP);

        if (firstInit) {
            log.info("UserAccount BloomFilter initialized");
        }
    }
}
