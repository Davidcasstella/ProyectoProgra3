package com.Zygo.proyecto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true) 
public class AsyncConfig implements AsyncConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("ZygoAsync-");
        executor.initialize();
        log.info("✅ ThreadPoolTaskExecutor configurado correctamente");
        return executor;
    }
    
    // 🆕 AGREGAR: Manejador de excepciones asíncronas
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("❌ ERROR ASÍNCRONO en método: {}", method.getName());
                log.error("❌ Mensaje: {}", ex.getMessage());
                log.error("❌ Stack trace:", ex);
                log.error("❌ Parámetros: {}", params);
            }
        };
    }
}