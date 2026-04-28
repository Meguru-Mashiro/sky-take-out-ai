package com.sky.service;
import com.sky.vo.DishVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiRecommendService {

    List<DishVO> aiRecommend(Long userId, Integer limit);

    String aiSearchDishes(String keywords, Long categoryId);

    String aiChat(String message, Long userId);
    public SseEmitter streamChat(String message, Long userId);
    Map<String, Object> aiAnalyzeUserPreference(Long userId);
}
