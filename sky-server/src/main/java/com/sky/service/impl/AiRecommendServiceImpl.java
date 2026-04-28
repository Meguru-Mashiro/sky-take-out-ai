package com.sky.service.impl;

import com.sky.client.AiClient;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.AiRecommendService;
import com.sky.service.UserBehaviorService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiRecommendServiceImpl implements AiRecommendService {

    @Autowired
    private AiClient aiClient;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String AI_RECOMMEND_CACHE = "ai:recommend:";
    private static final String AI_CHAT_HISTORY = "ai:chat:history:";
    private static final int MAX_HISTORY_SIZE = 11;

    @Override
    public List<DishVO> aiRecommend(Long userId, Integer limit) {
        String cacheKey = AI_RECOMMEND_CACHE + userId;
        List<DishVO> cached = (List<DishVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<Long> favoriteCategories = userBehaviorService.getUserFavoriteCategories(userId);
        List<Long> orderHistory = orderMapper.selectOrderHistory(userId);
        List<Long> hotDishes = userBehaviorService.getHotDishIds(10);

        String userContext = buildUserContext(userId, favoriteCategories, orderHistory);
        String hotContext = String.format("当前热销菜品ID：%s", hotDishes.toString());

        String systemPrompt = "你是本店专业的餐饮推荐助手，根据用户的口味偏好、历史订单和当前热销菜品，推荐最合适的菜品。" +
                "优先推荐符合用户口味且热销的菜品。" +
                "只返回菜品ID列表，用逗号分隔，不要其他内容。";

        String userMessage = String.format(
                "%s\n%s\n请推荐%d个菜品",
                userContext, hotContext, limit
        );

        String response = aiClient.chat(userMessage, systemPrompt);

        List<Long> recommendIds = parseDishIds(response);
        List<DishVO> result = getDishByIds(recommendIds, limit);

        if (result.isEmpty()) {
            result = getDishByIds(hotDishes, limit);
        }

        redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public String aiSearchDishes(String keywords, Long categoryId) {
        String systemPrompt = "你是外卖平台智能搜索助手，根据用户需求推荐菜品。" +
                "返回格式：推荐菜品列表，包含菜品名称、价格、推荐理由。";

        List<Dish> allDishes = dishMapper.selectAllDishes(categoryId);
        String dishContext = allDishes.stream()
                .limit(50)
                .map(d -> String.format("%s(价格:%.2f,描述:%s)",
                        d.getName(), d.getPrice(), d.getDescription()))
                .collect(Collectors.joining("\n"));

        String userMessage = String.format(
                "用户需求：%s\n可用菜品：\n%s\n请推荐合适的菜品",
                keywords, dishContext
        );

        return aiClient.chat(userMessage, systemPrompt);
    }

    @Override
    public String aiChat(String message, Long userId) {
        List<Long> orderHistory = orderMapper.selectOrderHistory(userId);
        String userContext = String.format("用户历史订单菜品ID：%s", orderHistory);

        String systemPrompt = "你是外卖平台智能客服，负责解答用户关于菜品、订单、配送等问题。\n" +
                "要求：\n" +
                "1. 回答简洁友好，不超过100字\n" +
                "2. 如果涉及具体订单，需要查询后回复\n" +
                "3. 遇到无法解决的问题，引导用户联系人工客服\n\n" +
                "用户信息：" + userContext;

        return aiClient.chat(message, systemPrompt);
    }


    @Override
    public SseEmitter streamChat(String message, Long userId) {
        SseEmitter emitter = new com.sky.config.SseEmitterUTF8(60000L);

        List<Long> orderHistory = orderMapper.selectOrderHistory(userId);
        String userContext = String.format("用户历史订单菜品ID：%s", orderHistory);

        String systemPrompt = "你是外卖平台智能客服，负责解答用户关于菜品、订单、配送等问题。\n" +
                "要求：\n" +
                "1. 回答简洁友好，不超过100字\n" +
                "2. 如果涉及具体订单，需要查询后回复\n" +
                "3. 遇到无法解决的问题，引导用户联系人工客服\n\n" +
                "用户信息：" + userContext;

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时，用户ID: {}", userId);
            try {
                Map<String, Object> errorData = new LinkedHashMap<>();
                errorData.put("content", "连接超时");
                errorData.put("isEnd", true);
                errorData.put("timestamp", System.currentTimeMillis());
                emitter.send(errorData);
                emitter.complete();
            } catch (IOException e) {
                log.error("发送超时消息失败", e);
            }
        });

        emitter.onError((ex) -> {
            log.error("SSE连接错误，用户ID: {}", userId, ex);
            emitter.completeWithError(ex);
        });

        new Thread(() -> {
            try {
                aiClient.streamChat(message, systemPrompt, chunk -> {
                    try {
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("content", chunk);
                        data.put("isEnd", false);
                        data.put("timestamp", System.currentTimeMillis());

                        log.info("【SSE输出】发送数据块: {}", com.alibaba.fastjson2.JSON.toJSONString(data));
                        emitter.send(data);
                    } catch (IOException e) {
                        log.error("发送SSE数据块失败", e);
                        emitter.completeWithError(e);
                    }
                });

                Map<String, Object> endData = new LinkedHashMap<>();
                endData.put("content", "");
                endData.put("isEnd", true);
                endData.put("timestamp", System.currentTimeMillis());

                log.info("【SSE输出】发送结束标记: {}", com.alibaba.fastjson2.JSON.toJSONString(endData));
                emitter.send(endData);
                emitter.complete();
            } catch (Exception e) {
                log.error("流式对话处理异常", e);
                try {
                    Map<String, Object> errorData = new LinkedHashMap<>();
                    errorData.put("content", "抱歉，系统繁忙，请稍后再试");
                    errorData.put("isEnd", true);
                    errorData.put("timestamp", System.currentTimeMillis());
                    emitter.send(errorData);
                } catch (IOException ex) {
                    log.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @Override
    public Map<String, Object> aiAnalyzeUserPreference(Long userId) {
        List<Long> orderHistory = orderMapper.selectOrderHistory(userId);
        List<Long> favoriteCategories = userBehaviorService.getUserFavoriteCategories(userId);

        String userContext = buildUserContext(userId, favoriteCategories, orderHistory);

        String systemPrompt = "你是餐饮数据分析专家，分析用户的饮食偏好。" +
                "返回JSON格式：{\"口味偏好\":\"...\",\"推荐分类\":\"...\",\"消费习惯\":\"...\"}";

        String response = aiClient.chat(userContext, systemPrompt);

        try {
            return com.alibaba.fastjson2.JSON.parseObject(response, Map.class);
        } catch (Exception e) {
            log.error("解析用户画像失败", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "分析失败");
            return errorMap;
        }
    }
    private String buildUserContext(Long userId, List<Long> categories, List<Long> orders) {
        return String.format(
                "用户ID：%d\n偏好分类：%s\n历史订单：%s",
                userId, categories, orders
        );
    }

    private List<Long> parseDishIds(String response) {
        try {
            String cleaned = response.replaceAll("[^0-9,，\\s]", "");
            return Arrays.stream(cleaned.split("[,，\\s]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析菜品ID失败: {}", response, e);
            return Collections.emptyList();
        }
    }

    private List<DishVO> getDishByIds(List<Long> ids, Integer limit) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        int end = Math.min(ids.size(), limit);
        return dishMapper.selectByIds(ids.subList(0, end));
    }
}
