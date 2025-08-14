package com.bxt.picturebackend.imageSearch.sub;

import cn.hutool.http.HttpResponse;
import com.mysql.cj.x.protobuf.MysqlxResultset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetImagePageUrlApiTest {
    @Test
    void testGetImagePageUrl() {
        String testImageUrl = "https://i2.hdslb.com/bfs/archive/ad698e40cc6dd3d03ae5d0ab7bfa50faf368bd9b.jpg";
        String response = GetImagePageUrlApi.getImagePageUrl(testImageUrl);
        System.out.println(response);
    }
    @Test
    void testGetUrlList() {
        String testImageUrl = "https://i2.hdslb.com/bfs/archive/ad698e40cc6dd3d03ae5d0ab7bfa50faf368bd9b.jpg";
        System.out.println(GetImagePageUrlApi.getUrlList(testImageUrl));
    }
}