package ru.kata.project.myprila;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {
    List<SalaryPayment> findAllByOrderByPaymentDateDesc();
}