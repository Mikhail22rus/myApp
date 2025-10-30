const API_BASE_URL = '/api';
const DEFAULT_SALARY = 3500;
let currentUser = null;

// Элементы DOM
const loginForm = document.getElementById('loginForm');
const mainContent = document.getElementById('mainContent');
const loginFormElement = document.getElementById('loginFormElement');
const currentUserNameSpan = document.getElementById('currentUserName');
const workdayForm = document.getElementById('workdayForm');
const salaryPaymentForm = document.getElementById('salaryPaymentForm');
const messageDiv = document.getElementById('message');
const loadWorkdaysBtn = document.getElementById('loadWorkdays');
const loadPaymentsBtn = document.getElementById('loadPayments');
const workdaysContainer = document.getElementById('workdaysContainer');
const paymentsContainer = document.getElementById('paymentsContainer');
const totalDaysSpan = document.getElementById('totalDays');
const totalEarnedSpan = document.getElementById('totalEarned');
const totalBonusSpan = document.getElementById('totalBonus');
const totalPaidSpan = document.getElementById('totalPaid');
const salaryBalanceSpan = document.getElementById('salaryBalance');
const tabs = document.querySelectorAll('.tab');
const tabContents = document.querySelectorAll('.tab-content');

// Элементы для отчетов
const reportTypeSelect = document.getElementById('reportType');
const reportYearSelect = document.getElementById('reportYear');
const reportMonthSelect = document.getElementById('reportMonth');
const monthField = document.getElementById('monthField');
const generateReportBtn = document.getElementById('generateReport');
const reportContainer = document.getElementById('reportContainer');
const reportTitle = document.getElementById('reportTitle');
const reportContent = document.getElementById('reportContent');
const exportReportBtn = document.getElementById('exportReport');

// ===== АВТОРИЗАЦИЯ =====
async function login(username, password) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const result = await response.json();

        if (result.success) {
            currentUser = result;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            showMainContent();
            showMessage(`Добро пожаловать, ${username}!`);
            return true;
        } else {
            showMessage(result.message || 'Ошибка входа', 'error');
            return false;
        }
    } catch (error) {
        showMessage('Ошибка соединения с сервером', 'error');
        return false;
    }
}

function logout() {
    currentUser = null;
    localStorage.removeItem('currentUser');
    showLoginForm();
    showMessage('Вы вышли из системы');
}

function checkAuth() {
    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
        currentUser = JSON.parse(savedUser);
        showMainContent();
    } else {
        showLoginForm();
    }
}

// ===== UI ФУНКЦИИ =====
function showMainContent() {
    loginForm.style.display = 'none';
    mainContent.style.display = 'block';
    currentUserNameSpan.textContent = currentUser.username;
    loadWorkdays();
    loadPayments();
    updateSummary();
    initReports();
}

function showLoginForm() {
    loginForm.style.display = 'block';
    mainContent.style.display = 'none';
    loginFormElement.reset();
}

function showMessage(text, type = 'success') {
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    setTimeout(() => { messageDiv.className = 'message'; }, 5000);
}

function formatDate(dateString) {
    try {
        return new Date(dateString).toLocaleDateString('ru-RU');
    } catch {
        return dateString;
    }
}

function formatMoney(amount) {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount;
    return isNaN(num) ? '0 ₽' : new Intl.NumberFormat('ru-RU').format(num) + ' ₽';
}

function safeNumber(value) {
    if (value === null || value === undefined) return 0;
    if (typeof value === 'number') return value;
    const num = parseFloat(value);
    return isNaN(num) ? 0 : num;
}

