package com.sky.controller.admin;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.sky.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/admin/ai")
@Tag(name = "AI测试接口")
@Slf4j
public class AiTestController {

    @Autowired(required = false)
    private DashScopeChatModel chatModel;

    @GetMapping("/test")
    @Operation(summary = "测试AI功能")
    public Result<String> testAI(@RequestParam(defaultValue = "你好") String message) {
        if (chatModel == null) {
            return Result.error("AI服务未配置");
        }

        try {
            log.info("收到AI测试请求: {}", message);
            
            Prompt prompt = new Prompt(new UserMessage(message));
            ChatResponse response = chatModel.call(prompt);
            
            String result = response.getResult().getOutput().getText();
            log.info("AI回复: {}", result);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("AI调用失败", e);
            return Result.error("AI调用失败: " + e.getMessage());
        }
    }
    @GetMapping(value = "/testStream")
    @Operation(summary = "测试AI流式输出功能")
    public Flux<String> testAIStream(@RequestParam(value="message",defaultValue = "你好") String message,
                                     HttpServletResponse response) {
        //中文乱码
        response.setCharacterEncoding("UTF-8");
        if (chatModel == null) {
            return Flux.just("AI服务未配置");
        }

        Prompt prompt = new Prompt(new UserMessage(message));
        return chatModel.stream(prompt)
                .map(chatResponse -> chatResponse.getResult().getOutput().getText());
    }

}
