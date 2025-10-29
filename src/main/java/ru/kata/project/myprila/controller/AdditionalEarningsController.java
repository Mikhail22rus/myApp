package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.User;
import ru.kata.project.myprila.repo.UserRepository;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/earnings")
public class AdditionalEarningsController {

    @Autowired
    private UserRepository userRepository;

    // ✅ Добавить допзаработок для Игоря
    @PostMapping("/add")
    public ResponseEntity<?> addEarnings(@RequestBody EarningsRequest request) {
        try {
            // Ищем пользователя Игорь
            User user = userRepository.findByUsername("Игорь");
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Пользователь Игорь не найден"
                ));
            }

            BigDecimal amount = request.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Сумма должна быть положительной"
                ));
            }

            // Добавляем к существующему допзаработку
            BigDecimal current = user.getAdditionalEarnings() != null ? user.getAdditionalEarnings() : BigDecimal.ZERO;
            user.setAdditionalEarnings(current.add(amount));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Допзаработок добавлен",
                    "additionalEarnings", user.getAdditionalEarnings()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка: " + e.getMessage()
            ));
        }
    }

    // ✅ Получить текущий допзаработок Игоря
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentEarnings() {
        try {
            User user = userRepository.findByUsername("Игорь");
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Пользователь Игорь не найден"
                ));
            }

            BigDecimal current = user.getAdditionalEarnings() != null ? user.getAdditionalEarnings() : BigDecimal.ZERO;
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "additionalEarnings", current
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка: " + e.getMessage()
            ));
        }
    }

    // ✅ DTO запроса
    public static class EarningsRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
