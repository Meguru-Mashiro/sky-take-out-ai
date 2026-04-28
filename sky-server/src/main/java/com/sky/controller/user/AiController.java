package com.sky.controller.user;
import com.sky.context.BaseContext;
import com.sky.dto.AiChatDTO;
import com.sky.result.Result;
import com.sky.service.AiRecommendService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController("userAiController")
@RequestMapping("/user/ai")
@Slf4j
@Tag(name = "C端-AI智能服务接口")
public class AiController {

    @Autowired
    private AiRecommendService aiRecommendService;

    @GetMapping("/recommend")
    @Operation(summary="AI智能推荐菜品")
    public Result<List<DishVO>> aiRecommend(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = BaseContext.getCurrentId();
        List<DishVO> list = aiRecommendService.aiRecommend(userId, limit);
        return Result.success(list);
    }

    @GetMapping("/search")
    @Operation(summary="AI语义搜索菜品")
    public Result<String> aiSearch(@RequestParam String keywords,
                                   @RequestParam(required = false) Long categoryId) {
        String result = aiRecommendService.aiSearchDishes(keywords, categoryId);
        return Result.success(result);
    }

    @PostMapping("/chat")
    @Operation(summary="AI智能客服对话")
    public Result<String> aiChat(@RequestBody AiChatDTO dto) {
        Long userId = BaseContext.getCurrentId();
        String response = aiRecommendService.aiChat(dto.getMessage(), userId);
        return Result.success(response);
    }
// ... existing code ...

    @GetMapping(value = "/chat/stream")
    @Operation(summary="AI智能客服对话-流式输出")
    public SseEmitter streamChat(@RequestParam String message) {
        Long userId = BaseContext.getCurrentId();
        log.info("【流式接口】用户{}发起流式对话请求: {}", userId, message);

        try {
            SseEmitter emitter = aiRecommendService.streamChat(message, userId);
            log.info("【流式接口】SseEmitter创建成功，用户ID: {}", userId);
            return emitter;
        } catch (Exception e) {
            log.error("【流式接口】创建流式对话失败，用户ID: {}", userId, e);
            throw e;
        }
    }

// ... existing code ...

    @GetMapping("/preference")
    @Operation(summary="AI分析用户偏好")
    public Result<Map<String, Object>> analyzePreference() {
        Long userId = BaseContext.getCurrentId();
        Map<String, Object> preference = aiRecommendService.aiAnalyzeUserPreference(userId);
        return Result.success(preference);
    }
}
