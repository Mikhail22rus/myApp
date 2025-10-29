package ru.kata.project.myprila.dto;

import java.math.BigDecimal;
import java.util.List;

public class AnnualReportDTO {
    private final int year;
    private final int totalDays;
    private final BigDecimal totalSalary;
    private final BigDecimal totalBonus;
    private final BigDecimal totalIncome;
    private final BigDecimal averageMonthlyIncome;
    private final List<MonthlyReportDTO> monthlyDetails;

    public AnnualReportDTO(int year, int totalDays, BigDecimal totalSalary, BigDecimal totalBonus,
                           BigDecimal totalIncome, BigDecimal averageMonthlyIncome, List<MonthlyReportDTO> monthlyDetails) {
        this.year = year;
        this.totalDays = totalDays;
        this.totalSalary = totalSalary;
        this.totalBonus = totalBonus;
        this.totalIncome = totalIncome;
        this.averageMonthlyIncome = averageMonthlyIncome;
        this.monthlyDetails = monthlyDetails;
    }

    // Геттеры
    public int getYear() { return year; }
    public int getTotalDays() { return totalDays; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public BigDecimal getTotalBonus() { return totalBonus; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getAverageMonthlyIncome() { return averageMonthlyIncome; }
    public List<MonthlyReportDTO> getMonthlyDetails() { return monthlyDetails; }
}