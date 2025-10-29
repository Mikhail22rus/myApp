package ru.kata.project.myprila.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.kata.project.myprila.entity.Bonus;

import java.util.List;

@Repository
public interface BonusRepository extends JpaRepository<Bonus, Long> {
    List<Bonus> findByUserIdOrderByDateDesc(String userId);
}