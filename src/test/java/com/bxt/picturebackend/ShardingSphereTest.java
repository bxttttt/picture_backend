package com.bxt.picturebackend;

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
        // userId=1，应该路由到 picture_1
        Picture pic1 = new Picture();
        pic1.setUserId(1L);
        pic1.setUrl("test1.png");
        pic1.setName("用户1图片");
        pictureMapper.insert(pic1);

        // userId=9，应该路由到 picture_1 (因为 9 % 8 = 1)
        Picture pic9 = new Picture();
        pic9.setUserId(9L);
        pic9.setUrl("test9.png");
        pic9.setName("用户9图片");
        pictureMapper.insert(pic9);

        // userId=2，应该路由到 picture_2
        Picture pic2 = new Picture();
        pic2.setUserId(2L);
        pic2.setUrl("test2.png");
        pic2.setName("用户2图片");
        pictureMapper.insert(pic2);

        System.out.println("✅ 插入完成，请到数据库检查 picture_1 和 picture_2 表的数据");
    }
}
