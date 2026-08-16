package com.hmall.api.client.fallback;

import com.hmall.api.client.TradeClient;
import com.hmall.api.dto.OrderVO;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
@Slf4j
public class TradeClientFallback implements FallbackFactory<TradeClient> {
    @Override
    public TradeClient create(Throwable cause) {
        return new TradeClient() {
            @Override
            public void markOrderPaySuccess(Long orderId) {
                log.error("远程调用TradeClient#markOrderPaySuccess方法出现异常，订单号：{}",orderId,cause);
                throw new BizIllegalException(cause);
            }

            @Override
            public OrderVO queryOrderById(Long orderId) {
                log.error("远程调用TradeClient#queryOrderById方法出现异常，订单号：{}", orderId, cause);
                return null;
            }
        };
    }
}
