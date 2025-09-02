package com.bxt.picturebackend.imageSearch;

import com.bxt.picturebackend.mapper.PictureMapper;
import com.bxt.picturebackend.model.entity.Picture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ShardingSphereTest {

    @Autowired
    private PictureMapper pictureMapper;

    @Test
    public void testInsertRouting() {
        System.out.println("开始测试分片插入...");

        // userId=1，应该路由到 picture_1 (1 % 8 = 1)
        Picture pic1 = new Picture();
        pic1.setUserId(1L);
        pic1.setUrl("test1.png");
        pic1.setName("用户1图片");
        pic1.setIntroduction("测试图片1");
        int result1 = pictureMapper.insert(pic1);
        System.out.println("插入用户1图片结果: " + result1 + ", ID: " + pic1.getId());

        // userId=9，应该路由到 picture_1 (因为 9 % 8 = 1)
        Picture pic9 = new Picture();
        pic9.setUserId(9L);
        pic9.setUrl("test9.png");
        pic9.setName("用户9图片");
        pic9.setIntroduction("测试图片9");
        int result9 = pictureMapper.insert(pic9);
        System.out.println("插入用户9图片结果: " + result9 + ", ID: " + pic9.getId());

        // userId=2，应该路由到 picture_2 (2 % 8 = 2)
        Picture pic2 = new Picture();
        pic2.setUserId(2L);
        pic2.setUrl("test2.png");
        pic2.setName("用户2图片");
        pic2.setIntroduction("测试图片2");
        int result2 = pictureMapper.insert(pic2);
        System.out.println("插入用户2图片结果: " + result2 + ", ID: " + pic2.getId());

        // userId=16，应该路由到 picture_0 (16 % 8 = 0)
        Picture pic16 = new Picture();
        pic16.setUserId(16L);
        pic16.setUrl("test16.png");
        pic16.setName("用户16图片");
        pic16.setIntroduction("测试图片16");
        int result16 = pictureMapper.insert(pic16);
        System.out.println("插入用户16图片结果: " + result16 + ", ID: " + pic16.getId());

        System.out.println("✅ 插入完成，请到数据库检查各分表的数据:");
        System.out.println("- picture_0: 应该有userId=16的数据");
        System.out.println("- picture_1: 应该有userId=1和userId=9的数据");
        System.out.println("- picture_2: 应该有userId=2的数据");
    }
}
