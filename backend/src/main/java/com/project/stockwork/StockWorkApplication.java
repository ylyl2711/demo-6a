package com.project.stockwork;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.project.stockwork.mapper")
public class StockWorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockWorkApplication.class, args);
    }
}
