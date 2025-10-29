package ru.kata.project.myprila.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kata.project.myprila.entity.SalaryPayment;
import ru.kata.project.myprila.entity.User;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.repo.SalaryPaymentRepository;
import ru.kata.project.myprila.repo.UserRepository;
import ru.kata.project.myprila.repo.WorkDayReposytory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class WorkDayService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    @Autowired
    private WorkDayReposytory workDayRepository;

    private static final BigDecimal DEFAULT_SALARY = new BigDecimal("3500.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ========== РАБОЧИЕ ДНИ ==========

    public List<WorkDay> getUserWorkDays(Long userId) {
        validateUserExists(userId);
        return workDayRepository.findByUserIdOrderByWorkDateDesc(userId);
    }

    public WorkDay createWorkDay(WorkDay workDay, Long userId) {
        User user = validateUserExists(userId);

        if (workDayRepository.existsByWorkDateAndUserId(workDay.getWorkDate(), userId)) {
            throw new RuntimeException("Рабочий день на эту дату уже существует");
        }

        workDay.setUser(user);

        if (workDay.getSalary() == null) {
            workDay.setSalary(DEFAULT_SALARY);
        }

        if (workDay.getBonus() == null) {
            workDay.setBonus(ZERO);
        }

        return workDayRepository.save(workDay);
    }

    public WorkDay updateWorkDay(Long workDayId, WorkDay workDayUpdates, Long userId) {
        validateUserExists(userId);

        WorkDay existingWorkDay = workDayRepository.findByIdAndUserId(workDayId, userId)
                .orElseThrow(() -> new RuntimeException("Рабочий день не найден"));

        if (workDayUpdates.getDescription() != null) {
            existingWorkDay.setDescription(workDayUpdates.getDescription());
        }
        if (workDayUpdates.getSalary() != null && workDayUpdates.getSalary().compareTo(ZERO) >= 0) {
            existingWorkDay.setSalary(workDayUpdates.getSalary());
        }
        if (workDayUpdates.getBonus() != null && workDayUpdates.getBonus().compareTo(ZERO) >= 0) {
            existingWorkDay.setBonus(workDayUpdates.getBonus());
        }

        return workDayRepository.save(existingWorkDay);
    }

    public void deleteWorkDay(Long workDayId, Long userId) {
        validateUserExists(userId);

        WorkDay workDay = workDayRepository.findByIdAndUserId(workDayId, userId)
                .orElseThrow(() -> new RuntimeException("Рабочий день не найден"));

        workDayRepository.delete(workDay);
    }

    // ========== ВЫПЛАТЫ ==========

    public SalaryPayment addSalaryPayment(BigDecimal amount, String description, Long userId) {
        User user = validateUserExists(userId);

        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new RuntimeException("Сумма выплаты должна быть положительной");
        }

        SalaryPayment payment = new SalaryPayment(amount, description, user);
        return salaryPaymentRepository.save(payment);
    }

    public List<SalaryPayment> getUserSalaryPayments(Long userId) {
        validateUserExists(userId);
        return salaryPaymentRepository.findByUserIdOrderByPaymentDateDesc(userId);
    }

    public void deleteSalaryPayment(Long paymentId, Long userId) {
        validateUserExists(userId);

        SalaryPayment payment = salaryPaymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new RuntimeException("Выплата не найдена"));

        salaryPaymentRepository.delete(payment);
    }

    // ========== СТАТИСТИКА ==========

    public WorkDayStatistics getStatistics(Long userId) {
        validateUserExists(userId);

        List<WorkDay> userDays = workDayRepository.findByUserId(userId);
        List<SalaryPayment> userPayments = salaryPaymentRepository.findByUserId(userId);

        int totalDays = userDays.size();

        // ✅ ЗАРАБОТОК (только зарплата, без бонусов)
        BigDecimal totalSalary = userDays.stream()
                .map(day -> day.getSalary() != null ? day.getSalary() : ZERO)
                .reduce(ZERO, BigDecimal::add);

        // ✅ БОНУСЫ (отдельно, не учитываются в долге)
        BigDecimal totalBonus = userDays.stream()
                .map(day -> day.getBonus() != null ? day.getBonus() : ZERO)
                .reduce(ZERO, BigDecimal::add);

        // ✅ ОБЩИЙ ДОХОД (для отображения)
        BigDecimal totalEarned = totalSalary.add(totalBonus);

        BigDecimal totalPaid = userPayments.stream()
                .map(SalaryPayment::getAmount)
                .reduce(ZERO, BigDecimal::add);

        // ✅ БАЛАНС (только зарплата минус выплаты, бонусы не учитываются)
        BigDecimal salaryBalance = totalSalary.subtract(totalPaid);

        return new WorkDayStatistics(totalDays, totalEarned, totalSalary, totalBonus, totalPaid, salaryBalance);
    }

    public BigDecimal getSalaryBalance(Long userId) {
        validateUserExists(userId);
        WorkDayStatistics stats = getStatistics(userId);
        return stats.getSalaryBalance();
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    // ========== КЛАСС СТАТИСТИКИ ==========

    public static class WorkDayStatistics {
        private final int totalDays;
        private final BigDecimal totalEarned;  // зарплата + бонусы (для отображения)
        private final BigDecimal totalSalary;  // только зарплата (для расчета долга)
        private final BigDecimal totalBonus;   // только бонусы
        private final BigDecimal totalPaid;    // выплачено
        private final BigDecimal salaryBalance; // баланс (зарплата - выплаты)

        public WorkDayStatistics(int totalDays, BigDecimal totalEarned, BigDecimal totalSalary,
                                 BigDecimal totalBonus, BigDecimal totalPaid, BigDecimal salaryBalance) {
            this.totalDays = totalDays;
            this.totalEarned = totalEarned;
            this.totalSalary = totalSalary;
            this.totalBonus = totalBonus;
            this.totalPaid = totalPaid;
            this.salaryBalance = salaryBalance;
        }

        // Геттеры
        public int getTotalDays() { return totalDays; }
        public BigDecimal getTotalEarned() { return totalEarned; }
        public BigDecimal getTotalSalary() { return totalSalary; }
        public BigDecimal getTotalBonus() { return totalBonus; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public BigDecimal getSalaryBalance() { return salaryBalance; }
    }
}