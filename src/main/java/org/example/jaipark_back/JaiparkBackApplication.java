package org.example.jaipark_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JaiparkBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(JaiparkBackApplication.class, args);
    }

}
