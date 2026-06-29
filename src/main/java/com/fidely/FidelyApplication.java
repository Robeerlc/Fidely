package com.fidely;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FidelyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FidelyApplication.class, args);
    }

}
