package ru.kata.project.myprila.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@RestController
public class TempFixController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/api/fix-db")
    public String fixDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("ALTER TABLE work_days DROP CONSTRAINT IF EXISTS uk_glj7caeg93637tvsu7cf03gkk");
            return "✅ Ограничение удалено! Теперь можно создавать рабочие дни с одинаковыми датами.";

        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }
}