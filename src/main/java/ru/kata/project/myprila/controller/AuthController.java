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

    // Пользователи с разными паролями
    private final Map<String, String> users = Map.of(
            "Миша", "пароль123",
            "Игорь", "igor2024"
    );

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String username = request.getUsername();
            String password = request.getPassword();

            // Проверяем существование пользователя и пароль
            if (users.containsKey(username)) {
                if (users.get(username).equals(password)) {

                    // Ищем пользователя в базе
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
                } else {
                    return ResponseEntity.status(401).body(Map.of(
                            "success", false,
                            "message", "Неверный пароль"
                    ));
                }
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

    // Endpoint для создания пользователей (вызвать один раз)
    @PostMapping("/create-users")
    public ResponseEntity<?> createUsers() {
        try {
            // Очищаем старых пользователей
            userRepository.deleteAll();

            // Создаем новых пользователей
            for (Map.Entry<String, String> entry : users.entrySet()) {
                User user = new User();
                user.setUsername(entry.getKey());
                user.setPassword(entry.getValue());
                userRepository.save(user);
                System.out.println("Создан пользователь: " + entry.getKey() + " / " + entry.getValue());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Пользователи созданы: Миша (пароль123), Игорь (igor2024)"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка создания пользователей: " + e.getMessage()
            ));
        }
    }

    // Endpoint для проверки существующих пользователей
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
                    "message", allUsers.size() > 0 ? "Пользователи существуют" : "Пользователи не найдены"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Ошибка проверки: " + e.getMessage()
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