// ===== ФИНАНСОВАЯ СВОДКА ПО МЕСЯЦАМ С НАКОПЛЕННЫМ ДОЛГОМ =====
async function updateSummary() {
    if (!currentUser) return;
    try {
        // Загружаем данные для помесячной группировки с накопленным долгом
        const monthlyData = await loadMonthlySummary();
        displayMonthlySummary(monthlyData);

    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

async function loadMonthlySummary() {
    if (!currentUser) return [];

    try {
        // Получаем все рабочие дни
        const workdaysRes = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`);
        const workdays = await workdaysRes.json();

        // Получаем все выплаты
        const paymentsRes = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`);
        const payments = await paymentsRes.json();

        return calculateMonthlySummary(workdays, payments);
    } catch (error) {
        console.error('Ошибка загрузки данных для сводки:', error);
        return [];
    }
}

function calculateMonthlySummary(workdays, payments) {
    const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
        'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

    // Группируем рабочие дни по месяцам
    const workdaysByMonth = {};
    workdays.forEach(day => {
        const date = new Date(day.workDate);
        const year = date.getFullYear();
        const month = date.getMonth();
        const key = `${year}-${month}`;

        if (!workdaysByMonth[key]) {
            workdaysByMonth[key] = {
                year: year,
                month: month,
                monthName: `${monthNames[month]} ${year}`,
                workdays: [],
                totalSalary: 0,
                totalBonus: 0,
                totalIncome: 0,
                daysCount: 0
            };
        }

        workdaysByMonth[key].workdays.push(day);
        workdaysByMonth[key].totalSalary += day.salary || 0;
        workdaysByMonth[key].totalBonus += day.bonus || 0;
        workdaysByMonth[key].totalIncome += (day.salary || 0) + (day.bonus || 0);
        workdaysByMonth[key].daysCount++;
    });

    // Группируем выплаты по месяцам
    const paymentsByMonth = {};
    payments.forEach(payment => {
        const date = new Date(payment.paymentDate);
        const year = date.getFullYear();
        const month = date.getMonth();
        const key = `${year}-${month}`;

        if (!paymentsByMonth[key]) {
            paymentsByMonth[key] = {
                totalPaid: 0,
                payments: []
            };
        }

        paymentsByMonth[key].totalPaid += payment.amount || 0;
        paymentsByMonth[key].payments.push(payment);
    });

    // Собираем итоговые данные по месяцам с накоплением долга
    const monthlyData = [];
    let cumulativeBalance = 0; // Накопленный долг

    // Сортируем месяцы по дате (от старых к новым)
    const sortedMonths = Object.keys(workdaysByMonth).sort();

    sortedMonths.forEach(key => {
        const workMonth = workdaysByMonth[key];
        const paymentMonth = paymentsByMonth[key] || { totalPaid: 0, payments: [] };

        // Заработок за месяц (только зарплата, без бонусов)
        const monthlySalary = workMonth.totalSalary;

        // Выплаты за месяц
        const monthlyPaid = paymentMonth.totalPaid;

        // Баланс за месяц (зарплата - выплаты)
        const monthlyBalance = monthlySalary - monthlyPaid;

        // Накопленный долг с учетом предыдущих месяцев
        cumulativeBalance += monthlyBalance;

        monthlyData.push({
            ...workMonth,
            totalPaid: monthlyPaid,
            monthlyBalance: monthlyBalance,
            cumulativeBalance: cumulativeBalance,
            payments: paymentMonth.payments
        });
    });

    return monthlyData.reverse(); // Возвращаем от новых к старым
}

function displayMonthlySummary(monthlyData) {
    const summaryContainer = document.querySelector('.summary-numbers');

    if (!monthlyData || monthlyData.length === 0) {
        summaryContainer.innerHTML = `
            <div class="summary-item" style="grid-column: 1 / -1;">
                <div class="summary-value">-</div>
                <div class="summary-label">Нет данных за периоды</div>
            </div>
        `;
        return;
    }

    // Показываем только последние 3 месяца или все, если меньше
    const displayMonths = monthlyData.slice(0, 3);

    let html = '';

    displayMonths.forEach(month => {
        const balanceClass = month.cumulativeBalance > 0 ? 'balance-positive' :
            month.cumulativeBalance < 0 ? 'balance-negative' : '';

        html += `
            <div class="summary-item">
                <div class="summary-value" style="font-size: 0.9rem; margin-bottom: 2px;">${month.monthName}</div>
                <div class="summary-value">${formatMoney(month.totalIncome)}</div>
                <div class="summary-label">
                    ${month.daysCount} дней • 
                    <span class="${balanceClass}">${formatMoney(month.cumulativeBalance)}</span>
                </div>
            </div>
        `;
    });

    // Если есть больше 3 месяцев, показываем общую сводку
    if (monthlyData.length > 3) {
        const totalMonths = monthlyData.length;
        const totalIncome = monthlyData.reduce((sum, month) => sum + month.totalIncome, 0);
        const currentBalance = monthlyData[0].cumulativeBalance; // Баланс последнего месяца

        html += `
            <div class="summary-item">
                <div class="summary-value" style="font-size: 0.9rem; margin-bottom: 2px;">Всего (${totalMonths} мес)</div>
                <div class="summary-value">${formatMoney(totalIncome)}</div>
                <div class="summary-label">
                    Итоговый баланс: 
                    <span class="${currentBalance > 0 ? 'balance-positive' : currentBalance < 0 ? 'balance-negative' : ''}">
                        ${formatMoney(currentBalance)}
                    </span>
                </div>
            </div>
        `;
    }

    summaryContainer.innerHTML = html;
}

// ===== СВОРАЧИВАЕМЫЙ СПИСОК ДНЕЙ =====
function initializeCollapsibleDays() {
    const workdaysContainer = document.getElementById('workdaysContainer');
    const loadWorkdaysBtn = document.getElementById('loadWorkdays');

    // Создаем контейнер для кнопок
    let controlsContainer = document.getElementById('workdaysControls');
    if (!controlsContainer) {
        controlsContainer = document.createElement('div');
        controlsContainer.id = 'workdaysControls';
        controlsContainer.className = 'workdays-controls';

        // Вставляем контейнер перед контейнером дней
        const workdaysList = workdaysContainer.parentNode;
        workdaysList.insertBefore(controlsContainer, workdaysContainer);

        // Перемещаем кнопку "Обновить" в контейнер
        controlsContainer.appendChild(loadWorkdaysBtn);
    }

    // Проверяем, есть ли уже кнопка сворачивания
    if (!document.getElementById('toggleDaysBtn')) {
        // Создаем кнопку для сворачивания/разворачивания
        const toggleBtn = document.createElement('button');
        toggleBtn.id = 'toggleDaysBtn';
        toggleBtn.type = 'button';
        toggleBtn.className = 'btn btn-info toggle-btn';
        toggleBtn.innerHTML = '📂 Свернуть список дней';
        toggleBtn.onclick = toggleWorkdaysList;

        // Вставляем кнопку в контейнер управления
        controlsContainer.appendChild(toggleBtn);
    }

    // Добавляем класс для анимации
    workdaysContainer.classList.add('collapsible');
}

function toggleWorkdaysList() {
    const workdaysContainer = document.getElementById('workdaysContainer');
    const toggleBtn = document.getElementById('toggleDaysBtn');

    if (workdaysContainer.classList.contains('collapsed')) {
        // Разворачиваем
        workdaysContainer.classList.remove('collapsed');
        toggleBtn.innerHTML = '📂 Свернуть список дней';
        toggleBtn.classList.remove('collapsed');
    } else {
        // Сворачиваем
        workdaysContainer.classList.add('collapsed');
        toggleBtn.innerHTML = '📁 Развернуть список дней';
        toggleBtn.classList.add('collapsed');
    }
}

// ===== API ФУНКЦИИ =====
async function loadWorkdays() {
    if (!currentUser) return;
    try {
        workdaysContainer.innerHTML = '<div class="loading">Загрузка...</div>';
        const res = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`);
        if (res.ok) {
            const workdays = await res.json();
            if (!workdays.length) {
                workdaysContainer.innerHTML = '<div class="empty-state">📭 Нет рабочих дней</div>';
                initializeCollapsibleDays();
                return;
            }

            // Получаем данные для отображения баланса
            const monthlyData = await loadMonthlySummary();
            displayWorkdaysWithBalance(workdays, monthlyData);

            // Инициализируем сворачивание после загрузки данных
            initializeCollapsibleDays();
        }
    } catch (e) {
        workdaysContainer.innerHTML = '<div class="loading">Ошибка загрузки</div>';
    }
}

function displayWorkdaysWithBalance(workdays, monthlyData) {
    const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];

    // Создаем карту балансов по месяцам для быстрого доступа
    const balanceMap = {};
    monthlyData.forEach(month => {
        const key = `${month.year}-${month.month}`;
        balanceMap[key] = month.cumulativeBalance;
    });

    workdays.sort((a, b) => new Date(b.workDate) - new Date(a.workDate));

    const monthGroups = {};

    workdays.forEach(day => {
        const date = new Date(day.workDate);
        const year = date.getFullYear();
        const month = date.getMonth();
        const key = `${year}-${month}`;

        if (!monthGroups[key]) {
            const monthBalance = balanceMap[key] || 0;
            monthGroups[key] = {
                monthName: `${monthNames[month]} ${year}`,
                days: [],
                totalSalary: 0,
                totalBonus: 0,
                daysCount: 0,
                cumulativeBalance: monthBalance
            };
        }

        monthGroups[key].days.push(day);
        monthGroups[key].totalSalary += day.salary;
        monthGroups[key].totalBonus += (day.bonus || 0);
        monthGroups[key].daysCount++;
    });

    const sortedGroups = Object.entries(monthGroups).sort(([a],[b]) => b.localeCompare(a));
    workdaysContainer.innerHTML = '';

    sortedGroups.forEach(([_, group]) => {
        const div = document.createElement('div');
        div.className = 'month-group';
        const totalIncome = group.totalSalary + group.totalBonus;
        const balanceClass = group.cumulativeBalance > 0 ? 'balance-positive' :
            group.cumulativeBalance < 0 ? 'balance-negative' : '';

        div.innerHTML = `
            <div class="month-header">
                <span>${group.monthName}</span>
                <div style="display: flex; flex-direction: column; align-items: flex-end; gap: 4px;">
                    <span class="month-total">${group.daysCount} дней • ${formatMoney(totalIncome)}</span>
                    <span style="font-size: 0.8rem; opacity: 0.9;">
                        Баланс: <span class="${balanceClass}">${formatMoney(group.cumulativeBalance)}</span>
                    </span>
                </div>
            </div>
            <div class="month-days">
                ${group.days.map(day => {
            const bonusHtml = (day.bonus && day.bonus > 0) ? `<span class="workday-bonus">+${formatMoney(day.bonus)} бонус</span>` : '';
            return `
                    <div class="workday-card">
                        <div class="workday-info">
                            <div class="workday-date">
                                📅 ${formatDate(day.workDate)}
                                <span class="workday-salary">${formatMoney(day.salary)}</span>
                                ${bonusHtml}
                            </div>
                            <div class="workday-description">${day.description || 'Рабочий день'}</div>
                        </div>
                        <div class="workday-actions">
                            <button class="btn btn-danger" onclick="deleteWorkday(${day.id})">🗑️ Удалить</button>
                        </div>
                    </div>
                `}).join('')}
            </div>
        `;
        workdaysContainer.appendChild(div);
    });
}

async function loadPayments() {
    if (!currentUser) return;
    try {
        paymentsContainer.innerHTML = '<div class="loading">Загрузка...</div>';
        const res = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`);
        if (res.ok) {
            const payments = await res.json();
            if (!payments.length) {
                paymentsContainer.innerHTML = '<div class="empty-state">💸 Нет выплат</div>';
                return;
            }
            payments.sort((a,b) => new Date(b.paymentDate) - new Date(a.paymentDate));
            paymentsContainer.innerHTML = payments.map(p => `
                <div class="payment-card">
                    <div class="payment-info">
                        <div class="payment-date">💵 ${formatDate(p.paymentDate)} <span class="payment-amount">${formatMoney(p.amount)}</span></div>
                        <div class="payment-description">${p.description || 'Выплата'}</div>
                    </div>
                    <div class="payment-actions">
                        <button class="btn btn-danger" onclick="deletePayment(${p.id})">🗑️ Удалить</button>
                    </div>
                </div>
            `).join('');
        }
    } catch(e) {
        paymentsContainer.innerHTML = '<div class="loading">Ошибка загрузки</div>';
    }
}

