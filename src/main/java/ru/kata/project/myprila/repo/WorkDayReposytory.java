package ru.kata.project.myprila.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.kata.project.myprila.entity.WorkDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkDayReposytory extends JpaRepository<WorkDay, Long> {

    // Найти рабочий день по дате
    Optional<WorkDay> findByWorkDate(LocalDate workDate);

    // Проверить существование дня по дате
    boolean existsByWorkDate(LocalDate workDate);

    // Получить все дни отсортированные по дате (сначала новые)
    List<WorkDay> findAllByOrderByWorkDateDesc();

    // Получить дни за определенный период
    List<WorkDay> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);
}