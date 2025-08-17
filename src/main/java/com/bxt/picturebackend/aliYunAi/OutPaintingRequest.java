package com.bxt.picturebackend.aliYunAi;

import lombok.Data;

// 请求体相关模型
@Data
public class OutPaintingRequest {
    private String model;
    private Input input;
    private Parameters parameters;

    // getter & setter
    @Data
    public static class Input {
        private String image_url;
        // getter & setter
    }
    @Data
    public static class Parameters {
        private Integer angle;       // 可选
        private Float x_scale;       // 可选
        private Float y_scale;       // 可选
        private String output_ratio; // 可选
        private Integer top_offset;
        private Integer bottom_offset;
        private Integer left_offset;
        private Integer right_offset;
        private Boolean best_quality;
        private Boolean limit_image_size;
        private Boolean add_watermark;
        // getter & setter
    }
}
