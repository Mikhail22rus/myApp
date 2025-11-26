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
const closeReportBtn = document.getElementById('closeReport');

// Переменные для управления состоянием
let isWorkdaysListCollapsed = true;
let isPaymentsListCollapsed = true;

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
    setTimeout(() => { messageDiv.className = 'message'; }, 2000);
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

// ===== ФИНАНСОВАЯ СВОДКА ТЕКУЩЕГО МЕСЯЦА =====
async function updateSummary() {
    if (!currentUser) return;
    try {
        const currentMonthData = await loadCurrentMonthSummary();
        displayCurrentMonthSummary(currentMonthData);
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
        showMessage('Ошибка загрузки финансовой сводки', 'error');
    }
}

async function loadCurrentMonthSummary() {
    if (!currentUser) return null;

    try {
        const workdaysRes = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`);
        const workdays = await workdaysRes.json();

        const paymentsRes = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`);
        const payments = await paymentsRes.json();

        return calculateCurrentMonthSummary(workdays, payments);
    } catch (error) {
        console.error('Ошибка загрузки данных для сводки:', error);
        return null;
    }
}

function calculateCurrentMonthSummary(workdays, payments) {
    const currentDate = new Date();
    const currentYear = currentDate.getFullYear();
    const currentMonth = currentDate.getMonth();
    const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
        'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

    const currentMonthWorkdays = workdays.filter(day => {
        const date = new Date(day.workDate);
        return date.getFullYear() === currentYear && date.getMonth() === currentMonth;
    });

    const currentMonthPayments = payments.filter(payment => {
        const date = new Date(payment.paymentDate);
        return date.getFullYear() === currentYear && date.getMonth() === currentMonth;
    });

    const currentMonthSalary = currentMonthWorkdays.reduce((sum, day) => sum + (day.salary || 0), 0);
    const currentMonthBonus = currentMonthWorkdays.reduce((sum, day) => sum + (day.bonus || 0), 0);
    const currentMonthIncome = currentMonthSalary + currentMonthBonus;
    const currentMonthPaid = currentMonthPayments.reduce((sum, payment) => sum + (payment.amount || 0), 0);

    const previousDebt = calculatePreviousMonthsDebt(workdays, payments, currentYear, currentMonth);
    const currentBalance = previousDebt + (currentMonthSalary - currentMonthPaid);

    return {
        monthName: `${monthNames[currentMonth]} ${currentYear}`,
        daysCount: currentMonthWorkdays.length,
        totalSalary: currentMonthSalary,
        totalBonus: currentMonthBonus,
        totalIncome: currentMonthIncome,
        totalPaid: currentMonthPaid,
        previousDebt: previousDebt,
        currentBalance: currentBalance
    };
}

function calculatePreviousMonthsDebt(workdays, payments, currentYear, currentMonth) {
    let totalDebt = 0;

    for (let year = 2020; year <= currentYear; year++) {
        const maxMonth = (year === currentYear) ? currentMonth - 1 : 11;

        for (let month = 0; month <= maxMonth; month++) {
            const monthWorkdays = workdays.filter(day => {
                const date = new Date(day.workDate);
                return date.getFullYear() === year && date.getMonth() === month;
            });

            const monthPayments = payments.filter(payment => {
                const date = new Date(payment.paymentDate);
                return date.getFullYear() === year && date.getMonth() === month;
            });

            const monthSalary = monthWorkdays.reduce((sum, day) => sum + (day.salary || 0), 0);
            const monthPaid = monthPayments.reduce((sum, payment) => sum + (payment.amount || 0), 0);
            const monthBalance = monthSalary - monthPaid;

            totalDebt += monthBalance;
        }
    }

    return totalDebt;
}

