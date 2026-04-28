package com.sky.service.impl;

import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.service.SceneRecommendService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SceneRecommendServiceImpl implements SceneRecommendService {

    @Autowired
    private DishMapper dishMapper;

    @Override
    public List<DishVO> getSceneRecommend(Long categoryId) {
        List<Dish> dishes = dishMapper.selectAllDishes(categoryId);
        return dishes.stream().limit(10).map(dish -> {
            DishVO vo = new DishVO();
            vo.setId(dish.getId());
            vo.setName(dish.getName());
            vo.setPrice(dish.getPrice());
            vo.setImage(dish.getImage());
            vo.setDescription(dish.getDescription());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public String getCurrentScene() {
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(10, 0))) {
            return "早餐";
        } else if (now.isAfter(LocalTime.of(10, 0)) && now.isBefore(LocalTime.of(14, 0))) {
            return "午餐";
        } else if (now.isAfter(LocalTime.of(14, 0)) && now.isBefore(LocalTime.of(17, 0))) {
            return "下午茶";
        } else if (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(21, 0))) {
            return "晚餐";
        } else {
            return "夜宵";
        }
    }
}
