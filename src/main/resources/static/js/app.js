/**
 * BankEase: NextGen Net Banking - Frontend Application Logic
 * Plain JavaScript (ES6+), Fetch API, no external frameworks required.
 */

// Application State
const state = {
    token: localStorage.getItem("bankease_token") || null,
    user: null,
    accounts: [],
    activeAccount: null,
    transactions: []
};

// API Base URL
const API_BASE = "";

// ==============================================================================
// DOM Elements
// ==============================================================================
const el = {
    // Nav
    navUser: document.getElementById("navUser"),
    navUserName: document.getElementById("navUserName"),
    navUserRole: document.getElementById("navUserRole"),
    btnLogout: document.getElementById("btnLogout"),

    // Views
    viewAuth: document.getElementById("viewAuth"),
    viewCustomer: document.getElementById("viewCustomer"),
    viewAdmin: document.getElementById("viewAdmin"),

    // Auth Forms & Tabs
    tabLogin: document.getElementById("tabLogin"),
    tabRegister: document.getElementById("tabRegister"),
    formLogin: document.getElementById("formLogin"),
    formRegister: document.getElementById("formRegister"),
    loginEmail: document.getElementById("loginEmail"),
    loginPassword: document.getElementById("loginPassword"),
    regFullName: document.getElementById("regFullName"),
    regEmail: document.getElementById("regEmail"),
    regPassword: document.getElementById("regPassword"),

    // Customer View
    customerWelcomeName: document.getElementById("customerWelcomeName"),
    customerAccountNumber: document.getElementById("customerAccountNumber"),
    customerBalance: document.getElementById("customerBalance"),
    formTransfer: document.getElementById("formTransfer"),
    transferFromAccount: document.getElementById("transferFromAccount"),
    transferToAccount: document.getElementById("transferToAccount"),
    transferAmount: document.getElementById("transferAmount"),
    transferRemarks: document.getElementById("transferRemarks"),
    btnTransfer: document.getElementById("btnTransfer"),
    btnRefreshTx: document.getElementById("btnRefreshTx"),
    txTableBody: document.getElementById("txTableBody"),

    // Admin View
    adminTotalUsers: document.getElementById("adminTotalUsers"),
    adminTotalAccounts: document.getElementById("adminTotalAccounts"),
    adminTotalLiquidity: document.getElementById("adminTotalLiquidity"),
    adminUsersTableBody: document.getElementById("adminUsersTableBody"),
    adminAccountsTableBody: document.getElementById("adminAccountsTableBody"),
    adminTxTableBody: document.getElementById("adminTxTableBody"),

    // Toast Container
    toastContainer: document.getElementById("toastContainer")
};

// ==============================================================================
// Initialization
// ==============================================================================
document.addEventListener("DOMContentLoaded", () => {
    setupEventListeners();
    if (state.token) {
        verifySession();
    } else {
        renderView();
    }
});

// ==============================================================================
// Event Listeners Setup
// ==============================================================================
function setupEventListeners() {
    // Auth Tabs
    el.tabLogin.addEventListener("click", () => switchAuthTab("login"));
    el.tabRegister.addEventListener("click", () => switchAuthTab("register"));

    // Forms
    el.formLogin.addEventListener("submit", handleLogin);
    el.formRegister.addEventListener("submit", handleRegister);
    el.formTransfer.addEventListener("submit", handleTransfer);
    el.btnLogout.addEventListener("click", handleLogout);
    el.btnRefreshTx.addEventListener("click", () => {
        if (state.activeAccount) {
            loadTransactionHistory(state.activeAccount.accountNumber);
            loadAccounts();
        }
    });

    // Quick autofill buttons for demo/testing
    document.querySelectorAll("[data-demo-user]").forEach(btn => {
        btn.addEventListener("click", (e) => {
            const userType = e.currentTarget.getAttribute("data-demo-user");
            if (userType === "rahul") {
                el.loginEmail.value = "rahul@bankease.com";
                el.loginPassword.value = "rahul123";
            } else if (userType === "priya") {
                el.loginEmail.value = "priya@bankease.com";
                el.loginPassword.value = "priya123";
            } else if (userType === "admin") {
                el.loginEmail.value = "admin@bankease.com";
                el.loginPassword.value = "admin123";
            }
            switchAuthTab("login");
            showToast("Demo credentials filled. Click 'Sign In'!", "info");
        });
    });

    // Quick receiver autofill button in transfer form
    const btnFillPriyaAcc = document.getElementById("btnFillPriyaAcc");
    if (btnFillPriyaAcc) {
        btnFillPriyaAcc.addEventListener("click", () => {
            el.transferToAccount.value = "1002002002";
        });
    }
    const btnFillRahulAcc = document.getElementById("btnFillRahulAcc");
    if (btnFillRahulAcc) {
        btnFillRahulAcc.addEventListener("click", () => {
            el.transferToAccount.value = "1001001001";
        });
    }
}

