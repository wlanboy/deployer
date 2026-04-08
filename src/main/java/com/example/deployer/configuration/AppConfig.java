package com.example.deployer.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean(destroyMethod = "shutdown")
    ExecutorService playbookExecutorService() {
        return Executors.newCachedThreadPool();
    }
}