function displayCurrentMonthSummary(data) {
    if (!data) {
        totalDaysSpan.textContent = '0';
        totalEarnedSpan.textContent = '0 ₽';
        totalBonusSpan.textContent = '0 ₽';
        totalPaidSpan.textContent = '0 ₽';
        salaryBalanceSpan.textContent = '0 ₽';
        return;
    }

    totalDaysSpan.textContent = data.daysCount;
    totalEarnedSpan.textContent = formatMoney(data.totalIncome);
    totalBonusSpan.textContent = formatMoney(data.totalBonus);
    totalPaidSpan.textContent = formatMoney(data.totalPaid);

    salaryBalanceSpan.textContent = formatMoney(data.currentBalance);
    salaryBalanceSpan.className = `summary-value ${data.currentBalance > 0 ? 'balance-positive' : data.currentBalance < 0 ? 'balance-negative' : ''}`;

    document.querySelector('.summary-card h2').textContent = `📊 ${data.monthName}`;
}

// ===== РАБОЧИЕ ДНИ =====
async function loadWorkdays() {
    if (!currentUser) return;

    try {
        workdaysContainer.innerHTML = '<div class="loading">Загрузка...</div>';

        const res = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`);
        if (!res.ok) throw new Error('Ошибка загрузки');

        const workdays = await res.json();

        if (!workdays.length) {
            workdaysContainer.innerHTML = '<div class="empty-state">📭 Нет рабочих дней</div>';
            initializeCollapsibleDays();
            return;
        }

        const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];

        const monthGroupsArray = workdays.reduce((acc, day) => {
            const date = new Date(day.workDate);
            const year = date.getFullYear();
            const month = date.getMonth();
            const monthKey = year * 12 + month;

            let group = acc.find(g => g.year === year && g.month === month);
            if (!group) {
                group = {
                    year,
                    month,
                    monthKey,
                    monthName: `${monthNames[month]} ${year}`,
                    days: [],
                    totalSalary: 0,
                    totalBonus: 0,
                    daysCount: 0
                };
                acc.push(group);
            }

            group.days.push(day);
            group.totalSalary += day.salary;
            group.totalBonus += day.bonus || 0;
            group.daysCount++;

            return acc;
        }, []);

        monthGroupsArray.sort((a, b) => b.monthKey - a.monthKey);

        workdaysContainer.innerHTML = '';

        monthGroupsArray.reverse().forEach(group => {
            const div = document.createElement('div');
            div.className = 'month-group';

            const totalIncome = group.totalSalary + group.totalBonus;

            group.days.sort((a, b) => new Date(b.workDate) - new Date(a.workDate));

            div.innerHTML = `
                <div class="month-header">
                    <span>${group.monthName}</span>
                    <span class="month-total">${group.daysCount} дней • ${formatMoney(totalIncome)}</span>
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
                        `;
            }).join('')}
                </div>
            `;

            workdaysContainer.prepend(div);
        });

        initializeCollapsibleDays();

    } catch (e) {
        console.error(e);
        workdaysContainer.innerHTML = '<div class="loading">Ошибка загрузки</div>';
    }
}

function initializeCollapsibleDays() {
    const workdaysContainer = document.getElementById('workdaysContainer');
    const loadWorkdaysBtn = document.getElementById('loadWorkdays');

    let controlsContainer = document.getElementById('workdaysControls');
    if (!controlsContainer) {
        controlsContainer = document.createElement('div');
        controlsContainer.id = 'workdaysControls';
        controlsContainer.className = 'workdays-controls';

        const workdaysList = workdaysContainer.parentNode;
        workdaysList.insertBefore(controlsContainer, workdaysContainer);
        controlsContainer.appendChild(loadWorkdaysBtn);
    }

    if (!document.getElementById('toggleDaysBtn')) {
        const toggleBtn = document.createElement('button');
        toggleBtn.id = 'toggleDaysBtn';
        toggleBtn.type = 'button';
        toggleBtn.className = 'btn btn-info toggle-btn';
        toggleBtn.innerHTML = '📁 Развернуть список дней';
        toggleBtn.onclick = toggleWorkdaysList;

        controlsContainer.appendChild(toggleBtn);
    }

    workdaysContainer.classList.add('collapsible', 'collapsed');
    isWorkdaysListCollapsed = true;
}

function toggleWorkdaysList() {
    const workdaysContainer = document.getElementById('workdaysContainer');
    const toggleBtn = document.getElementById('toggleDaysBtn');

    if (isWorkdaysListCollapsed) {
        workdaysContainer.classList.remove('collapsed');
        toggleBtn.innerHTML = '📂 Свернуть список дней';
        isWorkdaysListCollapsed = false;
    } else {
        workdaysContainer.classList.add('collapsed');
        toggleBtn.innerHTML = '📁 Развернуть список дней';
        isWorkdaysListCollapsed = true;
    }
}

