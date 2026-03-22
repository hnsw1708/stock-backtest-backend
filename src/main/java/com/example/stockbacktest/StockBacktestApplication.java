package com.example.stockbacktest;

import com.example.stockbacktest.service.FileStorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.Resource;

@SpringBootApplication
public class StockBacktestApplication implements CommandLineRunner {
    
    @Resource
    FileStorageService fileStorageService;
    
    public static void main(String[] args) {
        SpringApplication.run(StockBacktestApplication.class, args);
    }
    
    @Override
    public void run(String... arg) throws Exception {
        fileStorageService.init();
        System.out.println("股票回溯策略系统后端服务已启动！");
        System.out.println("访问地址: http://localhost:8080");
    }
}