async function addWorkday(workdayData) {
    if (!currentUser) return;
    try {
        workdayData.bonus = parseInt(workdayData.bonus) || 0;

        const res = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`, {
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify(workdayData)
        });
        if (res.ok) {
            const saved = await res.json();
            const bonusText = (saved.bonus && saved.bonus > 0) ? ` + ${formatMoney(saved.bonus)} допдоход` : '';
            showMessage(`День добавлен! +${formatMoney(saved.salary)}${bonusText}`);
            workdayForm.reset();
            document.getElementById('workDate').value = new Date().toISOString().split('T')[0];
            loadWorkdays();
            updateSummary();
        } else {
            const error = await res.json();
            showMessage(error.message || 'Ошибка', 'error');
        }
    } catch(e){ showMessage('Ошибка соединения', 'error'); }
}

async function addSalaryPayment(paymentData) {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`, {
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify(paymentData)
        });
        if (res.ok) {
            showMessage(`Выплата ${formatMoney(paymentData.amount)} добавлена!`);
            salaryPaymentForm.reset();
            loadPayments();
            updateSummary();
        } else {
            const error = await res.json();
            showMessage(error.message || 'Ошибка', 'error');
        }
    } catch(e){ showMessage('Ошибка соединения', 'error'); }
}

async function deleteWorkday(id) {
    if (!currentUser || !confirm('Удалить день?')) return;
    try {
        const res = await fetch(`${API_BASE_URL}/workdays/${id}?userId=${currentUser.userId}`, { method:'DELETE' });
        if (res.ok) { showMessage('День удален'); loadWorkdays(); updateSummary(); }
    } catch(e){ showMessage('Ошибка удаления', 'error'); }
}

