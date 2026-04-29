package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderCommentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;           // 订单ID（必填）
    private Integer tasteScore;     // 口味评分（一级评价必填，1-5）
    private Integer packagingScore; // 包装评分（一级评价必填，1-5）
    private Integer deliveryScore;  // 配送评分（一级评价必填，1-5）
    private String tags;            // 标签（可选，逗号分隔）
    private String content;         // 评价内容（可选）
    private String images;          // 图片（可选，逗号分隔）
    private Long parentId;          // 父评价ID（null表示一级评价）
    private Integer commentType;   // 评价类型（1-用户评价，2-商家回复,3-用户追评）
    private Long replyToUserId;     // 回复用户ID（null表示无回复对象）
    private Long userId;            // 用户ID
}
