package com.moa2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = { RedisAutoConfiguration.class })
public class Moa2Application {

    public static void main(String[] args) {
        SpringApplication.run(Moa2Application.class, args);
    }

}
