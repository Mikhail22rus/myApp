package ru.kata.project.myprila.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kata.project.myprila.entity.SalaryPayment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {
    List<SalaryPayment> findAllByOrderByPaymentDateDesc();
    List<SalaryPayment> findByUserIdOrderByPaymentDateDesc(Long userId);
    List<SalaryPayment> findByUserId(Long userId);
    Optional<SalaryPayment> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
    List<SalaryPayment> findByUserIdAndPaymentDateBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}