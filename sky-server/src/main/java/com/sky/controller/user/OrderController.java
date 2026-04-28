package com.sky.controller.user;

import com.github.pagehelper.PageHelper;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Tag(name = "c端-用户订单接口")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;
    @PostMapping("/submit")
    @Operation(summary ="用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单，参数为：{}",ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @Operation(summary ="订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }
    @GetMapping("/historyOrders")
    @Operation(summary ="历史订单查询")
    public Result<PageResult> page(int page, int pageSize,Integer status){
        log.info("历史订单查询，page={},pageSize={},status={}",page,pageSize,status);
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }
    @GetMapping("/orderDetail/{id}")
    @Operation(summary ="查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id){
        log.info("查询订单详情，id为{}",id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }
    @PutMapping("/cancel/{id}")
    @Operation(summary ="取消订单")
    public Result cancel(@PathVariable Long id) throws Exception {
         log.info("取消订单，id为{}",id);
         orderService.userCancelById(id);
         return Result.success();
    }
    @PostMapping("repetition/{id}")
    @Operation(summary ="再来一单")
    public Result repetition(@PathVariable Long id){
        log.info("再来一单，id为{}",id);
        orderService.repetition(id);
        return Result.success();
    }
    @GetMapping("reminder/{id}")
    @Operation(summary ="客户催单")
    public Result reminder(@PathVariable Long id){
        log.info("用户催单，id为{}",id);
        orderService.reminder(id);
        return Result.success();
    }
}
