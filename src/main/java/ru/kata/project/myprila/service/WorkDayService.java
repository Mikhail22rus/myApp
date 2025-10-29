package ru.kata.project.myprila.service;

import jakarta.transaction.Transactional;
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
import java.util.Optional;

@Service
@Transactional
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

    /**
     * Основной метод: создает или обновляет рабочий день
     * Можно передать и зарплату, и бонус, или только что-то одно
     */
    public WorkDay addOrUpdateWorkDay(LocalDate workDate, String description, BigDecimal salary, BigDecimal bonus, Long userId) {
        User user = validateUserExists(userId);

        // Ищем существующий день
        Optional<WorkDay> existingDay = workDayRepository.findByWorkDateAndUserId(workDate, userId);

        WorkDay workDay;

        if (existingDay.isPresent()) {
            // Обновляем существующий день
            workDay = existingDay.get();

            if (description != null && !description.trim().isEmpty()) {
                workDay.setDescription(description);
            }
            if (salary != null && salary.compareTo(ZERO) >= 0) {
                workDay.setSalary(salary);
            }
            if (bonus != null && bonus.compareTo(ZERO) >= 0) {
                workDay.setBonus(bonus);
            }
        } else {
            // Создаем новый день
            workDay = new WorkDay();
            workDay.setWorkDate(workDate);
            workDay.setUser(user);
            workDay.setDescription(description != null ? description : "Рабочий день");
            workDay.setSalary(salary != null ? salary : DEFAULT_SALARY);
            workDay.setBonus(bonus != null ? bonus : ZERO);
        }

        return workDayRepository.save(workDay);
    }

    /**
     * Добавить только заработок (без изменения бонуса)
     */
    public WorkDay addSalaryOnly(LocalDate workDate, BigDecimal salary, String description, Long userId) {
        User user = validateUserExists(userId);

        if (salary == null || salary.compareTo(ZERO) < 0) {
            throw new RuntimeException("Заработок должен быть положительным числом");
        }

        // Ищем существующий день
        Optional<WorkDay> existingDay = workDayRepository.findByWorkDateAndUserId(workDate, userId);

        WorkDay workDay;

        if (existingDay.isPresent()) {
            // Обновляем заработок в существующем дне
            workDay = existingDay.get();
            workDay.setSalary(salary);
            if (description != null && !description.trim().isEmpty()) {
                workDay.setDescription(description);
            }
        } else {
            // Создаем новый день только с заработком
            workDay = new WorkDay();
            workDay.setWorkDate(workDate);
            workDay.setUser(user);
            workDay.setDescription(description != null ? description : "Рабочий день");
            workDay.setSalary(salary);
            workDay.setBonus(ZERO);
        }

        return workDayRepository.save(workDay);
    }

    /**
     * Добавить только бонус (без изменения заработка)
     */
    public WorkDay addBonusOnly(LocalDate workDate, BigDecimal bonus, String description, Long userId) {
        User user = validateUserExists(userId);

        if (bonus == null || bonus.compareTo(ZERO) < 0) {
            throw new RuntimeException("Бонус должен быть положительным числом");
        }

        // Ищем существующий день
        Optional<WorkDay> existingDay = workDayRepository.findByWorkDateAndUserId(workDate, userId);

        WorkDay workDay;

        if (existingDay.isPresent()) {
            // Обновляем бонус в существующем дне
            workDay = existingDay.get();
            workDay.setBonus(bonus);
            if (description != null && !description.trim().isEmpty()) {
                // Добавляем описание к существующему, если нужно
                if (workDay.getDescription() != null && !workDay.getDescription().contains(description)) {
                    workDay.setDescription(workDay.getDescription() + "; " + description);
                } else if (workDay.getDescription() == null) {
                    workDay.setDescription(description);
                }
            }
        } else {
            // Создаем новый день только с бонусом
            workDay = new WorkDay();
            workDay.setWorkDate(workDate);
            workDay.setUser(user);
            workDay.setDescription(description != null ? description : "Дополнительный доход");
            workDay.setSalary(ZERO); // Основной заработок 0
            workDay.setBonus(bonus);
        }

        return workDayRepository.save(workDay);
    }

    /**
     * Получить рабочий день по дате
     */
    public Optional<WorkDay> getWorkDayByDate(LocalDate workDate, Long userId) {
        validateUserExists(userId);
        return workDayRepository.findByWorkDateAndUserId(workDate, userId);
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
        public int getTotalDays() {
            return totalDays;
        }

        public BigDecimal getTotalEarned() {
            return totalEarned;
        }

        public BigDecimal getTotalSalary() {
            return totalSalary;
        }

        public BigDecimal getTotalBonus() {
            return totalBonus;
        }

        public BigDecimal getTotalPaid() {
            return totalPaid;
        }

        public BigDecimal getSalaryBalance() {
            return salaryBalance;
        }
    }
}