// ===== ВЫПЛАТЫ =====
async function loadPayments() {
    if (!currentUser) {
        console.log('❌ Нет текущего пользователя');
        return;
    }

    try {
        console.log('🔄 Начало загрузки выплат для пользователя:', currentUser.userId);
        paymentsContainer.innerHTML = '<div class="loading">Загрузка...</div>';

        const response = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`);
        console.log('📡 Ответ сервера:', response.status, response.statusText);

        if (response.ok) {
            const payments = await response.json();
            console.log('✅ Получены выплаты:', payments);

            if (!payments || !payments.length) {
                paymentsContainer.innerHTML = '<div class="empty-state">💸 Нет выплат</div>';
                console.log('ℹ️ Нет выплат для отображения');
                initializeCollapsiblePayments();
                return;
            }

            // Группируем выплаты по месяцам
            const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];

            const monthGroupsArray = payments.reduce((acc, payment) => {
                // Исправляем преобразование даты
                let paymentDate;
                if (payment.paymentDate && typeof payment.paymentDate === 'string') {
                    paymentDate = new Date(payment.paymentDate);
                } else {
                    paymentDate = new Date();
                }

                const year = paymentDate.getFullYear();
                const month = paymentDate.getMonth();
                const monthKey = year * 12 + month;

                let group = acc.find(g => g.year === year && g.month === month);
                if (!group) {
                    group = {
                        year,
                        month,
                        monthKey,
                        monthName: `${monthNames[month]} ${year}`,
                        payments: [],
                        totalAmount: 0,
                        paymentsCount: 0
                    };
                    acc.push(group);
                }

                group.payments.push(payment);
                group.totalAmount += parseFloat(payment.amount) || 0;
                group.paymentsCount++;

                return acc;
            }, []);

            // Сортируем по дате (новые сверху)
            monthGroupsArray.sort((a, b) => b.monthKey - a.monthKey);

            paymentsContainer.innerHTML = '';

            if (monthGroupsArray.length === 0) {
                paymentsContainer.innerHTML = '<div class="empty-state">💸 Нет выплат</div>';
                initializeCollapsiblePayments();
                return;
            }

            monthGroupsArray.forEach(group => {
                const div = document.createElement('div');
                div.className = 'month-group';

                // Сортируем выплаты внутри месяца (новые сверху)
                group.payments.sort((a, b) => {
                    const dateA = new Date(a.paymentDate);
                    const dateB = new Date(b.paymentDate);
                    return dateB - dateA;
                });

                div.innerHTML = `
                    <div class="month-header">
                        <span>${group.monthName}</span>
                        <span class="month-total">${group.paymentsCount} выплат • ${formatMoney(group.totalAmount)}</span>
                    </div>
                    <div class="month-payments">
                        ${group.payments.map(payment => {
                    const paymentDate = payment.paymentDate ? formatDate(payment.paymentDate) : 'Неизвестная дата';
                    const amount = parseFloat(payment.amount) || 0;
                    return `
                                <div class="payment-card">
                                    <div class="payment-info">
                                        <div class="payment-date">
                                            💵 ${paymentDate} 
                                            <span class="payment-amount">${formatMoney(amount)}</span>
                                        </div>
                                        <div class="payment-description">${payment.description || 'Выплата'}</div>
                                    </div>
                                    <div class="payment-actions">
                                        <button class="btn btn-danger" onclick="deletePayment(${payment.id})">🗑️ Удалить</button>
                                    </div>
                                </div>
                            `;
                }).join('')}
                    </div>
                `;

                paymentsContainer.appendChild(div);
            });

            console.log('✅ Выплаты успешно отображены');
            initializeCollapsiblePayments();

        } else {
            console.error('❌ Ошибка HTTP:', response.status);
            paymentsContainer.innerHTML = '<div class="loading">Ошибка загрузки выплат</div>';
        }
    } catch(error) {
        console.error('❌ Ошибка при загрузке выплат:', error);
        paymentsContainer.innerHTML = '<div class="loading">Ошибка соединения</div>';
    }
}

function initializeCollapsiblePayments() {
    const paymentsContainer = document.getElementById('paymentsContainer');
    const loadPaymentsBtn = document.getElementById('loadPayments');

    let controlsContainer = document.getElementById('paymentsControls');
    if (!controlsContainer) {
        controlsContainer = document.createElement('div');
        controlsContainer.id = 'paymentsControls';
        controlsContainer.className = 'payments-controls';

        const paymentsList = paymentsContainer.parentNode;
        paymentsList.insertBefore(controlsContainer, paymentsContainer);
        controlsContainer.appendChild(loadPaymentsBtn);
    }

    if (!document.getElementById('togglePaymentsBtn')) {
        const toggleBtn = document.createElement('button');
        toggleBtn.id = 'togglePaymentsBtn';
        toggleBtn.type = 'button';
        toggleBtn.className = 'btn btn-info toggle-btn';
        toggleBtn.innerHTML = '📁 Развернуть список выплат';
        toggleBtn.onclick = togglePaymentsList;

        controlsContainer.appendChild(toggleBtn);
    }

    paymentsContainer.classList.add('collapsible', 'collapsed');
    isPaymentsListCollapsed = true;
}

function togglePaymentsList() {
    const paymentsContainer = document.getElementById('paymentsContainer');
    const toggleBtn = document.getElementById('togglePaymentsBtn');

    if (isPaymentsListCollapsed) {
        paymentsContainer.classList.remove('collapsed');
        toggleBtn.innerHTML = '📂 Свернуть список выплат';
        isPaymentsListCollapsed = false;
    } else {
        paymentsContainer.classList.add('collapsed');
        toggleBtn.innerHTML = '📁 Развернуть список выплат';
        isPaymentsListCollapsed = true;
    }
}

function showSuccessImage() {
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.7);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 10000;
    `;

    const container = document.createElement('div');
    container.style.cssText = `
        background: white;
        padding: 30px;
        border-radius: 10px;
        text-align: center;
        box-shadow: 0 0 20px rgba(0,0,0,0.3);
    `;

    const img = document.createElement('img');
    img.src = 'images/успех.jpg';
    img.alt = 'Успех';
    img.style.cssText = `
        max-width: 300px;
        max-height: 300px;
        display: block;
        margin: 0 auto 20px;
    `;

    const text = document.createElement('div');
    text.textContent = 'Успешно!';
    text.style.cssText = `
        font-size: 24px;
        font-weight: bold;
        color: #4CAF50;
    `;

    container.appendChild(img);
    container.appendChild(text);
    overlay.appendChild(container);
    document.body.appendChild(overlay);

    overlay.addEventListener('click', function() {
        document.body.removeChild(overlay);
    });

    setTimeout(() => {
        if (document.body.contains(overlay)) {
            document.body.removeChild(overlay);
        }
    }, 1200);
}

// ===== ДОБАВЛЕНИЕ ДАННЫХ =====
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
            showSuccessImage();
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
            showSuccessImage();
            showMessage(`Выплата ${formatMoney(paymentData.amount)} добавлена!`);
            salaryPaymentForm.reset();
            // Установите текущую дату по умолчанию после сброса формы
            document.getElementById('paymentDate').value = new Date().toISOString().split('T')[0];
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
        const isMonthlyReport = this.value === 'monthly';
        monthField.style.display = isMonthlyReport ? 'block' : 'none';
    });

    generateReportBtn.addEventListener('click', generateReport);
    exportReportBtn.addEventListener('click', exportReport);
    closeReportBtn.addEventListener('click', function() {
        reportContainer.style.display = 'none';
    });
}

