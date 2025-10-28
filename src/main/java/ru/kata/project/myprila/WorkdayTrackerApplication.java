package ru.kata.project.myprila;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.kata.project.myprila.repo")
@EntityScan(basePackages = "ru.kata.project.myprila.entity")
public class WorkdayTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkdayTrackerApplication.class, args);
        System.out.println("🚀 Приложение для учета рабочих дней запущено!");


    }
}