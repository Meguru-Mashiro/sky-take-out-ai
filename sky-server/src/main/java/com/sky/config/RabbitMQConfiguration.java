package com.sky.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfiguration {

    // 延迟交换机（订单创建后消息先到这里）
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    // 死信交换机（消息超时后转发到这里）
    public static final String ORDER_CANCEL_EXCHANGE = "order.cancel.exchange";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";

    @Value("${sky.rabbitmq.order-timeout}")
    private Long orderTimeout;

    // 1. 创建延迟交换机
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    // 2. 创建死信交换机
    @Bean
    public DirectExchange orderCancelExchange() {
        return new DirectExchange(ORDER_CANCEL_EXCHANGE, true, false);
    }

    // 3. 创建延迟队列（设置死信转发规则）
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 消息过期后转发到死信交换机
        args.put("x-dead-letter-exchange", ORDER_CANCEL_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_CANCEL_ROUTING_KEY);
        // 消息存活时间 15分钟
        args.put("x-message-ttl", orderTimeout);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    // 4. 创建死信队列（实际处理取消的队列）
    @Bean
    public Queue orderCancelQueue() {
        return new Queue(ORDER_CANCEL_QUEUE, true, false, false);
    }

    // 5. 绑定延迟队列到延迟交换机
    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    // 6. 绑定死信队列到死信交换机
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderCancelExchange())
                .with(ORDER_CANCEL_ROUTING_KEY);
    }
}
