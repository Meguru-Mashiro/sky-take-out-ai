package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Tag(name = "套餐相关接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @PostMapping
    @Operation(summary="新增套餐")
    @CacheEvict(cacheNames = "setmealCache",key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary="套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @Operation(summary="套餐批量删除")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result delete(@RequestParam List<Long> ids) {
        log.info("套餐批量删除：{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }
    @PostMapping("/status/{status}")
    @Operation(summary="套餐状态修改")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result StartOrStop(@PathVariable Integer status,@RequestParam Long id) {
        log.info("套餐状态修改：{}", status);
        setmealService.StartOrStop(status,id);
        return Result.success();
    }
    @GetMapping("/{id}")
    @Operation(summary="根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }
    @PutMapping
    @Operation(summary="修改套餐")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result update(@RequestBody SetmealVO setmealVO) {
        log.info("修改套餐：{}", setmealVO);
        setmealService.updateWithDish(setmealVO);
        return Result.success();

    }
}
