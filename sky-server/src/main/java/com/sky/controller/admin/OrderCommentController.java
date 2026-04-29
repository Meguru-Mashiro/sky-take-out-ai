package com.sky.controller.admin;

import com.sky.dto.OrderCommentDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminOrderCommentController")
@RequestMapping("/admin/comment")
@Tag(name = "管理端-评价管理接口")
@Slf4j
public class OrderCommentController {

    @Autowired
    private OrderCommentService orderCommentService;

    @GetMapping("/list")
    @Operation(summary = "分页查询评价列表")
    public Result<PageResult> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("管理员查询评价列表，页码：{}，每页条数：{}", pageNum, pageSize);
        PageResult pageResult = orderCommentService.listForAdmin(pageNum, pageSize);
        return Result.success(pageResult);
    }

    @PostMapping("/reply")
    @Operation(summary = "商家回复评价")
    public Result<String> reply(@RequestBody OrderCommentDTO dto) {
        log.info("商家回复评价，参数：{}", dto);
        // 设置默认商家ID（单商家架构）
        if (dto.getUserId() == null) {
            dto.setUserId(1L);  // 假设商家ID为1，根据实际情况调整
        }
        dto.setCommentType(2);
        orderCommentService.submit(dto);
        return Result.success("回复成功");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除评论")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        log.info("批量删除评论，IDs：{}", ids);
        orderCommentService.deleteBatch(ids);
        return Result.success("删除成功");
    }
}

