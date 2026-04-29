package com.sky.service;

import com.sky.dto.OrderCommentDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderCommentVO;

import java.util.List;

public interface OrderCommentService {

    void submit(OrderCommentDTO dto);

    List<OrderCommentVO> listByTag(String tag, Long currentUserId);

    void toggleLike(Long commentId, Long userId);

    PageResult listForAdmin(Integer pageNum, Integer pageSize);

    void deleteBatch(List<Long> ids);
}