async function deletePayment(id) {
    if (!currentUser || !confirm('Удалить выплату?')) return;
    try {
        const res = await fetch(`${API_BASE_URL}/payments/${id}?userId=${currentUser.userId}`, { method:'DELETE' });
        if (res.ok) { showMessage('Выплата удалена'); loadPayments(); updateSummary(); }
    } catch(e){ showMessage('Ошибка удаления', 'error'); }
}

// ===== ОТЧЕТЫ =====
function initReports() {
    initYearSelector();
    setupReportEventListeners();
}

function initYearSelector() {
    const currentYear = new Date().getFullYear();
    reportYearSelect.innerHTML = '';

    for (let year = currentYear - 2; year <= currentYear + 2; year++) {
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year;
        if (year === currentYear) {
            option.selected = true;
        }
        reportYearSelect.appendChild(option);
    }
}

function setupReportEventListeners() {
    reportTypeSelect.addEventListener('change', function() {
        const isDetailedReport = this.value === 'monthly-detailed';
        monthField.style.display = isDetailedReport ? 'block' : 'none';
    });

    generateReportBtn.addEventListener('click', generateReport);
    exportReportBtn.addEventListener('click', exportReport);
}

async function generateReport() {
    if (!currentUser) return;

    const reportType = reportTypeSelect.value;
    const year = parseInt(reportYearSelect.value);
    const month = parseInt(reportMonthSelect.value);

    try {
        showMessage('Формирование отчета...', 'success');
        generateReportBtn.disabled = true;

        let url = `${API_BASE_URL}/reports/${reportType}?userId=${currentUser.userId}&year=${year}`;

        if (reportType === 'monthly-detailed') {
            url += `&month=${month}`;
        }

        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Ошибка загрузки отчета');
        }

        const reportData = await response.json();
        displayReport(reportType, reportData, year, month);

    } catch (error) {
        console.error('Ошибка при формировании отчета:', error);
        showMessage('Ошибка при формировании отчета', 'error');
    } finally {
        generateReportBtn.disabled = false;
    }
}

