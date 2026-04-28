package com.sky.service.impl;

import com.sky.dto.UserBehaviorDTO;
import com.sky.entity.UserBehavior;
import com.sky.mapper.UserBehaviorMapper;
import com.sky.service.UserBehaviorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserBehaviorServiceImpl implements UserBehaviorService {

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String BEHAVIOR_KEY = "user:behavior:";
    private static final String HOT_DISH_KEY = "dish:hot:";
    private static final String COMBO_KEY = "dish:combo:";

    @Override
    public void recordBehavior(UserBehaviorDTO dto) {
        UserBehavior behavior = UserBehavior.builder()
                .userId(dto.getUserId())
                .dishId(dto.getDishId())
                .categoryId(dto.getCategoryId())
                .behaviorType(dto.getBehaviorType())
                .behaviorTime(LocalDateTime.now())
                .build();

        userBehaviorMapper.insert(behavior);

        String key = BEHAVIOR_KEY + dto.getUserId();
        redisTemplate.opsForZSet().add(key,
                dto.getDishId().toString(),
                System.currentTimeMillis());
        redisTemplate.expire(key, 30, TimeUnit.DAYS);

        if ("VIEW".equals(dto.getBehaviorType()) || "ORDER".equals(dto.getBehaviorType())) {
            String hotKey = HOT_DISH_KEY + dto.getDishId();
            redisTemplate.opsForValue().increment(hotKey);
            redisTemplate.expire(hotKey, 7, TimeUnit.DAYS);
        }
    }

    @Override
    public List<Long> getUserFavoriteCategories(Long userId) {
        String key = BEHAVIOR_KEY + userId + ":category";
        Set<Object> categories = redisTemplate.opsForZSet().reverseRange(key, 0, 4);
        if (categories == null || categories.isEmpty()) {
            Set<Long> dbCategories = userBehaviorMapper.selectFavoriteCategories(userId);
            if (dbCategories != null && !dbCategories.isEmpty()) {
                for (Long categoryId : dbCategories) {
                    redisTemplate.opsForZSet().add(key, categoryId.toString(), System.currentTimeMillis());
                }
                redisTemplate.expire(key, 7, TimeUnit.DAYS);
                return new ArrayList<>(dbCategories);
            }
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>();
        for (Object obj : categories) {
            try {
                if (obj instanceof String) {
                    result.add(Long.parseLong((String) obj));
                } else if (obj instanceof Long) {
                    result.add((Long) obj);
                } else if (obj instanceof Integer) {
                    result.add(((Integer) obj).longValue());
                }
            } catch (NumberFormatException e) {
                log.warn("解析分类ID失败: {}", obj);
            }
        }
        return result;
    }
    @Override
    public List<Long> getRecommendDishIds(Long userId, Integer limit) {
        List<Long> favoriteCategories = getUserFavoriteCategories(userId);
        if (favoriteCategories.isEmpty()) {
            return getHotDishIds(limit);
        }

        List<Long> recommendIds = userBehaviorMapper.selectByCategories(favoriteCategories, limit);
        return recommendIds;
    }

    @Override
    public List<Long> getHotDishIds(Integer limit) {
        Set<String> keys = redisTemplate.keys(HOT_DISH_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return userBehaviorMapper.selectHotDishes(limit);
        }

        Map<Long, Integer> hotMap = new HashMap<>();
        for (String key : keys) {
            String dishIdStr = key.replace(HOT_DISH_KEY, "");
            Long dishId = Long.parseLong(dishIdStr);
            Integer count = (Integer) redisTemplate.opsForValue().get(key);
            hotMap.put(dishId, count);
        }

        return hotMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getComboRecommendIds(Long dishId, Integer limit) {
        String key = COMBO_KEY + dishId;
        Set<Object> comboIds = redisTemplate.opsForZSet().reverseRange(key, 0, (long) limit - 1);
        if (comboIds == null || comboIds.isEmpty()) {
            List<Long> dbComboIds = userBehaviorMapper.selectComboDishes(dishId, limit);
            if (dbComboIds != null && !dbComboIds.isEmpty()) {
                redisTemplate.opsForZSet().add(key, dbComboIds.toArray(), System.currentTimeMillis());
                redisTemplate.expire(key, 3, TimeUnit.DAYS);
                return dbComboIds;
            }
        }
        return comboIds.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }
}
