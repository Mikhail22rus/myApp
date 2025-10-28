package ru.kata.project.myprila.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.kata.project.myprila.entity.SalaryPayment;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.repo.SalaryPaymentRepository;
import ru.kata.project.myprila.repo.WorkDayReposytory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class WorkDayService {

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    @Autowired
    private WorkDayReposytory workDayRepository;

    // Получить все рабочие дни
    public List<WorkDay> getAllWorkDays() {
        return workDayRepository.findAllByOrderByWorkDateDesc();
    }

    // Получить рабочий день по ID
    public Optional<WorkDay> getWorkDayById(Long id) {
        return workDayRepository.findById(id);
    }

    // Сохранить рабочий день
    public WorkDay saveWorkDay(WorkDay workDay) {
        // Проверяем, нет ли уже дня с такой датой
        if (workDayRepository.existsByWorkDate(workDay.getWorkDate())) {
            throw new RuntimeException("Рабочий день на дату " + workDay.getWorkDate() + " уже существует");
        }

        // Устанавливаем фиксированную зарплату 3500 рублей
        workDay.setSalary(3500);

        WorkDay saved = workDayRepository.save(workDay);
        System.out.println("✅ Сохранен рабочий день: " + saved);
        return saved;
    }

    // Удалить рабочий день
    public void deleteWorkDay(Long id) {
        if (workDayRepository.existsById(id)) {
            workDayRepository.deleteById(id);
            System.out.println("🗑️ Удален рабочий день с ID: " + id);
        } else {
            throw new RuntimeException("Рабочий день с ID " + id + " не найден");
        }
    }

    // 📊 МЕТОДЫ ДЛЯ УЧЕТА ЗАРПЛАТЫ

    public SalaryPayment addSalaryPayment(BigDecimal amount, String description) {
        SalaryPayment payment = new SalaryPayment(amount, description);
        return salaryPaymentRepository.save(payment);
    }

    public List<SalaryPayment> getAllSalaryPayments() {
        return salaryPaymentRepository.findAllByOrderByPaymentDateDesc();
    }

    public void deleteSalaryPayment(Long id) {
        salaryPaymentRepository.deleteById(id);
    }

    // Расчет общей суммы заработанного
    public BigDecimal getTotalEarned() {
        List<WorkDay> workDays = workDayRepository.findAll();
        return workDays.stream()
                .map(workDay -> BigDecimal.valueOf(workDay.getSalary()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Расчет общей суммы выплат
    public BigDecimal getTotalPaid() {
        List<SalaryPayment> payments = salaryPaymentRepository.findAll();
        return payments.stream()
                .map(SalaryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Расчет остатка (долг работодателя)
    public BigDecimal getSalaryBalance() {
        BigDecimal totalEarned = getTotalEarned();
        BigDecimal totalPaid = getTotalPaid();
        return totalEarned.subtract(totalPaid);
    }

    // 📈 ОБНОВЛЕННАЯ СТАТИСТИКА (объединенная версия)
    public WorkDayStatistics getStatistics() {
        List<WorkDay> allDays = workDayRepository.findAll();
        int totalDays = allDays.size();
        BigDecimal totalEarned = getTotalEarned();
        BigDecimal totalPaid = getTotalPaid();
        BigDecimal salaryBalance = getSalaryBalance();

        return new WorkDayStatistics(totalDays, totalEarned, totalPaid, salaryBalance);
    }

    // 📊 ЕДИНЫЙ КЛАСС СТАТИСТИКИ (исправленный)
    public static class WorkDayStatistics {
        private final int totalDays;
        private final BigDecimal totalEarned;
        private final BigDecimal totalPaid;
        private final BigDecimal salaryBalance;

        public WorkDayStatistics(int totalDays, BigDecimal totalEarned,
                                 BigDecimal totalPaid, BigDecimal salaryBalance) {
            this.totalDays = totalDays;
            this.totalEarned = totalEarned;
            this.totalPaid = totalPaid;
            this.salaryBalance = salaryBalance;
        }

        // Геттеры
        public int getTotalDays() { return totalDays; }
        public BigDecimal getTotalEarned() { return totalEarned; }
        public BigDecimal getTotalPaid() { return totalPaid; }
        public BigDecimal getSalaryBalance() { return salaryBalance; }
    }
}