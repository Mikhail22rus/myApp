package ru.kata.project.myprila.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kata.project.myprila.dto.*;
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

    // ---------------------------- FULL DAILY REPORT ----------------------------

    public FullDailyReportDTO getFullDailyReport(Long userId) {
        List<WorkDay> workDays = workDayRepository.findByUserIdOrderByWorkDateDesc(userId);

        // фильтруем null-даты и предупреждаем
        workDays = workDays.stream()
                .filter(day -> {
                    if (day.getWorkDate() == null) {
                        System.err.println("⚠ Найдена запись с NULL workDate, id=" + day.getId());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Конвертируем в DTO
        List<DailyReportDTO> dailyReportDTOs = workDays.stream()
                .map(workDay -> new DailyReportDTO(
                        workDay.getId(),
                        workDay.getWorkDate(),
                        workDay.getDescription(),
                        safe(workDay.getSalary()),
                        safe(workDay.getBonus())
                ))
                .collect(Collectors.toList());

        int totalDays = workDays.size();
        BigDecimal totalSalary = workDays.stream()
                .map(w -> safe(w.getSalary()))
                .reduce(ZERO, BigDecimal::add);
        BigDecimal totalBonus = workDays.stream()
                .map(w -> safe(w.getBonus()))
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalIncome = totalSalary.add(totalBonus);
        BigDecimal averagePerDay = totalDays > 0 ?
                totalIncome.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP) :
                ZERO;

        return new FullDailyReportDTO(totalDays, totalSalary, totalBonus, totalIncome, averagePerDay, dailyReportDTOs);
    }

    // ----------------------------- MONTHLY REPORT ------------------------------

    public List<MonthlyReportDTO> getMonthlyReport(Long userId, Integer year) {
        validateUserId(userId);

        List<WorkDay> userDays = workDayRepository.findByUserId(userId);
        int targetYear = year != null ? year : LocalDate.now().getYear();

        userDays = userDays.stream()
                .filter(d -> d.getWorkDate() != null)
                .collect(Collectors.toList());

        Map<Month, MonthStats> monthlyStats = userDays.stream()
                .filter(day -> day.getWorkDate().getYear() == targetYear)
                .collect(Collectors.groupingBy(
                        day -> day.getWorkDate().getMonth(),
                        Collectors.collectingAndThen(Collectors.toList(), this::calculateMonthStats)
                ));

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

    // ------------------------------- ANNUAL REPORT ----------------------------

    public AnnualReportDTO getAnnualReport(Long userId, Integer year) {
        validateUserId(userId);

        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<MonthlyReportDTO> monthlyReport = getMonthlyReport(userId, targetYear);

        int totalDays = monthlyReport.stream().mapToInt(MonthlyReportDTO::getDaysCount).sum();
        BigDecimal totalSalary = monthlyReport.stream().map(MonthlyReportDTO::getTotalSalary).reduce(ZERO, BigDecimal::add);
        BigDecimal totalBonus = monthlyReport.stream().map(MonthlyReportDTO::getTotalBonus).reduce(ZERO, BigDecimal::add);
        BigDecimal totalIncome = monthlyReport.stream().map(MonthlyReportDTO::getTotalIncome).reduce(ZERO, BigDecimal::add);

        long monthsWithData = monthlyReport.stream().filter(m -> m.getDaysCount() > 0).count();
        BigDecimal averageMonthlyIncome = monthsWithData > 0 ?
                totalIncome.divide(BigDecimal.valueOf(monthsWithData), 2, RoundingMode.HALF_UP) :
                ZERO;

        return new AnnualReportDTO(targetYear, totalDays, totalSalary, totalBonus,
                totalIncome, averageMonthlyIncome, monthlyReport);
    }

    // -------------------------- MONTHLY DETAILED REPORT -----------------------

    public MonthlyDetailedReportDTO getMonthlyDetailedReport(Long userId, Integer year, Integer month) {
        validateUserId(userId);

        List<WorkDay> userDays = workDayRepository.findByUserId(userId);

        userDays = userDays.stream()
                .filter(day -> day.getWorkDate() != null)
                .collect(Collectors.toList());

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

    // ---------------------------- Helper Methods ------------------------------

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

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    // ----------------------------- Inner class --------------------------------

    private static class MonthStats {
        private int daysCount = 0;
        private BigDecimal totalSalary = ZERO;
        private BigDecimal totalBonus = ZERO;
        private BigDecimal totalIncome = ZERO;

        public void addDay(WorkDay day) {
            daysCount++;

            BigDecimal salary = day.getSalary() != null ? day.getSalary() : ZERO;
            BigDecimal bonus = day.getBonus() != null ? day.getBonus() : ZERO;

            totalSalary = totalSalary.add(salary);
            totalBonus = totalBonus.add(bonus);
            totalIncome = totalIncome.add(salary).add(bonus);

            System.out.println("Обработан день: " + day.getWorkDate() +
                    " | salary: " + salary +
                    " | bonus: " + bonus +
                    " | totalIncome: " + totalIncome);
        }

        public int getDaysCount() { return daysCount; }
        public BigDecimal getTotalSalary() { return totalSalary; }
        public BigDecimal getTotalBonus() { return totalBonus; }
        public BigDecimal getTotalIncome() { return totalIncome; }
    }
}
