package com.moa2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableRedisRepositories(basePackages = "com.moa2.api.auth.domain.repository")
@SpringBootApplication(exclude = { RedisAutoConfiguration.class })
public class Moa2Application {

    public static void main(String[] args) {
        SpringApplication.run(Moa2Application.class, args);

//        ConfigurableApplicationContext context = SpringApplication.run(Moa2Application.class, args);
//        Environment env = context.getBean(Environment.class);
//
//        System.out.println("=======================================");
//        System.out.println("현재 연결된 DB URL: " + env.getProperty("spring.datasource.url"));
//        System.out.println("현재 연결된 DB USER: " + env.getProperty("spring.datasource.username"));
//        System.out.println("=======================================");
    }

}
