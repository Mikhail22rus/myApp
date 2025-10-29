package ru.kata.project.myprila.dto;

import java.math.BigDecimal;
import java.time.Month;

public class MonthlyReportDTO {
    private final Month month;
    private final int year;
    private final int daysCount;
    private final BigDecimal totalSalary;
    private final BigDecimal totalBonus;
    private final BigDecimal totalIncome;

    public MonthlyReportDTO(Month month, int year, int daysCount, BigDecimal totalSalary,
                            BigDecimal totalBonus, BigDecimal totalIncome) {
        this.month = month;
        this.year = year;
        this.daysCount = daysCount;
        this.totalSalary = totalSalary;
        this.totalBonus = totalBonus;
        this.totalIncome = totalIncome;
    }

    // Геттеры
    public Month getMonth() { return month; }
    public int getYear() { return year; }
    public int getDaysCount() { return daysCount; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public BigDecimal getTotalBonus() { return totalBonus; }
    public BigDecimal getTotalIncome() { return totalIncome; }
}