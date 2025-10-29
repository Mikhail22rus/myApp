package ru.kata.project.myprila.dto;

import ru.kata.project.myprila.entity.WorkDay;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;

public class MonthlyDetailedReportDTO {
    private final int year;
    private final Month month;
    private final int daysCount;
    private final BigDecimal totalSalary;
    private final BigDecimal totalBonus;
    private final BigDecimal totalIncome;
    private final List<WorkDay> workDays;

    public MonthlyDetailedReportDTO(int year, Month month, int daysCount, BigDecimal totalSalary,
                                    BigDecimal totalBonus, BigDecimal totalIncome, List<WorkDay> workDays) {
        this.year = year;
        this.month = month;
        this.daysCount = daysCount;
        this.totalSalary = totalSalary;
        this.totalBonus = totalBonus;
        this.totalIncome = totalIncome;
        this.workDays = workDays;
    }

    // Геттеры
    public int getYear() { return year; }
    public Month getMonth() { return month; }
    public int getDaysCount() { return daysCount; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public BigDecimal getTotalBonus() { return totalBonus; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public List<WorkDay> getWorkDays() { return workDays; }
}