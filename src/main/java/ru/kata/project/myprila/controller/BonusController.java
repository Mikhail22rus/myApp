package ru.kata.project.myprila.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.entity.Bonus;
import ru.kata.project.myprila.service.BonusService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bonuses")
@CrossOrigin(origins = "*") // Для фронта на другом порту
public class BonusController {

    private final BonusService bonusService;

    public BonusController(BonusService bonusService) {
        this.bonusService = bonusService;
    }

    // Получить все бонусы пользователя
    @GetMapping
    public ResponseEntity<List<Bonus>> getBonuses(@RequestParam String userId) {
        return ResponseEntity.ok(bonusService.getBonuses(userId));
    }

    // Добавить бонус
    @PostMapping
    public ResponseEntity<Bonus> addBonus(@RequestParam String userId, @RequestBody Bonus bonus) {
        bonus.setUserId(userId);
        return ResponseEntity.ok(bonusService.addBonus(bonus));
    }

    // Удалить бонус
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBonus(@PathVariable Long id) {
        bonusService.deleteBonus(id);
        return ResponseEntity.ok().build();
    }

    // Статистика бонусов
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Double>> getBonusStatistics(@RequestParam String userId) {
        Map<String, Double> stats = new HashMap<>();
        stats.put("totalBonus", bonusService.getTotalBonus(userId));
        return ResponseEntity.ok(stats);
    }
}