package com.hmall.trade.listener;

import com.hmall.api.client.UserClient;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class PayStatusListener {
    private final IOrderService orderService;
    private final UserClient userClient;


    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "trade.pay.success.queue", durable = "true"),
            exchange = @Exchange(name = "pay.direct"),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            log.warn("支付回调：订单{}不存在", orderId);
            return;
        }
        if (order.getStatus() == 5) {
            // 订单已被取消但支付成功，需要退款
            log.warn("支付回调：订单{}已取消，执行退款，金额：{}", orderId, order.getTotalFee());
            userClient.refundMoney(order.getTotalFee());
        } else if (order.getStatus() == 1) {
            // 正常情况：标记为已支付
            orderService.markOrderPaySuccess(orderId);
            log.info("支付回调：订单{}标记为已支付", orderId);
        } else {
            log.info("支付回调：订单{}状态为{}，无需处理", orderId, order.getStatus());
        }

    }
}
