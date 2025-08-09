package com.bxt.picturebackend.bloomFilter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bxt.picturebackend.mapper.UserMapper;
import com.bxt.picturebackend.model.entity.User;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class UserAccountBloomFilter {
    @Autowired
    private UserMapper userMapper;
    private BloomFilter<String> bloomFilter;
    @PostConstruct
    public void init(){
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        List<User> users=userMapper.selectList(queryWrapper);
        int expectedInsertions= users.isEmpty() ?1000:users.size();
        bloomFilter=BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                0.05) ;
        for (User user:users){
            bloomFilter.put(user.getUserAccount());
        }
        System.out.println("布隆过滤器初始化完成，共加载 UserId: " + users.size());
    }
    public boolean mightContain(String userAccount){
        return bloomFilter != null && bloomFilter.mightContain(userAccount);
    }
    public void add(String userAccount){
        bloomFilter.put(userAccount);
    }
}
