package ru.kata.project.myprila;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workdays")

public class WorkDayController {

    @Autowired
    private WorkDayService workDayService;

    // Получить все рабочие дни
    @GetMapping
    public ResponseEntity<List<WorkDay>> getAllWorkDays() {
        try {
            System.out.println("📥 GET /api/workdays - запрос всех рабочих дней");
            List<WorkDay> workDays = workDayService.getAllWorkDays();
            System.out.println("✅ Найдено рабочих дней: " + workDays.size());
            return ResponseEntity.ok(workDays);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении рабочих дней: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Добавить новый рабочий день
    @PostMapping
    public ResponseEntity<?> createWorkDay(@RequestBody WorkDay workDay) {
        try {
            System.out.println("📥 POST /api/workdays - создание дня: " + workDay.getWorkDate());

            WorkDay savedWorkDay = workDayService.saveWorkDay(workDay);
            return ResponseEntity.ok(savedWorkDay);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при создании рабочего дня: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // Удалить рабочий день
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkDay(@PathVariable Long id) {
        try {
            System.out.println("📥 DELETE /api/workdays/" + id + " - удаление дня");

            workDayService.deleteWorkDay(id);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при удалении: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // Получить статистику
    @GetMapping("/statistics")
    public ResponseEntity<WorkDayService.WorkDayStatistics> getStatistics() {
        try {
            System.out.println("📊 GET /api/workdays/statistics - запрос статистики");
            WorkDayService.WorkDayStatistics statistics = workDayService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении статистики: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Тестовый endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("✅ Тестовый endpoint вызван");
        return ResponseEntity.ok("Бэкенд для учета рабочих дней работает! 🚀");
    }

    // Класс для ошибок
    public static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}