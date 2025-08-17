package com.bxt.picturebackend.aliYunAi;

import com.bxt.picturebackend.config.AliyunAiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashScopeClient {
    @Autowired
    private AliyunAiConfig aliyunAiConfig; // 注入配置类
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

//    public DashScopeClient(AliyunAiConfig aliyunAiConfig) {
//        this.aliyunAiConfig = aliyunAiConfig;
//    }

    // Step 1: 创建任务
    public CreateTaskResponse createTask(String imageUrl) throws Exception {
        String apiKey = aliyunAiConfig.getApiKey();
        System.out.println("apiKey: " + apiKey);

        OutPaintingRequest request = new OutPaintingRequest();
        request.setModel("image-out-painting");

        OutPaintingRequest.Input input = new OutPaintingRequest.Input();
        input.setImage_url(imageUrl);
        request.setInput(input);

        OutPaintingRequest.Parameters params = new OutPaintingRequest.Parameters();
        params.setAngle(0);
        params.setX_scale(1.5f);
        params.setY_scale(1.5f);
        request.setParameters(params);

        String json = mapper.writeValueAsString(request);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/services/aigc/image2image/out-painting")
                .addHeader("Authorization", "Bearer " + apiKey) // ✅ 使用配置里的 key
                .addHeader("Content-Type", "application/json")
                .addHeader("X-DashScope-Async", "enable")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("请求失败: " + response);
            assert response.body() != null;
            return mapper.readValue(response.body().string(), CreateTaskResponse.class);
        }
    }

    // Step 2: 查询任务结果
    public QueryTaskResponse queryTask(String taskId) throws Exception {
        String apiKey = aliyunAiConfig.getApiKey();

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/tasks/" + taskId)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) throw new RuntimeException("请求失败: " + response);
            return mapper.readValue(response.body().string(), QueryTaskResponse.class);
        }
    }
}
