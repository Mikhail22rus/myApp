package ru.kata.project.myprila.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "work_days")
public class WorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "description", length = 500)
    private String description;

    // ✅ ИЗМЕНЕНО: salary больше не фиксированная, можно редактировать
    @Column(name = "salary")
    private BigDecimal salary;

    @Column(name = "bonus", precision = 10, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Конструкторы
    public WorkDay() {
    }

    public WorkDay(LocalDate workDate, String description,BigDecimal salary) {
        this.workDate = workDate;
        this.description = description;
        this.salary = salary;
    }

    public WorkDay(LocalDate workDate, String description, BigDecimal salary, BigDecimal bonus) {
        this.workDate = workDate;
        this.description = description;
        this.salary = salary;
        this.bonus = bonus;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }



    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "WorkDay{" +
                "id=" + id +
                ", workDate=" + workDate +
                ", description='" + description + '\'' +
                ", salary=" + salary +
                ", userId=" + (user != null ? user.getId() : "null") +
                '}';
    }
}