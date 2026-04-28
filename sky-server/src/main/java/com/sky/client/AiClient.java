package com.sky.client;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sky.properties.AiProperties;
import com.sky.utils.EmojiFilter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class AiClient {

    @Autowired
    private AiProperties aiProperties;

    private final OkHttpClient httpClient;

    public AiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String chat(String userMessage, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(createMessage("system", systemPrompt));
        }

        messages.add(createMessage("user", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", aiProperties.getTemperature());
        requestBody.put("max_tokens", aiProperties.getMaxTokens());

        String json = toJson(requestBody);

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, mediaType);

        Request request = new Request.Builder()
                .url(aiProperties.getApiUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("AI API调用失败，状态码: {}", response.code());
                return "抱歉，系统繁忙，请稍后再试";
            }

            String responseBody = response.body().string();
            return parseResponse(responseBody);
        } catch (IOException e) {
            log.error("AI API调用异常", e);
            return "抱歉，系统繁忙，请稍后再试";
        }
    }
    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    //流式输出
    //流式输出
    public void streamChat(String userMessage, String systemPrompt, Consumer<String> onChunk) {
        log.info("【AI流式】开始流式请求，消息: {}", userMessage);
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(createMessage("system", systemPrompt));
        }

        messages.add(createMessage("user", userMessage));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", aiProperties.getTemperature());
        requestBody.put("max_tokens", aiProperties.getMaxTokens());
        requestBody.put("stream", true);

        String json = toJson(requestBody);
        log.debug("【AI流式】请求参数: model={}, temperature={}, maxTokens={}",
                aiProperties.getModel(), aiProperties.getTemperature(), aiProperties.getMaxTokens());

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, mediaType);

        Request request = new Request.Builder()
                .url(aiProperties.getApiUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .build();

        log.info("【AI流式】发起HTTP请求到: {}", aiProperties.getApiUrl());

        try (Response response = httpClient.newCall(request).execute()) {
            log.info("【AI流式】收到响应，状态码: {}", response.code());

            if (!response.isSuccessful()) {
                log.error("【AI流式】API调用失败，状态码: {}, 消息: {}", response.code(), response.message());
                onChunk.accept("抱歉，系统繁忙，请稍后再试");
                return;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                log.error("【AI流式】响应体为空");
                onChunk.accept("抱歉，系统繁忙，请稍后再试");
                return;
            }

            log.info("【AI流式】开始读取SSE数据流");
            int lineCount = 0;
            int chunkCount = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    lineCount++;

                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);

                        if ("[DONE]".equals(data)) {
                            log.info("【AI流式】收到结束标记[DONE]，总共处理{}个数据块", chunkCount);
                            break;
                        }

                        try {
                            JSONObject jsonObject = JSONObject.parseObject(data);
                            JSONArray choices = jsonObject.getJSONArray("choices");

                            if (choices != null && !choices.isEmpty()) {
                                JSONObject choice = choices.getJSONObject(0);
                                JSONObject delta = choice.getJSONObject("delta");

                                if (delta != null && delta.containsKey("content")) {
                                    String content = delta.getString("content");
                                    if (content != null && !content.isEmpty()) {
                                        chunkCount++;

                                        // 过滤emoji等特殊字符
                                        String filteredContent = EmojiFilter.filterEmoji(content);

                                        log.debug("【AI流式】解析到第{}个数据块（过滤前）: {}", chunkCount, content);
                                        log.debug("【AI流式】解析到第{}个数据块（过滤后）: {}", chunkCount, filteredContent);

                                        // 只有过滤后有内容才发送
                                        if (!filteredContent.isEmpty()) {
                                            onChunk.accept(filteredContent);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("【AI流式】解析SSE数据块失败，行号: {}, 数据: {}", lineCount, data, e);
                        }
                    }
                }
            }

            log.info("【AI流式】SSE数据流读取完成，总行数: {}, 有效数据块: {}", lineCount, chunkCount);
        } catch (IOException e) {
            log.error("【AI流式】API调用异常", e);
            onChunk.accept("抱歉，系统繁忙，请稍后再试");
        }
    }

    private String parseResponse(String json) {
        try {
            com.alibaba.fastjson2.JSONObject jsonObject = com.alibaba.fastjson2.JSON.parseObject(json);
            com.alibaba.fastjson2.JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                return choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
            return "抱歉，系统繁忙，请稍后再试";
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            return "抱歉，系统繁忙，请稍后再试";
        }
    }

    private String toJson(Object obj) {
        return com.alibaba.fastjson2.JSON.toJSONString(obj);
    }
}
