package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.OrderCommentDTO;
import com.sky.result.Result;
import com.sky.service.OrderCommentService;
import com.sky.vo.OrderCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userOrderCommentController")
@RequestMapping("/user/comment")
@Tag(name = "C端-订单评价接口")
@Slf4j
public class OrderCommentController {

    @Autowired
    private OrderCommentService orderCommentService;

    @PostMapping
    @Operation(summary = "提交订单评价或追评")
    public Result<String> submit(@RequestBody OrderCommentDTO dto) {
        log.info("用户提交评价，参数：{}", dto);
        if (dto.getUserId() == null) {
            Long currentUserId = BaseContext.getCurrentId();
            dto.setUserId(currentUserId);
        }
        if (dto.getParentId() == null) {
            dto.setCommentType(1);
        } else {
            dto.setCommentType(3);
        }

        orderCommentService.submit(dto);
        return Result.success(dto.getParentId() == null ? "评价成功" : "追评成功");
    }

    @GetMapping("/list")
    @Operation(summary = "查询评价列表")
    public Result<List<OrderCommentVO>> list(@RequestParam(required = false) String tag) {
        log.info("查询评价列表，筛选标签：{}", tag);
        Long currentUserId = BaseContext.getCurrentId();
        List<OrderCommentVO> list = orderCommentService.listByTag(tag, currentUserId);
        return Result.success(list);
    }

    @PostMapping("/like/{commentId}")
    @Operation(summary = "切换点赞状态")
    public Result<String> toggleLike(@PathVariable Long commentId, @RequestParam(required = false) Long userId) {
        log.info("用户切换点赞状态，评论ID：{}，用户ID：{}", commentId, userId);
        orderCommentService.toggleLike(commentId, userId);
        return Result.success("操作成功");
    }
}

