package ru.kata.project.myprila.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kata.project.myprila.entity.SalaryPayment;

import java.util.List;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {
    List<SalaryPayment> findAllByOrderByPaymentDateDesc();
}