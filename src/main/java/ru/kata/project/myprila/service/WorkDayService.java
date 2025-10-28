package ru.kata.project.myprila.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(WorkDayService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    @Autowired
    private WorkDayReposytory workDayRepository;

    // ========== РАБОЧИЕ ДНИ ==========

    /**
     * Получить все рабочие дни пользователя
     */
    public List<WorkDay> getUserWorkDays(Long userId) {
        logger.info("📥 Получение рабочих дней для пользователя ID: {}", userId);
        try {
            validateUserExists(userId);
            List<WorkDay> workDays = workDayRepository.findByUserIdOrderByWorkDateDesc(userId);
            logger.info("✅ Найдено {} рабочих дней для пользователя ID: {}", workDays.size(), userId);
            return workDays;
        } catch (Exception e) {
            logger.error("❌ Ошибка при получении рабочих дней для пользователя ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения рабочих дней: " + e.getMessage(), e);
        }
    }

    /**
     * Получить рабочий день по ID с проверкой принадлежности пользователю
     */
    public Optional<WorkDay> getUserWorkDayById(Long workDayId, Long userId) {
        logger.info("📥 Получение рабочего дня ID: {} для пользователя ID: {}", workDayId, userId);
        try {
            validateUserExists(userId);
            Optional<WorkDay> workDay = workDayRepository.findByIdAndUserId(workDayId, userId);
            if (workDay.isPresent()) {
                logger.info("✅ Найден рабочий день: {}", workDay.get());
            } else {
                logger.warn("⚠️ Рабочий день с ID {} не найден для пользователя ID {}", workDayId, userId);
            }
            return workDay;
        } catch (Exception e) {
            logger.error("❌ Ошибка при поиске рабочего дня ID {} для пользователя ID {}: {}", workDayId, userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка поиска рабочего дня: " + e.getMessage(), e);
        }
    }

    /**
     * Создать новый рабочий день для пользователя
     */
    public WorkDay createWorkDay(WorkDay workDay, Long userId) {
        logger.info("🔄 Создание рабочего дня для пользователя ID: {}, дата: {}", userId, workDay.getWorkDate());

        try {
            User user = validateUserExists(userId);

            // Валидация данных
            validateWorkDayData(workDay);

            // Проверяем, нет ли уже дня с такой датой у этого пользователя
            if (workDayRepository.existsByWorkDateAndUserId(workDay.getWorkDate(), userId)) {
                String error = "Рабочий день на дату " + workDay.getWorkDate() + " уже существует";
                logger.error("❌ {}", error);
                throw new RuntimeException(error);
            }

            // Устанавливаем пользователя и зарплату по умолчанию
            workDay.setUser(user);
            if (workDay.getSalary() == null || workDay.getSalary() == 0) {
                workDay.setSalary(3500);
                logger.info("💰 Установлена зарплата по умолчанию: {}", workDay.getSalary());
            }

            // Сохраняем
            WorkDay savedWorkDay = workDayRepository.save(workDay);
            logger.info("✅ Успешно создан рабочий день ID: {} для пользователя ID: {}", savedWorkDay.getId(), userId);

            return savedWorkDay;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("🚨 Ошибка при создании рабочего дня для пользователя ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка создания рабочего дня: " + e.getMessage(), e);
        }
    }

    /**
     * Обновить рабочий день (только свои данные)
     */
    public WorkDay updateWorkDay(Long workDayId, WorkDay workDayUpdates, Long userId) {
        logger.info("🔄 Обновление рабочего дня ID: {} для пользователя ID: {}", workDayId, userId);

        try {
            validateUserExists(userId);

            // Находим существующий рабочий день
            WorkDay existingWorkDay = workDayRepository.findByIdAndUserId(workDayId, userId)
                    .orElseThrow(() -> new RuntimeException("Рабочий день не найден"));

            // Обновляем поля (кроме даты и пользователя)
            if (workDayUpdates.getDescription() != null) {
                existingWorkDay.setDescription(workDayUpdates.getDescription());
            }
            if (workDayUpdates.getSalary() != null && workDayUpdates.getSalary() > 0) {
                existingWorkDay.setSalary(workDayUpdates.getSalary());
            }

            WorkDay updatedWorkDay = workDayRepository.save(existingWorkDay);
            logger.info("✅ Успешно обновлен рабочий день ID: {}", workDayId);

            return updatedWorkDay;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("🚨 Ошибка при обновлении рабочего дня ID {} для пользователя ID {}: {}", workDayId, userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка обновления рабочего дня: " + e.getMessage(), e);
        }
    }

    /**
     * Удалить рабочий день пользователя
     */
    public void deleteWorkDay(Long workDayId, Long userId) {
        logger.info("🗑️ Удаление рабочего дня ID: {} для пользователя ID: {}", workDayId, userId);

        try {
            validateUserExists(userId);

            WorkDay workDay = workDayRepository.findByIdAndUserId(workDayId, userId)
                    .orElseThrow(() -> new RuntimeException("Рабочий день не найден"));

            workDayRepository.delete(workDay);
            logger.info("✅ Успешно удален рабочий день ID: {} для пользователя ID: {}", workDayId, userId);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("🚨 Ошибка при удалении рабочего дня ID {} для пользователя ID {}: {}", workDayId, userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка удаления рабочего дня: " + e.getMessage(), e);
        }
    }

    // ========== ВЫПЛАТЫ ЗАРПЛАТЫ ==========

    /**
     * Добавить выплату зарплаты для пользователя
     */
    public SalaryPayment addSalaryPayment(BigDecimal amount, String description, Long userId) {
        logger.info("💰 Добавление выплаты для пользователя ID: {}, сумма: {}, описание: {}", userId, amount, description);

        try {
            User user = validateUserExists(userId);

            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Сумма выплаты должна быть положительной");
            }

            SalaryPayment payment = new SalaryPayment(amount, description, user);
            SalaryPayment savedPayment = salaryPaymentRepository.save(payment);
            logger.info("✅ Успешно сохранена выплата ID: {} для пользователя ID: {}", savedPayment.getId(), userId);

            return savedPayment;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Ошибка при добавлении выплаты для пользователя ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения выплаты: " + e.getMessage(), e);
        }
    }

    /**
     * Получить все выплаты пользователя
     */
    public List<SalaryPayment> getUserSalaryPayments(Long userId) {
        logger.info("📥 Получение выплат для пользователя ID: {}", userId);

        try {
            validateUserExists(userId);
            List<SalaryPayment> payments = salaryPaymentRepository.findByUserIdOrderByPaymentDateDesc(userId);
            logger.info("✅ Найдено {} выплат для пользователя ID: {}", payments.size(), userId);
            return payments;
        } catch (Exception e) {
            logger.error("❌ Ошибка при получении выплат для пользователя ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения выплат: " + e.getMessage(), e);
        }
    }

    /**
     * Удалить выплату пользователя
     */
    public void deleteSalaryPayment(Long paymentId, Long userId) {
        logger.info("🗑️ Удаление выплаты ID: {} для пользователя ID: {}", paymentId, userId);

        try {
            validateUserExists(userId);

            SalaryPayment payment = salaryPaymentRepository.findByIdAndUserId(paymentId, userId)
                    .orElseThrow(() -> new RuntimeException("Выплата не найдена"));

            salaryPaymentRepository.delete(payment);
            logger.info("✅ Успешно удалена выплата ID: {} для пользователя ID: {}", paymentId, userId);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("❌ Ошибка при удалении выплаты ID {} для пользователя ID {}: {}", paymentId, userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка удаления выплаты: " + e.getMessage(), e);
        }
    }

    // ========== СТАТИСТИКА И ФИНАНСЫ ==========

    /**
     * Получить полную статистику пользователя
     */
    public WorkDayStatistics getStatistics(Long userId) {
        logger.info("📊 Получение статистики для пользователя ID: {}", userId);

        try {
            validateUserExists(userId);

            List<WorkDay> userDays = workDayRepository.findByUserId(userId);
            List<SalaryPayment> userPayments = salaryPaymentRepository.findByUserId(userId);

            int totalDays = userDays.size();
            BigDecimal totalEarned = calculateTotalEarned(userDays);
            BigDecimal totalPaid = calculateTotalPaid(userPayments);
            BigDecimal salaryBalance = totalEarned.subtract(totalPaid);

            logger.info("📊 Статистика для пользователя ID {}: дней={}, заработано={}, выплачено={}, баланс={}",
                    userId, totalDays, totalEarned, totalPaid, salaryBalance);

            return new WorkDayStatistics(totalDays, totalEarned, totalPaid, salaryBalance);

        } catch (Exception e) {
            logger.error("❌ Ошибка при получении статистики для пользователя ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошибка получения статистики: " + e.getMessage(), e);
        }
    }

    /**
     * Получить финансовый баланс пользователя
     */
    public BigDecimal getSalaryBalance(Long userId) {
        logger.debug("🧮 Расчет баланса для пользователя ID: {}", userId);

        try {
            validateUserExists(userId);

            WorkDayStatistics stats = getStatistics(userId);
            return stats.getSalaryBalance();

        } catch (Exception e) {
            logger.error("❌ Ошибка при расчете баланса для пользователя ID {}: {}", userId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверить существование пользователя
     */
    private User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));
    }

    /**
     * Валидация данных рабочего дня
     */
    private void validateWorkDayData(WorkDay workDay) {
        if (workDay.getWorkDate() == null) {
            throw new RuntimeException("Дата рабочего дня не может быть пустой");
        }

        if (workDay.getWorkDate().isAfter(LocalDate.now())) {
            logger.warn("⚠️ Создается рабочий день в будущем: {}", workDay.getWorkDate());
        }
    }

    /**
     * Расчет общего заработка
     */
    private BigDecimal calculateTotalEarned(List<WorkDay> workDays) {
        return workDays.stream()
                .map(day -> BigDecimal.valueOf(day.getSalary()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Расчет общих выплат
     */
    private BigDecimal calculateTotalPaid(List<SalaryPayment> payments) {
        return payments.stream()
                .map(SalaryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Проверка состояния сервиса
     */
    public String getServiceStatus(Long userId) {
        try {
            validateUserExists(userId);
            long workDayCount = workDayRepository.countByUserId(userId);
            long salaryPaymentCount = salaryPaymentRepository.countByUserId(userId);
            return String.format("✅ Сервис работает. Пользователь ID: %d, WorkDays: %d, Payments: %d",
                    userId, workDayCount, salaryPaymentCount);
        } catch (Exception e) {
            return "❌ Ошибка сервиса: " + e.getMessage();
        }
    }

    // ========== КЛАСС СТАТИСТИКИ ==========

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