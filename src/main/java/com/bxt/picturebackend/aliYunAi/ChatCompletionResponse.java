package com.bxt.picturebackend.aliYunAi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DashScope 兼容模式 chat/completions 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {

    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Message message;
        public Message getMessage() { return message; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;

        public String getRole() { return role; }
        public String getContent() { return content; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolCall {
        private String id;
        private Function function;

        public String getId() { return id; }
        public Function getFunction() { return function; }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Function {
        private String name;
        private String arguments;

        public String getName() { return name; }
        public String getArguments() { return arguments; }
    }
}
