package ru.kata.project.myprila;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")

public class SalaryPaymentController {

    @Autowired
    private WorkDayService workDayService;

    // Получить все выплаты
    @GetMapping
    public ResponseEntity<List<SalaryPayment>> getAllPayments() {
        try {
            System.out.println("💰 GET /api/payments - запрос всех выплат");
            List<SalaryPayment> payments = workDayService.getAllSalaryPayments();
            System.out.println("✅ Найдено выплат: " + payments.size());
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении выплат: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Добавить новую выплату
    @PostMapping
    public ResponseEntity<?> addPayment(@RequestBody PaymentRequest request) {
        try {
            System.out.println("💰 POST /api/payments - добавление выплаты: " + request.getAmount());

            // Валидация
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Сумма должна быть положительной"));
            }

            SalaryPayment payment = workDayService.addSalaryPayment(
                    request.getAmount(),
                    request.getDescription()
            );

            // Получаем обновленную статистику
            BigDecimal balance = workDayService.getSalaryBalance();
            System.out.println("✅ Выплата добавлена. Остаток долга: " + balance);

            PaymentResponse response = new PaymentResponse(payment, balance);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ Ошибка при добавлении выплаты: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Ошибка при добавлении выплаты"));
        }
    }

    // Удалить выплату
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id) {
        try {
            System.out.println("💰 DELETE /api/payments/" + id + " - удаление выплаты");

            workDayService.deleteSalaryPayment(id);

            // Получаем обновленную статистику после удаления
            BigDecimal balance = workDayService.getSalaryBalance();
            System.out.println("✅ Выплата удалена. Остаток долга: " + balance);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.out.println("❌ Ошибка при удалении выплаты: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new ErrorResponse("Ошибка при удалении выплаты"));
        }
    }

    // Получить статистику по выплатам
    @GetMapping("/statistics")
    public ResponseEntity<PaymentStatistics> getPaymentStatistics() {
        try {
            System.out.println("💰 GET /api/payments/statistics - запрос статистики выплат");

            BigDecimal totalEarned = workDayService.getTotalEarned();
            BigDecimal totalPaid = workDayService.getTotalPaid();
            BigDecimal balance = workDayService.getSalaryBalance();
            int totalPayments = workDayService.getAllSalaryPayments().size();

            PaymentStatistics statistics = new PaymentStatistics(totalEarned, totalPaid, balance, totalPayments);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении статистики выплат: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Тестовый endpoint
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