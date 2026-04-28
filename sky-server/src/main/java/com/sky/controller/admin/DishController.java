package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Tag(name = "菜品相关接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    @Operation(summary="新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        clearCache(key);
        return Result.success();
    }
    @GetMapping("/page")
    @Operation(summary="菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}",dishPageQueryDTO);
        PageResult pageResult = dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    @DeleteMapping
    @Operation(summary="菜品批量删除")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量删除：{}",ids);
        dishService.deleteBatch(ids);
        //将所有的菜品缓存数据删除
        clearCache("dish_*");
        return Result.success();
    }
    @PostMapping("/status/{status}")
    @Operation(summary="菜品起售停售")
    public Result startOrStop(@PathVariable Integer status,Long id) {
        log.info("菜品状态修改：{}", status);
        dishService.startOrStop(status,id);
        //将所有的菜品缓存数据删除
        clearCache("dish_*");
        return Result.success();
    }
    @GetMapping("/{id}")
    @Operation(summary="根据id查询菜品和对应的口味")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品：{}",id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }
    @PutMapping
    @Operation(summary="修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){

        log.info("修改菜品：{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);
        //将所有的菜品缓存数据删除
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
        return Result.success();
    }
    @GetMapping("/list")
    @Operation(summary="根据分类查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        log.info("根据分类查询菜品：{}",categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    private void clearCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
