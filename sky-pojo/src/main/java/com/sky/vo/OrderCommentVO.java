package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;                // 评价ID
    private Long orderId;           // 订单ID
    private String userName;        // 用户名（脱敏处理）
    private Integer tasteScore;     // 口味评分
    private Integer packagingScore; // 包装评分
    private Integer deliveryScore;  // 配送评分
    private Double averageScore;    // 平均评分
    private List<String> tags;      // 标签列表
    private String content;         // 评价内容
    private List<String> images;    // 图片列表
    private Integer likeCount;      // 点赞数
    private Boolean liked;          // 当前用户是否已点赞
    private LocalDateTime createTime; // 创建时间
    private Long parentId;
    private Integer commentType;
    private String commentTypeName;
    private Long replyToUserId;
    private String replyToUserName;
    private List<OrderCommentVO> replies;
}
