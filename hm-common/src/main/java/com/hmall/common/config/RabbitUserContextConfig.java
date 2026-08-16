package com.hmall.common.config;

import com.hmall.common.utils.RabbitMqHelper;
import com.hmall.common.utils.UserContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;

/**
 * RabbitMQ 用户上下文透传配置：
 * 1. 发送端：发送消息前自动把 UserContext 中的 userId 写入消息头
 * 2. 接收端：消费消息前自动从消息头恢复 UserContext，消费后清理
 * 3. 当 MqConsumeErrorAutoConfiguration 启用时，自动将重试拦截器加入 advice chain
 * 业务代码无感知，统一使用 UserContext.getUser() 获取登录用户
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitUserContextConfig {

    private static final String USER_ID_HEADER = "userId";

    /**
     * 发送端：自定义 RabbitTemplate，发送前自动把 userId 写入消息头
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         ObjectProvider<MessageConverter> converterProvider) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 复用各服务已定义的 MessageConverter（如 Jackson2JsonMessageConverter）
        MessageConverter converter = converterProvider.getIfUnique();
        if (converter != null) {
            rabbitTemplate.setMessageConverter(converter);
        }
        // 发送前拦截：将当前登录用户 id 写入消息头
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            Long userId = UserContext.getUser();
            if (userId != null) {
                message.getMessageProperties().setHeader(USER_ID_HEADER, userId);
            }
            return message;
        });
        return rabbitTemplate;
    }

    /**
     * RabbitMQ 消息发送工具类
     */
    @Bean
    public RabbitMqHelper rabbitMqHelper(RabbitTemplate rabbitTemplate) {
        return new RabbitMqHelper(rabbitTemplate);
    }

    /**
     * 接收端：自定义监听容器工厂，消费前从消息头恢复 UserContext，消费后清理
     * 当存在 MessageRecoverer 时（MqConsumeErrorAutoConfiguration 启用），
     * 自动将重试拦截器加入 advice chain，实现消费失败消息转投错误队列
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            ObjectProvider<MessageConverter> converterProvider,
            ObjectProvider<MessageRecoverer> recovererProvider) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 复用各服务已定义的 MessageConverter
        MessageConverter converter = converterProvider.getIfUnique();
        if (converter != null) {
            factory.setMessageConverter(converter);
        }
        // 构建 advice chain：用户上下文拦截 + 重试拦截（如有）
        MessageRecoverer recoverer = recovererProvider.getIfAvailable();
        if (recoverer != null) {
            StatefulRetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateful()
                    .recoverer(recoverer)
                    .build();
            factory.setAdviceChain(userContextAdvice(), retryInterceptor);
        } else {
            factory.setAdviceChain(userContextAdvice());
        }
        return factory;
    }

    private MethodInterceptor userContextAdvice() {
        return invocation -> {
            // 从拦截参数中找到原始 Message
            Message message = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Message) {
                    message = (Message) arg;
                    break;
                }
            }
            // 从消息头恢复用户上下文
            if (message != null) {
                Object userId = message.getMessageProperties().getHeader(USER_ID_HEADER);
                if (userId instanceof Number) {
                    UserContext.setUser(((Number) userId).longValue());
                }
            }
            try {
                return invocation.proceed();
            } finally {
                // 消费线程是池化复用的，必须清理 ThreadLocal，防止上下文污染
                UserContext.removeUser();
            }
        };
    }
}
