package com.bxt.picturebackend.bloomFilter;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bxt.picturebackend.dto.picture.PictureUploadRequest;
import com.bxt.picturebackend.mapper.PictureMapper;
import com.bxt.picturebackend.model.entity.Picture;
import com.bxt.picturebackend.service.PictureService;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class PictureIdBloomFilter {
    @Autowired
    private PictureMapper pictureMapper;
    private BloomFilter bloomFilter;

    @PostConstruct
    public void init(){
        QueryWrapper<Picture> queryWrapper=new QueryWrapper<>();
        List<Picture> pictures=pictureMapper.selectList(queryWrapper);
        int expectedInsertions= pictures.isEmpty() ?1000:pictures.size();
        bloomFilter=BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                0.05) ;
        for (Picture picture:pictures){
            bloomFilter.put(picture.getId().toString());
        }
        System.out.println("布隆过滤器初始化完成，共加载 PictureId: " + pictures.size());
    }
    public boolean mightContain(Long id){
        return bloomFilter != null && bloomFilter.mightContain(String.valueOf(id));
    }
    public void add(Long id){
        bloomFilter.put(id.toString());
    }

}
