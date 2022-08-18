package ru.matprojects.dbqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DbqueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbqueueApplication.class, args);
    }

}
