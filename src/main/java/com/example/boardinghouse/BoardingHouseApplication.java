package com.example.boardinghouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoardingHouseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardingHouseApplication.class, args);
    }
}
