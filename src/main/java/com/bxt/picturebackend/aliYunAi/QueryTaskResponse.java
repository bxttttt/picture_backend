package com.bxt.picturebackend.aliYunAi;

// 查询任务响应

import lombok.Data;

@Data
public class QueryTaskResponse {
    private String request_id;
    private Output output;
    private Usage usage;
    @Data
    public static class Output {
        private String task_id;
        private String task_status;
        private String submit_time;
        private String scheduled_time;
        private String end_time;
        private String output_image_url;
        // getter & setter
    }
    @Data
    public static class Usage {
        private Integer image_count;
        // getter & setter
    }

    // getter & setter
}