function displayReport(reportType, data, year, month) {
    reportContainer.style.display = 'block';
    reportContent.innerHTML = '';

    switch (reportType) {
        case 'monthly':
            displayMonthlyReport(data, year);
            break;
        case 'annual':
            displayAnnualReport(data, year);
            break;
        case 'monthly-detailed':
            displayMonthlyDetailedReport(data, year, month);
            break;
    }
}

function displayMonthlyReport(data, year) {
    reportTitle.textContent = `Отчет по месяцам за ${year} год`;

    const monthNames = {
        0: 'Январь', 1: 'Февраль', 2: 'Март', 3: 'Апрель', 4: 'Май', 5: 'Июнь',
        6: 'Июль', 7: 'Август', 8: 'Сентябрь', 9: 'Октябрь', 10: 'Ноябрь', 11: 'Декабрь'
    };

    let html = '<div class="month-cards">';

    data.forEach(monthReport => {
        const hasData = monthReport.daysCount > 0;
        const monthName = monthNames[monthReport.monthValue - 1] || monthReport.month;

        html += `
            <div class="month-card ${hasData ? 'active' : ''}">
                <div class="month-card-header">
                    <span class="month-name">${monthName}</span>
                    <span class="month-days-count">${monthReport.daysCount} дней</span>
                </div>
                <div class="month-stats">
                    <div class="stat-item">
                        <div class="stat-value">${formatMoney(monthReport.totalSalary)}</div>
                        <div class="stat-label">Зарплата</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">${formatMoney(monthReport.totalBonus)}</div>
                        <div class="stat-label">Бонусы</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">${formatMoney(monthReport.totalIncome)}</div>
                        <div class="stat-label">Всего</div>
                    </div>
                </div>
            </div>
        `;
    });

    html += '</div>';
    reportContent.innerHTML = html;
}

