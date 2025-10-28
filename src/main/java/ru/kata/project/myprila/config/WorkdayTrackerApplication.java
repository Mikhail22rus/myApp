package ru.kata.project.myprila;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkdayTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkdayTrackerApplication.class, args);
        System.out.println("🚀 Приложение для учета рабочих дней запущено!");
        System.out.println("📊 API доступно по: http://localhost:8082/api");
        System.out.println("🗄️  H2 Console: http://localhost:8082/h2-console");
    }
}