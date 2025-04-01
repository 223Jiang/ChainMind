package com.tencent.supersonic.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author JiangWeiWei
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数（默认CPU核心数）
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        
        // 最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        // 队列容量（等待执行的任务队列）
        executor.setQueueCapacity(200);
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 线程前缀名称（方便日志追踪）
        executor.setThreadNamePrefix("async-task-");
        
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 初始化线程池
        executor.initialize();
        
        return executor;
    }
}