// ==============================================================================
// Navigation & Views
// ==============================================================================
function switchAuthTab(tab) {
    if (tab === "login") {
        el.tabLogin.classList.add("active");
        el.tabRegister.classList.remove("active");
        el.formLogin.style.display = "block";
        el.formRegister.style.display = "none";
    } else {
        el.tabLogin.classList.remove("active");
        el.tabRegister.classList.add("active");
        el.formLogin.style.display = "none";
        el.formRegister.style.display = "block";
    }
}

function renderView() {
    if (!state.token || !state.user) {
        // Not logged in
        el.viewAuth.style.display = "block";
        el.viewCustomer.style.display = "none";
        el.viewAdmin.style.display = "none";
        el.navUser.style.display = "none";
        return;
    }

    // Logged in
    el.viewAuth.style.display = "none";
    el.navUser.style.display = "flex";
    el.navUserName.textContent = state.user.fullName;
    el.navUserRole.textContent = state.user.role;
    el.navUserRole.className = `role-badge ${state.user.role.toLowerCase()}`;

    if (state.user.role === "ADMIN") {
        el.viewCustomer.style.display = "none";
        el.viewAdmin.style.display = "block";
        loadAdminData();
    } else {
        el.viewCustomer.style.display = "block";
        el.viewAdmin.style.display = "none";
        loadCustomerDashboard();
    }
}

// ==============================================================================
// API Client Helper
// ==============================================================================
async function apiCall(endpoint, method = "GET", body = null) {
    const headers = {
        "Content-Type": "application/json"
    };

    if (state.token) {
        headers["Authorization"] = `Bearer ${state.token}`;
    }

    const config = {
        method,
        headers
    };

    if (body) {
        config.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, config);
        const data = await response.json().catch(() => null);

        if (!response.ok) {
            let errorMsg = (data && data.message) ? data.message : `Error ${response.status}: ${response.statusText}`;
            if (data && data.fieldErrors) {
                const fields = Object.entries(data.fieldErrors).map(([k, v]) => `${k}: ${v}`).join(", ");
                errorMsg += ` (${fields})`;
            }
            throw new Error(errorMsg);
        }

        return data;
    } catch (err) {
        throw err;
    }
}

