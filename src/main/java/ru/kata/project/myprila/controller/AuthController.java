package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.User;
import ru.kata.project.myprila.repo.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Словарь тестовых пользователей
    private final Map<String, String> users = Map.of(
            "Миша", "пароль123",
            "Игорь", "igor2024",
            "test", "1"
    );

    @PostMapping("/create-users")
    public ResponseEntity<?> createUsers() {
        try {
            int createdCount = 0;
            for (Map.Entry<String, String> entry : users.entrySet()) {
                String username = entry.getKey();
                if (userRepository.findByUsername(username) == null) {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(entry.getValue());
                    userRepository.save(user);
                    createdCount++;
                    System.out.println("Создан пользователь: " + username + " / " + entry.getValue());
                }
            }
            String message = createdCount > 0 ? "Создано пользователей: " + createdCount : "Все пользователи уже существуют";
            return ResponseEntity.ok(Map.of("success", true, "message", message));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Ошибка создания пользователей: " + e.getMessage()));
        }
    }

    // ❗ Тестовая переменная для хранения залогиненного пользователя
    private User currentUser = null;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (users.containsKey(username)) {
            if (!users.get(username).equals(password)) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Неверный пароль"
                ));
            }

            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Пользователь не найден в системе"
                ));
            }

            // ❗ Устанавливаем текущего пользователя
            currentUser = user;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));
        }

        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Пользователь не найден"
        ));
    }

    // ✅ Возвращает текущего пользователя
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser() {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Сначала войдите в систему"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", currentUser.getId(),
                "username", currentUser.getUsername()
        ));
    }

    // Остальные endpoints (create-users, check-users) оставляем как есть

    // DTO для логина
    public static class LoginRequest {
        private String username;
        private String password;

        // геттеры и сеттеры
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
