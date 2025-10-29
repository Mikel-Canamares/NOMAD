package com.nomad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NomadApplication {

    public static void main(String[] args) {
        SpringApplication.run(NomadApplication.class, args);
    }

}
