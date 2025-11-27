// DailyReportDTO.java
package ru.kata.project.myprila.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyReportDTO {
    private Long id;
    private LocalDate workDate;
    private String description;
    private BigDecimal salary;
    private BigDecimal bonus;

    public DailyReportDTO() {}

    public DailyReportDTO(Long id, LocalDate workDate, String description, BigDecimal salary, BigDecimal bonus) {
        this.id = id;
        this.workDate = workDate;
        this.description = description;
        this.salary = salary;
        this.bonus = bonus;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }

    public BigDecimal getTotalIncome() {
        return salary.add(bonus);
    }
}