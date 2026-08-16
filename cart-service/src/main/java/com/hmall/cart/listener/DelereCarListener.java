package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
@Slf4j
@Component
@RequiredArgsConstructor
public class DelereCarListener {
    private final ICartService cartService;
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "cart.clear.queue", durable = "true"),
            exchange = @Exchange(value = "trade.topic", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = "order.create"
    ))
    public void deleteCart(@Payload Set<Long> ids) {
        cartService.removeByItemIds(ids);
        log.info("删除购物车消息接收成功：ids={}, userId={}", ids, UserContext.getUser());
    }
}
