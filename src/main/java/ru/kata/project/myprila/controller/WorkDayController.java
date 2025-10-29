package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.service.WorkDayService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/workdays")
public class WorkDayController {

    @Autowired
    private WorkDayService workDayService;

    // ✅ Получить рабочие дни ПОЛЬЗОВАТЕЛЯ
    @GetMapping
    public ResponseEntity<List<WorkDay>> getUserWorkDays(@RequestParam Long userId) {
        try {
            System.out.println("📥 GET /api/workdays - запрос рабочих дней пользователя ID: " + userId);
            List<WorkDay> workDays = workDayService.getUserWorkDays(userId);
            System.out.println("✅ Найдено рабочих дней: " + workDays.size());
            return ResponseEntity.ok(workDays);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении рабочих дней: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Добавить новый рабочий день ДЛЯ ПОЛЬЗОВАТЕЛЯ
    @PostMapping
    public ResponseEntity<?> createWorkDay(@RequestBody WorkDay workDay, @RequestParam Long userId) {
        try {
            System.out.println("📥 POST /api/workdays - создание дня для пользователя ID: " + userId + ", дата: " + workDay.getWorkDate());

            WorkDay savedWorkDay = workDayService.createWorkDay(workDay, userId);
            return ResponseEntity.ok(savedWorkDay);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при создании рабочего дня: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ Обновить рабочий день
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkDay(@PathVariable Long id, @RequestBody WorkDay workDay, @RequestParam Long userId) {
        try {
            System.out.println("📥 PUT /api/workdays/" + id + " - обновление дня для пользователя ID: " + userId);

            WorkDay updatedWorkDay = workDayService.updateWorkDay(id, workDay, userId);
            return ResponseEntity.ok(updatedWorkDay);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при обновлении рабочего дня: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ Удалить рабочий день ПОЛЬЗОВАТЕЛЯ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkDay(@PathVariable Long id, @RequestParam Long userId) {
        try {
            System.out.println("📥 DELETE /api/workdays/" + id + " - удаление дня для пользователя ID: " + userId);

            workDayService.deleteWorkDay(id, userId);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при удалении: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ Получить статистику ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/statistics")
    public ResponseEntity<WorkDayService.WorkDayStatistics> getStatistics(@RequestParam Long userId) {
        try {
            System.out.println("📊 GET /api/workdays/statistics - запрос статистики пользователя ID: " + userId);
            WorkDayService.WorkDayStatistics statistics = workDayService.getStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении статистики: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Получить баланс ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getSalaryBalance(@RequestParam Long userId) {
        try {
            System.out.println("💰 GET /api/workdays/balance - запрос баланса пользователя ID: " + userId);
            BigDecimal balance = workDayService.getSalaryBalance(userId);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении баланса: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Тестовый endpoint
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