function displayAnnualReport(data, year) {
    reportTitle.textContent = `Годовой отчет за ${year} год`;

    let html = `
        <div class="annual-summary">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px;">
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${data.totalDays}</div>
                    <div class="stat-label">Всего дней</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalSalary)}</div>
                    <div class="stat-label">Общая зарплата</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalBonus)}</div>
                    <div class="stat-label">Общие бонусы</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalIncome)}</div>
                    <div class="stat-label">Общий доход</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.averageMonthlyIncome)}</div>
                    <div class="stat-label">Средний доход в месяц</div>
                </div>
            </div>
        </div>
        <h3 style="margin-bottom: 15px;">Детализация по месяцам</h3>
        <table class="report-table">
            <thead>
                <tr>
                    <th>Месяц</th>
                    <th>Дней</th>
                    <th>Зарплата</th>
                    <th>Бонусы</th>
                    <th>Всего</th>
                </tr>
            </thead>
            <tbody>
    `;

    data.monthlyDetails.forEach(month => {
        const monthNames = {
            'JANUARY': 'Январь', 'FEBRUARY': 'Февраль', 'MARCH': 'Март', 'APRIL': 'Апрель',
            'MAY': 'Май', 'JUNE': 'Июнь', 'JULY': 'Июль', 'AUGUST': 'Август',
            'SEPTEMBER': 'Сентябрь', 'OCTOBER': 'Октябрь', 'NOVEMBER': 'Ноябрь', 'DECEMBER': 'Декабрь'
        };

        const monthName = monthNames[month.month] || month.month;

        html += `
            <tr>
                <td>${monthName}</td>
                <td>${month.daysCount}</td>
                <td>${formatMoney(month.totalSalary)}</td>
                <td>${formatMoney(month.totalBonus)}</td>
                <td><strong>${formatMoney(month.totalIncome)}</strong></td>
            </tr>
        `;
    });

    html += `
            </tbody>
        </table>
    `;

    reportContent.innerHTML = html;
}

