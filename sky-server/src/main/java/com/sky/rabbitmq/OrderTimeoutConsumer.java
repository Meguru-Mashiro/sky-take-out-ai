package com.sky.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.sky.config.RabbitMQConfiguration;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
public class OrderTimeoutConsumer {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 监听订单取消队列
     */
    @RabbitListener(queues = RabbitMQConfiguration.ORDER_CANCEL_QUEUE)
    public void handleOrderTimeout(String messageJson, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 1. 解析消息
            Map<String, Object> messageMap = JSON.parseObject(messageJson, Map.class);
            Long orderId = Long.valueOf(messageMap.get("orderId").toString());
            
            // 2. 查询订单当前状态
            Orders orders = orderMapper.getById(orderId);
            
            // 3. 只有待支付状态的订单才取消
            if (orders != null && orders.getStatus().equals(Orders.PENDING_PAYMENT)) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时未支付，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
                log.info("订单超时取消成功：orderId={}", orderId);
            } else {
                log.info("订单状态已变更，无需取消：orderId={}, status={}", 
                        orderId, orders != null ? orders.getStatus() : "订单不存在");
            }
            
            // 4. 手动确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("处理订单超时消息失败：{}", e.getMessage());
            // 拒绝消息，不重新入队
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
