package com.sky.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.sky.config.RabbitMQConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class OrderMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单延迟消息
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    public void sendOrderDelayMessage(Long orderId, Long userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderId", orderId);
        message.put("userId", userId);
        message.put("createTime", System.currentTimeMillis());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfiguration.ORDER_DELAY_EXCHANGE,
            RabbitMQConfiguration.ORDER_DELAY_ROUTING_KEY,
            JSON.toJSONString(message)
        );
        log.info("发送订单延迟消息：orderId={}", orderId);
    }
}
