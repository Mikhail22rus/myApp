package ru.kata.project.myprila.entity;


import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "work_days")
public class WorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_date", nullable = false, unique = true)
    private LocalDate workDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "salary", nullable = false)
    private Integer salary = 3500; // Фиксированная ставка 3500 руб/день

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    // Конструкторы
    public WorkDay() {}

    public WorkDay(LocalDate workDate, String description) {
        this.workDate = workDate;
        this.description = description;
        this.salary = 3500;
    }

    public WorkDay(LocalDate workDate, String description, Integer salary) {
        this.workDate = workDate;
        this.description = description;
        this.salary = salary;
    }

    public void setUser(User user) {
        this.user = user;
    }
    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSalary() { return salary; }
    public void setSalary(Integer salary) { this.salary = salary; }

    @Override
    public String toString() {
        return "WorkDay{" +
                "id=" + id +
                ", workDate=" + workDate +
                ", description='" + description + '\'' +
                ", salary=" + salary +
                '}';
    }
}