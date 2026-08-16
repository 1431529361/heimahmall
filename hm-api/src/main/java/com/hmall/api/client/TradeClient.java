package com.hmall.api.client;

import com.hmall.api.client.fallback.TradeClientFallback;
import com.hmall.api.dto.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(value = "trade-service",fallbackFactory = TradeClientFallback.class)
public interface TradeClient {
    @PutMapping("/orders/{orderId}")
    void markOrderPaySuccess(@PathVariable("orderId") Long orderId);
    @GetMapping("/orders/{id}")
    OrderVO queryOrderById(@PathVariable("id") Long orderId);
}
