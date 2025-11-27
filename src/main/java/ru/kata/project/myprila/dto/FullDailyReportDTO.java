// FullDailyReportDTO.java
package ru.kata.project.myprila.dto;

import java.math.BigDecimal;
import java.util.List;

public class FullDailyReportDTO {
    private int totalDays;
    private BigDecimal totalSalary;
    private BigDecimal totalBonus;
    private BigDecimal totalIncome;
    private BigDecimal averagePerDay;
    private List<DailyReportDTO> workDays;

    public FullDailyReportDTO() {}

    public FullDailyReportDTO(int totalDays, BigDecimal totalSalary, BigDecimal totalBonus,
                              BigDecimal totalIncome, BigDecimal averagePerDay, List<DailyReportDTO> workDays) {
        this.totalDays = totalDays;
        this.totalSalary = totalSalary;
        this.totalBonus = totalBonus;
        this.totalIncome = totalIncome;
        this.averagePerDay = averagePerDay;
        this.workDays = workDays;
    }

    // Геттеры и сеттеры
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public BigDecimal getTotalSalary() { return totalSalary; }
    public void setTotalSalary(BigDecimal totalSalary) { this.totalSalary = totalSalary; }

    public BigDecimal getTotalBonus() { return totalBonus; }
    public void setTotalBonus(BigDecimal totalBonus) { this.totalBonus = totalBonus; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getAveragePerDay() { return averagePerDay; }
    public void setAveragePerDay(BigDecimal averagePerDay) { this.averagePerDay = averagePerDay; }

    public List<DailyReportDTO> getWorkDays() { return workDays; }
    public void setWorkDays(List<DailyReportDTO> workDays) { this.workDays = workDays; }
}