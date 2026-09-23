package com.seolytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SeolyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeolyticsApplication.class, args);
    }
}
