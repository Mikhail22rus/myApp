package ru.kata.project.myprila.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_payments")
public class SalaryPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime paymentDate; // ОСТАВЛЯЕМ LocalDateTime

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Конструкторы
    public SalaryPayment() {
    }

    public SalaryPayment(BigDecimal amount, String description, User user) {
        this.amount = amount;
        this.description = description;
        this.paymentDate = LocalDateTime.now();
        this.user = user;
    }

    // Конструктор с датой и временем
    public SalaryPayment(BigDecimal amount, String description, LocalDateTime paymentDate, User user) {
        this.amount = amount;
        this.description = description;
        this.paymentDate = paymentDate;
        this.user = user;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}