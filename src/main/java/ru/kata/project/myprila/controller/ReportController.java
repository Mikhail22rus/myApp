package ru.kata.project.myprila.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kata.project.myprila.dto.AnnualReportDTO;
import ru.kata.project.myprila.dto.FullDailyReportDTO;
import ru.kata.project.myprila.dto.MonthlyDetailedReportDTO;
import ru.kata.project.myprila.dto.MonthlyReportDTO;
import ru.kata.project.myprila.entity.WorkDay;
import ru.kata.project.myprila.repo.WorkDayReposytory;
import ru.kata.project.myprila.service.ReportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;



    // ✅ ОТЧЕТ ПО МЕСЯЦАМ
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyReportDTO>> getMonthlyReport(@RequestParam Long userId,
                                                                   @RequestParam(required = false) Integer year) {
        try {
            System.out.println("📊 GET /api/reports/monthly - отчет по месяцам для пользователя ID: " + userId);
            List<MonthlyReportDTO> report = reportService.getMonthlyReport(userId, year);
            System.out.println("✅ Отчет по месяцам готов, месяцев: " + report.size());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении месячного отчета: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ ОТЧЕТ ЗА ГОД
    @GetMapping("/annual")
    public ResponseEntity<AnnualReportDTO> getAnnualReport(@RequestParam Long userId,
                                                           @RequestParam(required = false) Integer year) {
        try {
            System.out.println("📊 GET /api/reports/annual - годовой отчет для пользователя ID: " + userId);
            AnnualReportDTO report = reportService.getAnnualReport(userId, year);
            System.out.println("✅ Годовой отчет готов за " + report.getYear() + " год");
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении годового отчета: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ ПОЛНЫЙ ОТЧЕТ ПО ДНЯМ ЗА ВСЕ ВРЕМЯ
    @GetMapping("/full-daily")
    public ResponseEntity<FullDailyReportDTO> getFullDailyReport(@RequestParam Long userId) {
        try {
            System.out.println("📊 GET /api/reports/full-daily - полный отчет по дням для пользователя ID: " + userId);
            FullDailyReportDTO report = reportService.getFullDailyReport(userId);
            System.out.println("✅ Полный отчет по дням готов, дней: " + report.getTotalDays());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении полного отчета по дням: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ ДЕТАЛЬНЫЙ ОТЧЕТ ПО МЕСЯЦУ
    @GetMapping("/monthly-detailed")
    public ResponseEntity<MonthlyDetailedReportDTO> getMonthlyDetailedReport(@RequestParam Long userId,
                                                                             @RequestParam Integer year,
                                                                             @RequestParam Integer month) {
        try {
            System.out.println("📊 GET /api/reports/monthly-detailed - детальный отчет за " + year + "-" + month);
            MonthlyDetailedReportDTO report = reportService.getMonthlyDetailedReport(userId, year, month);
            System.out.println("✅ Детальный отчет готов, дней: " + report.getDaysCount());
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.out.println("❌ Ошибка при получении детального отчета: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ ТЕСТОВЫЙ ЭНДПОИНТ
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("✅ ReportController тестовый endpoint вызван");
        return ResponseEntity.ok("Контроллер отчетов работает! 📊");
    }
}