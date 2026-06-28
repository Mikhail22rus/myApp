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
import java.time.LocalDateTime;
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

    private static final BigDecimal DEFAULT_SALARY = new BigDecimal("4000.00");
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

        Optional<WorkDay> existingDay = workDayRepository.findByWorkDateAndUserId(workDate, userId);
        WorkDay workDay;

        if (existingDay.isPresent()) {
            workDay = existingDay.get();
            if (description != null && !description.trim().isEmpty()) {
                workDay.setDescription(description);
            }

            workDay.setSalary(salary != null ? salary :
                    workDay.getSalary() != null ? workDay.getSalary() : DEFAULT_SALARY);

            workDay.setBonus(bonus != null ? bonus :
                    workDay.getBonus() != null ? workDay.getBonus() : ZERO);
        } else {
            workDay = new WorkDay();
            workDay.setWorkDate(workDate);
            workDay.setUser(user);
            workDay.setDescription(description != null ? description : "Рабочий день");
            workDay.setSalary(salary != null ? salary : DEFAULT_SALARY);
            workDay.setBonus(bonus != null ? bonus : ZERO); // Всегда ZERO, а не null
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

    // ========== ВЫПЛАТЫ ==========

    public SalaryPayment addSalaryPayment(BigDecimal amount, String description, LocalDate paymentDate, Long userId) {
        User user = validateUserExists(userId);

        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new RuntimeException("Сумма выплаты должна быть положительной");
        }

        // Преобразуем LocalDate в LocalDateTime (начало дня)
        LocalDateTime paymentDateTime;
        if (paymentDate == null) {
            paymentDateTime = LocalDateTime.now();
        } else {
            paymentDateTime = paymentDate.atStartOfDay(); // 00:00:00
        }

        SalaryPayment payment = new SalaryPayment();
        payment.setAmount(amount);
        payment.setDescription(description);
        payment.setPaymentDate(paymentDateTime); // LocalDateTime
        payment.setUser(user);

        SalaryPayment savedPayment = salaryPaymentRepository.save(payment);
        System.out.println("💾 Сохранена выплата: " + savedPayment.getAmount() +
                " на дату: " + savedPayment.getPaymentDate().toLocalDate());

        return savedPayment;
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

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // 📊 Данные текущего месяца
        List<WorkDay> currentMonthDays = workDayRepository.findByWorkDateBetween(startOfMonth, endOfMonth)
                .stream()
                .filter(day -> day.getUser().getId().equals(userId))
                .toList();

        List<SalaryPayment> currentMonthPayments = salaryPaymentRepository.findByUserIdAndPaymentDateBetween(
                userId,
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(23, 59, 59)
        );

        // 📊 Данные всех прошлых месяцев (для долга)
        List<WorkDay> allUserDays = workDayRepository.findByUserId(userId);
        List<SalaryPayment> allUserPayments = salaryPaymentRepository.findByUserId(userId);

        BigDecimal totalSalaryBefore = allUserDays.stream()
                .filter(day -> day.getWorkDate().isBefore(startOfMonth))
                .map(day -> day.getSalary() != null ? day.getSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaidBefore = allUserPayments.stream()
                .filter(p -> p.getPaymentDate().toLocalDate().isBefore(startOfMonth))
                .map(SalaryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 💰 Долг из прошлого месяца
        BigDecimal carriedDebt = totalSalaryBefore.subtract(totalPaidBefore);
        if (carriedDebt.compareTo(BigDecimal.ZERO) < 0) {
            carriedDebt = BigDecimal.ZERO; // Если переплата — долг не переносим
        }

        // ✅ Зарплата и бонусы за текущий месяц
        BigDecimal totalSalary = currentMonthDays.stream()
                .map(day -> day.getSalary() != null ? day.getSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBonus = currentMonthDays.stream()
                .map(day -> day.getBonus() != null ? day.getBonus() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEarned = totalSalary.add(totalBonus);

        BigDecimal totalPaid = currentMonthPayments.stream()
                .map(SalaryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🧾 Итоговый баланс (с учётом долга)
        BigDecimal salaryBalance = totalSalary.add(carriedDebt).subtract(totalPaid);

        return new WorkDayStatistics(
                currentMonthDays.size(),
                totalEarned,
                totalSalary,
                totalBonus,
                totalPaid,
                salaryBalance
        );
    }

    public BigDecimal getSalaryBalance(Long userId) {
        // вызываем нестатический метод getStatistics для данного пользователя
        return getStatistics(userId).getSalaryBalance();
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
        // В WorkDayService


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
