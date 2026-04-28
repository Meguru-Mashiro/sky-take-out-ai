package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
@Tag(name = "c端-购物车相关接口")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @RequestMapping("/add")
    @Operation(summary="添加购物车")
    public Result save(@RequestBody ShoppingCartDTO shoppingDTO){
        log.info("添加购物车,商品信息为：{}", shoppingDTO);
        shoppingCartService.addShoppingCart(shoppingDTO);
        return Result.success();
    }
    @GetMapping("/list")
    @Operation(summary="查看购物车")
    public Result<List<ShoppingCart>> list() {
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }
    @DeleteMapping("/clean")
    @Operation(summary="清空购物车")
    public Result clean() {
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
    @PostMapping("/sub")
    @Operation(summary="删除购物车中一个商品")
    public Result sub(@RequestBody ShoppingCartDTO shoppingDTO) {
        shoppingCartService.subShoppingCart(shoppingDTO);
        return Result.success();
    }

}
