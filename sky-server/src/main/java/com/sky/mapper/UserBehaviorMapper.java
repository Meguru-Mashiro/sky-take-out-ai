package com.sky.mapper;

import com.sky.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserBehaviorMapper {

    void insert(UserBehavior behavior);

    @Select("SELECT category_id FROM user_behavior WHERE user_id = #{userId} " +
            "GROUP BY category_id ORDER BY COUNT(*) DESC LIMIT 5")
    Set<Long> selectFavoriteCategories(Long userId);

    List<Long> selectByCategories(List<Long> categoryIds, Integer limit);

    @Select("SELECT dish_id FROM user_behavior WHERE behavior_type = 'ORDER' " +
            "GROUP BY dish_id ORDER BY COUNT(*) DESC LIMIT #{limit}")
    List<Long> selectHotDishes(Integer limit);

    List<Long> selectComboDishes(Long dishId, Integer limit);
}
