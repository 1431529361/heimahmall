package com.hmall.api.client;

import com.hmall.api.client.fallback.PayClientFallback;
import com.hmall.api.dto.PayOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "pay-service", fallbackFactory = PayClientFallback.class)
public interface PayClient {
    @GetMapping("/pay-orders/{id}")
    PayOrderDTO queryPayOrderById(@PathVariable("id") Long id);
    @PostMapping("/pay-orders/{id}/close")
    void closePayOrder(@PathVariable("id") Long id);

}
