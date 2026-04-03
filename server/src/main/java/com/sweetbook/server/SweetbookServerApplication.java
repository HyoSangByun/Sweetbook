package com.sweetbook.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SweetbookServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SweetbookServerApplication.class, args);
    }
}