// ==============================================================================
// Authentication Logic
// ==============================================================================
async function handleLogin(e) {
    e.preventDefault();
    const email = el.loginEmail.value.trim();
    const password = el.loginPassword.value.trim();

    try {
        const res = await apiCall("/api/auth/login", "POST", { email, password });
        if (res && res.data) {
            state.token = res.data.token;
            state.user = {
                id: res.data.userId,
                fullName: res.data.fullName,
                email: res.data.email,
                role: res.data.role
            };
            state.accounts = res.data.accounts || [];
            localStorage.setItem("bankease_token", state.token);

            showToast("Welcome back, " + state.user.fullName + "!", "success");
            renderView();
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const fullName = el.regFullName.value.trim();
    const email = el.regEmail.value.trim();
    const password = el.regPassword.value.trim();

    try {
        const res = await apiCall("/api/auth/register", "POST", { fullName, email, password });
        if (res && res.data) {
            state.token = res.data.token;
            state.user = {
                id: res.data.userId,
                fullName: res.data.fullName,
                email: res.data.email,
                role: res.data.role
            };
            state.accounts = res.data.accounts || [];
            localStorage.setItem("bankease_token", state.token);

            showToast("Account created successfully! ₹10,000 demo funds credited.", "success");
            renderView();
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function verifySession() {
    try {
        const res = await apiCall("/api/auth/me");
        if (res && res.data) {
            state.user = {
                id: res.data.userId,
                fullName: res.data.fullName,
                email: res.data.email,
                role: res.data.role
            };
            state.accounts = res.data.accounts || [];
            renderView();
        } else {
            handleLogout();
        }
    } catch (err) {
        handleLogout();
    }
}

async function handleLogout() {
    if (state.token) {
        try {
            await apiCall("/api/auth/logout", "POST");
        } catch (ignored) {}
    }
    state.token = null;
    state.user = null;
    state.accounts = [];
    state.activeAccount = null;
    localStorage.removeItem("bankease_token");
    showToast("Logged out successfully.", "info");
    renderView();
}

// ==============================================================================
// Customer Dashboard & Fund Transfer
// ==============================================================================
async function loadCustomerDashboard() {
    el.customerWelcomeName.textContent = state.user.fullName;
    await loadAccounts();
}

async function loadAccounts() {
    try {
        const res = await apiCall("/api/accounts/my-accounts");
        if (res && res.data && res.data.length > 0) {
            state.accounts = res.data;
            state.activeAccount = state.accounts[0];

            // Render Account Info Banner
            el.customerAccountNumber.textContent = state.activeAccount.accountNumber;
            el.customerBalance.textContent = formatCurrency(state.activeAccount.balance);

            // Populate Transfer Source Account Dropdown
            el.transferFromAccount.innerHTML = "";
            state.accounts.forEach(acc => {
                const opt = document.createElement("option");
                opt.value = acc.accountNumber;
                opt.textContent = `${acc.accountNumber} (${acc.accountType} - ${formatCurrency(acc.balance)})`;
                el.transferFromAccount.appendChild(opt);
            });

            // Load transaction history for the active account
            await loadTransactionHistory(state.activeAccount.accountNumber);
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleTransfer(e) {
    e.preventDefault();
    const fromAccountNumber = el.transferFromAccount.value;
    const toAccountNumber = el.transferToAccount.value.trim();
    const amount = parseFloat(el.transferAmount.value);
    const description = el.transferRemarks.value.trim();

    if (!toAccountNumber) {
        showToast("Please enter a destination account number.", "error");
        return;
    }

    if (isNaN(amount) || amount <= 0) {
        showToast("Please enter a valid transfer amount greater than 0.", "error");
        return;
    }

    el.btnTransfer.disabled = true;
    el.btnTransfer.textContent = "Processing Transfer...";

    try {
        const res = await apiCall("/api/transfers", "POST", {
            fromAccountNumber,
            toAccountNumber,
            amount,
            description
        });

        showToast("Fund transfer of " + formatCurrency(amount) + " succeeded!", "success");

        // Clear transfer inputs
        el.transferToAccount.value = "";
        el.transferAmount.value = "";
        el.transferRemarks.value = "";

        // Reload accounts & transaction history
        await loadAccounts();
    } catch (err) {
        showToast(err.message, "error");
        // Reload accounts & transaction history so FAILED transaction record appears in history!
        await loadAccounts();
    } finally {
        el.btnTransfer.disabled = false;
        el.btnTransfer.textContent = "Transfer Funds Now";
    }
}

async function loadTransactionHistory(accountNumber) {
    try {
        const res = await apiCall(`/api/transactions/account/${accountNumber}`);
        state.transactions = (res && res.data) ? res.data : [];
        renderTransactionTable(state.transactions);
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderTransactionTable(transactions) {
    el.txTableBody.innerHTML = "";

    if (!transactions || transactions.length === 0) {
        el.txTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-state">No transaction records found for this account.</td>
            </tr>
        `;
        return;
    }

    const currentAccNum = state.activeAccount ? state.activeAccount.accountNumber : "";

    transactions.forEach(tx => {
        const tr = document.createElement("tr");

        const isCredit = (tx.toAccountNumber === currentAccNum);
        const isFailed = (tx.status === "FAILED");

        let amountHtml = "";
        if (isFailed) {
            amountHtml = `<span style="color: var(--neutral-600);">${formatCurrency(tx.amount)}</span>`;
        } else if (isCredit) {
            amountHtml = `<span class="amount-credit">+ ${formatCurrency(tx.amount)}</span>`;
        } else {
            amountHtml = `<span class="amount-debit">- ${formatCurrency(tx.amount)}</span>`;
        }

        const formattedDate = formatDateTime(tx.createdAt);
        const statusClass = tx.status === "SUCCESS" ? "success" : "failed";

        tr.innerHTML = `
            <td><strong>#${tx.id}</strong><br><small style="color:var(--neutral-600);">${formattedDate}</small></td>
            <td>${escapeHtml(tx.description || tx.type)}</td>
            <td><small>From: <strong>${escapeHtml(tx.fromAccountNumber)}</strong><br>To: <strong>${escapeHtml(tx.toAccountNumber)}</strong></small></td>
            <td><span class="role-badge ${tx.type.toLowerCase()}">${tx.type}</span></td>
            <td>${amountHtml}</td>
            <td><span class="status-badge ${statusClass}">${tx.status}</span></td>
        `;
        el.txTableBody.appendChild(tr);
    });
}

// ==============================================================================
// Admin Management Logic
// ==============================================================================
async function loadAdminData() {
    try {
        const [usersRes, accountsRes, txRes] = await Promise.all([
            apiCall("/api/admin/users"),
            apiCall("/api/admin/accounts"),
            apiCall("/api/admin/transactions")
        ]);

        const users = usersRes.data || [];
        const accounts = accountsRes.data || [];
        const transactions = txRes.data || [];

        // Stats
        el.adminTotalUsers.textContent = users.length;
        el.adminTotalAccounts.textContent = accounts.length;

        const totalBalance = accounts.reduce((acc, curr) => acc + parseFloat(curr.balance || 0), 0);
        el.adminTotalLiquidity.textContent = formatCurrency(totalBalance);

        // Render Users Table
        el.adminUsersTableBody.innerHTML = "";
        users.forEach(u => {
            const tr = document.createElement("tr");
            const accList = (u.accounts || []).map(a => `${a.accountNumber} (${formatCurrency(a.balance)})`).join(", ") || "None";
            tr.innerHTML = `
                <td><strong>#${u.id}</strong></td>
                <td><strong>${escapeHtml(u.fullName)}</strong></td>
                <td>${escapeHtml(u.email)}</td>
                <td><span class="role-badge ${u.role.toLowerCase()}">${u.role}</span></td>
                <td><small>${escapeHtml(accList)}</small></td>
                <td><small>${formatDateTime(u.createdAt)}</small></td>
            `;
            el.adminUsersTableBody.appendChild(tr);
        });

        // Render Accounts Table
        el.adminAccountsTableBody.innerHTML = "";
        accounts.forEach(a => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${escapeHtml(a.accountNumber)}</strong></td>
                <td>${escapeHtml(a.ownerName)}</td>
                <td>${escapeHtml(a.ownerEmail)}</td>
                <td><span class="role-badge customer">${a.accountType}</span></td>
                <td><strong>${formatCurrency(a.balance)}</strong></td>
            `;
            el.adminAccountsTableBody.appendChild(tr);
        });

        // Render Admin Transactions Table
        el.adminTxTableBody.innerHTML = "";
        transactions.forEach(tx => {
            const tr = document.createElement("tr");
            const statusClass = tx.status === "SUCCESS" ? "success" : "failed";
            tr.innerHTML = `
                <td><strong>#${tx.id}</strong></td>
                <td>${formatDateTime(tx.createdAt)}</td>
                <td>${escapeHtml(tx.fromAccountNumber)} (${escapeHtml(tx.fromOwnerName)})</td>
                <td>${escapeHtml(tx.toAccountNumber)} (${escapeHtml(tx.toOwnerName)})</td>
                <td><strong>${formatCurrency(tx.amount)}</strong></td>
                <td><small>${escapeHtml(tx.description || tx.type)}</small></td>
                <td><span class="status-badge ${statusClass}">${tx.status}</span></td>
            `;
            el.adminTxTableBody.appendChild(tr);
        });

    } catch (err) {
        showToast(err.message, "error");
    }
}

// ==============================================================================
// Utilities
// ==============================================================================
function formatCurrency(amount) {
    const num = parseFloat(amount || 0);
    return "₹ " + num.toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatDateTime(isoString) {
    if (!isoString) return "N/A";
    const date = new Date(isoString);
    return date.toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showToast(message, type = "info") {
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    el.toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

