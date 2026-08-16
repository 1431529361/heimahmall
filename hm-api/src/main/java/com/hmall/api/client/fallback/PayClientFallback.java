package com.hmall.api.client.fallback;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
@Slf4j
public class PayClientFallback implements FallbackFactory<PayClient> {
    @Override
    public PayClient create(Throwable cause) {
        return new PayClient() {
            @Override
            public PayOrderDTO queryPayOrderById(Long id) {
                log.error("远程调用PayClient#queryPayOrderById方法出现异常，参数：{}", id, cause);
                throw new RuntimeException("查询支付订单失败，稍后重试", cause);
            }

            @Override
            public void closePayOrder(Long id) {
                log.error("远程调用PayClient#closePayOrder方法出现异常，参数：{}", id, cause);
                throw new RuntimeException("关闭支付单失败，稍后重试", cause);
            }
        };
    }
}
