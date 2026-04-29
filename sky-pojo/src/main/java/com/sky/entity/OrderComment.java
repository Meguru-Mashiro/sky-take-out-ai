package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderComment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;              // 评价ID
    private Long orderId;         // 订单ID
    private Long userId;          // 用户ID
    private Integer tasteScore;   // 口味评分(1-5分)
    private Integer packagingScore; // 包装评分(1-5分)
    private Integer deliveryScore;  // 配送评分(1-5分)
    private String tags;          // 标签(逗号分隔，如"味道好,包装精美")
    private String content;       // 评价内容
    private String images;        // 评价图片(逗号分隔的URL)
    private Integer likeCount;    // 点赞数
    private LocalDateTime createTime; // 创建时间
    private Long parentId;         // 父评价ID(null表示一级评价)
    private Integer commentType;  // 评价类型(1-用户评价，2-商家回复,3-用户追评)
    private Long replyToUserId;    // 回复用户ID(null表示无回复对象)
}
