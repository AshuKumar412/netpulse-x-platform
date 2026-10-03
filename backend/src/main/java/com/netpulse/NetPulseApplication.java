package com.netpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NetPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetPulseApplication.class, args);
    }
}
