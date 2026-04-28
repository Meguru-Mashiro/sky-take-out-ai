package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.UserBehaviorDTO;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SceneRecommendService;
import com.sky.service.UserBehaviorService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Tag(name = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserBehaviorService userBehaviorService;
    @Autowired
    private SceneRecommendService sceneRecommendService;
    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary="根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        String key = "dish_"+categoryId;
        List<DishVO> list = (List<DishVO>)redisTemplate.opsForValue().get(key);
        if(list != null&&list.size()>0) {
            return Result.success(list);
        }
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品
        //如果不存在，查询数据库，将查询到的数据放入redis中
        list = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key,list);
        return Result.success(list);
    }
    @PostMapping("/behavior")
    @Operation(summary="记录用户行为")
    public Result<Void> recordBehavior(@RequestBody UserBehaviorDTO dto) {
        dto.setUserId(BaseContext.getCurrentId());
        userBehaviorService.recordBehavior(dto);
        return Result.success();
    }
//
//    @GetMapping("/hot")
//    @Operation(summary="热门菜品")
//    public Result<List<DishVO>> hot() {
//        List<Long> hotIds = userBehaviorService.getHotDishIds(10);
//        if (hotIds.isEmpty()) {
//            return Result.success(List.of());
//        }
//        List<DishVO> list = dishService.listWithFlavor(new Dish());
//        return Result.success(list.stream()
//                .filter(vo -> hotIds.contains(vo.getId()))
//                .limit(10)
//                .toList());
//    }
//
//    @GetMapping("/scene")
//    @Operation(summary="场景推荐")
//    public Result<Map<String, Object>> sceneRecommend() {
//        String scene = sceneRecommendService.getCurrentScene();
//        List<DishVO> list = sceneRecommendService.getSceneRecommend(null);
//        Map<String, Object> result = new java.util.HashMap<>();
//        result.put("scene", scene);
//        result.put("dishes", list);
//        return Result.success(result);
//    }
//
//    @GetMapping("/combo/{dishId}")
//    @Operation(summary="组合推荐")
//    public Result<List<DishVO>> comboRecommend(@PathVariable Long dishId) {
//        List<Long> comboIds = userBehaviorService.getComboRecommendIds(dishId, 5);
//        if (comboIds.isEmpty()) {
//            return Result.success(List.of());
//        }
//        List<DishVO> allDishes = dishService.listWithFlavor(new Dish());
//        return Result.success(allDishes.stream()
//                .filter(vo -> comboIds.contains(vo.getId()))
//                .toList());
//    }

}
