package com.example.priutbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriutBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriutBotApplication.class, args);
    }
}
