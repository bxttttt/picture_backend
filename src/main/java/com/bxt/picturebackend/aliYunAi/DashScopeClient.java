package com.bxt.picturebackend.aliYunAi;

import cn.hutool.core.util.StrUtil;
import com.bxt.picturebackend.config.AliyunAiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashScopeClient {
    @Autowired
    private AliyunAiConfig aliyunAiConfig; // 注入配置类
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    /** 兼容 OpenAI 的对话接口（支持 tools） */
    private static final String CHAT_COMPLETIONS_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

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
//        params.setAngle(0);
//        params.setX_scale(1.5f);
//        params.setY_scale(1.5f);
        params.setLeft_offset(200);
        params.setRight_offset(150);
        params.setBest_quality(false);
        params.setLimit_image_size(true);
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

    /**
     * 调用兼容模式对话接口（支持 function calling）
     *
     * @param messages 消息列表，每项为 Map 含 role、content；若有 tool_calls 则含 tool_calls
     * @param tools    工具定义列表，每项为 Map type=function, function={name, description, parameters}
     * @return 响应中的 choices[0].message（content 与 tool_calls）
     */
    @SuppressWarnings("unchecked")
    public ChatCompletionResponse.Message chatWithTools(List<Map<String, Object>> messages, List<Map<String, Object>> tools) throws Exception {
        String apiKey = aliyunAiConfig.getApiKey();
        Map<String, Object> body = Map.of(
                "model", "qwen-plus",
                "messages", messages,
                "tools", tools
        );
        String json = mapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
        Request httpRequest = new Request.Builder()
                .url(CHAT_COMPLETIONS_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : response.message();
                throw new RuntimeException("DashScope 对话请求失败: " + response.code() + " " + err);
            }
            String responseBody = response.body() != null ? response.body().string() : "{}";
            ChatCompletionResponse parsed = mapper.readValue(responseBody, ChatCompletionResponse.class);
            if (parsed.getChoices() == null || parsed.getChoices().isEmpty()) {
                throw new RuntimeException("DashScope 返回无 choices");
            }
            return parsed.getChoices().get(0).getMessage();
        }
    }

    /**
     * 图片分析（多模态视觉理解）：根据图片 URL 和用户问题，返回对图片的描述或回答
     *
     * @param imageUrl   图片可公网访问的 URL
     * @param userPrompt 用户问题，如「描述这张图」「图中有什么」「什么风格」
     * @return 模型对图片的分析/描述文本
     */
    public String analyzeImage(String imageUrl, String userPrompt) throws Exception {
        String apiKey = aliyunAiConfig.getApiKey();
        List<Map<String, Object>> content = List.of(
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)),
                Map.of("type", "text", "text", StrUtil.isNotBlank(userPrompt) ? userPrompt : "请描述这张图片的内容、主体和风格。")
        );
        Map<String, Object> userMessage = Map.of("role", "user", "content", content);
        Map<String, Object> body = Map.of(
                "model", "qwen-vl-plus",
                "messages", List.of(userMessage)
        );
        String json = mapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(json, MediaType.parse("application/json"));
        Request httpRequest = new Request.Builder()
                .url(CHAT_COMPLETIONS_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : response.message();
                throw new RuntimeException("图片分析请求失败: " + response.code() + " " + err);
            }
            String responseBody = response.body() != null ? response.body().string() : "{}";
            ChatCompletionResponse parsed = mapper.readValue(responseBody, ChatCompletionResponse.class);
            if (parsed.getChoices() == null || parsed.getChoices().isEmpty()) {
                throw new RuntimeException("图片分析返回无内容");
            }
            String text = parsed.getChoices().get(0).getMessage().getContent();
            return text != null ? text : "无法解析图片内容。";
        }
    }
}
