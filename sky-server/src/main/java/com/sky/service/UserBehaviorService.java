package com.sky.service;

import com.sky.dto.UserBehaviorDTO;

import java.util.List;

public interface UserBehaviorService {

    void recordBehavior(UserBehaviorDTO dto);

    List<Long> getUserFavoriteCategories(Long userId);

    List<Long> getRecommendDishIds(Long userId, Integer limit);

    List<Long> getHotDishIds(Integer limit);

    List<Long> getComboRecommendIds(Long dishId, Integer limit);
}
