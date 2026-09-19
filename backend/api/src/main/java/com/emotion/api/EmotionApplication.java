package com.emotion.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({"com.emotion.api.repository.dao", "com.emotion.api.repository.mapper", "com.emotion.api.mapper"})
@EnableScheduling
@EnableAsync
public class EmotionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmotionApplication.class, args);
    }
}
