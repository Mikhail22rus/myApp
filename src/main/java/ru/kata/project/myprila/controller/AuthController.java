package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.User;
import ru.kata.project.myprila.repo.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Простые тестовые пользователи (логин:пароль)
    private final Map<String, String> testUsers = Map.of(
            "user1", "123",
            "user2", "123"
    );

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String username = request.getUsername();
            String password = request.getPassword();

            // Проверяем в тестовых пользователях
            if (testUsers.containsKey(username) && testUsers.get(username).equals(password)) {

                // Находим или создаем пользователя в базе
                User user = userRepository.findByUsername(username);
                if (user == null) {
                    user = new User();
                    user.setUsername(username);
                    user.setPassword(password); // Сохраняем пароль как есть (без шифрования)
                    user = userRepository.save(user);
                }

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "userId", user.getId(),
                        "username", user.getUsername()
                ));
            }

            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Неверный логин или пароль"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка сервера: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    // Простой endpoint для создания тестовых пользователей
    @PostMapping("/create-test-users")
    public ResponseEntity<?> createTestUsers() {
        try {
            for (Map.Entry<String, String> entry : testUsers.entrySet()) {
                if (userRepository.findByUsername(entry.getKey()) == null) {
                    User user = new User();
                    user.setUsername(entry.getKey());
                    user.setPassword(entry.getValue());
                    userRepository.save(user);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Тестовые пользователи созданы"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка создания пользователей: " + e.getMessage()
            ));
        }
    }

    public static class LoginRequest {
        private String username;
        private String password;

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