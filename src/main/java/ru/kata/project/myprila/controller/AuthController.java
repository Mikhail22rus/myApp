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


    private final Map<String, String> users = Map.of(
            "Миша", "пароль123",
            "Игорь", "igor2024",
            "test", "1"
    );


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
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
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка сервера: " + e.getMessage()
            ));
        }
    }

    // Создание пользователей (один раз)
    @PostMapping("/create-users")
    public ResponseEntity<?> createUsers() {
        try {
            int createdCount = 0;

            for (Map.Entry<String, String> entry : users.entrySet()) {
                String username = entry.getKey();

                // Создаем только если пользователя нет
                if (userRepository.findByUsername(username) == null) {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(entry.getValue());
                    userRepository.save(user);
                    createdCount++;
                    System.out.println("Создан пользователь: " + username + " / " + entry.getValue());
                }
            }

            String message = createdCount > 0
                    ? "Создано пользователей: " + createdCount
                    : "Все пользователи уже существуют";

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка создания пользователей: " + e.getMessage()
            ));
        }
    }

    // Проверка пользователей
    @GetMapping("/check-users")
    public ResponseEntity<?> checkUsers() {
        try {
            var allUsers = userRepository.findAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userCount", allUsers.size(),
                    "users", allUsers.stream()
                            .map(u -> Map.of("id", u.getId(), "username", u.getUsername()))
                            .toList(),
                    "message", allUsers.isEmpty() ? "Пользователи не найдены" : "Пользователи существуют"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка проверки: " + e.getMessage()
            ));
        }
    }

    // DTO для логина
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
