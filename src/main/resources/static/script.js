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

// ===== Авторизация =====
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

// ===== UI =====
function showMainContent() {
    loginForm.style.display = 'none';
    mainContent.style.display = 'block';
    currentUserNameSpan.textContent = currentUser.username;
    loadWorkdays();
    loadPayments();
    updateSummary();
}

function showLoginForm() {
    loginForm.style.display = 'block';
    mainContent.style.display = 'none';
    loginFormElement.reset();
}

// ===== Вспомогательные функции =====
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

// ===== API функции =====
async function updateSummary() {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE_URL}/workdays/statistics?userId=${currentUser.userId}`);
        if (res.ok) {
            const stats = await res.json();
            totalDaysSpan.textContent = safeNumber(stats.totalDays);
            totalEarnedSpan.textContent = formatMoney(stats.totalEarned);

            // ФИКС: Правильная обработка бонусов
            const totalBonusValue = safeNumber(stats.totalBonus || stats.bonusTotal || 0);
            totalBonusSpan.textContent = formatMoney(totalBonusValue);

            totalPaidSpan.textContent = formatMoney(stats.totalPaid);
            const balance = safeNumber(stats.salaryBalance);
            salaryBalanceSpan.textContent = formatMoney(balance);
            salaryBalanceSpan.className = `summary-value ${balance > 0 ? 'balance-positive' : balance < 0 ? 'balance-negative' : ''}`;
        }
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

async function loadWorkdays() {
    if (!currentUser) return;
    try {
        workdaysContainer.innerHTML = '<div class="loading">Загрузка...</div>';
        const res = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`);
        if (res.ok) {
            const workdays = await res.json();
            if (!workdays.length) {
                workdaysContainer.innerHTML = '<div class="empty-state">📭 Нет рабочих дней</div>';
                return;
            }

            workdays.sort((a, b) => new Date(b.workDate) - new Date(a.workDate));

            const monthGroups = {};
            const monthNames = ['Январь','Февраль','Март','Апрель','Май','Июнь','Июль','Август','Сентябрь','Октябрь','Ноябрь','Декабрь'];

            workdays.forEach(day => {
                const date = new Date(day.workDate);
                const key = `${date.getFullYear()}-${date.getMonth()}`;
                if (!monthGroups[key]) monthGroups[key] = {
                    monthName: `${monthNames[date.getMonth()]} ${date.getFullYear()}`,
                    days: [],
                    totalSalary: 0,
                    totalBonus: 0,
                    daysCount: 0
                };
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
                div.innerHTML = `
                <div class="month-header">
                    <span>${group.monthName}</span>
                    <span class="month-total">${group.daysCount} дней • ${formatMoney(totalIncome)}</span>
                </div>
                <div class="month-days">
                    ${group.days.map(day => {
                    // ФИКС: Правильное отображение бонусов
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
    } catch (e) {
        workdaysContainer.innerHTML = '<div class="loading">Ошибка загрузки</div>';
    }
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
        // ФИКС: Убедимся, что bonus всегда число
        workdayData.bonus = parseInt(workdayData.bonus) || 0;

        const res = await fetch(`${API_BASE_URL}/workdays?userId=${currentUser.userId}`, {
            method:'POST',
            headers:{'Content-Type':'application/json'},
            body:JSON.stringify(workdayData)
        });
        if (res.ok) {
            const saved = await res.json();
            // ФИКС: Правильное отображение бонуса в сообщении
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

// ===== Обработчики событий =====
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

    console.log('Отправляемые данные:', {
        workDate: data.get('workDate'),
        description: data.get('description'),
        salary: parseInt(salary),
        bonus: parseInt(bonus)
    });

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

// ===== Инициализация =====
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    document.getElementById('workDate').value = new Date().toISOString().split('T')[0];
});

// ===== Глобальные функции =====
window.deleteWorkday = deleteWorkday;
window.deletePayment = deletePayment;
window.logout = logout;