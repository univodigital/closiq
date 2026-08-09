package com.closiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClosiqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClosiqApplication.class, args);
    }
}
