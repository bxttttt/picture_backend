package com.bxt.picturebackend.aliYunAi;

import lombok.Data;

// 创建任务响应
@Data
public class CreateTaskResponse {
    private Output output;
    private String request_id;
    @Data
    public static class Output {
        private String task_status;
        private String task_id;
        // getter & setter
    }

    // getter & setter
}