async function generateReport() {
    if (!currentUser) return;

    const reportType = reportTypeSelect.value;
    const year = parseInt(reportYearSelect.value);
    const month = parseInt(reportMonthSelect.value);

    try {
        showMessage('Формирование отчета...', 'success');
        generateReportBtn.disabled = true;

        let url = `${API_BASE_URL}/reports/`;

        if (reportType === 'monthly') {
            url += `monthly?userId=${currentUser.userId}&year=${year}&month=${month}`;
        } else {
            url += `${reportType}?userId=${currentUser.userId}&year=${year}`;
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
            displayMonthlyReport(data, year, month);
            break;
        case 'annual':
            displayAnnualReport(data, year);
            break;
    }
}

function displayMonthlyReport(data, year, month) {
    const monthNames = {
        1: 'Январь', 2: 'Февраль', 3: 'Март', 4: 'Апрель', 5: 'Май', 6: 'Июнь',
        7: 'Июль', 8: 'Август', 9: 'Сентябрь', 10: 'Октябрь', 11: 'Ноябрь', 12: 'Декабрь'
    };

    const monthName = monthNames[month];
    reportTitle.textContent = `Отчет за ${monthName} ${year} года`;

    let html = `
        <div class="month-summary" style="margin-bottom: 20px;">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;">
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${data.daysCount || 0}</div>
                    <div class="stat-label">Рабочих дней</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalSalary || 0)}</div>
                    <div class="stat-label">Зарплата</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalBonus || 0)}</div>
                    <div class="stat-label">Бонусы</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalIncome || 0)}</div>
                    <div class="stat-label">Всего доход</div>
                </div>
            </div>
        </div>
    `;

    if (data.workDays && data.workDays.length > 0) {
        html += '<h3 style="margin-bottom: 15px;">Детализация по дням</h3>';
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

function displayAnnualReport(data, year) {
    reportTitle.textContent = `Годовой отчет за ${year} год`;

    let html = `
        <div class="annual-summary">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px;">
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${data.totalDays || 0}</div>
                    <div class="stat-label">Всего дней</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalSalary || 0)}</div>
                    <div class="stat-label">Общая зарплата</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalBonus || 0)}</div>
                    <div class="stat-label">Общие бонусы</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.totalIncome || 0)}</div>
                    <div class="stat-label">Общий доход</div>
                </div>
                <div class="stat-item" style="text-align: center;">
                    <div class="stat-value">${formatMoney(data.averageMonthlyIncome || 0)}</div>
                    <div class="stat-label">Средний доход в месяц</div>
                </div>
            </div>
        </div>
    `;

    if (data.monthlyDetails && data.monthlyDetails.length > 0) {
        html += '<h3 style="margin-bottom: 15px;">Детализация по месяцам</h3>';
        html += '<table class="report-table"><thead><tr><th>Месяц</th><th>Дней</th><th>Зарплата</th><th>Бонусы</th><th>Всего</th></tr></thead><tbody>';

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

        html += '</tbody></table>';
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

    const printWindow = window.open('', '_blank', 'width=800,height=600,scrollbars=yes');

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
                    @media print {
                        button { display: none; }
                    }
                </style>
            </head>
            <body>
                ${tempDiv.innerHTML}
            </body>
        </html>
    `);
    printWindow.document.close();
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

    // Получаем дату из формы
    const paymentDate = data.get('paymentDate');

    if (!paymentDate) {
        showMessage('Выберите дату выплаты!', 'error');
        return;
    }

    await addSalaryPayment({
        amount: parseFloat(data.get('amount')),
        description: data.get('paymentDescription'),
        paymentDate: paymentDate
    });
});

loadWorkdaysBtn.addEventListener('click', loadWorkdays);
loadPaymentsBtn.addEventListener('click', loadPayments);

tabs.forEach(tab => {
    tab.addEventListener('click', () => {
        tabs.forEach(t => t.classList.remove('active'));
        tabContents.forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById(tab.getAttribute('data-tab') + 'Tab').classList.add('active');
    });
});

// ===== ИНИЦИАЛИЗАЦИЯ =====
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    // Установка текущей даты по умолчанию для обеих форм
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('workDate').value = today;
    document.getElementById('paymentDate').value = today;
});

// ===== ГЛОБАЛЬНЫЕ ФУНКЦИИ =====
window.deleteWorkday = deleteWorkday;
window.deletePayment = deletePayment;
window.logout = logout;
window.toggleWorkdaysList = toggleWorkdaysList;
window.togglePaymentsList = togglePaymentsList;