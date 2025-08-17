package com.bxt.picturebackend.controller;

import com.bxt.picturebackend.aliYunAi.CreateTaskResponse;
import com.bxt.picturebackend.aliYunAi.DashScopeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    private final DashScopeClient dashScopeClient;

    public TestController(DashScopeClient dashScopeClient) {
        this.dashScopeClient = dashScopeClient;
    }

    @GetMapping("/test")
    public String test() throws Exception {
        CreateTaskResponse createResp = dashScopeClient.createTask(
            "https://bxttttt-1321961985.cos.ap-shanghai.myqcloud.com/public/1946081077693870082/2025-08-14_KkcCAq6i5zwOkOlx.webp"
        );
        return "任务ID: " + createResp.getOutput().getTask_id();
    }
}
