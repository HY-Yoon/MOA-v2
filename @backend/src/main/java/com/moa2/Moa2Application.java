package com.moa2;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

import java.util.TimeZone;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class Moa2Application {

    public static void main(String[] args) {
        SpringApplication.run(Moa2Application.class, args);
    }

}
