package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.SalaryPayment;
import ru.kata.project.myprila.service.WorkDayService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class SalaryPaymentController {

    @Autowired
    private WorkDayService workDayService;

    // ✅ Получить выплаты ПОЛЬЗОВАТЕЛЯ
    @GetMapping
    public ResponseEntity<List<SalaryPayment>> getUserPayments(@RequestParam Long userId) {
        try {
            System.out.println("💰 GET /api/payments - запрос выплат пользователя ID: " + userId);
            List<SalaryPayment> payments = workDayService.getUserSalaryPayments(userId);
            System.out.println("✅ Найдено выплат: " + payments.size());
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении выплат: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Добавить выплату ДЛЯ ПОЛЬЗОВАТЕЛЯ
    @PostMapping
    public ResponseEntity<?> addPayment(@RequestBody PaymentRequest request, @RequestParam Long userId) {
        try {
            System.out.println("💰 POST /api/payments - добавление выплаты для пользователя ID: " + userId + ", сумма: " + request.getAmount());

            // Валидация
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Сумма должна быть положительной"));
            }

            SalaryPayment payment = workDayService.addSalaryPayment(
                    request.getAmount(),
                    request.getDescription(),
                    userId
            );

            // Получаем обновленную статистику
            BigDecimal balance = workDayService.getSalaryBalance(userId);
            System.out.println("✅ Выплата добавлена. Остаток долга: " + balance);

            PaymentResponse response = new PaymentResponse(payment, balance);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при добавлении выплаты: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка при добавлении выплаты: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ Удалить выплату ПОЛЬЗОВАТЕЛЯ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id, @RequestParam Long userId) {
        try {
            System.out.println("💰 DELETE /api/payments/" + id + " - удаление выплаты для пользователя ID: " + userId);

            workDayService.deleteSalaryPayment(id, userId);

            // Получаем обновленную статистику после удаления
            BigDecimal balance = workDayService.getSalaryBalance(userId);
            System.out.println("✅ Выплата удалена. Остаток долга: " + balance);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            System.out.println("❌ Ошибка при удалении выплаты: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Неизвестная ошибка при удалении выплаты: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Внутренняя ошибка сервера"));
        }
    }

    // ✅ Получить статистику по выплатам ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/statistics")
    public ResponseEntity<PaymentStatistics> getPaymentStatistics(@RequestParam Long userId) {
        try {
            System.out.println("💰 GET /api/payments/statistics - запрос статистики выплат пользователя ID: " + userId);

            WorkDayService.WorkDayStatistics statistics = workDayService.getStatistics(userId);
            List<SalaryPayment> payments = workDayService.getUserSalaryPayments(userId);

            PaymentStatistics paymentStats = new PaymentStatistics(
                    statistics.getTotalEarned(),
                    statistics.getTotalPaid(),
                    statistics.getSalaryBalance(),
                    payments.size()
            );

            return ResponseEntity.ok(paymentStats);

        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении статистики выплат: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Получить баланс ПОЛЬЗОВАТЕЛЯ
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getSalaryBalance(@RequestParam Long userId) {
        try {
            System.out.println("💰 GET /api/payments/balance - запрос баланса пользователя ID: " + userId);
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
        System.out.println("✅ SalaryPaymentController тестовый endpoint вызван");
        return ResponseEntity.ok("Контроллер выплат работает! 💰");
    }

    // DTO классы

    public static class PaymentRequest {
        private BigDecimal amount;
        private String description;

        // Геттеры и сеттеры
        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class PaymentResponse {
        private SalaryPayment payment;
        private BigDecimal currentBalance;

        public PaymentResponse(SalaryPayment payment, BigDecimal currentBalance) {
            this.payment = payment;
            this.currentBalance = currentBalance;
        }

        // Геттеры
        public SalaryPayment getPayment() {
            return payment;
        }

        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }
    }

    public static class PaymentStatistics {
        private final BigDecimal totalEarned;
        private final BigDecimal totalPaid;
        private final BigDecimal currentBalance;
        private final int totalPayments;

        public PaymentStatistics(BigDecimal totalEarned, BigDecimal totalPaid,
                                 BigDecimal currentBalance, int totalPayments) {
            this.totalEarned = totalEarned;
            this.totalPaid = totalPaid;
            this.currentBalance = currentBalance;
            this.totalPayments = totalPayments;
        }

        // Геттеры
        public BigDecimal getTotalEarned() {
            return totalEarned;
        }

        public BigDecimal getTotalPaid() {
            return totalPaid;
        }

        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }

        public int getTotalPayments() {
            return totalPayments;
        }
    }

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