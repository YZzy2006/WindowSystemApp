package com.window;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.window.mapper")
@EnableScheduling
@EnableAsync
public class WindowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WindowApplication.class, args);
    }

}
