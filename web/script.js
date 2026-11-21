
document.addEventListener("DOMContentLoaded", function () {
    console.log("Banking Management System loaded successfully!");

    document.getElementById("totalAccounts").textContent = "3";
    document.getElementById("totalDeposits").textContent = "₹30,000";
    document.getElementById("totalTransactions").textContent = "4";
});


// CREATE ACCOUNT FUNCTION

function showCreateAccount() {

    const formArea = document.getElementById("formArea");

    formArea.innerHTML = `
        <h2>Create New Bank Account</h2>

        <form id="accountForm">

            <label>Customer Name</label>
            <input type="text" id="customerName" required>

            <label>Email Address</label>
            <input type="email" id="email" required>

            <label>Phone Number</label>
            <input type="tel" id="phone" required>

            <label>Date of Birth</label>
            <input type="date" id="dateOfBirth" required>

            <label>Branch</label>
            <select id="branchId" required>
                <option value="">Select Branch</option>
                <option value="1">Hyderabad Main Branch</option>
                <option value="2">Secunderabad Branch</option>
            </select>

            <label>Account Type</label>
            <select id="accountType" required>
                <option value="">Select Account Type</option>
                <option value="Savings">Savings</option>
                <option value="Current">Current</option>
            </select>

            <button type="submit">Create Account</button>

        </form>
    `;

    document.getElementById("accountForm").addEventListener("submit", async function(event) {

        event.preventDefault();

        const formData = new URLSearchParams({
            customerName: document.getElementById("customerName").value,
            email: document.getElementById("email").value,
            phone: document.getElementById("phone").value,
            dateOfBirth: document.getElementById("dateOfBirth").value,
            branchId: document.getElementById("branchId").value,
            accountType: document.getElementById("accountType").value
        });

        try {

            const response = await fetch("/api/create-account", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData
            });

            const result = await response.text();
            alert(result);

        } catch (error) {

            alert("Could not connect to the banking server.");
            console.error(error);

        }

    });

}


// DEPOSIT MONEY FUNCTION

function showDepositForm() {


    const formArea = document.getElementById("formArea");

    formArea.innerHTML = `
        <h2>Deposit Money</h2>

        <form id="depositForm">

            <label>Account Number</label>
            <input type="text" id="depositAccountNumber" required>

            <label>Deposit Amount</label>
            <input type="number" id="depositAmount" min="1" step="0.01" required>

            <button type="submit">Deposit</button>

        </form>
    `;

    document.getElementById("depositForm").addEventListener("submit", async function(event) {

        event.preventDefault();

        const formData = new URLSearchParams({
            accountNumber: document.getElementById("depositAccountNumber").value,
            amount: document.getElementById("depositAmount").value
        });

        try {

            const response = await fetch("http://localhost:8080/api/deposit", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData
            });

            const result = await response.text();
            alert(result);

        } catch (error) {

            alert("Could not connect to the banking server.");
            console.error(error);

        }

    });

}

function showWithdrawForm() {
    document.getElementById("formArea").innerHTML = `
        <h2>Withdraw Money</h2>
        <form id="withdrawForm">
            <input type="text" id="withdrawAccount" placeholder="Account Number" required>
            <input type="number" id="withdrawAmount" placeholder="Withdrawal Amount" min="0.01" step="0.01" required>
            <button type="submit">Withdraw</button>
        </form>
        <p id="withdrawMessage"></p>
    `;

    document.getElementById("withdrawForm").addEventListener("submit", async function(event) {
        event.preventDefault();

        const formData = new URLSearchParams({
            accountNumber: document.getElementById("withdrawAccount").value,
            amount: document.getElementById("withdrawAmount").value
        });

        try {
            const response = await fetch("http://localhost:8080/api/withdraw", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData
            });

            const result = await response.text();
            alert(result);

        } catch (error) {
            alert("Could not connect to the banking server.");
            console.error(error);
        }
    });
}
    function showBalanceForm() {
    document.getElementById("formArea").innerHTML = `
        <h2>Check Account Balance</h2>

        <form id="balanceForm">
            <input
                type="text"
                id="balanceAccountNumber"
                placeholder="Enter Account Number"
                required
            >

            <button type="submit">Check Balance</button>
        </form>
    `;

    document.getElementById("balanceForm").addEventListener(
        "submit", async function(event) {

            event.preventDefault();

            const formData = new URLSearchParams({
                accountNumber: document.getElementById(
                    "balanceAccountNumber"
                ).value
            });

            try {
                const response = await fetch(
                    "http://localhost:8080/api/balance",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/x-www-form-urlencoded"
                        },
                        body: formData
                    }
                );

                const result = await response.text();
                alert(result);

            } catch (error) {
                alert("Could not connect to the banking server.");
                console.error(error);
            }
        }
    );
}
function showTransactionHistoryForm() {
    document.getElementById("formArea").innerHTML = `
        <h2>Transaction History</h2>
        <form id="historyForm">
            <input type="text" id="historyAccount"
                placeholder="Account Number" required>
            <button type="submit">View History</button>
        </form>
    `;

    document.getElementById("historyForm").addEventListener(
        "submit", async function(event) {
            event.preventDefault();

            const formData = new URLSearchParams({
                accountNumber:
                    document.getElementById("historyAccount").value
            });

            try {
                const response = await fetch(
                    "http://localhost:8080/api/transactions",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded"
                        },
                        body: formData
                    }
                );

                const result = await response.text();
                alert(result);

            } catch (error) {
                alert("Could not connect to the banking server.");
                console.error(error);
            }
        }
    );
}
function showTransferForm() {
    document.getElementById("formArea").innerHTML = `
        <h2>Transfer Money</h2>
        <form id="transferForm">
            <input type="text" id="senderAccount"
                placeholder="Sender Account Number" required>

            <input type="text" id="receiverAccount"
                placeholder="Receiver Account Number" required>

            <input type="number" id="transferAmount"
                placeholder="Transfer Amount"
                min="0.01" step="0.01" required>

            <button type="submit">Transfer</button>
        </form>
    `;

    document.getElementById("transferForm").addEventListener(
        "submit", async function(event) {
            event.preventDefault();

            const formData = new URLSearchParams({
                senderAccount:
                    document.getElementById("senderAccount").value,
                receiverAccount:
                    document.getElementById("receiverAccount").value,
                amount:
                    document.getElementById("transferAmount").value
            });

            try {
                const response = await fetch(
                    "http://localhost:8080/api/transfer",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded"
                        },
                        body: formData
                    }
                );

                const result = await response.text();
                alert(result);

            } catch (error) {
                alert("Could not connect to the banking server.");
                console.error(error);
            }
        }
    );
}
async function loadDashboardStats() {
    try {
        const response = await fetch(
            "http://localhost:8080/api/dashboard"
        );

        if (!response.ok) {
            throw new Error("Unable to load dashboard statistics");
        }

        const result = await response.text();
        const stats = result.split(",");

        document.getElementById("totalAccounts").textContent = stats[0];

        document.getElementById("totalDeposits").textContent =
            "₹" + stats[1];

        document.getElementById("totalTransactions").textContent =
            stats[2];

    } catch (error) {
        console.error("Dashboard error:", error);
    }
}

document.addEventListener("DOMContentLoaded", loadDashboardStats);