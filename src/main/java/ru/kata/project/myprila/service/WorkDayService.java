package ru.kata.project.myprila.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kata.project.myprila.entity.SalaryPayment;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.repo.SalaryPaymentRepository;
import ru.kata.project.myprila.repo.WorkDayReposytory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WorkDayService {

    private static final Logger logger = LoggerFactory.getLogger(WorkDayService.class);

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    @Autowired
    private WorkDayReposytory workDayRepository;

    // Получить все рабочие дни
    public List<WorkDay> getAllWorkDays() {
        logger.info("📥 Получение всех рабочих дней");
        try {
            List<WorkDay> workDays = workDayRepository.findAllByOrderByWorkDateDesc();
            logger.info("✅ Найдено {} рабочих дней", workDays.size());
            return workDays;
        } catch (Exception e) {
            logger.error("❌ Ошибка при получении рабочих дней: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения данных: " + e.getMessage(), e);
        }
    }

    // Получить рабочий день по ID
    public Optional<WorkDay> getWorkDayById(Long id) {
        logger.info("📥 Получение рабочего дня по ID: {}", id);
        try {
            Optional<WorkDay> workDay = workDayRepository.findById(id);
            if (workDay.isPresent()) {
                logger.info("✅ Найден рабочий день: {}", workDay.get());
            } else {
                logger.warn("⚠️ Рабочий день с ID {} не найден", id);
            }
            return workDay;
        } catch (Exception e) {
            logger.error("❌ Ошибка при поиске рабочего дня по ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Ошибка поиска: " + e.getMessage(), e);
        }
    }

    // Сохранить рабочий день
    public WorkDay saveWorkDay(WorkDay workDay) {
        logger.info("🔄 Начало сохранения WorkDay: {}", workDay.getWorkDate());

        try {
            // Валидация входных данных
            if (workDay.getWorkDate() == null) {
                String error = "Дата рабочего дня не может быть пустой";
                logger.error("❌ {}", error);
                throw new RuntimeException(error);
            }

            if (workDay.getWorkDate().isAfter(LocalDate.now())) {
                logger.warn("⚠️ Создается рабочий день в будущем: {}", workDay.getWorkDate());
            }

            // Проверяем, нет ли уже дня с такой датой
            logger.info("📋 Проверка существования записи на дату: {}", workDay.getWorkDate());
            boolean exists = workDayRepository.existsByWorkDate(workDay.getWorkDate());
            logger.info("📋 Результат проверки: {}", exists);

            if (exists) {
                String error = "Рабочий день на дату " + workDay.getWorkDate() + " уже существует";
                logger.error("❌ {}", error);
                throw new RuntimeException(error);
            }

            // Устанавливаем фиксированную зарплату 3500 рублей, если не установлена
            if (workDay.getSalary() == null || workDay.getSalary() == 0) {
                workDay.setSalary(3500);
                logger.info("💰 Установлена зарплата по умолчанию: {}", workDay.getSalary());
            } else {
                logger.info("💰 Использована переданная зарплата: {}", workDay.getSalary());
            }

            // Сохраняем
            logger.info("💾 Вызов workDayRepository.save()");
            WorkDay saved = workDayRepository.save(workDay);
            logger.info("✅ Успешно сохранен WorkDay с ID: {}, Дата: {}, Зарплата: {}",
                    saved.getId(), saved.getWorkDate(), saved.getSalary());

            // Проверяем, что запись действительно сохранилась
            boolean existsAfterSave = workDayRepository.existsById(saved.getId());
            logger.info("🔍 Проверка существования после сохранения: {}", existsAfterSave);

            if (!existsAfterSave) {
                logger.error("🚨 ЗАПИСЬ НЕ СОХРАНИЛАСЬ В БАЗЕ! ID: {}", saved.getId());
                throw new RuntimeException("Запись не сохранилась в базе данных");
            }

            return saved;

        } catch (RuntimeException e) {
            // Перебрасываем бизнес-ошибки как есть
            throw e;
        } catch (Exception e) {
            logger.error("🚨 КРИТИЧЕСКАЯ ОШИБКА при сохранении WorkDay:", e);
            throw new RuntimeException("Ошибка сохранения в базе данных: " + e.getMessage(), e);
        }
    }

    // Удалить рабочий день
    public void deleteWorkDay(Long id) {
        logger.info("🗑️ Попытка удаления рабочего дня с ID: {}", id);

        try {
            boolean exists = workDayRepository.existsById(id);
            logger.info("📋 Запись с ID {} существует: {}", id, exists);

            if (exists) {
                workDayRepository.deleteById(id);
                logger.info("✅ Успешно удален рабочий день с ID: {}", id);

                // Проверяем, что запись действительно удалилась
                boolean existsAfterDelete = workDayRepository.existsById(id);
                if (existsAfterDelete) {
                    logger.error("🚨 ЗАПИСЬ НЕ УДАЛИЛАСЬ! ID: {}", id);
                    throw new RuntimeException("Запись не удалилась из базы данных");
                }
            } else {
                String error = "Рабочий день с ID " + id + " не найден";
                logger.error("❌ {}", error);
                throw new RuntimeException(error);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("🚨 Ошибка при удалении рабочего дня с ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Ошибка удаления: " + e.getMessage(), e);
        }
    }

    // 📊 МЕТОДЫ ДЛЯ УЧЕТА ЗАРПЛАТЫ

    public SalaryPayment addSalaryPayment(BigDecimal amount, String description) {
        logger.info("💰 Добавление выплаты: {}, {}", amount, description);

        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Сумма выплаты должна быть положительной");
            }

            SalaryPayment payment = new SalaryPayment(amount, description);
            SalaryPayment saved = salaryPaymentRepository.save(payment);
            logger.info("✅ Успешно сохранена выплата с ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            logger.error("❌ Ошибка при добавлении выплаты: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения выплаты: " + e.getMessage(), e);
        }
    }

    public List<SalaryPayment> getAllSalaryPayments() {
        logger.info("📥 Получение всех выплат");
        try {
            List<SalaryPayment> payments = salaryPaymentRepository.findAllByOrderByPaymentDateDesc();
            logger.info("✅ Найдено {} выплат", payments.size());
            return payments;
        } catch (Exception e) {
            logger.error("❌ Ошибка при получении выплат: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения выплат: " + e.getMessage(), e);
        }
    }

    public void deleteSalaryPayment(Long id) {
        logger.info("🗑️ Удаление выплаты с ID: {}", id);
        try {
            if (salaryPaymentRepository.existsById(id)) {
                salaryPaymentRepository.deleteById(id);
                logger.info("✅ Успешно удалена выплата с ID: {}", id);
            } else {
                throw new RuntimeException("Выплата с ID " + id + " не найдена");
            }
        } catch (Exception e) {
            logger.error("❌ Ошибка при удалении выплаты: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка удаления выплаты: " + e.getMessage(), e);
        }
    }

    // Расчет общей суммы заработанного
    public BigDecimal getTotalEarned() {
        logger.debug("🧮 Расчет общего заработка");
        try {
            List<WorkDay> workDays = workDayRepository.findAll();
            BigDecimal total = workDays.stream()
                    .map(workDay -> BigDecimal.valueOf(workDay.getSalary()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            logger.debug("💰 Общий заработок: {}", total);
            return total;
        } catch (Exception e) {
            logger.error("❌ Ошибка при расчете общего заработка: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // Расчет общей суммы выплат
    public BigDecimal getTotalPaid() {
        logger.debug("🧮 Расчет общих выплат");
        try {
            List<SalaryPayment> payments = salaryPaymentRepository.findAll();
            BigDecimal total = payments.stream()
                    .map(SalaryPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            logger.debug("💰 Общие выплаты: {}", total);
            return total;
        } catch (Exception e) {
            logger.error("❌ Ошибка при расчете общих выплат: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // Расчет остатка (долг работодателя)
    public BigDecimal getSalaryBalance() {
        logger.debug("🧮 Расчет баланса зарплаты");
        try {
            BigDecimal totalEarned = getTotalEarned();
            BigDecimal totalPaid = getTotalPaid();
            BigDecimal balance = totalEarned.subtract(totalPaid);
            logger.debug("💰 Баланс зарплаты: {} (заработано: {}, выплачено: {})",
                    balance, totalEarned, totalPaid);
            return balance;
        } catch (Exception e) {
            logger.error("❌ Ошибка при расчете баланса зарплаты: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // 📈 ОБНОВЛЕННАЯ СТАТИСТИКА (объединенная версия)
    public WorkDayStatistics getStatistics() {
        logger.info("📊 Получение статистики");
        try {
            List<WorkDay> allDays = workDayRepository.findAll();
            int totalDays = allDays.size();
            BigDecimal totalEarned = getTotalEarned();
            BigDecimal totalPaid = getTotalPaid();
            BigDecimal salaryBalance = getSalaryBalance();

            logger.info("📊 Статистика: дней={}, заработано={}, выплачено={}, баланс={}",
                    totalDays, totalEarned, totalPaid, salaryBalance);

            return new WorkDayStatistics(totalDays, totalEarned, totalPaid, salaryBalance);
        } catch (Exception e) {
            logger.error("❌ Ошибка при получении статистики: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка получения статистики: " + e.getMessage(), e);
        }
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

    // 📍 ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ДИАГНОСТИКИ

    public String getServiceStatus() {
        try {
            long workDayCount = workDayRepository.count();
            long salaryPaymentCount = salaryPaymentRepository.count();
            return String.format("✅ Сервис работает. WorkDays: %d, SalaryPayments: %d",
                    workDayCount, salaryPaymentCount);
        } catch (Exception e) {
            return "❌ Ошибка сервиса: " + e.getMessage();
        }
    }

    public WorkDay createTestWorkDay() {
        LocalDate testDate = LocalDate.now().plusYears(1); // Уникальная дата
        WorkDay testDay = new WorkDay(testDate, "Тестовая запись из сервиса");
        return saveWorkDay(testDay);
    }
}