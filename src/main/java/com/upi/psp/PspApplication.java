package com.upi.psp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PspApplication {
    public static void main(String[] args) {
        SpringApplication.run(PspApplication.class, args);
    }
}
