package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.service.WorkDayService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    // ✅ ОСНОВНОЙ МЕТОД: создать или обновить рабочий день (зарплата + бонус)
    @PostMapping
    public ResponseEntity<?> addOrUpdateWorkDay(@RequestBody WorkDayRequest request, @RequestParam Long userId) {
        try {
            System.out.println("📥 POST /api/workdays - создание/обновление дня для пользователя ID: " + userId +
                    ", дата: " + request.getWorkDate() +
                    ", зарплата: " + request.getSalary() +
                    ", бонус: " + request.getBonus());

            WorkDay workDay = workDayService.addOrUpdateWorkDay(
                    request.getWorkDate(),
                    request.getDescription(),
                    request.getSalary(),
                    request.getBonus(),
                    userId
            );

            System.out.println("✅ День успешно сохранен ID: " + workDay.getId());
            return ResponseEntity.ok(workDay);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при создании рабочего дня: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ ДОБАВИТЬ ТОЛЬКО ЗАРАБОТОК
    @PostMapping("/salary")
    public ResponseEntity<?> addSalaryOnly(@RequestBody SalaryRequest request, @RequestParam Long userId) {
        try {
            System.out.println("💰 POST /api/workdays/salary - добавление зарплаты для пользователя ID: " + userId +
                    ", дата: " + request.getWorkDate() +
                    ", сумма: " + request.getSalary());

            WorkDay workDay = workDayService.addSalaryOnly(
                    request.getWorkDate(),
                    request.getSalary(),
                    request.getDescription(),
                    userId
            );

            System.out.println("✅ Зарплата успешно добавлена ID: " + workDay.getId());
            return ResponseEntity.ok(workDay);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при добавлении зарплаты: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ ДОБАВИТЬ ТОЛЬКО БОНУС
    @PostMapping("/bonus")
    public ResponseEntity<?> addBonusOnly(@RequestBody BonusRequest request, @RequestParam Long userId) {
        try {
            System.out.println("🎁 POST /api/workdays/bonus - добавление бонуса для пользователя ID: " + userId +
                    ", дата: " + request.getWorkDate() +
                    ", сумма: " + request.getBonus());

            WorkDay workDay = workDayService.addBonusOnly(
                    request.getWorkDate(),
                    request.getBonus(),
                    request.getDescription(),
                    userId
            );

            System.out.println("✅ Бонус успешно добавлен ID: " + workDay.getId());
            return ResponseEntity.ok(workDay);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при добавлении бонуса: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ ПОЛУЧИТЬ ДЕНЬ ПО ДАТЕ
    @GetMapping("/by-date")
    public ResponseEntity<?> getWorkDayByDate(@RequestParam LocalDate workDate, @RequestParam Long userId) {
        try {
            System.out.println("📅 GET /api/workdays/by-date - запрос дня по дате: " + workDate + " для пользователя ID: " + userId);
            Optional<WorkDay> workDay = workDayService.getWorkDayByDate(workDate, userId);

            if (workDay.isPresent()) {
                System.out.println("✅ Найден день: " + workDay.get());
            } else {
                System.out.println("ℹ️ День не найден");
            }

            return ResponseEntity.ok(workDay.orElse(null));

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при поиске дня: " + e.getMessage());
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

    // DTO КЛАССЫ

    public static class WorkDayRequest {
        private LocalDate workDate;
        private String description;
        private BigDecimal salary;
        private BigDecimal bonus;

        public LocalDate getWorkDate() { return workDate; }
        public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getSalary() { return salary; }
        public void setSalary(BigDecimal salary) { this.salary = salary; }
        public BigDecimal getBonus() { return bonus; }
        public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    }

    public static class SalaryRequest {
        private LocalDate workDate;
        private BigDecimal salary;
        private String description;

        public LocalDate getWorkDate() { return workDate; }
        public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
        public BigDecimal getSalary() { return salary; }
        public void setSalary(BigDecimal salary) { this.salary = salary; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class BonusRequest {
        private LocalDate workDate;
        private BigDecimal bonus;
        private String description;

        public LocalDate getWorkDate() { return workDate; }
        public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
        public BigDecimal getBonus() { return bonus; }
        public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class ErrorResponse {
        private final String message;
        public ErrorResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
}