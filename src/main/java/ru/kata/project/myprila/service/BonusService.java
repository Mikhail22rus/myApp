package ru.kata.project.myprila.service;

import org.springframework.stereotype.Service;
import ru.kata.project.myprila.entity.Bonus;
import ru.kata.project.myprila.repo.BonusRepository;

import java.util.List;

@Service
public class BonusService {

    private final BonusRepository bonusRepository;

    public BonusService(BonusRepository bonusRepository) {
        this.bonusRepository = bonusRepository;
    }

    public List<Bonus> getBonuses(String userId) {
        return bonusRepository.findByUserIdOrderByDateDesc(userId);
    }

    public Bonus addBonus(Bonus bonus) {
        return bonusRepository.save(bonus);
    }

    public void deleteBonus(Long id) {
        bonusRepository.deleteById(id);
    }

    public double getTotalBonus(String userId) {
        return bonusRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .mapToDouble(Bonus::getAmount)
                .sum();
    }
}