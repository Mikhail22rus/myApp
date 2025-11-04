package ru.kata.project.myprila.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kata.project.myprila.dto.AnnualReportDTO;
import ru.kata.project.myprila.dto.MonthlyDetailedReportDTO;
import ru.kata.project.myprila.dto.MonthlyReportDTO;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.repo.WorkDayReposytory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private WorkDayReposytory workDayRepository;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * Отчет по месяцам за год
     */
    public List<MonthlyReportDTO> getMonthlyReport(Long userId, Integer year) {
        validateUserId(userId);

        List<WorkDay> userDays = workDayRepository.findByUserId(userId);
        int targetYear = year != null ? year : LocalDate.now().getYear();

        // Группируем по месяцам
        Map<Month, MonthStats> monthlyStats = userDays.stream()
                .filter(day -> day.getWorkDate().getYear() == targetYear)
                .collect(Collectors.groupingBy(
                        day -> day.getWorkDate().getMonth(),
                        Collectors.collectingAndThen(Collectors.toList(), this::calculateMonthStats)
                ));

        // Создаем отчет для всех месяцев
        List<MonthlyReportDTO> report = new ArrayList<>();
        for (Month month : Month.values()) {
            MonthStats stats = monthlyStats.getOrDefault(month, new MonthStats());
            report.add(new MonthlyReportDTO(
                    month, targetYear, stats.getDaysCount(),
                    stats.getTotalSalary(), stats.getTotalBonus(), stats.getTotalIncome()
            ));
        }

        return report;
    }

    /**
     * Годовой отчет
     */
    public AnnualReportDTO getAnnualReport(Long userId, Integer year) {
        validateUserId(userId);

        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<MonthlyReportDTO> monthlyReport = getMonthlyReport(userId, targetYear);

        // Считаем общие суммы
        int totalDays = monthlyReport.stream().mapToInt(MonthlyReportDTO::getDaysCount).sum();
        BigDecimal totalSalary = monthlyReport.stream()
                .map(MonthlyReportDTO::getTotalSalary)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalBonus = monthlyReport.stream()
                .map(MonthlyReportDTO::getTotalBonus)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalIncome = monthlyReport.stream()
                .map(MonthlyReportDTO::getTotalIncome)
                .reduce(ZERO, BigDecimal::add);

        // Средний месячный доход
        long monthsWithData = monthlyReport.stream()
                .filter(month -> month.getDaysCount() > 0)
                .count();
        BigDecimal averageMonthlyIncome = monthsWithData > 0 ?
                totalIncome.divide(BigDecimal.valueOf(monthsWithData), 2, RoundingMode.HALF_UP) :
                ZERO;

        return new AnnualReportDTO(targetYear, totalDays, totalSalary, totalBonus,
                totalIncome, averageMonthlyIncome, monthlyReport);
    }

    /**
     * Детальный отчет по месяцу
     */
    public MonthlyDetailedReportDTO getMonthlyDetailedReport(Long userId, Integer year, Integer month) {
        validateUserId(userId);

        List<WorkDay> userDays = workDayRepository.findByUserId(userId);
        Month targetMonth = month != null ? Month.of(month) : LocalDate.now().getMonth();

        List<WorkDay> monthDays = userDays.stream()
                .filter(day -> day.getWorkDate().getYear() == year &&
                        day.getWorkDate().getMonth() == targetMonth)
                .sorted(Comparator.comparing(WorkDay::getWorkDate))
                .collect(Collectors.toList());

        MonthStats stats = calculateMonthStats(monthDays);

        return new MonthlyDetailedReportDTO(year, targetMonth, stats.getDaysCount(),
                stats.getTotalSalary(), stats.getTotalBonus(),
                stats.getTotalIncome(), monthDays);
    }

    /**
     * Вспомогательный метод для расчета статистики месяца
     */
    private MonthStats calculateMonthStats(List<WorkDay> days) {
        MonthStats stats = new MonthStats();
        days.forEach(stats::addDay);
        return stats;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("User ID не может быть null");
        }
    }

    private static class MonthStats {
        private int daysCount = 0;
        private BigDecimal totalSalary = BigDecimal.ZERO;
        private BigDecimal totalBonus = BigDecimal.ZERO;
        private BigDecimal totalIncome = BigDecimal.ZERO;

        public void addDay(WorkDay day) {
            daysCount++;

            // Безопасное извлечение значений
            BigDecimal salary = safeGetBigDecimal(day.getSalary());
            BigDecimal bonus = safeGetBigDecimal(day.getBonus());

            // Сложение
            totalSalary = totalSalary.add(salary);
            totalBonus = totalBonus.add(bonus);
            totalIncome = totalIncome.add(salary).add(bonus);

            System.out.println("Обработан день: " + day.getWorkDate() +
                    " | salary: " + salary +
                    " | bonus: " + bonus +
                    " | totalIncome: " + totalIncome);
        }

        private BigDecimal safeGetBigDecimal(BigDecimal value) {
            return value != null ? value : BigDecimal.ZERO;
        }

        public int getDaysCount() { return daysCount; }
        public BigDecimal getTotalSalary() { return totalSalary; }
        public BigDecimal getTotalBonus() { return totalBonus; }
        public BigDecimal getTotalIncome() { return totalIncome; }
    }
}