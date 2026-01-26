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
let totalAllPayments = 0;

// ===== АВТОРИЗАЦИЯ =====
async function login(username, password) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, password})
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

    // Загружаем общую сумму выплат
    displayTotalAllPayments();
}

function showLoginForm() {
    loginForm.style.display = 'block';
    mainContent.style.display = 'none';
    loginFormElement.reset();
}

function showMessage(text, type = 'success') {
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    setTimeout(() => {
        messageDiv.className = 'message';
    }, 2000);
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

        // Загружаем общую сумму всех выплат для сводки
        const allTimeTotal = await calculateTotalAllPayments();
        const allTimeElement = document.getElementById('allTimeTotalPaid');
        if (allTimeElement) {
            allTimeElement.textContent = formatMoney(allTimeTotal);
        }
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

// ===== ОБЩАЯ СУММА ВСЕХ ВЫПЛАТ =====
async function calculateTotalAllPayments() {
    if (!currentUser) return 0;

    try {
        const response = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`);
        if (response.ok) {
            const payments = await response.json();

            // Суммируем все выплаты
            totalAllPayments = payments.reduce((total, payment) => {
                return total + (parseFloat(payment.amount) || 0);
            }, 0);

            return totalAllPayments;
        }
        return 0;
    } catch (error) {
        console.error('Ошибка расчета общей суммы выплат:', error);
        return 0;
    }
}

// Функция для отображения общей суммы выплат
async function displayTotalAllPayments() {
    const total = await calculateTotalAllPayments();

    // Создаем или находим элемент для отображения общей суммы
    let totalPaymentsElement = document.getElementById('totalAllPayments');

    if (!totalPaymentsElement) {
        // Создаем элемент для отображения общей суммы
        const paymentsControls = document.getElementById('paymentsControls');
        if (paymentsControls) {
            totalPaymentsElement = document.createElement('div');
            totalPaymentsElement.id = 'totalAllPayments';
            totalPaymentsElement.className = 'total-all-payments';
            totalPaymentsElement.innerHTML = `
                <div class="total-payments-card">
                    <div class="total-payments-title">💰 Общая сумма всех выплат:</div>
                    <div class="total-payments-amount">${formatMoney(total)}</div>
                    <button onclick="loadTotalAllPayments()" class="btn btn-success refresh-total-btn">🔄 Обновить</button>
                </div>
            `;

            // Вставляем перед кнопками управления
            paymentsControls.insertBefore(totalPaymentsElement, paymentsControls.firstChild);
        }
    } else {
        // Обновляем существующий элемент
        const amountElement = totalPaymentsElement.querySelector('.total-payments-amount');
        if (amountElement) {
            amountElement.textContent = formatMoney(total);
        }
    }
}

// Функция для обновления общей суммы (будет вызвана по кнопке)
async function loadTotalAllPayments() {
    showMessage('Обновление общей суммы выплат...', 'success');
    await displayTotalAllPayments();
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

        const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

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

            // ОБНОВЛЯЕМ ОБЩУЮ СУММУ
            totalAllPayments = payments.reduce((total, payment) => {
                return total + (parseFloat(payment.amount) || 0);
            }, 0);

            // Отображаем общую сумму
            displayTotalAllPayments();

            if (!payments || !payments.length) {
                paymentsContainer.innerHTML = '<div class="empty-state">💸 Нет выплат</div>';
                console.log('ℹ️ Нет выплат для отображения');
                initializeCollapsiblePayments();
                return;
            }

            // Группируем выплаты по месяцам
            const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

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
    } catch (error) {
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
    const overlay = document.getElementById('successImage');
    if (overlay) {
        overlay.style.display = 'flex';
        setTimeout(() => {
            overlay.style.display = 'none';
        }, 1200);
    }

}

// ===== ДОБАВЛЕНИЕ ДАННЫХ =====
async function addWorkday(workdayData) {
    if (!currentUser) return;
    try {
        workdayData.bonus = parseInt(workdayData.bonus) || 0;

        const res = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(workdayData)
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
    } catch (e) {
        showMessage('Ошибка соединения', 'error');
    }
}

async function addSalaryPayment(paymentData) {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE_URL}/payments?userId=${currentUser.userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(paymentData)
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
    } catch (e) {
        showMessage('Ошибка соединения', 'error');
    }
}

async function deleteWorkday(id) {
    if (!currentUser || !confirm('Удалить день?')) return;
    try {
        const res = await fetch(`${API_BASE_URL}/workdays/${id}?userId=${currentUser.userId}`, {method: 'DELETE'});
        if (res.ok) {
            showMessage('День удален');
            loadWorkdays();
            updateSummary();
        }
    } catch (e) {
        showMessage('Ошибка удаления', 'error');
    }
}

async function deletePayment(id) {
    if (!currentUser || !confirm('Удалить выплату?')) return;
    try {
        const res = await fetch(`${API_BASE_URL}/payments/${id}?userId=${currentUser.userId}`, {method: 'DELETE'});
        if (res.ok) {
            showMessage('Выплата удалена');
            loadPayments();
            updateSummary();
        }
    } catch (e) {
        showMessage('Ошибка удаления', 'error');
    }
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
    reportTypeSelect.addEventListener('change', function () {
        const isMonthlyReport = this.value === 'monthly';
        monthField.style.display = isMonthlyReport ? 'block' : 'none';
    });

    generateReportBtn.addEventListener('click', generateReport);
    exportReportBtn.addEventListener('click', exportReport);
    closeReportBtn.addEventListener('click', function () {
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
        } else if (reportType === 'full-daily') {
            url += `full-daily?userId=${currentUser.userId}`;
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
        case 'full-daily':
            displayFullDailyReport(data);
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
        // Сортируем дни по дате (новые сверху)
        const sortedDays = [...data.workDays].sort((a, b) => new Date(b.workDate) - new Date(a.workDate));

        sortedDays.forEach(day => {
            html += `
                <div class="detailed-day" style="padding: 8px 12px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; font-size: 14px;">
                    <div style="flex: 1;">
                        <div style="font-weight: bold; color: #333;">${formatDate(day.workDate)}</div>
                        <div style="color: #666; font-size: 12px; margin-top: 2px;">${day.description || 'Рабочий день'}</div>
                    </div>
                    <div style="text-align: right;">
                        <div style="font-weight: bold; color: #28a745;">${formatMoney(day.salary)}</div>
                        ${day.bonus > 0 ? `<div style="color: #ffc107; font-size: 12px;">+${formatMoney(day.bonus)}</div>` : ''}
                    </div>
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

function displayFullDailyReport(data) {
    reportTitle.textContent = `Полный отчет по всем рабочим дням`;

    let html = `
        <div class="full-report-summary" style="margin-bottom: 20px;">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;">
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
            </div>
        </div>
    `;

    if (data.workDays && data.workDays.length > 0) {
        // Группируем дни по годам и месяцам для удобства просмотра
        const groupedDays = groupDaysByYearMonth(data.workDays);

        // Сортируем группы по году и месяцу (новые сверху)
        const sortedGroups = Object.keys(groupedDays).sort((a, b) => {
            const [yearA, monthA] = a.split('-').map(Number);
            const [yearB, monthB] = b.split('-').map(Number);
            return yearB - yearA || monthB - monthA;
        });

        sortedGroups.forEach(yearMonth => {
            const group = groupedDays[yearMonth];
            html += `
                <div class="year-month-group" style="margin-bottom: 20px; border: 1px solid #ddd; border-radius: 6px; padding: 12px;">
                    <h3 style="margin: 0 0 12px 0; color: #333; background: #f8f9fa; padding: 8px; border-radius: 4px; font-size: 16px;">
                        ${group.yearMonthName}
                        <span style="float: right; font-size: 13px; color: #666;">
                            ${group.days.length} дней • ${formatMoney(group.totalIncome)}
                        </span>
                    </h3>
                    <div class="compact-days-list">
            `;

            // Дни внутри группы уже отсортированы по убыванию даты (новые сверху)
            group.days.forEach(day => {
                html += `
                    <div class="compact-day-item" style="padding: 6px 8px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; font-size: 13px; line-height: 1.3;">
                        <div style="flex: 1;">
                            <span style="font-weight: 500; color: #333;">${formatDate(day.workDate)}</span>
                            <span style="color: #666; font-size: 12px; margin-left: 8px;">${day.description || 'Рабочий день'}</span>
                        </div>
                        <div style="text-align: right; white-space: nowrap;">
                            <span style="font-weight: 600; color: #28a745;">${formatMoney(day.salary)}</span>
                            ${day.bonus > 0 ? `<span style="color: #ffc107; font-size: 12px; margin-left: 6px;">+${formatMoney(day.bonus)}</span>` : ''}
                        </div>
                    </div>
                `;
            });

            html += `
                    </div>
                </div>
            `;
        });
    } else {
        html += '<div class="empty-state"><div>📭</div><h3>Нет рабочих дней</h3><p>Не было добавлено ни одного рабочего дня</p></div>';
    }

    reportContent.innerHTML = html;
}

// Вспомогательная функция для группировки дней по годам и месяцам
function groupDaysByYearMonth(workDays) {
    const monthNames = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
        'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];

    const groups = {};

    workDays.forEach(day => {
        const date = new Date(day.workDate);
        const year = date.getFullYear();
        const month = date.getMonth();
        const yearMonthKey = `${year}-${month}`;
        const yearMonthName = `${monthNames[month]} ${year}`;

        if (!groups[yearMonthKey]) {
            groups[yearMonthKey] = {
                yearMonthName: yearMonthName,
                days: [],
                totalSalary: 0,
                totalBonus: 0,
                totalIncome: 0
            };
        }

        groups[yearMonthKey].days.push(day);
        groups[yearMonthKey].totalSalary += day.salary || 0;
        groups[yearMonthKey].totalBonus += day.bonus || 0;
        groups[yearMonthKey].totalIncome += (day.salary || 0) + (day.bonus || 0);
    });

    // Сортируем дни внутри каждой группы по дате (новые сверху)
    Object.keys(groups).forEach(key => {
        groups[key].days.sort((a, b) => new Date(b.workDate) - new Date(a.workDate));
    });

    return groups;
}

function exportReport() {
    const reportTitleText = reportTitle.textContent;
    const reportContentHtml = reportContent.innerHTML;

    // Создаем HTML для печати/экспорта
    const printHtml = `
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${reportTitleText}</title>
            <style>
                body { 
                    font-family: Arial, sans-serif; 
                    margin: 20px; 
                    line-height: 1.4;
                    color: #333;
                }
                .report-header {
                    text-align: center;
                    margin-bottom: 30px;
                    border-bottom: 2px solid #333;
                    padding-bottom: 15px;
                }
                .report-header h1 {
                    margin: 0;
                    font-size: 24px;
                    color: #2c3e50;
                }
                .summary-stats {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
                    gap: 15px;
                    margin-bottom: 25px;
                    text-align: center;
                }
                .stat-item {
                    padding: 10px;
                    border: 1px solid #ddd;
                    border-radius: 5px;
                }
                .stat-value {
                    font-size: 18px;
                    font-weight: bold;
                    color: #2c3e50;
                }
                .stat-label {
                    font-size: 12px;
                    color: #666;
                    margin-top: 5px;
                }
                .month-group {
                    margin-bottom: 20px;
                    page-break-inside: avoid;
                }
                .month-header {
                    background: #f8f9fa;
                    padding: 8px 12px;
                    border: 1px solid #dee2e6;
                    border-radius: 4px;
                    margin-bottom: 8px;
                    font-weight: bold;
                }
                .compact-day-item {
                    padding: 4px 6px;
                    border-bottom: 1px solid #eee;
                    display: flex;
                    justify-content: space-between;
                    font-size: 11px;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 15px 0;
                    font-size: 12px;
                }
                th, td {
                    padding: 6px 8px;
                    text-align: left;
                    border-bottom: 1px solid #ddd;
                }
                th {
                    background-color: #f8f9fa;
                    font-weight: bold;
                }
                .report-footer {
                    margin-top: 30px;
                    padding-top: 15px;
                    border-top: 1px solid #ddd;
                    font-size: 11px;
                    color: #666;
                    text-align: center;
                }
                @media print {
                    body { margin: 15px; }
                    .no-print { display: none; }
                    .month-group { page-break-inside: avoid; }
                }
            </style>
        </head>
        <body>
            <div class="report-header">
                <h1>${reportTitleText}</h1>
                <div style="font-size: 14px; color: #666;">
                    Сгенерировано: ${new Date().toLocaleString('ru-RU')}
                </div>
            </div>
            <div>${reportContentHtml}</div>
            <div class="report-footer">
                Отчет сгенерирован в системе учета рабочих дней и зарплаты
            </div>
            
            <div class="no-print" style="margin-top: 30px; text-align: center;">
                <button onclick="window.print()" style="padding: 10px 20px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer; margin: 5px;">
                    🖨️ Печать
                </button>
                <button onclick="window.close()" style="padding: 10px 20px; background: #6c757d; color: white; border: none; border-radius: 5px; cursor: pointer; margin: 5px;">
                    ✕ Закрыть
                </button>
            </div>

            <script>
                // Автоматически открыть диалог печати
                setTimeout(() => {
                    window.print();
                }, 500);
            </script>
        </body>
        </html>
    `;

    const printWindow = window.open('', '_blank', 'width=1000,height=700,scrollbars=yes');
    printWindow.document.write(printHtml);
    printWindow.document.close();

    // Фокус на новом окне
    printWindow.focus();
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

        // При переключении на вкладку выплат обновляем общую сумму
        if (tab.getAttribute('data-tab') === 'payments') {
            displayTotalAllPayments();
        }
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
window.loadTotalAllPayments = loadTotalAllPayments;