function displayMonthlyDetailedReport(data, year, month) {
    const monthNames = {
        1: 'Январь', 2: 'Февраль', 3: 'Март', 4: 'Апрель', 5: 'Май', 6: 'Июнь',
        7: 'Июль', 8: 'Август', 9: 'Сентябрь', 10: 'Октябрь', 11: 'Ноябрь', 12: 'Декабрь'
    };

    const monthName = monthNames[month];
    reportTitle.textContent = `Детальный отчет за ${monthName} ${year} года`;

    let html = `
        <div class="month-summary" style="margin-bottom: 20px;">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;">
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${data.daysCount}</div>
                    <div class="stat-label">Рабочих дней</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalSalary)}</div>
                    <div class="stat-label">Зарплата</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalBonus)}</div>
                    <div class="stat-label">Бонусы</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalIncome)}</div>
                    <div class="stat-label">Всего доход</div>
                </div>
            </div>
        </div>
        <h3 style="margin-bottom: 15px;">Детализация по дням</h3>
    `;

    if (data.workDays && data.workDays.length > 0) {
        data.workDays.forEach(day => {
            html += `
                <div class="detailed-day">
                    <div class="day-header">
                        <span class="day-date">${formatDate(day.workDate)}</span>
                        <div class="day-income">
                            <span class="workday-salary">${formatMoney(day.salary)}</span>
                            ${day.bonus > 0 ? `<span class="workday-bonus">+${formatMoney(day.bonus)}</span>` : ''}
                        </div>
                    </div>
                    <div class="day-description">${day.description || 'Рабочий день'}</div>
                </div>
            `;
        });
    } else {
        html += '<div class="empty-state"><div>📭</div><h3>Нет рабочих дней</h3><p>За выбранный месяц не было рабочих дней</p></div>';
    }

    reportContent.innerHTML = html;
}

function exportReport() {
    const reportTitleText = reportTitle.textContent;
    const reportContentHtml = reportContent.innerHTML;

    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = `
        <h1>${reportTitleText}</h1>
        <div>${reportContentHtml}</div>
        <div style="margin-top: 20px; font-size: 12px; color: #666;">
            Сгенерировано: ${new Date().toLocaleString('ru-RU')}
        </div>
    `;

    const printWindow = window.open('', '_blank');
    printWindow.document.write(`
        <html>
            <head>
                <title>${reportTitleText}</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                    th { background-color: #f5f5f5; }
                    .month-card { border: 1px solid #ddd; padding: 15px; margin-bottom: 10px; border-radius: 5px; }
                    .stat-item { text-align: center; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                ${tempDiv.innerHTML}
            </body>
        </html>
    `);
    printWindow.document.close();
    printWindow.print();
}

// ===== ОБРАБОТЧИКИ СОБЫТИЙ =====
loginFormElement.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = new FormData(loginFormElement);
    await login(data.get('username'), data.get('password'));
});

workdayForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = new FormData(workdayForm);
    const salary = data.get('salary') || DEFAULT_SALARY;
    const bonus = data.get('bonus') || 0;

    await addWorkday({
        workDate: data.get('workDate'),
        description: data.get('description'),
        salary: parseInt(salary),
        bonus: parseInt(bonus)
    });
});

salaryPaymentForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = new FormData(salaryPaymentForm);
    await addSalaryPayment({
        amount: parseFloat(data.get('amount')),
        description: data.get('paymentDescription')
    });
});

loadWorkdaysBtn.addEventListener('click', loadWorkdays);
loadPaymentsBtn.addEventListener('click', loadPayments);

tabs.forEach(tab => {
    tab.addEventListener('click', () => {
        tabs.forEach(t => t.classList.remove('active'));
        tabContents.forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById(`${tab.getAttribute('data-tab')}Tab`).classList.add('active');
    });
});

// ===== ИНИЦИАЛИЗАЦИЯ =====
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    document.getElementById('workDate').value = new Date().toISOString().split('T')[0];
});

// ===== ГЛОБАЛЬНЫЕ ФУНКЦИИ =====
window.deleteWorkday = deleteWorkday;
window.deletePayment = deletePayment;
window.logout = logout;
window.toggleWorkdaysList = toggleWorkdaysList;