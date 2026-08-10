package com.hmall.api.config;

import com.hmall.api.client.fallback.CartClientFallback;
import com.hmall.api.client.fallback.ItemClientFallback;
import com.hmall.api.client.fallback.TradeClientFallback;
import com.hmall.api.client.fallback.UserClientFallback;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 获取用户信息
                Long userId = UserContext.getUser();
                if(userId == null){
                    return;
                }
                template.header("user-info", userId.toString());

            }
        };
    }
    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }
    @Bean
    public CartClientFallback cartClientFallback() {
        return new CartClientFallback();
    }
    @Bean
    public UserClientFallback userClientFallback() {
        return new UserClientFallback();
    }
    @Bean
    public TradeClientFallback tradeClientFallback() {
        return new TradeClientFallback();
    }
}
