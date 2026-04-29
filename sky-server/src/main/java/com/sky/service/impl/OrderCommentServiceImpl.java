package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.OrderCommentDTO;
import com.sky.entity.OrderComment;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.exception.BaseException;
import com.sky.mapper.OrderCommentMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderCommentService;
import com.sky.vo.OrderCommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderCommentServiceImpl implements OrderCommentService {

    @Autowired
    private OrderCommentMapper orderCommentMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String COMMENT_LIKE_KEY = "comment:like:";

    @Override
    @Transactional
    public void submit(OrderCommentDTO dto) {
        log.info("提交评论，订单ID：{}，用户ID：{}，类型：{}", dto.getOrderId(), dto.getUserId(), dto.getCommentType());

        Integer commentType = dto.getCommentType();

        if (commentType == null) {
            throw new BaseException("评论类型不能为空");
        }

        if (commentType == 1) {
            validatePrimaryComment(dto);
        } else if (commentType == 2) {
            validateMerchantReply(dto);
        } else if (commentType == 3) {
            validateUserReply(dto);
        } else {
            throw new BaseException("无效的评论类型");
        }

        OrderComment comment = OrderComment.builder()
                .orderId(dto.getOrderId())
                .userId(dto.getUserId())
                .tasteScore(dto.getTasteScore())
                .packagingScore(dto.getPackagingScore())
                .deliveryScore(dto.getDeliveryScore())
                .tags(dto.getTags())
                .content(dto.getContent())
                .images(dto.getImages())
                .likeCount(0)
                .createTime(LocalDateTime.now())
                .parentId(dto.getParentId())
                .commentType(commentType)
                .replyToUserId(dto.getReplyToUserId())
                .build();

        orderCommentMapper.insert(comment);
    }

    private void validatePrimaryComment(OrderCommentDTO dto) {
        if (dto.getParentId() != null) {
            throw new BaseException("一级评价不能指定父评论");
        }

        Orders orders = orderMapper.getById(dto.getOrderId());
        if (orders == null) {
            throw new BaseException("订单不存在");
        }

        if (orders.getStatus() != 5) {
            throw new BaseException("只能对已完成的订单进行评价");
        }

        OrderComment existing = orderCommentMapper.getByOrderIdAndUserId(dto.getOrderId(), dto.getUserId());
        if (existing != null) {
            throw new BaseException("该订单已评价");
        }
    }

    private void validateUserReply(OrderCommentDTO dto) {
        if (dto.getParentId() == null) {
            throw new BaseException("用户追评必须指定父评论");
        }

        OrderComment parentComment = orderCommentMapper.getById(dto.getParentId());
        if (parentComment == null) {
            throw new BaseException("回复的评论不存在");
        }

        Orders orders = orderMapper.getById(dto.getOrderId());
        if (orders == null) {
            throw new BaseException("订单不存在");
        }

        if (!Objects.equals(orders.getId(), parentComment.getOrderId())) {
            throw new BaseException("回复的订单与父评论不匹配");
        }
    }

    private void validateMerchantReply(OrderCommentDTO dto) {
        if (dto.getParentId() == null) {
            throw new BaseException("商家回复必须指定父评论");
        }

        OrderComment parentComment = orderCommentMapper.getById(dto.getParentId());
        if (parentComment == null) {
            throw new BaseException("回复的评论不存在");
        }

        if (parentComment.getCommentType() == 2) {
            throw new BaseException("商家不能回复其他商家的评论");
        }

        Orders orders = orderMapper.getById(dto.getOrderId());
        if (orders == null) {
            throw new BaseException("订单不存在");
        }

        if (!Objects.equals(orders.getId(), parentComment.getOrderId())) {
            throw new BaseException("回复的订单与父评论不匹配");
        }
    }

    @Override
    public List<OrderCommentVO> listByTag(String tag, Long currentUserId) {
        log.info("查询评价列表，标签：{}", tag);

        List<OrderComment> comments = orderCommentMapper.listByTag(tag);
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> allCommentIds = collectAllCommentIds(comments);
        Map<Long, List<OrderComment>> repliesMap = batchLoadAllReplies(allCommentIds);
        Set<Long> allUserIds = collectAllUserIds(comments, repliesMap);
        Map<Long, User> userMap = batchLoadUsers(allUserIds);

        return comments.stream()
                .map(comment -> convertToVOWithCache(comment, currentUserId, repliesMap, userMap))
                .collect(Collectors.toList());
    }

    private Set<Long> collectAllCommentIds(List<OrderComment> comments) {
        Set<Long> ids = new HashSet<>();
        for (OrderComment comment : comments) {
            ids.add(comment.getId());
        }
        return ids;
    }

    private Map<Long, List<OrderComment>> batchLoadAllReplies(Set<Long> parentIds) {
        Map<Long, List<OrderComment>> result = new HashMap<>();
        Set<Long> toProcess = new HashSet<>(parentIds);
        Set<Long> processed = new HashSet<>();

        while (!toProcess.isEmpty()) {
            Set<Long> nextLevel = new HashSet<>();
            for (Long parentId : toProcess) {
                if (processed.contains(parentId)) {
                    continue;
                }

                List<OrderComment> replies = orderCommentMapper.getRepliesByParentId(parentId);
                if (replies != null && !replies.isEmpty()) {
                    result.put(parentId, replies);
                    processed.add(parentId);

                    for (OrderComment reply : replies) {
                        nextLevel.add(reply.getId());
                    }
                } else {
                    processed.add(parentId);
                }
            }
            toProcess = nextLevel;
        }

        return result;
    }

    private Set<Long> collectAllUserIds(List<OrderComment> comments, Map<Long, List<OrderComment>> repliesMap) {
        Set<Long> userIds = new HashSet<>();

        for (OrderComment comment : comments) {
            userIds.add(comment.getUserId());
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
        }

        for (List<OrderComment> replies : repliesMap.values()) {
            for (OrderComment reply : replies) {
                userIds.add(reply.getUserId());
                if (reply.getReplyToUserId() != null) {
                    userIds.add(reply.getReplyToUserId());
                }
            }
        }

        return userIds;
    }

    private Map<Long, User> batchLoadUsers(Set<Long> userIds) {
        Map<Long, User> userMap = new HashMap<>();
        for (Long userId : userIds) {
            User user = userMapper.getById(userId);
            if (user != null) {
                userMap.put(userId, user);
            }
        }
        return userMap;
    }

    private OrderCommentVO convertToVOWithCache(OrderComment comment, Long currentUserId,
                                                Map<Long, List<OrderComment>> repliesMap,
                                                Map<Long, User> userMap) {
        OrderCommentVO vo = new OrderCommentVO();
        BeanUtils.copyProperties(comment, vo);

        if (comment.getCommentType() != null && comment.getCommentType() == 2) {
            vo.setUserName("智能餐饮");
        } else {
            User user = userMap.get(comment.getUserId());
            vo.setUserName(user != null && user.getName() != null ? user.getName() : "匿名用户");
        }

        if (comment.getReplyToUserId() != null) {
            User replyToUser = userMap.get(comment.getReplyToUserId());
            vo.setReplyToUserName(replyToUser != null && replyToUser.getName() != null ? replyToUser.getName() : "匿名用户");
        }

        vo.setTags(parseToList(comment.getTags()));
        vo.setImages(parseToList(comment.getImages()));

        if (comment.getTasteScore() != null && comment.getPackagingScore() != null && comment.getDeliveryScore() != null) {
            double avg = (comment.getTasteScore() + comment.getPackagingScore() + comment.getDeliveryScore()) / 3.0;
            vo.setAverageScore(avg);
        }

        vo.setCommentTypeName(getCommentTypeName(comment.getCommentType()));

        String key = COMMENT_LIKE_KEY + comment.getId();
        Boolean isLiked = redisTemplate.opsForSet().isMember(key, currentUserId != null ? currentUserId.toString() : "0");
        vo.setLiked(Boolean.TRUE.equals(isLiked));

        List<OrderComment> replies = repliesMap.get(comment.getId());
        if (replies != null && !replies.isEmpty()) {
            List<OrderCommentVO> replyVOs = replies.stream()
                    .map(reply -> convertToVOWithCache(reply, currentUserId, repliesMap, userMap))
                    .collect(Collectors.toList());
            vo.setReplies(replyVOs);
        }

        return vo;
    }

    private String getCommentTypeName(Integer type) {
        if (type == null) return "评价";
        switch (type) {
            case 1: return "用户评价";
            case 2: return "商家回复";
            case 3: return "用户追评";
            default: return "未知";
        }
    }

    @Override
    public void toggleLike(Long commentId, Long userId) {
        String key = COMMENT_LIKE_KEY + commentId;
        String userIdStr = userId.toString();

        Boolean isLiked = redisTemplate.opsForSet().isMember(key, userIdStr);

        if (Boolean.TRUE.equals(isLiked)) {
            redisTemplate.opsForSet().remove(key, userIdStr);
            orderCommentMapper.decrementLikeCount(commentId);
        } else {
            redisTemplate.opsForSet().add(key, userIdStr);
            orderCommentMapper.incrementLikeCount(commentId);
        }
    }

    @Override
    public PageResult listForAdmin(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<OrderComment> comments = orderCommentMapper.listByTag(null);
        Page<OrderComment> page = (Page<OrderComment>) comments;

        if (comments == null || comments.isEmpty()) {
            return new PageResult(0L, Collections.emptyList());
        }

        Set<Long> allCommentIds = collectAllCommentIds(comments);
        Map<Long, List<OrderComment>> repliesMap = batchLoadAllReplies(allCommentIds);
        Set<Long> allUserIds = collectAllUserIds(comments, repliesMap);
        Map<Long, User> userMap = batchLoadUsers(allUserIds);

        List<OrderCommentVO> voList = page.getResult().stream()
                .map(comment -> convertToVOWithCache(comment, null, repliesMap, userMap))
                .collect(Collectors.toList());

        return new PageResult(page.getTotal(), voList);
    }

    private List<String> parseToList(String str) {
        if (str == null || str.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BaseException("删除的评论ID不能为空");
        }

        List<Long> allIdsToDelete = new ArrayList<>(ids);
        for (Long id : ids) {
            collectChildComments(id, allIdsToDelete);
        }

        orderCommentMapper.deleteBatch(allIdsToDelete);
        redisTemplate.delete(allIdsToDelete.stream()
                .map(id -> COMMENT_LIKE_KEY + id)
                .collect(Collectors.toList()));
    }

    private void collectChildComments(Long parentId, List<Long> allIds) {
        List<OrderComment> children = orderCommentMapper.getRepliesByParentId(parentId);
        if (children != null && !children.isEmpty()) {
            for (OrderComment child : children) {
                if (!allIds.contains(child.getId())) {
                    allIds.add(child.getId());
                    collectChildComments(child.getId(), allIds);
                }
            }
        }
    }
}

