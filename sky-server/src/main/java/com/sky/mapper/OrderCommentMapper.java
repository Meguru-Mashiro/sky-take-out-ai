package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.OrderComment;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderCommentMapper {

    @Insert("insert into order_comment (order_id, user_id, taste_score, packaging_score, delivery_score, tags, content, images, like_count, create_time, parent_id, comment_type, reply_to_user_id) " +
            "values (#{orderId}, #{userId}, #{tasteScore}, #{packagingScore}, #{deliveryScore}, #{tags}, #{content}, #{images}, #{likeCount}, #{createTime}, #{parentId}, #{commentType}, #{replyToUserId})")
    void insert(OrderComment comment);

    @Select("select * from order_comment where order_id = #{orderId} and user_id = #{userId} and parent_id IS NULL")
    OrderComment getByOrderIdAndUserId(Long orderId, Long userId);

    List<OrderComment> listByTag(String tag);

    @Select("select * from order_comment where parent_id = #{parentId} order by create_time asc")
    List<OrderComment> getRepliesByParentId(Long parentId);

    @Update("update order_comment set like_count = like_count + 1 where id = #{id}")
    void incrementLikeCount(Long id);

    @Update("update order_comment set like_count = like_count - 1 where id = #{id} and like_count > 0")
    void decrementLikeCount(Long id);

    @Select("select * from order_comment where id = #{id}")
    OrderComment getById(Long id);
    void deleteBatch(List<Long> ids);
}
